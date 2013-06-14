/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2007, Helios Development Group and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 *
 */
package org.helios.apmrouter.destination.opentsdb;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Delayed;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;

import net.opentsdb.core.TSDB;
import net.opentsdb.core.WritableDataPoints;

import org.apache.log4j.BasicConfigurator;
import org.hbase.async.HBaseClient;
import org.hbase.async.HBaseRpc;
import org.hbase.async.KeyValue;
import org.hbase.async.PleaseThrottleException;
import org.hbase.async.PutRequest;
import org.hbase.async.Scanner;
import org.helios.apmrouter.util.SystemClock;
import org.slf4j.LoggerFactory;

import com.stumbleupon.async.Callback;
import com.stumbleupon.async.Deferred;



/**
 * <p>Title: MetricRecorder</p>
 * <p>Description: Metric recorer for OpenTSDB</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.san.opentsdb.MetricRecorder</code></p>
 */
public class MetricRecorder extends NotificationBroadcasterSupport implements MetricRecorderMXBean, Callback<Object, Exception>, Runnable {
	/** The HBase connection URI (<code>hostname</code> or <code>hostname:port</code>)  */
	protected final String hbaseUri;
	/** The HBase client */
	protected final HBaseClient hClient;
	/** The time series DB client */
	protected final TSDB tsClient;
	private final MetricRecorder _recorder = this;
	/** The current drop timeout */
	private long dropTimeout = DEFAULT_DROPTIMEOUT;
	/** Total metric counter */
	protected final AtomicLong metricCounter = new AtomicLong(0);
	/** Throttle incident counter */
	protected final AtomicLong throttleCounter = new AtomicLong(0);
	/** Throttle time counter */
	protected final AtomicLong throttleTime= new AtomicLong(0);
	/** Dropped metrics counter */
	protected final AtomicLong dropCounter= new AtomicLong(0);
	/** A counter for asynch throttle exceptions received */
	protected final AtomicLong throttleExceptionCounter = new AtomicLong(0);
	/** A counter for asynch exceptions received that are not throttling related */
	protected final AtomicLong asynchExceptionCounter = new AtomicLong(0);
	
	/** The last time throttling started */
	protected long lastThrottleStartTime = 0;
	/** The throttling latch. If the value of the ref is null, then throttling is turned off */
	protected final AtomicReference<CountDownLatch> throttleLatch = new AtomicReference<CountDownLatch>(null);
	/** This object name */
	protected ObjectName objectName = null;
	
	/** A recording builder instance for each thread */
	private final ThreadLocal<Recording> recordingBuilder = new ThreadLocal<Recording>() {		
		protected Recording initialValue() {
			return new Recording(_recorder);
		}
	};
	/** A map of instances keyed by HBase URI  */
	private static final Map<String, MetricRecorder> instances = new ConcurrentHashMap<String, MetricRecorder>();
	/** The time series db table name  */
	public static final String timeSeriesTable = "tsdb";
	/** The unique name db table name  */
	public static final String uidTable = "tsdb-uid";
	/** The system property name of the property that specifies a default HBase URI */
	public static final String PROP_URI = "org.helios.opentsdb.hbaseuri";
	/** The default HBase URI */
	public static final String DEFAULT_URI = "localhost";
	/** The system property name of the property that specifies drop timeout in ms. */
	public static final String PROP_DROPTIMEOUT = "org.helios.opentsdb.droptimeout";
	/** The default Drop Timeout in ms. */
	public static final long DEFAULT_DROPTIMEOUT = 1000L;
	
	/** Notification sequence provider */
	private static final AtomicLong notifSequence = new AtomicLong(0);
	/** The JMX notification type for throttling started */
	public static final String NOTIF_STARTING = "org.helios.opentsdb.recorder.started";
	/** The JMX notification type for throttling ended */
	public static final String NOTIF_ENDING = "org.helios.opentsdb.recorder.ended";
	
	/** Static class logger */
	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(MetricRecorder.class);
	
	
	/** Writable DataPoints Cache */
	private final Map<String, WritableDataPoints> datapoints = new ConcurrentHashMap<String, WritableDataPoints>(100000);
	/** The throttle victim remuneration reprocessing queue */
	protected final ArrayBlockingQueue<PutRequest> reprocessorQueue = new ArrayBlockingQueue<PutRequest>(1000, false); 
	/** The throttle victim remuneration thread pool group */
	protected final ThreadGroup throttleVictimThreadGroup = new ThreadGroup("ThrottleVictimProcessorThreadGroup");
	/** The throttle victim remuneration thread pool and queue */
	protected final ThreadPoolExecutor throttleVictimProcessor = new ThreadPoolExecutor(1, 1, Long.MAX_VALUE, TimeUnit.DAYS, new ArrayBlockingQueue<Runnable>(10, false), 
		new ThreadFactory(){
			public Thread newThread(Runnable r) {
				Thread t = new Thread(throttleVictimThreadGroup, r, "ThrottleVictimProcessorThread");
				t.setDaemon(true);
				t.setPriority(Thread.MAX_PRIORITY);
				return t;
			}
		}
	);
	
