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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;
import java.net.SocketImplFactory;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>Title: TrackingSocketImpl</p>
 * <p>Description: A {@link SocketImpl} implementation and factory intended to wrap the provided implementation to track client and server socket activity.
 * The class can be installed early in the JVM startup or woven in using a class transformer. 
 * See {@link Socket#setSocketImplFactory(SocketImplFactory)}.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.byteman.sockets.impl.TrackingSocketImpl</code></p>
 */

public class TrackingSocketImpl extends SocketImpl implements SocketImplFactory {
	/** Indicates if the socket impl factory has been installed */
	protected static final AtomicBoolean installed = new AtomicBoolean(false);
	/** The delegate socket impl factory */
	protected static SocketImplFactory delegate = null;
	/** The delegate socket impl */
	protected final SocketImpl innerSocket;
	
	/**
	 * Indicates if the socket impl factory has been installed
	 * @return true if the socket impl factory has been installed, false otherwise
	 */
	public static boolean isInstalled() {
		return installed.get();
	}
	/**
	 * Creates a new TrackingSocketImpl
	 * @param innerSocket The delegate socket impl
	 */
	public TrackingSocketImpl(SocketImpl innerSocket) {
		this.innerSocket = innerSocket;
	}

	/**
	 * {@inheritDoc}
	 * @see java.net.SocketOptions#setOption(int, java.lang.Object)
	 */
	@Override
	public void setOption(int optID, Object value) throws SocketException {
		innerSocket.setOption(optID, value);
	}

	/**
	 * {@inheritDoc}
	 * @see java.net.SocketOptions#getOption(int)
	 */
	@Override
	public Object getOption(int optID) throws SocketException {
		return innerSocket.getOption(optID);
	}

	/**
	 * {@inheritDoc}
	 * @see java.net.SocketImplFactory#createSocketImpl()
	 */
	@Override
	public SocketImpl createSocketImpl() {
		return new TrackingSocketImpl(delegate.createSocketImpl());
	}

	/**
	 * {@inheritDoc}
	 * @see java.net.SocketImpl#create(boolean)
	 */
	@Override
	protected void create(boolean stream) throws IOException {
		innerSocket.create(stream);

	}

	/**
	 * {@inheritDoc}
	 * @see java.net.SocketImpl#connect(java.lang.String, int)
	 */
	@Override
	protected void connect(String host, int port) throws IOException {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see java.net.SocketImpl#connect(java.net.InetAddress, int)
	 */
	@Override
	protected void connect(InetAddress address, int port) throws IOException {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see java.net.SocketImpl#connect(java.net.SocketAddress, int)
	 */
	@Override
	protected void connect(SocketAddress address, int timeout)
			throws IOException {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see java.net.SocketImpl#bind(java.net.InetAddress, int)
	 */
	@Override
	protected void bind(InetAddress host, int port) throws IOException {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see java.net.SocketImpl#listen(int)
	 */
	@Override
	protected void listen(int backlog) throws IOException {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see java.net.SocketImpl#accept(java.net.SocketImpl)
	 */
	@Override
	protected void accept(SocketImpl s) throws IOException {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see java.net.SocketImpl#getInputStream()
	 */
	@Override
	protected InputStream getInputStream() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see java.net.SocketImpl#getOutputStream()
	 */
	@Override
	protected OutputStream getOutputStream() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see java.net.SocketImpl#available()
	 */
	@Override
	protected int available() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * @see java.net.SocketImpl#close()
	 */
	@Override
	protected void close() throws IOException {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see java.net.SocketImpl#sendUrgentData(int)
	 */
	@Override
	protected void sendUrgentData(int data) throws IOException {
		// TODO Auto-generated method stub

	}

}
