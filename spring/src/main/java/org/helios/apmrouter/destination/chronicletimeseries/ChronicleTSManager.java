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
package org.helios.apmrouter.destination.chronicletimeseries;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Logger;
import org.helios.apmrouter.catalog.EntryStatus;
import org.helios.apmrouter.catalog.EntryStatus.EntryStatusChange;
import org.helios.apmrouter.catalog.EntryStatusChangeListener;
import org.helios.apmrouter.catalog.jdbc.h2.adapters.chronicle.ChronicleTSAdapter;
import org.helios.apmrouter.collections.ConcurrentLongSlidingWindow;
import org.helios.apmrouter.server.ServerComponentBean;
import org.helios.apmrouter.tsmodel.Tier;
import org.helios.apmrouter.tsmodel.TimeSeriesModel;
import org.helios.apmrouter.util.SystemClock;
import org.helios.apmrouter.util.SystemClock.ElapsedTime;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.support.MetricType;

/**
 * <p>Title: ChronicleTSManager</p>
 * <p>Description: Configures and manages the chronicle time-series</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.chronicletimeseries.ChronicleTSManager</code></p>
 * TODO:
 * ==============================================
 * Add status check scheduler
 * Add worker pool
 * Execute status checks across multiple threads and wait for completion
 * Invoke fireEvents asynchronously
 * Implement status changes in metric catalog
 * Add metrics to track elapsed time and number of state changes during status checks
 * Check for first/last periods in entries (which do we want ?)  Oldest should be first.....
 * Status check optimization:  oldest period in a tier should be in the tier header
 * ==============================================
 * TODO: Add support for rolling up into next tiers.
 * TODO: Add basic query functionality
 * TODO: Fill-Ins for sticky metrics ?  Physical or implied
 */

public class ChronicleTSManager extends ServerComponentBean implements UncaughtExceptionHandler, Runnable {
	/** The time series model */
	private final TimeSeriesModel timeSeriesModel;
	/** A map of the time-series chronicle-tiers keyed by the tier name */
	private final Map<String, ChronicleTier> tiers;
	/** The live tier */
	private final ChronicleTier liveTier;
	/** The number of periods in the live tier that marks a metric stale */
	protected int stalePeriods = 4;
	/** The stale window length in ms. */
	protected long staleWindowSize = -1L;
	/** The number of periods in the live tier that marks a metric offline */
	protected int offLinePeriods = 20;
	/** The offLine window length in ms. */
	protected long offLineWindowSize = -1L;
	/** Status check timeout in ms. */
	protected long statusCheckTimeout = 5000;
	

