/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2012, Helios Development Group and individual contributors
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

import static org.helios.apmrouter.destination.h2timeseries.H2TimeSeries.AVG;
import static org.helios.apmrouter.destination.h2timeseries.H2TimeSeries.CNT;
import static org.helios.apmrouter.destination.h2timeseries.H2TimeSeries.MAX;
import static org.helios.apmrouter.destination.h2timeseries.H2TimeSeries.MIN;
import static org.helios.apmrouter.destination.h2timeseries.H2TimeSeries.PERIOD;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.h2.tools.SimpleResultSet;
import org.helios.apmrouter.collections.ArrayOverflowException;
import org.helios.apmrouter.collections.ConcurrentLongSlidingWindow;
import org.helios.apmrouter.collections.ILongSlidingWindow;
import org.helios.apmrouter.util.SystemClock;

/**
 * <p>Title: UnsafeH2TimeSeries</p>
 * <p>Description: An reimplementation of {@link H2TimeSeries} using unsafe arrays for storage</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.h2timeseries.UnsafeH2TimeSeries</code></p>
 */

public class UnsafeH2TimeSeries implements Serializable {
	/**  */
	private static final long serialVersionUID = -963955749386799856L;
	/** Static class logger */
	protected static final Logger LOG = Logger.getLogger(UnsafeH2TimeSeries.class);
	/** The STEP size in ms. */
	protected final long step;
	/** The WIDTH, or number of entries in the window */
	protected final int width;
	/** Indicates if the data stream should be compressed */
	protected final boolean compressed;
	
	/** The value store for the periods */
	protected final ConcurrentLongSlidingWindow periods;
	/** The value store for the mins */
	protected final ConcurrentLongSlidingWindow mins;
	/** The value store for the maxes */
	protected final ConcurrentLongSlidingWindow maxes;
	/** The value store for the averages */
	protected final ConcurrentLongSlidingWindow averages;
	/** The value store for the counts */
	protected final ConcurrentLongSlidingWindow counts;
	
	
	/** A counter of serialization reads */
	private static final AtomicLong SerializationReads = new AtomicLong(0L);
	/** A counter of serialization writes */
	private static final AtomicLong SerializationWrites = new AtomicLong(0L);
	/** The timestamp of the last serialization metric reset */
	private static final AtomicLong LastReset = new AtomicLong(System.currentTimeMillis());
	/** The rolling deser bytes */
	protected static final ILongSlidingWindow deserBytes = new ConcurrentLongSlidingWindow(60);
	/** The count of UnsafeH2TimeSeries allocated instances */
	private static final AtomicLong AllocatedInstances = new AtomicLong(0L);
	
	
	/**
	 * Returns the count of UnsafeH2TimeSeries allocated instances
	 * @return the count of UnsafeH2TimeSeries allocated instances
	 */
	public static long getAllocatedInstances() {
		return AllocatedInstances.get();
	}
	
	/**
	 * Returns the rolling average of the DB read deserialization bytes
	 * @return the rolling average of the DB read deserialization bytes
	 */
	public static long getRollingDeserBytes() {
		return deserBytes.avg();
	}
	
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
	
	Object writeReplace() throws ObjectStreamException {
		return UnsafeH2TimeSeries.serialize(this);
	}
	
	protected transient byte[] inBytes;
	
	Object readResolve() throws ObjectStreamException {
		UnsafeH2TimeSeries uts = UnsafeH2TimeSeries.deserialize(inBytes);
		inBytes = null;
		return uts;
	}
	
