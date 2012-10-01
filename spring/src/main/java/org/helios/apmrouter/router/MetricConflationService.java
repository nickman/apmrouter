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
package org.helios.apmrouter.router;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.server.ServerComponentBean;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;

/**
 * <p>Title: MetricConflationService</p>
 * <p>Description: Service to conflate incoming metrics</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.router.MetricConflationService</code></p>
 */
public class MetricConflationService extends ServerComponentBean implements Runnable, UncaughtExceptionHandler {
	/** The maximum number of metrics that can be pending conflation at a time */
	protected int maxQueueSize = 100000;
	/** The number of worker threads to spin up to service the queue */
	protected int workerThreads = 1;
	/** The minimum period of time each metric remains in the conflation queue, unless is it conflated into another metric */
	protected long conflationPeriod = 1000;
	/** The pattern router */
	protected PatternRouter router = null;
	/** The conflation queue */
	protected final ConcurrentSkipListMap<Long, Map<String, IMetric>> conflationQueue = new ConcurrentSkipListMap<Long, Map<String, IMetric>>();
	/** Indicates if the threads should be running */
	protected boolean keepRunning = false;
	/** The thread group containing the worker threads */
	protected ThreadGroup threadGroup = new ThreadGroup("MetricConflationService");
	/** Id factory for the threads */
	protected final AtomicInteger serial = new AtomicInteger(0);
	
	@Override
	protected void doStart() throws Exception {
		super.doStart();
		keepRunning = true;
		for(int i = 0; i < workerThreads; i++) {					
			Thread t = new Thread(threadGroup, this, "MetricConflationThread#" + serial.incrementAndGet());
			t.setDaemon(true);
			t.setUncaughtExceptionHandler(this);
			t.start();
		}
	}
	
	protected void doStop() {
		keepRunning = false;
		super.doStop();
	}
	
