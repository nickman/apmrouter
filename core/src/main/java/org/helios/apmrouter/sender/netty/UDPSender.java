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
package org.helios.apmrouter.sender.netty;

import static org.helios.apmrouter.util.Methods.nvl;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Iterator;
import java.util.concurrent.Executor;

import org.helios.apmrouter.jmx.ThreadPoolFactory;
import org.helios.apmrouter.sender.AbstractSender;
import org.helios.apmrouter.trace.DirectMetricCollection;
import org.helios.apmrouter.trace.DirectMetricCollection.SplitReader;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.FixedReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: UDPSender</p>
 * <p>Description: A Netty unicast UDP sender implementation</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.sender.netty.UDPSender</code></p>
 */

public class UDPSender extends AbstractSender implements ChannelPipelineFactory {
	/** Static class logger */
	protected static final Logger LOG = LoggerFactory.getLogger(UDPSender.class);	
	/** The netty server worker pool */
	protected final Executor workerPool;
	/** The netty bootstrap */
	protected final ConnectionlessBootstrap bstrap;
	/** The netty channel factory */
	protected final ChannelFactory channelFactory;
	/** The connected channel */
	protected final DatagramChannel channel;
	/** The server socket to send to */
	protected final InetSocketAddress socketAddress;
	/** A discard handler used for discarding self-sent messages */
	protected static final SimpleChannelUpstreamHandler discard = new SimpleChannelUpstreamHandler() {
		@Override
		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
			if(e.getChannel().getLocalAddress().equals(e.getRemoteAddress())) {
				LOG.info("Drop");
			} else {
				super.messageReceived(ctx, e);
			}
		}
	};
	
	/**
	 * Returns a built instance of a UDPSender for the passed URI
	 * @param serverURI The host/port to send to in the form of a URI. e.g. <b><code>udp://myhostname:2094</code></b>.
	 * @return a UDPSender
	 */
	public static UDPSender getInstance(URI serverURI) {
		UDPSender sender = (UDPSender) senders.get(nvl(serverURI, "Server URI"));
		if(sender==null) {
			synchronized(senders) {
				sender = (UDPSender) senders.get(serverURI);
				if(sender==null) {
					sender = new UDPSender(serverURI);
					senders.put(serverURI, sender);
				}
			}
		}
		return sender;
	}
	
	/**
	 * Creates a new UDPSender
	 * @param serverURI The host/port to send to
	 */
	private UDPSender(URI serverURI) {
		super(serverURI);
		workerPool =  ThreadPoolFactory.newCachedThreadPool(getClass().getPackage().getName(), "UDPSenderWorker/" + serverURI.getHost() + "/" + serverURI.getPort());
		channelFactory = new NioDatagramChannelFactory(workerPool);
		bstrap = new ConnectionlessBootstrap(channelFactory);
		bstrap.setPipelineFactory(this);
		bstrap.setOption("receiveBufferSizePredictorFactory", new FixedReceiveBufferSizePredictorFactory(1024));
		channel = (DatagramChannel) bstrap.bind(new InetSocketAddress(0));
		socketAddress = new InetSocketAddress(serverURI.getHost(), serverURI.getPort());
	}
	


	

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelPipelineFactory#getPipeline()
	 */
	@Override
	public ChannelPipeline getPipeline()  {
		ChannelPipeline pipeline = Channels.pipeline();
		pipeline.addLast("discard", discard);
		pipeline.addLast("metric-encoder", metricEncoder);
		return pipeline;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sender.ISender#send(org.helios.apmrouter.trace.DirectMetricCollection)
	 */
	@Override
	public void send(DirectMetricCollection dcm) {
//		System.out.println("Received [" + dcm.getMetricCount() + "]");
//		SplitReader sr = dcm.newSplitReader(1024);
//		int cnt = 0;
//		for(DirectMetricCollection d: sr) {
//			cnt += d.getMetricCount();
//			d.destroy();
//		}
//		dcm.destroy();
//		System.out.println("Sending [" + cnt + "] Dropped:" + sr.getDrops());
		
		
		
		final Channel ch = channelFactory.newChannel(getPipeline());
		SplitReader sr = dcm.newSplitReader(1024);
		for(final DirectMetricCollection d: sr) {
			final boolean last = !sr.hasNext();
			final int mcount = d.getMetricCount();
			ChannelFuture channelFuture = ch.write(d.toChannelBuffer()); 
			channelFuture.addListener(new ChannelFutureListener() {
				public void operationComplete(ChannelFuture future) throws Exception {					
					if(future.isSuccess()) {
						sent.addAndGet(mcount);
					} else {
						dropped.addAndGet(mcount);
					}
				}
			});
			if(last) channelFuture.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					ch.close();
				}
			});
		}
		dcm.destroy();
		dropped.addAndGet(sr.getDrops());
		
		
	}

}
