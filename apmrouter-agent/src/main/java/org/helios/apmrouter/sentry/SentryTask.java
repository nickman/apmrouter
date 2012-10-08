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

import java.util.concurrent.ScheduledFuture;

/**
 * <p>Title: SentryTask</p>
 * <p>Description: The sentry wrapper task for SentryWatched objects</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.sentry.SentryTask</code></p>
 */
public class SentryTask implements Runnable {
	/** The name of the watched task */
	protected final String name;
	/** The action period in ms. */
	protected final long period;	
	/** Indicates if the watched is polled (or callback) */
	protected final boolean polled;
	/** The watched object */
	protected final SentryWatched watched;
	/** The scheduler handle */
	protected ScheduledFuture<?> scheduleHandle = null;
	/** The cummulative number of failed reconnects */
	protected int connectAttempts = 0;
	
	/**
	 * Creates a new SentryTask
	 * @param watched The watched object 
	 */
	public SentryTask(SentryWatched watched) {
		if(watched==null) throw new IllegalArgumentException("The passed watched was null", new Throwable());
		this.name = watched.getName();
		this.period = watched.getPeriod();
		this.watched = watched;
		polled = this.watched instanceof PollingSentryWatched;
	}

	/**
	 * <p>This is what is run by the sentry scheduler on behalf of the watched object.
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		if(polled) {
			if(!((PollingSentryWatched)watched).sentryPoll()) {
				((PollingSentryWatched)watched).sentryPollFailed();
			}
		} else {
			if(!((CallbackSentryWatched)watched).connect(connectAttempts)) {
				connectAttempts++;				
			} else {				
				connectAttempts = 0;
				scheduleHandle.cancel(true);
				scheduleHandle = null;
			}
		}
	}

	/**
	 * Returns the sentry scheduler handle for this task
	 * @return the sentry scheduler handle for this task
	 */
	public ScheduledFuture<?> getScheduleHandle() {
		return scheduleHandle;
	}

	/**
	 * Sets the sentry scheduler handle for this task
	 * @param scheduleHandle the sentry scheduler handle for this task
	 */
	public void setScheduleHandle(ScheduledFuture<?> scheduleHandle) {
		this.scheduleHandle = scheduleHandle;
	}

	/**
	 * Returns the name of the watched task
	 * @return the name of the watched task
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the scheduler period for this task
	 * @return the scheduler period for this task
	 */
	public long getPeriod() {
		return period;
	}

	/**
	 * Indicates if the watched object is polled or callback
	 * @return true if the watched object is polled, false if it is callback
	 */
	public boolean isPolled() {
		return polled;
	}

	/**
	 * Returns the watched object
	 * @return the watched object
	 */
	public SentryWatched getWatched() {
		return watched;
	}

	/**
	 * Returns the connection attempts issued against the watched object
	 * @return the connection attempts issued against the watched object
	 */
	public int getConnectAttempts() {
		return connectAttempts;
	}

	
	
}
