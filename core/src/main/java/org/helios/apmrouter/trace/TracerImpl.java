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
package org.helios.apmrouter.trace;

import java.io.Serializable;

import org.helios.apmrouter.metric.ICEMetric;
import org.helios.apmrouter.metric.MetricType;
import org.helios.apmrouter.metric.catalog.ICEMetricCatalog;

import static org.helios.apmrouter.util.Methods.nvl;

/**
 * <p>Title: TracerImpl</p>
 * <p>Description: The main public tracing interface</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.trace.TracerImpl</code></p>
 */

public class TracerImpl implements ITracer {
	/** The originating host of the metrics created by this tracer */
	protected final String host;
	/** The originating agent of the metrics created by this tracer */
	protected final String agent;
	
	

	/**
	 * Creates a new TracerImpl
	 * @param host The originating host of the metrics created by this tracer
	 * @param agent The originating agent of the metrics created by this tracer
	 */
	public TracerImpl(String host, String agent) {
		this.host = nvl(host, "HostName");
		this.agent = nvl(agent, "Agent Name");
	}
	
	/**
	 * Coerces a long from an object
	 * @param value The value to coerce
	 * @return The coerced long
	 */
	protected long coerce(Object value) {
		long actual = 0;
		if(value==null) {
			actual = 0;
		} else if(value instanceof Long) {
			actual = (Long)value;
		} else if(value instanceof Number) {
			actual = ((Number)value).longValue();
		} else {
			try { actual = new Double(value.toString().trim()).longValue(); } catch (Exception e) {
				actual = 0;
			}
		}
		return actual;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#trace(java.lang.Object, java.lang.CharSequence, org.helios.apmrouter.metric.MetricType, java.lang.CharSequence[])
	 */
	@Override
	public ICEMetric trace(Object value, CharSequence name, MetricType type, CharSequence... namespace) {
		ICEMetric metric = null;
		try {
			if(type.isLong()) {
				if(type.equals(MetricType.DELTA)) {
					Long delta = ICEMetricCatalog.getInstance().getDelta(coerce(value), host, agent, name, namespace);
					if(delta==null) return null;
					metric = ICEMetric.trace(value, host, agent, name, type, namespace);
				} else {
					metric = ICEMetric.trace(coerce(value), host, agent, name, type, namespace);
				}
			} else {
				metric = ICEMetric.trace(value, host, agent, name, type, namespace);
			}
			return metric;
		} catch (Throwable t) {
			return null;
		}
	}

//	/**
//	 * {@inheritDoc}
//	 * @see org.helios.apmrouter.trace.ITracer#trace(java.lang.Number, java.lang.CharSequence, org.helios.apmrouter.metric.MetricType, java.lang.CharSequence[])
//	 */
//	@Override
//	public ICEMetric trace(Number value, CharSequence name, MetricType type, CharSequence... namespace) {
//		if(value==null) return null;
//		try {
//			if(type.equals(MetricType.DELTA)) {
//				Long delta = ICEMetricCatalog.getInstance().getDelta(value.longValue(), host, agent, name, namespace);
//				if(delta==null) return null;
//				return ICEMetric.trace(value, host, agent, name, type, namespace);
//			}
//			return ICEMetric.trace(value.longValue(), host, agent, name, type, namespace);			
//		} catch (Throwable t) {
//			return null;
//		}		
//	}

//	/**
//	 * {@inheritDoc}
//	 * @see org.helios.apmrouter.trace.ITracer#trace(long, java.lang.CharSequence, org.helios.apmrouter.metric.MetricType, java.lang.CharSequence[])
//	 */
//	@Override
//	public ICEMetric trace(long value, CharSequence name, MetricType type,CharSequence... namespace) {
//		try {
//			if(type.equals(MetricType.DELTA)) {
//				Long delta = ICEMetricCatalog.getInstance().getDelta(value, host, agent, name, namespace);
//				if(delta==null) return null;
//				return ICEMetric.trace(value, host, agent, name, type, namespace);
//			}
//			return ICEMetric.trace(value, host, agent, name, type, namespace);			
//		} catch (Throwable t) {
//			return null;
//		}		
//	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#traceLong(long, java.lang.CharSequence, java.lang.CharSequence[])
	 */
	@Override
	public ICEMetric traceLong(long value, CharSequence name, CharSequence... namespace) {
		try {				
			return ICEMetric.trace(value, host, agent, name, MetricType.LONG, namespace);			
		} catch (Throwable t) {
			return null;
		}		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#traceDelta(long, java.lang.CharSequence, java.lang.CharSequence[])
	 */
	@Override
	public ICEMetric traceDelta(long value, CharSequence name, CharSequence... namespace) {
		try {
			Long delta = ICEMetricCatalog.getInstance().getDelta(value, host, agent, name, namespace);
			if(delta==null) return null;			
			return ICEMetric.trace(delta.longValue(), host, agent, name, MetricType.DELTA, namespace);			
		} catch (Throwable t) {
			return null;
		}		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#traceString(java.lang.CharSequence, java.lang.CharSequence, java.lang.CharSequence[])
	 */
	@Override
	public ICEMetric traceString(CharSequence value, CharSequence name, CharSequence... namespace) {
		try {				
			return ICEMetric.trace(value, host, agent, name, MetricType.STRING, namespace);			
		} catch (Throwable t) {
			return null;
		}		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#traceError(java.lang.Throwable, java.lang.CharSequence, java.lang.CharSequence[])
	 */
	@Override
	public ICEMetric traceError(Throwable value, CharSequence name, CharSequence... namespace) {
		try {				
			return ICEMetric.trace(value, host, agent, name, MetricType.ERROR, namespace);			
		} catch (Throwable t) {
			return null;
		}		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#traceBlob(java.io.Serializable, java.lang.CharSequence, java.lang.CharSequence[])
	 */
	@Override
	public ICEMetric traceBlob(Serializable value, CharSequence name, CharSequence... namespace) {
		try {				
			return ICEMetric.trace(value, host, agent, name, MetricType.BLOB, namespace);			
		} catch (Throwable t) {
			return null;
		}		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#getHost()
	 */
	@Override
	public String getHost() {
		return host;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#getAgent()
	 */
	@Override
	public String getAgent() {
		return agent;
	}

}
