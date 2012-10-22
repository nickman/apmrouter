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
package org.helios.apmrouter.server.unification.pipeline.http.proxy;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.helios.apmrouter.server.services.session.ChannelType;
import org.helios.apmrouter.server.services.session.SharedChannelGroup;
import org.helios.apmrouter.server.unification.pipeline.http.AbstractHttpRequestHandler;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.util.CharsetUtil;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>Title: HttpRequestProxy</p>
 * <p>Description: An http handler implementation that asynchronously dispatches received requests to the configured remote server.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.unification.pipeline.http.HttpRequestProxy</code></p>
 * TODO: Remap URIs if required. e.g. "/jolokia" --> "/jmx"
 */

public class HttpRequestProxy extends AbstractHttpRequestHandler {
	/** The host to proxy for */
	protected String targetHost = null;
	/** The port to proxy for */
	protected int targetPort = -1;
	/** The remote key */
	protected String remoteKey = null;
	/** The channel factory for proxies */
	protected ProxyChannelFactory channelFactory = null;
	/** A cache of proxied connections */
	protected static final Map<String, Channel> proxyConnections = new ConcurrentHashMap<String, Channel>();
	
	/**
	 * Creates a new HttpRequestProxy
	 */
	public HttpRequestProxy() {
		super();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStart()
	 */
	@Override
	protected void doStart() throws Exception {
		remoteKey = targetHost + ":" + targetPort;
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.unification.pipeline.http.HttpRequestHandler#handle(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent, org.jboss.netty.handler.codec.http.HttpRequest, java.lang.String)
	 */
	@Override
	public void handle(final ChannelHandlerContext ctx, MessageEvent e, final HttpRequest request, String path) throws Exception {
		final HttpRequest newRequest = new DefaultHttpRequest(request.getProtocolVersion(), request.getMethod(), request.getUri());
		newRequest.setContent(request.getContent());
		for(String hdr: request.getHeaderNames()) {
			if("Host".equalsIgnoreCase(hdr)) continue;
			newRequest.setHeader(hdr, request.getHeader(hdr));
		}
		newRequest.setHeader("Host", targetHost + ":" + targetPort);
		newRequest.setChunked(request.isChunked());
		debug("Sending HttpRequest [\n", newRequest,"\n] to [", remoteKey, "]");
		Channel proxyChannel = proxyConnections.get(remoteKey);
		if(proxyChannel==null) {
			synchronized(proxyConnections) {
				proxyChannel = proxyConnections.get(remoteKey);
				if(proxyChannel==null) {					
					Runnable onConnectRunnable = new Runnable() {
						@Override
						public void run() {
							processProxyRequest(ctx, ctx.getChannel(), proxyConnections.get(remoteKey), newRequest);
						}
					};
					getProxyConnection(ctx, onConnectRunnable);
				}
			}
		}
		if(proxyChannel!=null) {
			processProxyRequest(ctx, ctx.getChannel(), proxyChannel, newRequest);
		}
	}
	
	/**
	 * Asynchronously acquires a connection to the proxied server
	 * @param originalCtx The channel handler context of the original request
	 * @param onConnectRunnable A task to run once the connection has been acquired
	 */
	protected void getProxyConnection(final ChannelHandlerContext originalCtx, final Runnable onConnectRunnable) {
		channelFactory.newChannelAsynch(targetHost, targetPort).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture f1) throws Exception {
				if(!f1.isSuccess()) {
					error("Failed to connect proxy to remote at [", targetHost, ":", targetPort, "]", f1.getCause());
					sendError(originalCtx, HttpResponseStatus.SERVICE_UNAVAILABLE);
				} else {
					debug("Connected proxy to remote at [", remoteKey, "]");
					final Channel proxyChannel = f1.getChannel();
					proxyChannel.getPipeline().addLast("responseHandler", new ProxyResponseHandler(targetHost, targetPort));
					Channel priorChannel = proxyConnections.put(remoteKey, proxyChannel);
					if(priorChannel!=null) priorChannel.close();
					SharedChannelGroup.getInstance().add(proxyChannel, ChannelType.LOCAL_CLIENT, "ProxyTo[" + remoteKey + "]", "", "");
					proxyChannel.getCloseFuture().addListener(new ChannelFutureListener() {
						@Override
						public void operationComplete(ChannelFuture f) throws Exception {
							Channel closedChannel = f.getChannel();
							Channel cachedChannel = proxyConnections.get(remoteKey);
							if(cachedChannel!=null && closedChannel.getId().equals(cachedChannel.getId())) {
								proxyConnections.remove(remoteKey);
							}
						}
					});
					if(onConnectRunnable!=null) {
						onConnectRunnable.run();
					}
				}
			}
		});		
	}
	
	/**
	 * Sends the original request to the proxied server. The response will be handled by the <code>responseHandler</code> installed into the pipeline.
	 * @param originalCtx The original request channel handler context
	 * @param originalChannel The original request channel handler context
	 * @param proxyChannel The connection to the proxied server
	 * @param request The modified Http request
	 */
	protected void processProxyRequest(final ChannelHandlerContext originalCtx, final Channel originalChannel, final Channel proxyChannel, final HttpRequest request) {
		proxyChannel.getPipeline().getContext("responseHandler").setAttachment(originalCtx);
		ProxyResponseHandler.httpRequestChannelLocal.set(proxyChannel, request);
		ProxyResponseHandler.ctxChannelLocal.set(proxyChannel, originalCtx);
		proxyChannel.write(request);
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
        ctx.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
    }
	

	/**
	 * Returns 
	 * @return the targetHost
	 */
	public String getTargetHost() {
		return targetHost;
	}

	/**
	 * Sets 
	 * @param targetHost the targetHost to set
	 */
	public void setTargetHost(String targetHost) {
		this.targetHost = targetHost;
	}

	/**
	 * Returns 
	 * @return the targetPort
	 */
	public int getTargetPort() {
		return targetPort;
	}

	/**
	 * Sets 
	 * @param targetPort the targetPort to set
	 */
	public void setTargetPort(int targetPort) {
		this.targetPort = targetPort;
	}

	/**
	 * Sets the proxy client channel factory
	 * @param channelFactory the channelFactory to set
	 */
	@Autowired(required=true)
	public void setChannelFactory(ProxyChannelFactory channelFactory) {
		this.channelFactory = channelFactory;
	}

}


