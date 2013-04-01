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

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.helios.apmrouter.OpCode;
import org.helios.apmrouter.util.SystemClock;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;

import com.sun.jmx.mbeanserver.DefaultMXBeanMappingFactory;
import com.sun.jmx.mbeanserver.MXBeanMapping;

/**
 * <p>Title: DecoratedChannel</p>
 * <p>Description: A netty channel wrapper and decorator</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.services.session.DecoratedChannel</code></p>
 */

public class DecoratedChannel implements Channel, DecoratedChannelMBean, Serializable {
	/**  */
	private static final long serialVersionUID = -4593645879241044163L;
	/** The wrapped channel */
	protected final Channel delegate;
	/** The assigned channel name */
	protected final String name;
	/** The channel connect time */
	protected final long connectTime;
	/** The channel type */
	protected final ChannelType type;
	/** The host (if an agent's connection) */
	protected String host = null;
	/** The agent (if an agent's connection) */
	protected String agent = null;
	/** A mapping factory to create open-type datas for decorated channels */
	protected static final MXBeanMapping mapping; 
	
	static {
		try {
			mapping = DefaultMXBeanMappingFactory.DEFAULT.mappingForType(DecoratedChannelMBean.class, DefaultMXBeanMappingFactory.DEFAULT);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
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
	 * Replaces this object with an opentype when being serialized
	 * @return an open type data object representing this channel
	 * @throws ObjectStreamException
	 */
	private Object writeReplace() throws ObjectStreamException {
		try {
			return mapping.toOpenValue(this);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Updates the host and agent
	 * @param host the agent host
	 * @param agent the agent name
	 */
	public void setWho(String host, String agent) {
		this.host = host;
		this.agent = agent;
	}
	
	/**
	 * Returns the names of the channel handlers in the pipeline
	 * @return the names of the channel handlers in the pipeline
	 */
	@Override
	public String[] getPipelineNames() {
		Map<String, ChannelHandler> copy = new LinkedHashMap<String, ChannelHandler>(delegate.getPipeline().toMap());
		String[] names = new String[copy.size()];
		int cnt = 0;
		for(Map.Entry<String, ChannelHandler> entry: copy.entrySet()) {
			names[cnt] = entry.getKey() + " [" + entry.getValue().getClass().getSimpleName() + "]";
			cnt++;
		}
		return names; 
	}
	
	/**
	 * Sends a {@link OpCode#WHO} to an unidentified agent in Whoville.
	 */
	public void sendWho() {
		byte[] bytes = delegate.getRemoteAddress().toString().getBytes();
		ChannelBuffer cb = ChannelBuffers.directBuffer(bytes.length+5);
		cb.writeByte(OpCode.WHO.op());
		cb.writeInt(bytes.length);
		cb.writeBytes(bytes);
		delegate.write(cb, delegate.getRemoteAddress());
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.services.session.DecoratedChannelMBean#getType()
	 */
	@Override
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
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.services.session.DecoratedChannelMBean#getURI()
	 */
	@Override
	public String getURI() {		
		if(type.name().startsWith("UDP")) {
			if(getRemoteAddress()==null) return "udp://" ; 
			return "udp://" + ((InetSocketAddress)getRemoteAddress()).getAddress().getHostAddress() + ":" + ((InetSocketAddress)getRemoteAddress()).getPort(); 
		} else if(type.name().startsWith("TCP")) {
			if(getRemoteAddress()==null) return "tcp://" ; 
			return "tcp://" + ((InetSocketAddress)getRemoteAddress()).getAddress().getHostAddress() + ":" + ((InetSocketAddress)getRemoteAddress()).getPort(); 
		} else if(type.name().startsWith("WEBSOCKET")) {
			if(getRemoteAddress()==null) return "ws://" ; 
			return "ws://" + ((InetSocketAddress)getRemoteAddress()).getAddress().getHostAddress() + ":" + ((InetSocketAddress)getRemoteAddress()).getPort();			
		} else {
			return "unknown";
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.services.session.DecoratedChannelMBean#getRemote()
	 */
	@Override
	public String getRemote() {
		SocketAddress sa = getRemoteAddress();
		return sa==null ? "" : sa.toString();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.services.session.DecoratedChannelMBean#getLocal()
	 */
	@Override
	public String getLocal() {
		SocketAddress sa = getLocalAddress();
		return sa==null ? "" : sa.toString();
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.services.session.DecoratedChannelMBean#getName()
	 */
	@Override
	public String getName() {
		return name;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.services.session.DecoratedChannelMBean#getConnectTime()
	 */
	@Override
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
	 * @see org.helios.apmrouter.server.services.session.DecoratedChannelMBean#getId()
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
	 * @see org.helios.apmrouter.server.services.session.DecoratedChannelMBean#isOpen()
	 */
	@Override
	public boolean isOpen() {
		return delegate.isOpen();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.services.session.DecoratedChannelMBean#isBound()
	 */
	@Override
	public boolean isBound() {
		return delegate.isBound();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.services.session.DecoratedChannelMBean#isConnected()
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
	 * @see org.helios.apmrouter.server.services.session.DecoratedChannelMBean#isReadable()
	 */
	@Override
	public boolean isReadable() {
		return delegate.isReadable();
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.services.session.DecoratedChannelMBean#isWritable()
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
	
	/**
	 * Returns the host of this agent connection
	 * @return the host of this agent connection
	 */
	@Override
	public String getHost() {
		return host;
	}

	/**
	 * Returns the agent of this agent connection
	 * @return the host of this agent connection
	 */
	@Override
	public String getAgent() {
		return agent;
	}
	

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((delegate == null) ? 0 : delegate.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DecoratedChannel other = (DecoratedChannel) obj;
		if (delegate == null) {
			if (other.delegate != null)
				return false;
		} else if (!delegate.equals(other.delegate))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (type != other.type)
			return false;
		return true;
	}

	/**
	 * Returns the 
	 * @return the delegate
	 */
	public Channel getDelegate() {
		return delegate;
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DecoratedChannel [name=");
		builder.append(name);
		builder.append(", connectTime=");
		builder.append(connectTime);
		builder.append(", type=");
		builder.append(type);
		builder.append(", host=");
		builder.append(host);
		builder.append(", agent=");
		builder.append(agent);
		builder.append("]");
		return builder.toString();
	}

}
