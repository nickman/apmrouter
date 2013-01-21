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

import java.util.Arrays;

import org.helios.apmrouter.unsafe.UnsafeAdapter;


/**
 * <p>Title: UnsafeDoubleArray</p>
 * <p>Description: Utility class for storing double arrays in direct memory with self resizing</p> 
 * <p><b><font color='red'>!!  NOTE !!&nbsp;&nbsp;</font>:&nbsp;&nbsp;</b>This class is disastrously THREAD UNSAFE. Only use with one thread at a time, or used one
 * of the concurrent/synchronized versions</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.collections.UnsafeDoubleArray</code></p>
 */
public class UnsafeDoubleArray extends UnsafeArray {
	// ==================================================================================
	//			Constants used in the array sort
	// ==================================================================================
    /** The maximum number of runs in merge sort.*/
    private static final int MAX_RUN_COUNT = 67;
    /** The maximum length of run in merge sort. */
    private static final int MAX_RUN_LENGTH = 33;
    /** If the length of an array to be sorted is less than this constant, Quicksort is used in preference to merge sort. */
    private static final int QUICKSORT_THRESHOLD = 286;
    /** If the length of an array to be sorted is less than this constant, insertion sort is used in preference to Quicksort. */
    private static final int INSERTION_SORT_THRESHOLD = 47;    
	// ==================================================================================
    /** The memory offset for a double array */
    public static final long DOUBLE_ARRAY_OFFSET = unsafe.arrayBaseOffset(double[].class);
    
    
 
    
	/**
	 * Creates a new UnsafeDoubleArray
	 * @param initialCapacity The initial allocated capacity
	 * @param sorted Indicates the array will be maintained in sorted order
	 * @param fixed Indicates the capacity of the array will be fixed
	 * @param maxCapacity The maximum capacity of the array
	 * @param minCapacity The minimum capacity of the array
	 * @param allocationIncrement The number of slots that will be allocated when the array needs to be extended
	 * @param clearedSlotsFree The number of excess slots that are emptied by rollLefts before the array capacity is shrunk
	 */
	private UnsafeDoubleArray(int initialCapacity, boolean sorted, boolean fixed, int maxCapacity,
			int minCapacity, int allocationIncrement, int clearedSlotsFree) {
		super(initialCapacity, sorted, fixed, fixed ? initialCapacity : maxCapacity, minCapacity, allocationIncrement, clearedSlotsFree);
	}
	
	
	
	/**
	 * Creates a new UnsafeDoubleArray. Used for cloning.
	 * @param size The size of the clone
	 * @param capacity The capacity of the clone
	 * @param address The memory address of the array to be cloned
	 * @param sorted Indicates the array will be maintained in sorted order
	 * @param fixed Indicates the capacity of the array will be fixed
	 * @param maxCapacity The maximum capacity of the array
	 * @param minCapacity The minimum capacity of the array
	 * @param allocationIncrement The number of slots that will be allocated when the array needs to be extended
	 * @param clearedSlotsFree The number of excess slots that are emptied by rollLefts before the array capacity is shrunk
	 */
	private UnsafeDoubleArray(int size, int capacity, long address, boolean sorted,
			boolean fixed, int maxCapacity, int minCapacity,
			int allocationIncrement, int clearedSlotsFree) {
		super(size, capacity, address, sorted, fixed, maxCapacity, minCapacity,
				allocationIncrement, clearedSlotsFree);
	}
	
	/**
	 * Creates a new fixed capacity and unsorted UnsafeDoubleArray with initial, min and max capacity set to the passed size.
	 * For internal use.
	 * @param size the initial capacity
	 */
	private UnsafeDoubleArray(int size) {
		this(size, false, true, size, size, 0, 0);
	}
	

	/**
	 * Creates a new UnsafeDoubleArray from the passed builder
	 * @param builder The builder to configure the new UnsafeDoubleArray
	 * @param data The optional data load load 
	 * @return the new UnsafeDoubleArray
	 */
	static UnsafeDoubleArray build(UnsafeArrayBuilder builder, Object data) {		
		
		UnsafeDoubleArray ula = new UnsafeDoubleArray(builder.initialCapacity(), builder.sorted(), builder.fixed(), builder.maxCapacity(), builder.minCapacity(), builder.allocationIncrement(), builder.clearedSlotsFree());
		if(data!=null) {
			if(data instanceof double[]) {
				ula.load(data);
			} else if(data instanceof UnsafeDoubleArray) {
				ula.load((UnsafeDoubleArray)data);
			}
		}
		return ula;
	}
	
