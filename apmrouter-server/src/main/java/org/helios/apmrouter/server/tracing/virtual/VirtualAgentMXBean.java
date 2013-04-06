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
package org.helios.apmrouter.server.tracing.virtual;

import java.util.Date;
import java.util.Map;

/**
 * <p>Title: VirtualAgentMXBean</p>
 * <p>Description: JMX interface for exposing virtual agent management interfaces</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.tracing.VirtualAgentMXBean</code></p>
 */

public interface VirtualAgentMXBean  {
	
	/** The ObjectName template for non-0-serial virtual agents */
	public static final String VA_OBJ_NAME = "org.helios.apmrouter.agent:type=VirtualAgent,host=%s,agent=%s";

	
	/**
	 * Returns the virtual agent's host
	 * @return the virtual agent's host
	 */
	public String getHost();
	/**
	 * Returns the virtual agent's agent name
	 * @return the virtual agent's agent name
	 */
	public String getAgent();
	
	/**
	 * Returns the state name of this agent
	 * @return the state name of this agent
	 */
	public String getStateName();
	
	
	/**
	 * Expires this virtual agent
	 */
	public void expire();
	
	/**
	 * Invalidates the virtual agent
	 */
	public void invalidate();
	
	
	/**
	 * Returns the number of ms. until expiry
	 * @return the imminent expiry time unless touched in ms.
	 */
	public long getTimeToExpiry();
	
	/**
	 * Returns the number of ms. until the next virtual tracer expiry
	 * @return the number of ms. until the next virtual tracer expiry
	 */
	public long getTimeToNextTracerExpiry();
	
	/**
	 * Returns the last touch timestamp for this virtual agent
	 * @return the last touch timestamp for this virtual agent
	 */
	public long getLastTouchTimestamp();
	
	/**
	 * Returns the last touch date for this virtual agent
	 * @return the last touch date for this virtual agent
	 */
	public Date getLastTouchDate();
	
	/**
	 * Returns the total number of virtual tracers registered
	 * @return the total number of virtual tracers registered
	 */
	public int getTracerCount();
	
	/**
	 * Returns the number of active virtual tracers registered
	 * @return the number of active virtual tracers registered
	 */
	public int getActiveTracerCount();
	
	/**
	 * Returns the time to invalidation in ms. or -1 if invalidation is not pending.
	 * @return the time to invalidation in ms.
	 */
	public long getTimeToInvalidation();
	
	
	/**
	 * Returns a map of the agent's virtual tracers
	 * @return a map of the agent's virtual tracers
	 */
	public Map<String, VirtualTracerMBean> getVirtualTracers();
	
}
