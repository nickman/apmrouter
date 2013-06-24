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

import java.io.File;
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.helios.apmrouter.collections.ConcurrentLongSlidingWindow;
import org.helios.apmrouter.collections.LongSlidingWindow;
import org.helios.apmrouter.server.ServerComponentBean;
import org.helios.apmrouter.server.tracing.ServerTracerFactory;
import org.helios.apmrouter.util.ByteSequenceIndexFinder;
import org.helios.apmrouter.util.SystemClock;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.UpstreamMessageEvent;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.support.MetricType;

/**
 * <p>Title: SanStatsParserTracer</p>
 * <p>Description: A multi-threaded parser/tracer for network submitted 3par SAN/LUN performance stats.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.services.mtxml.SanStatsParserTracer</code></p>
 */

public class SanStatsParserTracer extends ServerComponentBean implements ChannelUpstreamHandler {
	/** The worker thread pool that XML segments are assigned to for parsing by a worker */
	protected ExecutorService threadPool;
	/** The segment parsing thread pool queue size */
	protected int parseQueueSize = 1000;
	/** The parse queue fairness */
	protected boolean parseQueueFairness = false;
	/** The number of parsing worker threads */
	protected int parseWorkers = 5;
	/** The timeout waiting for queue processing completion */
	protected long parseQueueTimeout = 5000;
	/** The parse queue timeout completion unit */
	protected TimeUnit parseQueueTimeoutUnit = TimeUnit.MILLISECONDS;	
	/** The parsing task queue */
	protected BlockingQueue<ChannelBuffer> parseQueue = null;
	
	
	/** The granularity formatter */
	protected final String gformat;
	
	
	/** The XML header indicating the start of the statvluns */
	private static final byte[] ALL_STAT_OPENER = "<all_statvlun>".getBytes();
	/** Opener format for statvlun instances */
	private static final byte[] STATVLUN_OPENER = "<statvlun>".getBytes();
	/** Closer format for statvlun instances */
	private static final byte[] STATVLUN_CLOSER = "</statvlun>".getBytes();
	/** Opener format for system info instances */
	private static final byte[] SYSINFO_OPENER = "<system_info>".getBytes();
	/** Closer format for system info instances */
	private static final byte[] SYSINFO_CLOSER = "</system_info>".getBytes();
	
	/** Opener for flag in the xml file indicating that test data should be generated */
	private static final byte[] TEST_OPENER = "</test-data>".getBytes();
	
	/** Opener string format */
	public static final String OPENER = "<%s>";
	/** Closer string format */
	public static final String CLOSER = "</%s>";

	/**
	 * Creates a new SanStatsParserTracer
	 * @param gformat The granularity formatter 
	 */
	public SanStatsParserTracer(String gformat) {
		this.gformat = gformat;
	}
	
	/** The name of this decoder in the pipeline */
	public static final String PIPE_NAME = "SanStatsParserTracer";

	
	/**
	 * Creates a new SanStatsParserTracer
	 * @param gformat The granularity formatter
	 * @param threadPool The parse worker thread pool
	 */
	public SanStatsParserTracer(String gformat, ExecutorService threadPool) {
		this(gformat);
		this.threadPool = threadPool;
	}

