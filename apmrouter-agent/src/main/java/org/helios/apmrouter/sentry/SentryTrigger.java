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

import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>Title: SentryTrigger</p>
 * <p>Description: An atomic ref for SentryStates that triggers an event when the state changes</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.sentry.SentryTrigger</code></p>
 */
public class SentryTrigger implements SentryStateControl {
	/** The watched state */
	private final AtomicReference<SentryState> state = new AtomicReference<SentryState>(SentryState.PENDING); 
	/** A reference to the sentry to issue callbacks against */
	private final Sentry sentry;
	/** The sentry task wrapping the watched object */
	private final SentryTask sentryTask;
	
	/**
	 * Creates a new SentryTrigger with the default state of {@link SentryState#PENDING}
	 * @param sentry A reference to the sentry to issue callbacks against
	 * @param sentryTask The sentry task wrapping the watched object
	 */
	public SentryTrigger(Sentry sentry, SentryTask sentryTask) {
		this.sentry = sentry;
		this.sentryTask = sentryTask;
	}	
	
	/**
	 * Creates a new SentryTrigger with the passed state
	 * @param sentry A reference to the sentry to issue callbacks against
	 * @param sentryTask The sentry task wrapping the watched object
	 * @param state The {@link SentryState} of this trigger 
	 */
	public SentryTrigger(Sentry sentry, SentryTask sentryTask, SentryState state) {
		this(sentry, sentryTask);
		if(state==null) throw new IllegalArgumentException("The passed state was null", new Throwable());
		this.state.set(state);
	}
	


	/**
	 * Returns the associated sentry task
	 * @return the associated sentry task
	 */
	public SentryTask getSentryTask() {
		return sentryTask;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sentry.SentryStateControl#getState()
	 */
	@Override
	public SentryState getState() {
		return state.get();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sentry.SentryStateControl#setState(org.helios.apmrouter.sentry.SentryState)
	 */
	@Override
	public void setState(SentryState newState) {
		if(newState==null) throw new IllegalArgumentException("The passed new state was null", new Throwable());
		SentryState oldState = state.get();
		if(oldState!=newState) {
			state.set(newState);
			sentry.onStateChange(oldState, newState, sentryTask);
		}		
	}
}
