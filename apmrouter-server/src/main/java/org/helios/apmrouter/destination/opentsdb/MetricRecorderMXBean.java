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
package org.helios.apmrouter.destination.opentsdb;

import javax.management.MXBean;

/**
 * <p>Title: MetricRecorderMXBean</p>
 * <p>Description: JMX interface for the Metric Recorder </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.san.MetricRecorderMXBean</code></p>
 */
@MXBean(true)
public interface MetricRecorderMXBean {
	public long getDropTimeout();
	public void setDropTimeout(long timeout);
	public long getDropCounter();
	public boolean isThrottling();
	public long getMetricCounter();
	public long getThrottleIncidentCount();
	public long getThrottleTime();
	public long getContendedMetaLookupCount();
	public long getUncontendedMetaLookupCount();
	public int getUidCacheHits();
	public int getUidCacheMisses();
	public int getUidCacheSize();
	public int getReprocessorQueueSize();
	/**
	 * Returns the number of asynch throttle exceptions received since the last reset
	 * @return the number of asynch throttle exceptions received since the last reset
	 */
	public long getThrottleExceptionCounter();
	
	/**
	 * Returns the number of asynch exceptions received since the last reset
	 * @return the number of asynch exceptions received since the last reset
	 */
	public long getAsynchExceptionCounter();
	
	/**
	 * Resets the metric count counter only
	 */
	public void resetMetricCount();
	
	/**
	 * Flushes the reprocessing queue causing all the pending metric submissions to be dropped.
	 */
	public void flushReprocessingQueue();
	
	
	
	
	
	public void resetMetrics(); 
}
