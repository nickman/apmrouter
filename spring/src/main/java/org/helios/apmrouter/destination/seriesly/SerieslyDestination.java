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
package org.helios.apmrouter.destination.seriesly;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.helios.apmrouter.destination.BaseDestination;
import org.helios.apmrouter.destination.MetricTextFormatter;
import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.metric.JSONFormatterImpl;
import org.helios.apmrouter.util.SystemClock;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;

/**
 * <p>Title: SerieslyDestination</p>
 * <p>Description: Metric destination for the <a href="https://github.com/dustin/seriesly">Seriesly</a> time series database.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.seriesly.SerieslyDestination</code></p>
 */

public class SerieslyDestination extends BaseDestination implements MetricTextFormatter, Runnable, ChannelPipelineFactory, ChannelFutureListener  {
	/** The JSON formatter */
	protected JSONFormatterImpl jsonFormatter = new JSONFormatterImpl(false, false);
	/** The URL prefix of the seriesly data submission URL */
	protected String serieslyUrl = "http://localhost:3133/helios";
	/**
	 * Creates a new SerieslyDestination
	 * @param patterns The metric type patterns accepted by this detination
	 */
	public SerieslyDestination(String... patterns) {
		super(patterns);		
	}

	/**
	 * Creates a new SerieslyDestination
	 * @param patterns The metric type patterns accepted by this detination
	 */
	public SerieslyDestination(Collection<String> patterns) {
		super(patterns);		
	}

	/**
	 * Creates a new SerieslyDestination
	 */
	public SerieslyDestination() {	
	}
	
	/**
	 * Accept Route additive for BaseDestination extensions
	 * @param routable The metric to route
	 */
	@Override
	protected void doAcceptRoute(IMetric routable) {
		OutputStream output = null;
		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection)new URL(serieslyUrl  + "?ts=" + SystemClock.time()).openConnection();
			//connection.setDoOutput(true); // Triggers POST.
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Accept-Charset", "UTF-8");
			connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");	
			connection.setDoOutput(true);
			output = connection.getOutputStream();
			output.write(jsonFormatter.toJSONBytes(routable));
			output.flush();
			incr("MetricsForwarded");
		} catch (Exception e) {
			incr("MetricsForwardFailures");
		} finally {
			try { output.close(); } catch (Exception e) {}
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelFutureListener#operationComplete(org.jboss.netty.channel.ChannelFuture)
	 */
	@Override
	public void operationComplete(ChannelFuture future) throws Exception {
		// TODO Auto-generated method stub
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelPipelineFactory#getPipeline()
	 */
	@Override
	public ChannelPipeline getPipeline() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.MetricTextFormatter#format(java.io.OutputStream, org.helios.apmrouter.metric.IMetric[])
	 */
	@Override
	public int format(OutputStream os, IMetric... metrics) {
		
		int accumulated = 0;
		for(IMetric metric: metrics) {
			try {
				os.write(String.format("", metric.getFQN().replace('/', '.').replace(':', '.').replace(" ", ""), metric.getLongValue(), SystemClock.unixTime(metric.getTime())).getBytes());
				accumulated++;
			} catch (Exception e) {
				incr("MetricsForwardFailures");
			}			
		}
		return accumulated;
	}



	/**
	 * Sets the json formatter that will build the json documents to store in Seriesly
	 * @param jsonFormatter the jsonFormatter to set
	 */
	public void setJsonFormatter(JSONFormatterImpl jsonFormatter) {
		this.jsonFormatter = jsonFormatter;
	}

	/**
	 * Returns the URL prefix of the seriesly data submission URL
	 * @return the URL prefix of the seriesly data submission URL
	 */
	@ManagedAttribute(description="The URL prefix of the seriesly data submission URL")
	public String getSerieslyUrl() {
		return serieslyUrl;
	}

	/**
	 * Sets the URL prefix of the seriesly data submission URL
	 * @param serieslyUrl URL prefix of the seriesly data submission URL
	 */
	@ManagedAttribute(description="The URL prefix of the seriesly data submission URL")
	public void setSerieslyUrl(String serieslyUrl) {
		this.serieslyUrl = serieslyUrl;
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
		return _metrics;
	}
	
	
	/**
	 * Returns the number of metrics that failed on sending to Seriesly
	 * @return the number of metrics that failed on sending to Seriesly
	 */
	@ManagedMetric(category="Seriesly", metricType=MetricType.COUNTER, description="the number of metrics that failed on sending to Seriesly")
	public long getMetricsForwardFailures() {
		return getMetricValue("MetricsForwardFailures");
	}
	
	/**
	 * Returns the number of metrics that were dropped because Seriesly was down
	 * @return the number of metrics that were dropped because Seriesly was down
	 */
	@ManagedMetric(category="Seriesly", metricType=MetricType.COUNTER, description="the number of metrics that were dropped because Seriesly was down")
	public long getMetricsDropped() {
		return getMetricValue("MetricsDropped");
	}
	
	/**
	 * Returns the number of metrics forwarded to Seriesly
	 * @return the number of metrics forwarded to Seriesly
	 */
	@ManagedMetric(category="Seriesly", metricType=MetricType.COUNTER, description="the number of metrics forwarded to Seriesly")
	public long getMetricsForwarded() {
		return getMetricValue("MetricsForwarded");
	}
	

}
