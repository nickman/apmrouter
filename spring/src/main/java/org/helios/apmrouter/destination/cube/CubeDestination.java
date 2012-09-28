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
package org.helios.apmrouter.destination.cube;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.helios.apmrouter.destination.BaseDestination;
import org.helios.apmrouter.metric.IMetric;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.jboss.netty.handler.codec.http.websocket.DefaultWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocket.WebSocketFrame;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;

import se.cgbystrom.netty.http.websocket.WebSocketCallback;
import se.cgbystrom.netty.http.websocket.WebSocketClient;
import se.cgbystrom.netty.http.websocket.WebSocketClientHandler;

/**
 * <p>Title: CubeDestination</p>
 * <p>Description: Sends metrics to <a href="https://github.com/square/cube">Cube</a></p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.cube.CubeDestination</code></p>
 */
public class CubeDestination extends BaseDestination implements ChannelPipelineFactory, WebSocketCallback  {
	/** The netty boss pool */
	protected ExecutorService bossPool;
	/** The nety worker pool */
	protected ExecutorService workerPool;
	/** The client bootstrap */
	protected ClientBootstrap bstrap;
	/** The client channel factory */
	protected NioClientSocketChannelFactory channelFactory;
	/** The channel options */
	protected Map<String, Object> channelOptions = new HashMap<String, Object>();
	/** The Cube URI, e.g. <code>ws://localhost:1080/1.0/event/put</code> */
	protected URI cubeUri = null;
	/** The managed channel group */
	protected ChannelGroup channelGroup = null;
	/** The main forwarding connection channel */
	protected Channel channel = null;
	/** The main forwarding connection channel close future */
	protected ChannelFuture closeFuture = null;
	/** Indicates if the main graphite client channel is currently connected */
	protected final AtomicBoolean connected = new AtomicBoolean(false);
	/** Indicates if a disconnect is expected. (If a disconnect occures and this is false, a reconnect thread will start) */
	protected final AtomicBoolean expectDisconnect = new AtomicBoolean(false);
	/** The WebSocketClient */
	protected WebSocketClient webSockClient;

	/**
	 * Starts this listener
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStart()
	 */
	@Override
	protected void doStart() throws Exception {
		channelGroup = new DefaultChannelGroup(beanName);
		channelFactory = new NioClientSocketChannelFactory(bossPool, workerPool);
		bstrap = new ClientBootstrap(channelFactory);
		bstrap.setOptions(channelOptions);
		bstrap.setPipelineFactory(this);		
		doConnect();
	}
	
	/**
	 * Stops this listener
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStop()
	 */
	@Override
	protected void doStop() {	
		channelGroup.close().awaitUninterruptibly();
		channelFactory.releaseExternalResources();
		channelFactory = null;
		channelGroup = null;
		webSockClient = null;
		connected.set(false);
	}	
	
	/**
	 * Initiates an asynch connect to the graphite server
	 */
	protected void doConnect() {
		webSockClient = new WebSocketClientHandler(bstrap, cubeUri, this);
		webSockClient.connect();
	}
	
	/**
	 * Accept Route additive for BaseDestination extensions
	 * @param routable The metric to route
	 */
	protected void doAcceptRoute(IMetric routable) {
		if(connected.get()) {
			writeToCube(routable.getUnmapped());
			incr("MetricsForwarded");
		} else {
			incr("MetricsDropped");
		}
	}	
	
	/** The format of a Cube message */
	public static final String CUBE_MSG_FORMAT = "{\"type\":\"metric\", \"data\":{" + 
			"\"host\":\"%s\"," +
			"\"agent\":\"%s\"," +
			"\"fqn\":\"%s\"," +
			"\"type\":\"%s\"," +
			"\"ts\":%s," + 
			"\"val\":%s" + 
			"}}";
	
      
      
	
	
