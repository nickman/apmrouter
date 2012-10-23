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
package org.helios.apmrouter.subscription.impls.jmx;

import javax.management.NotificationFilter;
import javax.management.ObjectName;

import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.subscription.criteria.SubscriptionCriteria;
import org.helios.apmrouter.subscription.criteria.builder.AbstractSubscriptionCriteriaBuilder;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * <p>Title: JMXSubscriptionCriteriaBuilder</p>
 * <p>Description: A subscription criteria builder for JMX</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.subscription.impls.jmx.SubscriptionCriteriaBuilder</code></p>
 */

public class JMXSubscriptionCriteriaBuilder extends AbstractSubscriptionCriteriaBuilder<String, ObjectName, NotificationFilter> {

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.subscription.criteria.builder.SubscriptionCriteriaBuilder#build(org.json.JSONObject)
	 * FIXME: Implement the notification filter editor
	 */
	@Override
	public SubscriptionCriteria<String, ObjectName, NotificationFilter> build(JSONObject subRequest) throws JSONException {
		String eventSource = subRequest.getString(JSON_EVENT_SOURCE);
		ObjectName objectName = JMXHelper.objectName(subRequest.getString(JSON_EVENT_FILTER));
		String filterExpression = null;
		if(subRequest.has(JSON_EXTENDED_EVENT_FILTER)) {
			filterExpression = subRequest.getString(JSON_EXTENDED_EVENT_FILTER);
		}
		return new JMXSubscriptionCriteria(eventSource, objectName, null);
	}

}
