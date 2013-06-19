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
package org.helios.apmrouter.server.unification.pipeline2.encoding;

import org.helios.apmrouter.server.unification.pipeline2.Initiator;

/**
 * <p>Title: EncodingInitiator</p>
 * <p>Description: A netty channel handler that attempts to determine if the incoming traffic is encoded in a way that this initiator can supply a decoder for.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.unification.pipeline2.encoding.EncodingInitiator</code></p>
 */

public interface EncodingInitiator extends Initiator {
	/**
	 * This is true if the decoder requires the full payload to start decoding (e.g. the bzip2 decoder) or false if partial buffers can be streamed in to the decoder.
	 * When true, the switch decoder will aggregate all the incomng traffic and then send upstream.
	 * @return true for the full payload, false otherwise.
	 */
	public boolean requiresFullPayload();
}
