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

import org.helios.apmrouter.server.ServerComponentBean;
import org.helios.apmrouter.subscription.criteria.SubscriptionCriteria;
import org.helios.apmrouter.subscription.session.DefaultSubscriptionSessionImpl;
import org.helios.apmrouter.subscription.session.NettySubscriberChannel;
import org.helios.apmrouter.subscription.session.SubscriptionSession;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelLocal;

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
	/** A channel local of subscription sessions */
	protected final ChannelLocal<SubscriptionSession> subSessions = new ChannelLocal<SubscriptionSession>(true);
	
	/**
	 * Starts or returns the ID of the subscription session for the passed channel
	 * @param channel The channel to retrieve the session for
	 * @return the unique ID of the subscription session
	 */
	public long startSubscriptionSession(Channel channel) {
		if(channel==null) throw new IllegalArgumentException("The passed channel was null", new Throwable());
		if(!channel.isOpen()) throw new IllegalStateException("The passed channel is not open [" + channel + "]", new Throwable());
		info("Inquiring about SubscriptionSession for channel [", channel, "]");
		SubscriptionSession session = subSessions.get(channel);
		if(session==null) {
			synchronized(this) {
				session = subSessions.get(channel);
				if(session==null) {
					session = new DefaultSubscriptionSessionImpl(new NettySubscriberChannel(channel));
					subSessions.set(channel, session);					
				}
			}
		}
		return session.getSubscriptionSessionId();
	}
	
	/**
	 * Returns the session Id of the passed channel's subscription session, creating a session if one does not exist.
	 * @param channel The channel to get the session Id for.
	 * @return the session Id for the passed channel
	 */
	public long getSessionId(Channel channel) {		
		return startSubscriptionSession(channel);
	}
	
	/**
	 * Returns the subscription session Id for the passed channel, or null if a session does not exist.
	 * @param channel The channel to check for
	 * @return the session Id if one exists, null otherwise.
	 */
	public Long getSessionIdOrNull(Channel channel) {
		if(channel==null) throw new IllegalArgumentException("The passed channel was null", new Throwable());
		SubscriptionSession session = subSessions.get(channel);
		return session==null ? null : session.getSubscriptionSessionId();
	}
	
	/**
	 * Determines if the passed channel has a current subscription session
	 * @param channel The channel to check for a current subscription session
	 * @return true if the passed channel has a current subscription session, false otherwise
	 */
	public boolean hasSession(Channel channel) {
		if(channel==null) throw new IllegalArgumentException("The passed channel was null", new Throwable());
		return subSessions.get(channel)!=null;
	}
	
	/**
	 * Adds a subscription definition to the subscription session for the passed channel.
	 * If a session does not already exist, it will be created, but an additional call to {@link SubscriptionService#getSessionId(Channel)} may be required to get the session Id. 
	 * @param channel The channel to add criteria for
	 * @param criteria The criteria to add
	 * @return the Id of the criteria subscription
	 */
	public long addCriteria(Channel channel, SubscriptionCriteria<?,?,?> criteria) {
		if(channel==null) throw new IllegalArgumentException("The passed channel was null", new Throwable());
		startSubscriptionSession(channel);
		SubscriptionSession session = subSessions.get(channel);
		return session.addCriteria(criteria, session);
	}
}
