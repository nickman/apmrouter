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
package org.helios.apmrouter.dataservice.json.h2timeseries;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.gson.annotations.SerializedName;

/**
 * <p>Title: MetricDataSeries</p>
 * <p>Description: Simple pojo to contain the json-renderable time series entries for a metric data instance</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.dataservice.json.h2timeseries.H2TimeSeriesJSONDataService.MetricData.MetricDataSeries</code></p>
 */
public class MetricDataSeries {
	/** The series UTC long timestamp */
	@SerializedName("ts")
	protected final long ts;
	/** The minimum value for the associated timestamp range */
	@SerializedName("min")
	protected final long min;
	/** The maximum value for the associated timestamp range */
	@SerializedName("max")
	protected final long max;
	/** The average value for the associated timestamp range */
	@SerializedName("avg")
	protected final long avg;
	/** The total number of samples for the associated timestamp range */
	@SerializedName("cnt")
	protected final int cnt;
	
	/**
	 * Creates a new MetricDataSeries
	 * @param rset The (already navigated) result set to populate the series from.
	 * @throws SQLException thrown on any result set read errors
	 */
	public MetricDataSeries(ResultSet rset) throws SQLException {
		ts = rset.getTimestamp(H2TimeSeriesJSONDataService.TS).getTime();
		min = rset.getLong(H2TimeSeriesJSONDataService.MIN);
		max = rset.getLong(H2TimeSeriesJSONDataService.MAX);
		avg = rset.getLong(H2TimeSeriesJSONDataService.AVG);
		cnt = rset.getInt(H2TimeSeriesJSONDataService.CNT);
	}
}