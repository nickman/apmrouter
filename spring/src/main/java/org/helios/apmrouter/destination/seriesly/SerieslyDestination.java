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

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.helios.apmrouter.destination.accumulator.MetricTextAccumulator;
import org.helios.apmrouter.destination.accumulator.MetricTextFlushReceiver;
import org.helios.apmrouter.destination.netty.NettyTCPDestination;
import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.metric.JSONFormatterImpl;
import org.helios.apmrouter.util.SystemClock;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.springframework.jmx.export.annotation.ManagedAttribute;

/**
 * <p>Title: SerieslyDestination</p>
 * <p>Description: Metric destination for the <a href="https://github.com/dustin/seriesly">Seriesly</a> time series database.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.seriesly.SerieslyDestination</code></p>
 */

public class SerieslyDestination extends NettyTCPDestination implements MetricTextFlushReceiver  {
	/** The JSON formatter */
	protected JSONFormatterImpl jsonFormatter = new JSONFormatterImpl(false, false);
	/** THe metric text accumulator */
	protected MetricTextAccumulator accumulator;
	/** The seriesly db name */
	protected String dbName = "helios";
	//protected String serieslyUrl = uriPrefix "http://localhost:3133/helios";
	/** The time based flush trigger in ms. */
	protected long timeTrigger = 15000;
	/** The size based flush trigger in number of metrics accumulated */
	protected int sizeTrigger = 30;
	/** Indicates if the db has been created */
	protected final AtomicBoolean dbCreated = new AtomicBoolean(false);
	/** The URI prefix for the seriesly server data submission endpoint */
	protected String uriPrefixTemplate = null;
	
	/** JSON Opener Channel Buffer Constant */
	protected static final ChannelBuffer JSON_OPEN = ChannelBuffers.wrappedBuffer("{".getBytes());
	/** JSON Closer Channel Buffer Constant */
	protected static final ChannelBuffer JSON_CLOSE = ChannelBuffers.wrappedBuffer("}".getBytes());
	
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
	 * Creates the seriesly DB
	 * @return true if the DB was created or verified successfully, false otherwise
	 */
	protected synchronized boolean createDb() {
		info("Validating Seriesly DB [", dbName, "]");
		HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.PUT, String.format("http://%s:%s/%s", host, port, dbName));
		request.setHeader(HttpHeaders.Names.HOST, host);
		request.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);		
		ChannelFutureListener fl = new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture f) throws Exception {
				if(f.isSuccess()) {
					dbCreated.set(true);
					info("Validated Seriesly DB [", dbName, "]");
				} else {
					dbCreated.set(false);
					error("Failed to validate Seriesly DB [", dbName, "]", f.getCause());
					Throwable t = f.getCause().getCause();
					if(t!=null) {
						t.printStackTrace(System.err);
					}
				}				
			}
		};
		
		ChannelFuture cf = channel.write(request);
		cf.addListener(fl);
		if(!cf.awaitUninterruptibly(1000)) {
			return false;
		}
		return dbCreated.get();
	}
	
	/**
	 * <p>Creates the seriesly URI prefix.
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.netty.NettyDestination#doStart()
	 */
	@Override
	protected void doStart() throws Exception {
		super.doStart();
		accumulator = new MetricTextAccumulator(jsonFormatter, this, 10240, sizeTrigger, timeTrigger, TimeUnit.MILLISECONDS);
		uriPrefixTemplate =  String.format("http://%s:%s/%s?ts=%s", host, port, dbName, "%s");
		createDb();
	}
	
	/**
	 * Accept Route additive for BaseDestination extensions
	 * @param routable The metric to route
	 */
	@Override
	protected void doAcceptRoute(IMetric routable) {
		try {
			//accumulator.append(routable);
			flush(ChannelBuffers.wrappedBuffer(jsonFormatter.toJSONBytes(routable)), 1);
		} catch (Exception e) {
			incr("MetricsForwardFailures");
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.accumulator.MetricTextFlushReceiver#flush(org.jboss.netty.buffer.ChannelBuffer, int)
	 */
	@Override
	public void flush(ChannelBuffer metricText, final int metricCount) {
		//info("Flushing [", metricCount, "] metrics");
		if(metricCount != 1) return;
		if(!dbCreated.get()) {
			if(!createDb()) {
				incr("MetricsDropped", metricCount);
				return;
			}
		}
		String uri = String.format(uriPrefixTemplate, SystemClock.time());
		HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, uri);
		request.setHeader(HttpHeaders.Names.HOST, host);
		request.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
		request.setHeader(HttpHeaders.Names.CONTENT_TYPE, "application/json;charset=UTF-8");
		request.setHeader(HttpHeaders.Names.CONTENT_LENGTH, metricText.readableBytes());
		//request.setContent(ChannelBuffers.copiedBuffer(JSON_OPEN, metricText, JSON_CLOSE));
		request.setContent(metricText);
		//request.setHeader(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP);
	
		channel.write(request).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture f) throws Exception {
				if(f.isSuccess()) {
					//info("Successfully Forwarded [", metricCount, "] metrics");
					incr("MetricsForwarded", metricCount);
				} else {
					//error("Failed to forward [", metricCount, "] metrics");
					incr("MetricsForwardFailures", metricCount);
					
					if(getMetricsForwardFailures()<3) {
						f.getCause().printStackTrace(System.err);
						Throwable t = f.getCause().getCause();
						if(t!=null) {
							t.printStackTrace(System.err);
						}
					}
				}				
			}
		});
		
	}	
	
	

