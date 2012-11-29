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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.helios.apmrouter.monitor.script.rhino.INativeArray;
import org.helios.apmrouter.monitor.script.rhino.INativeObject;
import org.helios.apmrouter.monitor.script.rhino.IScriptableObject;
import org.helios.apmrouter.monitor.script.rhino.NativeFactory;

import org.json.JSONArray;
import org.json.JSONObject;

//import sun.org.mozilla.javascript.internal.NativeArray;
//import sun.org.mozilla.javascript.internal.NativeObject;
//import sun.org.mozilla.javascript.internal.ScriptableObject;


//import sun.org.mozilla.javascript.internal.NativeArray;
//import sun.org.mozilla.javascript.internal.NativeObject;
//import sun.org.mozilla.javascript.internal.ScriptableObject;

/**
 * <p>Title: JSONNativeizer</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.monitor.script.JSONNativeizer</code></p>
 */

public class JSONNativeizer {
	
	/**
	 * Creates a new native javascript object from the passed JSON object
	 * @param json The JSON object to convert
	 * @return a native Rhino javascript object
	 */
	public static Object toNative(JSONObject json) {
		return toNative(json, null);
	}
	
	/**
	 * Creates a new native javascript object from the passed JSON object
	 * @param json The JSON object to convert
	 * @param jsObject An existing native Rhino javascript object which is created anew if null. Used for a recusrive parse
	 * @return a native Rhino javascript object
	 */
	public static Object toNative(JSONObject json, Object jsObject) {
		if(json==null) return null;
		try {
			INativeObject no = null;
			if(jsObject==null) {
				no = NativeFactory.newNativeObject();
			} else {
				no = NativeFactory.newNativeObject(jsObject);
			}

			for(String key : JSONObject.getNames(json)) {
				
				if(key==null) continue;
				Object obj = json.get(key);
				if(obj == JSONObject.NULL) {
					no.putProperty(key, null);
				} else if(obj instanceof JSONArray) {
					no.putProperty(key, toNative((JSONArray)obj));
				} else if(obj instanceof JSONObject) {
					no.putProperty( key, toNative((JSONObject)obj, null));
				} else {
					no.putProperty(key, obj);
				}
				
			}
			return no.getUnderlying();
		} catch (Exception ex) {
			//ex.printStackTrace(System.err);
			throw new RuntimeException("Failed to convert json to native" , ex);
		}
	}
	
	/**
	 * Creates a new native javascript array from the passed JSON array
	 * @param json The JSON array to convert
	 * @return a native Rhino javascript array
	 */
	public static Object toNative(JSONArray json) {
		try {
			Object[] vars = new Object[json.length()];
			for(int i = 0; i < json.length(); i++) {
				Object obj = json.get(i);
				if(obj == JSONObject.NULL) {
					continue;
				} else if(obj instanceof JSONArray) {
					vars[i] = toNative((JSONArray)obj);
				} else if(obj instanceof JSONObject) {
					vars[i] = toNative((JSONObject)obj, null);
				} else {
					vars[i] = obj;
				}
			}
			return NativeFactory.newNativeArray(vars).getUnderlying();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Converts the passed ScriptableObject to a {@link JMXScriptRequest} array
	 * @param no The rhino native object 
	 * @return an array of {@link JMXScriptRequest}s
	 */
	public static JMXScriptRequest[] fromNative(Object o) {
		if(o==null) throw new IllegalArgumentException("The passed object was null", new Throwable());
		IScriptableObject no = NativeFactory.convertToNative(o);		
		Set<JMXScriptRequest> requests = new HashSet<JMXScriptRequest>();
		if(no instanceof INativeArray) {
			INativeArray na = (INativeArray)no;
			for(int i = 0; i < na.size(); i++) {
				Collections.addAll(requests, fromNative(na.get(i)));
			}
		} else if(no instanceof INativeObject) {
			String mbs = null;
			if(no.hasProperty(JMXScriptRequest.KEY_MBEAN_SERVER)) {
				mbs = no.getProperty(JMXScriptRequest.KEY_MBEAN_SERVER).toString();
			}			
			String objName = no.getProperty(JMXScriptRequest.KEY_OBJECT_NAME).toString();
			Set<String> attrs = new HashSet<String>();
			Map<String, String[]> compositeNames = new HashMap<String, String[]>();
			Object attrObj = no.getProperty(JMXScriptRequest.KEY_ATTRIBUTES);
			if(attrObj instanceof INativeArray) {
				INativeArray na = (INativeArray)attrObj;				
				for(int i = 0; i < na.size(); i++) {
					Object a = na.get(i);
					if(a instanceof INativeObject) {
						INativeObject co = (INativeObject)a;
						compositeNames.put(co.getProperty(JMXScriptRequest.KEY_COMPOSITES).toString(), getPathFromNative(co));
					} else {
						attrs.add(na.get(i).toString());
					}					
				}
			} else if(attrObj instanceof INativeObject) {
				INativeObject co = (INativeObject)attrObj;
				compositeNames.put(co.getProperty(JMXScriptRequest.KEY_COMPOSITES).toString(), getPathFromNative(co));
			} else if(attrObj instanceof CharSequence) {
				attrs.add(attrObj.toString());
			} else {
				throw new RuntimeException("Unexpected type in JSON request Attribute array [" + attrObj.getClass().getName() + "]", new Throwable());
			}
			requests.add(new JMXScriptRequest(mbs, objName, compositeNames, attrs.toArray(new String[attrs.size()])));
		} else {
			throw new RuntimeException("Unexpected type in JSON request [" + no.getClass().getName() + "]", new Throwable());
		}
		
		JMXScriptRequest[] done = requests.toArray(new JMXScriptRequest[requests.size()]);		
		return done;
	}
	
	/**
	 * Extracts the path from a native object
	 * @param no the native object
	 * @return A string array representing the path
	 */
	protected static String[] getPathFromNative(INativeObject no) {
		Object path = no.getProperty(JMXScriptRequest.KEY_PATH);
		if(path instanceof INativeArray) {
			INativeArray na = (INativeArray)path;
			String[] arr = new String[na.size()];
			for(int i = 0; i < na.size(); i++) {
				arr[i] = na.get(i).toString();
			}
			return arr;
		}
		return new String[]{path.toString()};
	}
	
//	public static void log(Object msg) {
//		System.out.println(msg);
//	}
	
}
