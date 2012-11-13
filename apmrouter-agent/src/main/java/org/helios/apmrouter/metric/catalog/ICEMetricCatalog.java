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
package org.helios.apmrouter.metric.catalog;

import static org.helios.apmrouter.util.Methods.nvl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.metric.MetricType;
import org.helios.apmrouter.util.SystemClock;
import org.helios.apmrouter.util.SystemClock.ElapsedTime;

/**
 * <p>Title: ICEMetricCatalog</p>
 * <p>Description: Maintains a map of all created {@link IDelegateMetric}s </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.metric.ICEMetricCatalog</code></p>
 */

public class ICEMetricCatalog implements IMetricCatalog {
	/** The singleton instance */
	private static volatile ICEMetricCatalog instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** The system property that defines the metric factory name*/
	public static final String METRIC_FACTORY_PROP = "apmrouter.metric.factory.name";
	/** The default metric factory name*/
	public static final String DEFAULT_METRIC_FACTORY = "org.helios.apmrouter.metric.catalog.heap.StringKeyedHeapMetricCatalog";
	
	/** The delegate concrete catalog implementation, loaded using the class name defined by the system property <b><code>METRIC_FACTORY_PROP</code></b>. */
	private volatile IMetricCatalog actualCatalog;
	
	/**
	 * Acquires the singleton instance
	 * @return the singleton ICEMetricCatalog 
	 */
	public static ICEMetricCatalog getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new ICEMetricCatalog();
				}
			}
		}
		return instance;
	}
	
	@SuppressWarnings("unchecked")
	private ICEMetricCatalog() {
		String className = System.getProperty(METRIC_FACTORY_PROP, DEFAULT_METRIC_FACTORY);
		IMetricCatalog tmp = null;
		try {
			Class<IMetricCatalog> clazz = (Class<IMetricCatalog>) Class.forName(className);
			tmp = clazz.newInstance();
		} catch (Exception e) {
			try {
				Class<IMetricCatalog> clazz = (Class<IMetricCatalog>) Class.forName(DEFAULT_METRIC_FACTORY);
				tmp = clazz.newInstance();				
			} catch (Exception ex) {
				throw new RuntimeException("Failed to load configured class name [" + className + "] and default class name [" + DEFAULT_METRIC_FACTORY + "]",  ex);
			}
		}
		actualCatalog = tmp;
	}
	
	/**
	 * Resets the metric catalog delegate. This is a testing hook.
	 * <p><b>DO NOT CALL THIS METHOD UNLESS YOU KNOW WHAT YOU'RE DOING.</b>
	 * @param newClassName
	 */
	@SuppressWarnings("unused")
	private static synchronized void reset(String newClassName) {
		if(instance==null || instance.actualCatalog==null) return;
		System.setProperty(METRIC_FACTORY_PROP, nvl(newClassName, "ICEMetricCatalog Reset ClassName"));
		IMetricCatalog toBeDisposed = instance.actualCatalog;
		instance.actualCatalog = null;
		instance = null;		
		toBeDisposed.dispose();
	}
	
	/**
	 * {@inheritDoc} 
	 * @see org.helios.apmrouter.metric.catalog.IMetricCatalog#size()
	 */
	@Override
	public int size() {
		return actualCatalog.size();
	}
	
	/**
	 * Returns the delegate metric id with the passed token
	 * @param metricIdToken The token to resolve
	 * @return The resolved metric ID or null if not found
	 */
	@Override
	public IDelegateMetric get(long metricIdToken) {
		return actualCatalog.get(metricIdToken);
	}
	
	/**
	 * Sets the serialization token for the passed metric identifier
	 * @param host The host name
	 * @param agent The agent name
	 * @param name The metric name
	 * @param type The metric type
	 * @param namespace The namespace segments
	 * @return  the assigned token
	 */
	@Override
	public long setToken(String host, String agent, CharSequence name, MetricType type, CharSequence... namespace) {
		return actualCatalog.setToken(host, agent, name, type, namespace);
	}
	
	/**
	 * Sets the serialization token for the passed metric identifier
	 * @param token The token to set on the metric 
	 * @param host The host name
	 * @param agent The agent name
	 * @param name The metric name
	 * @param type The metric type
	 * @param namespace The namespace segments
	 * @return  the assigned token
	 */
	@Override
	public long setToken(long token, String host, String agent, CharSequence name, MetricType type, CharSequence... namespace) {
		return actualCatalog.setToken(token, host, agent, name, type, namespace);
	}
	
	

	/**
	 * Retrieves the named IDelegateMetric, creating it if it not in the catalog
	 * @param host The host name
	 * @param agent The agent name
	 * @param name The metric name
	 * @param type The metric type
	 * @param namespace The namespace segments
	 * @return the IDelegateMetric
	 */
	@Override
	public IDelegateMetric get(String host, String agent, CharSequence name, MetricType type, CharSequence... namespace) {
		return actualCatalog.get(host, agent, name, type, namespace);
	}

	/**
	 * Retrieves the named IDelegateMetric, creating it if it not in the catalog
	 * @param host The host name
	 * @param agent The agent name
	 * @param name The metric name
	 * @param type The metric type
	 * @param namespace The namespace segments
	 * @return the IDelegateMetric
	 */
	@Override
	public IDelegateMetric get(String host, String agent, CharSequence name, int type, CharSequence... namespace) {
		return actualCatalog.get(host, agent, name, type, namespace);
	}

	/**
	 * Retrieves the named IDelegateMetric, creating it if it not in the catalog
	 * @param host The host name
	 * @param agent The agent name
	 * @param name The metric name
	 * @param type The metric type
	 * @param namespace The namespace segments
	 * @return the IDelegateMetric
	 */
	@Override
	public IDelegateMetric get(String host, String agent, CharSequence name, String type, CharSequence... namespace) {
		return actualCatalog.get(host, agent, name, type, namespace);
	}

	/**
	 * Returns the delta for the passed value and metricId key
	 * @param value The long value to get the delta for
	 * @param host The host name
 	 * @param agent The agent name
	 * @param name The metric name
	 * @param namespace The namespace
	 * @return the delta value or null if this was the first call for the metric
	 */
	@Override
	public Long getDelta(long value, String host, String agent, CharSequence name, CharSequence... namespace) {
		return instance.actualCatalog.getDelta(value, host, agent, name, namespace);
	}
	
	/**
	 * Returns the class name of the current catalog in use
	 * @return the class name of the current catalog in use
	 */
	public static String getCatalogClassName() {
		return (instance!=null && instance.actualCatalog!=null) ? instance.actualCatalog.getClass().getName() : null;
	}
	
	/**
	 * Returns a delegate metric for the passed FQN and type
	 * @param fqn The metric FQN
	 * @param type The metric type
	 * @return the delegate metric
	 */
	@Override
	public IDelegateMetric build(String fqn, MetricType type) {
		return build(fqn, type, actualCatalog);
	}

	
	/**
	 * Returns a delegate metric for the passed FQN and type
	 * @param fqn The metric FQN
	 * @param type The metric type
	 * @param catalog The catalog to look the metric up in
	 * @return the delegate metric
	 */
	public static IDelegateMetric build(String fqn, MetricType type, IMetricCatalog catalog) {
		nvl(fqn, "FQN");
		nvl(type, "MetricType");
		String host=null, agent=null, metricName=null;
		List<CharSequence> namespace = new ArrayList<CharSequence>();
		int sequence = 0;
		int lastIndex = 0;
		int index = fqn.indexOf(IMetric.NSDELIM, 0);
		while(index!=-1) {
			if(sequence==0) host = fqn.substring(lastIndex, index);
			else if(sequence==1) agent = fqn.substring(lastIndex+1, index);
			else namespace.add(fqn.substring(lastIndex+1, index));
			sequence++;
			lastIndex = index;
			index = fqn.indexOf(IMetric.NSDELIM, lastIndex+1);
		}
		
		index = fqn.indexOf(IMetric.NADELIM);
		if(sequence==1) {
			agent = fqn.substring(lastIndex+1, index);
		} else {
			namespace.add(fqn.substring(lastIndex+1, index));
		}
		
		metricName = fqn.substring(index+1);
		
		return (catalog==null ? ICEMetricCatalog.getInstance() : catalog).get(host, agent, metricName, type, namespace.toArray(new CharSequence[namespace.size()]));
	}
	
