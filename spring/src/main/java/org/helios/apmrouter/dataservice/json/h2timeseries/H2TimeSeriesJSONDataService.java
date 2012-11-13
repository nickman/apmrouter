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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.helios.apmrouter.collections.ConcurrentLongSlidingWindow;
import org.helios.apmrouter.dataservice.json.JSONRequestHandler;
import org.helios.apmrouter.dataservice.json.JsonRequest;
import org.helios.apmrouter.server.ServerComponentBean;
import org.helios.apmrouter.util.SystemClock;
import org.helios.apmrouter.util.SystemClock.ElapsedTime;
import org.jboss.netty.channel.Channel;
import org.json.JSONArray;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;

/**
 * <p>Title: H2TimeSeriesJSONDataService</p>
 * <p>Description: JSON Data Service for the local H2 time-series data</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.dataservice.json.h2timeseries.H2TimeSeriesJSONDataService</code></p>
 */
@JSONRequestHandler(name="h2ts")
public class H2TimeSeriesJSONDataService extends ServerComponentBean {
	/** The H2 data source */
	protected DataSource dataSource = null;
	
	/** The last liveData elapsed query time in ms */
	protected final ConcurrentLongSlidingWindow lastElapsedLiveData = new ConcurrentLongSlidingWindow(60);


	/** The column ID for the agent */
	public static final int AGENT_ID = 1;
	/** The column ID for the type */
	public static final int TYPE_ID = 2;
	/** The column ID for the namespace */
	public static final int NAMESPACE = 3;
	/** The column ID for the array-ized namespace */
	public static final int NARR = 4;
	/** The column ID for the metric name */
	public static final int NAME = 5;
	/** The column ID for the metric id */
	public static final int METRIC_ID = 6;  
	/** The column ID for the series timestamp */
	public static final int TS = 7;
	/** The column ID for the min value */
	public static final int MIN = 8;
	/** The column ID for the max value */
	public static final int MAX = 9;
	/** The column ID for the average value */
	public static final int AVG = 10;
	/** The column ID for the sample count */
	public static final int CNT = 11;
	/** The default IDs argument */
	private static final int[] DEFAULT_ID = {};

	/**
	 * Returns live time-series data
	 * @param request The JSON request
	 * @param channel The channel to respond on
	 */
	@JSONRequestHandler(name="liveData")
	public void liveData(JsonRequest request, Channel channel)   {
		SystemClock.startTimer();
		JSONArray ids = request.getArgumentOrNull("IDS", JSONArray.class);
		if(ids.length()==0) {
			channel.write(request.response().setContent("NOOP"));
			return;
		}
		Connection conn = null;
		Statement st = null;
		ResultSet rset = null;
		List<MetricData> response = new ArrayList<MetricData>(ids.length());
		try {
			StringBuilder sql = new StringBuilder("SELECT * FROM RICH_METRIC_DATA WHERE ID IN (");
			sql.append(ids.toString().replace("[", "").replace("]", "")).append(")");
			sql.append(" ORDER BY ID, TS");
			conn = dataSource.getConnection();
			st = conn.createStatement();
			rset = st.executeQuery(sql.toString());
			MetricData md = null;
			long currentId = -1;
			while(rset.next()) {
				long id = rset.getLong(H2TimeSeriesJSONDataService.METRIC_ID);
				if(id!=currentId) {					
					if(md==null) {
						md = new MetricData(rset);
						response.add(md);
					} else {
						response.add(md);
						md = new MetricData(rset);
					}
					currentId = id;
				} else {
					md.extractData(rset);
				}
				
			}
			channel.write(request.response().setContent(response));
			ElapsedTime et = SystemClock.endTimer();
			lastElapsedLiveData.insert(et.elapsedNs);
		} catch (Exception ex) {
			error("Failed to execute livedata for ", ids, "]", ex);
			channel.write(request.response().setContent(ex));
		} finally {
			if(rset!=null) try { rset.close(); } catch (Exception e) {}
			if(st!=null) try { st.close(); } catch (Exception e) {}
			if(conn!=null) try { conn.close(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Returns the rolling average of the liveData query times in ms
	 * @return the rolling average of the liveData query times in ms
	 */
	@ManagedMetric(category="H2TimeSeriesJSONDataService", metricType=MetricType.GAUGE, description="the rolling average of the liveData query times in ms")
	public long getRollingLiveDataQueryTimeMs() {
		return TimeUnit.MILLISECONDS.convert(getRollingLiveDataQueryTimeNs(), TimeUnit.NANOSECONDS); 
	}
	
	/**
	 * Returns the rolling average of the liveData query times in ns
	 * @return the rolling average of the liveData query times in ns
	 */
	@ManagedMetric(category="H2TimeSeriesJSONDataService", metricType=MetricType.GAUGE, description="the rolling average of the liveData query times in ns")
	public long getRollingLiveDataQueryTimeNs() {
		return lastElapsedLiveData.avg(); 
	}
	

	/**
	 * Sets the H2 datasource
	 * @param dataSource the dataSource to set
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
	
}
