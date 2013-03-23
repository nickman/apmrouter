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
package org.helios.apmrouter.server.services.session;

import javax.management.ObjectName;

import org.helios.apmrouter.jmx.JMXHelper;

/**
 * <p>Title: SharedChannelGroupMXBean</p>
 * <p>Description: MX interface for the SharedChannelGroup</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.services.session.SharedChannelGroupMXBean</code></p>
 */

public interface SharedChannelGroupMXBean  {

	/** The new channel session jmx event type */
	public static final String NEW_SESSION_EVENT = "apmrouter.session.start";
	/** The identfied channel session jmx event type */
	public static final String IDENTIFIED_SESSION_EVENT = "apmrouter.session.identified";	
	/** The closed channel session jmx event type */
	public static final String CLOSED_SESSION_EVENT = "apmrouter.session.end";
	/** The host up jmx event type */
	public static final String HOST_UP_EVENT = "apmrouter.host.up";
	/** The host down jmx event type */
	public static final String HOST_DOWN_EVENT = "apmrouter.host.down";
	
	/** The JMX ObjectName for this service */
	public static final ObjectName OBJECT_NAME = JMXHelper.objectName("org.helios.apmrouter.session:service=SharedChannelGroup");

	/**
	 * Returns the number of registered channels
	 * @return the number of registered channels
	 */
	public abstract int getSize();

	/**
	 * Determines if the group is empty
	 * @return true if the group is empty, false otherwise
	 */
	public abstract boolean isEmpty();

	/**
	 * Returns the name of this channel group
	 * @return the name of this channel group
	 */
	public abstract String getName();



	/**
	 * Clears all channels from the channel group 
	 */
	public abstract void clear();
	
	/**
	 * Returns an array of the decorated channels
	 * @return an array of the decorated channels
	 */
	public DecoratedChannelMBean[] getChannels();
	
	/**
	 * Returns a map of registered channels for the passed agent and host
	 * @param agent The agent
	 * @param host The host
	 * @return a [possibly empty] array of registered channels for the passed agent and host
	 */	
	public DecoratedChannelMBean[] findByAgentHost(String agent, String host);
	
	/**
	 * Returns the registered channels for the passed protocol, agent and host
	 * @param protocol The protocol
	 * @param agent The agent
	 * @param host The host
	 * @return the registered channel for the passed protocol, agent and host keyed or null if one was not found
	 */
	public DecoratedChannelMBean findByAgentHost(String protocol, String agent, String host);


}