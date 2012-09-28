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

import static org.helios.apmrouter.util.Methods.nvl;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.helios.apmrouter.metric.catalog.ICEMetricCatalog;
import org.helios.apmrouter.metric.catalog.IDelegateMetric;
import org.helios.apmrouter.trace.TXContext;
import org.helios.apmrouter.util.StringHelper;


/**
 * <p>Title: ICEMetric</p>
 * <p>Description: The public metricId implementation. ICEMetric wraps one {@link IDelegateMetric} and one {link ICEMetricValue}. One instance is created per trace operation.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.metric.ICEMetric</code></p>
 */

public class ICEMetric implements IMetric {
	/** The value for this metricId */
	protected final ICEMetricValue value;
	/** The metricId name that this instance represents*/
	protected final IDelegateMetric metricId;
	/** The attached TXContext */
	protected TXContext txContext;
	
	/** The unmapped version of this metric */
	protected ICEMetric unmapped = null;
	
	
	
	/**
	 * Creates a new ICEMetric
	 * @param timestamp THe metric value timestamp
	 * @param value The long value of the metric
	 * @param type The type of the metric
	 * @param dmetric The metric ID
	 * @return a new ICEMetric
	 */
	public static ICEMetric newMetric(long timestamp, long value, MetricType type, IDelegateMetric dmetric) {
		return new ICEMetric(new ICEMetricValue(type, value, timestamp), dmetric);
	}
	
	/**
	 * Creates a new ICEMetric
	 * @param timestamp The metric value timestamp
	 * @param value The non-long value of the metric
	 * @param type The type of the metric
	 * @param dmetric The metric ID
	 * @return a new ICEMetric
	 */
	public static ICEMetric newMetric(long timestamp, ByteBuffer value, MetricType type, IDelegateMetric dmetric) {
		return new ICEMetric(new ICEMetricValue(type, value, timestamp), dmetric);
	}
	
	/**
	 * Attaches a TXContext
	 * @param txContext The context to attach
	 */
	public void attachTXContext(TXContext txContext) {
		this.txContext = txContext;
	}
	
	/**
	 * Checks the TXContext and attaches it if one exists
	 * @return this ICEMetric
	 */
	public ICEMetric attachTXContext() {
		if(TXContext.hasContext()) {
			attachTXContext(TXContext.rollContext());
		}
		return this;
	}
	
	
	
	/**
	 * Creates a new ICEMetric
	 * @param value The value for this metricId
	 * @param metricId The metricId identifier
	 */
	ICEMetric(ICEMetricValue value, IDelegateMetric metricId) {
		this.value = value;
		this.metricId = metricId;
	}
	
	/**
	 * Creates a new ICEMetric
	 * @param value The metric value
	 * @param host The host name
	 * @param agent The agent name 
	 * @param name The metric name
	 * @param type The metric type
	 * @param namespace An optional array of namespace entries
	 * @return an ICEMetric
	 * @throws RuntimeException Thrown if any of the initial metricId parameters are invalid
	 */
	public static ICEMetric trace(Object value, String host, String agent, CharSequence name, MetricType type, CharSequence...namespace) {
		try {
			return new ICEMetric(
				type.write(value), 
				ICEMetricCatalog.getInstance().get(host, agent, name, type, namespace)
			).attachTXContext();
		} catch (Exception e) {
			throw new RuntimeException("Failed to create ICEMetricBuilder", e);
		}
	}
	
	/**
	 * Creates a new ICEMetric
	 * @param value The metric value
	 * @param host The host name
	 * @param agent The agent name 
	 * @param name The metric name
	 * @param type The metric type
	 * @param namespace An optional array of namespace entries
	 * @return an ICEMetric
	 * @throws RuntimeException Thrown if any of the initial metricId parameters are invalid
	 */
	public static ICEMetric trace(Number value, String host, String agent, CharSequence name, MetricType type, CharSequence...namespace) {
		try {
			return new ICEMetric(
				new ICEMetricValue(type, value.longValue()),
				ICEMetricCatalog.getInstance().get(host, agent, name, type, namespace)
			).attachTXContext();
		} catch (Exception e) {
			throw new RuntimeException("Failed to create ICEMetricBuilder", e);
		}
	}
	
	/**
	 * Creates a new ICEMetric
	 * @param value The metric value
	 * @param host The host name
	 * @param agent The agent name 
	 * @param name The metric name
	 * @param type The metric type
	 * @param namespace An optional array of namespace entries
	 * @return an ICEMetric
	 * @throws RuntimeException Thrown if any of the initial metricId parameters are invalid
	 */
	public static ICEMetric trace(long value, String host, String agent, CharSequence name, MetricType type, CharSequence...namespace) {
		try {
			return new ICEMetric(
				new ICEMetricValue(type, value),
				ICEMetricCatalog.getInstance().get(host, agent, name, type, namespace)
			).attachTXContext();
		} catch (Exception e) {
			throw new RuntimeException("Failed to create ICEMetricBuilder", e);
		}
	}
	


