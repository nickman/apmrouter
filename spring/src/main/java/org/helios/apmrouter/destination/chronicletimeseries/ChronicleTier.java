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
package org.helios.apmrouter.destination.chronicletimeseries;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.tsmodel.Tier;
import org.helios.apmrouter.util.SystemClock;

import vanilla.java.chronicle.Excerpt;
import vanilla.java.chronicle.impl.IndexedChronicle;
import vanilla.java.chronicle.impl.UnsafeExcerpt;

/**
 * <p>Title: ChronicleTier</p>
 * <p>Description: Represents one chronicle time-series tier.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.chronicletimeseries.ChronicleTier</code></p>
 */

public class ChronicleTier implements ChronicleTierMXBean {
	/** The chronicle file name */
	protected final String chronicleName;
	/** The chronicle parth */
	protected final String chroniclePath;	
	/** The chronicle */
	protected final IndexedChronicle chronicle;
	/** The parent tier (if one exists) */
	protected final ChronicleTier parent;
	/** Instance logger */
	protected final Logger log;
	/** The chronicle time-series manager */
	protected final ChronicleTSManager manager;
	/** The number of periods in this tier */
	protected final int periods;
	/** The duration of each period in this tier in seconds */
	protected final long periodDuration;
	/** The duration of each period in this tier in ms */
	protected final long periodDurationMs;
	
	/** The size of each chronicle entry in this tier */
	protected final int entrySize;
	/** The JMX ObjectName for this tier */
	protected final ObjectName objectName;
	/** The earliest period end time in this tier */
	protected final AtomicLong startPeriod = new AtomicLong(Long.MAX_VALUE);
	/** The latest period end time in this tier */
	protected final AtomicLong endPeriod = new AtomicLong(Long.MIN_VALUE);

	/** The number of values in each series entry */
	protected static final int SERIES_ENTRY_VALUES = 5;
	/** The header offset in each chronicle entry, ie. the length of the metricId (long) start time (long), end time (long) and size (int) */
	protected static final int HEADER_OFFSET = 8 + 8 + 8 + 4; 
	/** The size of each series entry, ie. longs for TS, MIN, MAX, AVG and CNTS */
	protected static final int SERIES_SIZE = SERIES_ENTRY_VALUES * 8; 
	
	/** The chronicle home directory. We're storing them in the same sub-dir as the H2 metric catalog */
	public static final File CHRONICLE_HOME_DIR = new File(System.getProperty("user.home") + File.separator + ".apmrouter" + File.separator + "h2" + File.separator + "time-series");
	/** The default chronicle databit size estimate */
	public static final int CHRONICLE_SIZE_EST = 10;
	
	// ===================================================
	// Series entry index
	// ===================================================
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
	// ===================================================
	// Series header offsets
	// ===================================================
	/** The series offset for the header start time */
	public static final int H_START = 0;
	/** The series offset for the header end time */
	public static final int H_END = H_START + 8;
	/** The series offset for the header entry count */
	public static final int H_SIZE = H_END + 4;
	

