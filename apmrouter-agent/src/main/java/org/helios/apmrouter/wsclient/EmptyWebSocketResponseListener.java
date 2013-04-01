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
package org.helios.apmrouter.wsclient;

import java.net.SocketAddress;

import org.helios.apmrouter.util.SimpleLogger;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * <p>Title: EmptyWebSocketResponseListener</p>
 * <p>Description: An empty (logging only) implementation of {@link WebSocketEventListener}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.wsclient.EmptyWebSocketResponseListener</code></p>
 */

public class EmptyWebSocketResponseListener implements WebSocketEventListener {

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.wsclient.WebSocketEventListener#onConnect(java.net.SocketAddress)
	 */
	@Override
	public void onConnect(SocketAddress remoteAddress) {
		SimpleLogger.info("WebSocketClient connected to [", remoteAddress, "]");
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.wsclient.WebSocketEventListener#onClose(java.net.SocketAddress)
	 */
	@Override
	public void onClose(SocketAddress remoteAddress) {
		SimpleLogger.info("WebSocketClient closed connection to [", remoteAddress, "]");
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.wsclient.WebSocketEventListener#onError(java.net.SocketAddress, java.lang.Throwable)
	 */
	@Override
	public void onError(SocketAddress remoteAddress, Throwable t) {
		SimpleLogger.info("WebSocketClient encountered exception on connection to [", remoteAddress, "] ", t);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.wsclient.WebSocketEventListener#onMessage(java.net.SocketAddress, org.json.JSONObject)
	 */
	@Override
	public void onMessage(SocketAddress remoteAddress, JSONObject message) {
		try {
			SimpleLogger.info("WebSocketClient connection to [", remoteAddress, "] received message [\n", message.toString(2), "\n]");
		} catch (JSONException e) {
			onError(remoteAddress, e);
		}
	}

}