	/**
	 * Creates a new UnsafeDoubleArray from the passed builder
	 * @param builder The builder to configure the new UnsafeDoubleArray
	 * @return the new UnsafeDoubleArray
	 */
	static UnsafeDoubleArray build(UnsafeArrayBuilder builder) {
		return build(builder, null);
	}
	

	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.UnsafeArray#getSlotSize()
	 */
	@Override
	protected int getSlotSize() {
		return 3;
	}
	
	public static double[] convert(byte[] arr) {
		int len = arr.length;
		if(len%8!=0) throw new RuntimeException("Mod check failed", new Throwable());
		int arrsize = len/8;
		double[] larr = new double[arrsize];
		UnsafeAdapter.copyMemory(arr, BYTE_ARRAY_OFFSET, larr, DOUBLE_ARRAY_OFFSET, arr.length);
		
		return larr;

	}
	
	

    // ======================================================================================
	//			Standard Load Impls.
	// ======================================================================================
	
	
	protected void initAndLoad(byte[] arr) {
		int len = arr.length;
		if(len%8!=0) throw new RuntimeException("Mod check failed", new Throwable());
		size = len/8;
		freeMemory(address);
		address = allocateMemory(len << 3);
		UnsafeAdapter.copyMemory(arr, BYTE_ARRAY_OFFSET, null, address, arr.length);		
	}
	
	/**
	 * Loads this array from a long array
	 * @param arr The array to load
	 */
	protected void load(double[] arr) {		
		if(arr.length<1) return;
		if(arr.length>maxCapacity) throw new ArrayOverflowException("Passed array of length [" + arr.length + "] is too large for this UnsafeDoubleArray with a max capacity of [" + maxCapacity + "]", new Throwable());
		freeMemory(address);
		address = allocateMemory(arr.length << 3);
		UnsafeAdapter.copyMemory(arr, DOUBLE_ARRAY_OFFSET, null, address, arr.length << 3);
		size = capacity = arr.length;
		if(sorted) sort();
	}
	
	/**
	 * Loads this array from another UnsafeDoubleArray
	 * @param ula The UnsafeDoubleArray to copy
	 */
	private void load(UnsafeDoubleArray ula) {				
		if(ula.size>maxCapacity) throw new ArrayOverflowException("Passed UnsafeDoubleArray of size [" + ula.size + "] is too large for this UnsafeDoubleArray with a max capacity of [" + maxCapacity + "]", new Throwable());
		freeMemory(address);		
		address = allocateMemory(ula.size << 3);
		unsafe.copyMemory(ula.address, address, ula.size << 3);
		size = capacity = ula.size;
		if(sorted) sort();
	}
	
	// ======================================================================================
	
	
    


    
    
    /**
     * <p>Rolls all the entries in the array one slot to the right after the referenced index, 
     * optionally extending the array capacity if it is full when this method is called. 
     * Logically, this opens a new slot at the referenced index, and the new slot is set to the passed new value.
     * Once this method completes, the size of the array will have been incremented by 1, unless <b><code>fixedSize==true</code></b>
     * in which case both the size and the capacity will be unchanged.</p>
     * If this array is fixed capacity when <b><code>size==capacity</code></b>, 
     * the right-most value of the array will be dropped, effectively creating a sliding-window when used with <b><code>index==0</code></b>.
     * <p><b>Note:</b> The rolling of the array values is performed by {@link sun.misc.Unsafe#copyMemory(long, long, long)}</p>
     * <p><b>Example</b> of calling <b><code>rollRight(1, 77, bool)</code></b> on an array of size 6 and capacity of 8</p>
     * <p>If this array is sorted, the index is checked for the passed value and a RuntimeException will be thrown if the index is incorrect.
     * @param index The index after which the remaining values are rolled to the right
     * @param newValue The value to place into the new slot 
     * @return this array
     */
    public UnsafeDoubleArray rollRight(int index, double newValue) {
    	if(sorted && normalizedBinarySearch(newValue)!=index) throw new RuntimeException("The index [" + index + "] is incorrect for the value [" + newValue + "] for this sorted array", new Throwable());
    	rollRight(index);
		a(index, newValue);
    	return this;
    }
    
