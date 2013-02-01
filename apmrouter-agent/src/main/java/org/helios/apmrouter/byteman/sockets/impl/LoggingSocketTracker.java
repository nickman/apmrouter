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

import static org.helios.apmrouter.util.SimpleLogger.log;

import java.io.FileDescriptor;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.util.Iterator;

import org.helios.apmrouter.byteman.sockets.ServerConnection;
import org.helios.apmrouter.util.SimpleLogger;

/**
 * <p>Title: LoggingSocketTracker</p>
 * <p>Description: A super simple {@link ISocketTracker} that logs all socket events.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.byteman.sockets.impl.LoggingSocketTracker</code></p>
 */

public class LoggingSocketTracker extends EmptySocketTracker implements LoggingSocketTrackerMBean {
	/*
	 * TODO:
	 * Logging level
	 * Tracked socket counts
	 */
	
	/**
	 * Called when EOF is set on a socket
	 * @param is the input stream
	 * @param socket the socket
	 * @param eof the eof value
	 */
	@Override
	public void onSetEOF(InputStream is, Object socket, boolean eof) {
		log(loggingLevel, "EOF [", eof, "]");
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
		log(loggingLevel, "Skipped [", skipped, "]");
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
		log(loggingLevel, "Socket Write [", len, "]");
		//log(loggingLevel, "Socket Write Conent: [", new String(b), "]");
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
		log(loggingLevel, "Socket Read [", actualBytesRead, "]");
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
		log(loggingLevel, "Socket Read [", actualBytesRead, "]");
	}
	
	
    /**
     * Called when a byte is read from a socket
     * @param is the input stream
     * @param value the value read
     * @param socket the socket
     */
    @Override
	public void onRead(InputStream is, int value, Object socket) {
    	log(loggingLevel, "Socket Read [1]");
    }

	/**
	 * Called on a socket impl connect. This is the only <i>actual</i> connect execution. The overloads are redirected here.
	 * @param socketImpl the socket impl that connected
	 * @param address the socket address that the socket impl connected to
	 * @param timeout the timeout used for connect
	 */
	@Override
	public void onConnect(ISocketImpl socketImpl, SocketAddress address, int timeout) {
		log(loggingLevel, "Connected (sa) [", socketImpl.getClass().getSimpleName() , ":", address, ":", timeout, "]");
	}
	
	/**
	 * Called on a socket impl connect
	 * @param socketImpl the socket impl that connected
	 * @param address the inet address that the socket impl connected to
	 * @param timeout the timeout used for connect
	 */
	@Override
	public void onConnect(ISocketImpl socketImpl, InetAddress address, int timeout) {
		log(loggingLevel, "Connected (ia) [", address, ":", timeout, "]");
	}
	
	/**
	 * Called on a socket impl connect
	 * @param socketImpl the socket impl that connected
	 * @param host the host name that the socket impl connected to
	 * @param port the port that the socket impl connected to
	 */
	@Override
	public void onConnect(ISocketImpl socketImpl, String host, int port) {
		log(loggingLevel, "Connected (ha) [", host, ":", port, "]");
	}
	
	/**
	 * Called when a socket impl binds to a socket
	 * @param socketImpl the socket impl that was bound 
	 * @param host the host of the bound socket
	 * @param port the port of the bound socket
	 */
	@Override
	public void onBind(ISocketImpl socketImpl, InetAddress host, int port) {
		log(loggingLevel, "Bind [", host, ":", port, "]");
		ServerSocket ss = socketImpl.getServerSocket();
		log(loggingLevel, "ServerSock: [", ss, "]");
	}
	
	/**
	 * Called when a socket impl has its backlog queue set
	 * @param socketImpl the socket impl that had its backlog queue set
	 * @param backlog the connection backlog
	 */
	@Override
	public void onListen(ISocketImpl socketImpl, int backlog) {
		log(loggingLevel, "Listen [", backlog, "]");
	}
	
	/**
	 * Called when a server socket impl accepts a new connection 
	 * @param socketImpl the socket impl that was accepted
	 * @param acceptedSocketImpl the accepted client socket impl
	 */
	@Override
	public void onAccept(ISocketImpl socketImpl, ISocketImpl acceptedSocketImpl) {		
		//serverSideSockets.add(acceptedSocketImpl);
		ServerConnection.getInstance(acceptedSocketImpl);
		StringBuilder b = new StringBuilder("Accepted Socket [").append(System.identityHashCode(acceptedSocketImpl)).append("]");
		
		b.append("\n\tAccepted:").append(acceptedSocketImpl);
		b.append("\n\tRemote Address:").append(acceptedSocketImpl.getInetAddress().getHostAddress()).append(":").append(acceptedSocketImpl.getPort());
		b.append("\n\tLocal Address:").append(socketImpl.getInetAddress().getHostAddress()).append(":").append(socketImpl.getLocalPort());
		
//		b.append("\n\tLocal Address:").append(acceptedSocketImpl.getSocket().getLocalSocketAddress());
//		b.append("\n\tRemote Address:").append(acceptedSocketImpl.getSocket().getRemoteSocketAddress());
		
		log(loggingLevel, b);
	}
	
	/**
	 * Called when the input stream is requested from a socket impl
	 * @param socketImpl the socket impl
	 * @param inputStream the returned input stream
	 */
	@Override
	public void onGetInputStream(ISocketImpl socketImpl, InputStream inputStream) {
		StringBuilder b = new StringBuilder("InputStream Accessed [").append(System.identityHashCode(socketImpl)).append("]");
		b.append("\n\tLocal Address:").append(socketImpl.getSocket().getLocalSocketAddress());
		b.append("\n\tRemote Address:").append(socketImpl.getSocket().getRemoteSocketAddress());		
		log(loggingLevel, b);
	}

