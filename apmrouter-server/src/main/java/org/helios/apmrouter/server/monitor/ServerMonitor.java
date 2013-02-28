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
package org.helios.apmrouter.server.monitor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.monitor.jvm.JVMMonitor;
import org.helios.apmrouter.monitor.nativex.NativeMonitor;
import org.helios.apmrouter.server.ServerComponentBean;
import org.helios.apmrouter.server.tracing.ServerTracerFactory;
import org.helios.apmrouter.trace.ITracer;
import org.helios.apmrouter.util.SystemClock;
import org.helios.apmrouter.util.SystemClock.ElapsedTime;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;
import org.springframework.util.ReflectionUtils;

/**
 * <p>Title: ServerMonitor</p>
 * <p>Description: A service to execute background localStats collection and submission</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.monitor.ServerMonitor</code></p>
 */

public class ServerMonitor extends ServerComponentBean implements Runnable {
	/** The internal server tracer */
	protected final ITracer tracer = ServerTracerFactory.getInstance().getTracer();
	/** The MBeanServer to retrieve the metric values from */
	protected final MBeanServer server = JMXHelper.getHeliosMBeanServer();
	/** A map of maps of GAUGE metric providing operation names for a bean keyed by the JMX ObjectName */
	protected final Map<ObjectName, Map<String[], String[]>> gaugeMetrics = new HashMap<ObjectName, Map<String[], String[]>>();
	/** A map of maps of COUNTER metric providing operation names for a bean keyed by the JMX ObjectName */
	protected final Map<ObjectName, Map<String[], String[]>> counterMetrics = new HashMap<ObjectName, Map<String[], String[]>>();
	
	private final boolean ONE_METRIC_ONLY = false;
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#onApplicationContextRefresh(org.springframework.context.event.ContextRefreshedEvent)
	 */
	@Override
	public void onApplicationContextRefresh(ContextRefreshedEvent event) {
		if(!ONE_METRIC_ONLY) {
			new JVMMonitor().startMonitor();
			new NativeMonitor().startMonitor();
		}		
		info("Collecting instances of ServerComponentBean....");
		final Set<String> invalids = new HashSet<String>();
		Map<String, ServerComponentBean> beans = event.getApplicationContext().getBeansOfType(ServerComponentBean.class, false, false);
		info("Located [", beans.size(), "] instances of ServerComponentBean....");
		int counterHits = 0;
		int gaugeHits = 0;
		for(Map.Entry<String, ServerComponentBean> bean: beans.entrySet()) {
			ObjectName on = bean.getValue().getComponentObjectName();
			final Set<String> attrNames = getMBeanAttributeNames(on);
			//Set<String> jmxOps = new HashSet<String>();
			try {
//				for(MBeanOperationInfo info: server.getMBeanInfo(on).getOperations()) {
//					jmxOps.add(info.getName());
//				}
				Map<String[], String[]> gauges = new HashMap<String[], String[]>();
				Map<String[], String[]> counters = new HashMap<String[], String[]>();
				String beanName = bean.getKey();
				for(Method m: ReflectionUtils.getAllDeclaredMethods(bean.getValue().getClass())) {
					ManagedMetric managedMetric = m.getAnnotation(ManagedMetric.class);
					if(managedMetric!=null) {
						String category = managedMetric.category();						
						String attributeName = attributizeMethodName(m);
						String displayName = managedMetric.displayName();
						
						if(category!=null && category.trim().isEmpty()) category=null;
						if(displayName!=null && displayName.trim().isEmpty()) displayName=null;
						
						String metricName = (displayName==null) ? attributeName : displayName;
						if(metricName==null || metricName.trim().isEmpty()) {
							invalids.add(on + "-" + m.getName() + " evaluated null metricName");
							continue;
						}
						metricName = metricName.trim();
						if(!attrNames.contains(attributeName)) {
							invalids.add("No attribute match for " + on + "-" + m.getName() + " evaluated null metricName");
							continue;							
						}
						
						String[] namespace = new String[category==null ? 2 : 3];
						String[] names = new String[]{metricName, attributeName};
						namespace[0] = "platform=APMRouter";
						namespace[1] = "component=" + beanName;
						if(category!=null) {
							namespace[2] =  "category=" + category;
						}
						
						if(managedMetric.metricType()==MetricType.COUNTER) {
							counters.put(namespace, names);
						} else {
							gauges.put(namespace, names);
						}
					}
				}
				if(!counters.isEmpty()) counterMetrics.put(on, counters);
				if(!gauges.isEmpty()) gaugeMetrics.put(on, gauges);
			} catch (Exception ex) {
				warn("Failed to collect meta-data for ObjectName [", on, "]", ex);
			}
		}
		if(!invalids.isEmpty()) {
			StringBuilder b = new StringBuilder("\n\tInvalid ManagedMetrics for Server Monitor");
			for(String s: invalids) {
				b.append("\n\t").append(s);
			}
			warn(b);
		}
		info("Created [", gaugeMetrics.size(), "] GAUGE metrics and [", counterMetrics.size(), "] COUNTER metrics");
		Thread t = new Thread(this, "ServerMonitorThread");
		t.setDaemon(true);
		t.start();
	}
	
