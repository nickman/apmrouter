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

/**
 * <p>Title: ISocketTracker</p>
 * <p>Description: Defines a socket tracker that can be registered with the {@link SocketTrackingAdapter} to capture socket events.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.byteman.sockets.impl.ISocketTracker</code></p>
 */

public interface ISocketTracker extends Runnable {
	
	/** The system property name for the name of the socket tracking class to install */
	public static final String SOCKET_TRACKER_PROP = "org.helios.apmrouter.socket.tracker";
	/** The default name of the socket tracking class to install */
	public static final String DEFAULT_SOCKET_TRACKER = EmptySocketTracker.class.getName();
	/** The system property name for the pause time between harvester runs */
	public static final String SOCKET_HARVESTER_PERIOD_PROP = "org.helios.apmrouter.socket.harvester";
	/** The default for the pause time between harvester runs in ms. */
	public static final long DEFAULT_SOCKET_HARVESTER_PERIOD = 5000;
	
	/**
	 * Indicates if this socket tracker requires a harvester thread
	 * @return true if this socket tracker requires a harvester thread, false otherwise
	 */
	public boolean requiresHarvester();
	
	/**
	 * Called when EOF is set on a socket
	 * @param is the input stream
	 * @param socket the socket
	 * @param eof the eof value
	 */
	public void onSetEOF(InputStream is, Object socket, boolean eof);
	
	/**
	 * Called when bytes are skipped on the input stream of a socket
	 * @param is The input stream
	 * @param skipped the actual number of bytes to skip
	 * @param socket the socket
	 * @param skip the number of bytes to skip
	 */
	public void onSkip(InputStream is, long skipped, Object socket, long skip);
	
	/**
	 * Called when a write completes to a socket
	 * @param os The output stream
	 * @param socket The socket
     * @param b the data that was written
     * @param off the start offset in the data
     * @param len the number of bytes that were written
	 */
	public void onSocketWrite(OutputStream os, Object socket, byte b[], int off, int len);
	
	

	
	/**
	 * Called when data is read from a socket
	 * @param is the input stream
	 * @param actualBytesRead the actual number of bytes read, -1 is returned when the end of the stream is reached
	 * @param socket the socket
	 * @param buffer the buffer into which the data is read
	 */
	public void onRead(InputStream is, int actualBytesRead, Object socket, byte[] buffer);
	
	/**
	 * Called when data is read from a socket
	 * @param is the input stream
	 * @param actualBytesRead the actual number of bytes read, -1 is returned when the end of the stream is reached
	 * @param socket the socket
	 * @param buffer the buffer into which the data is read
	 * @param off the start offset of the data
	 * @param length the maximum number of bytes read
	 */
	public void onRead(InputStream is, int actualBytesRead, Object socket, byte[] buffer, int off, int length);
	
	
    /**
     * Called when a byte is read from a socket
     * @param is the input stream
     * @param value the value read
     * @param socket the socket
     */
    public void onRead(InputStream is, int value, Object socket);

	/**
	 * Called on a socket impl connect. This is the only <i>actual</i> connect execution. The overloads are redirected here.
	 * @param socketImpl the socket impl that connected
	 * @param address the socket address that the socket impl connected to
	 * @param timeout the timeout used for connect
	 */
	public void onConnect(ISocketImpl socketImpl, SocketAddress address, int timeout);
	
	/**
	 * Called on a socket impl connect
	 * @param socketImpl the socket impl that connected
	 * @param address the inet address that the socket impl connected to
	 * @param timeout the timeout used for connect
	 */
	public void onConnect(ISocketImpl socketImpl, InetAddress address, int timeout);
	
	/**
	 * Called on a socket impl connect
	 * @param socketImpl the socket impl that connected
	 * @param host the host name that the socket impl connected to
	 * @param port the port that the socket impl connected to
	 */
	public void onConnect(ISocketImpl socketImpl, String host, int port);
	
