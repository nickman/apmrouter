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

/**
 * <p>Title: SynchronizedLongSortedSet</p>
 * <p>Description: An extension of {@link LongSortedSet} which synchronizes all thread-unsafe methods (which is all of them).</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.collections.SynchronizedLongSortedSet</code></p>
 */
public class SynchronizedLongSortedSet extends LongSortedSet {
	
	
	/**
	 * Creates a new SynchronizedLongSortedSet with the default initial capacity of {@link UnsafeArray#DEFAULT_CAPACITY}.
	 */
	public SynchronizedLongSortedSet() {
		this(UnsafeArray.DEFAULT_CAPACITY);
	}
	
	/**
	 * Creates a new SynchronizedLongSortedSet with the specified initial capacity
	 * @param initialCapacity The initial number of allocated slots
	 */
	public SynchronizedLongSortedSet(int initialCapacity) {
		super(initialCapacity);
	}
	
	/**
	 * Creates a new SynchronizedLongSortedSet initialized with the passed values
	 * @param values The long array to initialize with
	 */
	public SynchronizedLongSortedSet(long[] values) {
		super(values);
	}
	
	/**
	 * Creates a new SynchronizedLongSortedSet initialized with the passed values
	 * @param values the values to copy into this new sorted set
	 */
	public SynchronizedLongSortedSet(LongSortedSet values) {
		super(values);
	}
	
	/**
	 * Creates a new SynchronizedLongSortedSet from an existing array
	 * @param clone the cloned values
	 */
	public SynchronizedLongSortedSet(UnsafeLongArray clone) {
		super(clone);
	}

	/**
	 * Inserts the each passed value into the correct slot position in the array if the value is not present in the array already 
	 * @param values The values to insert
	 * @return true if any of the values were successfully added
	 */
	public synchronized boolean add(long...values) {
		return array.insertIfNotExists(values)>0;
	}
	
	/**
	 * Removes all the values from this array, shrinking the capacity if necessary.
	 */
	public synchronized void clear() {
		array.clear();
	}
	
	/**
	 * Removes all the passed values from the array
	 * @param values The values to remove from the array
	 * @return true if one or more of the values was removed
	 */
	public synchronized boolean remove(long...values) {
		return array.remove(values)>0;
	}
	
	/**
	 * Returns the long value at the specified array index
	 * @param index the index of the value to retrieve 
	 * @return the long value at the specified array index
	 */
	public synchronized long get(int index) {
		return array.get(index);
	}
	
	/**
	 * Returns the number of entries in the array
	 * @return the number of entries in the array
	 */
	public synchronized int size() {
		return array.size();
	}
	
    /**
     * Returns this array as an array of doubles
     * @return an array of doubles
     */
	public synchronized double[] asDoubleArray() {
		return array.asDoubleArray();
	}	
	
	/**
	 * Returns true if the passed long value is in the array
	 * @param value the long value to test for
	 * @return true if the passed long value is in the array, false otherwise
	 */
	public synchronized boolean contains(long value) {
		return array.binarySearch(value)>=0;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	public synchronized String toString() {
		return array.toString();
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#clone()
	 */
	public synchronized SynchronizedLongSortedSet clone() {
		return new SynchronizedLongSortedSet(array.clone());
	}
	
	/**
	 * Indicates if this set is empty
	 * @return true if this set is empty, false otherwise
	 */
	public synchronized boolean isEmpty() {
		return array.size()==0;
	}


}
