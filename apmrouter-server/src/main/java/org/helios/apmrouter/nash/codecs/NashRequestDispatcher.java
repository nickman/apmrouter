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
package org.helios.apmrouter.nash.codecs;

import org.helios.apmrouter.nash.NashRequest;
import org.helios.apmrouter.nash.handler.NashRequestHandler;
import org.helios.apmrouter.nash.handler.RequestHandlerRegistry;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.logging.InternalLogger;
import org.jboss.netty.logging.InternalLoggerFactory;

/**
 * <p>Title: NashRequestDispatcher</p>
 * <p>Description: The top channel handler in the netty pipeline that receives a fully decoded {@link NashRequest} 
 * and executes the request against the determined target.</p> 
 * <p>Targets for directing nailgun requests ae driven by registered handlers that are routed to according to:<ul>
 * 	<li>The command name</li>
 * 	<li>Optionally, on the arguments to the command</li>
 * </ul>Futher filtering can be done by the handlers based on:<ul>
 * 
 * </ul>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.nash.codecs.NashRequestDispatcher</code></p>
 */

public class NashRequestDispatcher extends SimpleChannelUpstreamHandler {
	/** The internal logger */
	protected final InternalLogger log = InternalLoggerFactory.getInstance(getClass());

	
	/**
	 * Processes a {@link NashRequest}.
	 * The passed {@link MessageEvent} is assumed to contain the {@link NashRequest}. 
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#messageReceived(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
	 */
	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
		System.out.println("[" + Thread.currentThread().toString() + "] Processing NashRequest:\n" + e.getMessage());
		Object message = e.getMessage();
		if(message==null || !(message instanceof NashRequest)) {
			log.warn("RequestDispatcher received invalid message [" + message + "]");
		}
		NashRequest request = (NashRequest)message;
		NashRequestHandler handler = RequestHandlerRegistry.getInstance().lookup(request);
		if(handler==null) {
			request.err("No Command Handler for " + request.getCommand()+"\n").end();
		} else {
			handler.onNashRequest(request);
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#exceptionCaught(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ExceptionEvent)
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		log.error("Exception handling request dispatch in context [" + ctx + "]", e.getCause());
		//super.exceptionCaught(ctx, e);
	}
	
}
