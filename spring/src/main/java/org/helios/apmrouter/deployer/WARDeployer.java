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
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.context.support.GenericApplicationContext;

/**
 * <p>Title: WARDeployer</p>
 * <p>Description: Hot deploys war files defined in the library directory of a hot deployed app context</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.deployer.WARDeployer</code></p>
 */

public class WARDeployer {
	/** Instance logger */
	protected static final Logger log = Logger.getLogger(WARDeployer.class);

	/**
	 * Deploys the passed war file into the passed application context
	 * @param appCtx The application context assumed to contain a {@link org.DataServer.jetty.server.Server}
	 * @param warFile The war file to deploy
	 */
	public static void deploy(GenericApplicationContext appCtx, File warFile) {
		if(warFile==null) throw new IllegalArgumentException("The passed file was null", new Throwable());
		if(!warFile.canRead()) throw new IllegalArgumentException("The passed file [" + warFile + "] cannot be read", new Throwable());
		final boolean exploded = warFile.isDirectory();
		
		BeanDefinition handlers = appCtx.getBeanDefinition("Handlers");
		if(handlers==null) {
			log.error("No Jetty Handler Bean Definition Found when deploying [" + warFile + "]");
			throw new RuntimeException("No Jetty Handler Bean Definition Found", new Throwable());
		}
		
		
		GenericBeanDefinition beanDef = new GenericBeanDefinition();
		beanDef.setBeanClassName("org.eclipse.jetty.webapp.WebAppContext");
		beanDef.setDescription(warFile.getName());		
		Map<Object,Object> values = new HashMap<Object, Object>();
		final String webAppName = warFile.getName().toLowerCase().replace(".war", "");
		values.put("contextPath", "/" + webAppName);
		values.put("extractWAR", !exploded);
		values.put("war", warFile.getAbsolutePath());		
		values.put("server", new RuntimeBeanReference("HttpServer"));
		if(appCtx.containsBeanDefinition("JettyConfigs")) {
			values.put("configurations", new RuntimeBeanReference("JettyConfigs"));
		}
		values.put("logUrlOnStart", true);
		beanDef.setPropertyValues(new MutablePropertyValues(values));
		appCtx.registerBeanDefinition(warFile.getName().toLowerCase(), beanDef);
		// need to add the war beanDef to the handlers list		
		ManagedList<RuntimeBeanReference> managedList = (ManagedList<RuntimeBeanReference>)handlers.getPropertyValues().getPropertyValue("handlers").getValue();
		managedList.add(new RuntimeBeanReference(warFile.getName().toLowerCase()));		
		// add webapp reference to JMX Exporter so we can manage the webpp through JMX 		
		if(appCtx.containsBeanDefinition("JettyJMXExporter")) {
			BeanDefinition jettyJmxEx = appCtx.getBeanDefinition("JettyJMXExporter");
			Map<TypedStringValue,RuntimeBeanReference> map = (Map<TypedStringValue,RuntimeBeanReference>) jettyJmxEx.getPropertyValues().getPropertyValue("beans").getValue();
			map.put(new TypedStringValue("org.helios.apmrouter.jetty:service=WebApp,name=" + webAppName), new RuntimeBeanReference(warFile.getName().toLowerCase()));
		}
		
	}

	/*
	Get display name from xxx.war/WEB-INF/web.xml
	=============================================
	<web-app><display-name>
	
	Bean Def to create
	==============================
	 	  <bean id="JolokiaWar" class="org.eclipse.jetty.webapp.WebAppContext">
	  <description>The Jolokia JMX Web App</description>
	      <property name="contextPath" value="/jmx"/>
	      <property name="extractWAR" value="true"/>
	      <property name="displayName" value="The Jolokia JMX Web App" />
	      <property name="war" value="/home/nwhitehead/.apmrouter/hotdir/jolokia/jolokia.lib/jolokia.war" />
	      <property name="logUrlOnStart" value="true"/>  
	  </bean>	
	  
	  WebAppContext.setServer() --> to instance of org.eclipse.jetty.server.Server
	  Add to instance of --->  org.eclipse.jetty.server.handler.HandlerCollection  (addHandler(Handler handler) )
	
	*/
	
}
