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
package org.helios.apmrouter.catalog;

import java.util.HashSet;
import java.util.Set;

import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.metric.MetricType;
import org.helios.apmrouter.metric.catalog.ICEMetricCatalog;
import org.helios.apmrouter.metric.catalog.IDelegateMetric;
import org.helios.apmrouter.metric.catalog.IMetricCatalog;
import org.helios.apmrouter.server.ServerComponentBean;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * <p>Title: MetricCatalogInitializer</p>
 * <p>Description: Initializes the metric catalog, overriding any instance already in place from spurious singleton calls</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.catalog.MetricCatalogInitializer</code></p>
 */
@ManagedResource
public class MetricCatalogInitializer extends ServerComponentBean implements IMetricCatalog {
	/** The catalog class name to load */
	protected String catalogClassName = null;
	/** The catalog */
	protected IMetricCatalog catalog;

	/**
	 * Initializes the metric catalog
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStart()
	 */
	@Override
	protected void doStart() throws Exception {
		Class<?> catalogClass = Class.forName(catalogClassName);
		if(!IMetricCatalog.class.isAssignableFrom(catalogClass)) {
			throw new Exception("The class [" + catalogClassName + "] is not an IMetricCatalog", new Throwable());
		}
		System.setProperty(ICEMetricCatalog.METRIC_FACTORY_PROP, catalogClassName);
		ICEMetricCatalog.getInstance().dispose();
		catalog = ICEMetricCatalog.getInstance();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponent#getSupportedMetricNames()
	 */
	@Override
	public Set<String> getSupportedMetricNames() {
		return super.getSupportedMetricNames();
	}
	/**
	 * @param host
	 * @param agent
	 * @param name
	 * @param type
	 * @param namespace
	 * @return  IDelegateMetric
	 * @see org.helios.apmrouter.metric.catalog.IMetricCatalog#get(java.lang.String, java.lang.String, java.lang.CharSequence, org.helios.apmrouter.metric.MetricType, java.lang.CharSequence[])
	 */
	@Override
	public IDelegateMetric get(String host, String agent, CharSequence name,
			MetricType type, CharSequence... namespace) {
		return catalog.get(host, agent, name, type, namespace);
	}

	/**
	 * @param metricIdToken
	 * @return  IDelegateMetric
	 * @see org.helios.apmrouter.metric.catalog.IMetricCatalog#get(long)
	 */
	@Override
	public IDelegateMetric get(long metricIdToken) {
		return catalog.get(metricIdToken);
	}

	/**
	 * @param host
	 * @param agent
	 * @param name
	 * @param type
	 * @param namespace
	 * @return    long
	 * @see org.helios.apmrouter.metric.catalog.IMetricCatalog#setToken(java.lang.String, java.lang.String, java.lang.CharSequence, org.helios.apmrouter.metric.MetricType, java.lang.CharSequence[])
	 */
   	public long setToken(String host, String agent, CharSequence name,
			MetricType type, CharSequence... namespace) {
		return catalog.setToken(host, agent, name, type, namespace);
	}

	/**
	 * @param metric
	 * @return  long
	 * @see org.helios.apmrouter.metric.catalog.IMetricCatalog#setToken(org.helios.apmrouter.metric.IMetric)
	 */

	public long setToken(IMetric metric) {
		return catalog.setToken(metric);
	}

	/**
	 * @param metricFqn
	 * @param token
	 * @see org.helios.apmrouter.metric.catalog.IMetricCatalog#setToken(java.lang.CharSequence, long)
	 */
  	public void setToken(CharSequence metricFqn, long token) {
		catalog.setToken(metricFqn, token);
	}

	/**
	 * @param metricId
	 * @param token
	 * @return  long
	 * @see org.helios.apmrouter.metric.catalog.IMetricCatalog#setToken(org.helios.apmrouter.metric.catalog.IDelegateMetric, long)
	 */
  	public long setToken(IDelegateMetric metricId, long token) {
		return catalog.setToken(metricId, token);
	}

	/**
	 * @param host
	 * @param agent
	 * @param name
	 * @param type
	 * @param namespace
	 * @return  IDelegateMetric
	 * @see org.helios.apmrouter.metric.catalog.IMetricCatalog#get(java.lang.String, java.lang.String, java.lang.CharSequence, int, java.lang.CharSequence[])
	 */
	public IDelegateMetric get(String host, String agent, CharSequence name,
			int type, CharSequence... namespace) {
		return catalog.get(host, agent, name, type, namespace);
	}

	/**
	 * @param host
	 * @param agent
	 * @param name
	 * @param type
	 * @param namespace
	 * @return  IDelegateMetric
	 * @see org.helios.apmrouter.metric.catalog.IMetricCatalog#get(java.lang.String, java.lang.String, java.lang.CharSequence, java.lang.String, java.lang.CharSequence[])
	 */
	public IDelegateMetric get(String host, String agent, CharSequence name,
			String type, CharSequence... namespace) {
		return catalog.get(host, agent, name, type, namespace);
	}

	/**
	 * @param value
	 * @param host
	 * @param agent
	 * @param name
	 * @param namespace
	 * @return Long
	 * @see org.helios.apmrouter.metric.catalog.IMetricCatalog#getDelta(long, java.lang.String, java.lang.String, java.lang.CharSequence, java.lang.CharSequence[])
	 */
	@Override
	public Long getDelta(long value, String host, String agent,
			CharSequence name, CharSequence... namespace) {
		return catalog.getDelta(value, host, agent, name, namespace);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.catalog.IMetricCatalog#size()
	 */	
	@Override
	public int size() {
		return catalog.size();
	}
	
	/**
	 * Returns the size of this metric catalog
	 * @return the size of this metric catalog
	 */
	@ManagedAttribute
	public int getSize() {
		return size();
	}

	/**
	 * @param fqn
	 * @param type
	 * @return  IDelegateMetric
	 * @see org.helios.apmrouter.metric.catalog.IMetricCatalog#build(java.lang.String, org.helios.apmrouter.metric.MetricType)
	 */
	@Override
	public IDelegateMetric build(String fqn, MetricType type) {
		return catalog.build(fqn, type);
	}

	/**
	 * 
	 * @see org.helios.apmrouter.metric.catalog.IMetricCatalog#dispose()
	 */
	@Override
	public void dispose() {
		catalog.dispose();
	}

	/**
	 * Returns the catalog class name
	 * @return the catalogClassName
	 */
	@ManagedAttribute
	public String getCatalogClassName() {
		return catalogClassName;
	}
	
	/**
	 * Sets the catalog class name
	 * @param className the catalog class name
	 */
	public void setCatalogClassName(String className) {
		this.catalogClassName = className;
	}

	/**
	 * Returns the catalog delegate
	 * @return the catalog
	 */
	public IMetricCatalog getCatalog() {
		return catalog;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.catalog.IMetricCatalog#setToken(long, java.lang.String, java.lang.String, java.lang.CharSequence, org.helios.apmrouter.metric.MetricType, java.lang.CharSequence[])
	 */
	@Override
	public long setToken(long token, String host, String agent, CharSequence name, MetricType type, CharSequence... namespace) {
		return catalog.setToken(token, host, agent, name, type, namespace);
				
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.catalog.IMetricCatalog#resetTokens()
	 */
	@Override
	public void resetTokens() {
		/* No Op */		
	}
}
