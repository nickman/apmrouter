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
package org.helios.apmrouter.byteman;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

import org.helios.apmrouter.byteman.SocketTracingLevel.SocketTracingLevelListener;
import org.helios.apmrouter.byteman.SocketTracingLevel.SocketTracingLevelWatcher;
import org.helios.apmrouter.collections.ConcurrentLongSlidingWindow;
import org.helios.apmrouter.util.SimpleLogger;
import org.jboss.byteman.rule.Rule;

/**
 * <p>Title: APMSocketMonitorHelper</p>
 * <p>Description: A byteman helper class for monitoring socket connections and throughput</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.byteman.APMSocketMonitorHelper</code></p>
 */

public class APMSocketMonitorHelper extends APMAgentHelper {
	
	/** The java.net.SocketOutputStream class */
	protected static final Class<?> SOCK_OUT_CLASS;
	/** The java.net.SocketInputStream class */
	protected static final Class<?> SOCK_IN_CLASS;
	/** The java.net.SocketOutputStream Stream socket field */
	protected static final Field SOCK_OUT_FIELD;	
	/** The java.net.SocketInputStream Stream socket field */
	protected static final Field SOCK_IN_FIELD;
	
	static {
		try {
			SOCK_OUT_CLASS = Class.forName("java.net.SocketOutputStream");
			SOCK_IN_CLASS = Class.forName("java.net.SocketInputStream");
			SOCK_OUT_FIELD = SOCK_OUT_CLASS.getDeclaredField("socket");
			SOCK_OUT_FIELD.setAccessible(true);
			SOCK_IN_FIELD = SOCK_IN_CLASS.getDeclaredField("socket");
			SOCK_IN_FIELD.setAccessible(true);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to load SocketStream classes", ex);
		}
	}
	
	/** The level listener for this helper's socket tracing level */
	protected static final SocketTracingLevelListener levelListener = new SocketTracingLevelListener() {

		@Override
		public void fromNullTo(SocketTracingLevel newLevel) {			
			
		}

		@Override
		public void toNull(SocketTracingLevel oldLevel) {
			
		}

		@Override
		public void change(SocketTracingLevel oldLevel, SocketTracingLevel newLevel) {
			
		}
		
	};
	/** The socket tracing level */
	protected static final SocketTracingLevelWatcher tracingLevel = new SocketTracingLevelWatcher(SocketTracingLevel.CONNECTIONS, levelListener); 
	
	/** The array index for socket input (reads) */
	public static final int INPUT = 0;
	/** The array index for socket output (writes) */
	public static final int OUTPUT = 1;
	/** The array index for the local socket */
	public static final int LOCAL = 0;
	/** The array index for the remote socket */
	public static final int REMOTE = 0;
	
	/**
	 * Events to track:
	 *  New Connections:
	 *  ================
	 *  ServerSocket.implAccept (on entry) --> returned socket is an INCOMING (server) connection
	 * 	
	 *  
	 *  Socket.connect()       -->   unless registered as an incoming, this socket is an OUTGOING (client) connection
	 *  	CONNECTIONS: Increment counter in serverConnections or clientConnections
	 *  	ADDRESS_TRAFFIC: [Check that socket is registered in serverAddressIO or clientAddressIO]
	 *  	PORT_TRAFFIC: [Check that socket is registered in serverSocketIO or clientSocketIO]
	 *  	ADDRESS_PORT_TRAFFIC: [Check that socket is registered in serverSocketIO or clientSocketIO]
	 *  
	 *  I/O Activity:
	 *  =============
	 *  
	 *  Socket.getInputStream()
	 *  Socket.getOutputStream()
	 *  	CONNECTIONS: None
	 *  	ADDRESS_TRAFFIC: Add stream and local/remote InetAddresses to outputAddresses or inputAddresses
	 *  	PORT_TRAFFIC: Add stream and local/remote InetSocketAddresses to outputPortAddresses or inputPortAddresses
	 *  	ADDRESS_PORT_TRAFFIC: Add stream and local/remote InetSocketAddresses to outputPortAddresses or inputPortAddresses
	 *  
	 *  SocketOutputStream.write(...)
	 *  SocketInputStream.read(...)
	 *  	CONNECTIONS: None
	 *  	ADDRESS_TRAFFIC: Add byte count to serverAddressIO or clientAddressIO
	 *  	PORT_TRAFFIC: Add byte count to serverSocketIO or clientSocketIO
	 *  	ADDRESS_PORT_TRAFFIC: Add byte count to serverSocketIO or clientSocketIO
	 *  
	 *  Closed Connections:
	 *  ===================
	 *  Socket.close()
	 *  	CONNECTIONS: Decrement counter in serverConnections or clientConnections
	 *  	ADDRESS_TRAFFIC: [Check that socket is de-registered from serverAddressIO or clientAddressIO  & clear outputAddresses or inputAddresses]
	 *  	PORT_TRAFFIC: [Check that socket is de-registered from serverSocketIO or clientSocketIO  & clear outputPortAddresses or inputPortAddresses]
	 *  	ADDRESS_PORT_TRAFFIC: [Check that socket is de-registered from serverSocketIO or clientSocketIO  & clear outputPortAddresses or inputPortAddresses]
	 *  
	 *  NOTE: No collection diff between PORT_TRAFFIC and ADDRESS_PORT_TRAFFIC. Flush thread will accumulate address level I/O on flush. 
	 */

