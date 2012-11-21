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
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.h2.tools.SimpleResultSet;

/**
 * <p>Title: H2StoredProcedure</p>
 * <p>Description: Java stored procedure to manage various bits-and-pieces in the catalog daabase.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.catalog.jdbc.h2.H2StoredProcedure</code></p>
 */

public class H2StoredProcedure {
	
	/** The default domain name assigned to hosts with no designated domain */
	public static final String DEFAULT_DOMAIN = "DefaultDomain";
	/** Constant int array of 1 */
	private static final int[] ARR_ONE = {1};
	/** Constant int array of 1 and 2 */
	private static final int[] ARR_ONE_TWO = {1,2};
	/** Constant int array of 1, 2 and three */
	private static final int[] ARR_ONE_TWO_THREE = {1,2,3};
	
	/**
	 * Called when an agent connects or disconnects (or times out)
	 * @param conn The H2 supplied connection
	 * @param connected true for a connect, false for a disconnect
	 * @param host The host name
	 * @param ip The host IP address
	 * @param agent The agent name
	 * @param agentURI The agent's listening URI
	 * @return A result set containing:<ol>
	 * 	<li>The number of connected agents for the passed host after this op completes</li>
	 * 	<li>The host ID</li>
	 * 	<li>The agent ID</li>
	 *  <li>The host's domain</li>
	 * </ol>
	 * @throws SQLException thrown on any SQL error
	 */
	public synchronized static ResultSet hostAgentState(Connection conn, boolean connected, String host, String ip, String agent, String agentURI) throws SQLException {
		Object[] results = key(conn, "SELECT HOST_ID, AGENTS, DOMAIN FROM HOST WHERE NAME=?", new Object[]{host}, "INSERT INTO HOST (NAME, DOMAIN, IP, FIRST_CONNECTED, LAST_CONNECTED, CONNECTED) VALUES (?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)", ARR_ONE_TWO_THREE, host, domain(host), ip);
		int hostId = ((Number)results[0]).intValue();
		int agentCount = ((Number)results[1]).intValue();
		String domain = results[2].toString();
		int agentId = ((Number)key(conn, "SELECT AGENT_ID FROM AGENT WHERE NAME=? AND HOST_ID = ?", new Object[]{agent, hostId}, "INSERT INTO AGENT (HOST_ID, NAME, MIN_LEVEL, URI, FIRST_CONNECTED, LAST_CONNECTED, CONNECTED) VALUES (?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", ARR_ONE, hostId, agent, 3200, agentURI)[0]).intValue();
		PreparedStatement  ps = null;
		String hostUpdateSQL = null;
		String agentUpdateSQL = null;
		if(connected) {
			agentCount++;
			hostUpdateSQL = "UPDATE HOST SET CONNECTED = CURRENT_TIMESTAMP, LAST_CONNECTED = CURRENT_TIMESTAMP, AGENTS = ? WHERE HOST_ID = ?";
			agentUpdateSQL = "UPDATE AGENT SET CONNECTED = CURRENT_TIMESTAMP, LAST_CONNECTED = CURRENT_TIMESTAMP, URI = ? WHERE AGENT_ID = ? AND HOST_ID = ?";
		} else {
			agentCount--;
			if(agentCount<0) {
				agentCount = 0;
			}
			hostUpdateSQL = "UPDATE HOST SET CONNECTED = NULL, AGENTS = ? WHERE HOST_ID = ?";
			agentUpdateSQL = "UPDATE AGENT SET CONNECTED = NULL, URI = NULL WHERE AGENT_ID = ? AND HOST_ID = ?";			
		}
		try {
			ps = conn.prepareStatement(hostUpdateSQL);
			ps.setInt(1, agentCount);
			ps.setInt(2, hostId);
			ps.executeUpdate();
			ps.close();
			ps = conn.prepareStatement(agentUpdateSQL);			
			if(connected) {
				ps.setString(1, agentURI);
				ps.setInt(2, agentId);
				ps.setInt(3, hostId);
			} else {
				ps.setInt(1, agentId);
				ps.setInt(2, hostId);
			}
			ps.executeUpdate();
			SimpleResultSet rs = new SimpleResultSet();
			
		    rs.addColumn("AGENTS", Types.INTEGER, 10, 0);
		    rs.addColumn("HOST_ID", Types.INTEGER, 10, 0);
		    rs.addColumn("AGENT_ID", Types.INTEGER, 10, 0);
		    rs.addColumn("DOMAIN", Types.VARCHAR, 255, 0);
		    rs.addRow(agentCount, hostId, agentId, domain);
		    return rs;			
		} catch (Exception ex) {
			throw new SQLException("Failed to touch agentHost State [" + String.format("%s/%s", host, agent) + "]", ex);
		} finally {
			if(ps!=null) try { ps.close(); } catch (Exception ex) {}
		}
	}
	
