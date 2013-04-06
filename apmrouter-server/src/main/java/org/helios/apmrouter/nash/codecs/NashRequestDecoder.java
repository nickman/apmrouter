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
package org.helios.apmrouter.nash.codecs;

import java.io.OutputStream;

import org.helios.apmrouter.nash.NashConstants;
import org.helios.apmrouter.nash.NashRequest;
import org.helios.apmrouter.nash.codecs.DecodingState;
import org.helios.apmrouter.nash.codecs.NashContextState;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.UpstreamMessageEvent;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.handler.codec.replay.ReplayingDecoder;
import org.jboss.netty.logging.InternalLogger;
import org.jboss.netty.logging.InternalLoggerFactory;

/**
 * <p>Title: NashRequestDecoder</p>
 * <p>Description: The Netty channel handler that decodes a nash request</p> 
 * <p>Based in great part on:<ol>
 * 	<li><b><code>com.martiansoftware.nailgun.NGSession</code></b> by <a href="http://www.martiansoftware.com/contact.html">Marty Lamb</a></li>
 *  <li><b><code>com.biasedbit.nettytutorials.customcodecs.common.Decoder</code></b> by <a href="http://biasedbit.com/about/">Bruno Decarvalho</a></li>
 * </ol>
 * <p>The nash stream processor does not rely on a particular in the way that requests are sent, but as of this writing, the order is this:<ol>
 * 	<li><b>ARGUMENTS</b></li>
 * 	<li><b>ENVIRONMENT</b></li>
 * 	<li><b>WORKING_DIR</b></li>
 * 	<li><b>COMMAND</b></li>
 *  </ol></p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.nash.codecs.NashRequestDecoder</code></p>
 */

public class NashRequestDecoder extends ReplayingDecoder<DecodingState>  {
	/** The internal logger */
	protected final InternalLogger log = InternalLoggerFactory.getInstance(getClass());
	/** The request context */
	//protected NashContextState ngcontext = new NashContextState();
	
	/** The signal to send the client when we're ready to handle the stream input */
	private static final ChannelBuffer STREAM_IN_READY = ChannelBuffers.buffer(5);
	/** The signal to send the client when the stream input procesing is complete */
	private static final ChannelBuffer STREAM_IN_DONE = ChannelBuffers.buffer(5);

	/** THe channel group where all active channels are maintained */
	private final ChannelGroup channelGroup = new DefaultChannelGroup("NashServer");
	
	
	static {
		// Prep the stream in readiness buffer
		STREAM_IN_READY.writeInt(0);
		STREAM_IN_READY.writeByte(NashConstants.CHUNKTYPE_STARTINPUT);
		// Prep the stream in complete buffer
		STREAM_IN_DONE.writeInt(0);
		STREAM_IN_DONE.writeByte(NashConstants.CHUNKTYPE_EXIT);
	}
	
	/**
	 * Returns the current NashContextState
	 * @param ctx The channel handler context which might have a current context as an attachment
	 * @return the current NashContextState
	 */
	protected NashContextState getContext(ChannelHandlerContext ctx) {
		NashContextState ncs = (NashContextState) ctx.getAttachment();
		if(ncs==null) {
			ncs = new NashContextState();
			ctx.setAttachment(ncs);
		}
		return ncs;
	}
	
	/**
	 * Creates a new NashRequestDecoder
	 */
	public NashRequestDecoder() {
		if(log.isDebugEnabled()) log.debug("Created NashRequestDecoder Instance");
		checkpoint(DecodingState.BYTES);
		//System.out.println("Created NashRequestDecoder Instance");
		//reset();
	}

	/**
	 * Resets the decoder
	 * @param ctx The channel handler context 
	 */
	private void reset(ChannelHandlerContext ctx) {
		if(log.isDebugEnabled()) log.debug("NashRequestDecoder Reset");
		getContext(ctx).cleanup();
        checkpoint(DecodingState.BYTES);        
    }

