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
package org.helios.apmrouter.timeseries;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Random;

import org.helios.apmrouter.util.SystemClock;
import org.helios.apmrouter.util.SystemClock.ElapsedTime;

/**
 * <p>Title: H2MetricValueDomain</p>
 * <p>Description: A custom user data type for H2 that stores a fixed window of time-series values</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.timeseries.H2MetricValueDomain</code></p>
 */

public class H2MetricValueDomain implements Externalizable {
	/** The step size in ms. */
	protected long step;
	/** The width, or number of entries in the window */
	protected int width;
	/** The current number of entries in the window */
	protected int size = -1;
	
	/** The value store */
	protected ByteBuffer store = null;
	
	/** The array index for the period */
	public static final int PERIOD = 0;
	/** The array index for the min value */
	public static final int MIN = 1;
	/** The array index for the max value */
	public static final int MAX = 2;
	/** The array index for the avg value */
	public static final int AVG = 3;
	/** The array index for the count value */
	public static final int CNT= 4;

	/** The size of each entry, that is 4 longs and an int */
	public static final int ENTRY_SIZE = (4*8) + 4;
	
	
	/**
	 * Creates a new H2MetricValueDomain.
	 * For externalizable only.
	 */
	public H2MetricValueDomain() {
		
	}
	
	/**
	 * Creates a new H2MetricValueDomain
	 * @param step The step size in ms.
	 * @param width The width, or number of entries in the window
	 * @param sticky Indicates if the metric is sticky
	 * @return a new H2MetricValueDomain
	 */
	public static H2MetricValueDomain make(long step, int width, boolean sticky) {
		return new H2MetricValueDomain(step, width);
	}
	
	/**
	 * Tests a byte array to see if it is a valid H2MetricValueDomain
	 * @param data The byte array to test
	 * @return true if the array is a valid H2MetricValueDomain
	 * @throws Exception thrown if the byte array is invalid
	 */
	public static boolean isType(byte[] data) throws Exception {
		if(data==null) return false;
		try {
			deserialize(data);
			return true;
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			throw ex;
		}		
	}
	
	public static H2MetricValueDomain add(byte[] data, long timestamp, long value) throws Exception {
		H2MetricValueDomain mvd = deserialize(data);
		mvd.addValue(timestamp, value);
		return mvd;
	}
	
	/**
	 * Creates a new H2MetricValueDomain
	 * @param step The step size in ms.
	 * @param width The width, or number of entries in the window
	 */
	public H2MetricValueDomain(long step, int width) {
		super();
		this.step = step;
		this.width = width-1;
		store = ByteBuffer.allocateDirect(storeByteSize());
	}
	
	/**
	 * Returns the size of the store in bytes based on the width of the time-series window
	 * @return the number of bytes in the store.
	 */
	public int storeByteSize() {
		return (width+1) * ENTRY_SIZE;
	}
	
