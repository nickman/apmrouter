/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2013, Helios Development Group and individual contributors
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
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.h2.tools.TriggerAdapter;

/**
 * <p>Title: AgentSelectTrigger</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.catalog.jdbc.h2.AgentSelectTrigger</code></p>
 */

public class AgentSelectTrigger extends TriggerAdapter {
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());

	/**
	 * Creates a new AgentSelectTrigger
	 */
	public AgentSelectTrigger() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * {@inheritDoc}
	 * @see org.h2.tools.TriggerAdapter#fire(java.sql.Connection, java.sql.ResultSet, java.sql.ResultSet)
	 */
	@Override
	public void fire(Connection conn, ResultSet oldRow, ResultSet newRow) throws SQLException {
		log.info("\n\t=================\n\tSELECT TRIGGER:\n\tOldRow:" + format(oldRow) + "\n\tNewRow:" + format(newRow) + "\n\t=================\n");		

	}
	
	protected static String format(ResultSet rs) throws SQLException {
		StringBuilder b = new StringBuilder("ResultSet [");
		if(rs==null) {
			b.append("\n\tNULL.");
		} else {
			ResultSetMetaData rsmd = rs.getMetaData();
			while(rs.next()) {
				b.append("\n\t-->");
				for(int i = 0; i < rsmd.getColumnCount(); i++) {
					Object value = rs.getObject(i+1);
					if(value==null) value = "<null>";
					b.append(rsmd.getColumnDisplaySize(i+1)).append(":[").append(value).append("],");
				}
				b.deleteCharAt(b.length()-1);
				b.append("<--");
			}
		}
		b.append("\n]");
		return b.toString();
	}

}
