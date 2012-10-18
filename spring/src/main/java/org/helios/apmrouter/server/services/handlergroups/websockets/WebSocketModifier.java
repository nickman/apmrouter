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
package org.helios.apmrouter.server.services.handlergroups.websockets;

import java.util.HashSet;
import java.util.Set;

import org.helios.apmrouter.server.services.AbstractPipelineModifier;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;

/**
 * <p>Title: WebSocketModifier</p>
 * <p>Description: Pipeline modifier for websockets push</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.services.handlergroups.websockets.WebSocketModifier</code></p>
 */

public class WebSocketModifier extends AbstractPipelineModifier {
	/** The handler that this modifier adds at the end of the pipeline */
	protected final ChannelHandler handler = new WebSocketServerHandler();

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.services.AbstractPipelineModifier#doGetChannelHandler()
	 */
	@Override
	protected ChannelHandler doGetChannelHandler() {
		return handler;
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.services.AbstractPipelineModifier#doModifyPipeline(org.jboss.netty.channel.ChannelPipeline)
	 */
	@Override
	protected void doModifyPipeline(ChannelPipeline pipeline) {
		if(pipeline.get(name)==null) {
			pipeline.addLast(name, handler);
		}
	}

}