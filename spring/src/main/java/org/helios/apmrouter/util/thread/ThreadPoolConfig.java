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
package org.helios.apmrouter.util.thread;

import java.lang.management.ManagementFactory;

/**
 * <p>Title: ThreadPoolConfig</p>
 * <p>Description: The thread pool configuration pojo.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.util.thread.ThreadPoolConfig</code></p>
 */

public class ThreadPoolConfig {
	/** The core pool size */
	int corePoolSize = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
	/** The maximum pool size */
	int maximumPoolSize = Integer.MAX_VALUE;
	/** The number of core threads to start */
	int coreThreadsStarted = 0;
	
	/** Idle thread keep alive time in ms. */
	long keepAliveTime = 60000;
	/** The work queue size */
	int queueSize = 1000;
	/** The work queue fairness */
	boolean fairQueue = true;
	/** If core threads are allowed to timeout */
	boolean coreThreadsTimeout = false;
	/** If threads are daemon threads */
	boolean daemonThreads = true;
	
	/**
	 * Sets the core pool size
	 * @param corePoolSize the corePoolSize to set
	 */
	public void setCorePoolSize(int corePoolSize) {
		this.corePoolSize = corePoolSize;
	}
	/**
	 * Sets the max pool size
	 * @param maximumPoolSize the maximumPoolSize to set
	 */
	public void setMaximumPoolSize(int maximumPoolSize) {
		this.maximumPoolSize = maximumPoolSize;
	}
	/**
	 * Sets the idle thread keep alive time
	 * @param keepAliveTime the keepAliveTime to set
	 */
	public void setKeepAliveTime(long keepAliveTime) {
		this.keepAliveTime = keepAliveTime;
	}
	/**
	 * Sets the work queue size
	 * @param queueSize the queueSize to set
	 */
	public void setQueueSize(int queueSize) {
		this.queueSize = queueSize;
	}
	/**
	 * Sets the work queue fairness
	 * @param fairQueue the fairQueue to set
	 */
	public void setFairQueue(boolean fairQueue) {
		this.fairQueue = fairQueue;
	}
	/**
	 * Sets if ccoreThreadsStartedore threads are allowed to timeout
	 * @param coreThreadsTimeout the coreThreadsTimeout to set
	 */
	public void setCoreThreadsTimeout(boolean coreThreadsTimeout) {
		this.coreThreadsTimeout = coreThreadsTimeout;
	}
	/**
	 * Sets the number of core threads to start
	 * @param coreThreadsStarted the coreThreadsStarted to set
	 */
	public void setCoreThreadsStarted(int coreThreadsStarted) {
		this.coreThreadsStarted = coreThreadsStarted;
	}
	/**
	 * Sets 
	 * @param daemonThreads the daemonThreads to set
	 */
	public void setDaemonThreads(boolean daemonThreads) {
		this.daemonThreads = daemonThreads;
	}
	
	
	
}
