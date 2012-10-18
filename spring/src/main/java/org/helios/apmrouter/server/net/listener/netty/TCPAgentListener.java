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

import java.util.concurrent.ExecutorService;

import org.helios.apmrouter.server.services.session.ChannelType;
import org.helios.apmrouter.server.services.session.SharedChannelGroup;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.DirectChannelBufferFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.socket.ServerSocketChannel;
import org.jboss.netty.channel.socket.nio.NioDatagramChannel;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * <p>Title: TCPAgentListener</p>
 * <p>Description: Base netty TCP server </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.net.listener.netty.TCPAgentListener</code></p>
 */

public class TCPAgentListener extends BaseAgentListener {
	/** The netty channel factory's boss thread pool */
	protected ExecutorService bossPool = null;
	/** The agent listener channel factory */
	protected NioServerSocketChannelFactory channelFactory;  
	/** The agent listener bootstrap */
	protected ServerBootstrap bstrap; 
	/** The server channel */
	protected ServerSocketChannel serverChannel;

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.BaseAgentListener#doStart()
	 */
	@Override
	protected void doStart() throws Exception {
		super.doStart();
		channelFactory = new NioServerSocketChannelFactory(bossPool, workerPool);	
		bstrap = new ServerBootstrap(channelFactory);
		bstrap.setOptions(channelOptions);
		bstrap.setPipelineFactory(this);
	}

	
	/**
	 * Callback when the current app context refreshes
	 * @param cre The context refreshed event
	 */
	public void onApplicationContextRefresh(ContextRefreshedEvent cre) {
		serverChannel = (ServerSocketChannel)bstrap.bind(socketAddress);
		SharedChannelGroup.getInstance().add(serverChannel, ChannelType.TCP_SERVER, getClass().getSimpleName());
		closeFuture = serverChannel.getCloseFuture();
		closeFuture.addListener(new ChannelFutureListener() {
			public void operationComplete(ChannelFuture future) throws Exception {
				connected.set(false);
			}
		});		
		serverChannel.getConfig().setBufferFactory(new DirectChannelBufferFactory());
		channelGroup.add(serverChannel, "DataServiceListener/" + socketAddress);
		connected.set(true);
		info("Started TCP listener on [", socketAddress , "]");		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.BaseAgentListener#doStop()
	 */
	@Override
	protected void doStop() {
		info("Closing ChannelGroup....");
		channelGroup.close().awaitUninterruptibly();
		info("Closing ChannelFactory....");
		channelFactory.releaseExternalResources();
		super.doStop();
	}
	
	
	
	
	
	
	
	/**
	 * Sets the boss pool for this listener
	 * @param bossPool the bossPool to set
	 */
	public void setBossPool(ExecutorService bossPool) {
		this.bossPool = bossPool;
	}

}
