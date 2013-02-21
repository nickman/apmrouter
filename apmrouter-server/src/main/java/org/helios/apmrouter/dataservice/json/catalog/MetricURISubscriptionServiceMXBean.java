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
package org.helios.apmrouter.dataservice.json.catalog;

import java.util.Collection;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;

/**
 * <p>Title: MetricURISubscriptionServiceMXBean</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.dataservice.json.catalog.MetricURISubscriptionServiceMXBean</code></p>
 */

public interface MetricURISubscriptionServiceMXBean {

	/**
	 * Returns the current subscriptions
	 * @return a collection of the current subscriptions
	 */
	public MetricURISubscription[] getSubscriptions();
	
	/**
	 * Returns the current number of subscriptions
	 * @return the current number of subscriptions
	 */
	public int getSubscriptionCount();

	/**
	 * Returns the number of threads to concurrently process the metric event queue
	 * @return the number of threads to concurrently process the metric event queue
	 */
	@ManagedAttribute(description = "The number of threads to concurrently process the metric event queue")
	public int getMetricQueueThreadCount();

	/**
	 * Returns the number of errors processing the metric queued events
	 * @return the number of errors processing the metric queued events
	 */
	@ManagedMetric(category = "MetricURISubscriptionService", displayName = "MetricQueueProcessingErrors", metricType = MetricType.COUNTER, description = "The number of errors processing the metric queued events")
	public long getMetricQueueProcessingErrors();

	/**
	 * Returns the number of pending metric events in the queue
	 * @return the number of pending metric events in the queue
	 */
	@ManagedMetric(category = "MetricURISubscriptionService", displayName = "MetricQueueDepth", metricType = MetricType.COUNTER, description = "The number of pending metric queued events")
	public long getMetricQueueDepth();

}