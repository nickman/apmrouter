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
package org.helios.apmrouter.dataservice.json.catalog;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import org.helios.apmrouter.cache.CacheStatistics;
import org.helios.apmrouter.catalog.domain.Metric;
import org.helios.apmrouter.jmx.ConfigurationHelper;
import org.helios.apmrouter.metric.MetricType;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * <p>Title: MetricURI</p>
 * <p>Description: Represents a metric query or set definition in the form of a <b><code>URI</code></b>.</p>
 * <p>Components:<ol>
 * 	<li>The URI representing the domain down to a specific folder</li>
 * 	<li><b>path:</b>The path of the URI representing the path of the metric from the domain down to the specified folder. Breaks out into:<ul>
 * 		<li><b>Domain</b></li>
 * 		<li><b>Host</b></li>
 * 		<li><b>Agent</b></li>
 * 		<li><b>Namespace</b></li>
 *  </ul></li>
 * 	<li><b>recursive:</b></li>
 * 	<li><b>maxdepth:</b></li>
 * 	<li><b>metricname:</b></li>
 * 	<li><b>metrictype:</b></li>
 * 	<li><b>status:</b></li>
 * 	<li><b>currentvalue:</b></li>
 * </ol></p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.dataservice.json.catalog.MetricURI</code></p>
 */

public class MetricURI {
	/** The full URI */
	protected final URI metricUri;
	/** The metric domain */
	protected final String domain;
	/** The metric host */
	protected final String host;
	/** The metric agent */
	protected final String agent;
	/** The metric namespace */
	protected final String namespace;
	/** Indicates if the query is recursive */
	protected final boolean recursive;
	/** The maximum depth of the query */
	protected final int maxDepth;
	/** The metric name expression */
	protected final String metricName;
	/** The metric data types */
	protected final int[] metricType;
	/** The metric statuses */
	protected final int[] metricStatus;
	/** The hibernate detached criteria */
	protected final DetachedCriteria detachedCriteria;
	

	/** The URI parameter parser */
	public static final Pattern PARM_PARSER = Pattern.compile("&");
	/** The URI parameter pair parser */
	public static final Pattern PARM_PAIR_PARSER = Pattern.compile("=");	
	/** The URI path parser */
	public static final Pattern PATH_PARSER = Pattern.compile("/");
	/** The comma parser */
	public static final Pattern COM_PARSER = Pattern.compile(",");
	
	/** Option key for a recursive query */
	public static final String OPT_RECURSIVE = "rec";
	/** Option key for the max depth of the query */
	public static final String OPT_MAX_DEPTH = "maxd";
	/** Option key for the max depth of the query */
	public static final String OPT_METRIC_NAME = "name";
	/** Option key for the metric type filter of the query, parse as comma separated ints */
	public static final String OPT_METRIC_TYPE = "type";
	/** Option key for the status filter of the query, parse as comma separated ints */
	public static final String OPT_METRIC_STATUS = "st";
	
	/** The default max depth */
	private static final int[] DEFAULT_DEPTH = new int[]{0};
	/** The default metric types */
	private static final int[] DEFAULT_TYPES;
	/** The default metric statuses */
	private static final int[] DEFAULT_METRIC_STATUS = new int[]{0,1};
	
	// =======================================================================
	//    MetricURI caching configuration and impl
	// =======================================================================
	/** The system property or env variable name that overrides the default max cache size (@link {@value #DEFAULT_METRIC_URI_CACHE_MAXSIZE} */
	public static final String METRIC_URI_CACHE_MAXSIZE_PROP = "org.helios.metricuri.maxsize";
	/** The default max cache size */
	public static final long DEFAULT_METRIC_URI_CACHE_MAXSIZE = 500;
	/** A cache of MetricURIs keyed by the string value of the URI */
	private static LoadingCache<String, MetricURI> metricURICache;
	/** The cache stats for the metricURICache */
	private static final CacheStatistics cacheStatistics;
	// =======================================================================
	
	
	static {
		MetricType[] longTypes = MetricType.getLongMetricTypes();
		int[] longCodes = new int[longTypes.length];
		for(int i = 0; i < longTypes.length; i++) {
			longCodes[i] = longTypes[i].ordinal();
		}
		DEFAULT_TYPES = longCodes;
		metricURICache = CacheBuilder.newBuilder().softValues().recordStats()
				.maximumSize(ConfigurationHelper.getLongSystemThenEnvProperty(METRIC_URI_CACHE_MAXSIZE_PROP, DEFAULT_METRIC_URI_CACHE_MAXSIZE))
				.build(new CacheLoader<String, MetricURI>(){
					@Override
					public MetricURI load(String key) throws Exception {
						return new MetricURI(key);
					}					
				});
		cacheStatistics = new CacheStatistics(metricURICache, "MetricURI");
		cacheStatistics.register();
	}
	
