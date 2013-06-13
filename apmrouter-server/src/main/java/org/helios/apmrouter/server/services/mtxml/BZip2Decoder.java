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
import java.nio.ByteOrder;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.log4j.Logger;
import org.helios.apmrouter.util.ByteSequenceIndexFinder;
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
 * 
 * <p><b>BZIP2 Format:</b>
 * <pre>
.magic:16                       = 'BZ' signature/magic number
.version:8                      = 'h' for Bzip2 ('H'uffman coding), '0' for Bzip1 (deprecated)
.hundred_k_blocksize:8          = '1'..'9' block-size 100 kB-900 kB (uncompressed)

.compressed_magic:48            = 0x314159265359 (BCD (pi))
.crc:32                         = checksum for this block
.randomised:1                   = 0=>normal, 1=>randomised (deprecated)
.origPtr:24                     = starting pointer into BWT for after untransform
.huffman_used_map:16            = bitmap, of ranges of 16 bytes, present/not present
.huffman_used_bitmaps:0..256    = bitmap, of symbols used, present/not present (multiples of 16)
.huffman_groups:3               = 2..6 number of different Huffman tables in use
.selectors_used:15              = number of times that the Huffman tables are swapped (each 50 bytes)
*.selector_list:1..6            = zero-terminated bit runs (0..62) of MTF'ed Huffman table (*selectors_used)
.start_huffman_length:5         = 0..20 starting bit length for Huffman deltas
*.delta_bit_length:1..40        = 0=>next symbol; 1=>alter length
                                                { 1=>decrement length;  0=>increment length } (*(symbols+2)*groups)
.contents:2..8                  = Huffman encoded data stream until end of block

.eos_magic:48                   = 0x177245385090 (BCD sqrt(pi))     println Double.toHexString(Math.sqrt(Math.PI))  -->   0x1.c5bf891b4ef6ap0
.crc:32                         = checksum for whole stream
.padding:0..7                   = align to whole byte


Logging:

70007 [New I/O  worker #25] INFO org.helios.apmrouter.server.services.mtxml.SanStatsTCPListener  - [id: 0xe86ec240, /0:0:0:0:0:0:0:1:65235 => /0:0:0:0:0:0:0:1:1089] RECEIVED: BigEndianHeapChannelBuffer(ridx=0, widx=18757, cap=18757)
         +-------------------------------------------------+
         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |
+--------+-------------------------------------------------+----------------+
|00004730| 31 34 40 52 11 11 98 6b 16 29 01 09 a5 23 32 63 |14@R...k.)...#2c|
|00004740| 34 34 c2 11 14 68 8a 51 06 4a a2 82 04 98 c8 d8 |44...h.Q.J......|
|00004750| 80 62 22 d8 34 63 32 21 8a 24 a4 58 13 12 94 50 |.b".4c2!.$.X...P|
|00004760| 28 29 0d 62 94 68 d3 18 12 88 cd 14 22 13 11 90 |().b.h......"...|
|00004770| 82 8d 20 51 8c 56 1b 20 94 48 4a 6c ca a2 46 29 |.. Q.V. .HJl..F)|
|00004780| 36 36 23 6d 64 99 88 68 ca 63 58 89 69 09 25 29 |66#md..h.cX.i.%)|
|00004790| 29 91 a2 4c a5 36 4b 0c a4 0c 68 93 30 c2 ca 92 |)..L.6K...h.0...|
|000047a0| a0 b1 a2 b2 1a 29 b1 12 49 b1 4c 4c 59 22 28 12 |.....)..I.LLY"(.|
|000047b0| 91 59 14 93 26 36 69 0c 18 6c 1a 80 a6 1a 09 31 |.Y..&6i..l.....1|
|000047c0| 34 d3 36 32 65 60 44 83 48 c8 29 1b 01 41 a1 a1 |4.62e`D.H.)..A..|
|000047d0| 2c 8d 84 49 34 6c d2 30 4c ca 54 c4 93 05 0c a1 |,..I4l.0L.T.....|
|000047e0| 30 9a 22 36 8c 89 32 49 a0 d3 26 31 49 a8 92 96 |0."6..2I..&1I...|
|000047f0| da d6 f2 17 98 b0 8f 67 a1 7d 42 f9 8f c4 82 30 |.......g.}B....0|
|00004800| c3 da 41 1e 54 11 f7 a0 8e bd 85 54 f9 a8 23 da |..A.T......T..#.|
|00004810| a5 0f 75 04 79 a0 8f 48 90 b8 a9 71 04 71 45 c2 |..u.y..H...q.qE.|
|00004820| 4a d2 aa 3e 64 11 c9 42 97 b4 1f a0 17 4a 08 f0 |J..>d..B.....J..|
|00004830| 88 e0 18 0f f7 0b 01 da f6 2f ee 50 47 95 15 ab |........./.PG...|
|00004840| d3 56 db 52 db 69 18 a0 8c d1 24 88 cc 88 d2 58 |.V.R.i....$....X|
|00004850| 32 21 91 90 29 26 8b 06 80 11 84 c8 44 49 88 b4 |2!..)&......DI..|
|00004860| 98 14 a0 c8 63 1a 28 12 32 86 93 25 03 14 83 33 |....c.(.2..%...3|
|00004870| 22 46 03 19 08 52 34 50 6c 06 d0 21 49 19 24 8a |"F...R4Pl..!I.$.|
|00004880| 33 2c 28 33 19 4b 05 0d 36 25 18 46 41 4c 94 53 |3,(3.K..6%.FAL.S|
|00004890| 0d 24 d1 0c 0c 49 02 6c 64 41 04 c6 8c 94 4b 11 |.$...I.ldA....K.|
|000048a0| 4c 46 81 24 99 a6 51 58 c6 2d 05 48 c0 a2 14 b3 |LF.$..QX.-.H....|
|000048b0| 01 12 8d 98 52 18 cc 65 19 36 48 99 66 6a 34 68 |....R..e.6H.fj4h|
|000048c0| 31 18 c1 93 44 22 c5 8c 24 68 c8 4c c5 0a 44 c4 |1...D"..$h.L..D.|
|000048d0| 12 66 61 88 49 a6 20 84 61 91 48 95 b2 6d bc 8f |.fa.I. .a.H..m..|
============================================================================
|000048e0| cc 7e 27 02 78 1f 41 e8 a0 8e 80 74 74 41 * f0 03 |.~'.x.A....ttA..|
============================================================================
|000048f0| 51 83 4c 1a 60 d3 06 55 5e 81 e6 90 8f bc a0 8e |Q.L.`..U^.......|
|00004900| c2 bf 50 f9 0f 92 82 3f 42 08 f5 1f 55 04 7d 10 |..P....?B...U.}.|
|00004910| 47 d1 04 7a a8 23 94 11 ca 08 d4 11 a8 23 50 46 |G..z.#.......#PF|
|00004920| a0 8d 41 1a 82 3e c0 bb 05 81 81 d5 04 75 41 1f |..A..>.......uA.|
|00004930| 22 aa 7b c2 59 41 1e a5 04 7f f8 bb 92 29 c2 84 |".{.YA.......)..|
|00004940| 86 00 b7 86 d8                                  |.....           |
+--------+-------------------------------------------------+----------------+