    /**
     * <p>Rolls all the entries in the array one slot to the right after the referenced index, 
     * optionally extending the array capacity if it is full when this method is called. 
     * Logically, this opens a new slot at the referenced index, and the new slot is set to the passed new value.
     * Once this method completes, the size of the array will have been incremented by 1, unless <b><code>fixedSize==true</code></b>
     * in which case both the size and the capacity will be unchanged.</p>
     * If this array is fixed capacity when <b><code>size==capacity</code></b>, 
     * the right-most value of the array will be dropped, effectively creating a sliding-window when used with <b><code>index==0</code></b>.
     * <p><b>Note:</b> The rolling of the array values is performed by {@link sun.misc.Unsafe#copyMemory(long, long, long)}</p>
     * <p><b>Example</b> of calling <b><code>rollRight(1, 77, bool)</code></b> on an array of size 6 and capacity of 8</p>
     * <p>If this array is sorted, the index is checked for the passed value and a RuntimeException will be thrown if the index is incorrect.
     * @param index The index after which the remaining values are rolled to the right
     * @param newValue The value to place into the new slot 
     * @return the value of the dropped slot, or null if a slot was not dropped
     */
    public Double rollRightCap(int index, double newValue) {
    	if(sorted && normalizedBinarySearch(newValue)!=index) throw new RuntimeException("The index [" + index + "] is incorrect for the value [" + newValue + "] for this sorted array", new Throwable());
    	final Double dropped = rollRightCap(index);
		a(index, newValue);
    	return dropped;
    }    
    
    /**
     * <p>Rolls all the entries in the array one slot to the right after the referenced index, 
     * optionally extending the array capacity if it is full when this method is called. 
     * Logically, this opens a new slot at the referenced index, and the new slot is set to the passed new value.
     * Once this method completes, the size of the array will have been incremented by 1, unless <b><code>this.fixed==true</code></b>
     * in which case both the size and the capacity will be unchanged.</p>
     * If this array is fixed capacity when <b><code>size==capacity</code></b>, 
     * the right-most value of the array will be dropped, effectively creating a sliding-window when used with <b><code>index==0</code></b>.
     * <p><b>Note:</b> The rolling of the array values is performed by {@link sun.misc.Unsafe#copyMemory(long, long, long)}</p>
     * <p><b>Example</b> of calling <b><code>rollRight(1, 77, bool)</code></b> on an array of size 6 and capacity of 8</p>
     * <b>Before Operation</b>
     * <pre>
	           -->  -->  -->  -->  -->
	    +--+ +--+ +--+ +--+ +--+ +--+               Size:      6     Index:   1
	    |23| |47| |19| |67| |42| |89|               Capacity:  8     Value:   77
	    +--+ +--+ +--+ +--+ +--+ +--+ +--+ +--+
	          /^\
	           |
	         Index
      </pre><b>After Operation</b><pre>
	     +--+ +--+ +--+ +--+ +--+ +--+ +--+          Size:      7
	     |23| |77| |47| |19| |67| |42| |89|          Capacity:  8
	     +--+ +--+ +--+ +--+ +--+ +--+ +--+ +--+
     * </pre>
     * @param index The index after which the remaining values are rolled to the right
     * @return the rightmost item that was dropped to make room for the new value, or null if no slot was dropped
     */
    protected Double rollRightCap(int index) {
    	_check(); _checkc(index);
    	final int numberOfSlotsToMove;
    	final boolean incrSize;
    	final Double dropped;
    	if(size==capacity) {
        	if(fixed) {
        		numberOfSlotsToMove = size-index-1;
        		incrSize=false;
        		dropped = a(size-1);
        	} else {
        		extend(false, 1);
        		numberOfSlotsToMove = size-index;
        		incrSize=true;
        		dropped = null;
        	}    	
    	} else {
    		numberOfSlotsToMove = size-index;
    		incrSize=true;
    		dropped = null;
    	}
    	
    	long srcOffset = (index << slotSize); 
    	long destOffset = ((index+1) << slotSize);
    	long bytes = numberOfSlotsToMove << slotSize;
		unsafe.copyMemory(
				(address + srcOffset),   	// src: the address of the first index we want to roll
				(address + destOffset), 	// dest: the address of the slot after the one we want to roll
				bytes						// bytes: the number of bytes in the entries that need to be rolled
		);		
		if(incrSize) size++;
		return dropped;
    }
    
    
    /**
     * Adjusts the binary search result to the actual index to insert into
     * @param v The double value to insert
     * @return the index to insert into
     */
    public int normalizedBinarySearch(double v) {
    	int index = binarySearch(v);
    	return (index<0) ? (index*-1)-1 : index;
    	
    }
    
