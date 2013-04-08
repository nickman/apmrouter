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
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.spring.ctx.ApplicationContextService;
import org.springframework.beans.factory.config.BeanDefinition;
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
	/** Indicates if the default module hot deploy lib directory class loading should be disabled. By default it is enabled */
	protected final boolean disableHotDirLibs;
	
	
	
	/**
	 * Creates a new ApplicationContextDeployer
	 * @param disableHotDirLibs Indicates if the default module hot deploy lib directory class loading should be disabled
	 */
	protected ApplicationContextDeployer(boolean disableHotDirLibs) {
		this.disableHotDirLibs = disableHotDirLibs;
	}

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
			HotDeployerClassLoader cl = findClassLoader(f);
			cl.init();
			StringBuilder b = new StringBuilder("\nHotDeployerClassLoader URLs [");
			for(URL url: cl.getURLs()) {
				b.append("\n\t").append(url);
			}
			b.append("\n]");
			log.info(b);
			final ClassLoader current = Thread.currentThread().getContextClassLoader();
			try {
				Thread.currentThread().setContextClassLoader(cl);
				GenericXmlApplicationContext appCtx = new GenericXmlApplicationContext();
				//appCtx.setClassLoader(findClassLoader(f));
				appCtx.setDisplayName(f.getAbsolutePath());	
				appCtx.setParent(parent);
				appCtx.load(new UrlResource(f.toURI().toURL()));
				for(String beanName: appCtx.getBeanDefinitionNames()) {
					BeanDefinition beanDef = appCtx.getBeanDefinition(beanName);
					if(HotDeployerClassLoader.class.getName().equals(beanDef.getBeanClassName())) {
						appCtx.removeBeanDefinition(beanName);
					}
				}
				// Add any located wars
				boolean hasWars = false;
				for(String warFileName: cl.getWars()) {
					File warFile = new File(warFileName);
					WARDeployer.deploy(appCtx, warFile);
					hasWars = true;
				}
				ObjectName on = JMXHelper.objectName(ApplicationContextService.HOT_OBJECT_NAME_PREF + ObjectName.quote(f.getAbsolutePath()));
				ApplicationContextService.register(on, appCtx);			
				appCtx.refresh();
				//jimmyTheJettyWebApps(appCtx, cl);
				if(hasWars) {
					//jiggleTheHandlers(appCtx, cl);
				}
				return appCtx;
			} finally {
				Thread.currentThread().setContextClassLoader(current);
			}
		} catch (Throwable ex) {
			log.error("Failed to deploy application context [" + fe + "]", ex);
			throw new RuntimeException("Failed to deploy application context [" + fe + "]", ex);
		}
	}
	
	/**
	 * Stops and starts the handler collection. For some reason, the webapps don't start correctly without this.
	 * @param appCtx The app context the handlers are deployed in
	 * @param cl The class loader 
	 */
	protected void jiggleTheHandlers(GenericXmlApplicationContext appCtx, ClassLoader cl) {
		try {
			Class<?> handlerClass = Class.forName("org.eclipse.jetty.server.handler.HandlerCollection", true, cl);
			Object handler = appCtx.getBean(handlerClass);
			handlerClass.getMethod("stop").invoke(handler);
			log.info("Stopped Handler");
			handlerClass.getMethod("start").invoke(handler);
			log.info("Started Handler");
		} catch (Exception ex) {
			log.error("Failed to jiggle the HandlerCollection", ex);
		}
		
	}
	
	protected void jimmyTheJettyWebApps(GenericXmlApplicationContext appCtx, ClassLoader cl) {
		final ClassLoader current = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(cl);
			//Class<?> webAppClazz = Class.forName("org.eclipse.jetty.annotations.AnnotationConfiguration", true, cl);
			if(!appCtx.containsBean("JettyAnnotations")) return;
			Object annotationConfiguration = appCtx.getBean("JettyAnnotations");
			if(annotationConfiguration!=null) {
				Method configMethod = null;  //annotationConfiguration.getClass().getDeclaredMethod("configure", webAppClazz);
				Class<?> webAppClass = null;
				for(Method m: annotationConfiguration.getClass().getDeclaredMethods()) {
					if(m.getName().equals("configure")) {
						configMethod = m;
						webAppClass = configMethod.getParameterTypes()[0];
						break;
					}
				}
				if(configMethod==null) throw new Exception("No Config Method Found");
				for(Object webApp: appCtx.getBeansOfType(webAppClass).values()) {
					configMethod.invoke(annotationConfiguration, webApp);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		} finally {
			Thread.currentThread().setContextClassLoader(current);
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
	 * @return the created GenericXmlApplicationContet
	 * @throws Exception thrown on any error
	 */
	protected HotDeployerClassLoader findClassLoader(File xmlFile) throws Exception {
		HotDeployerClassLoader cl = new HotDeployerClassLoader();
		Set<String>[] locateds = getHotDeployAutoEntries(xmlFile);
		cl.setClassPathEntries(locateds[0]);
		cl.addWars(locateds[1]);
		GenericXmlApplicationContext appCtx = new GenericXmlApplicationContext();
		appCtx.load(new UrlResource(xmlFile.toURI().toURL()));
		for(String beanName: appCtx.getBeanDefinitionNames()) {
			BeanDefinition beanDef = appCtx.getBeanDefinition(beanName);
			if(!HotDeployerClassLoader.class.getName().equals(beanDef.getBeanClassName())) {
				appCtx.removeBeanDefinition(beanName);
			}
		}
		appCtx.refresh();
		Map<String, HotDeployerClassLoader>  classLoaders = appCtx.getBeansOfType(HotDeployerClassLoader.class);
		if(classLoaders != null) {
			for(HotDeployerClassLoader hcl: classLoaders.values()) {
				if(cl==null) {
					cl = hcl;
				} else {
					cl.merge(hcl);
				}
			}
		}
		
		appCtx.close();
		return cl;
	}
	
	
	/**
	 * Auto locates libraries and wars for the deploying app context
	 * @param xmlFile The hot xml file
	 * @return An array of two sets, one with located libs, the next with wars
	 */
	@SuppressWarnings({ "cast", "unchecked" })
	protected Set<String>[] getHotDeployAutoEntries(File xmlFile) {
		Set<String> entries = new HashSet<String>();
		Set<String> wars = new HashSet<String>();
		File libDir = new File(xmlFile.getParent(), xmlFile.getName().split("\\.")[0] + ".lib");
		if(libDir.exists() && libDir.isDirectory()) {
			log.info("Auto adding libs in application directory [" + libDir + "]");
			for(File jar: libDir.listFiles()) {
				if(jar.toString().toLowerCase().endsWith(".jar")) {
					entries.add(jar.toString());
				}
				if(jar.toString().toLowerCase().endsWith(".war")) {
					wars.add(jar.toString());
				}				
			}			
		}
		return ((Set<String>[])new Set[]{entries, wars});
	}
	
}
