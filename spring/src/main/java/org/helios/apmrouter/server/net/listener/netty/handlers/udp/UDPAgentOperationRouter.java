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
package org.helios.apmrouter.server.net.listener.netty.handlers.udp;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;

import org.helios.apmrouter.OpCode;
import org.helios.apmrouter.jmx.mbeanserver.AgentMBeanServerConnectionFactory;
import org.helios.apmrouter.server.net.listener.netty.handlers.AbstractAgentRequestHandler;
import org.helios.apmrouter.server.net.listener.netty.handlers.AgentRequestHandler;
import org.helios.apmrouter.server.services.session.DecoratedChannel;
import org.helios.apmrouter.server.services.session.SharedChannelGroup;
import org.helios.apmrouter.util.ValueFilteredTimeoutListener;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLogLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;

/**
 * <p>Title: UDPAgentOperationRouter</p>
 * <p>Description: Routes agent requests to the correct service in accordance with the op-code in the request message</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.net.listener.netty.handlers.udp.UDPAgentOperationRouter</code></p>
 */

public class UDPAgentOperationRouter extends AbstractAgentRequestHandler implements ChannelUpstreamHandler, ValueFilteredTimeoutListener<String, OpCode> {  //ChannelGroupAware
//	/** The channel group */
//	protected ManagedChannelGroupMXBean channelGroup = null;
	

	/** A map of agent request handlers keyed by the opcode */
	protected final EnumMap<OpCode, AgentRequestHandler> handlers = new EnumMap<OpCode, AgentRequestHandler>(OpCode.class);
	
	
	
