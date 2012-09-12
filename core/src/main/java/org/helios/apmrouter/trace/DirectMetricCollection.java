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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.helios.apmrouter.metric.ICEMetric;
import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.metric.MetricType;
import org.helios.apmrouter.metric.catalog.ICEMetricCatalog;
import org.helios.apmrouter.metric.catalog.IDelegateMetric;
import org.helios.apmrouter.sender.Sender;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

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
    /** The initial capacity allocation which will be equal to the JVM's memory page size */
    public static final int INITIAL;
    /** The extend capacity allocation */
    public static final int EXTEND;
    /** The byte array offset */
    public static final int BYTE_ARRAY_OFFSET;
    
    /** The offset from the address where the actual metric data starts */
    public static final int METRIC_OFFSET = 10;
    /** The offset from the address where the DCM byte size is */
    public static final int SIZE_OFFSET = 0;
    /** The offset from the address where the DCM metric count is */
    public static final int COUNT_OFFSET = 4;
    
    /** Zero byte literal */
    public static final byte BYTE_ZERO = 0;
    /** One byte literal */
    public static final byte BYTE_ONE = 1;
    /** The byte order indicator */
    public static final byte BYTE_ORDER = ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN) ? BYTE_ZERO : BYTE_ONE;
    

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
     * Creates a new DirectMetricCollection using the default initial capacity of {@link #INITIAL} and loads it with the passed metrics
     * @param metrics The initial metrics to load
     * @return the loaded DirectMetricCollection
     */
    public static DirectMetricCollection newDirectMetricCollection(IMetric...metrics) {
    	return newDirectMetricCollection(INITIAL, metrics);
    }
    
    /**
     * Creates a new DirectMetricCollection and loads it with the passed metrics
     * @param initialCapacity The inital capacity of this DMC in bytes
     * @param metrics The initial metrics to load
     * @return the loaded DirectMetricCollection
     */
    public static DirectMetricCollection newDirectMetricCollection(int initialCapacity, IMetric...metrics) {
    	DirectMetricCollection dmc = new DirectMetricCollection(initialCapacity);
    	for(IMetric m: metrics) {
    		dmc._append(m);
    	}
    	return dmc;
    }
    
    
	/**
     * Loads a collection of IMetrics
     * @param metrics the metrics to load
     * @return the number of bytes in the collection after this operation completes
     */
    public synchronized int append(Collection<IMetric> metrics) {
    	if(metrics!=null && !metrics.isEmpty()) {
        	for(IMetric metric: metrics) {
        		_append(metric);
        	}    		
    	}
    	return size;
    }
     
	/**
     * Loads an array of IMetrics
     * @param metrics the metrics to load
     * @return the number of bytes in the collection after this operation completes
     */
    public synchronized int append(IMetric...metrics) {
    	for(IMetric metric: metrics) {
    		_append(metric);
    	}
    	return size;
    }
    
    
    /**
     * <p>Sends this collection
     * {@inheritDoc}
     * @see java.lang.Runnable#run()
     */
    public void run() {
    	Sender.getInstance().getDefaultSender().send(this);
    }
    
	/**
     * Loads an IMetric
     * @param metric the metric to load
     * @return the number of metrics in the collection after this operation completes
     */
    protected int _append(IMetric metric) {
    	while(size + metric.getSerSize()+6 > capacity) extend();    	
    	long token = metric.getToken();
    	final int currentSize = size;
    	// write the place-holder for the size
    	writeInt(0);
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
    	int metricSize = size - currentSize;
    	unsafe.putInt(address + currentSize, metricSize);
    	// update the DMC size
    	setSize(size);
    	return updateCount();
    }
    
    /**
     * Decodes this DMC back into an array of IMetrics
     * @return an array of IMetrics
     */
    public IMetric[] decode() {
    	List<IMetric> metrics = new ArrayList<IMetric>(getMetricCount());
    	for(IMetric metric: newMetricReader()) {
    		metrics.add(metric);
    	}
    	return metrics.toArray(new IMetric[0]);
    }
    
    /**
     * Writes this DMC to a direct {@link ChannelBuffer} and then destroys.
     * @return a loaded {@link ChannelBuffer}
     */
    public ChannelBuffer toChannelBuffer() {
    	ChannelBuffer cb = ChannelBuffers.directBuffer(size);
		byte[] bytes = new byte[size];
		unsafe.copyMemory(null, (address), bytes, BYTE_ARRAY_OFFSET, size);
		destroy();
		cb.writeBytes(bytes);
    	return cb;
    }
    
    
    /**
     * Copies memory in from another address
     * @param otherAddress The other address
     * @param byteCount The number of bytes to copy
     */
    protected void copyFrom(long otherAddress, int byteCount) {
    	while(size + byteCount > capacity) extend();    
    	unsafe.copyMemory(otherAddress, address+size, byteCount);
    	size += byteCount;
    	updateCount();
    	setSize(size);
    }
    
    /**
     * <p>Title: Reader</p>
     * <p>Description: Base class for cursor controlled readers</p> 
     * <p>Company: Helios Development Group LLC</p>
     * @author Whitehead (nwhitehead AT heliosdev DOT org)
     * <p><code>org.helios.apmrouter.trace.DirectMetricCollection.Reader</code></p>
     */
    protected abstract class Reader {
    	/** The current offset for this reader */
    	protected int offset = METRIC_OFFSET;
    	/** The number of metrics to read */
    	protected final int metricCount = getMetricCount();
    	/** The current cursor */
    	protected int cursor = -1;
		/** The root address offset for the current record */
		protected int rootAddress = METRIC_OFFSET; 
		/** The byte size of the current record */
		protected int byteCount = -1;

		protected Reader() {
			byteCount = getSize();
		}
    	
    	
		public boolean hasNext() {
			return cursor<metricCount;
		}
		
		/**
		 * No Op. Not supported yet.
		 */		
		public void remove() {
			
		}
		
		/**
		 * Increments the offset to the next root address
		 * @return true if the offset was incremented, false if the cursor is EOF
		 */
		protected boolean _next() {
			cursor++;
			if(cursor==metricCount) return false;			
			if(cursor==0) {				
				return true;
			}
			offset += byteCount;
			rootAddress = offset;
			byteCount = getSize();
			return true;
		}
		
		/**
		 * Returns the size of the record without incrementing the counter
		 * @return the size of the record in bytes
		 */
		protected int getSize() {
			//System.out.println("[" + Thread.currentThread() + "] Getting current record size [" + rootAddress + "], Cursor:" + cursor + " MetricCount:" + metricCount);
			return unsafe.getInt(address + rootAddress);
		}
		
		/**
		 * Reads a byte
		 * @return the read byte
		 */
		protected byte readByte() {
			byte b = unsafe.getByte(address + offset);
			offset++;
			return b;
		}
		
		/**
		 * Reads an int
		 * @return the read int
		 */
		protected int readInt() {
			int i = unsafe.getInt(address + offset);
			offset += 4;
			return i;
		}
		
		/**
		 * Reads a long
		 * @return the read long
		 */
		protected long readLong() {
			long v = unsafe.getLong(address + offset);
			offset += 8;
			return v;
		}
		
		/**
		 * Reads a string 
		 * @return the read string
		 */
		protected String readString() {
			return new String(readBytes(readInt()));
		}
		
		/**
		 * Reads the specified number of bytes and returns them in a byte array
		 * @param byteCount the number of bytes to read
		 * @return a byte array
		 */
		protected byte[] readBytes(int byteCount) {
			byte[] bytes = new byte[byteCount];
			unsafe.copyMemory(null, (address + offset), bytes, BYTE_ARRAY_OFFSET, byteCount);
			offset += byteCount;
			return bytes;
		}
		
		/**
		 * Reads the specified number of bytes and returns them in a direct ByteBuffer
		 * @param byteCount the number of bytes to read
		 * @return a direct ByteBuffer
		 */
		protected ByteBuffer readByteBuffer(int byteCount) {
			byte[] bytes = new byte[byteCount];
			unsafe.copyMemory(null, (address + offset), bytes, BYTE_ARRAY_OFFSET, byteCount);
			ByteBuffer bb = ByteBuffer.allocateDirect(byteCount);
			bb.put(bytes);
			bb.flip();
			offset += byteCount;
			return bb;
		}
    	
    	
    }
    
    /**
     * <p>Title: MetricReader</p>
     * <p>Description: A cursor supported metric decoder</p> 
     * <p>Company: Helios Development Group LLC</p>
     * @author Whitehead (nwhitehead AT heliosdev DOT org)
     * <p><code>org.helios.apmrouter.trace.DirectMetricCollection.MetricReader</code></p>
     */
    protected class MetricReader extends Reader implements Iterator<IMetric>, Iterable<IMetric> {
		/**
		 * {@inheritDoc}
		 * @see java.util.Iterator#next()
		 */
		@Override
		public IMetric next() {			
			cursor++;
    		MetricType type = null;
    		IDelegateMetric dmetric = null;
    		readInt(); // skip size
    		if(byteCount<1) throw new RuntimeException("Read a <= 1 byte count", new Throwable());
    		byte isToken = readByte();
    		if(BYTE_ONE==isToken) {
    			long token =  readLong();
    			dmetric = ICEMetricCatalog.getInstance().get(token);
    			type = dmetric.getType();
    		} else {
    			type = MetricType.valueOf(readByte());
				String fqn = readString();
				dmetric = ICEMetricCatalog.getInstance().build(fqn, type);
    		}
    		long time = readLong();
			if(type.isLong()) {
				long value = readLong();
				return ICEMetric.newMetric(time, value, type, dmetric);
			}
			ByteBuffer bb = readByteBuffer(readInt());
			return ICEMetric.newMetric(time, bb, type, dmetric);
		}

		@Override
		public Iterator<IMetric> iterator() {			
			return this;
		}
    }
    
    public class SplitReader extends Reader implements Iterable<DirectMetricCollection>, Iterator<DirectMetricCollection> {
    	/** The maximum size of one split DirectMetricCollection */
    	private final int maxSize;
    	/** The current DMC */
    	private DirectMetricCollection dmc; 
    	/** The droup count, which is metrics too large to serialize */
    	private int drops = 0;
    	
    	/**
    	 * The droup count, which is metrics too large to serialize
		 * @return the number of dropped metrics
		 */
		public int getDrops() {
			return drops;			
		}
		
		/**
    	 * Creates a new SplitReader
    	 * @param maxSize The maximum size of one split DirectMetricCollection
    	 */
    	public SplitReader(int maxSize) {
    		this.maxSize = maxSize;
    		dmc = DirectMetricCollection.newDirectMetricCollection(maxSize);
    	}
		/**
		 * {@inheritDoc}
		 * @see java.util.Iterator#next()
		 */
		@Override
		public DirectMetricCollection next() {
			while(_next()) {
				if(byteCount<maxSize) {
					if(dmc.size + byteCount < maxSize) {
						dmc.copyFrom(address + rootAddress, byteCount);
					} else {
						DirectMetricCollection retMet = dmc;
						dmc = DirectMetricCollection.newDirectMetricCollection(maxSize);
						return retMet.shrinkWrap();
					}
				} else {
					drops++;
				}
			}
			return dmc.shrinkWrap();
		}
		@Override
		public Iterator<DirectMetricCollection> iterator() {
			return this;
		}
    }
    
    /**
     * Returns a new SplitReader for splitting this DMC into multiple DMCs each with a byte size smaller than the passed value.
     * @param maxSize The maximum size of the split DMCs
     * @return a SplitReader
     */
    public SplitReader newSplitReader(int maxSize) {
    	return new SplitReader(maxSize);
    }
    
    /**
     * Returns a MetricReader that can iteratively decode all the {@ link IMetric}s in this DMC.
     * @return a MetricReader 
     */
    public MetricReader newMetricReader() {
    	return new MetricReader();
    }
    
    
    /**
     * Splits this DMC into multiple DMCs where each one is smaller than the passed maximum size in bytes.
     * This is provided for UDP based senders where the maximum message size may be smaller than the total in a combined DMC.
     * @param maxSize The maximum size in bytes for all DMCs returned
     * @return an array of DMCs
     */
    public DirectMetricCollection[] split(final int maxSize) {
    	List<DirectMetricCollection> splits = new ArrayList<DirectMetricCollection>();
    	for(DirectMetricCollection d: newSplitReader(maxSize)) {
    		splits.add(d);
    	}
    	return splits.toArray(new DirectMetricCollection[0]);
    }
    
    /**
     * Reallocated the memory block to be sized exactly for current size. (i.e. so that <b><code>capacity == size</code></b>)
     * @return this DMC
     */
    protected DirectMetricCollection shrinkWrap() {
    	if(capacity > size) {
    		address = unsafe.reallocateMemory(address, size);
    		capacity = size;    		
    	}
    	unsafe.putInt(address+SIZE_OFFSET, size);
    	return this;
    }
    
    
    /**
     * Returns the current size in bytes of this collection
	 * @return the current size in bytes of this collection
	 */
	public int getSize() {
		return unsafe.getInt(address+SIZE_OFFSET);
	}
	
	/**
	 * Updates the total size field
	 * @param s the new size
	 */
	private void setSize(int s) {
		unsafe.putInt(address+SIZE_OFFSET, s);
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
     * @param i the int value to write
     */
    protected void writeInt(int i) {
    	unsafe.putInt(address + size, i);
    	size += 4;
    }
    
    
    /**
     * Writes the passed byte to the memory block
     * @param b the byte to write
     */
    protected void writeByte(byte b) {
    	unsafe.putByte(address + size, b);
    	size += 1;
    }

    /**
     * <p>Creates a new DirectMetricCollection.
     * <p>This ctor writes some header bytes to the memory block.
     * <p><b>NOTE:</b> If you update the header, make sure you update {@link METRIC_OFFSET} accordingly !
     * @param initialCapacity The initial capacity of this DMC
     */
    private DirectMetricCollection(int initialCapacity) {
    	address = unsafe.allocateMemory(initialCapacity);
    	capacity = initialCapacity;
    	size = 0;
    	// the byte size of this DMC set during shrinkWrap()
    	writeInt(0);
    	// the number of metrics in this dmc
    	writeInt(0);  			
    	// the op code (0 for metrics)
    	writeByte(BYTE_ZERO);	
    	// The byte order of this message
    	writeByte(BYTE_ORDER); 
    	// set the initial size
    	setSize(size);
    }
    
    /**
     * Updates the count of metrics
     * @return the new metric count
     */
    private int updateCount() {    	
    	int cnt = unsafe.getInt(address+COUNT_OFFSET)+1;
    	unsafe.putInt(address+COUNT_OFFSET, cnt);
    	return cnt;
    }
    
    
    /**
     * Returns the number of metrics contained
     * @return the number of metrics contained
     */
    public synchronized int getMetricCount() {
    	if(address==0) {
    		throw new RuntimeException("Attempted to access destroyed DMA", new Throwable());
    	}
    	return unsafe.getInt(address + COUNT_OFFSET);
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
