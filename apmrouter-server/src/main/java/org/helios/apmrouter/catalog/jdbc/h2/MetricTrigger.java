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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.sql.DataSource;

import org.helios.apmrouter.catalog.EntryStatus;
import org.helios.apmrouter.destination.chronicletimeseries.ChronicleTier;
import org.helios.apmrouter.metric.MetricType;
import org.helios.apmrouter.util.SystemClock;

/**
 * <p>Title: MetricTrigger</p>
 * <p>Description: H2 new metric trigger</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.catalog.jdbc.h2.MetricTrigger</code></p>
 */

public class MetricTrigger extends AsynchAbstractTrigger implements MetricTriggerMBean {
	/** The metric type ordinal for the incrementor metric type  */
	protected static final int INCR_ID = MetricType.INCREMENTOR.ordinal();
	/** The metric type ordinal for the interval incrementor metric type  */
	protected static final int INT_INCR_ID = MetricType.INTERVAL_INCREMENTOR.ordinal();
	
	/** The ID of the column containing the metric id for a metric */
	public static final int METRIC_COLUMN_ID = 0;
	/** The ID of the column containing the agent id for a metric */
	public static final int AGENT_COLUMN_ID = 1;
	/** The ID of the column containing the metric type id for a metric */
	public static final int TYPE_COLUMN_ID = 2;
	/** The ID of the column containing the metric namespace for a metric */
	public static final int NAMESPACE_COLUMN_ID = 3;
	/** The ID of the column containing the metric namespace array for a metric */
	public static final int NARR_COLUMN_ID = 4;
	/** The ID of the column containing the metric level for a metric */
	public static final int LEVEL_COLUMN_ID = 5;
	/** The ID of the column containing the metric name for a metric */
	public static final int NAME_COLUMN_ID = 6;
	/** The ID of the column containing the metric first seen timestamp for a metric */
	public static final int FIRST_SEEN_COLUMN_ID = 7;
	/** The ID of the column containing the state for a metric */
	public static final int STATE_COLUMN_ID = 8;
	/** The ID of the column containing the metric last seen timestamp for a metric */
	public static final int LAST_SEEN_COLUMN_ID = 9;
	
