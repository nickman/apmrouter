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
package org.helios.apmrouter.sender;

import java.net.SocketAddress;
import java.net.URI;
import java.util.concurrent.TimeoutException;

import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.trace.DirectMetricCollection;
import org.helios.apmrouter.trace.MetricSubmitter;

/**
 * <p>Title: ISender</p>
 * <p>Description: Defines a metric sender that dispatches closed metrics to the apm-router</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.sender.ISender</code></p>
 */

public interface ISender extends MetricSubmitter {
	
	/**
	 * Returns a sliding window average of agent ping elapsed times to the server
	 * @return a sliding window average of agent ping elapsed times to the server
	 */
	public long getAveragePingTime();
	
	/**
	 * Sends a ping request to the passed address
	 * @param address The address to ping
	 * @param timeout the timeout in ms.
	 * @return true if ping was confirmed within the timeout, false otherwise
	 */
	public boolean ping(SocketAddress address, long timeout);
	
	/**
	 * Sends a ping request to the configured server
	 * @param timeout the timeout in ms.
	 * @return true if ping was confirmed within the timeout, false otherwise
	 */
	public boolean ping(long timeout);
	
	
	/**
	 * Sends the metrics in the passed DCM to the configured endpoint
	 * @param dcm the DCM containing the metrics to send
	 */
	public void send(DirectMetricCollection dcm);
	
	/**
	 * Sends the passed metric to the configured endpoint and waits for a confirmation
	 * @param metric the metric to send
	 * @param timeout The period of time to wait for the confirmation
	 * @throws TimeoutException Thrown if the confirmation is not received in the specified time.
	 */
	public void send(IMetric metric, long timeout) throws TimeoutException;
	
	/**
	 * Sends a HELLO op to the server
	 */
	public void sendHello();	
	

	
	/**
	 * Returns the total number of sent metrics on this sender
	 * @return the total number of sent metrics on this sender
	 */
	public long getSentMetrics();

	/**
	 * Returns the total number of dropped metrics on this sender.
	 * This typically occurs when the payload of an individual metric is too large
	 * for the limitations of the transport protocol of the sender.
	 * @return the total number of dropped metrics on this sender
	 */	
	public long getDroppedMetrics();
	
	/**
	 * Returns the number of metric send failures
	 * @return the number of metric send failures
	 */
	public long getFailedMetrics();
	

	
	/**
	 * Returns the URI that this sender is sending to
	 * @return the URI that this sender is sending to
	 */
	public URI getURI();
	
	/**
	 * Returns the frequency in ms. of heartbeat pings to the apmrouter server
	 * @return the heartbeat Ping Period
	 */
	public long getHeartbeatPingPeriod();

	/**
	 * Sets the frequency in ms. of heartbeat pings to the apmrouter server
	 * @param heartbeatPingPeriod the frequency in ms. of heartbeat pings to the apmrouter server
	 */
	public void setHeartbeatPingPeriod(long heartbeatPingPeriod);	
}
