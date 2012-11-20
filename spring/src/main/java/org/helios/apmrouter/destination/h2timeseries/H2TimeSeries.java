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
package org.helios.apmrouter.destination.h2timeseries;

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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.h2.tools.SimpleResultSet;
import org.helios.apmrouter.util.SystemClock;
import org.helios.apmrouter.util.SystemClock.ElapsedTime;

/**
 * <p>Title: H2TimeSeries</p>
 * <p>Description: A custom user data type for H2 that stores a fixed window of time-series values</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.h2timeseries.H2TimeSeries</code></p>
 */


public class H2TimeSeries implements Externalizable { 
	/**  */
	private static final long serialVersionUID = -963955749386799856L;
	/** Static class logger */
	protected static final Logger LOG = Logger.getLogger(H2TimeSeries.class);
	/** The STEP size in ms. */
	protected long step;
	/** The WIDTH, or number of entries in the window */
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
	
	/** A counter of serialization reads */
	private static final AtomicLong SerializationReads = new AtomicLong(0L);
	/** A counter of serialization writes */
	private static final AtomicLong SerializationWrites = new AtomicLong(0L);
	/** The timestamp of the last serialization metric reset */
	private static final AtomicLong LastReset = new AtomicLong(System.currentTimeMillis());
	
	/**
	 * Returns the number of Serialization Reads since the last metric reset
	 * @return The number of Serialization Reads since the last metric reset
	 */
	public static long getSerializationReads() {
		return SerializationReads.get();
	}
	
	/**
	 * Returns the number of Serialization Writes since the last metric reset
	 * @return The number of Serialization Writes since the last metric reset
	 */
	public static long getSerializationWrites() {
		return SerializationWrites.get();
	}
	
	/**
	 * Returns the UTC long timestamp of the last serialization metric reset
	 * @return the UTC long timestamp of the last serialization metric reset
	 */
	public static long getLastResetTimestamp() {
		return LastReset.get();
	}
	
	/**
	 * Returns the date of the last serialization metric reset
	 * @return the date of the last serialization metric reset
	 */
	public static Date getLastResetDate() {
		return new Date(LastReset.get());
	}
	
	/**
	 * Resets the serialization metrics and sets the last reset timestamp to current.
	 */
	public static void resetSerializationMetrics() {
		SerializationReads.set(0L);
		SerializationWrites.set(0L);
		LastReset.set(System.currentTimeMillis());
	}
	
	
	/**
	 * Creates a new H2TimeSeries.
	 * For externalizable only.
	 */
	public H2TimeSeries() {
		
	}
	
	/**
	 * Creates a new H2TimeSeries
	 * @param STEP The STEP size in ms.
	 * @param WIDTH The WIDTH, or number of entries in the window
	 * @param sticky Indicates if the metric is sticky
	 * @return a new H2TimeSeries
	 */
	public static H2TimeSeries make(long step, int width, boolean sticky) {
		return new H2TimeSeries(step, width);
	}
	
	/**
	 * Creates a new H2TimeSeries and adds a new value
	 * @param conn The connection to lookup the existing time-series
	 * @param STEP The STEP size in ms.
	 * @param WIDTH The WIDTH, or number of entries in the window
	 * @param sticky Indicates if the metric is sticky
	 * @param id The metric ID of the metric to upsert a time-series entry for
	 * @param ts The timestamp of the value to add
	 * @param value  The value to add
	 * @return an updated H2TimeSeries
	 * @throws Exception Thrown on any error
	 */
	public static H2TimeSeries make_and_add(Connection conn, long step, int width, boolean sticky, long id, Timestamp ts, long value) throws Exception {
		PreparedStatement ps = null;
		ResultSet rset = null;		
		H2TimeSeries mvd = null;
		try {
			ps = conn.prepareStatement("SELECT V FROM METRIC_VALUES WHERE ID = ?");
			ps.setLong(1, id);
			rset = ps.executeQuery();
			if(rset.next()) {
				mvd = (H2TimeSeries)rset.getObject(1);
			} else {
				mvd = new H2TimeSeries(step, width);
			}
			mvd.addValue(ts.getTime(), value);
			return mvd;
		} finally {
			if(rset!=null) try { rset.close(); } catch (Exception ex) {}
			if(ps!=null) try { ps.close(); } catch (Exception ex) {}
		}
	}	
	
