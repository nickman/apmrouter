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

import java.util.Date;

/**
 * <p>Title: TrackedScheduledFutureMBean</p>
 * <p>Description: MBean interface for opentype support</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.jmx.TrackedScheduledFutureMBean</code></p>
 */
public interface TrackedScheduledFutureMBean   {
	/**
	 * Returns the task description 
	 * @return the taskDescription
	 */
	public String getTaskDescription();

	/**
	 * Returns the time until the next execution in seconds
	 * @return the time until the next execution in seconds
	 */
	public long getDelay();
	
	/**
	 * Returns the originally requested task delay or period in seconds
	 * @return the originally requested task delay or period in seconds
	 */
	public long getPeriod();

	/**
	 * Returns the ID of the task
	 * @return the ID of the task
	 */
	public long getId();
	
	/**
	 * Returns the java date that the task was scheduled
	 * @return the startTime
	 */
	public Date getStartDate();
	
	/**
	 * Returns the long UTC time that the task was scheduled
	 * @return the startTime
	 */	
	public long getStartTime();
	
    /**
     * Returns <tt>true</tt> if this task completed.
     * Completion may be due to normal termination, an exception, or
     * cancellation -- in all of these cases, this method will return
     * <tt>true</tt>.
     * @return <tt>true</tt> if this task completed
     */
	public boolean isDone();	
	
	
	

}
