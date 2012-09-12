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

import static org.helios.apmrouter.util.Methods.nvl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.helios.apmrouter.metric.MetricType;

/**
 * <p>Title: AbstractMetricCatalog</p>
 * <p>Description: The base abstract metric catalog support class</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.metric.catalog.AbstractMetricCatalog</code></p>
 * @param <K> The key type used in the name cache
 * @param <V> The value type used in the name cache
 */

public abstract class AbstractMetricCatalog<K, V> implements IMetricCatalog {
	/** The name cache of metrics */
	protected final Map<K, V> namecache;
	/** The delta cache for metrics */
	protected final Map<K, Long> deltacache;
	/** The metric token map */
	protected final Map<Long, IDelegateMetric> tokencache;
	/** The token ID factory */
	protected final AtomicLong tokenSerial = new AtomicLong(0);
	
	/**
	 * Creates a new AbstractMetricCatalog
	 * TODO: Add overrides for the map sizing in sys props
	 */
	protected AbstractMetricCatalog() {
		namecache = new ConcurrentHashMap<K, V>(1024, 0.5f, 16);
		deltacache = new ConcurrentHashMap<K, Long>(128, 0.5f, 16);
		tokencache = new ConcurrentHashMap<Long, IDelegateMetric>(1024, 0.5f, 16);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.catalog.IMetricCatalog#get(long)
	 */
	@Override
	public IDelegateMetric get(long metricIdToken) {
		return tokencache.get(metricIdToken);
	}

	/**
	 * Sets the serialization token for the passed metric identifier using a contrived token.
	 * Intended only for testing or server side.
	 * @param host The host name
	 * @param agent The agent name
	 * @param name The metric name
	 * @param type The metric type
	 * @param namespace The namespace segments
	 * @return  the assigned token
	 */
	public long setToken(String host, String agent, CharSequence name, MetricType type, CharSequence... namespace) {
		return setToken(tokenSerial.incrementAndGet(), host,agent, name, type, namespace);
	}
	
	/**
	 * Sets the serialization token for the passed metric identifier
	 * @param token The token to set on the metric 
	 * @param host The host name
	 * @param agent The agent name
	 * @param name The metric name
	 * @param type The metric type
	 * @param namespace The namespace segments
	 * @return  the assigned token
	 */
	public long setToken(long token, String host, String agent, CharSequence name, MetricType type, CharSequence... namespace) {
		IDelegateMetric metric = get(host, agent, name, type, namespace);
		metric.setToken(token);
		tokencache.put(token, metric);
		return token;
	}
	
	
	
	/**
	 * Returns the delta for the passed value and metricId key
	 * @param value The long value to get the delta for
	 * @param host The host name
 	 * @param agent The agent name
	 * @param name The metric name
	 * @param namespace The namespace
	 * @return the delta value or null if this was the first call for the metric
	 */
	public Long getDelta(long value, String host, String agent, CharSequence name, CharSequence... namespace) {
		return _getDelta(value, host, agent, name, namespace);
	}
	
	protected Long _getDelta(long value, String host, String agent, CharSequence name, CharSequence... namespace) {
		K key = createKey(getFQN(host, agent, name, namespace));
		Long state = deltacache.put(key, value);
		if(state==null || value < state) return null;		
		return value-state;
	}
	
	
	/**
	 * {@inheritDoc} 
	 * @see org.helios.apmrouter.metric.catalog.IMetricCatalog#size()
	 */
	public int size() {
		return namecache.size();
	}
	
	/**
	 * Creates a key from the FQN
	 * @param fqn The fully qualified metric name
	 * @return the key that represents the fully qualified metric name
	 */
	protected abstract K createKey(String fqn);
	
	/**
	 * Returns the IDelegateMetric for the passed key
	 * @param key The metric catalog key
	 * @return the keyed IDelegateMetric or null if one was not found
	 */
	protected abstract IDelegateMetric get(K key);
	
	/**
	 * Creates a new IDelegateMetric and writes the cache entry
	 * @param key The key the metric will be cached by
	 * @param host The host name
	 * @param agent The agent name
	 * @param name The metric name
	 * @param type The metric type
	 * @param namespace The namespace segments
	 * @return the created metric
	 */
	protected abstract IDelegateMetric create(K key, String host, String agent, CharSequence name, MetricType type, CharSequence... namespace);

	
	/**
	 * Returns a catalog key for the passed metric id
	 * @param host The host name
 	 * @param agent The agent name
	 * @param name The metric name
	 * @param namespace The namespace
	 * @return the catalog key
	 */
	protected K createKey(String host, String agent, CharSequence name, CharSequence... namespace) {
		return createKey(getFQN(host, agent, name, namespace)); 
	}
	
	/**
	 * Builds the FQN
	 * @param host The host name
 	 * @param agent The agent name
	 * @param name The metric name
	 * @param namespace The namespace
	 * @return The fully qualified metric name
	 */
	public String getFQN(String host, String agent, CharSequence name, CharSequence... namespace) {
		StringBuilder sb = new StringBuilder(nvl(host, "Host Name")).append(NSDELIM).append(nvl(agent, "Agent Name"));		
		if(namespace!=null && namespace.length>0) {
			List<String> ns = new ArrayList<String>(namespace.length);
			for(int i = 0; i < namespace.length; i++) {
				if(namespace[i]==null) continue;
				String s = namespace[i].toString().trim();
				if(s.isEmpty()) continue;
				ns.add(s);
			}
			if(!ns.isEmpty()) {
				for(String s: ns) {
					sb.append(NSDELIM).append(s);
				}
			}
		}
		sb.append(NADELIM).append(nvl(name, "Metric Name"));
		return sb.toString();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.catalog.IMetricCatalog#get(java.lang.String, java.lang.String, java.lang.CharSequence, org.helios.apmrouter.metric.MetricType, java.lang.CharSequence[])
	 */
	@Override
	public IDelegateMetric get(String host, String agent, CharSequence name, MetricType type, CharSequence... namespace) {
		String fqn = getFQN(host, agent, name, namespace);
		K key = createKey(fqn);
		IDelegateMetric idm = get(key);
		if(idm==null) {
			synchronized(namecache) {
				idm = get(key);
				if(idm==null) {
					idm = create(key, host, agent, name, type, namespace);					
				}
			}
		}		
		assert fqn.equals(idm.getFQN());
		return idm;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.catalog.IMetricCatalog#get(java.lang.String, java.lang.String, java.lang.CharSequence, int, java.lang.CharSequence[])
	 */
	@Override
	public IDelegateMetric get(String host, String agent, CharSequence name, int type, CharSequence... namespace) {
		return get(host, agent, name, MetricType.valueOf(type), namespace);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.catalog.IMetricCatalog#get(java.lang.String, java.lang.String, java.lang.CharSequence, java.lang.String, java.lang.CharSequence[])
	 */
	@Override
	public IDelegateMetric get(String host, String agent, CharSequence name, String type, CharSequence... namespace) {
		return get(host, agent, name, MetricType.valueOfName(type), namespace);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.catalog.IMetricCatalog#dispose()
	 * <p><b>DO NOT CALL THIS METHOD UNLESS YOU KNOW WHAT YOU'RE DOING.</b>
	 */
	@Override
	public void dispose() {
		namecache.clear();
		deltacache.clear();
		tokencache.clear();
	}
	
	/**
	 * Returns a delegate metric for the passed FQN and type
	 * @param fqn The metric FQN
	 * @param type The metric type
	 * @return the delegate metric
	 */
	public IDelegateMetric build(String fqn, MetricType type) {
		return ICEMetricCatalog.build(fqn, type, this);
	}
	
	/**
	 * Calculates a low collision hash code for the passed string
	 * @param s The string to calculate the hash code for
	 * @return the long hashcode
	 */
	public static long longHashCode(String s) {
		long h = 0;
        int len = s.length();
    	int off = 0;
    	int hashPrime = s.hashCode();
    	char val[] = s.toCharArray();
        for (int i = 0; i < len; i++) {
            h = (31*h + val[off++] + (hashPrime*h));
        }
        return h;
	}
	
	

}
