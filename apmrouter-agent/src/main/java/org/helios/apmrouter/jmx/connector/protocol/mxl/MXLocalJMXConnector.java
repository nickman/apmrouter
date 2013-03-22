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
package org.helios.apmrouter.jmx.connector.protocol.mxl;

import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.jmx.notifications.NotificationListenerContainer;

import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.security.auth.Subject;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>Title: MXLocalJMXConnector</p>
 * <p>Description: Implements a {@link JMXConnector} to a connected agent from within the APMRouter server</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.jmx.connector.mxl.MXLocalJMXConnector</code></p>
 */

public class MXLocalJMXConnector implements JMXConnector {
	/** The agent's host name */
	protected final String host;
	/** The agent name */
	protected final String agent;
	/** The protocol to use to communicate with the agent */
	protected final String protocol;
	/** The default domain of the target MBeanServer */
	protected final String domain;
	/** The ObjectName query used to find a protocol adaptor for the represented agent */
	protected final ObjectName mbeanQuery;
	/** A set of registered connector listeners */
	protected final Set<NotificationListenerContainer> listeners = new CopyOnWriteArraySet<NotificationListenerContainer>();
	/** The connection id  */
	protected final String connectionId;
	/** Connection id serial number counters indexed by the connectionId prefixes */
	protected static final ConcurrentHashMap<String, AtomicLong> connectionIds = new ConcurrentHashMap<String, AtomicLong>();
	
	/** The ObjectName of the protocol adaptor for the represented agent */
	protected ObjectName protocolAdapter;
	
	/** The acquired mbeanserver connection */
	protected MBeanServerConnection connection;
	
	/** The domain where JMX MBeanServer proxies are registered */
	public static final String JMX_PROXY_DOMAIN = "org.helios.apmrouter.jmxproxy";
	
	
	/**
	 * Creates a new MXLocalJMXConnector
	 * @param host The agent's host name
	 * @param agent The agent name
	 * @param protocol The protocol to use to communicate with the agent
	 * @param domain The default domain of the target MBeanServer
	 */
	public MXLocalJMXConnector(String host, String agent, String protocol, String domain) {
		this.host = host;
		this.agent = agent;
		this.protocol = protocol;
		this.domain = domain;
		Hashtable<String, String> props = new Hashtable<String, String>(4);
		props.put("host", this.host);
		props.put("agent", this.agent);
		props.put("domain", this.domain);
		props.put("protocol", this.protocol==null ? "*" : this.protocol);
		mbeanQuery = JMXHelper.objectName(JMX_PROXY_DOMAIN, props);
		String connectionIdPrefix = this.protocol==null ? (agent + "@" + host) : (this.protocol + "://" + agent + "@" + host);
		connectionIds.putIfAbsent(connectionIdPrefix, new AtomicLong(0));
		connectionId = 	connectionIdPrefix + "-" + connectionIds.get(connectionIdPrefix).incrementAndGet();		
	}
	
	/**
	 * Creates a new MXLocalJMXConnector
	 * @param host The agent's host name
	 * @param agent The agent name
	 * @param domain The default domain of the target MBeanServer
	 */
	public MXLocalJMXConnector(String host, String agent, String domain) {
		this(host, agent, null, domain);
	}
	

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#connect()
	 */
	@Override
	public void connect() throws IOException {		
		if(protocol==null) {
			Set<ObjectName> adapters = JMXHelper.getHeliosMBeanServer().queryNames(mbeanQuery, null);
			if(adapters.isEmpty()) throw new IOException("Failed to find protocol adapter to acquire connection to [" + agent + "@" + host + "]\n\tUsing search filter [" + mbeanQuery + "]", new Throwable());
			protocolAdapter = adapters.iterator().next();
		} else {
			protocolAdapter = mbeanQuery;
		}
		if(!JMXHelper.getHeliosMBeanServer().isRegistered(protocolAdapter)) {
			throw new IOException("No protocol adapter named [" + protocolAdapter + "] to acquire connection to [" + agent + "@" + host + "]", new Throwable());
		}		
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#connect(java.util.Map)
	 */
	@Override
	public void connect(Map<String, ?> env) throws IOException {
		connect();
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#getMBeanServerConnection()
	 */
	@Override
	public MBeanServerConnection getMBeanServerConnection() throws IOException {
		return MBeanServerInvocationHandler.newProxyInstance(JMXHelper.getHeliosMBeanServer(), protocolAdapter, MBeanServerConnection.class, true);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#getMBeanServerConnection(javax.security.auth.Subject)
	 * FIXME: Need to implement a secure connection
	 */
	@Override
	public MBeanServerConnection getMBeanServerConnection(Subject delegationSubject) throws IOException {
		throw new UnsupportedOperationException("Authenticated proxy connection not supported");
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#close()
	 */
	@Override
	public void close() throws IOException {
		
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#addConnectionNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	@Override
	public void addConnectionNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) {
		if(listener!=null) {
			listeners.add(new NotificationListenerContainer(listener, filter, handback));
		}

	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#removeConnectionNotificationListener(javax.management.NotificationListener)
	 */
	@Override
	public void removeConnectionNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
		if(listener!=null) {
			if(!listeners.remove(new NotificationListenerContainer(listener, null, null))) {
				throw new ListenerNotFoundException();
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#removeConnectionNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	@Override
	public void removeConnectionNotificationListener(NotificationListener l, NotificationFilter f, Object handback) throws ListenerNotFoundException {
		if(l!=null) {
			if(!listeners.remove(new NotificationListenerContainer(l, f, handback))) {
				throw new ListenerNotFoundException();
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#getConnectionId()
	 */
	@Override
	public String getConnectionId() throws IOException {
		return connectionId;
	}

}
