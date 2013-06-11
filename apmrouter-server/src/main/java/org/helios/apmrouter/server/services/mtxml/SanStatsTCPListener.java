/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2013, Helios Development Group and individual contributors
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
package org.helios.apmrouter.server.services.mtxml;

import java.net.InetSocketAddress;
import java.nio.ByteOrder;
import java.util.concurrent.ExecutorService;

import org.helios.apmrouter.server.ServerComponentBean;
import org.helios.apmrouter.util.thread.ManagedThreadPool;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferFactory;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.buffer.DirectChannelBufferFactory;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.UpstreamMessageEvent;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.compression.ZlibDecoder;
import org.jboss.netty.handler.codec.compression.ZlibWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.jmx.export.annotation.ManagedAttribute;

/**
 * <p>Title: SanStatsTCPListener</p>
 * <p>Description: Netty TCP listener to listen on SAN stats network submissions</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.services.mtxml.SanStatsTCPListener</code></p>
 */

public class SanStatsTCPListener extends ServerComponentBean implements ChannelPipelineFactory, ChannelUpstreamHandler {
	/** The boss thread pool */
	@Autowired(required=true)
	@Qualifier("bossPool")
	protected ManagedThreadPool bossPool = null;
	/** The worker thread pool */
	@Autowired(required=true)
	@Qualifier("workerPool")	
	protected ManagedThreadPool workerPool = null;
	/** The channel factory */
	protected NioServerSocketChannelFactory channelFactory = null;
	/** The server bootstrap */
	protected ServerBootstrap bootstrap = null;
	/** The channel buffer factory */
	protected final ChannelBufferFactory chanelBufferFactory = new DirectChannelBufferFactory(ByteOrder.nativeOrder(), 1500000);
	/** The socket we're listening on */
	protected InetSocketAddress sock = null;
	/** A gzip decoder channel handler */
	protected final ZlibDecoder gunzip = new ZlibDecoder(ZlibWrapper.GZIP);
	/** The gzip stream sniffer */
	protected final GZipSniffer sniffer = new GZipSniffer();
	
	/** The san stats parser/tracer */
	@Autowired(required=true)
	protected SanStatsParserTracer parserTracer = null;
	/** The listening port. Defaults to 1089 */
	protected int port = 1089;
	/** The server socket receove buffer size. Defaults to 1048576 */
	protected int receiveSocketSize = 1048576;
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#onApplicationContextRefresh(org.springframework.context.event.ContextRefreshedEvent)
	 */
	@Override
	public void onApplicationContextRefresh(ContextRefreshedEvent event) {		
		super.onApplicationContextRefresh(event);		
		channelFactory = new NioServerSocketChannelFactory(bossPool, workerPool);
		
		bootstrap = new ServerBootstrap(channelFactory);
		bootstrap.setPipelineFactory(this);
		bootstrap.setOption("child.receiveBufferSize", receiveSocketSize);

		
		sock = new InetSocketAddress("0.0.0.0", port);
		info("Starting SanStatsTCPListener on [", sock, "]");
		bootstrap.bind(sock);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelUpstreamHandler#handleUpstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	@Override
	public void handleUpstream(final ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		if(UpstreamMessageEvent.class.isInstance(e)) {
			UpstreamMessageEvent me = (UpstreamMessageEvent)e;
			Object message = me.getMessage();
			if(ChannelBuffer.class.isInstance(message)) {
				ChannelBuffer cb = (ChannelBuffer)message;
				ChannelBuffer accumulator = (ChannelBuffer)ctx.getAttachment();
				if(accumulator==null) {
					accumulator = ChannelBuffers.dynamicBuffer(chanelBufferFactory);
					ctx.setAttachment(accumulator);
					info("Adding close handler to invoke SAN Stats Processing");
					e.getChannel().getCloseFuture().addListener(new ChannelFutureListener() {
						public void operationComplete(ChannelFuture future) throws Exception {
							ChannelBuffer cb = (ChannelBuffer)ctx.getAttachment();
							if(cb==null) {
								warn("SAN Stats Channel Closed but no buffer found as attachment");
							} else {
								info("SAN Stats Channel Closed. Channel Buffer [", cb, "]");
								ChannelBuffer x = ChannelBuffers.directBuffer(cb.writerIndex());
								x.writeBytes(cb);
								parserTracer.process(x);													
							}
						}
					});									
				}
				accumulator.writeBytes(cb);
			}
			ctx.sendUpstream(e);
		}
		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelPipelineFactory#getPipeline()
	 */
	@Override
	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline pipeline = Channels.pipeline();
		pipeline.addLast("sniffer", sniffer);
		pipeline.addLast("gzip", gunzip);
		pipeline.addLast("statsHandler", this);
		return pipeline;
	}
	
	/**
	 * <p>Title: GZipSniffer</p>
	 * <p>Description: Channel handler to detect if gzip is beinbg used, and if not, remove the gzip decoder from the pipleine</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.server.services.mtxml.SanStatsTCPListener.GZipSniffer</code></p>
	 */
	protected class GZipSniffer implements ChannelUpstreamHandler {

		/**
		 * {@inheritDoc}
		 * @see org.jboss.netty.channel.ChannelUpstreamHandler#handleUpstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
		 */
		@Override
		public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
			if(UpstreamMessageEvent.class.isInstance(e)) {
				info("GZip Sniffer handling UpstreamMessageEvent");
				UpstreamMessageEvent me = (UpstreamMessageEvent)e;
				Object message = me.getMessage();
				if(ChannelBuffer.class.isInstance(message)) {
					ChannelBuffer cb = (ChannelBuffer)message;
					if(cb.readableBytes()>=8) {
						boolean gzip = isGzip(cb.getInt(0), cb.getInt(4));
						info("Incoming SAN stats gzipped:", gzip);
						if(!gzip) {
							ctx.getPipeline().remove("gzip");
						}
						ctx.getPipeline().remove(this);
					}
				}
			}
			ctx.sendUpstream(e);
		}
		
		/**
		 * Determines if the channel is carrying a gzipped metric submssion
		 * @param magic1 The first byte of the incoming request
		 * @param magic2 The second byte of the incoming request
		 * @return true if the incoming payload is gzipped
		 */
		private boolean isGzip(int magic1, int magic2) {
			return magic1 == 31 && magic2 == 139;	
		}	
		
		
	}
	


	/**
	 * Returns the listening port
	 * @return the listening port
	 */
	@ManagedAttribute(description="The listening port")
	public int getPort() {
		return port;
	}


	/**
	 * Sets the listening port
	 * @param port the listening port
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * Returns the server socket receive buffer size
	 * @return the server socket receive buffer size
	 */
	@ManagedAttribute(description="The server socket receive buffer size")
	public int getReceiveSocketSize() {
		return receiveSocketSize;
	}

	/**
	 * Sets the server socket receive buffer size
	 * @param receiveSocketSize the server socket receive buffer size
	 */
	public void setReceiveSocketSize(int receiveSocketSize) {
		this.receiveSocketSize = receiveSocketSize;
	}


	
}
