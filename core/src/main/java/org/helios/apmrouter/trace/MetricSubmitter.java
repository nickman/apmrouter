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
package org.helios.apmrouter.trace;

import java.util.Collection;
import java.util.concurrent.TimeoutException;

import org.helios.apmrouter.metric.IMetric;

/**
 * <p>Title: MetricSubmitter</p>
 * <p>Description: Defines a class that can send an IMetric someplace </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.trace.MetricSubmitter</code></p>
 */

public interface MetricSubmitter {
	/**
	 * Sends a metric directly, bypassing the local buffer
	 * @param metric The metric to send
	 * @param timeout The period of time to wait for a confirm in ms.
	 * @throws TimeoutException Thrown if the confirmation is not received in the specified time.
	 */
	public void submitDirect(IMetric metric, long timeout) throws TimeoutException;
	
	/**
	 * Submits the passed metrics for a send to the apmrouter
	 * @param metrics A collection of metrics to send
	 */
	public void submit(Collection<IMetric> metrics);
	
	/**
	 * Submits the passed metrics for a send to the apmrouter
	 * @param metrics An array of metrics to send
	 */
	public void submit(IMetric...metrics);
	
	/**
	 * Returns the number of sent metrics
	 * @return the number of sent metrics
	 */
	public long getSentMetrics();

	/**
	 * Returns the number of sent metrics
	 * @return the number of sent metrics
	 */
	public long getDroppedMetrics();
	
	/**
	 * Resets the stats
	 */
	public void resetStats();

	/**
	 * Returns the number of unsent queued metrics
	 * @return the number of unsent queued metrics
	 */
	public long getQueuedMetrics();
	
	
}