	/**
	 * Called when a socket impl binds to a socket
	 * @param socketImpl the socket impl that was bound 
	 * @param host the host of the bound socket
	 * @param port the port of the bound socket
	 */
	public void onBind(ISocketImpl socketImpl, InetAddress host, int port);
	
	/**
	 * Called when a socket impl has its backlog queue set
	 * @param socketImpl the socket impl that had its backlog queue set
	 * @param backlog the connection backlog
	 */
	public void onListen(ISocketImpl socketImpl, int backlog);
	
	/**
	 * Called when a server socket impl accepts a new connection 
	 * @param socketImpl the socket impl that accepted
	 * @param acceptedSocketImpl the accepted client socket impl
	 */
	public void onAccept(ISocketImpl socketImpl, ISocketImpl acceptedSocketImpl);
	
	/**
	 * Called when the input stream is requested from a socket impl
	 * @param socketImpl the socket impl
	 * @param inputStream the returned input stream
	 */
	public void onGetInputStream(ISocketImpl socketImpl, InputStream inputStream);

	/**
	 * Called when the output stream is requested from a socket impl
	 * @param socketImpl the socket impl
	 * @param outputStream the returned output stream
	 */
	public void onGetOutputStream(ISocketImpl socketImpl, OutputStream outputStream);
	
	/**
	 * Called when the available bytes is requested from a socket impl
	 * @param socketImpl the socket impl
	 * @param available the available bytes returned
	 */
	public void  onAvailable(ISocketImpl socketImpl, int available);

	/**
	 * Called when a socket impl is closed
	 * @param socketImpl the socket impl
	 */
	public void  onClose(ISocketImpl socketImpl);

	/**
	 * Called when input is shutdown on a socket impl
	 * @param socketImpl the socket impl
	 */
	public void onShutdownInput(ISocketImpl socketImpl);
	
	/**
	 * Called when output is shutdown on a socket impl
	 * @param socketImpl the socket impl
	 */
	public void onShutdownOutput(ISocketImpl socketImpl);
	
	/**
	 * Called when urgent data is sent through a socket impl
	 * @param socketImpl the socket impl
	 * @param data the data that was sent
	 */
	public void onSendUrgentData(ISocketImpl socketImpl, int data);
	
	/**
	 * Called when the client socket is set on a socket impl
	 * @param socketImpl the socket impl
	 * @param socket the set socket
	 */
	public void onSetSocket(ISocketImpl socketImpl, Object socket);

	/**
	 * Called when the server socket is set on a socket impl
	 * @param socketImpl the socket impl
	 * @param serverSocket The set server socket
	 */
	public void onSetServerSocket(ISocketImpl socketImpl, Object serverSocket);
	
	/**
	 * Called when a socket impl is reset
	 * @param socketImpl the socket impl
	 */
	public void onReset(ISocketImpl socketImpl);
	
	
	/**
	 * Called when performance preferences are set on a socket impl
	 * @param socketImpl the socket impl
	 * @param connectionTime An <tt>int</tt> expressing the relative importance of a short connection time
	 * @param latency An <tt>int</tt> expressing the relative importance of low latency
	 * @param bandwidth An <tt>int</tt> expressing the relative importance of highbandwidth
	 */
	public void onSetPerformancePreferences(ISocketImpl socketImpl, int connectionTime, int latency, int bandwidth);
	
	/**
	 * Stops the socket tracker's harvester thread if it is running
	 */
	public void stop();
	
	/**
	 * Starts the socket tracker's harvester thread if it is not already running
	 */
	public void start();
	
	/**
	 * Indicates if the socket tracker's harvester thread is running
	 * @return true if the socket tracker's harvester thread is running, false otherwise
	 */
	public boolean isStarted();

	/**
	 * Sets the name of the active tracker
	 * @param simpleName the name of the active tracker
	 */
	public void setActiveTracker(String simpleName);
	
	/**
	 * Returns the name of the active tracker or null if there is none
	 * @return the name of the active tracker or null if there is none
	 */
	public String getActiveTracker();

}
