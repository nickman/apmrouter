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

import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.metric.MetricType;

/**
 * <p>Title: IMetricCatalog</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.metric.catalog.IMetricCatalog</code></p>
 */

public interface IMetricCatalog {
	
	/** The namespace delimiter */
	static final String NSDELIM = "/";
	/** The name delimiter */
	static final String NADELIM = ":";
	/** The timestamp start delimiter */
	static final String TS_S_DELIM = "[";
	/** The timestamp end delimiter */
	static final String TS_E_DELIM = "]";
	/** The value delimiter */
	static final String VDELIM = "/";
	/** The mapped namespace pair delimiter */
	static final String MDELIM = "=";

	/** The format for rendering a transmittable metricId */
	static final String TX_FORMAT = TS_S_DELIM + "%s" + TS_E_DELIM + "%s" + NSDELIM + "%s%s" + VDELIM + "%s" ;
	/** The format for rendering the fully qualified metricId name */
	static final String FQN_FORMAT = "%s" + NSDELIM + "%s%s" + NADELIM + "%s" ;
	

	/**
	 * Retrieves the named IDelegateMetric, creating it if it not in the catalog
	 * @param host The host name
	 * @param agent The agent name
	 * @param name The metric name
	 * @param type The metric type
	 * @param namespace The namespace segments
	 * @return the IDelegateMetric
	 */
	public abstract IDelegateMetric get(String host, String agent, CharSequence name, MetricType type,
			CharSequence... namespace);
	
	/**
	 * Returns the delegate metric id with the passed token
	 * @param metricIdToken The token to resolve
	 * @return The resolved metric ID or null if not found
	 */
	public abstract IDelegateMetric get(long metricIdToken);
	
	/**
	 * Sets the serialization token for the passed metric identifier
	 * @param host The host name
	 * @param agent The agent name
	 * @param name The metric name
	 * @param type The metric type
	 * @param namespace The namespace segments
	 * @return  the assigned token
	 */
	public abstract long setToken(String host, String agent, CharSequence name, MetricType type,
			CharSequence... namespace);
	
	/**
	 * Sets the serialization token for the passed metric identifier
	 * @param token The token to set on the metric 
	 * @param host The host name
	 * @param agent The agent name
	 * @param name The metric name
	 * @param type The metric type
	 * @param namespace The namespace segments
	 * @return  the assigned token
	 */
	public long setToken(long token, String host, String agent, CharSequence name, MetricType type, CharSequence... namespace);
	
	
	/**
	 * Sets the serialization token for the passed un-tokenized metric 
	 * @param metric the un-tokenized IMetric to tokenize
	 * @return  the assigned token
	 */
	public abstract long setToken(IMetric metric);
	
	/**
	 * Sets the serialization token for the IDelegateMetric with the passed FQN 
	 * @param metricFqn The FQN of the IDelegateMetric to update
	 * @param token The token 
	 */
	public abstract void setToken(CharSequence metricFqn, long token);	
	
	
	/**
	 * Sets the serialization token for the passed un-tokenized metricId 
	 * @param metricId the un-tokenized IDelegateMetric to tokenize
	 * @param token The token to set
	 * @return the set token
	 */
	public abstract long setToken(IDelegateMetric metricId, long token); 	

	/**
	 * Retrieves the named IDelegateMetric, creating it if it not in the catalog.
	 * Implementations of this method should throw an assertion exception if the retrieved delegate metric FQN does not match the key correlated FQN and assertions are enabled. This indicates a catalog hashing collision. 
	 * @param host The host name
	 * @param agent The agent name
	 * @param name The metric name
	 * @param type The metric type
	 * @param namespace The namespace segments
	 * @return the IDelegateMetric
	 */
	public abstract IDelegateMetric get(String host, String agent, CharSequence name, int type,
			CharSequence... namespace);

	/**
	 * Retrieves the named IDelegateMetric, creating it if it not in the catalog
	 * Implementations of this method should throw an assertion exception if the retrieved delegate metric FQN does not match the key correlated FQN and assertions are enabled. This indicates a catalog hashing collision.
	 * @param host The host name
	 * @param agent The agent name
	 * @param name The metric name
	 * @param type The metric type
	 * @param namespace The namespace segments
	 * @return the IDelegateMetric
	 */
	public abstract IDelegateMetric get(String host, String agent, CharSequence name, String type,
			CharSequence... namespace);
	
	/**
	 * Returns the delta for the passed value and metricId key
	 * Implementations of this method should throw an assertion exception if the retrieved delegate metric FQN does not match the key correlated FQN and assertions are enabled. This indicates a catalog hashing collision.
	 * @param value The long value to get the delta for
	 * @param host The host name
 	 * @param agent The agent name
	 * @param name The metric name
	 * @param namespace The namespace
	 * @return the delta value or null if this was the first call for the metric
	 */
	public Long getDelta(long value, String host, String agent, CharSequence name, CharSequence... namespace);
	
	/**
	 * Returns the number of entries in the metric catalog
	 * @return the number of entries in the metric catalog
	 */
	public int size();
	
	/**
	 * Returns a delegate metric for the passed FQN and type
	 * @param fqn The metric FQN
	 * @param type The metric type
	 * @return the delegate metric
	 */
	public IDelegateMetric build(String fqn, MetricType type);	
	
	/**
	 * <p>Testing hook for disposing a switched catalog.
	 * <p><b>DO NOT CALL THIS METHOD UNLESS YOU KNOW WHAT YOU'RE DOING.</b>
	 */
	public void dispose();
	

}