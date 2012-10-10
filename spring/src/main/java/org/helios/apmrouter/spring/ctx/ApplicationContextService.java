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
package org.helios.apmrouter.spring.ctx;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.helios.apmrouter.jmx.JMXHelper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * <p>Title: ApplicationContextService</p>
 * <p>Description: Exposes an application context as a JMX MBean.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.spring.ctx.ApplicationContextService</code></p>
 */

public class ApplicationContextService implements ApplicationContext, ApplicationContextServiceMBean {
	/** The wrapped app context */
	protected final GenericApplicationContext delegate;
	/** The app context service JMX ObjectName */
	protected final ObjectName objectName;
	/** The unique identifier of the app context used to add a key prop to the object name */
	protected final String id;
	/** The instance logger */
	protected final Logger log;
	
	/** Serial number factory for providing IDs for app ctxs with empty display names */
	protected static final AtomicLong serial = new AtomicLong(0L);
	
	/**
	 * Creates a new ApplicationContextService
	 * @param delegate The wrapped app context
	 * @return the created ApplicationContextService
	 */
	public static ApplicationContextService register(GenericApplicationContext delegate) {
		return new ApplicationContextService(delegate);
	}
	
	
	/**
	 * Creates a new ApplicationContextService
	 * @param delegate The wrapped app context
	 */
	public ApplicationContextService(GenericApplicationContext delegate) {
		this.delegate = delegate;
		id = getId(this.delegate);
		log = Logger.getLogger(getClass().getName() + "." + id);
		objectName = JMXHelper.objectName(OBJECT_NAME_PREF + id);
		try {
			JMXHelper.getHeliosMBeanServer().registerMBean(this, objectName);
		} catch (Exception ex) {
			if(log.isDebugEnabled()) {
				log.warn("Failed to register management interface. Continuing without.", ex);
			} else {
				log.warn("Failed to register management interface [" + ex + "]. Continuing without.");
			}
		}
	}
	
	/**
	 * Determines the unique ID assigned to the passed application context
	 * @param appCtx application context to assign an ID for
	 * @return the ID for the passed application context
	 */
	protected static String getId(ApplicationContext appCtx) {
		String dn = appCtx.getDisplayName().trim();
		if(dn.isEmpty()) {
			return "#" + serial.incrementAndGet();
		}
		return dn;		
	}

	/**
	 * @return
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	/**
	 * @param classLoader
	 * @see org.springframework.core.io.DefaultResourceLoader#setClassLoader(java.lang.ClassLoader)
	 */
	public void setClassLoader(ClassLoader classLoader) {
		delegate.setClassLoader(classLoader);
	}

	/**
	 * @return
	 * @see org.springframework.core.io.DefaultResourceLoader#getClassLoader()
	 */
	public ClassLoader getClassLoader() {
		return delegate.getClassLoader();
	}

	/**
	 * @param obj
	 * @return
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}

	/**
	 * @param parent
	 * @see org.springframework.context.support.GenericApplicationContext#setParent(org.springframework.context.ApplicationContext)
	 */
	public void setParent(ApplicationContext parent) {
		delegate.setParent(parent);
	}

	/**
	 * @param id
	 * @see org.springframework.context.support.GenericApplicationContext#setId(java.lang.String)
	 */
	public void setId(String id) {
		delegate.setId(id);
	}

	/**
	 * @param allowBeanDefinitionOverriding
	 * @see org.springframework.context.support.GenericApplicationContext#setAllowBeanDefinitionOverriding(boolean)
	 */
	public void setAllowBeanDefinitionOverriding(
			boolean allowBeanDefinitionOverriding) {
		delegate.setAllowBeanDefinitionOverriding(allowBeanDefinitionOverriding);
	}

	/**
	 * @param allowCircularReferences
	 * @see org.springframework.context.support.GenericApplicationContext#setAllowCircularReferences(boolean)
	 */
	public void setAllowCircularReferences(boolean allowCircularReferences) {
		delegate.setAllowCircularReferences(allowCircularReferences);
	}