3f fc 5b f8 91 b4 ef 6a 
0x1.c5bf891b4ef6ap0


48+32+7 = 87  (5 rows, + 7 bytes

	public static byte[] DecToBCDArray(long num) {
		int digits = 0;
 
		long temp = num;
		while (temp != 0) {
			digits++;
			temp /= 10;
		}
 
		int byteLen = digits % 2 == 0 ? digits / 2 : (digits + 1) / 2;
		boolean isOdd = digits % 2 != 0;
 
		byte bcd[] = new byte[byteLen];
 
		for (int i = 0; i < digits; i++) {
			byte tmp = (byte) (num % 10);
 
			if (i == digits - 1 && isOdd)
				bcd[i / 2] = tmp;
			else if (i % 2 == 0)
				bcd[i / 2] = tmp;
			else {
				byte foo = (byte) (tmp << 4);
				bcd[i / 2] |= foo;
			}
 
			num /= 10;
		}
 
		for (int i = 0; i < byteLen / 2; i++) {
			byte tmp = bcd[i];
			bcd[i] = bcd[byteLen - i - 1];
			bcd[byteLen - i - 1] = tmp;
		}
 
		return bcd;
	}
 
	public static String BCDtoString(byte bcd) {
		StringBuffer sb = new StringBuffer();
		
		byte high = (byte) (bcd & 0xf0);
		high >>>= (byte) 4;	
		high = (byte) (high & 0x0f);
		byte low = (byte) (bcd & 0x0f);
		
		sb.append(high);
		sb.append(low);
		
		return sb.toString();
	}
	
	public static String BCDtoString(byte[] bcd) {
 
	StringBuffer sb = new StringBuffer();
 
	for (int i = 0; i < bcd.length; i++) {
		sb.append(BCDtoString(bcd[i]));
	}
 
	return sb.toString();
	}
	
	
	String.format("%02x", b)

 * </pre></p>
 */

public class BZip2Decoder extends OneToOneDecoder {
	
	/** The unzipping input stream */
	private  volatile BZip2CompressorInputStream bzipStream;
	/** Indicates if the stream is finished */
	private volatile boolean finished;
	/** The swappable input stream */
	private final SwapableBufferInputStream swapIs = new SwapableBufferInputStream();
	/** The decoded content channel buffer */
	protected ChannelBuffer decoded = null;
	
	protected static final byte[] CLOSER = "</sample>".getBytes();
	
	protected final ByteSequenceIndexFinder finder = new ByteSequenceIndexFinder(CLOSER);
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
		final int readableBytes = buff.readableBytes();		
		swapIs.swapBuffer(buff);
		if(bzipStream==null) {
			synchronized(this) {
				if(bzipStream==null) {					
					bzipStream = new BZip2CompressorInputStream(swapIs, true);
					decoded = ChannelBuffers.dynamicBuffer(readableBytes*10, chanelBufferFactory);
				}
			}
		} 
		
		
		log.info("Reading [" + readableBytes + "] compressed bytes through BZIP2 input stream");
		byte[] tbuff = new byte[9126];
		int readBytes = 0, totalReadBytes = 0;
		while(buff.readableBytes()>0) {
			int b = bzipStream.read();
			decoded.writeByte(b);
			readBytes++;
			if(b==-1) {
				finished = true;
			}
		}
		log.info("Read [" + readBytes + "], Finished:" + finished );		
		swapIs.close();
		log.info("Decoded Channel Buffer: [" + decoded + "]");
		
//		byte[] decodedBytes = new byte[decoded.readableBytes()];
//		decoded.readBytes(decodedBytes);
//		decoded.resetReaderIndex();
//		log.info("Decoded So Far:\n" + new String(decodedBytes));
		if(finished) {
			log.info("Detected EOF in BZIP2 input stream");
			bzipStream.close();
			return decoded;
		}
		return null;
	}

}
