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
import java.sql.SQLException;
import java.util.Arrays;

import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.ObjectName;

import org.helios.apmrouter.catalog.EntryStatus;
import org.helios.apmrouter.destination.chronicletimeseries.ChronicleTier;
import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.metric.MetricType;
import org.helios.apmrouter.util.SystemClock;

/**
 * <p>Title: MetricTrigger</p>
 * <p>Description: H2 new metric trigger</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.catalog.jdbc.h2.MetricTrigger</code></p>
 */

public class MetricTrigger extends AbstractTrigger implements MetricTriggerMBean {
	protected static final int INCR_ID = MetricType.INCREMENTOR.ordinal();
	protected static final int INT_INCR_ID = MetricType.INTERVAL_INCREMENTOR.ordinal();
	
	/** The ID of the column containing the metric id for a metric */
	public static final int METRIC_COLUMN_ID = 0;
	/** The ID of the column containing the state for a metric */
	public static final int STATE_COLUMN_ID = 8;

	
//	/** The JMX ObjectName's prefix to which the tier name is appended to create the full object name */
//	public static final ObjectName LIVE_TIER_OBJECT_NAME = JMXHelper.objectName("org.helios.apmrouter.timeseries:type=chronicle,name=live");
//	/** The JMX notification type for metric status updates */
//	public static final String ENTRY_STATUS_UPDATE_TYPE = "metric.status.update";

	/** The event status message format */
	public static final String EVENT_STATUS_MESSAGE = "%s:" + EntryStatus.OFFLINE.byteOrdinal();
	
	/**
	 * Creates a new MetricTrigger
	 */
	public MetricTrigger() {
		super(new MBeanNotificationInfo(new String[]{NEW_METRIC}, Notification.class.getName(), "A new metric registration event"));
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.h2.api.Trigger#fire(java.sql.Connection, java.lang.Object[], java.lang.Object[])
	 */
	@Override
	public void fire(Connection conn, Object[] oldRow, Object[] newRow) throws SQLException {		
		if(TriggerOp.INSERT.isEnabled(type)) {
			short typeId = (Short)newRow[2];
			if(INCR_ID==typeId || INT_INCR_ID==typeId) {
				addIncr(conn, (Long)newRow[0], typeId);
			}
			//sendNotification(NEW_METRIC, newRow);
		} else if(TriggerOp.UPDATE.isEnabled(type)) {
			if(newRow!=null && newRow[STATE_COLUMN_ID].equals(EntryStatus.OFFLINE.byteOrdinal())) {
				sendOffLineNotification((Long)newRow[METRIC_COLUMN_ID]);
			}
		}
		callCount.incrementAndGet();
	}
	
	/**
	 * Sends a notification indicating a metric Id has been set OFFLINE
	 * @param metricId The metric ID
	 */
	protected void sendOffLineNotification(long metricId) {
		sendNotification(new Notification(ChronicleTier.ENTRY_STATUS_UPDATE_TYPE, ChronicleTier.LIVE_TIER_OBJECT_NAME, NewElementTriggers.serial.incrementAndGet(), SystemClock.time(), String.format(EVENT_STATUS_MESSAGE, metricId)));		
	}
	
	
	/**
	 * Adds the incrementor for the passed metric ID
	 * @param conn The H2 connection
	 * @param metricId The new metric Id
	 * @param typeId The {@link MetricType} ordinal
	 * @throws SQLException
	 */
	protected void addIncr(Connection conn, long metricId, int typeId) throws SQLException {
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(typeId==INCR_ID ?
				"INSERT INTO INCREMENTOR (METRIC_ID, INC_VALUE, LAST_INC) VALUES (?, 0, CURRENT_TIMESTAMP)"
				:
				"INSERT INTO INTERVAL_INCREMENTOR (METRIC_ID, INC_VALUE, LAST_INC) VALUES (?, 0, CURRENT_TIMESTAMP)"
			);
			ps.setLong(1, metricId);
			ps.executeUpdate();
		} finally {
			if(ps!=null) try { ps.close(); } catch (Exception ex) {/* No Op */}
		}
		
	}
	
}

