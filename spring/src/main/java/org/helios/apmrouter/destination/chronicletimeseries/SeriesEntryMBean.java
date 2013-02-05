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

/**
 * <p>Title: SeriesEntryMBean</p>
 * <p>Description: MBean interface to render {@link SeriesEntry} instances as OpenTypes.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.chronicletimeseries.SeriesEntryMBean</code></p>
 */

public interface SeriesEntryMBean {

	/**
	 * Returns the metric ID
	 * @return the metricId
	 */
	public abstract long getMetricId();

	/**
	 * Returns the start time of the first period in the window
	 * @return the start time of the first period in the window
	 */
	public abstract Date getStartPeriod();

	/**
	 * Returns the start time of the last period in the window
	 * @return the start time of the last period in the window
	 */
	public abstract Date getEndPeriod();

	/**
	 * Returns the number of periods in the window
	 * @return the periodCount
	 */
	public abstract int getPeriodCount();
	
	/**
	 * Returns the metric status of this entry
	 * @return the metric status of this entry
	 */
	public abstract EntryStatus getEntryStatus();

	/**
	 * Returns the values recorded in each period in the window keyed by the timestamp of the period
	 * @return the period data
	 */
	public abstract Map<Long, long[]> getPeriods();

}