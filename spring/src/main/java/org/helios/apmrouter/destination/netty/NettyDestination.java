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
package org.helios.apmrouter.destination.netty;

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
import java.util.concurrent.atomic.AtomicInteger;

import org.helios.apmrouter.destination.BaseDestination;
import org.helios.apmrouter.metric.IMetric;
import org.jboss.netty.bootstrap.Bootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLogLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.jmx.support.MetricType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * <p>Title: NettyDestination</p>
 * <p>Description: Base netty destination</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.NettyDestination</code></p>
 */

public abstract class NettyDestination extends BaseDestination implements ChannelPipelineFactory {
	/** The nety worker pool */
	protected ExecutorService workerPool;
	/** The client bootstrap */
	protected Bootstrap bstrap;
	/** The client channel factory */
	protected ChannelFactory channelFactory;
	/** The channel options */
	protected Map<String, Object> channelOptions = new HashMap<String, Object>();
	/** The endpoint server host name or IP address */
	protected String host = null;
	/** The port that the endpoint server is listening on */
	protected int port = -1;
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
	/** The number of consecutive failed channel initialization attempts */
	protected final AtomicInteger failedConnects = new AtomicInteger(0);

	/** The injected task scheduler */
	protected ThreadPoolTaskScheduler scheduler = null;
	/** The reconnect loop scheduling handle */
	protected ScheduledFuture<?> reconnectScheduleHandle = null;
	

	/**
	 * Creates a new NettyDestination
	 * @param patterns the patterns this destination accepts
	 */
	public NettyDestination(String... patterns) {
		super(patterns);
	}

	/**
	 * Creates a new NettyDestination
	 * @param patterns the patterns this destination accepts
	 */
	public NettyDestination(Collection<String> patterns) {
		super(patterns);
	}

