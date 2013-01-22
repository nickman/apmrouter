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

/**
 * <p>Title: SocketTrackingAdapter</p>
 * <p>Description: A static template class acting as a receiver for events emitted from instrumented socket implementations</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.byteman.sockets.impl.SocketTrackingAdapter</code></p>
 */



public class SocketTrackingAdapter {
	/** The registered socket tracker */
	protected static ISocketTracker socketTracker = null;  
	/** The socket tracking adapters for socket impls */
	public static final Map<String, String> SOCKET_IMPL_ADAPTERS;
	/** The socket tracking adapters for socket output stream */
	public static final Map<String, String> SOCKET_OS_ADAPTERS;
	/** The socket tracking adapters for socket input stream */
	public static final Map<String, String> SOCKET_IS_ADAPTERS;
	
	
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
		methodMap = new HashMap<String, String>();
		methodMap.put("socketWrite.([BII)V","org.helios.apmrouter.byteman.sockets.impl.SocketTrackingAdapter.onSocketWrite($0, socket, $$);");
		
		
		SOCKET_OS_ADAPTERS = Collections.unmodifiableMap(methodMap);
		methodMap = new HashMap<String, String>();
		methodMap.put("read.([B)I","org.helios.apmrouter.byteman.sockets.impl.SocketTrackingAdapter.onRead($0, $_, socket, $$);");
		methodMap.put("read.([BII)I","org.helios.apmrouter.byteman.sockets.impl.SocketTrackingAdapter.onRead($0, $_, socket, $$);");
		methodMap.put("read.()I","org.helios.apmrouter.byteman.sockets.impl.SocketTrackingAdapter.onRead($0, $_, socket);");
		methodMap.put("setEOF.(Z)V","org.helios.apmrouter.byteman.sockets.impl.SocketTrackingAdapter.onSetEOF($0, socket, $$);");
		methodMap.put("skip.(J)J","org.helios.apmrouter.byteman.sockets.impl.SocketTrackingAdapter.onSkip($0, $_, socket, $$);");
		SOCKET_IS_ADAPTERS = Collections.unmodifiableMap(methodMap);
		
