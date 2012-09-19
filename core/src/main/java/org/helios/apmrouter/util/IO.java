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
package org.helios.apmrouter.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.snmp4j.PDU;
import org.snmp4j.asn1.BERInputStream;
import org.snmp4j.asn1.BEROutputStream;

/**
 * <p>Title: IO</p>
 * <p>Description: IO utility methods</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.util.IO</code></p>
 */

public class IO {
	/**
	 * Reads an object from a byte buffer
	 * @param buff The byte buffer to read from
	 * @return The read object, or null if the buffer was null or zero size
	 */
	public static Object readFromByteBuffer(final ByteBuffer buff) {
		if(buff==null) return null;
		if(buff.capacity()<1) return null;
		ObjectInputStream ois = null;
		InputStream channelInputStream = null;
		GZIPInputStream zipInputStream = null;
		buff.rewind();
		try {
			if(buff.hasArray()) {
				channelInputStream = new ByteArrayInputStream(buff.array());
			} else {
				channelInputStream = Channels.newInputStream(new ReadableByteChannel(){
					protected boolean open = true;
					@Override
					public boolean isOpen() {
						return open;
					}

					@Override
					public void close() throws IOException {
						open = false;
					}

					@Override
					public int read(ByteBuffer dst) throws IOException {
						
						try {
							if(buff.remaining()<1) return -1;
							int bytes = 0;
							while(buff.remaining()>0 && dst.position()<dst.limit()) {
								dst.put(								
										buff.get()
								);
								bytes++;
							}					
							return bytes;
						} catch (Exception e) {
							e.printStackTrace(System.err);
							throw new IOException(e);
						}
					}
					
				});
			}
			byte c = buff.get();			
			boolean compressed = c==1;
			if(compressed) {
				zipInputStream = new GZIPInputStream(channelInputStream);
				ois = new ObjectInputStream(zipInputStream);
			} else {
				ois = new ObjectInputStream(channelInputStream);
			}
			return ois.readObject();
		} catch (Exception e) {
			throw new RuntimeException("Failed to Deserialize from ByteBuffer", e);
		} finally {
			try { ois.close(); } catch (Exception ex) {};
			if(zipInputStream!=null) try { zipInputStream.close(); } catch (Exception ex) {};
		}
	}
	
	private static String logb(ByteBuffer b) {
		StringBuilder sb = new StringBuilder(" ByteBuffer:");
		sb.append("\n\tCapacity:").append(b.capacity());
		sb.append("\n\tPosition:").append(b.position());
		sb.append("\n\tLimit:").append(b.limit());
		return sb.toString();
	}
	
	/**
	 * Serializes an object to a byte buffer using a default buffer size of 8192
	 * @param value The object to serialize
	 * @param direct true to return a direct buffer, false for a heap buffer
	 * @param compress Indicates if the byte array should be compressed
	 * @return The buffer containing the serialized object which may be empty if the value was null
	 */
	public static ByteBuffer writeToByteBuffer(Object value, boolean direct, boolean compress) {
		return writeToByteBuffer(value, direct, 8192, compress);
	}
	
	
	/**
	 * Returns an input stream for the passed ByteBuffer
	 * @param buff the ByteBuffer to read from
	 * @return an InputStream that reads from the passed ByteBuffer
	 * FIXME: Needs to support testing the compression byte and adding a decompressor if required.
	 */
	public static InputStream read(final ByteBuffer buff) {
		return 	Channels.newInputStream(new ReadableByteChannel(){
			protected boolean open = true;
			@Override
			public boolean isOpen() {
				return open;
			}

			@Override
			public void close() throws IOException {
				open = false;
			}

			@Override
			public int read(ByteBuffer dst) throws IOException {
				
				try {
					if(buff.remaining()<1) return -1;
					int bytes = 0;
					while(buff.remaining()>0 && dst.position()<dst.limit()) {
						dst.put(								
								buff.get()
						);
						bytes++;
					}					
					return bytes;
				} catch (Exception e) {
					e.printStackTrace(System.err);
					throw new IOException(e);
				}
			}			
		});
	}
	