	/**
	 * <p>If the passed event is a {@link UpstreamMessageEvent} and the message is an instance of {@link ChannelBuffer},
	 * we believe this is a decoded san stats XML submission, so we process it.</p> 
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelUpstreamHandler#handleUpstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) {
		if(e instanceof UpstreamMessageEvent) {
			Object msg = ((UpstreamMessageEvent)e).getMessage();
			if(msg instanceof ChannelBuffer) {
				process((ChannelBuffer)msg);
			}
		}
		ctx.sendUpstream(e);
	}

	/**
	 * Static tester
	 * @param args None
	 */
	public static void main(String[] args) {
		final int coreThreads = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors()*2;
		final int maxThreads = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors()*3;
		//BasicConfigurator.configure();
		ThreadPoolExecutor executor = new ThreadPoolExecutor(
			coreThreads,
			maxThreads,
			15000,
			TimeUnit.MILLISECONDS,
			new ArrayBlockingQueue<Runnable>(15000),
			new ThreadFactory(){
				final AtomicLong serial = new AtomicLong(0);
				@Override
				public Thread newThread(Runnable r) {
					Thread t = new Thread(r, "SanStatsParserThread#" + serial.incrementAndGet());
					t.setDaemon(true);
					return t;
				}
			},
			new RejectedExecutionHandler(){
				final AtomicLong rejections = new AtomicLong(0);
				@Override
				public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
					long rejection = rejections.incrementAndGet();
					System.err.println("REJECTED EXECUTIONS:" + rejection);
				}
			}
		); 
		executor.prestartAllCoreThreads();
		//String[] segments = new String[]{"resource=3par", "sysname=%s","vvname=%s", "hostname=%s", "portnode=%s", "portslot=%s", "portport=%s"};
		SanStatsParserTracer sspt = new SanStatsParserTracer("resource=3par/sysname=%s/vvname=%s/hostname=%s/portnode=%s", executor);
		//SanStatsParserTracer sspt = new SanStatsParserTracer("resource=3par/sysname=%s/vvname=%s/hostname=%s", executor);
		RandomAccessFile raf = null;
		FileChannel fc = null;
		MappedByteBuffer mbb = null;
		SystemClock.startTimer();
		int loops = 1;
		StringBuilder segmentBuilder = new StringBuilder();
		//for(int i = 0; i < segments.length; i++) {
		for(int i = 0; i < loops; i++) {
			try {
				//segmentBuilder.append("/").append(segments[i]);
				//SanStatsParserTracer sspt = new SanStatsParserTracer(segmentBuilder.toString(), executor);
				//sspt.info("Processing SAN Stats XML");
				//raf = new RandomAccessFile(new File("./src/test/resources/san/statvlun-small.xml"), "r");			
				raf = new RandomAccessFile(new File("./src/test/resources/san/statvlun.xml"), "r");
				//raf = new RandomAccessFile(new File("./src/test/resources/san/simple.xml"), "r");
				fc = raf.getChannel();
				mbb = fc.map(MapMode.READ_ONLY, 0, fc.size());
				//sspt.info("Loaded XML into buffer. Size [", mbb.limit(), "] Loaded: ", mbb.isLoaded(), "  Direct:", mbb.isDirect());
				ChannelBuffer cbuf = ChannelBuffers.wrappedBuffer(mbb);
				
				sspt.process(cbuf);		
			} catch (Exception ex) {		
				ex.printStackTrace(System.err);
			} finally {
				if(raf!=null) try {raf.close();} catch (Exception e) {}
				if(fc!=null) try {fc.close();} catch (Exception e) {}			
			}			
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponent#resetMetrics()
	 */
	@Override
	@ManagedOperation(description="Resets the San Stats Processor Metrics")
	public void resetMetrics() {
		rejectedParseQueueExecutions.set(0L);
		processedFiles.set(0L);
		fileProcessingTimesNs.clear();
		segmentProcessingTimesNs.clear();
		processedFiles.set(0L);
		super.resetMetrics();
	}
	
	
	/** The cummulative count of rejected parse queue task submissions  */
	protected final AtomicLong rejectedParseQueueExecutions = new AtomicLong(0L);
	/** The cummulative count of processes san stats files */
	protected final AtomicLong processedFiles = new AtomicLong(0L);
	
	/** Sliding windows of xml file processing elapsed times in ns. */
	protected final LongSlidingWindow fileProcessingTimesNs = new ConcurrentLongSlidingWindow(50);
	/** Sliding windows of xml segment processing elapsed times in ns. */
	protected final LongSlidingWindow segmentProcessingTimesNs = new ConcurrentLongSlidingWindow(100);
	
	/**
	 * Returns the last elapsed time to parse and process a statvlun file in ms
	 * @return the last elapsed time to parse and process a statvlun file 
	 */
	@ManagedMetric(category="SanStatsParser", displayName="LastFileProcessingTimesMs", metricType=MetricType.GAUGE, description="The last elapsed time to parse and process a statvlun file in ms")	
	public long getLastFileProcessingTimesMs() {
		return TimeUnit.MILLISECONDS.convert(fileProcessingTimesNs.getFirst(), TimeUnit.NANOSECONDS);
	}
	
	/**
	 * Returns the cummulative number of processed san stats files 
	 * @return the cummulative number of processed san stats files 
	 */
	@ManagedMetric(category="SanStatsParser", displayName="LastFileProcessingTimesMs", metricType=MetricType.COUNTER, description="The cummulative number of processed san stats files")	
	public long getProcessedFiles() {
		return processedFiles.get();
	}
	
	
	/**
	 * Returns the rolling average of the last 50 elapsed times to parse and process a statvlun file in ms
	 * @return the rolling average of the last 50 elapsed times to parse and process a statvlun file
	 */
	@ManagedMetric(category="SanStatsParser", displayName="AverageFileProcessingTimesMs", metricType=MetricType.GAUGE, description="The rolling average of the last 50 elapsed times to parse and process a statvlun file in ms")	
	public long getAverageFileProcessingTimesMs() {
		return TimeUnit.MILLISECONDS.convert(fileProcessingTimesNs.avg(), TimeUnit.NANOSECONDS);
	}
	
	/**
	 * Returns the total number of files processed
	 * @return the total number of files processed
	 */
	@ManagedMetric(category="SanStatsParser", displayName="FilesProcessed", metricType=MetricType.COUNTER, description="Thetotal number of files processed")
	public long getFilesProcessed() {
		return processedFiles.get();
	}
	
	/**
	 * Returns the last elapsed time to parse and process a statvlun segment in ns
	 * @return the last elapsed time to parse and process a statvlun segment 
	 */
	@ManagedMetric(category="SanStatsParser", displayName="LastSegmentProcessingTimesNs", metricType=MetricType.GAUGE, description="The last elapsed time to parse and process a statvlun segment in ns")	
	public long getLastSegmentProcessingTimesNs() {
		return segmentProcessingTimesNs.getFirst();
	}
	/**
	 * Returns the rolling average of the last 100 elapsed times to parse and process a statvlun segment in ns,
	 * @return the rolling average of the last 100 elapsed times to parse and process a statvlun segment
	 */
	@ManagedMetric(category="SanStatsParser", displayName="AverageSegmentProcessingTimesNs", metricType=MetricType.GAUGE, description="The rolling average of the last 100 elapsed times to parse and process a statvlun segment in ns")
	public long getAverageSegmentProcessingTimesNs() {
		return segmentProcessingTimesNs.avg();
	}
	
	/**
	 * Returns the cummulative count of rejected parse queue task submissions
	 * @return the cummulative count of rejected parse queue task submissions
	 */
	@ManagedMetric(category="SanStatsParser", displayName="RejectedParseQueueExecutions", metricType=MetricType.COUNTER, description="The cummulative count of rejected parse queue task submissions")
	public long getRejectedParseQueueExecutions() {
		return rejectedParseQueueExecutions.get();
	}
	
	private static ThreadLocal<Boolean> runTestData = new ThreadLocal<Boolean>() {
		@Override
		protected Boolean initialValue() {		
			return false;
		}
	};
	
	/**
	 * Processes a SAN stats xml file
	 * @param fileName The name of a SAN stats xml file to process
	 */
	@ManagedOperation(description="Processes a SAN stats xml file")
	@ManagedOperationParameter(name="FileName", description="The name of a SAN stats xml file to process")
	public void process(String fileName) {
		if(fileName==null || fileName.trim().isEmpty()) throw new IllegalArgumentException("The passed file name was null or empty", new Throwable());
		if("test".equalsIgnoreCase(fileName.trim())) {
			fileName = "./src/test/resources/san/statvlun.xml";
			runTestData.set(true);
			info("##########\tRunning process in test data mode");
		}
		File xmlFile = new File(fileName.trim());
		if(!xmlFile.canRead()) throw new IllegalArgumentException("The passed file name [" + xmlFile + "] could not be read", new Throwable());
		RandomAccessFile raf = null;
		FileChannel fc = null;
		MappedByteBuffer mbb = null;
		try {
			raf = new RandomAccessFile(xmlFile, "r");
			fc = raf.getChannel();
			mbb = fc.map(MapMode.READ_ONLY, 0, fc.size());
			ChannelBuffer cbuf = ChannelBuffers.wrappedBuffer(mbb);
			process(cbuf);		
		} catch (Exception ex) {		
			ex.printStackTrace(System.err);
			log.error("Failed to process file [" + xmlFile + "]", ex);
			throw new RuntimeException("Failed to process file [" + xmlFile + "]", ex);			
		} finally {
			if(raf!=null) try {raf.close();} catch (Exception e) {/* No Op */}
			if(fc!=null) try {fc.close();} catch (Exception e) {/* No Op */}			
		}					
	}
	
	private static final AtomicInteger concurrency = new AtomicInteger(0);
	static {
		Thread t = new Thread("nextLun Concurrency Thread") {
			public void run() {
				while(true) {
					System.err.println("nextLun Concurrency [" + concurrency + "]");
					try { Thread.currentThread().join(3000); } catch (Exception ex) {}					
				}
			}
		};
		t.setDaemon(true);
		//t.start();		
	}

	
	
	/**
	 * Processes a SAN stats xml file
	 * @param b A channel buffer containing the bytes of the SAN stats xml to process
	 */
	public void process(ChannelBuffer b) {
		ByteSequenceIndexFinder testDataFinder = new ByteSequenceIndexFinder(TEST_OPENER);
		if(testDataFinder.findIn(b)>=0) {
			runTestData.set(true);
		}
		final int bufferSize = b.readableBytes();
		
		final SanStatsParsingContext ctx = new SanStatsParsingContext(gformat, runTestData.get());
		long fileStart = System.nanoTime();
		try {
			ctx.setSysInfo(getSystemInfo(b));
		} catch (Throwable ex) {
			error("Failed to parse SysInfo ", ex);
			throw new RuntimeException(ex);
		}
		try {
			fastForwardToStartOf(b, ALL_STAT_OPENER);
		} catch (Throwable ex) {
			error("Failed to fast forward to [", ALL_STAT_OPENER, "] ", ex);
			throw new RuntimeException(ex);
		}
		final AtomicInteger parseQueueTasks = new AtomicInteger(0);
		final long _parseQueueTimeout = parseQueueTimeout;
		final TimeUnit _parseQueueTimeoutUnit = parseQueueTimeoutUnit;
		int c = 0;
		final CountDownLatch latch = new CountDownLatch(Short.MAX_VALUE);
		
			StringBuilder startMessage = new StringBuilder("\n\t=====================\n\tStarting SanStats Processing\n\t=====================");
			startMessage.append("\n\tBuffer Size:").append(bufferSize);
			startMessage.append("\n\tTimeout:").append(_parseQueueTimeout);
			startMessage.append("\n\tTimeout Unit:").append(_parseQueueTimeoutUnit);
			startMessage.append("\n\tTest Data:").append(runTestData.get());
			startMessage.append("\n\tData Header:  [");
			byte[] headerBytes = new byte[100];
			b.getBytes(0, headerBytes);
			String header = new String(headerBytes);
			startMessage.append("\n").append(header);
			startMessage.append("\n\t]");		
			startMessage.append("\n\t=====================");
			info(startMessage);
		while(true) {
			final ChannelBuffer vlunBuffer;
			try {
				concurrency.incrementAndGet();
				vlunBuffer = nextVlun(b);
			} finally {
				concurrency.decrementAndGet();
			}
			if(vlunBuffer==null) {
				info("Submitted a total of [", c, "] parsing tasks");
				break;
			}
			try {
				threadPool.execute(new Runnable() {
					@Override
					public void run() {
						try {
							long segmentStart = System.nanoTime();						
							Map<String, String> vlinnfo = getVlunInfo(vlunBuffer);
							ctx.addVLun(vlinnfo);
							parseQueueTasks.incrementAndGet();
							segmentProcessingTimesNs.insert(System.nanoTime()-segmentStart);
						} finally {
							latch.countDown();
						}
					}
				});
				c++;
				if(c%100==0) {
					info("Submitted [", c, "] parsing tasks so far");
				}
			} catch (Exception ex) {
				warn("ParseQueue Task Rejection [", ex.toString(), "]");
				rejectedParseQueueExecutions.incrementAndGet();
			}
		}
		info("Parsing loop complete");
		int diff = Short.MAX_VALUE - c;
		for(int x = 0; x < diff; x++) {
			latch.countDown();
		}
		try {
			if(!latch.await(_parseQueueTimeout, _parseQueueTimeoutUnit)) {
				error("Timeout waiting [", _parseQueueTimeout, " ", _parseQueueTimeoutUnit.name(), "] for parse queue completion", new Throwable());
			}
		} catch (InterruptedException e) {
			e.printStackTrace(System.err);
			e.printStackTrace();
		} finally {
			int remaining = ctx.completionQueue.size();
			info("Clearing completion queue with [", remaining, "] completions");
			ctx.completionQueue.clear();
		}
//		while(c!=0) {
//			if(!ctx.countdown(_parseQueueTimeout, _parseQueueTimeoutUnit)) {
//				error("Timeout waiting [", _parseQueueTimeout, " ", _parseQueueTimeoutUnit.name(), "] for parse queue completion", new Throwable());
//				return;				
//			}
//			c--;
//		}
		processedFiles.incrementAndGet();
		ctx.traceStats(ServerTracerFactory.getInstance().getTracer());
		long elapsed = System.nanoTime()-fileStart;
		fileProcessingTimesNs.insert(elapsed);

		info("Processed SanStats Buffer [", bufferSize, "] in [", TimeUnit.MILLISECONDS.convert(elapsed, TimeUnit.NANOSECONDS), "] ms.");
	}
	
	
	/**
	 * Returns the next vlun xml fragment in a sub-buffer
	 * @param b The buffer to read from
	 * @return the read channel buffer or null if one was not found
	 */
	protected ChannelBuffer nextVlun(ChannelBuffer b) {
		return slice(b, STATVLUN_OPENER, STATVLUN_CLOSER);
	}
	
	/**
	 * Extracts the sysinfo from the channel buffer resident xml
	 * @param b The channel buffer to extract from	 
	 * @return a name/value map of the SAN sysinfo
	 */
	protected Map<String, String> getSystemInfo(ChannelBuffer b) {
		Map<String, String> sysInfo = new HashMap<String, String>();
		ChannelBuffer sysBuffer = slice(b, SYSINFO_OPENER, SYSINFO_CLOSER);
		//info("SysInfo Buffer [", sysBuffer, "]");
		if(sysBuffer!=null) {
			getStringContentFromXML(sysBuffer, "serial_number", sysInfo);
			getStringContentFromXML(sysBuffer, "sys_name", sysInfo);
			getStringContentFromXML(sysBuffer, "cpu_mhz", sysInfo);
			getStringContentFromXML(sysBuffer, "ip_name", sysInfo);
			getStringContentFromXML(sysBuffer, "os_rev", sysInfo);
			getStringContentFromXML(sysBuffer, "system_model", sysInfo);
			getStringContentFromXML(sysBuffer, "ch_size_mb", sysInfo);
		}
		//b.readerIndex(b.readerIndex() + sysBuffer.readerIndex());
		//info("SysInfo:\n\n", sysInfo.toString().replace(",", "\n"));
		return sysInfo;
	}
 
	
	/**
	 * Parses a vlun xml fragment in the passed channel buffer
	 * @param b The channel buffer containing the vlun xml
	 * @return a map of the values in the vlun fragment
	 */
	protected Map<String, String> getVlunInfo(ChannelBuffer b) {
		Map<String, String> vlunInfo = new HashMap<String, String>();
		getStringContentFromXML(b, "vv_name", vlunInfo);
		getStringContentFromXML(b, "host_name", vlunInfo);
		getStringContentFromXML(b, "port_node", vlunInfo);
		getStringContentFromXML(b, "port_slot", vlunInfo);
		getStringContentFromXML(b, "port_port", vlunInfo);
		getStringContentFromXML(b, "now", vlunInfo);
		getStringContentFromXML(b, "qlen", vlunInfo);
		getStringContentFromXML(b, "busy", vlunInfo);
		getStringContentFromXML(b, "rcount", vlunInfo);
		getStringContentFromXML(b, "rbytes", vlunInfo);
		getStringContentFromXML(b, "rerror", vlunInfo);
		getStringContentFromXML(b, "rdrops", vlunInfo);
		getStringContentFromXML(b, "rticks", vlunInfo);
		getStringContentFromXML(b, "wcount", vlunInfo);
		getStringContentFromXML(b, "wbytes", vlunInfo);
		getStringContentFromXML(b, "werror", vlunInfo);
		getStringContentFromXML(b, "wdrops", vlunInfo);
		getStringContentFromXML(b, "wticks", vlunInfo);
		return vlunInfo;
	}
	
	
	/**
	 * Fastforwards the buffer reader index to the beginning of the first <b>statvlun</b>
	 * @param b The buffer to fast forward
	 * @param target The byte sequence to fast forward to
	 * @return true if the fast forward succeeded, false otherwise
	 */
	protected boolean fastForwardToStartOf(ChannelBuffer b, byte[] target) {				
		int allIndex = new ByteSequenceIndexFinder(target).findIn(b);
		if(allIndex==-1) return false;
		b.readerIndex(allIndex);		
		return true;
	}
	
	/**
	 * Fastforwards the buffer reader index to the end of the first <b>statvlun</b>
	 * @param b The buffer to fast forward
	 * @param target The byte sequence to fast forward to the end of
	 * @return true if the fast forward succeeded, false otherwise
	 */
	protected boolean fastForwardToEndOf(ChannelBuffer b, byte[] target) {
		boolean start = fastForwardToStartOf(b, target);
		if(!start) return false;
		b.readerIndex(b.readerIndex() + target.length);
		return true;
	}
	
	
	
	/**
	 * Extracts the content between and XML node named <b><code>nodeName</code></b> and returns it as a string
	 * @param buffer The buffer to read from
	 * @param nodeName The node name to read
	 * @return the read string
	 */
	public String getStringContentFromXML(ChannelBuffer buffer, String nodeName) {
		return toString(sliceBetween(buffer, String.format(OPENER, nodeName), String.format(CLOSER, nodeName)));
	}
	
	/**
	 * Extracts the content between and XML node named <b><code>nodeName</code></b> and saves it into the passed map
	 * @param buffer The buffer to read from
	 * @param nodeName The node name to read
	 * @param map the map to save into
	 * @return the read string
	 */
	public String getStringContentFromXML(ChannelBuffer buffer, String nodeName, Map<String, String> map) {
		String s = toString(sliceBetween(buffer, String.format(OPENER, nodeName), String.format(CLOSER, nodeName)));
		if(map!=null) map.put(nodeName, s);
		return s;
	}
	
	
	/**
	 * Reads all the bytes from the passed channel buffer and returns them as a string.
	 * @param cb The channel buffer to read from
	 * @return the created string
	 */
	public static String toString(ChannelBuffer cb) {
		byte[] bytes = new byte[cb.readableBytes()];
		cb.getBytes(cb.readerIndex(), bytes);
		return new String(bytes);
	}
	
	/**
	 * Reads all the bytes from the passed channel buffer and returns them as an int parsed from the string value.
	 * @param cb The channel buffer to read from
	 * @return the created int
	 */
	public static int toInt(ChannelBuffer cb) {
		byte[] bytes = new byte[cb.readableBytes()];
		cb.getBytes(0, bytes);
		return Integer.parseInt(new String(bytes));		
	}
	
	/**
	 * Reads all the bytes from the passed channel buffer and returns them as a long parsed from the string value.
	 * @param cb The channel buffer to read from
	 * @return the created long
	 */
	public static long toLong(ChannelBuffer cb) {
		byte[] bytes = new byte[cb.readableBytes()];
		cb.getBytes(0, bytes);
		return Long.parseLong(new String(bytes));		
	}
	
	
	/**
	 * Searches the passed buffer for the starting offsets of the <b><code>start</code></b> and <b><code>end</code></b>
	 * strings in the channel buffer. If found, returns s subslice of the channel containing 
	 * the content between and including the <b><code>start</code></b> and <b><code>end</code></b> delimeters.  
	 * @param b The buffer to search 
	 * @param start The starting delimeter
	 * @param end The ending delimeter
	 * @return The slice channel buffer or null if one or both of the delimeters were not found
	 */
	protected ChannelBuffer slice(ChannelBuffer b, String start, String end) {
		return slice(b, start.getBytes(), end.getBytes());
	}
	
	
	/**
	 * Searches the passed buffer for the starting offsets of the <b><code>start</code></b> and <b><code>end</code></b>
	 * strings in the channel buffer. If found, returns s subslice of the channel containing 
	 * the content between and including the <b><code>start</code></b> and <b><code>end</code></b> delimeters.  
	 * @param b The buffer to search 
	 * @param start The starting delimeter
	 * @param end The ending delimeter
	 * @return The slice channel buffer or null if one or both of the delimeters were not found
	 */
	protected ChannelBuffer slice(ChannelBuffer b, byte[] start, byte[] end) {
		ByteSequenceIndexFinder startFinder = new ByteSequenceIndexFinder(start);		
		ByteSequenceIndexFinder endFinder = new ByteSequenceIndexFinder(end);
		final int maxBytes = b.readableBytes();
		final int startLength = start.length;
		final int endLength = end.length;
		int startOffset = startFinder.findIn(b);
		if(startOffset==-1) return null;
		startOffset += b.readerIndex(); 
		//info("Start Offset:", startOffset, "\n\tEnd Finder - Starting Index:", (startOffset+startLength), " Max Length:" + (maxBytes-startOffset-startLength));
		
		//int endOffset = endFinder.findIn(b, maxBytes-startOffset-startLength);  // FIXME: We should limit the lengths a bit more
		int endOffset = endFinder.findIn(b, startOffset+startLength);  // FIXME: We should limit the lengths a bit more
		//int endOffset = endFinder.findIn(b);  // FIXME: We should limit the lengths a bit more

		if(endOffset==-1) {
			warn("No endOffset");
			return null;
		}
		// Index for [<system_info>]: 50 End offset:320 Content Length: 284
		int contentLength = (endOffset+endLength)-startOffset;	
		//int contentLength = endOffset+endLength;
		//info("Index for [", start, "]: ", startOffset, " End offset:", endOffset, " Content Length: ", contentLength);
		
		ChannelBuffer sysInfoBuffer = b.slice(startOffset, contentLength);
		b.readerIndex(startOffset + contentLength);
		return sysInfoBuffer;
	}
	
	/**
	 * Returns a sub-buffer of the passed buffer with the content between the first two start and end delimeters
	 * @param b The buffer to extract from
	 * @param start The starting delimeter
	 * @param end The ending delimeter
	 * @return the extracted buffer or null if the delimeters were not both found
	 */
	protected ChannelBuffer sliceBetween(ChannelBuffer b, String start, String end) {
		final int startLength = start.getBytes().length;
		final int endLength = end.getBytes().length;
		ChannelBuffer sub = slice(b, start, end);
		
		if(sub==null) {
			error("Failed to sub [" + start + "]");
			return null;		
		}
		//b.readerIndex(b.readerIndex() + sub.readableBytes());
		return sub.slice(startLength, sub.readableBytes()-startLength-endLength);
	}



	/**
	 * Returns the parse queue completion timeout
	 * @return the parse queue completion timeout
	 */
	@ManagedAttribute(description="The parse queue completion timeout")
	public long getParseQueueTimeout() {
		return parseQueueTimeout;
	}



	/**
	 * Sets the parse queue completion timeout
	 * @param parseQueueTimeout the parse queue completion timeout
	 */
	@ManagedAttribute(description="The parse queue completion timeout")
	public void setParseQueueTimeout(long parseQueueTimeout) {
		this.parseQueueTimeout = parseQueueTimeout;
	}



	/**
	 * Returns the parse queue completion timeout unit
	 * @return the parseQueueTimeoutUnit
	 */
	@ManagedAttribute(description="The parse queue completion timeout unit")
	public String getParseQueueTimeoutUnit() {
		return parseQueueTimeoutUnit.name();
	}



	/**
	 * Sets the parse queue completion timeout unit 
	 * @param parseQueueTimeoutUnit the parse queue completion timeout unit
	 */
	@ManagedAttribute(description="The parse queue completion timeout unit")
	public void setParseQueueTimeoutUnit(String parseQueueTimeoutUnit) {
		this.parseQueueTimeoutUnit = TimeUnit.valueOf(parseQueueTimeoutUnit.trim().toUpperCase());
	}



	/**
	 * Returns 
	 * @return the parseWorkers
	 */
	public int getParseWorkers() {
		return parseWorkers;
	}



	/**
	 * Sets 
	 * @param parseWorkers the parseWorkers to set
	 */
	public void setParseWorkers(int parseWorkers) {
		this.parseWorkers = parseWorkers;
	}



	/**
	 * Returns 
	 * @return the gformat
	 */
	public String getGformat() {
		return gformat;
	}



	/**
	 * Sets 
	 * @param threadPool the threadPool to set
	 */
	public void setThreadPool(ExecutorService threadPool) {
		this.threadPool = threadPool;
	}

}
