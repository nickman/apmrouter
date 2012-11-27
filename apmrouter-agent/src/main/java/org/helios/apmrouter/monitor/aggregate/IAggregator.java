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
package org.helios.apmrouter.monitor.aggregate;

import java.util.List;

/**
 * <p>Title: IAggregator</p>
 * <p>Description: Defines an aggregation function that accepts a list of objects and returns an object representing the aggregate.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.monitor.aggregate.IAggregator</code></p>
 */

public interface IAggregator {
	/**
	 * Computes an aggregate of the passed items
	 * @param items The items to compute an aggregate for
	 * @return The aggregate result
	 */
	public Object aggregate(List<Object> items);
	
	/**
	 * Aggregates a long array
	 * @param items The array of longs to aggregate
	 * @return the aggregated long value
	 */
	public long aggregate(long[] items);
	
	/**
	 * Aggregates a double array
	 * @param items The array of double to aggregate
	 * @return the aggregated double value
	 */	
	public double aggregate(double[] items);

	
}