	/**
	 * Returns the value object for this metricId
	 * @return the value
	 */
	ICEMetricValue getICEMetricValue() {
		return value;
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetric#getHost()
	 */
	@Override
	public String getHost() {
		return metricId.getHost();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetric#getAgent()
	 */
	@Override
	public String getAgent() {
		return metricId.getAgent();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetric#isFlat()
	 */
	@Override
	public boolean isFlat() {
		return metricId.isFlat();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetric#isMapped()
	 */
	@Override
	public boolean isMapped() {
		return metricId.isMapped();
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetric#getNamespace()
	 */
	@Override
	public String[] getNamespace() {
		return metricId.getNamespace();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetric#getName()
	 */
	@Override
	public String getName() {
		return metricId.getName();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetric#getTime()
	 */
	@Override
	public long getTime() {
		return value.getTime();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetric#getType()
	 */
	@Override
	public MetricType getType() {
		return value.getType();
	}

	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetric#getFQN()
	 */
	@Override
	public String getFQN() {
		return String.format(FQN_FORMAT, getHost(), getAgent(), getNamespaceF(), getName());
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetric#getNamespaceF()
	 */
	@Override
	public String getNamespaceF() {
		String[] ns = getNamespace();
		if(ns.length==0) return "";
		return StringHelper.fastConcatAndDelim(NSDELIM, ns);

	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetric#getNamespace(int)
	 */
	@Override
	public String getNamespace(int index) {
		return getNamespace()[index];
	}
	
	/**
	 * Returns the namespace element at the provided index.
	 * Throws a RuntimeException if the metric is not mapped
	 * @param index The namespace index
	 * @return a namespace element
	 */
	@Override
	public String getNamespace(CharSequence index) {
		if(metricId.isFlat()) throw new RuntimeException("Requesting named index namespace on non-mapped metric [" + getFQN() + "]", new Throwable());
		String[] ns = metricId.getNamespace();
		if(ns==null || ns.length<1) throw new RuntimeException("Requesting named index namespace on zero sized namespace in metric [" + getFQN() + "]", new Throwable());
		String key = nvl(index, "Mapped Namespace Index").toString().trim();		
		for(String s: ns) {
			int eq = s.indexOf('=');
			if(key.equals(s.substring(0, eq))) {
				return s.substring(eq+1);
			}
		}
		return null;		
	}
	
	/**
	 * Returns the namespace as a map.
	 * Throws a RuntimeException if the metric is not mapped
	 * @param tagHostAgent If true, includes the host and agent in the namespace
	 * @return a map representing the mapped namespace of this metric
	 */
	@Override
	public Map<String, String> getNamespaceMap(boolean tagHostAgent) {
		if(metricId.isFlat()) throw new RuntimeException("Requesting named index namespace on non-mapped metric [" + getFQN() + "]", new Throwable());
		String[] ns = metricId.getNamespace();
		if(ns==null || ns.length<1) return Collections.emptyMap();
		Map<String, String> map = new LinkedHashMap<String, String>(ns.length + (tagHostAgent ? 2 : 0));
		if(tagHostAgent) {
			map.put(HOST_TAG, metricId.getHost());
			map.put(AGENT_TAG, metricId.getAgent());
		}
		for(String s: ns) {
			int eq = s.indexOf('=');
			map.put(s.substring(0, eq), s.substring(eq+1));
		}		
		return map;
	}
	
	/**
	 * Returns the namespace as a map without the host and agent
	 * Throws a RuntimeException if the metric is not mapped
	 * @return a map representing the mapped namespace of this metric
	 */
	@Override
	public Map<String, String> getNamespaceMap() {
		return getNamespaceMap(false);
	}
	
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetric#getNamespaceSize()
	 */
	@Override
	public int getNamespaceSize() {
		return getNamespace().length;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetric#getDate()
	 */
	@Override
	public Date getDate() {
		return value.getDate();
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetric#getValue()
	 */
	@Override
	public Object getValue() {
		if(metricId.getType().isLong()) {
			return getLongValue();
		}
		return value.getValue();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetric#getLongValue()
	 */
	@Override
	public long getLongValue() {
		if(!metricId.getType().isLong()) {
			throw new RuntimeException("The metric [" + getFQN() + "] is not a long type: [" + metricId.getType() + "]", new Throwable());
		}
		return value.getLongValue();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetric#getToken()
	 */
	@Override
	public long getToken() {
		return metricId.getToken();
	}
	

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ICEMetric [>");
		builder.append(getType());
		builder.append("<");
		builder.append(getFQN());
		builder.append("[");
		builder.append(getDate());
		builder.append("]:");
		builder.append(getValue());
		return builder.toString();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetric#getSerSize()
	 */
	@Override
	public int getSerSize() {
		try {
			return 	1 + 							// the byteorder byte
					metricId.getSerSize() +1 +1 +	// the metric id size, 8 if tokenized, variable otherwise, +1 for the tokenization indicator byte 
					8 + 					 		// the timestamp  size (a long)
					1 +								// the type size (a byte)
					(metricId.getType().isLong() ?
						8 :							// the size of a long value
						value.getRawValue().limit()+4 // the size of the bytebuffer +4 for the size
					) + 
					(hasTXContext() ? TXContext.TXCONTEXT_SIZE : 0);  // the size of the TXContext, if attached
		} catch (Exception e) {
			throw new RuntimeException("Exception calculating size of metric [" + this + "]", e);
		}
		
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetric#getRawValue()
	 */
	@Override
	public ByteBuffer getRawValue() {
		if(metricId.getType().isLong()) throw new RuntimeException("Call to getRawValue on a long type metric", new Throwable());
		return value.getRawValue();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetric#hasTXContext()
	 */
	@Override
	public boolean hasTXContext() {
		return txContext!=null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetric#getTXContext()
	 */
	@Override
	public TXContext getTXContext() {
		return txContext;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetric#getRoutingKey()
	 */
	@Override
	public CharSequence getRoutingKey() {
		return String.format("%s-%s", getType().name(), getFQN());
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetric#getUnmapped()
	 */
	public IMetric getUnmapped() {
		if(isFlat()) return this;
		if(unmapped==null) {
			unmapped = new ICEMetric(value, metricId.unmap());
			unmapped.txContext = txContext;
		}
		return unmapped;
	}
	
}
