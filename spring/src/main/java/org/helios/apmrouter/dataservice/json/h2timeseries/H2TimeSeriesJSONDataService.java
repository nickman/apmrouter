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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.helios.apmrouter.collections.ConcurrentLongSlidingWindow;
import org.helios.apmrouter.dataservice.json.JSONRequestHandler;
import org.helios.apmrouter.dataservice.json.JsonRequest;
import org.helios.apmrouter.dataservice.json.marshalling.GSONJSONMarshaller;
import org.helios.apmrouter.destination.h2timeseries.H2TimeSeriesDestination;
import org.helios.apmrouter.server.ServerComponentBean;
import org.helios.apmrouter.util.SystemClock;
import org.helios.apmrouter.util.SystemClock.ElapsedTime;
import org.jboss.netty.channel.Channel;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
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
	/** The H2 destination for configs */
	protected H2TimeSeriesDestination h2Dest = null;
	/** The GSON marshaller */
	protected GSONJSONMarshaller gsonMarshaller = null;
	/** The time-series STEP in ms. */
	protected long step = -1;
	/** The time-series WIDTH */
	protected long width= -1;
	
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
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStart()
	 */
	@Override
	protected void doStart() throws Exception {
		step = h2Dest.getTimeSeriesStep();
		width = h2Dest.getTimeSeriesWidth();
		MetricData.STEP = step;
		MetricData.WIDTH = width;
	}

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
		CallableStatement cs = null;
		ResultSet rset = null;
		List<MetricData> response = new ArrayList<MetricData>(ids.length());
		try {
//			StringBuilder sql = new StringBuilder("SELECT * FROM RICH_METRIC_DATA WHERE ID IN (");
//			sql.append(ids.toString().replace("[", "").replace("]", "")).append(")");
//			sql.append(" ORDER BY ID, TS");
			StringBuilder sql = new StringBuilder("CALL MV(");
			sql.append(ids.toString().replace("[", "").replace("]", "")).append(")");
			
			
			conn = dataSource.getConnection();
			cs = conn.prepareCall(sql.toString());
			//cs.registerOutParameter(1, Types.OTHER);
			rset = cs.executeQuery();
			Map<Long, Map<Long, Set<long[]>>> metricSet = null;
			long currentId = -1;
			while(rset.next()) {
				long id = rset.getLong(1);
				if(id!=currentId) {					
					if(metricSet==null) {
						metricSet = new HashMap<Long, Map<Long, Set<long[]>>>(1);
						Map<Long, Set<long[]>> tsMap = new TreeMap<Long, Set<long[]>>();
						metricSet.put(id, tsMap);
						tsMap.put(rset.getTimestamp(2).getTime(), new LinkedHashSet<long[]>(Arrays.asList(new long[]{
								rset.getLong(3), rset.getLong(4), rset.getLong(5), rset.getLong(6)
						})));
						
					} else {
						//gsonMarshaller.marshallToChannel(metricSet, channel);
						channel.write(request.response().setContent(metricSet));  // TODO: replace this with a pojo
						metricSet = new HashMap<Long, Map<Long, Set<long[]>>>(1);
						Map<Long, Set<long[]>> tsMap = new TreeMap<Long, Set<long[]>>();
						metricSet.put(id, tsMap);
						tsMap.put(rset.getTimestamp(2).getTime(), new LinkedHashSet<long[]>(Arrays.asList(new long[]{
								rset.getLong(3), rset.getLong(4), rset.getLong(5), rset.getLong(6)
						})));												
					}
					currentId = id;
				} else {
					Map<Long, Set<long[]>> tsMap = metricSet.get(id);
					long ts = rset.getTimestamp(2).getTime();
					Set<long[]> data = tsMap.get(ts);
					if(data==null) {
						data = new LinkedHashSet<long[]>();
						tsMap.put(ts, data);
					}
					data.add(new long[]{rset.getLong(3), rset.getLong(4), rset.getLong(5), rset.getLong(6)});					
				}				
			}
			if(!metricSet.isEmpty()) {
				channel.write(request.response().setContent(metricSet));  // TODO: replace this with a pojo
				//gsonMarshaller.marshallToChannel(metricSet, channel);
			}
//			MetricData md = null;
//			long currentId = -1;
//			while(rset.next()) {
//				long id = rset.getLong(H2TimeSeriesJSONDataService.METRIC_ID);
//				if(id!=currentId) {					
//					if(md==null) {
//						md = new MetricData(rset);
//						response.add(md);
//					} else {
//						response.add(md);
//						md = new MetricData(rset);
//					}
//					currentId = id;
//				} else {
//					md.extractData(rset);
//				}
//				
//			}
//			channel.write(request.response().setContent(response));
			ElapsedTime et = SystemClock.endTimer();
			lastElapsedLiveData.insert(et.elapsedNs);
		} catch (Exception ex) {
			error("Failed to execute livedata for ", ids, "]", ex);
			channel.write(request.response().setContent(ex.toString()));
		} finally {
			if(rset!=null) try { rset.close(); } catch (Exception e) { /* No Op */ }
			if(cs!=null) try { cs.close(); } catch (Exception e) { /* No Op */ }
			if(conn!=null) try { conn.close(); } catch (Exception e) { /* No Op */ }
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

	/**
	 * Sets the H2Destination that provides the ts config
	 * @param h2Dest the H2Destination 
	 */
	@Autowired(required=true)
	public void setH2Dest(H2TimeSeriesDestination h2Dest) {
		this.h2Dest = h2Dest;
	}

	/**
	 * Sets the GSON marshaller
	 * @param gsonMarshaller The GSON marshaller
	 */
	@Autowired(required=true)
	public void setGsonMarshaller(GSONJSONMarshaller gsonMarshaller) {
		this.gsonMarshaller = gsonMarshaller;
	}
	
}
