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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.helios.apmrouter.catalog.domain.Metric;
import org.helios.apmrouter.metric.MetricType;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

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
	
	static {
		MetricType[] longTypes = MetricType.getLongMetricTypes();
		int[] longCodes = new int[longTypes.length];
		for(int i = 0; i < longTypes.length; i++) {
			longCodes[i] = longTypes[i].ordinal();
		}
		DEFAULT_TYPES = longCodes;
	}
	
	/**
	 * Creates a new MetricURI
	 * @param uri The URI string content
	 */
	public MetricURI(CharSequence uri) {
		this(toURI(uri));
	}
	
	/**
	 * Creates a new MetricURI
	 * @param uri The URI to build this MetricURI with
	 */
	public MetricURI(URI uri) {
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
	
	
	public Criteria toCriteriaQuery(Session session) {
		Criteria criteria = session.createCriteria(Metric.class)
				.createAlias("agent", "a")
				.createAlias("traceType", "t")
				.createAlias("a.host", "h")				
				.add(Restrictions.eq("h.domain", domain))
				.add(Restrictions.eq("h.name", host))
				.add(Restrictions.eq("a.name", agent));
		if(maxDepth>0) {
			criteria.add(Restrictions.and(
					Restrictions.like("namespace", namespace + "%"), 
					Restrictions.le("level", maxDepth)
			));
		} else {
			criteria.add(Restrictions.eq("namespace", namespace));
		}
		if(metricName!=null) {
			String mn = metricName.replace('*', '%');
			if(mn.indexOf('%')!=-1 || mn.indexOf('?')!=-1) {
				criteria.add(Restrictions.ilike("name", mn));
			} else {
				criteria.add(Restrictions.eq("name", mn));
			}
		}
		if(metricType!=null && metricType.length>0) {
			Set<Short> types = new HashSet<Short>(metricType.length);
			for(int i : metricType) {
				types.add((short)i);
			}
			criteria.add(Restrictions.in("t.typeId", types));
		}
		if(metricStatus!=null && metricStatus.length>0) {
			Set<Byte> statuses = new HashSet<Byte>(metricStatus.length);
			for(int i : metricStatus) {
				statuses.add((byte)i);
			}
			criteria.add(Restrictions.in("state", statuses));
		}
				
			
		return criteria;
	}

	public URI getMetricUri() {
		return metricUri;
	}

	public String getDomain() {
		return domain;
	}

	public String getHost() {
		return host;
	}

	public String getAgent() {
		return agent;
	}

	public String getNamespace() {
		return namespace;
	}

	public boolean isRecursive() {
		return recursive;
	}

	public int getMaxDepth() {
		return maxDepth;
	}

	public String getMetricName() {
		return metricName;
	}

	public int[] getMetricType() {
		return metricType;
	}

	public int[] getMetricStatus() {
		return metricStatus;
	}

}
