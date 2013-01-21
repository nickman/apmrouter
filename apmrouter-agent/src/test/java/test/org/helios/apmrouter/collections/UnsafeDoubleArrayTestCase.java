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
package test.org.helios.apmrouter.collections;

import java.util.Arrays;
import java.util.Random;

import org.helios.apmrouter.collections.UnsafeArray;
import org.helios.apmrouter.collections.UnsafeArrayBuilder;
import org.helios.apmrouter.collections.UnsafeDoubleArray;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

/**
 * <p>Title: UnsafeDoubleArrayTestCase</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.org.helios.apmrouter.collections.UnsafeDoubleArrayTestCase</code></p>
 */

public class UnsafeDoubleArrayTestCase {
	/**
	 * StdOut log
	 * @param obj The message to log
	 */
	protected static void log(Object obj) {
		System.out.println(obj);
	}
	/**
	 * StdErr log
	 * @param obj The message to log
	 */
	protected static void loge(Object obj) {
		System.err.println(obj);
	}
	/** The currently executing test name */
	@Rule public final TestName name = new TestName();

	/**
	 * Prints the test name about to be executed
	 */
	@Before
	public void printTestName() {
		log("\n\t==================================\n\tRunning Test [" + name.getMethodName() + "]\n\t==================================\n");
	}
	
	/** The number of unmanaged allocations before the tests start */
	private static long unmanagedAllocations = 0;
	
	/**
	 * Records the number of unmanaged unsafe allocations
	 */
	@BeforeClass
	public static void getUnsafeAllocations() {
		unmanagedAllocations = UnsafeArray.getPointerCount();
	}
	
	
	/**
	 * Asserts that the number of unsafe allocations is the same as before the tests started 
	 */
	@AfterClass
	public static void testZeroAllocations() {
		System.gc();
		System.runFinalization();
		System.gc();
		log("Starting Unmanaged Allocations:" + unmanagedAllocations);
		log("Ending Unmanaged Allocations:" + UnsafeArray.getPointerCount());
		Assert.assertEquals("The expected number of allocations", unmanagedAllocations, UnsafeArray.getPointerCount());
		
	}
	
	/** A random */
	protected final Random RANDOM = new Random(System.currentTimeMillis());
	
	/** A sample large-ish test array */
	static final double[] LARGE_TEST_ARR = new double[250000];
	
	static {  // Initializes the test array
		Random r = new Random(System.currentTimeMillis());
		for(int i = 0; i < LARGE_TEST_ARR.length; i++) {
			LARGE_TEST_ARR[i] = r.nextDouble();
		}
	}
	
	
	/**
	 * Returns the next positive random int
	 * @param upto The upper range of the next random int
	 * @return the next positive random int
	 */
	public int nextRandomInt(int upto) {
		return Math.abs(RANDOM.nextInt(upto));
	}	
	
	/**
	 * Generates a double array of the specified size populated with random doubles
	 * @param size The size of the array
	 * @param abs If true, the longs will be positive only
	 * @return the allocated test data array
	 */
	protected double[] allocateRandom(int size, boolean abs) {
		double[] arr = new double[size];
		for(int i = 0; i < size; i++) {
			arr[i] = abs ? Math.abs(RANDOM.nextDouble()) : RANDOM.nextDouble();
		}
		return arr;
	}
	
	/** Auto destroyed test ula */
	protected UnsafeDoubleArray ula = null;
	
	

	
	/**
	 * Deallocates the test ula
	 */
	@After
	public void killUla() {
		if(ula!=null) {
			ula.destroy();
			ula = null;
		}
	}
	
	
	/**
	 * Tests a no param ctor allocation
	 */
	@Test
	public void testNoParamAllocation() {
		ula = UnsafeArrayBuilder.newBuilder().buildDoubleArray();
		Assert.assertEquals(UnsafeDoubleArray.DEFAULT_CAPACITY, ula.capacity());
		Assert.assertEquals(0, ula.size());
		double[] defValues = new double[UnsafeDoubleArray.DEFAULT_CAPACITY];
		Arrays.fill(defValues, 0);
		
		Assert.assertArrayEquals(defValues, ula.getAllocatedArray(), 0D);
		Assert.assertArrayEquals(new double[]{}, ula.getArray(), 0D);
	}
	