	/**
	 * @param resourceLoader
	 * @see org.springframework.context.support.GenericApplicationContext#setResourceLoader(org.springframework.core.io.ResourceLoader)
	 */
	public void setResourceLoader(ResourceLoader resourceLoader) {
		delegate.setResourceLoader(resourceLoader);
	}

	/**
	 * @param location
	 * @return
	 * @see org.springframework.context.support.GenericApplicationContext#getResource(java.lang.String)
	 */
	public Resource getResource(String location) {
		return delegate.getResource(location);
	}

	/**
	 * @param locationPattern
	 * @return
	 * @throws IOException
	 * @see org.springframework.context.support.GenericApplicationContext#getResources(java.lang.String)
	 */
	public Resource[] getResources(String locationPattern) throws IOException {
		return delegate.getResources(locationPattern);
	}

	/**
	 * @return
	 * @see org.springframework.context.support.AbstractApplicationContext#getId()
	 */
	public String getId() {
		return delegate.getId();
	}

	/**
	 * @param displayName
	 * @see org.springframework.context.support.AbstractApplicationContext#setDisplayName(java.lang.String)
	 */
	public void setDisplayName(String displayName) {
		delegate.setDisplayName(displayName);
	}

	/**
	 * @return
	 * @see org.springframework.context.support.GenericApplicationContext#getBeanFactory()
	 */
	public final ConfigurableListableBeanFactory getBeanFactory() {
		return delegate.getBeanFactory();
	}

	/**
	 * @return
	 * @see org.springframework.context.support.GenericApplicationContext#getDefaultListableBeanFactory()
	 */
	public final DefaultListableBeanFactory getDefaultListableBeanFactory() {
		return delegate.getDefaultListableBeanFactory();
	}

	/**
	 * @return
	 * @see org.springframework.context.support.AbstractApplicationContext#getDisplayName()
	 */
	public String getDisplayName() {
		return delegate.getDisplayName();
	}

	/**
	 * @return
	 * @see org.springframework.context.support.AbstractApplicationContext#getParent()
	 */
	public ApplicationContext getParent() {
		return delegate.getParent();
	}

	/**
	 * @return
	 * @throws IllegalStateException
	 * @see org.springframework.context.support.AbstractApplicationContext#getAutowireCapableBeanFactory()
	 */
	public AutowireCapableBeanFactory getAutowireCapableBeanFactory()
			throws IllegalStateException {
		return delegate.getAutowireCapableBeanFactory();
	}

	/**
	 * @param beanName
	 * @param beanDefinition
	 * @throws BeanDefinitionStoreException
	 * @see org.springframework.context.support.GenericApplicationContext#registerBeanDefinition(java.lang.String, org.springframework.beans.factory.config.BeanDefinition)
	 */
	public void registerBeanDefinition(String beanName,
			BeanDefinition beanDefinition) throws BeanDefinitionStoreException {
		delegate.registerBeanDefinition(beanName, beanDefinition);
	}

	/**
	 * @return
	 * @see org.springframework.context.support.AbstractApplicationContext#getStartupDate()
	 */
	public long getStartupDate() {
		return delegate.getStartupDate();
	}

	/**
	 * @param event
	 * @see org.springframework.context.support.AbstractApplicationContext#publishEvent(org.springframework.context.ApplicationEvent)
	 */
	public void publishEvent(ApplicationEvent event) {
		delegate.publishEvent(event);
	}

	/**
	 * @param beanName
	 * @throws NoSuchBeanDefinitionException
	 * @see org.springframework.context.support.GenericApplicationContext#removeBeanDefinition(java.lang.String)
	 */
	public void removeBeanDefinition(String beanName)
			throws NoSuchBeanDefinitionException {
		delegate.removeBeanDefinition(beanName);
	}

	/**
	 * @param beanName
	 * @return
	 * @throws NoSuchBeanDefinitionException
	 * @see org.springframework.context.support.GenericApplicationContext#getBeanDefinition(java.lang.String)
	 */
	public BeanDefinition getBeanDefinition(String beanName)
			throws NoSuchBeanDefinitionException {
		return delegate.getBeanDefinition(beanName);
	}

