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
package test.org.helios.apmrouter.sockets;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

import org.helios.apmrouter.util.SystemClock;

/**
 * <p>Title: RunSocketClient</p>
 * <p>Description: Contrived socket client to test instrumented server</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.org.helios.apmrouter.sockets.RunSocketClient</code></p>
 */

public class RunSocketClient {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log("RunSocketClient");
		try {
			Socket client = new Socket();
			client.setTcpNoDelay(true);
			client.setSoLinger(false, -1);
			log("Client Default Timeout:" + client.getSoTimeout());
			client.setSoTimeout(3000);
			client.connect(new InetSocketAddress("localhost", 9384), 3000);
			log("Client Connected:" + client.isConnected());
			printOutput(client);
			SystemClock.sleep(10000);
			log("Closing Client.....");
			client.close();
			log("Client Closed");
			SystemClock.sleep(300000);						
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}

	}
	
	public static void log(Object msg) {
		System.out.println(msg);
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
	


}
