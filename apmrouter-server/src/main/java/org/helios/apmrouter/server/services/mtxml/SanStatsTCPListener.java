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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.helios.apmrouter.server.ServerComponentBean;
import org.helios.apmrouter.util.thread.ManagedThreadPool;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferFactory;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.buffer.CompositeChannelBuffer;
import org.jboss.netty.buffer.DirectChannelBufferFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.UpstreamMessageEvent;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.compression.ZlibDecoder;
import org.jboss.netty.handler.codec.compression.ZlibWrapper;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.jboss.netty.util.DefaultObjectSizeEstimator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;

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
	/** The gzip stream sniffer */
	protected final GZipSniffer sniffer = new GZipSniffer();
	
	/** The san stats parser/tracer */
	@Autowired(required=true)
	protected SanStatsParserTracer parserTracer = null;
	/** The listening port. Defaults to 1089 */
	protected int port = 1089;
	/** The server socket receove buffer size. Defaults to 1048576 */
	protected int receiveSocketSize = 1048576;
	
	/** The pooled execution handler's executor */
	protected OrderedMemoryAwareThreadPoolExecutor poolExecutor = null;
	/** The execution handler used to pass the parsing task off to another thread so we don't do that work in an IO worker thread */
	protected ExecutionHandler executionHandler = null;
	/** The core thread count for the pool executor */
	protected int poolExecutorCoreThreads = 16;
	/** The maximum channel memory size for the pool executor */
	protected long poolExecutorMaxChannelMemorySize = 1048576 *2;
	/** The maximum total memory size for the pool executor */
	protected long poolExecutorMaxTotalMemorySize = 1048576 * 10;
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStart()
	 */
	@Override
	protected void doStart() throws Exception {		
		channelFactory = new NioServerSocketChannelFactory(bossPool, workerPool);
		poolExecutor = new OrderedMemoryAwareThreadPoolExecutor(poolExecutorCoreThreads, poolExecutorMaxChannelMemorySize, poolExecutorMaxTotalMemorySize, 60, TimeUnit.SECONDS, new DefaultObjectSizeEstimator(), new ThreadFactory(){
			final AtomicInteger serial = new AtomicInteger(0);
			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r, "SanStatsPoolExecutorThread#" + serial.incrementAndGet());
				t.setDaemon(true);
				return t;
			}
		});
		executionHandler = new ExecutionHandler(poolExecutor);
		bootstrap = new ServerBootstrap(channelFactory);
		bootstrap.setPipelineFactory(this);
		bootstrap.setOption("child.receiveBufferSize", receiveSocketSize);

		
		sock = new InetSocketAddress("0.0.0.0", port);		
		super.doStart();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#onApplicationContextRefresh(org.springframework.context.event.ContextRefreshedEvent)
	 */
	@Override
	public void onApplicationContextRefresh(ContextRefreshedEvent event) {		
		super.onApplicationContextRefresh(event);		
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
				ChannelBuffer b = (ChannelBuffer)message;
//				if(b.order()!=ByteOrder.nativeOrder()) {
//					info("Switching Buff ByteOrder from [", ByteOrder.nativeOrder(), "] to [", b.order(), "]");
//					ChannelBuffer revb = ChannelBuffers.directBuffer(ByteOrder.nativeOrder(), b.readableBytes());
//					revb.writeBytes(b);
//					b = revb;
//				}
				ChannelBuffer cb = (ChannelBuffer)message;
				List<ChannelBuffer> accumulator = (List<ChannelBuffer>)ctx.getAttachment();
				if(accumulator==null) {
					accumulator = new ArrayList<ChannelBuffer>();
					ctx.setAttachment(accumulator);					
					info("Adding close handler to invoke SAN Stats Processing");
					e.getChannel().getCloseFuture().addListener(new ChannelFutureListener() {
						@Override
						public void operationComplete(ChannelFuture future) throws Exception {
							@SuppressWarnings("unchecked")
							List<ChannelBuffer> cbList = (List<ChannelBuffer>)ctx.getAttachment();							 
							if(cbList==null || cbList.isEmpty()) {
								warn("SAN Stats Channel Closed but no buffer found as attachment");
							} else {
//								StringBuilder b = new StringBuilder("Channel Buffer Types: [").append(cbList.size())
//										.append("] NativeType:").append(ByteOrder.nativeOrder());
//								for(ChannelBuffer buff: cbList) {
//									b.append("\n\tCB:[").append(buff.toString()).append("]:Type:").append(buff.order().toString());
//									
//								}
//								info(b);
								ChannelBuffer x = new CompositeChannelBuffer(cbList.get(0).order(), cbList, true);
								info("SAN Stats Channel Closed. Channel Buffers [", cbList.size(), "] Readable [", x.readableBytes(), "] bytes");
								ctx.setAttachment(null);
								Channel closedChannel = future.getChannel();
								ctx.sendUpstream(new UpstreamMessageEvent(closedChannel, x, closedChannel.getLocalAddress()));;
								//parserTracer.process(x);													
							}
						}
					});									
				}
				accumulator.add(cb);
				//info("Added Inremental Buffer. GZip:", isGzip(cb));
			}			
		} else {
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
		pipeline.addLast("encoder", responseEncoder);
		pipeline.addLast("sniffer", sniffer);
		pipeline.addLast("aggregator", this);
		pipeline.addLast("executionHandler", executionHandler);		
		pipeline.addLast("statsHandler", statsParserHandler);
		return pipeline;
	}
	
	/** Downstream message handler to encode responses as simple string byte arrays */
	protected final ChannelDownstreamHandler responseEncoder = new ChannelDownstreamHandler() {
		
		@Override
		public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
			if(e instanceof DownstreamMessageEvent) {
				Channel channel = e.getChannel();
				ctx.sendDownstream(
						new DownstreamMessageEvent(
								channel, 
								Channels.future(channel), 
								ChannelBuffers.wrappedBuffer(((DownstreamMessageEvent)e).getMessage().toString().getBytes()), 
								channel.getRemoteAddress()));
			} else {
				ctx.sendDownstream(e);
			}			
		}
	};
	
	/** The handler that delegates the decoded channel buffer to the parser/tracer */
	protected final OneToOneDecoder statsParserHandler = new OneToOneDecoder() {
		/**
		 * {@inheritDoc}
		 * @see org.jboss.netty.handler.codec.oneone.OneToOneDecoder#decode(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.Channel, java.lang.Object)
		 */
		@Override
		protected Object decode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
			if(msg!=null && ChannelBuffer.class.isInstance(msg)) {
				parserTracer.process((ChannelBuffer)msg);
				channel.write("\n\tRequest Submitted");
				ctx.setAttachment(null);
			}
			return null;
		}
		
	};
	
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
				UpstreamMessageEvent me = (UpstreamMessageEvent)e;
				Object message = me.getMessage();
				if(ChannelBuffer.class.isInstance(message)) {
					ChannelBuffer cb = (ChannelBuffer)message;
					if(isGzip(cb)) {
						ctx.getPipeline().addAfter("aggregator", "gzip", new ZlibDecoder(ZlibWrapper.GZIP) {
							@Override
							protected Object decode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
								incr("ZlibDecoderCalls");
								return super.decode(ctx, channel, msg);
							}
						});					
					} else if(isBzip2(cb)) {
						
						ctx.getPipeline().addAfter("aggregator", "bzip2", new BZip2Decoder()  {
							@Override
							protected Object decode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
								incr("BZip2DecoderCalls");
								return super.decode(ctx, channel, msg);
							}
						});					
					}
					ctx.getPipeline().remove(this);
				}
			}
			ctx.sendUpstream(e);
		}
		
		
		
	}
	
	/**
	 * Determines if the two passed bytes represent a gzipped stream of data
	 * @param magic1 The first byte of the incoming request
	 * @param magic2 The second byte of the incoming request
	 * @return true if the incoming payload is gzipped, false otherwise
	 */
	public static boolean isGzip(int magic1, int magic2) {
		return magic1 == 31 && magic2 == 139;	
	}	
	
	/**
	 * Determines if the three passed bytes represent a stream of data compressed with bzip2
	 * @param magic1 The first byte of the incoming request
	 * @param magic2 The second byte of the incoming request
	 * @param magic3 The second byte of the incoming request
	 * @return true if the incoming payload is compressed with bzip2, false otherwise
	 */
	public static boolean isBzip2(int magic1, int magic2, int magic3) {
		return magic1 == 66 && magic2 == 90 && magic3 == 104;
	}
	
	/**
	 * Determines if the channel is carrying a gzipped payload
	 * @param buffer The channel buffer to check
	 * @return true if the incoming payload is gzipped, false otherwise
	 */
	public static boolean isGzip(ChannelBuffer buffer) {
		if(buffer!=null && buffer.readableBytes()>=5) {
			return isGzip(buffer.getUnsignedByte(0), buffer.getUnsignedByte(1));
		}
		return false;
	}
	
	/**
	 * Determines the block size used in a bzip stream
	 * @param buffer The channel buffer to get the block size from
	 * @return the read block size
	 */
	public static int getBzip2BlockSize(ChannelBuffer buffer) {
		if(buffer!=null && buffer.readableBytes()<7) {
			throw new RuntimeException("Could not read the first 7 bytes");
		}
		return buffer.getUnsignedByte(3);
	}

	/**
	 * Determines if the channel is carrying a payload compressed with bzip2.
	 * @param buffer The channel buffer to check
	 * @return true if the incoming payload is compressed with bzip2, false otherwise
	 */
	public static boolean isBzip2(ChannelBuffer buffer) {
		if(buffer!=null && buffer.readableBytes()>=6) {
			return isBzip2(buffer.getUnsignedByte(0), buffer.getUnsignedByte(1), buffer.getUnsignedByte(2));
		}
		return false;
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

	/**
	 * Returns the core thread count for the pooled execution handler
	 * @return the core thread count for the pooled execution handler
	 */
	@ManagedAttribute(description="The core thread count for the pooled execution handler")
	public int getPoolExecutorCoreThreads() {
		return poolExecutorCoreThreads;
	}

	/**
	 * Sets the core thread count for the pooled execution handler
	 * @param poolExecutorCoreThreads the core thread count for the pooled execution handler
	 */
	public void setPoolExecutorCoreThreads(int poolExecutorCoreThreads) {
		this.poolExecutorCoreThreads = poolExecutorCoreThreads;
	}

	/**
	 * Returns the maximum channel memory size for the pool executor
	 * @return the maximum channel memory size for the pool executor
	 */
	@ManagedAttribute(description="The maximum channel memory size for the pool executor")
	public long getPoolExecutorMaxChannelMemorySize() {
		return poolExecutorMaxChannelMemorySize;
	}

	/**
	 * Sets the maximum channel memory size for the pool executor
	 * @param poolExecutorMaxChannelMemorySize the maximum channel memory size for the pool executor
	 */
	public void setPoolExecutorMaxChannelMemorySize(long poolExecutorMaxChannelMemorySize) {
		this.poolExecutorMaxChannelMemorySize = poolExecutorMaxChannelMemorySize;
	}

	/**
	 * Returns the maximum total memory size for the pool executor
	 * @return the maximum total memory size for the pool executor
	 */
	@ManagedAttribute(description="The maximum total memory size for the pool executor")
	public long getPoolExecutorMaxTotalMemorySize() {
		return poolExecutorMaxTotalMemorySize;
	}

	/**
	 * Sets the maximum total memory size for the pool executor
	 * @param poolExecutorMaxTotalMemorySize the maximum total memory size for the pool executor
	 */
	public void setPoolExecutorMaxTotalMemorySize(long poolExecutorMaxTotalMemorySize) {
		this.poolExecutorMaxTotalMemorySize = poolExecutorMaxTotalMemorySize;
	}

	/**
	 * Indicates if the pooled executor thread pool is shut down
	 * @return true if the pooled executor thread pool is shut down, false otherwise
	 */
	@ManagedAttribute(description="Indicates if the pooled executor thread pool is shut down")
	public boolean isPooledExecutorShutdown() {
		if(poolExecutor==null) return true;
		return poolExecutor.isShutdown();
	}

	/**
	 * Indicates if the pooled executor thread pool is terminating
	 * @return true if the pooled executor thread pool is terminating, false otherwise
	 */
	@ManagedAttribute(description="Indicates if the pooled executor thread pool is terminating")
	public boolean isPooledExecutorTerminating() {
		if(poolExecutor==null) return false;
		return poolExecutor.isTerminating();
	}

	/**
	 * Indicates if the pooled executor thread pool is terminated
	 * @return true if the pooled executor thread pool is terminated, false otherwise
	 */
	@ManagedAttribute(description="Indicates if the pooled executor thread pool is terminated")
	public boolean isPooledExecutorTerminated() {
		if(poolExecutor==null) return true;
		return poolExecutor.isTerminated();
	}

	/**
	 * Returns the pooled executor thread pool's maximum size
	 * @return the pooled executor thread pool's maximum size
	 */
	@ManagedAttribute(description="The pooled executor thread pool's maximum size")
	public int getPooledExecutorMaximumPoolSize() {
		if(poolExecutor==null) return -1;
		return poolExecutor.getMaximumPoolSize();
	}

	/**
	 * Returns the remaining capacity of the pooled executor thread pool's work queue
	 * @return the remaining capacity of the pooled executor thread pool's work queue
	 */
	@ManagedAttribute(description="The remaining capacity of the pooled executor thread pool's work queue")
	public int getPooledExecutorQueueCapacity() {
		return poolExecutor.getQueue().remainingCapacity();
	}
	
	/**
	 * Returns the size of the pooled executor thread pool's work queue
	 * @return the size of the pooled executor thread pool's work queue
	 */
	@ManagedAttribute(description="The size of the pooled executor thread pool's work queue")
	public int getPooledExecutorQueueSize() {
		return poolExecutor.getQueue().size();
	}
	

	/**
	 * Returns the pooled executor thread pool's size
	 * @return the pooled executor thread pool's size
	 */
	@ManagedAttribute(description="The pooled executor thread pool's size")
	public int getPooledExecutorPoolSize() {
		return poolExecutor.getPoolSize();
	}
	
	/**
	 * Returns the cummulative number of calls fielded by the ZlibDecoder
	 * @return the cummulative number of calls fielded by the ZlibDecoder
	 */
	@ManagedMetric(category="SanStatsTCPListener/ZlibDecoder", displayName="ZlibDecoderCalls", metricType=MetricType.COUNTER, description="The cummulative number of calls fielded by the ZlibDecoder")
	public long getZlibDecoderCalls() {
		return getMetricValue("ZlibDecoderCalls");
	}
	
	/**
	 * Returns the cummulative number of calls fielded by the BZip2Decoder
	 * @return the cummulative number of calls fielded by the BZip2Decoder
	 */
	@ManagedMetric(category="SanStatsTCPListener/BZip2Decoder", displayName="BZip2DecoderCalls", metricType=MetricType.COUNTER, description="The cummulative number of calls fielded by the BZip2Decoder")
	public long getBZip2DecoderCalls() {
		return getMetricValue("BZip2DecoderCalls");
	}
	

	/**
	 * Returns the pooled executor thread pool's active count
	 * @return the pooled executor thread pool's active count
	 */
	@ManagedMetric(category="SanStatsTCPListener/PooledExecutor", displayName="CompletedTaskCount", metricType=MetricType.GAUGE, description="The pooled executor thread pool's active count")
	public int getPooledExecutorActiveCount() {
		return poolExecutor.getActiveCount();
	}

	/**
	 * Returns the pooled executor thread pool's completed task count
	 * @return the pooled executor thread pool's completed task count
	 */
	@ManagedMetric(category="SanStatsTCPListener/PooledExecutor", displayName="CompletedTaskCount", metricType=MetricType.COUNTER, description="The pooled executor thread pool's completed task count")
	public long getPooledExecutorCompletedTaskCount() {
		return poolExecutor.getCompletedTaskCount();
	}


	
}
