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
import java.io.OutputStream;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.helios.apmrouter.jmx.ScheduledThreadPoolFactory;
import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.util.SystemClock;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.buffer.DirectChannelBufferFactory;

/**
 * <p>Title: MetricTextAccumulator</p>
 * <p>Description: Accumulates {@link IMetric}s as byte text in preparation for a metric count or time based flush.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.MetricTextAccumulator</code></p>
 */

public class MetricTextAccumulator implements Runnable {
	/** The scheduler shared amongst all monitor instances */
	protected static final ScheduledThreadPoolExecutor scheduler = ScheduledThreadPoolFactory.newScheduler("MetricAccumulator");
	/** The channel buffer factory */
	protected static final DirectChannelBufferFactory bufferFactory = new DirectChannelBufferFactory();
	/** The accumulation buffer */
	protected final ChannelBuffer accum;
	/** The outputstream to write meteric test bytes into the accum buffer */
	protected final OutputStream os;
	/** The number of accumulated metrics */
	protected final AtomicInteger metricCount = new AtomicInteger(0);
	/** The metric formatter */
	protected final MetricTextFormatter formatter;
	/** The flush receiver that will receive the metric text bytes buffer when a flush is triggered */
	protected final MetricTextFlushReceiver receiver;
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
	
	


	/**
	 * Creates a new MetricTextAccumulator
	 * @param formatter The metric formatter 
	 * @param receiver The flush receiver that will receive the metric text bytes buffer when a flush is triggered
	 * @param bufferSize The initial buffer size (in bytes) for the accumulation buffer
	 * @param sizeTrigger The number of accumulated metrics that will trigger a flush
	 * @param timeTrigger The elapsed time that will trigger a flush
	 * @param unit The unit of the time trigger
	 */
	public MetricTextAccumulator(MetricTextFormatter formatter, MetricTextFlushReceiver receiver, int bufferSize, int sizeTrigger, long timeTrigger, TimeUnit unit) {
		 accum = ChannelBuffers.dynamicBuffer(bufferSize, bufferFactory);
		 os = new ChannelBufferOutputStream(accum);
		 this.formatter = formatter;
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
		accum.clear();
		try {
			os.close();
		} catch (Exception e) {}
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
			int newCount = metricCount.addAndGet(formatter.format(os, metrics));
			if(newCount >= sizeTrigger) {
				flush();
			}
		}
	}
	
	/**
	 * Returns the number of accumulated metrics
	 * @return the number of accumulated metrics
	 */
	public int size() {
		return metricCount.get();
	}
	
	/**
	 * Copies the accumulated buffer into a new buffer, flushes the copy, clears the accumulated buffer and returns the copy
	 */
	public void flush() {
		if(flushInProgress.compareAndSet(false, true)) {
			try {
				ChannelBuffer toSend = ChannelBuffers.copiedBuffer(accum);
				accum.clear();				
				int mc = metricCount.getAndSet(0);
				lastFlush.set(SystemClock.time());
				receiver.flush(toSend, mc);
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
