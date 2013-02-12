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
package org.helios.apmrouter.cache;

/**
 * <p>Title: CacheStatisticsMBean</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.cache.CacheStatisticsMBean</code></p>
 */

public interface CacheStatisticsMBean {
	
	/**
	 * Discards all entries in the cache.
	 */
	public void invalidateAll();
	
	/**
	 * Performs any pending maintenance operations needed by the cache. 
	 */
	public void cleanup();	
	
	/**
	 * Returns the approximate number of entries in this cache.
	 * @return the approximate number of entries in this cache.
	 */
	public long getSize();
	
	/**
	 * Returns the number of times Cache lookup methods have returned either a cached or uncached value.
	 * @return the cache request count
	 */
	public long getRequestCount();

	/**
	 * Returns the number of times Cache lookup methods have returned a cached value. 
	 * @return the cache hit count
	 */
	public long getHitCount();

	/**
	 * Returns the ratio of cache requests which were hits. This is defined as hitCount / requestCount, or 1.0 when requestCount == 0. Note that hitRate + missRate =~ 1.0. 
	 * @return the cache hit rate
	 */
	public double getHitRate();

	/**
	 * Returns the number of times Cache lookup methods have returned an uncached (newly loaded) value, or null. Multiple concurrent calls to Cache lookup methods on an absent value can result in multiple misses, all returning the results of a single cache load operation. 
	 * @return the cache miss count
	 */
	public long getMissCount();

	/**
	 * Returns the ratio of cache requests which were misses. 
	 * This is defined as missCount / requestCount, or 0.0 when requestCount == 0. 
	 * Note that hitRate + missRate =~ 1.0. 
	 * Cache misses include all requests which weren't cache hits, including requests which 
	 * resulted in either successful or failed loading attempts, 
	 * and requests which waited for other threads to finish loading. 
	 * It is thus the case that missCount &gt;= loadSuccessCount + loadExceptionCount. 
	 * Multiple concurrent misses for the same key will result in a single load operation. 
	 * @return the cache miss rate
	 */
	public double getMissRate();

	/**
	 * Returns the total number of times that Cache lookup methods attempted to load new values.
	 * @return the cache load count
	 */
	public long getLoadCount();

	/**
	 * Returns the number of times Cache lookup methods have successfully loaded a new value.
	 * @return the cache successful load count
	 */
	public long getLoadSuccessCount();

	/**
	 * Returns the number of times Cache lookup methods threw an exception while loading a new value.
	 * @return the cache load exception count
	 */
	public long getLoadExceptionCount();

	/**
	 * Returns the ratio of cache loading attempts which threw exceptions.
	 * @return the cache load exception rate
	 */
	public double getLoadExceptionRate();

	/**
	 * Returns the total number of nanoseconds the cache has spent loading new values.
	 * @return the cache load time
	 */
	public long getTotalLoadTime();

	/**
	 * Returns the average time spent loading new values.
	 * @return the cache average load time
	 */
	public double getAverageLoadPenalty();

	/**
	 * Returns the number of times an entry has been evicted.
	 * @return the cache eviction count
	 */
	public long getEvictionCount();

}