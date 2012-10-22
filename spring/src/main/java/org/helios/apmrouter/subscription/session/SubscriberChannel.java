/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2012, Helios Development Group and individual contributors
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
package org.helios.apmrouter.subscription.session;

/**
 * <p>Title: SubscriberChannel</p>
 * <p>Description: Defines the construct responsible for delivering emitted events to the subscriber, and for
 * terminating the {@link SubscriptionSession} when the channel closes.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.subscription.session.SubscriberChannel</code></p>
 * @param <T> The expected type of the event
 */

public interface SubscriberChannel<T> {
	
	/**
	 * Returns the unique subscriber ID that this channel is delivering to
	 * @return the unique subscriber ID that this channel is delivering to
	 */
	public String getSubscriberId();
	
	/**
	 * Sends an event to the subscriber
	 * @param event The event to send
	 */
	public void send(T event);
	
	/**
	 * Sets the {@link SubscriptionSession} on this subscriber channel
	 * @param session the {@link SubscriptionSession} this channel is sending events for
	 */
	public void setSubscriptionSession(SubscriptionSession session);
}