    /**
     * {@inheritDoc}
     * @see org.helios.apmrouter.collections.UnsafeArray#append(java.lang.StringBuilder, int)
     */
    @Override
    protected StringBuilder append(StringBuilder b, int i) {
    	return b.append(a(i));
    }
    
    /**
     * Appends the passed double values to this array.
     * If this array is sorted, this operation will trigger a sort once the append is complete
     * @param values the values to add
     * @return this array
     * TODO: If the size is fixed, make space for the appended values by dropping on the left
     */
    public UnsafeDoubleArray append(double...values) {
    	_check();    	
    	if(values!=null && values.length>0) {
    		int vl = values.length;
    		int newSize = vl + size;
    		if(newSize > maxCapacity) throw new ArrayOverflowException("Passed array of length [" + vl + "] is too large for this UnsafeDoubleArray with a max capacity of [" + maxCapacity + "]", new Throwable());
    		while(newSize > capacity) {
    			extend(false, vl);
    		}
    		UnsafeAdapter.copyMemory(values, DOUBLE_ARRAY_OFFSET, null, address + (size << 3), vl << 3);    		
        	size += vl;
    	}
    	if(sorted) sort();
    	return this;
    }
    
    /**
     * Returns this array as a byte array
     * @return this array as a byte array
     */
    protected byte[] getBytes() {
    	byte[] bytes = new byte[size*8];
    	UnsafeAdapter.copyMemory(null, address, bytes, BYTE_ARRAY_OFFSET, size << 3);
    	return bytes;
    }
    
    protected byte[] getBytesReversed() {
    	byte[] bytes = new byte[size*8];
    	int offset = BYTE_ARRAY_OFFSET + (size << 3);
    	for(int i = 0; i < size; i++) {
    		UnsafeAdapter.copyMemory(null, address, bytes, offset, 8);
    		offset -= 8;
    	}
    	//unsafe.copyMemory(null, address, bytes, BYTE_ARRAY_OFFSET, size << 3);
    	return bytes;
    }    
    
    
    /**
     * Appends as many of the passed double values to this array as will fit up to the max capacity, discarding the remaining values.
     * If this array is sorted, this operation will trigger a sort once the append is complete
     * @param values the values to add
     * @return the number of dicarded values that were dropped
     * TODO: If the size is fixed, make space for the appended values by dropping on the left
     */
    public int appendWhatFits(double...values) {
    	_check();
    	int howManyWillFit = 0;
    	int vl = 0;
    	if(values!=null && values.length>0) {
    		vl = values.length;
    		int currentCap = (maxCapacity-size); 
    		howManyWillFit = vl<=currentCap ? vl : currentCap;
    		int newSize = size + howManyWillFit; 
    		while(newSize > capacity) {
    			extend(true, vl);
    		}
    		UnsafeAdapter.copyMemory(values, DOUBLE_ARRAY_OFFSET, null, address + (size << 3), howManyWillFit << 3);    		
        	size = newSize;
    	}
    	if(sorted) sort();
    	return vl-howManyWillFit;
    }
    
    
    /**
     * Inserts the passed long values to this array at the location returned from a binary search .
     * Throws a {@link RuntimeException} if this array is not sorted.
     * May throw a {@link PartialArrayOverflowException} if the capacity is exhausted in which case the exception will provide the number of values successfully inserted.
     * @param values the values to insert
     * @return this array
     */
    public UnsafeDoubleArray insert(double...values) {
    	_check();
    	if(!sorted) throw new RuntimeException("Cannot insert into an unsorted array", new Throwable());    	
    	if(values!=null && values.length>0) {
    		for(int i = 0; i < values.length; i++) {
    			try {
		    		_insert(values[i]);
    			} catch (Exception e) {
    				throw new PartialArrayOverflowException(i, "Partial overflow at item [" + i + "]", e);
    			}
    		}
    	}
    	return this;
    }
    
    
    
