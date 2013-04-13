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
package org.helios.apmrouter.satellite.services.cascade;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.remote.JMXServiceURL;

import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.util.SimpleLogger;



/**
 * <p>Title: Cascader</p>
 * <p>Description: Service to load the JDMK CascadeService and mount any recognized JVMs on the platform</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.satellite.services.cascade.Cascader</code></p>
 */
public class Cascader {
	/** Indicates if this VM was able to load the cascade service */
	public static final boolean available;
	/** The cascade service class */
	protected static final Class<?> cascadingClass;
	
	/** The cascade service object name */
	public static final ObjectName CASCADE_SERVICE_OBJECT_NAME = JMXHelper.objectName("org.helios.jmx:service=CascadingService");
	/** The cascade manager object name */
	public static final ObjectName CASCADE_MANAGER_OBJECT_NAME = JMXHelper.objectName("org.helios.jmx:service=CascadingManager");

	
	static {
		Class<?> _cascadingClass = null;
		boolean _available = false;
		try {
			_cascadingClass = Class.forName("com.sun.jdmk.remote.cascading.CascadingService");
			_available = true;
			SimpleLogger.info("CascadingService Loaded");			
		} catch (Exception ex) {
			_available = false;
			_cascadingClass = null;
			SimpleLogger.info("CascadingService Not Available");
		}
		available = _available;
		cascadingClass = _cascadingClass;
	}

	
	
	/**
	 * Initializes the cascade service
	 */
	public static void initCascade() {
		try {
			Class<?> cascadingClass = Class.forName("com.sun.jdmk.remote.cascading.CascadingService");
			//CascadingService cs = new CascadingService(JMXHelper.getHeliosMBeanServer());
			Constructor<?> ctor = cascadingClass.getDeclaredConstructor(MBeanServer.class);
			Object cs = ctor.newInstance(JMXHelper.getHeliosMBeanServer());			
			if(!JMXHelper.getHeliosMBeanServer().isRegistered(CASCADE_SERVICE_OBJECT_NAME)) {
				JMXHelper.getHeliosMBeanServer().registerMBean(cs, CASCADE_SERVICE_OBJECT_NAME);
			}
			SimpleLogger.info("Started CascadeServer.");
//			Method mountMethod = cascadingClass.getDeclaredMethod("mount", JMXServiceURL.class, Map.class, ObjectName.class, String.class);
//			final String mountPointID  = (String)mountMethod.invoke(cs, cascadeURL, null, null, "hserval/Cassandra");			
//			SimpleLogger.info("Mounted [", cascadeURL, "] at [", mountPointID, "]");
		} catch (Exception ex) {
			System.err.println("Failed to start Cascade Server. Stack trace follows:");
			ex.printStackTrace(System.err);			
		}
	}
	
	protected static void initTargets() {
		SimpleLogger.info("Starting JVM Discovery....");
		
	}
	
	/** The JMX invocation signature for mounting JVMs */
	private static final String[] MOUNT_SIG = {JMXServiceURL.class.getName(), Map.class.getName(), ObjectName.class.getName(), String.class.getName()};
	
	/**
	 * @param serviceURL
	 * @param env
	 * @param filter
	 * @param mountPoint
	 * @return
	 */
	public static String mount(JMXServiceURL serviceURL, Map<?,?> env, ObjectName filter, String mountPoint) {
		return (String)JMXHelper.invoke(CASCADE_SERVICE_OBJECT_NAME, JMXHelper.getHeliosMBeanServer(), "mount", new Object[]{serviceURL, env, filter, mountPoint}, MOUNT_SIG);
	}
	
	/**
	 * @param mountId
	 */
	public static void unmount(String mountId) {
		JMXHelper.invoke(CASCADE_SERVICE_OBJECT_NAME, JMXHelper.getHeliosMBeanServer(), "unmount", new Object[]{mountId}, new String[]{String.class.getName()}); 
	}

}