	/** Metric name and tag hash code cache */
	//private static final NonBlockingSetInt nameCache = new NonBlockingSetInt();
	private final Set<Integer> nameCache = new CopyOnWriteArraySet<Integer>();
	
	// -Dorg.helios.opentsdb.hbaseuri=njwUbuntu:2181
	
	
	
	public static void main(String[] args) {
		System.setProperty("java.net.preferIPv4Stack", "true");
		log("OpenTSDB MetricRecorder Test");
		System.setProperty("tsd.core.auto_create_metrics", "true"); 
		Random r = new Random(System.nanoTime());
		MetricRecorder recorder = MetricRecorder.getInstance("pdk-pt-cupas-01:2181");
		for(int i = 0; i < 100; i++) {
//			long value = Math.abs(r.nextInt(100));
//			recorder.newRecording("test.recording", value).tag("allocation", "vertical").record();
//			log("Recorded:" + value);
			collectGc(recorder);
			try { Thread.currentThread().join(5000); } catch (Exception e) {}
		}
		try { recorder.shutdown(); } catch (Exception e) {}
	}
	
	protected static void collectGc(MetricRecorder recorder) {
		SystemClock.startTimer();
		for(GarbageCollectorMXBean gc: ManagementFactory.getGarbageCollectorMXBeans()) {			
			String name = gc.getName();
			//tracer.traceDelta(gc.getCollectionCount(), "CollectionCount", "JVM", "GarbageCollection", name);
			recorder.newRecording("CollectionCount", gc.getCollectionCount())
				.tag("platform", "JVM")
				.tag("group", "GarbageCollection")
				.tag("name", name)
				.record();
			recorder.newRecording("CollectionTime", gc.getCollectionTime())
			.tag("platform", "JVM")
			.tag("group", "GarbageCollection")
			.tag("name", name)
			.record();
		}
		log("GC Recorded in " + SystemClock.endTimer());

	}	
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	public void flush(boolean synch) {
		if(synch) {
			try {
				tsClient.flush().joinUninterruptibly();
			} catch (Exception e) {
				throw new RuntimeException("Failed to call synch flush", e);
			}
		} else {
			tsClient.flush();
		}
	}

	/**
	 * Runnable that processes the reprocessor queue and if it is empty for > 1 sec. stops throttling (if applicable)
	 */
	public void run() {
		while(true) {
			try {
				PutRequest pr = reprocessorQueue.poll(1000, TimeUnit.MILLISECONDS);
				if(pr!=null) {
					try {
						hClient.put(pr).joinUninterruptibly(dropTimeout);
					} catch (Exception e) {
						dropCounter.incrementAndGet();
					}
				} else {
					
					CountDownLatch latch = throttleLatch.get();
					if(latch!=null) {
						LOG.info("Throttling Over.....");
						latch.countDown();
						throttleLatch.set(null);
						throttleTime.addAndGet(System.currentTimeMillis()-lastThrottleStartTime);
						this.sendNotification(new Notification(NOTIF_ENDING, objectName==null ? this : objectName, notifSequence.incrementAndGet(), System.currentTimeMillis(), "Throttling Ended"));
					}
				}
			} catch (Exception e) {
				if(throttleVictimProcessor.isShutdown() || throttleVictimProcessor.isTerminating()) {
					break;
				}
			}
		}
	}
	
	/**
	 * Shuts down the time series client
	 */
	public void shutdown() {
		throttleVictimProcessor.shutdownNow();		
		if(tsClient!=null) {
			try {
				tsClient.flush().joinUninterruptibly(3000);
			} catch (Exception e) {}
			try {
				tsClient.shutdown();
			} catch (Exception e) {}
		}
	}
	
	/**
	 * Flushes the reprocessing queue causing all the pending metric submissions to be dropped.
	 */
	public void flushReprocessingQueue() {
		dropCounter.addAndGet(reprocessorQueue.size());
		reprocessorQueue.clear();
	}
	
