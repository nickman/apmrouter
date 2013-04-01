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
package org.helios.apmrouter.server.tracing.virtual;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.helios.apmrouter.catalog.MetricCatalogService;
import org.helios.apmrouter.collections.delay.DelayChangeReceiver;
import org.helios.apmrouter.collections.delay.DynamicDelayQueue;
import org.helios.apmrouter.server.ServerComponentBean;
import org.helios.apmrouter.server.tracing.ServerTracerFactory;
import org.helios.apmrouter.trace.MetricSubmitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;

/**
 * <p>Title: VirtualAgentManager</p>
 * <p>Description: Managing container for {@link VirtualAgent}s.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.tracing.virtual.VirtualAgentManager</code></p>
 */
public class VirtualAgentManager extends ServerComponentBean implements DelayChangeReceiver<VirtualAgent>, Runnable {
	/** Invalidation scheduler */
	protected static final ScheduledExecutorService invalidationScheduler = Executors.newScheduledThreadPool(1, new ThreadFactory(){
		private final AtomicInteger serial = new AtomicInteger();
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "VirtualAgentInvalidatorThread#" + serial.incrementAndGet());
			t.setDaemon(true);
			return null;
		}
	});
	
	/** A dynamic queue of decaying virtual agents */
	protected final DynamicDelayQueue<VirtualAgent> expiryQueue = new DynamicDelayQueue<VirtualAgent>();
	
	/** A map of virtual agents keyed by host:agent */
	protected final Map<String, VirtualAgent> virtualAgents = new ConcurrentHashMap<String, VirtualAgent>(); 
	/** A reference to the metric catalog service */
	protected MetricCatalogService metricCatalogService = null;
	/** The delegate metricSubmitter */
	protected MetricSubmitter metricSubmitter = null;
	/** The configured invalidation period for expired virtual agents in ms. */
	protected long invalidationPeriod = DEFAULT_INVALIDATION_PERIOD;
	/** The default invalidation period for expired virtual agents */
	protected static final long DEFAULT_INVALIDATION_PERIOD = TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES); 
	
	
	/** The expiration thread */
	protected Thread expirationThread = null;
	/** A serial number factory for expiration threads */
	private static final AtomicInteger serial = new AtomicInteger();
	/** A serial number factory for virtual agent instance local URIs */
	private static final AtomicLong vaserial = new AtomicLong();
	
	
	/**
	 * Acquires the virtual agent for the passed host and agent
	 * @param host The virtual agent's host
	 * @param agent The virtual agent's name
	 * @return the virtual agent
	 */
	public VirtualAgent getVirtualAgent(String host, String agent) {
		if(host==null || host.trim().isEmpty()) throw new IllegalArgumentException("The passed host was null or empty", new Throwable());
		if(agent==null || agent.trim().isEmpty()) throw new IllegalArgumentException("The passed agent was null or empty", new Throwable());
		final String key = host + ":" + agent;
		VirtualAgent va = virtualAgents.get(key);
		if(va==null) {
			synchronized(virtualAgents) {
				va = virtualAgents.get(key);
				if(va==null) {
					final String localURI = String.format(ServerTracerFactory.LOCAL_SENDER_URI, vaserial.incrementAndGet()); 
					va = new VirtualAgent(host, agent, localURI, this);
					virtualAgents.put(key, va);
					expiryQueue.add(va);
					metricCatalogService.hostAgentState(true, host, "", agent, localURI);
					incr("InitializationEvents");
					info("Initialized " + va);
				}
			}
		}
		return va;
	}
	
	/**
	 * Returns the named VirtualTracer for the passed host and agent
	 * @param host The virtual agent host
	 * @param agent The virtual agent name
	 * @param tracerName The tracer name
	 * @param timeout The tracer timeout in ms.
	 * @return the virtual tracer
	 */
	public VirtualTracer getVirtualTracer(String host, String agent, String tracerName, long timeout) {
		VirtualAgent va = getVirtualAgent(host, agent);
		VirtualTracer vt = va.getVirtualTracer(tracerName);
		if(vt==null) {
			synchronized(va) {
				vt = va.getVirtualTracer(tracerName);
				if(vt==null) {
					vt = new VirtualTracer(host, agent, tracerName, timeout, metricSubmitter);
					va.addVirtualTracer(vt);
				}
			}
		}
		return vt;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#onApplicationContextStart(org.springframework.context.event.ContextStartedEvent)
	 */
	@Override
	public void onApplicationContextStart(ContextStartedEvent event) {		
		super.onApplicationContextStart(event);
		expirationThread = new Thread(this, "VirtualAgentExpirationThread#" + serial.incrementAndGet());
		expirationThread.setDaemon(true);
		expirationThread.start();
		info("Virtual Agent Expiration Thread Started");
	}
	
	/**
	 * Sets the agent state in the metric catalog
	 * @param virtualAgent The agent to set the state for
	 * @param active true to mark up, false to mark down
	 */
	void setAgentState(VirtualAgent virtualAgent, boolean active) {
		metricCatalogService.hostAgentState(active, virtualAgent.getHost(), "", virtualAgent.getAgent(), virtualAgent.getLocalURI());
	}
	
	
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.delay.DelayChangeReceiver#onDelayChange(org.helios.apmrouter.collections.delay.NotifyingDelay)
	 */
	@Override
	public void onDelayChange(VirtualAgent agent) {
		if(expiryQueue.remove(agent)) {
			expiryQueue.add(agent);
		}		
	}
	
	/**
	 * Returns the number of active virtual agents
	 * @return the number of active virtual agents
	 */
	@ManagedMetric(category="VirtualAgents", displayName="VirtualAgentCount", metricType=MetricType.GAUGE, description="The number of active VirtualAgents")
	public int getVirtualAgentCount() {
		return expiryQueue.size();
	}
	
	/**
	 * Returns the number of virtual agent expiries that have been processed
	 * @return the number of virtual agent expiries that have been processed
	 */
	@ManagedMetric(category="VirtualAgents", displayName="ExpirationEvents", metricType=MetricType.COUNTER, description="The number of virtual agent expiries that have been processed")
	public long getExpirationEvents() {
		return getMetricValue("ExpirationEvents");
	}
	
	/**
	 * Returns the number of virtual agent initializations that have been processed
	 * @return the number of virtual agent initializations that have been processed
	 */
	@ManagedMetric(category="VirtualAgents", displayName="InitializationEvents", metricType=MetricType.COUNTER, description="The number of virtual agent initializations that have been processed")
	public long getInitializationEvents() {
		return getMetricValue("InitializationEvents");
	}
	
	
	/**
	 * Returns the time in ms. until the next virtual agent expiry unless there is activity.
	 * Will return -1 if there are no active virtual agents
	 * @return the time in ms. until the next virtual agent expiry
	 */
	@ManagedAttribute(description="The time in ms. until the next virtual agent expiry unless there is activity")
	public long getTimeToNextExpiry() {
		VirtualAgent va = expiryQueue.peek();
		if(va==null) return -1L;
		return va.getTimeToExpiry();
	}
	
	/**
	 * <p>The runnable to pull from the virtual agent expiry queue</p>
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		while(isStarted()) {
			try {			
				final VirtualAgent va = expiryQueue.take();				
				info("Expired " + va);				
				metricCatalogService.hostAgentState(false, va.getHost(), "", va.getAgent(), va.getLocalURI());				
				va.setPendingInvalidation(invalidationScheduler.schedule(new Runnable(){
					@Override
					public void run() {						
						virtualAgents.remove(va.getKey());
						va.run();						
					}
				}, invalidationPeriod, TimeUnit.MILLISECONDS));
				incr("ExpirationEvents");			
			} catch (Exception ex) {
				if(!isStarted()) break;
				Thread.interrupted();
			}
		}
		info("\n\t------------------------------------------------\n\tVirtual Agent Expiration Thread Stopped\n\t------------------------------------------------\n");
	}

	/**
	 * Sets the metric catalog service
	 * @param metricCatalogService the metricCatalogService to set
	 */
	@Autowired(required=true)
	public void setMetricCatalogService(MetricCatalogService metricCatalogService) {
		this.metricCatalogService = metricCatalogService;
	}

	/**
	 * Sets the metric submitter the virtual tracers will send to
	 * @param metricSubmitter the metric submitter to use
	 */	
	@Autowired(required=true)
	public void setMetricSubmitter(MetricSubmitter metricSubmitter) {
		this.metricSubmitter = metricSubmitter;
	}

	/**
	 * Returns the configured invalidation period for expired virtual agents in ms.
	 * @return the invalidation period
	 */
	@ManagedAttribute(description="The configured invalidation period for expired virtual agents in ms.")
	public long getInvalidationPeriod() {
		return invalidationPeriod;
	}

	/**
	 * Sets the configured invalidation period for expired virtual agents in ms.
	 * @param invalidationPeriod the invalidation period to set
	 */
	@ManagedAttribute(description="The configured invalidation period for expired virtual agents in ms.")
	public void setInvalidationPeriod(long invalidationPeriod) {
		this.invalidationPeriod = invalidationPeriod;
	}
	
}
