/**
 * ICE Futures, US
 */
package org.helios.apmrouter.metric;

import static org.helios.apmrouter.util.Methods.nvl;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.helios.apmrouter.util.IO;
import org.snmp4j.PDU;


/**
 * <p>Title: MetricType</p>
 * <p>Description: Enumerates the metricId type interpretations and provides accessor methods for the individual types</p> 
 * <p>Company: ICE Futures US</p>
 * @author Whitehead (nicholas.whitehead@theice.com)
 * @version $LastChangedRevision$
 * <p><code>org.helios.apmrouter.metric.MetricType</code></p>
 */

public enum MetricType  implements IMetricDataAccessor {
	/** Standard numeric metricId type */
	LONG(new LongMDA()),
	/** Locally maintained delta numeric metricId type */
	DELTA(new DeltaMDA()),
	/** A throwable handler metricId type */	
	ERROR(new ErrorMDA()),
	/** A char sequence type message metricId type */
	STRING(new StringMDA()),
	/** An SNMP PDU */
	PDU(new PduMDA()),	
	/** A catch all metricId type in the form of a byte array for everything else */
	BLOB(new BlobMDA());
	
	
	/** Map of MetricTypes keyed by the ordinal */
	public static final Map<Integer, MetricType> ORD2ENUM;
	
	static {
		Map<Integer, MetricType> tmp = new HashMap<Integer, MetricType>(MetricType.values().length);
		for(MetricType mt: MetricType.values()) {
			tmp.put(mt.ordinal(), mt);
		}
		ORD2ENUM = Collections.unmodifiableMap(tmp);
	}
	
	private MetricType(IMetricDataAccessor mda) {
		this.mda = mda;
	}
	
	/** The MDA for this metric type */
	private final IMetricDataAccessor mda;
	
	/**
	 * Returns this type's metric data accessor
	 * @return this type's metric data accessor
	 */
	public IMetricDataAccessor getDataAccessor() {
		return mda;
	}
	
	/** Indicates if direct byte buffers should be used */
	private static boolean direct = false;
	/** Indicates if byte buffers should compressed for non-numeric types */
	private static boolean compress = false;
	
	/**
	 * Determines if this metricId type is long based
	 * @return true if this metricId type is long based, false otherwise
	 */
	public boolean isLong() {
		return ordinal() <= DELTA.ordinal();
	}
	
	/**
	 * Decodes the passed ordinal to a MetricType.
	 * Throws a runtime exception if the ordinal is invalud
	 * @param ordinal The ordinal to decode
	 * @return the decoded MetricType
	 */
	public static MetricType valueOf(int ordinal) {
		MetricType mt = ORD2ENUM.get(ordinal);
		if(mt==null) throw new IllegalArgumentException("The passed ordinal [" + ordinal + "] is not a valid MetricType ordinal", new Throwable());
		return mt;
	}
	
	/**
	 * Decodes the passed ordinal to a MetricType.
	 * Throws a runtime exception if the ordinal is invalud
	 * @param ordinal The ordinal to decode
	 * @return the decoded MetricType
	 */
	public static MetricType valueOf(byte ordinal) {
		int ord = ordinal;
		return valueOf(ord);
	}
	
	/**
	 * Decodes the passed name to a MetricType.
	 * Throws a runtime exception if the ordinal is invalud
	 * @param name The metricId type name to decode. Trimmed and uppercased.
	 * @return the decoded MetricType
	 */
	public static MetricType valueOfName(CharSequence name) {
		String n = nvl(name, "MetricType Name").toString().trim().toUpperCase();
		try {
			return MetricType.valueOf(n);
		} catch (Exception e) {
			throw new IllegalArgumentException("The passed name [" + name + "] is not a valid MetricType name", new Throwable());
		}
	}
	
	/**
	 * <p>Title: LongMDA</p>
	 * <p>Description: The {@link IMetricDataAccessor} for long metricId types</p> 
	 * <p>Company: ICE Futures US</p>
	 * @author Whitehead (nicholas.whitehead@theice.com)
	 * @version $LastChangedRevision$
	 * <p><code>org.helios.apmrouter.metric.MetricType.LongMDA</code></p>
	 */
	private static class LongMDA implements IMetricDataAccessor<long[]> {
		private static final Class<?> REF_CLASS = Long.class;
		private static final long[] NULL_LONG = {0};
		
