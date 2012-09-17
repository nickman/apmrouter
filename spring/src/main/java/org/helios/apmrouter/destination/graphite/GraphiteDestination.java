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
package org.helios.apmrouter.destination.graphite;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.helios.apmrouter.destination.BaseDestination;
import org.helios.apmrouter.metric.IMetric;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.SimpleChannelDownstreamHandler;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLogLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.support.MetricType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * <p>Title: GraphiteDestination</p>
 * <p>Description: Destination router for Graphite</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.graphite.GraphiteDestination</code></p>
 */

public class GraphiteDestination extends BaseDestination implements Runnable, ChannelPipelineFactory, ChannelFutureListener {
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
	/** The graphite server host name or IP address */
	protected String graphiteHost = null;
	/** The port that the graphite server is listening on */
	protected int graphitePort = -1;
	/** The channel pipeline initial handler bean names */
	protected final SortedMap<Integer, String> channelHandlers = new ConcurrentSkipListMap<Integer, String>();	
	/** The socket address this listener is bound to */
	protected InetSocketAddress socketAddress = null;
	/** The resolved channel handlers in insertion order */
	protected LinkedHashMap<String, ChannelHandler> resolvedHandlers = new LinkedHashMap<String, ChannelHandler>();
	/** Indicates if a logging handler is installed in the pipelines created for this listener */
	protected final AtomicBoolean loggingHandlerInstalled = new AtomicBoolean(false);
	/** The relative location of the logging handler */
	protected String loggingHandlerLocation = null;
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
	/** Indicates if the reconnect loop is running */
	protected final AtomicBoolean reconnecting = new AtomicBoolean(false);
	/** The frequency in ms. of the reconnect loop attempts */
	protected long reconnectPeriod = 10000;
	/** The accumulation buffer */
	protected final GraphiteMetricAccumulator accumulator = new GraphiteMetricAccumulator(10240);
	/** The time based flush trigger in ms. */
	protected long timeTrigger = 15000;
	/** The size based flush trigger in number of metrics accumulated */
	protected long sizeTrigger = 15000;
	/** The injected task scheduler */
	protected ThreadPoolTaskScheduler scheduler = null;
	/** The flush future */
	protected ScheduledFuture<?> flushScheduleHandle = null;
	
	
	


	/**
	 * Starts this listener
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStart()
	 */
	@Override
	protected void doStart() throws Exception {
		info("Resolving Channel Handlers");
		resolvedHandlers.clear();
		for(Map.Entry<Integer, String> entry: channelHandlers.entrySet()) {
			ChannelHandler handler = applicationContext.getBean(entry.getValue(), ChannelHandler.class);
			debug("Resolved Channel Handler [", entry.getValue(), "]");
			resolvedHandlers.put(beanName, handler);
		}
		info("Resolved [", resolvedHandlers.size(), "] Channel Handlers");
		socketAddress = new InetSocketAddress(graphiteHost, graphitePort);
		info("Socket Address:", socketAddress);
		channelGroup = new DefaultChannelGroup(beanName);
		channelFactory = new NioClientSocketChannelFactory(bossPool, workerPool);
		bstrap = new ClientBootstrap(channelFactory);
		bstrap.setOptions(channelOptions);
		bstrap.setPipelineFactory(this);
		doConnect();
		flushScheduleHandle = scheduler.scheduleAtFixedRate(this, timeTrigger);
	}
	