	/**
	 * Acquires the singleton MetricRecorder for the passed HBase URI
	 * @param hbaseUri The HBase connection URI (<code>hostname</code> or <code>hostname:port</code>)
	 * @return the singleton MetricRecorder for the passed HBase URI
	 */
	public static MetricRecorder getInstance(String hbaseUri) {
		if(hbaseUri==null) throw new IllegalArgumentException("The passed HBase URI was null", new Throwable());
		hbaseUri = hbaseUri.trim();
		MetricRecorder recorder = instances.get(hbaseUri);
		if(recorder==null) {
			synchronized(instances) {
				recorder = instances.get(hbaseUri);
				if(recorder==null) {
					recorder = new MetricRecorder(hbaseUri);
					recorder.populateMetricNameCache();
					instances.put(hbaseUri, recorder);
					try {
						recorder.objectName = new ObjectName(MetricRecorder.class.getPackage().getName() + ":quorum=" + hbaseUri.replace(':', ';'));
						ManagementFactory.getPlatformMBeanServer().registerMBean((MetricRecorderMXBean)recorder, recorder.objectName);
					} catch (Exception ex) {
						LOG.warn("Failed to register management interface for recorder [" + hbaseUri + "]", ex);
					}
				}
			}
		}
		LOG.warn("Recorder for [" + hbaseUri + "] created. Flush Period is: " +  recorder.hClient.getFlushInterval() + " ms.");
		return recorder;
	}
	
	/**
	 * Retrieves the writable datapoint for the passed metric name and tag map
	 * @param metric The metric name for the datapoint
	 * @param tags The tag map for the datapoint
	 * @return the writable data point
	 */
	private WritableDataPoints getDataPoints(final String metric, final Map<String, String> tags) {
	    final String key = metric + tags;
	    WritableDataPoints dp = datapoints.get(key);
	    if (dp != null) {
	      return dp;
	    }
	    dp = tsClient.newDataPoints();
	    try {
	    	String _metric = metric.replace("\\", "");
	    	dp.setSeries(_metric, tags);
	    } catch (Exception ex) {
	    	//ex.printStackTrace(System.err);
	    	LOG.debug("Failed to get data points for [" + metric + "]:" + tags, ex);
	    	return null;
	    }
	    dp.setBatchImport(true);
	    datapoints.put(key, dp);
	    return dp;
	  }
	
	
	/**
	 * Prints some basic localStats on the metric recorder
	 */
	public void printStats() {
		final StringBuilder s = new StringBuilder("\nMetric Recorder Stats:");
		//s.append("\n\t").append(tsClient.getPutLatencyHistogram());
		s.append("\n\tMetric Count:").append(metricCounter.get());		
		s.append("\n\tContendedMetaLookupCount:").append(hClient.contendedMetaLookupCount());
		s.append("\n\tUncontendedMetaLookupCount:").append(hClient.uncontendedMetaLookupCount());
		s.append("\n\tRootLookupCount:").append(hClient.rootLookupCount());
		s.append("\n\tThrottleCount:").append(throttleCounter.get());
		s.append("\n\tThrottleTime:").append(throttleTime.get());
		s.append("\n\tDataPointCacheSize:").append(datapoints.size());
		s.append("\n\tDropCount:").append(dropCounter.get());
		
		
//		s.append("\n\tTSDBStats:\n");
//		tsClient.collectStats(new StatsCollector("tsd") {
//	        @Override
//	        public final void emit(final String line) {
//	          s.append("\t\t").append(line);
//	        }
//	      });
		LOG.info(s.toString());
	}
	
	
	