		protected MetricType getType() {
			return LONG;
		}
		
		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.metric.IMetricDataAccessor#write(java.lang.Object)
		 */
		@Override
		public ICEMetricValue write(long[] value) {			
			return new ICEMetricValue(getType(), value[0]);
		}
		
		
		
		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.metric.IMetricDataAccessor#writeObject(java.lang.Object)
		 */
		@Override
		public ICEMetricValue writeObject(Object value) {
			long actual = 0;
			if(value==null) {
				actual = 0;
			} else if(REF_CLASS.equals(value.getClass())) {
				actual = (Long)value;
			} else if(value instanceof Number) {
				actual = ((Number)value).longValue();
			} else {
				try { actual = new Double(value.toString().trim()).longValue(); } catch (Exception e) {
					actual = 0;
				}
			}
			return new ICEMetricValue(getType(), actual);
		}
		
		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.metric.IMetricDataAccessor#read(org.helios.apmrouter.metric.ICEMetricValue)
		 */
		@Override
		public long[] read(ICEMetricValue metricValue) {
			return new long[]{metricValue.getLongValue()};
		}		
	}
	
	/**
	 * <p>Title: DeltaMDA</p>
	 * <p>Description: The {@link IMetricDataAccessor} for delta metricId types</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.metric.MetricType.DeltaMDA</code></p>
	 */
	private static class DeltaMDA extends LongMDA {
		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.metric.MetricType.LongMDA#getType()
		 */
		@Override
		protected MetricType getType() {
			return DELTA;
		}
		
		
	}
	
	/**
	 * <p>Title: StringMDA</p>
	 * <p>Description: The {@link IMetricDataAccessor} for string metricId types</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.metric.MetricType.StringMDA</code></p>
	 */
	private static class StringMDA implements IMetricDataAccessor<CharSequence> {
		private static final Class<?> REF_CLASS = CharSequence.class;
		private static final ByteBuffer NULL_BYTES = ByteBuffer.wrap(new byte[]{});
		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.metric.IMetricDataAccessor#write(java.lang.Object)
		 */
		@Override
		public ICEMetricValue write(CharSequence value) {
			return new ICEMetricValue(STRING, value==null ? NULL_BYTES : allocate(value.toString().getBytes()));
		}
		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.metric.IMetricDataAccessor#writeObject(java.lang.Object)
		 */
		@Override
		public ICEMetricValue writeObject(Object value) {
			return new ICEMetricValue(STRING, value==null ? NULL_BYTES : allocate(value.toString().getBytes()));
		}
		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.metric.IMetricDataAccessor#read(org.helios.apmrouter.metric.ICEMetricValue)
		 */
		@Override
		public CharSequence read(ICEMetricValue metricValue) {
			ByteBuffer buff = metricValue.getValue();
			buff.flip();
			return buff.asCharBuffer().toString();
		}
	}
	