	/**
	 * @param beanName
	 * @return
	 * @see org.springframework.context.support.GenericApplicationContext#isBeanNameInUse(java.lang.String)
	 */
	public boolean isBeanNameInUse(String beanName) {
		return delegate.isBeanNameInUse(beanName);
	}

	/**
	 * @param beanName
	 * @param alias
	 * @see org.springframework.context.support.GenericApplicationContext#registerAlias(java.lang.String, java.lang.String)
	 */
	public void registerAlias(String beanName, String alias) {
		delegate.registerAlias(beanName, alias);
	}

	/**
	 * @param alias
	 * @see org.springframework.context.support.GenericApplicationContext#removeAlias(java.lang.String)
	 */
	public void removeAlias(String alias) {
		delegate.removeAlias(alias);
	}

	/**
	 * @param beanName
	 * @return
	 * @see org.springframework.context.support.GenericApplicationContext#isAlias(java.lang.String)
	 */
	public boolean isAlias(String beanName) {
		return delegate.isAlias(beanName);
	}

	/**
	 * @param beanFactoryPostProcessor
	 * @see org.springframework.context.support.AbstractApplicationContext#addBeanFactoryPostProcessor(org.springframework.beans.factory.config.BeanFactoryPostProcessor)
	 */
	public void addBeanFactoryPostProcessor(
			BeanFactoryPostProcessor beanFactoryPostProcessor) {
		delegate.addBeanFactoryPostProcessor(beanFactoryPostProcessor);
	}

	/**
	 * @return
	 * @see org.springframework.context.support.AbstractApplicationContext#getBeanFactoryPostProcessors()
	 */
	public List<BeanFactoryPostProcessor> getBeanFactoryPostProcessors() {
		return delegate.getBeanFactoryPostProcessors();
	}

	/**
	 * @param listener
	 * @see org.springframework.context.support.AbstractApplicationContext#addApplicationListener(org.springframework.context.ApplicationListener)
	 */
	public void addApplicationListener(ApplicationListener listener) {
		delegate.addApplicationListener(listener);
	}

	/**
	 * @return
	 * @see org.springframework.context.support.AbstractApplicationContext#getApplicationListeners()
	 */
	public Collection<ApplicationListener> getApplicationListeners() {
		return delegate.getApplicationListeners();
	}

	/**
	 * @throws BeansException
	 * @throws IllegalStateException
	 * @see org.springframework.context.support.AbstractApplicationContext#refresh()
	 */
	public void refresh() throws BeansException, IllegalStateException {
		delegate.refresh();
	}

	/**
	 * 
	 * @see org.springframework.context.support.AbstractApplicationContext#registerShutdownHook()
	 */
	public void registerShutdownHook() {
		delegate.registerShutdownHook();
	}

	/**
	 * 
	 * @see org.springframework.context.support.AbstractApplicationContext#destroy()
	 */
	public void destroy() {
		delegate.destroy();
	}

	/**
	 * 
	 * @see org.springframework.context.support.AbstractApplicationContext#close()
	 */
	public void close() {
		delegate.close();
	}

	/**
	 * @return
	 * @see org.springframework.context.support.AbstractApplicationContext#isActive()
	 */
	public boolean isActive() {
		return delegate.isActive();
	}

	/**
	 * @param name
	 * @return
	 * @throws BeansException
	 * @see org.springframework.context.support.AbstractApplicationContext#getBean(java.lang.String)
	 */
	public Object getBean(String name) throws BeansException {
		return delegate.getBean(name);
	}

	/**
	 * @param name
	 * @param requiredType
	 * @return
	 * @throws BeansException
	 * @see org.springframework.context.support.AbstractApplicationContext#getBean(java.lang.String, java.lang.Class)
	 */
	public <T> T getBean(String name, Class<T> requiredType)
			throws BeansException {
		return delegate.getBean(name, requiredType);
	}

