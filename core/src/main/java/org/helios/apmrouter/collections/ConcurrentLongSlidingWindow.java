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

public class ConcurrentLongSlidingWindow extends LongSlidingWindow {
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
	 * Inserts the each passed value into the first slot position in the array dropping the values in the last slot to make room if required
	 * @param values The values to insert
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
	 * Removes all the values from this array, keeping the capacity fixed.
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
	 * Indicates if this set is empty
	 * @return true if this set is empty, false otherwise
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
	 * @see java.lang.Object#toString()
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
	 * @see java.lang.Object#clone()
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
	 * Returns the long value at the specified array index
	 * @param index the index of the value to retrieve 
	 * @return the long value at the specified array index
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
	 * Returns the number of entries in the array
	 * @return the number of entries in the array
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
	 * Returns the sum of all the longs in the array within the passed ending index range
	 * @param within The index of the last entry to sum 
	 * @return the sum of all the longs in the array
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
	 * Returns the sum of all the longs in the array 
	 * @return the sum of all the longs in the array
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
	 * Returns the average of all the longs in the array within the passed ending index range
	 * @param within The index of the last entry to average 
	 * @return the average of all the longs in the array
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
	 * Returns the average of all the longs in the array 
	 * @return the average of all the longs in the array
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
