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
package org.helios.apmrouter.catalog.jdbc.h2;

import java.sql.Connection;

/**
 * <p>Title: MetricTriggerMBean</p>
 * <p>Description: H2 new metric trigger JMX interface</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.catalog.jdbc.h2.MetricTriggerMBean</code></p>
 */

public interface MetricTriggerMBean extends AbstractTriggerMBean {
	/**
	 * Returns the number of entries in the Agent FQN prefix cache
	 * @return the number of entries in the Agent FQN prefix cache
	 */
	public int getAgentPrefixCacheSize();
	
	/**
	 * Flushes the Agent FQN prefix cache
	 */
	public void flushAgentPrefixCache();
	
	/**
	 * Returns the agent FQN prefix for the passed agent Id.
	 * @param agentId The agent id of the agent to get the agent FQN prefix for  
	 * @param conn The connection that will be used to query the value if there is no cache hit
	 * @return the agent FQN prefix or null if one was not found.
	 */
	public String getAgentPrefix(int agentId, Connection conn);
}
