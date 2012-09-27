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
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import org.helios.apmrouter.util.SystemClock;


/**
 * <p>Title: TrackedScheduledFuture</p>
 * <p>Description: An instrumented representation of the scheduler's scheduled tasks</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.jmx.TrackedScheduledFuture</code></p>
 */
public class TrackedScheduledFuture implements TrackedScheduledFutureMBean {
	/** Serial number generator */
	private static final AtomicLong serial = new AtomicLong(0L);
	/** The unique serial number for this task */
	protected final long id;
	/** The originally requested delay or period of the task in seconds */
	protected final long period;
	/** The tracked future */
	protected final RunnableScheduledFuture<?> future;
	/** The task description */
	protected final String taskDescription;
	/** The time the task was created */
	protected final long startTime; 
	

	/**
	 * Creates a new TrackedScheduledFuture
	 * @param task The wrapped future
	 * @param description The task description
	 * @param period The originally requested delay or period of the task in seconds
	 * @param activeTasks The set to remove itself from when this future is cancelled 
	 */
	public TrackedScheduledFuture(RunnableScheduledFuture<?> task, String description,long period,  final Set<TrackedScheduledFuture> activeTasks) {
		this.future = task;
		this.taskDescription = description;
		this.period = period;
		id = serial.incrementAndGet();
		startTime = SystemClock.time();
		TrackedScheduledFuture oldTask = null;
		for(Iterator<TrackedScheduledFuture> iter = activeTasks.iterator(); iter.hasNext();) {
			TrackedScheduledFuture t = iter.next();
			if(t.getTaskDescription().equals(description)) {
				oldTask = t;
				break;
			}			
		}
		if(oldTask!=null) {
			activeTasks.remove(oldTask);
			try { oldTask.cancel(false); } catch (Exception e) {}
		}
		activeTasks.add(this);
	}
	
	


	/**
	 * Returns true if this task is periodic , false if it is delayed
	 * @return true if this task is periodic, false if it is delayed
	 */
	public boolean isPeriodic() {
		return future.isPeriodic();			
	}	
	
	/**
	 * Returns unique ID of this task.
	 * @return the id
	 */
	@Override
	public long getId() {
		return id;
	}
	
	/**
	 * Returns the task description 
	 * @return the taskDescription
	 */
	@Override
	public String getTaskDescription() {
		return taskDescription;
	}

	
	/**
	 * Returns the time until the next execution in seconds
	 * @return the time until the next execution in seconds
	 */
	@Override
	public long getDelay() {
		return future.getDelay(TimeUnit.SECONDS);
	}
	
	/**
	 * Returns the originally requested task delay or period in seconds
	 * @return the originally requested task delay or period in seconds
	 */
	@Override
	public long getPeriod() {
		return period;
	}
	

	/**
	 * Returns the long UTC time that the task was scheduled
	 * @return the startTime
	 */	
	@Override
	public long getStartTime() {
		return startTime;
	}
	
	/**
	 * Returns the java date that the task was scheduled
	 * @return the startTime
	 */
	@Override
	public Date getStartDate() {
		return new Date(startTime);
	}
	
	

	/**
	 * Cancels this task
	 * @param mayInterruptIfRunning Indicates if the thread should be interrupted if this task is still running when cancelled
	 * @return true if the task was cancelled
	 */
	public boolean cancel(boolean mayInterruptIfRunning) {
		return future.cancel(mayInterruptIfRunning);
	}
	

    /**
     * Returns the remaining delay associated with this object, in the
     * given time unit.
     *
     * @param unit the time unit
     * @return the remaining delay; zero or negative values indicate
     * that the delay has already elapsed
     */
	public long getDelay(TimeUnit unit) {
		return future.getDelay(unit);
	}


	/**
	 * Compares this task with the specified task for order. Returns a negative integer, zero, or a positive integer as this task is scheduled before, at the same time or after the specified task. 
	 * @param d The task to compare to
	 * @return a negative integer, zero, or a positive integer as this task is scheduled before, at the same time or after the specified task
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Delayed d) {
		return future.compareTo(d);
	}

    /**
     * Returns <tt>true</tt> if this task was cancelled before it completed normally.
     * @return <tt>true</tt> if this task was cancelled before it completed
     */
	public boolean isCancelled() {
		return future.isCancelled();
	}

    /**
     * Returns <tt>true</tt> if this task completed.
     * Completion may be due to normal termination, an exception, or
     * cancellation -- in all of these cases, this method will return
     * <tt>true</tt>.
     * @return <tt>true</tt> if this task completed
     */
	public boolean isDone() {
		return future.isDone();
	}

    /**
     * Waits if necessary for the computation to complete, and then
     * retrieves its result.
     *
     * @return the computed result
     * @throws CancellationException if the computation was cancelled
     * @throws ExecutionException if the computation threw an
     * exception
     * @throws InterruptedException if the current thread was interrupted
     * while waiting
     */
	public Object get() throws InterruptedException, ExecutionException {
		return future.get();
	}

    /**
     * Waits if necessary for at most the given time for the computation
     * to complete, and then retrieves its result, if available.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return the computed result
     * @throws CancellationException if the computation was cancelled
     * @throws ExecutionException if the computation threw an
     * exception
     * @throws InterruptedException if the current thread was interrupted
     * while waiting
     * @throws TimeoutException if the wait timed out
     */
	public Object get(long timeout, TimeUnit unit) throws InterruptedException,
			ExecutionException, TimeoutException {
		return future.get(timeout, unit);
	}













	

}
