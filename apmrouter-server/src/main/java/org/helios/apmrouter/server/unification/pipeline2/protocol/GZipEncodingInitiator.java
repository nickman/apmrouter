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

import org.helios.apmrouter.server.unification.pipeline2.AbstractInitiator;
import org.helios.apmrouter.server.unification.pipeline2.FlushOnCloseBufferAggregator;
import org.helios.apmrouter.server.unification.pipeline2.ProtocolSwitchContext;
import org.helios.apmrouter.server.unification.pipeline2.ProtocolSwitchDecoder;
import org.helios.apmrouter.server.unification.pipeline2.SwitchPhase;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.compression.ZlibDecoder;
import org.jboss.netty.handler.codec.compression.ZlibWrapper;

/**
 * <p>Title: GZipEncodingInitiator</p>
 * <p>Description: An initiator that detects a GZip encoded stream of data</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.unification.pipeline2.encoding.GZipEncodingInitiator</code></p>
 */

/**
 * <p>Title: GZipEncodingInitiator</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.unification.pipeline2.protocol.GZipEncodingInitiator</code></p>
 */
public class GZipEncodingInitiator extends AbstractInitiator  {

	/**
	 * Creates a new GZipEncodingInitiator
	 */
	public GZipEncodingInitiator() {
		super(2, "gzip");
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.unification.pipeline2.Initiator#match(org.jboss.netty.buffer.ChannelBuffer)
	 */
	@Override
	public Object match(ChannelBuffer buff) {
		boolean match =  isGzip(buff) ? true : null;
		return match;
	}
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.unification.pipeline2.Initiator#process(org.helios.apmrouter.server.unification.pipeline2.ProtocolSwitchContext, java.lang.Object)
	 */
	@Override
	public SwitchPhase process(final ProtocolSwitchContext context, Object matchKey) {
		log.info("Protocol Switch:  Prepending ZlibDecoder");
		final ChannelPipeline pipeline = context.getPipeline();
		pipeline.remove("logging");
		pipeline.addBefore(ProtocolSwitchDecoder.PIPE_NAME, "gzip", new ZlibDecoder(ZlibWrapper.GZIP){
			@Override
			public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent evt) throws Exception {
				if(evt instanceof MessageEvent) {
					int rb = ((ChannelBuffer)((MessageEvent)evt).getMessage()).readableBytes();
					log.info("ZLibDecoder Handling [" + rb + "] bytes");
				}
				super.handleUpstream(ctx, evt);
			}
		});
		context.unReplayChannelBuffer();
		//context.clear();
		context.sendCurrentBufferUpstream("anchor");
		return null;
	}
//	/**
//	 * {@inheritDoc}
//	 * @see org.helios.apmrouter.server.unification.pipeline2.Initiator#process(org.helios.apmrouter.server.unification.pipeline2.ProtocolSwitchContext, java.lang.Object)
//	 */
//	@Override
//	public SwitchPhase process(final ProtocolSwitchContext context, Object matchKey) {
//		log.info("Switching Protocols");
////		LoggingHandler lh = decode(installedLogger);
////		if(lh!=null) pipeline.addLast("logging", lh);		
////		pipeline.addLast(ProtocolSwitchDecoder.PIPE_NAME, applicationContext.getBean("protocolSwitchDecoder", ProtocolSwitchDecoder.class));
////		pipeline.addLast("exec", executionHandler);
//		final ChannelPipeline pipeline = context.getPipeline();		
//		try { pipeline.remove("logging"); } catch (Exception ex) {/* No Op*/}
//		
//		
//		
//		final ChannelHandler pSwitch = pipeline.remove(ProtocolSwitchDecoder.PIPE_NAME);
//		pipeline.addLast("gzip", new ZlibDecoder(ZlibWrapper.GZIP));
//		pipeline.addLast(FlushOnCloseBufferAggregator.PIPE_NAME, FlushOnCloseBufferAggregator.getInstance());
//		pipeline.addLast(FlushOnCloseBufferAggregator.PIPE_NAME + "-Complete", new SimpleChannelUpstreamHandler(){
//			@Override
//		 	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
//				log.info("[" + FlushOnCloseBufferAggregator.PIPE_NAME + "-Complete] FlushOnCloseBufferAggregator Complete Event - [" + ((ChannelBuffer)e.getMessage()).readableBytes() + "] Bytes");
//		 		context.setAggregationHasOccured();		 	
//		 		pipeline.addBefore(EXEC_HANDLER_NAME, ProtocolSwitchDecoder.PIPE_NAME, pSwitch);
//		 		try { pipeline.remove("gzip"); } catch (Exception ex) {/* No Op*/}
//		 		try { pipeline.remove(FlushOnCloseBufferAggregator.PIPE_NAME); } catch (Exception ex) {/* No Op*/}
//		 		try { pipeline.remove(FlushOnCloseBufferAggregator.PIPE_NAME + "-Complete"); } catch (Exception ex) {/* No Op*/}
//		 		//ChannelHandlerContext targetCtx = pipeline.getContext(ctx.getPipeline().getFirst());
//		 		ChannelHandlerContext targetCtx = pipeline.getContext("anchor");
//		 		
//		 		log.info("Sending ungzipped payload back to switch decoder at [" + targetCtx.getName() + "]");
//		 		targetCtx.sendUpstream(e);
//				
//		 	}
//		});
//		
//		context.sendCurrentBufferUpstream(pipeline.getContext(pipeline.getFirst()).getName());
//		return SwitchPhase.INIT;
//	}

	/**
	 * Determines if the two passed bytes represent a gzipped stream of data
	 * @param magic1 The first byte of the incoming request
	 * @param magic2 The second byte of the incoming request
	 * @return true if the incoming payload is gzipped, false otherwise
	 */
	public static boolean isGzip(int magic1, int magic2) {
		return magic1 == 31 && magic2 == 139;	
	}	
	
	
	/**
	 * Determines if the channel is carrying a gzipped payload
	 * @param buffer The channel buffer to check
	 * @return true if the incoming payload is gzipped, false otherwise
	 */
	public static boolean isGzip(ChannelBuffer buffer) {
		return isGzip(buffer.getUnsignedByte(0), buffer.getUnsignedByte(1));
	}	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.unification.pipeline2.AbstractInitiator#requiresFullPayload()
	 */
	@Override
	public boolean requiresFullPayload() {
		return false;
	}

}
