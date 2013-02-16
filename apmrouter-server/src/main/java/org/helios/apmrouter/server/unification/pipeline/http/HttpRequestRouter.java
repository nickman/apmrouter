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
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.helios.apmrouter.server.ServerComponentBean;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.util.CharsetUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.jmx.export.annotation.ManagedAttribute;

/**
 * <p>Title: HttpRequestRouter</p>
 * <p>Description: Service to route incoming {@link HttpRequest}s to a {@link HttpRequestHandler} by the request URI/</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.unification.pipeline.http.HttpRequestRouter</code></p>
 */

public class HttpRequestRouter extends ServerComponentBean  implements ChannelUpstreamHandler {
	/** A map of {@link HttpRequestHandler}s keyed by their bean name */
	protected Map<String, HttpRequestHandler> handlers = new ConcurrentHashMap<String, HttpRequestHandler>();
	/** A map of {@link HttpRequestHandler}s keyed by the URI pattern they respond to */
	protected ConcurrentHashMap<String, HttpRequestHandler> uriRoutes = new ConcurrentHashMap<String, HttpRequestHandler>();
	/** The websocket handler to be inserted into the pipeline if a request comes in with a URI suffix of {@link #WS_URI_SUFFIX} */
	protected WebSocketServiceHandler webSocketHandler = null;
    /** Default page URI */
    public static final String DEFAULT_URI = "index.html";
    /** Root URI */
    public static final String ROOT_URI = "/";
    /** WebSocket URI Suffix */
    public static final String WS_URI_SUFFIX = "/ws";
	
	
	/**
	 * Creates a new HttpRequestRouter and adds {@link HttpRequestHandlerStarted} and {@link HttpRequestHandlerStopped} to
	 * the supported application event set.
	 */
	public HttpRequestRouter() {
		super();
		supportedEventTypes.add(HttpRequestHandlerStarted.class);
		supportedEventTypes.add(HttpRequestHandlerStopped.class);
	}
	
	/**
	 * On start, searches the app context for {@link HttpRequestHandler}s not already registered.
	 * @param event The app context refresh event
	 */
	@Override
	public void onApplicationContextRefresh(ContextRefreshedEvent event) {
		Map<String, HttpRequestHandler> inits = event.getApplicationContext().getBeansOfType(HttpRequestHandler.class);
		if(!inits.isEmpty()) {
			for(Map.Entry<String, HttpRequestHandler> entry: inits.entrySet()) {				
				if(!handlers.containsKey(entry.getKey())) {
					handlers.put(entry.getKey(), entry.getValue());
					int added = 0;
					for(String key: entry.getValue().getUriPatterns()) {
						key = key.trim().toLowerCase();
						if(uriRoutes.putIfAbsent(key, entry.getValue())==null) {
							added++;
						} else {
							warn("HttpRequestHandler [", entry.getKey(), "] attempted to register with URI [", key, "] which was already registered");
						}
					}
					info("Adding Discovered HttpRequestHandler [", entry.getKey(), "] with [", added, "] URI keys" );
				}
			}
		}
	}

	/**
	 * Called when a {@link HttpRequestHandler} starts
	 * @param handlerStarted The {@link HttpRequestHandler} start event
	 */
	public void onApplicationEvent(HttpRequestHandlerStarted handlerStarted) {
		HttpRequestHandler rh = handlerStarted.getHandler();
		if(!handlers.containsKey(rh.getBeanName())) {
			handlers.put(rh.getBeanName(), rh);
			int added = 0;
			for(String key: rh.getUriPatterns()) {
				key = key.trim().toLowerCase();
				if(uriRoutes.putIfAbsent(key, rh)==null) {
					added++;
				} else {
					warn("HttpRequestHandler [", rh.getBeanName() , "] attempted to register with URI [", key, "] which was already registered");
				}
			}
			info("Adding Discovered HttpRequestHandler [", rh.getBeanName(), "] with [", added, "] URI keys" );
		}
	}
	
	/**
	 * Called when a {@link HttpRequestHandler} stops
	 * @param handlerStarted The {@link HttpRequestHandler} stop event
	 */
	public void onApplicationEvent(HttpRequestHandlerStopped handlerStarted) {
		HttpRequestHandler rh = handlerStarted.getHandler();
		if(handlers.remove(rh.getBeanName())!=null) {
			int removed = 0;
			for(String key: rh.getUriPatterns()) {
				key = key.trim().toLowerCase();
				if(uriRoutes.remove(key)!=null) removed++;
			}
			info("Removed Stopped HttpRequestHandler [", rh.getBeanName(), "] with [", removed, "] URI keys" );
		}
	}
	

