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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import org.helios.apmrouter.collections.ConcurrentLongSortedSet;
import org.helios.apmrouter.collections.UnsafeArrayBuilder;
import org.helios.apmrouter.collections.UnsafeLongArray;
import org.helios.apmrouter.util.SystemClock;
import org.helios.apmrouter.util.SystemClock.ElapsedTime;
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

	static int sampleSize = 1500000;
//	static int sampleSize = 2500;
	
	/** A sample large-ish test array */
	static final long[] LARGE_TEST_ARR;
	static final Set<Long> LARGE_TEST_SET;
	
	static {  // Initializes the test array
		Random r = new Random(System.currentTimeMillis());
		Set<Long> set = new HashSet<Long>();
		for(int i = 0; i < sampleSize; i++) {
			set.add(r.nextLong());
		}
		LARGE_TEST_SET = new HashSet<Long>(set);
		LARGE_TEST_ARR = new long[LARGE_TEST_SET.size()];
		int cnt = 0;
		for(long v: LARGE_TEST_SET) {
			LARGE_TEST_ARR[cnt] = v;
			cnt++;
		}			
	}
	
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
	
	public static void main(String[] args) {
		final int SAMPLE_SIZE = LARGE_TEST_SET.size();
		final int THREADCOUNT = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
		final int LOOPCOUNT = 1;
		log("MultiThreaded Perftest. \n\tSample Size:" + SAMPLE_SIZE + "\n\tThread Count:" + THREADCOUNT + "\n\tLoop Count:" + LOOPCOUNT);
		
		
		log("Starting ConcurrentSkipListSet Test: ThreadCount:" + THREADCOUNT);
		final ConcurrentSkipListSet csl = new ConcurrentSkipListSet(LARGE_TEST_SET);
		final AtomicLong cslMatches = new AtomicLong(0L);
		final CountDownLatch startLatch = new CountDownLatch(1);
		final CountDownLatch endLatch = new CountDownLatch(THREADCOUNT);
		for(int i = 0; i < THREADCOUNT; i++) {
			Thread t = new Thread() {
				public void run() {
					try {startLatch.await();} catch (Exception ex) { throw new RuntimeException(); }
					//log("Started CSL Thread");
					int matches = 0;
					for(long v: LARGE_TEST_ARR) {
						matches += csl.contains(v) ? 1 : 0;
					}
					cslMatches.addAndGet(matches);
					endLatch.countDown();
				}
			};
			t.setDaemon(true);
			t.start();
		}
		SystemClock.startTimer();
		startLatch.countDown();
		try {endLatch.await();} catch (Exception ex) { throw new RuntimeException(ex); }
		ElapsedTime et = SystemClock.endTimer();
		log("ConcurrentSkipListSet:" + et);
		log("Starting ConcurrentLongSortedSet Test: ThreadCount:" + THREADCOUNT);

		
		final ConcurrentLongSortedSet ula = new ConcurrentLongSortedSet(LARGE_TEST_ARR);
		final AtomicLong ulaMatches = new AtomicLong(0L);
		final CountDownLatch ulaStartLatch = new CountDownLatch(1);
		final CountDownLatch ulaEndLatch = new CountDownLatch(THREADCOUNT);
		log("Warmup");
		int matches = 0;
		for(long v: LARGE_TEST_ARR) {
			matches += ula.contains(v) ? 1 : 0;
		}
		assert SAMPLE_SIZE == matches;
		log("Warmup Complete");
		for(int i = 0; i < THREADCOUNT; i++) {
			Thread t = new Thread() {
				public void run() {
					try {ulaStartLatch.await();} catch (Exception ex) { throw new RuntimeException(); }
					//log("Started ULA Thread");
					int matches = 0;
					for(long v: LARGE_TEST_ARR) {
						matches += ula.contains(v) ? 1 : 0;
					}
					ulaMatches.addAndGet(matches);
					ulaEndLatch.countDown();
					//log("ULA Thread:" + matches);
				}
			};
			t.setDaemon(true);
			t.start();
		}
		SystemClock.startTimer();
		ulaStartLatch.countDown();
		try {ulaEndLatch.await();} catch (Exception ex) { throw new RuntimeException(ex); }
		et = SystemClock.endTimer();
		log("ULA:" + et);
		log("CSL Matches:" + cslMatches.get() + "   ULA Matches:" + ulaMatches.get());
	}

}
