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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketImpl;
import java.net.SocketImplFactory;


import org.helios.apmrouter.byteman.sockets.impl.SocketImplTransformer;


import org.helios.apmrouter.jmx.JMXHelper;

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
			Instrumentation instrumentation = (Instrumentation)JMXHelper.getHeliosMBeanServer().getAttribute(org.helios.apmrouter.jagent.Instrumentation.OBJECT_NAME, "Instance");
//			instrumentation.addTransformer(new SocketImplTransformer());
			//log("Transformer Installed. TrackingSocketFactory Installed:" + TrackingSocketImplFactory.isInstalled());
			Socket sock = new Socket();
			sock.setSoTimeout(5000);
			sock.setTcpNoDelay(true);
			Field f = Socket.class.getDeclaredField("impl");
			Field f2 = Socket.class.getDeclaredField("factory");
			f.setAccessible(true);
			f2.setAccessible(true);			
			SocketImpl si = (SocketImpl)f.get(sock);
			SocketImplFactory sif = (SocketImplFactory)f2.get(null);
			log("SocketImpl Class:" + si.getClass().getName() + "  Connected:" + sock.isConnected());
			log("SocketImpl Factory:" + (sif==null ? "<null>" : sif.getClass().getName()));			
			sock.connect(new InetSocketAddress("localhost", 80), 5);
			InputStream is = sock.getInputStream();
			OutputStream os = sock.getOutputStream();
			os.write(("GET / HTTP\1.0\nUser-Agent: browser\n").getBytes());
			os.flush();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line = null;
			while((line=br.readLine()) !=null) {
				log(line);
			}
			
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
