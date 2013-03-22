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

import org.jboss.netty.buffer.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
/**
 * <p>Title: ReadableWritableByteChannelBuffer</p>
 * <p>Description: Wrapper class for a Netty ChannelBuffer to provide the functions of nio {@link ReadableByteChannel}s and {@link WritableByteChannel}s </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.util.ReadableWritableByteChannelBuffer</code></p>
 */

public class ReadableWritableByteChannelBuffer implements ChannelBuffer, ReadableByteChannel,WritableByteChannel {
	/** The Netty ChannelBuffer delegate */
	protected final ChannelBuffer buffer;
	
	/**
	 * Creates a new ReadableWritableByteChannelBuffer using a direct allocated channel buffer
	 * @param order The byte order of the channel buffer 
	 * @param capacity The capacity of the created channel buffer
	 * @return a new ReadableWritableByteChannelBuffer using a direct allocated channel buffer
	 */
	public static ReadableWritableByteChannelBuffer newDirect(ByteOrder order, int capacity) {
		return new ReadableWritableByteChannelBuffer(ChannelBuffers.directBuffer(order, capacity));
	}
	
	/**
	 * Creates a new ReadableWritableByteChannelBuffer using a direct allocated channel buffer using the native platform byte order
	 * @param capacity The capacity of the created channel buffer
	 * @return a new ReadableWritableByteChannelBuffer using a direct allocated channel buffer
	 */
	public static ReadableWritableByteChannelBuffer newDirect(int capacity) {
		return newDirect(ByteOrder.nativeOrder(), capacity);
	}
	
	/**
	 * Creates a new ReadableWritableByteChannelBuffer using a heap allocated channel buffer
	 * @param order The byte order of the channel buffer 
	 * @param capacity The capacity of the created channel buffer
	 * @return a new ReadableWritableByteChannelBuffer using a heap allocated channel buffer
	 */
	public static ReadableWritableByteChannelBuffer newHeap(ByteOrder order, int capacity) {
		return new ReadableWritableByteChannelBuffer(ChannelBuffers.buffer(order, capacity));
	}
	
	/**
	 * Creates a new ReadableWritableByteChannelBuffer using a heap allocated channel buffer using the native platform byte order
	 * @param capacity The capacity of the created channel buffer
	 * @return a new ReadableWritableByteChannelBuffer using a heap allocated channel buffer
	 */
	public static ReadableWritableByteChannelBuffer newHeap(int capacity) {
		return newHeap(ByteOrder.nativeOrder(), capacity);
	}
	
	/**
	 * Creates a new ReadableWritableByteChannelBuffer using a heap dynamic channel buffer
	 * @param order The byte order of the channel buffer 
	 * @param capacity The capacity of the created channel buffer
	 * @return a new ReadableWritableByteChannelBuffer using a heap dynamic channel buffer
	 */
	public static ReadableWritableByteChannelBuffer newDynamic(ByteOrder order, int capacity) {
		return new ReadableWritableByteChannelBuffer(ChannelBuffers.dynamicBuffer(order, capacity));
	}
	
	/**
	 * Creates a new ReadableWritableByteChannelBuffer using a dynamic channel buffer using the native platform byte order
	 * @param capacity The capacity of the created channel buffer
	 * @return a new ReadableWritableByteChannelBuffer using a dynamic channel buffer
	 */
	public static ReadableWritableByteChannelBuffer newDynamic(int capacity) {
		return newDynamic(ByteOrder.nativeOrder(), capacity);
	}
	
	/**
	 * Creates a new ReadableWritableByteChannelBuffer using a dynamic channel buffer
	 * @param order The byte order of the channel buffer 
	 * @param capacity The capacity of the created channel buffer
	 * @return a new ReadableWritableByteChannelBuffer using a dynamic channel buffer
	 */
	public static ReadableWritableByteChannelBuffer newDirectDynamic(ByteOrder order, int capacity) {
		return new ReadableWritableByteChannelBuffer(ChannelBuffers.dynamicBuffer(order, capacity, DirectChannelBufferFactory.getInstance(order)));
	}
	
	/**
	 * Creates a new ReadableWritableByteChannelBuffer using a direct dynamic channel buffer using the native platform byte order
	 * @param capacity The capacity of the created channel buffer
	 * @return a new ReadableWritableByteChannelBuffer using a direct dynamic channel buffer
	 */
	public static ReadableWritableByteChannelBuffer newDirectDynamic(int capacity) {
		return newDirectDynamic(ByteOrder.nativeOrder(), capacity);
	}

	
	/**
	 * Creates a new ReadableWritableByteChannelBuffer
	 * @param buffer The Netty ChannelBuffer delegate
	 */
	public ReadableWritableByteChannelBuffer(ChannelBuffer buffer) {
		this.buffer = buffer;
	}
	