	/**
	 * Writes the metric to cube
	 * @param metric The metric to write
	 */
	@SuppressWarnings("deprecation")
	protected void writeToCube(IMetric metric) {
		String message = String.format(CUBE_MSG_FORMAT, 
				metric.getHost(), metric.getAgent(),
				metric.getFQN(), metric.getType().name(),
				metric.getTime(), metric.getLongValue()
		);
		webSockClient.send(new DefaultWebSocketFrame(message));
//		try {
//			JSONObject json = new JSONObject();
//			json.putOnce("type", "metric");
//			Map<String, Object> m = new HashMap<String, Object>();
//			m.put("host", metric.getHost());
//			m.put("agent", metric.getAgent());
//			m.put("fqn", metric.getFQN());
//			m.put("type", metric.getType().name());
//			m.put("ts", metric.getTime());
//			m.put("val", metric.getLongValue());
//			json.put("data", m);
//			info("\n\n<", json, ">\n\n");
//			webSockClient.send(new DefaultWebSocketFrame(json.toString(1)));
//		} catch (JSONException e) {
//			e.printStackTrace(System.err);
//			incr("MetricsForwardFailures");
//		}
		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponent#getSupportedMetricNames()
	 */
	@Override
	public Set<String> getSupportedMetricNames() {
		Set<String> _metrics = new HashSet<String>(super.getSupportedMetricNames());
		_metrics.add("ChannelsCreated");
		_metrics.add("ChannelsClosed");
		_metrics.add("MetricsForwarded");
		_metrics.add("MetricsDropped");		
		_metrics.add("MetricsForwardFailures");
		return _metrics;
	}

	
	
	/**
	 * Creates a new CubeDestination
	 * @param patterns The {@link IMetric} pattern this destination accepts
	 */
	public CubeDestination(String... patterns) {
		super(patterns);
	}

	/**
	 * Creates a new CubeDestination
	 * @param patterns The {@link IMetric} pattern this destination accepts
	 */
	public CubeDestination(Collection<String> patterns) {
		super(patterns);
	}

	/**
	 * Creates a new CubeDestination
	 */
	public CubeDestination() {
		
	}
	
	/**
	 * Returns the cube websocket URI
	 * @return the cube websocket URI
	 */
	@ManagedAttribute
	public URI getCubeUri() {
		return cubeUri;
	}
	
	/**
	 * Sets the cube websocket URI
	 * @param cubeUri the cube websocket URI
	 */
	public void setCubeUri(URI cubeUri) {
		this.cubeUri = cubeUri;
	}	
	
	
	/**
	 * Returns the channel options applied to channels created by this listener
	 * @return the channel options applied
	 */
	public Map<String, Object> getChannelOptions() {
		return channelOptions;
	}

	/**
	 * Sets the worker pool for the graphite destination
	 * @param workerPool the netty worker thread pool
	 */
	public void setWorkerPool(ExecutorService workerPool) {
		this.workerPool = workerPool;
	}
	
	/**
	 * Sets the boss pool for the graphite destination
	 * @param bossPool the netty boss thread pool
	 */
	public void setBossPool(ExecutorService bossPool) {
		this.bossPool = bossPool;
	}
	
	
	/**
	 * Indicates if the cube channel is connected
	 * @return true if the cube channel is connected, false otherwise
	 */
	@ManagedAttribute
	public boolean isConnected() {
		return connected.get();
	}
	

	@Override
	public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = Channels.pipeline();
//        pipeline.addLast("logger", new LoggingHandler(beanName, InternalLogLevel.INFO, true));
        pipeline.addLast("decoder", new HttpResponseDecoder());
        pipeline.addLast("encoder", new HttpRequestEncoder());
        pipeline.addLast("ws-handler", (WebSocketClientHandler)webSockClient);
        return pipeline;
	}
	
	/**
	 * Returns the number of metrics forwarded to Cube
	 * @return the number of metrics forwarded to Cube
	 */
	@ManagedMetric(category="Cube", metricType=MetricType.COUNTER, description="the number of metrics forwarded to Cube")
	public long getMetricsForwarded() {
		return getMetricValue("MetricsForwarded");
	}
	
	/**
	 * Returns the number of metrics that failed on sending to Cube
	 * @return the number of metrics that failed on sending to Cube
	 */
	@ManagedMetric(category="Cube", metricType=MetricType.COUNTER, description="the number of metrics that failed on sending to Cube")
	public long getMetricsForwardFailures() {
		return getMetricValue("MetricsForwardFailures");
	}
	
	/**
	 * Returns the number of metrics that were dropped because Cube was down
	 * @return the number of metrics that were dropped because Cube was down
	 */
	@ManagedMetric(category="Cube", metricType=MetricType.COUNTER, description="the number of metrics that were dropped because Cube was down")
	public long getMetricsDropped() {
		return getMetricValue("MetricsDropped");
	}
	

	/**
	 * {@inheritDoc}
	 * @see se.cgbystrom.netty.http.websocket.WebSocketCallback#onConnect(se.cgbystrom.netty.http.websocket.WebSocketClient)
	 */
	@Override
	public void onConnect(WebSocketClient webSock) {
		info("Cube WebSocketClient Connected");
		connected.set(true);
		
	}

	/**
	 * {@inheritDoc}
	 * @see se.cgbystrom.netty.http.websocket.WebSocketCallback#onDisconnect(se.cgbystrom.netty.http.websocket.WebSocketClient)
	 */
	@Override
	public void onDisconnect(WebSocketClient webSock) {
		info("Cube WebSocketClient Disconnected");
		this.webSockClient = null;
		connected.set(false);
		
	}

	/**
	 * {@inheritDoc}
	 * @see se.cgbystrom.netty.http.websocket.WebSocketCallback#onError(java.lang.Throwable)
	 */
	@Override
	public void onError(Throwable t) {
		error("WebSock Error", t);
		
	}

	/**
	 * No Op
	 * {@inheritDoc}
	 * @see se.cgbystrom.netty.http.websocket.WebSocketCallback#onMessage(se.cgbystrom.netty.http.websocket.WebSocketClient, org.jboss.netty.handler.codec.http.websocket.WebSocketFrame)
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void onMessage(WebSocketClient webSock, WebSocketFrame frame) {
		if(frame.isText()) {
			info("Unexpected Cube Response [", frame.getTextData(), "]");
		} else {
			info("Unexpected Cube Response Type:", frame.getType());
		}
	}
}
