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

import org.helios.apmrouter.server.ServerComponentBean;

/**
 * <p>Title: AbstractSubscriberChannel</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.subscription.session.AbstractSubscriberChannel</code></p>
 */

public abstract class AbstractSubscriberChannel extends ServerComponentBean implements SubscriberChannel {
	/** The session delivering the events which should be cancelled when the underlying transport of this channel closes  */
	protected SubscriptionSession session = null;
	/** The unique subscriber ID that this channel is delivering to */
	protected final String subscriberId;
	
	/**
	 * Creates a new AbstractSubscriberChannel
	 * @param subscriberId The unique subscriber ID that this channel is delivering to
	 */
	protected AbstractSubscriberChannel(String subscriberId) {
		this.subscriberId = subscriberId;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.subscription.session.SubscriberChannel#setSubscriptionSession(org.helios.apmrouter.subscription.session.SubscriptionSession)
	 */
	@Override
	public void setSubscriptionSession(SubscriptionSession session) {
		this.session = session;
		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.subscription.session.SubscriberChannel#getSubscriberId()
	 */
	@Override
	public String getSubscriberId() {
		return subscriberId;
	}

}
