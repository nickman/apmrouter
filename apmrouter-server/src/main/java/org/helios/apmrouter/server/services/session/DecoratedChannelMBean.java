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


/**
 * <p>Title: DecoratedChannelMBean</p>
 * <p>Description: MBean interface for the {@link DecoratedChannel}</p> 
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
	 * Returns the remote URI for this channel
	 * @return the remote URI for this channel
	 */
	public abstract String getURI();	

	/**
	 * Returns the stringified local address
	 * @return the stringified local address
	 */
	public abstract String getLocal();
	
	/**
	 * Returns the names of the channel handlers in the pipeline
	 * @return the names of the channel handlers in the pipeline
	 */
	public abstract String[] getPipelineNames();

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

	public abstract Integer getId();


	public abstract boolean isOpen();

	public abstract boolean isBound();

	public abstract boolean isConnected();

	public abstract boolean isReadable();

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