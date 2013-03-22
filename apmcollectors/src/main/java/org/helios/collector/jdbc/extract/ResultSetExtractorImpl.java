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

import gnu.trove.map.hash.TIntObjectHashMap;
import org.helios.apmrouter.jmx.XMLHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.sql.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * <p>Title: ResultSetExtractorImpl</p>
 * <p>Description: Extracts a result set into a {@link ProcessedResultSet}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.collectors.jdbc.extract.ResultSetExtractorImpl</code></p>
 */
public class ResultSetExtractorImpl implements IResultSetExtractor {

	/**
	 * Creates a map of the name/values from a result set.
	 * @param rset The result set to convert.
	 * @param maxRows The maximum number of rows to read. < 1 means all rows.
	 * @param useStrings If true, retrieves values as strings. If false, retrieves values according to the JDBC type code.
	 * @return A map of name/values for the result set keyed as follows:<code>Map<ROWID, MAP<COLID, VALUE>></code>
	 * @throws SQLException
	 * @throws IOException
	 */
	public Map<Integer, Map<Integer, Object>> processResultSet(ResultSet rset, int maxRows, boolean useStrings) throws SQLException, IOException {
		
		Map<Integer, Map<Integer, Object>> prset = new TreeMap<Integer, Map<Integer, Object>>();
		ResultSetMetaData rsmd = rset.getMetaData();
		int columnCount = rsmd.getColumnCount();
		int rowCount = 0;
		while(rset.next()) {
			Map<Integer, Object> row = new HashMap<Integer, Object>(columnCount);
			for(int i = 1; i <= columnCount; i++) {
				Object val = useStrings ? getStringValue(i, rset) : getValue(i, rsmd.getColumnType(i), rset);
				row.put(i-1, val);
			}
			prset.put(rowCount, row);
			rowCount++;
			if(rowCount==maxRows) break;
		}
		return prset;
	}
	
	/**
	 * Creates a map of the name/values from a result set.
	 * @param rset The result set to convert.
	 * @param maxRows The maximum number of rows to read. < 1 means all rows.
	 * @param useStrings If true, retrieves values as strings. If false, retrieves values according to the JDBC type code.
	 * @return A map of name/values for the result set keyed as follows:<code>Map<ROWID, MAP<COLID, VALUE>></code>
	 * @throws SQLException
	 * @throws IOException
	 */
	public TIntObjectHashMap<TIntObjectHashMap<Object>> processResultSetX(ResultSet rset, int maxRows) throws SQLException, IOException {
		TIntObjectHashMap<TIntObjectHashMap<Object>> results = null;
		
		return results;
	}
	
	
	/**
	 * Retrieves a String value from the rset column, checking for null.
	 * @param index
	 * @param rset
	 * @return
	 * @throws SQLException
	 */
	protected Object getStringValue(int index, ResultSet rset) throws SQLException {
		String s  = rset.getString(index);
		if(rset.wasNull()) s = null;
		return s;
	}
	
