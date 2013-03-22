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

import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.helios.apmrouter.byteman.sockets.impl.ISocketImpl;
import org.helios.apmrouter.collections.ConcurrentLongSlidingWindow;
import org.helios.apmrouter.nativex.APMSigar;
import org.helios.apmrouter.nativex.TCPSocketState;
import org.helios.apmrouter.util.SimpleLogger;
import org.hyperic.sigar.NetStat;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
	/** A netstat instance for capturing the state of the remote side of this connection */
	protected NetStat remoteNetStat;
	/** A netstat instance for capturing the state of the local (listener server socket) side of this connection */
	protected NetStat localNetStat;
	
	/** Metric accumulator for the local side */
	protected final NonBlockingHashMap<String, ConcurrentLongSlidingWindow> localStats = newNetStatMap();
	/** Metric accumulator for the remote side */
	protected final NonBlockingHashMap<String, ConcurrentLongSlidingWindow> remoteStats = newNetStatMap();
	
	/** The socket state of the local side as a {@link TCPSocketState} bit mask */
	protected final AtomicInteger localSocketState = new AtomicInteger(0); 
	/** The socket state of the remote side as a {@link TCPSocketState} bit mask */
	protected final AtomicInteger remoteSocketState = new AtomicInteger(0); 
	
	/** A sigar reference to acquire netstats */
	protected static final APMSigar sigar;
	
	/** A map of server connections keyed by the compond of the local and remote address:port */
	protected static final NonBlockingHashMap<String, ServerConnection> isockets = new NonBlockingHashMap<String, ServerConnection>(); 
	
	static {
		APMSigar tmp = null;
		try {
			tmp = APMSigar.getInstance();
		} catch (Exception ex) {
			tmp = null;
		}
		sigar = tmp;
		Executors.newScheduledThreadPool(1, new ThreadFactory(){
			private final AtomicInteger serial = new AtomicInteger(0); 
			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r, "ServerConnectionHarvester#" + serial.incrementAndGet());
				return t;
			}
		}).scheduleWithFixedDelay(new Runnable(){
			@Override
			public void run() {
				if(isockets.isEmpty()) return;
				long start = System.currentTimeMillis();
				for(ServerConnection sc: isockets.values()) {
					sc.collect();
				}
				long elapsed = System.currentTimeMillis()-start;
				SimpleLogger.info("\n\t===================\n\tServerConnection Harvest in [" ,elapsed, "] ms.\n\t===================\n");
				
			}
		}, 5, 5, TimeUnit.SECONDS);
	}
	
	
	/**
	 * Acquires the ServerConnection instance for the passed ISocketImpl
	 * @param serverSocket the ISocketImpl to get the ServerConnection for.
	 */
	public static void registerInstance(ISocketImpl serverSocket) {
		if(serverSocket==null) throw new IllegalArgumentException("The passed server socket was null", new Throwable());
		String key = key(serverSocket);
		isockets.putIfAbsent(key, new ServerConnection(key, serverSocket));		
	}
	
	/**
	 * Generates a unique reference key for the passed server connection
	 * @param serverSocket the server connection to generate a key for
	 * @return the key
	 */
	public static String key(ISocketImpl serverSocket) {
		if(serverSocket==null) throw new IllegalArgumentException("The passed server socket was null", new Throwable());
		return "" + System.identityHashCode(serverSocket);
//		Socket sock = serverSocket.getSocket();
//		InetSocketAddress remoteSockAddr = (InetSocketAddress)sock.getRemoteSocketAddress(); 
//		return new StringBuilder(sock.getLocalAddress().getHostAddress())
//			.append("/").append(sock.getLocalPort())
//			.append("/").append(
//					remoteSockAddr
//					.getHostString()).append("/")
//					.append(remoteSockAddr.getPort())
//			.toString();
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
		} else {
			this.serverSocket = serverSocket;
			this.key = key;
		}
	}
	
	/**
	 * Creates a new stat map
	 * @return a new stat map
	 */
	public static NonBlockingHashMap<String, ConcurrentLongSlidingWindow> newNetStatMap() {
		NonBlockingHashMap<String, ConcurrentLongSlidingWindow> stats = new NonBlockingHashMap<String, ConcurrentLongSlidingWindow>(STAT_ALL.length);
		for(String name: STAT_ALL) {
			stats.put(name, new ConcurrentLongSlidingWindow(20));
		}	
		return stats;
	}
	
	/**
	 * Updates the local socket state, firing state change listeners if the state has changed
	 * @param newSocketState The new socket state
	 */
	protected void setLocalSocketState(int newSocketState) {
		int priorSocketState = localSocketState.getAndSet(newSocketState);
		if(priorSocketState!=newSocketState) {
			SimpleLogger.info("Server LocalSide Socket [", serverSocket.getSocket().getLocalSocketAddress() , "] Changed State from [", TCPSocketState.getEnabledStatesName(priorSocketState) , "] to [", TCPSocketState.getEnabledStatesName(newSocketState), "]" );
		}
	}
	
	/**
	 * Updates the remote socket state, firing state change listeners if the state has changed
	 * @param newSocketState The new socket state
	 */
	protected void setRemoteSocketState(int newSocketState) {
		int priorSocketState = remoteSocketState.getAndSet(newSocketState);
		if(priorSocketState!=newSocketState) {
			SimpleLogger.info("Server RemoteSide Socket [", serverSocket.getSocket().getLocalSocketAddress() , "] Changed State from [", TCPSocketState.getEnabledStatesName(priorSocketState) , "] to [", TCPSocketState.getEnabledStatesName(newSocketState), "]" );
		}
	}
	
	
	/**
	 * Collects stats and states for the local and remote side of this connection
	 */
	protected void collect() {		
		refreshNetStat(false);
		if(localNetStat!=null) {
			int socketState = TCPSocketState.getMaskedArray(localNetStat.getTcpStates());
			setLocalSocketState(socketState);
			SimpleLogger.info("Local TCP Socket State: [" , TCPSocketState.getEnabledStatesName(socketState), "]");			

//			localStats.get(STAT_TCPINBOUNDTOTAL).insert(localNetStat.getTcpInboundTotal());
//			localStats.get(STAT_TCPOUTBOUNDTOTAL).insert(localNetStat.getTcpOutboundTotal());
//			localStats.get(STAT_ALLINBOUNDTOTAL).insert(localNetStat.getAllInboundTotal());
//			localStats.get(STAT_ALLOUTBOUNDTOTAL).insert(localNetStat.getAllOutboundTotal());
//			localStats.get(STAT_TCPESTABLISHED).insert(localNetStat.getTcpEstablished());
//			localStats.get(STAT_TCPSYNSENT).insert(localNetStat.getTcpSynSent());
//			localStats.get(STAT_TCPSYNRECV).insert(localNetStat.getTcpSynRecv());
//			localStats.get(STAT_TCPFINWAIT1).insert(localNetStat.getTcpFinWait1());
//			localStats.get(STAT_TCPFINWAIT2).insert(localNetStat.getTcpFinWait2());
//			localStats.get(STAT_TCPTIMEWAIT).insert(localNetStat.getTcpTimeWait());
//			localStats.get(STAT_TCPCLOSE).insert(localNetStat.getTcpClose());
//			localStats.get(STAT_TCPCLOSEWAIT).insert(localNetStat.getTcpCloseWait());
//			localStats.get(STAT_TCPLASTACK).insert(localNetStat.getTcpLastAck());
//			localStats.get(STAT_TCPLISTEN).insert(localNetStat.getTcpListen());
//			localStats.get(STAT_TCPCLOSING).insert(localNetStat.getTcpClosing());
//			localStats.get(STAT_TCPIDLE).insert(localNetStat.getTcpIdle());
//			localStats.get(STAT_TCPBOUND).insert(localNetStat.getTcpBound());
//			SimpleLogger.info("Local ServerSide [", localSocketState.get(), "] [" ,serverSocket.getSocket().getLocalSocketAddress() , "]" , renderStats(localStats));
		}
		refreshNetStat(true);
		if(remoteNetStat!=null) {
			int socketState = TCPSocketState.getMaskedArray(remoteNetStat.getTcpStates());
			setRemoteSocketState(socketState);
			SimpleLogger.info("Remote TCP Socket State: [" , TCPSocketState.getEnabledStatesName(socketState), "]");			
			
//			remoteStats.get(STAT_TCPINBOUNDTOTAL).insert(remoteNetStat.getTcpInboundTotal());
//			remoteStats.get(STAT_TCPOUTBOUNDTOTAL).insert(remoteNetStat.getTcpOutboundTotal());
//			remoteStats.get(STAT_ALLINBOUNDTOTAL).insert(remoteNetStat.getAllInboundTotal());
//			remoteStats.get(STAT_ALLOUTBOUNDTOTAL).insert(remoteNetStat.getAllOutboundTotal());
//			remoteStats.get(STAT_TCPESTABLISHED).insert(remoteNetStat.getTcpEstablished());
//			remoteStats.get(STAT_TCPSYNSENT).insert(remoteNetStat.getTcpSynSent());
//			remoteStats.get(STAT_TCPSYNRECV).insert(remoteNetStat.getTcpSynRecv());
//			remoteStats.get(STAT_TCPFINWAIT1).insert(remoteNetStat.getTcpFinWait1());
//			remoteStats.get(STAT_TCPFINWAIT2).insert(remoteNetStat.getTcpFinWait2());
//			remoteStats.get(STAT_TCPTIMEWAIT).insert(remoteNetStat.getTcpTimeWait());
//			remoteStats.get(STAT_TCPCLOSE).insert(remoteNetStat.getTcpClose());
//			remoteStats.get(STAT_TCPCLOSEWAIT).insert(remoteNetStat.getTcpCloseWait());
//			remoteStats.get(STAT_TCPLASTACK).insert(remoteNetStat.getTcpLastAck());
//			remoteStats.get(STAT_TCPLISTEN).insert(remoteNetStat.getTcpListen());
//			remoteStats.get(STAT_TCPCLOSING).insert(remoteNetStat.getTcpClosing());
//			remoteStats.get(STAT_TCPIDLE).insert(remoteNetStat.getTcpIdle());
//			remoteStats.get(STAT_TCPBOUND).insert(remoteNetStat.getTcpBound());
//			SimpleLogger.info("Remote ServerSide [", remoteSocketState, "] [" , serverSocket.getSocket().getRemoteSocketAddress() , "]" , renderStats(remoteStats));
		}
	}
	
