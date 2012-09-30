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
package org.helios.apmrouter.trace;

import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.helios.apmrouter.jmx.ThreadPoolFactory;
import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.sender.ISender;
import org.helios.apmrouter.sender.SenderFactory;
import org.helios.apmrouter.util.SystemClock;

/**
 * <p>Title: CollectionFunnel</p>
 * <p>Description: The drop-off point for tracers to drop their collected metrics.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.util.CollectionFunnel</code></p>
 */

public class CollectionFunnel implements RejectedExecutionHandler, MetricSubmitter {
	/** the singleton instance */
	private static volatile CollectionFunnel instance = null;
	/** the singleton instance ctor lock */
	private static final Object lock = new Object();
	/** The timer thread */
	private final Thread timerThread;
	/** The timer period in ms. */
	private final long timerPeriod;
	/** The maximum size in bytes of a DMC before it is flushed */
	private final int maxDmcBytes;
	/** The maximum number of metrics in a DMC before it is flushed */
	private final int maxDmcMetrics;	
	/** The size of the switch queue */
	private final int switchQueueSize;
	/** The DCM switch that causes messages to be queued while the current DCM is being flushed */
	private final AtomicBoolean switchToQueue = new AtomicBoolean(false);
	/** The number of metrics dropped while trying to queue */
	private final AtomicLong dropped = new AtomicLong(0L);
	/** The number of metrics queued */
	private final AtomicLong queued = new AtomicLong(0L);	
	/** The number of metrics sent */
	private final AtomicLong sent = new AtomicLong(0L);
	/** The send thread pool */
	private final ThreadPoolExecutor executor; 
	/** The sender, for synchronous sends */
	private final ISender sender;
	

	/** The timestamp of the last flush */
	private volatile long lastFlush;
	/** The current DCM */
	private DirectMetricCollection dmc = null;
	
	/** The switch queue that metrics are written to while the dmc is being flushed */
	private final BlockingQueue<IMetric[]> offLineQueue;
	
	/**
	 * Returns the CollectionFunnel singleton
	 * @return the CollectionFunnel singleton
	 */
	public static CollectionFunnel getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new CollectionFunnel();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Sends a metric directly, bypassing the local buffer
	 * @param metric The metric to send
	 * @param timeout The period of time to wait for a confirm in ms.
	 * @throws TimeoutException Thrown if the confirmation is not received in the specified time.
	 */
	@Override
	public void submitDirect(IMetric metric, long timeout) throws TimeoutException {
		if(metric!=null) {			
			sender.send(metric, timeout);
		}
	}
	
	/**
	 * Submits the passed metrics for a send to the apmrouter
	 * @param metrics A collection of metrics to send
	 */
	@Override
	public void submit(Collection<IMetric> metrics) {
		if(metrics!=null && !metrics.isEmpty()) {
			submit(metrics.toArray(new IMetric[0]));
		}
	}
	
	/**
	 * Submits the passed metrics for a send to the apmrouter
	 * @param metrics An array of metrics to send
	 */
	@Override
	public void submit(IMetric...metrics) {
		if(metrics.length<1) return;
		if(switchToQueue.get()) {
			if(!offLineQueue.offer(metrics)) {
				dropped.addAndGet(metrics.length);
			} else {
				queued.addAndGet(metrics.length);
			}
		} else {
			synchronized(switchToQueue) {				
				if(dmc.append(maxDmcBytes, maxDmcMetrics, metrics)) {
					flush();
				}
			}
		}
	}
	
	/**
	 * Resets the sent and dropped stats
	 */
	@Override
	public void resetStats() {
		dropped.set(0L);
		sent.set(0L);
		queued.set(0);
	}
	
	
	/**
	 * Timer flush
	 */
	protected void timerFlush() {
		if(SystemClock.elapsedMsSince(lastFlush) >= timerPeriod) {
			//System.out.println("============> TIMER FLUSH");
			flush();
		}		
	}
	
	
	/**
	 * Flushes the current DCM
	 */
	protected void flush() {
		if(switchToQueue.compareAndSet(false, true)) {
			try {
				DirectMetricCollection toSend = null;				
				if(dmc.getMetricCount()>0) {
					toSend = dmc;
					dmc = DirectMetricCollection.newDirectMetricCollection();
					switchToQueue.set(false);
					sendDcm(toSend);
				}							
				drainQueue();
			} finally {
				lastFlush = SystemClock.time();
				switchToQueue.compareAndSet(true, false);
			}
		}
	}
	
	/**
	 * Drains the offline queue once the main DCM comes back online after a flush.
	 */
	protected void drainQueue() {
		if(!offLineQueue.isEmpty()) {
			IMetric[] metrics = null;
			DirectMetricCollection toSend = DirectMetricCollection.newDirectMetricCollection();
			while((metrics=offLineQueue.poll())!=null) {
				if(toSend.append(maxDmcBytes, maxDmcMetrics, metrics)) {
					sendDcm(toSend);
					toSend = DirectMetricCollection.newDirectMetricCollection();
				}
			}
		}
	}
	
