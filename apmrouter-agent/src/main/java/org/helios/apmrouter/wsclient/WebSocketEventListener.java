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

import org.json.JSONObject;

/**
 * <p>Title: WebSocketEventListener</p>
 * <p>Description: A listener notified of websocket responses and events</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.wsclient.WebSocketEventListener</code></p>
 */

public interface WebSocketEventListener {
	
	/**
	 * Fired when a websocket client connects to a remote server
	 * @param remoteAddress The remote address of the server that was connected to 
	 */
	public void onConnect(SocketAddress remoteAddress);
	
	/**
	 * Fired when a websocket client connection to a remote server closes
	 * @param remoteAddress The remote address of the server that was connected to 
	 */
	public void onClose(SocketAddress remoteAddress);
	
	/**
	 * Fired when a websocket client connection to a remote server encounters an exception
	 * @param remoteAddress The remote address of the server that was connected to 
	 * @param t The exception that was thrown
	 */
	public void onError(SocketAddress remoteAddress, Throwable t);
	
	/**
	 * Fired when a websocket client connection to a remote server receives a successfully decoded message
	 * @param remoteAddress The remote address of the server that was connected to 
	 * @param message The JSON object representing the message received
	 */
	public void onMessage(SocketAddress remoteAddress, JSONObject message);
	
	
}
