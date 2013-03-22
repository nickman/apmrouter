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

import org.helios.apmrouter.unsafe.UnsafeAdapter;
import sun.misc.Unsafe;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>Title: UnsafeArray</p>
 * <p>Description: Base class for unsafe array implementations</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.collections.UnsafeArray</code></p>
 */

public abstract class UnsafeArray {
	
	/** The default number of slots that will be allocated when the array needs to be extended */
	public static final int DEFAULT_ALLOC_INCR = 128;
	/** A marker placed in the {@link UnsafeArray#toFullString()} indicating where actual size elements terminate */
	public static final String SIZE_MARKER = ">><<,";
    /** The byte array offset */
    public static final int BYTE_ARRAY_OFFSET;
    /** The int array offset */
    public static final int INT_ARRAY_OFFSET;

	
	/** The default capacity of an empty created UnsafeArray */
	public static int DEFAULT_CAPACITY = 128;
	
	/** A gauge of the number of un-managed memory allocations */
	private static final AtomicLong UNMANAGED_MEM_ALLOCATIONS = new AtomicLong(0L);
	
	
    /** The unsafe instance */
    protected static final Unsafe unsafe;
    
	/** Indicates the array will be maintained in sorted order */
	protected final boolean sorted;	
	/** Indicates the capacity of the array will be fixed */
	protected final boolean fixed;	
	/** The maximum capacity of the array */
	protected final int maxCapacity;
	/** The minimum capacity of the array */
	protected final int minCapacity;	
	/** The number of slots that will be allocated when the array needs to be extended */
	protected final int allocationIncrement;
	/** The number of excess slots that are emptied by rollLefts before the array capacity is shrunk */
	protected final int clearedSlotsFree;
	/** The size of one slot */
	protected final int slotSize;
	

