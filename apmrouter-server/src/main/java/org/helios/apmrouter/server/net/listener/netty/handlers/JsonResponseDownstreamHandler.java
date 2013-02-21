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
package org.helios.apmrouter.server.net.listener.netty.handlers;

import static org.jboss.netty.channel.Channels.write;

import org.helios.apmrouter.OpCode;
import org.helios.apmrouter.dataservice.json.JsonResponse;
import org.helios.apmrouter.dataservice.json.marshalling.JSONMarshaller;
import org.helios.apmrouter.server.ServerComponentBean;
import org.helios.apmrouter.server.services.session.SharedChannelGroup;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;

/**
 * <p>Title: JsonResponseDownstreamHandler</p>
 * <p>Description: Downstream channel handler to marshall {@link JsonResponse} instances being sent to agents</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.net.listener.netty.handlers.JsonResponseDownstreamHandler</code></p>
 */

public class JsonResponseDownstreamHandler extends ServerComponentBean implements ChannelDownstreamHandler {
	/** The JSON marshaller */
	protected JSONMarshaller marshaller = null;
	/**
	 * Creates a new JsonResponseDownstreamHandler
	 */
	public JsonResponseDownstreamHandler() {

	}
	
	/**
	 * Returns the total number of json responses sent to agents
	 * @return the total number of json responses sent to agents
	 */
	@ManagedMetric(category="ResponseMarshaller", displayName="ResponsesSent", metricType=MetricType.COUNTER, description="The total number of json responses sent to agents")
	public long getTotalResponsesSent() {
		return getMetricValue("ResponsesSent");
	}
	
	/**
	 * Returns the total number of marshalling exceptions
	 * @return the total number of marshalling exceptions
	 */
	@ManagedMetric(category="ResponseMarshaller", displayName="MarshallingExceptions", metricType=MetricType.COUNTER, description="The total number of marshalling exceptions")
	public long getMarshallingExceptionCount() {
		return getMetricValue("MarshallingExceptions");
	}
	
	
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelDownstreamHandler#handleDownstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent evt) throws Exception {
        if (!(evt instanceof MessageEvent)) {
            ctx.sendDownstream(evt);
            return;
        }
        MessageEvent e = (MessageEvent) evt;
        if (!encode(ctx, ctx.getChannel(), e, e.getMessage())) {
            ctx.sendDownstream(e);
        }
		
	}

	/**
	 * Attempts to encode the passed message event object and write it downstream.
	 * @param ctx The channel handler context
	 * @param channel The channel
	 * @param evt The channel event
	 * @param msg The payload message
	 * @return true if the object was successfully encoded, false otherwise
	 * @throws Exception thrown on encoding errors
	 */
	protected boolean encode(ChannelHandlerContext ctx, Channel channel, MessageEvent evt, Object msg) throws Exception {
		if(msg!=null && msg instanceof JsonResponse) {
			SharedChannelGroup.getInstance().find(channel.getId());
			JsonResponse response = (JsonResponse)msg;
			OpCode op = response.getOpCode();
			write(ctx, evt.getFuture(), marshaller.marshallToChannel(op, response), evt.getRemoteAddress());
			incr("ResponsesSent");
			return true;		
		}
		return false;
	}
	
	/**
	 * Handler exception handler
	 * @param ctx The context of the handler that failed
	 * @param ex the thrown exception
	 */
	public void exceptionCaught(ChannelHandlerContext ctx, Exception ex) {
		error("Unexpected exception on channel [", ctx.getChannel(), "]", ex);
		incr("MarshallingExceptions");
	}


	/**
	 * Sets the JSON marshaller
	 * @param marshaller the JSON marshaller
	 */
	@Autowired(required=true)
	public void setMarshaller(JSONMarshaller marshaller) {
		this.marshaller = marshaller;
	}

}
