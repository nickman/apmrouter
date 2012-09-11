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
package org.helios.apmrouter.trace;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.helios.apmrouter.metric.IMetric;

import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

/**
 * <p>Title: DirectMetricCollection</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.trace.DirectMetricCollection</code></p>
 */
public class DirectMetricCollection {
    /** The unsafe instance */
    protected static final Unsafe unsafe;
    /** The initial capacity allocation */
    public static final int INITIAL = 512;
    /** The extend capacity allocation */
    public static final int EXTEND = 512;
    /** The byte array offset */
    public static final int BYTE_ARRAY_OFFSET;
    
    /** Zero byte literal */
    public static final byte BYTE_ZERO = 0;
    /** One byte literal */
    public static final byte BYTE_ONE = 0;
    

    /** the address of the direct memory allocation */
    protected long address = 0;
    /** the current size of this collection in bytes */
    protected int size = 0;
    /** the current capacity of this collection in bytes */
    protected int capacity = 0;
    
    
    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe)field.get(null);
            BYTE_ARRAY_OFFSET = unsafe.arrayBaseOffset(byte[].class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get the Unsafe instance", e);
        }
    }
    
    /**
     * Creates a new DirectMetricCollection and loads it with the passed metrics
     * @param metrics The initial metrics to load
     * @return the loaded DirectMetricCollection
     */
    public static DirectMetricCollection newDirectMetricCollection(IMetric...metrics) {
    	DirectMetricCollection dmc = new DirectMetricCollection();
    	for(IMetric m: metrics) {
    		dmc.append(m);
    	}
    	return dmc;
    }
    
    
    
    /**
     * Returns the current size in bytes of this collection
	 * @return the current size in bytes of this collection
	 */
	public int getSize() {
		return size;
	}



	/**
	 * Returns the current memory capacity in bytes of this collection
	 * @return the current memory capacity in bytes of this collection
	 */
	public int getCapacity() {
		return capacity;
	}



	/**
     * Loads an IMetric
     * @param metric the metric to load
     */
    public void append(IMetric metric) {
    	while(size + metric.getSerSize() > capacity) extend();
    	writeByte(ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN) ? BYTE_ZERO : BYTE_ONE);
    	long token = metric.getToken();
    	if(token!=-1) {
    		writeByte(BYTE_ONE);
    		writeLong(token);
    	} else {
    		writeByte(BYTE_ZERO);    		
    		writeByte((byte)metric.getType().ordinal());
    		byte[] fqnBytes = metric.getFQN().getBytes();
    		writeInt(fqnBytes.length);
    		writeBytes(fqnBytes);
    	}
    	writeLong(metric.getTime());
    	if(metric.getType().isLong()) {
    		writeLong(metric.getLongValue());
    	} else {
    		ByteBuffer bb = metric.getRawValue();
    		writeInt(bb.limit());
    		writeBytes(bb);    		
    	}
    	updateCount();
    }
    
    
    
    /**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DirectMetricCollection [size=");
		builder.append(getSize());
		builder.append(", capacity=");
		builder.append(getCapacity());
		builder.append(", metricCount=");
		builder.append(getMetricCount());
		builder.append("]");
		return builder.toString();
	}



	protected void writeBytes(ByteBuffer bb) {    	
    	if(bb.isDirect()) {
    		DirectBuffer db = (DirectBuffer)bb;
    		unsafe.copyMemory(null, db.address(), null, address + size, bb.limit());
    	} else {
    		writeBytes(bb.array());
    	}
    }

    
    protected void writeBytes(byte[] bytes) {
    	unsafe.copyMemory(bytes, BYTE_ARRAY_OFFSET, null, address + size, bytes.length);
    	size += bytes.length;
    }
    
    
    protected void writeLong(long v) {
    	unsafe.putLong(address + size, v);
    	size += 8;
    }
    
    
    protected void writeInt(int i) {
    	unsafe.putInt(address + size, i);
    	size += 4;
    }
    
    
    protected void writeByte(byte b) {
    	unsafe.putByte(address + size, b);
    	size += 1;
    }

    /**
     * Creates a new DirectMetricCollection
     */
    private DirectMetricCollection() {
    	address = unsafe.allocateMemory(INITIAL);
    	capacity = INITIAL;
    	writeInt(0);
    }
    
    /**
     * Updates the count of metrics
     */
    private void updateCount() {    	
    	unsafe.putInt(address, unsafe.getInt(address)+1);
    }
    
    /**
     * Returns the number of metrics contained
     * @return the number of metrics contained
     */
    public int getMetricCount() {
    	return unsafe.getInt(address);
    }
    
    /**
     * Extends the allocated memory space by {@link #EXTEND} bytes
     */
    private void extend() {
    	address = unsafe.reallocateMemory(address, capacity + EXTEND);
    	capacity += EXTEND;
    }
    
    /**
     * Deallocates the memory reserved for this object
     */
    public void destroy() {
    	if(address!=0) {
    		unsafe.freeMemory(address);
    		address = 0;
    	}
    }
    
    /**
     * <p>Deallocates the memory if it has not been deallocated already
     * {@inheritDoc}
     * @see java.lang.Object#finalize()
     */
    protected void finalize() throws Throwable {
    	destroy();
    	super.finalize();
    	
    }

}