	/**
	 * @param requiredType
	 * @return
	 * @throws BeansException
	 * @see org.springframework.context.support.AbstractApplicationContext#getBean(java.lang.Class)
	 */
	public <T> T getBean(Class<T> requiredType) throws BeansException {
		return delegate.getBean(requiredType);
	}

	/**
	 * @param name
	 * @param args
	 * @return
	 * @throws BeansException
	 * @see org.springframework.context.support.AbstractApplicationContext#getBean(java.lang.String, java.lang.Object[])
	 */
	public Object getBean(String name, Object... args) throws BeansException {
		return delegate.getBean(name, args);
	}

	/**
	 * @param name
	 * @return
	 * @see org.springframework.context.support.AbstractApplicationContext#containsBean(java.lang.String)
	 */
	public boolean containsBean(String name) {
		return delegate.containsBean(name);
	}

	/**
	 * @param name
	 * @return
	 * @throws NoSuchBeanDefinitionException
	 * @see org.springframework.context.support.AbstractApplicationContext#isSingleton(java.lang.String)
	 */
	public boolean isSingleton(String name)
			throws NoSuchBeanDefinitionException {
		return delegate.isSingleton(name);
	}

	/**
	 * @param name
	 * @return
	 * @throws NoSuchBeanDefinitionException
	 * @see org.springframework.context.support.AbstractApplicationContext#isPrototype(java.lang.String)
	 */
	public boolean isPrototype(String name)
			throws NoSuchBeanDefinitionException {
		return delegate.isPrototype(name);
	}

	/**
	 * @param name
	 * @param targetType
	 * @return
	 * @throws NoSuchBeanDefinitionException
	 * @see org.springframework.context.support.AbstractApplicationContext#isTypeMatch(java.lang.String, java.lang.Class)
	 */
	public boolean isTypeMatch(String name, Class targetType)
			throws NoSuchBeanDefinitionException {
		return delegate.isTypeMatch(name, targetType);
	}

