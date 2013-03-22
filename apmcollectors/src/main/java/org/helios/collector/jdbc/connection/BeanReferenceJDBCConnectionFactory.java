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
package org.helios.collector.jdbc.connection;

import org.helios.collector.jdbc.SQLMapping;
import org.helios.collector.timeout.ThreadWatcher;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * <p>Title: BeanReferenceJDBCConnectionFactory</p>
 * <p>Description: A JDBC Connection Factory that acquires connections from another spring bean.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class BeanReferenceJDBCConnectionFactory implements IJDBCConnectionFactory {
	/** A reference to the datasource */
	protected DataSource dataSource = null;

	/**
	 * Simple Constructor for BeanReferenceJDBCConnectionFactory
	 */
	public BeanReferenceJDBCConnectionFactory() {
	}
	
	/**
	 * Parameterized Constructor for BeanReferenceJDBCConnectionFactory
	 * @param dataSource The injected data source.
	 */
	public BeanReferenceJDBCConnectionFactory(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * Acquires and returns a JDBC Connection
	 * @return A JDBC Connection
	 * @throws JDBCConnectionFactoryException
	 * @see org.helios.collectors.jdbc.connection.IJDBCConnectionFactory#getJDBCConnection()
	 */
	public Connection getJDBCConnection() throws JDBCConnectionFactoryException {
		return getJDBCConnection(-1);
	}
	


	/**
	 * Sets the data source to be used by this factory.
	 * @param dataSource the dataSource to set
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * @param timeout
	 * @return
	 * @throws JDBCConnectionFactoryException
	 * @see org.helios.collectors.jdbc.connection.IJDBCConnectionFactory#getJDBCConnection(long)
	 */
	public Connection getJDBCConnection(long timeout) throws JDBCConnectionFactoryException {
		try {
			if(timeout > 0) {
				ThreadWatcher.getInstance().watch(timeout);
			}
			return dataSource.getConnection();
		} catch (InterruptedException ie) {
			throw new JDBCConnectionFactoryException("Connection Request Was Interrupted After Timeout of [" + timeout + "] ms.", ie);
		} catch (SQLException se) {
			throw new JDBCConnectionFactoryException("Failed to acquire connection", se);
		} finally {
			if(timeout > 0) {
				ThreadWatcher.getInstance().stop();
			}
		}
	}

	/**
	 * @param timeout
	 * @param sqlmap
	 * @return
	 * @throws JDBCConnectionFactoryException
	 * @see org.helios.collectors.jdbc.connection.IJDBCConnectionFactory#getJDBCConnection(long, org.helios.collectors.jdbc.SQLMapping)
	 */
	public Connection getJDBCConnection(long timeout, SQLMapping sqlmap) throws JDBCConnectionFactoryException {
		Connection conn = getJDBCConnection(timeout);
		if(sqlmap!=null) {
			
		}
		return conn;
	}

}
