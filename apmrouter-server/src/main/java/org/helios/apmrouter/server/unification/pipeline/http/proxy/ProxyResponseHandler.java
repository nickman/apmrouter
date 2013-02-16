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

import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelLocal;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

/**
 * <p>Title: ProxyResponseHandler</p>
 * <p>Description: A channel handler that waits for an {@link HttpResponse} to come down the pipe from a proxied server and sends it to the original requesting client.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.unification.pipeline.http.proxy.ProxyResponseHandler</code></p>
 */

public class ProxyResponseHandler implements ChannelUpstreamHandler {
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	/** The host to proxy for */
	protected final String targetHost;
	/** The port to proxy for */
	protected final int targetPort;
	/** The remote key */
	protected final String remoteKey;
	/** The port as a string for parsing a 302 */
	protected final String portKey;
	/** The port key length */
	protected final int portKeyLength;
	/** A channel local for holding the original http request in case we need to handle a 302 */
	public static final ChannelLocal<HttpRequest> httpRequestChannelLocal = new ChannelLocal<HttpRequest>(true);
	/** A channel local for holding the original channel handler context */
	public static final ChannelLocal<ChannelHandlerContext> ctxChannelLocal = new ChannelLocal<ChannelHandlerContext>(true);

	
	
	
	
	/**
	 * Creates a new ProxyResponseHandler
	 * @param targetHost The target host
	 * @param targetPort The target port
	 */
	public ProxyResponseHandler(String targetHost, int targetPort) {
		this.targetHost = targetHost;
		this.targetPort = targetPort;
		remoteKey = targetHost + ":" + targetPort;
		portKey = ":" + targetPort;
		portKeyLength = portKey.length();
	}



	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelUpstreamHandler#handleUpstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	@Override
	public void handleUpstream(final ChannelHandlerContext proxyCtx, final ChannelEvent proxyChannelEvent) throws Exception {
		final Channel proxyChannel = proxyCtx.getChannel();
		proxyCtx.setAttachment(null);
		if(proxyChannelEvent instanceof MessageEvent) {
			final HttpRequest newRequest = httpRequestChannelLocal.get(proxyChannel);
			final ChannelHandlerContext originalCtx = ctxChannelLocal.get(proxyChannel);
			final Channel originalChannel = originalCtx.getChannel();
			httpRequestChannelLocal.remove(proxyChannel);
			ctxChannelLocal.remove(proxyChannel);
			MessageEvent me = (MessageEvent)proxyChannelEvent;
			Object message = me.getMessage();
			if(message instanceof HttpResponse) {
				if(log.isDebugEnabled()) log.debug("Received response from remote [" + message + "]");
				HttpResponse resp = (HttpResponse)message;
				if(resp.getStatus().equals(HttpResponseStatus.FOUND)) {
					String reUri = resp.getHeader("Location");
					reUri = reUri.substring(reUri.indexOf(portKey)+portKeyLength);
					
					newRequest.setUri(reUri);
					proxyChannel.write(newRequest);
					httpRequestChannelLocal.remove(originalChannel);
				} else {
					// Send the response back to the caller
					ChannelFuture cf = Channels.future(originalChannel);
					originalCtx.sendDownstream(new DownstreamMessageEvent(originalChannel, cf, resp, originalChannel.getRemoteAddress()));
					cf.addListener(new ChannelFutureListener() {
						@Override
						public void operationComplete(ChannelFuture f) throws Exception {
							if(f.isSuccess()) {
								if(log.isDebugEnabled()) log.debug("Completed response write back to caller");
							} else {
								log.error("Failed to write response back to caller", f.getCause());
								f.getCause().printStackTrace(System.err);
							}
						}
					});
				}				
			}
		} else {
			proxyCtx.sendUpstream(proxyChannelEvent);
		}
	}

}
