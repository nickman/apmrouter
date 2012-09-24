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
package org.helios.apmrouter.server.net.listener.netty;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.helios.apmrouter.server.ServerComponentBean;
import org.helios.apmrouter.server.net.listener.netty.group.ManagedChannelGroup;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLogLevel;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Log4JLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;

/**
 * <p>Title: BaseAgentListener</p>
 * <p>Description: Base class for agent listeners that listen for agent connections, disconnections and requests</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.net.listener.netty.BaseAgentListener</code></p>
 * TODO: Migrate to generic Netty class structure
 */

public class BaseAgentListener extends ServerComponentBean implements ChannelPipelineFactory {
	/** The netty channel factory's worker thread pool */
	protected ExecutorService workerPool = null;
	/** The managed channel group */
	protected final ManagedChannelGroup channelGroup = ManagedChannelGroup.getInstance("APMRouterChannelGroup");
	/** The interface that this listener will bind to */
	protected String bindHost = null;
	/** The port that this listener will bind to */
	protected int bindPort = -1;
//	/** The netty bootstrap */
//	protected Bootstrap bootstrap = null;
//	/** The netty channel factory */
//	protected ChannelFactory channelFactory = null;
	/** The channel pipeline initial handler bean names */
	protected final SortedMap<Integer, String> channelHandlers = new ConcurrentSkipListMap<Integer, String>();	
	/** The channel options */
	protected final Map<String, Object> channelOptions = new ConcurrentHashMap<String, Object>();
	/** The socket address this listener is bound to */
	protected InetSocketAddress socketAddress = null;
	/** The resolved channel handlers in insertion order */
	protected LinkedHashMap<String, ChannelHandler> resolvedHandlers = new LinkedHashMap<String, ChannelHandler>();
	/** Indicates if a logging handler is installed in the pipelines created for this listener */
	protected final AtomicBoolean loggingHandlerInstalled = new AtomicBoolean(false);
	/** The relative location of the logging handler */
	protected String loggingHandlerLocation = null;
	/** The channel close future */
	protected ChannelFuture closeFuture = null;
	/** Indicates if the main channel is open */
	protected final AtomicBoolean connected = new AtomicBoolean(false);
	
	
	
	/**
	 * Creates a new BaseAgentListener
	 */
	protected BaseAgentListener() {
		InternalLoggerFactory.setDefaultFactory(new Log4JLoggerFactory());	
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
		try {
			for(Map.Entry<Integer, String> entry: channelHandlers.entrySet()) {
				ChannelHandler handler = applicationContext.getBean(entry.getValue(), ChannelHandler.class);
				if(handler instanceof ChannelGroupAware) {
					((ChannelGroupAware)handler).setChannelGroup(channelGroup);
				}
				debug("Resolved Channel Handler [", entry.getValue(), "]");
				resolvedHandlers.put(beanName, handler);
			}
			info("Resolved [", resolvedHandlers.size(), "] Channel Handlers");
			socketAddress = new InetSocketAddress(bindHost, bindPort);
			info("Socket Address:", socketAddress);			
		} catch (Exception e) {
			error("Failed to resolve channel handlers", e);
			throw e;
		}
		
	}
	
	/**
	 * Stops this listener
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStop()
	 */
	@Override
	protected void doStop() {
		for(ChannelHandler handler: resolvedHandlers.values()) {
			if(handler instanceof ExecutionHandler) {
				info("Stopping Execution Handler....");
				((ExecutionHandler)handler).releaseExternalResources();
			}
		}
		resolvedHandlers.clear();
		socketAddress = null;		
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
	 * Returns the channel group for this agent listener
	 * @return the agent listener channel group
	 */
	//@ManagedAttribute
	public ChannelGroup getChannelGroup() {
		return channelGroup;
	}

	/**
	 * Returns the interface this listener is bound to 
	 * @return the interface this listener is bound to
	 */
	@ManagedAttribute
	public String getBindHost() {
		return bindHost;
	}

	/**
	 * Sets the interface this listener is bound to 
	 * @param bindHost the bindHost to set
	 */
	@ManagedAttribute
	public void setBindHost(String bindHost) {
		if(isStarted()) throw new IllegalStateException("Cannot set the bind host once listener is bound", new Throwable());
		this.bindHost = bindHost;
	}




	/**
	 * Returns the port this listener is bound to 
	 * @return the port this listener is bound to 
	 */
	@ManagedAttribute
	public int getBindPort() {
		return bindPort;
	}




	/**
	 * Sets the port this listener will bind to 
	 * @param bindPort the port this listener will bind to 
	 */
	@ManagedAttribute
	public void setBindPort(int bindPort) {
		if(isStarted()) throw new IllegalStateException("Cannot set the bind port once listener is bound", new Throwable());
		this.bindPort = bindPort;
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
	 * Sets the worker pool for this listener
	 * @param workerPool the workerPool to set
	 */
	public void setWorkerPool(ExecutorService workerPool) {
		this.workerPool = workerPool;
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
	 * Indicates if the main channel is connected
	 * @return true if the main channel is connected, false otherwise
	 */
	@ManagedAttribute
	public boolean isConnected() {
		return connected.get();
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
		if(!isStarted()) throw new IllegalStateException("This operation can only be executed once the listener is started", new Throwable()); 
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
		if(!isStarted()) throw new IllegalStateException("This operation can only be executed once the listener is started", new Throwable());
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
		return getMetricValue("channelsCreated");
	}
	
	/**
	 * Returns the total number of channels closed
	 * @return the total number of channels closed
	 */
	@ManagedAttribute	
	public long getChannelsClosed() {
		return getMetricValue("channelsClosed");
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
		Set<String> metrics = new HashSet<String>(super.getSupportedMetricNames());
		metrics.add("channelsCreated");
		metrics.add("channelsClosed");
		return metrics;
	}
//	/**
//	 * Sets the channel group
//	 * @param channelGroup the channelGroup to set
//	 */
//	@Autowired(required=true)
//	public void setChannelGroup(ManagedChannelGroup channelGroup) {
//		this.channelGroup = channelGroup;
//	}

	

		
	

}