	/**
	 * Creates a new NettyDestination
	 */
	public NettyDestination() {
	}

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
		socketAddress = new InetSocketAddress(host, port);
		info("Socket Address:", socketAddress);
		channelGroup = new DefaultChannelGroup(beanName);
		channelFactory = buildChannelFactory();
		bstrap = buildBootstrap();
		bstrap.setOptions(channelOptions);
		bstrap.setPipelineFactory(this);
		doConnect();		
	}
	
	/**
	 * <p>Initiates an asynch connect to the graphite server.
	 * <p>When successful, the following are automatically done by this base class:<ul>
	 *  <li>Sets the {@link #channel} to the newly initialized channel</li>
	 *  <li>Sets the {@link #closeFuture} to the newly initialized channel's close future</li>
	 * 	<li>The {@link #connected} flag is set to true</li> 
	 * 	<li>The {@link #closeFuture} is set for the connected channel</li>
	 *  <li>Resets the {@link #failedConnects} counter to zero</li>
	 * 	<li>If the {@link #reconnectScheduleHandle} is not null, meaning a reconnect loop was in progress, it will be cleared
	 * and the {@link #reconnecting} flag is set to false.</li>
	 * </ul></p>
	 * <p>When fails, the following are automatically done by this base class:<ul>
	 * 	<li>Starts the reconnect loop using {@link #startReconnectLoop()} which will populate the {@link #reconnectScheduleHandle} field.</li>
	 *  <li>Sets the {@link #reconnecting} flag to true</li>
	 *  <li>Increments the {@link #failedConnects} counter</li>
	 *  <li>Sets the {@link #channel} to null</li>
	 *  <li>Sets the {@link #closeFuture} to null</li>
	 * </il></p>
	 */
	protected void doConnect() {
		if(connected.get()) return;
		initializeChannel().addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture f) throws Exception {
				if(f.isSuccess()) {
					channel = f.getChannel();
					connected.set(true);
					closeFuture = f.getChannel().getCloseFuture();
					failedConnects.set(0);
					if(reconnectScheduleHandle!=null) {
						reconnectScheduleHandle.cancel(true);
						reconnectScheduleHandle = null;
						reconnecting.set(false);
					}
					onConnect(f);
				} else {
					if(onConnectFailed(f)) {
						startReconnectLoop();
					}
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
			channel = null;
			closeFuture = null;
			error("Unexpected disconnect from [", socketAddress , "]");
			startReconnectLoop();
		} else {
			info("Disconnected from [", socketAddress , "]");
		}
	}
	
	/**
	 * Schdules a repeating reconnect task at a rate of {@link #reconnectPeriod}.
	 * Schedule will be cancelled when this component is stopped or a successful connect occurs. 
	 */
	protected void startReconnectLoop() {
		if(!reconnecting.compareAndSet(false, true)) return; 
		reconnectScheduleHandle = scheduler.scheduleAtFixedRate(new Runnable(){
			@Override
			public void run() {
				doConnect();
			}
		}, reconnectPeriod);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelPipelineFactory#getPipeline()
	 */
	@Override
	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline pipeline = Channels.pipeline();
		LinkedHashMap<String, ChannelHandler> tmpHandlers = null; 
		synchronized(resolvedHandlers) {
			tmpHandlers = new LinkedHashMap<String, ChannelHandler>(resolvedHandlers);
		}
		for(Map.Entry<String, ChannelHandler> entry: tmpHandlers.entrySet()) {
			pipeline.addLast(entry.getKey(), entry.getValue());
		}
		return pipeline;
	}
	
	
	
	/**
	 * Callback from the main channel initialization when connect succeeds
	 * @param connectFuture The channel future returned from a successful channel init.
	 */
	protected void onConnect(ChannelFuture connectFuture) {
		
	}

	/**
	 * Callback from the main channel initialization when connect succeeds
	 * @param failedConnectFuture The channel future returned from a failed channel init.
	 * @return true to start a reconnect loop, false to do nothing
	 */
	protected boolean onConnectFailed(ChannelFuture failedConnectFuture) {
		return true;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStop()
	 */
	@Override
	protected void doStop() {	
		if(reconnectScheduleHandle!=null) {
			reconnectScheduleHandle.cancel(true);
			reconnectScheduleHandle = null;
		}
		channelGroup.close().awaitUninterruptibly();
		channelFactory.releaseExternalResources();
		channelFactory = null;
		resolvedHandlers.clear();
		socketAddress = null;
		channelGroup = null;
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
	@Override
	protected void doAcceptRoute(IMetric routable) {
		try {
			final long start = System.currentTimeMillis();
			channel.write(routable).addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture f) throws Exception {
					if(f.isSuccess()) {
						incr("MetricsForwarded");
					} else {
						incr("MetricForwardFailures");
					}
				}
			});
		} catch (Exception e) {
			incr("MetricsForwardFailures");
		}
	}
	
	/**
	 * Returns the endpoint server name or IP address
	 * @return the endpoint server name or IP address
	 */
	@ManagedAttribute(description="The endpoint server name or IP address")
	public String getHost() {
		return host;
	}
	
	/**
	 * Indicates if the channel is connected (or ready)
	 * @return true if the channel is connected, false otherwise
	 */
	@ManagedAttribute(description="Indicates if the channel is connected (or ready)")
	public boolean isConnected() {
		return connected.get();
	}

	/**
	 * Sets the endpoint server name or IP address
	 * @param host the endpoint server name or IP address
	 */
	@ManagedAttribute(description="The endpoint server name or IP address")
	public void setHost(String host) {
		if(isStarted()) throw new IllegalStateException("Cannot set the host once the channel is started", new Throwable());
		this.host = host;
	}

	/**
	 * Sets the endpoint server listening port 
	 * @param port the endpoint server listening port 
	 */
	@ManagedAttribute(description="The endpoint server port")
	public void setPort(int port) {
		if(isStarted()) throw new IllegalStateException("Cannot set the port once the channel is started", new Throwable());
		this.port = port;
	}



	/**
	 * Returns the endpoint server listening port 
	 * @return the endpoint server listening port 
	 */
	@ManagedAttribute(description="The endpoint server name or IP address")
	public int getPort() {
		return port;
	}


	/**
	 * Returns the channel handler bean names in the order they are bound into the pipelines created for this listener
	 * @return an array of channel handler bean names
	 */
	@ManagedAttribute(description="The channel handler bean names")
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
	 * Indicates if a logging handler is installed in the pipeline
	 * @return true if a logging handler is installed, false otherwise
	 */
	@ManagedAttribute(description="Indicates if a logging handler is installed in the pipeline")
	public boolean isLoggingHandlerInstalled() {
		return loggingHandlerInstalled.get();
	}

	/**
	 * Returns the logging handler location
	 * @return the logging handler location
	 */
	@ManagedAttribute(description="The logging handler location")
	public String getLoggingHandlerLocation() {
		return loggingHandlerLocation;
	}
	
	/**
	 * Adds a logging handler to the pipeline map if not already present and if the listener is started.
	 * @param after The name of the handler after which the logger should be added. Adds first in the pipeline if this name is null or empty.
	 * @param level The level at which the logging handler should log, based on the names in {@link InternalLogLevel}.
	 * @param hex true if and only if the hex dump of the received message is logged
	 */
	@ManagedOperation(description="Adds a logging handler to the pipeline map if not already present and if the listener is started")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name="after", description="The name of the handler after which the logger should be added. Adds first in the pipeline if this name is null or empty."),
		@ManagedOperationParameter(name="level", description="The level at which the logging handler should log"),
		@ManagedOperationParameter(name="hex", description="true if and only if the hex dump of the received message is logged")
	})
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
	@ManagedOperation(description="Removes the logging handler from the pipeline if it is installed and the listener is started ")
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
	@ManagedMetric(category="NettyDestinations", description="The total number of channels created", metricType=MetricType.COUNTER)
	public long getChannelsCreated() {
		return getMetricValue("ChannelsCreated");
	}
	
	/**
	 * Returns the total number of channels closed
	 * @return the total number of channels closed
	 */
	@ManagedMetric(category="NettyDestinations", description="The total number of channels closed", metricType=MetricType.COUNTER)
	public long getChannelsClosed() {
		return getMetricValue("ChannelsClosed");
	}

	/**
	 * Returns the number of channels currently open
	 * @return the number of channels currently open
	 */
	@ManagedMetric(category="NettyDestinations", description="The total number of channels currently open", metricType=MetricType.COUNTER)
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
	 * Returns the frequency of reconnect attempts in ms.
	 * @return the frequency of reconnect attempts 
	 */
	@ManagedAttribute(description="The frequency of reconnect attempts in ms")
	public long getReconnectPeriod() {
		return reconnectPeriod;
	}

	/**
	 * Sets the frequency of reconnect attempts in ms
	 * @param reconnectPeriod the frequency of reconnect attempts
	 */
	@ManagedAttribute(description="The frequency of reconnect attempts in ms")
	public void setReconnectPeriod(long reconnectPeriod) {
		this.reconnectPeriod = reconnectPeriod;
	}
	
	/**
	 * Indicates if the client is currently in a reconnect loop
	 * @return true if the client is currently in a reconnect loop, false otherwise
	 */
	@ManagedAttribute(description="Indicates if the client is currently in a reconnect loop")
	public boolean isReconnecting() {
		return reconnecting.get();
	}
	
	/**
	 * Injects the scheduler
	 * @param scheduler the platform scheduler
	 */
	@Autowired(required=true)
	public void setScheduler(ThreadPoolTaskScheduler scheduler) {
		this.scheduler = scheduler;
	}

	

	/**
	 * Initializes the main channel (presumably through the bootstrap)
	 * @return the ChanelFuture for the channel initialization
	 */
	protected abstract ChannelFuture initializeChannel();
	
	/**
	 * Builds the channel factory for this netty client
	 * @return the built channel factory
	 */
	protected abstract ChannelFactory buildChannelFactory();
	
	/**
	 * Builds the bootstrap for this netty client
	 * @return the built bootstrap
	 */
	protected abstract Bootstrap buildBootstrap();
	
}
