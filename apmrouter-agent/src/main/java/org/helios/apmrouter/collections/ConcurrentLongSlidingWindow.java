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
package org.helios.apmrouter.collections;

import java.nio.LongBuffer;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

/**
 * <p>Title: ConcurrentLongSlidingWindow</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.collections.ConcurrentLongSlidingWindow</code></p>
 */

public class ConcurrentLongSlidingWindow extends LongSlidingWindow implements ILongSlidingWindow {
	/** The reentrant read/write lock */
	private final ReentrantReadWriteLock readWriteLock;
	/** The concurrent read lock */
	private final ReadLock readLock;
	/** The exclusive write lock */
	private final WriteLock writeLock;
	

	/**
	 * Creates a new ConcurrentLongSlidingWindow
	 * @param size the fixed size of the sliding window
	 */
	public ConcurrentLongSlidingWindow(int size) {
		super(size);
		readWriteLock = new ReentrantReadWriteLock(false);
		readLock = readWriteLock.readLock();
		writeLock = readWriteLock.writeLock();		
	}

	/**
	 * Creates a new ConcurrentLongSlidingWindow
	 * @param size the fixed size of the sliding window
	 * @param values the initial values of the sliding window
	 */
	public ConcurrentLongSlidingWindow(int size, long[] values) {
		super(size, values);
		readWriteLock = new ReentrantReadWriteLock(false);
		readLock = readWriteLock.readLock();
		writeLock = readWriteLock.writeLock();		
	}
	
	private ConcurrentLongSlidingWindow(UnsafeLongArray array) {
		super(array);
		readWriteLock = new ReentrantReadWriteLock(false);
		readLock = readWriteLock.readLock();
		writeLock = readWriteLock.writeLock();				
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#insert(long)
	 */
	@Override
	public void insert(long...values) {
		writeLock.lock();
		try {
			super.insert(values);
		} finally {
			writeLock.unlock();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#insert(java.nio.LongBuffer)
	 */
	@Override
	public void insert(LongBuffer longBuff) {
		writeLock.lock();
		try {
				long[] larr = new long[longBuff.limit()];
				longBuff.get(larr);
				super.insert(larr);
		} finally {
			writeLock.unlock();
		}
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#insert(long)
	 */
	@Override
	public Long insert(long value) {
		writeLock.lock();
		try {
			return super.insert(value);
		} finally {
			writeLock.unlock();
		}
	}	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#inc(int, long)
	 */
	@Override
	public long inc(int index, long value) {
		writeLock.lock();
		try {
			if(size()<index+1) throw new ArrayOverflowException("Attempted to increment at index [" + index + "] but size is [" + size() + "]", new Throwable());
			return array.set(index, array.get(index)+value).get(index);
		} finally {
			writeLock.unlock();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#inc(int)
	 */
	@Override
	public long inc(int index) {
		return inc(index, 1L);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#inc(long)
	 */
	@Override
	public long inc(long value) {
		return inc(0, value);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#inc()
	 */
	@Override
	public long inc() {
		return inc(0, 1L);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#find(long)
	 */
	@Override
	public int find(long value) {
		readLock.lock();
		try {
			return array.binarySearch(value);
		} finally {
			readLock.unlock();
		}
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#set(long)
	 */
	@Override
	public void set(long value) {
		writeLock.lock();
		try {
			array.set(0, value);
		} finally {
			writeLock.unlock();
		}
	}
	
    /**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#asDoubleArray()
	 */
	@Override
	public double[] asDoubleArray() {
		readLock.lock();
		try {
			return array.asDoubleArray();
		} finally {
			readLock.unlock();
		}
	}	
	
	/**
	 * Returns this sliding window as a long array
	 * @return a long array
	 */
	public long[] asLongArray() {
		readLock.lock();
		try {
			return array.getArray();
		} finally {
			readLock.unlock();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#load(byte[])
	 */
	@Override
	public void load(byte[] arr) {
		writeLock.lock();
		try {
			array.load(arr);
		} finally {
			writeLock.unlock();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#reinitAndLoad(byte[])
	 */
	@Override
	public void reinitAndLoad(byte[] arr) {
		writeLock.lock();
		try {
			array.initAndLoad(arr);
		} finally {
			writeLock.unlock();
		}
		
		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#clear()
	 */
	@Override
	public void clear() {
		writeLock.lock();
		try {
			super.clear();
		} finally {
			writeLock.unlock();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		readLock.lock();
		try {
			return super.isEmpty();
		} finally {
			readLock.unlock();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#toString()
	 */
	@Override
	public String toString() {
		readLock.lock();
		try {
			return super.toString();
		} finally {
			readLock.unlock();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#clone()
	 */
	@Override
	public LongSlidingWindow clone() {
		readLock.lock();
		try {
			return new LongSlidingWindow(array.clone());
		} finally {
			readLock.unlock();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#get(int)
	 */
	@Override
	public long get(int index) {
		readLock.lock();
		try {
			return array.get(index);
		} finally {
			readLock.unlock();
		}
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#size()
	 */
	@Override
	public int size() {
		readLock.lock();
		try {
			return array.size();
		} finally {
			readLock.unlock();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#sum(int)
	 */
	@Override
	public long sum(int within) {
		readLock.lock();
		try {
			return super.sum(within);
		} finally {
			readLock.unlock();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#sum()
	 */
	@Override
	public long sum() {
		readLock.lock();
		try {
			return super.sum();
		} finally {
			readLock.unlock();
		}

	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#avg(int)
	 */
	@Override
	public long avg(int within) {
		readLock.lock();
		try {
			return super.avg(within);
		} finally {
			readLock.unlock();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.ILongSlidingWindow#avg()
	 */
	@Override
	public long avg() {
		readLock.lock();
		try {
			return super.avg();
		} finally {
			readLock.unlock();
		}

	}
	
	

}
