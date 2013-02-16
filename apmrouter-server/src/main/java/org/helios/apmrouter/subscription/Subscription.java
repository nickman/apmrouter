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
package org.helios.apmrouter.subscription;

import java.util.Date;

/**
 * <p>Title: Subscription</p>
 * <p>Description: Defines a subscription wherein events published from a specified <b>Source</b> are relayed to the <b>Subscriber</b>.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.subscription.Subscription</code></p>
 * @param <T> The expected type of the events being delivered for this subscription
 */

public interface Subscription<T> {
	/**
	 * Returns the subscriber ID, a unique identifier of and endpoint that matching events will be relayed to.
	 * @return the subscriber ID
	 */
	public int getSubscriberId();
	
	/**
	 * Returns the subscription ID, a unique identifier of an event match pattern from a specific source.
	 * @return the subscription ID
	 */
	public long getSubscriptionId();
	
	/**
	 * Returns the start timestamp of this subscription
	 * @return the start timestamp of this subscription
	 */
	public long getStartTime();

	/**
	 * Returns the start date of this subscription
	 * @return the start date of this subscription
	 */	
	public Date getStartDate();
	
	/**
	 * Terminates this subscription
	 */
	public void terminate();
	
	/**
	 * Callback from an event source when an event is available to be delivered to the subscriber.
	 * @param event The event to deliver to the subscriber.
	 */
	public void onEvent(T event);
}
