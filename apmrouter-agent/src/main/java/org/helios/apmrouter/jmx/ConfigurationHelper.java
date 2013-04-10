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
package org.helios.apmrouter.jmx;

import java.util.Properties;


/**
 * <p>Title: ConfigurationHelper</p>
 * <p>Description: Configuration helper utilities</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.jmx.ConfigurationHelper</code></p>
 */
public class ConfigurationHelper {
	
	/**
	 * Merges the passed properties
	 * @param properties The properties to merge
	 * @return the merged properties
	 */
	public static Properties mergeProperties(Properties...properties) {
		Properties allProps = new Properties();
		if(properties==null || properties.length==0) {
			Properties sys = System.getProperties();
			for(String key: sys.stringPropertyNames()) {
				allProps.put(key, sys.getProperty(key));
			}			
		} else {
			for(int i = properties.length-1; i>=0; i--) {
				if(properties[i] != null && properties[i].size() >0) {
					allProps.putAll(properties[i]);
				}
			}
		}
		return allProps;
	}
	
	
	/**
	 * Looks up a property, first in the environment, then the system properties. 
	 * If not found in either, returns the supplied default.
	 * @param name The name of the key to look up.
	 * @param defaultValue The default to return if the name is not found.
	 * @param properties An array of properties to search in. If empty or null, will search system properties. The first located match will be returned.
	 * @return The located value or the default if it was not found.
	 */
	public static String getEnvThenSystemProperty(String name, String defaultValue, Properties...properties) {
		
		String value = System.getenv(name.replace('.', '_'));
		if(value==null) {			
			value = mergeProperties(properties).getProperty(name);
		}
		if(value==null) {
			value=defaultValue;
		}
		return value;
	}
	
	/**
	 * Looks up a property, first in the system properties, then the environment. 
	 * If not found in either, returns the supplied default.
	 * @param name The name of the key to look up.
	 * @param defaultValue The default to return if the name is not found.
	 * @param properties An array of properties to search in. If empty or null, will search system properties. The first located match will be returned.
	 * @return The located value or the default if it was not found.
	 */
	public static String getSystemThenEnvProperty(String name, String defaultValue, Properties...properties) {
		String value = mergeProperties(properties).getProperty(name);
		if(value==null) {
			value = System.getenv(name.replace('.', '_'));
		}
		if(value==null) {
			value=defaultValue;
		}
//		if(LOG.isDebugEnabled()) {
//			TempLogger.getTempLogger("SystemEnvProperty", "%m").info(name + ":" + value);
//		}
		return value;
	}	
	
	//public static final Logger SystemEnvPropertyLogger = TempLogger.getTempLogger("SystemEnvProperty", "%m");
	
	/**
	 * Determines if a name has been defined in the environment or system properties.
	 * @param name the name of the property to check for.
	 * @param properties An array of properties to search in. If empty or null, will search system properties. The first located match will be returned.
	 * @return true if the name is defined in the environment or system properties.
	 */
	public static boolean isDefined(String name, Properties...properties) {
		if(System.getenv(name) != null) return true;
		if(mergeProperties(properties).getProperty(name) != null) return true;
		return false;		
	}
	
	/**
	 * Determines if a name has been defined as a valid int in the environment or system properties.
	 * @param name the name of the property to check for.
	 * @param properties An array of properties to search in. If empty or null, will search system properties. The first located match will be returned.
	 * @return true if the name is defined as a valid int in the environment or system properties.
	 */
	public static boolean isIntDefined(String name, Properties...properties) {
		String tmp = getEnvThenSystemProperty(name, null, properties);
		if(tmp==null) return false;
		try {
			Integer.parseInt(tmp);
			return true;
		} catch (Exception e) {
			return false;
		}				
	}
	
