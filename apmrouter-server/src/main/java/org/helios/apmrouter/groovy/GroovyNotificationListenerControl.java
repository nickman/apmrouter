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
package org.helios.apmrouter.groovy;

import groovy.lang.Closure;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.helios.apmrouter.jmx.notifications.NotificationListenerControl;
import org.helios.apmrouter.jmx.notifications.NotificationListenerControl.Builder;

/**
 * <p>Title: GroovyNotificationListenerControl</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.groovy.GroovyNotificationListenerControl</code></p>
 */

public class GroovyNotificationListenerControl extends NotificationListenerControl {
	
	/**  */
	private static final long serialVersionUID = -6746334609195476766L;
	
	/**
	 * Creates a new builder
	 * @param objectName The ObjectName of the MBean[s] the listener will be registered with
	 * @param listenerDelegate The delegate NotificationListener
	 * @return a new builder
	 */
	public static Builder builder(ObjectName objectName,
				NotificationListener listenerDelegate) {
		return new Builder(objectName, listenerDelegate);
	}
	
	/**
	 * Creates a new builder
	 * @param objectName The ObjectName of the MBean[s] the listener will be registered with
	 * @param listenerDelegate The delegate NotificationListener
	 * @return a new builder
	 */
	public static Builder builder(ObjectName objectName,
				Closure<Void> listenerDelegate) {
		return new Builder(objectName, listenerDelegate);
	}	

	/**
	 * <p>Title: Builder</p>
	 * <p>Description: A fluent style builder to create {@link GroovyNotificationListenerControl} instances </p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.groovy.Builder</code></p>
	 */
	public static class Builder extends NotificationListenerControl.Builder {

		/**
		 * Creates a new Builder
		 * @param objectName The ObjectName of the MBean[s] the listener will be registered with
		 * @param listenerDelegate The delegate NotificationListener
		 */
		public Builder(ObjectName objectName,
				NotificationListener listenerDelegate) {
			super(objectName, listenerDelegate);
		}
		
		/**
		 * Creates a new Builder
		 * @param objectName The ObjectName of the MBean[s] the listener will be registered with
		 * @param listenerDelegate The delegate NotificationListener
		 */
		public Builder(ObjectName objectName,
				Closure<?> listenerDelegate) {
			super(objectName, listener(listenerDelegate));
		}
		
		/**
		 * Creates a new {@link NotificationListener} that wraps the passed closure
		 * @param closure The closure that accepts the subscribed emmited notifications
		 * @return a new {@link NotificationListener}
		 */
		protected static NotificationListener listener(final Closure<?> closure) {
			final int maxParams = closure.getMaximumNumberOfParameters();
			return new NotificationListener() {
				@Override
				public void handleNotification(Notification notification, Object handback) {
					if(maxParams==1) {
						closure.call(notification);
					} else if(maxParams==2) {
						closure.call(notification, handback);
					}
				}
			};
		}
		
		/**
		 * Sets a notification filter as a wrapped groovy closure
		 * @param closure the notification filtering groovy closure
		 * @return this builder
		 */
		public Builder filter(final Closure<Boolean> closure) {
			this.filterDelegate = new NotificationFilter() {
				/**  */
				private static final long serialVersionUID = -2096714032855942822L;
				/** The filtering closure */
				private Closure<Boolean> cl = closure;
				/**
				 * {@inheritDoc}
				 * @see javax.management.NotificationFilter#isNotificationEnabled(javax.management.Notification)
				 */
				@Override
				public boolean isNotificationEnabled(Notification notification) {					
					return cl.call(notification);
				}
				
				/**
				 * @param out
				 * @throws IOException
				 */
				private void writeObject(ObjectOutputStream out) throws IOException {
					out.writeObject(cl.dehydrate());
					
				}
				/**
				 * @param in
				 * @throws IOException
				 * @throws ClassNotFoundException
				 * @FIXME: I have no idea if this will actually work
				 */
				private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
					Closure<Boolean> _cl = (Closure<Boolean>)in.readObject();
					cl.rehydrate(_cl, this, cl);
				}
			};
			return this;
		}
		
		
		
	}

	/**
	 * Creates a new GroovyNotificationListenerControl
	 * @param connection The MBeanServer the listener will be registered with
	 * @param objectName The ObjectName of the MBean[s] the listener will be registered with
	 * @param finalSub Indicates that the listener registration object name set is final and suppresses the listener for new matching object names
	 * @param listenerDelegate The delegate NotificationListener
	 * @param filterDelegate The delegate NotificationFilter
	 * @param handback The optional handback to return to the listener
	 */
	protected GroovyNotificationListenerControl(MBeanServerConnection connection,
			ObjectName objectName, boolean finalSub,
			NotificationListener listenerDelegate,
			NotificationFilter filterDelegate, Object handback) {
		super(connection, objectName, finalSub, listenerDelegate,
				filterDelegate, handback);
		// TODO Auto-generated constructor stub
	}

}
