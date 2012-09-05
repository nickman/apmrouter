/**
 * ICE Futures, US
 */
package test.org.helios.apmrouter.performance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

/**
 * <p>Title: BenchmarkRunnable</p>
 * <p>Description: Articulated runnable for runniong benchmarks</p> 
 * <p>Company: ICE Futures US</p>
 * @author Whitehead (nicholas.whitehead@theice.com)
 * @version $LastChangedRevision$
 * <p><code>test.org.helios.apmrouter.performance.BenchmarkRunnable</code></p>
 */

public class BenchmarkRunnable implements Callable<ThreadBenchmarkResult> {
	/** The thread benchmark result */
	private ThreadBenchmarkResult benchmark;
	/** The number of profiled operations to be executed */
	private int opCount;
	/** The delegate runnable */
	private final Runnable runnable;
	/** The completion latch */
	private CountDownLatch completionLatch;
	
	/**
	 * Creates a new BenchmarkRunnable
	 * @param opCount The number of profiled operations to be executed
	 * @param runnable The delegate runnable
	 * @param completionLatch The latch to drop on completion
	 */
	public BenchmarkRunnable(int opCount, Runnable runnable, CountDownLatch completionLatch) {
		this.opCount = opCount;				
		this.runnable = runnable;
		this.completionLatch = completionLatch;
	}

	/**
	 * Creates a new collection of BenchmarkRunnable tasks for execution
	 * @param taskCount The number of tasks which is the expected number of threads that will be executing
	 * @param opCount The number of profiled operations that will be executed in each thread
	 * @param runnable The actual runnable task that implements the benchmark
	 * @return a collection of BenchmarkRunnable tasks 
	 */
	public static Collection<BenchmarkRunnable> newTaskCollection(int taskCount, int opCount, Runnable runnable) {
		Collection<BenchmarkRunnable> tasks = new ArrayList<BenchmarkRunnable>(taskCount);
		CountDownLatch latch = new CountDownLatch(taskCount);
		for(int i = 0; i < taskCount; i++) {
			tasks.add(new BenchmarkRunnable(opCount, runnable, latch));
		}
		return tasks;
	}
	
	/**
	 * Resets the completion latch in each BenchmarkRunnable in the passed collection
	 * @param tasks a collection of BenchmarkRunnable tasks to reset
	 * @return the new completion latch for the passed task collection
	 */
	public static CountDownLatch reset(final Collection<BenchmarkRunnable> tasks) {
		CountDownLatch latch = new CountDownLatch(tasks.size());
		for(BenchmarkRunnable task: tasks) {
			task.completionLatch = latch;
		}
		return latch;
	}



	/**
	 * {@inheritDoc} 
	 * @see java.util.concurrent.Callable#call()
	 */
	@Override
	public ThreadBenchmarkResult call() {
		benchmark = new ThreadBenchmarkResult(opCount);
		try {
			runnable.run();			
		} finally {
			benchmark.end();
			completionLatch.countDown();			
		}
		return benchmark;
	}

}
