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
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import org.helios.apmrouter.collections.LongSlidingWindow;
import org.helios.apmrouter.metric.ExpandedMetric;
import org.helios.apmrouter.metric.ICEMetric;
import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.server.ServerComponentBean;
import org.helios.apmrouter.util.SystemClock;
import org.helios.apmrouter.util.SystemClock.ElapsedTime;
import org.helios.apmrouter.util.thread.ManagedThreadPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;

/**
 * <p>Title: PatternRouter</p>
 * <p>Description: A pettern routing engine that distributes {@link Routable} instances to subscribers that advertise a pattern that matches the routing key supplied by the routables</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.router.PatternRouter</code></p>
 * FIXME:  This is supposed to be a generic class, it is temporarilly specific to IMetric.
 * FIXME:  Clean up the old/failed dedicated thread model
 */

public class PatternRouter extends ServerComponentBean implements UncaughtExceptionHandler, RejectedExecutionHandler  {
	/** The worker thread pool that reads the routing queue */
	protected ExecutorService threadPool;
	/** The routing queue size */
	protected int routingQueueSize = 1000;
	/** The routing queue fairness */
	protected boolean routingQueueFairness = false;
	/** The number of routing worker threads */
	protected int routingWorkers = 5;
	/** The routing queue */
	protected BlockingQueue<IMetric> routingQueue = null;
	/** The subscribers */
	protected final Set<RouteDestination<IMetric>> destinations = new CopyOnWriteArraySet<RouteDestination<IMetric>>();
	/** An uncaught exception handler applied to threads running in the router's thread pool */
	protected final UncaughtExceptionHandler ucex = this;
	
	/** Sliding windows of route elapsed times in ns. */
	protected final LongSlidingWindow elapsedTimesNs = new LongSlidingWindow(15);
	/** Sliding windows of route elapsed times in ms. */
	protected final LongSlidingWindow elapsedTimesMs = new LongSlidingWindow(15); 
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStart()
	 */
	@Override
	protected void doStart() throws Exception {
		super.doStart();
		((ManagedThreadPool)threadPool).setRejectedExecutionHandler(this);
		routingQueue = new ArrayBlockingQueue<IMetric>(routingQueueSize, routingQueueFairness);
		
//		for(int i = 0; i < routingWorkers; i++) {
//			threadPool.execute(new MatchingWorker());
//		}
	}
	
