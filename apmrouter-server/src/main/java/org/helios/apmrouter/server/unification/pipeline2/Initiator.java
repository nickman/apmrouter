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
package org.helios.apmrouter.server.unification.pipeline2;

import org.helios.apmrouter.server.unification.pipeline.PipelineModifier;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;

/**
 * <p>Title: Initiator</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.unification.pipeline2.Initiator</code></p>
 */

public interface Initiator {
	/**
	 * Returns the number of bytes requires by this initiator to determine the stream type
	 * @return the number of bytes requires by this initiator to determine the stream type
	 */
	public int requiredBytes();
	
	
	/**
	 * Tests this initiator to see if the initiating connection is a protocol match.
	 * @param buff The initial channel buffer passed on connect
	 * @return true for match, false otherwise
	 */
	public boolean match(ChannelBuffer buff);
	

	/**
	 * Modifies the passed pipeline to provide specific functionality after a successful protocol match
	 * @param ctx The channel handler context
	 * @param channel The current channel
	 * @param buffer  The initiating buffer
	 * @return The switch phase the protocol switch should transition to after this pipeline modification
	 */
	public SwitchPhase modifyPipeline(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer);
	
	
	/**
	 * Returns the initiator's name
	 * @return the initiator's name
	 */
	public String getName();
	
	
	

}
