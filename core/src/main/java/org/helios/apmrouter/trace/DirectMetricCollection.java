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
import java.util.Collection;

import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.sender.ISender;
import org.helios.apmrouter.sender.Sender;

import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

/**
 * <p>Title: DirectMetricCollection</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.trace.DirectMetricCollection</code></p>
 */
public class DirectMetricCollection implements Runnable {
    /** The unsafe instance */
    protected static final Unsafe unsafe;
    /** The initial capacity allocation */
    public static final int INITIAL;
    /** The extend capacity allocation */
    public static final int EXTEND;
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
            INITIAL = unsafe.pageSize();
            EXTEND = unsafe.pageSize()/8;
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
    		dmc._append(m);
    	}
    	return dmc;
    }
    
	/**
     * Loads a collection of IMetrics
     * @param metrics the metrics to load
     * @return the number of metrics in the collection after this operation completes
     */
    public synchronized int append(Collection<IMetric> metrics) {
    	if(metrics!=null && !metrics.isEmpty()) {
        	for(IMetric metric: metrics) {
        		_append(metric);
        	}    		
    	}
    	return getMetricCount();
    }
     
	/**
     * Loads an array of IMetrics
     * @param metrics the metrics to load
     * @return the number of metrics in the collection after this operation completes
     */
    public synchronized int append(IMetric...metrics) {
    	for(IMetric metric: metrics) {
    		_append(metric);
    	}
    	return getMetricCount();
    }
    
    
    /**
     * <p>Sends this collection
     * {@inheritDoc}
     * @see java.lang.Runnable#run()
     */
    public void run() {
    	//Sender.getInstance();
    }
    
	/**
     * Loads an IMetric
     * @param metric the metric to load
     * @return the number of metrics in the collection after this operation completes
     */
    protected int _append(IMetric metric) {
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
    	return updateCount();
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



	/**
	 * Writes the bytes contained in the passed ByteBuffer into this collection
	 * @param bb the ByteBuffer to write
	 */
	protected void writeBytes(ByteBuffer bb) {    	
    	if(bb.isDirect()) {
    		DirectBuffer db = (DirectBuffer)bb;
    		int byteCount = bb.limit();
    		unsafe.copyMemory(null, db.address(), null, address + size, byteCount);
    		size += byteCount;
    	} else {
    		byte[] bytes = new byte[bb.limit()];
    		bb.get(bytes);
    		writeBytes(bytes);
    	}
    	
    }

    
    /**
     * Writes the passed bytes to the memory block
     * @param bytes the bytes to write
     */
    protected void writeBytes(byte[] bytes) {
    	unsafe.copyMemory(bytes, BYTE_ARRAY_OFFSET, null, address + size, bytes.length);
    	size += bytes.length;
    }
    
    
    /**
     * Writes the passed long value to the memory block
     * @param v the long value to write
     */
    protected void writeLong(long v) {
    	unsafe.putLong(address + size, v);
    	size += 8;
    }
    
    
    /**
     * Writes the passed int value to the memory block
     * @param v the int value to write
     */
    protected void writeInt(int i) {
    	unsafe.putInt(address + size, i);
    	size += 4;
    }
    
    
    /**
     * Writes the passed byte to the memory block
     * @param v the byte to write
     */
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
     * @return the new metric count
     */
    private int updateCount() {    	
    	unsafe.putInt(address, unsafe.getInt(address)+1);
    	return getMetricCount();
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
    @Override
    protected void finalize() throws Throwable {
    	destroy();
    	super.finalize();
    	
    }

}
