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

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.BasicConfigurator;
import org.helios.apmrouter.OpCode;
import org.helios.apmrouter.jmx.ThreadPoolFactory;
import org.helios.apmrouter.metric.catalog.ICEMetricCatalog;
import org.helios.apmrouter.metric.catalog.IMetricCatalog;
import org.helios.apmrouter.sender.AbstractSender;
import org.helios.apmrouter.sender.netty.handler.ChannelStateAware;
import org.helios.apmrouter.sender.netty.handler.ChannelStateListener;
import org.helios.apmrouter.sentry.PollingSentryWatched;
import org.helios.apmrouter.sentry.Sentry;
import org.helios.apmrouter.sentry.SentryState;
import org.helios.apmrouter.sentry.SentryStateControl;
import org.helios.apmrouter.trace.DirectMetricCollection;
import org.helios.apmrouter.trace.DirectMetricCollection.SplitDMC;
import org.helios.apmrouter.trace.DirectMetricCollection.SplitReader;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.buffer.DirectChannelBufferFactory;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelState;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.FixedReceiveBufferSizePredictorFactory;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioDatagramChannel;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLogLevel;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Log4JLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Title: UDPSender</p>
 * <p>Description: A Netty unicast UDP sender implementation</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.sender.netty.UDPSender</code></p>
 * FIXME:  Where to start ......  1. Listen on channel closed exceptions, start reconnect loop, count dropped metrics in the interrim
 */

public class UDPSender extends AbstractSender implements ChannelPipelineFactory, ChannelStateAware, PollingSentryWatched {
	
	/** The maximum size of the payload this sender can reliably expect to be transmitted */
	public static final int MAXSIZE = 1024;
	
	/** Static class logger */
	protected static final Logger LOG = LoggerFactory.getLogger(UDPSender.class);	
	/** The netty server worker pool */
	protected final Executor workerPool;
	/** The netty bootstrap */
	protected final ConnectionlessBootstrap bstrap;
	/** The netty channel factory */
	protected final ChannelFactory channelFactory;
	
	
	/** The metric catalog for token updates */
	protected final IMetricCatalog metricCatalog;
	/** The channel close future */
	protected ChannelFuture closeFuture = null;
	/** Indicates if the sender is connected */
	protected final AtomicBoolean connected = new AtomicBoolean(false);
	/** The sentry state of the sender channel */
	protected final SentryStateControl sentryState;
	
	
	/** The channel state listener */
	private final ChannelStateListener channelStateListener = new ChannelStateListener();
	
