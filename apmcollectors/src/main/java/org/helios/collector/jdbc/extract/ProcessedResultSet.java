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

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;


/**
 * <p>Title: ProcessedResultSet</p>
 * <p>Description: A navigatable and DB detached container for a {@link ResultSet}.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.collectors.jdbc.extract.ProcessedResultSet</code></p>
 * @todo: <ol>
 * 	<li>Prefetch</li>
 *  <li>Externalizable</li>
 *  <li>Clob/Blob Support</li>
 *  <li>DB MetaData</li>
 *  <li>Session ID</li>
 *  <li>Cursor Name</li>
 * </ol>
 */
public class ProcessedResultSet implements IProcessedResultSet, IReadOnlyProcessedResultSet {
	protected IResultSetExtractor rsetExtractor = new ResultSetExtractorImpl();
	protected ThreadLocal<Integer> index = new ThreadLocal<Integer>() {
        protected synchronized Integer initialValue() {
            return -1;
        }
	};
	protected int rowCount = 0;
	protected int colCount = 0;
	protected Map<String, Integer> nameToIndex = null;
	protected Map<Integer, String> columnNames = null;
	protected Map<Integer, Integer> jdbcTypes = null;
	protected Map<Integer, String> jdbcTypeNames = null;
	protected Map<Integer, String> jdbcClassNames = null;
	protected Map<Integer, String> dbTypeNames = null;
	protected Map<Integer, Map<Integer, Object>> values = null;
	protected String queryName = null;
	protected long timeToGet = 0;
	
	
	/**
	 * @param rset
	 * @throws SQLException
	 * @throws IOException
	 */
	public ProcessedResultSet(ResultSet rset) throws SQLException, IOException {
		this(rset, -1, true);
	}
	
	/**
	 * @param rset
	 * @param maxRows
	 * @throws SQLException
	 * @throws IOException
	 */
	public ProcessedResultSet(ResultSet rset, int maxRows) throws SQLException, IOException {
		this(rset, maxRows , true);
	}
	
	
	/**
	 * @param rset
	 * @param maxRows
	 * @param useStrings
	 * @throws SQLException
	 * @throws IOException
	 */
	public ProcessedResultSet(ResultSet rset, int maxRows, boolean useStrings) throws SQLException, IOException {
		long start = System.currentTimeMillis();
		values = rsetExtractor.processResultSet(rset, maxRows, useStrings);
		timeToGet = System.currentTimeMillis()-start;
		ResultSetMetaData rsmd = rset.getMetaData();
		colCount = rsmd.getColumnCount();
		nameToIndex = new HashMap<String, Integer>(colCount);
		jdbcTypes = new HashMap<Integer,Integer>(colCount);
		jdbcTypeNames = new HashMap<Integer,String>(colCount);
		jdbcClassNames = new HashMap<Integer,String>(colCount);
		dbTypeNames = new HashMap<Integer,String>(colCount);
		columnNames = new HashMap<Integer,String>(colCount);
		for(int i = 1; i <= colCount; i++) {
			nameToIndex.put(rsmd.getColumnName(i), i-1);
			jdbcTypes.put(i, rsmd.getColumnType(i));
			jdbcTypeNames.put(i-1, rsmd.getColumnTypeName(i));
			jdbcClassNames.put(i-1, rsmd.getColumnClassName(i));
			dbTypeNames.put(i-1, rsmd.getColumnTypeName(i));
			columnNames.put(i-1, rsmd.getColumnName(i));
		}
		rowCount = values.size();
	}
	
	/**
	 * Increment the thread local index counter.
	 * @return true if the counter was incremented, false if it is at max.
	 */
	protected boolean incr() {
		int i = index.get();
		if(i==rowCount-1) return false;
		i++; index.set(i);
		return true;
	}
	
	/**
	 * Decrement the thread local index counter.
	 * @return true if the counter was decremented, false if it is at zero.
	 */
	protected boolean decr() {
		int i = index.get();
		if(i==0) return false;
		i--; index.set(i);
		return true;
	}
	
	
	/**
	 * 
	 * @see org.helios.collectors.jdbc.extract.IProcessedResultSet#first()
	 */
	public void first() {
		index.set(0);		
	}
	
	/**
	 * 
	 * @see org.helios.collectors.jdbc.extract.IProcessedResultSet#beforeFirst()
	 */
	public void beforeFirst() {
		index.set(-1);		
	}
	

	/**
	 * @param colId
	 * @return
	 * @see org.helios.collectors.jdbc.extract.IProcessedResultSet#flat(int)
	 */
	public String flat(int colId, String delim) {
		if(colId < 0 || colId > colCount) throw new RuntimeException("Requested column Id out of range. " + colId + " not in [0-" + colCount + "]");
		StringBuilder b = new StringBuilder(10*rowCount);
		for(Map<Integer, Object> row: values.values()) {
			b.append(row.get(colId).toString()).append(delim);
		}
		return b.toString();
		
	}
	
	/**
	 * @param colId
	 * @return
	 * @see org.helios.collectors.jdbc.extract.IProcessedResultSet#flat(int)
	 */
	public String flat(int colId) {
		return flat(colId, "");
	}

	/**
	 * @param colName
	 * @return
	 * @see org.helios.collectors.jdbc.extract.IProcessedResultSet#flat(java.lang.String)
	 */
	public String flat(String colName) {
		return flat(colName, "");
	}
	
