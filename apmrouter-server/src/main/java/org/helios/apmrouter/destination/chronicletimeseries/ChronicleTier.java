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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.helios.apmrouter.catalog.EntryStatus;
import org.helios.apmrouter.catalog.EntryStatus.EntryStatusChange;
import org.helios.apmrouter.catalog.jdbc.h2.MetricTrigger;
import org.helios.apmrouter.catalog.jdbc.h2.NewElementTriggers;
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

public class ChronicleTier implements ChronicleTierMXBean, NotificationListener {
	/** The chronicle file name */
	protected final String chronicleName;
	/** The tier pattern */
	protected final String pattern;
	
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
	/** The number of metric offline notifications received */
	protected final AtomicLong offlineNotifications = new AtomicLong(0);
	

	/** The number of values in each series entry */
	protected static final int SERIES_SIZE_IN_LONGS = 5;
	/** The header offset in each chronicle entry, ie. the length of the start time (long), end time (long), the size (int) and the status (byte) */
	protected static final int HEADER_OFFSET = 8 + 8 + 4 + 1;
//	/** The header offset in each chronicle entry, ie. the length of the start time (long), end time (long), the size (int) */
//	protected static final int HEADER_OFFSET = 8 + 8 + 4; 
	
	/** The size of each series entry, ie. longs for TS, MIN, MAX, AVG and CNTS */
	protected static final int SERIES_SIZE_IN_BYTES = SERIES_SIZE_IN_LONGS * 8; 
	
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
	public static final int H_END = H_START + 8; //8;
	/** The series offset for the header entry count */
	public static final int H_SIZE = H_END + 8; //16;
	/** The series offset for the header entry status */
	public static final int H_STATUS = H_SIZE + 4;
	
	/** The JMX ObjectName's prefix to which the tier name is appended to create the full object name */
	public static final String OBJECT_NAME_PREFIX = "org.helios.apmrouter.timeseries:type=chronicle,name=";
	/** The JMX ObjectName for the live tier */
	public static final ObjectName LIVE_TIER_OBJECT_NAME = JMXHelper.objectName("org.helios.apmrouter.timeseries:type=chronicle,name=live");
	/** The JMX notification type for metric status updates */
	public static final String ENTRY_STATUS_UPDATE_TYPE = "metric.event.statechange";
	/** The JMX ObjectName for the MetricTrigger */
	public static final ObjectName METRIC_TRIGGER_OBJECT_NAME = JMXHelper.objectName(NewElementTriggers.class.getPackage().getName(), "trigger", MetricTrigger.class.getSimpleName(), "type", "*UPDATE*");

