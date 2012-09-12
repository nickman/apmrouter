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
package org.helios.apmrouter.util;

import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.trace.DirectMetricCollection;

/**
 * <p>Title: CollectionFunnel</p>
 * <p>Description: The drop-off point for tracers to drop their collected metrics.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.util.CollectionFunnel</code></p>
 */

public class CollectionFunnel {
	/** the singleton instance */
	private static volatile CollectionFunnel instance = null;
	/** the singleton instance ctor lock */
	private static final Object lock = new Object();
	/** The timer thread */
	private final Thread timerThread;
	/** The timer period in ms. */
	private final long timerPeriod;
	/** The maximum size in bytes of a DCM before it is flushed */
	private final long maxDcmSize;
	/** The size of the switch queue */
	private final int switchQueueSize;
	/** The DCM switch that causes messages to be queued while the current DCM is being flushed */
	private final AtomicBoolean switchToQueue = new AtomicBoolean(false);
	/** The number of metrics dropped while trying to queue */
	private final AtomicLong dropped = new AtomicLong(0L);
	/** The timestamp of the last flush */
	private volatile long lastFlush;
	/** The current DCM */
	private DirectMetricCollection dcm = null;
	
	/** The switch queue that metrics are written to while the dcm is being flushed */
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
	 * Submits the passed metrics for a send to the apmrouter
	 * @param metrics A collection of metrics to send
	 */
	public void submit(Collection<IMetric> metrics) {
		if(metrics!=null && !metrics.isEmpty()) {
			submit(metrics.toArray(new IMetric[0]));
		}
	}
	
	/**
	 * Submits the passed metrics for a send to the apmrouter
	 * @param metrics An array of metrics to send
	 */
	public void submit(IMetric...metrics) {
		if(metrics.length<1) return;
		if(switchToQueue.get()) {
			if(!offLineQueue.offer(metrics)) {
				dropped.addAndGet(metrics.length);
			}
		} else {
			dcm.append(metrics);
		}
	}
	
	/**
	 * Timer flush
	 */
	protected void timerFlush() {
		if(SystemClock.elapsedMsSince(timerPeriod) >= timerPeriod) {
			flush();
		}		
	}
	
	
	/**
	 * Flushes the current DCM
	 */
	protected void flush() {
		if(switchToQueue.compareAndSet(false, true)) {
			DirectMetricCollection toSend = null;
			if(dcm.getMetricCount()>0) {
				toSend = dcm;
				dcm = DirectMetricCollection.newDirectMetricCollection();
			}
			lastFlush = SystemClock.time();
			switchToQueue.set(false);
			sendDcm(toSend);
			drainQueue();
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
				if(toSend.append(metrics)>=maxDcmSize) {
					sendDcm(toSend);
					toSend = DirectMetricCollection.newDirectMetricCollection();
				}
			}
		}
	}
	
	/**
	 * Sends the DCM 
	 * @param dcm the DCM to send
	 */
	protected void sendDcm(DirectMetricCollection dcm) {
		
	}
	
	private CollectionFunnel() {
		timerPeriod = 3000;
		maxDcmSize = 10240;
		switchQueueSize = 1000;
		offLineQueue = new ArrayBlockingQueue<IMetric[]>(switchQueueSize, false);
		timerThread = new Thread("CollectionFunnelTimer") {
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
		timerThread.setDaemon(false);
		timerThread.start();
		dcm = DirectMetricCollection.newDirectMetricCollection();
		
	}
	
}
