/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2012, Helios Development Group and individual contributors
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
package org.helios.apmrouter.jmx.mbeanserver;

import java.lang.reflect.Method;

import javax.management.Notification;

import org.helios.apmrouter.OpCode;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

/**
 * <p>Title: MBeanServerConnectionInvocationResponseHandler</p>
 * <p>Description: Netty handler that handles a JMX request response.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.jmx.mbeanserver.MBeanServerConnectionInvocationResponseHandler</code></p>
 * TODO: This will work for datagrams, but for TCP we will need a ReplayDecoder
 */

public class MBeanServerConnectionInvocationResponseHandler extends SimpleChannelUpstreamHandler {
	/** The connection admin to funnel synchronous request returns through */
	protected final MBeanServerConnectionAdmin connectionAdmin;
	
	/**
	 * Creates a new MBeanServerConnectionInvocationResponseHandler
	 * @param connectionAdmin The connection admin to funnel synchronous request returns through 
	 */
	public MBeanServerConnectionInvocationResponseHandler(MBeanServerConnectionAdmin connectionAdmin) {		
		this.connectionAdmin = connectionAdmin;
	}

	/**
	 * Handles a JMX response.
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#messageReceived(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
	 */
	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {				
		Object message = e.getMessage();
		if(message instanceof ChannelBuffer) {
			ChannelBuffer cb = (ChannelBuffer)message;
			byte op = cb.getByte(0);
			if(OpCode.JMX_RESPONSE.op()==op) {				
				int reqId=cb.getInt(1);
				AsynchJMXResponseListener waitingListener = AgentMBeanServerConnectionFactory.asynchTimeoutMap.remove(reqId);
				cb.skipBytes(5);  // skipping op code and request id.
				final byte methodId = cb.readByte();
				final int payloadSize = cb.readInt(); 
				final byte[] bytes = new byte[payloadSize];
				cb.readBytes(bytes);
				Object[] resp =AgentMBeanServerConnectionFactory.getInput(bytes);
				if(waitingListener!=null) {
					if(resp.length==1 && resp[0]!=null && resp[0] instanceof Throwable) {
						waitingListener.onException(reqId, (Throwable)resp[0]);
					} else {
						Method responseMethod = AgentMBeanServerConnectionFactory.keyToAsynchMethod.get(methodId);
						Object[] responseMethodParams = null;
						if(responseMethod.getParameterTypes().length==1) {
							responseMethodParams = new Object[]{reqId};
						} else {
							responseMethodParams = new Object[]{reqId, resp[0]};
						}
						responseMethod.invoke(waitingListener, responseMethodParams);
					}
				} else {
					connectionAdmin.onSynchronousResponse(reqId, resp.length==1 ? resp[0] : null);
				}
			} else if(OpCode.JMX_NOTIFICATION.op()==op) {
				// Notification Write Procedure
//				cb.writeByte(OpCode.JMX_NOTIFICATION.op());
//				cb.writeInt(requestId);
//				cb.writeInt(payload.length);
//				cb.writeBytes(payload);
				final int reqId=cb.readInt();
				final int payloadSize = cb.readInt();
				byte[] payload = new byte[payloadSize];
				Object[] notifValues = AgentMBeanServerConnectionFactory.getInput(payload);
				connectionAdmin.onNotification(reqId, (Notification)notifValues[0], notifValues[1]);
			}
		}				
		ctx.sendUpstream(e);
	}			

}
