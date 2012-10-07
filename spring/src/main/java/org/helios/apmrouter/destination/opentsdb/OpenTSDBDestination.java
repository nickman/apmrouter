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
package org.helios.apmrouter.destination.opentsdb;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.helios.apmrouter.destination.BaseDestination;
import org.helios.apmrouter.metric.IMetric;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;

/**
 * <p>Title: OpenTSDBDestination</p>
 * <p>Description: Destination to send metrics to an OpenTSDB instance</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.opentsdb.OpenTSDBDestination</code></p>
 */

public class OpenTSDBDestination extends BaseDestination {
	/** The OpenTSDB metric recorder */
	protected MetricRecorder recorder = null;
	/** The hBase host name or IP address */
	protected String host = null;
	/** The hBase port, defaults to <code>2181</code> */
	protected int port = 2181;
	/** Indicates if the main graphite client channel is currently connected */
	protected final AtomicBoolean connected = new AtomicBoolean(false);

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStart()
	 */
	@Override
	protected void doStart() throws Exception {
		if(host!=null) {
			doConnect();
		}
	}
	
	/**
	 * Connects the OpnTSDB hbase connection
	 */
	protected void doConnect() {
		// running this is a throw-away thread because it blocks
		Thread t = new Thread("OpenTSDBConnector Thread") {
			public void run() {
				recorder = MetricRecorder.getInstance(host + ":" + port);
				connected.set(true);
				info("OpenTSDB MetricRecorder Connected");				
			}
		};
		t.setDaemon(true);
		t.start();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStop()
	 */
	@Override
	protected void doStop() {	
	}
	
	/**
	 * Accept Route additive for BaseDestination extensions
	 * @param routable The metric to route
	 */
	@Override
	protected void doAcceptRoute(IMetric routable) {
		if(!connected.get()) return;
		if(routable.isMapped()) {
			recorder.newRecording(routable.getName(), routable.getLongValue())
				.tags(routable.getNamespaceMap(true, true))
				.record();
//			if(routable.hasTXContext()) {
//				info("TXContext:" + routable.getTXContext());
//			}
			incr("MetricsForwarded");
		} else {
			incr("UnmappedMetricDrops");
		}
	}

	/**
	 * Returns the number of metrics not forwarded to OpenTSDB because they were not mapped
	 * @return the number of metrics not forwarded to OpenTSDB because they were not mapped
	 */
	@ManagedMetric(category="OpenTSDB", metricType=MetricType.COUNTER, description="the number of metrics not forwarded to OpenTSDB because they were not mapped")
	public long getUnmappedMetricDrops() {
		return getMetricValue("UnmappedMetricDrops");
	}
	
	
	/**
	 * Returns the number of metrics forwarded to OpenTSDB
	 * @return the number of metrics forwarded to OpenTSDB
	 */
	@ManagedMetric(category="OpenTSDB", metricType=MetricType.COUNTER, description="the number of metrics forwarded to OpenTSDB")
	public long getMetricsForwarded() {
		return getMetricValue("MetricsForwarded");
	}
	
	/**
	 * Returns the number of metrics that failed on sending to OpenTSDB
	 * @return the number of metrics that failed on sending to OpenTSDB
	 */
	@ManagedMetric(category="OpenTSDB", metricType=MetricType.COUNTER, description="the number of metrics that failed on sending to OpenTSDB")
	public long getMetricsForwardFailures() {
		return getMetricValue("MetricsForwardFailures");
	}
	
	/**
	 * Returns the number of metrics that were dropped because OpenTSDB was down
	 * @return the number of metrics that were dropped because OpenTSDB was down
	 */
	@ManagedMetric(category="OpenTSDB", metricType=MetricType.COUNTER, description="the number of metrics that were dropped because OpenTSDB was down")
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
		_metrics.add("MetricsForwardFailures");
		_metrics.add("UnmappedMetricDrops");		
		return _metrics;
	}
	

	/**
	 * Returns the openTSDB hbase server name or IP address
	 * @return the openTSDB hbase server name or IP address
	 */
	@ManagedAttribute
	public String getOpenTSDBHost() {
		return host;
	}
	
	/**
	 * Indicates if the openTSDB hbase metric recorder is connected
	 * @return true if the openTSDB hbase metric recorder  is connected, false otherwise
	 */
	@ManagedAttribute
	public boolean isConnected() {
		return connected.get();
	}

	/**
	 * Sets the openTSDB hbase server name or IP address
	 * @param host the openTSDB hbase server name or IP address
	 */
	@ManagedAttribute
	public void setOpenTSDBHost(String host) {
		if(isStarted()) throw new IllegalStateException("Cannot set the openTSDB hbase host once listener is bound", new Throwable());
		this.host = host;
	}

	/**
	 * Sets the openTSDB hbase server listening port
	 * @param port the openTSDB hbase server listening port
	 */
	@ManagedAttribute
	public void setOpenTSDBPort(int port) {
		if(isStarted()) throw new IllegalStateException("Cannot set the openTSDB hbase port once listener is bound", new Throwable());
		this.port= port;
	}



	/**
	 * Returns the openTSDB hbase port 
	 * @return the openTSDB hbase
	 */
	@ManagedAttribute
	public int getOpenTSDBPort() {
		return port;
	}



	
}
