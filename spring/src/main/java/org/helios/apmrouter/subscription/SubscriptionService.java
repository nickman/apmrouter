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

import java.util.concurrent.atomic.AtomicLong;

import org.helios.apmrouter.server.ServerComponentBean;

/**
 * <p>Title: SubscriptionService</p>
 * <p>Description: Factory and lifecycle manager for event subscriptions</p>
 * <p>Summary:<p>
 * <p>Moving parts:<ul>
 * 	<li><b><code>SubscriptionCriteria</code></b>: Defines the source and filtering of events delivered to a <b><code>Subscriber</code></b></li>
 *  <li><b><code>SubscriptionSource</code></b>: A source of subscribable events</li>
 *  <li><b><code>Subscriber</code></b>: A recipient of events from a <b><code>SubscriptionSource</code></b> and filtered by a <b><code>SubscriptionCriteria</code></b></li>
 *  <li><b><code>Subscription</code></b>: The unique combination of a <b><code>Subscriber</code></b>, a <b><code>SubscriptionCriteria</code></b> and a <b><code>SubscriptionSource</code></b>.</li>
 *  <li><b><code>SubscriberSession</code></b>: A notional session associated to a <b><code>Subscriber</code></b> that can be terminated and will trigger the termination of the <b><code>Subscriber</code></b>'s <b><code>Subscription</code></b>s</li>
 * </ul></p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.subscription.SubscriptionService</code></p>
 */

public class SubscriptionService extends ServerComponentBean {
	/** Serial number generator for subscriptions  */
	protected static final AtomicLong serial = new AtomicLong(0L);
	
	
}
