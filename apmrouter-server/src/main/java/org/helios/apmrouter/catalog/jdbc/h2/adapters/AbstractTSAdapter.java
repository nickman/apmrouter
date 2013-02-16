/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2012, Helios Development Group and individual contributors
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
package org.helios.apmrouter.catalog.jdbc.h2.adapters;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.h2.tools.SimpleResultSet;

/**
 * <p>Title: AbstractTSAdapter</p>
 * <p>Description: Base abstract class for implementing external datasource adapters for H2</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.catalog.jdbc.h2.adapters.AbstractTSAdapter</code></p>
 */

public abstract class AbstractTSAdapter {

	/** The column descriptor result set */
	protected static final SimpleResultSet COL_DESCRIPTOR = newDef();

	public static ResultSet getValues(Connection conn, long oldestPeriod, Long...ids) throws Exception {
		return null;
	}
	
	/**
	 * Creates a result set for time-series data
	 * @return a result set for time-series data
	 */
	public static SimpleResultSet newDef() {
	    SimpleResultSet rs = new SimpleResultSet();
	    rs.addColumn("ID", Types.NUMERIC, 255, 22);
	    rs.addColumn("TS", Types.TIMESTAMP, 1, 22);
	    rs.addColumn("MIN", Types.NUMERIC, 255, 22);
	    rs.addColumn("MAX", Types.NUMERIC, 255, 22);
	    rs.addColumn("AVG", Types.NUMERIC, 255, 22);
	    rs.addColumn("CNT", Types.NUMERIC, 255, 22);
	    return rs;
	}

}