	/**
     * Deallocates this UnsafeH2TimeSeries
     */
    public void destroy() {
    	periods.destroy();
    	mins.destroy();
    	maxes.destroy();
    	averages.destroy();
    	counts.destroy();
    	AllocatedInstances.decrementAndGet();
    }

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buff = new byte[4096];
		int bytesRead = -1;
		while((bytesRead = in.read(buff))!=-1) {
			baos.write(buff, 0, bytesRead);
		}		inBytes = baos.toByteArray();
	}
	
	/**
	 * Creates a new UnsafeH2TimeSeries
	 * @param step The STEP size in ms.
	 * @param width The WIDTH, or number of entries in the window
	 * @param compressed True to compress the time series entries, false otherwise
	 * @param sticky Indicates if the metric is sticky
	 * @return a new UnsafeH2TimeSeries
	 */
	public static UnsafeH2TimeSeries make(long step, int width, boolean compressed, boolean sticky) {
		return new UnsafeH2TimeSeries(step, width, compressed);
	}
	
	/**
	 * Creates a new compressed UnsafeH2TimeSeries
	 * @param step The STEP size in ms.
	 * @param width The WIDTH, or number of entries in the window
	 * @param sticky Indicates if the metric is sticky
	 * @return a new UnsafeH2TimeSeries
	 */
	public static UnsafeH2TimeSeries make(long step, int width, boolean sticky) {
		return new UnsafeH2TimeSeries(step, width, true);
	}	
	
	/**
	 * Creates a new UnsafeH2TimeSeries
	 * @param step The STEP size in ms.
	 * @param width The WIDTH, or number of entries in the window
	 * @param compressed True to compress the time series entries, false otherwise
	 */
	public UnsafeH2TimeSeries(long step, int width, boolean compressed) {
		super();
		this.step = step;
		this.width = width-1;
		this.compressed = compressed;
		periods = new ConcurrentLongSlidingWindow(width);
		mins = new ConcurrentLongSlidingWindow(width);
		maxes = new ConcurrentLongSlidingWindow(width);
		averages = new ConcurrentLongSlidingWindow(width);
		counts = new ConcurrentLongSlidingWindow(width);
		AllocatedInstances.incrementAndGet();
		
	}
	
	/**
	 * Recreates a new UnsafeH2TimeSeries
	 * @param step The STEP size in ms.
	 * @param width The WIDTH, or number of entries in the window
	 * @param compressed True to compress the time series entries, false otherwise 
	 * @param arr The byte array to re-initialize with
	 */
	public UnsafeH2TimeSeries(long step, int width, boolean compressed, byte[] arr) {
		this(step, width, compressed); 		
		if(arr!=null) {			
			byte[][] split = split(arr, 5);
			periods.reinitAndLoad(split[PERIOD]);
			mins.reinitAndLoad(split[MIN]);
			maxes.reinitAndLoad(split[MAX]);
			averages.reinitAndLoad(split[AVG]);
			counts.reinitAndLoad(split[CNT]);
		}
		
//		periods.insert(UnsafeLongArray.convert(split[PERIOD]));
//		mins.insert(UnsafeLongArray.convert(split[MIN]));
//		maxes.insert(UnsafeLongArray.convert(split[MAX]));
//		averages.insert(UnsafeLongArray.convert(split[AVG]));
//		counts.insert(UnsafeLongArray.convert(split[CNT]));
	}
	
	/**
	 * Splits an array of bytes representing longs into <code>each</code> equally sized arrays
	 * @param arr The array to split
	 * @param each The number of groups to divide into
	 * @return an array of byte arrays
	 */
	public static byte[][] split(byte[] arr, int each) {
		if(arr==null) throw new IllegalArgumentException("The passed array was null", new Throwable());
		int len = arr.length;
		if(len<8) throw new IllegalArgumentException("The passed array was less than 8 bytes", new Throwable());
		if(len%8!=0) throw new IllegalArgumentException("The passed array failed mod check [" + len + "]", new Throwable());
		int totalEntries = len/8;
		if(totalEntries%each!=0) throw new IllegalArgumentException("The total entries failed mod check [" + totalEntries + "/" + each +  "]", new Throwable());
		int longsEach = totalEntries/each;
		
		final int bytesEach = longsEach << 3;
		byte[][] ret = new byte[each][bytesEach];
		int srcPos = 0;
		try {
			for(int i = 0; i < 5; i++) {
				System.arraycopy(arr, srcPos, ret[i], 0, bytesEach);
				srcPos += bytesEach;
			}
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		return ret;
	}
	
		
	
	/**
	 * Creates a new UnsafeH2TimeSeries and adds a new value
	 * @param conn The connection to lookup the existing time-series
	 * @param step The STEP size in ms.
	 * @param width The WIDTH, or number of entries in the window
	 * @param compressed True to compress, false otherwise
	 * @param sticky Indicates if the metric is sticky
	 * @param id The metric ID of the metric to upsert a time-series entry for
	 * @param ts The timestamp of the value to add
	 * @param value  The value to add
	 * @throws Exception Thrown on any error
	 */
	public static void make_and_add(Connection conn, long step, int width, boolean compressed, boolean sticky, long id, Timestamp ts, long value) throws Exception {
		PreparedStatement ps = null;
		ResultSet rset = null;		
		UnsafeH2TimeSeries mvd = null;
		try {
			ps = conn.prepareStatement("SELECT V FROM UNSAFE_METRIC_VALUES WHERE ID = ?");
			ps.setLong(1, id);
			rset = ps.executeQuery();
			if(rset.next()) {
	    		byte[] utsBytes = rset.getBytes(1);
	    		deserBytes.insert(utsBytes.length);
				mvd = UnsafeH2TimeSeries.deserialize(utsBytes);
			} else {
				mvd = new UnsafeH2TimeSeries(step, width, compressed);
			}
			mvd.addValue(ts.getTime(), value);
			rset.close();
			ps.close();
			ps = conn.prepareStatement("UPDATE UNSAFE_METRIC_VALUES SET V = ? WHERE ID = ?");
			ps.setBytes(1, UnsafeH2TimeSeries.serialize(mvd));
			ps.setLong(2, id);
			ps.execute();
		} finally {
			if(mvd!=null) mvd.destroy();
			if(rset!=null) try { rset.close(); } catch (Exception ex) { /* No Op */ }
			if(ps!=null) try { ps.close(); } catch (Exception ex) {/* No Op */}
		}
	}
	
	/**
	 * Creates a new compressed UnsafeH2TimeSeries and adds a new value
	 * @param conn The connection to lookup the existing time-series
	 * @param step The STEP size in ms.
	 * @param width The WIDTH, or number of entries in the window
	 * @param sticky Indicates if the metric is sticky
	 * @param id The metric ID of the metric to upsert a time-series entry for
	 * @param ts The timestamp of the value to add
	 * @param value  The value to add
	 * @throws Exception Thrown on any error
	 */
	public static void make_and_add(Connection conn, long step, int width, boolean sticky, long id, Timestamp ts, long value) throws Exception {
		make_and_add(conn, step, width, true, sticky, id, ts, value);
	}	
	
	
	/**
	 * Tests a byte array to see if it is a valid UnsafeH2TimeSeries
	 * @param data The byte array to test
	 * @return true if the array is a valid UnsafeH2TimeSeries
	 * @throws Exception thrown if the byte array is invalid
	 */
	public static boolean isType(byte[] data) throws Exception {
		if(data==null) return false;
		UnsafeH2TimeSeries hts = null;
		try {
			hts = deserialize(data);
			return true;
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			throw ex;
		} finally {
			if(hts!=null) hts.destroy();
		}
	}

	
	/**
	 * Adds a value to the time-series window
	 * @param timestamp The timestamp in long UTC
	 * @param value The long value to add
	 * @return The prior period's slot if a roll occurred, null if it did not
	 */
	public synchronized long[] addValue(long timestamp, long value) {
		final long period = SystemClock.period(step, timestamp);
		final long currentPeriod; 
		if(periods.size()==0) {
			// Insert
			periods.insert(period);
			mins.insert(value);
			maxes.insert(value);
			averages.insert(value);
			counts.insert(1L);
			return null;
		} 
		currentPeriod = periods.get(0);
		boolean update = period==currentPeriod;
		if(!update) {
			// FIXME: Not sure why it happens, but if it is legit, find the bucket and update it.
			if(period-currentPeriod < step) throw new IllegalStateException("Unexpected period increment.\n\tCurrent Period:" + new Date(currentPeriod) + "\n\tNew Period:" + new Date(period) + "\n\tDiff:" + (period-currentPeriod), new Throwable());
			// Roll new period
			final long[] rolledPeriod = getArray(0);
			final long[] droppedPeriod = new long[5];  // <---- use this guy to roll up to the next tier.
			if(periods.size()==step) {
				// Oldest period dropped
				droppedPeriod[PERIOD] = periods.insert(period);
				droppedPeriod[MIN] = mins.insert(value);
				droppedPeriod[MAX] = maxes.insert(value);
				droppedPeriod[AVG] = averages.insert(value);
				droppedPeriod[CNT] = counts.insert(1L);
			} else {
				// No drop yet
				periods.insert(period);
				mins.insert(value);
				maxes.insert(value);
				averages.insert(value);
				counts.insert(1L);				
			}
			return rolledPeriod;
		} 
		// Update current period
		final long[] newValues = calcValue(getArray(0), period, value);
		periods.set(newValues[PERIOD]);
		mins.set(newValues[MIN]);
		maxes.set(newValues[MAX]);
		averages.set(newValues[AVG]);
		counts.set(newValues[CNT]);
		return null;
	}	
	
	/**
	 * Returns an array of the values for the specified period index
	 * @param index The index representing the period to retrieve
	 * @return an array of the values for the specified period index
	 */
	protected long[] getArray(int index) {		
		if(index+1 > periods.size()) throw new ArrayOverflowException("Attempted to access at index [" + index + "] but size is [" + periods.size() + "]", new Throwable());
		return new long[]{periods.get(index), mins.get(index), maxes.get(index), averages.get(index), counts.get(index)};
	}
	
	/**
	 * Returns an array of the values for the specified period 
	 * @param timestamp The period to retrieve
	 * @return an array of the values for the specified period or null if the period was not found
	 */
	protected long[] getPeriodValues(long timestamp) {
		int index = periods.find(timestamp);
		if(index>0) return getArray(index);
		return null;		
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
	 * Returns the number of periods in this time series
	 * @return the number of periods in this time series
	 */
	public int size() {
		return periods.size();
	}
	
	/**
	 * Returns the time range of values held in this time-series
	 * @return a long array with the start time and end time, or null if there are no entries
	 */
	public long[] getTimeRange() {
		if(periods.isEmpty()) return null;		
		return new long[]{
				getArray(0)[PERIOD],
				getArray(size()-1)[PERIOD]
		};
	}
	
	/**
	 * Returns the date range of values held in this time-series
	 * @return a {@link java.sql.Date} array with the start time and end time, or null if there are no entries
	 */
	public java.sql.Date[] getDateRange() {
		if(periods.isEmpty()) return null;
		return new java.sql.Date[]{
				new java.sql.Date(getArray(0)[PERIOD]),
				new java.sql.Date(getArray(size()-1)[PERIOD])
		};
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
		return size();
	}
	
	/**
	 * Returns the number of bytes this object is expected to seralize to
	 * @return the number of bytes this object is expected to seralize to
	 */
	public int byteSize() {
		return ((8+4+4) /* step, width and size */ + (size()*8*5)) /* # of longs in each window X the # of windows*/;  
	}	
	
	/**
	 * Returns all the resolved values in the time-series in a result set
	 * @param data The byte array
	 * @return A result set iterating all the values in the time-series
	 * @throws Exception thrown on any error
	 */
	public static ResultSet allvalues(byte[] data) throws Exception {
		UnsafeH2TimeSeries mvd = null;
		try {
			mvd = deserialize(data);
			SimpleResultSet rs = new SimpleResultSet();
		    rs.addColumn("TS", Types.TIMESTAMP, 1, 22);
		    rs.addColumn("MIN", Types.NUMERIC, 255, 22);
		    rs.addColumn("MAX", Types.NUMERIC, 255, 22);
		    rs.addColumn("AVG", Types.NUMERIC, 255, 22);
		    rs.addColumn("CNT", Types.NUMERIC, 255, 22);
		    for(int i = 0; i < mvd.periods.size(); i++) {
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
		} finally {
			if(mvd!=null) mvd.destroy();
		}
	}
	
	/**
	 * Exposed as the SQL function <b><code>UNSAFE_MV</code></b>
	 * @param conn The H2 connection
	 * @param oldestPeriod The oldest period to retrieve values for
	 * @param ids An array of IDs to get data for
	 * @return A result set
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
	    	StringBuilder q = new StringBuilder("SELECT V, ID FROM UNSAFE_METRIC_VALUES");
	    	if(ids!=null && ids.length>0 && ids[0] != -1L) {
	    		q.append(" WHERE ID IN (");
		    	q.append(Arrays.toString(ids).replace("[", "").replace("]", ""));
		    	q.append(")");	    		
	    	}
	    	
	    	ps = conn.prepareStatement(q.toString());
	    	//ps.setArray(1, conn.createArrayOf("java.lang.Long", ids));
	    	rset = ps.executeQuery();
	    	while(rset.next()) {
	    		UnsafeH2TimeSeries mvd = null;
	    		try {
		    		byte[] utsBytes = rset.getBytes(1);
		    		deserBytes.insert(utsBytes.length);
		    		mvd = UnsafeH2TimeSeries.deserialize(utsBytes);
		    		Map<Long, long[]> orderedMap = new TreeMap<Long, long[]>();
		    		for(int i = 0; i < mvd.getSize(); i++) {
		    			long[] row = mvd.getArray(i);
		    			if(row[PERIOD]<oldestPeriod) continue;
		    			orderedMap.put(row[PERIOD], row);
		    		}
		    		long mid = rset.getLong(2);
		    	    for(Map.Entry<Long, long[]> entry: orderedMap.entrySet()) {
		    	    	long[] row = entry.getValue();
		    	    	if(row==null || row[PERIOD]<oldestPeriod) continue;	    	    	
		    	    	rs.addRow( 
		    	    			mid,
		    	    			new java.sql.Timestamp(row[PERIOD]), 
		    	    			row[MIN], 
		    	    			row[MAX], 
		    	    			row[AVG], 
		    	    			row[CNT]);
		    	    }
	    		} finally {
	    			if(mvd!=null) mvd.destroy();
	    		}
		    		
	    	}
	    } finally {
	    	if(rset!=null) try { rset.close(); } catch (Exception ex) {/* No Op */}
	    	if(ps!=null) try { ps.close(); } catch (Exception ex) {/* No Op */}
	    }
	    return rs;
	}		
	
	/**
	 * Adds a value to the UnsafeH2TimeSeries deserialized from the passed byte array
	 * @param data The byte array to be desrialized into a UnsafeH2TimeSeries
	 * @param timestamp The effective timestamp of the data to be added
	 * @param value The data to be added
	 * @throws Exception thrown on any error
	 */
	public static void add(byte[] data, Timestamp timestamp, long value) throws Exception {
		UnsafeH2TimeSeries mvd = null;
		try {
			mvd = deserialize(data);
			mvd.addValue(timestamp.getTime(), value);
		} finally {
			if(mvd!=null) mvd.destroy();
		}
	}

	

	
	/**
	 * Serializes a UnsafeH2TimeSeries to a byte array
	 * @param mvd The UnsafeH2TimeSeries to serialize
	 * @return A byte array
	 */
	public static byte[] serialize(UnsafeH2TimeSeries mvd) {
		ByteArrayOutputStream out = new ByteArrayOutputStream(mvd.byteSize());
		GZIPOutputStream dos = null;
		DataOutputStream dout = null;
		try {
			if(mvd.compressed) {
				dos = new GZIPOutputStream(out, mvd.byteSize(), true);
			}			
			dout = new DataOutputStream(mvd.compressed ? dos : out);
			dout.writeLong(mvd.step);
			dout.writeInt(mvd.width);
			int sz = mvd.periods.size();
			dout.writeInt(sz);
			if(sz>0) {
				dout.write(mvd.periods.getBytes());
				dout.write(mvd.mins.getBytes());
				dout.write(mvd.maxes.getBytes());
				dout.write(mvd.averages.getBytes());
				dout.write(mvd.counts.getBytes());
			}
			if(dos!=null) dos.finish();
			dout.flush();
			out.flush();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		SerializationWrites.incrementAndGet();
		return out.toByteArray();
	}
	
	/**
	 * Deserializes a UnsafeH2TimeSeries from a byte array
	 * @param arr The byte array to deserialize from
	 * @return The deserialized UnsafeH2TimeSeries 
	 */
	public static UnsafeH2TimeSeries deserialize(byte[] arr) {
		arr = testForClassDesc(arr);
		final boolean gzip = isGzip(arr);
		ByteArrayInputStream bais = new ByteArrayInputStream(arr);
		GZIPInputStream dis = null;
		DataInputStream dais = null;
		try {
			if(gzip) {
				dis = new GZIPInputStream(bais);
			}
			dais = new DataInputStream(gzip ? dis : bais);			
			long step = dais.readLong();
			int width = dais.readInt();
			int arrSize = dais.readInt();
			byte[] narr  = null;
			if(arrSize>0) {
				narr = new byte[arrSize*8*5];
				dais.readFully(narr);
			}
			SerializationReads.incrementAndGet();
			return new UnsafeH2TimeSeries(step, width, gzip, narr);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		
	}
	
	/**
	 * Determines if the channel is carrying a gzipped metric submssion
	 * @param arr The byte array being inspected for the gzip magic signature
	 * @return true if the incoming payload is gzipped
	 */
	public static boolean isGzip(byte[] arr) {
		if(arr==null || arr.length<2) return false;
		int magic1 = (short) (arr[0] & 0xFF);
		int magic2 = (short) (arr[1] & 0xFF);
		
		return magic1 == 31 && magic2 == 139;	
	}	
	
	public static byte[] testForClassDesc(byte[] arr) {  // 27 bytes
		if(arr==null || arr.length<2) return null;
		int magic1 = (short) (arr[0] & 0xFF);
		int magic2 = (short) (arr[1] & 0xFF);
		
		if(magic1 == 172 && magic2 == 237) {
			byte[] newArr = new byte[arr.length-27];
			System.arraycopy(arr, 27, newArr, 0, arr.length-27);
			return newArr;
		}
		return arr;
	}	
	
	public static void main(String[] args) {
		BasicConfigurator.configure();
		LOG.info("UnsafeH2TimeSeries Test");
		UnsafeH2TimeSeries ts = new UnsafeH2TimeSeries(2000, 3, false);
		Random random = new Random(System.currentTimeMillis());
		for(int x = 0; x < 5; x++) {
			int loops = Math.abs(random.nextInt(100)) + 100;
			for(int i = 0; i < loops; i++) {
				long[] rolled = ts.addValue(SystemClock.time(), Math.abs(random.nextInt(100))+1);
				if(rolled!=null) {
					LOG.info("Rolled Period:" + Arrays.toString(rolled));
				}
			}
			LOG.info("Completed Iter " + x);
			SystemClock.sleep(2000);
		}
		LOG.info("Completed Population:\n" + ts);
		LOG.info("Serializing. Expected Size:" + ts.byteSize());
		byte[] arr = UnsafeH2TimeSeries.serialize(ts);
		LOG.info("Serialized. Actual Size:" + arr.length);
		UnsafeH2TimeSeries ts2 = UnsafeH2TimeSeries.deserialize(arr);
		LOG.info("Completed Deserialization:\n" + ts2);
		
		LOG.info("Done");
		
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("MetricValue[STEP:").append(step).append(" WIDTH:").append(width).append("]");
		for(int i = 0; i < size(); i++) {
			b.append("\n\t").append(entryToString(getArray(i)));
		}
		return b.toString();
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


}
