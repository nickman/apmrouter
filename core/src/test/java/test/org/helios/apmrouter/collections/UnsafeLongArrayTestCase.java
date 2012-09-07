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

import org.helios.apmrouter.collections.UnsafeLongArray;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

/**
 * <p>Title: UnsafeLongArrayTestCase</p>
 * <p>Description: Test cases for {@link UnsafeLongArray}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.org.helios.apmrouter.collections.UnsafeLongArrayTestCase</code></p>
 */

public class UnsafeLongArrayTestCase {
	
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
	
	/** A random */
	protected final Random RANDOM = new Random(System.currentTimeMillis());
	
	/**
	 * Generates a long array of the specified size populated with random longs
	 * @param size The size of the array
	 * @param abs If true, the longs will be positive only
	 * @return the allocated test data array
	 */
	protected long[] allocateRandom(int size, boolean abs) {
		long[] arr = new long[size];
		for(int i = 0; i < size; i++) {
			arr[i] = abs ? Math.abs(RANDOM.nextLong()) : RANDOM.nextLong();
		}
		return arr;
	}
	
	/** Auto destroyed test ula */
	protected UnsafeLongArray ula = null;

	
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
		ula = new UnsafeLongArray();
		Assert.assertEquals(UnsafeLongArray.DEFAULT_ALLOC, ula.capacity());
		Assert.assertEquals(0, ula.size());
		long[] defValues = new long[UnsafeLongArray.DEFAULT_ALLOC];
		Arrays.fill(defValues, Long.MAX_VALUE);
		Assert.assertArrayEquals(defValues, ula.getAllocatedArray());
		Assert.assertArrayEquals(new long[]{}, ula.getArray());
	}
	
	/**
	 * Tests a sized ctor allocation
	 */
	@Test
	public void testSizedAllocation() {
		ula = new UnsafeLongArray(31, 0L);
		Assert.assertEquals(31, ula.capacity());
		Assert.assertEquals(0, ula.size());
		long[] defValues = new long[ula.capacity()];
		Arrays.fill(defValues, 0);
		Assert.assertArrayEquals(defValues, ula.getAllocatedArray());
		Assert.assertArrayEquals(new long[]{}, ula.getArray());
	}
	
	/**
	 * Tests an array based allocation
	 */
	@Test
	public void testArrayAllocation() {
		final long[] ARR_DATA = allocateRandom(31, false);		
		ula = new UnsafeLongArray(ARR_DATA);
		Assert.assertEquals(31, ula.capacity());
		Assert.assertEquals(31, ula.size());
		Assert.assertArrayEquals(ARR_DATA, ula.getAllocatedArray());
		Assert.assertArrayEquals(ARR_DATA, ula.getArray());
	}
	
	/**
	 * Tests a series of value add operations
	 */
	@Test
	public void testAddOperations() {
		
	}

}
