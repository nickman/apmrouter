/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2013, Helios Development Group and individual contributors
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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.util.ReflectionUtils;

/**
 * <p>Title: NamedGenericXmlApplicationContext</p>
 * <p>Description: An extension of {@link GenericXmlApplicationContext} that supports a settable and gettable application name</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.spring.ctx.NamedGenericXmlApplicationContext</code></p>
 */

public class NamedGenericXmlApplicationContext extends GenericXmlApplicationContext {
	/** The app name for this context */
	protected String applicationName = null;
	/** App logger */
	protected final Logger log = Logger.getLogger(getClass());
	/**
	 * Creates a new NamedGenericXmlApplicationContext
	 */
	public NamedGenericXmlApplicationContext() {

	}

	/**
	 * Creates a new NamedGenericXmlApplicationContext
	 * @param resources
	 */
	public NamedGenericXmlApplicationContext(Resource... resources) {
		super(resources);
	}

	/**
	 * Creates a new NamedGenericXmlApplicationContext
	 * @param resourceLocations
	 */
	public NamedGenericXmlApplicationContext(String... resourceLocations) {
		super(resourceLocations);
	}

	/**
	 * Creates a new NamedGenericXmlApplicationContext
	 * @param relativeClass
	 * @param resourceNames
	 */
	public NamedGenericXmlApplicationContext(Class<?> relativeClass,
			String... resourceNames) {
		super(relativeClass, resourceNames);
	}

	
	/**
	 * Sets the application context app name for this context
	 * @param name the application context app name for this context
	 */
	public void setApplicationName(String name) {
		this.applicationName = name;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.springframework.context.support.AbstractApplicationContext#getApplicationName()
	 */
	@Override
	public String getApplicationName() {
		return applicationName;
	}
	
//	protected Map<Object, String> getTriggerInitMethods(Set<Object> triggerBeans) {
//		if(triggerBeans.isEmpty()) return Collections.emptyMap();
//		Map<Object, String> triggerMethods = new HashMap<Object, String>(triggerBeans.size());
//		for(String beanName: getBeanDefinitionNames()) {
//			if(triggerBeans.contains(getBean(beanName))) {
//				BeanDefinition beanDef = getBeanDefinition(beanName);
//				beanDef.
//			}
//		}
//	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.springframework.context.support.AbstractApplicationContext#finishRefresh()
	 */
	@Override
	protected void finishRefresh() {
		log.info("\n\t#########################\n\tFiring OnRefresh Triggers\n\t#########################\n");
		int invocations = 0;
		AppContextRefreshTriggers triggers = new AppContextRefreshTriggers();
		getAutowireCapableBeanFactory().autowireBean(triggers);
		log.info("Located [" + triggers.getRefreshTriggers().size() + "] qualified triggers");
		Set<Object> triggerBeans = triggers.getRefreshTriggers();
		Map<String, Object> beans = getBeansWithAnnotation(OnRefresh.class);
		for(Object trigger: triggerBeans) {
			beans.put(trigger.getClass().getSimpleName() + "@" + System.identityHashCode(trigger), trigger);
		}
		try {			
			for(Map.Entry<String, Object> entry: beans.entrySet()) {
				log.info("Firing OnRefresh Bean [" + entry.getKey() + "]");
				if(entry.getValue().getClass().getAnnotation(OnRefresh.class)==null) {
					try {
						ReflectionUtils.invokeMethod(ReflectionUtils.findMethod(entry.getValue().getClass(), "start"), entry.getValue());						
					} catch (Exception ex) {
						log.warn("failed to invoke start on trigger bean [" + entry.getValue()+ "]", ex);
					}
					continue;
				}
				for(Method method: ReflectionUtils.getUniqueDeclaredMethods(entry.getValue().getClass())) {
					if(method.getAnnotation(OnRefresh.class)!=null) {
						if(method.getParameterTypes().length>0) continue;
						if(!method.isAccessible()) {
							method.setAccessible(true);
						}
						log.info("Invoking OnRefresh Method [" + method.getDeclaringClass().getSimpleName() + "." + method.getName() + "]");
						if(Modifier.isStatic(method.getModifiers())) {
							method.invoke(null);
						} else {
							method.invoke(entry.getValue());
						}
						invocations++;
					}
				}
			}
		} catch (Exception ex) {
			log.error("Failed to fire onRefresh Triggers", ex);
			throw new RuntimeException("Failed to fire onRefresh Triggers", ex);
		}
		log.info("\n\t#########################\n\tFired [" + invocations + "]  OnRefresh Triggers\n\t#########################\n");
		super.finishRefresh();
	}
}
