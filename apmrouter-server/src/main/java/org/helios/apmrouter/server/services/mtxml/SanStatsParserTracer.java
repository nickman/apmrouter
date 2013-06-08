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
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

import org.apache.log4j.BasicConfigurator;
import org.helios.apmrouter.collections.ConcurrentLongSlidingWindow;
import org.helios.apmrouter.collections.LongSlidingWindow;
import org.helios.apmrouter.server.ServerComponentBean;
import org.helios.apmrouter.util.ByteSequenceIndexFinder;
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
	 * Static tester
	 * @param args None
	 */
	public static void main(String[] args) {
		BasicConfigurator.configure();
		SanStatsParserTracer sspt = new SanStatsParserTracer();
		RandomAccessFile raf = null;
		FileChannel fc = null;
		MappedByteBuffer mbb = null;
		try {
			sspt.info("Processing SAN Stats XML");
			raf = new RandomAccessFile(new File("./src/test/resources/san/statvlun-small.xml"), "r");
			//raf = new RandomAccessFile(new File("./src/test/resources/san/simple.xml"), "r");
			fc = raf.getChannel();
			mbb = fc.map(MapMode.READ_ONLY, 0, fc.size());
			sspt.info("Loaded XML into buffer. Size [", mbb.limit(), "] Loaded: ", mbb.isLoaded(), "  Direct:", mbb.isDirect());
			ChannelBuffer cbuf = ChannelBuffers.wrappedBuffer(mbb);
			sspt.process(cbuf);
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		} finally {
			if(raf!=null) try {raf.close();} catch (Exception e) {}
			if(fc!=null) try {fc.close();} catch (Exception e) {}			
		}
	}
	
	/**
	 * Processes a SAN stats xml file
	 * @param b A channel buffer containing the bytes of the SAN stats xml to process
	 */
	public void process(ChannelBuffer b) {
		info("Processing SAN Stats XML");
		Map<String, String> sysInfo = getSystemInfo(b);
	}
	
	/**
	 * Extracts the sysinfo from the channel buffer resident xml
	 * @param b The channel buffer to extract from
	 * @return a name/value map of the SAN sysinfo
	 */
	protected Map<String, String> getSystemInfo(ChannelBuffer b) {
		Map<String, String> sysInfo = new HashMap<String, String>();
		ChannelBuffer sysBuffer = slice(b, "<system_info>", "</system_info>");
		info("SysInfo Buffer [", sysBuffer, "]");
		if(sysBuffer!=null) {
			sysInfo.put("serial", getStringContentFromXML(sysBuffer, "serial_number"));
			sysInfo.put("sysname", getStringContentFromXML(sysBuffer, "sys_name"));
			sysInfo.put("cpumhz", getStringContentFromXML(sysBuffer, "cpu_mhz"));
			sysInfo.put("ipname", getStringContentFromXML(sysBuffer, "ip_name"));
			sysInfo.put("osrev", getStringContentFromXML(sysBuffer, "os_rev"));
			sysInfo.put("systemmodel", getStringContentFromXML(sysBuffer, "system_model"));
			sysInfo.put("chsizemb", getStringContentFromXML(sysBuffer, "ch_size_mb"));
		}
		info("SysInfo:\n\n", sysInfo.toString().replace(",", "\n"));
		int allIndex = new ByteSequenceIndexFinder("<all_statvlun>".getBytes()).findIn(sysBuffer);
		info("All Index:", allIndex);
		sysBuffer.readerIndex(allIndex);
		info("FastForwarded to ", sysBuffer.readerIndex());
		return sysInfo;
	}
	
	/** Opener string format */
	public static final String OPENER = "<%s>";
	/** Closer string format */
	public static final String CLOSER = "</%s>";
	
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
	 * Reads all the bytes from the passed channel buffer and returns them as a string.
	 * @param cb The channel buffer to read from
	 * @return the created string
	 */
	public static String toString(ChannelBuffer cb) {
		byte[] bytes = new byte[cb.readableBytes()];
		cb.getBytes(0, bytes);
		return new String(bytes);
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
		ByteSequenceIndexFinder startFinder = new ByteSequenceIndexFinder(start.getBytes());		
		ByteSequenceIndexFinder endFinder = new ByteSequenceIndexFinder(end.getBytes());
		final int maxBytes = b.readableBytes();
		final int startLength = start.getBytes().length;
		final int endLength = end.getBytes().length;
		int startOffset = startFinder.findIn(b);
		if(startOffset==-1) return null;
		info("Start Offset:", startOffset, "\n\tEnd Finder - Starting Index:", (startOffset+startLength), " Max Length:" + (maxBytes-startOffset-startLength));
		
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
		info("Index for [", start, "]: ", startOffset, " End offset:", endOffset, " Content Length: ", contentLength);
		
		ChannelBuffer sysInfoBuffer = b.slice(startOffset, contentLength);
		
		return sysInfoBuffer;
	}
	
	protected ChannelBuffer sliceBetween(ChannelBuffer b, String start, String end) {
		final int startLength = start.getBytes().length;
		final int endLength = end.getBytes().length;
		ChannelBuffer sub = slice(b, start, end);
		return sub.slice(startLength, sub.readableBytes()-startLength-endLength);
	}

}
