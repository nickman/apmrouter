/**
 * ICE Futures, US
 */
package test.org.helios.apmrouter.performance;

import java.lang.management.MemoryUsage;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;

import org.helios.apmrouter.trace.TracerFactory;
import org.helios.apmrouter.util.SystemClock;
import org.helios.apmrouter.util.SystemClock.ElapsedTime;
import org.junit.Test;

/**
 * <p>Title: BaseTracingOperations</p>
 * <p>Description: Defines the base tracing operations for the microbenchmarks</p> 
 * <p>Company: ICE Futures US</p>
 * @author Whitehead (nicholas.whitehead@theice.com)
 * @version $LastChangedRevision$
 * <p><code>test.org.helios.apmrouter.performance.BaseTracingOperations</code></p>
 */

public class BaseTracingOperations extends BasePerformanceTestCase {
	
	/**
	 * Executes a microbenchmark for tracing native longs
	 * @param threadCount The thread count where each thread executes a full op loop
	 * @param uniqueMetricCount The number of unique metric names
	 * @param wordCount The number of words per namespace
	 * @param wordSize The word size of each metric name and namespace
	 * @param opCount The numberof times each unique metric will be traced
	 * @param warmupCount The number of times to run the op loop for JIT warmup
	 */
	@Test
	public void TraceLongPerformance(final int threadCount, final int uniqueMetricCount, final int wordCount, final int wordSize, final int opCount, final int warmupCount) {
		ThreadPoolExecutor es = newExecutorService(name.getMethodName(), threadCount);
		final String[] names = new String[uniqueMetricCount];
		for(int i = 0; i < wordCount; i++) {
			names[i] = randomWords(1, wordSize)[0];
		}				
		final String[][] namespaces = new String[uniqueMetricCount][];
		for(int i = 0; i < uniqueMetricCount; i++) {
			namespaces[i] = randomWords(wordCount, wordSize);
		}
		
		Runnable workLoadProfile = new Runnable() {
			public void run() {
				for(int i = 0; i < opCount; i++) {
					for(int x = 0; x < uniqueMetricCount; x++) {
						TracerFactory.getTracer().traceLong(RANDOM.nextInt(), names[i], namespaces[i]);
					}
				}				
			}
		};
		final CountDownLatch completionLatch = new CountDownLatch(threadCount);
		BenchmarkRunnable[] tasks = new BenchmarkRunnable[threadCount];
		for(int i = 0; i < threadCount; i++) {
			tasks[i] = new BenchmarkRunnable(opCount, workLoadProfile, completionLatch); 
		}
		try {
			log("Running warmup for [" + name.getMethodName() + "]");
			for(int w = 0; w < warmupCount; w++) {
				for(int i = 0; i < opCount; i++) {
					for(int x = 0; x < uniqueMetricCount; x++) {
						TracerFactory.getTracer().traceLong(RANDOM.nextInt(), names[i], namespaces[i]);
					}
				}
			}
			log("Running [" + name.getMethodName() + "]");
			SystemClock.startTimer();
			for(int i = 0; i < opCount; i++) {
				for(int x = 0; x < uniqueMetricCount; x++) {
					TracerFactory.getTracer().traceLong(RANDOM.nextInt(), names[i], namespaces[i]);
				}
			}
			ElapsedTime et = SystemClock.endTimer();
			Arrays.fill(names, null);
			Arrays.fill(namespaces,null);
			es.shutdownNow();
			es = null;
			MemoryUsage[] usages = memoryUsage(true);
		} finally {
			if(es!=null) try { es.shutdownNow(); } catch (Exception e) {}
			es = null;
			Arrays.fill(names, null);
			Arrays.fill(namespaces,null);
		}
	}
	
	
}
