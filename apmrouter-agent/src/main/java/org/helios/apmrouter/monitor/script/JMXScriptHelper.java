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
package org.helios.apmrouter.monitor.script;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.helios.apmrouter.jmx.JMXHelper;

/**
 * <p>Title: JMXScriptHelper</p>
 * <p>Description: A JMXHelper for scripting JMX checks and monitors</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.monitor.script.JMXScriptHelper</code></p>
 */

public class JMXScriptHelper {
	/** Static class logger */
	protected static final Logger LOG = Logger.getLogger(JMXScriptHelper.class);
	
	/**
	 * Reads a numeric MBean attribute as a long 
	 * @param server The MBeanServer to read from
	 * @param objectName The ObjectName of the target MBean
	 * @param attribute The attribute name
	 * @return The long value of the read attribute
	 */
	public static long getNumericAttribute(MBeanServerConnection server, CharSequence objectName, String attribute) {
		if(server==null) throw new UnavailableMBeanServerException();
		try {
			return ((Number)server.getAttribute(JMXHelper.objectName(objectName), attribute)).longValue();
		} catch (Exception ex) {
			LOG.error("Failed to read numeric attribute [" + attribute + "] from ObjectName [" + objectName + "]", ex);
			throw new RuntimeException("Failed to read numeric attribute [" + attribute + "] from ObjectName [" + objectName + "]", ex);
		}
	}
	
	/**
	 * Reads a numeric MBean attribute as a long from the default MBeanServer
	 * @param objectName The ObjectName of the target MBean
	 * @param attribute The attribute name
	 * @return The long value of the read attribute
	 */
	public static long getNumericAttribute(CharSequence objectName, String attribute) {
		return getNumericAttribute(JMXHelper.getHeliosMBeanServer(), objectName, attribute);
	}
	
	/**
	 * Reads a numeric MBean attribute as a long from the named MBeanServer
	 * @param mbeanServerName The default domain of the target MBeanServer
	 * @param objectName The ObjectName of the target MBean
	 * @param attribute The attribute name
	 * @return The long value of the read attribute
	 */
	public static long getNumericAttribute(String mbeanServerName, CharSequence objectName, String attribute) {
		return getNumericAttribute(JMXHelper.getLocalMBeanServer(mbeanServerName), objectName, attribute);
	}
	
	
	/**
	 * Reads multiple numeric MBean attributes as longs 
	 * @param server The MBeanServer to read from
	 * @param objectName The ObjectName of the target MBean
	 * @param attributes An array of attribute names
	 * @return A map of long values of the read attributes keyed by the corresponding attribute name
	 */
	public static Map<String, Long> getNumericAttributes(MBeanServerConnection server, CharSequence objectName, String...attributes) {
		if(server==null) throw new UnavailableMBeanServerException();
		try {
			if(attributes==null || attributes.length<1) throw new IllegalArgumentException("The passed attribute name array was null or zero length", new Throwable());
			AttributeList attrList = server.getAttributes(JMXHelper.objectName(objectName), attributes);
			Map<String, Long> map = new HashMap<String, Long>(attrList.size());
			String name = null;
			for(Attribute attr: attrList.asList()) {
				try {
					name = attr.getName();
					long value = ((Number)attr.getValue()).longValue();
					map.put(name, value);
				} catch (Exception ex) {
					LOG.warn("Failed to convert attribute [" + name + "] to a number");
				}
			}
			return map;
		} catch (Exception ex) {
			LOG.error("Failed to read numeric attributes " + Arrays.toString(attributes) + " from ObjectName [" + objectName + "]", ex);
			throw new RuntimeException("Failed to read numeric attributes " + Arrays.toString(attributes) + " from ObjectName [" + objectName + "]", ex);
		}
	}
	
	/**
	 * Reads multiple numeric MBean attributes as longs from the default MBeanServer 
	 * @param objectName The ObjectName of the target MBean
	 * @param attributes An array of attribute names
	 * @return A map of long values of the read attributes keyed by the corresponding attribute name
	 */
	public static Map<String, Long> getNumericAttributes(CharSequence objectName, String...attributes) {
		return getNumericAttributes(JMXHelper.getHeliosMBeanServer(), objectName, attributes);
	}
	