	/** The SQL to pre-populate the cache on trigger init */
	public static final String PREPOP_SQL = "SELECT A.AGENT_ID, H.DOMAIN || '/' || H.NAME || '/' || A.NAME FROM HOST H, AGENT A WHERE A.HOST_ID = H.HOST_ID";
	/** The SQL to get the cache entry on a cache miss */
	public static final String CACHE_MISS_SQL = "SELECT H.DOMAIN || '/' || H.NAME || '/' || A.NAME FROM HOST H, AGENT A WHERE A.HOST_ID = H.HOST_ID AND A.AGENT_ID = ?";
	
	
	/** A cache of <b><code>Domain/Host/Agent<code></b> prefixes for each agent Id */
	protected static final Map<Integer, String> metricFqnPrefixCache = new ConcurrentHashMap<Integer, String>(128, 0.75f, 16);
	
	
	/**
	 * <p>Overriden to populate the {@link #metricFqnPrefixCache} cache</p>
	 * {@inheritDoc}
	 * @see org.h2.api.Trigger#init(java.sql.Connection, java.lang.String, java.lang.String, java.lang.String, boolean, int)
	 */
	@Override
	public void init(Connection conn, String schemaName, String triggerName, String tableName, boolean before, int type) throws SQLException {
		super.init(conn, schemaName, triggerName, tableName, before, type);
		PreparedStatement ps = null;
		ResultSet rset = null;
		try {
			ps = conn.prepareStatement(PREPOP_SQL);
			rset = ps.executeQuery();
			while(rset.next()) {
				metricFqnPrefixCache.put(rset.getInt(1), rset.getString(2));
			}
			log.info("Populated AgentPrefix Cache with [" + metricFqnPrefixCache + "] Entries");
		} finally {
			if(rset!=null) try { rset.close(); } catch (Exception e) {/* No Op */}
			if(ps!=null) try { ps.close(); } catch (Exception e) {/* No Op */}			
		}
	}
	
	
	/**
	 * Retrieves the agent prefix for the passed agentId
	 * @param agentId The agentId
	 * @param dataSource The datasource in case the cache misses and the prefix needs to bbe looked up
	 * @return the agent prefix
	 */
	public String getAgentPrefix(int agentId, DataSource dataSource) {
		String prefix = metricFqnPrefixCache.get(agentId);
		if(prefix==null) {
			synchronized(metricFqnPrefixCache) {
				prefix = metricFqnPrefixCache.get(agentId);
				if(prefix==null) {
					Connection conn = null;
					PreparedStatement ps = null;
					ResultSet rset = null;
					try {
						conn = dataSource.getConnection();
						ps = conn.prepareStatement(CACHE_MISS_SQL);
						ps.setInt(agentId, 1);
						rset = ps.executeQuery();
						if(rset.next()) {
							prefix = rset.getString(1);
							metricFqnPrefixCache.put(agentId, prefix);
							return prefix;
						}
						return null;						
					} catch (Exception ex) {
						return null;
					} finally {
						if(rset!=null) try { rset.close(); } catch (Exception e) {/* No Op */}
						if(ps!=null) try { ps.close(); } catch (Exception e) {/* No Op */}			
						if(conn!=null) try { conn.close(); } catch (Exception e) {/* No Op */}
					}					
				}
			}
		}
		return prefix;
	}
	
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.catalog.jdbc.h2.MetricTriggerMBean#getAgentPrefixCacheSize()
	 */
	@Override
	public int getAgentPrefixCacheSize() {
		return metricFqnPrefixCache.size();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.catalog.jdbc.h2.MetricTriggerMBean#flushAgentPrefixCache()
	 */
	@Override
	public void flushAgentPrefixCache() {
		metricFqnPrefixCache.clear();
	}
	
	
	/** The event status message format */
	public static final String EVENT_STATUS_MESSAGE = "%s:%s";
	
	/**
	 * Creates a new MetricTrigger
	 */
	public MetricTrigger() {
		super(new MBeanNotificationInfo(new String[]{NEW_METRIC}, Notification.class.getName(), "A new metric registration event"));
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.catalog.jdbc.h2.AsynchAbstractTrigger#doFire(javax.sql.DataSource, java.lang.Object[], java.lang.Object[])
	 */
	@Override
	protected void doFire(DataSource dataSource, Object[] oldRow, Object[] newRow) {		
		if(TriggerOp.INSERT.isEnabled(type)) {
			short typeId = (Short)newRow[2];
			if(INCR_ID==typeId || INT_INCR_ID==typeId) {
				addIncr(dataSource, (Long)newRow[0], typeId);
			}
			String newMetricType = new StringBuilder(NEW_METRIC).append(getAgentPrefix((Integer)newRow[AGENT_COLUMN_ID], dataSource))
				.append(newRow[NAMESPACE_COLUMN_ID]).append(":").append(newRow[NAME_COLUMN_ID]).toString();
			sendNotification("METRIC", newMetricType, newRow);
		} else if(TriggerOp.UPDATE.isEnabled(type)) {
			if(newRow!=null && oldRow!=null && newRow[STATE_COLUMN_ID] != oldRow[STATE_COLUMN_ID]) {
				sendStateChangeNotification((Long)newRow[METRIC_COLUMN_ID], (byte)newRow[STATE_COLUMN_ID]);				
			}
		}
		callCount.incrementAndGet();
	}
	
	// sendNotif:  String elementType, String type, Object[] newRow
	
	/*
	 * TODO:
	 * Execute trigger asynch
	 * Standardize State Change and New Metric notifications
	 * 		Notif Type:  <prefix> + FQN + [state]
	 * 		Message:  event type, FQN, [state]
	 * 		UserData:  Object[] newRow
	 * 
	 */
	
	protected String generateEventType(DataSource dataSource, String eventType, Object[] newRow) {
		return new StringBuilder(eventType)
			.append(getAgentPrefix((Integer)newRow[AGENT_COLUMN_ID], dataSource))
			.append(newRow[NAMESPACE_COLUMN_ID])
			.append(":")
			.append(newRow[NAME_COLUMN_ID]).toString();
	}
	
	/**
	 * Sends a notification indicating a metric Id has changed state
	 * @param metricId The metric ID
	 * @param status The new {@link EntryStatus} byte ordinal
	 */
	protected void sendStateChangeNotification(long metricId, byte status) {
		sendNotification(
				new Notification(
						STATE_CHANGE_METRIC_EVENT, 
						ChronicleTier.LIVE_TIER_OBJECT_NAME, 
						NewElementTriggers.serial.incrementAndGet(), 
						SystemClock.time(), 
						String.format(EVENT_STATUS_MESSAGE, metricId, status)));		
	}
	
	
	/** The JMX notification type for new metric events */
	public static final String NEW_METRIC_EVENT = "metric.event.new";
	/** The JMX notification type for new metric events */
	public static final String STATE_CHANGE_METRIC_EVENT = "metric.event.statechange";
	
	
	/**
	 * Adds the incrementor for the passed metric ID
	 * @param dataSource The conn source for H2 connections
	 * @param metricId The new metric Id
	 * @param typeId The {@link MetricType} ordinal
	 * FIXME: If metric is new, value is 1, otherwise it is +1
	 */
	protected void addIncr(DataSource dataSource, long metricId, int typeId) {
		Connection conn = null;
		PreparedStatement ps = null;
		try {
			conn = dataSource.getConnection();
			ps = conn.prepareStatement(typeId==INCR_ID ?
				"INSERT INTO INCREMENTOR (METRIC_ID, INC_VALUE, LAST_INC) VALUES (?, 1, CURRENT_TIMESTAMP)"
				:
				"INSERT INTO INTERVAL_INCREMENTOR (METRIC_ID, INC_VALUE, LAST_INC) VALUES (?, 1, CURRENT_TIMESTAMP)"
			);
			ps.setLong(1, metricId);
			ps.executeUpdate();
		} catch (SQLException ex) {
			throw new RuntimeException("Failed to addIncr for metricId [" + metricId + "]", ex);
		} finally {			
			if(ps!=null) try { ps.close(); } catch (Exception ex) {/* No Op */}
			if(conn!=null) try { conn.close(); } catch (Exception ex) {/* No Op */}
		}
		
	}
	
}

