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
package org.helios.apmrouter.destination.mongodb;

import java.util.Set;

import org.helios.apmrouter.tsmodel.Tier;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;

/**
 * <p>Title: MongoDbDestinationMXBean</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.mongodb.MongoDbDestinationMXBean</code></p>
 */

public interface MongoDbDestinationMXBean {


	public abstract void resetMetrics();


	public abstract Set<String> getSupportedMetricNames();

	/**
	 * Returns the number of hosts queued for insert
	 * @return the number of hosts queued for insert
	 */
	@ManagedMetric(category = "MongoDbCatalog", metricType = MetricType.COUNTER, description = "the number of hosts queued for insert")
	public abstract long getHostsQueuedForInsert();

	/**
	 * Returns the number of hosts inserted
	 * @return the number of hosts inserted
	 */
	@ManagedMetric(category = "MongoDbCatalog", metricType = MetricType.COUNTER, description = "the number of hosts inserted")
	public abstract long getHostsInserted();

	/**
	 * Returns the number of agents queued for insert
	 * @return the number of agents queued for insert
	 */
	@ManagedMetric(category = "MongoDbCatalog", metricType = MetricType.COUNTER, description = "the number of agents queued for insert")
	public abstract long getAgentsQueuedForInsert();

	/**
	 * Returns the number of agents inserted
	 * @return the number of agents inserted
	 */
	@ManagedMetric(category = "MongoDbCatalog", metricType = MetricType.COUNTER, description = "the number of agents inserted")
	public abstract long getAgentsInserted();

	/**
	 * Returns the number of metrics queued for insert
	 * @return the number of metrics queued for insert
	 */
	@ManagedMetric(category = "MongoDbCatalog", metricType = MetricType.COUNTER, description = "the number of metrics queued for insert")
	public abstract long getMetricsQueuedForInsert();

	/**
	 * Returns the number of metrics inserted
	 * @return the number of metrics inserted
	 */
	@ManagedMetric(category = "MongoDbCatalog", metricType = MetricType.COUNTER, description = "the number of metrics inserted")
	public abstract long getMetricsInserted();

	/**
	 * Returns the last elapsed write time in ms
	 * @return the last elapsed write time in ms
	 */
	@ManagedMetric(category = "MongoDbCatalog", metricType = MetricType.GAUGE, description = "the last elapsed write time in ms")
	public abstract long getLastElapsedWriteTimeMs();

	/**
	 * Returns the rolling average of elapsed write times in ms
	 * @return the rolling average of elapsed write times in ms
	 */
	@ManagedMetric(category = "MongoDbCatalog", metricType = MetricType.GAUGE, description = "the rolling average of elapsed write times in ms")
	public abstract long getRollingElapsedWriteTimeMs();

	/**
	 * Returns the last elapsed write time in ns
	 * @return the last elapsed write time in ns
	 */
	@ManagedMetric(category = "MongoDbCatalog", metricType = MetricType.GAUGE, description = "the last elapsed write time in ns")
	public abstract long getLastElapsedWriteTimeNs();

	/**
	 * Returns the rolling average of elapsed write times in ns
	 * @return the rolling average of elapsed write times in ns
	 */
	@ManagedMetric(category = "MongoDbCatalog", metricType = MetricType.GAUGE, description = "the rolling average of elapsed write times in ns")
	public abstract long getRollingElapsedWriteTimeNs();

	/**
	 * Returns the last written batch size
	 * @return the last written batch size
	 */
	@ManagedMetric(category = "MongoDbMetrics", metricType = MetricType.GAUGE, description = "the last written batch size")
	public abstract long getLastBatchSize();

	/**
	 * Returns the rolling average of the written batch sizes
	 * @return the rolling average of the written batch sizes
	 */
	@ManagedMetric(category = "MongoDbMetrics", metricType = MetricType.GAUGE, description = "the rolling average of the written batch sizes")
	public abstract long getRollingBatchSizes();

	/**
	 * Returns the time based flush trigger in ms.
	 * @return the time based flush trigger
	 */
	@ManagedAttribute(description = "The elapsed time after which accumulated time-series writes are flushed")
	public abstract long getTimeTrigger();

	/**
	 * Returns the size based flush trigger
	 * @return the size based flush trigger
	 */
	@ManagedAttribute(description = "The number of accumulated time-series writes that triggers a flush")
	public abstract int getSizeTrigger();

	/**
	 * Returns the time-series model definition
	 * @return the time-series model definition
	 */
	@ManagedAttribute(description = "The time-series model definition")
	public abstract String getTsDefinition();

	/**
	 * Returns the time-series model matrix
	 * @return the time-series model matrix
	 */
	@ManagedAttribute(description = "The time-series model matrix")
	public abstract long[][] getModelMatrix();

	/**
	 * Returns the stringified time-series model matrix
	 * @return the stringified time-series model matrix
	 */
	@ManagedAttribute(description = "The time-series model matrix as a string [periodDuration.seconds, tier.tierDuration.seconds, tier.periodCount]")
	public abstract String getModelMatrixString();

	/**
	 * Returns the time-series tiers
	 * @return the time-series tiers
	 */
	@ManagedAttribute(description = "The time-series tiers")
	public abstract Tier[] getTimeSeriesTiers();

}