	/** The logging handler for debug */
	private LoggingHandler loggingHandler;
	/** The listener handle to handle requests/responses from the server */
	protected final SimpleChannelUpstreamHandler listenerHandler = new SimpleChannelUpstreamHandler() {
		public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
			log("[Listener] Caught exception event [" + e.getCause() + "]");
		}
		@Override
		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
			if(e.getChannel().getLocalAddress().equals(e.getRemoteAddress())) {
				LOG.info("Drop");
			} else {
				Object msg = e.getMessage();
				if(msg instanceof ChannelBuffer) {
					ChannelBuffer buff = (ChannelBuffer)msg;
					OpCode opCode = OpCode.valueOf(buff.readByte());					
					switch (opCode) {
						case CONFIRM_METRIC:							
							int keyLength = buff.readInt();
							byte[] keyBytes = new byte[keyLength];
							buff.readBytes(keyBytes);
							String key = new String(keyBytes);
							CountDownLatch latch = timeoutMap.remove(key);
							if(latch!=null) {
								latch.countDown();
							}
							break;
						case PING_RESPONSE:							
							decodePing(buff);
							break;
						case PING:							
							long pingKey = buff.readLong();
							ChannelBuffer ping = ChannelBuffers.buffer(1+8);
							ping.writeByte(OpCode.PING_RESPONSE.op());
							ping.writeLong(pingKey);
							senderChannel.write(ping,e.getRemoteAddress());							
							break;							
							
						case SEND_METRIC_TOKEN:
							int fqnLength = buff.readInt();
							byte[] bytes = new byte[fqnLength];
							buff.readBytes(bytes);
							String fqn = new String(bytes);
							long token = buff.readLong();
							metricCatalog.setToken(fqn, token);						
							break;
						default:
							break;
					}
				}
			}
		}
	};
	
	
	/**
	 * Executed when a disconnect is detected
	 */
	protected void processDisconnect() {
		connected.set(false);
		senderChannel = null;
		sentryState.setState(SentryState.DISCONNECTED);
	}
	
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
		BasicConfigurator.configure();
		
				
		InternalLoggerFactory.setDefaultFactory(new Log4JLoggerFactory());
		metricCatalog = ICEMetricCatalog.getInstance();
		channelStateListener.addChannelStateAware(this);
		loggingHandler = new LoggingHandler(InternalLogLevel.DEBUG, true);
		workerPool =  ThreadPoolFactory.newCachedThreadPool(getClass().getPackage().getName(), "UDPSenderWorker/" + serverURI.getHost() + "/" + serverURI.getPort());
		channelFactory = new NioDatagramChannelFactory(workerPool);
		bstrap = new ConnectionlessBootstrap(channelFactory);
		bstrap.setPipelineFactory(this);
		bstrap.setOption("broadcast", false);
		bstrap.setOption("receiveBufferSizePredictorFactory", new FixedReceiveBufferSizePredictorFactory(MAXSIZE));
		socketAddress = new InetSocketAddress(serverURI.getHost(), serverURI.getPort());
		try {
			listeningSocketAddress = new InetSocketAddress(Inet4Address.getLocalHost(), 0);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		sentryState = Sentry.getInstance().register(this);		
		senderChannel = (NioDatagramChannel) channelFactory.newChannel(getPipeline());
		senderChannel.bind(listeningSocketAddress).addListener(new ChannelFutureListener() {
			public void operationComplete(ChannelFuture f) throws Exception {
				if(f.isSuccess()) {
					log("Listening on [" + f.getChannel().getLocalAddress() + "]");
					sentryState.setState(SentryState.POLLING);
				} else {
					log("Failed to start listener. Stack trace follows");
					f.getCause().printStackTrace(System.err);
					
				}
				
			}
		});
		senderChannel.getConfig().setBufferFactory(new DirectChannelBufferFactory());
//		senderChannel.connect(socketAddress).addListener(new ChannelFutureListener() {
//			@Override
//			public void operationComplete(ChannelFuture future) throws Exception {
//				connected.set(true);	
//				sentryState.setState(SentryState.CALLBACK);
//			}
//		});
		
		
		//socketAddress = new InetSocketAddress("239.192.74.66", 25826);
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sender.netty.handler.ChannelStateAware#getInterestedChannelStates()
	 */
	@Override
	public ChannelState[] getInterestedChannelStates() {
		return new ChannelState[]{ChannelState.CONNECTED};
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sender.netty.handler.ChannelStateAware#onChannelStateEvent(boolean, org.jboss.netty.channel.ChannelStateEvent)
	 */
	@Override
	public void onChannelStateEvent(boolean upstream, ChannelStateEvent stateEvent) {
		if(upstream && stateEvent.getValue().equals(Boolean.FALSE)) {
			processDisconnect();
			log("Channel Disconnected");
		}
	}
	


	

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelPipelineFactory#getPipeline()
	 */
	@Override
	public ChannelPipeline getPipeline()  {
		ChannelPipeline pipeline = Channels.pipeline();		
		pipeline.addLast("logging", loggingHandler);		
		pipeline.addLast("metric-encoder", metricEncoder);
		pipeline.addLast("listener", listenerHandler);
		
		return pipeline;
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sender.ISender#send(org.helios.apmrouter.trace.DirectMetricCollection)
	 */
	@Override
	public void send(final DirectMetricCollection dcm) {
		
		if(dcm==null) return;
		final int METRIC_COUNT = dcm.getMetricCount(); 
		try {
			if(dcm.getSize()<MAXSIZE) {
				final int mcount = dcm.getMetricCount();
				ChannelFuture channelFuture = senderChannel.write(dcm, socketAddress);
				//System.out.println("Sent to [" + socketAddress + "]");
				channelFuture.addListener(new ChannelFutureListener() {
					public void operationComplete(ChannelFuture future) throws Exception {					
						if(future.isSuccess()) {
							sent.addAndGet(mcount);
						} else {
							//long d = failed.addAndGet(mcount);
							//System.err.println("Sender Fails:" + d );
							if(future.getCause()!=null) {
								if(future.getCause() instanceof ClosedChannelException) {
									log("Sender Channel Disconnected");
									processDisconnect();
								} else {
									future.getCause().printStackTrace(System.err);
								}
							}
						}					
					}
				});
				return;
			}
			
			
			
			SplitDMC sr = dcm.newSplitReader(MAXSIZE);
			for(final DirectMetricCollection d: sr) {
				final boolean last = !sr.hasNext();
				final int mcount = d.getMetricCount();
				ChannelFuture channelFuture = senderChannel.write(d, socketAddress);
				channelFuture.addListener(new ChannelFutureListener() {
					public void operationComplete(ChannelFuture future) throws Exception {					
						if(future.isSuccess()) {
							sent.addAndGet(mcount);
						} else {
							//long d = failed.addAndGet(mcount);
							//System.err.println("Sender Fails:" + d );
							if(future.getCause()!=null) {
								if(future.getCause() instanceof ClosedChannelException) {
									log("Sender Channel Disconnected");
									processDisconnect();
								} else {
									future.getCause().printStackTrace(System.err);
								}
							}
						}
					}
				});
				if(last) channelFuture.addListener(new ChannelFutureListener() {
					@Override
					public void operationComplete(ChannelFuture future) throws Exception {
						//ch.close();
					}
				});
			}
			dcm.destroy();
			dropped.addAndGet(((SplitReader)sr).getDrops());
		} catch (Exception cce) {
			if(cce instanceof ClosedChannelException) {
				log("Sender Channel Disconnected");
				processDisconnect();
			} else {
				cce.printStackTrace(System.err);
			}
		}
		
		
		
		
	}
	
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sentry.SentryWatched#getName()
	 */
	@Override
	public String getName() {
		return "UDPSender[" + socketAddress.getHostName() + ":" + socketAddress.getPort() + "]";
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sentry.SentryWatched#getSentryState()
	 */
	@Override
	public SentryState getSentryState() {
		return sentryState.getState();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sentry.SentryWatched#getPeriod()
	 */
	@Override
	public long getPeriod() {
		return 5000;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sentry.PollingSentryWatched#sentryPoll()
	 */
	@Override
	public boolean sentryPoll() {
		return ping(1500);		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sentry.PollingSentryWatched#sentryPollFailed()
	 */
	@Override
	public void sentryPollFailed() {
		// TODO Auto-generated method stub
		
	}

}
