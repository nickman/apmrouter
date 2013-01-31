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
package org.helios.apmrouter.byteman.sockets.impl;

/**
 * <p>Title: ServerSideSocket</p>
 * <p>Description: A container class for various bits-n-pieces regarding the server side socket of a remote incoming connection</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.byteman.sockets.impl.ServerSideSocket</code></p>
 */

public class ServerSideSocket {
	/** The listening server socket */
	protected final ISocketImpl serverSocketImpl;
	/** The socket representing the server side of an incoming connection */
	protected final ISocketImpl acceptedSocketImpl;
	/** A sigar netstat instance for tracking stats on this connection */
	
	/**
	 * Creates a new ServerSideSocket
	 * @param serverSocketImpl The listening server socket
	 * @param acceptedSocketImpl The socket representing the server side of an incoming connection
	 */
	public ServerSideSocket(ISocketImpl serverSocketImpl, ISocketImpl acceptedSocketImpl) {
		this.serverSocketImpl = serverSocketImpl;
		this.acceptedSocketImpl = acceptedSocketImpl;
	}
	
	
}