	//===============================================================================
	//   Tracking of sockets for socket streams used when using ADDRESS_TRAFFIC
	//   This saves us from having to reflect out the socket on every call.
	//===============================================================================	
	/** A map of of addresses (local and remote) keyed by an opaque socket output stream */
	protected static final ConcurrentHashMap<OutputStream, InetAddress[]> outputAddresses = new ConcurrentHashMap<OutputStream, InetAddress[]>(128, 0.75f, 16); 
	/** A map of of addresses (local and remote) keyed by an opaque socket input stream */
	protected static final ConcurrentHashMap<InputStream, InetAddress[]> inputAddresses = new ConcurrentHashMap<InputStream, InetAddress[]>(128, 0.75f, 16);
	//===============================================================================
	//   Tracking of sockets for socket streams used when using >= PORT_TRAFFIC 
	//   This saves us from having to reflect out the socket on every call.
	//===============================================================================	
	/** A map of of addresses (local and remote) keyed by an opaque socket output stream */
	protected static final ConcurrentHashMap<OutputStream, InetSocketAddress[]> outputPortAddresses = new ConcurrentHashMap<OutputStream, InetSocketAddress[]>(128, 0.75f, 16); 
	/** A map of of addresses (local and remote) keyed by an opaque socket input stream */
	protected static final ConcurrentHashMap<InputStream, InetSocketAddress[]> inputPortAddresses = new ConcurrentHashMap<InputStream, InetSocketAddress[]>(128, 0.75f, 16); 
	
	//===================================================================
	//   Accumulators used when we're tracing at at least PORT_TRAFFICconnection
	//===================================================================
	/** Two member arrays of interval accumulators for IN/OUT traffic marking, keyed by the local server socket the i/o occurs on. */
	protected static final ConcurrentHashMap<Socket, ConcurrentLongSlidingWindow[]> serverSocketIO = new ConcurrentHashMap<Socket, ConcurrentLongSlidingWindow[]>(128, 0.75f, 16); 
	/** Two member arrays of interval accumulators for IN/OUT traffic marking, keyed by the local client socket the i/o occurs on. */
	protected static final ConcurrentHashMap<Socket, ConcurrentLongSlidingWindow[]> clientSocketIO = new ConcurrentHashMap<Socket, ConcurrentLongSlidingWindow[]>(128, 0.75f, 16);
	//===================================================================
	//   Accumulators used when we're tracing at ADDRESS_TRAFFIC
	//===================================================================
	/** Two member arrays of interval accumulators for IN/OUT traffic marking, keyed by the local server socket the i/o occurs on. */
	protected static final ConcurrentHashMap<InetAddress, ConcurrentLongSlidingWindow[]> serverAddressIO = new ConcurrentHashMap<InetAddress, ConcurrentLongSlidingWindow[]>(128, 0.75f, 16); 
	/** Two member arrays of interval accumulators for IN/OUT traffic marking, keyed by the local client socket the i/o occurs on. */
	protected static final ConcurrentHashMap<InetAddress, ConcurrentLongSlidingWindow[]> clientAddressIO = new ConcurrentHashMap<InetAddress, ConcurrentLongSlidingWindow[]>(128, 0.75f, 16);
	//===================================================================
	//   Accumulators used when we're tracing at CONNECTIONS
	//===================================================================
	/** Counters for active connections INTO this JVM */
	protected static final ConcurrentHashMap<InetAddress, AtomicInteger> serverConnections = new ConcurrentHashMap<InetAddress, AtomicInteger>(128, 0.75f, 16); 
	/** Counters for active connections INTO this JVM */
	protected static final ConcurrentHashMap<InetAddress, AtomicInteger> clientConnections = new ConcurrentHashMap<InetAddress, AtomicInteger>(128, 0.75f, 16); 

	
	
	/** A set of pending closed sockets that will cleared after the next flush */
	protected static final Set<Socket> closedSockets = new CopyOnWriteArraySet<Socket>();
	
