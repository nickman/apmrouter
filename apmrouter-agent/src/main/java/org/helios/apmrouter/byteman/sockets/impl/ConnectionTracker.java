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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import org.cliffc.high_scale_lib.Counter;
import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.cliffc.high_scale_lib.NonBlockingHashSet;
import org.helios.apmrouter.util.SimpleLogger;

/**
 * <p>Title: ConnectionTracker</p>
 * <p>Description: Lightweight {@link ISocketTracker} that tracks simple TCP connectivity between the instrumented host and its clients and other hosts it connects to</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.byteman.sockets.impl.ConnectionTracker</code></p>
 */

public class ConnectionTracker extends EmptySocketTracker implements Runnable {
	/** The number of connections bound to a listening server socket */
	protected final NonBlockingHashMap<SocketAddress, NonBlockingHashMap<SocketAddress, Counter>> serverListenerConnections = new NonBlockingHashMap<SocketAddress, NonBlockingHashMap<SocketAddress, Counter>>();
	/** A hashset of server side sockets */
	protected final NonBlockingHashSet<ISocketImpl> serverSideSockets = new NonBlockingHashSet<ISocketImpl>();

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
		serverListenerConnections.put(socketImpl.getServerSocket().getLocalSocketAddress(), new NonBlockingHashMap<SocketAddress, Counter>());
	}


	/**
	 * <p>Increments the count of connections from the remote host of the passed accepted socket to the passed server socket.</p> 
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.EmptySocketTracker#onAccept(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl, org.helios.apmrouter.byteman.sockets.impl.ISocketImpl)
	 */
	@Override
	public void onAccept(ISocketImpl socketImpl, ISocketImpl acceptedSocketImpl) {
		serverSideSockets.add(acceptedSocketImpl);
		SocketAddress sa = acceptedSocketImpl.getSocket().getRemoteSocketAddress();
		NonBlockingHashMap<SocketAddress, Counter> clientSockMap = serverListenerConnections.get(socketImpl.getServerSocket().getLocalSocketAddress());
		Counter counter = new Counter();
		Counter tmpCounter = clientSockMap.get(sa);
		if(tmpCounter!=null) {
			counter = tmpCounter;
		}
		counter.increment();		
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onClose(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl)
	 */
	@Override
	public void onClose(ISocketImpl socketImpl) {
		if(socketImpl.getServerSocket()!=null) {
			onClose(socketImpl.getServerSocket());
		} else {
			Socket sock = socketImpl.getSocket();
			SocketAddress remoteSocketAddress = sock.getRemoteSocketAddress();
			SocketAddress localSocketAddress = sock.getLocalSocketAddress();
			if(serverSideSockets.remove(socketImpl)) {
				// the closed socket is a server side (accepted) socket
				// decrement the connection count for the server socket
				try {
					serverListenerConnections.get(localSocketAddress).get(remoteSocketAddress).decrement();
				} catch (NullPointerException npe) {
					System.err.println("Failed to decrement for remote disconnect on [" + localSocketAddress + "]");
				}
			} else {
				// the closed socket is a client socket 
			}
		}
	}
	
	/**
	 * Called when a remote client socket closes
	 * @param socket the closed remote socket
	 */
	protected void onCloseRemote(Socket socket) {
//		SocketAddress sa = socket.
//		NonBlockingHashMap<SocketAddress, Counter> clientSockMap = serverListenerConnections.get(socketImpl.getServerSocket().getLocalSocketAddress());
//		Counter counter = new Counter();
//		Counter tmpCounter = clientSockMap.get(sa);
//		if(tmpCounter!=null) {
//			counter = tmpCounter;
//		}
//		counter.increment();		
		
	}
	
	/**
	 * Called when a local client socket closes
	 * @param socket the closed local socket
	 */
	protected void onCloseLocal(Socket socket) {
		
	}
	
	
	/**
	 * Called when a formerly bound and listening server socket closes
	 * @param serverSocket the closed server socket
	 */
	protected void onClose(ServerSocket serverSocket) {
		
	}

	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#requiresHarvester()
	 */
	@Override
	public boolean requiresHarvester() {
		return true;
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.EmptySocketTracker#harvest()
	 */
	@Override
	protected void harvest() {
		
	}
	

}
