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

import org.helios.apmrouter.jmx.JMXHelper;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

/**
 * <p>Title: RegisterPlatformMXBeansConnectTrigger</p>
 * <p>Description: A connection trigger to register the platform MXBeans in the target MBeanServer.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.collector.jmx.connection.triggers.RegisterPlatformMXBeansConnectTrigger</code></p>
 */

public class RegisterPlatformMXBeansConnectTrigger extends AbstractOnFirstConnectTrigger {
	/** The class name for the concrete ThreadMXBean implementation */
	public static final String THREAD_MX_CLASSNAME = "sun.management.ThreadImpl";
	/** The class name for the concrete ThreadMXBean implementation */
	public static final String HOTSPOT_MX_CLASSNAME = "sun.management.HotspotInternal"; 
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.collector.jmx.connection.triggers.OnFirstConnectTrigger#onFirstConnect(javax.management.MBeanServerConnection)
	 */
	@Override
	public void onFirstConnect(MBeanServerConnection connection) {
		try {
			//register(connection, ManagementFactory.THREAD_MXBEAN_NAME, THREAD_MX_CLASSNAME);
			register(connection, "java.lang:type=HotspotInternal", HOTSPOT_MX_CLASSNAME);
		} catch (Exception ex) {
			log.error("Failed to register platform MXBeans", ex);
		}

	}
	
	
	/**
	 * Registers the specified MBean in the remote MBeanServer
	 * @param connection The connection to the remote  MBeanServer
	 * @param objectName The object name to register the MBean under
	 * @param className The class name of the MBean to register
	 * @throws Exception thrown on any error
	 */
	protected void register(MBeanServerConnection connection, String objectName, String className) throws Exception {
		ObjectName on = JMXHelper.objectName(objectName);
		if(!connection.isRegistered(on)) {
			connection.createMBean(className, on);
		}
	}

}
