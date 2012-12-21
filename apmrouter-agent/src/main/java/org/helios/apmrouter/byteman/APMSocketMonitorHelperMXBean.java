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
package org.helios.apmrouter.byteman;

import java.util.Map;

/**
 * <p>Title: APMSocketMonitorHelperMXBean</p>
 * <p>Description: MXBean interface for {@link APMSocketMonitorHelper}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.byteman.APMSocketMonitorHelperMXBean</code></p>
 */

public interface APMSocketMonitorHelperMXBean {
	/**
	 * Returns the current SocketTracingLevel name
	 * @return the current SocketTracingLevel name
	 */
	public String getSocketTracingLevel();
	
	/**
	 * Sets the SocketTracingLevel 
	 * @param level the SocketTracingLevel name to set
	 */
	public void setSocketTracingLevel(String level);
	
	/**
	 * Returns the number of open server sockets listening on client requests
	 * @return the number of open server sockets listening on client requests
	 */
	public int getServerSocketCount();
	
	/**
	 * Returns a map of connection listening ports and the number of accepted connections on each
	 * @return a map of connection listening ports and the number of accepted connections on each
	 */
	public Map<String, Long> getAcceptedConnectionCounts();	
	
	/**
	 * Returns the number of client sockets connected from this JVM to remotes
	 * @return the number of client sockets connected from this JVM to remotes
	 */
	public int getClientSocketCount();

}
