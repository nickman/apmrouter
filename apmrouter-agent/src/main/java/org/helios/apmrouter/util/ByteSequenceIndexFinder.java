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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferIndexFinder;

/**
 * <p>Title: ByteSequenceIndexFinder</p>
 * <p>Description: A {@link ChannelBufferIndexFinder} implementation for finding a sequence of bytes within a {@link ChannelBuffer}</p>
 * <p>This class is <b><i>not</i></b> thread-safe and use by multiple threads will almost certainly cause it to explode.</p> 
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.util.ByteSequenceIndexFinder</code></p>
 */
public class ByteSequenceIndexFinder implements ChannelBufferIndexFinder {
	/** The sequence of bytes to find */
	private final byte[] sequence;
	/** The length of the sequence */
	private final int slength;
	/** The position within the sequence that has been matched up to */
	private int pos = 0;
	/** The adjusted result of the last find */
	private int lastResult = -1;
	
	
	/**
	 * Creates a new ByteSequenceIndexFinder
	 * @param sequence The sequence we're searching for
	 */
	public ByteSequenceIndexFinder(byte[] sequence) {
		if(sequence==null || sequence.length==0) throw new IllegalArgumentException("The passed byte sequence was null or zero-length", new Throwable());
		this.sequence = sequence;
		slength = this.sequence.length;
	}


	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.buffer.ChannelBufferIndexFinder#find(org.jboss.netty.buffer.ChannelBuffer, int)
	 */
	@Override
	public boolean find(ChannelBuffer buffer, int guessedIndex) {
		byte nextByte = buffer.getByte(guessedIndex);
		boolean found = nextByte==sequence[pos];
		if(found) pos++;
		else {
			pos = 0;
			if(nextByte==sequence[pos]) pos++;
		}
		if(pos==slength) {
			pos = 0;
			lastResult = guessedIndex-slength+1;
			return true;
		}
		return false;
	}

	
	/**
	 * Locates the first starting offset in the passed {@link ChannelBuffer} 
	 * where the specified byte sequence was located.
	 * The search starts the specified {@code index} (inclusive) 
	 * and lasts for the specified {@code length}.
	 * This method does not modify the {@code readerIndex} or {@code writerIndex} 
	 * of the passed channelBuffer.
	 * The finder is {@link #reset()} once this method is complete.
	 * 
	 * @param index The offset where the search starts
	 * @param length The maximum number of bytes to search
	 * @param channelBuffer The buffer to search
     * @return the number of bytes between the specified {@code index}
     *         and the first starting offset where the {@code indexFinder} returned
     *         {@code true}.  {@code -1} if the {@code indexFinder} did not
     *         return {@code true} at all.
	 */
	public int findIn(int index, int length, ChannelBuffer channelBuffer) {
		checkReadableBytes(channelBuffer, length);
		try {
			int result = channelBuffer.bytesBefore(index, length, this);					
			return result==-1 ? -1 : result-slength+1;
		} finally {
			reset();
		}
	}
	

	
	/**
	 * Locates the first starting offset in the passed {@link ChannelBuffer} 
	 * where the specified byte sequence was located.
	 * The search starts the current {@link ChannelBuffer#readerIndex()} (inclusive) 
	 * and lasts for the specified {@code length}.
	 * This method does not modify the {@code readerIndex} or {@code writerIndex} 
	 * of the passed channelBuffer.
	 * The finder is {@link #reset()} once this method is complete.
	 * 
	 * @param length The maximum number of bytes to search
	 * @param channelBuffer The buffer to search
     * @return the number of bytes between the specified {@code index}
     *         and the first starting offset where the {@code indexFinder} returned
     *         {@code true}.  {@code -1} if the {@code indexFinder} did not
     *         return {@code true} at all.
	 */
	public int findIn(int length, ChannelBuffer channelBuffer) {		
		return findIn(channelBuffer.readerIndex(), length, channelBuffer);
	}
	
	/**
	 * Locates the first starting offset in the passed {@link ChannelBuffer} 
	 * where the specified byte sequence was located.
	 * The search starts the current {@link ChannelBuffer#readerIndex()} (inclusive) 
	 * to the current {@link ChannelBuffer#writerIndex()}.
	 * 
	 * This method does not modify the {@code readerIndex} or {@code writerIndex} 
	 * of the passed channelBuffer.
	 * The finder is {@link #reset()} once this method is complete.
	 * 
	 * @param channelBuffer The buffer to search
     * @return the number of bytes between the specified {@code index}
     *         and the first starting offset where the {@code indexFinder} returned
     *         {@code true}.  {@code -1} if the {@code indexFinder} did not
     *         return {@code true} at all.
	 */
	public int findIn(ChannelBuffer channelBuffer) {
		return findIn(channelBuffer.readerIndex(), channelBuffer.readableBytes(), channelBuffer);
	}
	
	/**
	 * Locates the first starting offset in the passed {@link ChannelBuffer} 
	 * where the specified byte sequence was located.
	 * The search starts the specified {@code index} (inclusive) 
	 * This method does not modify the {@code readerIndex} or {@code writerIndex} 
	 * of the passed channelBuffer.
	 * The finder is {@link #reset()} once this method is complete.
	 * 
	 * @param index The offset where the search starts
	 * @param channelBuffer The buffer to search
     * @return the offset from the beginning of the buffer's readable bytes and the first byte of the byte sequence
	 */
	
	public int findIn(ChannelBuffer channelBuffer, int index) {
		int idx = findIn(index, channelBuffer.capacity()-index, channelBuffer);
		if(idx==-1) return idx;
		return idx+index;
	}
	

	
	 
    /**
     * Throws an {@link IndexOutOfBoundsException} if the current
     * {@linkplain ChannelBuffer#readableBytes() readable bytes} of this buffer is less
     * than the specified value.
     * @param channelBuffer The channel buffer to test
     * @param minimumReadableBytes 
     */
    protected void checkReadableBytes(ChannelBuffer channelBuffer, int minimumReadableBytes) {
    	if(channelBuffer==null) throw new IllegalArgumentException("The passed channel buffer was null");
        if (channelBuffer.readableBytes() < minimumReadableBytes) {
            throw new IndexOutOfBoundsException("Not enough readable bytes - Need "
                    + minimumReadableBytes + ", maximum is " + channelBuffer.readableBytes());
        }
    }
	
	/**
	 * Resets this finder.
	 */
	public void reset() {
		pos = 0;
		lastResult = -1;
	}
	
	/**
	 * Returns the sequence length adjusted result of the most recent {@link ChannelBuffer#bytesBefore(ChannelBufferIndexFinder)} invocation.
	 * If a find has not been executed, this will be <code>-1</code>. Calling {@link #reset()} resets the last result to <code>-1</code>. 
	 * @return the result of the most recent find.
	 */
	public int getLastResult() {
		return lastResult;
	}
	
}