	/**
	 * Tests a sized ctor allocation
	 */
	@Test
	public void testSizedAllocation() {
		ula = UnsafeArrayBuilder.newBuilder().initialCapacity(31).buildDoubleArray();
		Assert.assertEquals(31, ula.capacity());
		Assert.assertEquals(0, ula.size());
		double[] defValues = new double[ula.capacity()];
		Arrays.fill(defValues, 0L);
		Assert.assertArrayEquals(defValues, ula.getAllocatedArray(), 0D);
		Assert.assertArrayEquals(new double[]{}, ula.getArray(), 0D);
	}
	
	/**
	 * Tests an array based allocation
	 */
	@Test
	public void testArrayAllocation() {
		final double[] ARR_DATA = allocateRandom(31, false);
		ula = UnsafeArrayBuilder.newBuilder().initialCapacity(31).buildDoubleArray(ARR_DATA);
		Assert.assertEquals(31, ula.capacity());
		Assert.assertEquals(31, ula.size());
		Assert.assertArrayEquals(ARR_DATA, ula.getAllocatedArray(), 0D);
		Assert.assertArrayEquals(ARR_DATA, ula.getArray(), 0D);
		UnsafeDoubleArray anotherUla = UnsafeArrayBuilder.newBuilder().initialCapacity(31).buildDoubleArray(ula);
		Assert.assertEquals(31, anotherUla.capacity());
		Assert.assertEquals(31, anotherUla.size());
		Assert.assertArrayEquals(ARR_DATA, anotherUla.getAllocatedArray(), 0D);
		Assert.assertArrayEquals(ARR_DATA, anotherUla.getArray(), 0D);
		anotherUla.destroy();
		
		
	}
	
	/**
	 * Tests the roll right operation with no extend required
	 */
	@Test
	public void testRollRightNoExtend() {
		int arrSize = 1000;
		for(int indexToInsertAt = 0; indexToInsertAt < arrSize; indexToInsertAt++) {
			ula = UnsafeArrayBuilder.newBuilder().initialCapacity(arrSize+1).buildDoubleArray();
			Assert.assertEquals(0, ula.size());
			Assert.assertEquals(arrSize+1, ula.capacity());
			final double[] ARR_DATA = allocateRandom(arrSize, false);
			ula.append(ARR_DATA);
			Assert.assertArrayEquals(ARR_DATA, ula.getArray(), 0D);
			double valueToInsert = RANDOM.nextDouble();
//			log("Index:" + indexToInsertAt +   "  Value:" + valueToInsert);
//			log("BEFORE:" + ula.toFullString());
			ula.rollRight(indexToInsertAt, valueToInsert);
//			log("AFTER:" + ula.toFullString());
//			log("=========================================");
			Assert.assertEquals(arrSize+1, ula.size());
			Assert.assertEquals(arrSize+1, ula.capacity());
			Assert.assertEquals(valueToInsert, ula.get(indexToInsertAt), 0D);
			double[] NEW_ARR_DATA = new double[arrSize+1];
			System.arraycopy(ARR_DATA, 0, NEW_ARR_DATA, 0, indexToInsertAt);
			System.arraycopy(ARR_DATA, indexToInsertAt, NEW_ARR_DATA, indexToInsertAt+1, ARR_DATA.length-indexToInsertAt);
			NEW_ARR_DATA[indexToInsertAt] = valueToInsert;
			Assert.assertArrayEquals(NEW_ARR_DATA, ula.getArray(), 0D);
			ula.destroy();
		}
	}
	
