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
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;

import org.helios.apmrouter.util.SimpleLogger;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelState;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.UpstreamMessageEvent;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.json.JSONObject;

/**
 * <p>Title: JsonCodec</p>
 * <p>Description: Coder/Encoder for JSON requests</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.wsclient.JsonCodec</code></p>
 */

@Sharable
public class JsonCodec extends SimpleChannelHandler {
	
	/** A set of web socket event listeners */
	protected final Set<WebSocketEventListener> eventListeners = new CopyOnWriteArraySet<WebSocketEventListener>();
	/** A thread pool to execute listener notifications in */
	protected final Executor threadPool;
	
	/** The elading string in the response containing the sessionId returned by the server */
	public static final String SESSION_SIGNATURE = "{\"sessionid\":";
	
	/**
	 * Creates a new JsonCodec
	 * @param threadPool The application thread pool
	 */
	public JsonCodec(Executor threadPool) {
		this.threadPool = threadPool;
	}
	
	/**
	 * Registers the passed WebSocketEventListener to be notified of events on any websocket
	 * @param listener the listener to register
	 */
	public void addWebSocketEventListener(WebSocketEventListener listener) {
		if(listener!=null) {
			eventListeners.add(listener);
		}
	}
	
	/**
	 * Unregisters the passed WebSocketEventListener 
	 * @param listener the listener to unregister
	 */
	public void removeWebSocketEventListener(WebSocketEventListener listener) {
		if(listener!=null) {
			eventListeners.remove(listener);
		}
	}
	
	/**
	 * Executes the {@link WebSocketEventListener#onMessage(SocketAddress, JSONObject)} event to all registered listeners.
	 * @param sa The remote address of the server the event came from
	 * @param json The message received from the server
	 */
	protected void fireJsonEvent(final SocketAddress sa, final JSONObject json) {
		threadPool.execute(new Runnable(){
			@Override
			public void run() {
				for(WebSocketEventListener listener: eventListeners) {
					listener.onMessage(sa, json);
				}
			}
		});
	}
	
	/**
	 * Executes the {@link WebSocketEventListener#onError(SocketAddress, Throwable)} event to all registered listeners.
	 * @param sa The remote address of the server the event came from
	 * @param exception The exception thrown from the pipeline
	 */
	protected void fireExceptionEvent(final SocketAddress sa, final Throwable exception) {
		threadPool.execute(new Runnable(){
			@Override
			public void run() {
				for(WebSocketEventListener listener: eventListeners) {
					listener.onError(sa, exception);
				}
			}
		});
	}
	
	/**
	 * Executes the {@link WebSocketEventListener#onClose(SocketAddress) } event to all registered listeners.
	 * @param sa The remote address of the server connected to
	 */
	protected void fireCloseEvent(final SocketAddress sa) {
		threadPool.execute(new Runnable(){
			@Override
			public void run() {
				for(WebSocketEventListener listener: eventListeners) {
					listener.onClose(sa);
				}
			}
		});
	}
	
	/**
	 * Executes the {@link WebSocketEventListener#onConnect(SocketAddress) } event to all registered listeners.
	 * @param sa The remote address of the server connected to
	 */
	protected void fireConnectedEvent(final SocketAddress sa) {
		threadPool.execute(new Runnable(){
			@Override
			public void run() {
				for(WebSocketEventListener listener: eventListeners) {
					listener.onConnect(sa);
				}
			}
		});
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.SimpleChannelHandler#messageReceived(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
	 */
	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		//SimpleLogger.debug("JsonCodec: Received from [", e.getRemoteAddress() , "]-->[", e.getMessage(), "]" );
		Object event = e.getMessage();
		if(event instanceof TextWebSocketFrame) {
			TextWebSocketFrame textFrame = (TextWebSocketFrame)event;
			String message = textFrame.getText();
			if(message!=null && message.indexOf(SESSION_SIGNATURE)!=-1) {
				super.messageReceived(ctx, new UpstreamMessageEvent(e.getChannel(), "" + new JSONObject(message).get("sessionid"), e.getRemoteAddress()));
			} else {
				ChannelHandlerContext synchContext = ctx.getPipeline().getContext("Synch" + ctx.getChannel().getId()); 
				if(synchContext!=null && synchContext.canHandleUpstream()) {
					ctx.getPipeline().getContext(this).sendUpstream(new UpstreamMessageEvent(e.getChannel(), new JSONObject(textFrame.getText()), e.getRemoteAddress()));
				} else {
					fireJsonEvent(e.getRemoteAddress(), new JSONObject(textFrame.getText()));
				}
			}
		}		
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.SimpleChannelHandler#exceptionCaught(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ExceptionEvent)
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		WebSocketClientHandler wsc = (WebSocketClientHandler)ctx.getPipeline().get("ws-handler");
		SocketAddress sa = wsc.getRemoteSocketAddress();
		fireExceptionEvent(sa, e.getCause());
		super.exceptionCaught(ctx, e);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.SimpleChannelHandler#channelClosed(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)
	 */
	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		if(e.getState().equals(ChannelState.OPEN) && e.getValue().equals(false)) {
			fireCloseEvent(e.getChannel().getRemoteAddress());
		}
		super.channelClosed(ctx, e);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.SimpleChannelHandler#channelConnected(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelStateEvent)
	 */
	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		if(e.getState().equals(ChannelState.CONNECTED)) {
			fireConnectedEvent(e.getChannel().getRemoteAddress());
		}
		super.channelConnected(ctx, e);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.SimpleChannelHandler#writeRequested(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
	 */
	@Override
	public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		Object msg = e.getMessage();
		final Channel channel = e.getChannel();
		if(msg instanceof JSONObject) {			
			byte[] payload = msg.toString().getBytes();						 
			super.writeRequested(ctx, new DownstreamMessageEvent(channel, Channels.future(channel), new TextWebSocketFrame(ChannelBuffers.wrappedBuffer(payload)), channel.getRemoteAddress()));
			return;
		} else if(msg instanceof CharSequence) {
			super.writeRequested(ctx, new DownstreamMessageEvent(channel, Channels.future(channel), new TextWebSocketFrame(msg.toString()), channel.getRemoteAddress()));
			return;
		}
		super.writeRequested(ctx, e);
	}

}
