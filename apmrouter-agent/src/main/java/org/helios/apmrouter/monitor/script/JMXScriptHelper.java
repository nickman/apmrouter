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
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.monitor.aggregate.AggregateFunction;
import org.helios.apmrouter.util.SystemClock;
import org.helios.apmrouter.util.SystemClock.ElapsedTime;
import org.json.JSONObject;

import sun.org.mozilla.javascript.internal.NativeArray;
import sun.org.mozilla.javascript.internal.NativeObject;

/**
 * <p>Title: JMXScriptHelper</p>
 * <p>Description: A JMXHelper for scripting JMX checks and monitors</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.monitor.script.JMXScriptHelper</code></p>
 */

public class JMXScriptHelper {
	/** The json evaluator code template */
	protected static final String code = "var %s = %s;";
	/** The json evaluator compiled function */
	protected static final CompiledScript cs;
	
	/** The scripting engine */
	protected static final ScriptEngine se;
	
	static {
		try {
			ScriptEngineManager sem = new ScriptEngineManager();			
			se = sem.getEngineByExtension("js");
			se.eval("function compile(json) { var c = eval([json]); return c;}");
			cs = ((Compilable)se).compile("function jsonize(){return eval([json]);}; jsonize();");
			//cs = ((Compilable)se).compile("eval([json]);");
			log("Compiled Script:" + cs.getClass().getName());
//			cs = ((Compilable)se).compile(" var json = eval(jsonx);");
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	
	public static void main(String[] args) {
		log("Jsonizer Test");
		for(int i = 0; i < 15000; i++) {
			Object ret = jsonize(System.getenv());
		}
		log("Warmup Complete");
		SystemClock.startTimer();
		for(int i = 0; i < 15000; i++) {
			Object ret = jsonize(System.getenv());
		}
		ElapsedTime et = SystemClock.endTimer();
		log("JS Elapsed:" + et + "   Avg Per:" + et.avgNs(15000));
//		log("Nativizer Test");
//		SystemClock.startTimer();
//		for(int i = 0; i < 15000; i++) {
//			nativeize(System.getenv());
//		}
//		et = SystemClock.endTimer();
//		log("Native Elapsed:" + et + "   Avg Per:" + et.avgNs(15000));
		
	}
	
	/**
	 * Returns a native javascript json object compiled from the passed json string
	 * @param json The json string to compile
	 * @return the native json object
	 */
	public static Object jsonize(String json) {
		//SystemClock.startTimer();
		final String scoped = "t" + Thread.currentThread().getId();
		try {
			Object obj = null;
//			se.getContext().setAttribute("json", json, ScriptContext.ENGINE_SCOPE);			
//			obj = cs.eval();
			//obj =  se.get("jsonx");
			
			
			obj = ((Invocable)se).invokeFunction("compile", json);
			se.eval(String.format(code, scoped, json));
			obj = se.getContext().removeAttribute(scoped, ScriptContext.ENGINE_SCOPE);  
			if(obj==null) throw new RuntimeException("Null result evaluating [" + json + "]", new Throwable());
			return obj;
		} catch (Exception e) {
			throw new RuntimeException("Failed to evaluate [" + json + "]", e);			
		} finally {
			//log("JSON compiled in " + SystemClock.endTimer());
		}
	}
	
	/**
	 * Returns a native javascript json object compiled from the passed json object
	 * @param json The json object to compile
	 * @return the native json object
	 */
	public static Object jsonize(JSONObject json) {
		return jsonize(json.toString());
	}
	
	/**
	 * Returns a native javascript json object compiled from the passed map
	 * @param map The values to add to the native object
	 * @return the native json object
	 */
	public static Object nativeize(Map<?, ?> map) {
		NativeObject no = new NativeObject();
		
		no.putAll(map);
		return no;
	}
	
	
	/**
	 * Returns a native javascript json object compiled from the passed map
	 * @param map The map to compile
	 * @return the native json object
	 */
	public static Object jsonize(Map<?, ?> map) {
		return jsonize(toJSONObject(map).toString());
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
	 * Reads multiple numeric MBean attributes as longs 
	 * @param server The MBeanServer to read from
	 * @param objectName The ObjectName of the target MBean
	 * @param attributes An array of attribute names
	 * @return A native javascript map of long values of the read attributes keyed by the corresponding attribute name
	 */
	public static Object getNumericAttributes(MBeanServerConnection server, CharSequence objectName, String...attributes) {
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
					LOG("Failed to convert attribute [" + name + "] to a number");
				}
			}
			return jsonize(map);
		} catch (Exception ex) {
			LOG("Failed to read numeric attributes " + Arrays.toString(attributes) + " from ObjectName [" + objectName + "]", ex);
			throw new RuntimeException("Failed to read numeric attributes " + Arrays.toString(attributes) + " from ObjectName [" + objectName + "]", ex);
		}
	}
	