	/**
	 * Tests the roll right operation with extend required
	 */
	@Test
	public void testRollRightWithExtend() {
		int arrSize = 300;
		int initialSize = 1;
		int expectedExtends = (arrSize/UnsafeDoubleArray.DEFAULT_CAPACITY)+1;
		int expectedCapacity = (expectedExtends*UnsafeDoubleArray.DEFAULT_CAPACITY)+1; 		
		for(int indexToInsertAt = 0; indexToInsertAt < arrSize; indexToInsertAt++) { 
			ula = UnsafeArrayBuilder.newBuilder().initialCapacity(1).buildDoubleArray();
			Assert.assertEquals(0, ula.size());
			Assert.assertEquals(initialSize, ula.capacity());
			final double[] ARR_DATA = allocateRandom(arrSize, false);
			ula.append(ARR_DATA);
			Assert.assertArrayEquals(ARR_DATA, ula.getArray(), 0D);
			long valueToInsert = RANDOM.nextLong();
			ula.rollRight(indexToInsertAt, valueToInsert);
			Assert.assertEquals(arrSize+1, ula.size());
			Assert.assertEquals(valueToInsert, ula.get(indexToInsertAt));
			double[] NEW_ARR_DATA = new double[arrSize+1];
			System.arraycopy(ARR_DATA, 0, NEW_ARR_DATA, 0, indexToInsertAt);
			System.arraycopy(ARR_DATA, indexToInsertAt, NEW_ARR_DATA, indexToInsertAt+1, ARR_DATA.length-indexToInsertAt);
			NEW_ARR_DATA[indexToInsertAt] = valueToInsert;
			Assert.assertArrayEquals(NEW_ARR_DATA, ula.getArray(), 0D);
			Assert.assertEquals(expectedCapacity, ula.capacity());
			ula.destroy();
		}
	}
	
	/**
	 * <p>Tests the roll right operation with fixed capacity and no extend
	 * <p><b>Before</b><pre>
           -->  -->  -->  -->  --> Dropped
    +--+ +--+ +--+ +--+ +--+ +--+               Size:      6     Index:   1
    |23| |47| |19| |67| |42| |89|               Capacity:  6     Value:   77
    +--+ +--+ +--+ +--+ +--+ +--+

           ^                   ^
           |                   |
           |                   |
         Index               Dropped
	</pre><b>After</b><pre>
     +--+ +--+ +--+ +--+ +--+ +--+               Size:      6
     |23| |77| |47| |19| |67| |42|               Capacity:  6
     +--+ +--+ +--+ +--+ +--+ +--+	 
     * </pre>
	 */
	@Test
	public void testRollRightFixedCapacityWithNoExtend() {
		final double[] BEFORE = {23, 47, 19, 67, 42, 89}; 
		final double[] AFTER  = {23, 77, 47, 19, 67, 42};
		final double INSERT = 77;
		final int INDEX = 1;
		ula = UnsafeArrayBuilder.newBuilder().fixed(true).maxCapacity(BEFORE.length).buildDoubleArray(BEFORE);
		Assert.assertEquals(BEFORE.length, ula.size());
		Assert.assertEquals(BEFORE.length, ula.capacity());
		Assert.assertArrayEquals(BEFORE, ula.getArray(), 0D);
		ula.rollRight(INDEX, INSERT);
		Assert.assertEquals(BEFORE.length, ula.size());
		Assert.assertEquals(BEFORE.length, ula.capacity());
		Assert.assertArrayEquals(AFTER, ula.getArray(), 0D);
		ula.destroy();
	}
	
	
	/**
	 * Tests the roll left operation.
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
     |23| |47| |19| |67| |42|               Capacity:  6
     +--+ +--+ +--+ +--+ +--+ +--+	 
     </pre> 
	 */
	@Test
	public void testRollLeft() {
		int arrSize = 300;
		for(int indexToRemove = 0; indexToRemove < arrSize; indexToRemove++) { 
			ula = UnsafeArrayBuilder.newBuilder().initialCapacity(arrSize+1).buildDoubleArray();
			Assert.assertEquals(0, ula.size());
			Assert.assertEquals(arrSize+1, ula.capacity());
			final double[] ARR_DATA = allocateRandom(arrSize, false);
			ula.append(ARR_DATA);
			Assert.assertArrayEquals(ARR_DATA, ula.getArray(), 0D);
			ula.rollLeft(indexToRemove);
			Assert.assertEquals(arrSize-1, ula.size());
			Assert.assertEquals(arrSize+1, ula.capacity());			
			double[] NEW_ARR_DATA = new double[arrSize-1];
			System.arraycopy(ARR_DATA, 0, NEW_ARR_DATA, 0, indexToRemove);
			System.arraycopy(ARR_DATA, indexToRemove+1, NEW_ARR_DATA, indexToRemove, ARR_DATA.length-indexToRemove-1);			
			Assert.assertArrayEquals(NEW_ARR_DATA, ula.getArray(), 0D);
			ula.destroy();
		}
	}
	
