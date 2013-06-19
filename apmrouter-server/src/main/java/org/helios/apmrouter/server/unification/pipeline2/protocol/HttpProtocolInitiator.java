/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2013, Helios Development Group and individual contributors
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
package org.helios.apmrouter.server.unification.pipeline2.protocol;

import org.helios.apmrouter.server.unification.pipeline.http.HttpRequestRouter;
import org.helios.apmrouter.server.unification.pipeline2.AbstractInitiator;
import org.helios.apmrouter.server.unification.pipeline2.ProtocolSwitchDecoder;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;

/**
 * <p>Title: HttpProtocolInitiator</p>
 * <p>Description: A Protocol Initiator to detect and install an HTTP stack into the pipeline.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.unification.pipeline2.protocol.HttpProtocolInitiator</code></p>
 * <br><br>
 * <h4>Network Header Example</h4><pre>
         +-------------------------------------------------+
         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
+--------+-------------------------------------------------+----------------+
|00000000| 47 45 54 20 2f 20 48 54 54 50 2f 31 2e 31 0d 0a |GET / HTTP/1.1..|
<br>
 * </pre>
 */

public class HttpProtocolInitiator extends AbstractInitiator implements ProtocolInitiator {
	/** The http request router */
	protected final HttpRequestRouter router;
	

	/**
	 * Creates a new HttpProtocolInitiator
	 * @param router The http request router 
	 */
	public HttpProtocolInitiator(HttpRequestRouter router) {
		super(2, "http");
		this.router = router;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.unification.pipeline2.Initiator#match(org.jboss.netty.buffer.ChannelBuffer)
	 */
	@Override
	public boolean match(ChannelBuffer buff) {
		if(buff.readableBytes()>=2) {
			return isHttp(buff.getUnsignedByte(0), buff.getUnsignedByte(1));
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.unification.pipeline2.Initiator#modifyPipeline(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.Channel, org.jboss.netty.buffer.ChannelBuffer)
	 */
	@Override
	public void modifyPipeline(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) {
		ChannelPipeline pipeline = ctx.getPipeline();		
		pipeline.addLast("http-decoder", new HttpRequestDecoder());
		pipeline.addLast("http-aggregator", new HttpChunkAggregator(65536));
		pipeline.addLast("http-encoder", new HttpResponseEncoder());       
		pipeline.addLast("http-chunkedWriter", new ChunkedWriteHandler());
		pipeline.addLast("router", router);
		pipeline.remove(ProtocolSwitchDecoder.PIPE_NAME);
	}
	
	/**
	 * Determines if the channel is carrying an HTTP request
	 * @param magic1 The first byte of the incoming request
	 * @param magic2 The second byte of the incoming request
	 * @return true if the incoming is HTTP, false otherwise
	 */
	private boolean isHttp(int magic1, int magic2) {
		 return
		 magic1 == 'G' && magic2 == 'E' || // GET
		 magic1 == 'P' && magic2 == 'O' || // POST
		 magic1 == 'P' && magic2 == 'U' || // PUT
		 magic1 == 'H' && magic2 == 'E' || // HEAD
		 magic1 == 'O' && magic2 == 'P' || // OPTIONS
		 magic1 == 'P' && magic2 == 'A' || // PATCH
		 magic1 == 'D' && magic2 == 'E' || // DELETE
		 magic1 == 'T' && magic2 == 'R' || // TRACE
		 magic1 == 'C' && magic2 == 'O';   // CONNECT
	}
	

}
