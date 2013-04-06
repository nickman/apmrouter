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
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;

/**
 * <p>Title: StreamProcessorPipelineFactory</p>
 * <p>Description: Factory for channel pipelines and channels to create a stream pipe which will process I/O as follows:<ul>
 * 	<li>The {@link NashRequestDecoder} will process input from the nailgun client's STDIN 
 * and write STDIN buffers and upstream events to the channel handler context of the pipeline created by this factory.</li>
 *  <li>The client stream handler will specify channel handlers to be placed in the pipeline to decode the nailgun client STDIN so it
 *  can consume that stream.</li>
 * </ul>
 * In summary, nailgun client supplied STDIN goes in, the decoded data comes out. If the handler specifies no handlers, the handler will
 * simply be called back with straight channel buffers.
 * </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.nash.streams.StreamProcessorPipelineFactory</code></p>
 */

public class StreamProcessorPipelineFactory implements ChannelPipelineFactory, ChannelFactory {
	
	
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelFactory#newChannel(org.jboss.netty.channel.ChannelPipeline)
	 */
	@Override
	public Channel newChannel(ChannelPipeline pipeline) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelFactory#releaseExternalResources()
	 */
	@Override
	public void releaseExternalResources() {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelPipelineFactory#getPipeline()
	 */
	@Override
	public ChannelPipeline getPipeline() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
