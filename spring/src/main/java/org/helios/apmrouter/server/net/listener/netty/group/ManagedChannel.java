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

import java.net.SocketAddress;
import java.net.URI;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.socket.SocketChannel;

/**
 * <p>Title: ManagedChannel</p>
 * <p>Description: A JMX managed netty channel</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.net.listener.netty.group.ManagedChannel</code></p>
 */

public class ManagedChannel implements Channel {
	/** The managed channel */
	private final Channel channel;
	/** The managed channel name */
	private final String name;
	
	/**
	 * Creates a new ManagedChannel
	 * @param channel The managed channel
	 * @param name The managed channel name
	 */
	public ManagedChannel(Channel channel, String name) {
		this.channel = channel;
		this.name = name;
	}

	/**
	 * Creates a new ManagedChannel
	 * @param channel The managed channel
	 */
	public ManagedChannel(Channel channel) {
		this.channel = channel;
		this.name = new StringBuilder(getClass().getSimpleName()).append(":").append(channel.getRemoteAddress()).toString();
	}

	/**
	 * @return
	 */
	public String getName() {
		return name;
	}

	public String getType() {
		return channel.getClass().getSimpleName();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ManagedChannelMBean#getId()
	 */
	@Override
	public Integer getId() {
		return channel.getId();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ManagedChannelMBean#isOpen()
	 */
	@Override
	public boolean isOpen() {
		return channel.isOpen();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ManagedChannelMBean#isBound()
	 */
	@Override
	public boolean isBound() {
		return channel.isBound();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ManagedChannelMBean#isConnected()
	 */
	@Override
	public boolean isConnected() {
		return channel.isConnected();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ManagedChannelMBean#getLocalAddress()
	 */
	public SocketAddress getLocalAddress() {		
		return channel.getLocalAddress();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ManagedChannelMBean#getRemoteAddress()
	 */
	public SocketAddress getRemoteAddress() {
		return channel.getRemoteAddress();
	}

	/**
	 * @return
	 */
	public String getLocalURI() {
		SocketAddress sa = channel.getLocalAddress();
		if(sa==null) return null;
		return sa.toString();

	}

	/**
	 * @return
	 */
	public String getRemoteURI() {
		SocketAddress sa = channel.getRemoteAddress();
		if(sa==null) return null;
		return sa.toString();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ManagedChannelMBean#isReadable()
	 */
	@Override
	public boolean isReadable() {
		return channel.isReadable();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ManagedChannelMBean#isWritable()
	 */
	@Override
	public boolean isWritable() {
		return channel.isWritable();
	}

	/**
	 * @return
	 */
	public boolean isTcpNoDelay() {
		if(channel instanceof SocketChannel) {
			return ((SocketChannel)channel).getConfig().isTcpNoDelay();
		}
		return false;
	}

	/**
	 * @return
	 */
	public int getSoLinger() {
		if(channel instanceof SocketChannel) {
			return ((SocketChannel)channel).getConfig().getSoLinger();
		}
		return -1;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Channel otherChannel) {
		return channel.compareTo(otherChannel);
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#getFactory()
	 */
	public ChannelFactory getFactory() {
		return channel.getFactory();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#getParent()
	 */
	public Channel getParent() {
		return channel.getParent();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#getConfig()
	 */
	public ChannelConfig getConfig() {
		return channel.getConfig();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#getPipeline()
	 */
	public ChannelPipeline getPipeline() {
		return channel.getPipeline();
	}

	/**
	 * @param message
	 * @return
	 * @see org.jboss.netty.channel.Channel#write(java.lang.Object)
	 */
	public ChannelFuture write(Object message) {
		return channel.write(message);
	}

	/**
	 * @param message
	 * @param remoteAddress
	 * @return
	 * @see org.jboss.netty.channel.Channel#write(java.lang.Object, java.net.SocketAddress)
	 */
	public ChannelFuture write(Object message, SocketAddress remoteAddress) {
		return channel.write(message, remoteAddress);
	}

	/**
	 * @param localAddress
	 * @return
	 * @see org.jboss.netty.channel.Channel#bind(java.net.SocketAddress)
	 */
	public ChannelFuture bind(SocketAddress localAddress) {
		return channel.bind(localAddress);
	}

	/**
	 * @param remoteAddress
	 * @return
	 * @see org.jboss.netty.channel.Channel#connect(java.net.SocketAddress)
	 */
	public ChannelFuture connect(SocketAddress remoteAddress) {
		return channel.connect(remoteAddress);
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#disconnect()
	 */
	public ChannelFuture disconnect() {
		return channel.disconnect();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#unbind()
	 */
	public ChannelFuture unbind() {
		return channel.unbind();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#close()
	 */
	public ChannelFuture close() {
		return channel.close();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#getCloseFuture()
	 */
	public ChannelFuture getCloseFuture() {
		return channel.getCloseFuture();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#getInterestOps()
	 */
	public int getInterestOps() {
		return channel.getInterestOps();
	}

	/**
	 * @param interestOps
	 * @return
	 * @see org.jboss.netty.channel.Channel#setInterestOps(int)
	 */
	public ChannelFuture setInterestOps(int interestOps) {
		return channel.setInterestOps(interestOps);
	}

	/**
	 * @param readable
	 * @return
	 * @see org.jboss.netty.channel.Channel#setReadable(boolean)
	 */
	public ChannelFuture setReadable(boolean readable) {
		return channel.setReadable(readable);
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#getAttachment()
	 */
	public Object getAttachment() {
		return channel.getAttachment();
	}

	/**
	 * @param attachment
	 * @see org.jboss.netty.channel.Channel#setAttachment(java.lang.Object)
	 */
	public void setAttachment(Object attachment) {
		channel.setAttachment(attachment);
	}


}
