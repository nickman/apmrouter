/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2013, Helios Development Group and individual contributors
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
package org.helios.collector.jmx.connection.triggers;

import javax.management.MBeanServerConnection;

/**
 * <p>Title: OnFirstConnectTrigger</p>
 * <p>Description: Defines a MBeanServerConnectionFactory trigger fired when the factory acquires a connection for the first time.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.collector.jmx.connection.triggers.OnFirstConnectTrigger</code></p>
 */

public interface OnFirstConnectTrigger extends ConnectTrigger {
	
	/**
	 * Determines if this the first connect ever, or a connection after a connection failure
	 * @return true if this the first connect ever, or a connection after a connection failure, false otherwise
	 */
	public boolean isFirstConnect();
	
	/**
	 * Callback when a MBeanServerConnectionFactory connects for the first time, or connects after a failure.
	 * @param connection the successful connection
	 */
	public void onFirstConnect(MBeanServerConnection connection);
}
