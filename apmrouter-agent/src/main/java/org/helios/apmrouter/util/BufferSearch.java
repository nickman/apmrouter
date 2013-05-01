/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2013, Helios Development Group and individual contributors
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
package org.helios.apmrouter.util;

import java.util.Arrays;
import java.util.Random;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

/**
 * <p>Title: BufferSearch</p>
 * <p>Description: Utility functions to search a notional buffer of bytes</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.util.BufferSearch</code></p>
 */

public class BufferSearch {
	/** The pattern of bytes to search for */
	protected final byte[] pattern;
	/** Counter target for match */
	protected final int matchCount;
	/** A thread local for the counter so we can reuse this search across multiple threads */
	protected final ThreadLocal<int[]> counter = new ThreadLocal<int[]>(){
		@Override
		protected int[] initialValue() {
			return new int[]{0};
		}
	};
	protected static final Random random = new Random(System.currentTimeMillis());
	
	
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	public static void main(String[] args) {
		log("BufferSearch Test");
		int bufferSize = 10000;
		int segmentSize = 10;
		int segmentStart = Math.abs(random.nextInt(bufferSize-segmentSize));
		final byte[] segment = new byte[segmentSize];
		
		
		byte[] buff = new byte[bufferSize];
		random.nextBytes(buff);
		System.arraycopy(buff, segmentStart, segment, 0, segmentSize);
		log("Segment:" + Arrays.toString(segment));
		log("Start:" + segmentStart);
		ChannelBuffer cb = ChannelBuffers.wrappedBuffer(buff);
		ByteSequenceIndexFinder finder = new ByteSequenceIndexFinder(segment);
		BufferSearch bsearch = new BufferSearch(segment); 
		log("Warmup Starting");
		for(int i = 0; i < 5000; i++) {
			bsearch.search(cb);
			cb.bytesBefore(finder);
		}
		log("Warmup Done");
		SystemClock.startTimer();
		for(int i = 0; i < 5000; i++) {
			bsearch.search(cb);
		}
		log("BufferSearch:" + SystemClock.endTimer());
		SystemClock.startTimer();
		for(int i = 0; i < 5000; i++) {
			cb.bytesBefore(finder);
		}
		log("Finder:" + SystemClock.endTimer());
		
		
//		int pos1 = new BufferSearch(segment).search(cb);
//		int pos2 = cb.bytesBefore(finder)-segmentSize+1;
//		log("Pos1:" + pos1);
//		byte[] extracted = new byte[segmentSize];
//		cb.getBytes(pos2, extracted);
//		log("Pos2:" + pos2 + "\n\t" + Arrays.toString(extracted));
		
	}
	
	/**
	 * Creates a new BufferSearch
	 * @param pattern The pattern of bytes to search for
	 */
	public BufferSearch(byte[] pattern) {
		if(pattern==null || pattern.length==0) throw new IllegalArgumentException("The passed byte pattern was null or zero-length", new Throwable());
		this.pattern = pattern;
		this.matchCount = this.pattern.length;
	}
	
	/**
	 * Searches the channel buffer and return the position of the match, or -1 if no match was found
	 * @param cb The buffer to search
	 * @return the position of the match, or -1 if no match was found
	 */
	public int search(ChannelBuffer cb) {
		int pos = 0;
		try {
			
			if(!cb.readable()) return -1;
			int readable = cb.readableBytes();
			while(pos<readable && !nextByte(cb.getByte(pos))) {
				if(ctr()==matchCount) break;
				pos++;
			}
			return matchCount==ctr() ? pos-matchCount+1 : -1;
		} catch (Exception ex) {
			throw new RuntimeException("Search failed. pos:" + pos + " ctr:" + ctr(), ex);
		} finally {
			counter.remove();
		}
	}
	
	private boolean nextByte(byte b) {		
		if(b==pattern[ctr()]) {
			return incr()==matchCount;
		}
		return reset();
	}
	
	/**
	 * Increments the counter and returns the new value
	 * @return the new value of the counter
	 */
	private int incr() {
		return counter.get()[0]++;
	}
	
	/**
	 * Returns the current value of the counter 
	 * @return the current value of the counter
	 */
	private int ctr() {
		return counter.get()[0];
	}
	
	/**
	 * Resets the counter to zero.
	 * @return false
	 */
	private boolean reset() {
		counter.get()[0] = 0;
		return false;
	}





}
