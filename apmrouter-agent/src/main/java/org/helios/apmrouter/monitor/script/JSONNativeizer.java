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

import org.json.JSONArray;
import org.json.JSONObject;

import sun.org.mozilla.javascript.internal.NativeArray;
import sun.org.mozilla.javascript.internal.NativeObject;
import sun.org.mozilla.javascript.internal.ScriptableObject;

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
	public static ScriptableObject toNative(JSONObject json) {
		return toNative(json, null);
	}
	
	/**
	 * Creates a new native javascript object from the passed JSON object
	 * @param json The JSON object to convert
	 * @param no An existing native Rhino javascript object which is created anew if null. Used for a recusrive parse
	 * @return a native Rhino javascript object
	 */
	public static ScriptableObject toNative(JSONObject json, ScriptableObject no) {
		try {
			if(no==null) no = new NativeObject();			
			for(String key : JSONObject.getNames(json)) {
				Object obj = json.get(key);
				if(obj == JSONObject.NULL) {
					ScriptableObject.putProperty( no, key, null);
				} else if(obj instanceof JSONArray) {
					ScriptableObject.putProperty( no, key, toNative((JSONArray)obj));
				} else if(obj instanceof JSONObject) {
					ScriptableObject.putProperty( no, key, toNative((JSONObject)obj, null));
				} else {
					ScriptableObject.putProperty( no, key, obj);
				}
			}
			return no;
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Creates a new native javascript array from the passed JSON array
	 * @param json The JSON array to convert
	 * @return a native Rhino javascript array
	 */
	public static NativeArray toNative(JSONArray json) {
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
			return new NativeArray(vars);
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
		if(!(o instanceof ScriptableObject)) throw new IllegalArgumentException("The passed object was not a ScriptableObject but a [" + o.getClass().getName() + "]", new Throwable());
		ScriptableObject no = (ScriptableObject)o;
		Set<JMXScriptRequest> requests = new HashSet<JMXScriptRequest>();
		if(no instanceof NativeArray) {
			NativeArray na = (NativeArray)no;
			for(int i = 0; i < na.size(); i++) {
				Collections.addAll(requests, fromNative((ScriptableObject)na.get(i)));
			}
		} else if(no instanceof NativeObject) {
			String mbs = null;
			if(ScriptableObject.hasProperty(no, JMXScriptRequest.KEY_MBEAN_SERVER)) {
				mbs = ScriptableObject.getProperty(no, JMXScriptRequest.KEY_MBEAN_SERVER).toString();
			}			
			String objName = ScriptableObject.getProperty(no, JMXScriptRequest.KEY_OBJECT_NAME).toString();
			Set<String> attrs = new HashSet<String>();
			Map<String, String[]> compositeNames = new HashMap<String, String[]>();
			Object attrObj = ScriptableObject.getProperty(no, JMXScriptRequest.KEY_ATTRIBUTES);
			if(attrObj instanceof NativeArray) {
				NativeArray na = (NativeArray)attrObj;				
				for(int i = 0; i < na.size(); i++) {
					Object a = na.get(i);
					if(a instanceof NativeObject) {
						NativeObject co = (NativeObject)a;
						compositeNames.put(ScriptableObject.getProperty(co, JMXScriptRequest.KEY_COMPOSITES).toString(), getPathFromNative(co));
					} else {
						attrs.add(na.get(i).toString());
					}					
				}
			} else if(attrObj instanceof NativeObject) {
				NativeObject co = (NativeObject)attrObj;
				compositeNames.put(ScriptableObject.getProperty(co, JMXScriptRequest.KEY_COMPOSITES).toString(), getPathFromNative(co));
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
		log(Arrays.toString(done));
		return done;
	}
	
	/**
	 * Extracts the path from a native object
	 * @param no the native object
	 * @return A string array representing the path
	 */
	protected static String[] getPathFromNative(NativeObject no) {
		Object path = ScriptableObject.getProperty(no, JMXScriptRequest.KEY_PATH);
		if(path instanceof NativeArray) {
			NativeArray na = (NativeArray)path;
			String[] arr = new String[na.size()];
			for(int i = 0; i < na.size(); i++) {
				arr[i] = na.get(i).toString();
			}
			return arr;
			
		} else {
			return new String[]{path.toString()};
		}
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
}