	/**
	 * @param colName
	 * @param delim
	 * @return
	 */
	public String flat(String colName, String delim) {
		Integer i = nameToIndex.get(colName);
		if(i==null) throw new RuntimeException("Could not get column index for name [" + colName + "]");		
		return flat(i, delim);
	}
	

	/**
	 * @param colId
	 * @return
	 * @see org.helios.collectors.jdbc.extract.IProcessedResultSet#get(int)
	 */
	public Object get(int colId) {
		if(colId < 0 || colId > colCount) throw new RuntimeException("Requested column Id out of range. " + colId + " not in [0-" + colCount + "]");
		return values.get(index.get()).get(colId);
	}

	/**
	 * @param colName
	 * @return
	 * @see org.helios.collectors.jdbc.extract.IProcessedResultSet#get(java.lang.String)
	 */
	public Object get(String colName) {
		Integer i = nameToIndex.get(colName);
		if(i==null) throw new RuntimeException("Could not get column index for name [" + colName + "]");		
		return values.get(index.get()).get(i);
	}
	
	/**
	 * @return
	 * @see org.helios.collectors.jdbc.extract.IProcessedResultSet#getColumnCount()
	 */
	public int getColumnCount() {
		return colCount;
	}
	
	/**
	 * Retrieves the JDBC <code>java.sql.Types</code> type code for the passed column id.
	 * @param col The column id.
	 * @return the JDBC <code>java.sql.Types</code> type code 
	 */
	public int getColumnType(int col) {
		return jdbcTypes.get(col);
	}
	
	/**
	 * Retrieves the JDBC type name for the passed column id.
	 * @param col The column id.
	 * @return the JDBC type name 
	 */
	public String getColumnTypeName(int col) {
		return jdbcTypeNames.get(col);
	}
	
	/**
	 * Retrieves the Java class name for the passed column id.
	 * @param col The column id.
	 * @return The Java Class Name.
	 */
	public String getColumnClassName(int col) {
		return jdbcClassNames.get(col);
	}
	
	/**
	 * Retrieves the Java class name for the passed column name.
	 * @param col The column name.
	 * @return The Java Class Name.
	 */
	public String getColumnClassName(String colName) {
		Integer i = nameToIndex.get(colName);
		if(i==null) throw new RuntimeException("Could not get column index for name [" + colName + "]");						
		return jdbcClassNames.get(i);
	}
	
	
	/**
	 * Retrieves the JDBC <code>java.sql.Types</code> type code for the passed column name.
	 * @param col The column name.
	 * @return the JDBC <code>java.sql.Types</code> type code 
	 */
	public int getColumnType(String colName) {
		Integer i = nameToIndex.get(colName);
		if(i==null) throw new RuntimeException("Could not get column index for name [" + colName + "]");				
		return jdbcTypes.get(i);
	}
	
	/**
	 * Retrieves the JDBC type name for the passed column name.
	 * @param col The column name.
	 * @return the JDBC type name 
	 */
	public String getColumnTypeName(String colName) {
		Integer i = nameToIndex.get(colName);
		if(i==null) throw new RuntimeException("Could not get column index for name [" + colName + "]");				
		return jdbcTypeNames.get(i);
	}
		
	
	/**
	 * Retrieves the database specific type name for the passed column id.
	 * @param col The column id.
	 * @return The database specific type Name.
	 */
	public String getDbTypeName(int col) {
		return dbTypeNames.get(col);
	}
	
	/**
	 * Retrieves the database specific type name for the passed column name.
	 * @param col The column name.
	 * @return The database specific type Name.
	 */
	public String getDbTypeName(String colName) {
		Integer i = nameToIndex.get(colName);
		if(i==null) throw new RuntimeException("Could not get column index for name [" + colName + "]");						
		return dbTypeNames.get(i);
	}
	
	/**
	 * Retrieves the database column name for the passed column id.
	 * @param col The column id.
	 * @return The database column Name.
	 */
	public String getColumnName(int col) {
		return columnNames.get(col);
	}
	
	/**
	 * Retrieves the database column name for the passed column name.
	 * @param col The column name.
	 * @return The database column Name.
	 */
	public String getColumnName(String colName) {
		Integer i = nameToIndex.get(colName);
		if(i==null) throw new RuntimeException("Could not get column index for name [" + colName + "]");						
		return columnNames.get(i);
	}	
	
	/**
	 * @return
	 * @see org.helios.collectors.jdbc.extract.IProcessedResultSet#getRowCount()
	 */
	public int getRowCount() {
		return rowCount;
	}

	/**
	 * @return
	 * @see org.helios.collectors.jdbc.extract.IProcessedResultSet#next()
	 */
	public boolean next() {
		return incr();
	}

	/**
	 * @return
	 * @see org.helios.collectors.jdbc.extract.IProcessedResultSet#prev()
	 */
	public boolean prev() {
		return decr();
	}

	/**
	 * @return the timeToGet
	 */
	public long getTimeToGet() {
		return timeToGet;
	}

	/**
	 * Gets the name of the query this result was geneated for.
	 * @return the name of the query this result was geneated for.
	 */
	public String getQueryName() {
		return queryName;
	}

	/**
	 * Sets the name of the query this result was geneated for.
	 * @param queryName the name of the query this result was geneated for.
	 */
	public void setQueryName(String queryName) {
		this.queryName = queryName;
	}

}