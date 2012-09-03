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
package org.helios.apmrouter.metric.catalog.direct;

import org.helios.apmrouter.metric.MetricType;
import org.helios.apmrouter.metric.catalog.AbstractMetricCatalog;
import org.helios.apmrouter.metric.catalog.IDelegateMetric;
import org.helios.apmrouter.metric.catalog.direct.chronicle.ChronicleController;
import org.helios.apmrouter.metric.catalog.heap.HeapICEMetric;

import vanilla.java.chronicle.Excerpt;
import vanilla.java.chronicle.impl.IndexedChronicle;

/**
 * <p>Title: StringKeyedChronicleMetricCatalog</p>
 * <p>Description: A metric factory that stores the metric catalog in a chronicle and indexes the metric names by the metric FQN.
 * Intended to be used instead of {@link LongKeyedChronicleMetricCatalog} in the event that the long hashcode algorithm creates too many collisions.
 * </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.metric.catalog.direct.StringKeyedChronicleMetricCatalog</code></p>
 */

public class StringKeyedChronicleMetricCatalog extends AbstractMetricCatalog<String, Long>{
	/** The chronicle controller */
	protected final ChronicleController chron;

	/**
	 * Creates a new StringKeyedChronicleMetricCatalog
	 */
	public StringKeyedChronicleMetricCatalog() {		
		super();
		chron = ChronicleController.getInstance();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.catalog.AbstractMetricCatalog#createKey(java.lang.String)
	 */
	@Override
	protected String createKey(String fqn) {
		return fqn;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.catalog.AbstractMetricCatalog#get(java.lang.Object)
	 */
	@Override
	protected IDelegateMetric get(String key) {
		Long metricId =  namecache.get(key);
		if(metricId==null) return null;
		Excerpt<IndexedChronicle> ex = ChronicleController.getInstance().createExcerpt();
		if(!ex.index(metricId)) return null;
		ChronicleICEMetric dim = new ChronicleICEMetric(ex, metricId);
		namecache.put(key, dim.index);
		return dim;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.catalog.AbstractMetricCatalog#create(java.lang.String, java.lang.String, java.lang.CharSequence, org.helios.apmrouter.metric.MetricType, java.lang.CharSequence[])
	 */
	@Override
	protected IDelegateMetric create(String key, String host, String agent, CharSequence name, MetricType type, CharSequence... namespace) {
		ChronicleICEMetric metric = ChronicleICEMetric.newInstance(host, agent, name, type, namespace);
		namecache.put(key, metric.index);
		return metric;
	}
	
	
}
