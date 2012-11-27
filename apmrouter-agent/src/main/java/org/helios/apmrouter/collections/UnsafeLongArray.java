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
 * <p>Title: UnsafeLongArray</p>
 * <p>Description: Utility class for storing long arrays in direct memory with self resizing</p> 
 * <p><b><font color='red'>!!  NOTE !!&nbsp;&nbsp;</font>:&nbsp;&nbsp;</b>This class is disastrously THREAD UNSAFE. Only use with one thread at a time, or used one
 * of the concurrent/synchronized versions</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.collections.UnsafeLongArray</code></p>
 */
public class UnsafeLongArray extends UnsafeArray {
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
    /** The memory offset for a long array */
    public static final long LONG_ARRAY_OFFSET = unsafe.arrayBaseOffset(long[].class);
    /** The memory offset for a double array */
    public static final long DOUBLE_ARRAY_OFFSET = unsafe.arrayBaseOffset(double[].class);
    
    
    public static void main(String[] args) {
    	
    	log("Long ArrOff:" +  unsafe.arrayBaseOffset(Long[].class));
    	log("Long IScale:" +  unsafe.arrayIndexScale(Long[].class));
    	log("long ArrOff:" +  unsafe.arrayBaseOffset(long[].class));
    	log("long IScale:" +  unsafe.arrayIndexScale(long[].class));
    	log("==========================");
    	log("Double ArrOff:" +  unsafe.arrayBaseOffset(Double[].class));
    	log("Double IScale:" +  unsafe.arrayIndexScale(Double[].class));
    	log("double ArrOff:" +  unsafe.arrayBaseOffset(double[].class));
    	log("double IScale:" +  unsafe.arrayIndexScale(double[].class));
    	
    	try {
	    	UnsafeLongArray ula = UnsafeArrayBuilder.newBuilder().sorted(true).initialCapacity(5).fixed(true).buildLongArray();
	    	for(int i = 0; i < 5; i++) { ula.insert(i); }
	    	log("ULA:" + ula);
	    	log("ULA Arr:" + Arrays.toString(ula.getArray()));
	    	byte[] arr = ula.getBytes();
	    	log("Arr Length:" + arr.length);
//	    	byte[] arr2 = new byte[arr.length];
//	    	for(int i = 0; i < arr.length; i++) {
//	    		arr2[arr.length-(1+i)] = arr[i];
//	    	}
	    	UnsafeLongArray ula2 = UnsafeArrayBuilder.newBuilder().sorted(true).initialCapacity(5).fixed(true).buildLongArray();
	    	ula2.initAndLoad(arr);
	    	log("ULA2:" + ula2);
	    	log("ULA2 DOUBLE:" + Arrays.toString(ula2.asDoubleArray()));
    	} catch (Exception ex) {
    		ex.printStackTrace(System.err);
    	}
    	
    }
    
    
    
	/**
	 * Creates a new UnsafeLongArray
	 * @param initialCapacity The initial allocated capacity
	 * @param sorted Indicates the array will be maintained in sorted order
	 * @param fixed Indicates the capacity of the array will be fixed
	 * @param maxCapacity The maximum capacity of the array
	 * @param minCapacity The minimum capacity of the array
	 * @param allocationIncrement The number of slots that will be allocated when the array needs to be extended
	 * @param clearedSlotsFree The number of excess slots that are emptied by rollLefts before the array capacity is shrunk
	 */
	private UnsafeLongArray(int initialCapacity, boolean sorted, boolean fixed, int maxCapacity,
			int minCapacity, int allocationIncrement, int clearedSlotsFree) {
		super(initialCapacity, sorted, fixed, fixed ? initialCapacity : maxCapacity, minCapacity, allocationIncrement, clearedSlotsFree);
	}
	
	
	
