/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2012, Helios Development Group and individual contributors
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

/**
 * <p>Title: DecoratedChannelMBean</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.services.session.DecoratedChannelMBean</code></p>
 */

public interface DecoratedChannelMBean {

	/**
	 * Returns the name of the channel type
	 * @return the name of the channel type
	 */
	public abstract String getType();

	/**
	 * Returns the stringified remote address
	 * @return the stringified remote address
	 */
	public abstract String getRemote();

	/**
	 * Returns the stringified local address
	 * @return the stringified local address
	 */
	public abstract String getLocal();

	/**
	 * Returns the channels's name
	 * @return the channels's name
	 */
	public abstract String getName();

	/**
	 * Returns the UTC long connect timestamp
	 * @return the UTC long connect timestamp
	 */
	public abstract long getConnectTime();

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#getId()
	 */
	public abstract Integer getId();

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#isOpen()
	 */
	public abstract boolean isOpen();

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#isBound()
	 */
	public abstract boolean isBound();

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#isConnected()
	 */
	public abstract boolean isConnected();

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#isReadable()
	 */
	public abstract boolean isReadable();

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#isWritable()
	 */
	public abstract boolean isWritable();
	
	/**
	 * Returns the host of this agent connection
	 * @return the host of this agent connection
	 */
	public String getHost();

	/**
	 * Returns the agent of this agent connection
	 * @return the host of this agent connection
	 */
	public String getAgent();
	

}