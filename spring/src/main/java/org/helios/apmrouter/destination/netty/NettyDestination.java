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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.helios.apmrouter.destination.BaseDestination;
import org.jboss.netty.bootstrap.Bootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
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
