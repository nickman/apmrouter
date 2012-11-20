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
 * <p>Title: LongSlidingWindow</p>
 * <p>Description: A fixed size sorted "list" of longs that when full, drops the oldest entry to make room for the newest.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.collections.LongSlidingWindow</code></p>
 */

public class LongSlidingWindow {
	/** The underlying UnsafeLongArray */
	protected final UnsafeLongArray array;
	
	/**
	 * Creates a new and empty LongSlidingWindow
	 * @param size The size of the sliding window
	 */
	public LongSlidingWindow(int size) {
		array = UnsafeArrayBuilder.newBuilder().sorted(false).fixed(true).maxCapacity(size).buildLongArray();
	}
	
	/**
	 * Creates a new LongSlidingWindow cloned from the passed array
	 * @param array The array to base this sliding window from
	 */
	protected LongSlidingWindow(UnsafeLongArray array) {
		this.array = array;
	}
	
	public void reinitAndLoad(byte[] arr) {
		array.initAndLoad(arr);
	}
	
    /**
     * Returns this sliding window as a (copied) byte array
     * @return this sliding window as a (copied) byte array
     */
    public byte[] getBytes() {
    	return array.getBytes();
    }	
	
	
	/**
	 * Creates a new LongSlidingWindow with the provided initial values
	 * @param size The size of the sliding window
	 * @param values The initial values to load
	 */
	public LongSlidingWindow(int size, long[] values) {
		array = UnsafeArrayBuilder.newBuilder().sorted(true).fixed(true).maxCapacity(size).buildLongArray();
		for(long v: values) {
			array.rollRight(0, v);
		}
	}
	
	/**
	 * Inserts the each passed value into the first slot position in the array dropping the values in the last slot to make room if required
	 * @param values The values to insert
	 */
	public void insert(long...values) {
		for(long v: values) {
			array.rollRight(0, v);
		}
	}
	
	/**
	 * Inserts the passed value into the first slot of the array, moving all other other populated slots to the right.
	 * @param value The value to insert
	 * @return The dropped value if one was dropped, otherwise null
	 */
	public Long insert(long value) {
		return array.rollRightCap(0, value);		
	}
	
	
	/**
	 * Removes all the values from this array, keeping the capacity fixed.
	 */
	public void clear() {
		array.clear();
	}
	
	/**
	 * Indicates if this set is empty
	 * @return true if this set is empty, false otherwise
	 */
	public boolean isEmpty() {
		return array.size()==0;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return array.toString();
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#clone()
	 */
	@Override
	public LongSlidingWindow clone() {
		return new LongSlidingWindow(array.clone());
	}
	
	/**
	 * Returns the long value at the specified array index
	 * @param index the index of the value to retrieve 
	 * @return the long value at the specified array index
	 */
	public long get(int index) {
		return array.get(index);
	}
	
	
	/**
	 * Returns the number of entries in the array
	 * @return the number of entries in the array
	 */
	public int size() {
		return array.size();
	}
	
	/**
	 * Returns the sum of all the longs in the array within the passed ending index range
	 * @param within The index of the last entry to sum 
	 * @return the sum of all the longs in the array
	 */
	public long sum(int within) {
		long total = 0;
		int end = within<array.size ? within : array.size;
		for(int i = 0; i < end; i++) {
			total += array.get(i);
		}
		return total;
	}
	
	/**
	 * Returns the sum of all the longs in the array 
	 * @return the sum of all the longs in the array
	 */
	public long sum() {
		return sum(array.size);
	}
	
	/**
	 * Returns the average of all the longs in the array within the passed ending index range
	 * @param within The index of the last entry to average 
	 * @return the average of all the longs in the array
	 */
	public long avg(int within) {
		double total = 0;
		double cnt = 0;
		int end = within<array.size ? within : array.size;
		for(int i = 0; i < end; i++) {
			total += array.get(i);
			cnt++;
		}
		if(total==0 || cnt==0) return 0;
		double d = total/cnt;
		return (long)d;
	}
	
	/**
	 * Returns the average of all the longs in the array 
	 * @return the average of all the longs in the array
	 */
	public long avg() {
		return avg(array.size);
	}
	
	

}
