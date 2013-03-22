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

import java.util.Set;

import javax.management.MXBean;

/**
 * <p>Title: ManagedChannelGroupMXBean</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.net.listener.netty.group.ManagedChannelGroupMXBean</code></p>
 */
@MXBean
public interface ManagedChannelGroupMXBean {

	/**
	 * Returns the managed channels in this group
	 * @return the managed channels in this group
	 */
	//@ManagedAttribute
	public abstract Set<ChannelTracker> getChannels();

	/**
	 * 
	 * @see java.util.Set#clear()
	 */
	public abstract void clear();

	/**
	 * @return
	 * @see org.jboss.netty.channel.group.ChannelGroup#close()
	 */
	//public abstract ChannelGroupFuture close();

	/**
	 * @return
	 * @see org.jboss.netty.channel.group.ChannelGroup#disconnect()
	 */
	//public abstract ChannelGroupFuture disconnect();

	/**
	 * @return     String
	 * @see org.jboss.netty.channel.group.ChannelGroup#getName()
	 */
	public abstract String getName();

	/**
	 * @param id
	 * @return
	 * @see org.jboss.netty.channel.group.ChannelGroup#find(java.lang.Integer)
	 */
	//public abstract Channel find(Integer id);

	/**
	 * @return   boolean
	 * @see java.util.Set#isEmpty()
	 */
	public abstract boolean isEmpty();

	/**
	 * @return    int
	 * @see java.util.Set#size()
	 */
	public abstract int size();

	/**
	 * @return
	 * @see org.jboss.netty.channel.group.ChannelGroup#unbind()
	 */
	//public abstract ChannelGroupFuture unbind();

}