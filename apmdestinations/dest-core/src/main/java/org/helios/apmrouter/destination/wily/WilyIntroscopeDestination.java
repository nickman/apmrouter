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
package org.helios.apmrouter.destination.wily;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.helios.apmrouter.destination.BaseDestination;
import org.helios.apmrouter.metric.IMetric;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;

/**
 * <p>Title: WilyIntroscopeDestination</p>
 * <p>Description: Destination to route metrics to a Wily Introscope Enterprise Manager instance.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.wily.WilyIntroscopeDestination</code></p>
 */

public class WilyIntroscopeDestination extends BaseDestination {
	/** The file name of the Introscope <code>agent.jar</code> */
	protected String wilyAgentLib = null;
	/** The file name of the Introscope agent configuration properties */
	protected String wilyAgentProps = null;
	/** The wily agent name for this apmrouter instance */
	protected String wilyAgentName = null;
	/** Indicates if the apm router host and agent name should be prepended to the wily metric name */
	protected boolean includeHostAgent = true;
	

	/** The wily agent tracer */
	protected WilyIntroscopeTracer wilyTracer = null;
	/** The classloader created to load the wily agent */
	protected ClassLoader wilyClassLoader = null;
	/** A cache of wily metric names keyed by the long hash code of the APMRouter metric's FQN */
	protected final ConcurrentHashMap<Long, String> wilyMetricCache = new ConcurrentHashMap<Long, String>(1024); 
	
	
	/**
	 * <p>Initializes the wily agent.
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStart()
	 */
	public void doStart() throws Exception {
		File agentJarFile = new File(wilyAgentLib);
		if(!agentJarFile.canRead()) {
			throw new Exception("The configured wily agent jar [" + wilyAgentLib + "] cannot be read", new Throwable());
		}
		// check to see if an agent name has been defined already
		String agentName = System.getProperty(WilyIntroscopeTracer.AGENT_NAME_SYSPROP);
		if(agentName==null || agentName.trim().isEmpty()) {
			System.setProperty(WilyIntroscopeTracer.AGENT_NAME_SYSPROP, wilyAgentName);
		} else {
			wilyAgentName = agentName;
		}
		String agentProps = System.getProperty(WilyIntroscopeTracer.AGENT_CONFIG_SYSPROP);
		if(agentProps==null || agentProps.trim().isEmpty()) {
			System.setProperty(WilyIntroscopeTracer.AGENT_CONFIG_SYSPROP, wilyAgentProps);
		} else {
			wilyAgentProps = agentProps;
		}
		
		info("Wily Agent Name [", wilyAgentName, "]");
		info("Wily Agent Props [", wilyAgentProps, "]");
		URL agentJarURL = agentJarFile.toURI().toURL();
		wilyClassLoader = new URLClassLoader(new URL[]{agentJarURL}, this.getClass().getClassLoader());
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(wilyClassLoader);
			wilyTracer = WilyIntroscopeTracer.getInstance();
			info("Loaded Wily Agent Tracer");
			super.doStart();
		} finally {
			Thread.currentThread().setContextClassLoader(cl);
		}		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.BaseDestination#doAcceptRoute(org.helios.apmrouter.metric.IMetric)
	 */
	protected void doAcceptRoute(IMetric routable) {
		IMetric metric = routable.getUnmapped();
		try {
			switch (routable.getType()) {
			case BLOB:
				incr("MetricsDropped");
				break;
			case DELTA_COUNTER:
				wilyTracer.recordCurrentValue(metric.getLongValue(), getWilyMetricName(metric));
				incr("MetricsForwarded");
				break;
			case DELTA_GAUGE:
				wilyTracer.recordCurrentValue(metric.getLongValue(), getWilyMetricName(metric));
				incr("MetricsForwarded");			
				break;
			case ERROR:
				break;
			case LONG_COUNTER:
				wilyTracer.recordCurrentValue(metric.getLongValue(), getWilyMetricName(metric));
				incr("MetricsForwarded");			
				break;
			case LONG_GAUGE:
				wilyTracer.recordCurrentValue(metric.getLongValue(), getWilyMetricName(metric));
				incr("MetricsForwarded");						
				break;
			case PDU:
				incr("MetricsDropped");
				break;
			case STRING:
				wilyTracer.recordDataPoint(metric.getValue().toString(), getWilyMetricName(metric));
				info("Traced String value: [" + metric.getValue().toString() + "]");
				incr("MetricsForwarded");									
				break;
			default:
				incr("MetricsDropped");
				break;		
			}
		} catch (Exception e) {
			incr("MetricsFailed");		
		}
	}
	