	/**
	 * Returns a set of attribute names for the passed object name
	 * @param on the target object name
	 * @return a set of attribute names
	 */
	protected Set<String> getMBeanAttributeNames(ObjectName on) {
		try {
			MBeanInfo info = JMXHelper.getHeliosMBeanServer().getMBeanInfo(on);
			MBeanAttributeInfo[] attrInfos = info.getAttributes();
			Set<String> attributeNames = new HashSet<String>(attrInfos.length);
			for(MBeanAttributeInfo attrInfo: attrInfos) {
				attributeNames.add(attrInfo.getName());
			}
			return Collections.unmodifiableSet(attributeNames);
		} catch (Exception ex) {
			return Collections.emptySet();
		}
	}
	
	/**
	 * Returns the attribute name for the passed method if it is a traditional getter.
	 * i.e. it is in the form <b><code>getXXX</code></b> with no parameters. 
	 * Otherwise, returns null.
	 * @param method The method to convert
	 * @return the attribute name or null
	 */
	protected String attributizeMethodName(Method method) {
		if(method==null) return null;
		String name = method.getName();
		if(name.startsWith("get") && method.getParameterTypes().length==0) {
			return name.substring(3);
		}
		return null;
	}
	
	/** Null argument const */
	private static final Object[] NULL_ARGS = {};
	/** Null signature const */
	private static final String[] NULL_SIG = {};
	
	/**
	 * Executes the traces 
	 */
	protected void trace() {
		MBeanServer server = JMXHelper.getHeliosMBeanServer();
		for(Map.Entry<ObjectName, Map<String[], String[]>> metrics: counterMetrics.entrySet()) {
			final ObjectName on = metrics.getKey();
			for(Map.Entry<String[], String[]> ops: metrics.getValue().entrySet()) {
				String[] namespace = ops.getKey();
				String metricName = ops.getValue()[0];				
				String attributeName = ops.getValue()[1];
				try {
					final long value = ((Number)server.getAttribute(on, attributeName)).longValue();
					tracer.traceDeltaCounter(value, metricName, namespace);
				} catch (Exception ex) {
					error("Failed to collect counter localStats for ObjectName [", on, "] on namespace: ", Arrays.toString(namespace) , " metricName:[", metricName, "]", ex);
				}				
			}
		}
		for(Map.Entry<ObjectName, Map<String[], String[]>> metrics: gaugeMetrics.entrySet()) {
			final ObjectName on = metrics.getKey();
			for(Map.Entry<String[], String[]> ops: metrics.getValue().entrySet()) {
				String[] namespace = ops.getKey();
				String metricName = ops.getValue()[0];				
				String attributeName = ops.getValue()[1];		
				try {
					final long value = ((Number)server.getAttribute(on, attributeName)).longValue();
					tracer.traceGauge(value, metricName, namespace);
				} catch (Exception ex) {
					error("Failed to collect gauge localStats for ObjectName [", on, "] on namespace: ", Arrays.toString(namespace) , " metricName:[", metricName, "]", ex);
				}				
			}
		}
		
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		while(true) {
			if(!ONE_METRIC_ONLY) {
				SystemClock.startTimer();
				try {
					debug("Collecting Server Metrics");
					trace();
				} finally {
					ElapsedTime et = SystemClock.endTimer();
					tracer.traceGauge(et.elapsedMs, "ElapsedTimeMs", "platform=APMRouter", "category=ServerMonitor");
				}
			} else {
				tracer.traceGauge(SystemClock.time()%5000, "ElapsedTimeMs", "platform=APMRouter", "category=ServerMonitor");
			}
			SystemClock.sleep(5000);
		}
		
	}

}
