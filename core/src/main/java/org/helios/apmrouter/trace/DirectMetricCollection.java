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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.helios.apmrouter.SenderOpCode;
import org.helios.apmrouter.metric.ICEMetric;
import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.metric.MetricType;
import org.helios.apmrouter.metric.catalog.ICEMetricCatalog;
import org.helios.apmrouter.metric.catalog.IDelegateMetric;
import org.helios.apmrouter.sender.ISender;
import org.helios.apmrouter.sender.Sender;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

import static org.helios.apmrouter.util.Methods.nvl;

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
    /** The offset from the address of the byte order byte */
    public static final int BYTE_ORDER_OFFSET = 1;
    
    /** The offset from the address where the DCM byte size is */
    public static final int SIZE_OFFSET = 2;
    /** The offset from the address where the DCM metric count is */
    public static final int COUNT_OFFSET = 6;
    
    /** Zero byte literal */
    public static final byte BYTE_ZERO = 0;
    /** One byte literal */
    public static final byte BYTE_ONE = 1;
    /** The byte order indicator */
    public static final byte BYTE_ORDER = ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN) ? BYTE_ZERO : BYTE_ONE;
    /** The reverse byte order indicator */
    public static final byte R_BYTE_ORDER = BYTE_ORDER==BYTE_ONE ? BYTE_ZERO : BYTE_ONE;
    
    /** The reverse byte order */
    public static final ByteOrder REV_BYTE_ORDER = ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN) ? ByteOrder.BIG_ENDIAN: ByteOrder.LITTLE_ENDIAN;
    
    

    /** the address of the direct memory allocation */
    protected long address = 0;
    /** the current size of this collection in bytes */
    protected int size = 0;
    /** the current capacity of this collection in bytes */
    protected int capacity = 0;
    
    /** The DMC sender */
    protected static volatile ISender sender;
    
    
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
    	if(sender==null) {
    		sender = Sender.getInstance().getDefaultSender();
    	}    	    	
    	DirectMetricCollection dmc = new DirectMetricCollection(initialCapacity);
    	for(IMetric m: metrics) {
    		dmc._append(m);
    	}
    	return dmc;
    }
    
    /**
     * Determines if the DMC is full
     * @param maxByteSize The maximum number of bytes
     * @param maxMetricCount The maximum number of metrics
     * @return true if the DMC is full, false otherwise
     */
    protected boolean isFull(int maxByteSize, int maxMetricCount) {
    	return (getSize()>=maxByteSize || getMetricCount()>=maxMetricCount);
    }
    
	/**
     * Loads a collection of IMetrics
     * @param maxByteSize The maximum number of bytes
     * @param maxMetricCount The maximum number of metrics  
     * @param metrics the metrics to load
     * @return true if the DMC is full after this operation, false otherwise
     */
    public boolean append(int maxByteSize, int maxMetricCount, Collection<IMetric> metrics) {
    	if(metrics!=null && !metrics.isEmpty()) {
        	for(IMetric metric: metrics) {
        		_append(metric);
        	}    		
    	}
    	return isFull(maxByteSize, maxMetricCount);
    }
    
    /**
     * Returns the current byte order
     * @return the current byte order
     */
    private byte getByteOrder() {
    	return unsafe.getByte(address + BYTE_ORDER_OFFSET);
    }
    
    /**
     * Reverses the byte order byte
     * @return this DMC
     */
    public DirectMetricCollection  reverseByteOrder() {
    	unsafe.putByte(address + BYTE_ORDER_OFFSET, unsafe.getByte(address + BYTE_ORDER_OFFSET)==BYTE_ZERO ? BYTE_ONE : BYTE_ZERO);
    	return this;
    }
     
	/**
     * Loads an array of IMetrics
     * @param maxByteSize The maximum number of bytes
     * @param maxMetricCount The maximum number of metrics 
     * @param metrics the metrics to load
     * @return true if the DMC is full after this operation, false otherwise
     */
    public boolean append(int maxByteSize, int maxMetricCount, IMetric...metrics) {
    	_check();
    	for(IMetric metric: metrics) {
    		_append(metric);
    	}
    	return isFull(maxByteSize, maxMetricCount);
    }
    
    /**
     * Overrides the opCode
     * @param opCode The op code to set to
     */
    public void setOpCode(SenderOpCode opCode) {
    	nvl(opCode, "SenderOpCode");
    	unsafe.putByte(address, opCode.op());
    }
    
    /**
     * Returns the currently set op code
     * @return the currently set op code
     */
    public SenderOpCode getOpCode() {
    	return SenderOpCode.valueOf(unsafe.getByte(address));
    }
    
    /**
     * Checks the memory allocation
     */
    private void _check() {
    	if(address==0) throw new RuntimeException("Call to DCM with unallocated address", new Throwable());
    }
    
    /**
     * <p>Sends this collection
     * {@inheritDoc}
     * @see java.lang.Runnable#run()
     */
    public void run() {
    	sender.send(this);
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
    	//int VS = 0;
    	// write the place-holder for the size
    	writeInt(0); //VS+= 4; 
    	// write the type code
    	writeByte((byte)metric.getType().ordinal()); //VS+= 1; 
    	if(token!=-1) {
    		writeByte(BYTE_ONE);  //VS+= 1;
    		writeLong(token); //VS+= 8;
    	} else {
    		writeByte(BYTE_ZERO);   //VS+= 1;  		    		
    		byte[] fqnBytes = metric.getFQN().getBytes();
    		writeInt(fqnBytes.length); //VS+= 4;
    		writeBytes(fqnBytes); //VS+= fqnBytes.length;
    	}
    	writeLong(metric.getTime()); //VS+= 8;
    	if(metric.getType().isLong()) {
    		writeLong(metric.getLongValue()); //VS+= 8; 
    	} else {
    		ByteBuffer bb = metric.getRawValue();
    		int sz = bb.limit();
    		writeInt(sz); //VS+= 4;
    		writeBytes(bb); //VS+= sz;     		
    	}
    	// ==========================================================
    	//		TXCONTEXT
    	// ==========================================================
    	if(metric.hasTXContext()) {
    		writeByte(BYTE_ONE); //VS+= 1;
    		TXContext tx = metric.getTXContext();
    		writeLong(tx.getTxId());
    		writeInt(tx.getTxQualifier());
    		writeInt(tx.getTxThreadId());
    		log("Attached TX Context " + metric.getTXContext());
    	} else {
    		writeByte(BYTE_ZERO); //VS+= 1;
    	}
    	// ==========================================================
    	int metricSize = size - currentSize;
    	unsafe.putInt(address + currentSize, metricSize);    
    	//log("Record Size:" + metricSize);
    	// update the DMC size
    	setSize(size);
    	//log("Metric Size:" + metricSize + " New Buff Size:" + size + "  Size Offset:" + currentSize + "  GS:" + getSize() + " RS:" + unsafe.getInt(address + currentSize));
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
     * Recreates a DirectMetricCollection from a ChannelBuffer
     * @param cb The ChannelBuffer to read the bytes from
     * @return the new DirectMetricCollection
     * FIXME: the order of ints and longs is wrong when we do this
     */
    public static DirectMetricCollection fromChannelBuffer(ChannelBuffer cb) {
    	ChannelBuffer rbuff = ChannelBuffers.directBuffer(cb.order().equals(ByteOrder.LITTLE_ENDIAN) ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN, cb.readableBytes());
    	rbuff.writeBytes(cb);
    	byte[] bytes = new byte[rbuff.readableBytes()];
    	rbuff.readBytes(bytes);
    	DirectMetricCollection d = new DirectMetricCollection(bytes.length);
    	unsafe.copyMemory(bytes, BYTE_ARRAY_OFFSET, null, d.address, bytes.length);
    	d.size = bytes.length;
    	d.setSize(bytes.length);
    	bytes = null;
    	return d;
    }
    
    /**
     * Writes this DMC to a direct {@link ChannelBuffer} and then destroys.
     * @return a loaded {@link ChannelBuffer}
     * TODO: Get rid of the call to ICEMetricCatalog. Need to store the type even if it is tokenized.
     */
    public ChannelBuffer toChannelBufferX() {
    	shrinkWrap();
    	byte[] bytes = new byte[size];
    	unsafe.copyMemory(null, address, bytes, BYTE_ARRAY_OFFSET, size);
    	destroy();
    	ChannelBuffer cb = ChannelBuffers.directBuffer(bytes.length);
    	cb.writeBytes(bytes);
    	return cb;
    }
    
    
    /**
     * Writes this DMC to a direct {@link ChannelBuffer} and then destroys.
     * @return a loaded {@link ChannelBuffer}
     * TODO: Get rid of the call to ICEMetricCatalog. Need to store the type even if it is tokenized.
     */
    public ChannelBuffer toChannelBuffer() {
    	shrinkWrap();
    	ChannelBuffer cb = ChannelBuffers.directBuffer(getSize());
    	// ===================================
    	// WRITE HEADER
    	// ===================================
    	// the op code (0 for metrics, 1 for direct metrics, etc.)
    	cb.writeByte(getOpCode().op());	
    	// The byte order of this message
    	cb.writeByte(BYTE_ORDER);
    	// The size of this message in bytes
    	cb.writeInt(getSize());
    	// the number of metrics in this dmc
    	cb.writeInt(getMetricCount());  			
    	// ===================================
    	// WRITE BODY
    	// ===================================
    	Reader r = new Reader();
    	
    	while(r._next()) {
    		int msize = r.readInt();
    		cb.writeInt(msize); // the size of this metric
    		byte typeOrdinal = r.readByte();
    		MetricType t = MetricType.valueOf(typeOrdinal);
    		cb.writeByte(typeOrdinal); 			// the metric type byte
    		byte isToken = r.readByte();  // the token indicator
    		cb.writeByte(isToken); 			// the token indicator
    		if(BYTE_ONE==isToken) {
    			long token  = r.readLong();
    			cb.writeLong(token); // the token
    		} else {
    			//byte[] fqn = r.readString().getBytes();
    			int fqnSize = r.readInt();
    			byte[] fqnBytes = r.readBytes(fqnSize);
    			cb.writeInt(fqnSize);    // the fqn length
    			cb.writeBytes(fqnBytes);    // the fqn 
    		}
    		cb.writeLong(r.readLong());
			if(t.isLong()) {
				cb.writeLong(r.readLong());
			} else {
				int byteLength = r.readInt();
				cb.writeInt(byteLength);			// the length of the next sequence of bytes
				ByteBuffer bb = r.readByteBuffer(byteLength);
				cb.writeBytes(bb);  // the value bytes
			}
			// ==========================================================
			//		TXCONTEXT
			// ==========================================================
			if(r.readByte()==BYTE_ONE) {
				long txId = r.readLong();
				int txQualifier = r.readInt();
				int txThreadId = r.readInt();
				
				cb.writeLong(txId);
				cb.writeInt(txQualifier);
				cb.writeInt(txThreadId);
				log("Wrote TXContext to ChannelBuffer" + String.format("[%s-%s-%s]", txId, txQualifier, txThreadId));
//				cb.writeBytes(r.readBytes(TXContext.TXCONTEXT_SIZE));txId, 
			}
    	}
//		byte[] bytes = new byte[size];
//		unsafe.copyMemory(null, (address), bytes, BYTE_ARRAY_OFFSET, size);
//		destroy();
//		cb.writeBytes(bytes);
//		logBits(cb);
//		logBits(bytes);
    	return cb;
    }
    
    
    private void logBits(ChannelBuffer cb) {
    	try {
	    	int size = cb.getInt(0);
	    	int count = cb.getInt(4);
	    	System.err.println("logBits [" + size + "/" + count + "]");
    	} catch (Exception e) {
    		e.printStackTrace(System.err);
    	}
    }

    
    private void logBits(byte[] bytes) {
    	try {
	    	ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
	    	DataInputStream dis = new DataInputStream(bais);
	    	int size = dis.readInt();
	    	int count = dis.readInt();
	    	System.err.println("logBits [" + size + "/" + count + "]");
    	} catch (Exception e) {
    		e.printStackTrace(System.err);
    	}
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
     * Copies memory in from another address
     * @param srcObject The source object
     * @param srcOffset The source object offset
     * @param byteCount The number of bytes to copy
     */
    protected void copyFrom(Object srcObject, long srcOffset, int byteCount) {
    	while(size + byteCount > capacity) extend();    
    	unsafe.copyMemory(srcObject, srcOffset, null, address, byteCount);
    	size += byteCount;
    	updateCount();
    	setSize(size);
    }    
    
    public static void log(Object msg) {
    	System.out.println(msg);
    }
    
    /**
     * <p>Title: Reader</p>
     * <p>Description: Base class for cursor controlled readers</p> 
     * <p>Company: Helios Development Group LLC</p>
     * @author Whitehead (nwhitehead AT heliosdev DOT org)
     * <p><code>org.helios.apmrouter.trace.DirectMetricCollection.Reader</code></p>
     */
    protected class Reader {
    	/** The current offset for this reader */
    	private int offset = METRIC_OFFSET;
    	/** The number of metrics to read */
    	private final int metricCount = getMetricCount();
    	/** The current cursor */
    	private int cursor = -1;
		/** The root address offset for the current record */
    	private int rootAddress = METRIC_OFFSET; 
		/** The byte size of the current record */
    	private int byteCount = -1;

		protected Reader() {
			byteCount = getRecordSize();
		}
		
		/**
		 * Returns the number of bytes in the current record
		 * @return the number of bytes in the current record
		 */
		public int getRecordByteSize() {			
			return byteCount;
		}
		
		/**
		 * Returns the memory offset of the first byte of the current record
		 * @return the memory offset of the first byte of the current record
		 */
		public int getRecordOffset() {
			return rootAddress;
		}
		
		/**
		 * Returns the Reader's cursor offset within the current record
		 * @return the Reader's cursor offset within the current record
		 */
		public int getReaderRecordOffset() {
			return offset;
		}
    	
		public int getRecordRemainingBytes() {
			return byteCount-offset+rootAddress;
		}
    	
		public boolean hasNext() {
			return cursor<metricCount-1;
			//        1			2
		}
		
		/**
		 * No Op. Not supported yet.
		 */		
		public void remove() {
			
		}
		
		/**
		 * Increments the offset to the next root address
		 * @return true if the offset was incremented, false if the cursor is EOF
		 * FIXME: After record 0 is run, (rootAddress + byteCount = offset) but is 3 more ON SERVER SIDE.
		 */
		protected boolean _next() {
			cursor++;
			if(cursor==metricCount) return false;
			if(cursor==0) return true;  // since we're already at the 0th record.
			// roll into the next record.
			rootAddress += byteCount;
			offset = rootAddress;
			byteCount = getRecordSize();
			return true;
		}
		
		/**
		 * Returns the size of the record without incrementing the counter
		 * @return the size of the record in bytes
		 */
		protected int getRecordSize() {
			//System.out.println("[" + Thread.currentThread() + "] Getting current record size [" + rootAddress + "], Cursor:" + cursor + " MetricCount:" + metricCount);
			int i = unsafe.getInt(address + rootAddress);
			return getByteOrder()==BYTE_ZERO ? i : Integer.reverseBytes(i);
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
			return getByteOrder()==BYTE_ZERO ? i : Integer.reverseBytes(i);
		}
		
		/**
		 * Reads a long
		 * @return the read long
		 */
		protected long readLong() {
			long v = unsafe.getLong(address + offset);
			offset += 8;
			return getByteOrder()==BYTE_ZERO ? v : Long.reverseBytes(v);
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
		 * @param bytesToRead the number of bytes to read
		 * @return a byte array
		 */
		protected byte[] readBytes(int bytesToRead) {
			byte[] bytes = new byte[bytesToRead];
			unsafe.copyMemory(null, (address + offset), bytes, BYTE_ARRAY_OFFSET, bytesToRead);
			offset += bytesToRead;
			return bytes;
		}
		
		/**
		 * Reads the specified number of bytes and returns them in a direct ByteBuffer
		 * @param bytesToRead the number of bytes to read
		 * @return a direct ByteBuffer
		 */
		protected ByteBuffer readByteBuffer(int bytesToRead) {
			ByteBuffer bb = ByteBuffer.allocateDirect(byteCount);
			bb.put(readBytes(bytesToRead));
			bb.flip();
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
		@SuppressWarnings("unused")
		@Override
		public IMetric next() {			
			if(!_next()) throw new RuntimeException("Iterator fetched pass EOF", new Throwable());
			IDelegateMetric dmetric = null;
    		int recordSize = readInt(); // skip size    	
    		byte typeOrdinal = readByte();
    		MetricType type = MetricType.valueOf(typeOrdinal);
    		byte isToken = readByte();
    		if(BYTE_ONE==isToken) {
    			long token =  readLong();
    			dmetric = ICEMetricCatalog.getInstance().get(token);    			
    		} else {    			
				String fqn = readString();
				dmetric = ICEMetricCatalog.getInstance().build(fqn, type);
    		}
    		long time = readLong();
    		ICEMetric metric = null;
			if(type.isLong()) {
				long value = readLong();
				metric = ICEMetric.newMetric(time, value, type, dmetric);
			} else {
				int bbSize = readInt();
				ByteBuffer bb = readByteBuffer(bbSize);
				metric = ICEMetric.newMetric(time, bb, type, dmetric);		
			}		
			// ==========================================================
			//      TXCONTEXT
			// ==========================================================
			byte tp = readByte();
			log("\nRead Byte: [" + tp + "]\nRemaining Bytes:" + getRecordRemainingBytes() + "\nValue:" + metric.getLongValue());
			if(getRecordRemainingBytes()>=TXContext.TXCONTEXT_SIZE) {		
				
				ByteBuffer bb = ByteBuffer.allocate(8).putLong(readLong());
				bb.flip();
				long txId = bb.getLong(); 
				int txQualifier = readInt();
				int txThreadId = readInt();
				TXContext tx = new TXContext(txId, txQualifier, txThreadId);
				metric.attachTXContext(tx);
				log("Attached TX Context " + metric.getTXContext());
			}
			// ==========================================================
			return metric;
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Iterable#iterator()
		 */
		@Override
		public Iterator<IMetric> iterator() {			
			return this;
		}
    }
    
    /**
     * <p>Title: SplitReader</p>
     * <p>Description: A reader implementation for breaking up a DMC into an array of DMCs, each with a maximum byte size.
     * This is intended to support transmissions using protocols with limited payload size.</p> 
     * <p>Company: Helios Development Group LLC</p>
     * @author Whitehead (nwhitehead AT heliosdev DOT org)
     * <p><code>org.helios.apmrouter.trace.DirectMetricCollection.SplitReader</code></p>
     */
    public class SplitReader extends Reader implements SplitDMC {
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
				int recordSize = getRecordByteSize();
				int recordOffset = getRecordOffset();
				if(recordSize<maxSize) {
					if(dmc.size + recordSize < maxSize) {
						dmc.copyFrom(address + recordOffset, recordSize);
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
    public SplitDMC newSplitReader(int maxSize) {
		if(maxSize>getSize()) {
			return new OneDMCReader(this);
		}
    	
    	return new SplitReader(maxSize);
    }
    
    public interface SplitDMC extends Iterator<DirectMetricCollection>, Iterable<DirectMetricCollection> {
       	/**
    	 * The droup count, which is metrics too large to serialize
		 * @return the number of dropped metrics
		 */
		public int getDrops();
    }
    
	private class OneDMCReader implements SplitDMC {
    	/** The single DMC */
    	private final List<DirectMetricCollection> dmc;
    	/** The single DMC iterator*/
    	private final Iterator<DirectMetricCollection> dmcIter;

    	/**
    	 * {@inheritDoc}
    	 * @see org.helios.apmrouter.trace.DirectMetricCollection.SplitDMC#getDrops()
    	 */
    	public int getDrops() {
    		return 0;
    	}
    	
    	/**
    	 * Creates a new OneDMCReader
    	 * @param dmc The DMC to wrap
    	 */
    	OneDMCReader(DirectMetricCollection dmc) {
    		this.dmc = Collections.singletonList(dmc);
    		dmcIter = this.dmc.iterator();
    	}

		@Override
		public boolean hasNext() {
			return dmcIter.hasNext();
		}

		@Override
		public DirectMetricCollection next() {
			return dmcIter.next();
		}

		@Override
		public void remove() {
			// No Op
			
		}

		@Override
		public Iterator<DirectMetricCollection> iterator() {
			return dmcIter;
		}
    	
    	

	}
    
    
    /**
     * Returns a MetricReader that can iteratively decode all the {@link IMetric}s in this DMC.
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
		int i = unsafe.getInt(address+SIZE_OFFSET);
		return getByteOrder()==BYTE_ZERO ? i : Integer.reverseBytes(i);
	}
	
	/**
	 * Updates the total size field
	 * @param i the new size
	 */
	private void setSize(int i) {
		unsafe.putInt(address+SIZE_OFFSET, getByteOrder()==BYTE_ZERO ? i : Integer.reverseBytes(i));
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
    		//size += bytes.length;
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
    	// the op code (0 for metrics)
    	writeByte(SenderOpCode.SEND_METRIC.op());	
    	// The byte order of this message
    	writeByte(BYTE_ORDER); 
    	// the byte size of this DMC set during shrinkWrap()
    	writeInt(0);
    	// the number of metrics in this dmc
    	writeInt(0);  			
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
    	int i = unsafe.getInt(address + COUNT_OFFSET);
    	return getByteOrder()==BYTE_ZERO ? i : Integer.reverseBytes(i);
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
