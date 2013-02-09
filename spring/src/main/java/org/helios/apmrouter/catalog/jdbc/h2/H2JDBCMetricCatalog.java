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
package org.helios.apmrouter.catalog.jdbc.h2;

import java.net.InetSocketAddress;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.helios.apmrouter.catalog.DChannelEvent;
import org.helios.apmrouter.catalog.DChannelEventType;
import org.helios.apmrouter.catalog.EntryStatus;
import org.helios.apmrouter.catalog.MetricCatalogService;
import org.helios.apmrouter.catalog.EntryStatus.EntryStatusChange;
import org.helios.apmrouter.collections.ConcurrentLongSlidingWindow;
import org.helios.apmrouter.collections.ConcurrentLongSortedSet;
import org.helios.apmrouter.collections.LongSlidingWindow;
import org.helios.apmrouter.destination.chronicletimeseries.ChronicleTSManager;
import org.helios.apmrouter.destination.chronicletimeseries.ChronicleTier;
import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.metric.MetricType;
import org.helios.apmrouter.metric.catalog.ICEMetricCatalog;
import org.helios.apmrouter.metric.catalog.IDelegateMetric;
import org.helios.apmrouter.server.ServerComponentBean;
import org.helios.apmrouter.server.services.session.DecoratedChannel;
import org.helios.apmrouter.util.SystemClock;
import org.helios.apmrouter.util.SystemClock.ElapsedTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;




/**
 * <p>Title: H2JDBCMetricCatalog</p>
 * <p>Description: The H2 implementation of the {@link MetricCatalogService}. When realtime is set to true
 * host, agent and metric timestamps are kept realtime with respect to their <i>last seen</i> timestamp.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.catalog.jdbc.H2JDBCMetricCatalog</code></p>
 */

public class H2JDBCMetricCatalog extends ServerComponentBean implements MetricCatalogService {
	/** The h2 datasource */
	protected DataSource ds = null;
	/** The Chronicle time-series manager */
	protected ChronicleTSManager chronicleManager = null;
	/** The Chronicle time-series live tier */
	protected ChronicleTier liveTier= null;
	
	/** Indicates if the metric catalog should be kept real time */
	protected boolean realtime = false;
	