	/**
	 * Creates a new APMSocketMonitorHelper
	 * @param rule The rule that triggers the helper load
	 */
	public APMSocketMonitorHelper(Rule rule) {
		super(rule);
	}
	
	/**
	 * <p>Starts tracking on the  passed socket. Called when:<ol>
	 * 	<li><b><code>ServerSocket.implAccept (entry)</code></b>: Passed socket is not connected, but we track it so we know it is a server accepted.</li>
	 *  <li><b><code>Socket.connect (exit)</code></b>: Passed socket is connected so we can get the remote/local addresses. If the socket is not registered as a server socket, it is a client socket.</li>
	 * </ol></p>
	 * <p>Note that server accepted sockets will be passed here twice, once on implAccept and once on connect. 
	 * @param socket The socket to track
	 * NOTE: An important distinction, INCOMING sockets are not connected yet so do not have local/remote addresses. 
	 */
	protected void trackSocket(Socket socket) {
		
		switch(tracingLevel.get()) {
			case CONNECTIONS:
				
				// add address counters if not exist
				// if outgoing, add addresses
				break;
			case ADDRESS_TRAFFIC:				
				break;
			default:				
		}
		ConcurrentLongSlidingWindow[] cls = new ConcurrentLongSlidingWindow[2];
		if((serverAccepted ? serverSocketIO : clientSocketIO).putIfAbsent(socket, cls)==null) {
			cls[0] = new ConcurrentLongSlidingWindow(128);
			cls[1] = new ConcurrentLongSlidingWindow(128);
		}
	}
	
	/**
	 * Traces a server socket bind
	 * @param ss The server socket
	 * @param sa The socket address
	 * @param backlog The bind backlog
	 */
	public void traceBoundServerSocket(ServerSocket ss, SocketAddress sa, int backlog) {	
		String host = ((InetSocketAddress)sa).getAddress().getHostAddress();
		String port = "" + ss.getLocalPort();
		SimpleLogger.info("\n\tServerSocket Bind [", host, ":", port, "]  Backlog:", backlog);
		traceCounter(1, "ServerSocketBind", "java", "net", "server", host, port);
		traceCounter(backlog, "ServerSocketBacklog", "java", "net", "server", host, port);
	}
	
	/**
	 * Traces a server socket bind with a default backlog of 50
	 * @param ss The server socket
	 * @param sa The socket address
	 */
	public void traceBoundServerSocket(ServerSocket ss, SocketAddress sa) {
		traceBoundServerSocket(ss, sa, 50);
	}
	
	/**
	 * Traces a server socket accept of a remote connection
	 * @param ss The server socket that accepted
	 * @param as The accepted socket created
	 */
	public void traceServerSocketAccept(ServerSocket ss, Socket as) {
		//trackSocket(as, true);
		SimpleLogger.info("\n\tServerSocket Accepted  on [", ss.getLocalSocketAddress(), "]");
	}
	
	/**
	 * Traces a server socket accept of a remote connection
	 * @param ss The server socket that accepted
	 * @param as The accepted socket created
	 */
	public void traceServerSockAccept(Object ss, Object as) {
		SimpleLogger.info("\n\tServerSocket Accept [", ss.getClass().getName(), "]:[", as.getClass().getName(), "]");
	}
	
	
	public void traceSocketIO(Object[] args) {
		Socket as = (Socket)getFieldValue(args[0], "socket");
		InetSocketAddress localSocket = (InetSocketAddress)as.getLocalSocketAddress();
		InetSocketAddress remoteSocket = (InetSocketAddress)as.getRemoteSocketAddress();
		int bytesMoved = 0;
		if(args.length==4) {
			bytesMoved = (Integer)args[3];
		} else {
			if(args[1].getClass()==byte[].class) {
				bytesMoved = ((byte[])args[1]).length;
			} else {
				bytesMoved = 1;
			}
		}
		//SimpleLogger.info("\n\tSocket Write [", bytesWritten, "] bytes\n\t  Socket Local:", localSocket.getAddress().getHostAddress(), ":", localSocket.getPort(), "\n\t  Socket Remote:", remoteSocket.getAddress().getHostAddress(), ":", remoteSocket.getPort());
	}
	
	/**
	 * Returns the socket from an opaque SocketInputStream
	 * @param obj the opaque SocketInputStream
	 * @return the SocketInputStream's socket or null if one was not found
	 */
	protected Socket getInputSocket(Object obj) {
		if(obj==null) return null;
		try {
			return (Socket) SOCK_IN_FIELD.get(obj);
		} catch (Exception ex) {
			return null;
		}
	}
	
