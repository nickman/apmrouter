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
package org.helios.apmrouter.destination.opentsdb2;

import java.util.Collection;

import net.opentsdb.core.DataPointSet;
import net.opentsdb.core.datastore.Datastore;
import net.opentsdb.core.exception.DatastoreException;

import org.helios.apmrouter.destination.BaseAsyncDestination;
import org.helios.apmrouter.metric.IMetric;

/**
 * <p>Title: EmbeddedOpenTSDBDestination</p>
 * <p>Description: Destination to store APMRouter metrics into an embedded open-tsdb instance.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.opentsdb2.EmbeddedOpenTSDBDestination</code></p>
 */

public class EmbeddedOpenTSDBDestination extends BaseAsyncDestination {
	/** The embedded OpenTSDB's data store */
	protected Datastore datastore = null;
	


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.BaseAsyncDestination#doFlush(java.util.Collection)
	 */
	@Override
	protected void doFlush(Collection<IMetric> flushedItems) {
		int cnt = 0;
		for(IMetric metric: flushedItems) {
			DataPointSet dps = toDataPointSet(metric);
			if(dps!=null) {
				try {
					datastore.putDataPoints(dps);
					cnt++;
				} catch (DatastoreException e) {
					e.printStackTrace();
				}
			}
		}		
		info("Wrote [", cnt, "] DataPoints");
	}
	
	/**
	 * Converts an APMRouter metric to an OpenTSDB {@link DataPointSet}.
	 * @param metric The metric to convert
	 * @return the created DataPointSet
	 */
	protected DataPointSet toDataPointSet(IMetric metric) {
		if(metric.isFlat() || !metric.getType().isLong()) return null;
		return new DataPointSet(metric.getName(), metric.getNamespaceMap(), metric.getTime(), metric.getLongValue());
		
	}
	
	
	/**
	 * Creates a new EmbeddedOpenTSDBDestination
	 * @param patterns The patterns this destination accepts
	 */
	public EmbeddedOpenTSDBDestination(String... patterns) {
		super(patterns);
	}
	
	/**
	 * Creates a new EmbeddedOpenTSDBDestination
	 */
	public EmbeddedOpenTSDBDestination() {
		super();
		this.matchPatterns.add("LONG.*|DELTA.*");
	}

	/**
	 * Creates a new EmbeddedOpenTSDBDestination
	 * @param patterns The patterns this destination accepts
	 */
	public EmbeddedOpenTSDBDestination(Collection<String> patterns) {
		super(patterns);
	}

	/**
	 * Returns the embedded OpenTSDB's data store
	 * @return the embedded OpenTSDB's data store
	 */
	public Datastore getDatastore() {
		return datastore;
	}


	/**
	 * Sets the embedded OpenTSDB's data store
	 * @param datastore the embedded OpenTSDB's data store
	 */
	public void setDatastore(Datastore datastore) {
		this.datastore = datastore;
	}


}
