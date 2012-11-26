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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanOperationInfo;
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

/**
 * <p>Title: ServerMonitor</p>
 * <p>Description: A service to execute background stats collection and submission</p> 
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
	protected final Map<ObjectName, Map<String, String>> gaugeMetrics = new HashMap<ObjectName, Map<String, String>>();
	/** A map of maps of COUNTER metric providing operation names for a bean keyed by the JMX ObjectName */
	protected final Map<ObjectName, Map<String, String>> counterMetrics = new HashMap<ObjectName, Map<String, String>>();
	
	private final boolean ONE_METRIC_ONLY = true;
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStart()
	 */
	@Override
	protected void doStart() throws Exception {
		if(!ONE_METRIC_ONLY) {
			new JVMMonitor().startMonitor();
			new NativeMonitor().startMonitor();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#onApplicationContextRefresh(org.springframework.context.event.ContextRefreshedEvent)
	 */
	@Override
	public void onApplicationContextRefresh(ContextRefreshedEvent event) {
		info("Collecting instances of ServerComponentBean....");
		Map<String, ServerComponentBean> beans = applicationContext.getBeansOfType(ServerComponentBean.class, false, false);
		info("Located [", beans.size(), "] instances of ServerComponentBean....");
		int counterHits = 0;
		int gaugeHits = 0;
		for(Map.Entry<String, ServerComponentBean> bean: beans.entrySet()) {
			ObjectName on = bean.getValue().getComponentObjectName();
			Set<String> jmxOps = new HashSet<String>();
			try {
				for(MBeanOperationInfo info: server.getMBeanInfo(on).getOperations()) {
					jmxOps.add(info.getName());
				}
				Map<String, String> gauges = new HashMap<String, String>();
				Map<String, String> counters = new HashMap<String, String>();
				for(Method m: bean.getValue().getClass().getDeclaredMethods()) {
					ManagedMetric managedMetric = m.getAnnotation(ManagedMetric.class);
					if(managedMetric!=null) {
						String category = managedMetric.category();
						if(category==null) {
							warn("Method [", m.toGenericString(), "] had no category");
							continue;
						}
						String opName = m.getName();
						if(managedMetric.metricType()==MetricType.COUNTER) {							
							counters.put(jmxOps.contains(opName) ? opName : opName.substring(3), category);
							counterHits++;
						} else {
							gauges.put(jmxOps.contains(opName) ? opName : opName.substring(3), category);
							gaugeHits++;
						}
					}
				}
				if(!counters.isEmpty()) counterMetrics.put(on, counters);
				if(!gauges.isEmpty()) gaugeMetrics.put(on, gauges);
			} catch (Exception ex) {
				warn("Failed to collect meta-data for ObjectName [", on, "]", ex);
			}
		}
		info("Created [", gaugeHits, "] GAUGE metrics and [", counterHits, "] COUNTER metrics");
		Thread t = new Thread(this, "ServerMonitorThread");
		t.setDaemon(true);
		t.start();
	}
	
	/** Null argument const */
	private static final Object[] NULL_ARGS = {};
	/** Null signature const */
	private static final String[] NULL_SIG = {};
	
	/**
	 * Executes the traces 
	 */
	protected void trace() {
		for(Map.Entry<ObjectName, Map<String, String>> metrics: counterMetrics.entrySet()) {
			final ObjectName on = metrics.getKey();
			for(Map.Entry<String, String> ops: metrics.getValue().entrySet()) {
				String opName = ops.getKey();
				final String category = ops.getValue();
				try {
					final long value;
					if(opName.startsWith("get")) {
						value = (Long)server.invoke(on, opName, NULL_ARGS, NULL_SIG);
						opName = opName.substring(3);
					} else {
						value = (Long)server.getAttribute(on, opName);
					}
					tracer.traceDeltaCounter(value, opName, "platform=APMRouter", "category=" + category);
				} catch (Exception ex) {
					error("Failed to collect stats for ObjectName [", on, "] on operation [", opName, "]", ex);
				}				
			}
		}
		for(Map.Entry<ObjectName, Map<String, String>> metrics: gaugeMetrics.entrySet()) {
			final ObjectName on = metrics.getKey();
			for(Map.Entry<String, String> ops: metrics.getValue().entrySet()) {
				String opName = ops.getKey();
				final String category = ops.getValue();
				try {
					final long value;
					if(opName.startsWith("get")) {
						value = (Long)server.invoke(on, opName, NULL_ARGS, NULL_SIG);
						opName = opName.substring(3);
					} else {
						value = (Long)server.getAttribute(on, opName);
					}
					tracer.traceGauge(value, opName, "platform=APMRouter", "category=" + category);
				} catch (Exception ex) {
					error("Failed to collect stats for ObjectName [", on, "] on operation [", opName, "]", ex);
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