	/**
	 * Creates a copy of this ReadableWritableByteChannelBuffer
	 * Modifying the content of the returned buffer or this buffer does not affect each other at all.
	 * @return a copied ReadableWritableByteChannelBuffer.
	 */
	public ReadableWritableByteChannelBuffer copyBuffer() {
		return new ReadableWritableByteChannelBuffer(ChannelBuffers.copiedBuffer(buffer));
	}
	
	/**
	 * Resets the buffer's writer and reader index
	 */
	public void reset() {
		buffer.resetWriterIndex();
		buffer.resetReaderIndex();
	}
	
	
	/**
	 * Returns the wrapped Netty channel buffer
	 * @return the wrapped Netty channel buffer
	 */
	public ChannelBuffer getChannelBuffer() {
		return buffer;
	}

	/**
	 * No Op
	 * {@inheritDoc}
	 * @see java.nio.channels.Channel#isOpen()
	 */
	@Override
	public boolean isOpen() {
		return true;
	}

	/**
	 * No Op
	 * {@inheritDoc}
	 * @see java.nio.channels.Channel#close()
	 */
	@Override
	public void close() throws IOException {
	}

	/**
	 * {@inheritDoc}
	 * @see java.nio.channels.WritableByteChannel#write(java.nio.ByteBuffer)
	 */
	@Override
	public int write(ByteBuffer src) throws IOException {
		int pre = buffer.writerIndex();
		buffer.writeBytes(src);
		return buffer.writerIndex()-pre;
	}

	/**
	 * {@inheritDoc}
	 * @see java.nio.channels.ReadableByteChannel#read(java.nio.ByteBuffer)
	 */
	@Override
	public int read(ByteBuffer dst) throws IOException {
		int pre = buffer.readerIndex();
		buffer.readBytes(dst);
		return buffer.readerIndex()-pre;
	}

	/**
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#factory()
	 */
	public ChannelBufferFactory factory() {
		return buffer.factory();
	}

	/**
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#capacity()
	 */
	public int capacity() {
		return buffer.capacity();
	}

	/**
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#order()
	 */
	public ByteOrder order() {
		return buffer.order();
	}

	/**
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#isDirect()
	 */
	public boolean isDirect() {
		return buffer.isDirect();
	}

	/**
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#readerIndex()
	 */
	public int readerIndex() {
		return buffer.readerIndex();
	}

	/**
	 * @param readerIndex
	 * @see org.jboss.netty.buffer.ChannelBuffer#readerIndex(int)
	 */
	public void readerIndex(int readerIndex) {
		buffer.readerIndex(readerIndex);
	}

	/**
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#writerIndex()
	 */
	public int writerIndex() {
		return buffer.writerIndex();
	}

	/**
	 * @param writerIndex
	 * @see org.jboss.netty.buffer.ChannelBuffer#writerIndex(int)
	 */
	public void writerIndex(int writerIndex) {
		buffer.writerIndex(writerIndex);
	}

	/**
	 * @param readerIndex
	 * @param writerIndex
	 * @see org.jboss.netty.buffer.ChannelBuffer#setIndex(int, int)
	 */
	public void setIndex(int readerIndex, int writerIndex) {
		buffer.setIndex(readerIndex, writerIndex);
	}

	/**
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#readableBytes()
	 */
	public int readableBytes() {
		return buffer.readableBytes();
	}

	/**
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#writableBytes()
	 */
	public int writableBytes() {
		return buffer.writableBytes();
	}

	/**
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#readable()
	 */
	public boolean readable() {
		return buffer.readable();
	}

	/**
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#writable()
	 */
	public boolean writable() {
		return buffer.writable();
	}

	/**
	 * 
	 * @see org.jboss.netty.buffer.ChannelBuffer#clear()
	 */
	public void clear() {
		buffer.clear();
	}

	/**
	 * 
	 * @see org.jboss.netty.buffer.ChannelBuffer#markReaderIndex()
	 */
	public void markReaderIndex() {
		buffer.markReaderIndex();
	}

	/**
	 * 
	 * @see org.jboss.netty.buffer.ChannelBuffer#resetReaderIndex()
	 */
	public void resetReaderIndex() {
		buffer.resetReaderIndex();
	}

	/**
	 * 
	 * @see org.jboss.netty.buffer.ChannelBuffer#markWriterIndex()
	 */
	public void markWriterIndex() {
		buffer.markWriterIndex();
	}

	/**
	 * 
	 * @see org.jboss.netty.buffer.ChannelBuffer#resetWriterIndex()
	 */
	public void resetWriterIndex() {
		buffer.resetWriterIndex();
	}

	/**
	 * 
	 * @see org.jboss.netty.buffer.ChannelBuffer#discardReadBytes()
	 */
	public void discardReadBytes() {
		buffer.discardReadBytes();
	}