	/**
	 * Creates a new ChronicleTier and initializes the underlying chronicle
	 * @param tier The time-series model tier for this ChronicleTier
	 * @param parentTier The parent ChronicleTier for this tier or null if it has no parent
	 * @param tsManager The ChronicleTSManager
	 */
	ChronicleTier(Tier tier, ChronicleTier parentTier, ChronicleTSManager tsManager) {
		chronicleName = tier.getName();
		log = Logger.getLogger(getClass().getName() + "." + chronicleName);
		parent = parentTier;
		manager = tsManager;
		periods = (int) tier.getPeriodCount();
		periodDuration = tier.getPeriodDuration().seconds;
		periodDurationMs = TimeUnit.MILLISECONDS.convert(periodDuration, TimeUnit.SECONDS);
		objectName = JMXHelper.objectName(new StringBuilder("org.helios.apmrouter.timeseries:type=chronicle,name=").append(chronicleName));
		entrySize = HEADER_OFFSET + (SERIES_SIZE * periods);
		
		if(!CHRONICLE_HOME_DIR.exists()) {
			if(!CHRONICLE_HOME_DIR.mkdir()) {
				throw new RuntimeException("Failed to create chronicle ts home directory [" + CHRONICLE_HOME_DIR + "]", new Throwable());
			}
		} else {
			if(!CHRONICLE_HOME_DIR.isDirectory()) {
				throw new RuntimeException("chronicle ts home directory [" + CHRONICLE_HOME_DIR + "] is a file not a directory", new Throwable());
			}
		}
		chroniclePath = CHRONICLE_HOME_DIR + File.separator + chronicleName;
		try {
			chronicle = new IndexedChronicle(chroniclePath, CHRONICLE_SIZE_EST);
			chronicle.useUnsafe(true);
			initSeries();
			JMXHelper.getHeliosMBeanServer().registerMBean(this, objectName);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create chronicle on path [" + chroniclePath + "]", e);
		}		
		log.info("Initialized chronicle [" + chronicle.name() + "] on path [" + chroniclePath + "] with size [" + chronicle.size() + "]");
	}
	
	/**
	 * Initializes the tier headers
	 */
	protected void initSeries() {
		if(chronicle.size()<1) return;
		log.info("Initializing [" + chronicle.size() + "] entries");
		for(long key = 0; key < chronicle.size(); key++) {
			UnsafeExcerpt<IndexedChronicle> ex = createUnsafeExcerpt(key) ;
			//if(!ex.index(key)) throw new RuntimeException("Failed to initialized tier [" + chronicleName + "]", new Throwable());
			long startTime = ex.readLong();
			long endTime = ex.readLong();		
			tickPeriods(startTime, endTime);
		}
		log.info("Initialized tier [" + chronicleName + "] with [" + chronicle.size() + "] entries");
	}

	/**
	 * Adds a new value to the corresponding series in this tier
	 * @param metricId The metric Id to validate the series header
	 * @param timestamp The timestamp of the metric submission
	 * @param value The metric value to be added to the time-series
	 * @return the rolled value or null if this operation updates the current period
	 */
	public long[] addValue(IMetric metric) {
		long metricId = metric.getToken(), timestamp = metric.getTime(), value = metric.getLongValue();
		if(metricId<0) throw new IllegalArgumentException("The metric ID cannot be < 0", new Throwable());
		UnsafeExcerpt<IndexedChronicle> ex = createUnsafeExcerpt(metricId);
		long period = SystemClock.period(periodDurationMs, timestamp);
		long startTime = ex.readLong();
		long endTime = ex.readLong();		
		int pCount = ex.readInt();		
		if(pCount==0) {
			//ex.position(0);
			ex.writeLongArray(period, period);
			ex.writeInt(1);
			ex.writeLongArray(period, value, value, value, 1);
			ex.finish();
			tickPeriods(period, period);
			return null;
		}
		int periodIndex = getPeriodIndex(metric.getToken(), period);
		switch(periodIndex) {
			case -1:
				return null;
			case 0:
				mergeIntoCurrentPeriod(metric, ex);
				break;
			case 1:
				// roll and update 
				rollAndMerge(pCount, period, metric, ex);
				break;				
			default:
				log.warn("Unexpected period index ["+ periodIndex + "]");
		}
		return null;
	}
	
	protected void rollAndMerge(int currentSize, long period, IMetric metric, UnsafeExcerpt<IndexedChronicle> ex) {
		ex.insertAndRollRight(HEADER_OFFSET, currentSize * SERIES_SIZE);
		ex.writeLongArray(HEADER_OFFSET, new long[]{Long.MAX_VALUE,Long.MIN_VALUE,0,0});
		currentSize++;
		ex.writeLongArray(H_START, new long[]{period, period + this.periodDurationMs});
		ex.writeInt(H_SIZE, currentSize);
		mergeIntoCurrentPeriod(metric, ex);
		
	}
	
