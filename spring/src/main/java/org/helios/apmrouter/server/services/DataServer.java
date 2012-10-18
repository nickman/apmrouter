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
package org.helios.apmrouter.server.services;

import org.helios.apmrouter.server.net.listener.netty.TCPAgentListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.springframework.beans.factory.annotation.Autowired;
/**
 * <p>Title: DataServer</p>
 * <p>Description: The multi-protocol data-server for fetching and subscribing to metric-data</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.services.DataServer</code></p>
 */
public class DataServer extends TCPAgentListener  {
	/** The data server pipeline factory */
	protected ServerPipelineFactory pipelineFactory;

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.BaseAgentListener#getPipeline()
	 */
	@Override
	public ChannelPipeline getPipeline() throws Exception {
		return pipelineFactory.getPipeline();
	}

	/**
	 * Sets the data server's delegate pipeline factory
	 * @param pipelineFactory the delegate pipeline factory to set
	 */
	@Autowired(required=true)
	public void setPipelineFactory(ServerPipelineFactory pipelineFactory) {
		this.pipelineFactory = pipelineFactory;
	}

}
