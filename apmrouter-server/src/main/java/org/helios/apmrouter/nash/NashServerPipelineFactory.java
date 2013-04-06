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
package org.helios.apmrouter.nash;

import static org.jboss.netty.channel.Channels.pipeline;

import org.helios.apmrouter.nash.codecs.NashRequestDecoder;
import org.helios.apmrouter.nash.codecs.NashRequestDispatcher;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLogLevel;

/**
 * <p>Title: NashServerPipelineFactory</p>
 * <p>Description: A server pipeline factory for the nash server</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.nash.NashServerPipelineFactory</code></p>
 */

public class NashServerPipelineFactory implements ChannelPipelineFactory {
	/** The request dispatcher */
	protected final NashRequestDispatcher requestDispatcher = new NashRequestDispatcher();
	/** The request executor which hands off the request to be processed by another thread */
	protected final ExecutionHandler executionHandler = new ExecutionHandler(
            new OrderedMemoryAwareThreadPoolExecutor(16, 1048576, 1048576));
	/** The shareable nash request decoder */
	protected final NashRequestDecoder requestDecoder = new NashRequestDecoder();
	/** The shareable response encoder */
	protected final StringEncoder responseEncoder = new StringEncoder();
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelPipelineFactory#getPipeline()
	 */
	@Override
	public ChannelPipeline getPipeline() {
		ChannelPipeline pipeline = pipeline();
		pipeline.addLast("nash-logger", new LoggingHandler(InternalLogLevel.INFO, true));
		//pipeline.addLast("nash-logger", new LoggingHandler(InternalLogLevel.INFO, false));
		//pipeline.addLast("nash-logger", new LoggingHandler(InternalLogLevel.INFO, false));
		//pipeline.addLast("response-encoder", responseEncoder);
		pipeline.addLast("nash-decoder", requestDecoder);
		pipeline.addLast("nash-executor", executionHandler);
		pipeline.addLast("nash-dispatcher", requestDispatcher);
		return pipeline;
	}

}
