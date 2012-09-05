/**
 * ICE Futures, US
 */
package test.org.helios.apmrouter.performance;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
	/**  */
	public static final int UNIQUE_METRIC_COUNT = 1000;
	/**  */
	public static final int WORD_COUNT = 5;
	/**  */
	public static final int WORD_SIZE = 10;
	/**  */
	public static final int OP_COUNT = 100;
	/**  */
	public static final int THREAD_COUNT = 5;
	/**  */
	public static final int WARMUP_COUNT = 5;
	
	/**
	 * Executes a microbenchmark for tracing native longs
	 */
	@Test
	public void TraceLongPerformance() {
		final String[] names;
		final String[][] namespaces;
		try {
			names = new String[UNIQUE_METRIC_COUNT];
			for(int i = 0; i < WORD_COUNT; i++) {
				names[i] = randomWords(1, WORD_SIZE)[0];
			}				
			namespaces = new String[UNIQUE_METRIC_COUNT][];
			for(int i = 0; i < UNIQUE_METRIC_COUNT; i++) {
				namespaces[i] = randomWords(WORD_COUNT, WORD_SIZE);
			}
			
			Runnable workLoadProfile = new Runnable() {
				public void run() {
					for(int i = 0; i < OP_COUNT; i++) {
						for(int x = 0; x < UNIQUE_METRIC_COUNT; x++) {
							TracerFactory.getTracer().traceLong(RANDOM.nextInt(), names[i], namespaces[i]);
						}
					}				
				}
			};
			
			Arrays.fill(names, null);
			Arrays.fill(namespaces,null);			
			executeBenchmark(workLoadProfile, THREAD_COUNT, OP_COUNT * UNIQUE_METRIC_COUNT, WARMUP_COUNT);
		} finally {
		}
		
	}
	

	
	/**
	 * Executes a microbenchmark
	 * @param workLoadProfile The runnable that executes the profiled operations
	 * @param threadCount The thread count where each thread executes a full op loop
	 * @param opCount The numberof times each unique metric will be traced
	 * @param warmupCount The number of times to run the op loop for JIT warmup
	 */
	public void executeBenchmark(final Runnable workLoadProfile, final int threadCount, final int opCount, final int warmupCount) {
		ThreadPoolExecutor es = newExecutorService(name.getMethodName(), threadCount);
		Collection<BenchmarkRunnable> tasks = BenchmarkRunnable.newTaskCollection(threadCount, opCount, workLoadProfile);
		try {
			log("Running warmup for [" + name.getMethodName() + "]");
			for(int w = 0; w < warmupCount; w++) {
				CountDownLatch latch = BenchmarkRunnable.reset(tasks);
				try {
					es.invokeAll(tasks);
					if(!latch.await(1, TimeUnit.MINUTES)) {
						throw new Exception("Benchmark task timed out", new Throwable());
					}
				} catch (Exception e) {
					throw new RuntimeException("Benchmark Loop Failure", e);
				}
			}
			log("\nWarmup Complete.\nRunning [" + name.getMethodName() + "]");
			SystemClock.startTimer();
			CountDownLatch latch = BenchmarkRunnable.reset(tasks);
			List<Future<ThreadBenchmarkResult>> completedTasks = null;
			BenchmarkResult benchmarkResult = new BenchmarkResult(memoryUsage(true)); 
			try {
				completedTasks = es.invokeAll(tasks);
				if(!latch.await(1, TimeUnit.MINUTES)) {
					throw new Exception("Benchmark task timed out", new Throwable());
				}
				ElapsedTime et = SystemClock.endTimer();
				log("Benchmark Complete [" + name.getMethodName() + "] in [" + et + "]");
				benchmarkResult.end(memoryUsage(false), completedTasks);
				if(getThreadPoolExceptionCount(es) >0) {
					throw new RuntimeException("Benchmark Loop Failures Detected in Thread Pool [" + getThreadPoolExceptionCount(es) + "]", new Throwable());
				}
				log("Benchmark Results [" + name.getMethodName() + "]:\n" + benchmarkResult);
			} catch (Exception e) {
				throw new RuntimeException("Benchmark Loop Failure", e);
			}
			es.shutdownNow();
			es = null;
		} finally {
			if(es!=null) try { es.shutdownNow(); } catch (Exception e) {}
			es = null;
		}
	}
	
	
}
