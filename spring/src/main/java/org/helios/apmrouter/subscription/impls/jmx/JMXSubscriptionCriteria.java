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
package org.helios.apmrouter.subscription.impls.jmx;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.management.NotificationFilter;
import javax.management.ObjectName;

import org.helios.apmrouter.dataservice.json.JsonRequest;
import org.helios.apmrouter.subscription.criteria.SubscriptionCriteria;
import org.helios.apmrouter.subscription.criteria.SubscriptionCriteriaInstance;

/**
 * <p>Title: JMXSubscriptionCriteria</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.subscription.criteria.jmx.JMXSubscriptionCriteria</code></p>
 */

public class JMXSubscriptionCriteria implements SubscriptionCriteria<String, ObjectName, NotificationFilter> {
	/** The target MBeanServer's JMXServiceURL string for this subscription */
	protected final String jmxServiceURL;
	/** The JMX ObjectName or pattern of the MBeans to subscribe to notifications from */
	protected final ObjectName objectName;
	/** The optional notification filter */
	protected final NotificationFilter filter;
	/** Indicates if the object name is a pattern */
	protected final boolean pattern;
	/** A set of ObjectNames this criteria is activated for */
	protected final Set<ObjectName> objectNames = new CopyOnWriteArraySet<ObjectName>();
	/** A set of ObjectNames this criteria failed to activated for */
	protected final Set<ObjectName> failedObjectNames = new CopyOnWriteArraySet<ObjectName>();
	
	
	
	/**
	 * Creates a new JMXSubscriptionCriteria
	 * @param jmxServiceURL The target MBeanServer's JMXServiceURL string for this subscription
	 * @param objectName The JMX ObjectName or pattern of the MBeans to subscribe to notifications from
	 * @param filter The optional notification filter
	 */
	public JMXSubscriptionCriteria(String jmxServiceURL, ObjectName objectName, NotificationFilter filter) {
		super();
		this.jmxServiceURL = jmxServiceURL;
		this.objectName = objectName;
		this.filter = filter;
		pattern = this.objectName.isPattern();
		
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.subscription.criteria.SubscriptionCriteria#instantiate(org.helios.apmrouter.dataservice.json.JsonRequest)
	 */
	public SubscriptionCriteriaInstance<?> instantiate(JsonRequest request) {
		return new JMXSubscriptionCriteriaInstance(this, request);
	}




	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.subscription.criteria.SubscriptionCriteria#getEventSource()
	 */
	@Override
	public String getEventSource() {
		return jmxServiceURL;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.subscription.criteria.SubscriptionCriteria#getEventFilter()
	 */
	@Override
	public ObjectName getEventFilter() {
		return objectName;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.subscription.criteria.SubscriptionCriteria#getEventExtendedFilter()
	 */
	@Override
	public NotificationFilter getEventExtendedFilter() {
		return filter;
	}

	/**
	 * Indicates if the object name is a pattern
	 * @return true if the object name is a pattern, false otherwise
	 */
	public boolean isPattern() {
		return pattern;
	}





}
