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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.util.SystemClock;
import org.helios.apmrouter.util.SystemClock.ElapsedTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import sun.org.mozilla.javascript.internal.ScriptableObject;

/**
 * <p>Title: JMXScriptHelper</p>
 * <p>Description: A JMXHelper for scripting JMX checks and monitors</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.monitor.script.JMXScriptHelper</code></p>
 */

public class JMXScriptHelper {
	
	public static void main(String[] args) {
		log("Testing JMXRequest Submission");
		try {
			ScriptEngine se = new ScriptEngineManager().getEngineByExtension("js");
			Object obj = se.eval("function foo(){var a = {mbs:'jboss', on:'domain:foo=bar', attrs:['aaa', 'bbb']}; return a;}; foo();");			
			log(Arrays.toString(JSONNativeizer.fromNative((ScriptableObject)obj)));
			obj = se.eval("function foo(){var a = [{mbs:'jboss', on:'domain:foo=bar', attrs:['aaa', 'bbb']}, {mbs:'jboss', on:'domain:foo=foo', attrs:['xxx', 'yyy']}]; return a;}; foo();");			
			log(Arrays.toString(JSONNativeizer.fromNative((ScriptableObject)obj)));
			obj = se.eval("function foo(){var a = [{mbs:'jboss', on:'domain:foo=bar', attrs:['aaa', 'bbb']}, {mbs:'jboss', on:'domain:foo=foo', attrs:'xxx'}]; return a;}; foo();");			
			log(Arrays.toString(JSONNativeizer.fromNative((ScriptableObject)obj)));
			// Now, actual tests
			Bindings b = se.createBindings();
			b.put("jmx", new JMXScriptHelper());
			se.eval("var composite = {attr : 'HeapMemoryUsage', path : 'used'}");
			se.eval("var compositeAll = {attr : 'HeapMemoryUsage', path : '*'}");
			se.eval("var lastGC = {attr : 'LastGcInfo', path : ['memoryUsageAfterGc','PS Perm Gen','*']}");
			se.getContext().setBindings(b, ScriptContext.GLOBAL_SCOPE);
			obj = se.eval("function foo(){var a = {on:'java.lang:type=Memory', attrs:['ObjectPendingFinalizationCount']}; jmx.getAttributes(a);}; foo();");
			obj = se.eval("function foo(){var a = {on:'java.lang:type=Memory', attrs:composite}; jmx.getAttributes(a);}; foo();");
			
			StringBuilder q = new StringBuilder();
			for(int i = 0; i < 750000; i++) {
				q.append("0");
			}
			System.gc();
			
			
			obj = se.eval("function foo(){var a = {on:'java.lang:type=GarbageCollector,*', attrs:lastGC}; jmx.getAttributes(a);}; foo();");
			

		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
	
	public static void perfTest(int loopCount, Map<?, ?> map) {
		log("Jsonizer Test");
		try {
			log("Nativizer Test");
			for(int i = 0; i < loopCount; i++) {
				Object ret = toNative(new JSONObject(map));
				if(i==0) {
					log("Ret:" + ret.getClass().getName());
				}
			}
			log("Warmup Complete");
			SystemClock.startTimer();
			for(int i = 0; i < loopCount; i++) {
				Object ret = toNative(new JSONObject(map));
			}
			ElapsedTime et = SystemClock.endTimer();
			log("Nativizer Elapsed:" + et + "   Avg Per:" + et.avgNs(loopCount));
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
	
	/**
	 * Converts the passed JSON object to a native javascript object
	 * @param json the JSON to convert
	 * @return a native javascript object
	 */
	public static Object toNative(JSONObject json) {
		return JSONNativeizer.toNative(json);
	}
	
	/**
	 * Converts the passed JSON string value to a native javascript object
	 * @param json the JSON to convert
	 * @return a native javascript object
	 */
	public static Object toNative(CharSequence json) {
		try {
			return JSONNativeizer.toNative(new JSONObject(json.toString()));
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Converts the passed map to a native javascript object
	 * @param map The map to convert
	 * @return a native javascript object
	 */
	public static Object toNative(Map<?, ?> map) {
		return JSONNativeizer.toNative(new JSONObject(map));
	}
	
	

	
	
	
	
	/**
	 * Runtime exception only map to json converter
	 * @param map The map to convert
	 * @return the JSON object
	 */
	public static JSONObject toJSONObject(Map<?, ?> map) {
		try {
			return new JSONObject(map);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	
	/**
	 * Formats the passed objects into a loggable string. 
	 * If the last object is a {@link Throwable}, it will be formatted into a stack trace.
	 * @param msgs The objects to log
	 * @return the loggable string
	 */
	public static String format(Object...msgs) {
		if(msgs==null||msgs.length<1) return "";
		StringBuilder b = new StringBuilder();
		int c = msgs.length-1;
		for(int i = 0; i <= c; i++) {
			if(i==c && msgs[i] instanceof Throwable) {
				b.append(formatStackTrace((Throwable)msgs[i]));
			} else {
				b.append(msgs[i]);
			}
		}
		return b.toString();
	}
	
	/**
	 * Formats a throwable's stack trace
	 * @param t The throwable to format
	 * @return the formatted stack trace
	 */
	public static String formatStackTrace(Throwable t) {
		if(t==null) return "";
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		t.printStackTrace(new PrintStream(baos, true));
		try {
			baos.flush();
		} catch (IOException e) { /* No Op */
		}
		return baos.toString();
	}

	/**
	 * Low maintenance logger
	 * @param msgs The objects to format and log 
	 */
	private static void LOG(Object...msgs) {
		System.out.println(format(msgs));
	}
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
			LOG("Failed to read numeric attribute [" + attribute + "] from ObjectName [" + objectName + "]", ex);
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
	 * Process an array of JMX requests in the form of rhino native objects
	 * @param requests the rhino native object
	 * @return The native rhino result object
	 * @throws JSONException thrown on any json exception
	 */
	public static Object getAttributes(Object requests) throws JSONException {
		if(requests==null || !(requests instanceof ScriptableObject)) {
			throw new IllegalArgumentException("The passed object was null or not a ScriptableObject", new Throwable());
		}
		final JSONObject result = new JSONObject();
		final JSONArray entries = new JSONArray();
		final JSONArray errors = new JSONArray();
			result.put("results", entries);
		
		JMXScriptRequest[] jmxRequests = JSONNativeizer.fromNative((ScriptableObject)requests);
		for(JMXScriptRequest req : jmxRequests) {
			if(req.attributeNames.length<1 && req.compositeNames.isEmpty()) continue;
			try {
				MBeanServerConnection conn = req.getMBeanServerConnection();
				ObjectName on = JMXHelper.objectName(req.objectName);
				Set<ObjectName> objectNames = new HashSet<ObjectName>();
				if(!on.isPattern()) {
					objectNames.add(on);
				} else {
					objectNames.addAll(conn.queryNames(on, null));  // TODO: Support exprs ?
				}
				for(ObjectName objectName: objectNames) {
					JSONObject data = new JSONObject();
					Map<String, Object> map = new HashMap<String, Object>();
					map.put("d", objectName.getDomain());
					map.put("p", new JSONObject(objectName.getKeyPropertyList()));
					 
					AttributeList attrs = conn.getAttributes(objectName, req.attributeNames);
					for(Attribute attr: attrs.asList()) {
						data.put(attr.getName(), attr.getValue());
					}
					attrs = conn.getAttributes(objectName, req.compositeNames.keySet().toArray(new String[0]));
					for(Attribute attr: attrs.asList()) {
						if(attr.getValue()==null) continue;
						String[] path = req.compositeNames.get(attr.getName());						
						if(attr.getValue() instanceof CompositeData || attr.getValue() instanceof TabularData) {							
							Object value = attr.getValue();
							for(int i = 0; i < path.length; i++) {
								String cKey = path[i].trim();
								if(i==path.length-1 && "*".equals(cKey)) {
									value = getNext(value);
								} else {
									value = getNext(value, cKey);
								}
							}
							insertCompositeResult(data, attr.getName(), path, value); 							
						} else {
							data.put(attr.getName(), attr.getValue());
						}
					}					
					map.put("data", data);
					entries.put(new JSONObject(map));
				}
			} catch (Exception ex) {
				Map<String, Object> errMap = new HashMap<String, Object>(2);
				errMap.put("req", req.toJSON());
				errMap.put("ex", ex.toString());
				JSONObject err = new JSONObject(errMap);
				errors.put(err);
				ex.printStackTrace(System.err);
			}
		}
		if(errors.length()>0) {
			result.put("errs", errors);
		}
		log(result.toString(2));
		return JSONNativeizer.toNative(result);
	}
	
	/**
	 * Inserts a composite result
	 * @param data The json object to insert into
	 * @param topName The top attribute name
	 * @param keys The composite path
	 * @param value The value to insert
	 */
	protected static void insertCompositeResult(final JSONObject data, final String topName, final String[] keys, final Object value) {
		try {
			List<String> names = new ArrayList<String>(keys.length + 1);
			names.add(topName);
			for(int i = 0; i < keys.length-1; i++) {
				names.add(keys[i]);
			}
			String lastName = keys[keys.length-1];
			JSONObject current = data;
			for(String s: names) {
				if(!current.has(s)) {
					current.put(s, new JSONObject());
				}
				current = current.getJSONObject(s);
			}
			current.put(lastName, value);
			
		} catch (Exception ex) {
			throw new RuntimeException("Failed to insety composite result", ex);
		}
	}
	
	/**
	 * Extracts the keyed value from an instance of {@link CompositeData} or {@link TabularData} 
	 * @param obj an instance of {@link CompositeData} or {@link TabularData}
	 * @param key The key
	 * @return The extracted value or null if the passed object was not an instance of {@link CompositeData} or {@link TabularData}
	 */
	@SuppressWarnings("rawtypes")
	protected static Object getNext(Object obj, String key) {
		if(obj==null) throw new IllegalArgumentException("The passed object was null", new Throwable());
		if(key==null || key.trim().isEmpty()) throw new IllegalArgumentException("The passed key was empty or null", new Throwable());
		if(obj instanceof CompositeData) {
			return ((CompositeData)obj).get(key);
		} else if(obj instanceof TabularData) {
			return ((TabularData)obj).get(new Object[]{key});  // TODO:  This will not work most of the time.			
		} else if(obj instanceof Map) {
			return ((Map)obj).get(obj);			
		} else {
			return null;
		}
	}
	
	protected static Map<String, Object> getNext(Object obj) {
		if(obj==null) return Collections.emptyMap();
		Map<String, Object> map = new HashMap<String, Object>();
		if(obj instanceof CompositeData) {
			CompositeData cd = (CompositeData)obj;
			for(String key: cd.getCompositeType().keySet()) {
				if(cd.get(key) instanceof CompositeData) {
					return getNext(cd.get(key));
				}
				map.put(key, cd.get(key));
			}
			return map;
		} else if(obj instanceof TabularData) {
			TabularData td = (TabularData)obj;
			Collection<CompositeData> data = (Collection<CompositeData>) td.values();
			int index = 0;
			for(CompositeData cd: data) {
				map.put("" + index, getNext(cd));
				index++;
			}
			return map;
		} else {
			return null;
		}
	}
	
	
	
	
//	/**
//	 * Retrieves the value of the named attribute from all matching MBeans by the supplied ObjectName pattern and computes an aggregate.
//	 * @param aggregateFunction The name of the aggregate function which should be one of the enumerated in {@link AggregateFunction}
//	 * @param server The MBeanServer to fetch from
//	 * @param objectName The JMX ObjectName pattern that defines the group of MBeans that values should be retrieved from
//	 * @param attributeName The name of the attribute to retrieve values from
//	 * @param objectNameKeys The property keys in the located matching ObjectNames that can be used to uniquely identify the MBean
//	 * @return The specified aggregate of all the retrieved values
//	 */
//	public static Map<String, Long> getSummaryAggregate(String aggregateFunction, MBeanServerConnection server, CharSequence objectName, String attributeName, String...objectNameKeys) {
//		AggregateFunction af = AggregateFunction.forName(aggregateFunction);
//		try {
//			Set<ObjectName> matches = server.queryNames(JMXHelper.objectName(objectName), null);
//			if(matches.isEmpty()) return 0;
//			List<Object> values = new ArrayList<Object>(matches.size());
//			for(ObjectName on: matches) {
//				try {
//					values.add(server.getAttribute(on, attributeName));
//				} catch (Exception ex) {/* No Op */}
//			}
//			if(values.isEmpty()) return 0;
//			Object result =  af.aggregate(values);
//			if(result instanceof Number) {
//				return ((Number)result).longValue(); 
//			}
//			throw new Exception("The aggregate function [" + af.name() + "] did not return a number but a [" + result.getClass().getName() + "]", new Throwable());
//		} catch (Exception ex) {
//			LOG.error("Failed to read numeric aggregate for " + attributeName + " from pattern [" + objectName + "]", ex);
//			throw new RuntimeException("Failed to read numeric aggregate for " + attributeName + " from pattern [" + objectName + "]", ex);
//		}
//	}
	
	
	/**
	 * Builds a key by concatenating the the passed ObjectName's key property values whose keys are in the passed keys into one string, comma separated.
	 * @param on The object name to build a key from
	 * @param objectNameKeys The keys of the object name's for which values should be included in the key
	 * @return the created key
	 */
	public static String formatKey(ObjectName on, String...objectNameKeys) {
		if(on==null) throw new IllegalArgumentException("The passed ObjectName was null", new Throwable());
		if(objectNameKeys==null || objectNameKeys.length<1) throw new IllegalArgumentException("The passed ObjectNameKeys was null or empty", new Throwable());
		StringBuilder b = new StringBuilder();
		Hashtable<String, String> props = on.getKeyPropertyList();
		for(String key: objectNameKeys) {
			key = key.trim();
			String val = props.get(key);
			if(val!=null) {
				b.append(val).append(",");
			}			
		}
		if(b.charAt(b.length()-1)==',') b.deleteCharAt(b.length()-1);
		return b.toString();
		
	}
	
	/**
	 * Determines if the passed ObjectName is a registered MBean in the passed server
	 * @param server The MBeanServer
	 * @param objectName The ObjectName to test
	 * @return true if the ObjectName represents at least one registered MBean
	 */
	public static boolean isRegistered(MBeanServerConnection server, CharSequence objectName) {
		try {
			ObjectName on = JMXHelper.objectName(objectName);
			if(on.isPattern()) {
				return !server.queryMBeans(on, null).isEmpty();
			}
			return server.isRegistered(JMXHelper.objectName(objectName));
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Determines if the passed ObjectName is a registered MBean in the named server
	 * @param mbeanServerName The default domain of the target MBeanServer 
	 * @param objectName The ObjectName to test
	 * @return true if the ObjectName represents at least one registered MBean
	 */
	public static boolean isRegistered(String mbeanServerName, CharSequence objectName) {
		return isRegistered(JMXHelper.getLocalMBeanServer(mbeanServerName), objectName);
	}
	
	/**
	 * Determines if the passed ObjectName is a registered MBean in the default server
	 * @param objectName The ObjectName to test
	 * @return true if the ObjectName represents at least one registered MBean
	 */
	public static boolean isRegistered(CharSequence objectName) {
		return isRegistered(JMXHelper.getHeliosMBeanServer(), objectName);
	}
	
}
