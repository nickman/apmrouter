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

import org.jboss.netty.buffer.ChannelBuffer;

/**
 * <p>Title: Initiator</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.unification.pipeline2.Initiator</code></p>
 */

public interface Initiator {
	
	/** The pipeline name for the execution handler */
	public static final String EXEC_HANDLER_NAME = "exec";
	
	/**
	 * Returns the number of bytes requires by this initiator to determine the stream type.
	 * Returning a value of <b><code>0</code></b> or less means this value is undetermined,
	 * meaning that the initiator will be attempted in each protocol negotiation loop until
	 * a match is made or the maximum number of negotiation bytes has been exceeded.
	 * @return the number of bytes requires by this initiator to determine the stream type
	 */
	public int requiredBytes();
	
	
	/**
	 * Tests this initiator to see if the initiating connection is a protocol match.
	 * @param buff The initial channel buffer passed on connect
	 * @return The object representing the match key, or null if no match was found.
	 * Note that the key does not necessarilly need to have any special significance,
	 * except for a null value which means no match was found.
	 */
	public Object match(ChannelBuffer buff);
	

	/**
	 * Modifies the passed pipeline to provide specific functionality after a successful protocol match
	 * @param context The context representing the state of protocol switching in the decoder
	 * @param matchKey An arbitrary key provided when the {@link #match(ChannelBuffer)} method matched the content.
	 * The object may have specific significance, except that it should not be null, since that would imply a match miss.
	 * @return The switch phase the protocol switch should transition to after this pipeline modification
	 */
	public SwitchPhase process(ProtocolSwitchContext context, Object matchKey);
	
	/**
	 * This is true if the decoder requires the full payload to start decoding (e.g. the bzip2 decoder) or false if partial buffers can be streamed in to the decoder.
	 * When true, the switch decoder will aggregate all the incomng traffic and then send upstream.
	 * @return true for the full payload, false otherwise.
	 */
	public boolean requiresFullPayload();	
	
	
	/**
	 * Returns the initiator's name
	 * @return the initiator's name
	 */
	public String getName();
	
	
	

}
