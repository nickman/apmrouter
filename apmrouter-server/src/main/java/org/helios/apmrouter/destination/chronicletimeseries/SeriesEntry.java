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

import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import org.helios.apmrouter.catalog.EntryStatus;

import vanilla.java.chronicle.impl.IndexedChronicle;
import vanilla.java.chronicle.impl.UnsafeExcerpt;

/**
 * <p>Title: SeriesEntry</p>
 * <p>Description: Represents a time-series tier for one metric type within one tier</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.chronicletimeseries.SeriesEntry</code></p>
 */

public class SeriesEntry implements SeriesEntryMBean {
	/** The metric ID */
	protected final long metricId;
	/** The start time of the first period in the window */
	protected long startPeriod = -1;
	/** The start time of the last period in the window */
	protected long endPeriod = -1;
	/** The number of periods in the window */
	protected int periodCount = -1;
	/** The up/down status for this entry */
	protected EntryStatus status = EntryStatus.ACTIVE;
	/** The excerpt used to read/write this entry */
	protected final UnsafeExcerpt<IndexedChronicle> excerpt;
	
	/** The values recorded in each period in the window keyed by the timestamp of the period */
	protected final Map<Long, long[]> periods = new TreeMap<Long, long[]>();
	
	/**
	 * Creates a new SeriesEntry for the specified metric
	 * @param ex The excerpt to read with
	 * @param metricId The metric id to read
	 * @param includePeriods true to incliude the period detail, false for the header only
	 */
	SeriesEntry(UnsafeExcerpt<IndexedChronicle> ex, long metricId, boolean includePeriods) {
		if(!ex.index(metricId)) throw new InvalidIndexExcetpion("Invalid index [" + metricId + "]", new Throwable());
		excerpt = ex;
		this.metricId = metricId;
		startPeriod = ex.readLong();
		endPeriod = ex.readLong();		
		periodCount = ex.readInt();		
		status = EntryStatus.forByte(ex.readByte());
		if(includePeriods) {
			for(int i = 0; i < periodCount; i++) {
				long ts = ex.readLong();
				long[] values = new long[4];
				for(int x = 0; x < 4; x++) {
					values[x] = ex.readLong();
				}
				periods.put(ts, values);
			}
		}
	}
	
	public String toString() {
		StringBuilder b = new StringBuilder("Metric ID:");
		b.append(metricId);
		b.append("\nStart Period:").append(new Date(startPeriod));
		b.append("\nEnd Period:").append(new Date(endPeriod));
		b.append("\nStatus:").append(status.name());
		b.append("\nPeriod Count:").append(periodCount).append("/").append(periods.size());
		if(!periods.isEmpty()) {
			for(Map.Entry<Long, long[]> pentry: periods.entrySet()) {
				long[] arr = pentry.getValue();
				b.append("\n\tPeriod:").append(new Date(pentry.getKey()))
					.append(" Min:").append(arr[0])
					.append(" Max:").append(arr[1])
					.append(" Avg:").append(arr[2])
					.append(" Cnt:").append(arr[3]);
			}
		}
		return b.toString();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.chronicletimeseries.SeriesEntryMBean#getMetricId()
	 */
	@Override
	public long getMetricId() {
		return metricId;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.chronicletimeseries.SeriesEntryMBean#getStartPeriod()
	 */
	@Override
	public Date getStartPeriod() {
		return new Date(startPeriod);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.chronicletimeseries.SeriesEntryMBean#getStartPeriodTimestamp()
	 */
	@Override
	public long getStartPeriodTimestamp() {
		return startPeriod;
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.chronicletimeseries.SeriesEntryMBean#getEndPeriod()
	 */
	@Override
	public Date getEndPeriod() {
		return new Date(endPeriod);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.chronicletimeseries.SeriesEntryMBean#getEndPeriodTimestamp()
	 */
	@Override
	public long getEndPeriodTimestamp() {
		return endPeriod;
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.chronicletimeseries.SeriesEntryMBean#getPeriodCount()
	 */
	@Override
	public int getPeriodCount() {
		return periodCount;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.chronicletimeseries.SeriesEntryMBean#getEntryStatus()
	 */
	@Override
	public EntryStatus getEntryStatus() {
		return status;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.chronicletimeseries.SeriesEntryMBean#getPeriods()
	 */
	@Override
	public Map<Long, long[]> getPeriods() {
		return periods;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.chronicletimeseries.SeriesEntryMBean#updateStatus(org.helios.apmrouter.catalog.EntryStatus)
	 */
	@Override
	public EntryStatus updateStatus(final EntryStatus status) {
		EntryStatus tmp = this.status;
		this.status = status;
		this.excerpt.write(ChronicleTier.H_STATUS, this.status.byteOrdinal());
		return this.status!=tmp ? tmp : null; 
	}
}