	/**
	 * Creates a new UnsafeLongArray. Used for cloning.
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
	private UnsafeLongArray(int size, int capacity, long address, boolean sorted,
			boolean fixed, int maxCapacity, int minCapacity,
			int allocationIncrement, int clearedSlotsFree) {
		super(size, capacity, address, sorted, fixed, maxCapacity, minCapacity,
				allocationIncrement, clearedSlotsFree);
	}
	
	/**
	 * Creates a new fixed capacity and unsorted UnsafeLongArray with initial, min and max capacity set to the passed size.
	 * For internal use.
	 * @param size the initial capacity
	 */
	private UnsafeLongArray(int size) {
		this(size, false, true, size, size, 0, 0);
	}
	

	/**
	 * Creates a new UnsafeLongArray from the passed builder
	 * @param builder The builder to configure the new UnsafeLongArray
	 * @param data The optional data load load 
	 * @return the new UnsafeLongArray
	 */
	static UnsafeLongArray build(UnsafeArrayBuilder builder, Object data) {		
		
		UnsafeLongArray ula = new UnsafeLongArray(builder.initialCapacity(), builder.sorted(), builder.fixed(), builder.maxCapacity(), builder.minCapacity(), builder.allocationIncrement(), builder.clearedSlotsFree());
		if(data!=null) {
			if(data instanceof long[]) {
				ula.load((long[])data);
			} else if(data instanceof UnsafeLongArray) {
				ula.load((UnsafeLongArray)data);
			}
		}
		return ula;
	}
	
