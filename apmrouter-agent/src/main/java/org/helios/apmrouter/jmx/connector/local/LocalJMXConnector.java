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
package org.helios.apmrouter.jmx.connector.local;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.security.auth.Subject;

import org.helios.apmrouter.jmx.JMXHelper;

/**
 * <p>Title: LocalJMXConnector</p>
 * <p>Description: Contrived JMX connector that connects to a local in-vm {@link MBeanServer} so that local or remote {@link MBeanServerConnection}s can be referenced by a single string or {@link JMXServiceURL}.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.jmx.connector.local.LocalJMXConnector</code></p>
 */

public class LocalJMXConnector implements JMXConnector {
	/** The MBeanServer the connector connects to */
	protected MBeanServer mbeanServer = null;
	/** The provided JMXServiceURL */
	protected JMXServiceURL localURL = null;
	/** The JMXServiceURL provided default domain name */
	protected String domain = null;
	/** The faux connection ID assigned to this imaginary connection */
	protected String connectionId = null;
	/** A connection ID serial number generator */
	protected static final AtomicLong serial = new AtomicLong(0);
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#connect()
	 */
	@Override
	public void connect() throws IOException {
		String urlPath = localURL.getURLPath().trim();
		if(urlPath.startsWith("/")) {
			urlPath = urlPath.substring(1);			
		}
		domain = urlPath;
		if(domain==null || domain.trim().isEmpty()) {
			//throw new IOException("Invalid domain. Was empty or null", new Throwable());
			domain = "DefaultDomain";
		}
		domain = domain.trim();
		mbeanServer = JMXHelper.getLocalMBeanServer(domain, false);
		connectionId = "local:" + domain + ":" + serial.incrementAndGet();
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
		return mbeanServer;
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#getMBeanServerConnection(javax.security.auth.Subject)
	 */
	@Override
	public MBeanServerConnection getMBeanServerConnection(Subject delegationSubject) throws IOException {
		return mbeanServer;
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#close()
	 */
	@Override
	public void close() throws IOException {
		// No Op
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#addConnectionNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	@Override
	public void addConnectionNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) {
		// No Op
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#removeConnectionNotificationListener(javax.management.NotificationListener)
	 */
	@Override
	public void removeConnectionNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
		// No Op
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#removeConnectionNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	@Override
	public void removeConnectionNotificationListener(NotificationListener l, NotificationFilter f, Object handback) throws ListenerNotFoundException {
		// No Op
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnector#getConnectionId()
	 */
	@Override
	public String getConnectionId() throws IOException {
		return connectionId;
	}

	
	@SuppressWarnings("unused")
	public static void main(String[] args) {
		try {
			log("Local JMXServiceURL Test");
			//System.setProperty("jmx.remote.protocol.provider.pkgs", "org.helios.apmrouter.jmx.connector");
			JMXServiceURL jurl = new JMXServiceURL("service:jmx:local://");
			log("URL:[" + jurl.getURLPath() + "]");
			JMXConnector connector = JMXConnectorFactory.connect(jurl, Collections.singletonMap("jmx.remote.protocol.provider.class.loader", LocalJMXConnector.class.getClassLoader()));
			log("Connected to [" + connector.getMBeanServerConnection().getDefaultDomain() + "]");
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}		
	}

	public static void log(Object msg) {
		System.out.println(msg);
	}
	
}
