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

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Random;

import org.helios.apmrouter.util.ResourceHelper;
import org.helios.apmrouter.util.SystemClock;
import org.helios.apmrouter.util.SystemClock.ElapsedTime;

import sun.misc.Unsafe;

/**
 * <p>Title: UnsafeLongArray</p>
 * <p>Description: Utility class for storing long arrays in direct memory with self resizing</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.collections.UnsafeLongArray</code></p>
 */
public class UnsafeLongArray {
    /**
     * The maximum number of runs in merge sort.
     */
    private static final int MAX_RUN_COUNT = 67;

    /**
     * The maximum length of run in merge sort.
     */
    private static final int MAX_RUN_LENGTH = 33;

    /**
     * If the length of an array to be sorted is less than this
     * constant, Quicksort is used in preference to merge sort.
     */
    private static final int QUICKSORT_THRESHOLD = 286;

    /**
     * If the length of an array to be sorted is less than this
     * constant, insertion sort is used in preference to Quicksort.
     */
    private static final int INSERTION_SORT_THRESHOLD = 47;
    
    /** The default allocation size for UnsafeLongArrays */
    public static final int DEFAULT_ALLOC = 128;

    /** The unsafe instance */
    private static final Unsafe unsafe;
    
    static
    {
        try
        {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe)field.get(null);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
    
    /** The capacity of the array, i.e. the total number of allocated slots */
    private int capacity;
    /** The size of the array as the number of occupied slots */
    private int size;
    /** The alllocation size that specifies the initial size of the array and how much to grow the array by when it needs to grow */
    private final int allocation;
    
    /** The memory address of the 0th slot in the array */
    private long address;

    public static void log(Object msg) {
    	System.out.println(msg);
    }

    public static void main(String[] args) {
    	final long[] TEST_DATA = new long[]{0L, 1L, 2L};
    	final long REMOVE = 1L;
    	log("Add longs to UnsafeLongArray Test");
    	UnsafeLongArray ula = new UnsafeLongArray(3, Long.MAX_VALUE);
    	log("Empty:\n\t" + ula.toFullString() + "\n\t" + ula.toString() + "\n\tSize:" + ula.size());
    	ula.insert(TEST_DATA);
    	log("Full At 3:\n\t" + ula.toFullString() + "\n\t" + ula.toString() + "\n\tSize:" + ula.size());
    	ula.insert(TEST_DATA);
    	log("Full At 6:\n\t" + ula.toFullString() + "\n\t" + ula.toString() + "\n\tSize:" + ula.size());
    	int removed = ula.removeAll(REMOVE);
    	log( "" + removed + " removed:\n\t" + ula.toFullString() + "\n\t" + ula.toString() + "\n\tSize:" + ula.size());
    	log("BinSearch for :" + REMOVE + ":" + ula.binarySearch(REMOVE));
    	log(ula.debugString());
    	ula.insertIfNotExists(REMOVE);
    	log("" + REMOVE + " back in once:\n\t" + ula.toFullString() + "\n\t" + ula.toString() + "\n\tSize:" + ula.size());
    	log(ula.debugString());
    	
    	
    	
    	
    }
    
    public static void mainx(String[] args) {
    	log("UnsafeLongArray Test");
    	Random r = new Random(System.currentTimeMillis());
    	int LONG_COUNT = 1000000;
    	int WARM_COUNT = 1000;
    	int WARMUP_LOOPS = 15002;
    	long[] TEST_DATA = new long[LONG_COUNT];
    	for(int i = 0; i < LONG_COUNT; i++) {
    		TEST_DATA[i] = r.nextLong();
    	}
    	UnsafeLongArray ula = new UnsafeLongArray(TEST_DATA);
    	long[] readOut = ula.getArray();
    	long[] testData = new long[TEST_DATA.length];
    	System.arraycopy(TEST_DATA, 0, testData, 0, TEST_DATA.length);
    	Arrays.sort(testData);
    	log("Equal:" + Arrays.equals(testData, readOut));
    	for(int i = 0; i < LONG_COUNT; i++) {
    		assert testData[i] == ula.a(i);
    	}
    	ula.destroy();
    	
    	//LONG_COUNT = 3000000;
    	//LONG_COUNT = 250000;
    	
    	log("Testing native long array sort");
    	testData = new long[WARM_COUNT];
    	for(int i = 0; i < WARMUP_LOOPS; i++) {
    		System.arraycopy(TEST_DATA, 0, testData, 0, testData.length);    	
    		Arrays.sort(testData);
    	}
    	log("Warmup Complete");
    	testData = new long[TEST_DATA.length];
    	System.arraycopy(TEST_DATA, 0, testData, 0, TEST_DATA.length);
    	SystemClock.startTimer();
    	Arrays.sort(testData);
    	ElapsedTime et = SystemClock.endTimer();
    	log("long array sorted:" + et + "\n\tAverage Per:" + et.avgNs(LONG_COUNT));
    	
    	

    	log("Testing UnsafeLongArray sort");
    	testData = new long[WARM_COUNT];
    	System.arraycopy(TEST_DATA, 0, testData, 0, testData.length);
    	for(int i = 0; i < WARMUP_LOOPS; i++) {
    		ula = new UnsafeLongArray(testData);    	
    		ula.destroy();
    	}
    	log("Warmup Complete");
    	SystemClock.startTimer();
    	ula = new UnsafeLongArray(TEST_DATA);
    	et = SystemClock.endTimer();
    	ula.destroy();
    	log("UnsafeLongArray sorted:" + et + "\n\tAverage Per:" + et.avgNs(LONG_COUNT));
    	
    	log("Testing native long array search");
    	testData = new long[WARM_COUNT];
		System.arraycopy(TEST_DATA, 0, testData, 0, testData.length);    	
		Arrays.sort(testData);
    	for(int i = 0; i < WARM_COUNT; i++) {
    		assert Arrays.binarySearch(testData, testData[i])==i;
    	}
    	log("Warmup Complete");
    	testData = new long[TEST_DATA.length];
    	System.arraycopy(TEST_DATA, 0, testData, 0, TEST_DATA.length);
    	Arrays.sort(testData);
    	SystemClock.startTimer();
    	for(int i = 0; i < LONG_COUNT; i++) {
    		assert Arrays.binarySearch(testData, testData[i])==i;
    		assert Arrays.binarySearch(testData, testData[i]*31)!=i;
    	}    	
    	et = SystemClock.endTimer();
    	log("long array search:" + et + "\n\tAverage Per:" + et.avgNs(LONG_COUNT));
    	
    	log("Testing UnsafeLongArray search");
    	testData = new long[WARM_COUNT];
    	System.arraycopy(TEST_DATA, 0, testData, 0, testData.length);
    	ula = new UnsafeLongArray(testData);
    	Arrays.sort(testData);
    	for(int i = 0; i < WARM_COUNT; i++) {
    		assert testData[i] == ula.a(i);
    		assert ula.binarySearch(testData[i])==i;
    		assert ula.binarySearch(testData[i]*31)!=i;
    	}
    	ula.destroy();
    	log("Warmup Complete");
    	ula = new UnsafeLongArray(TEST_DATA);
    	testData = ula.getArray();
    	SystemClock.startTimer();
    	
    	for(int i = 0; i < LONG_COUNT; i++) {
    		assert ula.binarySearch(testData[i])==i;
    		assert ula.binarySearch(testData[i]*31)!=i;
    	}
    	
    	et = SystemClock.endTimer();
    	ula.destroy();
    	log("UnsafeLongArray search:" + et + "\n\tAverage Per:" + et.avgNs(LONG_COUNT));
    	
    	final int HEAP_TEST_SIZE = 20;
    	
    	log("Testing long array heap size");
    	long[][] arrays = new long[HEAP_TEST_SIZE][];
    	ResourceHelper.memoryUsage(true);
    	long heapBefore = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    	for(int i = 0; i < HEAP_TEST_SIZE; i++) {
    		arrays[i] = new long[LONG_COUNT];
    		System.arraycopy(TEST_DATA, 0, arrays[i], 0, TEST_DATA.length);    	
    	}
    	long heapAfter = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    	long heapDiff = heapAfter-heapBefore;
    	log("Long Array Heap:" + heapDiff);
    	arrays = null;
    	ResourceHelper.memoryUsage(true);
    	long heapAfterRelease = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    	heapDiff = heapAfter-heapAfterRelease;
    	log("Long Array Heap Released:" + heapDiff);
    	
    	log("Testing UnsafeLongArray heap size");
    	UnsafeLongArray[] ulas = new UnsafeLongArray[HEAP_TEST_SIZE];
    	ResourceHelper.memoryUsage(true);
    	heapBefore = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    	for(int i = 0; i < HEAP_TEST_SIZE; i++) {
    		ulas[i] = new UnsafeLongArray(TEST_DATA);    	
    	}
    	heapAfter = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    	heapDiff = heapAfter-heapBefore;
    	log("UnsafeLongArray Heap:" + heapDiff);
    	for(int i = 0; i < HEAP_TEST_SIZE; i++) {
    		ulas[i].destroy();
    	}
    	ulas = null;
    	ResourceHelper.memoryUsage(true);
    	heapAfterRelease = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    	heapDiff = heapAfter-heapAfterRelease;
    	log("UnsafeLongArray Heap Released:" + heapDiff);
    	
    	
    }
    
    
    /**
     * Creates a new UnsafeLongArray using the default allocation {@value UnsafeLongArray#DEFAULT_ALLOC} and initial values of {@value Long#MAX_VALUE}.
     */
    public UnsafeLongArray() {
    	this(DEFAULT_ALLOC, Long.MAX_VALUE);
    }
    
    /**
     * Creates a new UnsafeLongArray with all entries set to zero
     * @param initialSize The initial number of longs
     * @param initialValue The value to set in all longs in the array
     */
    public UnsafeLongArray(int initialCapacity, long initialValue)  {
    	this(initialCapacity);
    	for(int i = 0; i < initialCapacity; i++) {
    		unsafe.putLong(this.address + (i << 3), initialValue);
    	}    	
    }
    
    /**
     * Creates a new un-initialized UnsafeLongArray. All values are garbage. 
     * @param initialCapacity The initial number of longs
     */
    private UnsafeLongArray(int initialCapacity)  {
        if (initialCapacity < 1) {
            throw new IllegalArgumentException("size must be at least 1 long", new Throwable());
        }
        allocation = initialCapacity;
        capacity = initialCapacity;
        this.address = unsafe.allocateMemory(initialCapacity << 3);
        this.size = 0;        
    }
    
    /**
     * Creates a new UnsafeLongArray initialized with the passed values
     * @param array The array to initialize from
     */
    public UnsafeLongArray(long[] array)  {
    	this(array.length);
    	for(int i = 0; i < array.length; i++) {
    		unsafe.putLong(this.address + (i << 3), array[i]);
    	}
    	size = array.length;
    	//sort(this);
    }
    
    /**
     * {@inheritDoc}
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
    	if(size==0) return "[]";
    	StringBuilder b = new StringBuilder("[");
    	for(int i = 0; i < size; i++) {
    		b.append(a(i)).append(",");
    	}
    	b.deleteCharAt(b.length()-1);
    	b.append("]");
    	return b.toString();
    }
    
    /**
     * Renders in the same format as {@link #toString()} except it includes the entire capacity of the array
     * @return a {@link #toString()} of the full array
     */
    public String toFullString() {
    	StringBuilder b = new StringBuilder("fc:[");
    	for(int i = 0; i < capacity; i++) {
    		if(i==size-1) {
    			b.append(a(i)).append(" ***end*** ,");
    		} else {
    			b.append(a(i)).append(",");
    		}
    		
    	}
    	b.deleteCharAt(b.length()-1);
    	b.append("]");
    	return b.toString();
    }


	/**
     * Returns a string containing some useful debug information about this instance
     * @return a disgnostic string
     */
    public String debugString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UnsafeLongArray [\n\tcapacity=");
		builder.append(capacity);
		builder.append("\n\tsize=");
		builder.append(size);
		builder.append("\n\tallocation=");
		builder.append(allocation);
		builder.append("\n\taddress=");
		builder.append(address);
		builder.append("\n]");
		return builder.toString();
    }
    
    /**
     * Adds the passed long values to this array 
     * @param values the values to add
     * @return this array
     */
    public UnsafeLongArray add(long...values) {
    	_check();
    	if(values!=null && values.length>0) {
    		for(long v: values) {
    			_add(v);
    		}
    	}
    	//sort(this);
    	return this;
    }
    
    /**
     * Inserts the passed long values to this array at the location returned from a binary search 
     * @param values the values to insert
     * @return this array
     */
    public UnsafeLongArray insert(long...values) {
    	_check();
    	if(values!=null && values.length>0) {
    		for(long v: values) {
    			_insert(v);
    		}
    	}
    	//sort(this);
    	return this;
    }
    
    
    /**
     * Adds the passed long values to this array if they are not present already 
     * @param values the values to add
     * @return this array
     */
    public UnsafeLongArray addIfNotExists(long...values) {
    	_check();
    	if(values!=null && values.length>0) {
    		for(long v: values) {
    			if(binarySearch(v)<0) {
    				_add(v);
    			}
    		}
    	}
    	//sort(this);
    	return this;
    }
    
    /**
     * Inserts the passed long values to this array if they are not present already 
     * @param values the values to insert
     * @return this array
     */
    public UnsafeLongArray insertIfNotExists(long...values) {
    	_check();
    	if(values!=null && values.length>0) {
    		for(long v: values) {
    			if(binarySearch(v)<0) {
    				_insert(v);
    			}
    		}
    	}
    	//sort(this);
    	return this;
    }
    
    
    
    /**
     * Adds the passed long to the array, extending the size of the array if necessary
     * @param v the long to add
     */
    private void _add(long v) {
    	if(size==capacity) {
    		extend();
    	}
    	unsafe.putLong(address + (size << 3), v);
    	size++;    	
    	//sort(this);
    }
    
    /**
     * Inserts the passed long to the array, extending the size of the array if necessary
     * @param v the long to insert
     */
    private void _insert(long v) {
    	if(size==capacity) {
    		extend();
    	}
		int index = binarySearch(v);
		if(index<0) index = (index*-1)-1;
		
		if(size!=0) {
	    	//if(index!=size-1) {
		    	long srcOffset = (index << 3); 
		    	long destOffset = ((index+1) << 3);
		    	long bytes = (size-index) << 3;
				unsafe.copyMemory(
						(address + srcOffset),   	// src: the address of the index where we want to insert
						(address + destOffset), 	// dest: the address of the slot after the one we want to insert
						bytes						// bytes: the number of bytes in the entries that need to be shifted down
				);
	    	//}
		} 
		unsafe.putLong(address + (index << 3), v);
		size++;
		log("\n\t--->At Size:" + size + "  Added:" + v +  "  Index:" + index + "  " +  toFullString());
    }
    
    
    /*
         	//int ind = binarySearch(v);
    	int index = binarySearch(v);
    	if(index<0) index = (index*-1)-1;
    	
    	if(size!=0) {
	    	//if(index!=size-1) {
		    	long srcOffset = (index << 3); 
		    	long destOffset = ((index+1) << 3);
		    	long bytes = (size-index) << 3;
				unsafe.copyMemory(
						(address + srcOffset),   	// src: the address of the index where we want to insert
						(address + destOffset), 	// dest: the address of the slot after the one we want to insert
						bytes						// bytes: the number of bytes in the entries that need to be shifted down
				);
	    	//}
    	} 
    	unsafe.putLong(address + (index+1 << 3), v);
    	size++;
    	if(capacity==10) log("\n\t--->At Size:" + size + "  Added:" + v +  "  Index:" + index + "  " +  toFullString());
 
     */
    
    /**
     * Extends the allocated memory by the configured allocation size.
     */
    private void extend() {
    	address = unsafe.reallocateMemory(address, (capacity << 3) + (allocation << 3));
    	capacity += allocation;
    }
    
    
    /**
     * Removes the first instance of each of the passed long values from this array if they are present 
     * @param values the values to remove
     * @return the number of removed values
     */
    public int remove(long...values) {
    	_check();
    	int removed = 0;
    	if(values!=null && values.length>0) {
    		for(long v: values) {
    			_remove(binarySearch(v));
    		}
    	}
    	//sort(this);
    	return removed;
    }
    
    /**
     * Removes all instances of each of the passed long values from this array if they are present 
     * @param values the values to remove
     * @return the number of removed items
     */
    public int removeAll(long...values) {
    	_check();
    	int _size = size;
    	if(values!=null && values.length>0) {
    		for(long v: values) {
    			boolean rem = true;
    			while(rem) {
    				rem = _remove(binarySearch(v));
    			}
    		}
    	}
    	//sort(this);
    	return _size - size;
    }
    
    
    /**
     * Removes the long at the passed index, if the index is <b><code>&gt;=0</code></b>,  by rolling all the values at the next index down by one and decrementing the size
     * @param index The index to remove thhe value from
     * @return true if the value was removed, false if no change occured
     */
    private boolean _remove(int index) {
    	if(index>=0) {    	
    		unsafe.copyMemory(
    				(address + ((index+1) << 3)),   // the address of the next index 
    				(address + ((index) << 3)), 	// the address of the index to remove
    				(size-index-1) << 3					// the number of bytes in the entries that need to be shifted down
    		);
    		size--;
    		return true;
    	}
    	return false;
    }
    
    /**
     * Checks to make sure this UnsafeLongArray has not been deallocated
     * @return true if this UnsafeLongArray is still allocated, false otherwise
     */
    public boolean check() {
    	return this.address !=0;
    }
    
    private void _check() {
    	if(!check()) throw new IllegalStateException("This UnsafeLongArray has been deallocated", new Throwable());
    }
    
    /**
     * Deallocates this UnsafeLongArrray
     */
    public void destroy() {
    	if(this.address!=0) {
    		try { unsafe.freeMemory(address); } catch (Throwable t) {}
    		this.address=0;
    	}
    }
    
    private void _checkRange(int index) {
    	if(index<0 || index > (size-1)) throw new IllegalArgumentException("The passed index was invalid [" + index + "]. Valid ranges are 0 - " + (size-1), new Throwable());
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
    	_check(); _checkRange(index);
    	return unsafe.getLong(this.address + (index << 3));    	
    }
    
    /**
     * Sets the long value at the specified index
     * @param index The index of the array to set the long at
     * @param value The long to set
     */
    public void set(int index, long value) {
    	_check(); _checkRange(index);
    	unsafe.putLong(this.address + (index << 3), value);
    }
    
    
//  public static void putLongArrayDirect(long[] array) {
//	  int base = unsafe.arrayBaseOffset(long[].class); 
//	  int scale = unsafe.arrayIndexScale(long[].class);
//	  int elementIdx = 1;
//	  int offsetForIdx = base + (elementIdx  * scale);
//	  unsafe.copyMemory(scale + base , bufferaddress?, array.length);
//}    
    
    public void finalize() throws Throwable {
    	if(this.address!=0) try { unsafe.freeMemory(address); } catch (Throwable t) {}
    }
    
    /**
     * Returns a traditional long array representing the longs in this array
     * @return a long array with the same values as this array
     */
    public long[] getArray() {
    	_check();
    	long[] arr = new long[size];
    	for(int i = 0; i < size; i++) {
    		arr[i] = unsafe.getLong(this.address + (i << 3));
    	}
    	return arr;
    }
    
    /**
     * Returns a traditional long array representing the all the allocated slots this array
     * @return a long array representing the all the allocated slots this array
     */
    public long[] getAllocatedArray() {
    	_check();
    	long[] arr = new long[capacity];
    	for(int i = 0; i < capacity; i++) {
    		arr[i] = unsafe.getLong(this.address + (i << 3));
    	}
    	return arr;
    }
    
    
    /**
     * {@inheritDoc}
     * @see java.lang.Object#clone()
     */
    public UnsafeLongArray clone() {
    	_check();
    	UnsafeLongArray cloned = new UnsafeLongArray(size);
    	for(int i = 0; i < size; i++) {
    		cloned.a(i, a(i));
    	}
    	return cloned;
    }
    
    


    /**
     * Returns the number of allocated longs in the array
     * @return the number of allocated longs in the array
     */
    public int size() {
    	_check();
    	return size;
    }

    /**
     * Returns the total number of allocated slots in the array
     * @return the total number of allocated slots in the array
     */    
    public int capacity() {
    	_check();
    	return capacity;    	
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
     * Sorts the specified range of the array.
     *
     * @param a the array to be sorted
     * @param left the index of the first element, inclusive, to be sorted
     * @param right the index of the last element, inclusive, to be sorted
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
            b = new UnsafeLongArray(ula.size());
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
