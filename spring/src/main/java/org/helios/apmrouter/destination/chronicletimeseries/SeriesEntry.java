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

import vanilla.java.chronicle.impl.IndexedChronicle;
import vanilla.java.chronicle.impl.UnsafeExcerpt;

/**
 * <p>Title: SeriesEntry</p>
 * <p>Description: Represents a time-series tier for one metric type within one tier</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.chronicletimeseries.SeriesEntry</code></p>
 */

public class SeriesEntry {
	protected final long metricId;
	protected long startPeriod = -1;
	protected long endPeriod = -1;
	protected int periodCount = -1;
	protected final Map<Long, long[]> periods = new TreeMap<Long, long[]>();
	
	SeriesEntry(UnsafeExcerpt<IndexedChronicle> ex, long metricId, boolean includePeriods) {
		if(!ex.index(metricId)) throw new IllegalArgumentException("Invalid index [" + metricId + "]", new Throwable());
		this.metricId = metricId;
		startPeriod = ex.readLong();
		endPeriod = ex.readLong();		
		periodCount = ex.readInt();		
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
}
