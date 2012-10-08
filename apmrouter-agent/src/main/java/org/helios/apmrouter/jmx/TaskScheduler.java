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

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;


/**
 * <p>Title: TaskScheduler</p>
 * <p>Description: Facade for scheduler</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.jmx.TaskScheduler</code></p>
 */
public interface TaskScheduler {
    /**
     * Creates and executes a one-shot action that becomes enabled after the given delay.
     * @param description A description of the command
     * @param command The runnable to schedule
     * @param delay The delay time
     * @param unit The delay unit
     * @return the scheduled future for the task
     */
	public TrackedScheduledFuture schedule(String description, Runnable command, long delay, TimeUnit unit);
    
    /**
     * Creates and executes a one-shot action that becomes enabled after the given delay.
     * @param description A description of the command
     * @param callable The callable to schedule
     * @param delay The delay time
     * @param unit The delay unit
     * @return the scheduled future for the task
     */    
    public TrackedScheduledFuture schedule(String description, Callable<?> callable, long delay, TimeUnit unit);
    
    /**
     * Creates and executes a periodic action that becomes enabled first after the given initial delay, 
     * and subsequently with the given period; that is executions will commence after initialDelay 
     * then initialDelay+period, then initialDelay + 2 * period, and so on.
     * @param description A description of the command
     * @param command The command to schedule
     * @param initialDelay the time to delay first execution
     * @param period the period between successive executions
     * @param unit The period unit
     * @return the scheduled future for the task
     */
	public TrackedScheduledFuture scheduleAtFixedRate(String description, Runnable command, long initialDelay, long period, TimeUnit unit);
    
    /**
     * Creates and executes a periodic action that becomes enabled first after the given initial delay, 
     * and subsequently with the given delay between the termination of one execution and the commencement of the next. 
     * If any execution of the task encounters an exception, subsequent executions are suppressed. Otherwise, the task will only terminate via cancellation or termination of the executor. 
     * @param description A description of the command
     * @param command The command to schedule
     * @param initialDelay the time to delay first execution
     * @param period the period between successive executions
     * @param unit The period unit
     * @return the scheduled future for the task
     */
	public TrackedScheduledFuture scheduleWithFixedDelay(String description, Runnable command, long initialDelay, long period, TimeUnit unit);

}