	/**
	 * Tests a byte array to see if it is a valid H2TimeSeries
	 * @param data The byte array to test
	 * @return true if the array is a valid H2TimeSeries
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
	
	/**
	 * Adds a value to the H2TimeSeries deserialized from the passed byte array
	 * @param data The byte array to be desrialized into a H2TimeSeries
	 * @param timestamp The effective timestamp of the data to be added
	 * @param value The data to be added
	 * @return the updated H2TimeSeries
	 * @throws Exception thrown on any error
	 */
	public static H2TimeSeries add(byte[] data, Timestamp timestamp, long value) throws Exception {
		H2TimeSeries mvd = deserialize(data);
		mvd.addValue(timestamp.getTime(), value);
		return mvd;
	}
	
	//public static ResultSet allvalues(byte[] data, Timestamp start, Timestamp end) throws Exception {
	
	public static ResultSet allvalues(byte[] data) throws Exception {
		H2TimeSeries mvd = deserialize(data);
		SimpleResultSet rs = new SimpleResultSet();
	    rs.addColumn("TS", Types.TIMESTAMP, 1, 22);
	    rs.addColumn("MIN", Types.NUMERIC, 255, 22);
	    rs.addColumn("MAX", Types.NUMERIC, 255, 22);
	    rs.addColumn("AVG", Types.NUMERIC, 255, 22);
	    rs.addColumn("CNT", Types.NUMERIC, 255, 22);
	    for(int i = 0; i <= mvd.size; i++) {
	    	long[] row = mvd.getArray(i);
	    	if(row==null) continue;
	    	rs.addRow( 
	    			new java.sql.Timestamp(row[PERIOD]), 
	    			row[MIN], 
	    			row[MAX], 
	    			row[AVG], 
	    			row[CNT]);
	    }
	    return rs;
	}
	
	/**
	 * Exposed as the SQL function <b><code>MV</code></b>
	 * @param conn
	 * @param oldestPeriod 
	 * @param ids
	 * @return
	 * @throws SQLException
	 */
	public static ResultSet getValues(Connection conn, long oldestPeriod, Long...ids) throws SQLException {
	    SimpleResultSet rs = new SimpleResultSet();
	    rs.addColumn("ID", Types.NUMERIC, 255, 22);
	    rs.addColumn("TS", Types.TIMESTAMP, 1, 22);
	    rs.addColumn("MIN", Types.NUMERIC, 255, 22);
	    rs.addColumn("MAX", Types.NUMERIC, 255, 22);
	    rs.addColumn("AVG", Types.NUMERIC, 255, 22);
	    rs.addColumn("CNT", Types.NUMERIC, 255, 22);
	    String url = conn.getMetaData().getURL();
	    if (url.equals("jdbc:columnlist:connection")) {
	        return rs;
	    }
	    Arrays.sort(ids);
	    PreparedStatement ps = null;
	    ResultSet rset = null;
	    try {
	    	StringBuilder q = new StringBuilder("SELECT V, ID FROM METRIC_VALUES");
	    	if(ids!=null && ids.length>0 && ids[0] != -1L) {
	    		q.append(" WHERE ID IN (");
		    	q.append(Arrays.toString(ids).replace("[", "").replace("]", ""));
		    	q.append(")");	    		
	    	}
	    	
	    	ps = conn.prepareStatement(q.toString());
	    	//ps.setArray(1, conn.createArrayOf("java.lang.Long", ids));
	    	rset = ps.executeQuery();
	    	while(rset.next()) {
	    		H2TimeSeries mvd = (H2TimeSeries)rset.getObject(1);
	    		long mid = rset.getLong(2);
	    	    for(int i = 0; i <= mvd.size; i++) {
	    	    	long[] row = mvd.getArray(i);
	    	    	if(row==null || row[PERIOD]<oldestPeriod) continue;
	    	    	rs.addRow( 
	    	    			mid,
	    	    			new java.sql.Timestamp(row[PERIOD]), 
	    	    			row[MIN], 
	    	    			row[MAX], 
	    	    			row[AVG], 
	    	    			row[CNT]);
	    	    }
	    		
	    	}
	    } finally {
	    	if(rset!=null) try { rset.close(); } catch (Exception ex) {}
	    	if(ps!=null) try { ps.close(); } catch (Exception ex) {}
	    }
	    return rs;
	}	
	
	/**
	 * Creates a new H2TimeSeries
	 * @param STEP The STEP size in ms.
	 * @param WIDTH The WIDTH, or number of entries in the window
	 */
	public H2TimeSeries(long step, int width) {
		super();
		this.step = step;
		this.width = width-1;
		store = ByteBuffer.allocateDirect(ENTRY_SIZE);
	}
	
	/**
	 * Returns the size of the store in bytes based on the WIDTH of the time-series window
	 * @return the number of bytes in the store.
	 */
	public int storeByteSize() {
		return (width+1) * ENTRY_SIZE;
	}
	
