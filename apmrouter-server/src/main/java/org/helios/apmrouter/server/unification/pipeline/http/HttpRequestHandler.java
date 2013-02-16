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
package org.helios.apmrouter.server.unification.pipeline.http;

import java.util.Set;

import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpRequest;

/**
 * <p>Title: HttpRequestHandler</p>
 * <p>Description: Defines an HTTP request handler that can be registered with the {@link HttpRequestRouter}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.unification.pipeline.http.HttpRequestHandler</code></p>
 */

public interface HttpRequestHandler  {
	
	/**
	 * Returns the bean name of this handler
	 * @return the bean name of this handler
	 */
	public String getBeanName();
	
	/**
	 * Returns the URI patterns supported by this handler
	 * @return the URI patterns supported by this handler
	 */
	public Set<String> getUriPatterns();
	
	/**
	 * Handles an HTTP request
	 * @param ctx The channel handler contex
	 * @param e The message event
	 * @param request The HTTP request
	 * @param path The requested URI
	 * @throws Exception thrown on any handling error
	 */
	public void handle(ChannelHandlerContext ctx, MessageEvent e, HttpRequest request, String path) throws Exception;
}
