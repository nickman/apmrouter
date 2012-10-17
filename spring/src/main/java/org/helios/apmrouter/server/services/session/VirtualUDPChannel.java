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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.helios.apmrouter.OpCode;
import org.helios.apmrouter.metric.AgentIdentity;
import org.helios.apmrouter.util.TimeoutListener;
import org.helios.apmrouter.util.TimeoutQueueMap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
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
 * FIXME: Ping related stuff should be configurable
 */

public class VirtualUDPChannel implements Channel, TimeoutListener<Integer, Integer> {
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
	/** The close future */
	protected final ChannelFuture myCloseFuture;
	// ===============================================================
	//   Ping related stuff
	// ===============================================================
	/** The ping timeout in ms. */
	protected long pingTimeout = 2000;
	/** The maximum number of consecutive ping fails */
	protected int maxPingFails = 3;
	/** The number of consecutive ping fails */
	protected final AtomicInteger consecutivePingFails = new AtomicInteger(0);
	/** The ping schedule handle (to cancel on close) */
	protected ScheduledFuture<?> pingSchedule;
	/** The pending ping requests */
	protected static final TimeoutQueueMap<Integer, Integer> timeoutMap = new TimeoutQueueMap<Integer, Integer>(5000);

	
	// ===============================================================
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
		myCloseFuture = Channels.future(this);
		channelLocal = new ChannelLocal<Object>();
		timeoutMap.addListener(this);
	}
	
	/**
	 * Sets the ping scheduler to be canceled on close
	 * @param pingSchedule The ping schedule handle (to cancel on close)
	 */
	public void setPingSchedule(ScheduledFuture<?> pingSchedule) {
		this.pingSchedule = pingSchedule;
	}
	
	/**
	 * <p>This callback occurs when a ping times out.
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.util.TimeoutListener#onTimeout(java.lang.Object, java.lang.Object)
	 */
	@Override
	public void onTimeout(Integer key, Integer value) {
		if(key.equals(getId())) {
			int timeouts = consecutivePingFails.incrementAndGet();
			if(timeouts>=maxPingFails) {
				close();
			}
		}
	}
	
	
	/**
	 * Sends a ping request to the agent at the end of this channel
	 */
	public void ping() {
		StringBuilder key = new StringBuilder();
		ChannelBuffer ping = encodePing(key);
		write(ping);
		timeoutMap.put(getId(), getId());
	}	
	
	/**
	 * Clears the current pending ping
	 */
	public void pingResponse() {
		timeoutMap.remove(getId());
	}

	/**
	 * Creates a ping channel buffer and appends the key to the passed buffer 
	 * @param key The buffer to place the key in
	 * @return the ping ChannelBuffer
	 */
	protected ChannelBuffer encodePing(final StringBuilder key) {
		String _key = new StringBuilder(AgentIdentity.ID.getHostName()).append("-").append(AgentIdentity.ID.getAgentName()).append("-").append(System.nanoTime()).toString();
		key.append(_key);
		byte[] bytes = _key.getBytes();
		ChannelBuffer ping = ChannelBuffers.buffer(1+4+bytes.length);
		ping.writeByte(OpCode.PING.op());
		ping.writeInt(bytes.length);
		ping.writeBytes(bytes);
		return ping;
	}

	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#close()
	 */
	@Override
	public ChannelFuture close() {
		pingSchedule.cancel(true);
		timeoutMap.removeListener(this);
		channelLocal.remove(this);		
		Channels.fireChannelClosed(this);
		myCloseFuture.setSuccess();
		return myCloseFuture;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#getCloseFuture()
	 */
	@Override
	public ChannelFuture getCloseFuture() {
		return myCloseFuture;
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
