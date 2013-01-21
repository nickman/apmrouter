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

import java.net.InetAddress;
import java.net.SocketAddress;

import org.cliffc.high_scale_lib.Counter;
import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.cliffc.high_scale_lib.NonBlockingHashSet;

/**
 * <p>Title: ConnectionTracker</p>
 * <p>Description: Lightweight {@link ISocketTracker} that tracks simple TCP connectivity between the instrumented host and its clients and other hosts it connects to</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.byteman.sockets.impl.ConnectionTracker</code></p>
 */

public class ConnectionTracker extends EmptySocketTracker {
	/** The server socket signatures of bound listeners */
	protected final NonBlockingHashSet<String> boundListeners = new NonBlockingHashSet<String>(); 
	/** The number of connections bound to a listening server socket */
	protected final NonBlockingHashMap<String, NonBlockingHashMap<String, Counter> serverListenerConnections = new NonBlockingHashMap<String, Counter>(); 
	/**  */
	//protected final Counter serverListenerConnections = new Counter();

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.EmptySocketTracker#onConnect(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl, java.net.SocketAddress, int)
	 */
	@Override
	public void onConnect(ISocketImpl socketImpl, SocketAddress address, int timeout) {

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onConnect(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl, java.net.InetAddress, int)
	 */
	@Override
	public void onConnect(ISocketImpl socketImpl, InetAddress address, int timeout) {

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onConnect(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl, java.lang.String, int)
	 */
	@Override
	public void onConnect(ISocketImpl socketImpl, String host, int port) {

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onBind(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl, java.net.InetAddress, int)
	 */
	@Override
	public void onBind(ISocketImpl socketImpl, InetAddress host, int port) {
		boundListeners.add(new StringBuilder(socketImpl.getServerSocket().getInetAddress().getHostAddress()).append(":").append(port).toString());
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onListen(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl, int)
	 */
	@Override
	public void onListen(ISocketImpl socketImpl, int backlog) {

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onAccept(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl, java.lang.Object)
	 */
	@Override
	public void onAccept(ISocketImpl socketImpl, Object acceptedSocketImpl) {

	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onClose(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl)
	 */
	@Override
	public void onClose(ISocketImpl socketImpl) {

	}


}