	/**
	 * Initiates an asynch connect to the graphite server
	 */
	protected void doConnect() {
		bstrap.connect(socketAddress).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture f) throws Exception {
				if(f.isSuccess()) {
					onConnect(f.getChannel());
				} else {
					startReconnectLoop();
				}
			}
		});
	}
	
	/**
	 * Handles a disconnect
	 */
	protected void doDisconnect() {
		connected.set(false);
		if(expectDisconnect.get()) {
			error("Unexpected disconnect from [", socketAddress , "]");
			startReconnectLoop();
		} else {
			info("Disconnected from [", socketAddress , "]");
		}
	}
	
	protected void startReconnectLoop() {
		// reconnecting
	}
	
	
	/**
	 * Stops this listener
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStop()
	 */
	@Override
	protected void doStop() {	
		if(flushScheduleHandle!=null) flushScheduleHandle.cancel(false);
		channelGroup.close().awaitUninterruptibly();
		channelFactory.releaseExternalResources();
		channelFactory = null;
		resolvedHandlers.clear();
		socketAddress = null;
		channelGroup = null;
	}
	
	/**
	 * <p>Flushes the accumulated buffer to the graphite server
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		int accumulatedCount = 0;
		ChannelBuffer cb = null;
		synchronized(accumulator) {
			accumulatedCount = accumulator.size();
			if(accumulatedCount > 0) { 
				cb = accumulator.flush();
			}
		}	
		if(accumulatedCount < 1) {
			set("LastMetricsForwarded", 0);
			return;
		}
		String s = new String(cb.array());
		if(connected.get()) {
			final int a = accumulatedCount;
			channel.write(cb).addListener(new ChannelFutureListener() {
				public void operationComplete(ChannelFuture f) throws Exception {
					if(f.isSuccess()) {
						incr("MetricsForwarded", a);
						set("LastMetricsForwarded", a);
					} else {
						incr("MetricsForwardFailures", a);
					}					
				}
			});
			
		} else {
			incr("MetricsDropped", accumulatedCount);
		}
	}
	
	/**
	 * Returns the number of metrics forwarded to Graphite
	 * @return the number of metrics forwarded to Graphite
	 */
	@ManagedMetric(category="Graphite", metricType=MetricType.COUNTER, description="the number of metrics forwarded to Graphite")
	public long getMetricsForwarded() {
		return getMetricValue("MetricsForwarded");
	}
	
	/**
	 * Returns the number of metrics forwarded to Graphite in the last flush
	 * @return the number of metrics forwarded to Graphite in the last flush
	 */
	@ManagedMetric(category="Graphite", metricType=MetricType.COUNTER, description="the number of metrics forwarded to Graphite in the last flush")
	public long getLastMetricsForwarded() {
		return getMetricValue("LastMetricsForwarded");
	}
	
	
	/**
	 * Returns the number of metrics that failed on sending to Graphite
	 * @return the number of metrics that failed on sending to Graphite
	 */
	@ManagedMetric(category="Graphite", metricType=MetricType.COUNTER, description="the number of metrics that failed on sending to Graphite")
	public long getMetricsForwardFailures() {
		return getMetricValue("MetricsForwardFailures");
	}
	
	/**
	 * Returns the number of metrics that were dropped because Graphite was down
	 * @return the number of metrics that were dropped because Graphite was down
	 */
	@ManagedMetric(category="Graphite", metricType=MetricType.COUNTER, description="the number of metrics that were dropped because Graphite was down")
	public long getMetricsDropped() {
		return getMetricValue("MetricsDropped");
	}
	
	/**
	 * Accept Route additive for BaseDestination extensions
	 * @param routable The metric to route
	 */
	protected void doAcceptRoute(IMetric routable) {
		synchronized(accumulator) {
			accumulator.append(routable);
		}
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelFutureListener#operationComplete(org.jboss.netty.channel.ChannelFuture)
	 */
	@Override
	public void operationComplete(ChannelFuture future) throws Exception {		
		
	}
	
	/**
	 * Fired when an asynch connect completes
	 * @param connectedChannel The channel that connected
	 */
	protected void onConnect(Channel connectedChannel) {
		channel = connectedChannel;
		channelGroup.add(channel);
		closeFuture = channel.getCloseFuture();
		closeFuture.addListener(new ChannelFutureListener() {
			public void operationComplete(ChannelFuture future) throws Exception {
				doDisconnect();
			}
		});
		connected.set(true);		
	}
	
	/**
	 * Fired when a connected channel disconnects
	 */
	protected void onDisconnect() {
		channel = null;
		closeFuture = null;
		connected.set(false);		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelPipelineFactory#getPipeline()
	 */
	@Override
	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline pipeline = Channels.pipeline();
//		LinkedHashMap<String, ChannelHandler> tmpHandlers = null; 
//		synchronized(resolvedHandlers) {
//			tmpHandlers = new LinkedHashMap<String, ChannelHandler>(resolvedHandlers);
//		}
//		for(Map.Entry<String, ChannelHandler> entry: tmpHandlers.entrySet()) {
//			pipeline.addLast(entry.getKey(), entry.getValue());
//		}
		pipeline.addLast("Up", new SimpleChannelUpstreamHandler());
		pipeline.addLast("Down", new SimpleChannelDownstreamHandler());
		return pipeline;
	}
	
	
	/**
	 * Returns the graphite server name or IP address
	 * @return the graphite server name or IP address
	 */
	@ManagedAttribute
	public String getGraphiteHost() {
		return graphiteHost;
	}
	
	/**
	 * Indicates if the graphite channel is connected
	 * @return true if the graphite channel is connected, false otherwise
	 */
	@ManagedAttribute
	public boolean isConnected() {
		return connected.get();
	}

	/**
	 * Sets the graphite server name or IP address
	 * @param graphiteHost the graphite server name or IP address
	 */
	@ManagedAttribute
	public void setGraphiteHost(String graphiteHost) {
		if(isStarted()) throw new IllegalStateException("Cannot set the graphite host once listener is bound", new Throwable());
		this.graphiteHost = graphiteHost;
	}

	/**
	 * Sets the graphite server listening port
	 * @param graphitePort the graphite server listening port
	 */
	@ManagedAttribute
	public void setGraphitePort(int graphitePort) {
		if(isStarted()) throw new IllegalStateException("Cannot set the graphite port once listener is bound", new Throwable());
		this.graphitePort= graphitePort;
	}



	/**
	 * Returns the graphite port 
	 * @return the graphite port 
	 */
	@ManagedAttribute
	public int getGraphitePort() {
		return graphitePort;
	}


	/**
	 * Returns the channel handler bean names in the order they are bound into the pipelines created for this listener
	 * @return an array of channel handler bean names
	 */
	@ManagedAttribute
	public String[] getChannelHandlerNames() {
		List<String> names = new ArrayList<String>();
		if(isStarted()) {
			synchronized(resolvedHandlers) {
				names.addAll(resolvedHandlers.keySet());
			}
		} else {
			names.addAll(channelHandlers.values());
		}
		return names.toArray(new String[names.size()]);
	}

	/**
	 * Sets the channel handlers bound into the pipelines created for this listener
	 * @param channelHandlers A map of channel handler bean names keyed by the ordering int of the handlers in the pipeline
	 */
	public void setChannelHandlers(Map<Integer, String> channelHandlers) {
		if(channelHandlers!=null) {
			synchronized(this.channelHandlers) {
				this.channelHandlers.clear();
				this.channelHandlers.putAll(channelHandlers);				
			}
		}
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
	 * Indicates if a logging handler is installed in the pipeline
	 * @return true if a logging handler is installed, false otherwise
	 */
	@ManagedAttribute
	public boolean isLoggingHandlerInstalled() {
		return loggingHandlerInstalled.get();
	}
	
	/**
	 * Returns the logging handler location
	 * @return the logging handler location
	 */
	@ManagedAttribute
	public String getLoggingHandlerLocation() {
		return loggingHandlerLocation;
	}
	
	/**
	 * Adds a logging handler to the pipeline map if not already present and if the listener is started.
	 * @param after The name of the handler after which the logger should be added. Adds first in the pipeline if this name is null or empty.
	 * @param level The level at which the logging handler should log, based on the names in {@link InternalLogLevel}.
	 * @param hex true if and only if the hex dump of the received message is logged
	 */
	@ManagedOperation
	public void addLoggingHandler(String after, String level, boolean hex) {
		if(loggingHandlerInstalled.get()) return;
		if(!isStarted()) throw new IllegalStateException("This operation can only be executed once the destination is started", new Throwable()); 
		if(after==null || after.trim().isEmpty()) {
			after = "first";
		} else {
			if(!resolvedHandlers.containsKey(after)) throw new IllegalArgumentException("Invalid handler location [" + after + "]", new Throwable());
		}
		InternalLogLevel logLevel = InternalLogLevel.valueOf(level.trim().toUpperCase());
		String handlerName = getClass().getSimpleName() + "." + beanName + "Logger";
		LoggingHandler loggingHandler = new LoggingHandler(handlerName, logLevel, hex);
		synchronized(resolvedHandlers) {
			LinkedHashMap<String, ChannelHandler> tmp = new LinkedHashMap<String, ChannelHandler>(resolvedHandlers);
			resolvedHandlers.clear();
			if(after.equals("first")) {
				resolvedHandlers.put(handlerName, loggingHandler);
				resolvedHandlers.putAll(tmp);			
			} else {
				for(Map.Entry<String, ChannelHandler> entry: tmp.entrySet()) {
					String loc = entry.getKey();
					resolvedHandlers.put(loc, entry.getValue());
					if(after.equals(loc)) {
						resolvedHandlers.put(handlerName, loggingHandler);
					}
				}
			}		
			loggingHandlerInstalled.set(true);
		}
	}
	
	/**
	 * Removes the logging handler from the pipeline if it is installed and the listener is started 
	 */
	@ManagedOperation
	public void removeLoggingHandler() {
		if(!loggingHandlerInstalled.get()) return;
		if(!isStarted()) throw new IllegalStateException("This operation can only be executed once the destination is started", new Throwable());
		String handlerName = getClass().getSimpleName() + "." + beanName + "Logger";
		synchronized(resolvedHandlers) {
			resolvedHandlers.remove(handlerName);
			loggingHandlerInstalled.set(false);
		}
	}
	
	/**
	 * Returns the total number of channels created
	 * @return the total number of channels created
	 */
	@ManagedAttribute
	public long getChannelsCreated() {
		return getMetricValue("ChannelsCreated");
	}
	
	/**
	 * Returns the total number of channels closed
	 * @return the total number of channels closed
	 */
	@ManagedAttribute	
	public long getChannelsClosed() {
		return getMetricValue("ChannelsClosed");
	}
	
	/**
	 * Returns the number of channels currently open
	 * @return the number of channels currently open
	 */
	@ManagedAttribute
	public int getCurrentChannels() {
		return channelGroup.size();
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
		_metrics.add("LastMetricsForwarded");
		_metrics.add("MetricsDropped");		
		_metrics.add("MetricsForwardFailures");
		return _metrics;
	}

	
	
	/**
	 * Creates a new GraphiteDestination
	 * @param patterns The {@link IMetric} pattern this destination accepts
	 */
	public GraphiteDestination(String... patterns) {
		super(patterns);
	}

	/**
	 * Creates a new GraphiteDestination
	 * @param patterns The {@link IMetric} pattern this destination accepts
	 */
	public GraphiteDestination(Collection<String> patterns) {
		super(patterns);
	}

	/**
	 * Creates a new GraphiteDestination
	 */
	public GraphiteDestination() {
		
	}

	/**
	 * Returns the frequency of reconnect attempts in ms.
	 * @return the frequency of reconnect attempts 
	 */
	@ManagedAttribute
	public long getReconnectPeriod() {
		return reconnectPeriod;
	}

	/**
	 * Sets the frequency of reconnect attempts in ms
	 * @param reconnectPeriod the frequency of reconnect attempts
	 */
	@ManagedAttribute
	public void setReconnectPeriod(long reconnectPeriod) {
		this.reconnectPeriod = reconnectPeriod;
	}

	/**
	 * Indicates if the client is currently in a reconnect loop
	 * @return true if the client is currently in a reconnect loop, false otherwise
	 */
	public boolean getReconnecting() {
		return reconnecting.get();
	}

	/**
	 * Returns the time based flush trigger in ms.
	 * @return the time based flush trigger
	 */
	@ManagedAttribute
	public long getTimeTrigger() {
		return timeTrigger;
	}

	/**
	 * Sets the time based flush trigger
	 * @param timeTrigger the frequency that the buffer is flushed in ms.
	 */
	@ManagedAttribute
	public void setTimeTrigger(long timeTrigger) {
		this.timeTrigger = timeTrigger;
	}

	/**
	 * Returns the size based flush trigger
	 * @return the size based flush trigger
	 */
	@ManagedAttribute
	public long getSizeTrigger() {
		return sizeTrigger;
	}

	/**
	 * Sets the size based flush trigger
	 * @param sizeTrigger the number of metrics to accumulate before they are flushed
	 */
	@ManagedAttribute
	public void setSizeTrigger(long sizeTrigger) {
		this.sizeTrigger = sizeTrigger;
	}
	
	/**
	 * Injects the scheduler
	 * @param scheduler the platform scheduler
	 */
	@Autowired(required=true)
	public void setScheduler(ThreadPoolTaskScheduler scheduler) {
		this.scheduler = scheduler;
	}


}