	/**
	 * Populates the metric name cache with all the known metric names
	 */
	protected void populateMetricNameCache() {
		try {
			Scanner scanner = hClient.newScanner("tsdb-uid");
			scanner.setFamily("name");
			scanner.setQualifier("metrics");
			ArrayList<ArrayList<KeyValue>> results = null;
			while((results = scanner.nextRows().joinUninterruptibly())!=null) {
				for(ArrayList<KeyValue> arrList: results) {
					for(KeyValue keyValue: arrList) {
						String metricName = new String(keyValue.value());
						if(!metricName.startsWith("san.")) continue;
						nameCache.add(metricName.hashCode());
						LOG.warn("Added Metric Name [" + metricName + "] to cache");
					}
				}
			}
			LOG.warn("Added [" + nameCache.size() + "] to cache");
		} catch (Exception e) {
			LOG.error("Failed to populate metric name cache", e);
			throw new RuntimeException("Failed to populate metric name cache", e);
		}
	}
	
	
//	import org.hbase.async.*;
//	import net.opentsdb.core.*;
//	import java.util.concurrent.*;
//	import org.slf4j.*;
//	import ch.qos.logback.classic.*;
//
//
//	LoggerFactory.getLogger("org.hbase").setLevel(Level.WARN);
//	LoggerFactory.getLogger("org.apache").setLevel(Level.WARN);
//	LoggerFactory.getLogger("com.stumbleupon").setLevel(Level.WARN);
//	Random r = new Random(System.nanoTime());
//	byte[] METRICS = "metrics".getBytes();
//	hb = new HBaseClient("njwUbuntu:2181");
//	def tsdb = null;
//	def scanner = null;
//	try {
//	    tsdb = new TSDB(hb, "tsdb", "tsdb-uid");
//	    scanner = hb.newScanner("tsdb-uid");
//	    scanner.setFamily("name");
//	    //println "Scanner:${scanner}";
//	    // Deferred<ArrayList<ArrayList<KeyValue>>>
//	    def results = null;
//	    
//	    //println results;
//	    while((results = scanner.nextRows().joinUninterruptibly())!=null) {  
//	        results.each() { 
//	            it.each() {
//	                if(Arrays.equals(METRICS, it.qualifier())) {            
//	                    println "Value:${new String(it.value())}";
//	                }
//	            }
//	        }    
//	    }



	
	
	/**
	 * Returns the first recorder found in the instance cache
	 * @return a MetricRecorder or null if none are created
	 */
	public static MetricRecorder getInstance() {
		if(instances.isEmpty()) {
			return getInstance(System.getProperty(PROP_URI, DEFAULT_URI));			
		} else {
			return instances.values().iterator().next();
		}
		
	}
	
	
	/**
	 * Creates a new MetricRecorder
	 * @param hbaseUri The connection string for hbase (<code>hostname</code> or <code>hostname:port</code>) 
	 */
	private MetricRecorder(String hbaseUri) {
		super(Executors.newFixedThreadPool(2), new MBeanNotificationInfo(new String[]{NOTIF_STARTING, NOTIF_ENDING}, "Throttling", "Notifications issued when throttling starts or ends"));
		System.setProperty("tsd.core.auto_create_metrics", "true"); 
		this.hbaseUri = hbaseUri;
		hClient = new HBaseClient(this.hbaseUri);
		tsClient = new TSDB(hClient, timeSeriesTable, uidTable);
		dropTimeout = Long.parseLong(System.getProperty(PROP_DROPTIMEOUT, "" + DEFAULT_DROPTIMEOUT));
		throttleVictimProcessor.execute(this);
	}

	/**
	 * Transmits the passed recording to the time series database
	 * @param recording the recording to transmit 
	 */
	public void record(Recording recording) {
		if(!checkThrottleLatch()) return;
		//validate(recording.name, recording.tags, recording.value, recording.timestamp);
		//tsClient.addPoint(recording.name, recording.timestamp,  recording.value, recording.tags);
        final WritableDataPoints dp = getDataPoints(recording.name, recording.tags);
        if(dp==null) {
        	dropCounter.incrementAndGet();
        	return;
        }
        Deferred<Object> d;
        try {
        	d = dp.addPoint(recording.timestamp, recording.value);
            d.addErrback(this);
            metricCounter.incrementAndGet();                	
        } catch (IllegalArgumentException iae) {
        	if(iae.getMessage().startsWith("New timestamp=")) {
        		dropCounter.incrementAndGet();
        	} else {
        		throw iae;
        	}
        }
	}
	
	/**
	 * Checks the throttle latchand waits on it if we're throttling
	 * @return
	 */
	private boolean checkThrottleLatch() {
		CountDownLatch latch = throttleLatch.get();
		if(latch!=null) {
			try {
				if(!latch.await(dropTimeout, TimeUnit.MILLISECONDS)) {
					dropCounter.incrementAndGet();
					return false;
				}
			} catch (Exception e) {
				LOG.error("Thread was interrupted while waiting on throttle latch", e);
				return false;
			}
        }		
		return true;
	}
	
	
	/**
	 * Returns the count of submitted metrics since the last reset
	 * @return the count of submitted metrics since the last reset
	 */
	public long getMetricCounter() {
		return metricCounter.get();
	}
	
