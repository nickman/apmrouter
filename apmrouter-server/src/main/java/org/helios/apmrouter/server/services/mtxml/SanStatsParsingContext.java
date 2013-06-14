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
package org.helios.apmrouter.server.services.mtxml;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.cliffc.high_scale_lib.Counter;
import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.helios.apmrouter.metric.MetricType;
import org.helios.apmrouter.trace.ITracer;

/**
 * <p>Title: SanStatsParsingContext</p>
 * <p>Description: A context shared across parsing operations for a single san stat xml document</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.services.mtxml.SanStatsParsingContext</code></p>
 */

public class SanStatsParsingContext {
/*
the tags that are important are the vv_name, host_name, port_node
port_node is not as important as vv_name and host
typically we would want to see the metrics broken down like this
sum across the array
sum by host
sum by vv_name
sum by port_node
on a deep dive trying to find issues on a host we might break it down by: host, vv_name
host, port
host, vv_name, port
the top two would be sum by host and sum by vv_name

 */
	/** Static class logger */
	protected static final Logger log = Logger.getLogger(SanStatsParserTracer.class);
	
	/** An unknown value */
	public static final String UNKNOWN = "<unknown>";
	
	/** The system serial number */
	private String serialNumber = UNKNOWN;
	/** The system name */
	private String systemName = UNKNOWN;
	/** The system cpu speed */
	private int cpuMhz = -1;
	/** The ip name */
	private String ipName = UNKNOWN;
	/** The system os version */
	private String osVersion = UNKNOWN;
	/** The system model name */
	private String modelName = UNKNOWN;
	/** The cache size in mb */
	private int cacheSize = -1;
	
	/** The tag name for the current timestamp */
	public static final String NOW = "now";	
	/** The tag name for the queue length */
	public static final String QUEUE_LENGTH = "qlen";
	/** The tag name for the busy time */
	public static final String BUSY_TIME = "busy";
	/** The tag name for the read count */
	public static final String READ_COUNT = "rcount";
	/** The tag name for the bytes read count */
	public static final String READ_BYTES = "rbytes";
	/** The tag name for the read errors */
	public static final String READ_ERRORS = "rerror";
	/** The tag name for the read drops */
	public static final String READ_DROPS= "rdrops";
	/** The tag name for the read ticks */
	public static final String READ_TICKS = "rticks";
	/** The tag name for the write count */
	public static final String WRITE_COUNT = "wcount";
	/** The tag name for the bytes write count */
	public static final String WRITE_BYTES = "wbytes";
	/** The tag name for the write errors */
	public static final String WRITE_ERRORS = "werror";
	/** The tag name for the write drops */
	public static final String WRITE_DROPS= "wdrops";
	/** The tag name for the write ticks */
	public static final String WRITE_TICKS = "wticks";

	/** The metric name for the queue length */
	public static final String QLENGTH = "QueueLength";
	/** The metric name for the ios per second */
	public static final String IOPS = "IOsPerSec";
	/** The metric name for the bytes per second */
	public static final String BPS = "BytesPerSec";
	/** The metric name for the service time */
	public static final String SVCTIME = "ServiceTime";
	/** The metric name for the IO Size */
	public static final String IOSIZE = "IOSize";
	/** The metric name for the busy time */
	public static final String BUSYTIME = "BusyTime";
	/** The metric name for the read drops */
	public static final String READDROPS = "ReadDrops";
	/** The metric name for the read errors */
	public static final String READERRORS = "ReadErrors";
	/** The metric name for the write drops */
	public static final String WRITEDROPS = "WriteDrops";
	/** The metric name for the write errors */
	public static final String WRITEERRORS = "WriteErrors";
	
	/** The metric names to be recorded */
	public static final Set<String> METRIC_NAMES = Collections.unmodifiableSet(new HashSet<String>(
			Arrays.asList(QLENGTH, IOPS, BPS, SVCTIME, IOSIZE, BUSYTIME, READDROPS, READERRORS, WRITEDROPS, WRITEERRORS)
	));
	
	
	/** The tag name for the virtual vlun name */
	public static final String VVNAME = "vv_name";
	/** The tag name for the virtual vlun host name */
	public static final String VVHOSTNAME = "host_name";
	/** The tag name for the virtual vlun port node */
	public static final String PORTNODE = "port_node";
	/** The tag name for the virtual vlun port slot */
	public static final String PORTSLOT = "port_slot";
	/** The tag name for the virtual vlun port port */
	public static final String PORTPORT = "port_port";
	