	/** The native memory address of the array */
	protected long address;	
	/** The current capacity of the array */
	protected int capacity;
	/** The current size of the array */
	protected int size;
	
	
    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe)field.get(null);
            BYTE_ARRAY_OFFSET = unsafe.arrayBaseOffset(byte[].class);
            INT_ARRAY_OFFSET = unsafe.arrayBaseOffset(int[].class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get the Unsafe instance", e);
        }
    }
    

    

    
    /**
     * Returns the bitshift factor for calculating the total number of bytes for this array's type.
     * i.e. where <b><code>1 << slotsize == number of bytes per slot</code></b>.
     * @return the bitshift factor
     */
    protected abstract int getSlotSize();
    
    /**
     * Appends the specified item to the passed StringBuilder
     * @param index The index of the item to append
     * @param b the stringbuilder to append to
     * @return the passed StringBuilder
     */
    protected abstract StringBuilder append(final StringBuilder b, int index);
    
    /**
     * Allocates the sized memory block, incrementing the unmanaged counter
     * @param size The size of the memory to allocate in bytes
     * @return a pointer to the memory block allocated
     */
    protected static long allocateMemory(long size) {
    	long address = unsafe.allocateMemory(size);
    	UNMANAGED_MEM_ALLOCATIONS.incrementAndGet();
    	return address;
    }
    
    /**
     * Frees the memory block pointed to by the passed address, decrementing the unmanaged counter
     * @param address The pointer to the memory block to free
     * @return the number of unmanaged allocations remaining
     */
    protected static long freeMemory(long address) {
    	unsafe.freeMemory(address);
    	return UNMANAGED_MEM_ALLOCATIONS.decrementAndGet();    	
    }
	
    
    /**
     * Returns the number of existing unmanaged memory allocations
     * @return the number of existing unmanaged memory allocations
     */
    public static long getPointerCount() {
    	return UNMANAGED_MEM_ALLOCATIONS.get();
    }
	
	/**
	 * Creates a new UnsafeArray
	 * @param initialCapacity The initial allocated capacity
	 * @param sorted Indicates the array will be maintained in sorted order
	 * @param fixed Indicates the capacity of the array will be fixed
	 * @param maxCapacity The maximum capacity of the array
	 * @param minCapacity The minimum capacity of the array
	 * @param allocationIncrement The number of slots that will be allocated when the array needs to be extended
	 * @param clearedSlotsFree The number of excess slots that are emptied by rollLefts before the array capacity is shrunk
	 */
	protected UnsafeArray(int initialCapacity, boolean sorted, boolean fixed, int maxCapacity, int minCapacity, int allocationIncrement, int clearedSlotsFree) {
		this.sorted = sorted;
		this.fixed = fixed;
		this.maxCapacity = maxCapacity;
		this.minCapacity = minCapacity;
		this.allocationIncrement = allocationIncrement;
		this.clearedSlotsFree = clearedSlotsFree;
		slotSize = getSlotSize();
		capacity = initialCapacity;
		address = allocateMemory(capacity << slotSize);
		UnsafeAdapter.setMemory(null, address, capacity << slotSize, (byte)0);		
		size = 0;
		
	}
	
	/**
	 * Creates a new UnsafeArray. Used for cloning.
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
	protected UnsafeArray(int size, int capacity, long address, boolean sorted, boolean fixed, int maxCapacity, int minCapacity, int allocationIncrement, int clearedSlotsFree) {
		this.sorted = sorted;
		this.fixed = fixed;
		this.maxCapacity = maxCapacity;
		this.minCapacity = minCapacity;
		this.allocationIncrement = allocationIncrement;
		this.clearedSlotsFree = clearedSlotsFree;
		slotSize = getSlotSize();		
		this.size = size;
		this.capacity = capacity;
		this.address = allocateMemory(capacity << slotSize);
		unsafe.copyMemory(address, this.address, size << slotSize);
		
	}

	/**
	 * Initializes this array from one of the following supported types:<ol>
	 * 	<li>Primitive Arrays</li>
	 * </ol>
	 * @param data An instance of one of the above supported classes
	 */
	protected void load(Object data) {		
		if(data.getClass().isArray()) {
			loadArray(data);
		}
	}
	
	/**
	 * Initializes this array from an array
	 * @param data The array to load
	 */
	protected void loadArray(Object data) {
		Class<?> clazz = data.getClass().getComponentType();
		if(!clazz.isPrimitive()) throw new RuntimeException("An array of [" + clazz.getName() + "]s cannot be loaded. Only primitives are currently supported", new Throwable());
		long offset = unsafe.arrayBaseOffset(data.getClass()); 
		int length = Array.getLength(data);
		
		if(length>maxCapacity) {
			throw new ArrayOverflowException("Passed array of length [" + length + "] is too large for this UnsafeArray with a max capacity of [" + maxCapacity + "]", new Throwable());
		}
		UnsafeAdapter.copyMemory(data, offset, null, address, length << slotSize);
	}
	
	/**
	 * If the passed object is assignable to an {@link UnsafeArray}, indicates if the two instances point to the same array (i.e. memory address). 
	 * @param obj The object to compare to
	 * @return true if the passed object is an {@link UnsafeLongArray} share the same array (memory address).
	 */
	public boolean isSameInstance(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!getClass().isAssignableFrom(obj.getClass())) {
			return false;
		}
		UnsafeArray other = (UnsafeArray) obj;
		if (address != other.address) {
			return false;
		}
		return true;
	}
	
	
	
    /**
     * Checks to make sure this UnsafeLongArray has not been deallocated
     * @return true if this UnsafeLongArray is still allocated, false otherwise
     */
    public boolean check() {
    	return this.address !=0;
    }
    
    
    
    /**
     * Checks that the array has not been deallocated, throwing a {@link IllegalStateException} if it has.
     */
    protected void _check() {
    	if(!check()) throw new IllegalStateException("This UnsafeLongArray has been deallocated", new Throwable());
    }
    
    /**
     * Deallocates this UnsafeLongArrray
     */
    public void destroy() {
    	if(this.address!=0) {
    		try { freeMemory(address); } catch (Throwable t) {}
    		this.address=0;
    	}
    }
    
    /**
     * Checks that an index is within the populated slots of this array.
     * If the check fails, throws a {@link IllegalArgumentException}
     * @param index The index to check
     */
    protected void _check(int index) {
    	if(index<0 || index > (size-1)) {
    		throw new IllegalArgumentException("The passed index was invalid [" + index + "]. Valid ranges are 0 - " + (size-1), new Throwable());
    	}
    }
    
    protected void _checkc(int index) {
    	if(index<0 || index > (capacity-1)) {
    		throw new IllegalArgumentException("The passed index was invalid [" + index + "]. Valid ranges are 0 - " + (capacity-1), new Throwable());
    	}
    }
    
    
    /**
     * Extends the allocated memory for the passed number of items in increments of the configured allocation increment size.
     * If this will result in a projected capacity that is larger than {@link Integer#MAX_VALUE}, 
     * will use the highest possible increment where <b><code>increment &lt; Integer.MAX_VALUE && capacity+increment &lt;= max-capacity</code></b>
     * @param allowTruncate If true, an allocation of less than what was asked for is ok, otherwise, throw an ArrayOverflowException
     * @param items The number of items we're extending for
     * @return the number of items additional capacity was allocated for 
     */
    protected int extend(boolean allowTruncate, int items) {
    	assert items > 0;
    	if(fixed && capacity==maxCapacity) throw new ArrayOverflowException("Capacity cannot be extended:" + (fixed ? "Array is fixed capacity" : "Array is at maximum capacity"), new Throwable());
    	long targetCap = (long)capacity + (long)items;
    	if(targetCap > Integer.MAX_VALUE) throw new ArrayOverflowException("Capacity cannot be extended by [" + items + "] as it would overflow Integer.MAX_ITEMS", new Throwable());

    	int targetCapacity = size + items;
    	if(fixed && targetCapacity > maxCapacity) { 		
    		if(!allowTruncate) throw new ArrayOverflowException("Capacity cannot be extended to [" + targetCapacity + "] as it would overflow the defined maximum capacity of [" + maxCapacity + "]", new Throwable());
    		items = targetCapacity - maxCapacity;
    		targetCapacity = maxCapacity;
    	}
    	int newAlloc = 0;
    	while(capacity<targetCapacity) {
    		long cap = capacity;
    		long alloc = allocationIncrement;
    		if(cap+alloc > maxCapacity) {
    			newAlloc = maxCapacity-capacity;
	    	} else if(cap+alloc > Integer.MAX_VALUE) {
	    		newAlloc = Integer.MAX_VALUE-capacity;
	    	} else {
	    		newAlloc = allocationIncrement;
	    	}    			
    		capacity += newAlloc;
    	}
    	address = unsafe.reallocateMemory(address, (capacity << slotSize) + (newAlloc << slotSize));
    	return items;
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
     * @return false if the rightmost item was dropped to make room for the new value, true otherwise
     */
    protected boolean rollRight(int index) {
    	_check(); _checkc(index);
    	final int numberOfSlotsToMove;
    	final boolean incrSize;
    	if(size==capacity) {
        	if(fixed) {
        		numberOfSlotsToMove = size-index-1;
        		incrSize=false;
        	} else {
        		extend(false, 1);
        		numberOfSlotsToMove = size-index;
        		incrSize=true;
        	}    	
    	} else {
    		numberOfSlotsToMove = size-index;
    		incrSize=true;
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
		return incrSize;
    }
    
    
    
    /**
     * Rolls all the entries in the array one slot to the left after the referenced index
     * Logically, this removes a new slot at the referenced index.
     * Once this method completes, the size of the array will have been decremented by 1.
     * @param shrink If true, a shrink check will be made after this op concludes.
     * @param index The index after which the remaining values are rolled to the left
	 * <p><b>Before</b><pre>
	            <--  <--  <--  <--
	    +--+ +--+ +--+ +--+ +--+ +--+               Size:      6     Index:   1
	    |23| |47| |19| |67| |42| |89|               Capacity:  6     
	    +--+ +--+ +--+ +--+ +--+ +--+
	           ^                                      
	           |                   
	         Delete               
		</pre><b>After</b><pre>
	     +--+ +--+ +--+ +--+ +--+               Size:      5
	     |23| |19| |67| |42| |89|               Capacity:  6
	     +--+ +--+ +--+ +--+ +--+ +--+	 
	     </pre> 
     */
    public void rollLeft(boolean shrink, int index) {
    	_check(); _check(index);
    	int newInd = index+1;
    	int numberOfSlotsToMove = size-newInd;
    	long srcOffset = (newInd << slotSize); 
    	long destOffset = (index << slotSize);
    	long bytes = numberOfSlotsToMove << slotSize;
		unsafe.copyMemory(
				(address + srcOffset),   	// src: the address of the first index we want to roll
				(address + destOffset), 	// dest: the address of the slot after the one we want to roll
				bytes						// bytes: the number of bytes in the entries that need to be rolled
		);
		size--;
		if(shrink) shrink();
    }
    
    /**
     * <p>Rolls all the entries in the array one slot to the left after the referenced index, then checks for shrink.
     * Logically, this removes a new slot at the referenced index.
     * Once this method completes, the size of the array will have been decremented by 1.</p>
     * @see {@link #rollLeft(boolean, int)}
     * 
     * @param index The index after which the remaining values are rolled to the left
     */
    public void rollLeft(int index) {
    	rollLeft(true, index);
    }
    
    
    /**
     * Checks the capacity to see if any empty slots can be released
     * @return the number of slots freed
     */
    public int shrink() {
			int freeSlots = capacity-size;	 // the number of empty slots	
			// if true, there are enough empty slots to trigger a shrink, 
			// and still leave the min capacity available
			if(freeSlots >= clearedSlotsFree && freeSlots-minCapacity>0) {  				
				int slotsToFree = freeSlots-minCapacity; // the number of slots to free and leave the min
				_shrink(slotsToFree);
				return slotsToFree;
			}
			return 0;
    }
    
    /**
     * Unchecked version of {@link UnsafeArray#shrink(int)}.
     * @param clear The number of free slots to shrink out of the array
     */
    private void _shrink(int clear) {
    	capacity -= clear;
    	address = unsafe.reallocateMemory(address, capacity << slotSize);
    }
    
    
    /**
     * {@inheritDoc}
     * @see java.lang.Object#finalize()
     */
    @Override
	public void finalize() throws Throwable {
    	if(this.address!=0) try { freeMemory(address); } catch (Throwable t) {}
    	super.finalize();
    }
    
    
    
	

	/**
	 * Returns the current capacity of the array, i.e. the number of used slots 
	 * @return the current capacity of the array
	 */
	public int capacity() {
		return capacity;
	}
	
	/**
	 * Removes all values and if applicable, shrinks the capacity.
	 */
	public void clear() {
		size = 0;
		shrink();
	}
	
	/**
	 * Returns the current size of the array, i.e. the number of used slots 
	 * @return the current size of the array
	 */
	public int size() {
		return size;
	}
	
	/**
	 * Indicates if this array is maintained in a sorted state 
	 * @return true if this array is maintained in a sorted state, false otherwise
	 */
	public boolean sorted() {
		return sorted;
	}
	
	/**
	 * Indicates if this array has a fixed capacity 
	 * @return true if this array has a fixed capacity, false otherwise
	 */
	public boolean fixed() {
		return fixed;
	}
	
	/**
	 * Returns the maximum capacity of this array 
	 * @return the maximum capacity of this array
	 */
	public int maxCapacity() {
		return maxCapacity;
	}
	
	/**
	 * Returns the minimum capacity of this array, i.e. the capacity will not be shrunk below this size 
	 * @return the minimum capacity of this array
	 */
	public int minCapacity() {
		return minCapacity;
	}
	
	/**
	 * Returns the number of slots that will be added when the array is extended, returning 0 if the capacity is fixed. 
	 * @return the number of slots that will be added when the array is extended
	 */
	public int allocationIncrement() {
		return allocationIncrement;
	}

	/**
	 * Returns the number of unallocated slots that will trigger a shrink of the capacity when a slot is logically deleted. 
	 * @return of unallocated slots that will trigger a shrink of the capacity 
	 */
	public int clearedSlotsFree() {
		return clearedSlotsFree;
	}

    /**
     * Out log
     * @param msg The message to log
     */
    public static void log(Object msg) {
    	System.out.println(msg);
    }
    
    /**
     * Err log
     * @param msg The message to log
     */
    public static void loge(Object msg) {
    	if(msg!=null && msg instanceof Throwable) {
    		((Throwable)msg).printStackTrace(System.err);
    	}
    	System.err.println(msg);
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
    		append(b, i).append(",");
    	}
    	b.deleteCharAt(b.length()-1);
    	b.append("]");
    	return b.toString();
    }
    
    /**
     * Renders in the same format as {@link #toString()} except it includes the entire capacity of the array.
     * <p><b>Note:</b>The empty slots may contain complete garbage.
     * @return a {@link #toString()} of the full array
     */
	public String toFullString() {
    	StringBuilder b = new StringBuilder("fc:[");
    	for(int i = 0; i < capacity; i++) {
    		if(i==size-1) {
    			append(b, i).append(SIZE_MARKER);
    		} else {
    			append(b, i).append(",");
    		}    		
    	}
    	b.deleteCharAt(b.length()-1);
    	b.append("]");
    	return b.toString();
    }
	
	public static int toInt(byte[] arr) {
		int[] iarr = new int[1];
		UnsafeAdapter.copyMemory(arr, INT_ARRAY_OFFSET, iarr, INT_ARRAY_OFFSET, 4);
		return iarr[0];
		
	}


}
