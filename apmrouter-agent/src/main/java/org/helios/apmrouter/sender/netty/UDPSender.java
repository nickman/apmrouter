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

import org.helios.apmrouter.OpCode;
import org.helios.apmrouter.jmx.mbeanserver.AgentMBeanServerConnectionFactory;
import org.helios.apmrouter.metric.AgentIdentity;
import org.helios.apmrouter.sender.AbstractSender;
import org.helios.apmrouter.trace.DirectMetricCollection;
import org.helios.apmrouter.trace.DirectMetricCollection.SplitDMC;
import org.helios.apmrouter.trace.DirectMetricCollection.SplitReader;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.buffer.DirectChannelBufferFactory;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLogLevel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.CountDownLatch;

import static org.helios.apmrouter.util.Methods.nvl;

/**
 * <p>Title: UDPSender</p>
 * <p>Description: A Netty unicast UDP sender implementation</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.sender.netty.UDPSender</code></p>
 * FIXME:  Where to start ......  1. Listen on channel closed exceptions, start reconnect loop, count dropped metrics in the interrim
 */

public class UDPSender extends AbstractSender  {
	
	/** The maximum size of the payload this sender can reliably expect to be transmitted */
	public static final int MAXSIZE = 1024;
	
	/** The netty bootstrap */
	protected final ConnectionlessBootstrap bstrap;
	
	
	
	/** The logging handler for debug */
	private LoggingHandler loggingHandler;
	/** The listener handle to handle requests/responses from the server */
	protected final SimpleChannelUpstreamHandler listenerHandler = new SimpleChannelUpstreamHandler() {
		public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
			log("[Listener] Caught exception event [" + e.getCause() + "]");
			e.getCause().printStackTrace(System.err);
			super.exceptionCaught(ctx, e);
		}
		@Override
		public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
			if(e.getChannel().getLocalAddress().equals(e.getRemoteAddress())) { /* No Op */
			} else {
				Object msg = e.getMessage();
				if(msg instanceof ChannelBuffer) {
					ChannelBuffer buff = (ChannelBuffer)msg;
					OpCode opCode = OpCode.valueOf(buff.readByte());	
					switch (opCode) {
						case JMX_REQUEST:
							AgentMBeanServerConnectionFactory.handleJMXRequest(e.getChannel(), e.getRemoteAddress(), buff);
							break;
						case JMX_MBS_INQUIRY:
							AgentMBeanServerConnectionFactory.sendMBeanServerDomains(e.getChannel(), e.getRemoteAddress());
							break;
						case RESET:
							metricCatalog.resetTokens();
							ChannelBuffer rsetConfirm = ChannelBuffers.buffer(1);
							rsetConfirm.writeByte(OpCode.RESET_CONFIRM.op());
							senderChannel.write(rsetConfirm,e.getRemoteAddress());														
							break;
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
						case HELLO_CONFIRM:
							latch = timeoutMap.remove("Hello");
							log("Confirmed HELLO");
							if(latch!=null) {
								latch.countDown();
							}							
							break;
						case WHO:
							byte[] hostBytes = AgentIdentity.ID.getHostName().getBytes();
							byte[] agentBytes = AgentIdentity.ID.getAgentName().getBytes();
							ChannelBuffer cb = ChannelBuffers.directBuffer(1 + 4 + 4 + hostBytes.length + agentBytes.length);
							cb.writeByte(OpCode.WHO_RESPONSE.op());
							cb.writeInt(hostBytes.length);
							cb.writeBytes(hostBytes);
							cb.writeInt(agentBytes.length);
							cb.writeBytes(agentBytes);
							SocketAddress sa = e.getRemoteAddress();
							log("Sending WHO Response to [" + sa + "]");
							e.getChannel().write(cb, sa);
							//AgentMBeanServerConnectionFactory.sendMBeanServerDomains(e.getChannel(), e.getRemoteAddress());
							break;
						case SEND_METRIC_TOKEN:
							int fqnLength = buff.readInt();
							byte[] bytes = new byte[fqnLength];
							buff.readBytes(bytes);
							String fqn = new String(bytes);
							long token = buff.readLong();
							metricCatalog.setToken(fqn, token);		
							processedTokens.incrementAndGet();
							break;
						case ON_METRIC_URI_EVENT:
							onMetricURIEvent(buff);
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
	private UDPSender(final URI serverURI) {
		super(serverURI);
		
				
		//InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
		channelStateListener.addChannelStateAware(this);
		loggingHandler = new LoggingHandler(InternalLogLevel.ERROR, true);		
		channelFactory = new NioDatagramChannelFactory(workerPool);
		bstrap = new ConnectionlessBootstrap(channelFactory);
		bstrap.setPipelineFactory(this);
		bstrap.setOption("broadcast", true);
		bstrap.setOption("localAddress", new InetSocketAddress(0));
		bstrap.setOption("remoteAddress", new InetSocketAddress(serverURI.getHost(), serverURI.getPort()));
		bstrap.setOption("receiveBufferSizePredictorFactory", new FixedReceiveBufferSizePredictorFactory(2048));
		
		listeningSocketAddress = new InetSocketAddress("0.0.0.0", 0);
		//listeningSocketAddress = new InetSocketAddress("127.0.0.1", 0);
			
		//senderChannel = (NioDatagramChannel) channelFactory.newChannel(getPipeline());
		senderChannel = bstrap.bind();
		closeGroup.add(senderChannel);
		log("Listening on [" + senderChannel.getLocalAddress()+  "]");					
		
		
//		senderChannel.bind().addListener(new ChannelFutureListener() {
//			public void operationComplete(ChannelFuture f) throws Exception {
//				if(f.isSuccess()) {
//					log("Listening on [" + f.getChannel().getLocalAddress()+  "]");					
//				} else {
//					log("Failed to start listener. Stack trace follows");
//					f.getCause().printStackTrace(System.err);
//					
//				}
//				
//			}
//		});
		senderChannel.getConfig().setBufferFactory(new DirectChannelBufferFactory());
//		senderChannel.connect(socketAddress).addListener(new ChannelFutureListener() {
//			@Override
//			public void operationComplete(ChannelFuture future) throws Exception {
//				connected.set(true);	
//				sentryState.setState(SentryState.CALLBACK);
//			}
//		});
		
		
		//socketAddress = new InetSocketAddress("239.192.74.66", 25826);
		sendHello();
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
		//pipeline.addLast("logging", loggingHandler);		
		pipeline.addLast("metric-encoder", metricEncoder);
		pipeline.addLast("listener", listenerHandler);
		
		return pipeline;
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sender.AbstractSender#doSendHello()
	 */
	@Override
	public void doSendHello() {
		ChannelBuffer cb = ChannelBuffers.buffer(1);
		cb.writeByte(OpCode.HELLO.op());
		senderChannel.write(cb, socketAddress);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sender.ISender#send(org.helios.apmrouter.trace.DirectMetricCollection)
	 */
	@Override
	public void send(final DirectMetricCollection dcm) {
		if(shutdown.get()) return;
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
							//System.err.println("SenderFactory Fails:" + d );
							if(future.getCause()!=null && !shutdown.get()) {
								if(future.getCause() instanceof ClosedChannelException) {
									log("SenderFactory Channel Disconnected");
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
							//System.err.println("SenderFactory Fails:" + d );
							if(future.getCause()!=null) {
								if(future.getCause() instanceof ClosedChannelException) {
									log("SenderFactory Channel Disconnected");
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
				log("SenderFactory Channel Disconnected");
				processDisconnect();
			} else {
				cce.printStackTrace(System.err);
			}
		}
		
		
		
		
	}


	
	





}
