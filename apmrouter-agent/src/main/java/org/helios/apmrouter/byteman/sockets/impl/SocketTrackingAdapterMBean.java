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
package org.helios.apmrouter.byteman.sockets.impl;

/**
 * <p>Title: SocketTrackingAdapterMBean</p>
 * <p>Description: JMX MBean interface for [@link SocketTrackingAdapter}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.byteman.sockets.impl.SocketTrackingAdapterMBean</code></p>
 */

public interface SocketTrackingAdapterMBean {
	/**
	 * Returns the class name of the installed tracker, or null if one is not installed
	 * @return the class name of the installed tracker, or null if one is not installed
	 */
	public String getInstalledTrackerName();
	/**
	 * Sets the class name of the installed tracker, or null uninstall the current tracker
	 * @param trackerClassName the class name of the installed tracker, or null uninstall the current tracker
	 */
	public void setInstalledTrackerName(String trackerClassName);
	
	/**
	 * Sets the class name of the installed tracker, or null uninstall the current tracker
	 * @param trackerClassName the class name of the installed tracker, or null uninstall the current tracker
	 * @param cl An optional classloader to load the socket tracker from
	 */
	public void setInstalledTrackerName(String trackerClassName, ClassLoader cl);
}
