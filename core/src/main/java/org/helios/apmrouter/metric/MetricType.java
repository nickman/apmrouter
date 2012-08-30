/**
 * ICE Futures, US
 */
package org.helios.apmrouter.metric;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.GZIPOutputStream;

/**
 * <p>Title: MetricType</p>
 * <p>Description: Enumerates the metric type interpretations and provides accessor methods for the individual types</p> 
 * <p>Company: ICE Futures US</p>
 * @author Whitehead (nicholas.whitehead@theice.com)
 * @version $LastChangedRevision$
 * <p><code>org.helios.apmrouter.metric.MetricType</code></p>
 */

public enum MetricType {
	/** Standard numeric metric type */
	LONG,
	/** Locally maintained delta numeric metric type */
	DELTA,
	/** A throwable handler metric type */	
	ERROR,
	/** A char sequence type message metric type */
	STRING,
	/** A catch call metric type in the form of a byte array for everything else */
	BLOB;
	
	/** Indicates if direct byte buffers should be used */
	private static boolean direct = false;
	/** Indicates if byte buffers should compressed for non-numeric types */
	private static boolean compress = false;
	
	/**
	 * <p>Title: LongMDA</p>
	 * <p>Description: The {@link IMetricDataAccessor} for long metric types</p> 
	 * <p>Company: ICE Futures US</p>
	 * @author Whitehead (nicholas.whitehead@theice.com)
	 * @version $LastChangedRevision$
	 * <p><code>org.helios.apmrouter.metric.MetricType.LongMDA</code></p>
	 */
	private static class LongMDA implements IMetricDataAccessor<long[]> {
		private static final Class<?> REF_CLASS = Long.class;
		private static final long[] NULL_LONG = {0};
		
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
	}
	
	/**
	 * Compresses a value payload byte array in a metric in accordance with the {@link MetricType#isCompress()} option
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
	 * Indicates if metric value byte buffers are allocated direct or on heap
	 * @return true if metric value byte buffers are allocated direct, false if they allocated on heap
	 */
	public static boolean isDirect() {
		return direct;
	}

	/**
	 * Sets if metric value byte buffers are allocated direct or on heap
	 * @param direct true if metric value byte buffers are allocated direct, false if they allocated on heap
	 */
	public static void setDirect(boolean direct) {
		MetricType.direct = direct;
	}

	/**
	 * Indicates if non-numeric metric values are being compressed
	 * @return true if values are compressed, false otherwise
	 */
	public static boolean isCompress() {
		return compress;
	}

	/**
	 * Sets the compression option for non-numeric metric values 
	 * @param compress true to compress, false otherwise
	 */
	public static void setCompress(boolean compress) {
		MetricType.compress = compress;
	}
	
}
