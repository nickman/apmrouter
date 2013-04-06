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
package org.helios.apmrouter.server.net.listener.netty.handlers;

import java.net.SocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.helios.apmrouter.OpCode;
import org.helios.apmrouter.collections.ConcurrentLongSlidingWindow;
import org.helios.apmrouter.collections.ILongSlidingWindow;
import org.helios.apmrouter.server.services.session.DecoratedChannel;
import org.helios.apmrouter.server.services.session.SharedChannelGroup;
import org.helios.apmrouter.server.services.session.VirtualUDPChannel;
import org.helios.apmrouter.util.TimeoutListener;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;

/**
 * <p>Title: PingRequestHandler</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.net.listener.netty.handlers.PingRequestHandler</code></p>
 * TODO: Need to associate the remote address of an agent to the agent identity by sniffing the metric names or have some upstream processor feedback this data.
 */

public class PingRequestHandler extends AbstractAgentRequestHandler implements TimeoutListener<String, SocketAddress> {
	/**  */
	private static final OpCode[] handledCodes = new OpCode[]{OpCode.PING, OpCode.PING_RESPONSE};
	/** Sliding window of ping times */
	protected final ILongSlidingWindow pingTimes = new ConcurrentLongSlidingWindow(64); 
	/**
	 * Creates a new PingRequestHandler
	 */
	public PingRequestHandler() {
		super();
		sessionTimeoutMap.addListener(this);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.handlers.AgentRequestHandler#processAgentRequest(org.helios.apmrouter.OpCode, org.jboss.netty.buffer.ChannelBuffer, java.net.SocketAddress, org.jboss.netty.channel.Channel)
	 */
	@Override
	public void processAgentRequest(OpCode opCode, ChannelBuffer buff, final SocketAddress remoteAddress, Channel channel) {
		switch(opCode) {
		case PING_RESPONSE:							
			incr("TotalPingResponseCount");
			long pingKey = buff.readLong();
			
			Channel session = SharedChannelGroup.getInstance().getByRemote(remoteAddress);
			if(session!=null && session instanceof DecoratedChannel) {
				if(((DecoratedChannel)session).getDelegate() instanceof VirtualUDPChannel) {
					VirtualUDPChannel vuc = (VirtualUDPChannel)((DecoratedChannel)session).getDelegate();
					if(vuc!=null) {
						vuc.pingResponse();
					}					
				}
			}
			
			CountDownLatch latch = timeoutMap.remove("" + pingKey);
			if(latch!=null) {
				latch.countDown();
			}
			pingTimes.insert(System.nanoTime()-pingKey);
			break;
		case PING:
			sessionTimeoutMap.put(remoteAddress.toString(), remoteAddress);			
			buff.resetReaderIndex();
			buff.readByte();
			final int byteCount = buff.readInt();
			final byte[] bytes = new byte[byteCount];
			buff.readBytes(bytes);
			ChannelBuffer ping = ChannelBuffers.buffer(1+4+bytes.length);
			ping.writeByte(OpCode.PING_RESPONSE.op());
			ping.writeInt(byteCount);
			ping.writeBytes(bytes);
			getChannelForRemote(channel, remoteAddress).write(ping,remoteAddress).addListener(new ChannelFutureListener() {
			//getChannelForRemote(channel, remoteAddress).write(ping,remoteAddress).addListener(new ChannelFutureListener() {
				public void operationComplete(ChannelFuture future) throws Exception {
					if(future.isSuccess()) {
						//log.info("Sent ping response to [" + remoteAddress + "]--->[" + new String(bytes) + "]");
						incr("TotalPingCount");
					} else {
						log.info("Failed to send ping response to [" + remoteAddress + "]");
					}
				}
			});
			break;							
		}

	}

	/**
	 * Returns a sliding window average of agent ping elapsed times to the server
	 * @return a sliding window average of agent ping elapsed times to the server
	 */
	@ManagedMetric(category="PingService", metricType=MetricType.GAUGE, description="a sliding window average of ping elapsed times to agents")
	public long getAveragePingTime() {
		return pingTimes.avg();
	}	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.handlers.AgentRequestHandler#getHandledOpCodes()
	 */
	@Override
	public OpCode[] getHandledOpCodes() {
		return handledCodes;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.util.TimeoutListener#onTimeout(java.lang.Object, java.lang.Object)
	 */
	@Override
	public void onTimeout(String key, SocketAddress value) {
		info("Client Session Timed Out [", key, "]");		
	}
	
	/**
	 * Returns the total number of pings sent
	 * @return the total number of pings sent
	 */
	@ManagedMetric(category="PingService", metricType=MetricType.GAUGE, description="the total number of pings sent")
	public long getTotalPingCount() {
		return getMetricValue("TotalPingCount");
	}
	
	/**
	 * Returns the total number of ping responses handled
	 * @return the total number of ping responses handled
	 */
	@ManagedMetric(category="PingService", metricType=MetricType.GAUGE, description="the total number of ping responses handled")
	public long getTotalPingResponseCount() {
		return getMetricValue("TotalPingResponseCount");
	}
	
	


}
