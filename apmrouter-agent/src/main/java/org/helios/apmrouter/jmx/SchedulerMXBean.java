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
package org.helios.apmrouter.jmx;

import javax.management.MXBean;
import javax.management.ObjectName;
import java.util.Set;


/**
 * <p>Title: SchedulerMXBean</p>
 * <p>Description: Defines the task scheduler JMX interface</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.jmx.SchedulerMXBean</code></p>
 */

@MXBean
public interface SchedulerMXBean {
	/**
	 * Returns the approximate number of threads that are actively executing tasks.
	 * @return the approximate number of threads that are actively executing tasks.
	 */
	public int getActiveCount();
	
	/**
	 * Returns the core number of threads.
	 * @return the core number of threads.
	 */
	public int getCorePoolSize();
	
	/**
	 * Returns the maximum number of threads.
	 * @return the maximum number of threads.
	 */
	public int getMaximumPoolSize();
	
	
	/**
	 * Returns the largest number of threads that have ever simultaneously been in the pool.
	 * @return the largest number of threads that have ever simultaneously been in the pool.
	 */
	public int getLargestPoolSize(); 
	
	/**
	 * Returns the current number of threads in the pool.
	 * @return the current number of threads in the pool.
	 */
	public int getPoolSize();
	
	/**
	 * Returns the approximate total number of tasks that have completed execution.
	 * @return the approximate total number of tasks that have completed execution.
	 */
	public long getCompletedTaskCount();
	
	
	/**
	 * Returns true if this executor has been shut down.
	 * @return true if this executor has been shut down.
	 */
	public boolean isShutdown();
	
	/**
	 * Returns true if this executor has been terminated.
	 * @return true if this executor has been terminated.
	 */
	public boolean isTerminated();
	
	/**
	 * Returns true if this executor is terminating.
	 * @return true if this executor is terminating.
	 */
	public boolean isTerminating();
	
	/**
	 * Returns a set of the scheduled tasks
	 * @return an set of the scheduled tasks
	 */
	public Set<TrackedScheduledFuture> getScheduledTasks();
	
	/**
	 * Returns the number of pending tasks
	 * @return the number of pending tasks
	 */
	public int getPendingTaskCount();
	
//    /**
//     * Creates and executes a one-shot action that becomes enabled after the given delay.
//     * @param description A description of the command
//     * @param command The runnable to schedule
//     * @param delay The delay time
//     * @param unit The delay unit
//     * @return the scheduled future for the task
//     */
//	public TrackedScheduledFuture schedule(String description, Runnable command, long delay, TimeUnit unit);
//    
//    /**
//     * Creates and executes a one-shot action that becomes enabled after the given delay.
//     * @param description A description of the command
//     * @param callable The callable to schedule
//     * @param delay The delay time
//     * @param unit The delay unit
//     * @return the scheduled future for the task
//     */    
//	public TrackedScheduledFuture schedule(String description, Callable<?> callable, long delay, TimeUnit unit);
//    
//    /**
//     * Creates and executes a periodic action that becomes enabled first after the given initial delay, 
//     * and subsequently with the given period; that is executions will commence after initialDelay 
//     * then initialDelay+period, then initialDelay + 2 * period, and so on.
//     * @param description A description of the command
//     * @param command The command to schedule
//     * @param initialDelay the time to delay first execution
//     * @param period the period between successive executions
//     * @param unit The period unit
//     * @return the scheduled future for the task
//     */
//	public TrackedScheduledFuture scheduleAtFixedRate(String description, Runnable command, long initialDelay, long period, TimeUnit unit);
    
    /**
     * Creates and executes a periodic action that becomes enabled first after the given initial delay, 
     * and subsequently with the given delay between the termination of one execution and the commencement of the next. 
     * If any execution of the task encounters an exception, subsequent executions are suppressed. Otherwise, the task will only terminate via cancellation or termination of the executor. 
     * @param description A description of the command
     * @param task The JMX ObjectName of the task to schedule
     * @param initialDelay the time to delay first execution
     * @param period the period between successive executions
     * @param unit The period unit
     * @return the task schedule handle use to cancel the task
     */
    public long scheduleWithFixedDelay(String description, ObjectName task, long initialDelay, long period, String unit);	
    
    /**
     * Cancels the task associated with the passed handle
     * @param taskId The task handle
     * @param mayInterruptIfRunning true if the task can be interrupted if running
     */
    public void cancelTask(long taskId, boolean mayInterruptIfRunning);    


}
