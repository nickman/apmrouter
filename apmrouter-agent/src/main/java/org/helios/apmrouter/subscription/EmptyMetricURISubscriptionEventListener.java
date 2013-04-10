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

/**
 * <p>Title: EmptyMetricURISubscriptionEventListener</p>
 * <p>Description: Empty implementation of {@link MetricURISubscriptionEventListener} for extending.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.subscription.EmptyMetricURISubscriptionEventListener</code></p>
 */

public class EmptyMetricURISubscriptionEventListener implements MetricURISubscriptionEventListener {

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.subscription.MetricURISubscriptionEventListener#onNewMetric(java.lang.Object)
	 */
	@Override
	public void onNewMetric(Object newMetric) {

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.subscription.MetricURISubscriptionEventListener#onMetricStateChangeEntry(java.lang.Object)
	 */
	@Override
	public void onMetricStateChangeEntry(Object metric) {

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.subscription.MetricURISubscriptionEventListener#onMetricStateChange(java.lang.Object)
	 */
	@Override
	public void onMetricStateChange(Object metric) {

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.subscription.MetricURISubscriptionEventListener#onMetricStateChangeExit(java.lang.Object)
	 */
	@Override
	public void onMetricStateChangeExit(Object metric) {

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.subscription.MetricURISubscriptionEventListener#onMetricData(java.lang.Object)
	 */
	@Override
	public void onMetricData(Object metricData) {

	}

}