	/**
	 * Returns an unmodifiable set of URI patterns that this modifier is activated for 
	 * @return an unmodifiable set of URI patterns that this modifier is activated for
	 */
	@ManagedAttribute(description="URI patterns that this modifier is activated for")
	public Map<String, String> getUriMappings() {
		Map<String, String> map = new HashMap<String, String>(handlers.size());
		for(Map.Entry<String, HttpRequestHandler> entry: uriRoutes.entrySet()) {
			map.put(entry.getKey(), entry.getValue().getClass().getName());
		}
		return map;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelUpstreamHandler#handleUpstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		HttpRequest request = null;
		if(!(e instanceof MessageEvent)) {
			if(e instanceof ExceptionEvent) {
				error("Http Routing Exception ", ((ExceptionEvent)e).getCause());
				sendError(ctx, INTERNAL_SERVER_ERROR);
				return;
			}
			ctx.sendUpstream(e);
			return;
		}
		MessageEvent me = (MessageEvent)e;
		Object message = me.getMessage();
		if(message instanceof WebSocketFrame) {
			ctx.sendUpstream(e);
			return;
		}
		try {
			request = (HttpRequest)((MessageEvent)e).getMessage();
			if(request==null) {
				throw new Exception("HttpRequest was null", new Throwable());
			}
		} catch (Exception ex) {
			throw new Exception("Failed to extract a message event for assumed type HttpRequest", ex);
		}
		// now we have a request...
		String uri = request.getUri();
		if(uri.endsWith(WS_URI_SUFFIX)) {
			ctx.getPipeline().addLast(webSocketHandler.getBeanName(), webSocketHandler);
			ctx.sendUpstream(e);
			return;
		}
        if(uri.isEmpty() || ROOT_URI.equals(uri)) {
        	uri = "index.html";
        }
		
		debug("Processing HTTP Request for URI [", uri, "]");
		HttpRequestHandler handler = uriRoutes.get(uri);
		if(handler==null) {
			handler = uriRoutes.get("");  // the default handler is the file server
		}
		if(handler==null) {
			sendError(ctx, NOT_FOUND);
		} else {
			try {
				handler.handle(ctx, (MessageEvent)e, request, uri);
			} catch (Exception ex) {
		        Channel ch = e.getChannel();
		        error("Uncaught exception", ex);
		        if (ex instanceof TooLongFrameException) {
		            sendError(ctx, BAD_REQUEST);
		            return;
		        }
		        if (ch.isConnected()) {
		            sendError(ctx, INTERNAL_SERVER_ERROR);
		        }
				
			}
		}
	}
	
    
    /**
     * Handles uncaught exceptions
     * @param ctx The channel handler context
     * @param e The exception event
     * @throws Exception the uncaught exception handling exception
     */
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        Channel ch = e.getChannel();
        Throwable cause = e.getCause();
        if(cause != null && cause instanceof TooLongFrameException) {
        	
        }
        error("Uncaught exception", cause);
        if (cause instanceof TooLongFrameException) {
            sendError(ctx, BAD_REQUEST);
            return;
        }
        if (ch.isConnected()) {
            sendError(ctx, INTERNAL_SERVER_ERROR);
        }
    }

	
    /**
     * Returns an HTTP error back to the caller
     * @param ctx The channel handler context
     * @param status The HTTP Status to send
     */
    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, status);
        response.setHeader(CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.setContent(ChannelBuffers.copiedBuffer(
                "Failure: " + status.toString() + "\r\n",
                CharsetUtil.UTF_8));

        // Close the connection as soon as the error message is sent.
        if(ctx.getChannel().isOpen()) {
        	ctx.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
        }
    }

	/**
	 * Sets the websocket handler to be inserted into the pipeline if a request comes in with a URI suffix of {@link #WS_URI_SUFFIX}
	 * @param webSocketHandler the webSocketHandler to set
	 */
    @Autowired(required=true)
	public void setWebSocketHandler(WebSocketServiceHandler webSocketHandler) {
		this.webSocketHandler = webSocketHandler;
	}
	
	

}