	/**
	 * Creates a map of the name/values from a result set. Defaults to all rows (no maxrows) and strings for value retrieval.
	 * @param rset The result set to convert.
	 * @return A map of name/values for the result set keyed as follows:<code>Map<ROWID, MAP<COLID, VALUE>></code>
	 * @throws SQLException
	 * @throws IOException
	 */
	public Map<Integer, Map<Integer, Object>> processResultSet(ResultSet rset) throws SQLException, IOException {
		return processResultSet(rset, -1, true);
	}
	
	
	/**
	 * Extracts the correctly typed object from the indexed result set row.
	 * @param columnIndex The index of the column in the current result set row.
	 * @param type The <code>java.sql.Types</code> type code of the column in the current row.
	 * @param rset The result set to extract the value from.
	 * @return The typed extracted object.
	 * @throws SQLException
	 * @throws IOException 
	 */
	public Object getValue(int columnIndex, int type, ResultSet rset) throws SQLException, IOException {
		Object obj = null;
		switch (type) {
			case -7:		//BIT
				obj = rset.getByte(columnIndex);
				break;
			case -6:		//TINYINT
			case 5:			//SMALLINT
				obj = rset.getShort(columnIndex);
				break;
			case 4:			//INTEGER
				obj = rset.getInt(columnIndex);
				break;
			case 6:			//FLOAT
			case 7:			//REAL
				obj = rset.getFloat(columnIndex);
				break;
			case 8:			//DOUBLE
				obj = rset.getDouble(columnIndex);
			case 2:			//NUMERIC
			case 3:			//DECIMAL
			case -5:		//BIGINT
				obj = rset.getLong(columnIndex);
				break;
			case 91:		//DATE
			case 93:		//TIMESTAMP
			case 92:		//TIME
				obj = new Date(rset.getDate(columnIndex).getTime());
				break;
			case -2:		//BINARY
			case -3:		//VARBINARY
			case -4:		//LONGVARBINARY
				obj = rset.getBytes(columnIndex);
				break;
			case 0:			//NULL
				obj = null;
				break;
			case 2000:		//JAVA_OBJECT
			case 2001:		//DISTINCT
			case 2002:		//STRUCT
			case 2003:		//ARRAY
				obj = rset.getObject(columnIndex);
				break;
			case 2004:		//BLOB
				Blob blob = rset.getBlob(columnIndex);
				long blength = blob.length();
				int baosLength = (blength > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int)blength; 
				ByteArrayOutputStream baos = new ByteArrayOutputStream(baosLength);
				InputStream is = blob.getBinaryStream();
				byte[] buffer = new byte[8096];
				int bytesRead = 0;
				while((bytesRead = is.read(buffer))!=-1) {
					baos.write(buffer, 0, bytesRead);
				}
				obj = baos.toByteArray();
				break;
			case 2005:		//CLOB
			case 2011:		//NCLOB
				Clob clob = rset.getClob(columnIndex);
				long clength = clob.length();
				int sbLength = (clength > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int)clength; 
				ByteArrayOutputStream caos = new ByteArrayOutputStream(sbLength);
				InputStream cis = clob.getAsciiStream();
				byte[] cbuffer = new byte[8096];
				int cbytesRead = 0;
				while((cbytesRead = cis.read(cbuffer))!=-1) {
					caos.write(cbuffer, 0, cbytesRead);
				}
				obj = caos.toString();
				break;
			case 2006:		//REF
				break;
			case 70:		//DATALINK
				break;
			case 16:		//BOOLEAN
				obj = rset.getBoolean(columnIndex);
				break;
			case -8:		//ROWID
				obj = rset.getRowId(columnIndex);
				break;
			case -15:		//NCHAR
			case -9:		//NVARCHAR
			case -16:		//LONGNVARCHAR
				Reader reader = rset.getNCharacterStream(columnIndex);
				StringBuilder b = new StringBuilder();
				char[] ncbuffer = new char[1024];
				int charsRead = -1;
				while((charsRead = reader.read(ncbuffer))!=-1) {
					b.append(ncbuffer, 0, charsRead);
				}
				obj = b.toString();							
				break;
			case 2009:		//SQLXML
				SQLXML sqlXml = null;
				try {
					sqlXml = rset.getSQLXML(columnIndex);
					obj = XMLHelper.parseXML(sqlXml.getString()).getDocumentElement();
				} finally {
					if(sqlXml!=null) try { sqlXml.free(); } catch (Exception e) {}
				}				
				break;
			case 1:			//CHAR
			case 12:		//VARCHAR
			case -1:		//LONGVARCHAR
			case 1111:		//OTHER
			default:    	// EVERYTHING ELSE
				obj = rset.getString(columnIndex);
				break;
		}
		if(rset.wasNull()) obj = null;
		return obj;
	}
	

}
