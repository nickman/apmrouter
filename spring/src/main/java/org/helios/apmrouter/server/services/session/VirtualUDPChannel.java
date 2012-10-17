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
package org.helios.apmrouter.server.services.session;

import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelLocal;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.Channels;

/**
 * <p>Title: VirtualUDPChannel</p>
 * <p>Description: Emulates a child channel for UDP, which of course does not exist, but we're mimicking a session</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.services.session.VirtualUDPChannel</code></p>
 */

public class VirtualUDPChannel implements Channel {
	/** Serial number generator */
	protected static final AtomicInteger serial = new AtomicInteger(0);
	/** The real parent channel */
	protected final Channel parent;
	/** The remote address of the client this channel represents */
	protected final SocketAddress remoteAddress;
	/** The virtual channel ID */
	protected final int id;
	/** Indicates if the channel is considered "open" */
	protected final AtomicBoolean connected = new AtomicBoolean(true);
	/** The virtual channel local */
	protected final ChannelLocal<Object> channelLocal;
	
	/**
	 * Creates a new VirtualUDPChannel
	 * @param parent The real parent channel 
	 * @param remoteAddress The remote address of the client this channel represents
	 */
	public VirtualUDPChannel(Channel parent, SocketAddress remoteAddress) {
		this.parent = parent;
		this.remoteAddress = remoteAddress;
		
		id = serial.decrementAndGet();
		if(id==Integer.MIN_VALUE) {
			synchronized(serial) {
				int i = serial.intValue();
				if(i == Integer.MIN_VALUE || i > 0) {
					serial.set(0);
				}
			}
		}
		channelLocal = new ChannelLocal<Object>();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#close()
	 */
	@Override
	public ChannelFuture close() {
		channelLocal.remove(this);		
		Channels.fireChannelClosed(this);
		return Channels.future(this);
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#getCloseFuture()
	 */
	@Override
	public ChannelFuture getCloseFuture() {
		return Channels.future(this);		
	}
	

	/**
	 * {@inheritDoc}
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */	
	@Override
	public int compareTo(Channel o) {
		return parent.compareTo(o);
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#getId()
	 */
	@Override
	public Integer getId() {
		return id;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#getFactory()
	 */
	@Override
	public ChannelFactory getFactory() {
		return parent.getFactory();
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#getParent()
	 */
	@Override
	public Channel getParent() {
		return parent;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#getConfig()
	 */
	@Override
	public ChannelConfig getConfig() {
		return parent.getConfig();
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#getPipeline()
	 */
	@Override
	public ChannelPipeline getPipeline() {
		return parent.getPipeline();
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#isOpen()
	 */
	@Override
	public boolean isOpen() {
		return connected.get();
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#isBound()
	 */
	@Override
	public boolean isBound() {
		return parent.isBound();
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#isConnected()
	 */
	@Override
	public boolean isConnected() {
		return connected.get();
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#getLocalAddress()
	 */
	@Override
	public SocketAddress getLocalAddress() {
		return parent.getLocalAddress();
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#getRemoteAddress()
	 */
	@Override
	public SocketAddress getRemoteAddress() {
		return remoteAddress;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#write(java.lang.Object)
	 */
	@Override
	public ChannelFuture write(Object message) {
		return parent.write(message, remoteAddress);
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#write(java.lang.Object, java.net.SocketAddress)
	 */
	@Override
	public ChannelFuture write(Object message, SocketAddress remoteAddress) {
		return parent.write(message, remoteAddress);
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#bind(java.net.SocketAddress)
	 */
	@Override
	public ChannelFuture bind(SocketAddress localAddress) {
		return parent.bind(localAddress);
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#connect(java.net.SocketAddress)
	 */
	@Override
	public ChannelFuture connect(SocketAddress remoteAddress) {
		return parent.connect(remoteAddress);
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#disconnect()
	 */
	@Override
	public ChannelFuture disconnect() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#unbind()
	 */
	@Override
	public ChannelFuture unbind() {
		return null;
	}


	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#getInterestOps()
	 */
	@Override
	public int getInterestOps() {
		return parent.getInterestOps();
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#isReadable()
	 */
	@Override
	public boolean isReadable() {
		return parent.isReadable();
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#isWritable()
	 */
	@Override
	public boolean isWritable() {
		return parent.isWritable();
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#setInterestOps(int)
	 */
	@Override
	public ChannelFuture setInterestOps(int interestOps) {
		return parent.setInterestOps(interestOps);
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#setReadable(boolean)
	 */
	@Override
	public ChannelFuture setReadable(boolean readable) {
		return parent.setReadable(readable);
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#getAttachment()
	 */
	@Override
	public Object getAttachment() {
		return channelLocal.get(this);
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#setAttachment(java.lang.Object)
	 */
	@Override
	public void setAttachment(Object attachment) {
		channelLocal.set(this, attachment);
	}
	
	
	
}
