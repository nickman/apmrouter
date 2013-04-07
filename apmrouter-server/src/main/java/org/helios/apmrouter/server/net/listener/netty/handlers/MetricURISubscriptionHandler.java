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

import org.helios.apmrouter.OpCode;
import org.helios.apmrouter.dataservice.json.JsonResponse;
import org.helios.apmrouter.dataservice.json.catalog.MetricURI;
import org.helios.apmrouter.dataservice.json.catalog.MetricURISubscriptionService;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;

/**
 * <p>Title: MetricURISubscriptionHandler</p>
 * <p>Description: Agent request handler for managing {@link MetricURI} subscriptions for remote agents.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.net.listener.netty.handlers.MetricURISubscriptionHandler</code></p>
 */

public class MetricURISubscriptionHandler extends AbstractAgentRequestHandler {
	/** The OpCodes handled by this handler */
	protected static final OpCode[] HANDLED_OP_CODES = new OpCode[]{OpCode.METRIC_URI_SUBSCRIBE, OpCode.METRIC_URI_UNSUBSCRIBE};
	/** The metric uri subscription service */
	protected MetricURISubscriptionService subService = null;
	
	/**
	 * Returns the total number of subscription requests received
	 * @return the total number of subscription requests received
	 */
	@ManagedMetric(category="MetricURISubscriptionHandler", displayName="SubRequests", metricType=MetricType.COUNTER, description="The total number of subscription requests received")
	public long getTotalSubRequests() {
		return getMetricValue("SubRequests");
	}

	/**
	 * Returns the total number of unsubscription requests received
	 * @return the total number of unsubscription requests received
	 */
	@ManagedMetric(category="MetricURISubscriptionHandler", displayName="UnSubRequests", metricType=MetricType.COUNTER, description="The total number of unsubscription requests received")
	public long getTotalUnSubRequests() {
		return getMetricValue("UnSubRequests");
	}

	/**
	 * Creates a new MetricURISubscriptionHandler
	 */
	public MetricURISubscriptionHandler() {
		super();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.handlers.AgentRequestHandler#processAgentRequest(org.helios.apmrouter.OpCode, org.jboss.netty.buffer.ChannelBuffer, java.net.SocketAddress, org.jboss.netty.channel.Channel)
	 */
	@Override
	public void processAgentRequest(OpCode opCode, ChannelBuffer buff, SocketAddress remoteAddress, Channel channel) {
		buff.skipBytes(1);
		switch(opCode) {
		case METRIC_URI_SUBSCRIBE:
			incr("SubRequests");
			subscribe(buff, getChannelForRemote(channel, remoteAddress) , true);
			break;
		case METRIC_URI_UNSUBSCRIBE:
			incr("UnSubRequests");
			subscribe(buff, getChannelForRemote(channel, remoteAddress) , false);
			break;
		default:
			break;
		}

	}
	
	/**
	 * Subscribes or unsubscribes the passed channel to a {@link MetricURI}
	 * @param buff The buffer containing a {@link OpCode#METRIC_URI_SUBSCRIBE} or {@link OpCode#METRIC_URI_UNSUBSCRIBE} request.
	 * @param channel The subscribing channel
	 * @param sub true for a subscribe, false for an unsubscribe
	 */
	protected void subscribe(ChannelBuffer buff, Channel channel, boolean sub) {
		try {
			final long rid = buff.readLong();
			int uriLength = buff.readInt();
			byte[] uriBytes = new  byte[uriLength];
			buff.readBytes(uriBytes);
			String uri = new String(uriBytes);
			JsonResponse responder = new JsonResponse(rid, "sub");
			final ChannelBuffer response = ChannelBuffers.buffer(10); // opCode(1) + success(1) + rid(8) = 10					
			if(sub) {
				response.writeByte(OpCode.METRIC_URI_SUB_CONFIRM.op());
				try {
					subService.subscribeMetricURI(MetricURI.getMetricURI(uri), responder, channel);
					response.writeByte(1);
				} catch (Exception ex) {
					response.writeByte(0);
				}				
			} else {
				response.writeByte(OpCode.METRIC_URI_UNSUB_CONFIRM.op());
				try {
					subService.cancelMetricURISubscription(MetricURI.getMetricURI(uri), responder, channel);
					response.writeByte(1);
				} catch (Exception ex) {
					response.writeByte(0);
				}
			}
			response.writeLong(rid);
			channel.write(response);
		} catch (Exception ex) {
			warn("Failed to subscribe to MetricURI from channel [", channel, "]", ex);
		}
	}
	
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.handlers.AgentRequestHandler#getHandledOpCodes()
	 */
	@Override
	public OpCode[] getHandledOpCodes() {
		return HANDLED_OP_CODES;
	}
	
	/**
	 * Sets the metric uri subscription service
	 * @param subService the metric uri subscription service
	 */
	@Autowired(required=true)
	public void setSubService(MetricURISubscriptionService subService) {
		this.subService = subService;
	}
	

}
