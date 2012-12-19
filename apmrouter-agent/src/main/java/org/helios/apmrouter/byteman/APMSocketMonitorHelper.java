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
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

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
	/** A set of publified classes that will be reverted on helper unload */
	protected static final Set<Class<?>> publifiedClasses = new CopyOnWriteArraySet<Class<?>>();
	
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
	
	/** The array index for socket input (reads) */
	public static final int INPUT = 0;
	/** The array index for socket output (writes) */
	public static final int OUTPUT = 1;
	/** The array index for the local socket */
	public static final int LOCAL = 0;
	/** The array index for the remote socket */
	public static final int REMOTE = 0;

	
	/** A map of of socket addresses (local and remote) keyed by an opaque socket output stream */
	protected static final ConcurrentHashMap<OutputStream, InetSocketAddress[]> outputIO = new ConcurrentHashMap<OutputStream, InetSocketAddress[]>(128, 0.75f, 16); 
	/** Two member arrays of socket addresses (local and remote) keyed by an opaque InetSocketAddress input stream */
	protected static final ConcurrentHashMap<InputStream, InetSocketAddress[]> inputIO = new ConcurrentHashMap<InputStream, InetSocketAddress[]>(128, 0.75f, 16); 
	
	/** Two member arrays of interval accumulators for IN/OUT traffic marking, keyed by the local server socket the i/o occurs on. */
	protected static final ConcurrentHashMap<Socket, ConcurrentLongSlidingWindow[]> serverSocketIO = new ConcurrentHashMap<Socket, ConcurrentLongSlidingWindow[]>(128, 0.75f, 16); 
	/** Two member arrays of interval accumulators for IN/OUT traffic marking, keyed by the local client socket the i/o occurs on. */
	protected static final ConcurrentHashMap<Socket, ConcurrentLongSlidingWindow[]> clientSocketIO = new ConcurrentHashMap<Socket, ConcurrentLongSlidingWindow[]>(128, 0.75f, 16);

	
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
	 * Adds the passed socket to the tracked socket map
	 * @param socket The socket to add
	 * @param serverAccepted if true, the socket is a server INCOMING, otherwise it is a client OUTGOING 
	 */
	protected void trackSocket(Socket socket, boolean serverAccepted) {
		ConcurrentLongSlidingWindow[] cls = new ConcurrentLongSlidingWindow[2];
		if((serverAccepted ? serverSocketIO : clientSocketIO).putIfAbsent(socket, cls)==null) {
			cls[0] = new ConcurrentLongSlidingWindow(128);
			cls[1] = new ConcurrentLongSlidingWindow(128);
			try {
			inputIO.put(socket.getInputStream(), new InetSocketAddress[]{
				(InetSocketAddress)socket.getLocalSocketAddress(),
				(InetSocketAddress)socket.getRemoteSocketAddress()
			});
			outputIO.put(socket.getOutputStream(), new InetSocketAddress[]{
				(InetSocketAddress)socket.getLocalSocketAddress(),
				(InetSocketAddress)socket.getRemoteSocketAddress()
			});
			
			} catch (IOException ioe) {
				throw new RuntimeException("Failed to acquire socket streams", ioe);
			}
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
		InetSocketAddress localAddress = (InetSocketAddress)as.getLocalSocketAddress();
		InetSocketAddress remoteAddress = (InetSocketAddress)as.getRemoteSocketAddress();
		trackSocket(as, true);
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

