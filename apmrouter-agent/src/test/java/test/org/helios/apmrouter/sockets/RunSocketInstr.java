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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketImpl;
import java.util.concurrent.CountDownLatch;

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
			
			SocketTrackingAdapter.getInvoker().setInstalledTrackerName("org.helios.apmrouter.byteman.sockets.impl.LoggingSocketTracker");
			
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
			final ServerSocket ss = new ServerSocket(9384, 213, InetAddress.getByName("0.0.0.0"));
			int cnt = 0;
			log("Server Started");
			for(;;) {
				final Socket sock = ss.accept();
				cnt++;
				Thread t = new Thread("ServerThread#" + cnt) {
					public void run() {
						//while(true) {
							try {							
								//sock.setKeepAlive(true);
								sock.setSoLinger(true, 30);
								sock.setSoTimeout(3000);
								sock.setTcpNoDelay(false);
								sock.setOOBInline(false);
								log("Accepted Socket: [" + System.identityHashCode(getSocketImpl(sock)) + "] [" + sock + "]  Available:" + sock.getInputStream().available() );
								
								OutputStreamWriter out = new OutputStreamWriter(sock.getOutputStream(  ));
							    out.write("You've connected to this server. Bye-bye now.\r\n");       
							    out.flush();
							    log(" OUTPUT WRITTEN");					    
							    SystemClock.sleep(5000);
							    //sock.close(  );						
							} catch (IOException e) {
								//e.printStackTrace();
							}
						//}						
					}
				};
				t.setDaemon(false);
				t.start();				
			}
			//ss.bind(new InetSocketAddress("0.0.0.0", 9384), 200);
			
//			Socket client = new Socket();
//			client.setTcpNoDelay(true);
//			client.setSoLinger(false, -1);
//			log("Client Default Timeout:" + client.getSoTimeout());
//			client.setSoTimeout(3000);
//			client.connect(new InetSocketAddress("localhost", 9384), 3000);
//			log("Client Connected:" + client.isConnected() + " [" + System.identityHashCode(getSocketImpl(client)) + "]");
//			printOutput(client);
//			SystemClock.sleep(10000);
//			log("Closing Client.....");
//			client.close();
//			log("Client Closed");
//			SystemClock.sleep(300000);			
//			ss.close();
//			log("Server Closed");
			
			
			
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		} finally {
			/** Noop */
		}
	}
	
	public static boolean testSockUD(Socket so) {
		try {
			so.sendUrgentData(-1);
			return true;
		}  catch (Exception ex) {
			if(so.isConnected()) {
				try { so.close(); } catch (Exception e) {}
			}
			return false;
		}
	}
	
	public static boolean testSockStreams(Socket so) {
		try {			
			so.getInputStream().available();
			so.getOutputStream();
			return true;
		}  catch (Exception ex) {
			if(so.isConnected()) {
				try { so.close(); } catch (Exception e) {}
			}
			return false;
		}
	}
	
	
	public static void printOutput(final Socket socket) {
		final CountDownLatch latch = new CountDownLatch(1); 
		Thread t = new Thread() {
			public void run() {
				InputStream is = null;
				InputStreamReader isReader = null;
				BufferedReader bReader = null;
				try {
					is = socket.getInputStream();
					isReader = new InputStreamReader(is);
					bReader = new BufferedReader(isReader);
					String line = null;
					while((line = bReader.readLine())!=null) {
						log("OUTPUT:" + line);
						latch.countDown();
						break;
					}
				} catch (Exception ex) {
					ex.printStackTrace(System.err);
				} finally {
					try { is.close(); } catch (Exception e) {}
				}				
			}
		};
		t.setDaemon(true);
		t.start();
		try { latch.await(); } catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
	
	public static SocketImpl getSocketImpl(Socket socket) {
		if(socket==null) return null;
		try {
			Field f = Socket.class.getDeclaredField("impl");
			f.setAccessible(true);
			return (SocketImpl)f.get(socket);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}

}