	/**
	 * Reads multiple numeric MBean attributes as longs from the default MBeanServer 
	 * @param mbeanServerName The default domain of the target MBeanServer
	 * @param objectName The ObjectName of the target MBean
	 * @param attributes An array of attribute names
	 * @return A map of long values of the read attributes keyed by the corresponding attribute name
	 */
	public static Map<String, Long> getNumericAttributes(String mbeanServerName, CharSequence objectName, String...attributes) {
		return getNumericAttributes(JMXHelper.getLocalMBeanServer(mbeanServerName), objectName, attributes);
	}
	
	
	
	/**
	 * Reads multiple numeric MBean attributes as longs from MBeans with ObjectNames matching the specified pattern 
	 * @param server The MBeanServer to read from
	 * @param objectName The ObjectName of the target MBean
	 * @param objectNameKeys The property keys in the located matching ObjectNames that can be used to uniquely identify the MBean 
	 * @param attributes An array of attribute names
	 * @return A map of maps containing long values of the read attributes keyed by the corresponding attribute name, in turn mapped by the comma separated key propery values
	 */
	public static Map<String, Map<String, Long>> getNumericAttributes(MBeanServerConnection server, CharSequence objectName, String[] objectNameKeys, String...attributes) {
		if(server==null) throw new UnavailableMBeanServerException();
		try {
			if(attributes==null || attributes.length<1) throw new IllegalArgumentException("The passed attribute name array was null or zero length", new Throwable());
			if(objectNameKeys==null || objectNameKeys.length<1) throw new IllegalArgumentException("The passed object Name Keys array was null or zero length", new Throwable());
			Set<ObjectName> matches = server.queryNames(JMXHelper.objectName(objectName), null);
			if(matches==null || matches.isEmpty()) return Collections.emptyMap();
			Map<String, Map<String, Long>> map = new HashMap<String, Map<String, Long>>(matches.size());
			for(ObjectName on: matches) {
				try {
					StringBuilder key = new StringBuilder();
					for(String k: objectNameKeys) {
						String prop = on.getKeyProperty(k);
						if(prop!=null) {
							key.append(prop);
						}
					}
					map.put(key.toString(), getNumericAttributes(server, on.toString(), attributes));
				} catch (Exception ex) {
					LOG.warn("Failed to process ObjectName [" + on+ "]:" + ex);
				}
			}
			return map;
		} catch (Exception ex) {
			LOG.error("Failed to read numeric attributes " + Arrays.toString(attributes) + " from ObjectName [" + objectName + "]", ex);
			throw new RuntimeException("Failed to read numeric attributes " + Arrays.toString(attributes) + " from ObjectName [" + objectName + "]", ex);
		}
	}
	
	/**
	 * Reads multiple numeric MBean attributes as longs from MBeans with ObjectNames matching the specified pattern on the default MBeanServer 
	 * @param objectName The ObjectName of the target MBean
	 * @param objectNameKeys The property keys in the located matching ObjectNames that can be used to uniquely identify the MBean 
	 * @param attributes An array of attribute names
	 * @return A map of maps containing long values of the read attributes keyed by the corresponding attribute name, in turn mapped by the comma separated key propery values
	 */
	public static Map<String, Map<String, Long>> getNumericAttributes(CharSequence objectName, String[] objectNameKeys, String...attributes) {
		return getNumericAttributes(JMXHelper.getHeliosMBeanServer(), objectName, objectNameKeys, attributes);
	}
	
	/**
	 * Reads multiple numeric MBean attributes as longs from MBeans with ObjectNames matching the specified pattern on the default MBeanServer
	 * @param mbeanServerName The default domain of the target MBeanServer 
	 * @param objectName The ObjectName of the target MBean
	 * @param objectNameKeys The property keys in the located matching ObjectNames that can be used to uniquely identify the MBean 
	 * @param attributes An array of attribute names
	 * @return A map of maps containing long values of the read attributes keyed by the corresponding attribute name, in turn mapped by the comma separated key propery values
	 */
	public static Map<String, Map<String, Long>> getNumericAttributes(String mbeanServerName, CharSequence objectName, String[] objectNameKeys, String...attributes) {
		return getNumericAttributes(JMXHelper.getLocalMBeanServer(mbeanServerName), objectName, objectNameKeys, attributes);
	}
	
}
