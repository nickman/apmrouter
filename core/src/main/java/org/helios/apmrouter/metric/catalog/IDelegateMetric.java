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
 * <p>Title: IDelegateMetric</p>
 * <p>Description: Represents a created and cached metric identifier</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.metric.IDelegateMetric</code></p>
 */

public interface IDelegateMetric {
	
	/** The format for rendering the fully qualified metricId name */
	static final String FQN_FORMAT = "%s" + IMetric.NSDELIM + "%s%s" + IMetric.NADELIM + "%s" ;


	/**
	 * Returns the host name that this metricId originated from
	 * @return the host name that this metricId originated from
	 */
	public abstract String getHost();

	/**
	 * The name of the agent that this metricId originated from
	 * @return the name of the agent that this metricId originated from
	 */
	public abstract String getAgent();

	/**
	 * Returns the metricId name
	 * @return the name
	 */
	public abstract String getName();

	/**
	 * The namespace of the metricId
	 * @return the namespace
	 */
	public abstract String[] getNamespace();
	
	/**
	 * Returns the fully qualified metric name
	 * @return the fully qualified metric name
	 */
	public abstract String getFQN();
	

	/**
	 * Returns the concatenated namespace
	 * @return the concatenated namespace
	 */
	public abstract String getNamespaceF();	

	/**
	 * Indicates if the metricId namespace is flat or mapped
	 * @return true if the metricId namespace is flat, false if it is mapped
	 */
	public abstract boolean isFlat();

	/**
	 * Indicates if the metricId namespace is flat or mapped
	 * @return true if the metricId namespace is mapped, false if it is flat
	 */
	public abstract boolean isMapped();
	
	/**
	 * Returns the metric type
	 * @return the metric type
	 */
	public abstract MetricType getType();
	
	/**
	 * Returns the serialization token for this IMetric
	 * @return the serialization token for this IMetric or -1 if one has not been assigned
	 */
	public abstract long getToken();
	
	/**
	 * Sets the serialization token for this IMetric
	 * @param token the serialization token for this IMetric
	 */
	public abstract void setToken(long token);
	

}