    /**
     * Inserts the passed long values to this array if they are not present already.
     * Throws a {@link RuntimeException} if this array is not sorted.
     * May throw a {@link PartialArrayOverflowException} if the capacity is exhausted in which case the exception will provide the number of values successfully inserted. 
     * @param values the values to insert
     * @return the number of items inserted
     */
    public int insertIfNotExists(double...values) {
    	_check();
    	int insertCount = 0; 
    	if(values!=null && values.length>0) {
    		for(int i = 0; i < values.length; i++) {
    			try {
    				if(binarySearch(values[i])<0) {
    					_insert(values[i]);
    					insertCount++;
    				} 
		 		} catch (Exception e) {
    				throw new PartialArrayOverflowException(i, "Partial overflow at item [" + i + "]", e);
    			}
    		}
    	}
    	return insertCount;
    }
    
    
    
    
    /**
     * Inserts the passed long to the array, extending the size of the array if necessary
     * @param v the long to insert
     */
    private void _insert(double v) {
    	if(size==capacity) {
    		extend(fixed, 1);  // allow truncation if capacity is fixed
    	}
		int index = binarySearch(v);
		if(index<0) index = (index*-1)-1;
		
		if(index==size) {
			append(v);
		} else if(size!=0) {
			rollRight(index, v);
		} else {
			a(0, v);
			size++;			
		}
		//log("\n\t--->At Size:" + size + "  Added:" + v +  "  Index:" + index + "  " +  toFullString());
    }
    
    
    
    
    
    /**
     * <p>Removes the first instance of each of the passed long values from this array if they are present.
     * <p><b>NOTE:</b>This operation can be slow-ish if the array is not sorted.
     * @param values the values to remove
     * @return the number of removed values
     */
    public int remove(double...values) {
    	_check();
    	int removed = 0;
    	if(values!=null && values.length>0) {
    		for(double v: values) {
    			if(sorted) {
    				removed += _remove(binarySearch(v)) ? 1 : 0;
    			} else {
    				for(int i = 0; i < size; i++) {
    					if(a(i)==v) {
    						removed += _remove(i) ? 1 : 0;
    						break;
    					}
    				}
    			}
    		}
    	}
    	//sort(this);
    	shrink();
    	return removed;
    }
    
    /**
     * Removes all instances of each of the passed long values from this array if they are present
     * <p><b>NOTE:</b>This operation can be slow-ish if the array is not sorted. 
     * @param values the values to remove
     * @return the number of removed items
     */
    public int removeAll(double...values) {
    	_check();
    	int _size = size;
    	if(values!=null && values.length>0) {
    		for(double v: values) {
    			if(sorted) {
	    			boolean rem = true;
	    			while(rem) {
	    				rem = _remove(binarySearch(v));
	    			}
    			} else {
    				for(int i = 0; i < size; i++) {
    					if(a(i)==v) {
    						_remove(i);    						
    					}
    				}    				
    			}
    		}
    	}
    	shrink();
    	return _size - size;
    }
    
    
    /**
     * Removes the long at the passed index, if the index is <b><code>&gt;=0</code></b>,  by rolling all the values at the next index down by one and decrementing the size
     * @param index The index to remove thhe value from
     * @return true if the value was removed, false if no change occured
     */
    private boolean _remove(int index) {
    	if(index>=0) {
    		rollLeft(false, index);
    		return true;
    	}
    	return false;
    }
    
    
    /**
     * Returns the double at the specified index
     * @param index The index of the double to retrieve
     * @return the specified double
     */
    private double a(int index) {
    	return unsafe.getDouble(this.address + (index << 3));    	
    }
    
