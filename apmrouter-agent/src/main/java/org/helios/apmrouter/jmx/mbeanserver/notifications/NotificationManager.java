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
package org.helios.apmrouter.jmx.mbeanserver.notifications;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.helios.apmrouter.jmx.mbeanserver.AgentMBeanServerConnectionFactory;
import org.helios.apmrouter.util.SimpleLogger;
import org.jboss.netty.channel.Channel;

/**
 * <p>Title: NotificationManager</p>
 * <p>Description: Manages notification listeners passed by the remote JMX client</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.jmx.mbeanserver.notifications.NotificationManager</code></p>
 */

public class NotificationManager {
	/** A map of registered listeners keyed by request id */
	protected final Map<Integer, NotificationListenerWrapper> listeners = new ConcurrentHashMap<Integer, NotificationListenerWrapper>();
	
	/**
	 * <p>Title: NotificationListenerWrapper</p>
	 * <p>Description: A wrapper class to contain a registered listener and the MBeanServer it was registered in</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.jmx.mbeanserver.notifications.NotificationManager.NotificationListenerWrapper</code></p>
	 */
	protected static class NotificationListenerWrapper {
		/** The listener that was registered */
		private final NotificationListener listener;
		/** The MBeanServer the listener was registered with */
		private final MBeanServer server;
		/** The ObjectName that the listener was registered on */
		private final ObjectName objectName;

		/**
		 * Creates a new NotificationListenerWrapper
		 * @param listener The listener that was registered
		 * @param objectName The ObjectName that the listener was registered on
		 * @param server The MBeanServer the listener was registered with 
		 */
		public NotificationListenerWrapper(NotificationListener listener, ObjectName objectName, MBeanServer server) {
			this.listener = listener;
			this.server = server;
			this.objectName = objectName;
		}
		
		/**
		 * Unregisters the listener
		 * @return true if the listener was unregistered, false otherwise
		 */
		public boolean unregister() {
			try {
				server.removeNotificationListener(objectName, listener);
				return true;
			} catch (Exception ex) {
				SimpleLogger.error("Failed to remove listener from MBean [", objectName, "]", ex);
				return false;
			}
		}
		
		
	}
	
	/**
	 * Adds a listener to a registered MBean in the passed MBeanServer
	 * @param channel The channel to which emitted notifications should be written back to
	 * @param remoteAddress The remote address where the notification should be sent
	 * @param server The MBeanServer to register the listener with
	 * @param requestId The id of the listener registration request
	 * @param name The name of the MBean on which the listener should be added.
	 * @param filter The filter object. If filter is null, no filtering will be performed before handling notifications
	 * @param handback The context to be sent to the listener when a notification is emitted
	 */
	public void addNotificationListener(final Channel channel, final SocketAddress remoteAddress, final MBeanServer server, final int requestId, final ObjectName name, final NotificationFilter filter, final Object handback) {
		try {
			NotificationListener listener = new NotificationListener() {
				@Override
				public void handleNotification(Notification notification, Object handback) {
					AgentMBeanServerConnectionFactory.writeNotification(channel, remoteAddress, requestId, notification, handback);
				}
			};
			
			listeners.put(requestId, new NotificationListenerWrapper(listener, name, server));
			server.addNotificationListener(name, listener, filter, handback);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Removes a listener from a registered MBean from the passed MBeanServer
	 * @param requestId The requestId of the request that registered the listener to be removed.
	 * @return true if the listener was found and unregistered, false otherwise
	 */
	public boolean removeNotificationListener(int requestId) {
		NotificationListenerWrapper wrapper = listeners.remove(requestId);
		if(wrapper!=null) {
			return wrapper.unregister();
		}
		return false;
	}
	
}
