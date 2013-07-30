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
package org.helios.apmrouter.jmx.notifications;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationFilterSupport;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.helios.apmrouter.jmx.JMXHelper;

/**
 * <p>Title: NotificationListenerControl</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.jmx.notifications.NotificationListenerControl</code></p>
 */

public class NotificationListenerControl implements Closeable, NotificationListener, NotificationFilter {
	/**  */
	private static final long serialVersionUID = 4828378016083478296L;
	/** The MBeanServer the listener will be registered with */
	protected final MBeanServerConnection connection;
	/** The ObjectName of the MBean[s] the listener will be registered with */
	protected final ObjectName objectName;
	/** Indicates if the target ObjectName is a wildcard */
	protected final boolean wildCard;
	/** Indicates if the target connection is local (i.e. implements MBeanServer) */
	protected final boolean localConnection;	
	
	/** Indicates that the listener registration object name set is final and suppresses the listener for new matching object names */
	protected final boolean finalSub;
	/** The delegate NotificationListener */
	protected final NotificationListener listenerDelegate;
	/** The delegate NotificationFilter */
	protected final NotificationFilter filterDelegate;
	/** The subscribed ObjectName subscription set */
	protected final Set<ObjectName> subscriptionSet;
	/** The optional handback to return to the listener */
	protected final Object handback;
	/** The JMXConnector providing the MBeanServer the listener will be registered with, which if provided will be closed on a call to {@link #close()} */
	protected JMXConnector connector = null;		

	/**
	 * Sets a JMXConnector which, if not null, will be closed on a call to {@link #close()}
	 * @param connector the connector to close
	 */
	protected void setJMXConnector(JMXConnector connector) {
		this.connector = connector;
	}
	
	
	/** A notification listener registered to listen on new MBean notifications */
	protected final NotificationListener newMBeanListener = new NotificationListener() {
		
		/**
		 * {@inheritDoc}
		 * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
		 */
		@Override
		public void handleNotification(Notification notification, Object handback) {
			if(notification instanceof MBeanServerNotification) {
				MBeanServerNotification mbsn = (MBeanServerNotification)notification;
				if(!subscriptionSet.contains(mbsn.getMBeanName())) {
					synchronized(subscriptionSet) {
						if(!subscriptionSet.contains(mbsn.getMBeanName())) {
							subscribe(mbsn.getMBeanName());
						}
					}
				}
			}
		}
	};
	
	/** A filter to allow only new MBean publication event notifications */
	protected static final NotificationFilterSupport newMBeanNotificationFilter = new NotificationFilterSupport();
	
	static {
		newMBeanNotificationFilter.disableAllTypes();
		newMBeanNotificationFilter.enableType(MBeanServerNotification.REGISTRATION_NOTIFICATION);
	}
	
	/**
	 * Creates a new NotificationListenerControl
	 * @param connection The MBeanServer the listener will be registered with
	 * @param objectName The ObjectName of the MBean[s] the listener will be registered with
	 * @param finalSub Indicates that the listener registration object name set is final and suppresses the listener for new matching object names
	 * @param listenerDelegate The delegate NotificationListener
	 * @param filterDelegate The delegate NotificationFilter
	 * @param handback The optional handback to return to the listener
	 */
	protected NotificationListenerControl(MBeanServerConnection connection,
			ObjectName objectName, boolean finalSub,
			NotificationListener listenerDelegate,
			NotificationFilter filterDelegate,
			Object handback) {
		this.connection = connection;
		localConnection = this.connection instanceof MBeanServer;
		this.objectName = objectName;
		this.finalSub = finalSub;
		this.listenerDelegate = listenerDelegate;
		this.filterDelegate = filterDelegate;
		wildCard = this.objectName.isPattern();
		subscriptionSet = wildCard ? Collections.synchronizedSet(new HashSet<ObjectName>()) : null;
		this.handback = handback;
		if(wildCard && !this.finalSub) {
			try {
				connection.addNotificationListener(MBeanServerDelegate.DELEGATE_NAME, this, newMBeanNotificationFilter, null);
			} catch (Exception ex) {
				ex.printStackTrace(System.err);
			}
		}
	}
	