    /**
     * Sets the double value at the specified index
     * @param index The index of the array to set the double at
     * @param value The double to set
     */
    private void a(int index, double value) {
    	unsafe.putDouble(this.address + (index << 3), value);
    }
    
    /**
     * Returns the long at the specified index
     * @param index The index of the long to retrieve
     * @return the specified long
     */
    public long get(int index) {
    	_check(); _check(index);
    	return unsafe.getLong(this.address + (index << 3));    	
    }
    
    /**
     * Sets the long value at the specified index
     * @param index The index of the array to set the long at
     * @param value The long to set
     * @return this array
     */
    public UnsafeDoubleArray set(int index, long value) {
    	_check(); _check(index);
    	unsafe.putLong(this.address + (index << 3), value);
    	return this;
    }
    

    
    
    
    /**
     * Returns a traditional double array representing the double in this array
     * @return a long array with the same values as this array
     */
    public double[] getArray() {
    	_check();
    	double[] arr = new double[size];
    	if(size>0) UnsafeAdapter.copyMemory(null, address, arr, DOUBLE_ARRAY_OFFSET, size << 3);
    	return arr;
    }
    
    /**
     * Returns a traditional double array representing the all the allocated slots this array
     * @return a double array representing the all the allocated slots this array
     * TODO: This should be deprecated unless there is any testing use for it.
     */
    public double[] getAllocatedArray() {
    	_check();
    	double[] arr = new double[capacity];
    	UnsafeAdapter.copyMemory(null, address, arr, DOUBLE_ARRAY_OFFSET, capacity << 3);
    	return arr;
    }
    
    /**
     * Returns this array as an array of doubles
     * @return an array of doubles
     */
    public double[] asDoubleArray() {
    	_check();
    	if(size==0) return new double[0];
    	double[] arr = new double[size];
    	UnsafeAdapter.copyMemory(null, address, arr, DOUBLE_ARRAY_OFFSET, size << 3);
    	return arr;    	
    }
    
    
    /**
     * <p>Creates a clone of this array in a completely seprarate memory adddress, meaning
     * that changes to the clone are not seen by this array and vice-versa.
     * {@inheritDoc}
     * @see java.lang.Object#clone()
     */
    @Override
	public UnsafeDoubleArray clone() {
    	_check();
    	return new UnsafeDoubleArray(size, capacity, address, sorted, fixed, maxCapacity, minCapacity, allocationIncrement, clearedSlotsFree);
    }
    

    
    
    /**
     * Searches this unsafe double array for the specified value using the binary search algorithm based on {@link java.util.Arrays#binarySearch(double[], double)}. 
     * If this unsafe long array contains multiple elements with the specified value, there is no guarantee which one will be found. 
     * @param key the value to be searched for 
     * @return index of the search key, if it is contained in this array; otherwise, <b><code>(-(insertion point) - 1)</code></b>.
     */
    public int binarySearch(double key) {
    	int low = 0;
    	int high = size - 1;

    	while (low <= high) {
    		int mid = (low + high) >>> 1;
    		double midVal = a(mid);

    		if (midVal < key)
    			low = mid + 1;
    		else if (midVal > key)
    			high = mid - 1;
    		else
    			return mid; // key found
    	}
    	return -(low + 1);  // key not found.
    }    
    
    /**
     * Sorts the array and returns
     * @return this array
     */
    public UnsafeDoubleArray sort() {
    	sort(this);
    	return this;
    }
    
	/**
	 * Calculates a low collision hash code for this array
	 * @return the long hashcode
	 */
	public long longHashCode() {
		_check();
		long h = 0;        
    	int off = 0;    	
    	int hashPrime = hashCode();
        for (int i = 0; i < size; i++) {
            h = (31*h + ((long)a(off++)) + (hashPrime*h));
        }
        return h;
	}
	

    /**
     * {@inheritDoc}
     * @see java.lang.Object#hashCode()
     */
    @Override
	public int hashCode() {
    	_check();
        int result = 1;
        for(int i = 0; i < size; i++) {
        	double element = a(i);
            int elementHash = (int)((long)element ^ (((long)element) >>> 32));
            result = 31 * result + elementHash;
        }
        return result;
    }
    