	protected void mergeIntoCurrentPeriod(IMetric metric, UnsafeExcerpt<IndexedChronicle> ex) {
		long[] values = ex.readLongArray(HEADER_OFFSET, SERIES_SIZE);
		long val = metric.getLongValue();
		if(val < values[MIN]) values[MIN] = val;
		if(val > values[MAX]) values[MAX] = val;
		if(values[CNT]==0) {
			values[AVG] = val;
		} else {
			values[AVG] = (values[AVG]+val)/2;
		}
		values[CNT]++;
		ex.writeLongArray(HEADER_OFFSET, values);
		ex.finish();
	}
	
	/**
	 * Returns the chronicle entry index that the passed period should be merged into, or -1 for a drop.
	 * Currently only returns an index if the merge is for the current period, or the next.
	 * At some point, may merge into earlier periods.
	 * @param period The period to get the index for
	 * @return the chronicle entry index or -1 for a drop
	 */
	protected int getPeriodIndex(long metricId, long period) {
		if(chronicle.size()==0) return 0;
		long firstIndex = createUnsafeExcerpt(metricId).readLong();
		if(period==firstIndex) return 0;
		if(period>firstIndex) return 1;
		return -1;
	}
	
	/**
	 * Ticks up the tier's earliest and latest period timestamp highwaters
	 * @param start The start time
	 * @param end The end time
	 */
	protected void tickPeriods(long start, long end) {
		long endp = endPeriod.get();
		if(end>endp) endPeriod.set(end);
		long startp = startPeriod.get();
		if(start<startp) startPeriod.set(start);
	}
	
	/**
	 * Reads a series entry from the series for the passed index
	 * @param index The chronicle index to read the series for
	 * @param seriesIndex The index of the series entry to read
	 * @return the series entry
	 */
	protected long[] readSeriesEntry(long index, int seriesIndex) {
		if(seriesIndex<0) throw new IllegalArgumentException("The index cannot be < 0", new Throwable());
		if(index<0) throw new IllegalArgumentException("The metric ID cannot be < 0", new Throwable());
		return createUnsafeExcerpt(index).readLongArray(HEADER_OFFSET + (seriesIndex * SERIES_SIZE), SERIES_ENTRY_VALUES);
	}
	
	/**
	 * Writes a series entry for the passed index
	 * @param index The chronicle index to write the series for
	 * @param seriesIndex The series index of the series entry to write
	 * @param values The values to write
	 */
	protected void writeSeriesEntry(long index, int seriesIndex, long[] values) {
		if(seriesIndex<0) throw new IllegalArgumentException("The index cannot be < 0", new Throwable());
		if(index<0) throw new IllegalArgumentException("The metric ID cannot be < 0", new Throwable());
		createUnsafeExcerpt(index).writeLongArray(HEADER_OFFSET + (seriesIndex * SERIES_SIZE), values);
	}
	
	/**
	 * Reads the series start and end timestamps
	 * @param ex The currently placed excerpt
	 * @return the series start and end timestamps
	 */
	protected long[] getSeriesTimes(UnsafeExcerpt<IndexedChronicle> ex) {		
		return ex.readLongArray(H_START, 2);
	}
	
	/**
	 * Reads the series start and end timestamps for the passed chronicle index
	 * @param index the index of the chronicle to read from
	 * @return the series start and end timestamps
	 */
	protected long[] getSeriesTimes(long index) {		
		return getSeriesTimes(createUnsafeExcerpt(index));
	}
	
	
	
	/**
	 * Reads the series start timestamp
	 * @param ex The currently placed excerpt
	 * @return the series start timestamp
	 */
	protected long getSeriesStartTime(UnsafeExcerpt<IndexedChronicle> ex) {
		return ex.readLong(H_START);
	}
	
