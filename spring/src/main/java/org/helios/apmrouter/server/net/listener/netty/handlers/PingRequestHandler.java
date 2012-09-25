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
 */

public class PingRequestHandler extends AbstractAgentRequestHandler implements TimeoutListener<String, SocketAddress> {
	/**  */
	private static final OpCode[] handledCodes = new OpCode[]{OpCode.PING, OpCode.PING_RESPONSE};
	/** Sliding window of ping times */
	protected final ConcurrentLongSlidingWindow pingTimes = new ConcurrentLongSlidingWindow(64); 
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
			long pingKey = buff.readLong();
			CountDownLatch latch = timeoutMap.remove("" + pingKey);
			if(latch!=null) {
				latch.countDown();
			}
			pingTimes.insert(System.nanoTime()-pingKey);
			break;
		case PING:							
			pingKey = buff.readLong();
			ChannelBuffer ping = ChannelBuffers.buffer(1+8);
			ping.writeByte(OpCode.PING_RESPONSE.op());
			ping.writeLong(pingKey);			
			getChannelForRemote(channel, remoteAddress).write(ping,remoteAddress).addListener(new ChannelFutureListener() {
				public void operationComplete(ChannelFuture future) throws Exception {
					if(future.isSuccess()) {
						log.info("Sent ping response to [" + remoteAddress + "]");
						incr("TotalPingCount");
					} else {
						log.info("Failed to send ping response to [" + remoteAddress + "]");
					}
				}
			});
			sessionTimeoutMap.put(remoteAddress.toString(), remoteAddress);
			
			
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
	 * Returns the total ping count handled
	 * @return the total ping count handled
	 */
	@ManagedMetric(category="PingService", metricType=MetricType.GAUGE, description="a sliding window average of ping elapsed times to agents")
	public long getTotalPingCount() {
		return getMetricValue("TotalPingCount");
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponent#getSupportedMetricNames()
	 */
	@Override
	public Set<String> getSupportedMetricNames() {
		HashSet<String> metrics = new HashSet<String>(super.getSupportedMetricNames());
		metrics.add("TotalPingCount");
		return metrics;
	}

}
