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

import java.sql.Connection;

import javax.sql.DataSource;

import org.helios.collector.jdbc.SQLMapping;

/**
 * <p>Title: IJDBCConnectionFactory</p>
 * <p>Description: Defines the interface for factrories that supply JDBC collectors with a JDBC collection.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 * @TODO: Provide native underlying connection
 * @TODO: Provide clean way of returning an underlying connection back to the pool.
 */
public interface IJDBCConnectionFactory {
	/**
	 * Acquires and returns a JDBC Connection.
	 * @return A JDBC Connection.
	 * @throws JDBCConnectionFactoryException
	 */
	public Connection getJDBCConnection() throws JDBCConnectionFactoryException;
	
	/**
	 * Acquires and returns a JDBC Connection with a thread watch of the defined time.
	 * @param timeout The timeout in ms.
	 * @return A JDBC Connection.
	 * @throws JDBCConnectionFactoryException
	 */
	public Connection getJDBCConnection(long timeout) throws JDBCConnectionFactoryException;
	
	/**
	 * Acquires and returns a JDBC Connection with a thread watch of the defined time and a connection check.
	 * @param timeout The timeout in ms.
	 * @param sqlmap Executed once the connection is made as a connection availability test.
	 * @return A JDBC Connection.
	 * @throws JDBCConnectionFactoryException
	 */
	public Connection getJDBCConnection(long timeout, SQLMapping sqlmap) throws JDBCConnectionFactoryException;
	
	/**
	 * Provides the underlying data source for this connection factory.
	 * @param dataSource The data source this connection factory will use.
	 */
	public void setDataSource(DataSource dataSource);

}