	/** Sliding windows of catalog call elapsed times in ns. */
	protected final LongSlidingWindow elapsedTimesNs = new ConcurrentLongSlidingWindow(15);
	/** Sliding windows of catalog call elapsed times in ms. */
	protected final LongSlidingWindow elapsedTimesMs = new ConcurrentLongSlidingWindow(15); 
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStart()
	 */
	@Override
	public void doStart() throws Exception {
		if(realtime) {
			info("\n\t#############################\n\tMetric Catalog [", getClass().getSimpleName(), "] is REALTIME\n\t#############################\n");
		}
		if(chronicleManager!=null) liveTier = chronicleManager.getLiveTier();
		chronicleManager.addStatusListener(this);
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = ds.getConnection();
			//MERGE INTO TEST KEY(ID) VALUES(2, 'World')
			ps = conn.prepareStatement("MERGE INTO TRACE_TYPE KEY(TYPE_ID) VALUES(?,?)");
			for(MetricType mt: MetricType.values()) {
				ps.setInt(1, mt.ordinal());
				ps.setString(2, mt.name());
				ps.addBatch();
			}
			ps.executeBatch();	
			ps.close();
			
		} catch (Exception e) {
			throw new RuntimeException("Failed to add metric types", e);
		} finally {
			if(ps!=null) try { ps.close(); } catch (Exception e) {/* No Op */}
			if(conn!=null) try { conn.close(); } catch (Exception e) {/* No Op */}
		}
		//SharedChannelGroup.getInstance().addSessionListener(this);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.catalog.EntryStatusChangeListener#onEntryStatusChange(java.util.Map)
	 */
	@Override
	public void onEntryStatusChange(Map<EntryStatus, EntryStatusChange> changeMap) {
		Connection conn = null;
		PreparedStatement ps = null;
		long start = System.currentTimeMillis();
		try {
			conn = ds.getConnection();
			ps = conn.prepareStatement("UPDATE METRIC SET STATE=?, LAST_SEEN=? WHERE METRIC_ID=?");
			for(Map.Entry<EntryStatus, EntryStatusChange> entry: changeMap.entrySet()) {
				byte status = entry.getKey().byteOrdinal();
				ConcurrentLongSortedSet metricIds = entry.getValue().getMetricIds();
				Timestamp ts = new Timestamp(entry.getValue().getTimestamp());				
				for(int i = 0; i < metricIds.size(); i++) {
					ps.setByte(1, status);
					ps.setTimestamp(2, ts);
					ps.setLong(3, metricIds.get(i));
					ps.addBatch();
				}
				ps.executeBatch();
			}
			ps.close(); ps = null;
			conn.close(); conn = null; 
			long elapsed = System.currentTimeMillis()-start;
			if("TRACE".equals(getLevel())) {
				trace(new StringBuilder(EntryStatus.renderStatusCounts(changeMap)).append("\n\tUpdate Elapsed:").append(elapsed).append(" ms."));
			}
		} catch (Exception e) {
			throw new RuntimeException("Exception processing entry status updates", e);
		} finally {
			if(ps!=null) try { ps.close(); } catch (Exception e) {/* No Op */}
			if(conn!=null) try { conn.close(); } catch (Exception e) {/* No Op */}
		}
		
	}
	

	
	/** The SQL to fetch a delegate metric ID from a token */
	public static final String GET_METRIC_SQL = "SELECT HOST.NAME, AGENT.NAME, TYPE_ID, NAMESPACE, METRIC.NAME "
			+ "FROM HOST, AGENT, METRIC " 
			+ "WHERE AGENT.HOST_ID = HOST.HOST_ID AND METRIC.AGENT_ID = AGENT.AGENT_ID " 
			+ "AND METRIC_ID = ?";
	