	/**
	 * Serializes an object to a byte buffer
	 * @param value The object to serialize
	 * @param direct true to return a direct buffer, false for a heap buffer
	 * @param bufferSize The incremental size of the buffers to create while streaming
	 * @param compress Indicates if the byte array should be compressed 
	 * @return The buffer containing the serialized object which may be empty if the value was null
	 */
	public static ByteBuffer writeToByteBufferX(final Object value, final boolean direct, final int bufferSize, final boolean compress) {
		if(value==null) return direct ? ByteBuffer.allocateDirect(0) : ByteBuffer.allocate(0);
		ObjectOutputStream ois = null;
		OutputStream channelOutputStream = null;
		GZIPOutputStream zipOutputStream = null;
		final AtomicLong cntr = new AtomicLong();
		final ByteBuffer[] buff = new ByteBuffer[]{direct ? ByteBuffer.allocateDirect(bufferSize) : ByteBuffer.allocate(bufferSize)}; 
		try {
			channelOutputStream = Channels.newOutputStream(new WritableByteChannel() {
				protected boolean open = true;				
				@Override
				public boolean isOpen() {
					return open;
				}
				
				@Override
				public void close() throws IOException {
					open = false;
				}
				
				@Override
				public int write(ByteBuffer src) throws IOException {
					int bytesRead = 0;
					while(src.remaining()>0) {
						if(buff[0].position()==buff[0].limit()-1) {
							ByteBuffer newBuff = direct ? ByteBuffer.allocateDirect(buff[0].capacity()+bufferSize) : ByteBuffer.allocate(buff[0].capacity()+bufferSize);
							newBuff.put(buff[0]);
							buff[0] = newBuff;
						}
						buff[0].put(src.get());						
						bytesRead++;
					}				
					cntr.addAndGet(bytesRead);
					return bytesRead;
				}
			});
			buff[0].position(0);
			if(compress) {
				buff[0].put((byte)1);
				zipOutputStream = new GZIPOutputStream(channelOutputStream);
				ois = new ObjectOutputStream(zipOutputStream);				
			} else {
				buff[0].put((byte)0);
				ois = new ObjectOutputStream(channelOutputStream);											
			}
			
			
			ois.writeObject(value);
			ois.flush();
			if(compress) zipOutputStream.finish();
			
			//log("Total Bytes:" + cntr.get());
			buff[0].flip();			
			return buff[0];
		} catch (Throwable e) {
			e.printStackTrace(System.err);
			throw new RuntimeException("Failed to write instance of [" + value.getClass().getName() + "]", e);
		} finally {
			if(ois!=null) try { ois.close(); } catch (Exception ex) {}
			if(zipOutputStream!=null) try { zipOutputStream.close(); } catch (Exception ex) {}
		}		
	}
	
	/**
	 * Serializes an object to a byte buffer
	 * @param value The object to serialize
	 * @param direct true to return a direct buffer, false for a heap buffer
	 * @param bufferSize The incremental size of the buffers to create while streaming
	 * @param compress Indicates if the byte array should be compressed 
	 * @return The buffer containing the serialized object which may be empty if the value was null
	 */
	public static ByteBuffer writeToByteBuffer(final Object value, final boolean direct, final int bufferSize, final boolean compress) {
		if(value==null) return direct ? ByteBuffer.allocateDirect(0) : ByteBuffer.allocate(0);
		ObjectOutputStream ois = null;
		ByteArrayOutputStream channelOutputStream = new ByteArrayOutputStream(bufferSize);
		GZIPOutputStream zipOutputStream = null;
		try {
			if(compress) {						
				zipOutputStream = new GZIPOutputStream(channelOutputStream);
				ois = new ObjectOutputStream(zipOutputStream);
				channelOutputStream.write(1);
			} else {			
				ois = new ObjectOutputStream(channelOutputStream);
				channelOutputStream.write(0);
			}
			ois.writeObject(value);
			ois.flush();
			if(compress) zipOutputStream.finish();
			channelOutputStream.flush();
			ByteBuffer bb = null;
			byte[] bytes = channelOutputStream.toByteArray();
			if(direct) {
				bb = ByteBuffer.allocateDirect(bytes.length);
				bb.put(bytes);
			} else {
				bb = ByteBuffer.wrap(bytes);
			}
			bb.flip();
			return bb;
		} catch (Throwable e) {
			e.printStackTrace(System.err);
			throw new RuntimeException("Failed to write instance of [" + value.getClass().getName() + "]", e);
		} finally {
			if(ois!=null) try { ois.close(); } catch (Exception ex) {}
			if(zipOutputStream!=null) try { zipOutputStream.close(); } catch (Exception ex) {}
		}		
	}
	
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	
	/**
	 * Writes out an SNMP PDU to a byte buffer
	 * @param pdu The PDU to write
	 * @param direct true for a direct buffer, false for a heap buffer
	 * @return the ByteBuffer the PDU was written to
	 */
	public static ByteBuffer writePDUToByteBuffer(PDU pdu, boolean direct)  {
		try {
			ByteBuffer bb = direct ? ByteBuffer.allocateDirect(pdu.getBERLength()) : ByteBuffer.allocate(pdu.getBERLength());
			BEROutputStream bos = new BEROutputStream(bb);
			pdu.encodeBER(bos);
			bos.flush(); 
			return bb;
		} catch (Exception e) {
			e.printStackTrace(System.err);
			throw new RuntimeException("Failed to write PDU", e);			
		}
	}
	
	/**
	 * Reads an SNMP PDU from the passed ByteBuffer
	 * @param bb The ByteBuffer to read the PDU from
	 * @return The read PDU
	 */
	public static PDU readPDUFromByteBuffer(ByteBuffer bb) {
		try {
			PDU pdu = new PDU();
			BERInputStream bis = new BERInputStream(bb);
			pdu.decodeBER(bis);
			return pdu;
		} catch (Exception e) {
			e.printStackTrace(System.err);
			throw new RuntimeException("Failed to read PDU", e);			
		}		
	}
	
	
	
}
