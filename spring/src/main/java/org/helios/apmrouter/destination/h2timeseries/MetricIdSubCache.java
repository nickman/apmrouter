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
package org.helios.apmrouter.destination.h2timeseries;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>Title: MetricIdSubCache</p>
 * <p>Description: A cache that tracks subscribed metric IDs and the number of interested subscribers for each</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.h2timeseries.MetricIdSubCache</code></p>
 */

public class MetricIdSubCache {
	/** The subCache map */
	protected final ConcurrentHashMap<Long, AtomicLong> subCache = new ConcurrentHashMap<Long, AtomicLong>(128, 0.75f, 16);
	
	/**
	 * Adds a metric id to the cache, incrementing the sub count if it is already registered
	 * @param metricId The metric id to add
	 */
	public void add(long metricId) {
		if(metricId != -1L) {
			subCache.putIfAbsent(metricId, new AtomicLong(1L));
		}
	}
	
	/**
	 * Indicates if the passed metricId is a key in the sub cache
	 * @param metricId The metric id to test for
	 * @return true if the metricId is in cache, false otherwise
	 */
	public boolean containsKey(long metricId) {
		return subCache.containsKey(metricId);
	}
	
	/**
	 * Decrements the subscriber count of the passed metric id, removing the key if it is decremented below zero 
	 * @param metricId the metric id to remove
	 */
	public void remove(long metricId) {
		if(metricId != -1L && subCache.containsKey(metricId)) {
			long cnt = subCache.get(metricId).decrementAndGet();
			if(cnt<1) subCache.remove(metricId);
		}
	}
	
	/**
	 * Returns the number of entries in the cache
	 * @return the number of entries in the cache
	 */
	public int size() {
		return subCache.size();
	}
	
	/**
	 * Returns a read-only copy of the sub cache
	 * @return a read-only copy of the sub cache
	 */
	public Map<Long, AtomicLong> getSubCache() {
		return Collections.unmodifiableMap(subCache);
	}
}
