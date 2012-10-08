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
 * <p>Title: UnsafeArrayBuilder</p>
 * <p>Description: A builder class to handle the construction parameters for instances of {@link UnsafeLongArray}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.collections.UnsafeArrayBuilder</code></p>
 */

public class UnsafeArrayBuilder {
	
	
	
	/** Indicates the array will be maintained in sorted order. Default is <code>false</code>. */
	private boolean sorted = false;	
	/** Indicates the capacity of the array will be fixed. Default is <code>false</code>. */
	private boolean fixed = false;	
	/** The initial capacity of the array. Default is <code>128</code>. */
	private int initialCapacity = UnsafeArray.DEFAULT_CAPACITY;
	/** The maximum capacity of the array. Default is {@value Integer#MAX_VALUE}. */
	private int maxCapacity = Integer.MAX_VALUE;
	/** The minimum capacity of the array. Default is same as {@link UnsafeArrayBuilder#initialCapacity}. */
	private int minCapacity = 1;	
	/** The number of slots that will be allocated when the array needs to be extended. <code>128</code> */
	private int allocationIncrement = UnsafeArray.DEFAULT_ALLOC_INCR;
	/** The number of excess slots that are emptied by rollLefts before the array capacity is shrunk. <code>128</code> */
	private int clearedSlotsFree = UnsafeArray.DEFAULT_ALLOC_INCR;
	
	/**
	 * Returns a new UnsafeArrayBuilder
	 * @return a new UnsafeArrayBuilder
	 */
	public static UnsafeArrayBuilder newBuilder() {
		return new UnsafeArrayBuilder();
	}
	
	/**
	 * Builds and returns an UnsafeLongArray 
	 * @return a new UnsafeLongArray
	 */
	public UnsafeLongArray buildLongArray() {
		return UnsafeLongArray.build(this);
	}
	
	/**
	 * Builds and returns an UnsafeLongArray, initializing it with the values from the passed object
	 * @param data The values to initialize the array with  
	 * @return a new UnsafeLongArray
	 */
	public UnsafeLongArray buildLongArray(Object data) {
		return UnsafeLongArray.build(this, data);
	}
	
	
	/**
	 * Private Ctor. Use {@link UnsafeArrayBuilder#newBuilder()}. 
	 */
	private UnsafeArrayBuilder(){}
	
	/**
	 * Returns true if sorted, false otherwise
	 * @return true if sorted, false otherwise
	 */
	public boolean sorted() {
		return sorted;
	}
	
	/**
	 * Sets if the built array will be sorted or not 
	 * @param sorted true for a sorted array, false for unsorted
	 * @return this builder
	 */
	public UnsafeArrayBuilder sorted(boolean sorted) {
		this.sorted = sorted;
		return this;
	}
	
	/**
	 * Returns true if fixed capacity, false otherwise
	 * @return true if fixed capacity, false otherwise
	 */
	public boolean fixed() {
		return fixed;
	}
	
	/**
	 * Sets if the built array will be fixed capacity or not
	 * @param fixed true for fixed capacity, false for extendable
	 * @return this builder
	 */
	public UnsafeArrayBuilder fixed(boolean fixed) {
		this.fixed = fixed;
		return this;
	}
	
	/**
	 * Returns the initial capacity of the array 
	 * @return the initial capacity of the array
	 */
	public int initialCapacity() {
		return initialCapacity;
	}
	
	/**
	 * Sets the initial capacity of the array
	 * @param initialCapacity the initial capacity of the array
	 * @return this builder
	 */
	public UnsafeArrayBuilder initialCapacity(int initialCapacity) {
		this.initialCapacity = initialCapacity;
		return this;
	}
	
	/**
	 * Returns the max capacity of the array 
	 * @return the initial capacity of the array
	 */
	public int maxCapacity() {
		return maxCapacity;
	}
	
	/**
	 * Sets the max capacity of the array 
	 * @param maxCapacity the max capacity of the array
	 * @return this builder
	 */
	public UnsafeArrayBuilder maxCapacity(int maxCapacity) {
		this.maxCapacity = maxCapacity;
		return this;
	}
	
	/**
	 * Returns the min capacity of the array
	 * @return the min capacity of the array
	 */
	public int minCapacity() {
		return minCapacity;
	}
	
	/**
	 * Sets the min capacity of the array 
	 * @param minCapacity the min capacity of the array
	 * @return this builder
	 */
	public UnsafeArrayBuilder minCapacity(int minCapacity) {
		this.minCapacity = minCapacity;
		return this;
	}
	
	/**
	 * Returns the allocation increment 
	 * @return the allocation increment
	 */
	public int allocationIncrement() {
		return allocationIncrement;
	}
	
	/**
	 * Sets the allocation increment
	 * @param allocationIncrement the allocation increment
	 * @return this builder
	 */
	public UnsafeArrayBuilder allocationIncrement(int allocationIncrement) {
		this.allocationIncrement = allocationIncrement;
		return this;
	}
	
	/**
	 * Returns the number of cleared slots that will trigger a shrink if not fixed capacity 
	 * @return the number of cleared slots that will trigger a shrink
	 */
	public int clearedSlotsFree() {
		return clearedSlotsFree;
	}
	
	/**
	 * Sets the number of cleared slots that will trigger a shrink if not fixed capacity 
	 * @param clearedSlotsFree the number of cleared slots that will trigger a shrink
	 * @return this builder
	 */
	public UnsafeArrayBuilder clearedSlotsFree(int clearedSlotsFree) {
		this.clearedSlotsFree = clearedSlotsFree;
		return this;
	}
	
	
	
}