	/**
	 * @param writableBytes
	 * @see org.jboss.netty.buffer.ChannelBuffer#ensureWritableBytes(int)
	 */
	public void ensureWritableBytes(int writableBytes) {
		buffer.ensureWritableBytes(writableBytes);
	}

	/**
	 * @param index
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#getByte(int)
	 */
	public byte getByte(int index) {
		return buffer.getByte(index);
	}

	/**
	 * @param index
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#getUnsignedByte(int)
	 */
	public short getUnsignedByte(int index) {
		return buffer.getUnsignedByte(index);
	}

	/**
	 * @param index
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#getShort(int)
	 */
	public short getShort(int index) {
		return buffer.getShort(index);
	}

	/**
	 * @param index
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#getUnsignedShort(int)
	 */
	public int getUnsignedShort(int index) {
		return buffer.getUnsignedShort(index);
	}

	/**
	 * @param index
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#getMedium(int)
	 */
	public int getMedium(int index) {
		return buffer.getMedium(index);
	}

	/**
	 * @param index
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#getUnsignedMedium(int)
	 */
	public int getUnsignedMedium(int index) {
		return buffer.getUnsignedMedium(index);
	}

	/**
	 * @param index
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#getInt(int)
	 */
	public int getInt(int index) {
		return buffer.getInt(index);
	}

	/**
	 * @param index
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#getUnsignedInt(int)
	 */
	public long getUnsignedInt(int index) {
		return buffer.getUnsignedInt(index);
	}

	/**
	 * @param index
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#getLong(int)
	 */
	public long getLong(int index) {
		return buffer.getLong(index);
	}

	/**
	 * @param index
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#getChar(int)
	 */
	public char getChar(int index) {
		return buffer.getChar(index);
	}

	/**
	 * @param index
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#getFloat(int)
	 */
	public float getFloat(int index) {
		return buffer.getFloat(index);
	}

	/**
	 * @param index
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#getDouble(int)
	 */
	public double getDouble(int index) {
		return buffer.getDouble(index);
	}

	/**
	 * @param index
	 * @param dst
	 * @see org.jboss.netty.buffer.ChannelBuffer#getBytes(int, org.jboss.netty.buffer.ChannelBuffer)
	 */
	public void getBytes(int index, ChannelBuffer dst) {
		buffer.getBytes(index, dst);
	}

	/**
	 * @param index
	 * @param dst
	 * @param length
	 * @see org.jboss.netty.buffer.ChannelBuffer#getBytes(int, org.jboss.netty.buffer.ChannelBuffer, int)
	 */
	public void getBytes(int index, ChannelBuffer dst, int length) {
		buffer.getBytes(index, dst, length);
	}

	/**
	 * @param index
	 * @param dst
	 * @param dstIndex
	 * @param length
	 * @see org.jboss.netty.buffer.ChannelBuffer#getBytes(int, org.jboss.netty.buffer.ChannelBuffer, int, int)
	 */
	public void getBytes(int index, ChannelBuffer dst, int dstIndex, int length) {
		buffer.getBytes(index, dst, dstIndex, length);
	}

	/**
	 * @param index
	 * @param dst
	 * @see org.jboss.netty.buffer.ChannelBuffer#getBytes(int, byte[])
	 */
	public void getBytes(int index, byte[] dst) {
		buffer.getBytes(index, dst);
	}

	/**
	 * @param index
	 * @param dst
	 * @param dstIndex
	 * @param length
	 * @see org.jboss.netty.buffer.ChannelBuffer#getBytes(int, byte[], int, int)
	 */
	public void getBytes(int index, byte[] dst, int dstIndex, int length) {
		buffer.getBytes(index, dst, dstIndex, length);
	}

	/**
	 * @param index
	 * @param dst
	 * @see org.jboss.netty.buffer.ChannelBuffer#getBytes(int, java.nio.ByteBuffer)
	 */
	public void getBytes(int index, ByteBuffer dst) {
		buffer.getBytes(index, dst);
	}

	/**
	 * @param index
	 * @param out
	 * @param length
	 * @throws IOException
	 * @see org.jboss.netty.buffer.ChannelBuffer#getBytes(int, java.io.OutputStream, int)
	 */
	public void getBytes(int index, OutputStream out, int length)
			throws IOException {
		buffer.getBytes(index, out, length);
	}

	/**
	 * @param index
	 * @param out
	 * @param length
	 * @return
	 * @throws IOException
	 * @see org.jboss.netty.buffer.ChannelBuffer#getBytes(int, java.nio.channels.GatheringByteChannel, int)
	 */
	public int getBytes(int index, GatheringByteChannel out, int length)
			throws IOException {
		return buffer.getBytes(index, out, length);
	}