	/**
	 * Reads multiple numeric MBean attributes as longs from the default MBeanServer 
	 * @param objectName The ObjectName of the target MBean
	 * @param attributes An array of attribute names
	 * @return A native javascript map of long values of the read attributes keyed by the corresponding attribute name
	 */
	public static Object getNumericAttributes(CharSequence objectName, String...attributes) {
		return getNumericAttributes(JMXHelper.getHeliosMBeanServer(), objectName, attributes);
	}
	
	/**
	 * Reads multiple numeric MBean attributes as longs from the default MBeanServer 
	 * @param mbeanServerName The default domain of the target MBeanServer
	 * @param objectName The ObjectName of the target MBean
	 * @param attributes An array of attribute names
	 * @return A map of long values of the read attributes keyed by the corresponding attribute name
	 */
	public static Object getNumericAttributes(String mbeanServerName, CharSequence objectName, String...attributes) {
		return getNumericAttributes(JMXHelper.getLocalMBeanServer(mbeanServerName), objectName, attributes);
	}
	
	
	
	/**
	 * Reads multiple numeric MBean attributes as longs from MBeans with ObjectNames matching the specified pattern 
	 * @param server The MBeanServer to read from
	 * @param objectName The ObjectName of the target MBean
	 * @param objectNameKeys The property keys in the located matching ObjectNames that can be used to uniquely identify the MBean 
	 * @param attributes An array of attribute names
	 * @return A native javascript map of maps containing long values of the read attributes keyed by the corresponding attribute name, in turn mapped by the comma separated key propery values
	 */
	public static Object getNumericAttributes(MBeanServerConnection server, CharSequence objectName, String[] objectNameKeys, String...attributes) {
		if(server==null) throw new UnavailableMBeanServerException();
		try {
			if(attributes==null || attributes.length<1) throw new IllegalArgumentException("The passed attribute name array was null or zero length", new Throwable());
			if(objectNameKeys==null || objectNameKeys.length<1) throw new IllegalArgumentException("The passed object Name Keys array was null or zero length", new Throwable());
			Set<ObjectName> matches = server.queryNames(JMXHelper.objectName(objectName), null);
			if(matches==null || matches.isEmpty()) return Collections.emptyMap();
			Map<String, Object> map = new HashMap<String, Object>(matches.size());
			for(ObjectName on: matches) {
				try {
					map.put(formatKey(on, objectNameKeys), getNumericAttributes(server, on.toString(), attributes));
				} catch (Exception ex) {
					LOG("Failed to process ObjectName [" + on+ "]:" + ex);
				}
			}
			return jsonize(map);
		} catch (Exception ex) {
			LOG("Failed to read numeric attributes " + Arrays.toString(attributes) + " from ObjectName [" + objectName + "]", ex);
			throw new RuntimeException("Failed to read numeric attributes " + Arrays.toString(attributes) + " from ObjectName [" + objectName + "]", ex);
		}
	}
	
	/**
	 * Reads multiple numeric MBean attributes as longs from MBeans with ObjectNames matching the specified pattern on the default MBeanServer 
	 * @param objectName The ObjectName of the target MBean
	 * @param objectNameKeys The property keys in the located matching ObjectNames that can be used to uniquely identify the MBean 
	 * @param attributes An array of attribute names
	 * @return A native javascript map of maps containing long values of the read attributes keyed by the corresponding attribute name, in turn mapped by the comma separated key propery values
	 */
	public static Object getNumericAttributes(CharSequence objectName, String[] objectNameKeys, String...attributes) {
		return getNumericAttributes(JMXHelper.getHeliosMBeanServer(), objectName, objectNameKeys, attributes);
	}
	
