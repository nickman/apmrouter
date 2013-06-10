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
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
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
import org.helios.apmrouter.util.ByteSequenceIndexFinder;
import org.helios.apmrouter.util.SystemClock;
import org.helios.apmrouter.util.SystemClock.ElapsedTime;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

/**
 * <p>Title: SanStatsParserTracer</p>
 * <p>Description: A multi-threaded parser/tracer for network submitted 3par SAN/LUN performance stats.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.services.mtxml.SanStatsParserTracer</code></p>
 */

public class SanStatsParserTracer extends ServerComponentBean {
	/** The worker thread pool that XML segments are assigned to for parsing by a worker */
	protected ExecutorService threadPool;
	/** The segment parsing thread pool queue size */
	protected int parseQueueSize = 1000;
	/** The parse queue fairness */
	protected boolean parseQueueFairness = false;
	/** The number of parsing worker threads */
	protected int parseWorkers = 5;
	/** The parsing task queue */
	protected BlockingQueue<ChannelBuffer> parseQueue = null;
	
	protected Set<String> vluns = new CopyOnWriteArraySet<String>();
	
	/** Sliding windows of xml file processing elapsed times in ns. */
	protected final LongSlidingWindow fileProcessingTimesNs = new ConcurrentLongSlidingWindow(50);
	/** Sliding windows of xml segment processing elapsed times in ns. */
	protected final LongSlidingWindow segmentProcessingTimesMs = new ConcurrentLongSlidingWindow(100); 
	

	/**
	 * Creates a new SanStatsParserTracer
	 */
	public SanStatsParserTracer() {
		// TODO Auto-generated constructor stub
	}
	
	
	
	/**
	 * Creates a new SanStatsParserTracer
	 * @param threadPool The parse worker thread pool
	 */
	public SanStatsParserTracer(ExecutorService threadPool) {		
		this.threadPool = threadPool;
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
		SanStatsParserTracer sspt = new SanStatsParserTracer(executor);
		RandomAccessFile raf = null;
		FileChannel fc = null;
		MappedByteBuffer mbb = null;
		SystemClock.startTimer();
		int loops = 1;
		for(int i = 0; i < loops; i++) {
			try {
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
		ElapsedTime et = SystemClock.endTimer();
		sspt.info("Elapsed Time:", et,"\n\tAverage Per Doc:", et.avgMs(loops), " ms.");		
//		StringBuilder b=new StringBuilder("\n\t============================\n\tVlun Hosts\n\t============================");
//		for(String s: sspt.vluns) {
//			b.append("\n\t").append(s);
//		}
		sspt.info("Unique Ports:", sspt.vluns.size());
	}
	
	/**
	 * Processes a SAN stats xml file
	 * @param b A channel buffer containing the bytes of the SAN stats xml to process
	 */
	public void process(ChannelBuffer b) {
		//info("Processing SAN Stats XML");
		
		final SanStatsParsingContext ctx = new SanStatsParsingContext();
		
		ctx.setSysInfo(getSystemInfo(b));
		
		fastForwardToStartOf(b, "<all_statvlun>".getBytes());
		
		final AtomicInteger i = new AtomicInteger(0);
		int c = 0;
		while(true) {
			final ChannelBuffer vlunBuffer = nextVlun(b);
			if(vlunBuffer==null) break;			
			threadPool.execute(new Runnable() {
				public void run() {
					Map<String, String> vlinnfo = getVlunInfo(vlunBuffer);
					ctx.addVLun(vlinnfo);
					i.incrementAndGet();
				}
			});
			c++;
			
			//System.out.println("VLUN XML:\n[" + toString(vlunBuffer) + "]");
			//info("VLUN:\n", getVlunInfo(vlunBuffer).toString().replace(",", "\n"));
			//info("VLUN XML:\n[", toString(vlunBuffer), "]");
			//b.readerIndex(index[0]+=vlunBuffer.readableBytes());
		}
		//info("Found [", c, "] vlun stat instances");
		while(c!=0) {
			ctx.countdown();
			c--;
		}
//		info("\n", ctx.printArrayTotals());
//		info("\n", ctx.printHostAggregates());
//		info("\n", ctx.printVVAggregates());
		info("\n\n\n");
		info("Total Hosts:", ctx.getTotalHosts());
		info("Total VVs:", ctx.getTotalVVHosts());
		info("Total Port Nodes:", ctx.getTotalPortNodes());
		
		
		
		//info("DONE");
	}
	/** Opener format for statvlun instances */
	private static final byte[] STATVLUN_OPENER = "<statvlun>".getBytes();
	/** Closer format for statvlun instances */
	private static final byte[] STATVLUN_CLOSER = "</statvlun>".getBytes();
	/** Opener string format */
	public static final String OPENER = "<%s>";
	/** Closer string format */
	public static final String CLOSER = "</%s>";
	
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
		ChannelBuffer sysBuffer = slice(b, "<system_info>", "</system_info>");
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
		String uniqueId = String.format("%s/%s/%s/%s/%s", vlunInfo.get("vv_name"), vlunInfo.get("host_name"), vlunInfo.get("port_node"), vlunInfo.get("port_slot"), vlunInfo.get("port_port")); 
		if(vluns.add(uniqueId)) {
			//info("------------> [", uniqueId, "]");
		}
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

}