	public static void main(String[] args) {
		log("Domain MetricValue Test");
		H2MetricValueDomain d = new H2MetricValueDomain(1000, 10);
		Random random = new Random(System.currentTimeMillis());
		try {
			for(int x = 0; x < 20; x++) {
				SystemClock.startTimer();
				for(int i = 0; i < 1000; i++) {
					d.addValue(SystemClock.time(), Math.abs(random.nextInt(1000)+1));
				}
				ElapsedTime et = SystemClock.endTimer();
				log(d);
				log(et + "Per:" + et.avgNs(1000));
				Thread.sleep(1000);
				d = deserialize(serialize(d));
			}

		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
	
	public int byteSize() {
		return (8+4+4) + ((size+1) * ENTRY_SIZE) + 7; 
	}
	
	
	
	
	
	
	/**
	 * Adds a value to the time-series window
	 * @param timestamp The timestamp in long UTC
	 * @param value The long value to add
	 */
	public synchronized void addValue(long timestamp, long value) {
		final long period = getPeriod(timestamp);
		store.position(0);
		if(size<0) {			
			size++;
			put(new long[]{period, value, value, value, 1});
		} else {			
			final long[] values = getArray(size);			
			final boolean update = values[PERIOD]==period;
			final long[] newValues;
			if(update) {
				store.position(size * ENTRY_SIZE);
				newValues = calcValue(values, period, value);
				put(newValues);
			} else {
				if(size<width) {
					size++;
					store.position(size * ENTRY_SIZE);
					log("Rolled to slot [" + size + "] Pos:[" + store.position() + "]");					
				} else {
					store.position(ENTRY_SIZE);
					store.compact();
					store.position(size * ENTRY_SIZE);
					log("Compacted. Size: [" + size + "] Pos:[" + store.position() + "]");
				}
				
				newValues = calcValue(null, period, value);
				put(newValues);
			}
			
		}
	}
	
	/**
	 * Stored the passed positional array into the current store position
	 * @param values The array of values
	 */
	protected void put(long[] values) {
		store.putLong(values[PERIOD]);
		store.putLong(values[MIN]);
		store.putLong(values[MAX]);
		store.putLong(values[AVG]);
		store.putInt((int)values[CNT]);		
	}
	
	/**
	 * Returns the values at the specified position
	 * @param position The time-series slot position
	 * @return The values in the positioned slot
	 */
	protected long[] getArray(int position) {
		ByteBuffer buff = store.duplicate();
		buff.position((position) * ENTRY_SIZE);
		long[] arr = new long[5];
		arr[PERIOD] = buff.getLong();
		arr[MIN] = buff.getLong();
		arr[MAX] = buff.getLong();
		arr[AVG] = buff.getLong();
		arr[CNT] = buff.getInt();
		return arr;
	}
	
	
	/**
	 * Calculates the new value range
	 * @param current The current array or null if there is none
	 * @param period The period this value will be allocated to
	 * @param newValue The submitted value
	 * @return The new value array
	 */
	protected long[] calcValue(long[] current, long period, long newValue) {		
		if(current==null) {
			return new long[]{period, newValue, newValue, newValue, 1};
		}
		if(newValue<current[MIN]) current[MIN] = newValue;
		if(newValue>current[MAX]) current[MAX] = newValue;
		current[AVG] = current[AVG]==0 ? newValue : (current[AVG]+newValue)/2;
		current[CNT] = current[CNT]+1; 
		return current;
	}
	
	/**
	 * Returns the current period
	 * @param timestamp The timestamp to get the period for
	 * @return the period
	 */
	public long getPeriod(long timestamp) {
		return (timestamp - (timestamp%step));
	}
	
	/**
	 * Returns the step in ms. 
	 * @return the step
	 */
	public long getStep() {
		return step;
	}

	/**
	 * Returns the width of the time-series window
	 * @return the width
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * Returns the current number of items in the window
	 * @return the size
	 */
	public int getSize() {
		return size;
	}

	/**
	 * Serializes a H2MetricValueDomain to a byte array
	 * @param mvd The H2MetricValueDomain to serialize
	 * @return A byte array
	 * @throws IOException Thrown on any io exception
	 */
	public static byte[] serialize(H2MetricValueDomain mvd) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(mvd.byteSize());
		ObjectOutputStream oos = new ObjectOutputStream(baos); 
		oos.writeObject(mvd);
		oos.flush();
		baos.flush();
		return baos.toByteArray();
	}
	
	/**
	 * Deserializes a H2MetricValueDomain from a byte array
	 * @param arr The byte array to deserialize from
	 * @return The deserialized H2MetricValueDomain 
	 * @throws IOException thrown on any io exception
	 * @throws ClassNotFoundException Will not be thrown.
	 */
	public static H2MetricValueDomain deserialize(byte[] arr) throws IOException, ClassNotFoundException {
		ByteArrayInputStream bais = new ByteArrayInputStream(arr);
		ObjectInputStream ois = new ObjectInputStream(bais);
		return (H2MetricValueDomain) ois.readObject();
	}
	

	/**
	 * {@inheritDoc}
	 * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
	 */
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeLong(step);
		out.writeInt(width);
		out.writeInt(size);		
		ByteBuffer buff = store.duplicate();
		buff.position(0);
		for(int i = 0; i <= size; i++) {
			// ts
			out.writeLong(buff.getLong());
			// min
			out.writeLong(buff.getLong());
			// max
			out.writeLong(buff.getLong());
			// avg
			out.writeLong(buff.getLong());
			// count
			out.writeInt(buff.getInt());
		}
	}