//	public static void main(String[] args) {
//		log("FQN Parse Test");
//		log(build("myhost/myagent:cpu1", MetricType.LONG));
//		log(build("myhost/myagent/foo/bar/baz/cpu:cpu1", MetricType.LONG));
//		
//	}
//	
//
//	public static void log(Object msg) {
//		System.out.println(msg);
//	}

	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.catalog.IMetricCatalog#dispose()
	 * <p><b>No Op</b>
	 */
	@Override
	public void dispose() {
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.catalog.IMetricCatalog#setToken(org.helios.apmrouter.metric.IMetric)
	 */
	@Override
	public long setToken(IMetric metric) {		
		return actualCatalog.setToken(metric);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.catalog.IMetricCatalog#setToken(java.lang.CharSequence, long)
	 */
	@Override
	public void setToken(CharSequence metricFqn, long token) {
		actualCatalog.setToken(metricFqn, token);
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.catalog.IMetricCatalog#setToken(org.helios.apmrouter.metric.catalog.IDelegateMetric, long)
	 */
	@Override
	public long setToken(IDelegateMetric metricId, long token) {
		return actualCatalog.setToken(metricId, token);
	}	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.catalog.IMetricCatalog#resetTokens()
	 */
	public void resetTokens() {
		actualCatalog.resetTokens();
	}
	
	
}



// =============================
// Some other parsers I tested.
// The one above was the fastest
// =============================

//public static String builds(String fqn, MetricType type) {
//	nvl(fqn, "FQN");
//	nvl(type, "MetricType");
//	String host=null, agent=null, metricName=null;
//	List<CharSequence> namespace = new ArrayList<CharSequence>();
//	int sequence = 0;
//	int lastIndex = 0;
//	int index = fqn.indexOf(IMetric.NSDELIM, 0);
//	while(index!=-1) {
//		if(sequence==0) host = fqn.substring(lastIndex, index);
//		else if(sequence==1) agent = fqn.substring(lastIndex+1, index);
//		else namespace.add(fqn.substring(lastIndex+1, index));
//		switch(sequence) {
//		case 0:
//				host = fqn.substring(lastIndex, index);
//				sequence++;
//				break;
//		case 1:
//				agent = fqn.substring(lastIndex+1, index);
//				sequence++;
//				break; 
//		default:
//				namespace.add(fqn.substring(lastIndex+1, index));
//				break; 					
//		}
//		lastIndex = index;
//		index = fqn.indexOf(IMetric.NSDELIM, lastIndex+1);
//	}
//	
//	index = fqn.indexOf(IMetric.NADELIM);
//	if(sequence==1) {
//		agent = fqn.substring(lastIndex+1, index);
//	} else {
//		namespace.add(fqn.substring(lastIndex+1, index));
//	}
//	
//	metricName = fqn.substring(index+1);
//	
//	return log(host, agent, metricName, namespace.toArray(new CharSequence[namespace.size()])); //ICEMetricCatalog.getInstance().get(host, agent, metricName, type, namespace.toArray(new CharSequence[namespace.size()]));
//}
//
///** FQN split pattern */
//public static final Pattern FQN_PATTERN = Pattern.compile("/|:");
//
//public static String buildr(String fqn, MetricType type) {
//	nvl(fqn, "FQN");
//	nvl(type, "MetricType");
//	String[] frags = FQN_PATTERN.split(fqn);
//	String host=frags[0], agent=frags[1], metricName=frags[frags.length-1];
//	if(frags.length>3) {
//		List<CharSequence> namespace = new ArrayList<CharSequence>();
//		for(int i = 2; i < frags.length-1; i++) {
//			namespace.add(frags[i]);
//		}
//		
//		return log(host, agent, metricName, namespace.toArray(new CharSequence[namespace.size()])); //ICEMetricCatalog.getInstance().get(host, agent, metricName, type, namespace.toArray(new CharSequence[namespace.size()]));
//	}
//	log(host, agent, metricName);
//	return log(host, agent, metricName); //ICEMetricCatalog.getInstance().get(host, agent, metricName, type);
//}
//
//public static void main(String[] args) {
//	log("FQN Parse Test");
//	
//	int LOOPS = 2000000;
//	for(int i = 0; i < LOOPS; i++) {
//		if(buildp("myhost/myagent/foo/bar/baz/cpu:cpu1", MetricType.LONG)==null) throw new RuntimeException();
//	}
//	SystemClock.startTimer();
//	for(int i = 0; i < LOOPS; i++) {
//		if(buildp("myhost/myagent/foo/bar/baz/cpu:cpu1", MetricType.LONG)==null) throw new RuntimeException();
//	}
//	ElapsedTime et = SystemClock.endTimer();
//	log("Parser Time:" + et);
//	for(int i = 0; i < LOOPS; i++) {
//		if(builds("myhost/myagent/foo/bar/baz/cpu:cpu1", MetricType.LONG)==null) throw new RuntimeException();
//	}
//
//	SystemClock.startTimer();
//	for(int i = 0; i < LOOPS; i++) {
//		if(builds("myhost/myagent/foo/bar/baz/cpu:cpu1", MetricType.LONG)==null) throw new RuntimeException();
//	}
//	et = SystemClock.endTimer();
//	log("Switcher Time:" + et);
//	for(int i = 0; i < LOOPS; i++) {
//		if(buildr("myhost/myagent/foo/bar/baz/cpu:cpu1", MetricType.LONG)==null) throw new RuntimeException();
//	}		
//	SystemClock.startTimer();
//	for(int i = 0; i < LOOPS; i++) {
//		if(buildr("myhost/myagent/foo/bar/baz/cpu:cpu1", MetricType.LONG)==null) throw new RuntimeException();
//	}
//	et = SystemClock.endTimer();
//	log("Splitter Time:" + et);
//	
////	log(buildp("myhost/myagent:cpu1", MetricType.LONG));
////	log(buildp("myhost/myagent/foo/bar/baz/cpu:cpu1", MetricType.LONG));
//}
//
//public static void log(Object msg) {
//	System.out.println(msg);
//}
//public static String log(String host, String agent, String name, CharSequence...ns) {
//	StringBuilder b = new StringBuilder("[(").append(host).append("-").append(agent).append(")");
//	if(ns.length>0) {
//		for(int i = 0; i < ns.length; i++) {
//			b.append("-").append(ns[i]);
//		}
//	}
//	b.append("]:").append(name);
//	return b.toString();
//}
//
