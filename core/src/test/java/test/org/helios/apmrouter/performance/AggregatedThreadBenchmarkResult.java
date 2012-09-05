/**
 * ICE Futures, US
 */
package test.org.helios.apmrouter.performance;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import org.helios.apmrouter.util.SystemClock.AggregatedElapsedTime;
import org.helios.apmrouter.util.SystemClock.ElapsedTime;

/**
 * <p>Title: AggregatedThreadBenchmarkResult</p>
 * <p>Description: An aggregator for a collection of {@link ThreadBenchmarkResult}s </p> 
 * <p>Company: ICE Futures US</p>
 * @author Whitehead (nicholas.whitehead@theice.com)
 * @version $LastChangedRevision$
 * <p><code>test.org.helios.apmrouter.performance.AggregatedThreadBenchmarkResult</code></p>
 */

public class AggregatedThreadBenchmarkResult {
	/** The aggregated elapsed time */
	public final AggregatedElapsedTime aggregatedElapsedTime;
	/** The aggregated average of the average per op ms. */
	public final long averageAvgPerOpMs;
	/** The aggregated average of the min per op ms. */
	public final long averageMinPerOpMs;
	/** The aggregated average of the max per op ms. */
	public final long averageMaxPerOpMs;
	/** The aggregated average of the average per op ns. */
	public final long averageAvgPerOpNs;
	/** The aggregated average of the min per op ns. */
	public final long averageMinPerOpNs;
	/** The aggregated average of the max per op ns. */
	public final long averageMaxPerOpNs;
	
	/** The average number of blocks */
	public final long averageBlocks;
	/** The min number of blocks */
	public final long minBlocks;
	/** The max number of blocks */
	public final long maxBlocks;
	/** The average block time */
	public final long averageBlockTime;
	/** The min block time */
	public final long minBlockTime;
	/** The max block time */
	public final long maxBlockTime;
	
	/** The average number of waits */
	public final long averageWaits;
	/** The min number of waits */
	public final long minWaits;
	/** The max number of waits */
	public final long maxWaits;
	/** The average wait time */
	public final long averageWaitTime;
	/** The min wait time */
	public final long minWaitTime;
	/** The max wait time */
	public final long maxWaitTime;
	
	/** The average cpu time */
	public final long averageCpuTime;
	/** The min cpu time */
	public final long minCpuTime;
	/** The max cpu time */
	public final long maxCpuTime;
	
	/** The number of long values */
	public static final long LONG_VALUE_CNT = 21;
	
	/**
	 * Creates a new AggregatedThreadBenchmarkResult
	 * @param time The aggregated elapsed time
	 * @param values The long values to initialize with
	 */
	private AggregatedThreadBenchmarkResult(AggregatedElapsedTime time, long[] values) {
		aggregatedElapsedTime = time;
		int index = 0;
		averageAvgPerOpMs = values[index++];
		averageMinPerOpMs = values[index++];
		averageMaxPerOpMs = values[index++];
		averageAvgPerOpNs = values[index++];
		averageMinPerOpNs = values[index++];
		averageMaxPerOpNs = values[index++];
		averageBlocks = values[index++];
		minBlocks = values[index++];
		maxBlocks = values[index++];
		averageBlockTime = values[index++];
		minBlockTime = values[index++];
		maxBlockTime = values[index++];
		averageWaits = values[index++];
		minWaits = values[index++];
		maxWaits = values[index++];
		averageWaitTime = values[index++];
		minWaitTime = values[index++];
		maxWaitTime = values[index++];
		averageCpuTime = values[index++];
		minCpuTime = values[index++];
		maxCpuTime = values[index++];
		
	}
	
	
	/**
	 * Returns an aggregate of the passed ThreadBenchmarkResult
	 * @param results the ThreadBenchmarkResult to aggregate
	 * @return the aggregate
	 */
	public static AggregatedThreadBenchmarkResult aggregate(ThreadBenchmarkResult...results) {
		long[] args = new long[21];
		AggregatedElapsedTime aggregatedElapsedTime = AggregatedElapsedTime.aggregate(getElapsedTimes(results));
		int index = 0;
		long[] values = getAggregate("AvgPerOpMs", results);
		args[index++] = values[0];
		args[index++] = values[1];
		args[index++] = values[2];
		values = getAggregate("AvgPerOpNs", results);
		args[index++] = values[0];
		args[index++] = values[1];
		args[index++] = values[2];
		values = getAggregate("BlockCount", results);
		args[index++] = values[0];
		args[index++] = values[1];
		args[index++] = values[2];
		values = getAggregate("BlockTime", results);
		args[index++] = values[0];
		args[index++] = values[1];
		args[index++] = values[2];
		values = getAggregate("WaitCount", results);
		args[index++] = values[0];
		args[index++] = values[1];
		args[index++] = values[2];
		values = getAggregate("WaitTime", results);
		args[index++] = values[0];
		args[index++] = values[1];
		args[index++] = values[2];
		values = getAggregate("ElapsedSysCpu", results);
		args[index++] = values[0];
		args[index++] = values[1];
		args[index++] = values[2];
		
		return new AggregatedThreadBenchmarkResult(aggregatedElapsedTime, args);
		
	}
	
