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

import java.net.URI;

/**
 * <p>Title: AbstractSenderMXBean</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.sender.AbstractSenderMXBean</code></p>
 */

public interface AbstractSenderMXBean  {

	/** The system property name for the heartbeat period */
	public static final String HBEAT_PERIOD_PROP = "org.helios.apmrouter.heartbeat.period";
	/** The default heartbeat period */
	public static final long DEFAULT_HBEAT_PERIOD = 15000;
	/** The system property name for the heartbeat timeout */
	public static final String HBEAT_TO_PROP = "org.helios.apmrouter.heartbeat.timeout";
	/** The default heartbeat timeout */
	public static final long DEFAULT_HBEAT_TO = 1000;
	/** The system property name for the metric URI op timeout */
	public static final String METRIC_URI_TO_PROP = "org.helios.apmrouter.metricuri.timeout";
	/** The default metric URI op timeout */
	public static final long DEFAULT_METRIC_URI_TO = 2000; 
	
	/** The system property name for the number of consecutive ping failures to trigger a disconnect state */
	public static final String HBEAT_DISC_PROP = "org.helios.apmrouter.heartbeat.disconnect";
	/** The default number of consecutive ping failures to trigger a disconnect state */
	public static final long DEFAULT_HBEAT_DISC = 2;

	/**
	 * @return
	 */
	public abstract long getSentMetrics();

	/**
	 * @return
	 */
	public abstract long getDroppedMetrics();

	/**
	 * @return
	 */
	public abstract long getFailedMetrics();

	/**
	 * Returns a sliding window average of agent ping elapsed times to the server
	 * @return a sliding window average of agent ping elapsed times to the server
	 */
	public abstract long getAveragePingTime();
	
	/**
	 * Returns the number of metric sub listeners
	 * @return the number of metric sub listeners
	 */
	public int getMetricURIEventListenerCount();

	/**
	 * Sends a ping request to the configured server
	 * @param timeout the timeout in ms.
	 * @return true if ping was confirmed within the timeout, false otherwise
	 */
	public abstract boolean ping(long timeout);
	
	/**
	 * Returns the metric URI op timeout in ms.
	 * @return the metric URI op timeout in ms.
	 */
	public long getMetricURITimeout();
	
	/**
	 * Sets the metric URI op timeout in ms.
	 * @param timeout the metric URI op timeout in ms.
	 */
	public void setMetricURITimeout(long timeout);

	/**
	 * @return
	 */
	public abstract URI getURI();

	/**
	 * Returns the frequency in ms. of heartbeat pings to the apmrouter server
	 * @return the heartbeat Ping Period
	 */
	public abstract long getHeartbeatPingPeriod();

	/**
	 * Sets the frequency in ms. of heartbeat pings to the apmrouter server
	 * @param heartbeatPingPeriod the frequency in ms. of heartbeat pings to the apmrouter server
	 */
	public abstract void setHeartbeatPingPeriod(long heartbeatPingPeriod);

	/**
	 * Returns the heartbeat ping timeout in ms.
	 * @return the heartbeat ping timeout in ms.
	 */
	public abstract long getHeartbeatTimeout();

	/**
	 * Sets the heartbeat ping timeout in ms.
	 * @param heartbeatTimeout the heartbeat ping timeout in ms.
	 */
	public abstract void setHeartbeatTimeout(long heartbeatTimeout);
	
	
	/**
	 * Returns the number of consecutive heartbeat timeouts that will trigger a disconnect state
	 * @return the number of consecutive heartbeat timeouts that will trigger a disconnect state
	 */
	public abstract int getHeartbeatTimeoutTrigger();
	
	/**
	 * Sets the number of consecutive heartbeat timeouts that will trigger a disconnect state
	 * @param heartbeatTimeoutDiscTrigger the number of consecutive heartbeat timeouts that will trigger a disconnect state
	 */
	public abstract void setHeartbeatTimeoutTrigger(int heartbeatTimeoutDiscTrigger);
	
	/**
	 * Returns the number of consecutive heartbeat ping timeouts
	 * @return the number of consecutive heartbeat ping timeouts
	 */
	public abstract long getConsecutiveTimeouts();
	
	/**
	 * Returns true if the sender is connected, false if it has been timed out.
	 * @return true if the sender is connected
	 */
	public abstract boolean isConnnected();
	
	/**
	 * Returns the logical name for this sender
	 * @return the logical name for this sender
	 */
	public String getName();	
	
	/**
	 * Returns the number of processed metric tokens
	 * @return the number of processed metric tokens
	 */
	public long getProcessedTokens();
	


}