	/**
	 * Returns the count of dropped metrics since the last reset
	 * @return the count of dropped metrics since the last reset
	 */
	public long getDropCounter() {
		return dropCounter.get();
	}
	
	
	/**
	 * Returns the count of submitted metrics since the last reset and resets the counter
	 * @return the count of submitted metrics since the last reset
	 */
	public long getMetricAndResetCounter() {
		long value =  metricCounter.get();
		resetMetrics();
		return value;		
	}
	
	/**
	 * Returns the number of entries in the reprocessor queue
	 * @return the number of entries in the reprocessor queue 
	 */
	public int getReprocessorQueueSize() {
		return reprocessorQueue.size();
	}
	
	/**
	 * Indicates if the recorder is throttling
	 * @return true if the recorder is throttling, false otherwise
	 */
	public boolean isThrottling() {
		return throttleLatch.get()!=null;
	}
	
	/**
	 * Resets the metric counters
	 */
	public void resetMetrics() {
		metricCounter.set(0L);
		throttleCounter.set(0L);
		throttleTime.set(0L);
		dropCounter.set(0L);
		throttleExceptionCounter.set(0L);
		asynchExceptionCounter.set(0L);
	}
	
	/**
	 * Resets the metric count counter only
	 */
	public void resetMetricCount() {
		metricCounter.set(0L);
	}
	
