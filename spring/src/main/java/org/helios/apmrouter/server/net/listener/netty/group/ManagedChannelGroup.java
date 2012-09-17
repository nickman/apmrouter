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
import java.util.Collection;
import java.util.Iterator;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;

/**
 * <p>Title: ManagedChannelGroup</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.net.listener.netty.ManagedChannelGroup</code></p>
 */

public class ManagedChannelGroup implements ChannelGroup {
	/** The delegate channel group */
	protected final ChannelGroup channelGroup;
	
	/**
	 * Creates a new ManagedChannelGroup
	 * @param name The name of this group
	 */
	public ManagedChannelGroup(String name) {
		channelGroup = new DefaultChannelGroup(name);
	}

	/**
	 * @param arg0
	 * @return
	 * @see java.util.Set#add(java.lang.Object)
	 */
	public boolean add(Channel arg0) {
		return channelGroup.add(arg0);
	}

	/**
	 * @param arg0
	 * @return
	 * @see java.util.Set#addAll(java.util.Collection)
	 */
	public boolean addAll(Collection<? extends Channel> arg0) {
		return channelGroup.addAll(arg0);
	}

	/**
	 * 
	 * @see java.util.Set#clear()
	 */
	public void clear() {
		channelGroup.clear();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.group.ChannelGroup#close()
	 */
	public ChannelGroupFuture close() {
		return channelGroup.close();
	}

	/**
	 * @param arg0
	 * @return
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(ChannelGroup arg0) {
		return channelGroup.compareTo(arg0);
	}

	/**
	 * @param arg0
	 * @return
	 * @see java.util.Set#contains(java.lang.Object)
	 */
	public boolean contains(Object arg0) {
		return channelGroup.contains(arg0);
	}

	/**
	 * @param arg0
	 * @return
	 * @see java.util.Set#containsAll(java.util.Collection)
	 */
	public boolean containsAll(Collection<?> arg0) {
		return channelGroup.containsAll(arg0);
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.group.ChannelGroup#disconnect()
	 */
	public ChannelGroupFuture disconnect() {
		return channelGroup.disconnect();
	}

	/**
	 * @param arg0
	 * @return
	 * @see java.util.Set#equals(java.lang.Object)
	 */
	public boolean equals(Object arg0) {
		return channelGroup.equals(arg0);
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.group.ChannelGroup#getName()
	 */
	public String getName() {
		return channelGroup.getName();
	}

	/**
	 * @param id
	 * @return
	 * @see org.jboss.netty.channel.group.ChannelGroup#find(java.lang.Integer)
	 */
	public Channel find(Integer id) {
		return channelGroup.find(id);
	}

	/**
	 * @return
	 * @see java.util.Set#hashCode()
	 */
	public int hashCode() {
		return channelGroup.hashCode();
	}

	/**
	 * @return
	 * @see java.util.Set#isEmpty()
	 */
	public boolean isEmpty() {
		return channelGroup.isEmpty();
	}

	/**
	 * @return
	 * @see java.util.Set#iterator()
	 */
	public Iterator<Channel> iterator() {
		return channelGroup.iterator();
	}

	/**
	 * @param arg0
	 * @return
	 * @see java.util.Set#remove(java.lang.Object)
	 */
	public boolean remove(Object arg0) {
		return channelGroup.remove(arg0);
	}

	/**
	 * @param arg0
	 * @return
	 * @see java.util.Set#removeAll(java.util.Collection)
	 */
	public boolean removeAll(Collection<?> arg0) {
		return channelGroup.removeAll(arg0);
	}

	/**
	 * @param arg0
	 * @return
	 * @see java.util.Set#retainAll(java.util.Collection)
	 */
	public boolean retainAll(Collection<?> arg0) {
		return channelGroup.retainAll(arg0);
	}

	/**
	 * @param interestOps
	 * @return
	 * @see org.jboss.netty.channel.group.ChannelGroup#setInterestOps(int)
	 */
	public ChannelGroupFuture setInterestOps(int interestOps) {
		return channelGroup.setInterestOps(interestOps);
	}

	/**
	 * @param readable
	 * @return
	 * @see org.jboss.netty.channel.group.ChannelGroup#setReadable(boolean)
	 */
	public ChannelGroupFuture setReadable(boolean readable) {
		return channelGroup.setReadable(readable);
	}

	/**
	 * @return
	 * @see java.util.Set#size()
	 */
	public int size() {
		return channelGroup.size();
	}

	/**
	 * @return
	 * @see java.util.Set#toArray()
	 */
	public Object[] toArray() {
		return channelGroup.toArray();
	}

	/**
	 * @param arg0
	 * @return
	 * @see java.util.Set#toArray(T[])
	 */
	public <T> T[] toArray(T[] arg0) {
		return channelGroup.toArray(arg0);
	}

	/**
	 * @param message
	 * @return
	 * @see org.jboss.netty.channel.group.ChannelGroup#write(java.lang.Object)
	 */
	public ChannelGroupFuture write(Object message) {
		return channelGroup.write(message);
	}

	/**
	 * @param message
	 * @param remoteAddress
	 * @return
	 * @see org.jboss.netty.channel.group.ChannelGroup#write(java.lang.Object, java.net.SocketAddress)
	 */
	public ChannelGroupFuture write(Object message, SocketAddress remoteAddress) {
		return channelGroup.write(message, remoteAddress);
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.group.ChannelGroup#unbind()
	 */
	public ChannelGroupFuture unbind() {
		return channelGroup.unbind();
	}
}
