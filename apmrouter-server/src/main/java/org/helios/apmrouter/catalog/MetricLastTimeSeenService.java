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
package org.helios.apmrouter.catalog;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.helios.apmrouter.collections.ConcurrentLongSlidingWindow;
import org.helios.apmrouter.collections.ConcurrentLongSortedSet;
import org.helios.apmrouter.collections.LongSlidingWindow;
import org.helios.apmrouter.server.ServerComponentBean;
import org.helios.apmrouter.util.SystemClock;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;

/**
 * <p>Title: MetricLastTimeSeenService</p>
 * <p>Description: Service that maintains a skip-list map of metric-ids and the last time-stamp they were seen.
 * Intended to support event broadcasts when metrics go stale.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.catalog.MetricLastTimeSeenService</code></p>
 */

public class MetricLastTimeSeenService extends ServerComponentBean implements Runnable {
	/** A map of second timestamps keyed by the metric-id */
	protected final ConcurrentSkipListMap<Integer, ConcurrentLongSortedSet> metricTimestampMap = new ConcurrentSkipListMap<Integer, ConcurrentLongSortedSet>();
	/** The stale metric event handler */
	protected StaleMetricEventProcessor staleEventProcessor = null;
	/** The stale threshold level in seconds. Default is {@link #DEFAULT_STALE_THRESHOLD} */
	protected int staleThreshold = DEFAULT_STALE_THRESHOLD;
	/** The stale window size in seconds. Last seen entries are added into a sliding window of buckets of this size. 
	 * Larger window sizes offer beter performance at the cost of stale detection precision. Default is {@link #DEFAULT_STALE_THRESHOLD} */
	protected int windowSize = DEFAULT_WINDOW_SIZE;
	/** The scheduler to perform scans */
	protected final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, new ThreadFactory(){
		final AtomicInteger serial = new AtomicInteger(0);
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "LastTimeSeenScanThread#" + serial.incrementAndGet());
			t.setDaemon(true);
			return t;
		}
	});
	/** The handle of the scheduled task */
	protected ScheduledFuture<?> taskHandle = null;
	
	/** The default stale threshold level in seconds */
	public static int DEFAULT_STALE_THRESHOLD = 90;
	/** The default windoiws in seconds */
	public static int DEFAULT_WINDOW_SIZE = 15;
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStart()
	 */
	@Override
	protected void doStart() throws Exception {
		taskHandle = scheduler.scheduleWithFixedDelay(this, windowSize, windowSize, TimeUnit.SECONDS);
		super.doStart();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStop()
	 */
	@Override
	protected void doStop() {
		if(taskHandle!=null) {
			taskHandle.cancel(true);
		}
	}
	
	/**
	 * The scheduler callback
	 */
	@Override
	public void run() {
		scan();
	}
	
	/**
	 * Updates the metric timestamp map when a metric is seen.
	 * @param metricIds The ids of the metrics to add
	 * @param timestamp The time the metric was seen as a UTC long timestamo
	 */	
	public void onMetricSeen(long timestamp, long...metricIds) {
		try {
			SystemClock.startTimer();
			int timeInSecs = (int)TimeUnit.SECONDS.convert(timestamp, TimeUnit.MILLISECONDS);
			int timeWindow = timeInSecs + timeInSecs%windowSize; 
			
			ConcurrentLongSortedSet metrics = metricTimestampMap.get(timeWindow);
			if(metrics==null) {
				synchronized(metricTimestampMap) {
					metrics = metricTimestampMap.get(timeWindow);
					if(metrics==null) {
						metrics = new ConcurrentLongSortedSet();
						metricTimestampMap.put(timeWindow, metrics);
					}
				}
			}		
			metrics.add(metricIds);
		} finally {
			lastSeenTimesNs.insert(SystemClock.endTimer().elapsedNs);
		}
	}
	
	/**
	 * Clears metric Ids from older time windows when a metric is seen now
	 * @param timeWindow The time window in which a metric was most recently seen
	 * @param metricIds The metric Ids that were seen in the passed time window
	 */
	protected void clearOlderEntries(int timeWindow, long...metricIds) {
		long startTime = System.nanoTime();
		for(Entry<Integer, ConcurrentLongSortedSet> entry: metricTimestampMap.headMap(timeWindow).entrySet()) {
			entry.getValue().remove(metricIds);
		}
		long elapsedTime = System.nanoTime()-startTime;
		coeTimesNs.insert(elapsedTime);
	}
	
	/**
	 * Scans for the oldest window in {@link #metricTimestampMap}. 
	 * If the window is stale, stale events will be broadcast for all the assoiated metric Ids, and the window will be removed.
	 * If the oldest window is not stale, returns without action.
	 */
	public void scan() {
		debug("Scanning for stale windows");
		try {
			SystemClock.startTimer();
			int staleTime = (int)(SystemClock.currentClock().unixTime()-staleThreshold);
			
			Entry<Integer, ConcurrentLongSortedSet> entry = metricTimestampMap.ceilingEntry(staleTime);
			if(entry== null) {
				debug("No stale window found");
				return;
			}
			int lastSeen = entry.getKey();
			metricTimestampMap.remove(lastSeen);
			ConcurrentLongSortedSet staleIds = entry.getValue();
			for(int i = 0; i < staleIds.size(); i++) {
				long metricId = staleIds.get(i);
				if(staleEventProcessor!=null) {
					staleEventProcessor.onStaleMetric(metricId, lastSeen);
				}
			}
			incr("StaleMetricEvents", staleIds.size());
		} finally {
			scanTimesNs.insert(SystemClock.endTimer().elapsedNs);
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponent#getSupportedMetricNames()
	 */
	@Override
	public Set<String> getSupportedMetricNames() {
		Set<String> metrics = new HashSet<String>(super.getSupportedMetricNames());
		metrics.add("StaleMetricEvents");
		return metrics;
	}
	
	/** Sliding windows of scan times in ns. */
	protected final LongSlidingWindow scanTimesNs = new ConcurrentLongSlidingWindow(15);
	/** Sliding windows of last seen processing times in ns. */
	protected final LongSlidingWindow lastSeenTimesNs = new ConcurrentLongSlidingWindow(15);
	/** Sliding windows of clearing older entries (COE) processing times in ns. */
	protected final LongSlidingWindow coeTimesNs = new ConcurrentLongSlidingWindow(15);
	
	/**
	 * Returns the last scan time in ns.
	 * @return the last scan time in ns.
	 */
	@ManagedMetric(category="LastScanTimeNs", metricType=MetricType.GAUGE, description="The last scan time in ns.")
	public long getLastScanTimeNs() {
		return scanTimesNs.isEmpty() ? -1L : scanTimesNs.get(0);
	}

	/**
	 * Returns the rolling average scan time in ns.
	 * @return the rolling average scan time in ns.
	 */
	@ManagedMetric(category="AverageScanTimeNs", metricType=MetricType.GAUGE, description="The rolling average scan time in ns.")
	public long getAverageScanTimeNs() {
		return scanTimesNs.isEmpty() ? -1L : scanTimesNs.avg();
	}
	
	/**
	 * Returns the last last-seen processing time in ns.
	 * @return the last last-seen processing time in ns.
	 */
	@ManagedMetric(category="LastLastSeenTimeNs", metricType=MetricType.GAUGE, description="The last last-seen processing time in ns.")
	public long getLastLastSeenTimeNs() {
		return lastSeenTimesNs.isEmpty() ? -1L : lastSeenTimesNs.get(0);
	}

	/**
	 * Returns the rolling average last-seen processing time in ns.
	 * @return the rolling average last-seen processing time in ns.
	 */
	@ManagedMetric(category="AverageLastSeenTimeNs", metricType=MetricType.GAUGE, description="The rolling average last-seen processing time in ns.")
	public long getAverageLastSeenTimeNs() {
		return lastSeenTimesNs.isEmpty() ? -1L : lastSeenTimesNs.avg();
	}

	/**
	 * Returns the last coe processing time in ns.
	 * @return the last coe processing time in ns.
	 */
	@ManagedMetric(category="LastCOETimeNs", metricType=MetricType.GAUGE, description="The last coe processing time in ns.")
	public long getLastCOETimeNs() {
		return coeTimesNs.isEmpty() ? -1L : coeTimesNs.get(0);
	}
	
	/**
	 * Returns the rolling average coe processing time in ns.
	 * @return the rolling average coe processing time in ns.
	 */
	@ManagedMetric(category="AverageCOETimeNs", metricType=MetricType.GAUGE, description="The rolling average coe processing time in ns.")
	public long getAverageCOETimeNs() {
		return coeTimesNs.isEmpty() ? -1L : coeTimesNs.avg();
	}
	
	

	
}

























