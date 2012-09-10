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

import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.helios.apmrouter.collections.ConcurrentLongSortedSet;
import org.helios.apmrouter.collections.UnsafeArrayBuilder;
import org.helios.apmrouter.collections.UnsafeLongArray;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * <p>Title: ConcurrentLongSortedSetTestCase</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.org.helios.apmrouter.collections.ConcurrentLongSortedSetTestCase</code></p>
 */
public class ConcurrentLongSortedSetTestCase {

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	static int sampleSize = 2500000;
	
//	/** A sample large-ish test array */
//	static  long[] LARGE_TEST_ARR;
//	static Set<Long> LARGE_TEST_SET;
//	
//	static {  // Initializes the test array
//		Random r = new Random(System.currentTimeMillis());
//		Set<Long> set = new HashSet<Long>();
//		for(int i = 0; i < sampleSize; i++) {
//			set.add(r.nextLong());
//		}
//		LARGE_TEST_SET = new HashSet<Long>(set);
//		LARGE_TEST_ARR = new long[LARGE_TEST_SET.size()];
//		int cnt = 0;
//		for(long v: LARGE_TEST_SET) {
//			LARGE_TEST_ARR[cnt] = v;
//			cnt++;
//		}
//		log("Sample Size:" + cnt);
//		
//	}
	
	protected static void log(Object obj) {
		System.out.println(obj);
	}
	

	
	@Test
	public void test() {
		final long initial = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
		Random r = new Random(System.currentTimeMillis());
		log("Starting CopyOnWriteArraySet");
		Set<Long> tmp = new HashSet<Long>(sampleSize);
		for(int i = 0; i < sampleSize; i++) tmp.add(r.nextLong());
		Set<Long> set = new ConcurrentSkipListSet<Long>(tmp);
		tmp.clear(); tmp = null;
		System.gc(); System.runFinalization(); System.gc(); 
		
		final long cheap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed()-initial;
		log("Heap Used:" + cheap);
		set = null;
		//set = null;		
		System.gc(); System.runFinalization(); System.gc(); 
		log("ConcurrentLongSortedSet");
		UnsafeLongArray u = UnsafeArrayBuilder.newBuilder().initialCapacity(sampleSize).buildLongArray();
		for(int i = 0; i < sampleSize; i++) u.append(r.nextLong());
		u.sort();
		ConcurrentLongSortedSet ula = new ConcurrentLongSortedSet(u.getArray());
		u.destroy(); u = null;
		
		System.gc(); System.runFinalization(); System.gc();
		log("Heap Used:" + (ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed()-initial));
//		Assert.assertEquals(hits, chits);
//		Assert.assertEquals(set.size(), ula.size());	
	}

}