	/**
	 * Returns a MetricURI for the passed string representation
	 * @param uri The URI string representation
	 * @return a MetricURI
	 */
	public static MetricURI getMetricURI(CharSequence uri) {
		if(uri==null || uri.toString().trim().isEmpty()) throw new RuntimeException("The passed URI was empty or null", new Throwable());		
		try {
			return getMetricURI(new URI(uri.toString().trim()));
		} catch (Exception ex) {
			throw new RuntimeException("Failed to create MetricURI for [" + uri + "]", ex);
		}		
	}
	
	/**
	 * Returns a MetricURI for the passed {@link URI}
	 * @param uri The URI 
	 * @return a MetricURI
	 */
	public static MetricURI getMetricURI(final URI uri) {
		if(uri==null) throw new RuntimeException("The passed URI was null", new Throwable());
		final String key = uri.toString().trim();
		try {			
			return metricURICache.get(key);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to create MetricURI for [" + uri + "]", ex);
		}
	}
	
	
	/**
	 * Creates a new MetricURI
	 * @param uri The URI string content
	 */
	private MetricURI(CharSequence uri) {
		this(toURI(uri));
	}
	
	/**
	 * Creates a new MetricURI
	 * @param uri The URI to build this MetricURI with
	 */
	private MetricURI(URI uri) {
		if(uri==null) throw new IllegalArgumentException("Passed URI was null", new Throwable());
		metricUri = uri;
		
		String path = uri.getPath();
		if(path==null || path.trim().isEmpty()) {
			throw new IllegalArgumentException("Unexpected empty or null path", new Throwable());
		}
		String[] splitPath = PATH_PARSER.split(uri.getPath());
		if(splitPath.length<3) {
			throw new IllegalArgumentException("Unexpected path length (<3) [" + uri + "]", new Throwable());
		}
		domain = splitPath[0];
		host = splitPath[1];
		agent = splitPath[2];
		if(splitPath.length>3) {
			StringBuilder b = new StringBuilder();
			for(int i = 3; i < splitPath.length; i++) {
				b.append("/").append(splitPath[i]);
			}
			namespace = b.toString();
		} else {
			namespace = null;
		}
		Map<String, String> paramMap = new HashMap<String, String>();
		if(uri.getQuery()!=null) {
			for(String pair: PARM_PARSER.split(uri.getQuery())) {
				if(pair==null || pair.trim().isEmpty() || pair.indexOf('=')==-1) continue;
				String[] paramPair = PARM_PAIR_PARSER.split(pair);				
				paramMap.put(paramPair[0].trim().toLowerCase(), paramPair[1].trim());
			}
		}
		recursive = opt(paramMap, OPT_RECURSIVE, false);
		maxDepth = opt(paramMap, OPT_MAX_DEPTH, DEFAULT_DEPTH)[0];
		metricName = paramMap.get(OPT_METRIC_NAME);
		metricType = opt(paramMap, OPT_METRIC_TYPE, DEFAULT_TYPES);
		metricStatus = opt(paramMap, OPT_METRIC_STATUS, DEFAULT_METRIC_STATUS);
		detachedCriteria = generateCriteria(this);
	}
	
