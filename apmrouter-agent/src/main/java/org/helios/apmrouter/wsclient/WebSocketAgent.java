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

import java.net.URI;

/**
 * <p>Title: WebSocketAgent</p>
 * <p>Description: Agent implementation using websockets.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.wsclient.WebSocketAgent</code></p>
 */

public class WebSocketAgent {
	/** The WebSocketClient this agent will use to comm with the server */
	protected final WebSocketClient wsClient;
	
	/**
	 * Returns a WebSocketAgent for the passed web socket URI
	 * @param wsUri the web socket URI to connect to
	 * @return a WebSocketAgent connected to the passed web socket URI
	 */
	public static WebSocketAgent newInstance(URI wsUri) {
		return new WebSocketAgent(WebSocketClient.getInstance(wsUri));
	}

	/**
	 * Creates a new WebSocketAgent
	 * @param wsClient The WebSocketClient this agent will use to comm with the server
	 */
	protected WebSocketAgent(WebSocketClient wsClient) {
		super();
		this.wsClient = wsClient;
	}
	
	
}
