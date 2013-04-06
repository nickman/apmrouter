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
package org.helios.apmrouter.nash.handler;

import java.util.Map;

import org.jboss.netty.channel.ChannelHandler;

/**
 * <p>Title: NashStreamHandler</p>
 * <p>Description: Defines a class that can supply zero, one or more channel handlers that will decode an incoming 
 * STDIN stream from the nailgun client and then process the decoded content. If no handlers are supplied, the handler will
 * will be called back with simple ChannelBuffers.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.nash.handler.NashStreamHandler</code></p>
 * @param <T> The type that the STDIN stream will be decoded to and called back with 
 */

public interface NashStreamHandler<T> {
	/**
	 * Returns a map of channel handlers keyed by arbitrary (but informative) names that will be used to create the netty pipeline
	 * that will decode the STDIN stream.
	 * @return a map of channel handlers
	 */
	public Map<String, ChannelHandler> getChannelHandlers();
	
	/**
	 * Callback from the created pipeline when a STDIN stream decode event is complete
	 * @param decodedEvent The decoded STDIN event
	 */
	public void onStreamDecode(T decodedEvent);
}