	/**
	 * Determines if a name has been defined as a valid boolean in the environment or system properties.
	 * @param name the name of the property to check for.
	 * @param properties An array of properties to search in. If empty or null, will search system properties. The first located match will be returned.
	 * @return true if the name is defined as a valid boolean in the environment or system properties.
	 */
	public static boolean isBooleanDefined(String name, Properties...properties) {
		String tmp = getEnvThenSystemProperty(name, null, properties);
		if(tmp==null) return false;
		try {
			tmp = tmp.toUpperCase();
			if(
					tmp.equalsIgnoreCase("TRUE") || tmp.equalsIgnoreCase("Y") || tmp.equalsIgnoreCase("YES") ||
					tmp.equalsIgnoreCase("FALSE") || tmp.equalsIgnoreCase("N") || tmp.equalsIgnoreCase("NO")
			) return true;
			return false;
		} catch (Exception e) {
			return false;
		}				
	}	
	
	/**
	 * Determines if a name has been defined as a valid long in the environment or system properties.
	 * @param name the name of the property to check for.
	 * @param properties An array of properties to search in. If empty or null, will search system properties. The first located match will be returned.
	 * @return true if the name is defined as a valid long in the environment or system properties.
	 */
	public static boolean isLongDefined(String name, Properties...properties) {
		String tmp = getEnvThenSystemProperty(name, null, properties);
		if(tmp==null) return false;
		try {
			Long.parseLong(tmp);
			return true;
		} catch (Exception e) {
			return false;
		}				
	}
	
	/**
	 * Returns the value defined as an Integer looked up from the Environment, then System properties.
	 * @param name The name of the key to lookup.
	 * @param defaultValue The default value to return if the name is not defined or the value is not a valid int.
	 * @param properties An array of properties to search in. If empty or null, will search system properties. The first located match will be returned.
	 * @return The located integer or the passed default value.
	 */
	public static Integer getIntSystemThenEnvProperty(String name, Integer defaultValue, Properties...properties) {
		String tmp = getSystemThenEnvProperty(name, null, properties);
		try {
			return Integer.parseInt(tmp);
		} catch (Exception e) {
			return defaultValue;
		}
	}
	
	/**
	 * Returns the value defined as an Float looked up from the Environment, then System properties.
	 * @param name The name of the key to lookup.
	 * @param defaultValue The default value to return if the name is not defined or the value is not a valid int.
	 * @param properties An array of properties to search in. If empty or null, will search system properties. The first located match will be returned.
	 * @return The located float or the passed default value.
	 */
	public static Float getFloatSystemThenEnvProperty(String name, Float defaultValue, Properties...properties) {
		String tmp = getSystemThenEnvProperty(name, null, properties);
		try {
			return Float.parseFloat(tmp);
		} catch (Exception e) {
			return defaultValue;
		}
	}
	
	
	/**
	 * Returns the value defined as a Long looked up from the Environment, then System properties.
	 * @param name The name of the key to lookup.
	 * @param defaultValue The default value to return if the name is not defined or the value is not a valid long.
	 * @param properties An array of properties to search in. If empty or null, will search system properties. The first located match will be returned.
	 * @return The located long or the passed default value.
	 */
	public static Long getLongSystemThenEnvProperty(String name, Long defaultValue, Properties...properties) {
		String tmp = getSystemThenEnvProperty(name, null, properties);
		try {
			return Long.parseLong(tmp);
		} catch (Exception e) {
			return defaultValue;
		}
	}	
	
	
	
	
	/**
	 * Returns the value defined as a Boolean looked up from the Environment, then System properties.
	 * @param name The name of the key to lookup.
	 * @param defaultValue The default value to return if the name is not defined or the value is not a valid boolean.
	 * @param properties An array of properties to search in. If empty or null, will search system properties. The first located match will be returned.
	 * @return The located boolean or the passed default value.
	 */
	public static Boolean getBooleanSystemThenEnvProperty(String name, Boolean defaultValue, Properties...properties) {
		String tmp = getSystemThenEnvProperty(name, null, properties);
		if(tmp==null) return defaultValue;
		tmp = tmp.toUpperCase();
		if(tmp.equalsIgnoreCase("TRUE") || tmp.equalsIgnoreCase("Y") || tmp.equalsIgnoreCase("YES")) return true;
		if(tmp.equalsIgnoreCase("FALSE") || tmp.equalsIgnoreCase("N") || tmp.equalsIgnoreCase("NO")) return false;
		return defaultValue;
	}		

}
