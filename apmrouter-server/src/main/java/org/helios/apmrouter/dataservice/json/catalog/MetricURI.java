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
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.helios.apmrouter.cache.CacheStatistics;
import org.helios.apmrouter.catalog.EntryStatus;
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
 * 	<li><b>maxdepth:</b></li>
 * 	<li><b>metrictype:</b></li>
 * 	<li><b>status:</b></li>
 * </ol></p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.dataservice.json.catalog.MetricURI</code></p>
 */

public class MetricURI implements MetricURIMBean {
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
	/** The metric data type mask */
	protected final int metricTypeMask;	
	/** The metric statuses */
	protected final byte[] metricStatus;
	/** The metric status mask */
	protected final byte metricStatusMask;

	/** A mask representing the metric type, status and subscription type  (mask of int/byte/byte) */ 
	protected final long metricTypeStatusSubTypeMask;
	
	
	/** The hibernate detached criteria */
	protected final DetachedCriteria detachedCriteria;

	/** The metric Id retrieveal sql */
	protected final String metricIdSql;
	/** The bit mask of the metric subscription types requested */
	protected final byte subscriptionType;
	


	/** The URI parameter parser */
	public static final Pattern PARM_PARSER = Pattern.compile("&");
	/** The URI parameter pair parser */
	public static final Pattern PARM_PAIR_PARSER = Pattern.compile("=");	
	/** The URI path parser */
	public static final Pattern PATH_PARSER = Pattern.compile("/");
	/** The comma parser */
	public static final Pattern COM_PARSER = Pattern.compile(",");
	/** The name bit parser*/
	public static final Pattern NAME_BIT_PARSER = Pattern.compile(":");
	
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
	
	/** The default max depth */
	private static final int[] DEFAULT_DEPTH = new int[]{-1};
	/** The default metric types */
	private static final int[] DEFAULT_TYPES;
	/** The default metric types */
	private static final byte[] DEFAULT_SUB_TYPES = MetricURISubscriptionType.getAllSubTypeOrdinals();
	
	/** The default metric statuses */
	private static final byte[] DEFAULT_METRIC_STATUS = new byte[]{0,1};
	
	/** The escaped single character wild card */
	public static final String ONE_CHAR_WILDCARD = "\\_";
	/** The escaped multiple character wild card */
	public static final String MULTI_CHAR_WILDCARD = "\\%";
	
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
	
