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

import java.util.Collection;
import java.util.EnumMap;

import org.helios.apmrouter.SenderOpCode;
import org.helios.apmrouter.server.ServerComponentBean;
import org.helios.apmrouter.server.net.listener.netty.handlers.AgentRequestHandler;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.MessageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;

/**
 * <p>Title: UDPAgentOperationRouter</p>
 * <p>Description: Routes agent requests to the correct service in accordance with the op-code in the request message</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.net.listener.netty.handlers.udp.UDPAgentOperationRouter</code></p>
 */

public class UDPAgentOperationRouter extends ServerComponentBean implements ChannelUpstreamHandler {
	
	/** A map of agent request handlers keyed by the opcode */
	protected final EnumMap<SenderOpCode, AgentRequestHandler> handlers = new EnumMap<SenderOpCode, AgentRequestHandler>(SenderOpCode.class);
	
	
	
	/**
	 * Sets the agent request handlers
	 * @param agentRequestHandlers a collection of agent request handlers
	 */
	@Autowired(required=true)
	public void setAgentRequestHandlers(Collection<AgentRequestHandler> agentRequestHandlers) {
		for(AgentRequestHandler arh: agentRequestHandlers) {
			for(SenderOpCode soc: arh.getHandledOpCodes()) {
				handlers.put(soc, arh);
				info("Added AgentRequestHandler [", arh.getClass().getSimpleName(), "] for Op [", soc , "]");
			}
		}
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
				ChannelBuffer buff = (ChannelBuffer)msg;
				incr("RequestsReceived");
				SenderOpCode opCode = SenderOpCode.valueOf(buff);
				try {
					handlers.get(opCode).processAgentRequest(opCode, buff, ((MessageEvent) e).getRemoteAddress(), e.getChannel());
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
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponent#getSupportedMetricNames()
	 */
	@Override
	public String[] getSupportedMetricNames() {		
		return new String[]{"RequestsReceived", "RequestsCompleted", "RequestsFailed"};
	}
	
	



}
