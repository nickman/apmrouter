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
package org.helios.apmrouter.sender.netty;

import org.helios.apmrouter.sender.AbstractSender;
import org.helios.apmrouter.trace.DirectMetricCollection;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelState;
import org.jboss.netty.channel.ChannelStateEvent;

import java.net.URI;

/**
 * <p>Title: TCPSender</p>
 * <p>Description: A Netty TCP sender implementation</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.sender.netty.TCPSender</code></p>
 */

public class TCPSender extends AbstractSender {
	
	public void doSendHello() {
		
	}

	/**
	 * Creates a new TCPSender
	 * @param serverURI The URI of the server to connect to
	 */
	public TCPSender(URI serverURI) {
		super(serverURI);
		// TODO Auto-generated constructor stub
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sender.AbstractSender#getInterestedChannelStates()
	 */
	@Override
	public ChannelState[] getInterestedChannelStates() {
		return new ChannelState[]{ChannelState.CONNECTED};
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sender.AbstractSender#onChannelStateEvent(boolean, org.jboss.netty.channel.ChannelStateEvent)
	 */
	@Override
	public void onChannelStateEvent(boolean upstream, ChannelStateEvent stateEvent) {

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sender.AbstractSender#getPipeline()
	 */
	@Override
	public ChannelPipeline getPipeline() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sender.AbstractSender#send(org.helios.apmrouter.trace.DirectMetricCollection)
	 */
	@Override
	public void send(DirectMetricCollection dcm) {
		// TODO Auto-generated method stub

	}

}
