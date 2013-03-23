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

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import org.helios.apmrouter.catalog.MetricCatalogService;
import org.helios.apmrouter.destination.chronicletimeseries.ChronicleTSManager;
import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.metric.AgentIdentity;
import org.helios.apmrouter.metric.ICEMetric;
import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.metric.catalog.ICEMetricCatalog;
import org.helios.apmrouter.metric.catalog.IDelegateMetric;
import org.helios.apmrouter.metric.catalog.IMetricCatalog;
import org.helios.apmrouter.server.ServerComponentBean;
import org.helios.apmrouter.server.net.listener.netty.handlers.AgentMetricHandler;
import org.helios.apmrouter.server.services.session.SharedChannelGroup;
import org.helios.apmrouter.server.tracing.RefreshingExpiryTest.Expirer;
import org.helios.apmrouter.trace.ITracer;
import org.helios.apmrouter.trace.ITracerFactory;
import org.helios.apmrouter.trace.MetricSubmitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;

/**
 * <p>Title: ServerTracerFactory</p>
 * <p>Description: An alternate implementation of the client side <code>TracerFactory</code> that sends traced metrics directly to the pattern router. </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.tracing.ServerTracerFactory</code></p>
 */
public class ServerTracerFactory extends ServerComponentBean implements MetricSubmitter, ITracerFactory {
	/** The default tracer */
	protected ITracer defaultTracer = new ServerTracerImpl(APMROUTER_HOST_NAME, APMROUTER_AGENT_NAME, this, 0);
	/** A map of created tracers keyed by host/agent */
	private final Map<String, ITracer> tracers = new ConcurrentHashMap<String, ITracer>(Collections.singletonMap(APMROUTER_HOST_NAME + ":" + APMROUTER_AGENT_NAME, defaultTracer));
	/** The metric catalog service */
	protected MetricCatalogService metricCatalogService = null;
	/** The metric catalog */
	protected IMetricCatalog metricCatalog = ICEMetricCatalog.getInstance();
	/** The delegate metricSubmitter */
	protected MetricSubmitter metricSubmitter = null;
	/** The virtual agent serial generator */
	private static final AtomicLong virtualAgentSerial = new AtomicLong(0);
	/** The virtual agent timeout period */
	protected long vaTimeout;
	/** The virtual agent expiry queue */
	protected final DelayQueue<VirtualTracer> vaExpiryQueue = new DelayQueue<VirtualTracer>(); 
	
	
	/** The local sender URI */
	public static final String LOCAL_SENDER_URI = "local:%s";
	/** The APMRouter agent name */
	public static final String APMROUTER_AGENT_NAME = "APMRouterServer";
	/** The APMRouter host name */
	public static final String APMROUTER_HOST_NAME = AgentIdentity.ID.getHostName();
	
