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

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.helios.apmrouter.util.URLHelper;

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
	private final String domain;
	/** The subscription's host */
	private final String host;
	/** The subscription's agent */
	private final String agent;
	/** The subscription's metric name*/
	private String metricName = null;
	/** The subscription's max depth */
	private int maxd = -1;
	
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
	
	/** A map of enum constants indexed by the ordinal externally indexed by the enum class */
	private static final Map<Class<? extends Enum<?>>, Map<Integer, ? extends Enum<?>>> ORDMAPS = new HashMap<Class<? extends Enum<?>>, Map<Integer, ? extends Enum<?>>>();
	
	/** The metric namespace delimiter splitter */
	public static final Pattern SLASH_SPLITTER = Pattern.compile("/");
	/** The query delimiter splitter */
	public static final Pattern AMP_SPLITTER = Pattern.compile("&");
	/** The query value comma splitter */
	public static final Pattern COMMA_SPLITTER = Pattern.compile(",");
	
	
	
	public static <T extends Enum<?>> Set<T> extractEnums(Class<T> type, CharSequence value) {
		
		T[] enumValues = type.getEnumConstants();
		Set<T> set = EnumSet.noneOf(enumValues[0].getClass());
		Map<String, T> ORD2ENUM = new HashMap<String, T>(enumValues.length);
		for(T t : enumValues) {
			ORD2ENUM.put("" + t.ordinal(), t);
		}
		String[] evalues = COMMA_SPLITTER.split(value);
		if(evalues.length>0) {
			for(String evalue : evalues) {
				evalue = evalue.trim().toUpperCase();
				T t = ORD2ENUM.get(evalue);
				if(t!=null) {
					set.add(t);
					continue;
				}
				try {
					t = enumValues[0].valueOf(type, evalue);
				} catch (Exception ex) {
					
				}
			}
		}
		
		return set;
	}


	
	/** Option key for the max depth of the query */
	public static final String OPT_MAX_DEPTH = "maxd";
	/** Option key for the metric type filter of the query, parse as comma separated ints */
	public static final String OPT_METRIC_TYPE = "type";
	/** Option key for the status filter of the query, parse as comma separated ints */
	public static final String OPT_METRIC_STATUS = "st";
	/** Option key for the subscription type bit mask */
	public static final String OPT_SUB_TYPE = "subtype";
	
	/** The query keys in a set for quick validation */
	public static final Set<String> QUERY_KEYS = Collections.unmodifiableSortedSet(new TreeSet<String>(Arrays.asList(
			OPT_MAX_DEPTH, OPT_METRIC_TYPE, OPT_METRIC_STATUS, OPT_SUB_TYPE
	)));
	
	/**
	 * Creates a new MetricURIBuilder
	 * @param domain The metric's domain
	 * @param host The metric's host
	 * @param agent The metric's agent
	 * @return a new MetricURIBuilder
	 */
	public static MetricURIBuilder newBuilder(String domain, String host, String agent) {
		return new MetricURIBuilder(domain, host, agent);
	}
	
	/**
	 * Creates a new MetricURIBuilder
	 * @param metricUriPath The metric URI that must contain at least the domain, host and agent. 
	 * @return a new MetricURIBuilder
	 */
	public static MetricURIBuilder newBuilder(CharSequence metricUriPath) {
		return new MetricURIBuilder(metricUriPath);
	}
	
	
	/**
	 * Creates a new MetricURIBuilder
	 * @param domain The metric's domain
	 * @param host The metric's host
	 * @param agent The metric's agent
	 */
	private MetricURIBuilder(String domain, String host, String agent) {
		if(domain==null || domain.trim().isEmpty()) throw new IllegalArgumentException("The passed domain was null or empty", new Throwable());
		if(host==null || host.trim().isEmpty()) throw new IllegalArgumentException("The passed host was null or empty", new Throwable());
		if(agent==null || agent.trim().isEmpty()) throw new IllegalArgumentException("The passed agent was null or empty", new Throwable());
		this.domain = domain;
		this.host = host;
		this.agent = agent;
	}
	
	/**
	 * Creates a new MetricURIBuilder
	 * @param metricUriPath The metric URI that must contain at least the domain, host and agent.
	 */
	private MetricURIBuilder(CharSequence metricUriPath) {
		// ====================================
		// parse the domain, host and agent
		// the 3rd fragment might also contain
		// a metric name
		// ====================================
		if(metricUriPath==null) throw new IllegalArgumentException("The passed metricUriPath was null", new Throwable());
		URI uri = URLHelper.toURI(metricUriPath.toString().trim());
		String path = uri.toString().trim();
		if(path.isEmpty()) throw new IllegalArgumentException("The passed metricUriPath was empty", new Throwable());
		String[] fragments = SLASH_SPLITTER.split(path);
		if(fragments.length<3) throw new IllegalArgumentException("The passed metricUriPath [" + metricUriPath + "] did not have the mandatory domain, host and agent", new Throwable());
		domain = fragments[0].trim();
		host = fragments[1].trim();		
		if(domain.isEmpty()) throw new IllegalArgumentException("The passed domain was null or empty", new Throwable());
		if(host.isEmpty()) throw new IllegalArgumentException("The passed host empty", new Throwable());
		metricName = getMetricName(fragments);		
		agent = fragments[2].trim();
		if(agent.isEmpty()) throw new IllegalArgumentException("The passed agent empty", new Throwable());
		// ====================================
		// parse the namespace
		// ====================================
		if(fragments.length>3) {
			for(int i = 3; i < fragments.length; i++) {
				String frag = fragments[i].trim();
				if(frag.isEmpty()) continue;
				namespaces.add(frag);
			}
		}
		// ====================================
		// there might be a query on the end of the path, so parse for it.
		// ====================================
		String query = uri.getQuery();
		if(query==null || query.trim().isEmpty()) return;
		// QUERY_KEYS
		fragments = AMP_SPLITTER.split(query.trim());
		for(String frag: fragments) {
			frag = frag.trim().toLowerCase();
			int index = frag.indexOf("=");
			if(index==-1) throw new RuntimeException("Invalid MetricURI Option [" + frag + "] was not a key/value pair", new Throwable());
			String key = frag.substring(0, index).trim();
			String value = frag.substring(index+1).trim();
			if(!QUERY_KEYS.contains(key)) throw new RuntimeException("Invalid MetricURI Option Key [" + key + "] was not a valid option key", new Throwable());
			
			
		}
		
		
	}
	
	
	/**
	 * Extracts the metric name from the last member of the passed array.
	 * If a metric name is found, the array member is stripped of the metric name and the <b><code>":"</code></b> delimiter.
	 * @param fragments The path of the metric URI split by <b><code>"/"</code></b>.
	 * @return The extracted metric name or null if one was not found
	 */
	private String getMetricName(String[] fragments) {
		String lastFragment = fragments[fragments.length-1];
		int index = lastFragment.indexOf(':');
		if(index!=-1) {
			String metricName = lastFragment.substring(index+1);
			fragments[fragments.length-1] = lastFragment.substring(0, index);
			if(metricName.trim().isEmpty()) return null;
			return metricName.trim();
		}
		return null;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
