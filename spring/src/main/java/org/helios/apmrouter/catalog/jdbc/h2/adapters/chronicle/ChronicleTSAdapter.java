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
package org.helios.apmrouter.catalog.jdbc.h2.adapters.chronicle;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;

import org.h2.tools.SimpleResultSet;
import org.helios.apmrouter.destination.chronicletimeseries.ChronicleTSManager;
import org.helios.apmrouter.destination.chronicletimeseries.ChronicleTier;
import static org.helios.apmrouter.destination.chronicletimeseries.ChronicleTier.*;

/**
 * <p>Title: ChronicleTSAdapter</p>
 * <p>Description: H2 adaptor for integrating Chronicle time-series data into the H2 SQL domain</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.catalog.jdbc.h2.adapters.chronicle.ChronicleTSAdapter</code></p>
 */

public class ChronicleTSAdapter {
	/** The chronicle tier manager */
	protected static ChronicleTSManager cts = null;
	/** The column descriptor result set */
	protected static final SimpleResultSet COL_DESCRIPTOR = newDef();
	
	
	public static ResultSet getValues(Connection conn, long oldestPeriod, Long...ids) throws SQLException {
	    String url = conn.getMetaData().getURL();
	    if (url.equals("jdbc:columnlist:connection")) {
	        return COL_DESCRIPTOR;
	    }
	    if(cts==null) throw new SQLException("The ChronicleTSManager is not available", new Throwable());
	    SimpleResultSet rs = newDef();
	    Arrays.sort(ids);
	    ChronicleTier tier = cts.getLiveTier();
	    if(ids.length==1 && ids[0]==-1) {
	    	long tierSize = tier.getSize();
	    	for(long id = 0; id < tierSize; id++) {
	    		try {
	    			processId(tier, rs, id);
	    		} catch (Exception ex) {}
	    	}
	    } else {
		    for(long id: ids) {
		    	processId(tier, rs, id);
		    }
	    }
	    return rs;
	}
	
	protected static void processId(ChronicleTier tier, SimpleResultSet rs, long metricId) {		
    	List<long[]> valueArrays = tier.getValues(metricId);
    	String status = tier.getEntryStatusName(metricId);
    	for(long[] row: valueArrays) {
	    	rs.addRow( 
	    			metricId,
	    			status,
	    			new java.sql.Timestamp(row[PERIOD]), 
	    			row[MIN], 
	    			row[MAX], 
	    			row[AVG], 
	    			row[CNT]);
    	}		
	}
	
	/**
	 * Creates a result set for Chronicle time-series data
	 * @return a result set for Chronicle time-series data
	 */
	public static SimpleResultSet newDef() {
	    SimpleResultSet rs = new SimpleResultSet();
	    rs.addColumn("ID", Types.NUMERIC, 255, 22);
	    rs.addColumn("STATUS", Types.VARCHAR, 255, 0);
	    rs.addColumn("TS", Types.TIMESTAMP, 1, 22);
	    rs.addColumn("MIN", Types.NUMERIC, 255, 22);
	    rs.addColumn("MAX", Types.NUMERIC, 255, 22);
	    rs.addColumn("AVG", Types.NUMERIC, 255, 22);
	    rs.addColumn("CNT", Types.NUMERIC, 255, 22);
	    return rs;
	}
	
	/**
	 * Sets the ChronicleTSManager
	 * @param cts the ChronicleTSManager
	 */
	public static void setCts(ChronicleTSManager cts) {
		ChronicleTSAdapter.cts = cts;
	}
}