	/**
	 * Returns the delegate metric for the passed token
	 * @param token the token
	 * @return a delegate metric ID
	 */
	@Override
	public IDelegateMetric getMetricID(long token) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rset = null;
		try {
			incr("TokenLookups");
			conn = ds.getConnection();
			ps = conn.prepareStatement(GET_METRIC_SQL);
			ps.setLong(1, token);
			rset = ps.executeQuery();
			if(!rset.next()) {
				return null;
			}
			// MetricLastTimeSeenService
			
			CharSequence[] namespace = rset.getString(4).replaceFirst("/", "").split("/");
			ICEMetricCatalog.getInstance().setToken(token, rset.getString(1), rset.getString(2), rset.getString(5), MetricType.valueOf(rset.getInt(3)), namespace);
			return ICEMetricCatalog.getInstance().get(token);
		} catch (SQLException sex) {
			sex.printStackTrace(System.err);
			return null;
		} finally {
			if(rset!=null) try { rset.close(); } catch (Exception e) {/* No Op */}
			if(ps!=null) try { ps.close(); } catch (Exception e) {/* No Op */}
			if(conn!=null) try { conn.close(); } catch (Exception e) {/* No Op */}
		}				
	}
	
	/**
	 * Finds the assigned metric ID for the passed host/agent/name and namespace
	 * @param host The host name
	 * @param agent The agent name
	 * @param namespace The metric namespace
	 * @param name The metric name
	 * @return The metric ID or -1 if one was not found
	 */
	public long isAssigned(String host, String agent, String namespace, String name)  {
		Connection conn = null;
		CallableStatement cs = null;
		try {
			conn = ds.getConnection();
			cs = conn.prepareCall("? = CALL ASSIGNED(?,?,?,?)");
			cs.registerOutParameter(1, Types.NUMERIC);
			cs.setNull(1, Types.NULL);
			cs.setString(2, host);
			cs.setString(3, agent);
			cs.setString(4, namespace);
			cs.setString(5, name);
			cs.execute();			
			return cs.getLong(1);
		} catch (Exception ex) {
			throw new RuntimeException("Assign exception", ex);
		} finally {
			if(cs!=null) try { cs.close(); } catch (Exception ex) {/* No Op */}
			if(conn!=null) try { conn.close(); } catch (Exception ex) {/* No Op */}
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.catalog.MetricCatalogService#getID(long, java.lang.String, java.lang.String, int, java.lang.String, java.lang.String)
	 */
	@Override
	public long getID(long tokenRef, String host, String agent, int typeId, String namespace, String name) {
		if(tokenRef!=-1 && !realtime) return 0;
		SystemClock.startTimer();
		final long token = tokenRef!=-1 ? tokenRef : liveTier.createNewMetric();
		incr("CallCount");		
		Connection conn = null;
		CallableStatement cs = null;		
		PreparedStatement ps = null;
		try {
			conn = ds.getConnection();
			cs = realtime ? conn.prepareCall("? = CALL TOUCH(?,?,?,?,?,?)") : conn.prepareCall("? = CALL GET_ID(?,?,?,?,?,?)");
			cs.registerOutParameter(1, Types.NUMERIC);
			cs.setNull(1, Types.NULL);			
			cs.setLong(2, token);
			cs.setString(3, host);
			cs.setString(4, agent);
			cs.setInt(5, typeId);
			cs.setString(6, namespace);
			cs.setString(7, name);
			cs.execute();
			long id = cs.getLong(1);
			incr("AssignedMetricIDs");
			ElapsedTime et = SystemClock.endTimer();
			elapsedTimesNs.insert(et.elapsedNs);
			elapsedTimesMs.insert(et.elapsedMs);			
			return id;
		} catch (Exception e) {
			error("Failed to get ID for [" , String.format("%s/%s%s:%s", host, agent, namespace, name) , "]", e);
			Throwable cause = e.getCause();
			if(cause!=null) cause.printStackTrace(System.err);
			//throw new RuntimeException("Failed to get ID", e);
			return 0;
		} finally {
			if(ps!=null) try { ps.close(); } catch (Exception e) {/* No Op */}
			if(cs!=null) try { cs.close(); } catch (Exception e) {/* No Op */}
			if(conn!=null) try { conn.close(); } catch (Exception e) {/* No Op */}
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.catalog.MetricCatalogService#touch(java.util.Collection)
	 */
	public int touch(Collection<IMetric> metrics) {
		if(realtime && metrics!=null && !metrics.isEmpty()) {
			Connection conn = null;
			PreparedStatement ps = null;
			try {
				conn = ds.getConnection();
				ps = conn.prepareStatement("UPDATE METRIC SET LAST_SEEN = systimestamp WHERE METRIC_ID = ?");
				for(IMetric metric: metrics) {
					ps.setLong(1, metric.getMetricId().getToken());
					ps.addBatch();
				}
				return ps.executeBatch().length;
			} catch (Exception ex) {
				error("Failed to touch timestamps on [" + metrics.size() + "] metrics", ex);
				return 0;
			} finally {
				if(ps!=null) try { ps.close(); } catch (Exception e) {/* No Op */}
				if(conn!=null) try { conn.close(); } catch (Exception e) {/* No Op */}
			}
		}
		return 0;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.catalog.MetricCatalogService#hostAgentState(boolean, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public DChannelEvent hostAgentState(boolean connected, String host, String ip, String agent, String agentURI) {
		SystemClock.startTimer();
		incr("CallCount");		
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rset = null;
		try {
			conn = ds.getConnection();
			ps = conn.prepareStatement("CALL HOSTAGENTSTATE(?,?,?,?,?)");		
			ps.setBoolean(1, connected);
			ps.setString(2, host);
			ps.setString(3, ip);
			ps.setString(4, agent);
			ps.setString(5, agentURI);
			rset = ps.executeQuery();
			rset.next();
			int agentCount = rset.getInt(1);
			int hostId = rset.getInt(2);
			int agentId = rset.getInt(3);
			String[] domain = rset.getString(4).split("\\.");
			ElapsedTime et = SystemClock.endTimer();
			elapsedTimesNs.insert(et.elapsedNs);
			elapsedTimesMs.insert(et.elapsedMs);	
			return DChannelEvent.newEvent(connected ? DChannelEventType.IDENT : DChannelEventType.CLOSED, 
					domain, host, hostId, agent, agentId, 
					connected ? agentCount==1 : agentCount<1 
					);
		} catch (Exception e) {
			error("Failed to update host/agent state for [" , String.format("%s/%s:%s", connected, host, agent) , "]", e);
			Throwable cause = e.getCause();
			if(cause!=null) cause.printStackTrace(System.err);
			return null;
		} finally {
			if(rset!=null) try { rset.close(); } catch (Exception e) {/* No Op */}
			if(ps!=null) try { ps.close(); } catch (Exception e) {/* No Op */}
			if(conn!=null) try { conn.close(); } catch (Exception e) {/* No Op */}			
		}
	}
	
	/** The base host SQL */
	public static final String LIST_HOSTS_SQL = "SELECT NAME, HOST_ID FROM HOST";
	/** The online host SQL */
	public static final String LIST_ONLINE_HOSTS_SQL = "SELECT NAME, HOST_ID FROM HOST WHERE CONNECTED IS NOT NULL";
	
	
	/**
	 * Lists registered hosts
	 * @param onlineOnly If true, only lists online hosts
	 * @return A map of host names keyed by host ID
	 */
	public Map<Integer, String> listHosts(boolean onlineOnly) {
		SystemClock.startTimer();
		incr("CallCount");		
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rset = null;
		Map<Integer, String> map = new HashMap<Integer, String>();
		try {
			conn = ds.getConnection();
			ps = conn.prepareStatement(onlineOnly ? LIST_ONLINE_HOSTS_SQL : LIST_HOSTS_SQL);
			rset = ps.executeQuery();
			while(rset.next()) {
				map.put(rset.getInt(2), rset.getString(1));
			}
			ElapsedTime et = SystemClock.endTimer();
			elapsedTimesNs.insert(et.elapsedNs);
			elapsedTimesMs.insert(et.elapsedMs);
			return map;
		} catch (Exception e) {
			error("Failed to list hosts" , e);
			Throwable cause = e.getCause();
			if(cause!=null) cause.printStackTrace(System.err);
			throw new RuntimeException("Failed to list hosts" , e);
		} finally {
			if(rset!=null) try { rset.close(); } catch (Exception e) {/* No Op */}
			if(ps!=null) try { ps.close(); } catch (Exception e) {/* No Op */}
			if(conn!=null) try { conn.close(); } catch (Exception e) {/* No Op */}
		}
		
	}
	
	
	
	/**
	 * Sets the h2 datasource
	 * @param ds the h2 datasource
	 */
	@Autowired(required=true)
	@Qualifier("H2DataSource")
	public void setDs(DataSource ds) {
		this.ds = ds;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponent#getSupportedMetricNames()
	 */
	@Override
	public Set<String> getSupportedMetricNames() {
		Set<String> metrics = new HashSet<String>(super.getSupportedMetricNames());
		metrics.add("AssignedMetricIDs");
		metrics.add("CallCount");		
		metrics.add("TokenLookups");
		return metrics;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponent#resetMetrics()
	 */
	@Override
	public void resetMetrics() {
		super.resetMetrics();
		elapsedTimesNs.clear();
		elapsedTimesMs.clear();
	}
	
	/**
	 * Returns the number of assigned metric IDs
	 * @return the number of assigned metric IDs
	 */
	@ManagedMetric(category="MetricCatalogService", metricType=org.springframework.jmx.support.MetricType.COUNTER, description="The number of assigned metric IDs")
	public long getAssignedMetricIDs() {
		return getMetricValue("AssignedMetricIDs");
	}
	
	/**
	 * Returns the number of token lookups
	 * @return the number of token lookups
	 */
	@ManagedMetric(category="MetricCatalogService", metricType=org.springframework.jmx.support.MetricType.COUNTER, description="The number of token lookups")
	public long getTokenLookups() {
		return getMetricValue("TokenLookups");
	}	
	

	/**
	 * Returns the cumulative number of catalog calls
	 * @return the cumulative number of catalog calls
	 */
	@ManagedMetric(category="MetricCatalogService", metricType=org.springframework.jmx.support.MetricType.COUNTER, description="The cumulative number of catalog calls")
	public long getCallCount() {
		return getMetricValue("CallCount");
	}	
	
	/**
	 * Returns the sliding average elapsed time in ns. of the last 15 catalog calls
	 * @return the sliding average elapsed time in ns. of the last 15 catalog calls
	 */
	@ManagedMetric(category="MetricCatalogService", metricType=org.springframework.jmx.support.MetricType.GAUGE, description="The sliding average elapsed time in ns. of the last 15 catalog calls")
	public long getAverageCallTimeNs() {
		return elapsedTimesNs.avg();
	}
	
	/**
	 * Returns the sliding average elapsed time in ms. of the last 15 catalog calls
	 * @return the sliding average elapsed time in ms. of the last 15 catalog calls
	 */
	@ManagedMetric(category="MetricCatalogService", metricType=org.springframework.jmx.support.MetricType.GAUGE, description="The sliding average elapsed time in ms. of the last 15 catalog calls")
	public long getAverageCallTimeMs() {
		return elapsedTimesMs.avg();
	}	
	

	/**
	 * Indicates if the metric catalog is real time 
	 * @return true if the metric catalog is real time , false otherwise
	 */
	@Override
	@ManagedAttribute(description="Indicates if the metric catalog is real time ")
	public boolean isRealtime() {
		return realtime;
	}

	/**
	 * Sets the realtime attribute of the metric catalog
	 * @param realtime true for a realtime metric catalog, false otherwise
	 */
	@Override
	@ManagedAttribute(description="Indicates if the metric catalog is real time ")
	public void setRealtime(boolean realtime) {
		this.realtime = realtime;
	}

	/**
	 * <p>NoOp</p>
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.services.session.ChannelSessionListener#onConnectedChannel(org.helios.apmrouter.server.services.session.DecoratedChannel)
	 */
	@Override
	public void onConnectedChannel(DecoratedChannel channel) {
		// No OP
	}

	/**
	 * <p>Updates the catalog service to mark the agent down if the agent name is not null or empty</p>
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.services.session.ChannelSessionListener#onClosedChannel(org.helios.apmrouter.server.services.session.DecoratedChannel)
	 */
	@Override
	public DChannelEvent onClosedChannel(DecoratedChannel channel) {
		if(channel.getAgent()!=null && !channel.getAgent().trim().isEmpty()) {
			DChannelEvent result = hostAgentState(false, channel.getHost(), ((InetSocketAddress)channel.getRemoteAddress()).getAddress().getHostAddress(), channel.getAgent(), channel.getType() + "/" + channel.getRemoteAddress().toString());
			info("Marked [", channel.getHost(), "/", channel.getAgent(), "] DOWN. Context:" + result);
			return result;
		}
		return null;
	}

	/**
	 * <p>Updates the catalog service to mark the agent up if the agent name is not null or empty</p>
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.services.session.ChannelSessionListener#onIdentifiedChannel(org.helios.apmrouter.server.services.session.DecoratedChannel)
	 */
	@Override
	public DChannelEvent onIdentifiedChannel(DecoratedChannel channel) {
		if(channel.getAgent()!=null && !channel.getAgent().trim().isEmpty()) {
			DChannelEvent result = hostAgentState(true, channel.getHost(), ((InetSocketAddress)channel.getRemoteAddress()).getAddress().getHostAddress(), channel.getAgent(), channel.getURI());
			info("Marked [", channel.getHost(), "/", channel.getAgent(), "] UP. Context:" + result);
			return result;
		}
		return null;
	}

	/**
	 * Sets the chronicle time-series manager
	 * @param chronicleManager the chronicleManager to set
	 */
	public void setChronicleManager(ChronicleTSManager chronicleManager) {
		this.chronicleManager = chronicleManager;
	}
}

