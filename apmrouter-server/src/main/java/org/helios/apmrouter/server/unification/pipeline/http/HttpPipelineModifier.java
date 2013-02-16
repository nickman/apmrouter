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

import org.helios.apmrouter.server.unification.pipeline.AbstractPipelineModifier;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;
import org.jboss.netty.logging.InternalLogLevel;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>Title: HttpPipelineModifier</p>
 * <p>Description: Pipeline modifier for HTTP connections</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.unification.pipeline.http.HttpPipelineModifier</code></p>
 */
public class HttpPipelineModifier extends AbstractPipelineModifier {
	/** The pipeline logging handler */
	protected final LoggingHandler loggingHandler = new LoggingHandler(getClass(), InternalLogLevel.ERROR, true);

	/** The http request router */
	protected HttpRequestRouter router = null;
	
	/**
	 * Creates a new HttpPipelineModifier
	 */
	public HttpPipelineModifier() {
		super("http");
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.unification.pipeline.AbstractPipelineModifier#doModifyPipeline(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.Channel, org.jboss.netty.buffer.ChannelBuffer)
	 */
	@Override
	protected void doModifyPipeline(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) {
		ChannelPipeline pipeline = ctx.getPipeline();
		pipeline.addLast("decoder", new HttpRequestDecoder());
		pipeline.addLast("aggregator", new HttpChunkAggregator(65536));
		pipeline.addLast("encoder", new HttpResponseEncoder());       
		pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
		pipeline.addLast("router", router);
		if(this.log.isDebugEnabled()) {
			pipeline.addFirst("logger", loggingHandler);
		}
//		pipeline.addLast(wsHandler.getBeanName(), wsHandler);
//		pipeline.addLast(HttpStaticFileServerHandler.NAME , fsHandler);
		
	}

	/**
	 * Sets the http request router
	 * @param router the router to set
	 */
	@Autowired(required=true)
	public void setRouter(HttpRequestRouter router) {
		this.router = router;
	}
	
	


	
	

}
