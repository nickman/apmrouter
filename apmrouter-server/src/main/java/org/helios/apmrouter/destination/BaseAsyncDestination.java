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
package org.helios.apmrouter.destination;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.helios.apmrouter.collections.ConcurrentLongSlidingWindow;
import org.helios.apmrouter.destination.accumulator.FlushQueueReceiver;
import org.helios.apmrouter.destination.accumulator.TimeSizeFlushQueue;
import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.util.SystemClock;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;

/**
 * <p>Title: BaseAsyncDestination</p>
 * <p>Description: A base class for destinations that queue incoming metrics and process in size and/or time based batches.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.BaseAsyncDestination</code></p>
 */

public abstract class BaseAsyncDestination extends BaseDestination implements FlushQueueReceiver<IMetric> {
	/** The queue into which incoming metrics are stored */
	protected TimeSizeFlushQueue<IMetric> flushQueue = null;
	/** The queue's size flush trigger */
	protected int sizeTrigger = -1;
	/** The queue's time flush trigger */
	protected long timeTrigger = -1;
	
	/** A sliding window of flush elapsed times */
	protected final ConcurrentLongSlidingWindow flushElapsedTimes = new ConcurrentLongSlidingWindow(20); 
	/** A sliding window of flush sizes */
	protected final ConcurrentLongSlidingWindow flushSize = new ConcurrentLongSlidingWindow(20); 
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.BaseDestination#doStart()
	 */
	@Override
	protected void doStart() throws Exception {
		flushQueue = new TimeSizeFlushQueue<IMetric>(beanName, sizeTrigger, timeTrigger, this);
	}
	
	/**
	 * Accept Route additive for BaseDestination extensions
	 * @param routable The metric to route
	 */
	@Override
	protected void doAcceptRoute(IMetric routable) {
		if(flushQueue.add(routable)) {
			incr("QueuedRoutes");
		} else {
			incr("DroppedRoutes");
		}
	}
	
	/**
	 * Returns the number messages queued by this destination
	 * @return the number messages queued by this destination
	 */
	@ManagedMetric(category="AsyncRoutingDestinations", metricType=MetricType.COUNTER, description="The number messages queued by this destination", displayName="QueuedRoutes")
	public long getQueuedRouteCount() {
		return getMetricValue("QueuedRoutes");
	}
	
	/**
	 * Returns the number messages dequeued by this destination
	 * @return the number messages dequeued by this destination
	 */
	@ManagedMetric(category="AsyncRoutingDestinations", metricType=MetricType.COUNTER, description="The number messages dequeued by this destination", displayName="DequeuedRoutes")
	public long getDequeuedRouteCount() {
		return getMetricValue("DequeuedRoutes");
	}
	
	
	/**
	 * Returns the number messages dropped by this destination
	 * @return the number messages dropped by this destination
	 */
	@ManagedMetric(category="AsyncRoutingDestinations", metricType=MetricType.COUNTER, description="The number messages dropped by this destination", displayName="DropedRoutes")
	public long getDroppedRouteCount() {
		return getMetricValue("DroppedRoutes");
	}
	
	/**
	 * Returns the rolling average elapsed flush time in ns.
	 * @return the rolling average elapsed flush time in ns.
	 */
	@ManagedMetric(category="AsyncRoutingDestinations", metricType=MetricType.GAUGE, description="The rolling average elapsed flush time in ns.", displayName="AverageElapsedFlushTimeNs")
	public long getAverageElapsedFlushTimeNs() {
		return flushElapsedTimes.avg();
	}	
	
	/**
	 * Returns the rolling average elapsed flush time in ms.
	 * @return the rolling average elapsed flush time in ms.
	 */
	@ManagedMetric(category="AsyncRoutingDestinations", metricType=MetricType.GAUGE, description="The rolling average elapsed flush time in ms.", displayName="AverageElapsedFlushTimeMs")
	public long getAverageElapsedFlushTimeMs() {
		return TimeUnit.MILLISECONDS.convert(flushElapsedTimes.avg(), TimeUnit.NANOSECONDS);
	}	
	
