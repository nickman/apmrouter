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

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.ObjectName;

import org.cliffc.high_scale_lib.Counter;
import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.cliffc.high_scale_lib.NonBlockingHashSet;
import org.helios.apmrouter.byteman.APMAgentHelper;
import org.helios.apmrouter.byteman.APMSocketMonitorHelper;
import org.helios.apmrouter.collections.ConcurrentLongSlidingWindow;
import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.trace.TracerFactory;
import org.helios.apmrouter.util.SimpleLogger;
import org.helios.apmrouter.util.SystemClock;
import org.helios.apmrouter.util.SystemClock.ElapsedTime;
import org.jboss.byteman.rule.Rule;

/**
 * <p>Title: SocketMonitor</p>
 * <p>Description: Byteman socket monitor</p> 
 * <p><a href="http://apmrouter.blogspot.com/2012/12/socket-monitoring-instrumentation.html">Docs.</a></p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.byteman.sockets.SocketMonitor</code></p>
 */

public class SocketMonitor extends APMAgentHelper {
	/** The default size of collections holding socket constructs */
	public static final int DEFAULT_ACC_SIZE = 128;  
	/** The default flush period in ms. */
	public static final int DEFAULT_FLUSH_PERIOD = 5000;  

	/** The configured accumulator size */
	protected static int accumulatorSize = DEFAULT_ACC_SIZE;
	/** The configured flush period in ms. */
	protected static long flushPeriod = DEFAULT_FLUSH_PERIOD;
	/** The configured SocketTracingLevel */
	protected static SocketTracingLevel tracingLevel = SocketTracingLevel.CONNECTIONS;
	
