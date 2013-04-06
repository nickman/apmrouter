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
package org.helios.apmrouter.nash.streams;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.helios.apmrouter.nash.handler.NashRequestHandler;
import org.helios.apmrouter.nash.handler.NashStreamHandler;
import org.helios.apmrouter.nash.streams.NashRequestChannelFactory;
import org.helios.apmrouter.nash.streams.NashRequestHandlerStreamServer;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.jboss.netty.logging.InternalLogger;
import org.jboss.netty.logging.InternalLoggerFactory;

/**
 * <p>Title: NashRequestHandlerStreamServer</p>
 * <p>Description: Creates local Netty server endpoints identified by and listening on virtual sockets named after the
 * name of the nailgun command that the owning request handler iss servicing.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.nash.streams.NashRequestHandlerStreamServer</code></p>
 */

public class NashRequestHandlerStreamServer {
	/** The singleton instance */
	private static volatile NashRequestHandlerStreamServer instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	/** The internal logger */
	protected final InternalLogger log = InternalLoggerFactory.getInstance(getClass());	
	/** A map of server bootstraps keyed by the command name that owning request handler is handling */
	protected final Map<String, ServerBootstrap> commandServers = new ConcurrentHashMap<String, ServerBootstrap>();
	/** The thread factory providing threads to the execution handler */
	protected final ThreadFactory threadFactory = new ThreadFactory(){
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "StreamServerThread#" + serial.incrementAndGet());
			t.setDaemon(true);
			return t;
		}
	};			
	/** A serial number factory for the executor threads */
	protected final AtomicLong serial = new AtomicLong(0L);
	/*
	 * Need to specify some configurability here.
	 */
	/** The request handling thread pool */
	protected final OrderedMemoryAwareThreadPoolExecutor threadPool;
	/** The execution handler to handle all stream actions passed to the request handler */
	protected final ExecutionHandler executionHandler;
	

	/**
	 * Returns the singleton NashRequestHandlerStreamServer instance
	 * @return the singleton NashRequestHandlerStreamServer instance
	 */
	public static NashRequestHandlerStreamServer getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new NashRequestHandlerStreamServer();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Creates a new NashRequestHandlerStreamServer
	 */
	private NashRequestHandlerStreamServer() {
		threadPool = new OrderedMemoryAwareThreadPoolExecutor(10, 0, 0, 60, TimeUnit.SECONDS, threadFactory);
		executionHandler = new ExecutionHandler(threadPool);
	}
	
	/**
	 * Creates a new StreamServer through which the NashRequestDecoder can locate the correct handler for a given command,
	 * hand off the decoded request and initiate a stream processor to route the incoming STDIN stream from the nailgun client.
	 * @param requestHandler The request handler to which decoded nailgun requests will be dispatched to
	 * into a form that the request handler wants to process it. If this map is null, the request handler is assumed to not support processing the STDIN stream.
	 */
	/*
	 * NOTE: Right now, NailgunRequestHandlers MUST implement NashStreamHandler
	 * but we want to separate the two, so that if NashStreamHandler is NOT implemented,
	 * the the request handler will not handle STDIN. The request decoder must be aware whether or not
	 * the request handler supports STDIN so it knows to start the streaming (or not).
	 */
	
	public void newCommandHandlerStreamServer(NashRequestHandler requestHandler) {
		Map<String, ChannelHandler> channelHandlers = ((NashStreamHandler<?>)requestHandler).getChannelHandlers();
		String commandName = requestHandler.getCommandName().trim().toUpperCase();
		ServerBootstrap sb = commandServers.get(commandName);
		if(sb==null) {
			synchronized(commandServers) {
				ChannelPipeline pipeline = Channels.pipeline();
				if(channelHandlers!=null) {
					for(Map.Entry<String, ChannelHandler> entry: channelHandlers.entrySet()) {
						pipeline.addLast(entry.getKey(), entry.getValue());						
					}					
				}
				pipeline.addFirst("execution", executionHandler);
				ServerBootstrap sboot = new ServerBootstrap(new NashRequestChannelFactory(Channels.pipelineFactory(pipeline)));  
				commandServers.put(commandName, sboot);
				log.info("\n\t============================\n\tRegistered Nailgun Command Handler for [" + commandName + "]\n\t============================\n");
			}
		} else {
			throw new IllegalStateException("A command server for the command [" + commandName + "] is already registered", new Throwable());
		}
	}
	
	
	
}
















