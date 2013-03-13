/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2013, Helios Development Group and individual contributors
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
package org.helios.apmrouter.metric;

import java.util.Set;

/**
 * <p>Title: IMetricListener</p>
 * <p>Description: Defines a metric listener that will subscribe to metrics using a pattern set and receive emitted metrics.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.metric.IMetricListener</code></p>
 */

public interface IMetricListener {
	/**
	 * Returns the metric patterns this listener is interested in
	 * @return the metric patterns this listener is interested in
	 */
	public Set<String> getPatterns();
	
	/**
	 * A callback from the subscription service providing the id of the subscription created
	 * @param subId the subscription id
	 */
	public void setSubscriptionId(long subId);
	
	/**
	 * Returns this listener's subscription Id
	 * @return this listener's subscription Id
	 */
	public long getSubscriptionId();
	
	/**
	 * Callback from the subscription service when a metric matching this subscriber's patterns is received
	 * @param metric the received metric
	 */
	public void onMetric(IMetric metric);
}
