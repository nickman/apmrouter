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

import static org.helios.apmrouter.util.SimpleLogger.info;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;

/**
 * <p>Title: LoggingSocketTracker</p>
 * <p>Description: A super simple {@link ISocketTracker} that logs all socket events.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.byteman.sockets.impl.LoggingSocketTracker</code></p>
 */

public class LoggingSocketTracker implements ISocketTracker {
	/**
	 * Called when EOF is set on a socket
	 * @param is the input stream
	 * @param socket the socket
	 * @param eof the eof value
	 */
	@Override
	public void onSetEOF(InputStream is, Object socket, boolean eof) {
		info("EOF [", eof, "]");
	}
	
	/**
	 * Called when bytes are skipped on the input stream of a socket
	 * @param is The input stream
	 * @param skipped the actual number of bytes to skip
	 * @param socket the socket
	 * @param skip the number of bytes to skip
	 */
	@Override
	public void onSkip(InputStream is, long skipped, Object socket, long skip) {
		info("Skipped [", skipped, "]");
	}
	
	/**
	 * Called when a write completes to a socket
	 * @param os The output stream
	 * @param socket The socket
     * @param b the data that was written
     * @param off the start offset in the data
     * @param len the number of bytes that were written
	 */
	@Override
	public void onSocketWrite(OutputStream os, Object socket, byte b[], int off, int len) {
		info("Socket Write [", len, "]");
		//info("Socket Write Conent: [", new String(b), "]");
	}
	
	

	
	/**
	 * Called when data is read from a socket
	 * @param is the input stream
	 * @param actualBytesRead the actual number of bytes read, -1 is returned when the end of the stream is reached
	 * @param socket the socket
	 * @param buffer the buffer into which the data is read
	 */
	@Override
	public void onRead(InputStream is, int actualBytesRead, Object socket, byte[] buffer) {
		info("Socket Read [", actualBytesRead, "]");
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
	@Override
	public void onRead(InputStream is, int actualBytesRead, Object socket, byte[] buffer, int off, int length) {
		info("Socket Read [", actualBytesRead, "]");
	}
	
	
    /**
     * Called when a byte is read from a socket
     * @param is the input stream
     * @param value the value read
     * @param socket the socket
     */
    @Override
	public void onRead(InputStream is, int value, Object socket) {
    	info("Socket Read [1]");
    }

	/**
	 * Called on a socket impl connect. This is the only <i>actual</i> connect execution. The overloads are redirected here.
	 * @param socketImpl the socket impl that connected
	 * @param address the socket address that the socket impl connected to
	 * @param timeout the timeout used for connect
	 */
	@Override
	public void onConnect(ISocketImpl socketImpl, SocketAddress address, int timeout) {
		info("Connected (sa) [", socketImpl.getClass().getSimpleName() , ":", address, ":", timeout, "]");
	}
	
	/**
	 * Called on a socket impl connect
	 * @param socketImpl the socket impl that connected
	 * @param address the inet address that the socket impl connected to
	 * @param timeout the timeout used for connect
	 */
	@Override
	public void onConnect(ISocketImpl socketImpl, InetAddress address, int timeout) {
		info("Connected (ia) [", address, ":", timeout, "]");
	}
	
	/**
	 * Called on a socket impl connect
	 * @param socketImpl the socket impl that connected
	 * @param host the host name that the socket impl connected to
	 * @param port the port that the socket impl connected to
	 */
	@Override
	public void onConnect(ISocketImpl socketImpl, String host, int port) {
		info("Connected (ha) [", host, ":", port, "]");
	}
	
	/**
	 * Called when a socket impl binds to a socket
	 * @param socketImpl the socket impl that was bound 
	 * @param host the host of the bound socket
	 * @param port the port of the bound socket
	 */
	@Override
	public void onBind(ISocketImpl socketImpl, InetAddress host, int port) {
		info("Bind [", host, ":", port, "]");
		ServerSocket ss = socketImpl.getServerSocket();
		info("ServerSock: [", ss, "]");
	}
	
	/**
	 * Called when a socket impl has its backlog queue set
	 * @param socketImpl the socket impl that had its backlog queue set
	 * @param backlog the connection backlog
	 */
	@Override
	public void onListen(ISocketImpl socketImpl, int backlog) {
		info("Listen [", backlog, "]");
	}
	
	/**
	 * Called when a server socket impl accepts a new connection 
	 * @param socketImpl the socket impl that accepted
	 * @param acceptedSocketImpl the accepted client socket impl
	 */
	@Override
	public void onAccept(ISocketImpl socketImpl, Object acceptedSocketImpl) {
		info("Accept [", acceptedSocketImpl, "]");
	}
	
	/**
	 * Called when the input stream is requested from a socket impl
	 * @param socketImpl the socket impl
	 * @param inputStream the returned input stream
	 */
	@Override
	public void onGetInputStream(ISocketImpl socketImpl, InputStream inputStream) {
		info("GetInputStream [", inputStream, "]");
	}

	/**
	 * Called when the output stream is requested from a socket impl
	 * @param socketImpl the socket impl
	 * @param outputStream the returned output stream
	 */
	@Override
	public void onGetOutputStream(ISocketImpl socketImpl, OutputStream outputStream) {
		info("GetOutputStream [", outputStream, "]");
	}
	
	/**
	 * Called when the available bytes is requested from a socket impl
	 * @param socketImpl the socket impl
	 * @param available the available bytes returned
	 */
	@Override
	public void  onAvailable(ISocketImpl socketImpl, int available) {
		info("Available [", available, "]");
	}

	/**
	 * Called when a socket impl is closed
	 * @param socketImpl the socket impl
	 */
	@Override
	public void  onClose(ISocketImpl socketImpl) {
		info("Close [", socketImpl, "]");
	}

	/**
	 * Called when input is shutdown on a socket impl
	 * @param socketImpl the socket impl
	 */
	@Override
	public void onShutdownInput(ISocketImpl socketImpl)  {
		info("ShutdownInput [", socketImpl, "]");
    }
	
	/**
	 * Called when output is shutdown on a socket impl
	 * @param socketImpl the socket impl
	 */
	@Override
	public void onShutdownOutput(ISocketImpl socketImpl)  {
		info("ShutdownOutput [", socketImpl, "]");
    }
	
	/**
	 * Called when urgent data is sent through a socket impl
	 * @param socketImpl the socket impl
	 * @param data the data that was sent
	 */
	@Override
	public void onSendUrgentData(ISocketImpl socketImpl, int data)  {
		info("SendUrgentData [", data, "]");
	}
	
	/**
	 * Called when the client socket is set on a socket impl
	 * @param socketImpl the socket impl
	 * @param socket the set socket
	 */
	@Override
	public void onSetSocket(ISocketImpl socketImpl, Object socket) {
		info("SetSocket [", socket, "]");
	}

	/**
	 * Called when the server socket is set on a socket impl
	 * @param socketImpl the socket impl
	 * @param serverSocket The set server socket
	 */
	@Override
	public void onSetServerSocket(ISocketImpl socketImpl, Object serverSocket) {
		info("SetServerSocket [", serverSocket, "]");
	}
	
	/**
	 * Called when a socket impl is reset
	 * @param socketImpl the socket impl
	 */
	@Override
	public void onReset(ISocketImpl socketImpl) {
		info("Reset [", socketImpl, "]");
	}
	
	
	/**
	 * Called when performance preferences are set on a socket impl
	 * @param socketImpl the socket impl
	 * @param connectionTime An <tt>int</tt> expressing the relative importance of a short connection time
	 * @param latency An <tt>int</tt> expressing the relative importance of low latency
	 * @param bandwidth An <tt>int</tt> expressing the relative importance of highbandwidth
	 */
	@Override
	public void onSetPerformancePreferences(ISocketImpl socketImpl, int connectionTime, int latency, int bandwidth) {
		info("SetPerformancePreferences [", connectionTime, ":", latency, ":", bandwidth,  "]");
	}

	

}
