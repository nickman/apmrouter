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
package org.helios.apmrouter.catalog.jdbc.h2;

/**
 * <p>Title: AsynchAbstractTriggerMBean</p>
 * <p>Description: MBean interface for {@link AsynchAbstractTrigger}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.catalog.jdbc.h2.AsynchAbstractTriggerMBean</code></p>
 */

public interface AsynchAbstractTriggerMBean extends AbstractTriggerMBean {
	/**
	 * @return  int
	 */
	public int getActiveCount();

	/**
	 * @return   String
	 * @see org.helios.apmrouter.server.ServerComponentBean#getBeanName()
	 */
	public String getBeanName();

	/**
	 * @return  int
	 * @see org.helios.apmrouter.util.thread.ManagedThreadPool#getPoolSize()
	 */
	public int getPoolSize();

	/**
	 * @return   int
	 * @see org.helios.apmrouter.util.thread.ManagedThreadPool#getQueueSize()
	 */
	
	public int getQueueSize();

	/**
	 * 
	 * @see org.helios.apmrouter.server.ServerComponent#resetMetrics()
	 */
	public void resetMetrics();

	/**
	 * @return    long
	 * @see org.helios.apmrouter.util.thread.ManagedThreadPool#getTaskCount()
	 */
	public long getTaskCount();

	/**
	 * @return   boolean
	 * @see org.helios.apmrouter.util.thread.ManagedThreadPool#isShutdown()
	 */
	public boolean isShutdown();

	/**
	 * @return   boolean
	 * @see org.helios.apmrouter.util.thread.ManagedThreadPool#isTerminated()
	 */
	public boolean isTerminated();

	/**
	 * @return    boolean
	 * @see org.helios.apmrouter.util.thread.ManagedThreadPool#isTerminating()
	 */
	public boolean isTerminating();

	/**
	 * 
	 * @see org.helios.apmrouter.util.thread.ManagedThreadPool#purge()
	 */
	public void purge();

	/**
	 * @return   String[]
	 * @see org.helios.apmrouter.util.thread.ManagedThreadPool#getThreadStats()
	 */
	public String[] getThreadStats();

	/**
	 * @return    boolean
	 * @see org.helios.apmrouter.server.ServerComponentBean#isStarted()
	 */
	public boolean isStarted();
	
	/**
	 * @return  int
	 * @see java.util.concurrent.ThreadPoolExecutor#getLargestPoolSize()
	 */
	public int getLargestPoolSize();
	
	/**
	 * @return   int
	 * @see java.util.concurrent.ThreadPoolExecutor#getMaximumPoolSize()
	 */
	public int getMaximumPoolSize();	
	
	public int getCorePoolSize();	

}
