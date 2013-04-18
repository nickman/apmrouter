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
import java.util.Arrays;
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
	/** The subscription's domain */
	private String domain = null;
	/** The subscription's host */
	private String host = null;
	/** The subscription's agent */
	private String agent = null;
	private int state = 0;
	/** The subscription's metric name spaces, defaulting to none */
	private List<String> namespaces = new ArrayList<String>();
	
	/** The subscription's metric types, defaulting to all long types */
	private Set<MetricType> metricTypes = EnumSet.of(MetricType.DELTA_COUNTER, MetricType.getLongMetricTypes()) ;
	/** The subscription's sub event types, defaulting to all */
	private Set<SubscriptionType> subTypes = EnumSet.allOf(SubscriptionType.class);
	/** The subscription's event statuses, defaulting to all */
	private Set<MetricStatus> statuses = EnumSet.allOf(MetricStatus.class);
	
	
	/**
	 * <p>Title: SubscriptionType</p>
	 * <p>Description: Defines the MetricURI events that a MetricURI subscription is interested in.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.metric.MetricURIBuilder.SubscriptionType</code></p>
	 */
	public static enum SubscriptionType {
		/** Subscribes to metric state change events */
		STATE_CHANGE,
		/** Subscribes to new metric events */
		NEW_METRIC,
		/** Subscribes to a data feed for the metrics */
		DATA;		
	}
	
	/**
	 * <p>Title: MetricStatus</p>
	 * <p>Description: Defines the possibles statuses of a metric that a subscriber might be interested to know about</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.metric.MetricURIBuilder.MetricStatus</code></p>
	 */
	public static enum MetricStatus {
		/** The entry is active and has had recent inserts */
		ACTIVE,
		/** The entry is stale and has not seen inserts within the stale window */
		STALE,
		/** The entry is offline and has not seen inserts within one time series tier */
		OFFLINE;		
	}
	
	


	
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
