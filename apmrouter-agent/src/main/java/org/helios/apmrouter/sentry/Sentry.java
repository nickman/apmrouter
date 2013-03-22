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
package org.helios.apmrouter.sentry;

import org.helios.apmrouter.jmx.ScheduledThreadPoolFactory;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>Title: Sentry</p>
 * <p>Description: Singleton scheduler for managing sentry scheduled task executions</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.sentry.Sentry</code></p>
 */

public class Sentry implements ThreadFactory, RejectedExecutionHandler, Thread.UncaughtExceptionHandler {
	/** The singleton instance */
	private static volatile Sentry instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();

	/** The sentry thread pool's thread group */
	private final ThreadGroup threadGroup = new ThreadGroup("SentrySchedulerThreadGroup");
	/** A serial number factory for sentry threads */
	private final AtomicInteger serial = new AtomicInteger(0);
	/** The sentry scheduled thread pool */
	private final ScheduledThreadPoolExecutor scheduler = ScheduledThreadPoolFactory.newScheduler("AgentScheduler");
	
	/** A map of sentry triggers keyed by the watched object the trigger was created for */
	private final Map<SentryWatched, SentryTrigger> watches = new ConcurrentHashMap<SentryWatched, SentryTrigger>();
	
	/**
	 * Returns the Sentry instance
	 * @return the Sentry instance
	 */
	public static Sentry getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new Sentry();
				}
			}
		}
		return instance;
	}
	
	private Sentry() {
		
	}
	
	/**
	 * Registers a new watched object 
	 * @param watched The object to watch
	 * @return A state control that allows the watched object (or someone else) to change the state of the watched object 
	 * and trigger events in the sentry.
	 */
	public SentryStateControl register(SentryWatched watched) {
		if(watched==null) throw new IllegalArgumentException("The passed watched object was null", new Throwable());
		SentryTrigger trigger = watches.get(watched);
		if(trigger==null) {
			synchronized(watches) {
				trigger = watches.get(watched);
				if(trigger==null) {
					trigger = new SentryTrigger(this, new SentryTask(watched), SentryState.PENDING);
				}
			}
		}
		return trigger;
	}
	
	/**
	 * Processes state changes based on sets in the registered {@link SentryStateControl}s
	 * @param oldState The prior sentry state
	 * @param newState The new sentry state
	 * @param task The task reference for the state changed watched object
	 */
	void onStateChange(SentryState oldState, SentryState newState, SentryTask task) {
		log("Task [" + task.getWatched().getName() + "] switched from " + oldState + "-->" + newState);
		if(newState==SentryState.CANCELLED) {
			if(task.getScheduleHandle()!=null) {
				task.getScheduleHandle().cancel(true);
				task.setScheduleHandle(null);
			}
			watches.remove(task.getWatched());
		}
		if(task.isPolled()) {
			if(newState==SentryState.POLLING) {
				ScheduledFuture<?> sf = scheduler.scheduleAtFixedRate(task, task.getPeriod(), task.getPeriod(), TimeUnit.MILLISECONDS);
				task.setScheduleHandle(sf);
				log("Scheduled Poller for [" + task.getWatched().getName() +  "]");
			}
		} else {
			if(newState==SentryState.CALLBACK) {
				if(task.getScheduleHandle()!=null) {
					task.getScheduleHandle().cancel(true);
					task.setScheduleHandle(null);
				}
			} else if(newState==SentryState.DISCONNECTED) {
				log("Task [" + task.getWatched().getName() +  "] disconnected. Attempting immediate reconnect");
				if(!((CallbackSentryWatched)task.getWatched()).connect(-1)) {				
					ScheduledFuture<?> sf = scheduler.scheduleAtFixedRate(task, task.getPeriod(), task.getPeriod(), TimeUnit.MILLISECONDS);
					task.setScheduleHandle(sf);								
					log("Task [" + task.getWatched().getName() +  "] immediate reconnect failed. Scheduled reconnected loop");
				}
			}
		}
	}
	
	
	
    public static void log(Object msg) {
    	System.out.println("[Sentry]" + msg);
    }
    

	/**
	 * {@inheritDoc}
	 * @see java.lang.Thread.UncaughtExceptionHandler#uncaughtException(java.lang.Thread, java.lang.Throwable)
	 */
	@Override
	public void uncaughtException(Thread t, Throwable e) {
		System.err.println("Sentry Uncaught exception on thread [" + t + "]. Stack trace follows:");
		e.printStackTrace(System.err);
		
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.RejectedExecutionHandler#rejectedExecution(java.lang.Runnable, java.util.concurrent.ThreadPoolExecutor)
	 */
	@Override
	public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
		System.err.println("Sentry Rejected Execution of task [" + r + "]");
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
	 */
	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(threadGroup, r, "SentrySchedulerThread#" + serial.incrementAndGet());
		t.setDaemon(true);
		t.setUncaughtExceptionHandler(this);
		return t;
	}
}
