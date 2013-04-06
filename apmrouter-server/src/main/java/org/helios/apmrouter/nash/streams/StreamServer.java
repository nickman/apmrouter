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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.helios.apmrouter.nash.streams.StreamServer;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.local.DefaultLocalClientChannelFactory;
import org.jboss.netty.channel.local.LocalAddress;
import org.jboss.netty.channel.local.LocalClientChannelFactory;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.jboss.netty.util.CharsetUtil;

/**
 * <p>Title: StreamServer</p>
 * <p>Description: Local netty server that bridges streams from the ng client to handlers interested in processing STDIN from the client and sending STDOUT, STDERR and exit codes to the ng client.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.nash.streams.StreamServer</code></p>
 */
public class StreamServer implements ChannelPipelineFactory, ChannelFactory {
	/** The local address of the StreamServer */
	public static final String LOCAL_STREAMS_ADDRESS = "NGStreamServer";
	/** The singleton instance */
	protected static volatile StreamServer instance = null;
	/** The singleton instance ctor lock */
	protected static final Object lock = new Object();
	
	/** The address impl for the StreamServer */
	protected final LocalAddress localAddress = new LocalAddress(LOCAL_STREAMS_ADDRESS);
	/** The server bootstrap */
	protected final ServerBootstrap serverBootstrap =  new ServerBootstrap(this);
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
	/** The execution handler to handle the incoming byte streams */
	/*
	 * Need to specify some configurability here.
	 */
	protected final ExecutionHandler executionHandler = new ExecutionHandler(new OrderedMemoryAwareThreadPoolExecutor(10, 0, 0, 60, TimeUnit.SECONDS, threadFactory));
	/** The default channel pipeline factory instance */
	protected final ChannelPipelineFactory DEFAULT_PIPELINE_FACTORY = new DefaultPipelineFactory();
	/** The local channel factory */
	protected final LocalClientChannelFactory channelFactory = new DefaultLocalClientChannelFactory();
	/**
	 * Acquires the stream server singleton instance
	 * @return the stream server singleton instance
	 */
	public static StreamServer getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new StreamServer();
				}
			}
		} 
		return instance;
	}
	
	/**
	 * Creates a new StreamServer
	 */
	private StreamServer() {
				
	}
	
	
	/**
	 * <p>Returns the default pipeline. See {@link DefaultPipelineFactory}.</p>
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelPipelineFactory#getPipeline()
	 */
	@Override
	public ChannelPipeline getPipeline() throws Exception {		
		return DEFAULT_PIPELINE_FACTORY.getPipeline();
	}	
	
	/**
	 * Creates a new pipeline using the passed handlers to populate as well as the execution handler
	 * @param handlers An array of handlers 
	 * @return a new pipeline
	 * @throws Exception
	 */
	public ChannelPipeline getPipeline(ChannelHandler...handlers) throws Exception {
		if(handlers==null || handlers.length<1) {
			return DEFAULT_PIPELINE_FACTORY.getPipeline();
		}
		ChannelPipeline pipeline = Channels.pipeline(handlers);
		pipeline.addFirst("executionHandler", executionHandler);
		return pipeline;
	}	
	
	/**
	 * Returns an empty pipeline excepting the execution handler. If unmodified, any added endpoint will simply receive ChannelBuffers.
	 * @return an empty pipeline
	 */
	public ChannelPipeline getEmptyPipeline() {
		ChannelPipeline pipeline = Channels.pipeline();
		pipeline.addLast("executionHandler", executionHandler);
		return pipeline;
	}
	

	/**
	 * <p>Creates a new channel with the default pipeline.
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelFactory#newChannel(org.jboss.netty.channel.ChannelPipeline)
	 */
	public Channel newChannel(ChannelPipeline pipeline) {
		try {
			return channelFactory.newChannel(getPipeline());
		} catch (Exception e) {
			throw new RuntimeException("Failed to create new channel", e);
		}
	}
	
	/**
	 * Creates a new channel with a pipeeline implementing the passed map of channel handlers
	 * @param handlers a map of channel handlers that will be added to the pipeline
	 * @return a channel connected to the stream handler specified pipeline
	 */
	public Channel newChannel(LinkedHashMap<String, ChannelHandler> handlers) {
		ChannelPipeline pipeline = Channels.pipeline();
		pipeline.addLast("executionHandler", executionHandler);
		for(Map.Entry<String, ChannelHandler> entry: handlers.entrySet()) {
			pipeline.addLast(entry.getKey(), entry.getValue());
		}
		return channelFactory.newChannel(pipeline);
	}
	
	/**
	 * Creates a new channel with a pipeeline implementing the passed channel handlers
	 * @param handlers an array of channel handlers to add to the pipeline
	 * @return  a channel connected to the stream handler specified pipeline
	 */
	public Channel newChannel(ChannelHandler...handlers) {
		ChannelPipeline pipeline = Channels.pipeline();
		pipeline.addLast("executionHandler", executionHandler);
		int id = 1;
		if(handlers!=null) {
			for(ChannelHandler handler: handlers) {
				if(handler!=null) {
					pipeline.addFirst("handler#" + id, handler);
					id++;
				}
			}
		}
		return channelFactory.newChannel(pipeline);
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelFactory#releaseExternalResources()
	 */
	@Override
	public void releaseExternalResources() {
		channelFactory.releaseExternalResources();
		// stop executor
		
	}
	
	
	/**
	 * <p>Title: DefaultPipelineFactory</p>
	 * <p>Description: The default pipeline factory. The default pipeline contains:<ol>
	 * <li>The execution handler so that streaming is handed off to a new thread</li>
	 * <li>A frame decoder with a 1024 line length maximum and an EOL line delimeter</li>
	 * <li>A string decoder for the UTF-8 character set</li>
	 * </ol></p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.nash.streams.StreamServer.DefaultPipelineFactory</code></p>
	 */
	public class DefaultPipelineFactory implements ChannelPipelineFactory {

		@Override
		public ChannelPipeline getPipeline() throws Exception {
			ChannelPipeline pipeline = Channels.pipeline();
			pipeline.addLast("executionHandler", executionHandler);
			pipeline.addLast("frameDecoder", new DelimiterBasedFrameDecoder(1024, Delimiters.lineDelimiter()));
			pipeline.addLast("stringDecoder", new StringDecoder(CharsetUtil.UTF_8));		
			return pipeline;
		}
		
	}
	
}
