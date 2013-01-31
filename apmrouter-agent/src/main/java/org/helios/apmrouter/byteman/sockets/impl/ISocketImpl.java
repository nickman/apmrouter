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
package org.helios.apmrouter.byteman.sockets.impl;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;

/**
 * <p>Title: ISocketImpl</p>
 * <p>Description: Synthetic interface to be woven onto {@link SocketImpl} concrete implementations</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.byteman.sockets.impl.ISocketImpl</code></p>
 */

public interface ISocketImpl {
	public void create(boolean stream) throws IOException;

	public void connect(String host, int port) throws IOException;

	public void connect(InetAddress address, int port) throws IOException;

	public void connect(SocketAddress address, int timeout)	throws IOException;

	public void bind(InetAddress host, int port) throws IOException;
	
	public void listen(int backlog) throws IOException;

	public void accept(SocketImpl s) throws IOException;

	public InputStream getInputStream() throws IOException;

	public OutputStream getOutputStream() throws IOException;

	public int available() throws IOException;

	public void close() throws IOException;
	
	public void sendUrgentData(int data) throws IOException;
	
	public void setOption(int optID, Object value) throws SocketException;

	public Object getOption(int optID) throws SocketException;
	
    public ServerSocket getServerSocket();
    
    public FileDescriptor getFileDescriptor();
    
    public InetAddress getInetAddress();    

    public Socket getSocket();
    
    public int getLocalPort();
    
    public int getPort();
	

}
