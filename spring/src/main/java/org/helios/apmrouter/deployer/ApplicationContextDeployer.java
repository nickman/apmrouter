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
package org.helios.apmrouter.deployer;

import java.io.File;
import java.util.Set;

import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.spring.ctx.ApplicationContextService;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.UrlResource;

/**
 * <p>Title: ApplicationContextDeployer</p>
 * <p>Description: Manages the deployment/undeployment of hot deployed application contexts</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.deployer.ApplicationContextDeployer</code></p>
 */

public class ApplicationContextDeployer {
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	
	
	/**
	 * Hot deploys the application context defined in the passed file
	 * @param parent The parent context
	 * @param fe The file event referencing a new or modified file
	 * @return the deployed application context
	 */
	protected GenericApplicationContext deploy(ApplicationContext parent, FileEvent fe) {
		try {
			log.info("Deploying AppCtx [" + fe.getFileName() + "]");
			File f = new File(fe.getFileName());
			if(!f.canRead()) throw new Exception("Cannot read file [" + fe + "]", new Throwable());
			GenericXmlApplicationContext appCtx = new GenericXmlApplicationContext();
			appCtx.setClassLoader(findClassLoader(f));
			appCtx.setDisplayName(f.getAbsolutePath());	
			appCtx.setParent(parent);
			appCtx.load(new UrlResource(f.toURI().toURL()));
			ObjectName on = JMXHelper.objectName(ApplicationContextService.HOT_OBJECT_NAME_PREF + ObjectName.quote(f.getAbsolutePath()));
			ApplicationContextService.register(on, appCtx);			
			appCtx.refresh();
			return appCtx;
		} catch (Throwable ex) {
			log.error("Failed to deploy application context [" + fe + "]", ex);
			throw new RuntimeException("Failed to deploy application context [" + fe + "]", ex);
		}
	}
	
	/**
	 * Unregisters the application context mbean and closes the app context
	 * @param appCtx The app context to undeploy
	 */
	protected void undeploy(GenericApplicationContext appCtx) {
		try { 
			ObjectName on = JMXHelper.objectName(ApplicationContextService.HOT_OBJECT_NAME_PREF + ObjectName.quote(appCtx.getDisplayName()));
			if(JMXHelper.getHeliosMBeanServer().isRegistered(on)) {
				JMXHelper.getHeliosMBeanServer().unregisterMBean(on);
			}
		} catch (Exception ex) {
			log.warn("Failed to undeploy AppCtx MBean for [" + appCtx.getDisplayName() + "]", ex);
		}
		appCtx.close();		
	}
	
	/**
	 * Creates a new GenericXmlApplicationContext configured by the passed XML file.
	 * Attempts to locate a {@link HotDeployerClassLoader} definition in the bean definitions.
	 * If one is found, it is instantiated and configured, then used as the GenericXmlApplicationContext's
	 * classloader. The bean definition is removed from the context before being returned since it is no longer needed. 
	 * @param xmlFile The file to inspect
	 * @return the created GenericXmlApplicationContext
	 * @throws Exception thrown on any error
	 */
	protected ClassLoader findClassLoader(File xmlFile) throws Exception {
		GenericXmlApplicationContext appCtx = new GenericXmlApplicationContext();
		appCtx.load(new UrlResource(xmlFile.toURI().toURL()));
		for(String beanName: appCtx.getBeanDefinitionNames()) {
			BeanDefinition beanDef = appCtx.getBeanDefinition(beanName);
			if(HotDeployerClassLoader.class.getName().equals(beanDef.getBeanClassName())) {
				appCtx.removeBeanDefinition(beanName);
				HotDeployerClassLoader classLoader = new HotDeployerClassLoader();
				MutablePropertyValues props = beanDef.getPropertyValues();
				PropertyValue cpEntries = props.getPropertyValue("classPathEntries");
				PropertyValue vdate = props.getPropertyValue("validateEntries");
				if(cpEntries!=null) {
					Set<String> entries = (Set<String>)cpEntries.getConvertedValue();
					classLoader.setClassPathEntries(entries);
				}
				if(vdate!=null) {
					boolean vdateValue = "true".equals(((TypedStringValue)vdate.getValue()).getValue());
					classLoader.setValidateEntries(vdateValue);
				}
				log.info("Adding Extended ClassLoader for Hot Deployed AppCtx [" + xmlFile + "]");
				return classLoader;
			}
		}
		return null;
	}
	
}