	public static void main(String[] args) {
		log("Domain MetricValue Test");
		H2TimeSeries d = new H2TimeSeries(1000, 10);
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
	 * @return The prior period's slot if a roll occurred, null if it did not
	 */
	public synchronized long[] addValue(long timestamp, long value) {
		final long period = getPeriod(timestamp);
		store.position(0);
		if(size<0) {			
			size++;
			put(new long[]{period, value, value, value, 1});
			LOG.info("Initial Entry");
			return null;
		}			
		final long[] values = getArray(size);			
		final boolean update = values[PERIOD]==period;
		final long[] newValues;
		if(update) {
			store.position(size * ENTRY_SIZE);
			newValues = calcValue(values, period, value);
			put(newValues);
			LOG.info("Updated existing Entry:" + new Date(period));
			return null;
		} 
		if(size<width) {
			size++;
			store.position(size * ENTRY_SIZE);
			LOG.info("Rolled to slot [" + size + "] Pos:[" + store.position() + "]");					
		} else {
			store.position(ENTRY_SIZE);
			store.compact();
			store.position(size * ENTRY_SIZE);
			LOG.info("Compacted. Size: [" + size + "] Pos:[" + store.position() + "]" + 
					"\n\tPrior Period:" + new Date(values[PERIOD]) +
					"\n\tNew Period:" + new Date(period) +
					"\n\tStep Diff:" + (period-values[PERIOD]) + " ms."
			);
		}
		
		newValues = calcValue(null, period, value);
		put(newValues);
		return values;		
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
	 * Returns the time range of values held in this time-series
	 * @return a long array with the start time and end time, or null if there are no entries
	 */
	public long[] getTimeRange() {
		if(size<0) return null;
		return new long[]{
				getArray(0)[PERIOD],
				getArray(size)[PERIOD]
		};
	}
	
	/**
	 * Returns the date range of values held in this time-series
	 * @return a {@link java.sql.Date} array with the start time and end time, or null if there are no entries
	 */
	public java.sql.Date[] getDateRange() {
		if(size<0) return null;
		return new java.sql.Date[]{
				new java.sql.Date(getArray(0)[PERIOD]),
				new java.sql.Date(getArray(size)[PERIOD])
		};
	}
	
	
	/**
	 * Returns the values at the specified position
	 * @param position The time-series slot position
	 * @return The values in the positioned slot or null if there are no slots
	 */
	protected long[] getArray(int position) {
		if(size<0) return null;
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
	 * Returns the STEP in ms. 
	 * @return the STEP
	 */
	public long getStep() {
		return step;
	}

	/**
	 * Returns the WIDTH of the time-series window
	 * @return the WIDTH
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
	 * Serializes a H2TimeSeries to a byte array
	 * @param mvd The H2TimeSeries to serialize
	 * @return A byte array
	 * @throws IOException Thrown on any io exception
	 */
	public static byte[] serialize(H2TimeSeries mvd) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(mvd.byteSize());
		ObjectOutputStream oos = new ObjectOutputStream(baos); 
		oos.writeObject(mvd);
		oos.flush();
		baos.flush();
		return baos.toByteArray();
	}
	
	/**
	 * Deserializes a H2TimeSeries from a byte array
	 * @param arr The byte array to deserialize from
	 * @return The deserialized H2TimeSeries 
	 * @throws IOException thrown on any io exception
	 * @throws ClassNotFoundException Will not be thrown.
	 */
	public static H2TimeSeries deserialize(byte[] arr) throws IOException, ClassNotFoundException {
		ByteArrayInputStream bais = new ByteArrayInputStream(arr);
		ObjectInputStream ois = new ObjectInputStream(bais);
		return (H2TimeSeries) ois.readObject();
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
		SerializationWrites.incrementAndGet();
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
		SerializationReads.incrementAndGet();
		//log("Read In. Buff:" + store + " Step:" + STEP + " Width:" + WIDTH + " Size:" + size);		
		 
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
		b.append("MetricValue[STEP:").append(step).append(" WIDTH:").append(width).append("]");
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
	 * <p><code>org.helios.apmrouter.destination.h2timeseries.H2TimeSeries.CompactObjectOutputStream</code></p>
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
	 * <p><code>org.helios.apmrouter.destination.h2timeseries.H2TimeSeries.CompactObjectInputStream</code></p>
	 */
	protected static class CompactObjectInputStream extends ObjectInputStream {
		public static final ObjectStreamClass OSC;
		
		static {
			try {
				OSC = ObjectStreamClass.lookup(H2TimeSeries.class);
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
