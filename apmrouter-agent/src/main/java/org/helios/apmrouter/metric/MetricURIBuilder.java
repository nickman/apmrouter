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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.management.Attribute;
import javax.management.AttributeList;

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
	 * Builds the metric URI from the current state of this builder
	 * @param useOrdinals If true, enum constants will be rendered using their ordinals, otherwise they will be rendered using their names.
	 * @return the metric URI
	 */
	public URI build(boolean useOrdinals) {
		StringBuilder b = new StringBuilder();
		b.append(domain).append("/").append(host).append("/").append(agent);
		for(String s: namespaces) {
			b.append("/").append(s);
		}
		if(metricName != null) {
			b.append(":").append(metricName);
		}
		b.append("?");
		boolean ao = false;
		if(!metricTypes.isEmpty()) {
			b.append("&").append(OPT_METRIC_TYPE).append("=");
			for(MetricType t: metricTypes) {
				b.append(useOrdinals ? t.ordinal() : t.name()).append(",");
				ao = true;
			}
			if(ao) b.deleteCharAt(b.length()-1);		
		}
		ao = false;
		if(!subTypes.isEmpty()) {
			b.append("&").append(OPT_SUB_TYPE).append("=");
			for(SubscriptionType t: subTypes) {
				b.append(useOrdinals ? t.ordinal() : t.name()).append(",");
				ao = true;
			}
			if(ao) b.deleteCharAt(b.length()-1);		
		}
		ao = false;
		if(!statuses.isEmpty()) {
			b.append("&").append(OPT_METRIC_STATUS).append("=");
			for(MetricStatus t: statuses) {
				b.append(useOrdinals ? t.ordinal() : t.name()).append(",");
				ao = true;
			}
			if(ao) b.deleteCharAt(b.length()-1);		
		}		
		if(maxd>0) {
			b.append("&").append(OPT_MAX_DEPTH).append("=").append(maxd);
		}
		
		return URLHelper.toURI(b);
	}
	
	/**
	 * Builds the metric URI from the current state of this builder, rendering enum constants using ordinals
	 * @return the metric URI
	 */
	public URI build() {
		return build(true);
	}
	
	
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
	private static final Map<Class<? extends Enum<?>>, Map<String, ? extends Enum<?>>> ORDMAPS = new HashMap<Class<? extends Enum<?>>, Map<String, ? extends Enum<?>>>();
	
	/** The metric namespace delimiter splitter */
	public static final Pattern SLASH_SPLITTER = Pattern.compile("/");
	/** The query delimiter splitter */
	public static final Pattern AMP_SPLITTER = Pattern.compile("&");
	/** The query value comma splitter */
	public static final Pattern COMMA_SPLITTER = Pattern.compile(",");
	
	
	
	/**
	 * Parses the passed string value for comma separated values representing enum constants in the passed type.
	 * Enum constants may be specified as the name, or as the oridinal.
	 * @param type The enum type representing the MetricURI option
	 * @param value The comma separated string value to parse
	 * @return a set of select enum options
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Enum<?>> Set<T> extractEnums(Class<T> type, CharSequence value) {		
		T[] enumValues = type.getEnumConstants();
		if(enumValues.length==0) return Collections.emptySet();
		Map<String, T> ORD2ENUM = getEnumCache(type);
		Set<T> set = EnumSet.noneOf(enumValues[0].getClass());
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
					t = (T) Enum.valueOf(enumValues[0].getClass(), evalue);
					set.add(t);
					continue;					
				} catch (Exception ex) { /* No Op */ }
				throw new RuntimeException("Unrecognized option value [" + evalue + "] for option type [" + type.getSimpleName() + "]", new Throwable());
			}
		}		
		return set;
	}
		
	/**
	 * Creates an enum lookup cache for the passed enum type
	 * @param type The enum type to prep a cache for
	 */
	private static <T extends Enum<?>> Map<String, T> getEnumCache(Class<T> type) {
		Map<String, T> ORD2ENUM = (Map<String, T>) ORDMAPS.get(type); 
		if(ORD2ENUM==null) {
			synchronized(ORDMAPS) {
				ORD2ENUM = (Map<String, T>) ORDMAPS.get(type);
				if(ORD2ENUM==null) {
					T[] enumValues = type.getEnumConstants();
					ORD2ENUM = new HashMap<String, T>(enumValues.length);
					for(T t : enumValues) {
						ORD2ENUM.put("" + t.ordinal(), t);
					}
					ORDMAPS.put(type, Collections.unmodifiableMap(ORD2ENUM));					
				}
			}
		}
		return ORD2ENUM;
	}


