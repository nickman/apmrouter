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
package org.helios.apmrouter.nash;

import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

/**
 * <p>Title: Server</p>
 * <p>Description: Nash server bootstrap bean and command line</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.nash.Server</code></p>
 */

public class Server {
	/** The binding interface */
	protected final String bindInterface;
	/** The listening port */
	protected final int port;
	/** The pipeline factory */
	protected final NashServerPipelineFactory pipelineFactory = new NashServerPipelineFactory();
	/** The channel group in which to place all created channels */
	protected final ChannelGroup channelGroup = new DefaultChannelGroup();	
	/** The server bootstrap */
	protected final ServerBootstrap bootstrap = new ServerBootstrap();
	/** The server channel factory */
	protected final NioServerSocketChannelFactory channelFactory;
	/** The listening inet socket */
	protected final InetSocketAddress isock;
	/** The boss thread pool */
	protected final Executor bossPool;
	/** The worker thread pool */
	protected final Executor workerPool;
	
	
	static {
		//InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());		
	}	
	
	
	/**
	 * Creates a new Server
	 * @param bindInterface The binding interface
	 * @param port The listening port
	 * @param bossPool The boss thread pool
	 * @param workerPool The worker thread pool
	 */
	Server(String bindInterface, int port, Executor bossPool, Executor workerPool) {
		this.bindInterface = bindInterface;
		this.port = port;
		isock = new InetSocketAddress(bindInterface, port);
		this.bossPool = bossPool;
		this.workerPool = workerPool;
		channelFactory = new NioServerSocketChannelFactory(
				bossPool,
				workerPool);
		bootstrap.setPipelineFactory(pipelineFactory);
		
		
	}

	/**
	 * Creates a new Server using basic default settings except for the bind interface and the port.
	 * @param bindInterface The binding interface
	 * @param port The listening port
	 */
	public Server(String bindInterface, int port) {
		this.bindInterface = bindInterface;
		this.port = port;
		isock = new InetSocketAddress(bindInterface, port);
		bossPool = Executors.newCachedThreadPool(new ThreadFactory(){
			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setDaemon(true);
				return t;
			}
		});
		workerPool = Executors.newCachedThreadPool(new ThreadFactory(){
			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setDaemon(true);
				return t;
			}
		});
		channelFactory = new NioServerSocketChannelFactory(
				bossPool,
				workerPool);
		
	}

	/**
	 * Boots a standalone nash server using nearly all default configuration except for:
	 * @param args Command line paramters:<ol>
	 * 	<li>The binding interface. Defaults to "0.0.0.0"</li>
	 *  <li>The listening port. Defaults to {@link NashConstants#DEFAULT_PORT}</li>
	 * </ol>
	 */
	public static void main(String[] args) {
		String bind = "0.0.0.0";
		int port = NashConstants.DEFAULT_PORT;
		if(args.length>0) {
			bind = args[0];
		}
		if(args.length>1) {
			try {
				port = Integer.parseInt(args[1]);
			} catch (Exception e) {
				System.err.println("Invalid port specification [" + args[1] + "]");
				banner();
			}
		}
		Server server = new Server(bind, port);
		server.start();
	}
	
	/**
	 * Starts the configured server
	 */
	public void start() {
		System.out.println("Starting nash server on [" + isock + "]");
		bootstrap.setOption("child.tcpNoDelay", true);
		bootstrap.setOption("child.receiveBufferSize", 1048576);
		bootstrap.setFactory(channelFactory);
		bootstrap.bind(isock);		
		System.out.println("Nash Server Started on [" + isock + "]");
	}
	
	/**
	 * Stops the server
	 */
	public void stop() {
		channelGroup.close().awaitUninterruptibly();
		channelFactory.releaseExternalResources();
	}
	
	/**
	 * Prints a usage banner to std out
	 */
	public static void banner() {
		StringBuilder b = new StringBuilder("Usage: java ")
			.append(Server.class.getName())
			.append(" [Listener Binding Interface]")
			.append(" [Listener Port]");
		System.out.println(b);
	}

//	/**
//	 * {@inheritDoc}
//	 * @see org.jboss.netty.channel.ChannelFactory#newChannel(org.jboss.netty.channel.ChannelPipeline)
//	 */
//	@Override
//	public ServerChannel newChannel(ChannelPipeline pipeline)  {
//		ServerChannel channel = channelFactory.newChannel(pipelineFactory.getPipeline());
//		if(channel.isConnected()) {
//			channelGroup.add(channel);
//		}
//		return channel;
//	}


}