	/**
	 * Reads multiple numeric MBean attributes as longs from MBeans with ObjectNames matching the specified pattern on the default MBeanServer
	 * @param mbeanServerName The default domain of the target MBeanServer 
	 * @param objectName The ObjectName of the target MBean
	 * @param objectNameKeys The property keys in the located matching ObjectNames that can be used to uniquely identify the MBean 
	 * @param attributes An array of attribute names
	 * @return A native javascript map of maps containing long values of the read attributes keyed by the corresponding attribute name, in turn mapped by the comma separated key propery values
	 */
	public static Object getNumericAttributes(String mbeanServerName, CharSequence objectName, String[] objectNameKeys, String...attributes) {
		return getNumericAttributes(JMXHelper.getLocalMBeanServer(mbeanServerName), objectName, objectNameKeys, attributes);
	}
	
	/**
	 * Retrieves the value of the named attribute from all matching MBeans by the supplied ObjectName pattern and computes an aggregate.
	 * @param aggregateFunction The name of the aggregate function which should be one of the enumerated in {@link AggregateFunction}
	 * @param server The MBeanServer to fetch from
	 * @param objectName The JMX ObjectName pattern that defines the group of MBeans that values should be retrieved from
	 * @param attributeName The name of the attribute to retrieve values from
	 * @return The specified aggregate of all the retrieved values
	 */
	public static long getNumericAggregate(String aggregateFunction, MBeanServerConnection server, CharSequence objectName, String attributeName) {
		AggregateFunction af = AggregateFunction.forName(aggregateFunction);
		try {
			Set<ObjectName> matches = server.queryNames(JMXHelper.objectName(objectName), null);
			if(matches.isEmpty()) return 0;
			List<Object> values = new ArrayList<Object>(matches.size());
			for(ObjectName on: matches) {
				try {
					values.add(server.getAttribute(on, attributeName));
				} catch (Exception ex) {/* No Op */}
			}
			if(values.isEmpty()) return 0;
			Object result =  af.aggregate(values);
			if(result instanceof Number) {
				return ((Number)result).longValue(); 
			}
			throw new Exception("The aggregate function [" + af.name() + "] did not return a number but a [" + result.getClass().getName() + "]", new Throwable());
		} catch (Exception ex) {
			LOG("Failed to read numeric aggregate for " + attributeName + " from pattern [" + objectName + "]", ex);
			throw new RuntimeException("Failed to read numeric aggregate for " + attributeName + " from pattern [" + objectName + "]", ex);
		}
	}

	/**
	 * Retrieves the value of the named attribute from all matching MBeans in the named MBeanServer by the supplied ObjectName pattern and computes an aggregate.
	 * @param aggregateFunction The name of the aggregate function which should be one of the enumerated in {@link AggregateFunction}
	 * @param mbeanServerName The default domain of the target MBeanServer 
	 * @param objectName The JMX ObjectName pattern that defines the group of MBeans that values should be retrieved from
	 * @param attributeName The name of the attribute to retrieve values from
	 * @return The specified aggregate of all the retrieved values
	 */
	public static long getNumericAggregate(String aggregateFunction, String mbeanServerName, CharSequence objectName, String attributeName) {
		return getNumericAggregate(aggregateFunction, JMXHelper.getLocalMBeanServer(mbeanServerName), objectName, attributeName);
	}
	
	
	/**
	 * Retrieves the value of the named attribute from all matching MBeans in the default MBeanServer by the supplied ObjectName pattern and computes an aggregate.
	 * @param aggregateFunction The name of the aggregate function which should be one of the enumerated in {@link AggregateFunction}
	 * @param objectName The JMX ObjectName pattern that defines the group of MBeans that values should be retrieved from
	 * @param attributeName The name of the attribute to retrieve values from
	 * @return The specified aggregate of all the retrieved values
	 */
	public static long getNumericAggregate(String aggregateFunction, CharSequence objectName, String attributeName) {
		return getNumericAggregate(aggregateFunction, JMXHelper.getHeliosMBeanServer(), objectName, attributeName);
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
