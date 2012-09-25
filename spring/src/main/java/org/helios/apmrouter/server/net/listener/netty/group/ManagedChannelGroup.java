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

import java.lang.management.ManagementFactory;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.ObjectName;

import org.helios.apmrouter.jmx.JMXHelper;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;


/**
 * <p>Title: ManagedChannelGroup</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.net.listener.netty.group.ManagedChannelGroup</code></p>
 */
public class ManagedChannelGroup implements ChannelGroup, ManagedChannelGroupMXBean {
	private static final Map<String, ManagedChannelGroup> groups = new ConcurrentHashMap<String, ManagedChannelGroup>();
	
	/**
	 * @param name
	 * @return
	 */
	public static final ManagedChannelGroup getInstance(String name) {
		ManagedChannelGroup mcg = groups.get(name);
		if(mcg==null) {
			synchronized(groups) {
				mcg = groups.get(name);
				if(mcg==null) {
					mcg = new ManagedChannelGroup(name);
				}
			}
		}
		return mcg;
	}
	
	/** The delegate channel group */
	protected final ChannelGroup channelGroup;

	/**
	 * Creates a new ManagedChannelGroup
	 * @param name The name of this group
	 */
	private ManagedChannelGroup(String name) {
		channelGroup = new DefaultChannelGroup(name);
		ObjectName on = JMXHelper.objectName("org.helios.apmrouter.server:service=ChannelGroup,name=" + name);
		try {
			ManagementFactory.getPlatformMBeanServer().registerMBean(this, on);
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
	
	
	/**
	 * Finds the first channel in this group that has the passed remote address
	 * @param address The remote address to find
	 * @return the first matching channel or null if one was not found
	 */
	public Channel findRemote(SocketAddress address) {
		if(address==null) return null;
		for(Channel channel: channelGroup) {
			if(address.equals(channel.getRemoteAddress())) return channel;			
		}
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ManagedChannelGroupMXBean#getManagedChannels()
	 */
	@Override
	//@ManagedAttribute
	public Set<ChannelTracker> getChannels() {
		Set<ChannelTracker>  set = new HashSet<ChannelTracker>(size());
		for(ManagedChannel mc: toArray(new ManagedChannel[0])) {
			set.add(new ChannelTracker(mc));
		}
		return set;
	}
	
	/**
	 * Adds a channel
	 * @param channel The channel to add
	 * @return true if the channel was not in the group before the add
	 * @see java.util.Set#add(java.lang.Object)
	 */
	@Override
	public boolean add(Channel channel) {
		return channelGroup.add(channel instanceof ManagedChannel ? channel : new ManagedChannel(channel));
	}
	
	/**
	 * Adds a named channel
	 * @parma name The name of this channel
	 * @param channel The channel to add
	 * @return true if the channel was not in the group before the add
	 * @see java.util.Set#add(java.lang.Object)
	 */
	public boolean add(Channel channel, String name) {
		return channelGroup.add(channel instanceof ManagedChannel ? 
				channel : 
				new ManagedChannel(channel, name)
		);
	}
	

	/**
	 * @param arg0
	 * @return
	 * @see java.util.Set#addAll(java.util.Collection)
	 */
	@Override
	public boolean addAll(Collection<? extends Channel> channels) {
		boolean changed = false;
		for(Channel channel: channels) {
			if(add(channel)) changed = true;
		}
		return true;
	}
	



	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ManagedChannelGroupMXBean#clear()
	 */
	@Override
	public void clear() {
		channelGroup.clear();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ManagedChannelGroupMXBean#close()
	 */
	@Override
	public ChannelGroupFuture close() {
		return channelGroup.close();
	}

	/**
	 * @param arg0
	 * @return
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(ChannelGroup arg0) {
		return channelGroup.compareTo(arg0);
	}

	/**
	 * @param arg0
	 * @return
	 * @see java.util.Set#contains(java.lang.Object)
	 */
	@Override
	public boolean contains(Object arg0) {
		return channelGroup.contains(arg0);
	}

	/**
	 * @param arg0
	 * @return
	 * @see java.util.Set#containsAll(java.util.Collection)
	 */
	@Override
	public boolean containsAll(Collection<?> arg0) {
		return channelGroup.containsAll(arg0);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ManagedChannelGroupMXBean#disconnect()
	 */
	@Override
	public ChannelGroupFuture disconnect() {
		return channelGroup.disconnect();
	}

	/**
	 * @param arg0
	 * @return
	 * @see java.util.Set#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object arg0) {
		return channelGroup.equals(arg0);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ManagedChannelGroupMXBean#getName()
	 */
	@Override
	public String getName() {
		return channelGroup.getName();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ManagedChannelGroupMXBean#find(java.lang.Integer)
	 */
	@Override
	public Channel find(Integer id) {
		return channelGroup.find(id);
	}
	

	/**
	 * @return
	 * @see java.util.Set#hashCode()
	 */
	@Override
	public int hashCode() {
		return channelGroup.hashCode();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ManagedChannelGroupMXBean#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		return channelGroup.isEmpty();
	}

	/**
	 * @return
	 * @see java.util.Set#iterator()
	 */
	@Override
	public Iterator<Channel> iterator() {
		return channelGroup.iterator();
	}

	/**
	 * @param arg0
	 * @return
	 * @see java.util.Set#remove(java.lang.Object)
	 */
	@Override
	public boolean remove(Object arg0) {
		return channelGroup.remove(arg0);
	}

	/**
	 * @param arg0
	 * @return
	 * @see java.util.Set#removeAll(java.util.Collection)
	 */
	@Override
	public boolean removeAll(Collection<?> arg0) {
		return channelGroup.removeAll(arg0);
	}

	/**
	 * @param arg0
	 * @return
	 * @see java.util.Set#retainAll(java.util.Collection)
	 */
	@Override
	public boolean retainAll(Collection<?> arg0) {
		return channelGroup.retainAll(arg0);
	}

	/**
	 * @param interestOps
	 * @return
	 * @see org.jboss.netty.channel.group.ChannelGroup#setInterestOps(int)
	 */
	@Override
	public ChannelGroupFuture setInterestOps(int interestOps) {
		return channelGroup.setInterestOps(interestOps);
	}

	/**
	 * @param readable
	 * @return
	 * @see org.jboss.netty.channel.group.ChannelGroup#setReadable(boolean)
	 */
	@Override
	public ChannelGroupFuture setReadable(boolean readable) {
		return channelGroup.setReadable(readable);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ManagedChannelGroupMXBean#size()
	 */
	@Override
	public int size() {
		return channelGroup.size();
	}

	/**
	 * @return
	 * @see java.util.Set#toArray()
	 */
	@Override
	public Object[] toArray() {
		return channelGroup.toArray();
	}

	/**
	 * @param arg0
	 * @return
	 * @see java.util.Set#toArray(T[])
	 */
	@Override
	public <T> T[] toArray(T[] arg0) {
		return channelGroup.toArray(arg0);
	}

	/**
	 * @param message
	 * @return
	 * @see org.jboss.netty.channel.group.ChannelGroup#write(java.lang.Object)
	 */
	@Override
	public ChannelGroupFuture write(Object message) {
		return channelGroup.write(message);
	}

	/**
	 * @param message
	 * @param remoteAddress
	 * @return
	 * @see org.jboss.netty.channel.group.ChannelGroup#write(java.lang.Object, java.net.SocketAddress)
	 */
	@Override
	public ChannelGroupFuture write(Object message, SocketAddress remoteAddress) {
		return channelGroup.write(message, remoteAddress);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ManagedChannelGroupMXBean#unbind()
	 */
	@Override
	public ChannelGroupFuture unbind() {
		return channelGroup.unbind();
	}
}
