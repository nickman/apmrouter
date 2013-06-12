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
package org.helios.apmrouter.server.services.mtxml;

import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteOrder;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferFactory;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.buffer.DirectChannelBufferFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
/**
 * <p>Title: BZip2Decoder</p>
 * <p>Description: Decompresses a {@link ChannelBuffer} using the bzip2 algorithm.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.services.mtxml.BZip2Decoder</code></p>
 */

public class BZip2Decoder extends OneToOneDecoder {
	
	/** The unzipping input stream */
	private  BZip2CompressorInputStream bzipStream;
	/** Indicates if the stream is finished */
	private volatile boolean finished;
	
	/** The channel buffer factory */
	protected static final ChannelBufferFactory chanelBufferFactory = new DirectChannelBufferFactory(ByteOrder.nativeOrder(), 1500000);

	/** Static class logger */
	protected static final Logger log = Logger.getLogger(BZip2Decoder.class);
	
	/**
	 * Creates a new BZip2Decoder
	 */
	public BZip2Decoder() {
	}




	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.handler.codec.oneone.OneToOneDecoder#decode(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.Channel, java.lang.Object)
	 */
	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
		if (!(msg instanceof ChannelBuffer) || finished) {
			return msg;
		}
		ChannelBuffer buff = (ChannelBuffer)msg;
		InputStream inStream = new ChannelBufferInputStream(buff, buff.readableBytes());
		ChannelBuffer decompressed = ChannelBuffers.dynamicBuffer(5 * buff.readableBytes(), chanelBufferFactory);
		bzipStream = new BZip2CompressorInputStream(inStream, true);
		byte[] bytes = new byte[9016];
		int bytesRead = -1;
		while((bytesRead=bzipStream.read(bytes))!=-1) {
			decompressed.writeBytes(bytes, 0, bytesRead);
		}
		bzipStream.close();
		finished = true;
		log.info("Decompression Complete --> [" + decompressed.readableBytes() + "] bytes" );
		return decompressed;
	}

}
