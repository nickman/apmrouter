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
package org.helios.apmrouter.destination.accumulator;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.helios.apmrouter.jmx.ScheduledThreadPoolFactory;
import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.util.SystemClock;

/**
 * <p>Title: MetricAccumulator</p>
 * <p>Description: Accumulates and conflates {@link IMetric}s in preparation for a metric count or time based flush.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.accumulator.MetricAccumulator</code></p>
 */

public class MetricAccumulator implements Runnable {
	/** The scheduler shared amongst all monitor instances */
	protected static final ScheduledThreadPoolExecutor scheduler = ScheduledThreadPoolFactory.newScheduler("MetricAccumulator");
	/** The flush receiver that will receive the metrics when a flush is triggered */
	protected final MetricFlushReceiver receiver;
	/** The last flush event timestamp */
	protected final AtomicLong lastFlush = new AtomicLong(0L);
	/** The metric count size flush trigger */
	protected final int sizeTrigger;
	/** The elapsed time flush trigger in ms. */
	protected final long timeTrigger;	
	/** Indicates if a flush is in progress */
	protected final AtomicBoolean flushInProgress = new AtomicBoolean(false);
	/** The timed flush schedule handle */
	protected ScheduledFuture<?> scheduleHandle = null;
	/** The accumulation map */
	protected final ConcurrentHashMap<String, IMetric> accumulatedMetrics; 

	/**
	 * Creates a new MetricAccumulator
	 * @param receiver The flush receiver that will receive the metrics when a flush is triggered
	 * @param bufferSize The initial buffer size (in bytes) for the accumulation buffer
	 * @param sizeTrigger The number of accumulated metrics that will trigger a flush
	 * @param timeTrigger The elapsed time that will trigger a flush
	 * @param unit The unit of the time trigger
	 */
	public MetricAccumulator(MetricFlushReceiver receiver, int bufferSize, int sizeTrigger, long timeTrigger, TimeUnit unit) {
		accumulatedMetrics = new ConcurrentHashMap<String, IMetric>(bufferSize);
		this.receiver = receiver;
		this.sizeTrigger = sizeTrigger;
		this.timeTrigger = TimeUnit.MILLISECONDS.convert(timeTrigger, unit);
		scheduleHandle = scheduler.scheduleAtFixedRate(this, this.timeTrigger, this.timeTrigger, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Stops this accumulator and releases all its resources
	 */
	public void shutdown() {
		if(!scheduleHandle.isCancelled()) {
			scheduleHandle.cancel(true);
		}
		accumulatedMetrics.clear();
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		if(SystemClock.elapsedMsSince(lastFlush.get()) >= timeTrigger) {
			flush();
		}
	}
	
	/**
	 * Appends the passed {@link IMetric}s to the accumulation buffer in the configured metric format
	 * @param metrics The metrics to accumulate
	 * @throws IOException thrown on any error writing to the output stream
	 */
	public void append(IMetric...metrics) throws IOException {
		if(scheduleHandle.isCancelled()) {
			throw new IllegalStateException("This MetricTextAccumulator has been shutdown", new Throwable());
		}
		if(metrics!=null && metrics.length>0) {
			for(IMetric metric: metrics) {
				synchronized(flushInProgress) {
					IMetric alreadyQueued = accumulatedMetrics.get(metric.getFQN());
					if(alreadyQueued!=null) {
						alreadyQueued.conflate(metric);
					} else {
						accumulatedMetrics.put(metric.getFQN(), metric);
					}
				}
			}
			
			if(accumulatedMetrics.size() >= sizeTrigger) {
				flush();
			}
		}
	}
	
	/**
	 * Returns the number of accumulated metrics
	 * @return the number of accumulated metrics
	 */
	public int size() {
		return accumulatedMetrics.size();
	}
	
	/**
	 * Copies the accumulated buffer into a new buffer, flushes the copy, clears the accumulated buffer and returns the copy
	 */
	public void flush() {
		if(flushInProgress.compareAndSet(false, true)) {
			try {
				Map<String, IMetric> flushed = null;
				synchronized(flushInProgress) {
					flushed = new HashMap<String, IMetric>(accumulatedMetrics);
					accumulatedMetrics.clear();
				}
				lastFlush.set(SystemClock.time());
				receiver.flush(flushed.values(), flushed.size());
			} finally {
				flushInProgress.set(false);
			}
		}
	}

	/**
	 * Returns the timestamp of the last flush
	 * @return the timestamp of the last flush
	 */
	public long getLastFlushTimestamp() {
		return lastFlush.get();
	}
	
	/**
	 * Returns the date of the last flush
	 * @return the date of the last flush
	 */
	public Date getLastFlushDate() {
		return new Date(lastFlush.get());
	}
	

	/**
	 * Returns the configured size trigger for this accumulator
	 * @return the configured size trigger for this accumulator
	 */
	public int getSizeTrigger() {
		return sizeTrigger;
	}

	/**
	 * Returns the configured time trigger in ms. for this accumulator
	 * @return the configured time trigger in ms. for this accumulator
	 */
	public long getTimeTrigger() {
		return timeTrigger;
	}

	/**
	 * Indicates if there is a flush in progress
	 * @return true if there is a flush in progress, false otherwise
	 */
	public boolean getFlushInProgress() {
		return flushInProgress.get();
	}
	

}
