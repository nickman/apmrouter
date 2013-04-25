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

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.helios.apmrouter.server.ServerComponentBean;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.util.CharsetUtil;

/**
 * <p>Title: AbstractHttpRequestHandler</p>
 * <p>Description: Abstract base class for {@link HttpRequestHandler} impls.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.unification.pipeline.http.AbstractHttpRequestHandler</code></p>
 */

public abstract class AbstractHttpRequestHandler extends ServerComponentBean implements HttpRequestHandler {
	/** The set of URI patterns that this handler handles. */
	protected final Set<String> uriPatterns = new HashSet<String>();
	
	/**
	 * Creates a new AbstractHttpRequestHandler and looks for a {@link URIHandler} annotation to add URI patterns for.
	 */
	protected AbstractHttpRequestHandler() {
		URIHandler handler = getClass().getAnnotation(URIHandler.class);
		if(handler!=null) {
			Collections.addAll(uriPatterns, handler.uri());
		}
	}
	
    /**
     * Sends an error back to the HTTP caller
     * @param ctx The channel handler context on which the request was receives
     * @param status the repsonse status
     * @param messages An optional array of messages added to the error
     */
    protected void sendError(ChannelHandlerContext ctx, HttpResponseStatus status, String...messages) {
        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, status);
        response.setHeader(CONTENT_TYPE, "text/plain; charset=UTF-8");
        StringBuilder b = new StringBuilder();
        if(messages!=null) {
        	for(String m: messages) {
        		b.append(m).append("\r\n");
        	}
        }
        response.setContent(ChannelBuffers.copiedBuffer(
                "Failure: " + status.toString() + "\r\n" + b.toString(),
                CharsetUtil.UTF_8));

        // Close the connection as soon as the error message is sent.
        ctx.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
    }


	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.unification.pipeline.http.HttpRequestHandler#getUriPatterns()
	 */
	@Override
	public Set<String> getUriPatterns() {		
		return Collections.unmodifiableSet(uriPatterns);
	}
	
	/**
	 * Adds the passed patterns to the URI patterns for this handler
	 * @param patterns a set of URI patterns
	 */
	public void setUriPatterns(Set<String> patterns) {
		if(patterns!=null) {
			for(String s: patterns) {
				uriPatterns.add(s.trim());
			}
		}
	}
	
	
}