	/**
	 * @param index
	 * @param value
	 * @see org.jboss.netty.buffer.ChannelBuffer#setByte(int, int)
	 */
	public void setByte(int index, int value) {
		buffer.setByte(index, value);
	}

	/**
	 * @param index
	 * @param value
	 * @see org.jboss.netty.buffer.ChannelBuffer#setShort(int, int)
	 */
	public void setShort(int index, int value) {
		buffer.setShort(index, value);
	}

	/**
	 * @param index
	 * @param value
	 * @see org.jboss.netty.buffer.ChannelBuffer#setMedium(int, int)
	 */
	public void setMedium(int index, int value) {
		buffer.setMedium(index, value);
	}

	/**
	 * @param index
	 * @param value
	 * @see org.jboss.netty.buffer.ChannelBuffer#setInt(int, int)
	 */
	public void setInt(int index, int value) {
		buffer.setInt(index, value);
	}

	/**
	 * @param index
	 * @param value
	 * @see org.jboss.netty.buffer.ChannelBuffer#setLong(int, long)
	 */
	public void setLong(int index, long value) {
		buffer.setLong(index, value);
	}

	/**
	 * @param index
	 * @param value
	 * @see org.jboss.netty.buffer.ChannelBuffer#setChar(int, int)
	 */
	public void setChar(int index, int value) {
		buffer.setChar(index, value);
	}

	/**
	 * @param index
	 * @param value
	 * @see org.jboss.netty.buffer.ChannelBuffer#setFloat(int, float)
	 */
	public void setFloat(int index, float value) {
		buffer.setFloat(index, value);
	}

	/**
	 * @param index
	 * @param value
	 * @see org.jboss.netty.buffer.ChannelBuffer#setDouble(int, double)
	 */
	public void setDouble(int index, double value) {
		buffer.setDouble(index, value);
	}

	/**
	 * @param index
	 * @param src
	 * @see org.jboss.netty.buffer.ChannelBuffer#setBytes(int, org.jboss.netty.buffer.ChannelBuffer)
	 */
	public void setBytes(int index, ChannelBuffer src) {
		buffer.setBytes(index, src);
	}

	/**
	 * @param index
	 * @param src
	 * @param length
	 * @see org.jboss.netty.buffer.ChannelBuffer#setBytes(int, org.jboss.netty.buffer.ChannelBuffer, int)
	 */
	public void setBytes(int index, ChannelBuffer src, int length) {
		buffer.setBytes(index, src, length);
	}

	/**
	 * @param index
	 * @param src
	 * @param srcIndex
	 * @param length
	 * @see org.jboss.netty.buffer.ChannelBuffer#setBytes(int, org.jboss.netty.buffer.ChannelBuffer, int, int)
	 */
	public void setBytes(int index, ChannelBuffer src, int srcIndex, int length) {
		buffer.setBytes(index, src, srcIndex, length);
	}

	/**
	 * @param index
	 * @param src
	 * @see org.jboss.netty.buffer.ChannelBuffer#setBytes(int, byte[])
	 */
	public void setBytes(int index, byte[] src) {
		buffer.setBytes(index, src);
	}

	/**
	 * @param index
	 * @param src
	 * @param srcIndex
	 * @param length
	 * @see org.jboss.netty.buffer.ChannelBuffer#setBytes(int, byte[], int, int)
	 */
	public void setBytes(int index, byte[] src, int srcIndex, int length) {
		buffer.setBytes(index, src, srcIndex, length);
	}

	/**
	 * @param index
	 * @param src
	 * @see org.jboss.netty.buffer.ChannelBuffer#setBytes(int, java.nio.ByteBuffer)
	 */
	public void setBytes(int index, ByteBuffer src) {
		buffer.setBytes(index, src);
	}

	/**
	 * @param index
	 * @param in
	 * @param length
	 * @return
	 * @throws IOException
	 * @see org.jboss.netty.buffer.ChannelBuffer#setBytes(int, java.io.InputStream, int)
	 */
	public int setBytes(int index, InputStream in, int length)
			throws IOException {
		return buffer.setBytes(index, in, length);
	}

	/**
	 * @param index
	 * @param in
	 * @param length
	 * @return
	 * @throws IOException
	 * @see org.jboss.netty.buffer.ChannelBuffer#setBytes(int, java.nio.channels.ScatteringByteChannel, int)
	 */
	public int setBytes(int index, ScatteringByteChannel in, int length)
			throws IOException {
		return buffer.setBytes(index, in, length);
	}

	/**
	 * @param index
	 * @param length
	 * @see org.jboss.netty.buffer.ChannelBuffer#setZero(int, int)
	 */
	public void setZero(int index, int length) {
		buffer.setZero(index, length);
	}

	/**
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#readByte()
	 */
	public byte readByte() {
		return buffer.readByte();
	}

