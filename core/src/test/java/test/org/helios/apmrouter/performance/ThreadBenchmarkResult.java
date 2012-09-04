/**
 * ICE Futures, US
 */
package test.org.helios.apmrouter.performance;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;

import org.helios.apmrouter.util.SystemClock;
import org.helios.apmrouter.util.SystemClock.ElapsedTime;

/**
 * <p>Title: ThreadBenchmarkResult</p>
 * <p>Description: Container class for the results of a bechmark within one thread</p> 
 * <p>Company: ICE Futures US</p>
 * @author Whitehead (nicholas.whitehead@theice.com)
 * @version $LastChangedRevision$
 * <p><code>test.org.helios.apmrouter.performance.ThreadBenchmarkResult</code></p>
 */

public class ThreadBenchmarkResult {
	
	/** Indicates if this JVM supports current cpu time per thread */
	public static final boolean CPU_TIME_SUPPORTED = ManagementFactory.getThreadMXBean().isCurrentThreadCpuTimeSupported();
	/** Indicates if this JVM supports thread contention monitoring */
	public static final boolean THREAD_CONTENTION_SUPPORTED = ManagementFactory.getThreadMXBean().isThreadContentionMonitoringSupported();

	/** The captured elapsed time */
	protected ElapsedTime elapsedTime = null;
	/** The starting thread info for the current thread */
	protected final ThreadInfo startingThreadInfo;
	/** The starting system cpu time for this thread */
	protected final long startingSysCpu;
	/** The elapsed system cpu time for this thread */
	protected long elapsedSysCpu;
	/** The thread ID */
	protected final long threadId;
	/** The number of blocks */
	protected long blockCount = -1L; 
	/** The number of waits */
	protected long waitCount = -1L; 
	/** The block time */
	protected long blockTime = -1L; 
	/** The wait time */
	protected long waitTime = -1L; 
	/** The op count */
	protected final int opCount; 
	
	/**
	 * Initializes the ThreadBenchmarkResult
	 * @param opCount The number of profiled ops to be executed in this thread
	 */
	public ThreadBenchmarkResult(int opCount) {
		this.opCount = opCount;
		threadId = Thread.currentThread().getId();
		startingThreadInfo = ManagementFactory.getThreadMXBean().getThreadInfo(threadId);
		startingSysCpu = CPU_TIME_SUPPORTED && ManagementFactory.getThreadMXBean().isThreadCpuTimeEnabled() ? ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime() : -1L;  
		SystemClock.startTimer();
	}
	
	/**
	 * Closes the benchmark collection 
	 */
	public void end() {
		elapsedTime = SystemClock.endTimer();
		elapsedSysCpu = CPU_TIME_SUPPORTED && ManagementFactory.getThreadMXBean().isThreadCpuTimeEnabled() ? (ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime()-startingSysCpu) : -1L;
		blockCount = ManagementFactory.getThreadMXBean().getThreadInfo(threadId).getBlockedCount() - startingThreadInfo.getBlockedCount();
		waitCount = ManagementFactory.getThreadMXBean().getThreadInfo(threadId).getWaitedCount() - startingThreadInfo.getWaitedCount();
		if(THREAD_CONTENTION_SUPPORTED && ManagementFactory.getThreadMXBean().isThreadContentionMonitoringEnabled()) {
			blockTime = ManagementFactory.getThreadMXBean().getThreadInfo(threadId).getBlockedTime() - startingThreadInfo.getBlockedTime();
			waitTime = ManagementFactory.getThreadMXBean().getThreadInfo(threadId).getBlockedTime() - startingThreadInfo.getWaitedTime();
		}
	}

	/**
	 * Returns the elapsed time object
	 * @return the elapsed time object.
	 */
	public ElapsedTime getElapsedTime() {
		return elapsedTime;
	}
	
	/**
	 * Returns the elapsed time in ms.
	 * @return the elapsed time in ms.
	 */
	public long getElapsedTimeMs() {
		return elapsedTime.elapsedMs;
	}
	
	/**
	 * Returns the elapsed time in ns.
	 * @return the elapsed time in ns.
	 */
	public long getElapsedTimeNs() {
		return elapsedTime.elapsedNs;
	}
	
	/**
	 * Returns the average elapsed time of a single op in ms.
	 * @return the average elapsed time of a single op in ms.
	 */
	public long getAvgPerOpMs() {
		return elapsedTime.avgMs(opCount);
	}
	
	/**
	 * Returns the average elapsed time of a single op in ns.
	 * @return the average elapsed time of a single op in ns.
	 */
	public long getAvgPerOpNs() {
		return elapsedTime.avgNs(opCount);
	}
	

	/**
	 * The elapsed CPU time for this thread.
	 * @return The elapsed CPU time for this thread or -1 if not supported
	 */
	public long getElapsedSysCpu() {
		return elapsedSysCpu;
	}

	/**
	 * Returns the tota block count for this thread
	 * @return the tota block count for this thread
	 */
	public long getBlockCount() {
		return blockCount;
	}

	/**
	 * Returns the tota wait count for this thread
	 * @return the tota wait count for this thread
	 */
	public long getWaitCount() {
		return waitCount;
	}

	/**
	 * Returns the tota block time for this thread in ms. unless not enabled in which case will return -1.
	 * @return the tota block time for this thread in ms.
	 */
	public long getBlockTime() {
		return blockTime;
	}

	/**
	 * Returns the tota wait time for this thread in ms. unless not enabled in which case will return -1.
	 * @return the tota wait time for this thread in ms.
	 */
	public long getWaitTime() {
		return waitTime;
	}

	/**
	 * {@inheritDoc} 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ThreadBenchmarkResult [");
		builder.append("\n\tElapsedTimeMs=");
		builder.append(getElapsedTimeMs());
		builder.append("\n\tElapsedTimeNs=");
		builder.append(getElapsedTimeNs());
		builder.append("\n\tAvgPerOpMs=");
		builder.append(getAvgPerOpMs());
		builder.append("\n\tAvgPerOpNs=");
		builder.append(getAvgPerOpNs());
		builder.append("\n\tElapsedSysCpu=");
		builder.append(getElapsedSysCpu());
		builder.append("\n\tBlockCount=");
		builder.append(getBlockCount());
		builder.append("\n\tWaitCount=");
		builder.append(getWaitCount());
		builder.append("\n\tBlockTime=");
		builder.append(getBlockTime());
		builder.append("\n\tWaitTime=");
		builder.append(getWaitTime());
		builder.append("\n]");
		return builder.toString();
	}
	
	
	
}
