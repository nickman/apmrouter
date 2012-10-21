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

import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static org.jboss.netty.handler.codec.http.HttpHeaders.setContentLength;
import static org.jboss.netty.handler.codec.http.HttpMethod.GET;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;

import org.helios.apmrouter.dataservice.json.JSONRequestRouter;
import org.helios.apmrouter.server.ServerComponentBean;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelLocal;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import org.jboss.netty.util.CharsetUtil;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>Title: WebSocketServiceHandler</p>
 * <p>Description: WebSocket handler for fronting JSON based data-services</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.services.session.WebSocketServiceHandler</code></p>
 */

public class WebSocketServiceHandler extends ServerComponentBean implements ChannelUpstreamHandler, ChannelDownstreamHandler {
	/** The JSON Request Router */
	protected JSONRequestRouter router = null;
	/** A channel local for the websocket handshaker */
	protected final ChannelLocal<WebSocketServerHandshaker> wsHandShaker = new ChannelLocal<WebSocketServerHandshaker>(true); 

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelUpstreamHandler#handleUpstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		if(e instanceof MessageEvent) {
			Object message = ((MessageEvent)e).getMessage();
			if (message instanceof HttpRequest) {
				handleRequest(ctx, (HttpRequest) message, (MessageEvent)e);
			} else if (message instanceof WebSocketFrame) {
				handleRequest(ctx, (WebSocketFrame) message);
			}
		} else {
			ctx.sendUpstream(e);
		}		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelDownstreamHandler#handleDownstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	@Override
	public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		Channel channel = e.getChannel();
		if(!channel.isOpen()) return;
		if(!(e instanceof MessageEvent)) {
            ctx.sendDownstream(e);
            return;
        }
		Object message = ((MessageEvent)e).getMessage();
		if(!(message instanceof JSONObject) && !(message instanceof CharSequence)) {
            ctx.sendDownstream(e);
            return;			
		}
		WebSocketFrame frame = new TextWebSocketFrame(message.toString());		
		ctx.sendDownstream(new DownstreamMessageEvent(channel, Channels.future(channel), frame, channel.getRemoteAddress()));		
	}		

	/**
	 * Handles uncaught exceptions in the pipeline from this handler
	 * @param ctx The channel context
	 * @param ev The exception event
	 */
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent ev) {
		error("Uncaught exception ", ev.getCause());
	}
	
	/**
	 * Processes a websocket request
	 * @param ctx The channel handler context
	 * @param frame The websocket frame request to process
	 */
	public void handleRequest(ChannelHandlerContext ctx, WebSocketFrame frame) {
        // Check for closing frame
        if (frame instanceof CloseWebSocketFrame) {
        	wsHandShaker.get(ctx.getChannel()).close(ctx.getChannel(), (CloseWebSocketFrame) frame);
            return;
        } else if (frame instanceof PingWebSocketFrame) {
            ctx.getChannel().write(new PongWebSocketFrame(frame.getBinaryData()));
            return;
        } else if (!(frame instanceof TextWebSocketFrame)) {
            throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass()
                    .getName()));
        }
        String request = ((TextWebSocketFrame) frame).getText();
        JSONObject wsRequest = null;
        try {
        	wsRequest = new JSONObject(request);
        	if("who".equals(wsRequest.get("t"))) {
        		SocketAddress sa = ctx.getChannel().getRemoteAddress();
        		String host = "unknown";
        		String agent = "unknown";
        		if(sa!=null) {
        			host = ((InetSocketAddress)sa).getHostName();        			
        		}
        		if(wsRequest.get("agent")!=null) {
        			agent = wsRequest.get("agent").toString();
        		}
        		SharedChannelGroup.getInstance().add(ctx.getChannel(), ChannelType.WEBSOCKET_REMOTE, "ClientWebSocket", host, agent);
        	} else {
        		router.invoke(wsRequest, ctx.getChannel());
        	}
        	
        		
        } catch (Exception ex) {
        	log.error("Failed to parse request [" + request + "]", ex);
        }		
	}
	
	/**
	 * Processes an HTTP request
	 * @param ctx The channel handler context
	 * @param req The HTTP request
	 * @param me The message event that will be sent upstream if not handled here
	 */
	public void handleRequest(ChannelHandlerContext ctx, HttpRequest req, MessageEvent me) {
        if (req.getMethod() != GET) {
            sendHttpResponse(ctx, req, new DefaultHttpResponse(HTTP_1_1, FORBIDDEN));
            return;
        }
        String uri = req.getUri();
        if(!"/ws".equals(uri)) {
        	ctx.sendUpstream(me);
        	return;
        }
        // Handshake
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(getWebSocketLocation(req), null, false);
        WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            wsFactory.sendUnsupportedWebSocketVersionResponse(ctx.getChannel());
        } else {
        	wsHandShaker.set(ctx.getChannel(), handshaker);
        	ChannelFuture cf = handshaker.handshake(ctx.getChannel(), req); 
            cf.addListener(WebSocketServerHandshaker.HANDSHAKE_LISTENER);
            cf.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture f) throws Exception {
					if(f.isSuccess()) {
						Channel wsChannel = f.getChannel();
						//SharedChannelGroup.getInstance().add(f.getChannel(), ChannelType.WEBSOCKET_REMOTE, "WebSocketClient-" + f.getChannel().getId());
						wsChannel.write(new JSONObject(Collections.singletonMap("sessionid", wsChannel.getId())));
						//wsChannel.getPipeline().remove(DefaultChannelHandler.NAME);
					}
				}
			});
        }
	}
	
    /**
     * Sends an HTTP response
     * @param ctx The channel handler context
     * @param req The HTTP request being responded to
     * @param res The HTTP response to send
     */
    private void sendHttpResponse(ChannelHandlerContext ctx, HttpRequest req, HttpResponse res) {
        // Generate an error page if response status code is not OK (200).
        if (res.getStatus().getCode() != 200) {
            res.setContent(ChannelBuffers.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8));
            setContentLength(res, res.getContent().readableBytes());
        }

        // Send the response and close the connection if necessary.
        ChannelFuture f = ctx.getChannel().write(res);
        if (!isKeepAlive(req) || res.getStatus().getCode() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }
    
    /**
     * Generates a websocket URL for the passed request
     * @param req The http request
     * @return The websocket URL
     */
    private String getWebSocketLocation(HttpRequest req) {
        return "ws://" + req.getHeader(HttpHeaders.Names.HOST) + "/ws";
    }
	
	/**
	 * Sets the json request router
	 * @param router the router to set
	 */
	@Autowired(required=true)
	public void setRouter(JSONRequestRouter router) {
		this.router = router;
	}


}