	/**
	 * Subscribes this listener to the MBean identified by the passed ObjectName
	 * @param on The objetc name of the MBean to subscribe to notifications from
	 */
	protected void subscribe(ObjectName on) {
		Set<ObjectName> subscribed = on.isPattern() ? new HashSet<ObjectName>() : new HashSet<ObjectName>(Arrays.asList(on));
		try {
			Set<ObjectName> objectNames = on.isPattern() ? new HashSet<ObjectName>() : new HashSet<ObjectName>(Arrays.asList(on));
			
			for(ObjectName objectName : objectNames) {
				if(!subscriptionSet.contains(objectName)) {
					synchronized(subscriptionSet) {
						if(!subscriptionSet.contains(objectName)) {
							connection.addNotificationListener(objectName, this, filterDelegate, handback);
							subscribed.add(objectName);											
						}
					}
				}
			}
			this.subscriptionSet.addAll(subscribed);
		} catch (Exception ex) {
			if(!subscribed.isEmpty()) unsubAll(subscribed);
			throw new RuntimeException("Failed to subscribe to [" + on + "]", ex);
		}
	}
	
	/**
	 * Unsubscribes this listener from all the passed object names
	 * @param objectNames The object names to unsubscribe from
	 */
	protected void unsubAll(Set<ObjectName> objectNames) {
		for(ObjectName objectName : objectNames) {
			try { connection.removeNotificationListener(objectName, this); } catch (Exception e) {}
		}
	}
	
	

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationFilter#isNotificationEnabled(javax.management.Notification)
	 */
	@Override
	public boolean isNotificationEnabled(Notification notification) {		
		return filterDelegate==null ? true : filterDelegate.isNotificationEnabled(notification);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
	 */
	@Override
	public void handleNotification(Notification notification, Object handback) {
		listenerDelegate.handleNotification(notification, handback);
	}

	/**
	 * {@inheritDoc}
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {
		for(ObjectName on: subscriptionSet) {
			try { connection.removeNotificationListener(on, this); } catch (Exception e) {}
		}
		if(wildCard && !finalSub) {
			try { connection.removeNotificationListener(MBeanServerDelegate.DELEGATE_NAME, this.newMBeanListener); } catch (Exception e) {}
		}
		if(connector!=null) {
			try { connector.close(); } catch (Exception ex) {}
		}
	}
	
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
	 * <p>Title: Builder</p>
	 * <p>Description: A fluent style builder for {@link NotificationListenerControl}s</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.jmx.notifications.NotificationListenerControl.Builder</code></p>
	 */
	public static class Builder {
		/** The ObjectName of the MBean[s] the listener will be registered with */
		protected final ObjectName objectName;
		/** The delegate NotificationListener */
		protected final NotificationListener listenerDelegate;
		
		// =============
		// Optional
		// =============
		/** The MBeanServer the listener will be registered with */
		protected MBeanServerConnection connection;
		/** The JMXConnection providing the MBeanServer the listener will be registered with */
		protected JMXConnector connector = null;		
		
		/** Indicates that the listener registration object name set is final and suppresses the listener for new matching object names */
		protected boolean finalSub = false;
		/** The delegate NotificationFilter */
		protected NotificationFilter filterDelegate;
		/** Notification type filter ins */
		protected final Set<String> filterIns = new HashSet<String>();
		/** Notification type filter outs */
		protected final Set<String> filterOuts = new HashSet<String>();
		/** The optional handback to return to the listener */
		protected Object handback;
		/** The jmx service url to connect to */
		protected String jmxServiceUrl;
		/** The jmx credentials to use when connecting */
		protected String[] credentials;
		
		
		/**
		 * Creates a new Builder. If no connection or remote url is provided, will connect to the local helios mbeanserver.
		 * @param objectName The ObjectName of the MBean[s] the listener will be registered with
		 * @param listenerDelegate The delegate NotificationListener
		 */
		protected Builder(ObjectName objectName,
				NotificationListener listenerDelegate) {
			this.objectName = objectName;
			this.listenerDelegate = listenerDelegate;
		}
		
		
		/**
		 * Builds a new {@link NotificationListenerControl}
		 * @return a new {@link NotificationListenerControl}
		 */
		public NotificationListenerControl build() {
			NotificationFilter nf = null;
			if(filterDelegate!=null) {
				nf = filterDelegate;
			} else {
				if(!filterIns.isEmpty() || !filterOuts.isEmpty()) {
					NotificationFilterSupport nfs = new NotificationFilterSupport();
					for(String prefix: filterIns) {
						nfs.enableType(prefix);
					}
					for(String prefix: filterOuts) {
						nfs.disableType(prefix);
					}
					nf = nfs;
				}
			}
			NotificationListenerControl nlc = new NotificationListenerControl(getMBeanServerConnection(), objectName, finalSub, listenerDelegate, nf, handback);
			nlc.setJMXConnector(connector);
			return nlc;
		}
		
		/**
		 * Acquires the MBeanServerConnection defined by this builders config
		 * @return the MBeanServerConnection
		 */
		protected MBeanServerConnection getMBeanServerConnection() {
			if(connection!= null) return connection;
			if(jmxServiceUrl!=null && !jmxServiceUrl.trim().isEmpty()) {
				Map<String, ?> environment = credentials==null ? null : Collections.singletonMap(JMXConnector.CREDENTIALS, credentials);
				try {
					JMXConnectorFactory.connect(new JMXServiceURL(jmxServiceUrl), environment);
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
				
			}
			return JMXHelper.getHeliosMBeanServer();
		}
		
		/**
		 * Sets the optional handback to return to the listener
		 * @param handback optional handback to return to the listener
		 * @return this builder
		 */
		public Builder handback(Object handback) {
			this.handback = handback;
			return this;
		}
		
		/**
		 * Sets the JMX service URL of the remote MBeanServer to connect to
		 * @param jmxServiceUrl the the JMX service URL of the remote MBeanServer to connect to
		 * @return this builder
		 */
		public Builder jmxServiceUrl(String jmxServiceUrl) {
			this.jmxServiceUrl = jmxServiceUrl.trim();
			return this;
		}
		
		/**
		 * Sets the credentials to connect with
		 * @param userName The username
		 * @param password The password
		 * @return this builder
		 */
		public Builder credentials(String userName, String password) {
			credentials = new String[]{userName, password};
			return this;
		}

		/**
		 * Sets the connection to register listeners with
		 * @param connection the connection to register listeners with
		 * @return this builder
		 */
		public Builder connection(MBeanServerConnection connection) {
			this.connection = connection;
			return this;
		}
		
		/**
		 * Sets the connector to acquire the MBeanServerConnection from
		 * @param connector the connector to acquire the MBeanServerConnection from
		 * @return this builder
		 */
		public Builder connector(JMXConnector connector) {
			this.connector = connector;
			return this;
		}
		

		/**
		 * Sets a notification filter
		 * @param filterDelegate the filterDelegate to set
		 * @return this builder
		 */
		public Builder filter(NotificationFilter filterDelegate) {
			this.filterDelegate = filterDelegate;
			return this;
		}
		
		/**
		 * Converts the passed charsequence array to trimmed strings and adds them to the target set
		 * @param target The target set to add to 
		 * @param filters The filter prefixes to add
		 */
		protected void filters(final Set<String> target, CharSequence...filters) {
			if(filters!=null && filters.length!=0) {
				for(CharSequence f: filters) {
					if(f==null) continue;
					String fs = f.toString().trim();
					if(fs.isEmpty()) continue;
					target.add(fs);
				}
			}
		}
		
		/**
		 * Adds the passed filters to the filter ins or notification type prefixes that should match
		 * @param filters The filters to add
		 * @return this builder
		 */
		public Builder filterIn(CharSequence...filters) {
			filters(filterIns, filters);
			return this;
		}

		/**
		 * Adds the passed filters to the filter outs or notification type prefixes that should not match
		 * @param filters The filters to add
		 * @return this builder
		 */
		public Builder filterOut(CharSequence...filters) {
			filters(filterOuts, filters);
			return this;
		}
		
		/**
		 * Sets a flag indicating that the initial subscription set of MBeans is final
		 * @param finalSub the finalSub to set
		 * @return this builder
		 */
		public Builder finalSub(boolean finalSub) {
			this.finalSub = finalSub;
			return this;
		}
	}

	
}
