/**
 * ICE Futures, US
 */
package test.org.helios.apmrouter.performance;

import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.helios.apmrouter.metric.ICEMetric;
import org.helios.apmrouter.metric.MetricType;
import org.helios.apmrouter.metric.catalog.ICEMetricCatalog;
import org.helios.apmrouter.sender.SenderFactory;
import org.helios.apmrouter.trace.CollectionFunnel;
import org.helios.apmrouter.trace.ITracer;
import org.helios.apmrouter.trace.TracerFactory;
import org.helios.apmrouter.util.StringHelper;
import org.helios.apmrouter.util.SystemClock;
import org.helios.apmrouter.util.SystemClock.ElapsedTime;
import org.helios.apmrouter.util.ThreadUtils;
import org.helios.apmrouter.util.ThreadUtils.LockInfos;
import org.junit.After;
import org.junit.AfterClass;
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
	/** The number of unique metrics */
	public static final int UNIQUE_METRIC_COUNT = 1000;
	/** The namespace size */
	public static final int WORD_COUNT = 5;
	/** The size of the metric name and namespace segments */
	public static final int WORD_SIZE = 10;
	/** The number of times each metric is traced in the benchmark */
	public static final int OP_COUNT = 1000;
	/** The number of threads to execute tests in */
	public static final int THREAD_COUNT = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
	/** The number of warmup loops to execute */
	public static final int WARMUP_COUNT = 2;
	/** The total number of profiled executions */
	public static final int TOTAL_OP_COUNT = OP_COUNT * UNIQUE_METRIC_COUNT;
	/** The total number of expected executions */
	public static final int TOTAL_EXEC_COUNT = ((TOTAL_OP_COUNT*WARMUP_COUNT) + TOTAL_OP_COUNT) * THREAD_COUNT;
	
	/** The metric names */
	static final String[] names;
	/** The metric namespaces */
	static final String[][] namespaces;
	/** The metric long values */
	static final long[] longValues;
	/** The metric string values */
	static final String[] stringValues;
	/** The metric blob values */
	static final byte[][] blobValues;
	
	
	/** Indicates if the benchmark is currently in warmup */
	private static final AtomicBoolean IN_WARMUP = new AtomicBoolean(false);
	
	static {
		ManagementFactory.getThreadMXBean().setThreadContentionMonitoringEnabled(true);
		longValues = new long[UNIQUE_METRIC_COUNT];
		for(int i = 0; i < UNIQUE_METRIC_COUNT; i++) {
			longValues[i] = nextRandomInt();
		}
		stringValues = new String[UNIQUE_METRIC_COUNT];
		for(int i = 0; i < UNIQUE_METRIC_COUNT; i++) {
			stringValues[i] = StringHelper.fastConcat(randomWords(5, 10));			
		}
		log("Sample String Value [" + stringValues[0] + "]");
		blobValues = new byte[UNIQUE_METRIC_COUNT][];
		for(int i = 0; i < UNIQUE_METRIC_COUNT; i++) {
			byte[] bytes = new byte[1024];
			RANDOM.nextBytes(bytes);
			blobValues[i] = bytes;			
		}
		
		names = new String[UNIQUE_METRIC_COUNT];
		for(int i = 0; i < UNIQUE_METRIC_COUNT; i++) {
			names[i] = randomWords(1, WORD_SIZE)[0];
		}				
		namespaces = new String[UNIQUE_METRIC_COUNT][];
		for(int i = 0; i < UNIQUE_METRIC_COUNT; i++) {
			namespaces[i] = randomWords(WORD_COUNT, WORD_SIZE);
		}
		log("Total Profiled Op Count:" + TOTAL_OP_COUNT);
		log("Total Executed Op Count:" + TOTAL_EXEC_COUNT);
		MetricType.setDirect(true);
		MetricType.setCompress(false);
	}
	
	/**
	 * Prints tracing stats
	 */
	@AfterClass
	public static void printEnvSummary() {
		log("CollectionFunnel:\n " + CollectionFunnel.getInstance().status());
		log("SenderFactory Sends: " + SenderFactory.getInstance().getDefaultSender().getSentMetrics());
		log("SenderFactory Drops: " + SenderFactory.getInstance().getDefaultSender().getDroppedMetrics());
		log("SenderFactory Fails: " + SenderFactory.getInstance().getDefaultSender().getFailedMetrics());
	}
	
	/**
	 * The thread pool used for each test
	 */
	protected ThreadPoolExecutor es = null;
	
	/**
	 * Kills the thread pool if it still exists
	 */
	@After
	public void killThreadPool() {
		if(es!=null) {
			try { es.shutdownNow(); } catch (Exception ex) {}
			es=null;
		}
	}

	
	/**
	 * Executes a microbenchmark for tracing native longs
	 */
	@Test(timeout=120000)
	public void TraceLongPerformance() {
		final ITracer tracer = TracerFactory.getTracer();
		Runnable workLoadProfile = newTracingRunnable(new TracingDirective<ICEMetric>(){
			@Override
			public ICEMetric doTrace(int index, int outerIndex) {
				return tracer.traceLong(longValues[index], names[index], namespaces[index]);
			}
		});
		executeBenchmark(workLoadProfile, THREAD_COUNT, TOTAL_OP_COUNT, WARMUP_COUNT);
	}
	
	/**
	 * Executes a microbenchmark for tracing object longs
	 */
	@Test(timeout=60000)
	public void TraceObjectLongPerformance() {
		final ITracer tracer = TracerFactory.getTracer();
		Runnable workLoadProfile = newTracingRunnable(new TracingDirective<ICEMetric>(){
			@Override
			public ICEMetric doTrace(int index, int outerIndex) {
				return tracer.trace(longValues[index], names[index], MetricType.LONG,  namespaces[index]);
			}
		});
		executeBenchmark(workLoadProfile, THREAD_COUNT, TOTAL_OP_COUNT, WARMUP_COUNT);
	}
	
	/**
	 * Executes a microbenchmark for tracing long deltas
	 */
	@Test(timeout=60000)
	public void TraceDeltaLongPerformance() {
		final ITracer tracer = TracerFactory.getTracer();
		Runnable workLoadProfile = newTracingRunnable(new TracingDirective<ICEMetric>(){
			@Override
			public ICEMetric doTrace(int index, int outerIndex) {
				return tracer.traceDelta(longValues[index], names[index]+"D", namespaces[index]);
			}
		});
		executeBenchmark(workLoadProfile, THREAD_COUNT, TOTAL_OP_COUNT, WARMUP_COUNT);
	}
	
	
	/**
	 * Executes a microbenchmark for tracing strings
	 */
	@Test(timeout=60000)
	public void TraceStringPerformance() {
		final ITracer tracer = TracerFactory.getTracer();
		Runnable workLoadProfile = newTracingRunnable(new TracingDirective<ICEMetric>(){
			@Override
			public ICEMetric doTrace(int index, int outerIndex) {
				return tracer.traceString(stringValues[index], names[index]+"S",  namespaces[index]);
			}
		});
		executeBenchmark(workLoadProfile, THREAD_COUNT, TOTAL_OP_COUNT, WARMUP_COUNT);
	}
	
	
	static final AtomicReference<Thread> THREAD = new AtomicReference<Thread>(null);
	static final AtomicReference<LockInfos> LOCKINFOS = new AtomicReference<LockInfos>(null);
	
	private static void watchThread() {
		new Thread("LockInfoTracker") {
			public void run() {
				LOCKINFOS.set(ThreadUtils.trackLocksForThread(THREAD.get(), 10, TimeUnit.SECONDS, 2000));
			}
		}.start();
	}
	
	/**
	 * Executes a microbenchmark for tracing throwables
	 */
	@Test(timeout=120000)
	public void TraceErrorPerformance() {
		final ITracer tracer = TracerFactory.getTracer();
		final Throwable t = new Throwable();
		log("Throwable Stack Size:" + t.getStackTrace().length);
		
		Runnable workLoadProfile = newTracingRunnable(new TracingDirective<ICEMetric>(){
			@Override
			public ICEMetric doTrace(int index, int outerIndex) {
//				if(outerIndex==0 && index==0 && THREAD.get()==null) {
//					if(THREAD.compareAndSet(null, Thread.currentThread())) {					
//						watchThread();
//					}
//				}
				//if(index==(UNIQUE_METRIC_COUNT-1) && outerIndex > 1 && outerIndex%50==0) log("[" + Thread.currentThread() + "] Completing Error Loop. Outer Index:" + outerIndex);
				return tracer.traceError(t, names[index]+"E",  namespaces[index]);
			}
		});
		executeBenchmark(workLoadProfile, THREAD_COUNT, TOTAL_OP_COUNT, WARMUP_COUNT);
	}
	
	/**
	 * Executes a microbenchmark for tracing Dates (blobs)
	 */
	@Test(timeout=120000)
	public void TraceBlobPerformance() {
		final ITracer tracer = TracerFactory.getTracer();		
		Runnable workLoadProfile = newTracingRunnable(new TracingDirective<ICEMetric>(){
			@Override
			public ICEMetric doTrace(int index, int outerIndex) {
				return tracer.traceBlob(blobValues[index], names[index]+"B",  namespaces[index]);
			}
		});
		executeBenchmark(workLoadProfile, THREAD_COUNT, TOTAL_OP_COUNT, WARMUP_COUNT);
	}
	
	
	
	
	
	
	
	/**
	 * <p>Title: TracingDirective</p>
	 * <p>Description: Defines the individual task that comprises the profiled benchmark operation</p> 
	 * <p>Company: ICE Futures US</p>
	 * @author Whitehead (nicholas.whitehead@theice.com)
	 * @version $LastChangedRevision$
	 * <p><code>test.org.helios.apmrouter.performance.BaseTracingOperations.TracingDirective</code></p>
	 */
	protected static interface TracingDirective<T> {
		/**
		 * Executes the profiled operation
		 * @param index The index of the metric name reference
		 * @param outerIndex The index of thew op count loop
		 * @return the return value of the profiled operation
		 */
		public T doTrace(int index, int outerIndex);
	}
	
	/**
	 * Creates a new runnable task for the passed tracing directive
	 * @param td The tracing directive that will be executed by the runnable
	 * @return the runnable task
	 */
	protected Runnable newTracingRunnable(final TracingDirective<?> td) {
		return new Runnable() {
			public void run() {
				for(int i = 0; i < OP_COUNT; i++) {
					for(int x = 0; x < UNIQUE_METRIC_COUNT; x++) {
						td.doTrace(x, i);						
					}
				}
				//if(!IN_WARMUP.get()) log("Thread [" + Thread.currentThread().getName() + "] Elapsed:" + elapsed + " ns.  " + TimeUnit.MILLISECONDS.convert(elapsed, TimeUnit.NANOSECONDS)  + " ms.  Avg:" + avg + " ns.");
			}
		};		
	}
	

	
	/**
	 * Executes a microbenchmark
	 * @param workLoadProfile The runnable that executes the profiled operations
	 * @param threadCount The thread count where each thread executes a full op loop
	 * @param opCount The numberof times each unique metric will be traced
	 * @param warmupCount The number of times to run the op loop for JIT warmup
	 */
	public void executeBenchmark(final Runnable workLoadProfile, final int threadCount, final int opCount, final int warmupCount) {
		es = newExecutorService(name.getMethodName(), threadCount);
		Collection<BenchmarkRunnable> tasks = BenchmarkRunnable.newTaskCollection(threadCount, opCount, workLoadProfile);
		try {
			IN_WARMUP.set(true);
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
			IN_WARMUP.set(false);
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
				log("Catalog Size:" + ICEMetricCatalog.getInstance().size());
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