	/** The flush scheduler */
	protected static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory(){
		private final AtomicInteger serial = new AtomicInteger(0);
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "SocketMonitorFlushThread#" + serial.incrementAndGet());
			t.setDaemon(true);
			return t;
		}
	});
	
	/** The current flush schedule handle */
	protected static ScheduledFuture<?> flushSchedule = null;
	
	/** The array index for socket input (reads) */
	public static final int INPUT = 0;
	/** The array index for socket output (writes) */
	public static final int OUTPUT = 1;
	/** The array index for the local socket */
	public static final int LOCAL = 0;
	/** The array index for the remote socket */
	public static final int REMOTE = 0;
	
	//==========================================================================================
	//   JMX Ops and Attrs for SocketHelper MBean
	//==========================================================================================
	
	/** The JMX MBean ObjectName */
	protected static final ObjectName objectName = JMXHelper.objectName(APMSocketMonitorHelper.class.getPackage().getName() + ":helper=" + SocketMonitor.class.getSimpleName());
	
	
	/**
	 * <p>Title: SocketMonitorJMX</p>
	 * <p>Description: JMX management for the SocketMonitor helper.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.byteman.sockets.SocketMonitor.SocketMonitorJMX</code></p>
	 */
	public static class SocketMonitorJMX implements SocketMonitorMXBean {

		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.byteman.sockets.SocketMonitorMXBean#getSocketTracingLevel()
		 */
		@Override
		public String getSocketTracingLevel() {
			return tracingLevel.name();
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.byteman.sockets.SocketMonitorMXBean#setSocketTracingLevel(java.lang.String)
		 */
		@Override
		public void setSocketTracingLevel(String level) {
			tracingLevel = SocketTracingLevel.valueOfName(level);
			
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.byteman.sockets.SocketMonitorMXBean#getServerSocketCount()
		 */
		@Override
		public int getServerSocketCount() {
			return ServerSocketTracker.serverSideSockets.size();
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.byteman.sockets.SocketMonitorMXBean#getAcceptedConnectionCounts()
		 */
		@Override
		public Map<String, Long> getAcceptedConnectionCountsByAddress() {
			return ServerSocketTracker.getAcceptedConnectionCountsByAddress();
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.byteman.sockets.SocketMonitorMXBean#getClientSocketCount()
		 */
		@Override
		public int getClientSocketCount() {
			return SocketTracker.CTRACKERS.size();
		}
		
	}

	/** The flush runnable */
	protected static final Runnable flushProcedure = new Runnable() {
		@Override
		public void run() {
			SystemClock.startTimer();
			ServerSocketTracker.flush();
			SocketTracker.flush();
			//SimpleLogger.info("\n\tSocketMonitor Flushing....");
//			flushData();
//			cleanClosedSockets();
			ElapsedTime et = SystemClock.endTimer();
			//SimpleLogger.info("SocketMonitor Flush completed in [", et, "]");			
		}
	};
	
	//================================================================================
	//		SocketMonitor configuration methods, called by the rule script execution
	//================================================================================
	
	/**
	 * Sets the size of the collections created to track socket activity.
	 * Defaults to 128.
	 * @param accSize The size of the collections
	 */
	public void setAccumulatorSize(int accSize) {
		accumulatorSize = accSize;
	}
	
	/**
	 * Sets the flush period in ms.
	 * Defaults to 5000.
	 * @param flushPeriod The flush period in ms.
	 */
	public void setFlushPeriod(long flushPeriod) {
		SocketMonitor.flushPeriod = flushPeriod;
	}
	
	/**
	 * Sets the socket tracing level.
	 * Defaults to CONNECTIONS.
	 * @param levelName The name of the level to set to, i.e. on the enums from {@link SocketTracingLevel}.
	 */
	public void setTracingLevel(String levelName) {
		SocketTracingLevel level = null;
		try {
			level = SocketTracingLevel.valueOfName(levelName);
			tracingLevel = level;
		} catch (IllegalArgumentException ex) {
			SimpleLogger.warn("Invalid SocketTracingLevel Name [", levelName, "]. Reverting to default CONNECTIONS");
			tracingLevel = SocketTracingLevel.CONNECTIONS; 
		}
	}
	
	//================================================================================
	
	/**
	 * Called when the first instance of this helper class is instantiated for an active rule
	 */
	public static void activated() {
		itracer = TracerFactory.getTracer();
		if(!JMXHelper.getHeliosMBeanServer().isRegistered(objectName)) {
			try {
				JMXHelper.getHeliosMBeanServer().registerMBean(new SocketMonitorJMX(), objectName);
			} catch (Exception ex) {
				SimpleLogger.warn("Failed to register SocketMonitor MBean", ex);
			}
		}
		initCollections();
		if(flushSchedule!=null) {
			flushSchedule.cancel(true);
		}
		flushSchedule = scheduler.scheduleWithFixedDelay(flushProcedure, DEFAULT_FLUSH_PERIOD, DEFAULT_FLUSH_PERIOD, TimeUnit.MILLISECONDS);
		SimpleLogger.info("\n\t======================\n\tActivated SocketMonitor\n\t======================\n");
	}
	
	
	/**
	 * Called when the last rule using this helper class is uninstalled
	 */
	public static void deactivated() {
		if(JMXHelper.getHeliosMBeanServer().isRegistered(objectName)) {
			try {
				JMXHelper.getHeliosMBeanServer().unregisterMBean(objectName);
			} catch (Exception ex) {
				/* No Op */
			}
		}
		if(flushSchedule!=null) {
			flushSchedule.cancel(false);
			flushSchedule = null;
		}
		SimpleLogger.info("\n\t======================\n\tDeactivated SocketMonitor\n\t======================\n");
	}
	
	/**
	 * Initializes the tracking collections
	 */
	protected static void initCollections() {
		//serverSockets = new NonBlockingHashMap<ServerSocket, Counter>(accumulatorSize);
	}
	
	
	//==============================================================================
	//   Server Socket Tracking
	//==============================================================================
//	/** A map of tracked server sockets with a counter tracking accept counts for each */
//	protected static NonBlockingHashMap<ServerSocket, Counter> serverSockets = null;
	
	
	/**
	 * Creates a new SocketMonitor
	 * @param rule The rule this helper is being created for
	 */
	public SocketMonitor(Rule rule) {
		super(rule);
	}
	
	/**
	 * Called when a new {@link ServerSocket} is bound and starts listening.
	 * Triggered by {@link ServerSocket#bind(java.net.SocketAddress)} or {@link ServerSocket#bind(java.net.SocketAddress, int)}.
	 * Note that server sockets bound before this rule is installed will not be tracked.
	 * However, once the rule is installed, invocations of {@link ServerSocket#accept()} will start tracking.  
	 * @param serverSocket The bound server socket
	 */
	public void serverSocketBind(ServerSocket serverSocket) {
		ServerSocketTracker.getInstance(serverSocket);		
	}

	
	/**
	 * Starts tracking on the socket created by the {@link ServerSocket#accept()} operation.
	 * Triggered by {@link ServerSocket#accept()}.
	 * @param serverSocket The tracked {@link ServerSocket}
	 * @param socket The connected {@link Socket}
	 */
	public void serverSocketAccept(ServerSocket serverSocket, Socket socket) {
		ServerSocketTracker sst = ServerSocketTracker.getInstance(serverSocket).increment();
		if(!socket.isConnected()) {			
			sst.trackAccept(socket);
		} else {
			sst.trackConnection(socket);
		}
	}
	
	/**
	 * Queues the passed server socket for close and cleanup.
	 * Triggered by {@link ServerSocket#close()}.
	 * @param serverSocket The server socket to close
	 */
	public void serverSocketClose(ServerSocket serverSocket) {
		ServerSocketTracker.getInstance(serverSocket).closeServerSocket(serverSocket);		
	}
	
	/**
	 * Starts tracking on the passed socket.
	 * At this interception, there's no way to differentiate between a client socket and a server socket.
	 * The method {@link SocketTracker#getInstance(Socket)} will determine which it is and start appropriate tracking on the socket.
	 * Triggered by {@link Socket#connect(SocketAddress)} and {@link Socket#connect(SocketAddress, int)}.  
	 * @param socket The connected socket.
	 */
	public void socketConnect(Socket socket) {
		if(!ServerSocketTracker.isTrackedServerSideSocket(socket)) {
			SocketTracker.getInstance(socket);
		}
	}
	
	/**
	 * Queues the passed socket for close and cleanup.
	 * Triggered by {@link Socket#close()}.
	 * @param socket The closed socket.
	 */
	public void socketClose(Socket socket) {
		if(ServerSocketTracker.isTrackedServerSideSocket(socket)) {
			ServerSocketTracker.closeServerSideSocket(socket);
		} else {
			SocketTracker st = SocketTracker.getInstance(socket);
			st.closeSocket();
		}
	}
	
	
	/**
	 * Updates the socket tracker with the socket's opened input stream.
	 * Triggered by {@link Socket#getInputStream()}.
	 * @param socket The socket
	 * @param is The socket's opened input stream
	 */
	public void clientSocketInput(Socket socket, InputStream is) {
		if(tracingLevel.ordinal()>SocketTracingLevel.CONNECTIONS.ordinal()) {
			SocketTracker.getInstance(socket).setInputStream(is);
		}  // else --->  remove rule ?
	}
	
	/**
	 * Updates the socket tracker with the socket's opened output stream.
	 * Triggered by {@link Socket#getOutputStream()}.
	 * @param socket The socket
	 * @param os The socket's opened output stream
	 */
	public void clientSocketOutput(Socket socket, OutputStream os) {
		if(tracingLevel.ordinal()>SocketTracingLevel.CONNECTIONS.ordinal()) {
			SocketTracker.getInstance(socket).setOutputStream(os);
		} // else --->  remove rule ?
	}
	
	
	
	/**
	 * <p>Title: ServerSocketTracker</p>
	 * <p>Description: A container class to track the accept count and created sockets for a ServerSocket.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.byteman.sockets.SocketMonitor.ServerSocketTracker</code></p>
	 */
	protected static class ServerSocketTracker {
		/** A map of the created ServerSocketTracker instances keyed by the tracked {@ServerSocket} */
		final static NonBlockingHashMap<ServerSocket, ServerSocketTracker> TRACKERS = new NonBlockingHashMap<ServerSocket, ServerSocketTracker>(accumulatorSize);
		/** A map of the created ServerSocketTracker instances keyed by the tracked {@ServerSocket}'s local socket address */
		final static NonBlockingHashMap<SocketAddress, ServerSocketTracker> TRACKERS_BY_BIND = new NonBlockingHashMap<SocketAddress, ServerSocketTracker>(accumulatorSize); 
		
		/** A set of pending server socket closes */
		final static NonBlockingHashSet<ServerSocket> pendingCloses = new NonBlockingHashSet<ServerSocket>(); 

		/** A set of accepted sockets (server side sockets) to distinguish between client and server side socket instances */
		final static NonBlockingHashSet<Socket> serverSideSockets = new NonBlockingHashSet<Socket>(); 
		
		
		/** The tracked {@link ServerSocket} */
		final ServerSocket serverSocket;
		/** The total cummulative number of accepts performed by the tracked {@link ServerSocket}. */
		final Counter acceptCount = new Counter();
		/** The number of curently connected clients to the tracked {@link ServerSocket} */
		final Counter connectedClients = new Counter();
		/** A map of connected sockets accepted by the tracked {@link ServerSocket} keyed by the remote socket address of the connector */
		final NonBlockingHashMap<InetSocketAddress, SocketTracker> connectorsBySocketAddress = new NonBlockingHashMap<InetSocketAddress, SocketTracker>(accumulatorSize);
		/** A map of sets of connected sockets accepted by the tracked {@link ServerSocket} keyed by the remote address of the connector */
		final NonBlockingHashMap<InetAddress, NonBlockingHashSet<SocketTracker>> connectorsByAddress = new NonBlockingHashMap<InetAddress, NonBlockingHashSet<SocketTracker>>(accumulatorSize);
		
		/**
		 * Interval flush procedure for server socket tracking  
		 */
		static void flush() {
			for(ServerSocket ss: pendingCloses) {
				
				ServerSocketTracker sst = TRACKERS.remove(ss);
				//itracer.traceGauge(0, "Bound", "SocketMonitor", "ServerSockets", sst.serverSocket.getInetAddress().getHostAddress(), "" + sst.serverSocket.getLocalPort());
				TRACKERS_BY_BIND.remove(ss.getLocalSocketAddress());
				sst.connectorsByAddress.clear();
				sst.connectorsBySocketAddress.clear();
				
			}
			for(ServerSocketTracker sst : TRACKERS.values()) {
				sst._flush();
			}
		}
		
		private void _flush() {
			itracer.traceGauge(1, "Bound", "SocketMonitor", "ServerSockets", serverSocket.getInetAddress().getHostAddress(), "" + serverSocket.getLocalPort());
			itracer.traceDeltaCounter(acceptCount.get(), "Accepts", "SocketMonitor", "ServerSockets", serverSocket.getInetAddress().getHostAddress(), "" + serverSocket.getLocalPort());
			itracer.traceGauge(connectedClients.get(), "ConnectedClients", "SocketMonitor", "ServerSockets", serverSocket.getInetAddress().getHostAddress(), "" + serverSocket.getLocalPort());
			
//			acceptCount.increment();
//			connectedClients.increment();
			
		}
		
		
		/**
		 * Returns a map of the total cummulative accept counts by address
		 * @return a map of the total cummulative accept counts by address
		 */
		public static Map<String, Long> getAcceptedConnectionCountsByAddress() {
			Map<String, Long> map = new HashMap<String, Long>(TRACKERS.size());
			for(Map.Entry<ServerSocket, ServerSocketTracker> entry: TRACKERS.entrySet()) {
				String address = entry.getKey().getInetAddress().getHostName() + ":" + entry.getKey().getLocalPort();
				if(!map.containsKey(address)) map.put(address, 0L);
				map.put(address, map.get(address)+entry.getValue().acceptCount.get());				
			}
			return map;
		}
		/**
		 * Returns the {@link ServerSocketTracker} for the passed {@ServerSocket}
		 * @param serverSocket The {@ServerSocket}
		 * @return the {@link ServerSocketTracker}
		 */
		public static ServerSocketTracker getInstance(ServerSocket serverSocket) {
			ServerSocketTracker sst = TRACKERS.get(serverSocket);
			if(sst==null) {
				synchronized(TRACKERS) {
					sst = TRACKERS.get(serverSocket);
					if(sst==null) {
						sst = new ServerSocketTracker(serverSocket);
						TRACKERS.put(serverSocket, sst);
					}
				}
			}
			return sst;
		}
		
		/**
		 * Returns the server socket tracker for the server socket bound to the passed address.
		 * @param boundAddress The address the server socket is bound to
		 * @return the server socket tracker, or null if one is not found.
		 */
		public static ServerSocketTracker getInstance(SocketAddress boundAddress) {
			return TRACKERS_BY_BIND.get(boundAddress);
		}
		
		/**
		 * Indicates if the passed socket is registered as a server side socket
		 * @param socket The socket to test
		 * @return true if the socket is registered as a server side socket, 
		 * false if the socket is null or not registered as a server side socket 
		 */
		public static boolean isTrackedServerSideSocket(Socket socket) {
			if(socket==null) return false;
			return serverSideSockets.contains(socket);			
		}
		
		/**
		 * Handles a closed server connection to a bound server side socket
		 * @param socket the closed socket
		 */
		public static void closeServerSideSocket(Socket socket) {
			ServerSocketTracker sst = getInstance(socket.getLocalSocketAddress());
			sst.closeServerSide(socket);
		}
		
		
		/**
		 * Creates a new ServerSocketTracker
		 * @param serverSocket The tracked {@link ServerSocket} 
		 */
		private ServerSocketTracker(ServerSocket serverSocket) {
			this.serverSocket = serverSocket;
			TRACKERS_BY_BIND.put(this.serverSocket.getLocalSocketAddress(), this);
		}
		
		/**
		 * Closes and cleans up references to a server side socket
		 * @param socket the closed socket
		 */
		protected void closeServerSide(Socket socket) {
			decrement();
			serverSideSockets.remove(socket);
			connectorsBySocketAddress.remove(socket.getRemoteSocketAddress());
			Set<SocketTracker> socketTrackers = connectorsByAddress.get(socket.getInetAddress());
			SocketTracker socketTracker = SocketTracker.getServerInstance(socket);
			if(socketTrackers!=null) {
				socketTrackers.remove(socketTracker);
			}
			socketTracker.closeSocket();
			
		}
		
		/**
		 * Increments the accept count for the tracked {@link ServerSocket}
		 * @return this tracker 
		 */
		public ServerSocketTracker increment() {
			acceptCount.increment();
			connectedClients.increment();
			return this;
		}
		
		/**
		 * Decrements the connected client count for the tracked {@link ServerSocket}
		 * @return this tracker 
		 */
		public ServerSocketTracker decrement() {
			connectedClients.decrement();
			return this;
		}
		
		
		/**
		 * Registers the passed server socket for close and cleanup in the next flush
		 * @param serverSocket The server socket to be closed
		 * @return this tracker
		 */
		public ServerSocketTracker closeServerSocket(ServerSocket serverSocket) {
			if(serverSocket!=null) {
				pendingCloses.add(serverSocket);
				// terminate and remove all associated sockets
			}
			return this;
		}
		
		/**
		 * Adds a socket that created during a {@link ServerSocket#accept()} but before the create socket's connection.
		 * This allows the created socket to be registered as a server side socket before the {@link Socket#connect(SocketAddress)} callback,
		 * so we know it is a server side socket.
		 * @param socket The connecting socket
		 * @return this tracker
		 */
		public ServerSocketTracker trackAccept(Socket socket) {
			if(socket==null) return this;			
			serverSideSockets.add(socket);
			
			return this;
		}
		
		/**
		 * Adds a socket that connected to this server socket for tracking
		 * @param socket The connecting socket
		 * @return this tracker
		 */
		public ServerSocketTracker trackConnection(Socket socket) {
			if(socket==null) return this;
			serverSideSockets.add(socket);
			if(tracingLevel.isAddressCollecting()) {
				NonBlockingHashSet<SocketTracker> socketTrackers = connectorsByAddress.get(socket.getInetAddress());
				if(socketTrackers==null) {
					socketTrackers = new NonBlockingHashSet<SocketTracker>();
					connectorsByAddress.put(socket.getInetAddress(), socketTrackers);
				}
				socketTrackers.add(SocketTracker.getServerInstance(socket));
			} else if(tracingLevel.isPortCollecting()) {
				connectorsBySocketAddress.putIfAbsent((InetSocketAddress)socket.getRemoteSocketAddress(), SocketTracker.getServerInstance(socket));
			}
			return this;
		}
		
	}
	
	
	
	/**
	 * <p>Title: SocketTracker</p>
	 * <p>Description: A container class to track a socket connection and its traffic. 
	 * SocketTracker maintains three types of collections for indexing references and counters:<ul>
	 * 	<li>Reference collections for tracking instances related to tracked sockets</li>
	 *  <li>Accumulator references for interval accumulation of tracked socket activity</li>
	 *  <li>Counter references for counting events associated to tracked sockets</li>
	 * </ul></p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.byteman.sockets.SocketMonitor.SocketTracker</code></p>
	 */
	protected static class SocketTracker {
		/** A map of the created server side SocketTracker instances keyed by the tracked {@Socket} */
		final static NonBlockingHashMap<Socket, SocketTracker> STRACKERS = new NonBlockingHashMap<Socket, SocketTracker>(accumulatorSize); 
		/** A map of the created client side SocketTracker instances keyed by the tracked {@Socket} */
		final static NonBlockingHashMap<Socket, SocketTracker> CTRACKERS = new NonBlockingHashMap<Socket, SocketTracker>(accumulatorSize); 

		/** A map of registered SocketTracker instances keyed by the tracked {@Socket}'s input stream */
		final static NonBlockingHashMap<InputStream, SocketTracker> INSTREAMS = new NonBlockingHashMap<InputStream, SocketTracker>(accumulatorSize); 
		/** A map of registered SocketTracker instances keyed by the tracked {@Socket}'s output stream */
		final static NonBlockingHashMap<OutputStream, SocketTracker> OUTSTREAMS = new NonBlockingHashMap<OutputStream, SocketTracker>(accumulatorSize); 
		
		/** A map of client side connection counters keyed by the tracked {@Socket}'s remote socket address */
		final static NonBlockingHashMap<InetSocketAddress, Counter> CLIENT_CONNECTION_COUNTERS = new NonBlockingHashMap<InetSocketAddress, Counter>(accumulatorSize); 
		
		
		
		/** The tracked {@link Socket} */
		final Socket socket;
		/** The socket's local address */
		final InetAddress localAddress;
		/** The socket's remote address */
		final InetAddress remoteAddress;
		/** The socket's local socket address */
		final InetSocketAddress localSocketAddress;
		/** The socket's remote address */
		final InetSocketAddress remoteSocketAddress;
		/** The socket's local port */
		final int localPort;
		/** The socket's remote port */
		final int remotePort;
		
		/** The socket's input stream */
		private InputStream is = null;
		/** The socket's output stream */
		private OutputStream os = null;
		
		/** The socket's writes */
		private ConcurrentLongSlidingWindow socketWrites; 
		/** The socket's reads */
		private ConcurrentLongSlidingWindow socketReads; 
		
		
		/**
		 * Interval flush procedure for socket tracking
		 */
		static void flush() {
			
		}
		
		/**
		 * Closes a socket
		 */
		public void closeSocket() {
			Counter counter = CLIENT_CONNECTION_COUNTERS.get(remoteSocketAddress);
			if(counter!=null) counter.decrement();
		}
		
		/**
		 * Sets and indexes the input stream of the tracked socket
		 * @param is the input stream of the tracked socket
		 */
		public void setInputStream(InputStream is) {
			this.is = is;
			INSTREAMS.putIfAbsent(is, this);
			socketReads = new ConcurrentLongSlidingWindow(accumulatorSize);
		}
		/**
		 * Sets and indexes the output stream of the tracked socket
		 * @param os the output stream of the tracked socket
		 */
		public void setOutputStream(OutputStream os) {
			this.os = os;
			OUTSTREAMS.putIfAbsent(os, this);
			socketWrites = new ConcurrentLongSlidingWindow(accumulatorSize);
		}
		
		/**
		 * Records a socket write
		 * @param bytes the number of bytes written
		 */
		public void recordWrite(int bytes) {
			socketWrites.insert(bytes);
		}
		
		/**
		 * Records a socket read
		 * @param bytes the number of bytes read
		 */
		public void recordRead(int bytes) {
			socketReads.insert(bytes);
		}
		
		/**
		 * Returns the socket tracker map
		 * @param serverSide true for server side sockets, false for client side
		 * @return a socket tracker map
		 */
		private static NonBlockingHashMap<Socket, SocketTracker> tracker(boolean serverSide) {
			return serverSide ? STRACKERS : CTRACKERS;
		}
		
		/**
		 * Returns the {@link SocketTracker} for the passed server side {@Socket}
		 * @param socket The {@socket}
		 * @return the {@link SocketTracker}
		 */
		public static SocketTracker getServerInstance(Socket socket) {
			return getInstance(true, socket);
		}
		
		/**
		 * Returns the {@link SocketTracker} for the passed client side {@Socket}
		 * @param socket The {@socket}
		 * @return the {@link SocketTracker}
		 */
		public static SocketTracker getClientInstance(Socket socket) {
			return getInstance(false, socket);
		}
		
		/**
		 * Returns the {@link SocketTracker} for the passed unknown side {@Socket}.
		 * If the socket is not already registered, it will be registered.
		 * To determine the type, the local socket address will be bounced up against
		 * the ServerSocketTracker registry to try and find a match. If a match is found,
		 * the passed socket is a <b>server</b> socket. Otherwise it is a <b>client</b> socket.
		 * @param socket The socket to get the tracker for
		 * @return the socket tracker.
		 */
		public static SocketTracker getInstance(Socket socket) {
			SocketTracker tracker = CTRACKERS.get(socket);
			if(tracker==null) tracker = STRACKERS.get(socket);
			if(tracker==null) tracker = getInstance(ServerSocketTracker.getInstance(socket.getLocalSocketAddress())!=null, socket);
			return tracker;
		}
		
		/**
		 * Returns the {@link SocketTracker} for the passed stream's socket
		 * @param is The input stream
		 * @return the {@link SocketTracker} for the passed stream
		 */
		public static SocketTracker getInstance(InputStream is) {
			SocketTracker tracker = INSTREAMS.get(is);
			if(tracker==null) {
				tracker = getInstance(getStreamSocket(is));
			}
			return tracker;
		}
		
		/**
		 * Returns the {@link SocketTracker} for the passed stream's socket
		 * @param os The output stream
		 * @return the {@link SocketTracker} for the passed stream
		 */
		public static SocketTracker getInstance(OutputStream os) {
			SocketTracker tracker = INSTREAMS.get(os);
			if(tracker==null) {
				tracker = getInstance(getStreamSocket(os));
			}
			return tracker;
		}
		
		
		/**
		 * Returns the {@link SocketTracker} for the passed {@Socket}
		 * @param serverSide true for server side sockets, false for client side
		 * @param socket The {@socket}
		 * @return the {@link SocketTracker}
		 */
		public static SocketTracker getInstance(boolean serverSide, Socket socket) {
			final NonBlockingHashMap<Socket, SocketTracker> TRACKERS = tracker(serverSide); 
			SocketTracker st = TRACKERS.get(socket);
			if(st==null) {
				synchronized(tracker(serverSide)) {
					st = TRACKERS.get(socket);
					if(st==null) {
						st = new SocketTracker(socket);
						TRACKERS.put(socket, st);
					}
				}
			}
			return st;
		}
		
		/**
		 * Creates a new SocketTracker for a <i>connected</i> socket.
		 * @param socket The tracked {@link Socket} 
		 */
		private SocketTracker(Socket socket) {
			this.socket = socket;
			localAddress = socket.getLocalAddress();
			localPort = socket.getLocalPort();
			localSocketAddress = (InetSocketAddress)socket.getLocalSocketAddress();
			remoteAddress = socket.getInetAddress();
			remotePort = socket.getPort();
			remoteSocketAddress = (InetSocketAddress)socket.getRemoteSocketAddress();
			if(!ServerSocketTracker.isTrackedServerSideSocket(socket)) {
				Counter counter = CLIENT_CONNECTION_COUNTERS.get(remoteSocketAddress);
				if(counter==null) {
					synchronized(CLIENT_CONNECTION_COUNTERS) {
						counter = CLIENT_CONNECTION_COUNTERS.get(remoteSocketAddress);
						if(counter==null) {
							counter = new Counter();
							CLIENT_CONNECTION_COUNTERS.put(remoteSocketAddress, counter);
						}
					}
				}
				counter.increment();
			}
			
		}		
		
		/**
		 * Reflects out the socket from a socket input stream
		 * @param is The socket input stream to get the socket for
		 * @return the socket
		 */
		public static Socket getStreamSocket(InputStream is) {
			try {
				return (Socket)SOCK_IN_FIELD.get(is);
			} catch (Exception ex) {
				throw new RuntimeException("Failed to get socket from SocketInputStream", ex);
			}
		}
		
		/**
		 * Reflects out the socket from a socket output stream
		 * @param os The socket output stream to get the socket for
		 * @return the socket
		 */
		public static Socket getStreamSocket(OutputStream os) {
			try {
				return (Socket)SOCK_OUT_FIELD.get(os);
			} catch (Exception ex) {
				throw new RuntimeException("Failed to get socket from SocketOutputStream", ex);
			}
		}
		
		/**
		 * Returns this socket's input stream
		 * @return this socket's input stream or null if it has not been opened
		 */
		public InputStream getInputStream() {
			return is;
		}
		/**
		 * Returns this socket's output stream
		 * @return this socket's output stream or null if it has not been opened
		 */
		public OutputStream getOutputStream() {
			return os;
		}
		
	}	
	
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

}