	/**
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#readUnsignedByte()
	 */
	public short readUnsignedByte() {
		return buffer.readUnsignedByte();
	}

	/**
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#readShort()
	 */
	public short readShort() {
		return buffer.readShort();
	}

	/**
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#readUnsignedShort()
	 */
	public int readUnsignedShort() {
		return buffer.readUnsignedShort();
	}

	/**
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#readMedium()
	 */
	public int readMedium() {
		return buffer.readMedium();
	}

	/**
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#readUnsignedMedium()
	 */
	public int readUnsignedMedium() {
		return buffer.readUnsignedMedium();
	}

	/**
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#readInt()
	 */
	public int readInt() {
		return buffer.readInt();
	}

	/**
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#readUnsignedInt()
	 */
	public long readUnsignedInt() {
		return buffer.readUnsignedInt();
	}

	/**
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#readLong()
	 */
	public long readLong() {
		return buffer.readLong();
	}

	/**
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#readChar()
	 */
	public char readChar() {
		return buffer.readChar();
	}

	/**
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#readFloat()
	 */
	public float readFloat() {
		return buffer.readFloat();
	}

	/**
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#readDouble()
	 */
	public double readDouble() {
		return buffer.readDouble();
	}

	/**
	 * @param length
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#readBytes(int)
	 */
	public ChannelBuffer readBytes(int length) {
		return buffer.readBytes(length);
	}

	/**
	 * @param indexFinder
	 * @return
	 * @deprecated
	 * @see org.jboss.netty.buffer.ChannelBuffer#readBytes(org.jboss.netty.buffer.ChannelBufferIndexFinder)
	 */
	public ChannelBuffer readBytes(ChannelBufferIndexFinder indexFinder) {
		return buffer.readBytes(indexFinder);
	}

	/**
	 * @param length
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#readSlice(int)
	 */
	public ChannelBuffer readSlice(int length) {
		return buffer.readSlice(length);
	}

	/**
	 * @param indexFinder
	 * @return
	 * @deprecated
	 * @see org.jboss.netty.buffer.ChannelBuffer#readSlice(org.jboss.netty.buffer.ChannelBufferIndexFinder)
	 */
	public ChannelBuffer readSlice(ChannelBufferIndexFinder indexFinder) {
		return buffer.readSlice(indexFinder);
	}

	/**
	 * @param dst
	 * @see org.jboss.netty.buffer.ChannelBuffer#readBytes(org.jboss.netty.buffer.ChannelBuffer)
	 */
	public void readBytes(ChannelBuffer dst) {
		buffer.readBytes(dst);
	}

	/**
	 * @param dst
	 * @param length
	 * @see org.jboss.netty.buffer.ChannelBuffer#readBytes(org.jboss.netty.buffer.ChannelBuffer, int)
	 */
	public void readBytes(ChannelBuffer dst, int length) {
		buffer.readBytes(dst, length);
	}

	/**
	 * @param dst
	 * @param dstIndex
	 * @param length
	 * @see org.jboss.netty.buffer.ChannelBuffer#readBytes(org.jboss.netty.buffer.ChannelBuffer, int, int)
	 */
	public void readBytes(ChannelBuffer dst, int dstIndex, int length) {
		buffer.readBytes(dst, dstIndex, length);
	}

	/**
	 * @param dst
	 * @see org.jboss.netty.buffer.ChannelBuffer#readBytes(byte[])
	 */
	public void readBytes(byte[] dst) {
		buffer.readBytes(dst);
	}

	/**
	 * @param dst
	 * @param dstIndex
	 * @param length
	 * @see org.jboss.netty.buffer.ChannelBuffer#readBytes(byte[], int, int)
	 */
	public void readBytes(byte[] dst, int dstIndex, int length) {
		buffer.readBytes(dst, dstIndex, length);
	}

	/**
	 * @param dst
	 * @see org.jboss.netty.buffer.ChannelBuffer#readBytes(java.nio.ByteBuffer)
	 */
	public void readBytes(ByteBuffer dst) {
		buffer.readBytes(dst);
	}

	/**
	 * @param out
	 * @param length
	 * @throws IOException
	 * @see org.jboss.netty.buffer.ChannelBuffer#readBytes(java.io.OutputStream, int)
	 */
	public void readBytes(OutputStream out, int length) throws IOException {
		buffer.readBytes(out, length);
	}

	/**
	 * @param out
	 * @param length
	 * @return
	 * @throws IOException
	 * @see org.jboss.netty.buffer.ChannelBuffer#readBytes(java.nio.channels.GatheringByteChannel, int)
	 */
	public int readBytes(GatheringByteChannel out, int length)
			throws IOException {
		return buffer.readBytes(out, length);
	}

