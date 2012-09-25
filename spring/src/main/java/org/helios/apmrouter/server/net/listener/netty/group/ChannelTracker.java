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
package org.helios.apmrouter.server.net.listener.netty.group;


/**
 * <p>Title: ChannelTracker</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.net.listener.netty.group.ChannelTracker</code></p>
 */

public class ChannelTracker implements ChannelTrackerMBean {
	/** The managed channel */
	protected final ManagedChannel channel;

	/**
	 * Creates a new ChannelTracker
	 * @param channel the managed channel
	 */
	ChannelTracker(ManagedChannel channel) {
		super();
		this.channel = channel;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ChannelTrackerMBean#getName()
	 */
	@Override
	public String getName() {
		return channel.getName();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ChannelTrackerMBean#getType()
	 */
	@Override
	public String getType() {
		return channel.getType();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ChannelTrackerMBean#getId()
	 */
	@Override
	public int getId() {
		return channel.getId();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ChannelTrackerMBean#isOpen()
	 */
	@Override
	public boolean isOpen() {
		return channel.isOpen();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ChannelTrackerMBean#isBound()
	 */
	@Override
	public boolean isBound() {
		return channel.isBound();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ChannelTrackerMBean#isConnected()
	 */
	@Override
	public boolean isConnected() {
		return channel.isConnected();
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ChannelTrackerMBean#getLocalURI()
	 */
	@Override
	public String getLocalURI() {
		return channel.getLocalURI();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ChannelTrackerMBean#getRemoteURI()
	 */
	@Override
	public String getRemoteURI() {
		return channel.getRemoteURI();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ChannelTrackerMBean#isReadable()
	 */
	@Override
	public boolean isReadable() {
		return channel.isReadable();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ChannelTrackerMBean#isWritable()
	 */
	@Override
	public boolean isWritable() {
		return channel.isWritable();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ChannelTrackerMBean#isTcpNoDelay()
	 */
	@Override
	public boolean isTcpNoDelay() {
		return channel.isTcpNoDelay();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ChannelTrackerMBean#getSoLinger()
	 */
	@Override
	public int getSoLinger() {
		return channel.getSoLinger();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ChannelTrackerMBean#getInterestOps()
	 */
	@Override
	public int getInterestOps() {
		return channel.getInterestOps();
	}
	
	
}