/*
  
 ServerSocket[addr=/0.0.0.0,port=0,localport=9384 
 Accepted Socket: [1212866551] [Socket[addr=/0:0:0:0:0:0:0:1,port=35812,localport=9384]]  Available:0
INFO [ServerThread#1]OutputStream Accessed [1212866551]
	Local Address:/0:0:0:0:0:0:0:1:9384
	Remote Address:/0:0:0:0:0:0:0:1:35812
 
tcp6       0      0 :::9384                 :::*                    LISTEN      18277/java       off (0.00/0/0)
tcp6       0      0 ::1:35461               ::1:9384                ESTABLISHED 18295/nc         off (0.00/0/0)
tcp6       0      0 ::1:9384                ::1:35461               ESTABLISHED 18277/java       off (0.00/0/0)
=====
tcp6       0      0 :::9384                 :::*                    LISTEN      18277/java       off (0.00/0/0)
tcp6       0      0 ::1:35461               ::1:9384                FIN_WAIT2   -                timewait (56.35/0/0)
tcp6       1      0 ::1:9384                ::1:35461               CLOSE_WAIT  18277/java       off (0.00/0/0)

-Djava.net.preferIPv4Stack=true

addr=/0.0.0.0,port=0,localport=9384
INFO [ServerThread#1]OutputStream Accessed [1807104969]
	Local Address:/127.0.0.1:9384
	Remote Address:/127.0.0.1:57136

tcp        0      0 0.0.0.0:9384            0.0.0.0:*               LISTEN      18367/java       off (0.00/0/0)
tcp        0      0 127.0.0.1:9384          127.0.0.1:56931         ESTABLISHED 18367/java       off (0.00/0/0)
tcp        0      0 127.0.0.1:56931         127.0.0.1:9384          ESTABLISHED 18385/nc         off (0.00/0/0)
====
tcp        0      0 0.0.0.0:9384            0.0.0.0:*               LISTEN      18367/java       off (0.00/0/0)
tcp        1      0 127.0.0.1:9384          127.0.0.1:56931         CLOSE_WAIT  18367/java       off (0.00/0/0)
tcp        0      0 127.0.0.1:56931         127.0.0.1:9384          FIN_WAIT2   -                timewait (56.93/0/0)
 	
 	
 */
	
	/**
	 * Formats a stats map into printable string
	 * @param stats The map to render
	 * @return a string
	 */
	protected String renderStats(Map<String, ConcurrentLongSlidingWindow> stats) {
		StringBuilder b = new StringBuilder();
		for(Map.Entry<String, ConcurrentLongSlidingWindow> entry: stats.entrySet()) {
			ConcurrentLongSlidingWindow lsw = entry.getValue();
			b.append("\n\t").append(entry.getKey()).append(": ").append(lsw.isEmpty() ? 0 : lsw.get(0));			
		}
		return b.toString();
	}
	
	/**
	 * Refreshes the netstats, creating them if they're null
	 * @param remoteSide true for the remote side, false for the local side
	 * @return true if the netstat was updated successfully, false otherwise
	 */
	protected boolean refreshNetStat(boolean remoteSide) {		
		if(remoteSide) {
			if(remoteNetStat==null) remoteNetStat = getNetStatOrNull(remoteSide);
			if(remoteNetStat!=null) 
			try {
				remoteNetStat.stat(sigar.getSigar(), serverSocket.getSocket().getInetAddress().getAddress(), serverSocket.getSocket().getPort()); 
				return true;
			} catch (Exception ex) {
				return false;
			}
		} else if(!remoteSide) {
			if(localNetStat==null) localNetStat = getNetStatOrNull(remoteSide);
			if(localNetStat!=null) 			
			try {				 
				localNetStat.stat(sigar.getSigar(), serverSocket.getSocket().getLocalAddress().getAddress(), serverSocket.getSocket().getLocalPort());
				return true;
			} catch (Exception ex) {
				return false;
			}
		}
		return false;
	}
	
	protected void dumpSocketSides() {
		StringBuilder b = new StringBuilder("Socket Sides Dump:");
		b.append("\n\tLocal").append("\n\t\tAddress:").append(serverSocket.getSocket().getLocalAddress()).append("\n\t\tPort:").append(serverSocket.getSocket().getLocalPort()).append("\n\t\tNetStat:").append(localNetStat);
		b.append("\n\tRemote").append("\n\t\tAddress:").append(serverSocket.getSocket().getInetAddress()).append("\n\t\tPort:").append(serverSocket.getSocket().getPort()).append("\n\t\tNetStat:").append(remoteNetStat);		
		InetSocketAddress isa = (InetSocketAddress)serverSocket.getSocket().getRemoteSocketAddress(); 
		b.append("\n\tRSA").append("\n\t\tAddress:").append(isa.getAddress()).append("\n\t\tPort:").append(isa.getPort());
		SimpleLogger.info(b);
	}
	
	/**
	 * Returns a netstat for the remote of the passed socket impl
	 * @param remoteSide true for the remote side, false for the local side

	 * @return a netstat or null if one could not be acquired 
	 */
	protected NetStat getNetStatOrNull(boolean remoteSide) {
		try {
			if(remoteSide) {
				if(remoteNetStat!=null) return remoteNetStat;
				return sigar.getNetStat(serverSocket.getSocket().getInetAddress().getAddress(),  serverSocket.getSocket().getPort());
			}
			if(localNetStat!=null) return localNetStat;
			return sigar.getNetStat(serverSocket.getSocket().getLocalAddress().getAddress(), serverSocket.getSocket().getLocalPort());				
		} catch (Exception ex) {
			SimpleLogger.error("NetStat fail on [", (remoteSide ? "Remote" : "Local"), ":", ex);
			return null;
		}
	}
	
	/**
	 * Tests a socket's input and output streams to see if the socket is active.
	 * @param so The socket to test
	 * @return true if the socket is active, false otherwise
	 */
	protected boolean testSocketStreams(Socket so) {
		if(so==null) return false;
		try {			
			if(!so.isConnected() || so.isClosed()) return false;
			boolean ok = testSocketInput(so) && testSocketOutput(so);
			if(!ok) return false;
			//so.sendUrgentData(0);
			
			NetStat ns = sigar.getNetStat(so.getLocalAddress().getAddress(), so.getLocalPort());
			SimpleLogger.info("Local CloseWaits:", ns.getTcpCloseWait());
			try {
				int cw  = sigar.getNetStat(so.getInetAddress().getAddress(),  so.getPort()).getTcpCloseWait();
				SimpleLogger.info("Remote CloseWaits on [:" + so.getRemoteSocketAddress() + "]", cw);
				if(cw>0) return false;
			} catch (Exception ex) { SimpleLogger.warn("Failed to get Remote CloseWaits"); }
//			if(ns.getTcpCloseWait()>0) {
//				return false;
//			}
			return ok;
		}  catch (Exception ex) {
			if(so.isConnected()) {
				try { so.close(); } catch (Exception e) {}
			}
			return false;
		}
	}
	
	/**
	 * Tests the socket's input stream to determine if input is closed 
	 * @param so the socket to test
	 * @return true if the input is still active, false otherwise
	 */
	protected boolean testSocketInput(Socket so) {
		if(so==null) return false;
		try {			
			if(so.isInputShutdown()) return false;
			so.getInputStream().available();			
			return true;
		}  catch (Exception ex) {
			return false;
		}
	}
	
	/** An empty byte array buffer constant */
	public static final byte[] EMPTY_BYTE_ARR = {};
	/** An one byte array buffer constant */
	public static final byte[] ONE_BYTE_ARR = {0};
	
	/**
	 * Tests the socket's output stream to determine if output is closed 
	 * @param so the socket to test
	 * @return true if the output is still active, false otherwise
	 */
	protected boolean testSocketOutput(Socket so) {
		if(so==null) return false;
		try {
			if(so.isOutputShutdown()) return false;
			OutputStream os = so.getOutputStream();			
			os.write(ONE_BYTE_ARR, 0, 0);			
			return true;
		}  catch (Exception ex) {
			SimpleLogger.info("Got exception writing zero bytes of ONE_BYTE_ARR");
			return false;
		}
	}
	

}