    /**
     * 
     * {@inheritDoc}
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		UnsafeDoubleArray other = (UnsafeDoubleArray) obj;
		if (address != other.address) {
			return false;
		}
		return true;
    	
    }
	

	
    
	
    
    /**
     * Sorts the passed UnsafeDoubleArray.
     * TODO: Implement native algorithm.
     * @param ula The UnsafeDoubleArray to sort
     */
    public static void sort(UnsafeDoubleArray ula) {
    	double[] arr = ula.asDoubleArray();
    	Arrays.sort(arr);
    	ula.clear();
    	ula.load(arr);
	}

    
}


//
//public static void mainy(String[] args) {
//	final long[] TEST_DATA = new long[]{0L, 1L, 2L};
//	final long REMOVE = 1L;
//	log("Add longs to UnsafeDoubleArray Test");
//	UnsafeDoubleArray ula = new UnsafeDoubleArray(3, Long.MAX_VALUE);
//	log("Empty:\n\t" + ula.toFullString() + "\n\t" + ula.toString() + "\n\tSize:" + ula.size());
//	ula.insert(TEST_DATA);
//	log("Full At 3:\n\t" + ula.toFullString() + "\n\t" + ula.toString() + "\n\tSize:" + ula.size());
//	ula.insert(TEST_DATA);
//	log("Full At 6:\n\t" + ula.toFullString() + "\n\t" + ula.toString() + "\n\tSize:" + ula.size());
//	int removed = ula.removeAll(REMOVE);
//	log( "" + removed + " removed:\n\t" + ula.toFullString() + "\n\t" + ula.toString() + "\n\tSize:" + ula.size());
//	log("BinSearch for :" + REMOVE + ":" + ula.binarySearch(REMOVE));
//	log(ula.debugString());
//	ula.insertIfNotExists(REMOVE);
//	log("" + REMOVE + " back in once:\n\t" + ula.toFullString() + "\n\t" + ula.toString() + "\n\tSize:" + ula.size());
//	log(ula.debugString());
//	
//	
//	
//	
//}
//
//public static void mainx(String[] args) {
//	log("UnsafeDoubleArray Test");
//	Random r = new Random(System.currentTimeMillis());
//	int LONG_COUNT = 1000000;
//	int WARM_COUNT = 1000;
//	int WARMUP_LOOPS = 15002;
//	long[] TEST_DATA = new long[LONG_COUNT];
//	for(int i = 0; i < LONG_COUNT; i++) {
//		TEST_DATA[i] = r.nextLong();
//	}
//	UnsafeDoubleArray ula = new UnsafeDoubleArray(TEST_DATA);
//	long[] readOut = ula.getArray();
//	long[] testData = new long[TEST_DATA.length];
//	System.arraycopy(TEST_DATA, 0, testData, 0, TEST_DATA.length);
//	Arrays.sort(testData);
//	log("Equal:" + Arrays.equals(testData, readOut));
//	for(int i = 0; i < LONG_COUNT; i++) {
//		assert testData[i] == ula.a(i);
//	}
//	ula.destroy();
//	
//	//LONG_COUNT = 3000000;
//	//LONG_COUNT = 250000;
//	
//	log("Testing native long array sort");
//	testData = new long[WARM_COUNT];
//	for(int i = 0; i < WARMUP_LOOPS; i++) {
//		System.arraycopy(TEST_DATA, 0, testData, 0, testData.length);    	
//		Arrays.sort(testData);
//	}
//	log("Warmup Complete");
//	testData = new long[TEST_DATA.length];
//	System.arraycopy(TEST_DATA, 0, testData, 0, TEST_DATA.length);
//	SystemClock.startTimer();
//	Arrays.sort(testData);
//	ElapsedTime et = SystemClock.endTimer();
//	log("long array sorted:" + et + "\n\tAverage Per:" + et.avgNs(LONG_COUNT));
//	
//	
//
//	log("Testing UnsafeDoubleArray sort");
//	testData = new long[WARM_COUNT];
//	System.arraycopy(TEST_DATA, 0, testData, 0, testData.length);
//	for(int i = 0; i < WARMUP_LOOPS; i++) {
//		ula = new UnsafeDoubleArray(testData);    	
//		ula.destroy();
//	}
//	log("Warmup Complete");
//	SystemClock.startTimer();
//	ula = new UnsafeDoubleArray(TEST_DATA);
//	et = SystemClock.endTimer();
//	ula.destroy();
//	log("UnsafeDoubleArray sorted:" + et + "\n\tAverage Per:" + et.avgNs(LONG_COUNT));
//	
//	log("Testing native long array search");
//	testData = new long[WARM_COUNT];
//	System.arraycopy(TEST_DATA, 0, testData, 0, testData.length);    	
//	Arrays.sort(testData);
//	for(int i = 0; i < WARM_COUNT; i++) {
//		assert Arrays.binarySearch(testData, testData[i])==i;
//	}
//	log("Warmup Complete");
//	testData = new long[TEST_DATA.length];
//	System.arraycopy(TEST_DATA, 0, testData, 0, TEST_DATA.length);
//	Arrays.sort(testData);
//	SystemClock.startTimer();
//	for(int i = 0; i < LONG_COUNT; i++) {
//		assert Arrays.binarySearch(testData, testData[i])==i;
//		assert Arrays.binarySearch(testData, testData[i]*31)!=i;
//	}    	
//	et = SystemClock.endTimer();
//	log("long array search:" + et + "\n\tAverage Per:" + et.avgNs(LONG_COUNT));
//	
//	log("Testing UnsafeDoubleArray search");
//	testData = new long[WARM_COUNT];
//	System.arraycopy(TEST_DATA, 0, testData, 0, testData.length);
//	ula = new UnsafeDoubleArray(testData);
//	Arrays.sort(testData);
//	for(int i = 0; i < WARM_COUNT; i++) {
//		assert testData[i] == ula.a(i);
//		assert ula.binarySearch(testData[i])==i;
//		assert ula.binarySearch(testData[i]*31)!=i;
//	}
//	ula.destroy();
//	log("Warmup Complete");
//	ula = new UnsafeDoubleArray(TEST_DATA);
//	testData = ula.getArray();
//	SystemClock.startTimer();
//	
//	for(int i = 0; i < LONG_COUNT; i++) {
//		assert ula.binarySearch(testData[i])==i;
//		assert ula.binarySearch(testData[i]*31)!=i;
//	}
//	
//	et = SystemClock.endTimer();
//	ula.destroy();
//	log("UnsafeDoubleArray search:" + et + "\n\tAverage Per:" + et.avgNs(LONG_COUNT));
//	
//	final int HEAP_TEST_SIZE = 20;
//	
//	log("Testing long array heap size");
//	long[][] arrays = new long[HEAP_TEST_SIZE][];
//	ResourceHelper.memoryUsage(true);
//	long heapBefore = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
//	for(int i = 0; i < HEAP_TEST_SIZE; i++) {
//		arrays[i] = new long[LONG_COUNT];
//		System.arraycopy(TEST_DATA, 0, arrays[i], 0, TEST_DATA.length);    	
//	}
//	long heapAfter = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
//	long heapDiff = heapAfter-heapBefore;
//	log("Long Array Heap:" + heapDiff);
//	arrays = null;
//	ResourceHelper.memoryUsage(true);
//	long heapAfterRelease = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
//	heapDiff = heapAfter-heapAfterRelease;
//	log("Long Array Heap Released:" + heapDiff);
//	
//	log("Testing UnsafeDoubleArray heap size");
//	UnsafeDoubleArray[] ulas = new UnsafeDoubleArray[HEAP_TEST_SIZE];
//	ResourceHelper.memoryUsage(true);
//	heapBefore = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
//	for(int i = 0; i < HEAP_TEST_SIZE; i++) {
//		ulas[i] = new UnsafeDoubleArray(TEST_DATA);    	
//	}
//	heapAfter = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
//	heapDiff = heapAfter-heapBefore;
//	log("UnsafeDoubleArray Heap:" + heapDiff);
//	for(int i = 0; i < HEAP_TEST_SIZE; i++) {
//		ulas[i].destroy();
//	}
//	ulas = null;
//	ResourceHelper.memoryUsage(true);
//	heapAfterRelease = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
//	heapDiff = heapAfter-heapAfterRelease;
//	log("UnsafeDoubleArray Heap Released:" + heapDiff);
//	
//	
//}
//
