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
package org.helios.apmrouter.server.unification.pipeline;

import java.util.Set;

import org.helios.apmrouter.server.unification.protocol.ProtocolInitiator;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;

/**
 * <p>Title: PipelineModifier</p>
 * <p>Description: Defines a class that modifies a pipeline for a specific purpose</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.unification.pipeline.PipelineModifier</code></p>
 */
public interface PipelineModifier {
	/**
	 * Modifies the passed pipeline to provide specific functionality
	 * @param ctx The channel handler context
	 * @param channel The current channel
	 * @param buffer  The initiating buffer
	 */
	public void modifyPipeline(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer);
	
	/**
	 * Returns the name of this modifier
	 * @return the name of this modifier
	 */
	public String getBeanName();
	
	
	/**
	 * Returns the protocol implemented by this modifier.
	 * This protocol should match up with a {@link ProtocolInitiator}.
	 * @return the protocol name
	 */
	public String getProtocol();
}