	/**
	 * Sends the DCM 
	 * @param dcmToSend the DCM to send
	 */
	protected void sendDcm(final DirectMetricCollection dcmToSend) {
		if(dcmToSend==null) {
			System.err.println("Null DCM to send");
			new Throwable().printStackTrace(System.err);
			return;
		}		
		sent.addAndGet(dcmToSend.getMetricCount());
		executor.execute(dcmToSend);
		
	}
	
//	/**
//	 * Sends the DMC in the current thread
//	 * @param dcmToSend The DMC to send
//	 * @param timeout The timeout on this send in ms.
//	 */
//	protected void sendDcmInCurrentThread(final DirectMetricCollection dcmToSend, long timeout) {
//		sent.addAndGet(dcmToSend.getMetricCount());		
//		sender.send(dcmToSend, timeout);
//	}
	
	
	/**
	 * Returns the total number of metrics dropped
	 * @return the total number of metrics dropped
	 */
	@Override
	public long getDroppedMetrics() {
		return dropped.get();
	}
	
	/**
	 * Returns the total number of metrics sent
	 * @return the total number of metrics sent
	 */
	@Override
	public long getSentMetrics() {
		return sent.get();
	}
	
	/**
	 * Returns the total number of metrics queued
	 * @return the total number of metrics queued
	 */
	public long getQueued() {
		return queued.get();
	}
	
	
	/**
	 * Returns the timer flush period in ms.
	 * @return the timer flush period
	 */
	public long getTimerPeriod() {
		return timerPeriod;
	}
	
	private CollectionFunnel() {
		timerPeriod = 3000;
		maxDmcBytes = 10240  * 10;
		maxDmcMetrics = 100 * 100;
		switchQueueSize = 1000;
		executor = new ThreadPoolFactory(
				ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors()/2,
				ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors(),
				60000,
				100000,
				false,
				this,
				true,
				getClass().getPackage().getName(), 
				"CollectionFunnel"				
		);
		sender = SenderFactory.getInstance().getDefaultSender();
		executor.allowCoreThreadTimeOut(false);
		offLineQueue = new ArrayBlockingQueue<IMetric[]>(switchQueueSize, false);
		timerThread = new Thread("CollectionFunnelTimer") {
			@Override
			public void run() {
				while(true) {
					try {
						SystemClock.sleep(timerPeriod);
						timerFlush();
					} catch (Exception e) {}
				}
			}
		};
		timerThread.setPriority(Thread.MAX_PRIORITY);
		timerThread.setDaemon(true);
		timerThread.start();
		dmc = DirectMetricCollection.newDirectMetricCollection();
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.RejectedExecutionHandler#rejectedExecution(java.lang.Runnable, java.util.concurrent.ThreadPoolExecutor)
	 */
	@Override
	public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
		if(r!=null && (r instanceof DirectMetricCollection)) {
			long dr = dropped.addAndGet(((DirectMetricCollection)r).getMetricCount());
			System.err.println("Execution Dropped Count:" + dr + "  Queue Depth:" + executor.getQueue().size());
		}
	}
	
	
	public int getCorePoolSize() {
		return executor.getCorePoolSize();
	}

	public int getMaximumPoolSize() {
		return executor.getMaximumPoolSize();
	}

	public int getPoolSize() {
		return executor.getPoolSize();
	}

	public int getActiveCount() {
		return executor.getActiveCount();
	}

	public int getLargestPoolSize() {
		return executor.getLargestPoolSize();
	}

	public long getTaskCount() {
		return executor.getTaskCount();
	}

	public long getCompletedTaskCount() {
		return executor.getCompletedTaskCount();
	}

	
	public String status() {
		return String
				.format("CollectionFunnel Status[\n\tDropped=%s \n\tSent=%s, \n\tQueued=%s \n\tTimerPeriod=%s \n\tCorePoolSize=%s \n\tMaximumPoolSize=%s \n\tPoolSize=%s \n\tActiveCount=%s \n\tLargestPoolSize=%s \n\tTaskCount=%s \n\tCompletedTaskCount=%s\n]",
						getDroppedMetrics(), getSentMetrics(), getQueued(), getTimerPeriod(),
						getCorePoolSize(), getMaximumPoolSize(), getPoolSize(),
						getActiveCount(), getLargestPoolSize(), getTaskCount(),
						getCompletedTaskCount());
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.MetricSubmitter#getQueuedMetrics()
	 */
	@Override
	public long getQueuedMetrics() {
		try {
			return dmc.getMetricCount();
		} catch (Exception e) {
			return 0;
		}
	}

	
	
}
