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

import java.lang.management.ManagementFactory;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>Title: SentryScheduler</p>
 * <p>Description: Singleton scheduler for managing sentry scheduled task executions</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.sentry.SentryScheduler</code></p>
 */

public class SentryScheduler implements ThreadFactory, RejectedExecutionHandler, Thread.UncaughtExceptionHandler {
	/** The singleton instance */
	private static volatile SentryScheduler instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();

	/** The sentry thread pool's thread group */
	private final ThreadGroup threadGroup = new ThreadGroup("SentrySchedulerThreadGroup");
	/** A serial number factory for sentry threads */
	private final AtomicInteger serial = new AtomicInteger(0);
	/** The sentry scheduled thread pool */
	private final ScheduledThreadPoolExecutor scheduler; 
	
	/**
	 * Returns the SentryScheduler instance
	 * @return the SentryScheduler instance
	 */
	public static SentryScheduler getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new SentryScheduler();
				}
			}
		}
		return instance;
	}
	
	private SentryScheduler() {
		scheduler = new ScheduledThreadPoolExecutor(ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors(), this, this);
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
