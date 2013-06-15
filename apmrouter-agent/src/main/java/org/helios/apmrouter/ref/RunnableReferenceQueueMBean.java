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
package org.helios.apmrouter.ref;

/**
 * <p>Title: RunnableReferenceQueueMBean</p>
 * <p>Description: JMX interface for {@link RunnableReferenceQueue}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.ref.RunnableReferenceQueueMBean</code></p>
 */

public interface RunnableReferenceQueueMBean {
	/**
	 * Returns the cleaner work queue depth
	 * @return the cleaner work queue depth
	 */
	public int getWorkQueueDepth();
	
	/**
	 * Returns the cleaner work queue remaining capacity
	 * @return the cleaner work queue remaining capacity
	 */
	public int getWorkQueueCapacity();
	

	/**
	 * Returns the worker completed task count 
	 * @return the worker completed task count
	 */
	public long getCompletedTaskCount();
	
	/**
	 * Returns the number of active worker threads
	 * @return the number of active worker threads
	 */
	public int getActiveWorkers();
	
	/**
	 * Returns the current number of worker threads
	 * @return the current number of worker threads
	 */
	public int getWorkerCount();
	
	/**
	 * Returns the highwater number of worker threads
	 * @return the highwater number of worker threads
	 */
	public int getHighwaterWorkerCount();
	
	

	/**
//	 * Returns the number of rejected executions
	 * @return the number of rejected executions
	 */
	public long getRejectedExecutions();

	/**
	 * Returns the number of failed executions
	 * @return the number of failed executions
	 */
	public long getFailedExecutions();

	/**
	 * Returns the number of completed callbacks
	 * @return the number of completed callbacks
	 */
	public long getCompletedCallbacks();

	/**
	 * Returns the number of submitted callbacks
	 * @return the number of submitted callbacks
	 */
	public long getSubmittedCallbacks();
	
	/**
	 * Returns the number of in-flight callbacks
	 * @return the number of in-flight callbacks
	 */
	public long getPendingCallbacks();

}
