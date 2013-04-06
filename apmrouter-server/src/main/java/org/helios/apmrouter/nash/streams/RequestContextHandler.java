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
package org.helios.apmrouter.nash.streams;

import org.helios.apmrouter.nash.NashRequest;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.SimpleChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;

/**
 * <p>Title: RequestContextHandler</p>
 * <p>Description: Attaches incoming {@link NashRequest}s to the channel handler context</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.nash.streams.RequestContextHandler</code></p>
 */
public class RequestContextHandler extends SimpleChannelDownstreamHandler {
	/** The listening channel in the ng input stream handler */
	protected final Channel listeningChannel;
	
	/**
	 * Creates a new RequestContextHandler
	 * @param listeningChannel The listening channel in the ng input stream handler
	 */
	public RequestContextHandler(Channel listeningChannel) {
		this.listeningChannel = listeningChannel;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelDownstreamHandler#handleDownstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	@Override
	public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		// TODO Auto-generated method stub
		
	}


}