	/**
	 * Returns the last elapsed flush time in ns.
	 * @return the last elapsed flush time in ns.
	 */
	@ManagedMetric(category="AsyncRoutingDestinations", metricType=MetricType.GAUGE, description="The last elapsed flush time in ns.", displayName="LastElapsedFlushTimeNs")
	public long getLastElapsedFlushTimeNs() {
		return flushElapsedTimes.isEmpty() ? -1L : flushElapsedTimes.get(0);
	}	
	
	/**
	 * Returns the last elapsed flush time in ms.
	 * @return the last elapsed flush time in ms.
	 */
	@ManagedMetric(category="AsyncRoutingDestinations", metricType=MetricType.GAUGE, description="The rolling last flush time in ms.", displayName="LastElapsedFlushTimeMs")
	public long getLastElapsedFlushTimeMs() {
		return TimeUnit.MILLISECONDS.convert(getLastElapsedFlushTimeNs(), TimeUnit.NANOSECONDS);
	}	
	
	/**
	 * Returns the rolling average flush size.
	 * @return the rolling average flush size.
	 */
	@ManagedMetric(category="AsyncRoutingDestinations", metricType=MetricType.GAUGE, description="The rolling average flush size", displayName="AverageFlushSize")
	public long getAverageFlushSize() {
		return flushSize.avg();
	}	
	
	/**
	 * Returns the last flush size.
	 * @return the last flush size.
	 */
	@ManagedMetric(category="AsyncRoutingDestinations", metricType=MetricType.GAUGE, description="The last flush size", displayName="LastFlushSize")
	public long getLastFlushSize() {
		return flushSize.isEmpty() ? -1L : flushSize.get(0);
	}		
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.accumulator.FlushQueueReceiver#flushTo(java.util.Collection)
	 */
	@Override
	public void flushTo(Collection<IMetric> flushedItems) {
		if(flushedItems==null || flushedItems.isEmpty()) return;
		int size = flushedItems.size();
		incr("DequeuedRoutes", size);
		flushSize.insert(size);
		final long start = System.nanoTime();
		doFlush(flushedItems);
		final long elapsed = System.nanoTime()-start;
		flushElapsedTimes.insert(elapsed);
	}	
	
	/**
	 * Handles the flush of queued items
	 * @param flushedItems The items being flushed
	 */
	protected abstract void doFlush(Collection<IMetric> flushedItems);
	
	/**
	 * Returns the size trigger threshold
	 * @return the sizeTrigger
	 */
	@ManagedAttribute(description="The size queue flush trigger")
	public int getSizeTrigger() {
		if(flushQueue==null) return sizeTrigger;
		return flushQueue.getSizeTrigger();
	}
	
	/**
	 * Sets the size trigger
	 * @param size the size to set the size triger
	 */
	public void setSizeTrigger(int size) {
		sizeTrigger = size;
		if(flushQueue!=null) flushQueue.setSizeTrigger(size);
	}	
	
	/**
	 * Returns the time trigger threshold
	 * @return the timeTrigger
	 */
	@ManagedAttribute(description="The time queue flush trigger")
	public long getTimeTrigger() {
		if(flushQueue==null) return timeTrigger;
		return flushQueue.getTimeTrigger();
	}
	
	/**
	 * Sets the time trigger
	 * @param time the time to set the time triger
	 */
	public void setTimeTrigger(long time) {	
		timeTrigger = time;
		if(flushQueue!=null) flushQueue.setTimeTrigger(time);
	}
	
	/**
	 * Creates a new BaseAsyncDestination
	 * @param patterns The patterns this destination accepts
	 */
	public BaseAsyncDestination(String... patterns) {
		super(patterns);
	}

	/**
	 * Creates a new BaseAsyncDestination
	 * @param patterns The patterns this destination accepts
	 */
	public BaseAsyncDestination(Collection<String> patterns) {
		super(patterns);
	}

	/**
	 * Creates a new BaseAsyncDestination
	 */
	public BaseAsyncDestination() {
		super();
	}



}