	/** The granularity format */
	protected final String gformat;
	/** Indicates if the parse should add random pos values for testing purposes. Defaults to false */
	protected final boolean testValues;
	/** The aggregation counter tag names */
	protected static final String[] COUNTER_TAGS = new String[]{
		QUEUE_LENGTH, BUSY_TIME, 
		READ_COUNT, READ_BYTES, READ_ERRORS, READ_DROPS, READ_TICKS, 
		WRITE_COUNT, WRITE_BYTES, WRITE_ERRORS, WRITE_DROPS, WRITE_TICKS
	};
	/** The aggregation counter metric types that should positionally match the {@link #COUNTER_TAGS}  */
	protected static final MetricType[] COUNTER_METRIC_TYPES = new MetricType[]{
		MetricType.LONG_GAUGE, MetricType.DELTA_GAUGE, 
		MetricType.DELTA_GAUGE, MetricType.DELTA_GAUGE, MetricType.DELTA_GAUGE, MetricType.DELTA_GAUGE, MetricType.DELTA_GAUGE, 
		MetricType.DELTA_GAUGE, MetricType.DELTA_GAUGE, MetricType.DELTA_GAUGE, MetricType.DELTA_GAUGE, MetricType.DELTA_GAUGE,		
	};
	
	/** The number of successfully parsed lun fragments */
	private final AtomicInteger lunsParsed = new AtomicInteger(0);
	/** The completion queue */
	private final BlockingQueue<Map<String, String>> completionQueue = new ArrayBlockingQueue<Map<String, String>>(3000, false);
	
	
	// ================================================================================================
	//   Aggregations
	// ================================================================================================
	/** The total values across the array */
	protected final NonBlockingHashMap<String, NonBlockingHashMap<String, Counter>> arrayTotals = new NonBlockingHashMap<String, NonBlockingHashMap<String, Counter>>(1024);
	/** The aggregated and calced values for each node */
	protected final NonBlockingHashMap<String, NonBlockingHashMap<String, Long>> arrayCalcedTotals = new NonBlockingHashMap<String, NonBlockingHashMap<String, Long>>(1024);
	/** The parsed metric namespaces keyed by the node key */
	protected final NonBlockingHashMap<String, String[]> metricNameSpaces = new NonBlockingHashMap<String, String[]>(1024);
	

	/**
	 * Returns the number of unique nodes collected for in this context
	 * @return the number of unique nodes collected for in this context
	 */
	public int getNodeCount() {
		return arrayTotals.size();
	}
	
	/**
	 * Increments the counter for the passed aggregate key by the passed amount
	 * @param granularityKey The target granularity key that identifies the SAN node for which stats are beng supplied.
	 * @param valueKey The key of the counter stat which is being supplied
	 * @param value The value to increment by
	 */
	protected void increment(String granularityKey, String valueKey, long value) {
		NonBlockingHashMap<String, Counter> counterMap = arrayTotals.get(granularityKey);
		if(counterMap==null) {
			synchronized(arrayTotals) {
				counterMap = arrayTotals.get(granularityKey);
				if(counterMap==null) {
					counterMap = new NonBlockingHashMap<String, Counter>(COUNTER_TAGS.length);
					initCounters(counterMap, COUNTER_TAGS);
					arrayTotals.put(granularityKey, counterMap);
					granularityKey = granularityKey.trim();
					while(granularityKey.startsWith("/")) {
						granularityKey = granularityKey.substring(1);
					}
					String[] tags=  granularityKey.trim().split("/");
					metricNameSpaces.put(granularityKey, tags);
				}
			}
		}
		counterMap.get(valueKey).add(value);
	}
	
	
	/**
	 * Intializes the passed counter map with a target key and counter for each key
	 * @param counterMap The map to insert into
	 * @param keys The keys to insert
	 */
	protected void initCounters(NonBlockingHashMap<String, Counter> counterMap, String...keys) {
		for(String s: keys) {
			counterMap.put(s, new Counter());
		}
	}
	
	
	/**
	 * Creates a new SanStatsParsingContext and initializes the aggregate counters
	 * @param gformat The granularity format
	 */
	public SanStatsParsingContext(String gformat) {
		this.gformat = gformat;		
		this.testValues = false;
	}
	