	/**
	 * Copies the decoding state into the current context and delegates the checkpoint to the super.
	 * @param ctx The channel handler context 
	 * @param state The current decoding state
	 */
	protected void checkpoint(ChannelHandlerContext ctx, DecodingState state) {
		getContext(ctx).setState(state);
		checkpoint(state);
		if(log.isDebugEnabled()) log.debug("checkpoint:" + state);
	}
	
	@Override
	protected void checkpoint(DecodingState state) {
		super.checkpoint(state);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.handler.codec.replay.ReplayingDecoder#decode(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.Channel, org.jboss.netty.buffer.ChannelBuffer, java.lang.Enum)
	 */
	@Override
	protected NashRequest decode(final ChannelHandlerContext ctx, final Channel channel, ChannelBuffer buffer, DecodingState decodeState) throws Exception {
		channel.getCloseFuture().addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				reset(ctx);
			}
		});
		if(decodeState==null) return null;
		NashContextState context = getContext(ctx);
		context.setState(decodeState);
		if(context.getMessage().getRemoteAddress()==null) {
			channelGroup.add(channel);
			channel.getCloseFuture().addListener(new ChannelFutureListener(){
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					channelGroup.remove(future.getChannel());
				}
			});
			context.getMessage().setChannel(channel);
		}
		while(true) {
			switch(context.getState()) {
				case BYTES:
					// set the output stream in the nash request,
					// create the piped input stream and connect it to the output stream
					// and send the request upstream		
					int bytesToRead = buffer.readInt();
					if(log.isDebugEnabled()) log.debug("NG Chunk [BYTES]:" + bytesToRead);
					context.setBytesToRead(bytesToRead);  
//					if(context.isStdInReady()) {
//						context.initOutputStream(bytesToRead);
//						ctx.sendUpstream(new UpstreamMessageEvent(channel, context.getMessage(), channel.getRemoteAddress()));												
//					}
					checkpoint(ctx, DecodingState.TYPE);
					break;
				case TYPE:
					byte type = buffer.readByte();
					context.setChunkType(type);
					checkpoint(ctx, context.getState());
					if(log.isDebugEnabled()) log.debug("NG Chunk [" + type + "]:" + NashConstants.decode(type));					
					break;
				case WORKING_DIR:
					context.readBytes(buffer);
					context.getMessage().setWorkingDirectory(new String(context.getBytes()));
					checkpoint(ctx, DecodingState.BYTES);
					if(log.isDebugEnabled()) log.debug("NG Chunk [WORKING_DIR]:" + context.getMessage().getWorkingDirectory());
					break;
				case ENVIRONMENT:
					context.readBytes(buffer);
					context.getMessage().addToEnvironment(new String(context.getBytes()));
					checkpoint(ctx, DecodingState.BYTES);
					//if(log.isDebugEnabled()) log.debug("NG Chunk [ENVIRONMENT]:" + new String(bytes));
					break;
				case ARGUMENTS:
					context.readBytes(buffer);
					context.getMessage().addArgument(new String(context.getBytes()));
					checkpoint(ctx, DecodingState.BYTES);
					if(log.isDebugEnabled()) log.debug("NG Chunk [ARGUMENTS]:" + new String(context.getBytes()));
					break;			
				case COMMAND:
					context.readBytes(buffer);
					context.getMessage().setCommand(new String(context.getBytes()));					
					if(log.isDebugEnabled()) log.debug("NG Chunk [COMMAND]:" + context.getMessage().getCommand());
					
					
					// at this point, we can complete the nash request
					// and send it for dispatch. However, since we have to kep processing
					// a possible input stream from the client, we will keep the decoder looping
					// on this request and simply send the NashRequest upstream. 
					if(log.isDebugEnabled()) log.debug("nash Client Complete:\n" + context.getMessage());
					ctx.sendUpstream(new UpstreamMessageEvent(channel, context.getMessage(), channel.getRemoteAddress()));
					checkpoint(ctx, DecodingState.BYTES);
					context.setStdInReady(true);
//					sendStartStdInSignal(ctx, channel);
					
					// If the client has input to send:
					// 		1. It will send a DecodingState.BYTES with the number of bytes to read
					//		2. It will send a DecodingState.TYPE which will be either:
					//			2a.  STDIN the bytes to be read
					//			2b.  STDIN_EOF indicating that the input stream is ended and no further bytes will be sent.
					break;
				case STDIN:
					log.info("STDIN Bytes To Read:" + context.getBytesToRead());
					OutputStream os = context.getMessage().setStdInStream(context.getBytesToRead());
					byte[] stdin = new byte[context.getBytesToRead()-1];					
					buffer.readBytes(stdin);
					buffer.readByte();  buffer.readByte();
					os.write(stdin);
					log.debug("Wrote [" + stdin.length + "] bytes to Message InputStream");
					checkpoint(ctx, DecodingState.TYPE);							
				case STDIN_EOF:
					// this means that the std in has completed
					if(log.isDebugEnabled()) log.debug("STDIN EOF: NG Chunk [" + context.getState() + "]");
					context.getMessage().cancelStdInStream();
					reset(ctx);
					//sendCompletedStdInSignal(ctx, channel);
					return null;
//				case STARTINPUT:
//					channel.write(NashConstants.CHUNKTYPE_STARTINPUT).awaitUninterruptibly();
//					checkpoint(DecodingState.DEBUG);
//					break;
//				case STDIN_EOF:
//					// the request is complete
//					System.out.println(state);
//				case STDIN:
//					// starting to stream input
//					System.out.println(state);
//				case EXIT:
//					// exit code from client
//					System.out.println(state);
//				case STARTINPUT:
//					System.out.println(state);
//				case STDERR:
//					System.out.println(state);
//				case STDOUT:
//					System.out.println(state);
				default:
					
					System.out.println("DEBUG:" + (char)buffer.readByte());
//					while(true) {
//						StringBuilder b = new StringBuilder();
//						try {
//							b.append((char)buffer.readByte());
//							System.out.println(b);
//						} catch (Throwable t) {
//							System.out.println(b);
//							done = true;
//							break;
//						}
//						
//					}
//					done = true;
					break;
//					throw new IllegalStateException("Unhandled Decoding State: [" + state + "]", new Throwable());
			}
		}
	}
	
	/**
CHUNKTYPE_ARGUMENT:65:A
CHUNKTYPE_ENVIRONMENT:69:E
CHUNKTYPE_COMMAND:67:C
CHUNKTYPE_WORKINGDIRECTORY:68:D
CHUNKTYPE_STDIN:48:0
CHUNKTYPE_STDIN_EOF:46:.
CHUNKTYPE_STDOUT:49:1
CHUNKTYPE_STDERR:50:2
CHUNKTYPE_EXIT:88:X
CHUNKTYPE_STARTINPUT:83:S

	 */
	
	/**
	 * Sends a signal back to the nash client indicating that we're ready to accept the input stream
	 * @param ctx The ChannelHandlerContext
	 * @param channel The channel
	 */
	protected void sendStartStdInSignal(ChannelHandlerContext ctx, Channel channel) {
		DownstreamMessageEvent dme = new DownstreamMessageEvent(channel, Channels.future(channel), STREAM_IN_READY, channel.getRemoteAddress());
		ctx.sendDownstream(dme);
	}
	
	/**
	 * Sends a signal back to the nash client indicating that we've completed reading the input stream
	 * @param ctx The ChannelHandlerContext
	 * @param channel The channel
	 */
	protected void sendCompletedStdInSignal(ChannelHandlerContext ctx, Channel channel) {
		DownstreamMessageEvent dme = new DownstreamMessageEvent(channel, Channels.future(channel), STREAM_IN_DONE, channel.getRemoteAddress());
		ctx.sendDownstream(dme);
		channel.close();
	}	
	


}