//	static {
//		getEnumCache(MetricType.class);
//		getEnumCache(SubscriptionType.class);
//		getEnumCache(MetricStatus.class);
//		log("ORD MAP DUMP:");
//		for(Map.Entry<Class<? extends Enum<?>>, Map<String, ? extends Enum<?>>> entry: ORDMAPS.entrySet()) {
//			StringBuilder b = new StringBuilder("\n\t").append(entry.getKey().getSimpleName());
//			for(Map.Entry<String, ? extends Enum<?>> en: entry.getValue().entrySet()) {
//				b.append("\n\t\t[").append(en.getValue().name()).append(":").append(en.getValue().ordinal()).append("/").append(en.getKey()).append("]");
//			}
//			log(b);
//		}
//		
//	}
	
//	private static void log(Object msg) {
//		System.out.println(msg);
//	}
	
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
	 * Creates a new MetricURIBuilder from a minimal or full uri string which is fully validated for MetricURI rules
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
			// One of OPT_MAX_DEPTH, OPT_METRIC_TYPE, OPT_METRIC_STATUS, OPT_SUB_TYPE
			if(OPT_MAX_DEPTH.equals(key)) {
				try {
					maxd = Integer.parseInt(value);
					if(maxd<0) throw new Exception();
				} catch (Exception ex) {
					throw new RuntimeException("Invalid MaxDepth option [" + value + "]", new Throwable());
				}
			} else if(OPT_METRIC_TYPE.equals(key)) {
				metricTypes.clear();
				metricTypes.addAll(extractEnums(MetricType.class, value));
			} else if(OPT_METRIC_STATUS.equals(key)) {
				statuses.clear();
				statuses.addAll(extractEnums(MetricStatus.class, value));				
			} else if(OPT_SUB_TYPE.equals(key)) {
				subTypes.clear();
				subTypes.addAll(extractEnums(SubscriptionType.class, value));				
			}			
		}				
	}
	
	/**
	 * Sets the metric name
	 * @param metricName The metric name to set
	 * @return this builder
	 */
	public MetricURIBuilder metricName(String metricName) {
		if(metricName==null || metricName.trim().isEmpty()) throw new IllegalArgumentException("The passed metric name was null or empty", new Throwable());
		this.metricName = metricName.trim();
		return this;
	}
	
	/**
	 * Set the MetricURI namespace to the passed namespaces, clearing the prior set (even if the passed array is empty !).
	 * @param namespaces The namespaces to set.
	 * @return this builder
	 */
	public MetricURIBuilder namespace(String...namespaces) {
		return namespace(false, namespaces);
	}
	
	/**
	 * Set the MetricURI namespace to the passed namespaces
	 * @param append true to add to the existing, false to replace
	 * @param namespaces The namsepaces to add
	 * @return this builder
	 */
	public MetricURIBuilder namespace(boolean append, String...namespaces) {
		if(!append) this.namespaces.clear();
		if(namespaces!=null) {
			for(String s: namespaces) {
				if(s==null || s.trim().isEmpty()) continue;
				this.namespaces.add(s.trim());
			}
		}
		return this;
	}
	
	/**
	 * Adds metric types to the subscription URI
	 * @param append true to add, false to replace
	 * @param metricTypes An array of metric type enum constants to add
	 * @return this builder
	 */
	public MetricURIBuilder metricType(boolean append, MetricType...metricTypes) {
		if(!append) this.metricTypes.clear();
		if(metricTypes!=null) {
			for(MetricType t: metricTypes) {
				if(t!=null) this.metricTypes.add(t);
			}
		}
		return this;
	}
	
	/**
	 * Appends metric types to the subscription URI
	 * @param metricTypes An array of metric type enum constants to add
	 * @return this builder
	 */
	public MetricURIBuilder appendMetricType(MetricType...metricTypes) {
		return metricType(true, metricTypes);
	}
	
	/**
	 * Replaces the metric types in the subscription URI
	 * @param metricTypes An array of metric type enum constants to replace the existing ones
	 * @return this builder
	 */
	public MetricURIBuilder metricType(MetricType...metricTypes) {
		return metricType(false, metricTypes);
	}
	
	/**
	 * Sets the metric types to long values only
	 * @return this builder
	 */
	public MetricURIBuilder longMetricTypes() {
		return metricType(false, MetricType.getLongMetricTypes());
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
	 * Adds metric statuses to the subscription URI
	 * @param append true to add, false to replace
	 * @param metricStatuses An array of metric status enum constants to add
	 * @return this builder
	 */
	public MetricURIBuilder metricStatus(boolean append, MetricStatus...metricStatuses) {
		if(!append) this.statuses.clear();
		if(metricStatuses!=null) {
			for(MetricStatus t: metricStatuses) {
				if(t!=null) this.statuses.add(t);
			}
		}
		return this;
	}
	
	/**
	 * Sets the maximum recursion depth for the metric URI
	 * @param maxDepth The maximum recursion depth
	 * @return this builder
	 */
	public MetricURIBuilder maxDepth(int maxDepth) {
		if(maxDepth<0) throw new IllegalArgumentException("Invalid max depth [" + maxDepth + "]", new Throwable());
		maxd = maxDepth;
		return this;
	}
	
	/**
	 * Appends metric statuses to the subscription URI
	 * @param metricStatuses An array of metric status enum constants to append
	 * @return this builder
	 */
	public MetricURIBuilder appendMetricStatus(MetricStatus...metricStatuses) {
		return metricStatus(true, metricStatuses);
	}
	
	/**
	 * Replaces the metric statuses in the subscription URI
	 * @param metricStatuses An array of metric status enum constants to replace the current ones with
	 * @return this builder
	 */
	public MetricURIBuilder metricStatus(MetricStatus...metricStatuses) {
		return metricStatus(false, metricStatuses);
	}
	
	/**
	 * Adds subscription types to the subscription URI
	 * @param append true to add, false to replace
	 * @param subTypes An array of subscription type enum constants to add
	 * @return this builder
	 */
	public MetricURIBuilder subType(boolean append, SubscriptionType...subTypes) {
		if(!append) this.subTypes.clear();
		if(subTypes!=null) {
			for(SubscriptionType t: subTypes) {
				if(t!=null) this.subTypes.add(t);
			}
		}
		return this;
	}
	
	/**
	 * Appends subscription types to the subscription URI
	 * @param subTypes An array of subscription type enum constants to add
	 * @return this builder
	 */
	public MetricURIBuilder appendSubType(SubscriptionType...subTypes) {
		return subType(true, subTypes);
	}
	
	/**
	 * Replaces subscription types in the subscription URI
	 * @param subTypes An array of subscription type enum constants to replace the current ones
	 * @return this builder
	 */
	public MetricURIBuilder subType(SubscriptionType...subTypes) {
		return subType(false, subTypes);
	}
	
	
	
	

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log("MetricURI Builder Test");
		log(
				MetricURIBuilder.newBuilder("DefaultDomain", "njw810", "GroovyAgent")
					.namespace("A=B", "C=D", "E=F")
					.metricName("TCPOperations")
					.longMetricTypes()
					.subType(SubscriptionType.NEW_METRIC)
					.metricStatus(MetricStatus.values())
					.build()
		);
		

	}
	
//	public Map<String, Object> attrListToMap(AttributeList attrList) {
//		Map<String, Object> attributeMap = new HashMap<String, Object>(attrList.size());
//		for(Attribute attr: attrList.asList()) {
//			attributeMap.put(attr.getName(), attr.getValue());
//		}
//		return attributeMap;
//	}

	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
}