	/**
	 * Upsert on a metric's last seen timestamp.
	 * @param conn The h2 provided connection
	 * @param token The metric ID which may be -1 meaning the metric does not exist yet
	 * @param host The host name
	 * @param agent The agent name
	 * @param typeId The metric type
	 * @param namespace The metric namespace
	 * @param name The metric name
	 * @return the newly assigned metric id if the incoming token was -1, 0 if the metric already existed and was timestamp updated.
	 * @throws SQLException thrown on any error
	 */
	public static long touch(Connection conn, long token, String host, String agent, int typeId, String namespace, String name) throws SQLException {
		PreparedStatement ps = null;
		try {
			if(token==-1) {
				long newToken = getID(conn, token, host, agent, typeId, namespace, name);
				return newToken;
			}
			ps = conn.prepareStatement("UPDATE METRIC SET LAST_SEEN = CURRENT_TIMESTAMP WHERE METRIC_ID = ?");
			ps.setLong(1, token);
			ps.executeUpdate();
			return 0;
		} catch (Exception e) {
			throw new SQLException("Failed to touch metric [" + String.format("%s/%s%s:%s", host, agent, namespace, name) + "]", e);
		} finally {
			if(ps!=null && !ps.isClosed()) try { ps.close(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Returns the unique identifier for a metric
	 * @param conn The h2 provided connection
	 * @param token The metric ID which may be -1 meaning the metric does not exist yet
	 * @param host The host name
	 * @param agent The agent name
	 * @param typeId The metric type
	 * @param namespace The metric namespace
	 * @param name The metric name
	 * @return the assigned ID
	 * @throws SQLException thrown on any error
	 */
	public static long getID(Connection conn, long token, String host, String agent, int typeId, String namespace, String name) throws SQLException {
		if(token!=-1) return 0;
		int hostId = ((Number)key(conn, "SELECT HOST_ID FROM HOST WHERE NAME=?", new Object[]{host}, "INSERT INTO HOST (NAME, DOMAIN, FIRST_CONNECTED, LAST_CONNECTED) VALUES (?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)", ARR_ONE, host, domain(host))[0]).intValue();
		Object[] nums = key(conn, "SELECT AGENT_ID,MIN_LEVEL FROM AGENT WHERE NAME=? AND HOST_ID=?", new Object[]{agent, hostId}, "INSERT INTO AGENT (HOST_ID, NAME, MIN_LEVEL, FIRST_CONNECTED, LAST_CONNECTED) VALUES (?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)", ARR_ONE_TWO, hostId, agent, nsLevel(namespace));
		int agentId = ((Number)nums[0]).intValue();
		int agentMinLevel = ((Number)nums[1]).intValue();
		int nsLevel = nsLevel(namespace);
		long metricId = ((Number)key(conn, "SELECT METRIC_ID FROM METRIC WHERE AGENT_ID=? AND NAMESPACE=? AND NAME=?", new Object[]{agentId, namespace, name}, 
				"INSERT INTO METRIC (AGENT_ID, TYPE_ID, NAMESPACE, NARR, LEVEL, NAME, FIRST_SEEN, LAST_SEEN) VALUES (?,?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)", ARR_ONE, agentId, typeId, namespace, nsItems(namespace), nsLevel, name)[0]).longValue();
		if(nsLevel<agentMinLevel) {
			setAgentMinLevel(conn, agentId, nsLevel);
		}
		return metricId;
	}
	
	private static void setAgentMinLevel(Connection conn, int agentId, int minLevel) throws SQLException {
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement("UPDATE AGENT SET MIN_LEVEL = ? WHERE AGENT_ID = ?");
			ps.setInt(1, minLevel);
			ps.setInt(2, agentId);
			ps.executeUpdate();
		} finally {
			if(ps!=null) try { ps.close(); } catch (Exception ex) {}
		}
	}
	
	/** Split pattern for namespaces */
	private static final Pattern NS_DELIM = Pattern.compile("/");
	
	/**
	 * Returns the number of entries in the passed namespace
	 * @param namespace The namespace to get the count for
	 * @return the number of namespace entries
	 */
	private static int nsLevel(String namespace) {
		if(namespace==null || namespace.trim().isEmpty()) return 0;
		String[] frags = NS_DELIM.split(namespace);
		int cnt = 0;
		for(String s: frags) {
			if(s!=null && !s.trim().isEmpty()) cnt++;
		}
		return cnt;
	}
	
	/**
	 * Returns individual entries in the passed namespace
	 * @param namespace The namespace to get the entries for
	 * @return the array of namespace entries
	 */
	private static String[] nsItems(String namespace) {
		if(namespace==null || namespace.trim().isEmpty()) return new String[0];
		List<String> items = new ArrayList<String>();
		String[] frags = NS_DELIM.split(namespace);
		for(int i = 0; i < frags.length; i++) {
			if(!frags[i].trim().isEmpty()) {
				items.add(frags[i].trim());
			}
		}
		return items.toArray(new String[items.size()]);
	}
	
	
	/**
	 * Returns the parent of the passed namespace 
	 * @param namespace the namespace to get the parent of 
	 * @return the parent
	 */
	public static String parent(String namespace) {
		if(namespace==null || namespace.trim().isEmpty()) return "";
		StringBuilder sb = new StringBuilder(namespace).reverse();
		sb.delete(0, sb.indexOf("/")+1);
		return sb.reverse().toString();
	}
	
	/**
	 * Returns the root of the passed namespace 
	 * @param namespace the namespace to get the root of 
	 * @return the root
	 */
	public static String root(String namespace) {
		if(namespace==null || namespace.trim().isEmpty()) return "";
		String[] frags = NS_DELIM.split(namespace.indexOf('/')==0 ? namespace.substring(1) : namespace);
		return "/" + frags[0];
	}
	
	/**
	 * Extracts and returns the domain name of the passed host name
	 * @param fqHostName The fully qualified host name 
	 * @return The domain name or {@link #DEFAULT_DOMAIN} if the passed host name has no domain
	 */
	public static String domain(String fqHostName) {
		if(fqHostName.indexOf('.')==-1) return DEFAULT_DOMAIN;
		StringBuilder b = new StringBuilder(fqHostName).reverse();
		return b.delete(0, b.indexOf(".")+1).reverse().toString();		
	}
	
	
	/**
	 * Acquires a key
	 * @param conn The connection
	 * @param selectSql The select to find the key
	 * @param binds Bind variables for the select
	 * @param insertSql The insert to create the record if the key was not found
	 * @param keyIndexes An array with the index of the key within the select result set plus any other columns that are required 
	 * @param insertValues The values to insert if an insert is necessary
	 * @return an array containing the requested key plus other requested numbers
	 * @throws SQLException thrown on any error
	 */
	public static Object[] key(Connection conn, String selectSql, Object[] binds, String insertSql, int[] keyIndexes, Object...insertValues) throws SQLException {
		PreparedStatement ps = null;
		ResultSet rset = null;
		try {
			ps = conn.prepareStatement(selectSql);
			for(int i = 0; i < binds.length; i++) {
				ps.setObject(i+1, binds[i]);
			}
			rset = ps.executeQuery();
			if(rset.next()) {
				Object[] nums = new Object[keyIndexes.length];
				for(int i = 0; i < keyIndexes.length; i++) {
					nums[i] = rset.getObject(keyIndexes[i]);
				}
				return nums;
			}
			rset.close(); rset = null;
			ps.close();
			ps = conn.prepareStatement(insertSql);
			for(int i = 0; i < insertValues.length; i++) {
				ps.setObject(i+1, insertValues[i]);
			}
			ps.executeUpdate();
			return key(conn, selectSql, binds, insertSql, keyIndexes, insertValues);
		} catch (Exception e) {
			throw new SQLException("Failed to find key for [" + selectSql + "]", e);
		} finally {
			if(rset!=null && !rset.isClosed()) try { rset.close(); } catch (Exception e) {}
			if(ps!=null && !ps.isClosed()) try { ps.close(); } catch (Exception e) {}
		}
	}
	

}