	/**
	 * @param length
	 * @see org.jboss.netty.buffer.ChannelBuffer#skipBytes(int)
	 */
	public void skipBytes(int length) {
		buffer.skipBytes(length);
	}

	/**
	 * @param indexFinder
	 * @return
	 * @deprecated
	 * @see org.jboss.netty.buffer.ChannelBuffer#skipBytes(org.jboss.netty.buffer.ChannelBufferIndexFinder)
	 */
	public int skipBytes(ChannelBufferIndexFinder indexFinder) {
		return buffer.skipBytes(indexFinder);
	}

	/**
	 * @param value
	 * @see org.jboss.netty.buffer.ChannelBuffer#writeByte(int)
	 */
	public void writeByte(int value) {
		buffer.writeByte(value);
	}

	/**
	 * @param value
	 * @see org.jboss.netty.buffer.ChannelBuffer#writeShort(int)
	 */
	public void writeShort(int value) {
		buffer.writeShort(value);
	}

	/**
	 * @param value
	 * @see org.jboss.netty.buffer.ChannelBuffer#writeMedium(int)
	 */
	public void writeMedium(int value) {
		buffer.writeMedium(value);
	}

	/**
	 * @param value
	 * @see org.jboss.netty.buffer.ChannelBuffer#writeInt(int)
	 */
	public void writeInt(int value) {
		buffer.writeInt(value);
	}

	/**
	 * @param value
	 * @see org.jboss.netty.buffer.ChannelBuffer#writeLong(long)
	 */
	public void writeLong(long value) {
		buffer.writeLong(value);
	}

	/**
	 * @param value
	 * @see org.jboss.netty.buffer.ChannelBuffer#writeChar(int)
	 */
	public void writeChar(int value) {
		buffer.writeChar(value);
	}

	/**
	 * @param value
	 * @see org.jboss.netty.buffer.ChannelBuffer#writeFloat(float)
	 */
	public void writeFloat(float value) {
		buffer.writeFloat(value);
	}

	/**
	 * @param value
	 * @see org.jboss.netty.buffer.ChannelBuffer#writeDouble(double)
	 */
	public void writeDouble(double value) {
		buffer.writeDouble(value);
	}

	/**
	 * @param src
	 * @see org.jboss.netty.buffer.ChannelBuffer#writeBytes(org.jboss.netty.buffer.ChannelBuffer)
	 */
	public void writeBytes(ChannelBuffer src) {
		buffer.writeBytes(src);
	}

	/**
	 * @param src
	 * @param length
	 * @see org.jboss.netty.buffer.ChannelBuffer#writeBytes(org.jboss.netty.buffer.ChannelBuffer, int)
	 */
	public void writeBytes(ChannelBuffer src, int length) {
		buffer.writeBytes(src, length);
	}

	/**
	 * @param src
	 * @param srcIndex
	 * @param length
	 * @see org.jboss.netty.buffer.ChannelBuffer#writeBytes(org.jboss.netty.buffer.ChannelBuffer, int, int)
	 */
	public void writeBytes(ChannelBuffer src, int srcIndex, int length) {
		buffer.writeBytes(src, srcIndex, length);
	}

	/**
	 * @param src
	 * @see org.jboss.netty.buffer.ChannelBuffer#writeBytes(byte[])
	 */
	public void writeBytes(byte[] src) {
		buffer.writeBytes(src);
	}

	/**
	 * @param src
	 * @param srcIndex
	 * @param length
	 * @see org.jboss.netty.buffer.ChannelBuffer#writeBytes(byte[], int, int)
	 */
	public void writeBytes(byte[] src, int srcIndex, int length) {
		buffer.writeBytes(src, srcIndex, length);
	}

	/**
	 * @param src
	 * @see org.jboss.netty.buffer.ChannelBuffer#writeBytes(java.nio.ByteBuffer)
	 */
	public void writeBytes(ByteBuffer src) {
		buffer.writeBytes(src);
	}

	/**
	 * @param in
	 * @param length
	 * @return
	 * @throws IOException
	 * @see org.jboss.netty.buffer.ChannelBuffer#writeBytes(java.io.InputStream, int)
	 */
	public int writeBytes(InputStream in, int length) throws IOException {
		return buffer.writeBytes(in, length);
	}

	/**
	 * @param in
	 * @param length
	 * @return
	 * @throws IOException
	 * @see org.jboss.netty.buffer.ChannelBuffer#writeBytes(java.nio.channels.ScatteringByteChannel, int)
	 */
	public int writeBytes(ScatteringByteChannel in, int length)
			throws IOException {
		return buffer.writeBytes(in, length);
	}

	/**
	 * @param length
	 * @see org.jboss.netty.buffer.ChannelBuffer#writeZero(int)
	 */
	public void writeZero(int length) {
		buffer.writeZero(length);
	}

