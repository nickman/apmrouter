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

import org.helios.apmrouter.destination.BaseDestination;
import org.helios.apmrouter.metric.IMetric;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;

/**
 * <p>Title: EmbeddedOpenTSDBDestination</p>
 * <p>Description: Destination to store APMRouter metrics into an embedded open-tsdb instance.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.opentsdb2.EmbeddedOpenTSDBDestination</code></p>
 */

public class EmbeddedOpenTSDBDestination extends BaseDestination {
	/** The embedded OpenTSDB's data store */
	protected Datastore datastore = null;
	


//	/**
//	 * {@inheritDoc}
//	 * @see org.helios.apmrouter.destination.BaseAsyncDestination#doFlush(java.util.Collection)
//	 */
//	@Override
//	protected void doFlush(Collection<IMetric> flushedItems) {
//		int cnt = 0;
//		for(IMetric metric: flushedItems) {
//			DataPointSet dps = toDataPointSet(metric);
//			if(dps!=null) {
//				try {
//					datastore.putDataPoints(dps);
//					cnt++;
//				} catch (DatastoreException e) {
//					e.printStackTrace();
//				}
//			}
//		}		
//		info("Wrote [", cnt, "] DataPoints");
//	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.BaseDestination#doAcceptRoute(org.helios.apmrouter.metric.IMetric)
	 */
	@Override
	protected void doAcceptRoute(IMetric routable) {
		try {
			//datastore.putDataPoints(toDataPointSet(routable));
			if(routable!=null) {
				datastore.queueDataPoints(toDataPointSet(routable));				
			}
		} catch (Exception ex) {
			//ex.printStackTrace(System.err);
		}
	}
	
	/**
	 * Returns the total number of enqueued datapoints
	 * @return the total number of enqueued datapoints
	 */
	@ManagedMetric(category="EmbeddedOpenTSDB", displayName="EnqueuedPoints", metricType=MetricType.COUNTER, description="The total number of enqueued datapoints")
	public long getEnqueuedPoints() {
		if(datastore!=null) return datastore.getEnqueuedCount();
		return -1L;
	}
	
	/**
	 * Returns the total number of dequeued datapoints
	 * @return the total number of dequeued datapoints
	 */
	@ManagedMetric(category="EmbeddedOpenTSDB", displayName="DequeuedPoints", metricType=MetricType.COUNTER, description="The total number of dequeued datapoints")
	public long getDequeuedPoints() {		
		if(datastore!=null) return datastore.getDequeuedCount();
		return -1L;
	}
	
	/**
	 * Returns the current queue size
	 * @return the current queue size
	 */
	@ManagedMetric(category="EmbeddedOpenTSDB", displayName="QueueDepth", metricType=MetricType.COUNTER, description="The current queue size")
	public int getQueueDepth() {		
		if(datastore!=null) return datastore.getQueueSize();
		return -1;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.BaseDestination#doStart()
	 */
	@Override
	protected void doStart() throws Exception {
		info("OpenTSDB Datastore [", datastore.getClass().getSimpleName(), "] Started:" + datastore.isStarted());
		super.doStart();
	}
	
	/**
	 * Converts an APMRouter metric to an OpenTSDB {@link DataPointSet}.
	 * @param metric The metric to convert
	 * @return the created DataPointSet
	 */
	protected DataPointSet toDataPointSet(IMetric metric) {
		if(metric.isFlat() || !metric.getType().isLong()) return null;
		try {
			return new DataPointSet(metric.getName(), metric.getNamespaceMap(), metric.getTime(), metric.getLongValue());
		} catch (Exception ex) {
			return null;
		}
		
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
