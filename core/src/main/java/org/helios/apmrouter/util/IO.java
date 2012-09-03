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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

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
		try {
			
			ois = buff.hasArray() ? new ObjectInputStream(new ByteArrayInputStream(buff.array())) : new ObjectInputStream(Channels.newInputStream(new ReadableByteChannel(){
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
				
			}));
			return ois.readObject();
		} catch (Exception e) {
			throw new RuntimeException("Failed to Deserialize from ByteBuffer", e);
		} finally {
			try { ois.close(); } catch (Exception ex) {};
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
	 * @return The buffer containing the serialized object which may be empty if the value was null
	 */
	public static ByteBuffer writeToByteBuffer(Object value, boolean direct) {
		return writeToByteBuffer(value, direct, 8192);
	}
	
	
	/**
	 * Serializes an object to a byte buffer
	 * @param value The object to serialize
	 * @param direct true to return a direct buffer, false for a heap buffer
	 * @param bufferSize The incremental size of the buffers to create while streaming 
	 * @return The buffer containing the serialized object which may be empty if the value was null
	 */
	public static ByteBuffer writeToByteBuffer(final Object value, final boolean direct, final int bufferSize) {
		if(value==null) return direct ? ByteBuffer.allocateDirect(0) : ByteBuffer.allocate(0);
		ObjectOutputStream ois = null;
		final AtomicLong cntr = new AtomicLong();
		final ByteBuffer[] buff = new ByteBuffer[]{direct ? ByteBuffer.allocateDirect(bufferSize) : ByteBuffer.allocate(bufferSize)}; 
		try {
			ois = new ObjectOutputStream(Channels.newOutputStream(new WritableByteChannel() {
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
			}));
			ois.writeObject(value);
			ois.flush();
			log("Total Bytes:" + cntr.get());
			buff[0].flip();			
			return buff[0];
		} catch (Exception e) {
			throw new RuntimeException("Failed to write instance of [" + value.getClass().getName() + "]", e);
		} finally {
			if(ois!=null) try { ois.close(); } catch (Exception ex) {}
		}		
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	public static void main(String[] args) {
		try {
			log("ByteBuffer OIS Test");
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(System.getProperties());
			oos.flush();
			baos.flush();
			byte[] bytes = baos.toByteArray();
			log("Bytes:" + bytes.length + " Props Size:" + System.getProperties().size());
			
			ByteBuffer bb = writeToByteBuffer(System.getProperties(), true);
			//ByteBuffer bb = ByteBuffer.wrap(bytes);
			//ByteBuffer bb = ByteBuffer.allocateDirect(bytes.length).put(bytes);
			
			log("Write Complete:" + logb(bb));
			
			Properties p = (Properties)readFromByteBuffer(bb);
			log("Read Complete:" + p.size());
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		
	}
}
