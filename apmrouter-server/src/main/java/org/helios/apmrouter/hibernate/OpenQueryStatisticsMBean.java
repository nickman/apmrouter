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
package org.helios.apmrouter.hibernate;

/**
 * <p>Title: OpenQueryStatisticsMBean</p>
 * <p>Description: Open data type interface for {@link OpenQueryStatistics}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.hibernate.OpenQueryStatisticsMBean</code></p>
 */

public interface OpenQueryStatisticsMBean {

	/**
	 * @return
	 * @see org.hibernate.stat.CategorizedStatistics#getCategoryName()
	 */
	public String getCategoryName();

	/**
	 * @return
	 * @see org.hibernate.stat.QueryStatistics#getExecutionCount()
	 */
	public long getExecutionCount();

	/**
	 * @return
	 * @see org.hibernate.stat.QueryStatistics#getCacheHitCount()
	 */
	public long getCacheHitCount();

	/**
	 * @return
	 * @see org.hibernate.stat.QueryStatistics#getCachePutCount()
	 */
	public long getCachePutCount();

	/**
	 * @return
	 * @see org.hibernate.stat.QueryStatistics#getCacheMissCount()
	 */
	public long getCacheMissCount();

	/**
	 * @return
	 * @see org.hibernate.stat.QueryStatistics#getExecutionRowCount()
	 */
	public long getExecutionRowCount();

	/**
	 * @return
	 * @see org.hibernate.stat.QueryStatistics#getExecutionAvgTime()
	 */
	public long getExecutionAvgTime();

	/**
	 * @return
	 * @see org.hibernate.stat.QueryStatistics#getExecutionMaxTime()
	 */
	public long getExecutionMaxTime();

	/**
	 * @return
	 * @see org.hibernate.stat.QueryStatistics#getExecutionMinTime()
	 */
	public long getExecutionMinTime();

}