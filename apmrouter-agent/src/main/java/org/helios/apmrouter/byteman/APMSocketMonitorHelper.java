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

import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.helios.apmrouter.instrumentation.publifier.ClassPublifier;
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
	
	/**
	 * Called when the first instance of this helper class is instantiated for an active rule
	 */
	public static void activated() {		
		APMAgentHelper.activated();
		Class<?>[] publified = null;
		try {
			publified = ClassPublifier.getInstance().publify(true, Class.forName("java.net.SocketOutputStream"), Class.forName("java.net.SocketInputStream"));
			SimpleLogger.info("Publified SocketMonitor Classes:" + publified.length);
			StringBuilder b = new StringBuilder("\nPublified SocketMonitor Classes:");
			for(Class<?> clazz: publifiedClasses) {
				b.append("\n\t[").append(clazz.getName()).append("]  Public:").append(Modifier.isPublic(clazz.getModifiers()));
			}
			b.append("\n");
			SimpleLogger.info(b);
			Collections.addAll(publifiedClasses, publified);
		} catch (Exception ex) {
			SimpleLogger.error("Failed to publify socket streams", ex);
			throw new RuntimeException("Failed to publify socket streams", ex);
		}
	}
	
	/**
	 * Called when the last rule using this helper class is uninstalled
	 */
	public static void deactivated() {
		APMAgentHelper.deactivated();
		Class<?>[] reverted = null;
		try {
			reverted = ClassPublifier.getInstance().revert(true, Class.forName("java.net.SocketOutputStream"), Class.forName("java.net.SocketInputStream"));
			SimpleLogger.info("Reverted SocketMonitor Classes:" + reverted.length);
			for(Class<?> clazz: reverted) {
				publifiedClasses.remove(clazz);
			}
			if(!publifiedClasses.isEmpty()) {
				StringBuilder b = new StringBuilder("\nUnexpected SocketMonitor Publified Classes after Helper Deactivation:");
				for(Class<?> clazz: publifiedClasses) {
					b.append("\n\t[").append(clazz.getName()).append("]-->").append(clazz.getClassLoader());
				}
				b.append("\n");
				SimpleLogger.warn(b);
			}
		} catch (Exception ex) {
			SimpleLogger.error("Failed to revert publified socket streams", ex);
			throw new RuntimeException("Failed to revert publify socket streams", ex);			
		}
	}
	
	
	
	/**
	 * Creates a new APMSocketMonitorHelper
	 * @param rule The rule that triggers the helper load
	 */
	public APMSocketMonitorHelper(Rule rule) {
		super(rule);
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
		InetSocketAddress localSocket = (InetSocketAddress)as.getLocalSocketAddress();
		InetSocketAddress remoteSocket = (InetSocketAddress)as.getRemoteSocketAddress();
		SimpleLogger.info("\n\tServerSocket Accept \n\t  Socket Local:", localSocket.getAddress().getHostAddress(), ":", localSocket.getPort(), "\n\t  Socket Remote:", remoteSocket.getAddress().getHostAddress(), ":", remoteSocket.getPort());
		traceCounter(1, "ServerSocketAccept", "java", "net", "server", localSocket.getAddress().getHostAddress(), "" + localSocket.getPort());
		traceCounter(1, "ServerSocketAccept", "java", "net", "server", localSocket.getAddress().getHostAddress(), "" + localSocket.getPort(), "remote", remoteSocket.getAddress().getHostAddress(), "" + remoteSocket.getPort());
	}
	
	/**
	 * Traces a server socket accept of a remote connection
	 * @param ss The server socket that accepted
	 * @param as The accepted socket created
	 */
	public void traceServerSockAccept(Object ss, Object as) {
		SimpleLogger.info("\n\tServerSocket Accept [", ss.getClass().getName(), "]:[", as.getClass().getName(), "]");
	}
	
	/**
	 * Traces the number of bytes written to a socket output stream
	 * @param as The socket whose output stream was written to
	 * @param length The number of bytes that were written to the output stream
	 */
	public void traceSocketWriteBytes(Socket as, int length) {
		InetSocketAddress localSocket = (InetSocketAddress)as.getLocalSocketAddress();
		InetSocketAddress remoteSocket = (InetSocketAddress)as.getRemoteSocketAddress();		
		SimpleLogger.info("\n\tSocket Write [", length, "] bytes\n\t  Socket Local:", localSocket.getAddress().getHostAddress(), ":", localSocket.getPort(), "\n\t  Socket Remote:", remoteSocket.getAddress().getHostAddress(), ":", remoteSocket.getPort());
	}
	/**
	 * Traces the number of bytes written to a socket output stream
	 * @param stream The socketoutputstream being written to
	 * @param length The number of bytes written
	 */
	public void traceSocketWriteByteArrayOffset(Object stream, int length) {
		Socket as = (Socket)getFieldValue(stream, "socket");
		InetSocketAddress localSocket = (InetSocketAddress)as.getLocalSocketAddress();
		InetSocketAddress remoteSocket = (InetSocketAddress)as.getRemoteSocketAddress();		
		SimpleLogger.info("\n\tSocket Write [", length, "] bytes\n\t  Socket Local:", localSocket.getAddress().getHostAddress(), ":", localSocket.getPort(), "\n\t  Socket Remote:", remoteSocket.getAddress().getHostAddress(), ":", remoteSocket.getPort());		
	}
	/**
	 * Traces the number (in this case, one) of bytes written to a socket output stream
	 * @param as The socket whose output stream was written to
	 */
	public void traceSocketWriteByte(Socket as) {
		InetSocketAddress localSocket = (InetSocketAddress)as.getLocalSocketAddress();
		InetSocketAddress remoteSocket = (InetSocketAddress)as.getRemoteSocketAddress();		
		SimpleLogger.info("\n\tSocket Write [", 1, "] bytes\n\t  Socket Local:", localSocket.getAddress().getHostAddress(), ":", localSocket.getPort(), "\n\t  Socket Remote:", remoteSocket.getAddress().getHostAddress(), ":", remoteSocket.getPort());				
	}
	

}