	/**
	 * Tests an array with a small initial capacity that grows to a much larger size, 
	 * allocating aditional memory as required, and then shrinking as items are removed.
	 */
	@Test
	public void testExtendsWithAddMemory() {
		final int LOOPS = 1000;
		final int ALLOC_INCR = 100;
		final int MIN_CAP = 100;
		int expectedCapacity = 1;
		double[] testArray = new double[1];
		ula = UnsafeArrayBuilder.newBuilder().initialCapacity(1).minCapacity(MIN_CAP).allocationIncrement(ALLOC_INCR).buildDoubleArray();
		for(int i = 0; i < LOOPS; i++) {
			ula.append(i);
			Assert.assertEquals(i+1, ula.size());
			Assert.assertEquals(expectedCapacity, ula.capacity());
			testArray[testArray.length-1] = i;
			Assert.assertArrayEquals(testArray, ula.getArray(), 0D);
			// ==================
			if(ula.capacity()==ula.size()) {
				expectedCapacity += ALLOC_INCR;
			}
			// ==================
			double[] tmp = new double[testArray.length+1];
			System.arraycopy(testArray, 0, tmp, 0, testArray.length);
			testArray = tmp;
		}
		ula.removeAll(testArray);  // will trigger a shrink.
		Assert.assertEquals(MIN_CAP, ula.capacity());
		Assert.assertEquals(0, ula.size());
		int newDataLength = (testArray.length/2)-50;
		double[] tmp = new double[newDataLength];
		System.arraycopy(testArray, 0, tmp, 0, newDataLength);
		ula.append(tmp);
		expectedCapacity = newDataLength + newDataLength%ALLOC_INCR;  // round up to the next ALLOC size
		Assert.assertEquals(expectedCapacity, ula.capacity());
		Assert.assertEquals(newDataLength, ula.size());
	}
	
	
	/**
	 * Tests the array fast clone
	 */
	@Test
	public void testClone() {
		int arrSize = LARGE_TEST_ARR.length;
		ula = UnsafeArrayBuilder.newBuilder().buildDoubleArray(LARGE_TEST_ARR);		
		Assert.assertEquals(arrSize, ula.size());
		Assert.assertEquals(arrSize, ula.capacity());
		UnsafeDoubleArray clonedUla = null;
		try {
			clonedUla = ula.clone();
			Assert.assertEquals(arrSize, clonedUla.size());
			Assert.assertEquals(arrSize, clonedUla.capacity());
			Assert.assertArrayEquals(ula.getArray(), clonedUla.getArray(), 0D);
		} finally {
			if(clonedUla!=null) {
				clonedUla.destroy();
			}
		}
	}
	
	/**
	 * Tests the array sort
	 */
	@Test
	public void testSort() {
		int arrSize = LARGE_TEST_ARR.length;
		ula = UnsafeArrayBuilder.newBuilder().buildDoubleArray(LARGE_TEST_ARR);
		Assert.assertEquals(arrSize, ula.size());
		Assert.assertEquals(arrSize, ula.capacity());
		ula.sort();
		double[] arrToSort = new double[arrSize];
		System.arraycopy(LARGE_TEST_ARR, 0, arrToSort, 0, arrSize);
		Arrays.sort(arrToSort);
		Assert.assertArrayEquals(arrToSort, ula.getArray(), 0D);
	}
	