//	OutputStream output = null;
//	HttpURLConnection connection = null;
//	try {
//		connection = (HttpURLConnection)new URL(serieslyUrl  + "?ts=" + SystemClock.time()).openConnection();
//		//connection.setDoOutput(true); // Triggers POST.
//		connection.setRequestMethod("POST");
//		connection.setRequestProperty("Accept-Charset", "UTF-8");
//		connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");	
//		connection.setDoOutput(true);
//		output = connection.getOutputStream();
//		output.write(jsonFormatter.toJSONBytes(routable));
//		output.flush();
//		incr("MetricsForwarded");
//	} catch (Exception e) {
//		incr("MetricsForwardFailures");
//	} finally {
//		try { output.close(); } catch (Exception e) {}
//	}






	/**
	 * Sets the json formatter that will build the json documents to store in Seriesly
	 * @param jsonFormatter the jsonFormatter to set
	 */
	public void setJsonFormatter(JSONFormatterImpl jsonFormatter) {
		this.jsonFormatter = jsonFormatter;
	}

	/**
	 * Returns the time based flush trigger in ms.
	 * @return the time based flush trigger
	 */
	@ManagedAttribute(description="The time based flush trigger in ms.")
	public long getTimeTrigger() {
		return timeTrigger;
	}

	/**
	 * Sets the time based flush trigger
	 * @param timeTrigger the frequency that the buffer is flushed in ms.
	 */
	public void setTimeTrigger(long timeTrigger) {
		this.timeTrigger = timeTrigger;
	}

	/**
	 * Returns the size based flush trigger
	 * @return the size based flush trigger
	 */
	@ManagedAttribute(description="The metric size based flush trigger")
	public int getSizeTrigger() {
		return sizeTrigger;
	}

	/**
	 * Sets the size based flush trigger
	 * @param sizeTrigger the number of metrics to accumulate before they are flushed
	 */
	public void setSizeTrigger(int sizeTrigger) {
		this.sizeTrigger = sizeTrigger;
	}

	/**
	 * Returns the name of the seriesly database
	 * @return the name of the seriesly database
	 */
	@ManagedAttribute(description="The name of the seriesly database")
	public String getDbName() {
		return dbName;
	}

	/**
	 * Sets the name of the seriesly database
	 * @param dbName the name of the seriesly database
	 */
	public void setDbName(String dbName) {
		this.dbName = dbName;
	}


}
