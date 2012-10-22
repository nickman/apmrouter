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
package org.helios.apmrouter.subscription.criteria.jmx;

import javax.management.MBeanServerConnection;
import javax.management.NotificationFilter;
import javax.management.ObjectName;

import org.helios.apmrouter.subscription.criteria.SubscriptionCriteria;

/**
 * <p>Title: JMXSubscriptionCriteria</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.subscription.criteria.jmx.JMXSubscriptionCriteria</code></p>
 */

public class JMXSubscriptionCriteria implements SubscriptionCriteria<MBeanServerConnection, ObjectName, NotificationFilter> {

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.subscription.criteria.SubscriptionCriteria#getEventSource()
	 */
	@Override
	public MBeanServerConnection getEventSource() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.subscription.criteria.SubscriptionCriteria#getEventFilter()
	 */
	@Override
	public ObjectName getEventFilter() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.subscription.criteria.SubscriptionCriteria#getEventExtendedFilter()
	 */
	@Override
	public NotificationFilter getEventExtendedFilter() {
		// TODO Auto-generated method stub
		return null;
	}


}
