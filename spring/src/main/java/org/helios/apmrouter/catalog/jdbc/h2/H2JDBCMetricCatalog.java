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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Types;

import javax.sql.DataSource;

import org.helios.apmrouter.catalog.MetricCatalogService;
import org.helios.apmrouter.metric.MetricType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;




/**
 * <p>Title: H2JDBCMetricCatalog</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.catalog.jdbc.H2JDBCMetricCatalog</code></p>
 */

public class H2JDBCMetricCatalog implements MetricCatalogService {
	/** The h2 datasource */
	protected DataSource ds = null;
	
	/**
	 * Startup procedure
	 * @throws Exception on any error
	 */
	public void start() throws Exception {
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
			try { ps.close(); } catch (Exception e) {}
			try { conn.close(); } catch (Exception e) {}
		}
		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.catalog.MetricCatalogService#getID(java.lang.String, java.lang.String, int, java.lang.String, java.lang.String)
	 */
	@Override
	public long getID(String host, String agent, int typeId, String namespace, String name) {
		Connection conn = null;
		CallableStatement cs = null;
		try {
			conn = ds.getConnection();
			cs = conn.prepareCall("{? = getID(?,?,?,?,?}");
			cs.registerOutParameter(1, Types.NUMERIC);
			cs.setString(2, host);
			cs.setString(3, agent);
			cs.setInt(4, typeId);
			cs.setString(5, namespace);
			cs.setString(6, name);
			cs.execute();
			return cs.getLong(1);
		} catch (Exception e) {
			throw new RuntimeException("Failed to get ID", e);
		} finally {
			try { cs.close(); } catch (Exception e) {}
			try { conn.close(); } catch (Exception e) {}
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
}
