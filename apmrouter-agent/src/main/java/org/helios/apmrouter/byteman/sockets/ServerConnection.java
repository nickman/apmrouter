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
package org.helios.apmrouter.byteman.sockets;

import java.net.InetSocketAddress;
import java.net.Socket;

import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.helios.apmrouter.byteman.sockets.impl.ISocketImpl;
import org.helios.apmrouter.collections.ConcurrentLongSlidingWindow;
import org.helios.apmrouter.nativex.APMSigar;
import org.hyperic.sigar.NetStat;

/**
 * <p>Title: ServerConnection</p>
 * <p>Description: A container representing an incoming connection to a tracked server socket.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.byteman.sockets.ServerConnection</code></p>
 */

public class ServerConnection implements NetStatConst {
	/** The time the connection was established */
	protected final long connectTime = System.currentTimeMillis();
	/** The server socket interface */
	protected final ISocketImpl serverSocket;
	/** The server socket reference key */
	protected final String key;
	/** A netstat instance for this connection */
	protected final NetStat netStat;
	/** Metric accumulator */
	protected final NonBlockingHashMap<String, ConcurrentLongSlidingWindow> stats; 
	
	/** A sigar reference to acquire netstats */
	protected static final APMSigar sigar;
	
	static {
		APMSigar tmp = null;
		try {
			tmp = APMSigar.getInstance();
		} catch (Exception ex) {
			tmp = null;
		}
		sigar = tmp;
	}
	
	/** A map of server connections keyed by the compond of the local and remote address:port */
	protected static final NonBlockingHashMap<String, ServerConnection> isockets = new NonBlockingHashMap<String, ServerConnection>(); 
	
	/**
	 * Acquires the ServerConnection instance for the passed ISocketImpl
	 * @param serverSocket the ISocketImpl to get the ServerConnection for.
	 * @return a ServerConnection wrapping the passed serverSocket
	 */
	public static ServerConnection getInstance(ISocketImpl serverSocket) {
		if(serverSocket==null) throw new IllegalArgumentException("The passed server socket was null", new Throwable());
		String key = key(serverSocket);
		isockets.putIfAbsent(key, new ServerConnection(key, serverSocket));
		return isockets.get(key);
	}
	
	/**
	 * Generates a unique reference key for the passed server connection
	 * @param serverSocket the server connection to generate a key for
	 * @return the key
	 */
	public static String key(ISocketImpl serverSocket) {
		if(serverSocket==null) throw new IllegalArgumentException("The passed server socket was null", new Throwable());
		Socket sock = serverSocket.getSocket();
		InetSocketAddress remoteSockAddr = (InetSocketAddress)sock.getRemoteSocketAddress(); 
		return new StringBuilder(sock.getLocalAddress().getHostAddress())
			.append("/").append(sock.getLocalPort())
			.append("/").append(remoteSockAddr.getHostString()).append("/").append(remoteSockAddr.getPort())
			.toString();
	}
	
	/**
	 * Creates a new ServerConnection
	 * @param serverSocket The tracked server connection socket
	 * @param key The designated key 
	 */
	private ServerConnection(String key, ISocketImpl serverSocket) {
		if(isockets.containsKey(key)) {
			this.serverSocket = null;
			this.key = null;
			this.netStat = null;
			this.stats = null;
			return;
		}
		this.serverSocket = serverSocket;
		this.key = key;
		this.netStat = getNetStatOrNull(serverSocket);
		if(netStat==null) {
			stats = null;
		} else {
			stats = new NonBlockingHashMap<String, ConcurrentLongSlidingWindow>(STAT_ALL.length);
			for(String name: STAT_ALL) {
				stats.put(name, new ConcurrentLongSlidingWindow(20));
			}
		}
		
	}
	
	/**
	 * Returns a netstat for the remote of the passed socket impl
	 * @param serverSocket the server socket
	 * @return a netstat or null if one could not be acquired 
	 */
	protected NetStat getNetStatOrNull(ISocketImpl serverSocket) {
		try {
			return sigar==null ? null : sigar.getNetStat(serverSocket.getSocket().getLocalAddress().getAddress(), serverSocket.getSocket().getLocalPort());
		} catch (Exception ex) {
			return null;
		}
	}

}
