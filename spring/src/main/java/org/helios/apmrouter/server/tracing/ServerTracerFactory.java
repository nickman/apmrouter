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
package org.helios.apmrouter.server.tracing;

import static org.helios.apmrouter.util.Methods.nvl;

import java.net.SocketAddress;
import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.router.PatternRouter;
import org.helios.apmrouter.server.ServerComponentBean;
import org.helios.apmrouter.trace.DirectMetricCollection;
import org.helios.apmrouter.trace.ITracer;
import org.helios.apmrouter.trace.MetricSubmitter;
import org.helios.apmrouter.trace.TracerImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;

/**
 * <p>Title: ServerTracerFactory</p>
 * <p>Description: An alternate implementation of the client side <code>TracerFactory</code> that sends traced metrics directly to the pattern router. </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.tracing.ServerTracerFactory</code></p>
 */
public class ServerTracerFactory extends ServerComponentBean implements MetricSubmitter {
	/** The pattern router the server tracers will send to */
	protected PatternRouter patternRouter = null;
	/** The default tracer */
	protected ITracer defaultTracer = null;
	/** The count of metric sends */
	protected final AtomicLong sent = new AtomicLong(0);
	/** The count of dropped metric sends */
	protected final AtomicLong dropped = new AtomicLong(0);
	/** The count of failed metric sends */
	protected final AtomicLong failed = new AtomicLong(0);
	/** A map of created tracers keyed by host/agent */
	private final Map<String, ITracer> tracers = new ConcurrentHashMap<String, ITracer>();
	
	/** The local sender URI */
	public static final URI LOCAL_SENDER_URI = makeURI("local:0");
	
	/**
	 * Returns the default tracer instance
	 * @return the default tracer instance
	 */
	public ITracer getTracer() {
		return defaultTracer;
	}
	
	
	
	private static URI makeURI(String string) {
		try {
			return new URI(string);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}		
	}



	/**
	 * Returns a tracer instance for the passed host and agent
	 * @param host The host name to create a tracer for
	 * @param agent The agent name to create a tracer for
	 * @return a tracer instance
	 */
	public ITracer getTracer(String host, String agent) {
		String key = nvl(host, "Host Name").trim() + ":" + nvl(agent, "Agent Name").trim();
		ITracer tracer = tracers.get(key);
		if(tracer==null) {
			synchronized(tracers) {
				tracer = tracers.get(key);
				if(tracer==null) {
					tracer = new TracerImpl(host.trim(), agent.trim(), this);
					tracers.put(key, tracer);
				}
			}
		}
		return tracer;
	}

	
	/**
	 * Sets the pattern router the server tracers will send to
	 * @param patternRouter the patternRouter to set
	 */
	@Autowired
	public void setPatternRouter(PatternRouter patternRouter) {
		this.patternRouter = patternRouter;
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sender.ISender#getSentMetrics()
	 */
	@Override
	@ManagedMetric(category="ServerSender", metricType=MetricType.COUNTER, description="the number of metrics sent")
	public long getSentMetrics() {
		return sent.get();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sender.ISender#getDroppedMetrics()
	 */
	@Override
	@ManagedMetric(category="ServerSender", metricType=MetricType.COUNTER, description="the number of metrics dropped")
	public long getDroppedMetrics() {
		return dropped.get();
	}

	@ManagedMetric(category="ServerSender", metricType=MetricType.COUNTER, description="the number of metrics failed")
	public long getFailedMetrics() {
		return failed.get();
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.MetricSubmitter#submitDirect(org.helios.apmrouter.metric.IMetric, long)
	 */
	@Override
	public void submitDirect(IMetric metric, long timeout) throws TimeoutException {
		try {
			patternRouter.route(metric);
			sent.incrementAndGet();
		} catch (Exception e) {
			failed.incrementAndGet();
		}				
	}



	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.MetricSubmitter#submit(java.util.Collection)
	 */
	@Override
	public void submit(Collection<IMetric> metrics) {
		try {
			patternRouter.route(metrics);
			sent.incrementAndGet();
		} catch (Exception e) {
			failed.incrementAndGet();
		}				
		
	}



	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.MetricSubmitter#submit(org.helios.apmrouter.metric.IMetric[])
	 */
	@Override
	public void submit(IMetric... metrics) {
		try {
			patternRouter.route(metrics);
			sent.incrementAndGet();
		} catch (Exception e) {
			failed.incrementAndGet();
		}						
	}



	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.MetricSubmitter#resetStats()
	 */
	@Override
	public void resetStats() {
		// TODO Auto-generated method stub
		
	}



	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.MetricSubmitter#getQueuedMetrics()
	 */
	@Override
	public long getQueuedMetrics() {
		// TODO Auto-generated method stub
		return 0;
	}
}
