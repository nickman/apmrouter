/*
 * Copyright 2011 Peter Lawrey
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package vanilla.java.chronicle.impl;

import java.lang.reflect.Field;

import org.helios.apmrouter.unsafe.UnsafeAdapter;

import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

/**
 * @author peter.lawrey
 */
public class UnsafeExcerpt<C extends DirectChronicle> extends AbstractExcerpt<C> {
	/** The address of the current excerpt */
	long address  = -1;
	
	
    protected UnsafeExcerpt(C chronicle) {
        super(chronicle);
    }

    protected void index0(long index, long startPosition, long endPosition) {
        this.index = index;
        this.startPosition = startPosition;

        buffer = chronicle.acquireDataBuffer(startPosition);

        address = ((DirectBuffer) buffer).address();
        start = position = address + chronicle.positionInBuffer(startPosition);
        limit = address + chronicle.positionInBuffer(endPosition - 1) + 1;

        assert limit > start && position < limit && endPosition > startPosition;
    }
    
    /**
     * Rolls the bytes in this excerpt to the right
     * @param startingPosition The starting position of the roll
     * @param slotsToMove The number of bytes to roll
     */
    public void insertAndRollRight(long startingPosition, long slotsToMove) {
    	long address = ((DirectBuffer) buffer).address();
    	UNSAFE.copyMemory(address + startingPosition, address + startingPosition + slotsToMove, slotsToMove);
    }
    
    /**
     * Rolls the bytes in this excerpt to the right starting at position zero
     * @param slotsToMove The number of bytes to roll
     */
    public void insertAndRollRight(long slotsToMove) {
    	insertAndRollRight(0, slotsToMove);
    }
    

    // RandomDataInput

    @Override
    public byte readByte() {
        return UNSAFE.getByte(position++);
    }

    @Override
    public byte readByte(int offset) {
        return UNSAFE.getByte(start + offset);
    }

    @Override
    public void write(int offset, byte[] b) {
    	UnsafeAdapter.copyMemory(b, BYTES_OFFSET, null, position, b.length);
        position += b.length;
    }

    @Override
    public void write(byte[] b, int off, int len) {
    	UnsafeAdapter.copyMemory(b, BYTES_OFFSET + off, null, position, len);
        position += len;
    }
    
    @Override
    public void readFully(byte[] b, int off, int len) {
    	UnsafeAdapter.copyMemory(null, position, b, BYTES_OFFSET + off, len);
        position += len;
    }

    @Override
    public short readShort() {
        short s = UNSAFE.getShort(position);
        position += 2;
        return s;
    }

    @Override
    public short readShort(int offset) {
        return UNSAFE.getShort(start + offset);
    }

    @Override
    public char readChar() {
        char ch = UNSAFE.getChar(position);
        position += 2;
        return ch;
    }

    @Override
    public char readChar(int offset) {
        return UNSAFE.getChar(start + offset);
    }

    @Override
    public int readInt() {
        int i = UNSAFE.getInt(position);
        position += 4;
        return i;
    }

    @Override
    public int readInt(int offset) {
        return UNSAFE.getInt(start + offset);
    }

    @Override
    public long readLong() {
        long l = UNSAFE.getLong(position);
        position += 8;
        return l;
    }
    
    /**
     * Reads and returns a long array from the excerpt
     * @param offset The offset in the excerpt to read from
     * @param length The length of the long array to read
     * @return the read array
     */
    public long[] readLongArray(int offset, int length) {
    	long[] arr = new long[length];
    	UNSAFE.copyMemory(null, address + offset, arr, LONGS_OFFSET, length << 3);
    	return arr;
    }
    
    /**
     * Reads and returns a long array from the excerpt
     * @param length The length of the long array to read
     * @return the read array
     */
    public long[] readLongArray(int length) {
    	long[] arr = new long[length];
    	UNSAFE.copyMemory(null, position, arr, LONGS_OFFSET, length << 3);
    	return arr;
    }
    
    
    /**
     * Writes a long array to the specified offset in this excerpt
     * @param offset The offset to write at
     * @param arr The array to write
     */
    public void writeLongArray(int offset, long...arr) {
    	UNSAFE.copyMemory(arr, LONGS_OFFSET, null, address + offset, arr.length << 3);
    }
    
    /**
     * Writes a long array to the current position
     * @param arr The array to write
     */
    public void writeLongArray(long...arr) {
    	UNSAFE.copyMemory(arr, LONGS_OFFSET, null, position, arr.length << 3);
    	position += arr.length << 3;
    }    

    @Override
    public long readLong(int offset) {
        return UNSAFE.getLong(start + offset);
    }

    @Override
    public float readFloat() {
        float f = UNSAFE.getFloat(position);
        position += 4;
        return f;
    }

    @Override
    public float readFloat(int offset) {
        return UNSAFE.getFloat(start + offset);
    }

    @Override
    public double readDouble() {
        double d = UNSAFE.getDouble(position);
        position += 8;
        return d;
    }

    @Override
    public double readDouble(int offset) {
        return UNSAFE.getDouble(start + offset);
    }

    @Override
    public void write(int b) {
        UNSAFE.putByte(position++, (byte) b);
    }

    @Override
    public void write(int offset, int b) {
        UNSAFE.putByte(start + offset, (byte) b);
    }


    @Override
    public void writeShort(int v) {
        UNSAFE.putShort(position, (short) v);
        position += 2;
    }

    @Override
    public void writeShort(int offset, int v) {
        UNSAFE.putShort(start + offset, (short) v);
    }

    @Override
    public void writeChar(int v) {
        UNSAFE.putChar(position, (char) v);
        position += 2;
    }

    @Override
    public void writeChar(int offset, int v) {
        UNSAFE.putChar(start + offset, (char) v);
    }

    @Override
    public void writeInt(int v) {
        UNSAFE.putInt(position, v);
        position += 4;
    }

    @Override
    public void writeInt(int offset, int v) {
        UNSAFE.putInt(start + offset, v);
    }

    @Override
    public void writeLong(long v) {
        UNSAFE.putLong(position, v);
        position += 8;
    }

    @Override
    public void writeLong(int offset, long v) {
        UNSAFE.putLong(start + offset, v);
    }
    
    public void writeToken(long token) {
    	buffer.putLong(0, token);
    	UNSAFE.putLong(start, token);
    	
    }

    @Override
    public void writeFloat(float v) {
        UNSAFE.putFloat(position, v);
        position += 4;
    }

    @Override
    public void writeFloat(int offset, float v) {
        UNSAFE.putFloat(start + offset, v);
    }

    @Override
    public void writeDouble(double v) {
        UNSAFE.putDouble(position, v);
        position += 8;
    }

    @Override
    public void writeDouble(int offset, double v) {
        UNSAFE.putDouble(start + offset, v);
    }

    /**
     * *** Access the Unsafe class *****
     */
    private static final Unsafe UNSAFE;
    private static final int BYTES_OFFSET;
    private static final int LONGS_OFFSET;
    private static final int INTS_OFFSET;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (Unsafe) theUnsafe.get(null);
            BYTES_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);
            LONGS_OFFSET = UNSAFE.arrayBaseOffset(long[].class);
            INTS_OFFSET = UNSAFE.arrayBaseOffset(int[].class);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