	/**
	 * Returns all the elapsed times of the passed ThreadBenchmarkResults in an array
	 * @param results The ThreadBenchmarkResult to array the elapsed times for
	 * @return an array of elapsed times
	 */
	protected static ElapsedTime[] getElapsedTimes(ThreadBenchmarkResult...results) {
		ElapsedTime[] times = new ElapsedTime[results.length];
		for(int i = 0; i < results.length; i++) {
			times[i] = results[i].getElapsedTime();
		}
		return times;
	}
	
	/**
	 * Returns the aggregate summary for the named value from the passed ThreadBenchmarkResults
	 * @param name The method accessor name
	 * @param results the results to aggregate from
	 * @return An array of longs containing <b><code>AVG, MIN, MAX</code></b>
	 */
	protected static long[] getAggregate(String name, ThreadBenchmarkResult...results) {
		try {
			long[] values = new long[results.length];
			Method method = ThreadBenchmarkResult.class.getDeclaredMethod("get" + name);
			for(int i = 0; i < results.length; i++) {
				values[i] = ((Long)method.invoke(results[i])).longValue();
			}
			return aggregate(values);
		} catch (Exception e) {
			throw new RuntimeException("Failed to collect aggregate base value for [" + name + "]", e);
		}
	}
	
	
	
	/**
	 * Creates an aggregate of the passed long values
	 * @param values the longs to aggregate
	 * @return An array of longs containing <b><code>AVG, MIN, MAX</code></b>.
	 */
	public static long[] aggregate(long...values) {
		if(values==null || values.length<1) return new long[]{0,0,0};
		long[] aggr = new long[3];
		long max = Long.MIN_VALUE;
		long min = Long.MAX_VALUE;
		int cnt = 0;
		BigDecimal total = new BigDecimal(0);
		for(long value: values) {
			if(value==-1) continue;
			total = total.add(BigDecimal.valueOf(value));
			if(value>max) max = value;
			if(value<min) min = value;
			cnt++;
		}
		aggr[0] = (cnt!=0 && total.longValue()>0) ? total.divide(BigDecimal.valueOf(cnt)).longValue() : -1;
		aggr[1] = min==Long.MAX_VALUE ? -1 : min;
		aggr[2] = max==Long.MIN_VALUE ? -1 : max;
		return aggr;
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AggregatedThreadBenchmarkResult [\n\taggregatedElapsedTime=");
		builder.append(aggregatedElapsedTime);
		builder.append("\n\taverageAvgPerOpMs=");
		builder.append(averageAvgPerOpMs);
		builder.append("\n\taverageMinPerOpMs=");
		builder.append(averageMinPerOpMs);
		builder.append("\n\taverageMaxPerOpMs=");
		builder.append(averageMaxPerOpMs);
		builder.append("\n\taverageAvgPerOpNs=");
		builder.append(averageAvgPerOpNs);
		builder.append("\n\taverageMinPerOpNs=");
		builder.append(averageMinPerOpNs);
		builder.append("\n\taverageMaxPerOpNs=");
		builder.append(averageMaxPerOpNs);
		builder.append("\n\taverageBlocks=");
		builder.append(averageBlocks);
		builder.append("\n\tminBlocks=");
		builder.append(minBlocks);
		builder.append("\n\tmaxBlocks=");
		builder.append(maxBlocks);
		builder.append("\n\taverageBlockTime=");
		builder.append(averageBlockTime);
		builder.append("\n\tminBlockTime=");
		builder.append(minBlockTime);
		builder.append("\n\tmaxBlockTime=");
		builder.append(maxBlockTime);
		builder.append("\n\taverageWaits=");
		builder.append(averageWaits);
		builder.append("\n\tminWaits=");
		builder.append(minWaits);
		builder.append("\n\tmaxWaits=");
		builder.append(maxWaits);
		builder.append("\n\taverageWaitTime=");
		builder.append(averageWaitTime);
		builder.append("\n\tminWaitTime=");
		builder.append(minWaitTime);
		builder.append("\n\tmaxWaitTime=");
		builder.append(maxWaitTime);
		builder.append("\n\taverageCpuTime=");
		builder.append(averageCpuTime);
		builder.append("\n\tminCpuTime=");
		builder.append(minCpuTime);
		builder.append("\n\tmaxCpuTime=");
		builder.append(maxCpuTime);
		builder.append("\n]");
		return builder.toString();
	}
	
	
	
}
