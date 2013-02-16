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

import org.helios.apmrouter.server.services.session.ChannelType;
import org.helios.apmrouter.server.services.session.SharedChannelGroup;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.buffer.DirectChannelBufferFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.FixedReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.socket.nio.NioDatagramChannel;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * <p>Title: UDPAgentListener</p>
 * <p>Description: Service to listen for agent requests over UDP.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.net.listener.netty.UDPAgentListener</code></p>
 */

public class UDPAgentListener extends BaseAgentListener {
	/** The agent listener channel factory */
	protected NioDatagramChannelFactory channelFactory;  
	/** The agent listener bootstrap */
	protected ConnectionlessBootstrap bstrap; 
	/** The server channel */
	protected NioDatagramChannel serverChannel;
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.BaseAgentListener#doStart()
	 */
	@Override
	protected void doStart() throws Exception {
		super.doStart();
		channelFactory = new NioDatagramChannelFactory(workerPool);	
		bstrap = new ConnectionlessBootstrap(channelFactory);
		bstrap.setOptions(channelOptions);
		bstrap.setOption("receiveBufferSizePredictorFactory", new FixedReceiveBufferSizePredictorFactory(1024));
		bstrap.setPipelineFactory(this);
	}
	

	
	/**
	 * Callback when the current app context refreshes
	 * @param cre The context refreshed event
	 */
	public void onApplicationContextRefresh(ContextRefreshedEvent cre) {
		serverChannel = (NioDatagramChannel)bstrap.bind(socketAddress);
		SharedChannelGroup.getInstance().add(serverChannel, ChannelType.UDP_SERVER, getClass().getSimpleName());
		closeFuture = serverChannel.getCloseFuture();
		closeFuture.addListener(new ChannelFutureListener() {
			public void operationComplete(ChannelFuture future) throws Exception {
				connected.set(false);
			}
		});		
		serverChannel.getConfig().setBufferFactory(new DirectChannelBufferFactory());
		connected.set(true);
		info("Started UDP listener on [", socketAddress , "]");		
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.BaseAgentListener#doStop()
	 */
	@Override
	protected void doStop() {
		info("Closing ChannelGroup....");
		info("Closing ChannelFactory....");
		channelFactory.releaseExternalResources();
		super.doStop();
	}
		

	

	

	
	

}
