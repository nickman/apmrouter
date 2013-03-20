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
package org.helios.apmrouter.collections;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;


/**
 * <p>Title: LongAccumulator</p>
 * <p>Description: Direct buffered long accumulator</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.collections.LongAccumulator</code></p>
 */

public class LongAccumulator {
	/** The read/write lock */
	protected final ReentrantReadWriteLock readWriteLock;
	/** The read lock */
	protected final ReadLock readLock;
	/** The write lock */
	protected final WriteLock writeLock;
	/** The capacity of the buffer */
	protected final int size;
	/** The accumulator byte buffer */
	protected final ByteBuffer buffer;
	/** The accumulator byte buffer as a long buffer */
	protected final LongBuffer longBuffer;
	/** The current number of longs in the buffer */
	protected int currentSize = 0;
	
	/** An empty long array constant */
	public static final long[] EMPTY_ARR = new long[0];
	/** The number of bytes in a long */
	public static final int LONG_BYTES = 8;
	/**
	 * Creates a new LongAccumulator
	 * @param direct true for an off-heap buffer, false for a heap-buffer
	 * @param fair true for a fair lock, false for an unfair lock
	 * @param size the number of longs in this buffer
	 */
	public LongAccumulator(boolean direct, boolean fair, int size) {
		readWriteLock = new ReentrantReadWriteLock(fair);
		readLock = readWriteLock.readLock();
		writeLock = readWriteLock.writeLock();
		this.size = size;
		buffer = direct ? ByteBuffer.allocateDirect(size*LONG_BYTES) : ByteBuffer.allocate(size*LONG_BYTES);
		log("Buff:" + buffer);
		longBuffer = buffer.asLongBuffer();
		log("LBuff:" + longBuffer);
	}
	
	/**
	 * Returns the content of the buffer as a long array
	 * @return the content of the buffer as a long array
	 */
	public long[] getLongs() {
		readLock.lock();
		try {
			if(currentSize==0) return EMPTY_ARR;
			long[] arr = new long[currentSize];
			for(int i = 0; i < currentSize; i++) {
				arr[i]= longBuffer.get(i);
			}			
			return arr;
		} finally {
			readLock.unlock();
		}		
	}
	
	
	
	/**
	 * Clears the buffer, setting the current size to zero.
	 */
	public void clear() {
		writeLock.lock();
		try {
			if(currentSize==0) return;
			buffer.clear();
			currentSize = 0;
		} finally {
			writeLock.unlock();
		}		
	}
	
	/**
	 * Returns the current number of longs in the buffer
	 * @return the current number of longs in the buffer
	 */
	public int getCurrentSize() {
		readLock.lock();
		try {
			return currentSize;
		} finally {
			readLock.unlock();
		}			
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuilder b = new StringBuilder("Long Accumulator[ ");
		long[] longs = getLongs();
		b.append("\n\tSize:").append(longs.length);
		b.append("\n\tContent:").append(Arrays.toString(longs));
		b.append("\n]");
		return b.toString();
	}
	
	/**
	 * Pushes the passed longs into the end of the buffer, compacting the existing ones to the left, and dropping the overflow.
	 * @param values the longs to push into the buffer
	 */
	public void push(long...values) {
		if(values==null || values.length==0) return;
		writeLock.lock();
		try {
			for(long value: values) {
				if(size==currentSize) {
					longBuffer.position(1);
					longBuffer.compact();
				} else {					
					currentSize++;
					longBuffer.position(currentSize-1);
				}
				longBuffer.put(value);				
			}
		} finally {
			writeLock.unlock();
		}		
	}
	
	/**
	 * Returns the minimum value in the buffer, or zero if the buffer is empty.
	 * @return the minimum value in the buffer
	 */
	public long min() {
		long[] arr = getLongs();
		if(arr.length==0) return 0;
		Arrays.sort(arr);
		return arr[0];
	}
	
	/**
	 * Returns the maximum value in the buffer, or zero if the buffer is empty.
	 * @return the maximum value in the buffer
	 */
	public long max() {
		long[] arr = getLongs();
		if(arr.length==0) return 0;
		Arrays.sort(arr);
		return arr[arr.length-1];
	}
	
	/**
	 * Returns the average value in the buffer, or zero if the buffer is empty.
	 * @return the average value in the buffer
	 */
	public long avg() {
		long[] arr = getLongs();
		if(arr.length==0) return 0;
		BigInteger total = BigInteger.valueOf(0);
		for(long l: arr) {
			total = total.add(BigInteger.valueOf(l));
		}
		return avg(total, arr.length);
	}
	
		
	/**
	 * Calcs a long average
	 * @param total The total sum of the longs
	 * @param length The number of longs
	 * @return the average
	 */
	protected long avg(BigInteger total, int length) {
		BigInteger count = BigInteger.valueOf(length);
		if(total.doubleValue()==0 || count.doubleValue()==0) return 0;
		return total.divide(count).longValue();
	}
	
	
	
	public static void main(String[] args) {
		LongAccumulator la = new LongAccumulator(true, false, 10000);
		Random random = new Random(System.currentTimeMillis());
		
		for(int i = 0; i < la.size*2; i++) {
			//la.push(Math.abs(random.nextInt(100)));			
			la.push(Math.abs(random.nextLong()));
		}
		log(la);
		log("Min:" + la.min());
		log("Max:" + la.max());
		log("Avg:" + la.avg());
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
}
