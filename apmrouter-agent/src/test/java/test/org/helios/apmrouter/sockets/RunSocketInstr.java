/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2013, Helios Development Group and individual contributors
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
package test.org.helios.apmrouter.sockets;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.helios.apmrouter.byteman.sockets.impl.LoggingSocketTracker;
import org.helios.apmrouter.byteman.sockets.impl.SocketTrackingAdapter;
import org.helios.apmrouter.util.SystemClock;

/**
 * <p>Title: RunSocketInstr</p>
 * <p>Description: Simple test for installing the transformer manager</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.org.helios.apmrouter.sockets.RunSocketInstr</code></p>
 */

public class RunSocketInstr {
	/** Sample socket impls */
	public static final String[] SOCKET_IMPLS_X = {
		"java.net.DualStackPlainSocketImpl",
		"java.net.PlainSocketImpl",
		"java.net.SdpSocketImpl",
		"java.net.SocksSocketImpl",
		"java.net.TwoStacksPlainSocketImpl"		
	};
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log("SocketInstr Test");
		try {
//			Instrumentation instrumentation = (Instrumentation)JMXHelper.getHeliosMBeanServer().getAttribute(org.helios.apmrouter.jagent.Instrumentation.OBJECT_NAME, "Instance");
////			instrumentation.addTransformer(new SocketImplTransformer());
//			//log("Transformer Installed. TrackingSocketFactory Installed:" + TrackingSocketImplFactory.isInstalled());
//			Socket sock = new Socket();
//			sock.setSoTimeout(5000);
//			sock.setTcpNoDelay(true);
//			Field f = Socket.class.getDeclaredField("impl");
//			Field f2 = Socket.class.getDeclaredField("factory");
//			f.setAccessible(true);
//			f2.setAccessible(true);			
//			SocketImpl si = (SocketImpl)f.get(sock);
//			SocketImplFactory sif = (SocketImplFactory)f2.get(null);
//			log("SocketImpl Class:" + si.getClass().getName() + "  Connected:" + sock.isConnected());
//			log("SocketImpl Factory:" + (sif==null ? "<null>" : sif.getClass().getName()));			
//			sock.connect(new InetSocketAddress("localhost", 80), 5000);
//			InputStream is = sock.getInputStream();
//			OutputStream os = sock.getOutputStream();
//			String delim = new String(new char[]{(char)10, (char)13});
//			os.write(("OPTIONS * HTTP/1.1").getBytes());
//			os.flush();
//			InputStreamReader isr = new InputStreamReader(is);
//			BufferedReader br = new BufferedReader(isr);
//			String line = null;
//			while((line=br.readLine()) !=null) {
//				log(line);
//			}
			
			SocketTrackingAdapter.setISocketTracker(new LoggingSocketTracker());
			
//			URL url = new URL("http://localhost:80");
//			SocketTrackingAdapter.setISocketTracker(new LoggingSocketTracker());
//			URL url = new URL("http://www.oracle.com");
//			InputStream is = url.openStream();
//			InputStreamReader isr = new InputStreamReader(is);
//			BufferedReader br = new BufferedReader(isr);
//			String line = null;
//			while((line=br.readLine()) !=null) {
//				//log(line);
//			}
//			is.close();
			Field f = ServerSocket.class.getDeclaredField("impl");
			f.setAccessible(true);
			final ServerSocket ss = new ServerSocket(9384, 213, InetAddress.getByName("0.0.0.0"));
			Thread t = new Thread() {
				public void run() {
					try {
						log("Server Started");
						Socket sock = ss.accept();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					log("Accepted");
					
				}
			};
			t.setDaemon(true);
			t.start();
			//ss.bind(new InetSocketAddress("0.0.0.0", 9384), 200);
			
			Socket client = new Socket();
			client.setTcpNoDelay(true);
			client.connect(new InetSocketAddress("localhost", 9384), 3000);
			log("Client Connected:" + client.isConnected());
			client.close();
			log("Client Closed");
			SystemClock.sleep(2000);			
			ss.close();
			log("Server Closed");
			
			
			
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		} finally {
			/** Noop */
		}

	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}

}