	/**
	 * {@inheritDoc}
	 * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
	 */
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		step = in.readLong();
		width = in.readInt();
		size = in.readInt();
		store = ByteBuffer.allocateDirect(storeByteSize());
		store.position(0);
		for(int i = 0; i <= size; i++) {
			// ts
			store.putLong(in.readLong());
			// min
			store.putLong(in.readLong());
			// max
			store.putLong(in.readLong());
			// avg
			store.putLong(in.readLong());
			// count
			store.putInt(in.readInt());
		}
		log("Read In. Buff:" + store + " Step:" + step + " Width:" + width + " Size:" + size);		
		 
	}

	/**
	 * Renders a slot entry as a string
	 * @param arr The entry to render
	 * @return the rendered entry
	 */
	protected String entryToString(long[] arr) {
		StringBuilder b = new StringBuilder();
		b.append("[").append(new Date(arr[PERIOD])).append("]:");
		b.append("[").append(arr[CNT]).append("]");
		b.append(" min:").append(arr[MIN]);
		b.append(" max:").append(arr[MAX]);
		b.append(" avg:").append(arr[AVG]);
		return b.toString();
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("MetricValue[step:").append(step).append(" width:").append(width).append("]");
		for(int i = 0; i <= size; i++) {
			b.append("\n\t").append(entryToString(getArray(i)));
		}
		return b.toString();
	}
	
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	
	/**
	 * <p>Title: CompactObjectOutputStream</p>
	 * <p>Description: Custom {@link ObjectOutputStream} that writes no class descriptor</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.timeseries.H2MetricValueDomain.CompactObjectOutputStream</code></p>
	 */
	protected static class CompactObjectOutputStream extends ObjectOutputStream {

		/**
		 * Creates a new CompactObjectOutputStream
		 * @param out The output stream
		 * @throws IOException thrown on any io exception
		 */
		public CompactObjectOutputStream(OutputStream out) throws IOException {
			super(out);
			enableReplaceObject(false);
		}
		
	    /**
	     * {@inheritDoc}
	     * @see java.io.ObjectOutputStream#writeStreamHeader()
	     */
	    @Override
	    protected void writeStreamHeader() throws IOException {
	    }

	    /**
	     * {@inheritDoc}
	     * @see java.io.ObjectOutputStream#writeClassDescriptor(java.io.ObjectStreamClass)
	     */
	    @Override
	    protected void writeClassDescriptor(ObjectStreamClass desc) throws IOException {
	    	
	    }
	    
		
	}
	
	/**
	 * <p>Title: CompactObjectInputStream</p>
	 * <p>Description: Custom {@link ObjectInputStream} that knows the class descriptor</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.timeseries.H2MetricValueDomain.CompactObjectInputStream</code></p>
	 */
	protected static class CompactObjectInputStream extends ObjectInputStream {
		public static final ObjectStreamClass OSC;
		
		static {
			try {
				OSC = ObjectStreamClass.lookup(H2MetricValueDomain.class);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		/**
		 * Creates a new CompactObjectInputStream
		 * @param in The input stream
		 * @throws IOException Thrown on any io exception
		 */
		public CompactObjectInputStream(InputStream in) throws IOException {
			super(in);
		}
		
	    /**
	     * @throws IOException
	     */
	    @Override
	    protected void readStreamHeader() throws IOException {
	    }

	    /**
	     * @return
	     * @throws IOException
	     * @throws ClassNotFoundException
	     */
	    @Override
	    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
	    	return OSC;
	    }
		
		
	}
	
}
