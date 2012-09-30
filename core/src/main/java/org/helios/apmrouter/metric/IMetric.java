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
package org.helios.apmrouter.metric;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Map;

import org.helios.apmrouter.router.Routable;
import org.helios.apmrouter.trace.TXContext;

/**
 * <p>Title: IMetric</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.metric.IMetric</code></p>
 */

public interface IMetric extends Routable {
	
	/** The namespace delimiter */
	public static final String NSDELIM = "/";
	/** The name delimiter */
	public static final String NADELIM = ":";
	/** The timestamp start delimiter */
	public static final String TS_S_DELIM = "[";
	/** The timestamp end delimiter */
	public static final String TS_E_DELIM = "]";
	/** The value delimiter */
	public static final String VDELIM = "/";
	/** The mapped namespace pair delimiter */
	public static final String MDELIM = "=";

	/** The format for rendering a transmittable metricId */
	static final String TX_FORMAT = TS_S_DELIM + "%s" + TS_E_DELIM + "%s" + NSDELIM + "%s%s" + VDELIM + "%s" ;
	/** The format for rendering the fully qualified metricId name */
	static final String FQN_FORMAT = "%s" + NSDELIM + "%s%s" + NADELIM + "%s" ;
	
	/** The tag name for the host */
	public static final String HOST_TAG = "host";
	/** The tag name for the agent */
	public static final String AGENT_TAG = "agent";
	/** The tag name for the TXContext txId */
	public static final String TXID_TAG = "txid";
	/** The tag name for the TXContext qualifier */
	public static final String TXQ_TAG = "txqual";
	/** The tag name for the TXContext thread id */
	public static final String TXTHREAD_TAG = "txthread";
	
	

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
	 * The namespace of the metricId
	 * @return the namespace
	 */
	public abstract String[] getNamespace();

	/**
	 * Returns the metricId name
	 * @return the name
	 */
	public abstract String getName();

	/**
	 * Returns the metricId timestamp or -1 if no timestamp has been set
	 * @return the time
	 */
	public abstract long getTime();

	/**
	 * Returns the metricId type
	 * @return the type
	 */
	public abstract MetricType getType();

	/**
	 * Returns the fully qualified metricId name
	 * @return the fully qualified metricId name
	 */
	public abstract String getFQN();

	/**
	 * Returns the concatenated namespace
	 * @return the concatenated namespace
	 */
	public abstract String getNamespaceF();

	/**
	 * Returns the namespace element at the provided index
	 * @param index The namespace index
	 * @return a namespace element
	 */
	public abstract String getNamespace(int index);
	
	/**
	 * Returns the namespace element at the provided index.
	 * Throws a RuntimeException if the metric is not mapped
	 * @param index The namespace index
	 * @return a namespace element
	 */
	public abstract String getNamespace(CharSequence index);
	
	/**
	 * Returns the namespace as a map.
	 * Throws a RuntimeException if the metric is not mapped
	 * @return a map representing the mapped namespace of this metric
	 */
	public abstract Map<String, String> getNamespaceMap();
	
	/**
	 * Returns the namespace as a map.
	 * Throws a RuntimeException if the metric is not mapped
	 * @param tagHostAgent If true, includes the host and agent in the namespace
	 * @param includeTXContext If true, adds the txcontext to the namespace map
	 * @return a map representing the mapped namespace of this metric
	 */
	public abstract Map<String, String> getNamespaceMap(boolean tagHostAgent, boolean includeTXContext);
	

	/**
	 * Returns the number of elements in the namespace
	 * @return the number of elements in the namespace
	 */
	public abstract int getNamespaceSize();

	/**
	 * The timestamp of this metricId as a java date
	 * @return the date
	 */
	public abstract Date getDate();

	/**
	 * Returns the value of this metricId
	 * @return the value
	 */
	public abstract Object getValue();

	/**
	 * Returns the value as a long, or throws a RuntimeException if the type is not long based
	 * @return the long value
	 */
	public abstract long getLongValue();
	
	/**
	 * Returns the number of bytes required to marshall this metric
	 * @return the number of bytes required to marshall this metric
	 */
	public abstract int getSerSize();
	
	/**
	 * Returns the non-long value as it's native byte buffer
	 * @return the non-long value as it's native byte buffer
	 */
	public ByteBuffer getRawValue();
	/**
	 * Returns the serialization token for this IMetric
	 * @return the serialization token for this IMetric or -1 if one has not been assigned
	 */
	public abstract long getToken();
	
	/**
	 * Indicates if this metric has an attached TXContext
	 * @return true if this metric has an attached TXContext, false otherwise
	 */
	public abstract boolean hasTXContext();
	
	/**
	 * Returns the attached TXContext
	 * @return the attached TXContext or null if one is not attached
	 */
	public abstract TXContext getTXContext();
	
	/**
	 * Forces a mapped metric to return an unmapped instance of itself.
	 * If this metric is flat, it just returns this.
	 * @return a flat metric 
	 */
	public IMetric getUnmapped();
	

}