	/** The singleton instance */
	private static volatile ServerTracerFactory instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** The virtual agent expiry thread */
	private Thread expiryThread = null;
	
	
	/**
	 * Creates a new ServerTracerFactory
	 */
	private ServerTracerFactory() {
		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStart()
	 */
	@Override
	protected void doStart() throws Exception {
		metricCatalogService.hostAgentState(true, APMROUTER_HOST_NAME, "", APMROUTER_AGENT_NAME, String.format(LOCAL_SENDER_URI, 0));
		metricSubmitter = applicationContext.getBean(AgentMetricHandler.class);
		vaTimeout = applicationContext.getBean("chronicleTs", ChronicleTSManager.class).getStaleWindowSize();
		info("Set ServerTracerFactory MetricSubmitter");
		expiryThread = new Thread("VAExpiryThread") {
			@Override
			public void run() {
				while(true) {
					try {
						VirtualTracer expired = vaExpiryQueue.poll(2000, TimeUnit.MILLISECONDS);
						if(expired==null && vaExpiryQueue.size()>0) {
							Set<VirtualTracer> drain = new HashSet<VirtualTracer>(vaExpiryQueue.size());
							Collections.addAll(drain, vaExpiryQueue.toArray(new VirtualTracer[0]));							
							vaExpiryQueue.clear();
							vaExpiryQueue.addAll(drain);							
							continue;
						}
						expired.expire();	
						try {
							JMXHelper.getHeliosMBeanServer().unregisterMBean(expired.getObjectName());
						} catch (Exception ex) {
							ex.printStackTrace(System.err);
						}
						SharedChannelGroup.getInstance().sendVirtualAgentExpiredEvent(
								metricCatalogService.hostAgentState(false, expired.getHost(), "", expired.getAgent(), String.format(LOCAL_SENDER_URI, expired.getSerial()))
						);

						incr("expiredCount");						
						tracers.remove(expired.getHost() + ":" + expired.getAgent());
					} catch (InterruptedException e) {
						if(!isStarted()) {
							break;
						}
						Thread.interrupted();
					}
				}
			}
		};
		expiryThread.setDaemon(true);
		expiryThread.start();		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStop()
	 */
	@Override
	protected void doStop() {
		metricSubmitter = null;
		super.doStop();
		if(expiryThread!=null) {
			expiryThread.interrupt();
			VirtualTracer[] toExpire = new VirtualTracer[vaExpiryQueue.size()];
			vaExpiryQueue.toArray(toExpire);
			vaExpiryQueue.clear();
			for(VirtualTracer sti: toExpire) {
				sti.expire();
			}
		}
		info("Stopping all local agents");
		for(ITracer tracer: tracers.values()) {
			metricCatalogService.hostAgentState(false, tracer.getHost(), "", tracer.getAgent(), String.format(LOCAL_SENDER_URI, 0));
		}
		info("Stopped [", tracers.size(), "] local agents");
		tracers.clear();
		
	}
	
	/**
	 * Acquires the ServerTracerFactory singleton instance
	 * @return the ServerTracerFactory singleton instance
	 */
	public static ServerTracerFactory getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new ServerTracerFactory();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Returns the default tracer instance
	 * @return the default tracer instance
	 */
	@Override
	public ITracer getTracer() {
		return defaultTracer;
	}
	
	/**
	 * Returns the number of times a virtual agent has been expired 
	 * @return the number of times a virtual agent has been expired
	 */
	@ManagedMetric(category="VirtualAgents", displayName="ExpiredVirtualAgentCount", metricType=MetricType.COUNTER, description="The number of times a virtual agent has been expired")
	public long getExpiredVirtualAgentCount() {
		return this.getMetricValue("expiredCount");
	}
	
	/**
	 * Builds an agent URI from the passed string
	 * @param string
	 * @return
	 */
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
	@Override
	public ITracer getTracer(String host, String agent) {
		String key = nvl(host, "Host Name").trim() + ":" + nvl(agent, "Agent Name").trim();
		ITracer tracer = tracers.get(key);
		if(tracer==null) {
			synchronized(tracers) {
				tracer = tracers.get(key);
				if(tracer==null) {
					final long serial = virtualAgentSerial.incrementAndGet();
					tracer = new VirtualTracer(host.trim(), agent.trim(), this, serial, vaTimeout);
					vaExpiryQueue.add((VirtualTracer)tracer);
					tracers.put(key, tracer);
					SharedChannelGroup.getInstance().sendVirtualAgentStartedEvent(
							metricCatalogService.hostAgentState(true, host.trim(), "", agent.trim(), String.format(LOCAL_SENDER_URI, serial))
					);
				}
			}
		}
		return tracer;
	}

	
	/**
	 * Sets the metric submitter the server tracers will send to
	 * @param metricSubmitter the metric submitter to use
	 */	
	public void setMetricSubmitter(MetricSubmitter metricSubmitter) {
		this.metricSubmitter = metricSubmitter;
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sender.ISender#getSentMetrics()
	 */
	@Override
	@ManagedMetric(category="ServerSender", displayName="SentMetrics", metricType=MetricType.COUNTER, description="The number of metrics sent")
	public long getSentMetrics() {
		return metricSubmitter.getSentMetrics();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sender.ISender#getDroppedMetrics()
	 */
	@Override
	@ManagedMetric(category="ServerSender", displayName="DroppedMetrics", metricType=MetricType.COUNTER, description="the number of metrics dropped")
	public long getDroppedMetrics() {
		return metricSubmitter.getDroppedMetrics();
	}

	/**
	 * Returns the number of failed metric submissions
	 * @return the number of failed metric submissions
	 */
	@ManagedMetric(category="ServerSender", displayName="FailedSubmissions", metricType=MetricType.COUNTER, description="the number of metrics failed")
	public long getFailedMetrics() {
		return getMetricValue("MetricsFailed");
	}
	
	/**
	 * Returns the number of assigned metric tokens
	 * @return the number of assigned metric tokens
	 */
	@ManagedMetric(category="ServerSender", displayName="TokensAssigned", metricType=MetricType.COUNTER, description="the number of assigned metric tokens")
	public long getTokensAssigned() {
		return getMetricValue("TokensAssigned");
	}
	
	/**
	 * Returns the number of metrics dropped because of a token lookup failure
	 * @return the number of metrics dropped because of a token lookup failure
	 */
	@ManagedMetric(category="ServerSender", displayName="TokenLookupDrop", metricType=MetricType.COUNTER, description="the number of metrics dropped because of a token lookup failure")
	public long getTokensLookupDrops() {
		return getMetricValue("TokenLookupDrop");
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.MetricSubmitter#submitDirect(org.helios.apmrouter.metric.IMetric, long)
	 */
	@Override
	public void submitDirect(IMetric metric, long timeout) throws TimeoutException {
		submit(Collections.singletonList(metric));
	}
	
	/**
	 * Processes submitted metrics through the metric catalog
	 * @param metrics A collection of metrics
	 * @return the number of metrics left in the collection after processing
	 */
	protected int processMetrics(Collection<IMetric> metrics) {
		int cnt = 0;
		for(Iterator<IMetric> iter = metrics.iterator(); iter.hasNext();) {
			IMetric metric = iter.next();
			if(metric.getToken()==-1) {
				long token = -1;
				token = metricCatalogService.isAssigned(metric.getHost(), metric.getAgent(), metric.getNamespaceF(), metric.getName());
				if(token==-1) {
					token = metricCatalogService.getID(metric.getToken(), metric.getHost(), metric.getAgent(), metric.getType().ordinal(), metric.getNamespaceF(), metric.getName());
				}
				if(token!=-1) {
					metricCatalog.setToken(metric.getHost(), metric.getAgent(), metric.getName(), metric.getType(), metric.getNamespace());
					metric.getMetricId().setToken(token);
				}

			}
			IDelegateMetric metricId = metricCatalogService.getMetricID(metric.getToken());			
			if(metricId==null) {
				iter.remove();
				debug("Token Lookup Miss [", metric.getToken(), "]");
				incr("TokenLookupDrop");
			} else {
				cnt++;
				((ICEMetric)metric).setMetricId(metricId);
			}
		}
		return cnt;
		
	}



	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.MetricSubmitter#submit(java.util.Collection)
	 */
	@Override
	public void submit(Collection<IMetric> metrics) {
		final int metricCount = processMetrics(metrics);
		if(metricCount==0) return;
		try {
			if(metricSubmitter==null) {
				incr("MetricsDropped");
				warn("ServerTracerFactory has not been started yet. Dropped [", metricCount, "] metrics");				
				return;
			}			
			metricSubmitter.submit(metrics);
			incr("MetricsSent");
		} catch (Exception e) {
			incr("MetricsFailed");
		}				
	}



	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.MetricSubmitter#submit(org.helios.apmrouter.metric.IMetric[])
	 */
	@Override
	public void submit(IMetric... metrics) {
		if(metrics!=null && metrics.length>0) {
			submit(new ArrayList<IMetric>(Arrays.asList(metrics)));
		}
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.MetricSubmitter#resetStats()
	 */
	@Override
	public void resetStats() {
		super.resetMetrics();
		
	}



	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.MetricSubmitter#getQueuedMetrics()
	 */
	@Override
	public long getQueuedMetrics() {
		return 0;
	}
	
	/**
	 * Sets the metricCatalogService
	 * @param metricCatalogService the metricCatalogService to set
	 */
	@Autowired(required=true)
	public void setMetricCatalogService(MetricCatalogService metricCatalogService) {
		this.metricCatalogService = metricCatalogService;
	}

	/**
	 * Returns the virtual agent timeout in ms. 
	 * @return the virtual agent timeout in ms.
	 */
	@ManagedAttribute(description="The VirtualAgent timeout in ms.")
	public long getVirtualAgentTimeout() {
		return vaTimeout;
	}
	
	/**
	 * Returns the number of registered live virtual agents
	 * @return the number of registered live virtual agents
	 */
	@ManagedMetric(category="VirtualAgents", displayName="LiveVirtualAgentCount", metricType=MetricType.GAUGE, description="The number of live virtual agents")
	public int getVirtualAgentCount() {
		return vaExpiryQueue.size();
	}
	

	/**
	 * Sets the virtual agent timeout in ms.
	 * @param vaTimeout the virtual agent timeout in ms.
	 */
	public void setVirtualAgentTimeout(long vaTimeout) {
		this.vaTimeout = vaTimeout;
	}	
}