	public static void main(String[] args) {
		//new MetricURI("DefaultDomain/njw810/APMRouterServer/platform=os/resource=netstat?recursive=false&status=0|1");
		MetricURI m = new MetricURI("DefaultDomain/njw810/APMRouterServer/platform=os/resource=netstat?recursive=false&status=0,1");
		log("MetricURI:" + m);
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	/**
	 * Converts the passed string to a {@link URI}.
	 * @param uri The URI string value
	 * @return the created URI
	 */
	protected static URI toURI(CharSequence uri) {
		if(uri==null) throw new IllegalArgumentException("Passed URI was null", new Throwable());
		String urs = uri.toString().trim(); if(urs.isEmpty()) throw new IllegalArgumentException("Passed URI was empty", new Throwable());
		try {
			return new URI(urs);
		} catch (Exception ex) {
			throw new RuntimeException("Invalid URI [" + uri + "]", new Throwable());
		}
	}
	
	
	
	/**
	 * Extracts a boolean argument from the parameter map
	 * @param optMap The parameter map
	 * @param key The parameter key
	 * @param defaultValue The default value
	 * @return the extracted value or default
	 */
	public static boolean opt(Map<String, String> optMap, String key, boolean defaultValue) {
		if(optMap.containsKey(key)) {
			return Boolean.parseBoolean(optMap.get(key));
		}
		return defaultValue;
	}
	
	/**
	 * Extracts an integer array argument from the parameter map
	 * @param optMap The parameter map
	 * @param key The parameter key
	 * @param defaultValue The default value
	 * @return the extracted value or default
	 */
	public static int[] opt(Map<String, String> optMap, String key, int[] defaultValue) {
		if(optMap.containsKey(key)) {
			String[] values = COM_PARSER.split(optMap.get(key));
			int[] intValues = new int[values.length];
			for(int i = 0; i < values.length; i++) {
				intValues[i] = Integer.parseInt(values[i]);
			}
			return intValues;
		}
		return defaultValue;
	}
	
	/**
	 * Generates a hibernate detached criteria for the passed MetricURI
	 * @param metricUri The MetricURI 
	 * @return the detached criteria
	 */
	protected static DetachedCriteria generateCriteria(MetricURI metricUri) {
		DetachedCriteria criteria = DetachedCriteria.forClass(Metric.class)
				.createAlias("agent", "a")
				.createAlias("traceType", "t")
				.createAlias("a.host", "h")				
				.add(Restrictions.eq("h.domain", metricUri.domain))
				.add(Restrictions.eq("h.name", metricUri.host))
				.add(Restrictions.eq("a.name", metricUri.agent));
		if(metricUri.maxDepth>0) {
			criteria.add(Restrictions.and(
					Restrictions.like("namespace", metricUri.namespace + "%"), 
					Restrictions.le("level", metricUri.maxDepth)
			));
		} else {
			criteria.add(Restrictions.eq("namespace", metricUri.namespace));
		}
		if(metricUri.metricName!=null) {
			String mn = metricUri.metricName.replace('*', '%');
			if(mn.indexOf('%')!=-1 || mn.indexOf('?')!=-1) {
				criteria.add(Restrictions.ilike("name", mn));
			} else {
				criteria.add(Restrictions.eq("name", mn));
			}
		}
		if(metricUri.metricType!=null && metricUri.metricType.length>0) {
			Set<Short> types = new HashSet<Short>(metricUri.metricType.length);
			for(int i : metricUri.metricType) {
				types.add((short)i);
			}
			criteria.add(Restrictions.in("t.typeId", types));
		}
		if(metricUri.metricStatus!=null && metricUri.metricStatus.length>0) {
			Set<Byte> statuses = new HashSet<Byte>(metricUri.metricStatus.length);
			for(int i : metricUri.metricStatus) {
				statuses.add((byte)i);
			}
			criteria.add(Restrictions.in("state", statuses));
		}
		return criteria;
	}
	
	/**
	 * Returns the detached criteria for this MetricURI
	 * @return the detached criteria 
	 */
	public DetachedCriteria getDetachedCriteria() {
		return this.detachedCriteria;
	}
	
	/**
	 * Executes the MetricURI's detached criteria and returns a list of matching metrics
	 * @param session A hibernate session to execute with
	 * @return a list of matching metrics
	 */
	@SuppressWarnings("unchecked")
	public List<Metric> execute(Session session) {
		return detachedCriteria.getExecutableCriteria(session).list();
	}

	/**
	 * Returns the underlying URI that represents this MetricURI
	 * @return the underlying URI that represents this MetricURI
	 */
	public URI getMetricUri() {
		return metricUri;
	}

	/**
	 * Returns the metric domain 
	 * @return the metric domain
	 */
	public String getDomain() {
		return domain;
	}

	/**
	 * Returns the metric host 
	 * @return the metric host
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Returns the metric agent 
	 * @return the metric agent
	 */
	public String getAgent() {
		return agent;
	}

	/**
	 * Returns the metric namespace
	 * @return the metric namespace
	 */
	public String getNamespace() {
		return namespace;
	}


	/**
	 * Returns the maximum depth to recurse from the starting point
	 * @return the maximum depth to recurse 
	 */
	public int getMaxDepth() {
		return maxDepth;
	}

	/**
	 * Returns the metric name specified in the URI
	 * @return the metric name
	 */
	public String getMetricName() {
		return metricName;
	}

	/**
	 * Returns an array of the metric type ordinals specified in the URI
	 * @return an array of the metric type ordinals
	 */
	public int[] getMetricType() {
		return metricType;
	}

	/**
	 * Returns an array of the metric status ordinals specified in the URI
	 * @return an array of the metric status ordinals
	 */
	public int[] getMetricStatus() {
		return metricStatus;
	}
	

}