	/**
	 * Creates a new SanStatsParsingContext and initializes the aggregate counters
	 * @param gformat The granularity format
	 * @param testValues Indicates if test values should be added to the values read in from the channel buffer 
	 */
	public SanStatsParsingContext(String gformat, boolean testValues) {
		this.gformat = gformat;
		this.testValues = testValues;
	}

	
	
	
	/**
	 * Sets the system info
	 * @param sysInfo a map of the system info values keyed by the xml tag name
	 */
	public void setSysInfo(Map<String, String> sysInfo) {
		serialNumber = sysInfo.get("serial_number");
		systemName = sysInfo.get("sys_name");
		ipName = sysInfo.get("ip_name");
		osVersion = sysInfo.get("os_version");
		modelName = sysInfo.get("model_name");
		cacheSize = Integer.parseInt(sysInfo.get("ch_size_mb"));
		cpuMhz = Integer.parseInt(sysInfo.get("cpu_mhz"));
		
	}
	
	
	/**
	 * Returns the value for the passed key from the array aggregate
	 * @param valueKey The key of the metric type being retrieved
	 * @param keyParts The key to retrieve the value for
	 * @return the long value
	 */
	public long getArrayValue(String valueKey, String...keyParts) {		
		return arrayTotals.get(String.format(gformat, keyParts)).get(valueKey).get();
	}
	
	/**
	 * Adds the passed value to the counter in the passed counter map keyed by the nodekey, creating a new counter if one does not exist
	 * @param counterMap The counter map to update
	 * @param nodeKey The node key of the target counter to update or create
	 * @param value The value to add to the counter
	 */
	protected void addToMap(NonBlockingHashMap<String, Long> counterMap, String nodeKey, long value) {		
		Counter current = counterMap.get(nodeKey);
		if(current==null) {
			synchronized(counterMap) {
				current = counterMap.get(nodeKey);
				if(current==null) {
					current = new Counter();
					counterMap.put(nodeKey, current);
				}
			}
		}
		current.add(value);
	}
	
