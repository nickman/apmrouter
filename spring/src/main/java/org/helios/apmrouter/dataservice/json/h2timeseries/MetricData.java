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
import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * <p>Title: MetricData</p>
 * <p>Description: Container class for time-series flat-data read from the h2 time-series store</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.dataservice.json.h2timeseries.H2TimeSeriesJSONDataService.MetricData</code></p>
 */
public class MetricData {		
	/** The agent Id */
	@SerializedName("agentId")
	protected final int agentId;
	/** The type Id */
	@SerializedName("typeId")
	protected final int typeId;
	/** The metric namespace */
	@SerializedName("namespace")
	protected final String namespace;
	/** The metric namespace as an array */
	@SerializedName("narr")
	protected final String[] narr;
	/** The metric name */
	@SerializedName("name")
	protected final String name;
	/** The metric Id */
	@SerializedName("id")
	protected final long metricId;
	/** The time-series data for averages */
	@SerializedName("avgdata")
	protected final List<long[]> avgdata = new ArrayList<long[]>(); 
	/** The time-series data for mins */
	@SerializedName("mindata")
	protected final List<long[]> mindata = new ArrayList<long[]>();
	/** The time-series data for maxes */
	@SerializedName("maxdata")
	protected final List<long[]> maxdata = new ArrayList<long[]>(); 
	/** The time-series data for sample counts */
	@SerializedName("cntdata")
	protected final List<long[]> cntdata = new ArrayList<long[]>(); 


	/**
	 * Creates a new MetricData
	 * @param rset The result set to populate the metric from.
	 * @throws SQLException thrown on any result set read errors
	 */
	public MetricData(ResultSet rset) throws SQLException {
		agentId = rset.getInt(H2TimeSeriesJSONDataService.AGENT_ID);
		typeId = rset.getInt(H2TimeSeriesJSONDataService.TYPE_ID);
		namespace = rset.getString(H2TimeSeriesJSONDataService.NAMESPACE);
		Object[] onarr = (Object[])rset.getArray(H2TimeSeriesJSONDataService.NARR).getArray();
		narr = new String[onarr.length];
		System.arraycopy(onarr, 0, narr, 0, onarr.length);
		name = rset.getString(H2TimeSeriesJSONDataService.NAME);
		metricId = rset.getLong(H2TimeSeriesJSONDataService.METRIC_ID);
		extractData(rset);
//		while(rset.next()) {
//			if(rset.getLong(H2TimeSeriesJSONDataService.METRIC_ID)==metricId) {
//				extractData(rset);
//			} else {
//				rset.previous();
//				break;
//			}
//		}
//		alldata.add(new MetricDataSeries(rset));
//		while(rset.next()) {
//			if(rset.getLong(H2TimeSeriesJSONDataService.METRIC_ID)==metricId) {
//				alldata.add(new MetricDataSeries(rset));
//			} else {
//				rset.previous();
//				break;
//			}
//		}
	}
	
	/**
	 * Extracts the time-series rows from the result set
	 * @param rset The result set
	 * @throws SQLException thrown on any result-set processing error
	 */
	protected void extractData(ResultSet rset) throws SQLException {
		long ts = rset.getTimestamp(H2TimeSeriesJSONDataService.TS).getTime();
		long min = rset.getLong(H2TimeSeriesJSONDataService.MIN);
		mindata.add(new long[]{ts, min});
		long max = rset.getLong(H2TimeSeriesJSONDataService.MAX);
		maxdata.add(new long[]{ts, max});
		long avg = rset.getLong(H2TimeSeriesJSONDataService.AVG);
		avgdata.add(new long[]{ts, avg});
		long cnt = rset.getInt(H2TimeSeriesJSONDataService.CNT);
		cntdata.add(new long[]{ts, cnt});
	}
	
}