	/**
	 * Tests that new items are placed into the correct slots in a sorted array
	 * and that binary searches for values not in the ula return <0.
	 */
	@Test
	public void testInsertIntoSorted() {
		final int ARR_SIZE = 10000;
		ula = UnsafeArrayBuilder.newBuilder().initialCapacity(ARR_SIZE*2).sorted(true).buildDoubleArray();
		double[] odds = new double[ARR_SIZE];
		double[] evens = new double[ARR_SIZE];
		double[] all = new double[ARR_SIZE*2];
		// ============================================
		// Initialize 3 arrays:
		// An array of all odds
		// An array of all evens
		// An array of both
		// ============================================
		int value = 0;
		for(int i = 0; i < ARR_SIZE; i++) {
			if(value%2==0) {				
				evens[i] = value;
				value++;
				odds[i] = value;
			} else {				
				odds[i] = value;
				value++;
				evens[i] = value;
			}
			value++;
		}
		for(int i = 0; i < ARR_SIZE*2; i++) {
			all[i] = i;
		}
		// ============================================
//		log("Even Array:" + Arrays.toString(evens));
//		log("Odd Array:" + Arrays.toString(odds));
//		log("All Array:" + Arrays.toString(all));
		// ============================================
		//   Append all the evens
		// ============================================
		ula.append(evens);
		// ============================================
		//		Insert all the odds
		// ============================================
		for(int i = 0; i < ARR_SIZE; i++) {
			Assert.assertTrue(ula.binarySearch(odds[i])<0);
			ula.insert(odds[i]);
		}
		// ============================================
		//		The ula should now be equal to all
		// ============================================		
		Assert.assertArrayEquals(all, ula.getArray(), 0D);
		//log(ula.toString());
	}
	
	/**
	 * Tests that removed from a sorted array are remobed from the correct slot
	 * and the remaining array is adjusted for capacity and shrinks.
	 */
	@Test
	public void testRemoveFromSorted() {
		final int ARR_SIZE = 5;
		ula = UnsafeArrayBuilder.newBuilder().initialCapacity(ARR_SIZE*2).sorted(true).buildDoubleArray();
		double[] odds = new double[ARR_SIZE];
		double[] evens = new double[ARR_SIZE];
		double[] all = new double[ARR_SIZE*2];
		// ============================================
		// Initialize 3 arrays:
		// An array of all odds
		// An array of all evens
		// An array of both
		// ============================================
		int value = 0;
		for(int i = 0; i < ARR_SIZE; i++) {
			if(value%2==0) {				
				evens[i] = value;
				value++;
				odds[i] = value;
			} else {				
				odds[i] = value;
				value++;
				evens[i] = value;
			}
			value++;
		}
		for(int i = 0; i < ARR_SIZE*2; i++) {
			all[i] = i;
		}
		// ============================================
		//   Append the ALL array
		// ============================================
		ula.append(all);
		// ============================================
		//		Remove each of the odds
		// ============================================
		for(int i = 0; i < ARR_SIZE; i++) {
			int preSize = ula.size();
			int removed = ula.remove(odds[i]);
			Assert.assertEquals(1, removed);
			Assert.assertEquals(preSize-1, ula.size());
			removed = ula.remove(odds[i]);
			Assert.assertEquals(0, removed);
			Assert.assertEquals(preSize-1, ula.size());			
		}
		// ============================================
		//		BATCH Add all the odds back in again
		// ============================================
		int preSize = ula.size();
		int inserted = ula.insertIfNotExists(odds);
		Assert.assertEquals(odds.length, inserted);
		Assert.assertEquals(preSize+odds.length, ula.size());
		// ============================================
		//		BATCH Remove all the odds 
		// ============================================
		preSize = ula.size();
		int removed = ula.remove(odds);
		Assert.assertEquals(odds.length, removed);
		Assert.assertEquals(preSize-odds.length, ula.size());
		
		// ============================================
		//		The ula should now be equal to evens
		// ============================================		
		Assert.assertArrayEquals(evens, ula.getArray(), 0D);
		//log(ula.toString());
	}
	
	
	/**
	 * Tests that the clear() method correctly clears the size and shrinks the capacity where applicable
	 */
	@Test
	public void testClear() {
		ula = UnsafeArrayBuilder.newBuilder().minCapacity(200).buildDoubleArray(LARGE_TEST_ARR);
		Assert.assertEquals(LARGE_TEST_ARR.length, ula.size());
		ula.clear();
		Assert.assertEquals(0, ula.size());
		Assert.assertEquals(200, ula.capacity());
	}
	

}