	/**
	 * Creates a new ChronicleTier and initializes the underlying chronicle
	 * @param tier The time-series model tier for this ChronicleTier
	 * @param parentTier The parent ChronicleTier for this tier or null if it has no parent
	 * @param tsManager The ChronicleTSManager
	 */
	ChronicleTier(Tier tier, ChronicleTier parentTier, ChronicleTSManager tsManager) {
		chronicleName = tier.getName();
		pattern = tier.getPattern();
		log = Logger.getLogger(getClass().getName() + "." + chronicleName);
		parent = parentTier;
		manager = tsManager;
		periods = (int) tier.getPeriodCount();
		periodDuration = tier.getPeriodDuration().seconds;
		periodDurationMs = TimeUnit.MILLISECONDS.convert(periodDuration, TimeUnit.SECONDS);
		objectName = JMXHelper.objectName(new StringBuilder(OBJECT_NAME_PREFIX).append(chronicleName));
		entrySize = HEADER_OFFSET + (SERIES_SIZE_IN_BYTES * periods);
		
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
		// Register for notifications from the metric trigger
		try {
			for(ObjectName mtName: JMXHelper.getHeliosMBeanServer().queryNames(METRIC_TRIGGER_OBJECT_NAME, null)) {
				JMXHelper.getHeliosMBeanServer().addNotificationListener(mtName, this, new NotificationFilter(){
					/**  */
					private static final long serialVersionUID = -8194483721587020208L;
					@Override
					public boolean isNotificationEnabled(Notification notification) {
						return ENTRY_STATUS_UPDATE_TYPE.equals(notification.getType());
					}
				}, null);
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to register as MetricTrigger notification listener", e);
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
			long startTime = ex.readLong();
			long endTime = ex.readLong();		
			tickPeriods(startTime, endTime);
			ex.finish();
		}
		log.info("Initialized tier [" + chronicleName + "] with [" + chronicle.size() + "] entries");
	}
	
	
	/**
	 * Fires a status change event to all registered listeners through the manager
	 * @param changeMap the map containing the status changes
	 */
	protected void fireEventStatusChangeEvent(final Map<EntryStatus, EntryStatusChange> changeMap) {
		manager.fireEventStatusChangeEvent(changeMap);
	}
	
	/** Colon Splitter Pattern */
	public static final Pattern COLON_SPLITTER = Pattern.compile(":");
	
	/**
	 * <p>Listener for notifications from the H2 metric table trigger indicating a status change for a metric.</p>
	 * {@inheritDoc}
	 * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
	 */
	@Override
	public void handleNotification(Notification notification, Object handback) {
		if(notification!=null && ENTRY_STATUS_UPDATE_TYPE.equals(notification.getType())) {
			try {
				String[] msg = COLON_SPLITTER.split(notification.getMessage());
				long metricId = Long.parseLong(msg[0]);
				byte status = Byte.parseByte(msg[1]);
				EntryStatus es = EntryStatus.forByte(status);
				triggeredStatusUpdate(metricId, es);
				offlineNotifications.incrementAndGet();
			} catch (Exception ex) {
				log.error("Failed to process entry change notification", ex);
			}
		}		
	}	
	
	/**
	 * Returns the total number of metric offline notifications received.
	 * @return the total number of metric offline notifications received.
	 */
	public long getOffLineNotificationCount() {
		return offlineNotifications.incrementAndGet();
	}
	
	
	/**
	 * Returns a list containing the period data arrays in this tier for the passed metric Id.
	 * @param metricId The metric Id
	 * @return A list of long arrays
	 */
	public List<long[]> getValues(long metricId) {
		UnsafeExcerpt<IndexedChronicle> ex = createUnsafeExcerpt(metricId);
		try {
			int size = ex.readInt(H_SIZE);
			List<long[]> results = new ArrayList<long[]>(size-1);
			//StringBuilder b = new StringBuilder("Data series for [" + metricId + "]");
//			int cnt = 0;
			for(int i = size-1; i > 0; i--) {
				long[] pData = ex.readLongArray(HEADER_OFFSET + (i*SERIES_SIZE_IN_BYTES) , SERIES_SIZE_IN_LONGS);
				results.add(pData);
				//b.append("\n\tRownum:").append(i).append(":").append(r(pData));
//				cnt++;
			}
//			b.append("\n\tCount:").append(cnt);
//			log.info(b);
			return results;
		} finally {
			ex.finish();
		}
	}
	
	/**
	 * Returns the entry status for the passed metric Id
	 * @param metricId The metric Id to get the status for
	 * @return the entry status
	 */
	public EntryStatus getEntryStatus(long metricId) {
		UnsafeExcerpt<IndexedChronicle> ex = createUnsafeExcerpt(metricId);
		try {
			//return EntryStatus.ACTIVE;
			return EntryStatus.forByte(ex.readByte(H_STATUS));
		} finally {
			ex.finish();
		}		
	}
	
	/**
	 * Returns the entry status name for the passed metric Id
	 * @param metricId The metric Id to get the status for
	 * @return the entry status name
	 */
	@Override
	public String getEntryStatusName(long metricId) {
		return getEntryStatus(metricId).name();
	}

	/**
	 * Adds a new value to the corresponding series in this tier
	 * @param metric The metric add values into the series from
	 * @return the rolled value or null if this operation updates the current period
	 */
	public long[] addValue(IMetric metric) {
		try {
			long metricId = metric.getToken(), timestamp = metric.getTime();
			if(metricId<0) throw new IllegalArgumentException("The metric ID cannot be < 0", new Throwable());
			UnsafeExcerpt<IndexedChronicle> ex = createUnsafeExcerpt(metricId);
			final long period = SystemClock.period(periodDurationMs, timestamp);
			ex.readLong(); ex.readLong();
			int pCount = ex.readInt();	
			if(pCount>this.periods || pCount<0) {
				SeriesEntry se = new SeriesEntry(createUnsafeExcerpt(), metric.getToken(), false);
				Throwable t = new Throwable();
				t.printStackTrace(System.err);
				throw new RuntimeException("Read pCount was [" + pCount + "] but tier count is [" + this.periods + "] for metric [" + metric.getToken() + "/" + metric.getFQN() + "]\n\tSeries Dump:" + se , new Throwable());
			}
			if(pCount==0) {
				ex.finish();
				writeNewPeriod(period, metric);		
				tickPeriods(period, period);
				return null;
			}
			int periodIndex = getPeriodIndex(metric.getToken(), period);
			switch(periodIndex) {
				case -1:
					return null;
				case 0:
					ex.finish();
					writeNewPeriod(period, metric);
					break;				
				case 1:
					updateCurrentPeriod(ex, metric, true);
					break;
				case 2:
					// roll and update 
					return rollAndMerge(pCount, period, metric, ex);
					//break;
				default:
					log.warn("Unexpected period index ["+ periodIndex + "]");
			}
		} catch (Throwable t) {
			log.error("Add Value Error:", t);
		}
		return null;
	}
	
	/**
	 * Triggers a period roll, where the existing period data is rolled one period to the right and a new period is initialized in the current slot.
	 * @param currentSize The current number of periods in the series
	 * @param period The period of the incoming metric
	 * @param metric The metric to write
	 * @param ex The excerpt to use
	 * @return the values of the prior period that was rolled into the next slot
	 */
	protected long[] rollAndMerge(int currentSize, long period, IMetric metric, UnsafeExcerpt<IndexedChronicle> ex) {
		if((currentSize)>this.periods || (currentSize)<1) {
			Throwable t = new Throwable();
			t.printStackTrace(System.err);
			throw new RuntimeException("Attempted to set size to [" + (currentSize+1) + "] but tier count is [" + this.periods + "]", new Throwable());
		}
		final int rollSize;
		final boolean incPos;
		if(currentSize<this.periods) {			
			rollSize = currentSize;
			incPos = true;
		} else {
			rollSize = currentSize-1;
			incPos = false;
		}
		long[] retValues = ex.insertNewPeriod(incPos, SERIES_SIZE_IN_LONGS, rollSize, HEADER_OFFSET, new long[]{period, Long.MAX_VALUE,Long.MIN_VALUE,0,0});
		ex.writeLongArray(H_START, new long[]{period, (period + this.periodDurationMs)});
		tickPeriods(period, period + this.periodDurationMs);		
		updateCurrentPeriod(ex, metric, false);
		if(incPos) ex.writeInt(H_SIZE, (currentSize+1));
		ex.finish();
		return retValues;
	}
	
	/**
	 * Creates and writes the passed metric into a new period
	 * @param period The period
	 * @param metric The metric to write
	 */
	protected void writeNewPeriod(long period, IMetric metric) {
		UnsafeExcerpt<IndexedChronicle> ex = createUnsafeExcerpt(metric.getToken());
		ex.writeLongArray(new long[]{period, period + this.periodDurationMs});
		ex.writeInt(1);
		byte priorStatus = ex.readByte(H_STATUS);
		ex.writeByte(EntryStatus.ACTIVE.byteOrdinal());
		long value = metric.getLongValue();
		ex.writeLongArray(new long[]{period, value, value, value, 1});
		ex.finish();
		if(priorStatus!=EntryStatus.ACTIVE.byteOrdinal()) {
			fireEventStatusChangeEvent(EntryStatus.EntryStatusChange.getChangeMap(SystemClock.time(), metric.getMetricId().getToken(), EntryStatus.ACTIVE));
		}
	}
	
	/**
	 * Writes the passed metric into the current period
	 * @param ex The excerpt to use
	 * @param metric The metric to write
	 * @param finish If true, the write is committed, otherwise, the excerpt is left open
	 */
	protected void updateCurrentPeriod(UnsafeExcerpt<IndexedChronicle> ex, IMetric metric, boolean finish) {		
		long[] values = ex.readLongArray(HEADER_OFFSET, SERIES_SIZE_IN_LONGS);
		long val = metric.getLongValue();
		if(val < values[MIN]) values[MIN] = val;
		if(val > values[MAX]) values[MAX] = val;
		if(values[CNT]==0) {
			values[AVG] = val;
		} else {
			long tmpTotal = values[AVG]+val;
			values[AVG] = tmpTotal==0 ? 0 : tmpTotal/2;
		}
		values[CNT]++;
		byte priorStatus = ex.readByte(H_STATUS);
		ex.write(H_STATUS, EntryStatus.ACTIVE.byteOrdinal());		
		ex.writeLongArray(HEADER_OFFSET, values);
		if(finish) {
			ex.finish();
		}
		if(priorStatus!=EntryStatus.ACTIVE.byteOrdinal()) {
			fireEventStatusChangeEvent(EntryStatus.EntryStatusChange.getChangeMap(SystemClock.time(), metric.getMetricId().getToken(), EntryStatus.ACTIVE));
		}

	}
	
	/**
	 * Finds the starting and ending periods within the periods in the passed excerpt
	 * @param ex The excerpt which has already been set to appropriate index
	 * @return a long array with the starting date and ending date
	 */
	protected long[] getPeriodBoundaries(UnsafeExcerpt<IndexedChronicle> ex) {
		int size = ex.readInt(H_SIZE);
		int offset = (size-1) * SERIES_SIZE_IN_BYTES;		
		return new long[]{
				ex.readLong(HEADER_OFFSET + offset),
				ex.readLong(HEADER_OFFSET)				
		};
	}
	
	/**
	 * Returns the chronicle entry index that the passed period should be merged into, or -1 for a drop.
	 * Currently only returns an index if the merge is for the current period, or the next.
	 * At some point, may merge into earlier periods.
	 * @param metricId The metric Id to get the period index for
	 * @param period The period to get the index for
	 * @return the chronicle entry index or -1 for a drop
	 */
	protected int getPeriodIndex(long metricId, long period) {
		if(chronicle.size()==0) return 0;
		long firstIndex = createUnsafeExcerpt(metricId).readLong(HEADER_OFFSET);
		if(period==firstIndex) return 1;
		if(period>firstIndex) return 2;
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
	 * Returns the series for the passed metric ID
	 * @param metricId The metric ID to get the series for
	 * @return the series pojo
	 */
	@Override
	public SeriesEntryMBean getSeries(long metricId) {
		return new SeriesEntry(createUnsafeExcerpt(), metricId, true);
	}
	
	
	/**
	 * Performs a status check on this entry.
	 * If the state requires updating, the new state will be returned,
	 * otherwise null will be returned.
	 * @param metricId The metric id of the entry to check.
	 * @param currentTime The effective time of this check in ms.
	 * @param staleThreshold The maximum age of the entry  in ms. before it is marked stale
	 * @param offLineThreshold  The maximum age of the entry  in ms. before it is marked offline
	 * @return the changed state or null if there was no change.
	 */
	public EntryStatus statusCheck(long metricId, long currentTime, long staleThreshold, long offLineThreshold) {
		SeriesEntry se = new SeriesEntry(createUnsafeExcerpt(), metricId, false);
		
		final long elapsed = currentTime-se.getStartPeriodTimestamp();
		final EntryStatus status = se.getEntryStatus();
		if(elapsed >= offLineThreshold) {
			if(status!=EntryStatus.OFFLINE) {
				se.updateStatus(EntryStatus.OFFLINE);
				return EntryStatus.OFFLINE;
			}
		} else 	if(elapsed >= staleThreshold) {
			if(status!=EntryStatus.STALE) {
				se.updateStatus(EntryStatus.STALE);
				return EntryStatus.STALE;
			}
		}

		return null;
	}
	
	/**
	 * An update of a metric's entry status in the live tier triggered by the h2 metric table trigger.
	 * Since this is coming from the metric table, no event is needed.
	 * @param metricId The id of the metric to update
	 * @param status The status to update to
	 */
	public void triggeredStatusUpdate(long metricId, EntryStatus status) {
		try {
			SeriesEntry se = new SeriesEntry(createUnsafeExcerpt(), metricId, false);
			se.updateStatus(status);
		} catch (Exception ex) {
			/* No Op */
		}
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
		return createUnsafeExcerpt(index).readLongArray(HEADER_OFFSET + (seriesIndex * SERIES_SIZE_IN_BYTES), SERIES_SIZE_IN_LONGS);
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
		UnsafeExcerpt<IndexedChronicle> ex = createUnsafeExcerpt(index); 
		EntryStatus status = EntryStatus.forByte(ex.readByte(H_STATUS)); 
		ex.writeLongArray(HEADER_OFFSET + (seriesIndex * SERIES_SIZE_IN_BYTES), values);
		if(status!=EntryStatus.ACTIVE) {
			ex.write(H_STATUS, EntryStatus.ACTIVE.byteOrdinal());
			fireEventStatusChangeEvent(EntryStatus.EntryStatusChange.getChangeMap(SystemClock.time(), index, EntryStatus.ACTIVE));
		}
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
		try {
			Excerpt<IndexedChronicle> ex = chronicle.createExcerpt();
			ex.startExcerpt(entrySize);
			long time = SystemClock.period(periodDuration);		
			ex.writeLong(time);
			ex.writeLong(time);
			ex.writeInt(0);
			ex.writeByte(EntryStatus.ACTIVE.byteOrdinal());
			int extendSize = periods * SERIES_SIZE_IN_LONGS;
			for(int i = 0; i < extendSize; i++) {
				ex.writeLong(-1L);
			}
			ex.finish();		
			return ex.index();
		} catch (Exception ex) {
			log.error("Failed to create new metric entry", ex);
			throw new RuntimeException("Failed to create new metric entry", ex);
		}
	}	
	
	
	/**
	 * Dumps a formatted output of the excerpt at the passed index
	 * @param index The index to dump
	 * @param includePeriods Defines if the dump should include period data
	 * @return A formatted string
	 */
	@Override
	public String dump(long index, boolean includePeriods) {
		String s = new SeriesEntry(createUnsafeExcerpt(), index, includePeriods).toString();
		return s;
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
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.chronicletimeseries.ChronicleTierMXBean#getPerMetricDataSize()
	 */
	@Override
	public long getPerMetricDataSize() {
		return periods * SERIES_SIZE_IN_BYTES;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.chronicletimeseries.ChronicleTierMXBean#getPerMetricSize()
	 */
	@Override
	public long getPerMetricSize() {
		return (periods * SERIES_SIZE_IN_BYTES) + HEADER_OFFSET;
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
	
	/**
	 * Returns the tier definition pattern
	 * @return the tier definition pattern
	 */
	@Override
	public String getPattern() {
		return pattern;
	}

	/**
	 * Returns the number of periods in this tier
	 * @return the number of periods in this tier
	 */
	@Override
	public int getPeriodCount() {
		return periods;
	}	
	
	/**
	 * Returns the period duration in seconds for this tier
	 * @return the period duration in seconds for this tier
	 */
	@Override
	public long getPeriodDuration() {
		return periodDuration;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.chronicletimeseries.ChronicleTierMXBean#getPeriodDurationMs()
	 */
	@Override
	public long getPeriodDurationMs() {
		return periodDurationMs;
	}
	
	/**
	 * Utility method to render a period value array to a readable string
	 * @param values The period values
	 * @return A formatted string
	 */
	protected static String r(long[] values) {
		StringBuilder b = new StringBuilder("Period Values:");
		if(values==null) {
			b.append("null");
		} else {
			if(values.length!=SERIES_SIZE_IN_LONGS) {
				b.append("Invalid Size:").append(Arrays.toString(values));
			} else {
				b.append(new Date(values[0])).append("[");
				for(int i = 1; i < SERIES_SIZE_IN_LONGS; i++) {
					b.append(values[i]).append(",");
				}
				b.deleteCharAt(b.length()-1);
				b.append("]");
			}
		}		
		return b.toString();
	}


		
}