	/** Static class logger */
	protected static final Logger LOG = Logger.getLogger(MetricURI.class);
	
	
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
	 * Computes the mask of all the passed ints
	 * @param ints the ints to compute the masks for
	 * @return the mask
	 */
	protected int mask(int[] ints) {
		if(ints==null || ints.length==0) return 0;
		int start = 0;
		for(int entry: ints) {
			start = (start | entry);
		}
		return start;
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
		String cleanedPath = null;
		String uriPath = uri.getPath().trim();
		int cIndex = uriPath.lastIndexOf(':');
		if(cIndex==-1) {
			cleanedPath = uriPath;
			metricName = null;
		} else {
			String[] bitsOfPath = NAME_BIT_PARSER.split(uriPath);
			if(bitsOfPath.length>2) {
				cleanedPath = uriPath.substring(0, cIndex);
				metricName = uriPath.substring(cIndex).replace('*', '%');
			} else {
				metricName = bitsOfPath[1].replace('*', '%');
				cleanedPath = bitsOfPath[0];
			}
		}
		String[] splitPath = PATH_PARSER.split(cleanedPath);
		
		if(splitPath.length<3) {
			throw new IllegalArgumentException("Unexpected path length (<3) [" + uri + "]", new Throwable());
		}
		domain = splitPath[0].replace('*', '%');
		host = splitPath[1].replace('*', '%');
		agent = splitPath[2].replace('*', '%');
		if(splitPath.length>3) {
			StringBuilder b = new StringBuilder();
			for(int i = 3; i < splitPath.length; i++) {
				b.append("/").append(splitPath[i]);
			}
			namespace = b.toString().replace('*', '%');
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
		metricType = getTypes(paramMap);
		metricTypeMask = MetricType.getMaskFor(metricType);
		metricStatus = opt(paramMap, OPT_METRIC_STATUS, DEFAULT_METRIC_STATUS);
		metricStatusMask = EntryStatus.getMaskFor(metricStatus); 
		detachedCriteria = generateCriteria(this);
		metricIdSql = generateCriteriaSQL(this);
		subscriptionType = getSubTypeMask(paramMap);
		metricTypeStatusSubTypeMask = mask(metricTypeMask, metricStatusMask, subscriptionType);
	}
	
	private static final byte ZERO_BYTE = 0;
	
	/**
	 * Computes a long mask that represents the enabled metric types, metric statuses and subscription types.
	 * @param metricTypeMask The enabled metric type mask
	 * @param metricStatusMask The enabled metric status mask
	 * @param subTypeMask The enabled subscription type mask
	 * @return a mask that represents the enabled metric types, metric statuses and subscription types.
	 */
	public static long mask(int metricTypeMask, byte metricStatusMask, byte subTypeMask) {
		ByteBuffer buff = ByteBuffer.allocate(8).put(ZERO_BYTE).put(ZERO_BYTE)
				.putInt(metricTypeMask)
				.put(metricStatusMask)
				.put(subTypeMask);
		buff.flip();
		return buff.getLong();
	}
	
	/**
	 * Detemines if this MetricURI is a match for the passed mask representing enabled metric types, metric statuses and subscription types
	 * @param metricTypeStatusSubTypeMask a long mask representing enabled metric types, metric statuses and subscription types
	 * @return true for a match, false otherwise
	 */
	public boolean isEnabledFor(long metricTypeStatusSubTypeMask) {
		return metricTypeStatusSubTypeMask==(this.metricTypeStatusSubTypeMask | metricTypeStatusSubTypeMask);
	}
	
	
	
	/**
	 * Detemines if this MetricURI is a match for the passed metric type, metric status and subscription type
	 * @param metricType The metric type ordinal
	 * @param metricStatus The metric status ordinal
	 * @param subscriptionType The subscription type ordinal
	 * @return true for a match, false otherwise
	 */
	public boolean isEnabledFor(int metricType, byte metricStatus, byte subscriptionType) {
		return metricTypeStatusSubTypeMask==(this.metricTypeStatusSubTypeMask | mask(metricType, metricStatus, subscriptionType));
	}
	
	/**
	 * Returns the enabled subscription type bit mask 
	 * @return the enabled subscription type bit mask
	 */
	@Override
	public byte getSubscriptionType() {
		return subscriptionType;
	}
	
	/**
	 * Returns a pipe delimited string of the enabled subscription type names
	 * @return a pipe delimited string of the enabled subscription type names
	 */
	@Override
	public String getSubscriptionTypeNames() {
		return MetricURISubscriptionType.getNamesFor(subscriptionType);
	}
	
	/**
	 * Determines if this MetricURI is enabled for the passed subscription type 
	 * @param subType the subscription type to test for
	 * @return true if this MetricURI is enabled for the passed subscription type , false otherwise
	 */
	public boolean isEnabledFor(MetricURISubscriptionType subType) {
		return subType.isEnabled(subscriptionType);
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MetricURI [");
		builder.append("\n\tdomain:");
		builder.append(domain);
		builder.append("\n\thost:");
		builder.append(host);
		builder.append("\n\tagent:");
		builder.append(agent);
		builder.append("\n\tnamespace:");
		builder.append(namespace);
		builder.append("\n\tmaxDepth:");
		builder.append(maxDepth);
		builder.append("\n\tmetricName:");
		builder.append(metricName);
		builder.append("\n\tmetricTypes:");
		builder.append(Arrays.toString(metricType));
		builder.append("\n\tmetricStatus:");
		builder.append(Arrays.toString(metricStatus));
		builder.append("\n]");
		return builder.toString();
	}

	public static void main(String[] args) {
		//new MetricURI("DefaultDomain/njw810/APMRouterServer/platform=os/resource=netstat?recursive=false&status=0|1");
		MetricURI m = new MetricURI("DefaultDomain/njw810/APMRouterServer/platform=os/resource=netstat:FooBar?recursive=false&status=0,1");
		try {
			URI uri = new URI("DefaultDomain/njw810/APMRouterServer/platform=os/resource=netstat:FooBar?recursive=false&status=0,1");
			log("URI Path" + uri.getPath());
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
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
	 * Extracts a byte array argument from the parameter map
	 * @param optMap The parameter map
	 * @param key The parameter key
	 * @param defaultValue The default value
	 * @return the extracted value or default
	 */
	public static byte[] opt(Map<String, String> optMap, String key, byte[] defaultValue) {
		if(optMap.containsKey(key)) {
			String[] values = COM_PARSER.split(optMap.get(key));
			byte[] byteValues = new byte[values.length];
			for(int i = 0; i < values.length; i++) {
				byteValues[i] = Byte.parseByte(values[i]);
			}
			return byteValues;
		}
		return defaultValue;
	}
		
	
	/**
	 * Extracts the type specifications from the option map
	 * @param optMap The option map
	 * @return an array of {@link MetricType} ordinals
	 */
	public static int[] getTypes(Map<String, String> optMap) {
		if(optMap.containsKey(OPT_METRIC_TYPE)) {
			String optValue = optMap.get(OPT_METRIC_TYPE);
			if(optValue.trim().isEmpty()) return DEFAULT_TYPES;
			String[] values = COM_PARSER.split(optValue);
			
			int[] intValues = new int[values.length];			
			for(int i = 0; i < values.length; i++) {
				intValues[i] = MetricType.valueOfName(values[i]).ordinal();
			}
			return intValues;
		}
		return DEFAULT_TYPES;		
	}
	
	/**
	 * Extracts the subscription type bit mask from the option map
	 * @param optMap The option map
	 * @return a a bit mask representation of the enabled {@link MetricURISubscriptionType}s
	 */
	public static byte getSubTypeMask(Map<String, String> optMap) {
		return MetricURISubscriptionType.enable(getSubTypes(optMap));
	}
	
	/**
	 * Extracts the subscription type specifications from the option map
	 * @param optMap The option map
	 * @return an array of {@link MetricURISubscriptionType} ordinals
	 */
	public static byte[] getSubTypes(Map<String, String> optMap) {
		if(optMap.containsKey(OPT_SUB_TYPE)) {
			String optValue = optMap.get(OPT_SUB_TYPE);
			if(optValue.trim().isEmpty()) return DEFAULT_SUB_TYPES;
			String[] values = COM_PARSER.split(optValue);			
			byte[] byteValues = new byte[values.length];			
			for(int i = 0; i < values.length; i++) {
				byteValues[i] = MetricURISubscriptionType.valueOfName(values[i]).getCode();
			}
			return byteValues;
		}
		return DEFAULT_SUB_TYPES;		
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
				.createAlias("a.host", "h");
		
				if(!"%".equals(metricUri.domain)) {
					criteria.add(metricUri.domain.indexOf('%')==-1 ? 
							Restrictions.eq("h.domain", metricUri.domain) : 
								Restrictions.like("h.domain", metricUri.domain));

				}
				if(!"%".equals(metricUri.host)) {
					criteria.add(metricUri.host.indexOf('%')==-1 ? 
							Restrictions.eq("h.name", metricUri.host) : 
								Restrictions.like("h.name", metricUri.host));
				}
				if(!"%".equals(metricUri.agent)) {
					criteria.add(metricUri.agent.indexOf('%')==-1 ? 
							Restrictions.eq("a.name", metricUri.agent) : 
								Restrictions.like("a.name", metricUri.agent));
				}
				
				
				
//				.add(Restrictions.eq("h.name", metricUri.host))
//				.add(Restrictions.eq("a.name", metricUri.agent));
		if(metricUri.maxDepth>0) {
			criteria.add(Restrictions.and(
					Restrictions.like("namespace", metricUri.namespace + "%"), 
					Restrictions.le("level", metricUri.maxDepth)
			));
		} else {
			if(metricUri.namespace.indexOf('%')!=-1) {
				criteria.add(Restrictions.like("namespace", metricUri.namespace));
			} else {
				criteria.add(Restrictions.eq("namespace", metricUri.namespace));
			}			
		}
		if(metricUri.metricName!=null) {
			
			if(metricUri.metricName.indexOf('%')!=-1) {
				criteria.add(Restrictions.like("name", metricUri.metricName));
			} else {
				criteria.add(Restrictions.eq("name", metricUri.metricName));
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
	 * Generates a SQL statement for retrieving the metric Ids that are in the result set for the passed MetricURI
	 * @param metricUri The MetricURI 
	 * @return the SQL statement
	 * FIXME: Replace literals with bind variables
	 */
	protected static String generateCriteriaSQL(MetricURI metricUri) {
		StringBuilder sql = new StringBuilder("SELECT METRIC_ID FROM METRIC M, AGENT A, HOST H WHERE M.AGENT_ID = A.AGENT_ID AND A.HOST_ID = H.HOST_ID ");
		
				if(!"%".equals(metricUri.domain)) {
					sql.append(metricUri.domain.indexOf('%')==-1 ?
							"AND H.DOMAIN = '" + metricUri.domain + "' "
							:
							"AND H.DOMAIN LIKE '" + metricUri.domain + "' ");
				}

				if(!"%".equals(metricUri.host)) {
					sql.append(metricUri.host.indexOf('%')==-1 ?
							"AND H.NAME = '" + metricUri.host + "' "
							:
							"AND H.NAME LIKE '" + metricUri.host + "' ");
				}

				if(!"%".equals(metricUri.agent)) {
					sql.append(metricUri.agent.indexOf('%')==-1 ?
							"AND A.NAME = '" + metricUri.agent + "' "
							:
							"AND A.NAME LIKE '" + metricUri.agent + "' ");
				}
				
		if(metricUri.maxDepth>0) {
			sql.append(" AND (M.NAMESPACE LIKE '").append(metricUri.namespace).append("%").append("'")
				.append(" AND M.LEVEL <= ").append(metricUri.maxDepth).append(") ");
		} else {
			if(metricUri.namespace.indexOf('%')!=-1) {
				sql.append(" AND M.NAMESPACE LIKE '").append(metricUri.namespace).append("' ");
			} else {
				sql.append(" AND M.NAMESPACE = '").append(metricUri.namespace).append("' ");
			}			
		}
		
		if(metricUri.metricName!=null) {			
			if(metricUri.metricName.indexOf('%')!=-1) {
				sql.append(" AND M.NAME LIKE '").append(metricUri.metricName).append("' ");
			} else {
				sql.append(" AND M.NAME = '").append(metricUri.metricName).append("' ");
			}
		}
		if(metricUri.metricType!=null && metricUri.metricType.length>0) {
			Set<Short> types = new HashSet<Short>(metricUri.metricType.length);
			for(int i : metricUri.metricType) {
				types.add((short)i);
			}
			sql.append(" AND M.TYPE_ID IN (");
			for(short typeId: types) {
				sql.append(typeId).append(",");
			}
			sql.deleteCharAt(sql.length()-1);
			sql.append(") ");			
		}
		if(metricUri.metricStatus!=null && metricUri.metricStatus.length>0) {
			Set<Byte> statuses = new HashSet<Byte>(metricUri.metricStatus.length);
			for(int i : metricUri.metricStatus) {
				statuses.add((byte)i);
			}
			sql.append(" AND M.STATE IN (");
			for(byte status: statuses) {
				sql.append(status).append(",");
			}
			sql.deleteCharAt(sql.length()-1);
			sql.append(") ");			
		}
		LOG.info("Generated SQL for Metric IDs [\n\t" + sql + "\n\t]");
		return sql.toString();
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
	 * @param timeout Set a timeout for the underlying JDBC query in seconds
	 * @return a list of matching metrics
	 */
	@SuppressWarnings("unchecked")
	public List<Metric> execute(Session session, int timeout) {
		return detachedCriteria.getExecutableCriteria(session).setTimeout(timeout).list();
	}
	
	/**
	 * Executes the MetricURI's detached criteria and returns a list of matching metrics with no timeout
	 * @param session A hibernate session to execute with
	 * @return a list of matching metrics
	 */	
	@SuppressWarnings("unchecked")
	public List<Metric> execute(Session session) {
		return detachedCriteria.getExecutableCriteria(session).list();
	}	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.dataservice.json.catalog.MetricURIMBean#getMetricUri()
	 */
	@Override
	public String getMetricUri() {
		return metricUri.toString();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.dataservice.json.catalog.MetricURIMBean#getDomain()
	 */
	@Override
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
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.dataservice.json.catalog.MetricURIMBean#getAgent()
	 */
	@Override
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
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.dataservice.json.catalog.MetricURIMBean#getMaxDepth()
	 */
	@Override
	public int getMaxDepth() {
		return maxDepth;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.dataservice.json.catalog.MetricURIMBean#getMetricName()
	 */
	@Override
	public String getMetricName() {
		return metricName;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.dataservice.json.catalog.MetricURIMBean#getMetricType()
	 */
	@Override
	public int[] getMetricType() {
		return metricType;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.dataservice.json.catalog.MetricURIMBean#getMetricTypeNames()
	 */
	@Override
	public String[] getMetricTypeNames() {
		String[] arr = new String[metricType.length];
		for(int i = 0; i < metricType.length; i++) {
			arr[i] = MetricType.valueOf(metricType[i]).name();
		}
		return arr;
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.dataservice.json.catalog.MetricURIMBean#getMetricStatus()
	 */
	@Override
	public byte[] getMetricStatus() {
		return metricStatus;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.dataservice.json.catalog.MetricURIMBean#getMetricStatusNames()
	 */
	@Override
	public String[] getMetricStatusNames() {
		String[] arr = new String[metricStatus.length];
		for(int i = 0; i < metricStatus.length; i++) {
			arr[i] = EntryStatus.decode(metricStatus[i]);
		}
		return arr;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.dataservice.json.catalog.MetricURIMBean#getMetricIdSql()
	 */
	@Override
	public String getMetricIdSql() {
		return metricIdSql;
	}

	/**
	 * Returns the mask of the enabled metric types 
	 * @return the metricTypeMask
	 */
	public int getMetricTypeMask() {
		return metricTypeMask;
	}

	/**
	 * Returns the mask of the enabled metric statuses
	 * @return the metricStatusMask
	 */
	public int getMetricStatusMask() {
		return metricStatusMask;
	}

	/**
	 * Returns the combined bit mask of the bit masks for metric type, metric status and subscription type.
	 * @return the combined bit mask of the bit masks for metric type, metric status and subscription type.
	 */
	public long getMetricTypeStatusSubTypeMask() {
		return metricTypeStatusSubTypeMask;
	}
	

}
