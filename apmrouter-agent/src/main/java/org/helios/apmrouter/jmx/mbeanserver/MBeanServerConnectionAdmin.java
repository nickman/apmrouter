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
package org.helios.apmrouter.jmx.mbeanserver;

import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationListener;

/**
 * <p>Title: MBeanServerConnectionAdmin</p>
 * <p>Description: Defines some admin commands added to the {@link MBeanServerConnection} proxy invocation handler</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.jmx.mbeanserver.MBeanServerConnectionAdmin</code></p>
 */

public interface MBeanServerConnectionAdmin {
	/**
	 * Closes the {@link MBeanServerConnection} but not the underlying channel
	 */
	public void closeMBeanServerConnection();
	
	/**
	 * Latched method that the calling thread waits on to get the synchronous call response
	 * @param requestId The request Id for which this is a response
	 * @param timeout The timeout of the call in ms.
	 */
	public void waitForSynchronousResponse(int requestId, long timeout);
	
	/**
	 * A callback from the {@MBeanServerConnectionInvocationResponseHandler} when it receives a response
	 * @param requestId The request Id for which this is a response
	 * @param value The return value of an invocation
	 */
	public void onSynchronousResponse(int requestId, Object value);
	
	/**
	 * Adds a registered listener for tracking and callbacks
	 * @param requestId The id of the request that registered the listener 
	 * @param listener The listener that was registered
	 */
	public void addRegisteredListener(int requestId, NotificationListener listener);
	
	/**
	 * Removes a registered listener from tracking
	 * @param listener The listener to remove
	 */
	public void removeRegisteredListener(NotificationListener listener);
	
	/**
	 * Callback from the JMX Op channel handler when a notification is received
	 * @param requestId The id of the request that registered the the listener that the notification is intended for
	 * @param notification The delivered notification
	 * @param handback The contextual handback
	 */
	public void onNotification(int requestId, Notification notification, Object handback);
}
