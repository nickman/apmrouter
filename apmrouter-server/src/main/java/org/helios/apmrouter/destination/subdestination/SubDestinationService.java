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
package org.helios.apmrouter.destination.subdestination;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.helios.apmrouter.OpCode;
import org.helios.apmrouter.collections.ConcurrentLongSortedSet;
import org.helios.apmrouter.destination.BaseDestination;
import org.helios.apmrouter.destination.event.DestinationStartedEvent;
import org.helios.apmrouter.destination.event.DestinationStoppedEvent;
import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.metric.IMetricListener;
import org.helios.apmrouter.server.net.listener.netty.handlers.AbstractAgentRequestHandler;
import org.helios.apmrouter.server.net.listener.netty.handlers.AgentRequestHandler;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;

/**
 * <p>Title: SubDestinationService</p>
 * <p>Description: A service to create/register/unregister/stop sub-destinations on demand.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.subdestination.SubDestinationService</code></p>
 */

public class SubDestinationService extends AbstractAgentRequestHandler implements ChannelFutureListener  {
	/** A serial number generator for new subscriptions */
	protected final AtomicLong subscriberSerial = new AtomicLong(0L);
	/** A map of sub-destinations keyed by the sub id */
	protected final Map<Long, SubDestination> subDestinations = new ConcurrentHashMap<Long, SubDestination>();
	
	/** A map of longs representing subIds keyed by the subscribed channel ID */
	protected final Map<Integer, ConcurrentLongSortedSet> channelSubs = new ConcurrentHashMap<Integer, ConcurrentLongSortedSet>();
	
	/** The OpCodes supported by this {@link AgentRequestHandler} */
	protected static final OpCode[] SUPPORTED_OP_CODES = new OpCode[]{OpCode.START_SUB_DEST, OpCode.STOP_SUB_DEST};
	
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.handlers.AgentRequestHandler#processAgentRequest(org.helios.apmrouter.OpCode, org.jboss.netty.buffer.ChannelBuffer, java.net.SocketAddress, org.jboss.netty.channel.Channel)
	 */
	@Override
	public void processAgentRequest(OpCode opCode, ChannelBuffer buff, SocketAddress remoteAddress, Channel channel) {
		Channel connectedChannel = getChannelForRemote(channel, remoteAddress);
		if(opCode==OpCode.START_SUB_DEST) {
			buff.skipBytes(1);
			int patternCount = buff.readInt();
			String[] patterns = new String[patternCount];
			for(int i = 0; i < patternCount; i++) {
				int patternLength = buff.readInt();
				byte[] patternBytes = new byte[patternLength];
				buff.readBytes(patternBytes);
				patterns[i] = new String(patternBytes);				
			}
			ConcurrentLongSortedSet subIds = channelSubs.get(connectedChannel.getId());
			if(subIds==null) {
				synchronized(channelSubs) {
					subIds = channelSubs.get(connectedChannel.getId());
					if(subIds==null) {
						connectedChannel.getCloseFuture().addListener(this);
						subIds = new ConcurrentLongSortedSet();
						channelSubs.put(connectedChannel.getId(), subIds);
						AgentSubDestinationListener agentListener = new AgentSubDestinationListener(connectedChannel, patterns);
						startSubDestination(agentListener);
					}
				}
			}
			
		} else if(opCode==OpCode.STOP_SUB_DEST) {
			/* Not implemented yet */
		} else {
			warn("Unsupported OpCode [", opCode, "]");
		}
		
	}
	
	/**
	 * <p>Terminates a channel's subscriptions when it closes.</p>
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelFutureListener#operationComplete(org.jboss.netty.channel.ChannelFuture)
	 */
	@Override
	public void operationComplete(ChannelFuture future) throws Exception {
		Channel ch = future.getChannel();
		Integer channelId = ch.getId();
		ConcurrentLongSortedSet subIds = channelSubs.remove(channelId);
		if(subIds!=null && !subIds.isEmpty()) {
			for(int i = 0; i < subIds.size(); i++) {
				stopSubDestination(subIds.get(i));
			}
		}
		
	}
	
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.handlers.AgentRequestHandler#getHandledOpCodes()
	 */
	@Override
	public OpCode[] getHandledOpCodes() {
		return SUPPORTED_OP_CODES;
	}	
	
	/**
	 * Creates and registers a subscription for the passed listener
	 * @param metricListener The metric listener
	 * @return the subscription id
	 */
	public long startSubDestination(IMetricListener metricListener) {
		if(metricListener==null) return -1L;
		final long subId = subscriberSerial.incrementAndGet();
		metricListener.setSubscriptionId(subId);
		final SubDestination sd = new SubDestination(metricListener);
		subDestinations.put(subId, sd);
		applicationContext.publishEvent(new DestinationStartedEvent(sd, "SubDestination#" + subId));
		info("Started SubDestination [", subId, "]");
		return subId;
	}
	
	/**
	 * Stops the sub destination for the passed listener
	 * @param metricListener The listener to stop the sub for
	 */
	public void stopSubDestination(IMetricListener metricListener) {
		if(metricListener!=null) {
			stopSubDestination(metricListener.getSubscriptionId());
		}
	}
	
	/**
	 * Stops the sub destination for the listener with the passed id.
	 * @param subId The subId of the listener to stop the sub for
	 */
	public void stopSubDestination(long subId) {
		final SubDestination sd = subDestinations.remove(subId);
		if(sd!=null) {
			applicationContext.publishEvent(new DestinationStoppedEvent(sd, "SubDestination#" + subId));
			info("Stopped SubDestination [", subId, "]");
		} else {
			warn("No SubDestination Found to Stop for [", subId, "]");
		}
	}
	
	
	/**
	 * <p>Title: SubDestination</p>
	 * <p>Description: A sub-destination created and registered when a subscriber subscribes</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.destination.subdestination.SubDestinationService.SubDestination</code></p>
	 */
	private class SubDestination extends BaseDestination {
		/** the listener this sub will forward matrching metrics to */
		private final IMetricListener listener;
		/**
		 * Creates a new SubDestination
		 * @param listener the listener this sub will forward matrching metrics to
		 */
		protected SubDestination(IMetricListener listener) {
			super(listener.getPatterns());
			this.listener = listener;
		}
		
		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.destination.BaseDestination#doAcceptRoute(org.helios.apmrouter.metric.IMetric)
		 */
		@Override
		protected void doAcceptRoute(IMetric routable) {			
			listener.onMetric(routable);
		}

		/**
		 * Returns this sub's listener
		 * @return this sub's listener
		 */
		public IMetricListener getListener() {
			return listener;
		}
		
	}





}