	/**
	 * @param name
	 * @return
	 * @throws NoSuchBeanDefinitionException
	 * @see org.springframework.context.support.AbstractApplicationContext#getType(java.lang.String)
	 */
	public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
		return delegate.getType(name);
	}

	/**
	 * @param name
	 * @return
	 * @see org.springframework.context.support.AbstractApplicationContext#getAliases(java.lang.String)
	 */
	public String[] getAliases(String name) {
		return delegate.getAliases(name);
	}

	/**
	 * @param beanName
	 * @return
	 * @see org.springframework.context.support.AbstractApplicationContext#containsBeanDefinition(java.lang.String)
	 */
	public boolean containsBeanDefinition(String beanName) {
		return delegate.containsBeanDefinition(beanName);
	}

	/**
	 * @return
	 * @see org.springframework.context.support.AbstractApplicationContext#getBeanDefinitionCount()
	 */
	public int getBeanDefinitionCount() {
		return delegate.getBeanDefinitionCount();
	}

	/**
	 * @return
	 * @see org.springframework.context.support.AbstractApplicationContext#getBeanDefinitionNames()
	 */
	public String[] getBeanDefinitionNames() {
		return delegate.getBeanDefinitionNames();
	}

	/**
	 * @param type
	 * @return
	 * @see org.springframework.context.support.AbstractApplicationContext#getBeanNamesForType(java.lang.Class)
	 */
	public String[] getBeanNamesForType(Class type) {
		return delegate.getBeanNamesForType(type);
	}

	/**
	 * @param type
	 * @param includeNonSingletons
	 * @param allowEagerInit
	 * @return
	 * @see org.springframework.context.support.AbstractApplicationContext#getBeanNamesForType(java.lang.Class, boolean, boolean)
	 */
	public String[] getBeanNamesForType(Class type,
			boolean includeNonSingletons, boolean allowEagerInit) {
		return delegate.getBeanNamesForType(type, includeNonSingletons,
				allowEagerInit);
	}

	/**
	 * @param type
	 * @return
	 * @throws BeansException
	 * @see org.springframework.context.support.AbstractApplicationContext#getBeansOfType(java.lang.Class)
	 */
	public <T> Map<String, T> getBeansOfType(Class<T> type)
			throws BeansException {
		return delegate.getBeansOfType(type);
	}

	/**
	 * @param type
	 * @param includeNonSingletons
	 * @param allowEagerInit
	 * @return
	 * @throws BeansException
	 * @see org.springframework.context.support.AbstractApplicationContext#getBeansOfType(java.lang.Class, boolean, boolean)
	 */
	public <T> Map<String, T> getBeansOfType(Class<T> type,
			boolean includeNonSingletons, boolean allowEagerInit)
			throws BeansException {
		return delegate.getBeansOfType(type, includeNonSingletons,
				allowEagerInit);
	}

	/**
	 * @param annotationType
	 * @return
	 * @throws BeansException
	 * @see org.springframework.context.support.AbstractApplicationContext#getBeansWithAnnotation(java.lang.Class)
	 */
	public Map<String, Object> getBeansWithAnnotation(
			Class<? extends Annotation> annotationType) throws BeansException {
		return delegate.getBeansWithAnnotation(annotationType);
	}

	/**
	 * @param beanName
	 * @param annotationType
	 * @return
	 * @see org.springframework.context.support.AbstractApplicationContext#findAnnotationOnBean(java.lang.String, java.lang.Class)
	 */
	public <A extends Annotation> A findAnnotationOnBean(String beanName,
			Class<A> annotationType) {
		return delegate.findAnnotationOnBean(beanName, annotationType);
	}

	/**
	 * @return
	 * @see org.springframework.context.support.AbstractApplicationContext#getParentBeanFactory()
	 */
	public BeanFactory getParentBeanFactory() {
		return delegate.getParentBeanFactory();
	}

	/**
	 * @param name
	 * @return
	 * @see org.springframework.context.support.AbstractApplicationContext#containsLocalBean(java.lang.String)
	 */
	public boolean containsLocalBean(String name) {
		return delegate.containsLocalBean(name);
	}

	/**
	 * @param code
	 * @param args
	 * @param defaultMessage
	 * @param locale
	 * @return
	 * @see org.springframework.context.support.AbstractApplicationContext#getMessage(java.lang.String, java.lang.Object[], java.lang.String, java.util.Locale)
	 */
	public String getMessage(String code, Object[] args, String defaultMessage,
			Locale locale) {
		return delegate.getMessage(code, args, defaultMessage, locale);
	}

	/**
	 * @param code
	 * @param args
	 * @param locale
	 * @return
	 * @throws NoSuchMessageException
	 * @see org.springframework.context.support.AbstractApplicationContext#getMessage(java.lang.String, java.lang.Object[], java.util.Locale)
	 */
	public String getMessage(String code, Object[] args, Locale locale)
			throws NoSuchMessageException {
		return delegate.getMessage(code, args, locale);
	}

	/**
	 * @param resolvable
	 * @param locale
	 * @return
	 * @throws NoSuchMessageException
	 * @see org.springframework.context.support.AbstractApplicationContext#getMessage(org.springframework.context.MessageSourceResolvable, java.util.Locale)
	 */
	public String getMessage(MessageSourceResolvable resolvable, Locale locale)
			throws NoSuchMessageException {
		return delegate.getMessage(resolvable, locale);
	}

	/**
	 * 
	 * @see org.springframework.context.support.AbstractApplicationContext#start()
	 */
	public void start() {
		delegate.start();
	}

	/**
	 * 
	 * @see org.springframework.context.support.AbstractApplicationContext#stop()
	 */
	public void stop() {
		delegate.stop();
	}

	/**
	 * @return
	 * @see org.springframework.context.support.AbstractApplicationContext#isRunning()
	 */
	public boolean isRunning() {
		return delegate.isRunning();
	}

	/**
	 * @return
	 * @see org.springframework.context.support.AbstractApplicationContext#toString()
	 */
	@Override
	public String toString() {
		return delegate.toString();
	}
}