		setISocketTracker(new LoggingSocketTracker());
	}
	
	/**
	 * Sets the socket tracker
	 * @param tracker the socket tracker or null to clear the tracker
	 */
	public static void setISocketTracker(ISocketTracker tracker) {
		socketTracker = tracker;
	}
	
	/**
	 * Called when EOF is set on a socket
	 * @param is the input stream
	 * @param socket the socket
	 * @param eof the eof value
	 */
	public static void onSetEOF(InputStream is, Object socket, boolean eof) {
		if(socketTracker!=null) socketTracker.onSetEOF(is, socket, eof);
	}
	
	/**
	 * Called when bytes are skipped on the input stream of a socket
	 * @param is The input stream
	 * @param skipped the actual number of bytes to skip
	 * @param socket the socket
	 * @param skip the number of bytes to skip
	 */
	public static void onSkip(InputStream is, long skipped, Object socket, long skip) {
		if(socketTracker!=null) socketTracker.onSkip(is, skipped, socket, skip);
	}
	
	/**
	 * Called when a write completes to a socket
	 * @param os The output stream
	 * @param socket The socket
     * @param b the data that was written
     * @param off the start offset in the data
     * @param len the number of bytes that were written
	 */
	public static void onSocketWrite(OutputStream os, Object socket, byte b[], int off, int len) {
		if(socketTracker!=null) socketTracker.onSocketWrite(os, socket, b, off, len);
	}
	
	

	
	/**
	 * Called when data is read from a socket
	 * @param is the input stream
	 * @param actualBytesRead the actual number of bytes read, -1 is returned when the end of the stream is reached
	 * @param socket the socket
	 * @param buffer the buffer into which the data is read
	 */
	public static void onRead(InputStream is, int actualBytesRead, Object socket, byte[] buffer) {
		if(socketTracker!=null) socketTracker.onRead(is, actualBytesRead, socket, buffer);
	}
	
	/**
	 * Called when data is read from a socket
	 * @param is the input stream
	 * @param actualBytesRead the actual number of bytes read, -1 is returned when the end of the stream is reached
	 * @param socket the socket
	 * @param buffer the buffer into which the data is read
	 * @param off the start offset of the data
	 * @param length the maximum number of bytes read
	 */
	public static void onRead(InputStream is, int actualBytesRead, Object socket, byte[] buffer, int off, int length) {
		if(socketTracker!=null) socketTracker.onRead(is, actualBytesRead, socket, buffer, off, length);
	}
	
	
    /**
     * Called when a byte is read from a socket
     * @param is the input stream
     * @param value the value read
     * @param socket the socket
     */
    public static void onRead(InputStream is, int value, Object socket) {
    	if(socketTracker!=null) socketTracker.onRead(is, value, socket);
    }

	/**
	 * Called on a socket impl connect. This is the only <i>actual</i> connect execution. The overloads are redirected here.
	 * @param socketImpl the socket impl that connected
	 * @param address the socket address that the socket impl connected to
	 * @param timeout the timeout used for connect
	 */
	public static void onConnect(Object socketImpl, SocketAddress address, int timeout) {
		if(socketTracker!=null) socketTracker.onConnect((ISocketImpl)socketImpl, address, timeout);
	}
	
	/**
	 * Called on a socket impl connect
	 * @param socketImpl the socket impl that connected
	 * @param address the inet address that the socket impl connected to
	 * @param timeout the timeout used for connect
	 */
	public static void onConnect(Object socketImpl, InetAddress address, int timeout) {
		if(socketTracker!=null) socketTracker.onConnect((ISocketImpl)socketImpl, address, timeout);
	}
	
	/**
	 * Called on a socket impl connect
	 * @param socketImpl the socket impl that connected
	 * @param host the host name that the socket impl connected to
	 * @param port the port that the socket impl connected to
	 */
	public static void onConnect(Object socketImpl, String host, int port) {
		if(socketTracker!=null) socketTracker.onConnect((ISocketImpl)socketImpl, host, port);
	}
	
	/**
	 * Called when a socket impl binds to a socket
	 * @param socketImpl the socket impl that was bound 
	 * @param host the host of the bound socket
	 * @param port the port of the bound socket
	 */
	public static void onBind(Object socketImpl, InetAddress host, int port) {
		if(socketTracker!=null) socketTracker.onBind((ISocketImpl)socketImpl, host, port);
	}
	
	/**
	 * Called when a socket impl has its backlog queue set
	 * @param socketImpl the socket impl that had its backlog queue set
	 * @param backlog the connection backlog
	 */
	public static void onListen(Object socketImpl, int backlog) {
		if(socketTracker!=null) socketTracker.onListen((ISocketImpl)socketImpl, backlog);
	}
	
	/**
	 * Called when a server socket impl accepts a new connection 
	 * @param socketImpl the socket impl that accepted
	 * @param acceptedSocketImpl the accepted client socket impl
	 */
	public static void onAccept(Object socketImpl, Object acceptedSocketImpl) {
		if(socketTracker!=null) socketTracker.onAccept((ISocketImpl)socketImpl, (ISocketImpl)acceptedSocketImpl);
	}
	
	/**
	 * Called when the input stream is requested from a socket impl
	 * @param socketImpl the socket impl
	 * @param inputStream the returned input stream
	 */
	public static void onGetInputStream(Object socketImpl, InputStream inputStream) {
		if(socketTracker!=null) socketTracker.onGetInputStream((ISocketImpl)socketImpl, inputStream);
	}

	/**
	 * Called when the output stream is requested from a socket impl
	 * @param socketImpl the socket impl
	 * @param outputStream the returned output stream
	 */
	public static void onGetOutputStream(Object socketImpl, OutputStream outputStream) {
		if(socketTracker!=null) socketTracker.onGetOutputStream((ISocketImpl)socketImpl, outputStream);
	}
	
	/**
	 * Called when the available bytes is requested from a socket impl
	 * @param socketImpl the socket impl
	 * @param available the available bytes returned
	 */
	public static void  onAvailable(Object socketImpl, int available) {
		if(socketTracker!=null) socketTracker.onAvailable((ISocketImpl)socketImpl, available);
	}

	/**
	 * Called when a socket impl is closed
	 * @param socketImpl the socket impl
	 */
	public static void  onClose(Object socketImpl) {
		if(socketTracker!=null) socketTracker.onClose((ISocketImpl)socketImpl);
	}

	/**
	 * Called when input is shutdown on a socket impl
	 * @param socketImpl the socket impl
	 */
	public static void onShutdownInput(Object socketImpl)  {
		if(socketTracker!=null) socketTracker.onShutdownInput((ISocketImpl)socketImpl);
    }
	
	/**
	 * Called when output is shutdown on a socket impl
	 * @param socketImpl the socket impl
	 */
	public static void onShutdownOutput(Object socketImpl)  {
		if(socketTracker!=null) socketTracker.onShutdownOutput((ISocketImpl)socketImpl);
    }
	
	/**
	 * Called when urgent data is sent through a socket impl
	 * @param socketImpl the socket impl
	 * @param data the data that was sent
	 */
	public static void onSendUrgentData(Object socketImpl, int data)  {
		if(socketTracker!=null) socketTracker.onSendUrgentData((ISocketImpl)socketImpl, data);
	}
	
	/**
	 * Called when the client socket is set on a socket impl
	 * @param socketImpl the socket impl
	 * @param socket the set socket
	 */
	public static void onSetSocket(Object socketImpl, Object socket) {
		if(socketTracker!=null) socketTracker.onSetSocket((ISocketImpl)socketImpl, socket);
	}

	/**
	 * Called when the server socket is set on a socket impl
	 * @param socketImpl the socket impl
	 * @param serverSocket The set server socket
	 */
	public static void onSetServerSocket(Object socketImpl, Object serverSocket) {
		if(socketTracker!=null) socketTracker.onSetServerSocket((ISocketImpl)socketImpl, serverSocket);
	}
	
	/**
	 * Called when a socket impl is reset
	 * @param socketImpl the socket impl
	 */
	public static void onReset(Object socketImpl) {
		if(socketTracker!=null) socketTracker.onReset((ISocketImpl)socketImpl);
	}
	
	
	/**
	 * Called when performance preferences are set on a socket impl
	 * @param socketImpl the socket impl
	 * @param connectionTime An <tt>int</tt> expressing the relative importance of a short connection time
	 * @param latency An <tt>int</tt> expressing the relative importance of low latency
	 * @param bandwidth An <tt>int</tt> expressing the relative importance of highbandwidth
	 */
	public static void onSetPerformancePreferences(Object socketImpl, int connectionTime, int latency, int bandwidth) {
		if(socketTracker!=null) socketTracker.onSetPerformancePreferences((ISocketImpl)socketImpl, connectionTime, latency, bandwidth);
	}
	
}
