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

import javax.management.MXBean;

/**
 * <p>Title: ChannelTrackerMBean</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.net.listener.netty.group.ChannelTrackerMBean</code></p>
 */
public interface ChannelTrackerMBean {

	/**
	 * @return
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ManagedChannel#getName()
	 */
	public abstract String getName();

	/**
	 * @return
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ManagedChannel#getType()
	 */
	public abstract String getType();

	/**
	 * @return
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ManagedChannel#getId()
	 */
	public abstract int getId();

	/**
	 * @return
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ManagedChannel#isOpen()
	 */
	public abstract boolean isOpen();

	/**
	 * @return
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ManagedChannel#isBound()
	 */
	public abstract boolean isBound();

	/**
	 * @return
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ManagedChannel#isConnected()
	 */
	public abstract boolean isConnected();

	/**
	 * @return
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ManagedChannel#getLocalURI()
	 */
	public abstract String getLocalURI();

	/**
	 * @return
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ManagedChannel#getRemoteURI()
	 */
	public abstract String getRemoteURI();

	/**
	 * @return
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ManagedChannel#isReadable()
	 */
	public abstract boolean isReadable();

	/**
	 * @return
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ManagedChannel#isWritable()
	 */
	public abstract boolean isWritable();

	/**
	 * @return
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ManagedChannel#isTcpNoDelay()
	 */
	public abstract boolean isTcpNoDelay();

	/**
	 * @return
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ManagedChannel#getSoLinger()
	 */
	public abstract int getSoLinger();

	/**
	 * @return
	 * @see org.helios.apmrouter.server.net.listener.netty.group.ManagedChannel#getInterestOps()
	 */
	public abstract int getInterestOps();

}