	/** Flag indicating if a status check is running */
	protected final AtomicBoolean statusCheckRunning = new AtomicBoolean(false);
	/** The manager's worker thread pool */
	protected ExecutorService threadPool = null;
	/** The manager's period scheduler */
	protected ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, new ThreadFactory(){
		final AtomicInteger serial = new AtomicInteger(0);
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "TimeSeriesScheduler#" + serial.incrementAndGet());
			t.setPriority(Thread.MAX_PRIORITY);
			t.setDaemon(true);
			t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {				
				@Override
				public void uncaughtException(Thread t, Throwable e) {
					error("Uncaught exception in TimeSeries scheduler [", t, "]", e);					
				}
			});
			return t;
		}
	});
	
	/** The number of processing threads to create */
	protected final int workerThreadCount = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
	/** A shared latch reference */
	protected final AtomicReference<CountDownLatch> latch = new AtomicReference<CountDownLatch>(null);
	/** A shared EntryStatusChange reference */
	protected final AtomicReference<Map<EntryStatus, EntryStatusChange>> changeCollector = new AtomicReference<Map<EntryStatus, EntryStatusChange>>(null);
	
	
	/** The worker tasks to execute */
	protected final StatusCheckWorker[] workers;
	
	/** A set of {@link EntryStatus} change listeners to be notified when an entry changes state */
	protected final Set<EntryStatusChangeListener> statusListeners = new CopyOnWriteArraySet<EntryStatusChangeListener>();
	
	/** Long sliding window of the elapsed times in ns. for status checks */
	protected final ConcurrentLongSlidingWindow statusCheckElapsedNs = new ConcurrentLongSlidingWindow(100); 
	/** Long sliding window of the number of entries checked in the last status checks */
	protected final ConcurrentLongSlidingWindow totalEntriesChecked = new ConcurrentLongSlidingWindow(30); 
	/** Long sliding window of the number of entries set to stale in the last status checks */
	protected final ConcurrentLongSlidingWindow totalStaleEntries = new ConcurrentLongSlidingWindow(30); 
	/** Long sliding window of the number of entries set to offline in the last status checks */
	protected final ConcurrentLongSlidingWindow totalOffLineEntries = new ConcurrentLongSlidingWindow(30); 
	/** Long sliding window of the number of exceptions in the last status checks */
	protected final ConcurrentLongSlidingWindow statusExceptions = new ConcurrentLongSlidingWindow(30); 
	
	
	
	// default ts model = p=15s,t=5m
	
	/**
	 * Sets the thread pool
	 * @param threadPool the thread pool
	 */
	public void setExecutorService(ExecutorService threadPool) {
		this.threadPool = threadPool;		
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Thread.UncaughtExceptionHandler#uncaughtException(java.lang.Thread, java.lang.Throwable)
	 */
	@Override
	public void uncaughtException(Thread t, Throwable e) {
		error("Uncaught exception in TimeSeries worker [", t, "]", e);
	}
	
	
	/**
	 * Returns the number of periods in the live tier that marks a metric stale
	 * @return the stale period count
	 */
	@ManagedAttribute(description="The number of periods in the live tier that marks a metric stale")
	public int getStalePeriods() {
		return stalePeriods;
	}

	/**
	 * Sets the number of periods in the live tier that marks a metric stale
	 * @param stalePeriods the number periods in the live tier without activity to mark a metric stale 
	 */
	@ManagedAttribute(description="The number of periods in the live tier that marks a metric stale")
	public void setStalePeriods(int stalePeriods) {
		this.stalePeriods = stalePeriods;
		if(this.isStarted()) {
			recalcStaleWindowSize();
		}
	}
	
	/**
	 * An update of a metric's entry status in the live tier triggered by the h2 metric table trigger.
	 * Since this is coming from the metric table, no event is needed.
	 * @param metricId The id of the metric to update
	 * @param status The status to update to
	 */
	public void triggeredStatusUpdate(long metricId, EntryStatus status) {
		liveTier.triggeredStatusUpdate(metricId, status);
	}
	


	/**
	 * Returns the number of periods in the live tier that marks a metric offline
	 * @return the offline period count
	 */
	@ManagedAttribute(description="The number of periods in the live tier that marks a metric offline")
	public int getOffLinePeriods() {
		return offLinePeriods;
	}

	/**
	 * Sets the number of periods in the live tier that marks a metric offline
	 * @param offLinePeriods the number periods in the live tier without activity to mark a metric offline 
	 */
	@ManagedAttribute(description="The number of periods in the live tier that marks a metric offline")
	public void setOffLinePeriods(int offLinePeriods) {
		this.offLinePeriods = offLinePeriods;
		if(this.isStarted()) {
			recalcStaleWindowSize();
		}		
	}

	
	/**
	 * Returns the calculated elapsed time of the stale window in ms.
	 * @return the calculated elapsed time of the stale window in ms.
	 */
	@ManagedAttribute(description="The calculated elapsed time of the stale window in ms.")
	public long getStaleWindowSize() {
		return staleWindowSize;
	}

	/**
	 * Returns the calculated elapsed time of the offline window in ms.
	 * @return the calculated elapsed time of the offline window in ms.
	 */
	@ManagedAttribute(description="The calculated elapsed time of the offline window in ms.")	
	public long getOffLineWindowSize() {
		return offLineWindowSize;
	}

	/**
	 * Returns the model definition the time series model was built with
	 * @return the model definition the time series model was built with
	 */
	@ManagedAttribute(description="The model definition the time series model was built with")
	public String getTimeSeriesModel() {
		return timeSeriesModel.getModelDef();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStart()
	 */
	@Override
	protected void doStart() throws Exception {
		recalcStaleWindowSize();
		recalcOffLineWindowSize();
		Thread t = new Thread() {
			@Override
			public void run() {
				info("\n\t========================\n\tCLOSING CTS\n\t========================\n");
				for(ChronicleTier ct : tiers.values()) {
					info("Closing [", ct.chronicleName, "]");
					ct.close();
				}
				info("\n\t========================\n\tCLOSED CTS\n\t========================\n");
			}
		};
		t.setDaemon(false);
		t.setPriority(Thread.MAX_PRIORITY);
		Runtime.getRuntime().addShutdownHook(t);
		ChronicleTSAdapter.setCts(this);
		scheduler.scheduleAtFixedRate(new Runnable(){
			@Override
			public void run() {
				if(!statusCheckRunning.compareAndSet(false, true)) {
					warn("StatusCheck already running when scheduled fired");
				} else {
					runStatusCheck();
				}
			}
		}, liveTier.getPeriodDuration(), liveTier.getPeriodDuration(), TimeUnit.SECONDS);
		threadPool.execute(new Runnable() {
			@Override
			public void run() {
				statusCheckRunning.set(true);
				runStatusCheck();
			}
		});
	}
	
	/**
	 * Runs an entry status check on the live tier
	 */
	@ManagedOperation(description="Runs an entry status check on the live tier")
	public void statusCheck() {
		threadPool.execute(new Runnable() {
			@Override
			public void run() {
				statusCheckRunning.set(true);
				runStatusCheck();
			}
		});
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		
	}
	
	/**
	 * Scans the live tier for stale and off line metrics
	 */
	protected void runStatusCheck() {
		debug("TimeSeries Live Tier Status Check Started");
		try {
			
			CountDownLatch cdl = new CountDownLatch(workerThreadCount);
			Map<EntryStatus, EntryStatusChange> changeMap = EntryStatusChange.getChangeMap(SystemClock.time());
			changeCollector.set(changeMap);
			latch.set(cdl);
			for(StatusCheckWorker worker: workers) {
				threadPool.execute(worker);
			}
			SystemClock.startTimer();
			try {
				if(cdl.await(statusCheckTimeout, TimeUnit.MILLISECONDS)) {
					long totalUpdates = 0;
					long totalStales = changeMap.get(EntryStatus.STALE).getMetricIds().size();
					long totalOffLines = changeMap.get(EntryStatus.OFFLINE).getMetricIds().size();		
					long totalExceptions = 0;
					for(StatusCheckWorker worker: workers) {
						totalUpdates += worker.totalUpdates;
						totalExceptions += worker.totalInvalidIndexes;
						totalExceptions += worker.totalExceptions;
					}
					totalEntriesChecked.insert(totalUpdates);
					totalStaleEntries.insert(totalStales);
					totalOffLineEntries.insert(totalOffLines);
					statusExceptions.insert(totalExceptions);
					if((totalStales + totalOffLines)>0) {
						fireEventStatusChangeEvent(changeMap);
					}

					ElapsedTime et = SystemClock.endTimer();
					statusCheckElapsedNs.insert(et.elapsedNs);
					debug("Status check complete in ", et);
				} else {
					error("Scheduler thread timed out after [", statusCheckTimeout, "] ms waiting for status check");
				}
			} catch (Exception ex) {
				error("Scheduler thread interrupted while waiting for status check", ex);
			}
		} finally {
			statusCheckRunning.set(false);
		}
	}
	
	private class StatusCheckWorker implements Runnable {
		protected final ChronicleTier tier;
		protected final int indexMod;
		protected final int workers;
		protected final Logger log;
		protected long totalUpdates = 0;
		protected long totalInvalidIndexes = 0;
		protected long totalExceptions = 0;
		
		/**
		 * Creates a new StatusCheckWorker
		 * @param tier The tier that will be checked
		 * @param indexMod The index mod that this worker handles
		 * @param workers The total number of workers
		 */
		public StatusCheckWorker(ChronicleTier tier, int indexMod, int workers) {
			super();
			log =  Logger.getLogger(getClass().getName() + ".#" + indexMod );
			this.tier = tier;
			this.indexMod = indexMod;
			this.workers = workers;
		}
		/**
		 * {@inheritDoc}
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			totalUpdates = 0;
			totalInvalidIndexes = 0;
			totalExceptions = 0;
			final Map<EntryStatus, EntryStatusChange> changeMap = changeCollector.get();
			

			try {
				final long now = SystemClock.time();
				for(long index = 0; index < tier.getSize(); index++) {
					if(index%workers == indexMod) {
						try {
							EntryStatus status = tier.statusCheck(index, now, staleWindowSize, offLineWindowSize);							
							totalUpdates++;
							if(status!=null) {
								changeMap.get(status).addMetricIds(index);
							}
						} catch (InvalidIndexExcetpion iie) {
							totalInvalidIndexes++;
						} catch (Exception ex) {
							totalExceptions++;
						}
					}
				}
			} finally {
				latch.get().countDown();
			}
		}
	}
	
	/**
	 * Creates a new ChronicleTSManager
	 * @param timeSeriesConfig The string representation of the time series configuration
	 */
	public ChronicleTSManager(String timeSeriesConfig) {
		timeSeriesModel = TimeSeriesModel.create(timeSeriesConfig);
		tiers = new HashMap<String, ChronicleTier>(timeSeriesModel.getTierCount());
		List<Tier[]> _tiers = timeSeriesModel.getModelTierPairs();
		for(int i = _tiers.size()-1; i >= 0; i--) {
			Tier[] tierPair = _tiers.get(i);
			ChronicleTier cTier = new ChronicleTier(tierPair[0], tierPair[1]==null ? null : getTier(tierPair[1].getName()), this);
			tiers.put(tierPair[0].getName(), cTier);			
		}
		liveTier = tiers.get("live");
		if(liveTier==null) throw new IllegalStateException("There was no live tier", new Throwable());
		workers = new StatusCheckWorker[workerThreadCount];
		for(int i = 0; i < workerThreadCount; i++) {
			workers[i] = new StatusCheckWorker(liveTier, i, workerThreadCount); 
		}
		
	}
	
	/**
	 * Recalculates and sets the stale window size in ms,  
	 */
	protected void recalcStaleWindowSize() {
		staleWindowSize = TimeUnit.MILLISECONDS.convert(liveTier.getPeriodDuration() * stalePeriods, TimeUnit.SECONDS);
	}
	
	/**
	 * Recalculates and sets the offline window size in ms,  
	 */
	protected void recalcOffLineWindowSize() {
		offLineWindowSize = TimeUnit.MILLISECONDS.convert(liveTier.getPeriodDuration() * offLinePeriods, TimeUnit.SECONDS);
	}
	
	/**
	 * Fires a status change event to all registered listeners
	 * @param changeMap the change map with all the status changes
	 */
	protected void fireEventStatusChangeEvent(final Map<EntryStatus, EntryStatusChange> changeMap) {
		threadPool.execute(new Runnable() {
			@Override
			public void run() {
				for(EntryStatusChangeListener listener: statusListeners) {
					listener.onEntryStatusChange(changeMap);
				}
			}
		});		
	}
	
	
	
	/**
	 * Returns the live tier 
	 * @return the live tier
	 */
	public ChronicleTier getLiveTier() {
		return liveTier;
	}
	
	/**
	 * Returns the named chronicle tier
	 * @param name The name of the tier to retrieve
	 * @return a chronicle tier
	 * @throws IllegalArgumentException thrown if the name is null, empty or does not map to a ChronicleTier
	 */
	public ChronicleTier getTier(String name) {
		if(name==null || name.trim().isEmpty()) throw new IllegalArgumentException("The passed tier name was null or empty", new Throwable());
		ChronicleTier ct = tiers.get(name.trim());
		if(ct==null) throw new IllegalArgumentException("The passed tier name [" + name + "] was invalid", new Throwable());
		return ct;
	}
	/**
	 * Registers a new {@link EntryStatusChangeListener}.
	 * @param statusListener the listener to register
	 */
	public void addStatusListener(EntryStatusChangeListener statusListener) {
		if(statusListener!=null) {
			statusListeners.add(statusListener);
		}
	}
	
	/**
	 * Removes a registered {@link EntryStatusChangeListener}.
	 * @param statusListener the listener to remove
	 */
	public void removeStatusListener(EntryStatusChangeListener statusListener) {
		if(statusListener!=null) {
			statusListeners.remove(statusListener);
		}
	}

	/**
	 * Returns the status check timeout in ms.
	 * @return the status check timeout in ms.
	 */
	@ManagedAttribute(description="The status check timeout in ms.")
	public long getStatusCheckTimeout() {
		return statusCheckTimeout;
	}

	/**
	 * Sets the status check timeout in ms.
	 * @param statusCheckTimeout the status check timeout in ms.
	 */
	@ManagedAttribute(description="The status check timeout in ms.")
	public void setStatusCheckTimeout(long statusCheckTimeout) {
		this.statusCheckTimeout = statusCheckTimeout;
	}
	
	/**
	 * Returns the elapsed time of the most recent entry status check in ns.
	 * @return the elapsed time of the most recent entry status check in ns.
	 */
	@ManagedMetric(category="ChronicleTimeSeries", displayName="LastStatusCheckTimeNs", metricType=MetricType.GAUGE, description="The elapsed time of the most recent entry status check in ns.")
	public long getLastStatusCheckTimeNs() {
		return statusCheckElapsedNs.isEmpty() ? -1L : statusCheckElapsedNs.get(0);
	}
	
	/**
	 * Returns the rolling average elapsed time of the last 100 status checks in ns.
	 * @return the rolling average elapsed time of the last 100 status checks in ns.
	 */
	@ManagedMetric(category="ChronicleTimeSeries", displayName="AverageStatusCheckTimeNs", metricType=MetricType.GAUGE, description="The rolling average elapsed time of the last 100 status checks in ns.")
	public long getAverageStatusCheckTimeNs() {
		return statusCheckElapsedNs.isEmpty() ? -1L : statusCheckElapsedNs.avg();
	}
	
	/**
	 * Returns the elapsed time of the most recent entry status check in ms.
	 * @return the elapsed time of the most recent entry status check in ms.
	 */
	@ManagedMetric(category="ChronicleTimeSeries", displayName="LastStatusCheckTimeMs", metricType=MetricType.GAUGE, description="The elapsed time of the most recent entry status check in ms.")
	public long getLastStatusCheckTimeMs() {
		return TimeUnit.MILLISECONDS.convert(getLastStatusCheckTimeNs(), TimeUnit.NANOSECONDS);
	}
	
	/**
	 * Returns the rolling average elapsed time of the last 100 status checks in ms.
	 * @return the rolling average elapsed time of the last 100 status checks in ms.
	 */
	@ManagedMetric(category="ChronicleTimeSeries", displayName="AverageStatusCheckTimeMs", metricType=MetricType.GAUGE, description="The rolling average elapsed time of the last 100 status checks in Ms.")
	public long getAverageStatusCheckTimeMs() {
		return TimeUnit.MILLISECONDS.convert(getAverageStatusCheckTimeNs(), TimeUnit.NANOSECONDS);
	}
	
	/**
	 * Returns the number of entries checked in the last status check
	 * @return the number of entries checked in the last status check
	 */
	@ManagedMetric(category="ChronicleTimeSeries", displayName="LastEntriesChecked", metricType=MetricType.GAUGE, description="The the number of entries checked in the last status check")
	public long getLastEntriesChecked() {
		return totalEntriesChecked.isEmpty() ? -1L : totalEntriesChecked.get(0);
	}

	/**
	 * Returns the rolling average of entries checked in the last 30 status checks
	 * @return the rolling average of entries checked in the last 30 status checks
	 */
	@ManagedMetric(category="ChronicleTimeSeries", displayName="AverageEntriesChecked", metricType=MetricType.GAUGE, description="The the rolling average of entries checked in the last 30 status checks")
	public long getAverageEntriesChecked() {
		return totalEntriesChecked.isEmpty() ? -1L : totalEntriesChecked.avg();
	}

	/**
	 * Returns the number of stale entries in the last status check
	 * @return the number of stale entries in the last status check
	 */
	@ManagedMetric(category="ChronicleTimeSeries", displayName="LastStaleEntries", metricType=MetricType.GAUGE, description="The the number of stale entries in the last status check")
	public long getLastStaleEntries() {
		return totalStaleEntries.isEmpty() ? -1L : totalStaleEntries.get(0);
	}

	/**
	 * Returns the rolling average of stale entries in the last 30 status checks
	 * @return the rolling average of stale entries in the last 30 status checks
	 */
	@ManagedMetric(category="ChronicleTimeSeries", displayName="AverageStaleEntries", metricType=MetricType.GAUGE, description="The the rolling average of stale entries in the last 30 status checks")
	public long getAverageStaleEntries() {
		return totalStaleEntries.isEmpty() ? -1L : totalStaleEntries.avg();
	}

	/**
	 * Returns the number of offline entries in the last status check
	 * @return the number of offline entries in the last status check
	 */
	@ManagedMetric(category="ChronicleTimeSeries", displayName="LastOffLineEntries", metricType=MetricType.GAUGE, description="The the number of offline entries in the last status check")
	public long getLastOffLineEntries() {
		return totalOffLineEntries.isEmpty() ? -1L : totalOffLineEntries.get(0);
	}

	/**
	 * Returns the rolling average of offline entries in the last 30 status checks
	 * @return the rolling average of offline entries in the last 30 status checks
	 */
	@ManagedMetric(category="ChronicleTimeSeries", displayName="AverageOffLineEntries", metricType=MetricType.GAUGE, description="The the rolling average of offline entries in the last 30 status checks")
	public long getAverageOffLineEntries() {
		return totalOffLineEntries.isEmpty() ? -1L : totalOffLineEntries.avg();
	}

	/**
	 * Returns the number of status check exceptions in the last status check
	 * @return the number of status check exceptions in the last status check
	 */
	@ManagedMetric(category="ChronicleTimeSeries", displayName="StatusExceptions", metricType=MetricType.GAUGE, description="The the number of statusExceptions in the last status check")
	public long getLastStatusExceptions() {
		return statusExceptions.isEmpty() ? -1L : statusExceptions.get(0);
	}

	/**
	 * Returns the rolling average of status check exceptions in the last 30 status checks
	 * @return the rolling average of status check exceptions in the last 30 status checks
	 */
	@ManagedMetric(category="ChronicleTimeSeries", displayName="StatusExceptions", metricType=MetricType.GAUGE, description="The the rolling average of statusExceptions in the last 30 status checks")
	public long getAverageStatusExceptions() {
		return statusExceptions.isEmpty() ? -1L : statusExceptions.avg();
	}


}
