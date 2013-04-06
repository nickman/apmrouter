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

import org.helios.apmrouter.nash.codecs.NashRequestDecoder;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelPipeline;

/**
 * <p>Title: StreamProcessorPipeline</p>
 * <p>Description: A stream processor that receives channel buffers from the {@link NashRequestDecoder} and processes
 * them in its own pipeline, delivering the decoded output to the request handler.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.nash.streams.StreamProcessorPipeline</code></p>
 */

public class StreamProcessorPipeline {
	/** The stream processor's pipeline */
	protected final ChannelPipeline pipeline;

	/**
	 * Creates a new StreamProcessorPipeline
	 * @param pipeline The pipeline that will process the stream
	 */
	public StreamProcessorPipeline(ChannelPipeline pipeline) {
		this.pipeline = pipeline;
	}

	/**
	 * Returns the stream processor's pipeline
	 * @return the stream processor's pipeline
	 */
	ChannelPipeline getPipeline() {
		return pipeline;
	}
	
	public void writeStdInChunk(ChannelBuffer channelBuffer) { 
		
		
	}
}
