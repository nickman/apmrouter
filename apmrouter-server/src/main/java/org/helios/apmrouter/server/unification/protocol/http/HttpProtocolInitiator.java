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
package org.helios.apmrouter.server.unification.protocol.http;

import org.helios.apmrouter.server.unification.pipeline.http.HttpPipelineModifier;
import org.helios.apmrouter.server.unification.protocol.AbstractProtocolInitiator;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>Title: HttpProtocolInitiator</p>
 * <p>Description: Protocol iniator for an HTTP connection.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.unification.protocol.http.HttpProtocolInitiator</code></p>
 */

public class HttpProtocolInitiator extends AbstractProtocolInitiator {
	/** The HTTP pipeline modifier */
	protected HttpPipelineModifier pipelineModifier = null;
	
	/**
	 * Creates a new HttpProtocolInitiator
	 */
	protected HttpProtocolInitiator() {
		super("http");
	}
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.unification.protocol.ProtocolInitiator#modifyPipeline(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.Channel, org.jboss.netty.buffer.ChannelBuffer)
	 */
	@Override
	public void modifyPipeline(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) {
		pipelineModifier.modifyPipeline(ctx, channel, buffer);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.unification.protocol.ProtocolInitiator#match(int, int)
	 */
	@Override
	public boolean match(int magic1, int magic2) {
		return isHttp(magic1, magic2);
	}	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.unification.protocol.AbstractProtocolInitiator#match(org.jboss.netty.buffer.ChannelBuffer)
	 */
	@Override
	public boolean match(ChannelBuffer buf) {
		return false;
	}
	
	/**
	 * Determines if the channel is carrying an HTTP request
	 * @param magic1 The first byte of the incoming request
	 * @param magic2 The second byte of the incoming request
	 * @return true if the incoming is HTTP, false otherwise
	 */
	private boolean isHttp(int magic1, int magic2) {
		 return
		 magic1 == 'G' && magic2 == 'E' || // GET
		 magic1 == 'P' && magic2 == 'O' || // POST
		 magic1 == 'P' && magic2 == 'U' || // PUT
		 magic1 == 'H' && magic2 == 'E' || // HEAD
		 magic1 == 'O' && magic2 == 'P' || // OPTIONS
		 magic1 == 'P' && magic2 == 'A' || // PATCH
		 magic1 == 'D' && magic2 == 'E' || // DELETE
		 magic1 == 'T' && magic2 == 'R' || // TRACE
		 magic1 == 'C' && magic2 == 'O';   // CONNECT
	}

	/**
	 * Sets the HTTP pipeline modifier
	 * @param pipelineModifier the HTTP pipeline modifier
	 */
	@Autowired(required=true)
	public void setPipelineModifier(HttpPipelineModifier pipelineModifier) {
		this.pipelineModifier = pipelineModifier;
	}
	

}
