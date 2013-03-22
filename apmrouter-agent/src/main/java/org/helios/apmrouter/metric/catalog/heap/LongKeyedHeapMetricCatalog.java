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
package org.helios.apmrouter.metric.catalog.heap;

import org.helios.apmrouter.metric.MetricType;
import org.helios.apmrouter.metric.catalog.AbstractMetricCatalog;
import org.helios.apmrouter.metric.catalog.IDelegateMetric;

import static org.helios.apmrouter.util.Methods.nvl;

/**
 * <p>Title: LongKeyedHeapMetricCatalog</p>
 * <p>Description: A metric factory that stores the metric catalog in a heap map and indexes the metric names by a long hash code</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.metric.catalog.heap.LongKeyedHeapMetricCatalog</code></p>
 */

public class LongKeyedHeapMetricCatalog extends AbstractMetricCatalog<Long, IDelegateMetric>{

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.catalog.AbstractMetricCatalog#createKey(java.lang.String)
	 */
	@SuppressWarnings("boxing")
	@Override
	protected Long createKey(String fqn) {		
		return longHashCode(nvl(fqn, "FQN"));
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.catalog.AbstractMetricCatalog#get(java.lang.Object)
	 */
	@Override
	protected IDelegateMetric get(Long key) {
		return namecache.get(nvl(key, "Metric Key"));		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.catalog.AbstractMetricCatalog#create(java.lang.Object, java.lang.String, java.lang.String, java.lang.CharSequence, org.helios.apmrouter.metric.MetricType, java.lang.CharSequence[])
	 */
	@Override
	protected IDelegateMetric create(Long key, String host, String agent, CharSequence name, MetricType type, CharSequence... namespace) {
		IDelegateMetric metric = new HeapICEMetric(host, agent, name, type, namespace);
		namecache.put(key, metric);
		return metric;
	}

	

}
