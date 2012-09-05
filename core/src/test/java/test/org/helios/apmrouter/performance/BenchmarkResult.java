/**
 * ICE Futures, US
 */
package test.org.helios.apmrouter.performance;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.helios.apmrouter.util.ResourceHelper;
import org.helios.apmrouter.util.ResourceHelper.NamedMemoryUsage;

/**
 * <p>Title: BenchmarkResult</p>
 * <p>Description: Container class and tabulator for a benchmark test</p> 
 * <p>Company: ICE Futures US</p>
 * @author Whitehead (nicholas.whitehead@theice.com)
 * @version $LastChangedRevision$
 * <p><code>test.org.helios.apmrouter.performance.BenchmarkResult</code></p>
 */

public class BenchmarkResult {
	/** Heap and non-Heap memory usage when the test starts */
	private final MemoryUsage[] startingUsage;
	/** GC collections and times at start */
	private final Map<String, long[]> startingGc;
	
	/** The heap delta memory usage */
	private NamedMemoryUsage heapMemoryDelta = null;
	/** The non-heap delta memory usage */
	private NamedMemoryUsage nonHeapMemoryDelta = null;
	/** GC collection and time deltas at end */
	private final Map<String, long[]> deltaGc;
	
	/** The aggregated per thread metrics */
	private AggregatedThreadBenchmarkResult threadBenchmark = null;

	/**
	 * Creates a new BenchmarkResult
	 * @param startingUsage Heap and non-Heap memory usage when the test starts
	 */
	public BenchmarkResult(MemoryUsage[] startingUsage) {
		this.startingUsage = startingUsage;
		List<GarbageCollectorMXBean> collectors = ManagementFactory.getGarbageCollectorMXBeans();
		startingGc = new HashMap<String, long[]>(collectors.size());
		deltaGc = new HashMap<String, long[]>(collectors.size());
		for(GarbageCollectorMXBean gc: collectors) {
			startingGc.put(gc.getName(), new long[]{gc.getCollectionCount(), gc.getCollectionTime()});
		}
	}
	
	/**
	 * Closes the benchmark result and tabulates the results
	 * @param endingUsage The ending memory usage (heap and non-heap)
	 */
	public void end(MemoryUsage[] endingUsage, List<Future<ThreadBenchmarkResult>> threadTasks) {
		heapMemoryDelta = new NamedMemoryUsage(endingUsage[0], "Heap State");
		nonHeapMemoryDelta = new NamedMemoryUsage(endingUsage[1], "NonHeap State");
		for(GarbageCollectorMXBean gc: ManagementFactory.getGarbageCollectorMXBeans()) {
			long[] starting = startingGc.get(gc.getName());
			deltaGc.put(gc.getName(), new long[]{gc.getCollectionCount()-starting[0], gc.getCollectionTime()-starting[1]});
		}
		ThreadBenchmarkResult[] threadResults = new ThreadBenchmarkResult[threadTasks.size()];
		for(int i = 0; i < threadTasks.size(); i++) {
			try {
				threadResults[i] = threadTasks.get(i).get();
			} catch (InterruptedException e) {
				throw new RuntimeException("Interrupted Exception waiting for thread result, which should not happen...", e);
			} catch (ExecutionException e) {
				throw new RuntimeException("Execution Exception waiting for thread result, which should not happen...", e);
			}
		}
		threadBenchmark = AggregatedThreadBenchmarkResult.aggregate(threadResults);
	}

	/**
	 * Returns the heap memory delta
	 * @return the heapMemoryDelta
	 */
	public NamedMemoryUsage getHeapMemoryDelta() {
		return heapMemoryDelta;
	}

	/**
	 * Returns the non-heap memory delta
	 * @return the nonHeapMemoryDelta
	 */
	public NamedMemoryUsage getNonHeapMemoryDelta() {
		return nonHeapMemoryDelta;
	}

	/**
	 * Returns the GC activity delta
	 * @return the deltaGc
	 */
	public Map<String, long[]> getDeltaGc() {
		return deltaGc;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder("BenchmarkResult [");
		b.append("\n\t").append("Heap Memory State:");
		for(Map.Entry<String, Long> memEntry: heapMemoryDelta.getUsageMap().entrySet()) {
			b.append("\n\t\t").append(memEntry.getKey()).append(":").append(memEntry.getValue());
		}
		b.append("\n\t").append("NonHeap Memory State:");
		for(Map.Entry<String, Long> memEntry: nonHeapMemoryDelta.getUsageMap().entrySet()) {
			b.append("\n\t\t").append(memEntry.getKey()).append(":").append(memEntry.getValue());
		}
		b.append("\n\t").append("GC Activity:");
		for(Map.Entry<String, long[]> gcEntry: deltaGc.entrySet()) {
			b.append("\n\t\t").append(gcEntry.getKey()).append(":")
			.append("\n\t\t\tCollections:").append(gcEntry.getValue()[0])
			.append("\n\t\t\tCollection Times:").append(gcEntry.getValue()[1]);
		}
		b.append("\n\t").append("Workload Summary:\n").append(threadBenchmark);
		b.append("\n]");
		return b.toString();
	}
	
	
	
	
}