	/**
	 * Called when the output stream is requested from a socket impl
	 * @param socketImpl the socket impl
	 * @param outputStream the returned output stream
	 */
	@Override
	public void onGetOutputStream(ISocketImpl socketImpl, OutputStream outputStream) {		
		StringBuilder b = new StringBuilder("OutputStream Accessed [").append(System.identityHashCode(socketImpl)).append("]");
		b.append("\n\tLocal Address:").append(socketImpl.getSocket().getLocalSocketAddress());
		b.append("\n\tRemote Address:").append(socketImpl.getSocket().getRemoteSocketAddress());		
		log(loggingLevel, b);
	}
	
	/**
	 * Called when the available bytes is requested from a socket impl
	 * @param socketImpl the socket impl
	 * @param available the available bytes returned
	 */
	@Override
	public void  onAvailable(ISocketImpl socketImpl, int available) {
		log(loggingLevel, "Available [", available, "]");
	}

	/**
	 * Called when a socket impl is closed
	 * @param socketImpl the socket impl
	 */
	@Override
	public void  onClose(ISocketImpl socketImpl) {
		StringBuilder b = new StringBuilder("Closing [");
		b.append(socketImpl.getClass().getSimpleName()).append("]");
		if(socketImpl.getServerSocket()!=null) {
			b.append(":  ServerSocket");
			b.append("\n\tBound Address:").append(socketImpl.getServerSocket().getLocalSocketAddress());
		} else {
			if(serverSideSockets.remove(socketImpl)) {
				b.append(":  ServerSide Socket [" + System.identityHashCode(socketImpl) + "]");				
			} else {
				b.append(":  ClientSocket [" + System.identityHashCode(socketImpl) + "]");
			}
			b.append("\n\tLocal Address:").append(socketImpl.getSocket().getLocalSocketAddress());
			b.append("\n\tRemote Address:").append(socketImpl.getSocket().getRemoteSocketAddress());			
		}
		log(loggingLevel, b);
	}

	/**
	 * Called when input is shutdown on a socket impl
	 * @param socketImpl the socket impl
	 */
	@Override
	public void onShutdownInput(ISocketImpl socketImpl)  {
		log(loggingLevel, "ShutdownInput [", socketImpl, "]");
    }
	
	/**
	 * Called when output is shutdown on a socket impl
	 * @param socketImpl the socket impl
	 */
	@Override
	public void onShutdownOutput(ISocketImpl socketImpl)  {
		log(loggingLevel, "ShutdownOutput [", socketImpl, "]");
    }
	
	/**
	 * Called when urgent data is sent through a socket impl
	 * @param socketImpl the socket impl
	 * @param data the data that was sent
	 */
	@Override
	public void onSendUrgentData(ISocketImpl socketImpl, int data)  {
		log(loggingLevel, "SendUrgentData [", data, "]");
	}
	
	/**
	 * Called when the client socket is set on a socket impl
	 * @param socketImpl the socket impl
	 * @param socket the set socket
	 */
	@Override
	public void onSetSocket(ISocketImpl socketImpl, Object socket) {
		log(loggingLevel, "SetSocket [", socket, "]");
	}

	/**
	 * Called when the server socket is set on a socket impl
	 * @param socketImpl the socket impl
	 * @param serverSocket The set server socket
	 */
	@Override
	public void onSetServerSocket(ISocketImpl socketImpl, Object serverSocket) {
		log(loggingLevel, "SetServerSocket [", serverSocket, "]");
	}
	
	/**
	 * Called when a socket impl is reset
	 * @param socketImpl the socket impl
	 */
	@Override
	public void onReset(ISocketImpl socketImpl) {
		log(loggingLevel, "Reset [", socketImpl, "]");
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
		log(loggingLevel, "SetPerformancePreferences [", connectionTime, ":", latency, ":", bandwidth,  "]");
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
		if(serverSideSockets.isEmpty()) return;
		final long start = System.currentTimeMillis();
		//SimpleLogger.log(loggingLevel, "[", getClass().getSimpleName(), "] Harvesting...");
		final int startSize = serverSideSockets.size();
		Iterator<ISocketImpl> socketIter = serverSideSockets.iterator();
		int socketsClosed = 0;
		for(; socketIter.hasNext();) {
			ISocketImpl isocket = socketIter.next();
			FileDescriptor fd = isocket.getFileDescriptor();
			boolean fdValid = fd!=null && fd.valid();
			if(fd!=null) {
				SimpleLogger.log(loggingLevel, "FD: [", fd, "]:" , fd.valid());
				try { fd.sync(); } catch (Exception ex) {}
			}
			
			if(!fdValid || !testSocketStreams(isocket.getSocket())) {
				socketIter.remove();
				socketsClosed++;
				try { isocket.close(); } catch (Exception ex) {}
			}
		}
		final int endSize = serverSideSockets.size();
		final long elapsed = System.currentTimeMillis()-start;
		SimpleLogger.log(loggingLevel, "[", getClass().getSimpleName(), "] Harvester closed [", socketsClosed, "] accepted sockets. \n\tStartCount:", startSize, " \n\tEndCount:", endSize, "\n\tElapsed:", elapsed, " ms.");
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.EmptySocketTracker#hasJMXInterface()
	 */
	@Override
	public boolean hasJMXInterface() {
		return true;
	}


}
