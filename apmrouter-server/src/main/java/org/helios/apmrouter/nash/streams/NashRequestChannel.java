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

import java.net.SocketAddress;

import org.helios.apmrouter.nash.NashRequest;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.local.LocalAddress;
import org.jboss.netty.channel.local.LocalServerChannel;

/**
 * <p>Title: NashRequestChannel</p>
 * <p>Description: A channel wrapper that provides a shortcut for passing a NashRequest from the decoder to a request handler without
 * serializing. </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.nash.streams.NashRequestChannel</code></p>
 */

public class NashRequestChannel implements LocalServerChannel {
	/** The delegate channel */
	private final LocalServerChannel innerChannel;
	/** The request to deliver to the request handler */
	private NashRequest request = null;

	/**
	 * Creates a new NashRequestChannel
	 * @param innerChannel The real channel that this wrapper delegates to 
	 */
	public NashRequestChannel(LocalServerChannel innerChannel) {
		this.innerChannel = innerChannel;
	}

	/**
	 * Returns the request to the request handler
	 * @return the request provided by the request decoder
	 */
	NashRequest getRequest() {
		return request;
	}

	/**
	 * Sets the nailgun request decoded by the nailgun decoder 
	 * @param request the request to deliver to the request handler
	 */
	void setRequest(NashRequest request) {
		this.request = request;
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.local.LocalServerChannel#getLocalAddress()
	 */
	public LocalAddress getLocalAddress() {
		return innerChannel.getLocalAddress();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.local.LocalServerChannel#getRemoteAddress()
	 */
	public LocalAddress getRemoteAddress() {
		return innerChannel.getRemoteAddress();
	}

	/**
	 * @param o
	 * @return
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Channel o) {
		return innerChannel.compareTo(o);
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#getId()
	 */
	public Integer getId() {
		return innerChannel.getId();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#getFactory()
	 */
	public ChannelFactory getFactory() {
		return innerChannel.getFactory();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#getParent()
	 */
	public Channel getParent() {
		return innerChannel.getParent();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#getConfig()
	 */
	public ChannelConfig getConfig() {
		return innerChannel.getConfig();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#getPipeline()
	 */
	public ChannelPipeline getPipeline() {
		return innerChannel.getPipeline();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#isOpen()
	 */
	public boolean isOpen() {
		return innerChannel.isOpen();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#isBound()
	 */
	public boolean isBound() {
		return innerChannel.isBound();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#isConnected()
	 */
	public boolean isConnected() {
		return innerChannel.isConnected();
	}

	/**
	 * @param message
	 * @return
	 * @see org.jboss.netty.channel.Channel#write(java.lang.Object)
	 */
	public ChannelFuture write(Object message) {
		return innerChannel.write(message);
	}

	/**
	 * @param message
	 * @param remoteAddress
	 * @return
	 * @see org.jboss.netty.channel.Channel#write(java.lang.Object, java.net.SocketAddress)
	 */
	public ChannelFuture write(Object message, SocketAddress remoteAddress) {
		return innerChannel.write(message, remoteAddress);
	}

	/**
	 * @param localAddress
	 * @return
	 * @see org.jboss.netty.channel.Channel#bind(java.net.SocketAddress)
	 */
	public ChannelFuture bind(SocketAddress localAddress) {
		return innerChannel.bind(localAddress);
	}

	/**
	 * @param remoteAddress
	 * @return
	 * @see org.jboss.netty.channel.Channel#connect(java.net.SocketAddress)
	 */
	public ChannelFuture connect(SocketAddress remoteAddress) {
		return innerChannel.connect(remoteAddress);
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#disconnect()
	 */
	public ChannelFuture disconnect() {
		return innerChannel.disconnect();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#unbind()
	 */
	public ChannelFuture unbind() {
		return innerChannel.unbind();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#close()
	 */
	public ChannelFuture close() {
		return innerChannel.close();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#getCloseFuture()
	 */
	public ChannelFuture getCloseFuture() {
		return innerChannel.getCloseFuture();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#getInterestOps()
	 */
	public int getInterestOps() {
		return innerChannel.getInterestOps();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#isReadable()
	 */
	public boolean isReadable() {
		return innerChannel.isReadable();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#isWritable()
	 */
	public boolean isWritable() {
		return innerChannel.isWritable();
	}

	/**
	 * @param interestOps
	 * @return
	 * @see org.jboss.netty.channel.Channel#setInterestOps(int)
	 */
	public ChannelFuture setInterestOps(int interestOps) {
		return innerChannel.setInterestOps(interestOps);
	}

	/**
	 * @param readable
	 * @return
	 * @see org.jboss.netty.channel.Channel#setReadable(boolean)
	 */
	public ChannelFuture setReadable(boolean readable) {
		return innerChannel.setReadable(readable);
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#getAttachment()
	 */
	@Override
	public Object getAttachment() {
		return innerChannel.getAttachment();
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#setAttachment(java.lang.Object)
	 */
	@Override
	public void setAttachment(Object attachment) {
		innerChannel.setAttachment(attachment); 
	}

}