	/**
	 * Creates a new UnsafeLongArray from the passed builder
	 * @param builder The builder to configure the new UnsafeLongArray
	 * @return the new UnsafeLongArray
	 */
	static UnsafeLongArray build(UnsafeArrayBuilder builder) {
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
	
	public static long[] convert(byte[] arr) {
		int len = arr.length;
		if(len%8!=0) throw new RuntimeException("Mod check failed", new Throwable());
		int arrsize = len/8;
		long[] larr = new long[arrsize];
		UnsafeAdapter.copyMemory(arr, BYTE_ARRAY_OFFSET, larr, LONG_ARRAY_OFFSET, arr.length);
		
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
	protected void load(long[] arr) {		
		if(arr.length<1) return;
		if(arr.length>maxCapacity) throw new ArrayOverflowException("Passed array of length [" + arr.length + "] is too large for this UnsafeLongArray with a max capacity of [" + maxCapacity + "]", new Throwable());
		freeMemory(address);
		address = allocateMemory(arr.length << 3);
		UnsafeAdapter.copyMemory(arr, LONG_ARRAY_OFFSET, null, address, arr.length << 3);
		size = capacity = arr.length;
		if(sorted) sort();
	}
	
	/**
	 * Loads this array from another UnsafeLongArray
	 * @param ula The UnsafeLongArray to copy
	 */
	private void load(UnsafeLongArray ula) {				
		if(ula.size>maxCapacity) throw new ArrayOverflowException("Passed UnsafeLongArray of size [" + ula.size + "] is too large for this UnsafeLongArray with a max capacity of [" + maxCapacity + "]", new Throwable());
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
    public UnsafeLongArray rollRight(int index, long newValue) {
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
    public Long rollRightCap(int index, long newValue) {
    	if(sorted && normalizedBinarySearch(newValue)!=index) throw new RuntimeException("The index [" + index + "] is incorrect for the value [" + newValue + "] for this sorted array", new Throwable());
    	final Long dropped = rollRightCap(index);
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
    protected Long rollRightCap(int index) {
    	_check(); _checkc(index);
    	final int numberOfSlotsToMove;
    	final boolean incrSize;
    	final Long dropped;
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
     * @param v The long value to insert
     * @return the index to insert into
     */
    public int normalizedBinarySearch(long v) {
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
     * Appends the passed long values to this array.
     * If this array is sorted, this operation will trigger a sort once the append is complete
     * @param values the values to add
     * @return this array
     * TODO: If the size is fixed, make space for the appended values by dropping on the left
     */
    public UnsafeLongArray append(long...values) {
    	_check();    	
    	if(values!=null && values.length>0) {
    		int vl = values.length;
    		int newSize = vl + size;
    		if(newSize > maxCapacity) throw new ArrayOverflowException("Passed array of length [" + vl + "] is too large for this UnsafeLongArray with a max capacity of [" + maxCapacity + "]", new Throwable());
    		while(newSize > capacity) {
    			extend(false, vl);
    		}
    		UnsafeAdapter.copyMemory(values, LONG_ARRAY_OFFSET, null, address + (size << 3), vl << 3);    		
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
     * Appends as many of the passed long values to this array as will fit up to the max capacity, discarding the remaining values.
     * If this array is sorted, this operation will trigger a sort once the append is complete
     * @param values the values to add
     * @return the number of dicarded values that were dropped
     * TODO: If the size is fixed, make space for the appended values by dropping on the left
     */
    public int appendWhatFits(long...values) {
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
    		UnsafeAdapter.copyMemory(values, LONG_ARRAY_OFFSET, null, address + (size << 3), howManyWillFit << 3);    		
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
    public UnsafeLongArray insert(long...values) {
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
    public int insertIfNotExists(long...values) {
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
    private void _insert(long v) {
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
    public int remove(long...values) {
    	_check();
    	int removed = 0;
    	if(values!=null && values.length>0) {
    		for(long v: values) {
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
    public int removeAll(long...values) {
    	_check();
    	int _size = size;
    	if(values!=null && values.length>0) {
    		for(long v: values) {
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
     * Returns the long at the specified index
     * @param index The index of the long to retrieve
     * @return the specified long
     */
    private long a(int index) {
    	return unsafe.getLong(this.address + (index << 3));    	
    }
    
    /**
     * Sets the long value at the specified index
     * @param index The index of the array to set the long at
     * @param value The long to set
     */
    private void a(int index, long value) {
    	unsafe.putLong(this.address + (index << 3), value);
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
    public UnsafeLongArray set(int index, long value) {
    	_check(); _check(index);
    	unsafe.putLong(this.address + (index << 3), value);
    	return this;
    }
    

    
    
    
    /**
     * Returns a traditional long array representing the longs in this array
     * @return a long array with the same values as this array
     */
    public long[] getArray() {
    	_check();
    	long[] arr = new long[size];
    	if(size>0) UnsafeAdapter.copyMemory(null, address, arr, LONG_ARRAY_OFFSET, size << 3);
    	return arr;
    }
    
    /**
     * Returns a traditional long array representing the all the allocated slots this array
     * @return a long array representing the all the allocated slots this array
     * TODO: This should be deprecated unless there is any testing use for it.
     */
    public long[] getAllocatedArray() {
    	_check();
    	long[] arr = new long[capacity];
    	UnsafeAdapter.copyMemory(null, address, arr, LONG_ARRAY_OFFSET, capacity << 3);
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
	public UnsafeLongArray clone() {
    	_check();
    	return new UnsafeLongArray(size, capacity, address, sorted, fixed, maxCapacity, minCapacity, allocationIncrement, clearedSlotsFree);
    }
    

    
    
    /**
     * Searches this unsafe long array for the specified value using the binary search algorithm based on {@link java.util.Arrays#binarySearch(long[], long)}. 
     * If this unsafe long array contains multiple elements with the specified value, there is no guarantee which one will be found. 
     * @param key the value to be searched for 
     * @return index of the search key, if it is contained in this array; otherwise, <b><code>(-(insertion point) - 1)</code></b>.
     */
    public int binarySearch(long key) {
    	int low = 0;
    	int high = size - 1;

    	while (low <= high) {
    		int mid = (low + high) >>> 1;
    		long midVal = a(mid);

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
    public UnsafeLongArray sort() {
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
            h = (31*h + a(off++) + (hashPrime*h));
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
        	long element = a(i);
            int elementHash = (int)(element ^ (element >>> 32));
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
		UnsafeLongArray other = (UnsafeLongArray) obj;
		if (address != other.address) {
			return false;
		}
		return true;
    	
    }
	

	
    
	
    
    /**
     * Sorts the passed UnsafeLongArray.
     * @param ula The UnsafeLongArray to sort
     */
    public static void sort(UnsafeLongArray ula) {
    	int left = 0;
    	int right = ula.size-1;
        // Use Quicksort on small arrays
        if (right - left < QUICKSORT_THRESHOLD) {
            ula.sort(left, right, true);
            return;
        }

        /*
         * Index run[i] is the start of i-th run
         * (ascending or descending sequence).
         */
        int[] run = new int[MAX_RUN_COUNT + 1];
        int count = 0; run[0] = left;

        // Check if the array is nearly sorted
        for (int k = left; k < right; run[count] = k) {
            if (ula.a(k) < ula.a(k + 1)) { // ascending
                while (++k <= right && ula.a(k - 1) <= ula.a(k));
            } else if (ula.a(k) > ula.a(k + 1)) { // descending
                while (++k <= right && ula.a(k - 1) >= ula.a(k));
                for (int lo = run[count] - 1, hi = k; ++lo < --hi; ) {
                    long t = ula.a(lo); ula.a(lo, ula.a(hi)); ula.a(hi, t);
                }
            } else { // equal
                for (int m = MAX_RUN_LENGTH; ++k <= right && ula.a(k - 1) == ula.a(k); ) {
                    if (--m == 0) {
                        ula.sort(left, right, true);
                        return;
                    }
                }
            }

            /*
             * The array is not highly structured,
             * use Quicksort instead of merge sort.
             */
            if (++count == MAX_RUN_COUNT) {
                ula.sort(left, right, true);
                return;
            }
        }

        // Check special cases
        if (run[count] == right++) { // The last run contains one element
            run[++count] = right;
        } else if (count == 1) { // The array is already sorted
            return;
        }

        

        
        /*
         * Create temporary array, which is used for merging.
         * Implementation note: variable "right" is increased by 1.
         */
        UnsafeLongArray b; byte odd = 0;
        for (int n = 1; (n <<= 1) < count; odd ^= 1);

        if (odd == 0) {
            b = ula; ula = new UnsafeLongArray(b.size());
            for (int i = left - 1; ++i < right; ula.a(i, b.a(i)));
        } else {
            b = new UnsafeLongArray(ula.size);
        }

        // Merging
        for (int last; count > 1; count = last) {
            for (int k = (last = 0) + 2; k <= count; k += 2) {
                int hi = run[k], mi = run[k - 1];
                for (int i = run[k - 2], p = i, q = mi; i < hi; ++i) {
                    if (q >= hi || p < mi && ula.a(p) <= ula.a(q)) {
                        b.a(i, ula.a(p++));
                    } else {
                        b.a(i, ula.a(q++));
                    }
                }
                run[++last] = hi;
            }
            if ((count & 1) != 0) {
                for (int i = right, lo = run[count - 1]; --i >= lo;
                		b.a(i, ula.a(i))
                );
                run[++last] = right;
            }
            UnsafeLongArray t = ula; ula = b; b = t;
        }
        
    }
    

	/**
     * Sorts the specified range of the array by Dual-Pivot Quicksort.
     *
     * @param a the array to be sorted
     * @param left the index of the first element, inclusive, to be sorted
     * @param right the index of the last element, inclusive, to be sorted
     * @param leftmost indicates if this part is the leftmost in the range
     */
    private void sort(int left, int right, boolean leftmost) {
        int length = right - left + 1;

        // Use insertion sort on tiny arrays
        if (length < INSERTION_SORT_THRESHOLD) {
            if (leftmost) {
                /*
                 * Traditional (without sentinel) insertion sort,
                 * optimized for server VM, is used in case of
                 * the leftmost part.
                 */
                for (int i = left, j = i; i < right; j = ++i) {
                    long ai = a(i + 1);
                    while (ai < a(j)) {
                        a(j + 1, a(j));
                        if (j-- == left) {
                            break;
                        }
                    }
                    a(j + 1, ai);
                }
            } else {
                /*
                 * Skip the longest ascending sequence.
                 */
                do {
                    if (left >= right) {
                        return;
                    }
                } while (a(++left) >= a(left - 1));

                /*
                 * Every element from adjoining part plays the role
                 * of sentinel, therefore this allows us to avoid the
                 * left range check on each iteration. Moreover, we use
                 * the more optimized algorithm, so called pair insertion
                 * sort, which is faster (in the context of Quicksort)
                 * than traditional implementation of insertion sort.
                 */
                for (int k = left; ++left <= right; k = ++left) {
                    long a1 = a(k), a2 = a(left);

                    if (a1 < a2) {
                        a2 = a1; a1 = a(left);
                    }
                    while (a1 < a(--k)) {
                        a(k + 2, a(k));
                    }
                    a(++k + 1, a1);

                    while (a2 < a(--k)) {
                        a(k + 1, a(k));
                    }
                    a(k + 1, a2);
                }
                long last = a(right);

                while (last < a(--right)) {
                    a(right + 1, a(right));
                }
                a(right + 1, last);
            }
            return;
        }

        // Inexpensive approximation of length / 7
        int seventh = (length >> 3) + (length >> 6) + 1;

        /*
         * Sort five evenly spaced elements around (and including) the
         * center element in the range. These elements will be used for
         * pivot selection as described below. The choice for spacing
         * these elements was empirically determined to work well on
         * a wide variety of inputs.
         */
        int e3 = (left + right) >>> 1; // The midpoint
        int e2 = e3 - seventh;
        int e1 = e2 - seventh;
        int e4 = e3 + seventh;
        int e5 = e4 + seventh;

        // Sort these elements using insertion sort
        if (a(e2) < a(e1)) { long t = a(e2); a(e2, a(e1)); a(e1,t); }

        if (a(e3) < a(e2)) { long t = a(e3); a(e3, a(e2)); a(e2, t);
            if (t < a(e1)) { a(e2, a(e1)); a(e1, t); }
        }
        if (a(e4) < a(e3)) { long t = a(e4); a(e4, a(e3)); a(e3, t);
            if (t < a(e2)) { a(e3, a(e2)); a(e2, t);
                if (t < a(e1)) { a(e2, a(e1)); a(e1, t); }
            }
        }
        if (a(e5) < a(e4)) { long t = a(e5); a(e5, a(e4)); a(e4, t);
            if (t < a(e3)) { a(e4, a(e3)); a(e3, t);
                if (t < a(e2)) { a(e3, a(e2)); a(e2, t);
                    if (t < a(e1)) { a(e2, a(e1)); a(e1, t); }
                }
            }
        }

        // Pointers
        int less  = left;  // The index of the first element of center part
        int great = right; // The index before the first element of right part

        if (a(e1) != a(e2) && a(e2) != a(e3) && a(e3) != a(e4) && a(e4) != a(e5)) {
            /*
             * Use the second and fourth of the five sorted elements as pivots.
             * These values are inexpensive approximations of the first and
             * second terciles of the array. Note that pivot1 <= pivot2.
             */
            long pivot1 = a(e2);
            long pivot2 = a(e4);

            /*
             * The first and the last elements to be sorted are moved to the
             * locations formerly occupied by the pivots. When partitioning
             * is complete, the pivots are swapped back into their final
             * positions, and excluded from subsequent sorting.
             */
            a(e2, a(left));
            a(e4, a(right));

            /*
             * Skip elements, which are less or greater than pivot values.
             */
            while (a(++less) < pivot1);
            while (a(--great) > pivot2);

            /*
             * Partitioning:
             *
             *   left part           center part                   right part
             * +--------------------------------------------------------------+
             * |  < pivot1  |  pivot1 <= && <= pivot2  |    ?    |  > pivot2  |
             * +--------------------------------------------------------------+
             *               ^                          ^       ^
             *               |                          |       |
             *              less                        k     great
             *
             * Invariants:
             *
             *              all in (left, less)   < pivot1
             *    pivot1 <= all in [less, k)     <= pivot2
             *              all in (great, right) > pivot2
             *
             * Pointer k is the first index of ?-part.
             */
            outer:
            for (int k = less - 1; ++k <= great; ) {
                long ak = a(k);
                if (ak < pivot1) { // Move a(k) to left part
                    a(k, a(less));
                    /*
                     * Here and below we use "a[i] = b; i++;" instead
                     * of "a[i++] = b;" due to performance issue.
                     */
                    a(less, ak);
                    ++less;
                } else if (ak > pivot2) { // Move a(k) to right part
                    while (a(great) > pivot2) {
                        if (great-- == k) {
                            break outer;
                        }
                    }
                    if (a(great) < pivot1) { // a[great] <= pivot2
                        a(k,a(less));
                        a(less,a(great));
                        ++less;
                    } else { // pivot1 <= a[great] <= pivot2
                        a(k,a(great));
                    }
                    /*
                     * Here and below we use "a[i] = b; i--;" instead
                     * of "a[i--] = b;" due to performance issue.
                     */
                    a(great, ak);
                    --great;
                }
            }

            // Swap pivots into their final positions
            a(left, a(less  - 1)); a(less  - 1, pivot1);
            a(right, a(great + 1)); a(great + 1, pivot2);

            // Sort left and right parts recursively, excluding known pivots
            sort(left, less - 2, leftmost);
            sort(great + 2, right, false);

            /*
             * If center part is too large (comprises > 4/7 of the array),
             * swap internal pivot values to ends.
             */
            if (less < e1 && e5 < great) {
                /*
                 * Skip elements, which are equal to pivot values.
                 */
                while (a(less) == pivot1) {
                    ++less;
                }

                while (a(great) == pivot2) {
                    --great;
                }

                /*
                 * Partitioning:
                 *
                 *   left part         center part                  right part
                 * +----------------------------------------------------------+
                 * | == pivot1 |  pivot1 < && < pivot2  |    ?    | == pivot2 |
                 * +----------------------------------------------------------+
                 *              ^                        ^       ^
                 *              |                        |       |
                 *             less                      k     great
                 *
                 * Invariants:
                 *
                 *              all in (*,  less) == pivot1
                 *     pivot1 < all in [less,  k)  < pivot2
                 *              all in (great, *) == pivot2
                 *
                 * Pointer k is the first index of ?-part.
                 */
                outer:
                for (int k = less - 1; ++k <= great; ) {
                    long ak = a(k);
                    if (ak == pivot1) { // Move a[k] to left part
                        a(k, a(less));
                        a(less, ak);
                        ++less;
                    } else if (ak == pivot2) { // Move a[k] to right part
                        while (a(great) == pivot2) {
                            if (great-- == k) {
                                break outer;
                            }
                        }
                        if (a(great) == pivot1) { // a[great] < pivot2
                            a(k, a(less));
                            /*
                             * Even though a[great] equals to pivot1, the
                             * assignment a[less] = pivot1 may be incorrect,
                             * if a[great] and pivot1 are floating-point zeros
                             * of different signs. Therefore in float and
                             * double sorting methods we have to use more
                             * accurate assignment a[less] = a[great].
                             */
                            a(less, pivot1);
                            ++less;
                        } else { // pivot1 < a[great] < pivot2
                            a(k, a(great));
                        }
                        a(great, ak);
                        --great;
                    }
                }
            }

            // Sort center part recursively
            sort(less, great, false);

        } else { // Partitioning with one pivot
            /*
             * Use the third of the five sorted elements as pivot.
             * This value is inexpensive approximation of the median.
             */
            long pivot = a(e3);

            /*
             * Partitioning degenerates to the traditional 3-way
             * (or "Dutch National Flag") schema:
             *
             *   left part    center part              right part
             * +-------------------------------------------------+
             * |  < pivot  |   == pivot   |     ?    |  > pivot  |
             * +-------------------------------------------------+
             *              ^              ^        ^
             *              |              |        |
             *             less            k      great
             *
             * Invariants:
             *
             *   all in (left, less)   < pivot
             *   all in [less, k)     == pivot
             *   all in (great, right) > pivot
             *
             * Pointer k is the first index of ?-part.
             */
            for (int k = less; k <= great; ++k) {
                if (a(k) == pivot) {
                    continue;
                }
                long ak = a(k);
                if (ak < pivot) { // Move a[k] to left part
                    a(k, a(less));
                    a(less, ak);
                    ++less;
                } else { // a[k] > pivot - Move a[k] to right part
                    while (a(great) > pivot) {
                        --great;
                    }
                    if (a(great) < pivot) { // a[great] <= pivot
                        a(k, a(less));
                        a(less, a(great));
                        ++less;
                    } else { // a[great] == pivot
                        /*
                         * Even though a[great] equals to pivot, the
                         * assignment a[k] = pivot may be incorrect,
                         * if a[great] and pivot are floating-point
                         * zeros of different signs. Therefore in float
                         * and double sorting methods we have to use
                         * more accurate assignment a[k] = a[great].
                         */
                        a(k, pivot);
                    }
                    a(great, ak);
                    --great;
                }
            }

            /*
             * Sort left and right parts recursively.
             * All elements from center part are equal
             * and, therefore, already sorted.
             */
            sort(left, less - 1, leftmost);
            sort(great + 1, right, false);
        }
    }
    
}


//
//public static void mainy(String[] args) {
//	final long[] TEST_DATA = new long[]{0L, 1L, 2L};
//	final long REMOVE = 1L;
//	log("Add longs to UnsafeLongArray Test");
//	UnsafeLongArray ula = new UnsafeLongArray(3, Long.MAX_VALUE);
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
//	log("UnsafeLongArray Test");
//	Random r = new Random(System.currentTimeMillis());
//	int LONG_COUNT = 1000000;
//	int WARM_COUNT = 1000;
//	int WARMUP_LOOPS = 15002;
//	long[] TEST_DATA = new long[LONG_COUNT];
//	for(int i = 0; i < LONG_COUNT; i++) {
//		TEST_DATA[i] = r.nextLong();
//	}
//	UnsafeLongArray ula = new UnsafeLongArray(TEST_DATA);
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
//	log("Testing UnsafeLongArray sort");
//	testData = new long[WARM_COUNT];
//	System.arraycopy(TEST_DATA, 0, testData, 0, testData.length);
//	for(int i = 0; i < WARMUP_LOOPS; i++) {
//		ula = new UnsafeLongArray(testData);    	
//		ula.destroy();
//	}
//	log("Warmup Complete");
//	SystemClock.startTimer();
//	ula = new UnsafeLongArray(TEST_DATA);
//	et = SystemClock.endTimer();
//	ula.destroy();
//	log("UnsafeLongArray sorted:" + et + "\n\tAverage Per:" + et.avgNs(LONG_COUNT));
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
//	log("Testing UnsafeLongArray search");
//	testData = new long[WARM_COUNT];
//	System.arraycopy(TEST_DATA, 0, testData, 0, testData.length);
//	ula = new UnsafeLongArray(testData);
//	Arrays.sort(testData);
//	for(int i = 0; i < WARM_COUNT; i++) {
//		assert testData[i] == ula.a(i);
//		assert ula.binarySearch(testData[i])==i;
//		assert ula.binarySearch(testData[i]*31)!=i;
//	}
//	ula.destroy();
//	log("Warmup Complete");
//	ula = new UnsafeLongArray(TEST_DATA);
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
//	log("UnsafeLongArray search:" + et + "\n\tAverage Per:" + et.avgNs(LONG_COUNT));
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
//	log("Testing UnsafeLongArray heap size");
//	UnsafeLongArray[] ulas = new UnsafeLongArray[HEAP_TEST_SIZE];
//	ResourceHelper.memoryUsage(true);
//	heapBefore = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
//	for(int i = 0; i < HEAP_TEST_SIZE; i++) {
//		ulas[i] = new UnsafeLongArray(TEST_DATA);    	
//	}
//	heapAfter = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
//	heapDiff = heapAfter-heapBefore;
//	log("UnsafeLongArray Heap:" + heapDiff);
//	for(int i = 0; i < HEAP_TEST_SIZE; i++) {
//		ulas[i].destroy();
//	}
//	ulas = null;
//	ResourceHelper.memoryUsage(true);
//	heapAfterRelease = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
//	heapDiff = heapAfter-heapAfterRelease;
//	log("UnsafeLongArray Heap Released:" + heapDiff);
//	
//	
//}
//
