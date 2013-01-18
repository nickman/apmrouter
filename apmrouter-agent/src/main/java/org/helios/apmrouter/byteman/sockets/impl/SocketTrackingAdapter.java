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

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import static org.helios.apmrouter.util.SimpleLogger.*;

/**
 * <p>Title: SocketTrackingAdapter</p>
 * <p>Description: A static template class acting as a receiver for events emitted from instrumented socket implementations</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.byteman.sockets.impl.SocketTrackingAdapter</code></p>
 */



public class SocketTrackingAdapter {
	
	/** The socket tracking adapters for socket impls */
	public static final Map<String, String> SOCKET_IMPL_ADAPTERS;
	
	static {
		Map<String, String> methodMap = new HashMap<String, String>();
		methodMap.put("connect.(Ljava/lang/String;I)V","org.helios.apmrouter.byteman.sockets.impl.SocketTrackingAdapter.onConnect($0, $$);");
		methodMap.put("connect.(Ljava/net/InetAddress;I)V","org.helios.apmrouter.byteman.sockets.impl.SocketTrackingAdapter.onConnect($0, $$);");
		methodMap.put("connect.(Ljava/net/SocketAddress;I)V","org.helios.apmrouter.byteman.sockets.impl.SocketTrackingAdapter.onConnect($0, $$);");
		methodMap.put("bind.(Ljava/net/InetAddress;I)V","org.helios.apmrouter.byteman.sockets.impl.SocketTrackingAdapter.onBind($0, $$);");
		methodMap.put("listen.(I)V","org.helios.apmrouter.byteman.sockets.impl.SocketTrackingAdapter.onListen($0, $$);");
		methodMap.put("accept.(Ljava/net/SocketImpl;)V","org.helios.apmrouter.byteman.sockets.impl.SocketTrackingAdapter.onAccept($0, $$);");
		methodMap.put("getInputStream.()Ljava/io/InputStream;","org.helios.apmrouter.byteman.sockets.impl.SocketTrackingAdapter.onGetInputStream($0, $_);");
		methodMap.put("getOutputStream.()Ljava/io/OutputStream;","org.helios.apmrouter.byteman.sockets.impl.SocketTrackingAdapter.onGetOutputStream($0, $_);");
		methodMap.put("available.()I","org.helios.apmrouter.byteman.sockets.impl.SocketTrackingAdapter.onAvailable($0, $_);");
		methodMap.put("close.()V","org.helios.apmrouter.byteman.sockets.impl.SocketTrackingAdapter.onClose($0);");
		methodMap.put("shutdownInput.()V","org.helios.apmrouter.byteman.sockets.impl.SocketTrackingAdapter.onShutdownInput($0);");
		methodMap.put("shutdownOutput.()V","org.helios.apmrouter.byteman.sockets.impl.SocketTrackingAdapter.onShutdownOutput($0);");
		methodMap.put("sendUrgentData.(I)V","org.helios.apmrouter.byteman.sockets.impl.SocketTrackingAdapter.onSendUrgentData($0, $$);");
		methodMap.put("setSocket.(Ljava/net/Socket;)V","org.helios.apmrouter.byteman.sockets.impl.SocketTrackingAdapter.onSetSocket($0, $$);");
		methodMap.put("setServerSocket.(Ljava/net/ServerSocket;)V","org.helios.apmrouter.byteman.sockets.impl.SocketTrackingAdapter.onSetServerSocket($0, $$);");
		methodMap.put("reset.()V","org.helios.apmrouter.byteman.sockets.impl.SocketTrackingAdapter.onReset($0);");
		methodMap.put("setPerformancePreferences.(III)V","org.helios.apmrouter.byteman.sockets.impl.SocketTrackingAdapter.onSetPerformancePreferences($0, $$);");
		SOCKET_IMPL_ADAPTERS = Collections.unmodifiableMap(methodMap);
	}

	/**
	 * Called on a socket impl connect. This is the only <i>actual</i> connect execution. The overloads are redirected here.
	 * @param socketImpl the socket impl that connected
	 * @param address the socket address that the socket impl connected to
	 * @param timeout the timeout used for connect
	 */
	public static void onConnect(Object socketImpl, SocketAddress address, int timeout) {
		info("Connected (sa) [", address, ":", timeout, "]");
	}
	
