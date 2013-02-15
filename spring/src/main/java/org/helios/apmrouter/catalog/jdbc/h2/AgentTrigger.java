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

import org.helios.apmrouter.catalog.EntryStatus;

/**
 * <p>Title: AgentTrigger</p>
 * <p>Description: H2 new agent trigger</p> 
 * <p>Called by <b><code>AGENT_TRG  AFTER INSERT ON AGENT FOR EACH ROW</code></b></p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.catalog.jdbc.h2.AgentTrigger</code></p>
 */

public class AgentTrigger extends AbstractTrigger implements AgentTriggerMBean {
	/** The ID of the column containing the conncted timestamp for an agent */
	public static final int CONNECT_COLUMN_ID = 6;
	/** The ID of the column containing the agent id for an agent */
	public static final int AGENT_COLUMN_ID = 0;
	/** The ID of the column containing the agent name for an agent */
	public static final int AGENT_NAME_ID = 2;
	
	/**
	 * Creates a new AgentTrigger
	 */
	public AgentTrigger() {
		super(new MBeanNotificationInfo(new String[]{NEW_AGENT}, Notification.class.getName(), "A new agent registration event"));
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.h2.api.Trigger#fire(java.sql.Connection, java.lang.Object[], java.lang.Object[])
	 */
	@Override
	public void fire(Connection conn, Object[] oldRow, Object[] newRow) throws SQLException {
		if(TriggerOp.UPDATE.isEnabled(type)) {
			// An agent has gone off-line, so we need to cascade this event down to the
			// agent's metrics and mark them off-line.
			if(newRow!=null && newRow[CONNECT_COLUMN_ID]==null) {
				int rowsUpdated = markAgentMetricsDown(conn, (Long)newRow[AGENT_COLUMN_ID]);
				log.info("Marked [" + rowsUpdated + "] metrics OFFLINE for agent [" + newRow[AGENT_NAME_ID] + "]");
			}
		} else if(TriggerOp.INSERT.isEnabled(type)) {
			log.info("\n\t=================\n\tNEW AGENT:" + Arrays.toString(newRow) + "\n\t=================\n");
			//sendNotification(NEW_AGENT, newRow);								
		}
	}
	
	/**
	 * Updates the STATE of all an agent's metrics to {@link EntryStatus#OFFLINE}.
	 * @param conn The trigger provided connection
	 * @param agentId The id of the agent to update metrics for
	 * @return the number of metrics updated
	 * @throws SQLException thrown on any SQL exception
	 */
	protected int markAgentMetricsDown(Connection conn, long agentId) throws SQLException {
		PreparedStatement ps = null;		
		try {
			ps = conn.prepareStatement("UPDATE METRIC SET STATE = ? WHERE AGENT_ID = ?");
			ps.setInt(1, EntryStatus.OFFLINE.ordinal());
			ps.setLong(2, agentId);
			return ps.executeUpdate();
		} finally {
			if(ps!=null) try { ps.close(); } catch (Exception ex) { /* No Op */ }
		}
	}

}