	protected void validate(String name, Map<String, String> tags, long value, long timestamp) {
		int code = name.hashCode();
		if(!nameCache.contains(code)) {
			synchronized(nameCache) {
				if(!nameCache.contains(code)) {
					try {	
						
						tsClient.addPoint(name, timestamp, value, tags);
						tsClient.flush().joinUninterruptibly();
						nameCache.add(code);
					} catch (Exception e) {
						LOG.error("Failed to validate metric name [" + name + "]", e);
					}
				}
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.stumbleupon.async.Callback#call(java.lang.Object)
	 */
	@Override
	public Object call(Exception ex) throws Exception {
		if (ex instanceof PleaseThrottleException) {
			throttleExceptionCounter.incrementAndGet();
			final PleaseThrottleException e = (PleaseThrottleException) ex;
            LOG.warn("[" + Thread.currentThread().getName() + "] Need to throttle, HBase isn't keeping up.", e);
            final HBaseRpc rpc = e.getFailedRpc();
            if (rpc instanceof PutRequest) {
              //hClient.put((PutRequest) rpc);  // Don't lose edits.
            	if(!reprocessorQueue.offer((PutRequest) rpc)) {
            		dropCounter.incrementAndGet();
            	}
            }            
            if(throttleLatch.get()==null) {
            	throttleLatch.set(new CountDownLatch(1));
            	lastThrottleStartTime = System.currentTimeMillis();
            	throttleCounter.incrementAndGet();
            	this.sendNotification(new Notification(NOTIF_STARTING, objectName==null ? this : objectName, notifSequence.incrementAndGet(), System.currentTimeMillis(), "Throttling Started"));
            }
        } else {
        	asynchExceptionCounter.incrementAndGet();
        }
        return null;
     }
	
	
	/**
	 * Returns the number of asynch throttle exceptions received since the last reset
	 * @return the number of asynch throttle exceptions received since the last reset
	 */
	public long getThrottleExceptionCounter() {
		return throttleExceptionCounter.get();
	}
	
	/**
	 * Returns the number of asynch exceptions received since the last reset
	 * @return the number of asynch exceptions received since the last reset
	 */
	public long getAsynchExceptionCounter() {
		return asynchExceptionCounter.get();
	}
	
	
	/**
	 * Starts a new recording
	 * @param name The metric name
	 * @param value The metric value
	 * @return a new recording
	 */
	public Recording newRecording(String name, long value) {
		return recordingBuilder.get().record(name, value);
	}
	
	/**
	 * Submits a recording
	 * @param name The metric name
	 * @param value The metric value
	 * @param tags The metric tags
	 */
	public void record(String name, long value, Map<String, String> tags) {
		recordingBuilder.get().record(name, value).tags(tags).record();
	}
	
	
	/**
	 * Returns how many lookups in .META. were performed (contended).
	 * @return the number of contended .META lookups
	 * @see org.hbase.async.HBaseClient#contendedMetaLookupCount()
	 */
	public long getContendedMetaLookupCount() {
		return hClient.contendedMetaLookupCount();
	}

	/**
	 * Returns how many lookups in .META. were performed (uncontended).
	 * @return the number of uncontended .META lookups
	 * @see org.hbase.async.HBaseClient#uncontendedMetaLookupCount()
	 */
	public long getUncontendedMetaLookupCount() {
		return hClient.uncontendedMetaLookupCount();
	}

	/**
	 * Returns the number of cache hits during lookups involving UIDs.
	 * @return the number of cache hits during lookups involving UIDs.
	 * @see net.opentsdb.core.TSDB#uidCacheHits()
	 */
	public int getUidCacheHits() {
		return tsClient.uidCacheHits();
	}

	/**
	 * Returns the number of cache misses during lookups involving UIDs.
	 * @return the number of cache misses during lookups involving UIDs.
	 * @see net.opentsdb.core.TSDB#uidCacheMisses()
	 */
	public int getUidCacheMisses() {
		return tsClient.uidCacheMisses();
	}

	/**
	 * Returns the number of cache entries currently in RAM for lookups involving UIDs. 
	 * @return the number of cache entries currently in RAM for lookups involving UIDs.
	 * @see net.opentsdb.core.TSDB#uidCacheSize()
	 */
	public int getUidCacheSize() {
		return tsClient.uidCacheSize();
	}
	
	/**
	 * Returns the number of throttle incidents since the last reset
	 * @return the number of throttle incidents since the last reset
	 */
	public long getThrottleIncidentCount() {
		return throttleCounter.get();
	}
	
	/**
	 * Returns the amount of throttle time (in ns.) since the last reset
	 * @return the amount of throttle time since the last reset
	 */
	public long getThrottleTime() {
		return throttleTime.get();
	}
	
	/**
	 * Returns the drop timeout in ms.
	 * @return the drop timeout in ms.
	 */
	public long getDropTimeout() {
		return dropTimeout;
	}

	/**
	 * Sets the drop timeout in ms.
	 * @param dropTimeout the drop timeout in ms.
	 */
	public void setDropTimeout(long dropTimeout) {
		this.dropTimeout = dropTimeout;
	}	
	
	
	
	/**
	 * <p>Title: Recording</p>
	 * <p>Description: A recording builder</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.san.opentsdb.MetricRecorder.Recording</code></p>
	 */
	public static class Recording implements Delayed {
		protected final MetricRecorder recorder;
		protected String name;
		protected long barrier = Long.MIN_VALUE;
		protected long timestamp= Long.MIN_VALUE;
		protected long value= Long.MIN_VALUE;
		protected final Map<String, String> tags = new HashMap<String, String>();
		
		/**
		 * Creates a new Recording
		 * @param recorder
		 */
		public Recording(MetricRecorder recorder) {
			this.recorder = recorder;
		}
		
		
		/**
		 * Initializes a new recording
		 * @param name The metric name
		 * @param value The metric value
		 * @return this builder
		 */
		public Recording record(String name, long value) {
			if(name==null) throw new IllegalArgumentException("The passed metric name was null", new Throwable());
			this.name = name;
			this.value = value;			
			time();
			return this;
		}
		
		
		/**
		 * Sets the recording timestamp
		 * @return this builder
		 */
		public Recording time() {
			long now = System.currentTimeMillis();
			this.timestamp = TimeUnit.SECONDS.convert(now, TimeUnit.MILLISECONDS);
			this.barrier = now + 1000;  // one second from now
			return this;
		}
		
		public Recording tags(Map<String, String> tags) {
			this.tags.clear();
			this.tags.putAll(tags);
			return this;
		}
		
		/**
		 * Adds a tag
		 * @param key The tag key
		 * @param value The tag value
		 * @return this builder
		 */
		public Recording tag(Object key, Object value) {
			if(key==null) throw new IllegalArgumentException("The passed tag key was null", new Throwable());
			if(value==null) throw new IllegalArgumentException("The passed tag value was null", new Throwable());
			tags.put(key.toString().replace(" ", ""), value.toString().replace(" ", ""));
			return this;
		}
		
		/**
		 * Resets the tags
		 * @return this builder
		 */
		public Recording resetTags() {
			tags.clear();
			return this;
		}
		
		/**
		 * Submits the built recording
		 */
		public void record() {
			recorder.record(this);
		}


		@Override
		public int compareTo(Delayed other) {
			long thisDelay = getDelay(TimeUnit.MILLISECONDS), thatDelay = other.getDelay(TimeUnit.MILLISECONDS);
			if(thisDelay==thatDelay) return -1;
			return (thisDelay<thatDelay ? -1 : 1);

		}


		@Override
		public long getDelay(TimeUnit unit) {
			return unit.convert(barrier-System.currentTimeMillis(), TimeUnit.MILLISECONDS);
		}
	}










	
}