	/**
	 * Called on a socket impl connect
	 * @param socketImpl the socket impl that connected
	 * @param address the inet address that the socket impl connected to
	 * @param timeout the timeout used for connect
	 */
	public static void onConnect(Object socketImpl, InetAddress address, int timeout) {
		info("Connected (ia) [", address, ":", timeout, "]");
	}
	
	/**
	 * Called on a socket impl connect
	 * @param socketImpl the socket impl that connected
	 * @param host the host name that the socket impl connected to
	 * @param port the port that the socket impl connected to
	 */
	public static void onConnect(Object socketImpl, String host, int port) {
		info("Connected (ha) [", host, ":", port, "]");
	}
	
	/**
	 * Called when a socket impl binds to a socket
	 * @param socketImpl the socket impl that was bound 
	 * @param host the host of the bound socket
	 * @param port the port of the bound socket
	 */
	public static void onBind(Object socketImpl, InetAddress host, int port) {
		
	}
	
	/**
	 * Called when a socket impl has its backlog queue set
	 * @param socketImpl the socket impl that had its backlog queue set
	 * @param backlog the connection backlog
	 */
	public static void onListen(Object socketImpl, int backlog) {
		
	}
	
	/**
	 * Called when a server socket impl accepts a new connection 
	 * @param socketImpl the socket impl that accepted
	 * @param acceptedSocketImpl the accepted client socket impl
	 */
	public static void onAccept(Object socketImpl, Object acceptedSocketImpl) {
		
	}
	
	/**
	 * Called when the input stream is requested from a socket impl
	 * @param socketImpl the socket impl
	 * @param inputStream the returned input stream
	 */
	public static void onGetInputStream(Object socketImpl, InputStream inputStream) {
		
	}

	/**
	 * Called when the output stream is requested from a socket impl
	 * @param socketImpl the socket impl
	 * @param outputStream the returned output stream
	 */
	public static void onGetOutputStream(Object socketImpl, OutputStream outputStream) {
		
	}
	
	/**
	 * Called when the available bytes is requested from a socket impl
	 * @param socketImpl the socket impl
	 * @param available the available bytes returned
	 */
	public static void  onAvailable(Object socketImpl, int available) {
		
	}

	/**
	 * Called when a socket impl is closed
	 * @param socketImpl the socket impl
	 */
	public static void  onClose(Object socketImpl) {
		
	}

	/**
	 * Called when input is shutdown on a socket impl
	 * @param socketImpl the socket impl
	 */
	public static void onShutdownInput(Object socketImpl)  {
      
    }
	
	/**
	 * Called when output is shutdown on a socket impl
	 * @param socketImpl the socket impl
	 */
	public static void onShutdownOutput(Object socketImpl)  {
	      
    }
	
	/**
	 * Called when urgent data is sent through a socket impl
	 * @param socketImpl the socket impl
	 * @param data the data that was sent
	 */
	public static void onSendUrgentData(Object socketImpl, int data)  {
		
	}
	
	/**
	 * Called when the client socket is set on a socket impl
	 * @param socketImpl the socket impl
	 * @param socket the set socket
	 */
	public static void onSetSocket(Object socketImpl, Object socket) {
		
	}

	/**
	 * Called when the server socket is set on a socket impl
	 * @param socketImpl the socket impl
	 * @param serverSocket The set server socket
	 */
	public static void onSetServerSocket(Object socketImpl, Object serverSocket) {
		
	}
	
	/**
	 * Called when a socket impl is reset
	 * @param socketImpl the socket impl
	 */
	public static void onReset(Object socketImpl) {
		
	}
	
	
	/**
	 * Called when performance preferences are set on a socket impl
	 * @param socketImpl the socket impl
	 * @param connectionTime An <tt>int</tt> expressing the relative importance of a short connection time
	 * @param latency An <tt>int</tt> expressing the relative importance of low latency
	 * @param bandwidth An <tt>int</tt> expressing the relative importance of highbandwidth
	 */
	public static void onSetPerformancePreferences(Object socketImpl, int connectionTime, int latency, int bandwidth) {
		
	}
	
}
