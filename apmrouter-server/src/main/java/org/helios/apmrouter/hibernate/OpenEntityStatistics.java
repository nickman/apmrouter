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

import javax.management.MXBean;

import org.hibernate.stat.EntityStatistics;

/**
 * <p>Title: OpenEntityStatistics</p>
 * <p>Description: Open type wrapper for a {@link EntityStatistics} instance </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.hibernate.OpenEntityStatistics</code></p>
 */
public class OpenEntityStatistics implements OpenEntityStatisticsMBean {
	/** The delegate instance */
	protected final EntityStatistics stats;

	/**
	 * Creates a new OpenEntityStatistics
	 * @param stats The delegate {@link EntityStatistics} instance 
	 */
	public OpenEntityStatistics(EntityStatistics stats) {
		this.stats = stats;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.OpenEntityStatisticsMBean#getDeleteCount()
	 */
	@Override
	public long getDeleteCount() {
		return stats.getDeleteCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.OpenEntityStatisticsMBean#getInsertCount()
	 */
	@Override
	public long getInsertCount() {
		return stats.getInsertCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.OpenEntityStatisticsMBean#getCategoryName()
	 */
	@Override
	public String getCategoryName() {
		return stats.getCategoryName();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.OpenEntityStatisticsMBean#getLoadCount()
	 */
	@Override
	public long getLoadCount() {
		return stats.getLoadCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.OpenEntityStatisticsMBean#getUpdateCount()
	 */
	@Override
	public long getUpdateCount() {
		return stats.getUpdateCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.OpenEntityStatisticsMBean#getFetchCount()
	 */
	@Override
	public long getFetchCount() {
		return stats.getFetchCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.OpenEntityStatisticsMBean#getOptimisticFailureCount()
	 */
	@Override
	public long getOptimisticFailureCount() {
		return stats.getOptimisticFailureCount();
	}

}