/*
OLD IMPL.
=========

		//processProxyRequest(final ChannelHandlerContext originalCtx, final Channel originalChannel, final Channel proxyChannel, final HttpRequest request)
		
//		channelFactory.newChannelAsynch(targetHost, targetPort).addListener(new ChannelFutureListener() {
//			@Override
//			public void operationComplete(ChannelFuture f1) throws Exception {
//				if(!f1.isSuccess()) {
//					error("Failed to connect proxy to remote at [", targetHost, ":", targetPort, "]", f1.getCause());
//					sendError(ctx, HttpResponseStatus.SERVICE_UNAVAILABLE);
//				} else {
//					debug("Connected proxy to remote at [", remoteKey, "]");
//					final Channel clientChannel = f1.getChannel();
//					SharedChannelGroup.getInstance().add(clientChannel, ChannelType.LOCAL_CLIENT, "ProxyTo[" + remoteKey + "]", "", "");
//					clientChannel.getPipeline().addLast("proxyResponder", new ChannelUpstreamHandler() {
//						@Override
//						public void handleUpstream(ChannelHandlerContext ct, ChannelEvent e) throws Exception {
//							if(e instanceof MessageEvent) {
//								MessageEvent me = (MessageEvent)e;
//								Object message = me.getMessage();
//								if(message instanceof HttpResponse) {
//									debug("Received response from remote [", message, "]");
//									HttpResponse resp = (HttpResponse)message;
//									if(resp.getStatus().equals(HttpResponseStatus.FOUND)) {
//										String reUri = resp.getHeader("Location");
//										reUri = reUri.substring(reUri.indexOf("" + targetPort)+(""+targetPort).length());
//										newRequest.setUri(reUri);
//										clientChannel.write(newRequest);
//									} else {
//										Channel ch = ctx.getChannel();
//										ChannelFuture cf = Channels.future(ch);
//										ctx.sendDownstream(new DownstreamMessageEvent(ch, cf, resp, ch.getRemoteAddress()));
//										cf.addListener(new ChannelFutureListener() {
//											@Override
//											public void operationComplete(ChannelFuture f3) throws Exception {
//												if(f3.isSuccess()) {
//													debug("Completed response write back to caller");
//												} else {
//													error("Failed to write response back to caller", f3.getCause());
//													f3.getCause().printStackTrace(System.err);
//												}
//											}
//										});
//									}
//									return;
//								}
//							}
//							ct.sendUpstream(e);
//						}
//					});
//					clientChannel.write(newRequest).addListener(new ChannelFutureListener() {
//						@Override
//						public void operationComplete(ChannelFuture f2) throws Exception {
//							if(f2.isSuccess()) {
//								debug("Completed proxy write to remote at [", remoteKey, "]");
//							} else {
//								error("Failed to write request to remote at [", remoteKey, "]", f2.getCause());
//								f2.getCause().printStackTrace(System.err);
//							}
//						}
//					});
//				}
//			}
//		});



*/