	/**
	 * @param initialDests
	 */
	@Autowired(required=true)
	public void setDestinations(Map<String, RouteDestination> initialDests) {
		for(Map.Entry<String, RouteDestination> entry: initialDests.entrySet()) {
			destinations.add(entry.getValue());
			info("Registered [", entry.getKey(), "] as a Route Destination");
		}
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStop()
	 */
	@Override
	protected void doStop() {
		
		
		super.doStop();
	}
	
	/**
	 * <p>Title: MatchingWorker</p>
	 * <p>Description: Worker runnables for executing matches</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.router.PatternRouter.MatchingWorker</code></p>
	 * FIXME: what do we do when the interrupted exception is caught ?
	 */
	protected class MatchingWorker implements Runnable {
		@Override
		public void run() {
			Thread.currentThread().setUncaughtExceptionHandler(ucex);
			while(true) {
				try {
					IMetric metric = routingQueue.take();
					if(metric.getType()==org.helios.apmrouter.metric.MetricType.BLOB) {
						metric = new ExpandedMetric((ICEMetric)metric);
					}
					for(RouteDestination<IMetric> destination: destinations) {
						destination.acceptRoute(metric);
					}
				} catch (InterruptedException e) {
					e.printStackTrace(System.err);
				}
			}
		}
		
	}
	
	
	/**
	 * Routes an array of routables to their pattern matched endpoints
	 * @param metrics The routables to route
	 */
	public void route(IMetric...metrics) {
		for(final IMetric metric: metrics) {
			if(metric==null) continue;
			this.threadPool.execute(new Runnable(){
				public void run() {
					try {
						SystemClock.startTimer();
						IMetric routableMetric = metric;
						if(metric.getType()==org.helios.apmrouter.metric.MetricType.BLOB) {
							routableMetric = new ExpandedMetric((ICEMetric)metric);
						}						
						for(RouteDestination<IMetric> destination: destinations) {
							destination.acceptRoute(routableMetric);
							incr("CompletedRoutes");
						}
						ElapsedTime et = SystemClock.endTimer();
						elapsedTimesNs.insert(et.elapsedNs);
						elapsedTimesMs.insert(et.elapsedMs);
					} catch (Throwable e) {
						SystemClock.endTimer();
						incr("DroppedRoutes");
						e.printStackTrace(System.err);
					}
					
				}
			});
//			if(routingQueue.offer(metric)) {
//				incr("CompletedRoutes");
//			} else {
//				incr("DroppedRoutes");
//			}			
		}
	}
	
	/**
	 * Routes a collection of routables to their pattern matched endpoints
	 * @param metrics The routables to route
	 */
	public void route(Collection<IMetric> metrics) {
		for(IMetric metric: metrics) {
			if(metric==null) continue;
			if(routingQueue.offer(metric)) {
				incr("CompletedRoutes");
			} else {
				incr("DroppedRoutes");
			}			
		}	
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponent#getSupportedMetricNames()
	 */
	@Override
	public Set<String> getSupportedMetricNames() {
		Set<String> metrics = new HashSet<String>(super.getSupportedMetricNames());
		metrics.add("DroppedRoutes");
		metrics.add("CompletedRoutes");
		return metrics;
	}

	/**
	 * Returns the configured size of the routing queue
	 * @return the routingQueueSize
	 */
	@ManagedAttribute
	public int getRoutingQueueSize() {
		return routingQueueSize;
	}
	
	/**
	 * Returns the number of metrics in the routing queue
	 * @return the routingQueueSize
	 */
	@ManagedMetric(category="MetricRouter", metricType=MetricType.COUNTER, description="The number of metrics in the routing queue")
	public long getRoutingQueueDepth() {
		return routingQueue.size();
	}
	
	/**
	 * Returns the sliding average elapsed time in ns. of the last 15 route events
	 * @return the sliding average elapsed time in ns. of the last 15 route events
	 */
	@ManagedMetric(category="MetricRouter", metricType=MetricType.GAUGE, description="The sliding average elapsed time in ns. of the last 15 route events")
	public long getAverageRouteTimeNs() {
		return elapsedTimesNs.avg();
	}
	
	/**
	 * Returns the sliding average elapsed time in ms. of the last 15 route events
	 * @return the sliding average elapsed time in ms. of the last 15 route events
	 */
	@ManagedMetric(category="MetricRouter", metricType=MetricType.GAUGE, description="The sliding average elapsed time in ms. of the last 15 route events")
	public long getAverageRouteTimeMs() {
		return elapsedTimesMs.avg();
	}
	
	
	
	/**
	 * Returns the number of routed metrics
	 * @return the number of routed metrics
	 */
	@ManagedMetric(category="MetricRouter", metricType=MetricType.COUNTER, description="The number of metrics routed")
	public long getRoutedMetricCount() {
		return getMetricValue("CompletedRoutes");
	}
	
	/**
	 * Returns the number of metrics dropped in routing
	 * @return the number of metrics dropped in routing
	 */
	@ManagedMetric(category="MetricRouter", metricType=MetricType.COUNTER, description="The number of metrics dropped in routing")
	public long getDroppedMetricCount() {
		return getMetricValue("DroppedRoutes");
	}
	
	

	/**
	 * Sets 
	 * @param routingQueueSize the routingQueueSize to set
	 */
	public void setRoutingQueueSize(int routingQueueSize) {
		this.routingQueueSize = routingQueueSize;
	}

	/**
	 * Indicates if the routing queue is fair
	 * @return true if the routing queue is fair, false otherwise
	 */
	@ManagedAttribute
	public boolean isRoutingQueueFair() {
		return routingQueueFairness;
	}

	/**
	 * Indicates if the routing queue should be fair
	 * @param routingQueueFairness the routingQueueFairness to set
	 */
	public void setRoutingQueueFair(boolean routingQueueFairness) {
		this.routingQueueFairness = routingQueueFairness;
	}

	/**
	 * Sets the routing thread pool
	 * @param threadPool the threadPool to set
	 */
	public void setThreadPool(ExecutorService threadPool) {
		this.threadPool = threadPool;
	}

	/**
	 * Returns the number of routing worker threads
	 * @return the number of routing worker threads
	 */
	@ManagedAttribute
	public int getRoutingWorkers() {
		return routingWorkers;
	}

	/**
	 * Sets the number of routing worker threads
	 * @param routingWorkers the number of routing worker threads to set
	 */
	public void setRoutingWorkers(int routingWorkers) {
		this.routingWorkers = routingWorkers;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Thread.UncaughtExceptionHandler#uncaughtException(java.lang.Thread, java.lang.Throwable)
	 */
	@Override
	public void uncaughtException(Thread t, Throwable e) {
		warn("Uncaught exception [", t, "]", e);
		
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.RejectedExecutionHandler#rejectedExecution(java.lang.Runnable, java.util.concurrent.ThreadPoolExecutor)
	 */
	@Override
	public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
		warn("Pattern Router Rejected execution\n\tTask:", r.getClass().getName(), "\n\tWorker QueueDepth:" + executor.getQueue().size(), new Throwable());
		incr("DroppedRoutes");
	}
	
	
	
}
