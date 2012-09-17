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
package org.helios.apmrouter.server;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.helios.apmrouter.jmx.JMXHelper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.naming.ObjectNamingStrategy;

/**
 * <p>Title: ServerComponentBean</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.ServerComponentBean</code></p>
 */

public abstract class ServerComponentBean extends ServerComponent implements 
		ApplicationContextAware, 
		BeanNameAware, 
		SmartApplicationListener,
		ApplicationEventMulticaster,
		ObjectNamingStrategy,
		InitializingBean,
		DisposableBean {

	/** The application context for this bean */
	protected GenericApplicationContext applicationContext = null;
	/** The bean name for this bean */
	protected String beanName = null;
	/** The delegate app multicaster */
	protected SimpleApplicationEventMulticaster eventMulticaster = null;
	/** The event supporting executor */	
	protected Executor eventExecutor = null;
	/** The ordering priority of this component */
	protected int priority = Ordered.LOWEST_PRECEDENCE;
	/** The JMX ObjectName for this component */
	protected ObjectName objectName = null;
	/** The started state of this component */
	protected final AtomicBoolean started = new AtomicBoolean(false);
	
	/**
	 * {@inheritDoc}
	 * @see org.springframework.context.ApplicationListener#onApplicationEvent(org.springframework.context.ApplicationEvent)
	 */
	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		debug("Received ApplicationEvent [", event, "]");

	}
	
	/**
	 * {@inheritDoc}
	 * @see org.springframework.jmx.export.naming.ObjectNamingStrategy#getObjectName(java.lang.Object, java.lang.String)
	 */
	@Override
	public ObjectName getObjectName(Object managedBean, String beanKey) throws MalformedObjectNameException {
		StringBuilder b = new StringBuilder(getClass().getPackage().getName());
		b.delete(b.lastIndexOf("."), b.length()-1);
		objectName = JMXHelper.objectName(b.append(":service=ThreadPool,name=").append(beanName));
		return objectName;
	}	

	/**
	 * {@inheritDoc}
	 * @see org.springframework.core.Ordered#getOrder()
	 */
	@Override
	public int getOrder() {
		return priority;
	}


	/**
	 * {@inheritDoc}
	 * @see org.springframework.context.event.SmartApplicationListener#supportsEventType(java.lang.Class)
	 */
	@Override
	public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
		return false;
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.context.event.SmartApplicationListener#supportsSourceType(java.lang.Class)
	 */
	@Override
	public boolean supportsSourceType(Class<?> sourceType) {
		return false;
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.beans.factory.BeanNameAware#setBeanName(java.lang.String)
	 */
	@Override
	public void setBeanName(String name) {
		beanName = name;
		log = Logger.getLogger(getClass().getName() + "." + beanName);

	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		if(applicationContext instanceof GenericApplicationContext) {
			this.applicationContext = (GenericApplicationContext)applicationContext;
			eventMulticaster = new SimpleApplicationEventMulticaster(applicationContext);
			if(eventExecutor!=null) {
				eventMulticaster.setTaskExecutor(eventExecutor);
			}
		} else {
			throw new IllegalArgumentException("This bean requires a GenericApplicationContext but was passed a [" + applicationContext.getClass().getName() + "]", new Throwable());
		}

	}

	/**
	 * Add a listener to be notified of all events. 
	 * @param listener The listener to add
	 * @see org.springframework.context.event.AbstractApplicationEventMulticaster#addApplicationListener(org.springframework.context.ApplicationListener)
	 */
	@Override
	public void addApplicationListener(@SuppressWarnings("rawtypes") ApplicationListener listener) {
		if(eventMulticaster==null) throw new IllegalStateException("This component's eventMulticaster has not been initialized yet", new Throwable());
		eventMulticaster.addApplicationListener(listener);
	}

	/**
	 * Add a listener bean to be notified of all events. 
	 * @param listenerBeanName The name of the bean to register
	 * @see org.springframework.context.event.AbstractApplicationEventMulticaster#addApplicationListenerBean(java.lang.String)
	 */
	@Override
	public void addApplicationListenerBean(String listenerBeanName) {
		if(eventMulticaster==null) throw new IllegalStateException("This component's eventMulticaster has not been initialized yet", new Throwable());
		eventMulticaster.addApplicationListenerBean(listenerBeanName);
	}

	/**
	 * Removes an application listener
	 * @param listener the listener to remove
	 * @see org.springframework.context.event.AbstractApplicationEventMulticaster#removeApplicationListener(org.springframework.context.ApplicationListener)
	 */
	@Override
	public void removeApplicationListener(@SuppressWarnings("rawtypes") ApplicationListener listener) {
		if(eventMulticaster==null) throw new IllegalStateException("This component's eventMulticaster has not been initialized yet", new Throwable());
		eventMulticaster.removeApplicationListener(listener);
	}

	/**
	 * Multicast the given application event to appropriate listeners. 
	 * @param event The event to multicast
	 * @see org.springframework.context.event.SimpleApplicationEventMulticaster#multicastEvent(org.springframework.context.ApplicationEvent)
	 */
	@Override
	public void multicastEvent(ApplicationEvent event) {
		if(eventMulticaster==null) throw new IllegalStateException("This component's eventMulticaster has not been initialized yet", new Throwable());
		eventMulticaster.multicastEvent(event);
	}

	/**
	 * Removes an application listener bean
	 * @param listenerBeanName The bean name of the listener to remove
	 * @see org.springframework.context.event.AbstractApplicationEventMulticaster#removeApplicationListenerBean(java.lang.String)
	 */
	@Override
	public void removeApplicationListenerBean(String listenerBeanName) {
		if(eventMulticaster==null) throw new IllegalStateException("This component's eventMulticaster has not been initialized yet", new Throwable());
		eventMulticaster.removeApplicationListenerBean(listenerBeanName);
	}

	/**
	 * Removes all registered listeners
	 * @see org.springframework.context.event.AbstractApplicationEventMulticaster#removeAllListeners()
	 */
	@Override
	public void removeAllListeners() {
		if(eventMulticaster==null) throw new IllegalStateException("This component's eventMulticaster has not been initialized yet", new Throwable());
		eventMulticaster.removeAllListeners();
	}

	/**
	 * Sets the spring event executor 
	 * @param eventExecutor the eventExecutor to set
	 */
	@Autowired(required=false)
	@Qualifier("SpringEvent")
	public void setEventExecutor(Executor eventExecutor) {
		this.eventExecutor = eventExecutor;
		if(eventMulticaster!=null) {
			eventMulticaster.setTaskExecutor(eventExecutor);
		}
	}
	
	/**
	 * Called when bean is started.
	 */
	@Override
	@ManagedOperation
	public final void start() throws Exception {
		super.start();
		if(isStarted()) throw new IllegalStateException("Cannot start component once it is started", new Throwable());
		info(banner("Starting [", beanName, "]"));
		doStart();
		started.set(true);
		info(banner("Started [", beanName, "]"));
	}
	
	/**
	 * Called when bean is stopped
	 */
	@Override
	@ManagedOperation
	public final void stop() {
		if(!isStarted()) throw new IllegalStateException("Cannot stop component once it is stopped", new Throwable());
		try {
			info(banner("Stopping [", beanName, "]"));
			doStop();			
			info(banner("Stopped [", beanName, "]"));
		} finally {
			started.set(false);
		}
		super.stop();
	}
	
	/**
	 * To be implemented by concrete classes that have a specific start operation
	 * @throws Exception thrown if startup fails
	 */
	protected void doStart() throws Exception {}
	
	/**
	 * To be implemented by concrete classes that have a specific stop operation
	 */
	protected void doStop(){};
	
	/**
	 * Indicates if this component is started
	 * @return true if this component is started, false otherwise
	 */
	@ManagedAttribute
	public boolean isStarted() {
		return started.get();
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	@Override
	public void destroy() throws Exception {
		stop();		
	}

	/**
	 * {@inheritDoc}
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		start();
	}

	/**
	 * Returns this components JMX ObjectName
	 * @return the objectName
	 */
	public ObjectName getObjectName() {
		return objectName;
	}

	/**
	 * Sets this components JMX ObjectName
	 * @param objectName the objectName to set
	 */
	public void setObjectName(ObjectName objectName) {
		this.objectName = objectName;
	}

}
