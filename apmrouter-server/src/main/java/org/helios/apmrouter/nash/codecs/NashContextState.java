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

import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.helios.apmrouter.nash.DefaultNashRequestImpl;
import org.jboss.netty.buffer.ChannelBuffer;

/**
 * <p>
 * Title: NashContextState
 * </p>
 * <p>
 * Description: A class whose instances retain the stateful context of a nash
 * request that is in flight. These instances are attached to the
 * ChannelHandlerContext for the duration of the conversation.
 * </p>
 * <p>
 * Company: Helios Development Group LLC
 * </p>
 * 
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 *         <p>
 *         <code>org.helios.apmrouter.nash.codecs.NashContextState</code>
 *         </p>
 */

public class NashContextState {
	/** The request created */
	private DefaultNashRequestImpl message;
	/** byte array buffer */
	private byte[] bytes = null;
	/** The type decoding state */
	private DecodingState state = null;
	/** The current chunk type */
	private byte chunkType;
	/** The number of bytes to read in the next event */
	private int bytesToRead;
	
	/** Indicates if the request decoder is ready to stream STDIN */
	private boolean stdInReady = false;

	/** The piped output stream to stream the nash client's input stream */
	private PipedOutputStream pipeOut = null;
	/** The piped input stream feeding the out pipe*/
	private PipedInputStream pipeIn = null;
	
	
	

	/**
	 * Creates a new NashContextState
	 */
	NashContextState() {
		cleanup();
	}
	
	/**
	 * Indicates if the request decoder is ready to stream STDIN 
	 * @return true if the request decoder is ready to stream STDIN 
	 */
	public boolean isStdInReady() {
		return stdInReady;
	}



	/**
	 * Sets the STDIN readiness flaf
	 * @param stdInReady true if the request decoder is ready to stream STDIN, false otherwise 
	 */
	public void setStdInReady(boolean stdInReady) {
		this.stdInReady = stdInReady;
	}
	

	/**
	 * Cleans up any remaining state
	 */
	protected void cleanup() {
		bytes = null;
		state = null;
		chunkType = -10;
		bytesToRead = -1;
		try { pipeOut.close(); } catch (Exception e) {}
		pipeOut = null;
		message = new DefaultNashRequestImpl();
	}

	/**
	 * Returns
	 * 
	 * @return the message
	 */
	DefaultNashRequestImpl getMessage() {
		return message;
	}

	/**
	 * Sets
	 * 
	 * @param message
	 *            the message to set
	 */
	void setMessage(DefaultNashRequestImpl message) {
		this.message = message;
	}

	/**
	 * Returns
	 * 
	 * @return the bytes
	 */
	byte[] getBytes() {
		return bytes;
	}

	/**
	 * Sets
	 * 
	 * @param bytes
	 *            the bytes to set
	 */
	void setBytes(byte[] bytes) {
		this.bytes = bytes;
	}

	/**
	 * Returns
	 * 
	 * @return the state
	 */
	DecodingState getState() {
		return state;
	}

	/**
	 * Sets
	 * 
	 * @param state
	 *            the state to set
	 */
	void setState(DecodingState state) {
		// if(state==null) throw new
		// IllegalArgumentException("The passed DecodingState was null", new
		// Throwable());
		this.state = state;
	}

	/**
	 * Returns
	 * 
	 * @return the chunkType
	 */
	byte getChunkType() {
		return chunkType;
	}

	/**
	 * Sets the chunk type. If the chunk type is validated, the corresponding
	 * DecodingState is set as well. If the chunk type is not validated, an
	 * IllegalStateException is thrown.
	 * 
	 * @param chunkType
	 *            the chunkType to set
	 */
	void setChunkType(byte chunkType) {
		DecodingState ds = DecodingState.getState(chunkType);
		if (ds == null) {
			throw new IllegalStateException("Invalid ChunkType ["
					+ (char) chunkType + "]", new Throwable());
		}
		this.chunkType = chunkType;
		this.state = ds;
	}

	/**
	 * Creates a new byte array sized by the current value of
	 * <code>bytesToRead</code> and fills the array from the passed buffer
	 * 
	 * @param buffer
	 *            The channel buffer to read from
	 */
	void readBytes(ChannelBuffer buffer) {
		bytes = new byte[bytesToRead];
		buffer.readBytes(bytes);
	}
	

	/**
	 * Returns
	 * 
	 * @return the bytesToRead
	 */
	int getBytesToRead() {
		return bytesToRead;
	}

	/**
	 * Sets
	 * 
	 * @param bytesToRead
	 *            the bytesToRead to set
	 */
	void setBytesToRead(int bytesToRead) {
		this.bytesToRead = bytesToRead;
	}

	/**
	 * Constructs a <code>String</code> with all attributes in name = value
	 * format.
	 * 
	 * @return a <code>String</code> representation of this object.
	 */
	public String toString() {
		final String TAB = "\n\t";
		StringBuilder retValue = new StringBuilder("NashContextState [")
				.append(TAB).append("message:").append(this.message)
				.append(TAB).append("bytes:").append(this.bytes).append(TAB)
				.append("state:").append(this.state).append(TAB)
				.append("chunkType:").append(this.chunkType).append(TAB)
				.append("bytesToRead:").append(this.bytesToRead).append("\n]");
		return retValue.toString();
	}




}