	/**
	 * Enqueues an array of metrics to the conflation queue
	 * @param imetrics  array of metrics to queue
	 */
	public void queue(IMetric...imetrics) {
		if(imetrics==null || imetrics.length==0) return;
		long now = System.currentTimeMillis() + conflationPeriod;	
		Map<String, IMetric> map = conflationQueue.get(now);
		if(map==null) {
			synchronized(conflationQueue) {
				map = conflationQueue.get(now);
				if(map==null) {
					map = new ConcurrentHashMap<String, IMetric>();
					conflationQueue.put(now, map);
				}
			}
		}		
		for(IMetric metric: imetrics) {
			if(!metric.getType().isLong()) {
				router.queue(metric);
				incr("MetricsForwarded");

			}
			IMetric alreadyQueued = map.get(metric.getFQN());
			if(alreadyQueued==null) {
				synchronized(map) {
					alreadyQueued = map.get(metric.getFQN());
					if(alreadyQueued!=null) {
						alreadyQueued.conflate(metric);
						incr("MetricsConflated");
					} else {
						if(conflationQueue.size() >= maxQueueSize) {
							incr("MetricsDropped");
						} else {
							map.put(metric.getFQN(), metric);
							incr("MetricsQueued");
						}						
					}
				}
			}
				
		}	
	}

	
	/**
	 * Returns the number of periods in the queue
	 * @return the number of periods in the queue
	 */
	@ManagedAttribute
	public int getQueueSize() {
		return conflationQueue.size();
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		while(keepRunning) {
			try {
				long now = System.currentTimeMillis();
				Map<Long, Map<String, IMetric>> clearMetrics = conflationQueue.tailMap(now, true);
				Map<String, IMetric> forwards = new HashMap<String, IMetric>();
				for(Map.Entry<Long, Map<String, IMetric>> entry: clearMetrics.entrySet()) {
					Map<String, IMetric> map = conflationQueue.remove(entry.getKey());
					if(map!=null) {									
						for(Map.Entry<String, IMetric> fentry: map.entrySet()) {
							IMetric existing = forwards.get(fentry.getKey());
							if(existing!=null) {
								existing.conflate(fentry.getValue());
								incr("MetricsConflated");
								continue;
							}
							forwards.put(fentry.getKey(), fentry.getValue());
						}
					}
				}
				if(!forwards.isEmpty()) {
					router.queue(forwards.values());
					incr("MetricsForwarded", forwards.size());
					forwards.clear();
					
				}
				Thread.currentThread().join(50);
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
	}
	
	/**
	 * Returns the maximum number of metrics that can be pending conflation at a time
	 * @return the maximum number of metrics that can be pending conflation at a time
	 */
	public int getMaxQueueSize() {
		return maxQueueSize;
	}


	/**
	 * Sets the maximum number of metrics that can be pending conflation at a time
	 * @param maxQueueSize the maximum number of metrics that can be pending conflation at a time
	 */
	public void setMaxQueueSize(int maxQueueSize) {
		this.maxQueueSize = maxQueueSize;
	}


	/**
	 * Returns the number of worker threads to spin up to service the queue
	 * @return the number of worker threads to spin up to service the queue
	 */
	public int getWorkerThreads() {
		return workerThreads;
	}


	/**
	 * Sets the number of worker threads to spin up to service the queue
	 * @param workerThreads the number of worker threads to spin up to service the queue
	 */
	public void setWorkerThreads(int workerThreads) {
		this.workerThreads = workerThreads;
	}


	/**
	 * Returns the minimum period of time in ms each metric remains in the conflation queue, unless is it conflated into another metric 
	 * @return the minimum period of time in ms each metric remains in the conflation queue, unless is it conflated into another metric
	 */
	public long getConflationPeriod() {
		return conflationPeriod;
	}


	/**
	 * Sets the minimum period of time in ms each metric remains in the conflation queue, unless is it conflated into another metric
	 * @param conflationPeriod the minimum period of time in ms each metric remains in the conflation queue, unless is it conflated into another metric
	 */
	public void setConflationPeriod(long conflationPeriod) {
		this.conflationPeriod = conflationPeriod;
	}

	/**
	 * Sets the pattern router where metrics are forwarded to 
	 * @param router the pattern router
	 */
	public void setRouter(PatternRouter router) {
		this.router = router;
	}
	
	
	/**
	 * Returns the number of metrics forwarded from ConflationService
	 * @return the number of metrics forwarded from ConflationService
	 */
	@ManagedMetric(category="ConflationService", metricType=MetricType.COUNTER, description="the number of metrics forwarded from ConflationService")
	public long getMetricsForwarded() {
		return getMetricValue("MetricsForwarded");
	}
	
	
	/**
	 * Returns the number of metrics that were dropped because ConflationService was backlogged
	 * @return the number of metrics that were dropped because ConflationService was backlogged
	 */
	@ManagedMetric(category="ConflationService", metricType=MetricType.COUNTER, description="the number of metrics that were dropped because ConflationService was backlogged")
	public long getMetricsDropped() {
		return getMetricValue("MetricsDropped");
	}
	
	/**
	 * Returns the number of metrics that were conflated
	 * @return the number of metrics that were conflated
	 */
	@ManagedMetric(category="ConflationService", metricType=MetricType.COUNTER, description="the number of metrics that were conflated")
	public long getMetricsConflated() {
		return getMetricValue("MetricsConflated");
	}
	
	/**
	 * Returns the number of metrics that were queued
	 * @return the number of metrics that were queued
	 */
	@ManagedMetric(category="ConflationService", metricType=MetricType.COUNTER, description="the number of metrics that were queued")
	public long getMetricsQueued() {
		return getMetricValue("MetricsQueued");
	}
	
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponent#getSupportedMetricNames()
	 */
	@Override
	public Set<String> getSupportedMetricNames() {
		Set<String> _metrics = new HashSet<String>(super.getSupportedMetricNames());
		_metrics.add("MetricsQueued");
		_metrics.add("MetricsForwarded");
		_metrics.add("MetricsDropped");
		_metrics.add("MetricsConflated");		
		return _metrics;
	}
	


	@Override
	public void uncaughtException(Thread t, Throwable e) {
		error("Failure in worker thread [" + t + "]", e);
		
	}



}
