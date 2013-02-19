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
package org.helios.collector.jdbc.extract;

/**
 * <p>Title: IReadOnlyProcessedResultSet</p>
 * <p>Description: Defines a disconnected non-navigatable and read only JDBC ResultSet.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public interface IReadOnlyProcessedResultSet {
	/**
	 * @return
	 */
	public int getColumnCount();
	/**
	 * @param colId
	 * @return
	 */
	public Object get(int colId);
	/**
	 * @param colName
	 * @return
	 */
	public Object get(String colName);

	/**
	 * Retrieves the JDBC <code>java.sql.Types</code> type code for the passed column id.
	 * @param col The column id.
	 * @return the JDBC <code>java.sql.Types</code> type code 
	 */
	public int getColumnType(int col);

	/**
	 * Retrieves the JDBC type name for the passed column id.
	 * @param col The column id.
	 * @return the JDBC type name 
	 */
	public String getColumnTypeName(int col);
	
	/**
	 * Retrieves the JDBC <code>java.sql.Types</code> type code for the passed column name.
	 * @param col The column name.
	 * @return the JDBC <code>java.sql.Types</code> type code 
	 */
	public int getColumnType(String colName);
	
	/**
	 * Retrieves the JDBC type name for the passed column name.
	 * @param col The column name.
	 * @return the JDBC type name 
	 */
	public String getColumnTypeName(String colName);
	
	/**
	 * Retrieves the Java class name for the passed column id.
	 * @param col The column id.
	 * @return The Java Class Name.
	 */
	public String getColumnClassName(int col);
	
	/**
	 * Retrieves the Java class name for the passed column name.
	 * @param col The column name.
	 * @return The Java Class Name.
	 */
	public String getColumnClassName(String colName);
	
	/**
	 * Retrieves the database specific type name for the passed column id.
	 * @param col The column id.
	 * @return The database specific type Name.
	 */
	public String getDbTypeName(int col);
	
	/**
	 * Retrieves the database specific type name for the passed column name.
	 * @param col The column name.
	 * @return The database specific type Name.
	 */
	public String getDbTypeName(String colName);
	
	/**
	 * Retrieves the database column name for the passed column id.
	 * @param col The column id.
	 * @return The database column Name.
	 */
	public String getColumnName(int col);
	
	/**
	 * Retrieves the database column name for the passed column name.
	 * @param col The column name.
	 * @return The database column Name.
	 */
	public String getColumnName(String colName);
	
	/**
	 * Gets the name of the query this result was geneated for.
	 * @return the name of the query this result was geneated for.
	 */
	public String getQueryName();
	
		
	
				
	
}

