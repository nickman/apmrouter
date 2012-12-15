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
package org.helios.apmrouter.jmx.mbeanserver.proxy;

import java.io.IOException;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServerConnection;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnectionNotification;
import javax.management.remote.JMXConnector;

import org.helios.apmrouter.jmx.JMXHelper;

/**
 * <p>Title: MBeanServerConnectionProxy</p>
 * <p>Description: A {@link MBeanServerConnection} implementation that serves as a proxy within the APMRouter server</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.jmx.mbeanserver.proxy.MBeanServerConnectionProxy</code></p>
 */

public class MBeanServerConnectionProxy extends NotificationBroadcasterSupport implements MBeanServerConnectionProxyMXBean {
	/** The delegate {@link MBeanServerConnection} */
	protected final MBeanServerConnection mbeanServerConnection;
	/** The {@link JMXConnector} to attach to the target MBeanServer */
	protected final JMXConnector jmxConnector;
	/** The agent host name */
	protected final String host;
	/** The agent name */
	protected final String agent;
	/** The underlying protocol name used to communicate with the agent */
	protected final String protocol;
	/** The JMX object name for this bean */
	protected final ObjectName objectName;
	/**
	 * Creates a new MBeanServerConnectionProxy
	 * @param jmxConnector The {@link JMXConnector} to attach to the target MBeanServer
	 * @param host The agent host name 
	 * @param agent The agent name  
	 * @param protocol The underlying protocol name used to communicate with the agent
	 * @throws IOException Thrown if the {@link MBeanServerConnection} cannot be established
	 */
	public MBeanServerConnectionProxy(JMXConnector jmxConnector, String host, String agent, String protocol) throws IOException {
		this.host = host;
		this.agent = agent;
		this.protocol = protocol;
		this.jmxConnector = jmxConnector;		
		mbeanServerConnection = this.jmxConnector.getMBeanServerConnection();
		objectName = JMXHelper.objectName(ObjectName.quote(new StringBuilder("org.helios.apmrouter.jmxproxy:protocol=")
			.append(protocol)
			.append(",host=").append(host)
			.append(",agent=").append(agent)
			.toString()			
		));
		try {
			if(JMXHelper.getHeliosMBeanServer().isRegistered(objectName)) {
				JMXHelper.getHeliosMBeanServer().unregisterMBean(objectName);
			}
			JMXHelper.getHeliosMBeanServer().registerMBean(this, objectName);
			this.jmxConnector.addConnectionNotificationListener(new NotificationListener(){
				@Override
				public void handleNotification(Notification notification, Object handback) {
					try {
						JMXHelper.getHeliosMBeanServer().unregisterMBean(objectName);
					} catch (Exception ex) {/* No Op */}
				}
			}, new NotificationFilter(){
				/**  */
				private static final long serialVersionUID = -7392322124255404480L;
				@Override
				public boolean isNotificationEnabled(Notification notification) {
					return ((notification instanceof JMXConnectionNotification) && JMXConnectionNotification.CLOSED.equals(notification.getType()));
				}
			}, null);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to register MBean for [" + objectName + "]", ex);
		}
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#createMBean(java.lang.String, javax.management.ObjectName)
	 */
	@Override
	public ObjectInstance createMBean(String className, ObjectName name)
			throws ReflectionException, InstanceAlreadyExistsException,
			MBeanRegistrationException, MBeanException,
			NotCompliantMBeanException, IOException {
		return mbeanServerConnection.createMBean(className, name);
	}
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#createMBean(java.lang.String, javax.management.ObjectName, javax.management.ObjectName)
	 */
	@Override
	public ObjectInstance createMBean(String className, ObjectName name,
			ObjectName loaderName) throws ReflectionException,
			InstanceAlreadyExistsException, MBeanRegistrationException,
			MBeanException, NotCompliantMBeanException,
			InstanceNotFoundException, IOException {
		return mbeanServerConnection.createMBean(className, name, loaderName);
	}
	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#createMBean(java.lang.String, javax.management.ObjectName, java.lang.Object[], java.lang.String[])
	 */
	@Override
	public ObjectInstance createMBean(String className, ObjectName name,
			Object[] params, String[] signature) throws ReflectionException,
			InstanceAlreadyExistsException, MBeanRegistrationException,
			MBeanException, NotCompliantMBeanException, IOException {
		return mbeanServerConnection.createMBean(className, name, params,
				signature);
	}
	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#createMBean(java.lang.String, javax.management.ObjectName, javax.management.ObjectName, java.lang.Object[], java.lang.String[])
	 */
	@Override
	public ObjectInstance createMBean(String className, ObjectName name,
			ObjectName loaderName, Object[] params, String[] signature)
			throws ReflectionException, InstanceAlreadyExistsException,
			MBeanRegistrationException, MBeanException,
			NotCompliantMBeanException, InstanceNotFoundException, IOException {
		return mbeanServerConnection.createMBean(className, name, loaderName,
				params, signature);
	}
	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#unregisterMBean(javax.management.ObjectName)
	 */
	@Override
	public void unregisterMBean(ObjectName name)
			throws InstanceNotFoundException, MBeanRegistrationException,
			IOException {
		mbeanServerConnection.unregisterMBean(name);
	}
	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#getObjectInstance(javax.management.ObjectName)
	 */
	@Override
	public ObjectInstance getObjectInstance(ObjectName name)
			throws InstanceNotFoundException, IOException {
		return mbeanServerConnection.getObjectInstance(name);
	}
	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#queryMBeans(javax.management.ObjectName, javax.management.QueryExp)
	 */
	@Override
	public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query)
			throws IOException {
		return mbeanServerConnection.queryMBeans(name, query);
	}
	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#queryNames(javax.management.ObjectName, javax.management.QueryExp)
	 */
	@Override
	public Set<ObjectName> queryNames(ObjectName name, QueryExp query)
			throws IOException {
		return mbeanServerConnection.queryNames(name, query);
	}
	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#isRegistered(javax.management.ObjectName)
	 */
	@Override
	public boolean isRegistered(ObjectName name) throws IOException {
		return mbeanServerConnection.isRegistered(name);
	}
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#getMBeanCount()
	 */
	@Override
	public Integer getMBeanCount() throws IOException {
		return mbeanServerConnection.getMBeanCount();
	}
	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#getAttribute(javax.management.ObjectName, java.lang.String)
	 */
	@Override
	public Object getAttribute(ObjectName name, String attribute)
			throws MBeanException, AttributeNotFoundException,
			InstanceNotFoundException, ReflectionException, IOException {
		return mbeanServerConnection.getAttribute(name, attribute);
	}
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#getAttributes(javax.management.ObjectName, java.lang.String[])
	 */
	@Override
	public AttributeList getAttributes(ObjectName name, String[] attributes)
			throws InstanceNotFoundException, ReflectionException, IOException {
		return mbeanServerConnection.getAttributes(name, attributes);
	}
	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#setAttribute(javax.management.ObjectName, javax.management.Attribute)
	 */
	@Override
	public void setAttribute(ObjectName name, Attribute attribute)
			throws InstanceNotFoundException, AttributeNotFoundException,
			InvalidAttributeValueException, MBeanException,
			ReflectionException, IOException {
		mbeanServerConnection.setAttribute(name, attribute);
	}
	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#setAttributes(javax.management.ObjectName, javax.management.AttributeList)
	 */
	@Override
	public AttributeList setAttributes(ObjectName name, AttributeList attributes)
			throws InstanceNotFoundException, ReflectionException, IOException {
		return mbeanServerConnection.setAttributes(name, attributes);
	}
	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#invoke(javax.management.ObjectName, java.lang.String, java.lang.Object[], java.lang.String[])
	 */
	@Override
	public Object invoke(ObjectName name, String operationName,
			Object[] params, String[] signature)
			throws InstanceNotFoundException, MBeanException,
			ReflectionException, IOException {
		return mbeanServerConnection.invoke(name, operationName, params,
				signature);
	}
	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#getDefaultDomain()
	 */
	@Override
	public String getDefaultDomain() throws IOException {
		return mbeanServerConnection.getDefaultDomain();
	}
	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#getDomains()
	 */
	@Override
	public String[] getDomains() throws IOException {
		return mbeanServerConnection.getDomains();
	}
	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#addNotificationListener(javax.management.ObjectName, javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	@Override
	public void addNotificationListener(ObjectName name,
			NotificationListener listener, NotificationFilter filter,
			Object handback) throws InstanceNotFoundException, IOException {
		mbeanServerConnection.addNotificationListener(name, listener, filter,
				handback);
	}
	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#addNotificationListener(javax.management.ObjectName, javax.management.ObjectName, javax.management.NotificationFilter, java.lang.Object)
	 */
	@Override
	public void addNotificationListener(ObjectName name, ObjectName listener,
			NotificationFilter filter, Object handback)
			throws InstanceNotFoundException, IOException {
		mbeanServerConnection.addNotificationListener(name, listener, filter,
				handback);
	}
	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#removeNotificationListener(javax.management.ObjectName, javax.management.ObjectName)
	 */
	@Override
	public void removeNotificationListener(ObjectName name, ObjectName listener)
			throws InstanceNotFoundException, ListenerNotFoundException,
			IOException {
		mbeanServerConnection.removeNotificationListener(name, listener);
	}
	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#removeNotificationListener(javax.management.ObjectName, javax.management.ObjectName, javax.management.NotificationFilter, java.lang.Object)
	 */
	@Override
	public void removeNotificationListener(ObjectName name,
			ObjectName listener, NotificationFilter filter, Object handback)
			throws InstanceNotFoundException, ListenerNotFoundException,
			IOException {
		mbeanServerConnection.removeNotificationListener(name, listener,
				filter, handback);
	}
	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#removeNotificationListener(javax.management.ObjectName, javax.management.NotificationListener)
	 */
	@Override
	public void removeNotificationListener(ObjectName name,
			NotificationListener listener) throws InstanceNotFoundException,
			ListenerNotFoundException, IOException {
		mbeanServerConnection.removeNotificationListener(name, listener);
	}
	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#removeNotificationListener(javax.management.ObjectName, javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	@Override
	public void removeNotificationListener(ObjectName name,
			NotificationListener listener, NotificationFilter filter,
			Object handback) throws InstanceNotFoundException,
			ListenerNotFoundException, IOException {
		mbeanServerConnection.removeNotificationListener(name, listener,
				filter, handback);
	}
	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#getMBeanInfo(javax.management.ObjectName)
	 */
	@Override
	public MBeanInfo getMBeanInfo(ObjectName name)
			throws InstanceNotFoundException, IntrospectionException,
			ReflectionException, IOException {
		return mbeanServerConnection.getMBeanInfo(name);
	}
	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanServerConnection#isInstanceOf(javax.management.ObjectName, java.lang.String)
	 */
	@Override
	public boolean isInstanceOf(ObjectName name, String className)
			throws InstanceNotFoundException, IOException {
		return mbeanServerConnection.isInstanceOf(name, className);
	}


	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
	 */
	@Override
	public void handleNotification(Notification notification, Object handback) {		
		sendNotification(notification);
	}
}
