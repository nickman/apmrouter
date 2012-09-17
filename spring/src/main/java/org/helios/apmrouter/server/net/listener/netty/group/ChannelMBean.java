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

import javax.management.MXBean;

/**
 * <p>Title: ChannelMBean</p>
 * <p>Description: An MXBean wrapper for channels registered in a {@link ManagedChannelGroup}.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.net.listener.netty.group.ChannelMBean</code></p>
 */
@MXBean
public interface ChannelMBean {
	
	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#getId()
	 */
	public Integer getId();



	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#isOpen()
	 */
	public boolean isOpen();

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#isBound()
	 */
	public boolean isBound();

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#isConnected()
	 */
	public boolean isConnected();

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#getLocalAddress()
	 */
	public SocketAddress getLocalAddress();

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#getRemoteAddress()
	 */
	public SocketAddress getRemoteAddress();

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#isReadable()
	 */
	public boolean isReadable();

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#isWritable()
	 */
	public boolean isWritable();
	
    /**
     * Gets the {@link StandardSocketOptions#TCP_NODELAY} option.
     */
    boolean isTcpNoDelay();
    
    /**
     * Gets the {@link StandardSocketOptions#SO_LINGER} option.
     */
    int getSoLinger();    
    
    
}
