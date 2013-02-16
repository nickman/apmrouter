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
package org.helios.apmrouter.server.unification.pipeline.http.proxy;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.helios.apmrouter.server.ServerComponentBean;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;
import org.jboss.netty.logging.InternalLogLevel;
import org.springframework.jmx.export.annotation.ManagedAttribute;

/**
 * <p>Title: ProxyChannelFactory</p>
 * <p>Description: Spins up a netty channel and pipeline factory for HTTP proxy clients</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.unification.pipeline.http.ProxyChannelFactory</code></p>
 */

public class ProxyChannelFactory extends ServerComponentBean implements ChannelPipelineFactory {
	/** The netty channel factory's boss thread pool */
	protected ExecutorService bossPool = null;
	/** The netty channel factory's worker thread pool */
	protected ExecutorService workerPool = null;
	/** The netty client bootstrap */
	protected ClientBootstrap bstrap = null;
	
	/** The channel options */
	protected final Map<String, Object> channelOptions = new ConcurrentHashMap<String, Object>();	
	/** The netty channel factory */
	protected NioClientSocketChannelFactory channelFactory = null;
	/** The pipeline logging handler */
	protected final LoggingHandler loggingHandler = new LoggingHandler(getClass(), InternalLogLevel.ERROR, true);
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStart()
	 */
	@Override
	protected void doStart() throws Exception {
		super.doStart();
		channelFactory = new NioClientSocketChannelFactory(bossPool, workerPool);	
		bstrap = new ClientBootstrap(channelFactory);		
		bstrap.setOptions(channelOptions);
		bstrap.setPipelineFactory(this);
	}
	
	/**
	 * Returns an un-connected channel.
	 * @return an un-connected channel.
	 */
	public Channel newChannel() {
		return channelFactory.newChannel(getPipeline());
	}
	
	/**
	 * Issues a request for a connection to the passed host and port, and returns a future for the connect op.
	 * @param host The host to connect to
	 * @param port The port to connect to
	 * @return A future for the connection request
	 */
	public ChannelFuture newChannelAsynch(String host, int port) {
		return bstrap.connect(new InetSocketAddress(host, port));
	}
	
	/**
	 * Issues a request for a connection to the passed host and port, and returns the connected channel
	 * @param host The host to connect to
	 * @param port The port to connect to
	 * @return A connected channel
	 */
	public Channel newChannel(String host, int port) {
		return bstrap.connect(new InetSocketAddress(host, port)).awaitUninterruptibly().getChannel();
	}
	
	
	
	/**
	 * Sets the worker pool for this listener
	 * @param workerPool the workerPool to set
	 */
	public void setWorkerPool(ExecutorService workerPool) {
		this.workerPool = workerPool;
	}
	
	/**
	 * Sets the boss pool for this listener
	 * @param bossPool the bossPool to set
	 */
	public void setBossPool(ExecutorService bossPool) {
		this.bossPool = bossPool;
	}
	
	/**
	 * Adds the passed map of channel options to the listener's channel options
	 * @param options A map of channel options
	 */
	public void setChannelOptions(Map<String, Object> options) {
		if(options!=null) {
			channelOptions.putAll(options);
		}
	}

	/**
	 * Returns the channel options in string format
	 * @return the channel options in string format
	 */
	@ManagedAttribute(description="The channel options")
	public Map<String, String> getChannelOptionNames() {
		Map<String, String> map = new HashMap<String, String>(channelOptions.size());
		for(Map.Entry<String, Object> entry: channelOptions.entrySet()) {
			map.put(entry.getKey(), entry.getValue().toString());
		}
		return map;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelPipelineFactory#getPipeline()
	 */
	@Override
	public ChannelPipeline getPipeline()  {
		ChannelPipeline pipeline = Channels.pipeline();
		pipeline.addLast("encoder", new HttpRequestEncoder());
		pipeline.addLast("aggregator", new HttpChunkAggregator(65536));
		pipeline.addLast("decoder", new HttpResponseDecoder());       
		pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
		if(this.log.isDebugEnabled()) {
			pipeline.addFirst("logger", loggingHandler);
		}
		return pipeline;
	}	

}