	/**
	 * Reads the series start timestamp for the passed chronicle index
	 * @param index the index of the chronicle to read from
	 * @return the series start timestamp
	 */
	protected long getSeriesStartTime(long index) {
		return getSeriesStartTime(createUnsafeExcerpt(index));
	}	
	
	/**
	 * Reads the series end timestamp
	 * @param ex The currently placed excerpt
	 * @return the series end timestamp
	 */
	protected long getSeriesEndTime(UnsafeExcerpt<IndexedChronicle> ex) {
		return ex.readLong(H_END);
	}	
	
	/**
	 * Reads the series end timestamp for the passed chronicle index
	 * @param index the index of the chronicle to read from
	 * @return the series end timestamp
	 */
	protected long getSeriesEndTime(long index) {
		return getSeriesEndTime(createUnsafeExcerpt(index));
	}	
	
	
	/**
	 * Reads the series entry count
	 * @param ex The currently placed excerpt
	 * @return the series entry count
	 */
	protected long getSeriesEntryCount(UnsafeExcerpt<IndexedChronicle> ex) {
		return ex.readInt(H_SIZE);
	}
	
	/**
	 * Reads the series entry count for the passed chronicle index
	 * @param index the index of the chronicle to read from
	 * @return the series entry count
	 */
	protected long getSeriesEntryCount(long index) {
		return getSeriesEntryCount(createUnsafeExcerpt(index));
	}	
	


	
	/**
	 * Creates a new metric chronicle entry and populates it
	 * @return the index of the new entry
	 */
	public synchronized long createNewMetric() {		
		
		Excerpt<IndexedChronicle> ex = chronicle.createExcerpt();
		ex.startExcerpt(entrySize);
		//ex.startExcerpt(entrySize);		
		long time = SystemClock.period(periodDuration);		
		ex.writeLong(time);
		ex.writeLong(time);
		//ex.writeLongArray(metricId, time, time);
		ex.writeInt(0);
		for(int i = 0; i < periods; i++) {
			ex.writeLong(0);
		}
//		long[] emptyData = new long[periods];
//		ex.writeLongArray(emptyData);
		ex.finish();		
		return ex.index();
	}	
	
	
	/**
	 * Dumps a formatted output of the excerpt at the passed index
	 * @param index The index to dump
	 * @return A formatted string
	 */
	public String dump(long index) {
		StringBuilder b = new StringBuilder();
		UnsafeExcerpt<IndexedChronicle> ex = createUnsafeExcerpt(index);		
		b.append("Start Period:").append(new Date(ex.readLong())).append("\n");
		b.append("End Period:").append(new Date(ex.readLong())).append("\n");
		int size = ex.readInt();
		b.append("Period Count:").append(size).append("\n");
		long[] arr = ex.readLongArray(size * SERIES_ENTRY_VALUES);
		b.append("Data:").append(Arrays.toString(arr));
		return b.toString();
	}


	/**
	 * Returns the name of this chronicle tier
	 * @return the name of this chronicle tier
	 */
	@Override
	public String getName() {
		return chronicle.name();
	}



	/**
	 * Returns the size of this chronicle tier
	 * @return the size of this chronicle tier
	 */
	@Override
	public long getSize() {
		return chronicle.size();
	}






	/**
	 * Creates a new excerpt for this chronicle tier
	 * @return a new excerpt for this chronicle tier
	 */
	public Excerpt<IndexedChronicle> createExcerpt() {
		return chronicle.createExcerpt();
	}

	/**
	 * Creates a new unsafe excerpt for this chronicle tier
	 * @return a new unsafe excerpt for this chronicle tier
	 * @see vanilla.java.chronicle.impl.IndexedChronicle#createUnsafeExcerpt()
	 */
	public UnsafeExcerpt<IndexedChronicle> createUnsafeExcerpt() {
		return chronicle.createUnsafeExcerpt();
	}
	
