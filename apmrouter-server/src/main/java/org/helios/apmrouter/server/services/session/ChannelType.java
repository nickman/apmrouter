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
package org.helios.apmrouter.server.services.session;

/**
 * <p>Title: ChannelType</p>
 * <p>Description: Enumerates the different channel types that can be registered in the {@link SharedChannelGroup}.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.services.session.ChannelType</code></p>
 */
public enum ChannelType {
	/** A remotely connected UDP agent */
	UDP_AGENT("udp"),
	/** A remotely connected TCP agent */
	TCP_AGENT("tcp"),
	/** A remotely connected HTTP agent */
	HTTP_AGENT("http"),
	/** A remotely connected websocket client */
	WEBSOCKET_REMOTE("ws"),
	/** A local websocket connected to a remote */
	WEBSOCKET_LOCAL("ws"),
	/** A local UDP connection to a remote */
	UDP_CLIENT("udp"),
	/** A local TCP connection to a remote */
	TCP_CLIENT("tcp"),
	/** A netty local client */
	LOCAL_CLIENT("local"),
	/** A TCP server */
	TCP_SERVER("tcp"),
	/** A UDP server */
	UDP_SERVER("udp"),	
	/** A netty local server */
	LOCAL_SERVER("local"),
	/** An undefined channel type */	
	OTHER("other");
	
	private ChannelType(String protocol) {
		this.protocol = protocol;
	}
	
	public final String protocol;
	
}
