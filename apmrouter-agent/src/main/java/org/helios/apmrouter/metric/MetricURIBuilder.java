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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * <p>Title: MetricURIBuilder</p>
 * <p>Description: A fluent style builder for a MetricURI.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.metric.MetricURIBuilder</code></p>
 */

public class MetricURIBuilder {
	
	//  <Domain>/<Host>/<Agent>/<Namespace>[:<MetricName>][?<OptionKey1>=<OptionValue1>[...<OptionKeyn>=<OptionValuen>]]
	
	private String domain = null;
	private String host = null;
	private String agent = null;
	private int state = 0;
	private List<String> namespaces = new ArrayList<String>();
	private Set<MetricType> types = EnumSet.noneOf(MetricType.class);
	
	//  SubTypes
//	/** Subscribes to metric state change events */
//	STATE_CHANGE(1),
//	/** Subscribes to new metric events */
//	NEW_METRIC(2),
//	/** Subscribes to a data feed for the metrics */
//	DATA(4);
//
	

	//  Metric Status
//	/** The entry is active and has had recent inserts */
//	ACTIVE((byte)1),
//	/** The entry is stale and has not seen inserts within the stale window */
//	STALE((byte)2),
//	/** The entry is offline and has not seen inserts within one time series tier */
//	OFFLINE((byte)4);

	
	/** Option key for a recursive query */
	public static final String OPT_RECURSIVE = "rec";
	/** Option key for the max depth of the query */
	public static final String OPT_MAX_DEPTH = "maxd";
	/** Option key for the metric type filter of the query, parse as comma separated ints */
	public static final String OPT_METRIC_TYPE = "type";
	/** Option key for the status filter of the query, parse as comma separated ints */
	public static final String OPT_METRIC_STATUS = "st";
	/** Option key for the subscription type bit mask */
	public static final String OPT_SUB_TYPE = "subtype";
	
	/** The metric namespace delimiter splitter */
	public static final Pattern SLASH_SPLITTER = Pattern.compile("/");
	
	/**
	 * Creates a new MetricURIBuilder
	 * @return a new MetricURIBuilder
	 */
	public static MetricURIBuilder newBuilder() {
		return new MetricURIBuilder();
	}
	
	/**
	 * Creates a new MetricURIBuilder
	 */
	private MetricURIBuilder() {
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