	/**
	 * Creates a new unsafe excerpt for this chronicle tier and attempts to set the index on it.
	 * @param index The index to set the excerpt to 
	 * @return a new unsafe excerpt for this chronicle tier with the index set
	 * @throws IllegalArgumentException thrown if the index does not exist in this chronicle
	 * @see vanilla.java.chronicle.impl.IndexedChronicle#createUnsafeExcerpt()
	 */
	public UnsafeExcerpt<IndexedChronicle> createUnsafeExcerpt(long index) {
		UnsafeExcerpt<IndexedChronicle> ex = chronicle.createUnsafeExcerpt();
		if(!ex.index(index)) throw new IllegalArgumentException("Failed to set the excerpt index to [" + index + "] in tier [" + chronicleName + "]", new Throwable());
		return ex;
	}	
	

	

	


	/**
	 * Returns the positional index after setting it to the passed index
	 * @param indexId The index to set 
	 * @return The new positional index
	 * @see vanilla.java.chronicle.impl.IndexedChronicle#getIndexData(long)
	 */
	public long getIndexData(long indexId) {
		return chronicle.getIndexData(indexId);
	}



	/**
	 * Returns the underlying byte buffer for the chronicle
	 * @param startPosition The position to set on the buffer
	 * @return The underlying byte buffer 
	 * @see vanilla.java.chronicle.impl.IndexedChronicle#acquireDataBuffer(long)
	 */
	public ByteBuffer acquireDataBuffer(long startPosition) {
		return chronicle.acquireDataBuffer(startPosition);
	}



	/**
	 * Sets the position in the buffer
	 * @param startPosition The position
	 * @return the new position
	 * @see vanilla.java.chronicle.impl.IndexedChronicle#positionInBuffer(long)
	 */
	public int positionInBuffer(long startPosition) {
		return chronicle.positionInBuffer(startPosition);
	}



	/**
	 * Sets the index data
	 * @param indexId The index Id
	 * @param indexData The index data
	 * @see vanilla.java.chronicle.impl.IndexedChronicle#setIndexData(long, long)
	 */
	public void setIndexData(long indexId, long indexData) {
		chronicle.setIndexData(indexId, indexData);
	}



	/**
	 * Starts a new excerpt 
	 * @param capacity The capacity of the excerpt
	 * @return The index of the created excerpt
	 * @see vanilla.java.chronicle.impl.IndexedChronicle#startExcerpt(int)
	 */
	public long startExcerpt(int capacity) {
		return chronicle.startExcerpt(capacity);
	}



	/** 
	 * Increments the size of the chronicle
	 * @see vanilla.java.chronicle.impl.IndexedChronicle#incrSize()
	 */
	public void incrSize() {
		chronicle.incrSize();
	}



	/**
	 * Clears the chronicle
	 */
	@Override
	public void clear() {
		chronicle.clear();
	}

	/**
	 * Closes the chronicle
	 */
	@Override
	public void close() {
		chronicle.close();
	}

	/**
	 * Returns the chronicle path 
	 * @return the chroniclePath
	 */
	@Override
	public String getChroniclePath() {
		return chroniclePath;
	}

	/**
	 * Returns the earliest period end timestamp in this tier
	 * @return the earliest period end timestamp in this tier
	 */
	@Override
	public Date getStartPeriod() {
		long dt = startPeriod.get();		
		return dt==Long.MAX_VALUE ? null : new Date(dt);
	}

	/**
	 * Returns the latest period end timestamp in this tier
	 * @return the latest period end timestamp in this tier
	 */
	@Override
	public Date getEndPeriod() {
		long dt = endPeriod.get();		
		return dt==Long.MIN_VALUE ? null : new Date(dt);
	}
	
	/**
	 * Returns the size of the index data file
	 * @return the size of the index data file
	 */
	@Override
	public long getIndexSize() {
		return new File(chroniclePath + ".index").length();
	}
	
	/**
	 * Returns the size of the data file
	 * @return the size of the data file
	 */
	@Override
	public long getDataSize() {
		return new File(chroniclePath + ".data").length();
	}
		
}