	/**
	 * Returns the socket from an opaque SocketOutputStream
	 * @param obj the opaque SocketOutputStream
	 * @return the SocketOutputStream's socket or null if one was not found
	 */
	protected Socket getOutputSocket(Object obj) {
		if(obj==null) return null;
		try {
			return (Socket) SOCK_OUT_FIELD.get(obj);
		} catch (Exception ex) {
			return null;
		}
	}
	
	
	public void traceSocketWrite(Object[] args) {
		//outputIO
		Socket sock = (Socket)getFieldValue(args[0], "socket");
//		InetSocketAddress localSocket = (InetSocketAddress)as.getLocalSocketAddress();
//		InetSocketAddress remoteSocket = (InetSocketAddress)as.getRemoteSocketAddress();
		int bytesWritten = 0;
		if(args.length==4) {
			bytesWritten = (Integer)args[3];
		} else {
			if(args[1].getClass()==byte[].class) {
				bytesWritten = ((byte[])args[1]).length;
			} else {
				bytesWritten = 1;
			}
		}
				
	}
	
	public void traceSocketRead(Object[] args) {
		Socket as = (Socket)getFieldValue(args[0], "socket");
		InetSocketAddress localSocket = (InetSocketAddress)as.getLocalSocketAddress();
		InetSocketAddress remoteSocket = (InetSocketAddress)as.getRemoteSocketAddress();
		int bytesWritten = 0;
		if(args.length==4) {
			bytesWritten = (Integer)args[3];
		} else {
			if(args[1].getClass()==byte[].class) {
				bytesWritten = ((byte[])args[1]).length;
			} else {
				bytesWritten = 1;
			}
		}
		SimpleLogger.info("\n\tSocket Read [", bytesWritten, "] bytes\n\t  Socket Local:", localSocket.getAddress().getHostAddress(), ":", localSocket.getPort(), "\n\t  Socket Remote:", remoteSocket.getAddress().getHostAddress(), ":", remoteSocket.getPort());		
	}
	
	/**
	 * Adds the passed socket to the pending close set
	 * @param socket the closed socket
	 */
	public void traceSocketClosed(Socket socket) {
		closedSockets.add(socket);
	}
	
	/**
	 * Starts tracking on a socket connect.
	 * If the passed socket was not tracked as a {@link ServerSocket#accept()} it will be assumed to be a client socket.
	 * @param socket the connecting socket
	 */
	public void traceSocketConnect(Socket socket) {
		
	}

	
	

}



///**
//* Called when the first instance of this helper class is instantiated for an active rule
//*/
//public static void activated() {		
//	APMAgentHelper.activated();
//	Class<?>[] publified = null;
//	try {
//		publified = ClassPublifier.getInstance().publify(true, Class.forName("java.net.SocketOutputStream"), Class.forName("java.net.SocketInputStream"));
//		SimpleLogger.info("Publified SocketMonitor Classes:" + publified.length);
//		StringBuilder b = new StringBuilder("\nPublified SocketMonitor Classes:");
//		for(Class<?> clazz: publifiedClasses) {
//			b.append("\n\t[").append(clazz.getName()).append("]  Public:").append(Modifier.isPublic(clazz.getModifiers()));
//		}
//		b.append("\n");
//		SimpleLogger.info(b);
//		Collections.addAll(publifiedClasses, publified);
//	} catch (Exception ex) {
//		SimpleLogger.error("Failed to publify socket streams", ex);
//		throw new RuntimeException("Failed to publify socket streams", ex);
//	}
//}
//
///**
//* Called when the last rule using this helper class is uninstalled
//*/
//public static void deactivated() {
//	APMAgentHelper.deactivated();
//	Class<?>[] reverted = null;
//	try {
//		reverted = ClassPublifier.getInstance().revert(true, Class.forName("java.net.SocketOutputStream"), Class.forName("java.net.SocketInputStream"));
//		SimpleLogger.info("Reverted SocketMonitor Classes:" + reverted.length);
//		for(Class<?> clazz: reverted) {
//			publifiedClasses.remove(clazz);
//		}
//		if(!publifiedClasses.isEmpty()) {
//			StringBuilder b = new StringBuilder("\nUnexpected SocketMonitor Publified Classes after Helper Deactivation:");
//			for(Class<?> clazz: publifiedClasses) {
//				b.append("\n\t[").append(clazz.getName()).append("]-->").append(clazz.getClassLoader());
//			}
//			b.append("\n");
//			SimpleLogger.warn(b);
//		}
//	} catch (Exception ex) {
//		SimpleLogger.error("Failed to revert publified socket streams", ex);
//		throw new RuntimeException("Failed to revert publify socket streams", ex);			
//	}
//}

