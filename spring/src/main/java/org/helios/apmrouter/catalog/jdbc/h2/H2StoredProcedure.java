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

/**
 * <p>Title: H2StoredProcedure</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.catalog.jdbc.h2.H2StoredProcedure</code></p>
 */

public class H2StoredProcedure {
	/**
	 * Returns the unique identifier for a metric
	 * @param conn The h2 provided connection
	 * @param host The host name
	 * @param agent The agent name
	 * @param typeId The metric type
	 * @param namespace The metric namespace
	 * @param name The metric name
	 * @return the assigned ID
	 * @throws SQLException thrown on any error
	 */
	public static long getID(Connection conn, String host, String agent, int typeId, String namespace, String name) throws SQLException {
		int hostId = key(conn, "SELECT HOST_ID FROM HOST", new Object[]{host}, "INSERT INTO HOST (NAME, FIRST_CONNECTED, LAST_CONNECTED) VALUES (?,SYSTIME,SYSTIME)", 1, host).intValue();
		int agentId = key(conn, "SELECT AGENT_ID FROM AGENT", new Object[]{agent}, "INSERT INTO AGENT (HOST_ID, NAME, FIRST_CONNECTED, LAST_CONNECTED) VALUES (?,?,SYSTIME,SYSTIME)", 1, host, agent).intValue();
		long metricId = key(conn, "SELECT AGENT_ID FROM AGENT", new Object[]{agent}, "INSERT INTO AGENT (HOST_ID, NAME, FIRST_CONNECTED, LAST_CONNECTED) VALUES (?,?,SYSTIME,SYSTIME)", 1, host, agent).intValue(); 
		return 0;
	}
	


	
	/**
	 * Acquires a key
	 * @param conn The connection
	 * @param selectSql The select to find the key
	 * @param binds Bind variables for the select
	 * @param insertSql The insert to create the record if the key was not found
	 * @param keyIndex The index of the key within the select result set 
	 * @param insertValues The values to insert if an insert is necessary
	 * @return the requested key
	 * @throws SQLException thrown on any error
	 */
	public static Number key(Connection conn, String selectSql, Object[] binds, String insertSql, int keyIndex, Object...insertValues) throws SQLException {
		PreparedStatement ps = null;
		ResultSet rset = null;
		try {
			ps = conn.prepareStatement(selectSql);
			for(int i = 0; i < binds.length; i++) {
				ps.setObject(i+1, binds[i]);
			}
			rset = ps.executeQuery();
			if(rset.next()) {
				return rset.getLong(keyIndex);
			}
			rset.close(); rset = null;
			ps.close();
			ps = conn.prepareStatement(insertSql);
			for(int i = 0; i < insertValues.length; i++) {
				ps.setObject(i+1, insertValues[i]);
			}
			ps.executeUpdate();
			rset = ps.getGeneratedKeys();
			rset.next();
			return rset.getLong(1);
		} catch (Exception e) {
			throw new SQLException("Failed to find key for [" + selectSql + "]", e);
		} finally {
			if(rset!=null && !rset.isClosed()) try { rset.close(); } catch (Exception e) {}
			if(ps!=null && !ps.isClosed()) try { ps.close(); } catch (Exception e) {}
		}
	}
	

}
