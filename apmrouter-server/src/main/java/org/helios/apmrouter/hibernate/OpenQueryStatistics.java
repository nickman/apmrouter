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

import org.hibernate.stat.QueryStatistics;

/**
 * <p>Title: OpenQueryStatistics</p>
 * <p>Description: Open type wrapper for a {@link QueryStatistics} instance </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.hibernate.OpenQueryStatistics</code></p>
 */

public class OpenQueryStatistics implements OpenQueryStatisticsMBean {
	/** The delegate stats */
	protected final QueryStatistics stats;

	/**
	 * Creates a new OpenQueryStatistics
	 * @param stats the delegate stats
	 */
	public OpenQueryStatistics(QueryStatistics stats) {
		this.stats = stats;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.OpenQueryStatisticsMBean#getCategoryName()
	 */
	@Override
	public String getCategoryName() {
		return stats.getCategoryName();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.OpenQueryStatisticsMBean#getExecutionCount()
	 */
	@Override
	public long getExecutionCount() {
		return stats.getExecutionCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.OpenQueryStatisticsMBean#getCacheHitCount()
	 */
	@Override
	public long getCacheHitCount() {
		return stats.getCacheHitCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.OpenQueryStatisticsMBean#getCachePutCount()
	 */
	@Override
	public long getCachePutCount() {
		return stats.getCachePutCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.OpenQueryStatisticsMBean#getCacheMissCount()
	 */
	@Override
	public long getCacheMissCount() {
		return stats.getCacheMissCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.OpenQueryStatisticsMBean#getExecutionRowCount()
	 */
	@Override
	public long getExecutionRowCount() {
		return stats.getExecutionRowCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.OpenQueryStatisticsMBean#getExecutionAvgTime()
	 */
	@Override
	public long getExecutionAvgTime() {
		return stats.getExecutionAvgTime();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.OpenQueryStatisticsMBean#getExecutionMaxTime()
	 */
	@Override
	public long getExecutionMaxTime() {
		return stats.getExecutionMaxTime();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.OpenQueryStatisticsMBean#getExecutionMinTime()
	 */
	@Override
	public long getExecutionMinTime() {
		return stats.getExecutionMinTime();
	}

}
