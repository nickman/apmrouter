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
import java.util.Date;

import org.helios.apmrouter.util.SystemClock;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;

/**
 * <p>Title: DecoratedChannel</p>
 * <p>Description: A netty channel wrapper and decorator</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.services.session.DecoratedChannel</code></p>
 */

public class DecoratedChannel implements Channel {
	/** The wrapped channel */
	protected final Channel delegate;
	/** The assigned channel name */
	protected final String name;
	/** The channel connect time */
	protected final long connectTime;
	/** The channel type */
	protected final ChannelType type;
	
	/**
	 * Creates a new DecoratedChannel
	 * @param channel The wrapped channel
	 * @param channelType The channel type
	 * @param channelName The assigned channel name
	 */
	protected DecoratedChannel(Channel channel, ChannelType channelType, String channelName) {
		delegate = channel;
		name = channelName + "#" + channel.getId();
		type = channelType;
		connectTime = SystemClock.time();
	}
	
	/**
	 * Returns the name of the channel type
	 * @return the name of the channel type
	 */
	public String getType() {
		return type.name();
	}
	
	/**
	 * Returns the channel type
	 * @return the channel type
	 */
	public ChannelType getChannelType() {
		return type;
	}
	
	/**
	 * Returns the stringified remote address
	 * @return the stringified remote address
	 */
	public String getRemote() {
		SocketAddress sa = getRemoteAddress();
		return sa==null ? "" : sa.toString();
	}
	
	/**
	 * Returns the stringified local address
	 * @return the stringified local address
	 */
	public String getLocal() {
		SocketAddress sa = getLocalAddress();
		return sa==null ? "" : sa.toString();
	}
	
	
	/**
	 * Returns the channels's name
	 * @return the channels's name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Returns the UTC long connect timestamp
	 * @return the UTC long connect timestamp
	 */
	public long getConnectTime() {
		return connectTime;		
	}
	
	/**
	 * Returns the connect date
	 * @return the connect date
	 */
	public Date getConnectDate() {
		return new Date(connectTime);
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Channel o) {
		return delegate.compareTo(o);
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#getId()
	 */
	@Override
	public Integer getId() {
		return delegate.getId();
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#getFactory()
	 */
	@Override
	public ChannelFactory getFactory() {
		return delegate.getFactory();
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#getParent()
	 */
	@Override
	public Channel getParent() {
		return delegate.getParent();
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#getConfig()
	 */
	@Override
	public ChannelConfig getConfig() {
		return delegate.getConfig();
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#getPipeline()
	 */
	@Override
	public ChannelPipeline getPipeline() {
		return delegate.getPipeline();
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#isOpen()
	 */
	@Override
	public boolean isOpen() {
		return delegate.isOpen();
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#isBound()
	 */
	@Override
	public boolean isBound() {
		return delegate.isBound();
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#isConnected()
	 */
	@Override
	public boolean isConnected() {
		return delegate.isConnected();
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#getLocalAddress()
	 */
	@Override
	public SocketAddress getLocalAddress() {
		return delegate.getLocalAddress();
	}


	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#getRemoteAddress()
	 */
	@Override
	public SocketAddress getRemoteAddress() {
		return delegate.getRemoteAddress();
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#write(java.lang.Object)
	 */
	@Override
	public ChannelFuture write(Object message) {
		return delegate.write(message);
	}


	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#write(java.lang.Object, java.net.SocketAddress)
	 */
	@Override
	public ChannelFuture write(Object message, SocketAddress remoteAddress) {
		return delegate.write(message, remoteAddress);
	}


	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#bind(java.net.SocketAddress)
	 */
	@Override
	public ChannelFuture bind(SocketAddress localAddress) {
		return delegate.bind(localAddress);
	}


	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#connect(java.net.SocketAddress)
	 */
	@Override
	public ChannelFuture connect(SocketAddress remoteAddress) {
		return delegate.connect(remoteAddress);
	}


	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#disconnect()
	 */
	@Override
	public ChannelFuture disconnect() {
		return delegate.disconnect();
	}


	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#unbind()
	 */
	@Override
	public ChannelFuture unbind() {
		return delegate.unbind();
	}


	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#close()
	 */
	@Override
	public ChannelFuture close() {
		return delegate.close();
	}


	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#getCloseFuture()
	 */
	@Override
	public ChannelFuture getCloseFuture() {
		return delegate.getCloseFuture();
	}


	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#getInterestOps()
	 */
	@Override
	public int getInterestOps() {
		return delegate.getInterestOps();
	}


	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#isReadable()
	 */
	@Override
	public boolean isReadable() {
		return delegate.isReadable();
	}


	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#isWritable()
	 */
	@Override
	public boolean isWritable() {
		return delegate.isWritable();
	}


	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#setInterestOps(int)
	 */
	@Override
	public ChannelFuture setInterestOps(int interestOps) {
		return delegate.setInterestOps(interestOps);
	}


	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#setReadable(boolean)
	 */
	@Override
	public ChannelFuture setReadable(boolean readable) {
		return delegate.setReadable(readable);
	}


	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#getAttachment()
	 */
	@Override
	public Object getAttachment() {
		return delegate.getAttachment();
	}


	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#setAttachment(java.lang.Object)
	 */
	@Override
	public void setAttachment(Object attachment) {
		delegate.setAttachment(attachment);
	}
}
