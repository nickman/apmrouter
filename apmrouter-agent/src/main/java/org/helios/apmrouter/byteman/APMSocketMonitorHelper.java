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

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Set;

import org.cliffc.high_scale_lib.Counter;
import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.cliffc.high_scale_lib.NonBlockingHashSet;
import org.helios.apmrouter.byteman.SocketTracingLevel.SocketTracingLevelListener;
import org.helios.apmrouter.byteman.SocketTracingLevel.SocketTracingLevelWatcher;
import org.helios.apmrouter.collections.ConcurrentLongSlidingWindow;
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
			/* No Op */
		}

		@Override
		public void toNull(SocketTracingLevel oldLevel) {
			/* No Op */
		}

		@Override
		public void change(SocketTracingLevel oldLevel, SocketTracingLevel newLevel) {
			/* No Op */
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
	//   Tracking of socket streams per socket so we can clean them when the socket closes.
	//===============================================================================	
	/** A map of of opaque socket streams keyed by socket output stream */
	protected static final NonBlockingHashMap<Socket, Closeable[]> socketStreams = new NonBlockingHashMap<Socket, Closeable[]>(128); 

	//===============================================================================
	//   Tracking of sockets for socket streams used when using ADDRESS_TRAFFIC
	//   This saves us from having to reflect out the socket on every call.
	//===============================================================================	
	/** A map of of remote addresses keyed by an opaque socket output stream */
	protected static final NonBlockingHashMap<OutputStream, InetAddress> outputAddresses = new NonBlockingHashMap<OutputStream, InetAddress>(128); 
	/** A map of of remote addresses keyed by an opaque socket input stream */
	protected static final NonBlockingHashMap<InputStream, InetAddress> inputAddresses = new NonBlockingHashMap<InputStream, InetAddress>(128);
	//===============================================================================
	//   Tracking of sockets for socket streams used when using >= PORT_TRAFFIC 
	//   This saves us from having to reflect out the socket on every call.
	//===============================================================================	
	/** A map of of remote addresses keyed by an opaque socket output stream */
	protected static final NonBlockingHashMap<OutputStream, InetSocketAddress> outputPortAddresses = new NonBlockingHashMap<OutputStream, InetSocketAddress>(128); 
	/** A map of of remote addresses keyed by an opaque socket input stream */
	protected static final NonBlockingHashMap<InputStream, InetSocketAddress> inputPortAddresses = new NonBlockingHashMap<InputStream, InetSocketAddress>(128); 
	
	//===================================================================
	//   Accumulators used when we're tracing at at least PORT_TRAFFICconnection
	//===================================================================
	/** Two member arrays of interval accumulators for IN/OUT traffic marking, keyed by the local server socket the i/o occurs on. */
	protected static final NonBlockingHashMap<Socket, ConcurrentLongSlidingWindow[]> serverSocketIO = new NonBlockingHashMap<Socket, ConcurrentLongSlidingWindow[]>(128); 
	/** Two member arrays of interval accumulators for IN/OUT traffic marking, keyed by the local client socket the i/o occurs on. */
	protected static final NonBlockingHashMap<Socket, ConcurrentLongSlidingWindow[]> clientSocketIO = new NonBlockingHashMap<Socket, ConcurrentLongSlidingWindow[]>(128);
	//===================================================================
	//   Accumulators used when we're tracing at ADDRESS_TRAFFIC
	//===================================================================
	/** Two member arrays of interval accumulators for IN/OUT traffic marking, keyed by the local server socket the i/o occurs on. */
	protected static final NonBlockingHashMap<InetAddress, ConcurrentLongSlidingWindow[]> serverAddressIO = new NonBlockingHashMap<InetAddress, ConcurrentLongSlidingWindow[]>(128); 
	/** Two member arrays of interval accumulators for IN/OUT traffic marking, keyed by the local client socket the i/o occurs on. */
	protected static final NonBlockingHashMap<InetAddress, ConcurrentLongSlidingWindow[]> clientAddressIO = new NonBlockingHashMap<InetAddress, ConcurrentLongSlidingWindow[]>(128);
	//===================================================================
	//   Accumulators used when we're tracing at CONNECTIONS
	//===================================================================
	/** Counters for active connections INTO this JVM */
	protected static final NonBlockingHashMap<InetAddress, Counter> serverConnections = new NonBlockingHashMap<InetAddress, Counter>(128); 
	/** Counters for active connections INTO this JVM */
	protected static final NonBlockingHashMap<InetAddress, Counter> clientConnections = new NonBlockingHashMap<InetAddress, Counter>(128); 
	//===================================================================
	//   Basic server and client socket tracking combined with possible
	//   read and write counts. 
	//===================================================================
	/** Counters for active connections INTO this JVM */
	protected static final NonBlockingHashMap<Socket, Counter[]> serverSockets = new NonBlockingHashMap<Socket, Counter[]>(128); 
	/** Counters for active connections INTO this JVM */
	protected static final NonBlockingHashMap<Socket, Counter[]> clientSockets = new NonBlockingHashMap<Socket, Counter[]>(128); 

	
	
	/** A set of pending closed sockets that will cleared after the next flush */
	protected static final Set<Socket> closedSockets = new NonBlockingHashSet<Socket>();
	
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
	public void trackSocket(Socket socket) {
		if(socket==null) return;
		
		// ==========================================================================
		//	Tracked Socket Registration
		// ==========================================================================
		
		Counter[] counters = new Counter[2];
		if(!socket.isConnected()) {
			// Add the socket to the serverSocket tracker, 
			// since only server sockets will come here unconnected
			if(serverSockets.putIfAbsent(socket, counters)==null) {
				counters[0] = new Counter(); counters[1] = new Counter();
			}
		} else {
			boolean addedClientSocket = false;
			// If the socket is not a server socket, it's a client socket
			if(!serverSockets.containsKey(socket)) {
				if(clientSockets.putIfAbsent(socket, counters)==null) {
					counters[0] = new Counter(); counters[1] = new Counter();
					addedClientSocket = true;
				}				
			} else {
				// This means the socket is a server socket and is now connected
				initConnectedServerSocket(socket);
			}
			// ==========================================================================
			//	Tracked Socket Address Registration
			// ==========================================================================
				// if the socket is new and connected, we can register the local and remote addresses.
			if(addedClientSocket) {
				initConnectedClientSocket(socket);
			}
		}
	}
	
	/**
	 * Determines if the passed socket is tracked at all
	 * @param socket The socket to test
	 * @return true if the socket is tracked, false otherwise
	 */
	public static boolean isSocketTracked(Socket socket) {
		if(socket==null) return false;
		return clientSockets.containsKey(socket) || serverSockets.containsKey(socket);
	}
	
	/**
	 * Determines if the passed socket is a tracked client socket
	 * @param socket The socket to test
	 * @return true if the socket is a tracked client socket, false otherwise
	 */
	public static boolean isSocketClientTracked(Socket socket) {
		if(socket==null) return false;
		return clientSockets.containsKey(socket);
	}
	
	/**
	 * Determines if the passed socket is a tracked server side socket
	 * @param socket The socket to test
	 * @return true if the socket is a tracked server side socket, false otherwise
	 */
	public static boolean isSocketServerTracked(Socket socket) {
		if(socket==null) return false;
		return serverSockets.containsKey(socket);
	}
	
	
	/**
	 * Initializes the tracking counters for the passed connected socket
	 * @param socket The socket to initialize for
	 */
	protected void initConnectedClientSocket(Socket socket) {
		if(socket==null || !socket.isConnected()) return;
		final SocketTracingLevel level = tracingLevel.get();  // the address we're connected to
		final InetAddress remoteAddress = socket.getInetAddress();
		if(level.isActiveConnections()) {
			// ==============================================================================
			// Increment the counter for clientConnections for the sockets remote address
			// ==============================================================================
			clientConnections.putIfAbsent(remoteAddress, new Counter());
			clientConnections.get(remoteAddress).increment();
		} else if(level.isAddressCollecting()) {
			socketStreams.putIfAbsent(socket, new Closeable[2]);
			// ==============================================================================
			// Initialize clientAddressIO with the traffic counters 
			// ==============================================================================
			ConcurrentLongSlidingWindow[] cls = new ConcurrentLongSlidingWindow[2];
			if(clientAddressIO.putIfAbsent(remoteAddress, cls)==null) {
				cls[0] = new ConcurrentLongSlidingWindow(128);
				cls[1] = new ConcurrentLongSlidingWindow(128);				
			}						
		} else if(level.isPortCollecting()) {
			socketStreams.putIfAbsent(socket, new Closeable[2]);
			// ==============================================================================
			// Initialize clientSocketIO with the traffic counters 
			// ==============================================================================
			ConcurrentLongSlidingWindow[] cls = new ConcurrentLongSlidingWindow[2];
			if(clientSocketIO.putIfAbsent(socket, cls)==null) {
				cls[0] = new ConcurrentLongSlidingWindow(128);
				cls[1] = new ConcurrentLongSlidingWindow(128);				
			}									
		}
	}
	
	/**
	 * Initializes the tracking counters for the passed connected socket
	 * @param socket The socket to initialize for
	 */
	protected void initConnectedServerSocket(Socket socket) {
		if(socket==null || !socket.isConnected()) return;
		final SocketTracingLevel level = tracingLevel.get();
		final InetAddress remoteAddress = socket.getInetAddress(); // the address connecting to us
		if(level.isActiveConnections()) {
			// ==============================================================================
			// Increment the counter for serverConnections for the sockets remote address
			// ==============================================================================
			serverConnections.putIfAbsent(remoteAddress, new Counter());
			serverConnections.get(remoteAddress).increment();
		} else if(level.isAddressCollecting()) {
			// ==============================================================================
			// Initialize serverAddressIO with the traffic counters 
			// ==============================================================================
			ConcurrentLongSlidingWindow[] cls = new ConcurrentLongSlidingWindow[2];
			if(serverAddressIO.putIfAbsent(remoteAddress, cls)==null) {
				cls[0] = new ConcurrentLongSlidingWindow(128);
				cls[1] = new ConcurrentLongSlidingWindow(128);				
			}						
		} else if(level.isPortCollecting()) {
			// ==============================================================================
			// Initialize serverSocketIO with the traffic counters 
			// ==============================================================================
			ConcurrentLongSlidingWindow[] cls = new ConcurrentLongSlidingWindow[2];
			if(serverSocketIO.putIfAbsent(socket, cls)==null) {
				cls[0] = new ConcurrentLongSlidingWindow(128);
				cls[1] = new ConcurrentLongSlidingWindow(128);				
			}									
		}
	}
	
	/**
	 * Initializes counters for input stream (read) counters
	 * Should be bound to {@link Socket#getInputStream()} (exit).
	 * @param socket The socket for which the input stream is being acquired
	 * @param is The socket input stream
	 */
	public void trackInputStream(Socket socket, InputStream is) {
		if(is==null) return;
		final SocketTracingLevel level = tracingLevel.get();
		if(level.isActiveConnections()) {
			// disable rule ?
			return;
		}
		if(!isSocketTracked(socket)) {
			trackSocket(socket);
		}
		// Store the association of the socketinput stream to the socket, 
		// so we can clear the streams references after the socket closes.
		socketStreams.get(socket)[INPUT] = is;
		if(level.isAddressCollecting()) {
			// Store the association of the input stream to the remote address
			inputAddresses.put(is, socket.getInetAddress());
		} else if(level.isPortCollecting()) {
			// Store the association of the input stream to the remote port
			inputPortAddresses.put(is, (InetSocketAddress)socket.getRemoteSocketAddress());			
		}
	}
	
	/**
	 * Initializes counters for output stream (write) counters
	 * Should be bound to {@link Socket#getOutputStream()} (exit).
	 * @param socket The socket for which the output stream is being acquired
	 * @param os The socket output stream
	 * 
	 */
	public void trackOutputStream(Socket socket, OutputStream os) {
		if(os==null) return;
		final SocketTracingLevel level = tracingLevel.get();
		if(level.isActiveConnections()) {
			// disable rule ?
			return;
		}		
		if(!isSocketTracked(socket)) {
			trackSocket(socket);
		}
		// Store the association of the socketoutput stream to the socket, 
		// so we can clear the streams references after the socket closes.
		socketStreams.get(socket)[OUTPUT] = os;
		if(level.isAddressCollecting()) {
			// Store the association of the output stream to the remote address
			outputAddresses.put(os, socket.getInetAddress());
		} else if(level.isPortCollecting()) {
			// Store the association of the output stream to the remote port
			outputPortAddresses.put(os, (InetSocketAddress)socket.getRemoteSocketAddress());			
		}
	}	
	
	/**
	 * Records the number of bytes written to the passed socket
	 * @param socket the socket written to
	 * @param bytes The number of bytes written
	 */
	public void socketWrite(Socket socket, int bytes) {
		if(socket==null) return;
		final SocketTracingLevel level = tracingLevel.get();
		if(level.isActiveConnections()) {
			// disable rule ?
			return;
		}		
		if(!isSocketTracked(socket)) {
			trackSocket(socket);
		}
		if(level.isAddressCollecting()) {
			// Add the byte count to the sliding window associated to the socket's remote address
			if(isSocketServerTracked(socket)) {
				serverAddressIO.get(socket)[INPUT].insert(bytes);
			} else {
				clientAddressIO.get(socket)[INPUT].insert(bytes);
			}			
		} else if(level.isPortCollecting()) {
			// Add the byte count to the sliding window associated to the socket's remote port
			if(isSocketServerTracked(socket)) {
				serverSocketIO.get(socket)[INPUT].insert(bytes);
			} else {
				clientSocketIO.get(socket)[INPUT].insert(bytes);
			}			
		}
	}
	
	/**
	 * Records the number of bytes read from the passed socket
	 * @param socket the socket read from
	 * @param bytes The number of bytes read
	 */
	public void socketRead(Socket socket, int bytes) {
		if(socket==null) return;
		final SocketTracingLevel level = tracingLevel.get();
		if(level.isActiveConnections()) {
			// disable rule ?
			return;
		}		
		if(!isSocketTracked(socket)) {
			trackSocket(socket);
		}
		if(level.isAddressCollecting()) {
			// Add the byte count to the sliding window associated to the socket's remote address
			if(isSocketServerTracked(socket)) {
				serverAddressIO.get(socket)[OUTPUT].insert(bytes);
			} else {
				clientAddressIO.get(socket)[OUTPUT].insert(bytes);
			}			
		} else if(level.isPortCollecting()) {
			// Add the byte count to the sliding window associated to the socket's remote port
			if(isSocketServerTracked(socket)) {
				serverSocketIO.get(socket)[OUTPUT].insert(bytes);
			} else {
				clientSocketIO.get(socket)[OUTPUT].insert(bytes);
			}			
		}
	}
	
	
	/**
	 * Fired when a socket closes.
	 * Should be bound to {@link Socket#close()}.
	 * @param socket The closing socket
	 */
	public void socketClosed(Socket socket) {
		if(socket==null) return;
		closedSockets.add(socket);
	}
	

}



///**

//public void traceSocketIO(Object[] args) {
//	Socket as = (Socket)getFieldValue(args[0], "socket");
//	InetSocketAddress localSocket = (InetSocketAddress)as.getLocalSocketAddress();
//	InetSocketAddress remoteSocket = (InetSocketAddress)as.getRemoteSocketAddress();
//	int bytesMoved = 0;
//	if(args.length==4) {
//		bytesMoved = (Integer)args[3];
//	} else {
//		if(args[1].getClass()==byte[].class) {
//			bytesMoved = ((byte[])args[1]).length;
//		} else {
//			bytesMoved = 1;
//		}
//	}
//	//SimpleLogger.info("\n\tSocket Write [", bytesWritten, "] bytes\n\t  Socket Local:", localSocket.getAddress().getHostAddress(), ":", localSocket.getPort(), "\n\t  Socket Remote:", remoteSocket.getAddress().getHostAddress(), ":", remoteSocket.getPort());
//}




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

