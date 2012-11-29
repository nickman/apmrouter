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
package org.helios.apmrouter.monitor.script;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServerConnection;

import org.helios.apmrouter.jmx.JMXHelper;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * <p>Title: JMXScriptRequest</p>
 * <p>Description: Container for JavaScript submitted JMX requests</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.monitor.script.JMXScriptRequest</code></p>
 */

public class JMXScriptRequest {
	/** The name representing an MBeanServer */
	public final String mbeanServerName;
	/** The ObjectName or pattern */
	public final String objectName;
	/** The names of the attributes to retrieve */
	public final String[] attributeNames;
	/** A map of composite data attributes */
	public final Map<String, String[]> compositeNames;
	
	
	/** The json key for the object name */
	public static final String KEY_OBJECT_NAME = "on";
	/** The json key for the MBeanServer */
	public static final String KEY_MBEAN_SERVER = "mbs";
	/** The json key for the mbean attribute names */
	public static final String KEY_ATTRIBUTES = "attrs";
	/** The json key for cpomposite attribute name */
	public static final String KEY_COMPOSITES = "attr";
	/** The json key for cpomposite attribute path */
	public static final String KEY_PATH = "path";
	
	/**
	 * Creates a new JMXScriptRequest
	 * @param mbeanServerName The name representing an MBeanServer
	 * @param objectName The ObjectName or pattern
	 * @param compositeNames A map of composite data attributes
	 * @param attributeNames The names of the attributes to retrieve
	 */
	public JMXScriptRequest(String mbeanServerName, String objectName, Map<String, String[]> compositeNames, String...attributeNames) {
		this.mbeanServerName = mbeanServerName;
		this.objectName = objectName;
		this.attributeNames = attributeNames;
		this.compositeNames = Collections.unmodifiableMap(compositeNames==null ? new HashMap<String, String[]>(0) : compositeNames);
	}
	
	/**
	 * Returns the JMX MBeanServerConnection for this request
	 * @return a MBeanServerConnection
	 */
	public MBeanServerConnection getMBeanServerConnection() {
		if(mbeanServerName==null || mbeanServerName.trim().isEmpty()) return JMXHelper.getHeliosMBeanServer();
		return JMXHelper.getLocalMBeanServer(mbeanServerName, false);
	}


	
	/**
	 * Converts this request back into a native object
	 * @return a rhino native object
	 */
	public Object toNativeObject() {
		return JSONNativeizer.toNative(toJSON());
	}
	
	/**
	 * Converts this request back into a JSONObject
	 * @return a JSONObject
	 */
	public JSONObject toJSON() {
		try {
			JSONObject jo = new JSONObject();
			if(mbeanServerName!=null && !mbeanServerName.trim().isEmpty()) {
				jo.put(KEY_MBEAN_SERVER, mbeanServerName);
			}
			jo.put(KEY_OBJECT_NAME, objectName);
			
			
			if(!compositeNames.isEmpty()) {
				jo.put(KEY_ATTRIBUTES, new JSONArray(attributeNames));
			} else {
				Object[] flatAttrs = new Object[attributeNames.length + compositeNames.size()];
				System.arraycopy(attributeNames, 0, flatAttrs, 0, attributeNames.length);
				int index = attributeNames.length-1;
				for(Map.Entry<String, String[]> entry: compositeNames.entrySet()) {
					JSONObject obj = new JSONObject();
					obj.put(KEY_COMPOSITES, entry.getKey());
					obj.put(KEY_PATH, new JSONArray(entry.getValue()));
					flatAttrs[index] = obj;
					index++;
				}
				jo.put(KEY_ATTRIBUTES, new JSONArray(flatAttrs));
			}			
			return jo;
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}		
	}


	
	/**
	 * Renders the composite names as a string
	 * @return the composite names as a string
	 */
	public String renderComposites() {
		if(compositeNames.isEmpty()) return "";
		StringBuilder b = new StringBuilder();
		for(Map.Entry<String, String[]> entry: compositeNames.entrySet()) {
			b.append("[").append(entry.getKey()).append("]:").append(Arrays.toString(entry.getValue())).append(",");
		}
		b.deleteCharAt(b.length()-1);
		return b.toString();
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("JMXScriptRequest [mbeanServerName=");
		builder.append(mbeanServerName);
		builder.append(", objectName=");
		builder.append(objectName);
		builder.append(", attributeNames=");
		builder.append(Arrays.toString(attributeNames));
		builder.append(", compositeNames=");
		builder.append(renderComposites());
		builder.append("]");
		return builder.toString();
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(attributeNames);
		result = prime * result
				+ ((compositeNames == null) ? 0 : compositeNames.hashCode());
		result = prime * result
				+ ((mbeanServerName == null) ? 0 : mbeanServerName.hashCode());
		result = prime * result
				+ ((objectName == null) ? 0 : objectName.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		JMXScriptRequest other = (JMXScriptRequest) obj;
		if (!Arrays.equals(attributeNames, other.attributeNames)) {
			return false;
		}
		if (compositeNames == null) {
			if (other.compositeNames != null) {
				return false;
			}
		} else if (!compositeNames.equals(other.compositeNames)) {
			return false;
		}
		if (mbeanServerName == null) {
			if (other.mbeanServerName != null) {
				return false;
			}
		} else if (!mbeanServerName.equals(other.mbeanServerName)) {
			return false;
		}
		if (objectName == null) {
			if (other.objectName != null) {
				return false;
			}
		} else if (!objectName.equals(other.objectName)) {
			return false;
		}
		return true;
	}
	
	
	
	
	
	
}