	/**
	 * <p>Title: ErrorMDA</p>
	 * <p>Description: The {@link IMetricDataAccessor} for error metricId types</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.metric.MetricType.ErrorMDA</code></p>
	 */
	private static class ErrorMDA implements IMetricDataAccessor<Throwable> {
		private static final ByteBuffer NULL_BYTES = ByteBuffer.wrap(new byte[]{});
		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.metric.IMetricDataAccessor#write(java.lang.Object)
		 */
		@Override
		public ICEMetricValue write(Throwable value) {			
			Throwable et = new Throwable("Throwable Stub. Type [" + value.getClass().getName() + "] Message [" + value.getMessage() + "]");
			et.setStackTrace(value.getStackTrace());
			return new ICEMetricValue(ERROR, value==null ? NULL_BYTES : IO.writeToByteBuffer(et, direct, false));
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.metric.IMetricDataAccessor#writeObject(java.lang.Object)
		 */
		@Override
		public ICEMetricValue writeObject(Object value) {			
			return write((Throwable)value);
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.metric.IMetricDataAccessor#read(org.helios.apmrouter.metric.ICEMetricValue)
		 */
		@Override
		public Throwable read(ICEMetricValue metricValue) {
			return getThrowable(metricValue.getValue());
		}
		
		/**
		 * Reads a throwable from the byte buffer, returning contrived place-holders if the bytebuffer's payload is zero or deserialization fails.
		 * @param buff The byte buffer to read from
		 * @return A throwable
		 */
		protected Throwable getThrowable(final ByteBuffer buff) {
			buff.flip();
			if(buff.capacity()<1) return new Throwable("Null Throwable");
			return (Throwable)IO.readFromByteBuffer(buff);
		}
		
//		private final AtomicLong exec = new AtomicLong(0L);
//		private final AtomicLong alt = new AtomicLong(0L);
		
		/**
		 * Converts the passed throwable to a byte array. If the standard java serialization fails,
		 * creates a contrived exception using the stack trace as the error message.
		 * @param t the throwable to serialize
		 * @return a byte array
		 */
		protected byte[] getBytes(Throwable t) {			
			ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
			ObjectOutputStream oos = null;
			long start = 0L;
//			boolean trace = exec.incrementAndGet()%1000000==0;
//			if(trace) start = System.nanoTime();
			try {
				oos = new ObjectOutputStream(baos);
				oos.writeObject(t);
				oos.flush();
				//baos.flush();
			} catch (Exception e) {
//				alt.incrementAndGet();
				Throwable et = new Throwable("Non-Serializable Throwable Stub. Type [" + t.getClass().getName() + "] Message [" + t.getMessage() + "]");
				et.setStackTrace(t.getStackTrace());
				return getBytes(t);
			} finally {
				try { oos.close(); } catch (Exception ex) {}
				try { baos.close(); } catch (Exception ex) {}
			}
			byte[]  bytes = baos.toByteArray();
//			if(trace) {
//				long elapsed = System.nanoTime()-start;
//				long execCount = exec.get();
//				long altCount = alt.get();
//				if(execCount>BaseTracingOperations.TOTAL_EXEC_COUNT) {
//					System.err.println("Throwable Ser Elapsed:" + elapsed + " ns.  " + TimeUnit.MILLISECONDS.convert(elapsed, TimeUnit.NANOSECONDS)  + " ms.  Size:" + bytes.length + " Invoke Count:" + execCount + " Overage:" + (execCount-BaseTracingOperations.TOTAL_EXEC_COUNT) + " Alt:" + altCount);
//				} else {
//					System.out.println("Throwable Ser Elapsed:" + elapsed + " ns.  " + TimeUnit.MILLISECONDS.convert(elapsed, TimeUnit.NANOSECONDS)  + " ms.  Size:" + bytes.length + " Invoke Count:" + execCount  + " Alt:" + altCount);
//				}
//				//new Throwable().printStackTrace(System.err);
//			}
			return bytes;
		}		
	}
	
	private static class BlobMDA implements IMetricDataAccessor<Serializable> {

		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.metric.IMetricDataAccessor#write(java.lang.Object)
		 */
		@SuppressWarnings("synthetic-access")
		@Override
		public ICEMetricValue write(Serializable value) {
			return new ICEMetricValue(BLOB, IO.writeToByteBuffer(value, direct, compress));
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.metric.IMetricDataAccessor#writeObject(java.lang.Object)
		 */
		@Override
		public ICEMetricValue writeObject(Object value) {			
			return new ICEMetricValue(BLOB, IO.writeToByteBuffer(value, direct, compress));
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.metric.IMetricDataAccessor#read(org.helios.apmrouter.metric.ICEMetricValue)
		 */
		@Override
		public Serializable read(ICEMetricValue metricValue) {
			return (Serializable)IO.readFromByteBuffer(metricValue.getValue());
		}		
	}
	
	private static class PduMDA implements IMetricDataAccessor<PDU> {
		/** TODO:  Replace IO.writeToBuffer with BEROutputStream */
		@Override
		public ICEMetricValue write(org.snmp4j.PDU value) {
			return new ICEMetricValue(PDU, IO.writeToByteBuffer(value, direct, compress));			
		}

		@Override
		public ICEMetricValue writeObject(Object value) {
			return new ICEMetricValue(PDU, IO.writeToByteBuffer(value, direct, compress));
		}

		@Override
		public org.snmp4j.PDU read(ICEMetricValue metricValue) {
			return (PDU)IO.readFromByteBuffer(metricValue.getValue());
		}
		
	}
	
	/**
	 * Compresses a value payload byte array in a metricId in accordance with the {@link MetricType#isCompress()} option
	 * @param payload The byte array to compress
	 * @return The compressed payload
	 */
	public static byte[] compress(byte[] payload) {
		if(!compress) return payload;
		if(payload==null) return new byte[]{};
		ByteArrayOutputStream baos = null;
		GZIPOutputStream gzos = null;
		try {
			baos = new ByteArrayOutputStream(payload.length); 
			gzos =  new GZIPOutputStream(baos);
			gzos.write(payload);			
			gzos.close();
			return baos.toByteArray();
		} catch (Exception e) {
			return payload;
		} 
	}
	
	/**
	 * Allocates byte buffers in accordance with the {@link MetricType#isDirect()} option
	 * @param size The allocation size
	 * @return the allocated byte buffer
	 */
	public static ByteBuffer allocate(int size) {
		return direct ? ByteBuffer.allocateDirect(size) : ByteBuffer.allocate(size);  
	}
	
	/**
	 * Allocates byte buffers in accordance with the {@link MetricType#isDirect()} option
	 * @param bytes The bytes to allocate a ByteBuffer for
	 * @return the allocated byte buffer
	 */
	public static ByteBuffer allocate(byte[] bytes) {
		return direct ? ByteBuffer.allocateDirect(bytes.length).put(bytes) : ByteBuffer.wrap(bytes); 
	}
	
	
	/**
	 * Returns the passed long in the form of a little endian formatted byte array 
	 * @param payloadLength The long value to encode
	 * @return an byte array
	 */
	public static byte[] encodeLittleEndianLongBytes(long payloadLength) {
		return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(payloadLength).array();
	}
	
	/**
	 * Decodes the little endian encoded bytes to a long
	 * @param bytes The bytes to decode
	 * @return the decoded long value
	 */
	public static long decodeLittleEndianLongBytes(byte[] bytes) {
		return ((ByteBuffer) ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).put(bytes).flip()).getLong();
	}

	/**
	 * Indicates if metricId value byte buffers are allocated direct or on heap
	 * @return true if metricId value byte buffers are allocated direct, false if they allocated on heap
	 */
	public static boolean isDirect() {
		return direct;
	}

	/**
	 * Sets if metricId value byte buffers are allocated direct or on heap
	 * @param direct true if metricId value byte buffers are allocated direct, false if they allocated on heap
	 */
	public static void setDirect(boolean direct) {
		MetricType.direct = direct;
	}

	/**
	 * Indicates if non-numeric metricId values are being compressed
	 * @return true if values are compressed, false otherwise
	 */
	public static boolean isCompress() {
		return compress;
	}

	/**
	 * Sets the compression option for non-numeric metricId values 
	 * @param compress true to compress, false otherwise
	 */
	public static void setCompress(boolean compress) {
		MetricType.compress = compress;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetricDataAccessor#write(java.lang.Object)
	 */
	@Override
	public ICEMetricValue write(Object value) {
		return mda.write(value);
	}
	
	public ICEMetricValue write(long value) {
		if(isLong()) {
			return ((LongMDA)mda).write(new long[]{value});
		}
		return mda.write(value);
	}
	
	public ICEMetricValue write(Number value) {
		
		if(isLong() && value!=null) {
			return ((LongMDA)mda).write(new long[]{value.longValue()});
		}
		return mda.write(value);
	}
	
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetricDataAccessor#writeObject(java.lang.Object)
	 */
	@Override
	public ICEMetricValue writeObject(Object value) {
		return mda.writeObject(value);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetricDataAccessor#read(org.helios.apmrouter.metric.ICEMetricValue)
	 */
	@Override
	public Object read(ICEMetricValue metricValue) {
		if(isLong()) {
			return ((LongMDA)mda).read(metricValue)[0];
		}
		return mda.read(metricValue);
	}

	
}


/*
		@Override
		public ByteBuffer write(long...value) {
			long actual = (value==null || value.length<1) ? 0 : value[0];
			ByteBuffer bb = allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(actual);
			bb.flip();
			return bb;			
		}
		@Override
		public ByteBuffer writeObject(Object value) {
			long actual = 0;
			if(value==null) {
				actual = 0;
			} else if(REF_CLASS.equals(value.getClass())) {
				actual = (Long)value;
			} else if(value instanceof Number) {
				actual = ((Number)value).longValue();
			} else {
				try { actual = new Double(value.toString().trim()).longValue(); } catch (Exception e) {
					actual = 0;
				}
			}
			ByteBuffer bb = allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(actual);
			bb.flip();
			return bb;
		}

		@Override
		public long[] read(ByteBuffer metricBuffer) {
			if(metricBuffer==null) return NULL_LONG;
			metricBuffer.rewind();
			return new long[]{metricBuffer.order(ByteOrder.LITTLE_ENDIAN).getLong()};
		}		

 */
