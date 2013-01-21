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

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketAddress;

/**
 * <p>Title: EmptySocketTracker</p>
 * <p>Description: An empty {@link ISocketTracker} for extending.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.byteman.sockets.impl.EmptySocketTracker</code></p>
 */

public class EmptySocketTracker implements ISocketTracker {

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onSetEOF(java.io.InputStream, java.lang.Object, boolean)
	 */
	@Override
	public void onSetEOF(InputStream is, Object socket, boolean eof) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onSkip(java.io.InputStream, long, java.lang.Object, long)
	 */
	@Override
	public void onSkip(InputStream is, long skipped, Object socket, long skip) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onSocketWrite(java.io.OutputStream, java.lang.Object, byte[], int, int)
	 */
	@Override
	public void onSocketWrite(OutputStream os, Object socket, byte[] b,
			int off, int len) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onRead(java.io.InputStream, int, java.lang.Object, byte[])
	 */
	@Override
	public void onRead(InputStream is, int actualBytesRead, Object socket,
			byte[] buffer) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onRead(java.io.InputStream, int, java.lang.Object, byte[], int, int)
	 */
	@Override
	public void onRead(InputStream is, int actualBytesRead, Object socket,
			byte[] buffer, int off, int length) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onRead(java.io.InputStream, int, java.lang.Object)
	 */
	@Override
	public void onRead(InputStream is, int value, Object socket) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onConnect(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl, java.net.SocketAddress, int)
	 */
	@Override
	public void onConnect(ISocketImpl socketImpl, SocketAddress address,
			int timeout) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onConnect(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl, java.net.InetAddress, int)
	 */
	@Override
	public void onConnect(ISocketImpl socketImpl, InetAddress address,
			int timeout) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onConnect(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl, java.lang.String, int)
	 */
	@Override
	public void onConnect(ISocketImpl socketImpl, String host, int port) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onBind(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl, java.net.InetAddress, int)
	 */
	@Override
	public void onBind(ISocketImpl socketImpl, InetAddress host, int port) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onListen(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl, int)
	 */
	@Override
	public void onListen(ISocketImpl socketImpl, int backlog) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onAccept(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl, java.lang.Object)
	 */
	@Override
	public void onAccept(ISocketImpl socketImpl, Object acceptedSocketImpl) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onGetInputStream(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl, java.io.InputStream)
	 */
	@Override
	public void onGetInputStream(ISocketImpl socketImpl, InputStream inputStream) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onGetOutputStream(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl, java.io.OutputStream)
	 */
	@Override
	public void onGetOutputStream(ISocketImpl socketImpl,
			OutputStream outputStream) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onAvailable(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl, int)
	 */
	@Override
	public void onAvailable(ISocketImpl socketImpl, int available) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onClose(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl)
	 */
	@Override
	public void onClose(ISocketImpl socketImpl) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onShutdownInput(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl)
	 */
	@Override
	public void onShutdownInput(ISocketImpl socketImpl) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onShutdownOutput(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl)
	 */
	@Override
	public void onShutdownOutput(ISocketImpl socketImpl) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onSendUrgentData(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl, int)
	 */
	@Override
	public void onSendUrgentData(ISocketImpl socketImpl, int data) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onSetSocket(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl, java.lang.Object)
	 */
	@Override
	public void onSetSocket(ISocketImpl socketImpl, Object socket) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onSetServerSocket(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl, java.lang.Object)
	 */
	@Override
	public void onSetServerSocket(ISocketImpl socketImpl, Object serverSocket) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onReset(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl)
	 */
	@Override
	public void onReset(ISocketImpl socketImpl) {
		

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.byteman.sockets.impl.ISocketTracker#onSetPerformancePreferences(org.helios.apmrouter.byteman.sockets.impl.ISocketImpl, int, int, int)
	 */
	@Override
	public void onSetPerformancePreferences(ISocketImpl socketImpl,
			int connectionTime, int latency, int bandwidth) {
		

	}

}