	/**
	 * @param fromIndex
	 * @param toIndex
	 * @param value
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#indexOf(int, int, byte)
	 */
	public int indexOf(int fromIndex, int toIndex, byte value) {
		return buffer.indexOf(fromIndex, toIndex, value);
	}

	/**
	 * @param fromIndex
	 * @param toIndex
	 * @param indexFinder
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#indexOf(int, int, org.jboss.netty.buffer.ChannelBufferIndexFinder)
	 */
	public int indexOf(int fromIndex, int toIndex,
			ChannelBufferIndexFinder indexFinder) {
		return buffer.indexOf(fromIndex, toIndex, indexFinder);
	}

	/**
	 * @param value
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#bytesBefore(byte)
	 */
	public int bytesBefore(byte value) {
		return buffer.bytesBefore(value);
	}

	/**
	 * @param indexFinder
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#bytesBefore(org.jboss.netty.buffer.ChannelBufferIndexFinder)
	 */
	public int bytesBefore(ChannelBufferIndexFinder indexFinder) {
		return buffer.bytesBefore(indexFinder);
	}

	/**
	 * @param length
	 * @param value
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#bytesBefore(int, byte)
	 */
	public int bytesBefore(int length, byte value) {
		return buffer.bytesBefore(length, value);
	}

	/**
	 * @param length
	 * @param indexFinder
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#bytesBefore(int, org.jboss.netty.buffer.ChannelBufferIndexFinder)
	 */
	public int bytesBefore(int length, ChannelBufferIndexFinder indexFinder) {
		return buffer.bytesBefore(length, indexFinder);
	}

	/**
	 * @param index
	 * @param length
	 * @param value
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#bytesBefore(int, int, byte)
	 */
	public int bytesBefore(int index, int length, byte value) {
		return buffer.bytesBefore(index, length, value);
	}

	/**
	 * @param index
	 * @param length
	 * @param indexFinder
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#bytesBefore(int, int, org.jboss.netty.buffer.ChannelBufferIndexFinder)
	 */
	public int bytesBefore(int index, int length,
			ChannelBufferIndexFinder indexFinder) {
		return buffer.bytesBefore(index, length, indexFinder);
	}

	/**
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#copy()
	 */
	public ChannelBuffer copy() {
		return buffer.copy();
	}

	/**
	 * @param index
	 * @param length
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#copy(int, int)
	 */
	public ChannelBuffer copy(int index, int length) {
		return buffer.copy(index, length);
	}

	/**
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#slice()
	 */
	public ChannelBuffer slice() {
		return buffer.slice();
	}

	/**
	 * @param index
	 * @param length
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#slice(int, int)
	 */
	public ChannelBuffer slice(int index, int length) {
		return buffer.slice(index, length);
	}

	/**
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#duplicate()
	 */
	public ChannelBuffer duplicate() {
		return buffer.duplicate();
	}

	/**
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#toByteBuffer()
	 */
	public ByteBuffer toByteBuffer() {
		return buffer.toByteBuffer();
	}

	/**
	 * @param index
	 * @param length
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#toByteBuffer(int, int)
	 */
	public ByteBuffer toByteBuffer(int index, int length) {
		return buffer.toByteBuffer(index, length);
	}

	/**
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#toByteBuffers()
	 */
	public ByteBuffer[] toByteBuffers() {
		return buffer.toByteBuffers();
	}

	/**
	 * @param index
	 * @param length
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#toByteBuffers(int, int)
	 */
	public ByteBuffer[] toByteBuffers(int index, int length) {
		return buffer.toByteBuffers(index, length);
	}

	/**
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#hasArray()
	 */
	public boolean hasArray() {
		return buffer.hasArray();
	}

	/**
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#array()
	 */
	public byte[] array() {
		return buffer.array();
	}

	/**
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#arrayOffset()
	 */
	public int arrayOffset() {
		return buffer.arrayOffset();
	}

	/**
	 * @param charset
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#toString(java.nio.charset.Charset)
	 */
	public String toString(Charset charset) {
		return buffer.toString(charset);
	}

	/**
	 * @param index
	 * @param length
	 * @param charset
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#toString(int, int, java.nio.charset.Charset)
	 */
	public String toString(int index, int length, Charset charset) {
		return buffer.toString(index, length, charset);
	}

	/**
	 * @param charsetName
	 * @return
	 * @deprecated
	 * @see org.jboss.netty.buffer.ChannelBuffer#toString(java.lang.String)
	 */
	public String toString(String charsetName) {
		return buffer.toString(charsetName);
	}

