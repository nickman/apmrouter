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

import org.jboss.netty.buffer.ChannelBuffer;

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
		if(!cb.readable()) return -1;
		int readable = cb.readableBytes();
		while(pos<readable && !nextByte(cb.getByte(pos))) {
			pos++;
		}
		return matchCount==ctr() ? pos : -1;
	}
	
	private boolean nextByte(byte b) {
		if(b==pattern[ctr()]) {
			return matchCount==incr();
		}
		reset();
		return false;
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
	 */
	private void reset() {
		counter.get()[0] = 0;
	}




	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
