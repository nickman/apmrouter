/**
 * ICE Futures, US
 */
package test.org.helios.apmrouter.performance;

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
	private final CountDownLatch completionLatch;
	
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
