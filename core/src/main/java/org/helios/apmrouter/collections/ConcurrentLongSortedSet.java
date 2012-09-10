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
 * <p>Title: ConcurrentLongSortedSet</p>
 * <p>Description: An extension of {@link LongSortedSet} which adds thread safety and increased concurrency through a Read/Write lock pair.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.collections.ConcurrentLongSortedSet</code></p>
 */
public class ConcurrentLongSortedSet extends LongSortedSet {

	/** The reentrant read/write lock */
	private final ReentrantReadWriteLock readWriteLock;
	/** The concurrent read lock */
	private final ReadLock readLock;
	/** The exclusive write lock */
	private final WriteLock writeLock;
	
	
	
	
	/**
	 * Creates a new ConcurrentLongSortedSet with an unfair lock
	 * @param array the cloned array
	 */
	protected ConcurrentLongSortedSet(UnsafeLongArray array) {
		this(false, array);
	}
	
	/**
	 * Creates a new ConcurrentLongSortedSet
	 * @param the fairness of the lock 
	 * @param array the cloned array
	 */
	protected ConcurrentLongSortedSet(boolean fair, UnsafeLongArray array) {
		super(array);
		readWriteLock = new ReentrantReadWriteLock(fair);
		readLock = readWriteLock.readLock();
		writeLock = readWriteLock.writeLock();		
	}
	
	/**
	 * Creates a new LongSortedSet with the default initial capacity of {@link UnsafeArray#DEFAULT_CAPACITY}.
	 * @param boolean the fairness of the lock
	 */
	public ConcurrentLongSortedSet(boolean fair) {
		this(fair, UnsafeArray.DEFAULT_CAPACITY);
	}
	
	/**
	 * Creates a new LongSortedSet with the default initial capacity of {@link UnsafeArray#DEFAULT_CAPACITY} and an unfair lock.
	 */
	public ConcurrentLongSortedSet() {
		this(UnsafeArray.DEFAULT_CAPACITY);
	}
	
	/**
	 * Creates a new ConcurrentLongSortedSet with the specified initial capacity and an unfair lock
	 * @param initialCapacity The initial number of allocated slots
	 */
	public ConcurrentLongSortedSet(int initialCapacity) {
		this(false, initialCapacity);
	}
	
	/**
	 * Creates a new ConcurrentLongSortedSet with the specified initial capacity.
	 * @param the fairness of the lock
	 * @param initialCapacity The initial number of allocated slots
	 */
	public ConcurrentLongSortedSet(boolean fair, int initialCapacity) {
		super(UnsafeArrayBuilder.newBuilder().sorted(true).initialCapacity(initialCapacity).buildLongArray());
		readWriteLock = new ReentrantReadWriteLock(fair);
		readLock = readWriteLock.readLock();
		writeLock = readWriteLock.writeLock();		
	}
	
	
	/**
	 * Creates a new ConcurrentLongSortedSet initialized with the passed values and with an unfair lock
	 * @param values The long array to initialize with
	 */
	public ConcurrentLongSortedSet(long...values) {
		this(false, values);
	}
	
	/**
	 * Creates a new ConcurrentLongSortedSet initialized with the passed values
	 * @param the fairness of the lock
	 * @param values The long array to initialize with
	 * 
	 */
	public ConcurrentLongSortedSet(boolean fair, long...values) {
		super(UnsafeArrayBuilder.newBuilder().sorted(true).buildLongArray(values));
		readWriteLock = new ReentrantReadWriteLock(fair);
		readLock = readWriteLock.readLock();
		writeLock = readWriteLock.writeLock();				
	}
	
	
	/**
	 * Creates a new ConcurrentLongSortedSet initialized with the passed values and an unfair lock
	 * @param values the values to copy into this new sorted set
	 */
	public ConcurrentLongSortedSet(LongSortedSet values) {
		this(false, values);
	}

	/**
	 * Creates a new ConcurrentLongSortedSet initialized with the passed values
	 * @param the fairness of the lock
	 * @param values the values to copy into this new sorted set
	 */
	public ConcurrentLongSortedSet(boolean fair, LongSortedSet values) {
		super(UnsafeArrayBuilder.newBuilder().sorted(true).buildLongArray(values));
		readWriteLock = new ReentrantReadWriteLock(fair);
		readLock = readWriteLock.readLock();
		writeLock = readWriteLock.writeLock();				
	}
	
	/**
	 * Inserts the each passed value into the correct slot position in the array if the value is not present in the array already. 
	 * @param values The values to insert
	 * @return true if any of the values were successfully added
	 * <p>Requires a <b>READ</b> lock.
	 */
	public boolean add(long...values) {
		writeLock.lock();
		try {
			return super.add(values);
		} finally {
			writeLock.unlock();
		}
	}
	
	/**
	 * Removes all the values from this array, shrinking the capacity if necessary.
	 * <p>Requires a <b>READ</b> lock.
	 */
	public void clear() {
		writeLock.lock();
		try {
			super.clear();
		} finally {
			writeLock.unlock();
		}
	}
	
	/**
	 * Removes all the passed values from the array
	 * @param values The values to remove from the array
	 * @return true if one or more of the values was removed
	 * <p>Requires a <b>READ</b> lock.
	 */
	public boolean remove(long...values) {
		writeLock.lock();
		try {
			return super.remove(values);
		} finally {
			writeLock.unlock();
		}
	}
	
	/**
	 * Returns the long value at the specified array index
	 * @param index the index of the value to retrieve 
	 * @return the long value at the specified array index
	 * <p>Requires a <b>WRITE</b> lock.
	 */
	public long get(int index) {
		readLock.lock();
		try {
			return super.get(index);
		} finally {
			readLock.unlock();
		}
	}
	
	/**
	 * Returns the number of entries in the array
	 * @return the number of entries in the array
	 * <p>Requires a <b>WRITE</b> lock.
	 */
	public int size() {
		readLock.lock();
		try {
			return super.size();
		} finally {
			readLock.unlock();
		}		
	}
	
	/**
	 * Returns true if the passed long value is in the array
	 * @param value the long value to test for
	 * @return true if the passed long value is in the array, false otherwise
	 * <p>Requires a <b>WRITE</b> lock.
	 */
	public boolean contains(long value) {
		readLock.lock();
		try {
			return super.contains(value);
		} finally {
			readLock.unlock();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 * <p>Requires a <b>WRITE</b> lock.
	 */
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
	 * <p>Requires a <b>WRITE</b> lock.
	 */
	public LongSortedSet clone() {
		readLock.lock();
		try {
			return new ConcurrentLongSortedSet(array.clone());
		} finally {
			readLock.unlock();
		}				
	}
	
	/**
	 * Indicates if this set is empty
	 * @return true if this set is empty, false otherwise
	 * <p>Requires a <b>WRITE</b> lock.
	 */
	public boolean isEmpty() {
		readLock.lock();
		try {
			return super.isEmpty();
		} finally {
			readLock.unlock();
		}
		
		
	}
	

}
