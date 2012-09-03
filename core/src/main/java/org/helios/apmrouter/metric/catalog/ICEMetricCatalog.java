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
package org.helios.apmrouter.metric.catalog;

import org.helios.apmrouter.metric.MetricType;

/**
 * <p>Title: ICEMetricCatalog</p>
 * <p>Description: Maintains a map of all created {@link IDelegateMetric}s </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.metric.ICEMetricCatalog</code></p>
 */

public class ICEMetricCatalog implements IMetricCatalog {
	/** The singleton instance */
	private static volatile IMetricCatalog instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** The system property that defines the metric factory name*/
	public static final String METRIC_FACTORY_PROP = "apmrouter.metric.factory.name";
	/** The default metric factory name*/
	public static final String DEFAULT_METRIC_FACTORY = "org.helios.apmrouter.metric.catalog.heap.LongKeyedHeapMetricCatalog";
	
	/** The delegate concrete catalog implementation, loaded using the class name defined by the system property <b><code>METRIC_FACTORY_PROP</code></b>. */
	private final IMetricCatalog actualCatalog;
	
	/**
	 * Acquires the singleton instance
	 * @return the singleton ICEMetricCatalog 
	 */
	public static IMetricCatalog getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new ICEMetricCatalog();
				}
			}
		}
		return instance;
	}
	
	@SuppressWarnings("unchecked")
	private ICEMetricCatalog() {
		String className = System.getProperty(METRIC_FACTORY_PROP, DEFAULT_METRIC_FACTORY);
		IMetricCatalog tmp = null;
		try {
			Class<IMetricCatalog> clazz = (Class<IMetricCatalog>) Class.forName(className);
			tmp = clazz.newInstance();
		} catch (Exception e) {
			try {
				Class<IMetricCatalog> clazz = (Class<IMetricCatalog>) Class.forName(DEFAULT_METRIC_FACTORY);
				tmp = clazz.newInstance();				
			} catch (Exception ex) {
				throw new RuntimeException("Failed to load configured class name [" + className + "] and default class name [" + DEFAULT_METRIC_FACTORY + "]",  ex);
			}
		}
		actualCatalog = tmp;
	}

	/**
	 * Retrieves the named IDelegateMetric, creating it if it not in the catalog
	 * @param host The host name
	 * @param agent The agent name
	 * @param name The metric name
	 * @param type The metric type
	 * @param namespace The namespace segments
	 * @return the IDelegateMetric
	 */
	@Override
	public IDelegateMetric get(String host, String agent, CharSequence name, MetricType type, CharSequence... namespace) {
		return actualCatalog.get(host, agent, name, type, namespace);
	}

	/**
	 * Retrieves the named IDelegateMetric, creating it if it not in the catalog
	 * @param host The host name
	 * @param agent The agent name
	 * @param name The metric name
	 * @param type The metric type
	 * @param namespace The namespace segments
	 * @return the IDelegateMetric
	 */
	@Override
	public IDelegateMetric get(String host, String agent, CharSequence name, int type, CharSequence... namespace) {
		return actualCatalog.get(host, agent, name, type, namespace);
	}

	/**
	 * Retrieves the named IDelegateMetric, creating it if it not in the catalog
	 * @param host The host name
	 * @param agent The agent name
	 * @param name The metric name
	 * @param type The metric type
	 * @param namespace The namespace segments
	 * @return the IDelegateMetric
	 */
	@Override
	public IDelegateMetric get(String host, String agent, CharSequence name, String type, CharSequence... namespace) {
		return actualCatalog.get(host, agent, name, type, namespace);
	}

	/**
	 * Returns the delta for the passed value and metricId key
	 * @param value The long value to get the delta for
	 * @param host The host name
 	 * @param agent The agent name
	 * @param name The metric name
	 * @param namespace The namespace
	 * @return the delta value or null if this was the first call for the metric
	 */
	@Override
	public Long getDelta(long value, String host, String agent,
			CharSequence name, CharSequence... namespace) {
		return instance.getDelta(value, host, agent, name, namespace);
	}
	
	
	
}
