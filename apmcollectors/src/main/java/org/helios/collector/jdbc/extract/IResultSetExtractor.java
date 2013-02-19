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
import java.sql.SQLException;
import java.util.Map;

/**
 * <p>Title: IResultSetExtractor</p>
 * <p>Description: Implementors of this class manage the conversion of live result sets into a result map.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public interface IResultSetExtractor {
	
	/**
	 * Creates a map of the name/values from a result set.
	 * @param rset The result set to convert.
	 * @param maxRows The maximum number of rows to read. < 1 means all rows.
	 * @param useStrings If true, retrieves values as strings. If false, retrieves values according to the JDBC type code.
	 * @return A map of name/values for the result set keyed as follows:<code>Map<ROWID, MAP<COLID, VALUE>></code>
	 * @throws SQLException
	 * @throws IOException
	 */
	public Map<Integer, Map<Integer, Object>> processResultSet(ResultSet rset, int maxRows, boolean useStrings) throws SQLException, IOException;
	
	/**
	 * Creates a map of the name/values from a result set. Defaults the maxRows to -1 and useStrings to true.
	 * @param rset The result set to convert.
	 * @return A map of name/values for the result set keyed as follows:<code>Map<ROWID, MAP<COLID, VALUE>></code>
	 * @throws SQLException
	 * @throws IOException
	 */
	public Map<Integer, Map<Integer, Object>> processResultSet(ResultSet rset) throws SQLException, IOException;
	
}
