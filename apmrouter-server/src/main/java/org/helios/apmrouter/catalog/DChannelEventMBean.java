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
package org.helios.apmrouter.catalog;

/**
 * <p>Title: DChannelEventMBean</p>
 * <p>Description: MBean interface for {@link DChannelEvent} to support open-type notifications</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.catalog.DChannelEventMBean</code></p>
 */

public interface DChannelEventMBean {
	/**
	 * Returns the event type name
	 * @return the event type name
	 */
	public String getEventType();

	/**
	 * Returns the agent's host domain
	 * @return the domain
	 */
	public String[] getDomain();

	/**
	 * Returns the agent's host 
	 * @return the host
	 */
	public String getHost();

	/**
	 * Returns the agent's host id
	 * @return the hostId
	 */
	public int getHostId();

	/**
	 * Returns the agent's name
	 * @return the agent
	 */
	public String getAgent();

	/**
	 * Returns the agent's id
	 * @return the agentId
	 */
	public int getAgentId();

	/**
	 * Indicates if this event caused a state change in the agent's host
	 * @return true if this event caused a state change in the agent's host, false otherwise
	 */
	public boolean isHostChange();

}