	/**
	 * @param charsetName
	 * @param terminatorFinder
	 * @return
	 * @deprecated
	 * @see org.jboss.netty.buffer.ChannelBuffer#toString(java.lang.String, org.jboss.netty.buffer.ChannelBufferIndexFinder)
	 */
	public String toString(String charsetName,
			ChannelBufferIndexFinder terminatorFinder) {
		return buffer.toString(charsetName, terminatorFinder);
	}

	/**
	 * @param index
	 * @param length
	 * @param charsetName
	 * @return
	 * @deprecated
	 * @see org.jboss.netty.buffer.ChannelBuffer#toString(int, int, java.lang.String)
	 */
	public String toString(int index, int length, String charsetName) {
		return buffer.toString(index, length, charsetName);
	}

	/**
	 * @param index
	 * @param length
	 * @param charsetName
	 * @param terminatorFinder
	 * @return
	 * @deprecated
	 * @see org.jboss.netty.buffer.ChannelBuffer#toString(int, int, java.lang.String, org.jboss.netty.buffer.ChannelBufferIndexFinder)
	 */
	public String toString(int index, int length, String charsetName,
			ChannelBufferIndexFinder terminatorFinder) {
		return buffer.toString(index, length, charsetName, terminatorFinder);
	}

	/**
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#hashCode()
	 */
	public int hashCode() {
		return buffer.hashCode();
	}

	/**
	 * @param obj
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		return buffer.equals(obj);
	}

	/**
	 * @param buffer
	 * @return
	 * @see org.jboss.netty.buffer.ChannelBuffer#compareTo(org.jboss.netty.buffer.ChannelBuffer)
	 */
	public int compareTo(ChannelBuffer buffer) {
		return buffer.compareTo(buffer);
	}


	
	/**
	 * Returns an {@link OutputStream} facade of this buffer.
	 * @return an OutputStream
	 */
	public OutputStream asOutputStream() {
		return new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				writeByte(b);
			}
		};
	}
	
	/**
	 * Returns an {@link InputStream} facade of this buffer.
	 * @return an InputStream
	 */
	public InputStream asInputStream() {
		return new InputStream() {
			@Override
			public int read() throws IOException {
				int i = readByte();
				return i;
			}
		};
	}
	
	/**
	 * Reads the passed input stream into this buffer and closes the stream on completion
	 * If the read fails, the writer index is reset, leaving the buffer unmodified.
	 * @param is The input stream
	 * @return the number of bytes read and written into this buffer
	 */
	public long readInputStream(InputStream is) {
		return readInputStream(is, true);
	}
	
	
	/**
	 * Reads the passed input stream into this buffer.
	 * If the read fails, the writer index is reset, leaving the buffer unmodified.
	 * @param is The input stream
	 * @param closeOnDone If true, the input stream will be closed on completion
	 * @return the number of bytes read and written into this buffer
	 */
	public long readInputStream(InputStream is, boolean closeOnDone) {
		if(is==null) throw new IllegalArgumentException("The passed stream was null", new Throwable());
		long bytes = 0;
		final int wi = buffer.writerIndex();
		byte[] buff = new byte[1024];
		int bytesRead = -1;
		try {
			while((bytesRead = is.read(buff))!=-1) {
				bytes += bytesRead;
				buffer.writeBytes(buff, 0, bytesRead);
			}
			return bytes;
		} catch (Exception  e) {
			buffer.writerIndex(wi);
			throw new RuntimeException("Failed to read in stream", e);
		} finally {
			if(closeOnDone) try { is.close(); } catch (Exception e) {}			
		}
	}
	
	/**
	 * Writes the readable content of this buffer to the passed output stream, flushing and closing the output stream on completion 
	 * If the write fails, the reader index is reset, leaving the buffer unmodified.
	 * @param os The output stream to write to
	 * @return The number of bytes written to the output stream
	 */
	public long writeOutputStream(OutputStream os) {
		return writeOutputStream(os, true);
	}
	
	
	/**
	 * Writes the readable content of this buffer to the passed output stream.
	 * If the write fails, the reader index is reset, leaving the buffer unmodified.
	 * @param os The output stream to write to
	 * @param closeOnDone If true, the output stream will be flushed and closed on completion
	 * @return The number of bytes written to the output stream
	 */
	public long writeOutputStream(OutputStream os, boolean closeOnDone) {
		if(os==null) throw new IllegalArgumentException("The passed stream was null", new Throwable());
		long bytes = buffer.readableBytes();
		final int ri = buffer.readerIndex();
		try {
			for(long i = 0; i < bytes; i++) {
				os.write(buffer.readByte());
			}
			return bytes;
		} catch (Exception  e) {
			buffer.readerIndex(ri);
			throw new RuntimeException("Failed to read in stream", e);
		} finally {
			if(closeOnDone) {
				try { os.flush(); } catch (Exception e) {}
				try { os.close(); } catch (Exception e) {}			
			}
		}
		
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return buffer.toString();
	}
	

}
