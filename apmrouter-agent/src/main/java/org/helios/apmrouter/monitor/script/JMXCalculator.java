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
package org.helios.apmrouter.monitor.script;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.helios.apmrouter.monitor.aggregate.AggregateFunction;

/**
 * <p>Title: JMXCalculator</p>
 * <p>Description: A java wrapper for a java-script JSON specified calculation formula</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.monitor.script.JMXCalculator</code></p>
 */

public class JMXCalculator {
	/** The designated name for this calculation */
	public final String name;
	/** The grouping keys for the calculation */
	public final String[] group;
	/** The aggregate function to use */
	public final AggregateFunction aggregateFunction;
	/** The drivers for the calculator */
	public final JMXScriptRequest[] jmxRequests;
	/** Additional parameters key/values provided for grouping by values not in the ObjectName */
	public final Map<String, String> xParams = new HashMap<String, String>();
	
	/** The json key for the object name */
	public static final String KEY_NAME = "name";
	/** The json key for the group */
	public static final String KEY_GROUP = "group";
	/** The json key for the aggregation function */
	public static final String KEY_FUNCTION = "aggr";
	/** The json key for the additional parameters */
	public static final String KEY_XPARAM = "xparams";
	/** The json key for the driver request */
	public static final String KEY_QUERY = "query";
	/** The json key for the calc responses */
	public static final String KEY_CALCS = "calcs";
	

	
	/**
	 * Creates a new JMXCalculator
	 * @param name The designated name for this calculation
	 * @param group The grouping keys for the calculation
	 * @param aggregateFunction The name of the aggregate function to use
	 * @param jmxRequest The jmx request that drives the calculator
	 */
	public JMXCalculator(String name, String[] group, String aggregateFunction, JMXScriptRequest[] jmxRequests) {
		this.name = name;
		this.group = group;
		this.aggregateFunction = AggregateFunction.forName(aggregateFunction);
		this.jmxRequests = jmxRequests;
	}
	
	/**
	 * Adds an additional key/value pair to be used for grouping
	 * @param key The key
	 * @param value The value
	 */
	public void addXParam(String key, String value) {
		xParams.put(key, value);
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("JMXCalculator [name=");
		builder.append(name);
		builder.append(", group=");
		builder.append(Arrays.toString(group));
		builder.append(", aggregateFunction=");
		builder.append(aggregateFunction);
		if(!xParams.isEmpty()) {
			builder.append(", xParams=");
			for(Map.Entry<String, String> entry: xParams.entrySet()) {
				builder.append("[").append(entry.getKey()).append(":").append(entry.getValue()).append("],");
			}
			builder.deleteCharAt(builder.length()-1);
		}
		builder.append("]");
		return builder.toString();
	}
	
	
	
	
}