	/**
	 * Adds a vlunstat sampling
	 * @param vlunstats a vlunstat sampling
 
	 */
	public void addVLun(Map<String, String> vlunstats)  {
		
		long[] lvalues = new long[13];
		int seq = 0;
		
		lvalues[seq++] = Long.parseLong(vlunstats.get(NOW));			// 0		
		lvalues[seq++] = Long.parseLong(vlunstats.get(QUEUE_LENGTH));	// 1	
		lvalues[seq++] = Long.parseLong(vlunstats.get(BUSY_TIME));		// 2
		
		lvalues[seq++] = Long.parseLong(vlunstats.get(READ_COUNT));		// 3
		lvalues[seq++] = Long.parseLong(vlunstats.get(READ_BYTES));		// 4
		lvalues[seq++] = Long.parseLong(vlunstats.get(READ_ERRORS));	// 5
		lvalues[seq++] = Long.parseLong(vlunstats.get(READ_DROPS));		// 6
		lvalues[seq++] = Long.parseLong(vlunstats.get(READ_TICKS));		// 7
		
		lvalues[seq++] = Long.parseLong(vlunstats.get(WRITE_COUNT));	// 8
		lvalues[seq++] = Long.parseLong(vlunstats.get(WRITE_BYTES));	// 9
		lvalues[seq++] = Long.parseLong(vlunstats.get(WRITE_ERRORS));	// 10
		lvalues[seq++] = Long.parseLong(vlunstats.get(WRITE_DROPS));	// 11
		lvalues[seq++]= Long.parseLong(vlunstats.get(WRITE_TICKS));		// 12

		
		
		// =====================================================
		//   Calculate the granularity appropriate key
		// =====================================================
		String nodeKey = String.format(gformat, 	
				systemName, 
				vlunstats.get(VVNAME), 
				vlunstats.get(VVHOSTNAME), 
				vlunstats.get(PORTNODE),
				vlunstats.get(PORTSLOT),
				vlunstats.get(PORTPORT)
		);
		
		sumTotals(nodeKey, lvalues);
		
		//========================
		// Update calced values
		//========================
		NonBlockingHashMap<String, Long> cm = arrayCalcedTotals.get(nodeKey);
		if(cm==null) {
			synchronized(arrayCalcedTotals) {
				cm = arrayCalcedTotals.get(nodeKey);
				if(cm==null) {
					cm = new NonBlockingHashMap<String, Long>(10);
					arrayCalcedTotals.put(nodeKey, cm);
				}
			}
		}
		NonBlockingHashMap<String, Counter> at = arrayTotals.get(nodeKey);
		addToMap(at, QLENGTH, lvalues[1]);
		addToMap(at, IOPS, calcIosPerSec(lvalues[0], at));
		addToMap(at, BPS, calcBytesPerSec(lvalues[0], at));
		addToMap(at, SVCTIME, calcServiceTime(at));
		addToMap(at, IOSIZE, calcIoSize(at));
		addToMap(at, BUSYTIME, calcBusyTime(lvalues[0], at));
		addToMap(at, READERRORS, lvalues[5]);
		addToMap(at, READDROPS, lvalues[6]);
		addToMap(at, WRITEERRORS, lvalues[10]);
		addToMap(at, WRITEDROPS, lvalues[11]);

		cm.put(QLENGTH, at.get(QUEUE_LENGTH).get());
//		cm.put(IOPS, calcIosPerSec(lvalues[0], at));
//		cm.put(BPS, calcBytesPerSec(lvalues[0], at));
//		cm.put(SVCTIME, calcServiceTime(at));
//		cm.put(IOSIZE, calcIoSize(at));
//		cm.put(BUSYTIME, calcBusyTime(lvalues[0], at));
//		cm.put(READERRORS, lvalues[5]);
//		cm.put(READDROPS, lvalues[6]);
//		cm.put(WRITEERRORS, lvalues[10]);
//		cm.put(WRITEDROPS, lvalues[11]);
		
//		lvalues[seq++] = Long.parseLong(vlunstats.get(READ_ERRORS));	// 5
//		lvalues[seq++] = Long.parseLong(vlunstats.get(READ_DROPS));		// 6		
//		lvalues[seq++] = Long.parseLong(vlunstats.get(WRITE_ERRORS));	// 10
//		lvalues[seq++] = Long.parseLong(vlunstats.get(WRITE_DROPS));	// 11
		
		
		lunsParsed.incrementAndGet();

		try {
			completionQueue.put(vlunstats);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns a positive random int between zero and the passed value.
	 * @param upTo The max range of the random number
	 * @return the random value
	 */
	protected static int getTestValue(int upTo) {
		return Math.abs(ThreadLocalRandom.current().nextInt(upTo));
	}
	
	/**
	 * Returns a positive random int
	 * @return the random value
	 */
	protected static int getTestValue() {
		return getTestValue(Integer.MAX_VALUE);
	}
	
	
	/**
	 * Traces the contents of this context
	 * @param tracer The tracer to trace the stats with
	 */
	public void traceStats(ITracer tracer) {
		// {QLENGTH, IOPS, BPS, SVCTIME, IOSIZE, BUSYTIME};
		for(Map.Entry<String, NonBlockingHashMap<String, Long>> entry: arrayCalcedTotals.entrySet()) {
			NonBlockingHashMap<String, Long> cm = entry.getValue();
			if(testValues) {
				tracer.traceGauge(cm.get(QLENGTH)+getTestValue(10), QLENGTH, metricNameSpaces.get(entry.getKey()));
				tracer.traceGauge(cm.get(IOPS)+getTestValue(100), IOPS, metricNameSpaces.get(entry.getKey()));
				tracer.traceGauge(cm.get(BPS)+getTestValue(10000), BPS, metricNameSpaces.get(entry.getKey()));
				tracer.traceGauge(cm.get(SVCTIME)+getTestValue(500000), SVCTIME, metricNameSpaces.get(entry.getKey()));
				tracer.traceGauge(cm.get(IOSIZE)+getTestValue(2000000), IOSIZE, metricNameSpaces.get(entry.getKey()));
				tracer.traceGauge(cm.get(BUSYTIME)+getTestValue(5), BUSYTIME, metricNameSpaces.get(entry.getKey()));
				tracer.traceDeltaGauge(cm.get(READERRORS)+getTestValue(10), READERRORS, metricNameSpaces.get(entry.getKey()));
				tracer.traceDeltaGauge(cm.get(READDROPS)+getTestValue(10), READDROPS, metricNameSpaces.get(entry.getKey()));
				tracer.traceDeltaGauge(cm.get(WRITEERRORS)+getTestValue(10), WRITEERRORS, metricNameSpaces.get(entry.getKey()));
				tracer.traceDeltaGauge(cm.get(WRITEDROPS)+getTestValue(10), WRITEDROPS, metricNameSpaces.get(entry.getKey()));				
			} else {
				tracer.traceGauge(cm.get(QLENGTH), QLENGTH, metricNameSpaces.get(entry.getKey()));
				tracer.traceGauge(cm.get(IOPS), IOPS, metricNameSpaces.get(entry.getKey()));
				tracer.traceGauge(cm.get(BPS), BPS, metricNameSpaces.get(entry.getKey()));
				tracer.traceGauge(cm.get(SVCTIME), SVCTIME, metricNameSpaces.get(entry.getKey()));
				tracer.traceGauge(cm.get(IOSIZE), IOSIZE, metricNameSpaces.get(entry.getKey()));
				tracer.traceGauge(cm.get(BUSYTIME), BUSYTIME, metricNameSpaces.get(entry.getKey()));				
				tracer.traceDeltaGauge(cm.get(READERRORS), READERRORS, metricNameSpaces.get(entry.getKey()));
				tracer.traceDeltaGauge(cm.get(READDROPS), READDROPS, metricNameSpaces.get(entry.getKey()));
				tracer.traceDeltaGauge(cm.get(WRITEERRORS), WRITEERRORS, metricNameSpaces.get(entry.getKey()));
				tracer.traceDeltaGauge(cm.get(WRITEDROPS), WRITEDROPS, metricNameSpaces.get(entry.getKey()));
				
			}
			if(log.isInfoEnabled()) {
				StringBuilder b = new StringBuilder("\n======= [").append(entry.getKey()).append("] =======");
				b.append("\n\t").append(QLENGTH).append(":").append(cm.get(QLENGTH));
				b.append("\n\t").append(IOPS).append(":").append(cm.get(IOPS));
				b.append("\n\t").append(BPS).append(":").append(cm.get(BPS));
				b.append("\n\t").append(SVCTIME).append(":").append(cm.get(SVCTIME));
				b.append("\n\t").append(IOSIZE).append(":").append(cm.get(IOSIZE));
				b.append("\n\t").append(BUSYTIME).append(":").append(cm.get(BUSYTIME));
				b.append("\n\t").append(READERRORS).append(":").append(cm.get(READERRORS));
				b.append("\n\t").append(READDROPS).append(":").append(cm.get(READDROPS));
				b.append("\n\t").append(WRITEERRORS).append(":").append(cm.get(WRITEERRORS));
				b.append("\n\t").append(WRITEDROPS).append(":").append(cm.get(WRITEDROPS));
				
				log.info(b);
			}
		}
		
//		increment(nodeKey, QUEUE_LENGTH, rawValues[seq++]);
//		increment(nodeKey, BUSY_TIME, rawValues[seq++]);
//		increment(nodeKey, READ_COUNT, rawValues[seq++]);
//		increment(nodeKey, READ_BYTES, rawValues[seq++]);
//		increment(nodeKey, READ_ERRORS, rawValues[seq++]);
//		increment(nodeKey, READ_DROPS, rawValues[seq++]);
//		increment(nodeKey, READ_TICKS, rawValues[seq++]);
//
//		increment(nodeKey, WRITE_COUNT, rawValues[seq++]);
//		increment(nodeKey, WRITE_BYTES, rawValues[seq++]);
//		increment(nodeKey, WRITE_ERRORS, rawValues[seq++]);
//		increment(nodeKey, WRITE_DROPS, rawValues[seq++]);
//		increment(nodeKey, WRITE_TICKS, rawValues[seq++]);
		
	}
	
	
	/**
	 * Applies the raw values to the passed aggregate counter map
	 * @param nodeKey The compound key identifying the SAN node for which values are beng aggregated
	 * @param rawValues The raw values read from a statvlun
	 */
	protected void sumTotals(String nodeKey, long[] rawValues)  {
		int seq = 1;
		increment(nodeKey, QUEUE_LENGTH, rawValues[seq++]);
		increment(nodeKey, BUSY_TIME, rawValues[seq++]);
		increment(nodeKey, READ_COUNT, rawValues[seq++]);
		increment(nodeKey, READ_BYTES, rawValues[seq++]);
		increment(nodeKey, READ_ERRORS, rawValues[seq++]);
		increment(nodeKey, READ_DROPS, rawValues[seq++]);
		increment(nodeKey, READ_TICKS, rawValues[seq++]);

		increment(nodeKey, WRITE_COUNT, rawValues[seq++]);
		increment(nodeKey, WRITE_BYTES, rawValues[seq++]);
		increment(nodeKey, WRITE_ERRORS, rawValues[seq++]);
		increment(nodeKey, WRITE_DROPS, rawValues[seq++]);
		increment(nodeKey, WRITE_TICKS, rawValues[seq++]);
	}
	
	/**
	 * Returns a formatted string reporting the array total values
	 * @return the array total values
	 */
	public String printArrayTotals() {
		StringBuilder b = new StringBuilder("\n\t==============================\n\tArray Totals [").append(systemName).append("]\n\t==============================");
		b.append("\n\tQueue Length:").append(getArrayValue(QUEUE_LENGTH));
		b.append("\n\tBusy Time:").append(getArrayValue(BUSY_TIME));
		b.append("\n\tRead Count:").append(getArrayValue(READ_COUNT));
		b.append("\n\tRead Bytes:").append(getArrayValue(READ_BYTES));
		b.append("\n\tRead Errors:").append(getArrayValue(READ_ERRORS));
		b.append("\n\tRead Drops:").append(getArrayValue(READ_DROPS));
		b.append("\n\tRead Ticks:").append(getArrayValue(READ_TICKS));
		b.append("\n\tWrite Count:").append(getArrayValue(WRITE_COUNT));
		b.append("\n\tWrite Bytes:").append(getArrayValue(WRITE_BYTES));
		b.append("\n\tWrite Errors:").append(getArrayValue(WRITE_ERRORS));
		b.append("\n\tWrite Drops:").append(getArrayValue(WRITE_DROPS));
		b.append("\n\tWrite Ticks:").append(getArrayValue(WRITE_TICKS));
		
		return b.toString();
	}
	
	
	
	
	/**
	 * Waits for the next parse queue event completion
	 * @param timeout The timeout of the wait
	 * @param unit The unit of the timeout
	 * @return true if an event was received, false otherwise
	 */
	public boolean countdown(long timeout, TimeUnit unit) {
		try {
			return completionQueue.poll(timeout, unit)!=null;
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Returns the SAN serial number 
	 * @return the serialNumber
	 */
	public String getSerialNumber() {
		return serialNumber;
	}

	/**
	 * Returns the SAN system name
	 * @return the systemName
	 */
	public String getSystemName() {
		return systemName;
	}

	/**
	 * Returns the SAN cpu speed in Mhz
	 * @return the cpuMhz
	 */
	public int getCpuMhz() {
		return cpuMhz;
	}

	/**
	 * Returns the SAN ip name
	 * @return the ipName
	 */
	public String getIpName() {
		return ipName;
	}

	/**
	 * Returns the SAN OS version
	 * @return the osVersion
	 */
	public String getOsVersion() {
		return osVersion;
	}

	/**
	 * Returns the SAN model name
	 * @return the modelName
	 */
	public String getModelName() {
		return modelName;
	}

	/**
	 * Returns the SAN cache size in MB
	 * @return the cacheSize
	 */
	public int getCacheSize() {
		return cacheSize;
	}

	/**
	 * Returns the number of vlunstats parsed for this file
	 * @return the lunsParsed
	 */
	public int getLunsParsed() {
		return lunsParsed.get();
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return new StringBuilder("SANStatsParsingContext[").append(this.systemName).append("] vluns parsed:").append(lunsParsed.get()).toString();
	}
	
	
	
	/**
	 * Calculates the IOs per second
	 * @param rCount The read count
	 * @param wCount The write count
	 * @param now The timestamp
	 * @return the number of ios per second
	 */
	protected static long calcIosPerSec(long rCount, long wCount, long now) {
		if(now<1) return 0;
		long total = rCount + wCount * 1000000;
		double d = total / now;
		return (long)d;
	}
	
	/**
	 * Calculates the IOs per second
	 * @param now The vlun stat now timestamp
	 * @param counterMap The accumulated counter map for a node 
	 * @return the ios per second for the passed node
	 */
	protected static long calcIosPerSec(long now, NonBlockingHashMap<String, Counter> counterMap) {
		return calcIosPerSec(counterMap.get(READ_COUNT).get(), counterMap.get(WRITE_COUNT).get(), now); 
	}
	
	
	/**
	 * Calculates the bytes per second
	 * @param rBytes The read byte count
	 * @param wBytes The write byte count
	 * @param now The timestamp
	 * @return the number of bytes per second
	 */
	protected static long calcBytesPerSec(long rBytes, long wBytes, long now) {
		if(now<1) return 0;
		long total = rBytes+ wBytes * 1000;
		double d = total / now;
		return (long)d;
	}
	
	/**
	 * Calculates the bytes per second
	 * @param now The vlun stat now timestamp
	 * @param counterMap The accumulated counter map for a node 
	 * @return the bytes per second for the passed node
	 */
	protected static long calcBytesPerSec(long now, NonBlockingHashMap<String, Counter> counterMap) {
		return calcIosPerSec(counterMap.get(READ_BYTES).get(), counterMap.get(WRITE_BYTES).get(), now); 
	}
	
	
	/**
	 * Calculates the service time for the passed node stats
	 * @param rCount The read count
	 * @param wCount The write count
	 * @param rTicks The read ticks
	 * @param wTicks The write count
	 * @return the service time
	 */
	protected static long calcServiceTime(long rCount, long wCount, long rTicks, long wTicks) {		
		try {			
			double d = ((rTicks + wTicks) / (rCount + wCount)) * 1000;  
			return (long)d;
		} catch (Exception  e) {
			return 0;
		}
	}
	
	/**
	 * Calculates the service time
	 * @param counterMap The accumulated counter map for a node 
	 * @return the service time for the passed node
	 */
	protected static long calcServiceTime(NonBlockingHashMap<String, Counter> counterMap) {
		return calcServiceTime(counterMap.get(READ_COUNT).get(), counterMap.get(WRITE_COUNT).get(), counterMap.get(READ_TICKS).get(), counterMap.get(WRITE_TICKS).get()); 
	}
	
	
	/**
	 * Calculates the IO size for the last period
	 * @param rBytes The read bytes
	 * @param wBytes The written bytes
	 * @param rCount The read count
	 * @param wCount The write count
	 * @return The IO size
	 */
	protected static long calcIoSize(long rBytes, long wBytes, long rCount, long wCount) {		
		try {			
			double d = ((rBytes + wBytes) / (rCount + wCount)) * 1000;  
			return (long)d;
		} catch (Exception  e) {
			return 0;
		}
	}
	
	/**
	 * Calculates the IO size
	 * @param counterMap The accumulated counter map for a node 
	 * @return the IO size for the passed node
	 */
	protected static long calcIoSize(NonBlockingHashMap<String, Counter> counterMap) {
		return calcIoSize(counterMap.get(READ_BYTES).get(), counterMap.get(WRITE_BYTES).get(), counterMap.get(READ_COUNT).get(), counterMap.get(WRITE_COUNT).get()); 
	}
	
	
	/**
	 * Calculates the busy time in the last period
	 * @param busy The busy time
	 * @param now The timestamp
	 * @return The busy time
	 */
	protected static long calcBusyTime(long busy, long now) {		
		try {			
			double d = (busy * 100) / now;  
			return (long)d;
		} catch (Exception  e) {
			return 0;
		}
	}
	
	/**
	 * Calculates the busy time
	 * @param now The vlun stat now timestamp
	 * @param counterMap The accumulated counter map for a node 
	 * @return the busy time for the passed node
	 */
	protected static long calcBusyTime(long now, NonBlockingHashMap<String, Counter> counterMap) {
		return calcBusyTime(counterMap.get(BUSY_TIME).get(), now); 
	}
	
	
	

}