	/**
	 * Sets the agent request handlers
	 * @param agentRequestHandlers a collection of agent request handlers
	 */
	@Autowired(required=true)
	public void setAgentRequestHandlers(Collection<AgentRequestHandler> agentRequestHandlers) {
		for(AgentRequestHandler arh: agentRequestHandlers) {
			for(OpCode soc: arh.getHandledOpCodes()) {
				handlers.put(soc, arh);
				info("Added AgentRequestHandler [", arh.getClass().getSimpleName(), "] for Op [", soc , "]");
			}
		}
		handlers.put(OpCode.WHO_RESPONSE, this);
		handlers.put(OpCode.BYE, this);
		handlers.put(OpCode.HELLO, this);
		handlers.put(OpCode.HELLO_CONFIRM, this);
		handlers.put(OpCode.JMX_MBS_INQUIRY_RESPONSE, this);
	}
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelUpstreamHandler#handleUpstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		if(e instanceof MessageEvent) {
			Object msg = ((MessageEvent)e).getMessage();
			if(msg instanceof ChannelBuffer) {
				SocketAddress remoteAddress = ((MessageEvent)e).getRemoteAddress();
				ChannelBuffer buff = (ChannelBuffer)msg;
				incr("RequestsReceived");
				OpCode opCode = OpCode.valueOf(buff);
				if(opCode==OpCode.BYE) {
					info("Processing BYE for client at [" + remoteAddress + "]");
					DecoratedChannel dc = (DecoratedChannel) SharedChannelGroup.getInstance().getByRemote(remoteAddress);
					if(dc!=null) {
						dc.close();
					}
					return;
				}
				
				if(!containsPendingOp(remoteAddress, OpCode.WHO)) {
					Channel remoteChannel = SharedChannelGroup.getInstance().getByRemote(remoteAddress);
					if(remoteChannel==null) {
						try {
							remoteChannel = getChannelForRemote(e.getChannel(), remoteAddress);						
						} catch (Exception ex) {
							ex.printStackTrace(System.err);
						}
					}
				}
				try {
					
					AgentRequestHandler handler = handlers.get(opCode);
					if(handler==null) {
						error("No handler registered for OpCode [", opCode, "]");
						return;
					}
					handler.processAgentRequest(opCode, 
							buff, 
							((MessageEvent) e).getRemoteAddress(), 
							e.getChannel());
					incr("RequestsCompleted");
				} catch (Throwable t) {
					incr("RequestsFailed");
					error("Failed to handle [", opCode, "]", t );
				}
				return;
			}
		}
		ctx.sendUpstream(e);
	}
	
	/** Logging handler */
	private static final LoggingHandler clientConnLogHandler = new LoggingHandler("org.helios.UDPAgentOperationRouter", InternalLogLevel.INFO, true);

	
	
	
	
	/**
	 * Returns the total number of agent operations received
	 * @return the total number of agent operations received
	 */
	@ManagedMetric(category="UDPOpRequests", metricType=MetricType.COUNTER, description="total number of agent operations received")
	public long getRequestsReceived() {
		return getMetricValue("RequestsReceived");
	}
	
	/**
	 * Returns the total number of agent operations completed
	 * @return the total number of agent operations completed
	 */
	@ManagedMetric(category="UDPOpRequests", metricType=MetricType.COUNTER, description="total number of agent operations completed")
	public long getRequestsCompleted() {
		return getMetricValue("RequestsCompleted");
	}
	
	/**
	 * Returns the total number of agent operations failed
	 * @return the total number of agent operations failed
	 */
	@ManagedMetric(category="UDPOpRequests", metricType=MetricType.COUNTER, description="total number of agent operations failed")
	public long getRequestsFailed() {
		return getMetricValue("RequestsFailed");
	}
	
	/**
	 * Returns the total number of timed out {@link OpCode#Who} requests
	 * @return the total number of timed out {@link OpCode#Who} requests
	 */
	@ManagedMetric(category="UDPOpRequests", metricType=MetricType.COUNTER, description="total number of timed out WHO requests")
	public long getTimedOutWhos() {
		return getMetricValue("TimedOutWhoOps");
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponent#getSupportedMetricNames()
	 */
	@Override
	public Set<String> getSupportedMetricNames() {
		Set<String> metrics = new HashSet<String>(super.getSupportedMetricNames());
		metrics.add("RequestsReceived");
		metrics.add("RequestsCompleted");
		metrics.add("RequestsFailed");
		metrics.add("TimedOutWhoOps");
		return metrics;
	}	
	
	
	
//	/**
//	 * Sets the channel group
//	 * @param channelGroup the injected channel group
//	 */
//	public void setChannelGroup(ManagedChannelGroup channelGroup) {
//		this.channelGroup = channelGroup;
//		for(AgentRequestHandler arh: handlers.values()) {
//			if(arh instanceof ChannelGroupAware) {
//				((ChannelGroupAware)arh).setChannelGroup(channelGroup);
//			}
//		}
//	}
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.handlers.AgentRequestHandler#processAgentRequest(org.helios.apmrouter.OpCode, org.jboss.netty.buffer.ChannelBuffer, java.net.SocketAddress, org.jboss.netty.channel.Channel)
	 */
	@Override
	public void processAgentRequest(OpCode opCode, ChannelBuffer buff, SocketAddress remoteAddress, Channel channel) {
		if(opCode==OpCode.WHO_RESPONSE) {			
			removePendingOp(remoteAddress, OpCode.WHO);
			buff.readByte();
			int hostLength = buff.readInt();
			byte[] hostBytes = new byte[hostLength];
			buff.readBytes(hostBytes);
			int agentLength = buff.readInt();
			byte[] agentBytes = new byte[agentLength];
			buff.readBytes(agentBytes);
			String host = new String(hostBytes);
			String agent = new String(agentBytes);
			DecoratedChannel dc = (DecoratedChannel) SharedChannelGroup.getInstance().getByRemote(remoteAddress);
			dc.setWho(host, agent);
			SharedChannelGroup.getInstance().sendIdentifiedChannelEvent(dc);
			
			info("Agent at [", remoteAddress, "] identified itself as [", host, "/", agent, "]");
		} else if(opCode==OpCode.HELLO) {
			getChannelForRemote(channel, remoteAddress);
			info("Agent at [", remoteAddress, "] sent HELLO");
			ChannelBuffer cb = ChannelBuffers.directBuffer(1);
			cb.writeByte(OpCode.HELLO_CONFIRM.op());
			channel.write(cb, remoteAddress);
			sendWho(channel, remoteAddress);
			
		} else if(opCode==OpCode.HELLO_CONFIRM) {
			warn("Received HELLO_CONFIRM ????");
		} else if(opCode==OpCode.JMX_MBS_INQUIRY_RESPONSE) {
			buff.readByte();
			int arrSize = buff.readInt();
			String[] domains = new String[arrSize];
			for(int i = 0; i < arrSize; i++) {
				byte[] bytes = new byte[buff.readInt()];
				buff.readBytes(bytes);
				domains[i] = new String(bytes);
			}
			DecoratedChannel dc = (DecoratedChannel) SharedChannelGroup.getInstance().getByRemote(remoteAddress);
			String agent = dc.getAgent();
			String host = dc.getHost();
			AgentMBeanServerConnectionFactory.registerRemoteMBeanServerConnections(channel, remoteAddress, host, agent, "udp", domains);
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.handlers.AgentRequestHandler#getHandledOpCodes()
	 */
	@Override
	public OpCode[] getHandledOpCodes() {
		return new OpCode[]{OpCode.WHO_RESPONSE, OpCode.BYE, OpCode.HELLO, OpCode.JMX_MBS_INQUIRY_RESPONSE};
	}
	
	/**
	 * <p>Callback when a {@link OpCode#WHO} op times out.
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.util.TimeoutListener#onTimeout(java.lang.Object, java.lang.Object)
	 */
	@Override
	public void onTimeout(String key, OpCode value) {
		incr("TimedOutWhoOps");
		debug("Who op timed out [", key, "]");		
	}
	
	/**
	 * <p>Specifies interest in {@link OpCode#WHO} ops only. 
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.util.ValueFilteredTimeoutListener#include(java.lang.Object)
	 */
	@Override
	public boolean include(OpCode value) {
		return OpCode.WHO==value;
	}


}