	/**
	 * Returns the equivalent wily metric name for the passed metric
	 * @param metric The metric to get the wily metric name for 
	 * @return the wily metric
	 */
	protected String getWilyMetricName(IMetric metric) {
		String s = wilyMetricCache.get(metric.getLongHashCode());		
		if(s==null) {
			String[] segs = new String[1 + metric.getNamespaceSize() + (includeHostAgent ? 2 : 0)];
			int offset = 0;
			if(includeHostAgent) {
				segs[offset++] = metric.getHost();
				segs[offset++] = metric.getAgent();
			}
			System.arraycopy(metric.getNamespace(), 0, segs, offset, metric.getNamespaceSize());
			segs[segs.length-1] = metric.getName();
			s = wilyTracer.buildMetricName(segs);
			wilyMetricCache.putIfAbsent(metric.getLongHashCode(), s);
		}
		return s;
	}
	
	
	/**
	 * Creates a new WilyIntroscopeDestination
	 * @param patterns The routing patterns that this destination will accept
	 */
	public WilyIntroscopeDestination(String... patterns) {
		super(patterns);
	}

	/**
	 * Creates a new WilyIntroscopeDestination
	 * @param patterns The routing patterns that this destination will accept
	 */
	public WilyIntroscopeDestination(Collection<String> patterns) {
		super(patterns);
	}

	/**
	 * Creates a new WilyIntroscopeDestination
	 */
	public WilyIntroscopeDestination() {
	}

	/**
	 * Returns the file name of the Introscope <code>agent.jar</code>
	 * @return the Introscope <code>agent.jar</code>
	 */
	@ManagedAttribute(description="The file name of the agent jar")
	public String getWilyAgentLib() {
		return wilyAgentLib;
	}

	/**
	 * Sets the file name of the Introscope <code>agent.jar</code>
	 * @param wilyAgentLib the file name of the Introscope <code>agent.jar</code>
	 */
	public void setWilyAgentLib(String wilyAgentLib) {
		this.wilyAgentLib = wilyAgentLib;
	}

	/**
	 * Returns the file name of the agent configuration properties file
	 * @return the file name of the agent configuration properties file
	 */
	@ManagedAttribute(description="The file name of the agent configuration properties file")
	public String getWilyAgentProps() {
		return wilyAgentProps;
	}

	/**
	 * Sets the file name of the agent configuration properties file
	 * @param wilyAgentProps the file name of the agent configuration properties file
	 */
	public void setWilyAgentProps(String wilyAgentProps) {
		this.wilyAgentProps = wilyAgentProps;
	}

	/**
	 * Returns the wily agent name
	 * @return the wily agent name
	 */
	@ManagedAttribute(description="The wily agent name")
	public String getWilyAgentName() {
		return wilyAgentName;
	}

	/**
	 * Sets the wily agent name
	 * @param wilyAgentName the wily agent name
	 */
	public void setWilyAgentName(String wilyAgentName) {
		this.wilyAgentName = wilyAgentName;
	}

	/**
	 * Indicates if the apm router host and agent name should be prepended to the wily metric name
	 * @return true if host and agent name should be prepended, false otherwise
	 */
	@ManagedAttribute(description="Indicates if the apm router host and agent name should be prepended to the wily metric name")
	public boolean isIncludeHostAgent() {
		return includeHostAgent;
	}

	/**
	 * Sets if the apm router host and agent name should be prepended to the wily metric name
	 * @param includeHostAgent true to prepend the host and agent, false otherwise
	 */
	public void setIncludeHostAgent(boolean includeHostAgent) {
		this.includeHostAgent = includeHostAgent;
	}
	
	/**
	 * Returns the size of the wily metric cache
	 * @return the size of the wily metric cache
	 */
	@ManagedAttribute(description="The size of the wily metric cache") 
	public int getMetricCacheSize() {
		return wilyMetricCache.size();
	}
	
	/**
	 * Returns the number of metrics forwarded to WilyIntroscope
	 * @return the number of metrics forwarded to WilyIntroscope
	 */
	@ManagedMetric(category="WilyIntroscope", metricType=MetricType.COUNTER, description="the number of metrics forwarded to WilyIntroscope")
	public long getMetricsForwarded() {
		return getMetricValue("MetricsForwarded");
	}
	
	/**
	 * Returns the number of metrics that failed on sending to WilyIntroscope
	 * @return the number of metrics that failed on sending to WilyIntroscope
	 */
	@ManagedMetric(category="WilyIntroscope", metricType=MetricType.COUNTER, description="the number of metrics that failed on sending to WilyIntroscope")
	public long getMetricsForwardFailures() {
		return getMetricValue("MetricsFailed");
	}
	
	/**
	 * Returns the number of metrics that were dropped because of unsupported types
	 * @return the number of metrics that were dropped because of unsupported types
	 */
	@ManagedMetric(category="WilyIntroscope", metricType=MetricType.COUNTER, description="the number of metrics that were dropped because of of unsupported types")
	public long getMetricsDropped() {
		return getMetricValue("MetricsDropped");
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponent#getSupportedMetricNames()
	 */
	@Override
	public Set<String> getSupportedMetricNames() {
		Set<String> _metrics = new HashSet<String>(super.getSupportedMetricNames());
		_metrics.add("MetricsForwarded");
		_metrics.add("MetricsDropped");
		_metrics.add("MetricsFailed");		
		return _metrics;
	}	
}
