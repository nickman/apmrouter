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

import javax.management.AttributeChangeNotification;

import org.helios.apmrouter.catalog.DChannelEvent;
import org.helios.apmrouter.catalog.MetricCatalogService;
import org.helios.apmrouter.collections.delay.DynamicDelayQueue;
import org.helios.apmrouter.server.ServerComponentBean;
import org.helios.apmrouter.server.services.session.SharedChannelGroup;
import org.helios.apmrouter.server.tracing.ServerTracerFactory;
import org.helios.apmrouter.trace.MetricSubmitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedNotification;
import org.springframework.jmx.export.annotation.ManagedNotifications;
import org.springframework.jmx.support.MetricType;

/**
 * <p>Title: VirtualAgentManager</p>
 * <p>Description: Managing container for {@link VirtualAgent}s.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.tracing.virtual.VirtualAgentManager</code></p>
 */
@ManagedNotifications({
	@ManagedNotification(description="Notification issued when a virtual agent changes state", name="javax.management.AttributeChangeNotification", notificationTypes={"jmx.attribute.change"}),
	@ManagedNotification(description="Notification issued when a virtual tracer changes state", name="javax.management.AttributeChangeNotification", notificationTypes={"jmx.attribute.change"})
})
public class VirtualAgentManager extends ServerComponentBean implements Runnable {
	/** Invalidation scheduler */
	protected static final ScheduledExecutorService invalidationScheduler = Executors.newScheduledThreadPool(1, new ThreadFactory(){
		private final AtomicInteger serial = new AtomicInteger();
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "VirtualAgentInvalidatorThread#" + serial.incrementAndGet());
			t.setDaemon(true);
			return t;
		}
	});
	
	/** The notification type name for a virtual agent state change */
	public static final String AGENT_STATE_CHANGE_NOTIF = "virtual.statechange.agent";
	/** The notification type name for a virtual tracer state change */
	public static final String TRACER_STATE_CHANGE_NOTIF = "virtual.statechange.tracer";
	
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
	/** The configured polling period for expired virtual agents in ms. */
	protected long pollingPeriod = POLLING_EXPIRATION_PERIOD;
	/** The default polling period for expired virtual agents */
	protected static final long POLLING_EXPIRATION_PERIOD = 1000; 
	
	
	/** The expiration thread */
	protected Thread expirationThread = null;
	/** The availability thread */
	protected Thread availabilityThread = null;
	
	/** A serial number factory for expiration threads */
	private static final AtomicInteger serial = new AtomicInteger();
	/** A serial number factory for virtual agent instance local URIs */
	private static final AtomicLong vaserial = new AtomicLong();
	/** A serial number factory for notifications */
	private static final AtomicLong nserial = new AtomicLong();

	/**
	 * Acquires the virtual agent for the passed host and agent.
	 * Throws a runtime error if the agent does not exist, since this call needs to be preceeded by
	 * a call to {@link #getVirtualAgent(String, String, VirtualTracer)} or {@link #getVirtualTracer(String, String, String, long)}.
	 * @param host The virtual agent's host
	 * @param agent The virtual agent's name
	 * @return the virtual agent
	 */
	public VirtualAgent getVirtualAgent(String host, String agent) {
		return getVirtualAgent(host, agent, null);
	}
	
	
	/**
	 * Acquires the virtual agent for the passed host and agent
	 * @param host The virtual agent's host
	 * @param agent The virtual agent's name
	 * @param initialTracer The optional initial tracer. If the agent has not been created yet, this is mandatory, since
	 * without tracers, the agent will expire immediately.
	 * @return the virtual agent
	 */
	public VirtualAgent getVirtualAgent(String host, String agent, VirtualTracer initialTracer) {
		if(host==null || host.trim().isEmpty()) throw new IllegalArgumentException("The passed host was null or empty", new Throwable());
		if(agent==null || agent.trim().isEmpty()) throw new IllegalArgumentException("The passed agent was null or empty", new Throwable());
		final String key = host + ":" + agent;
		VirtualAgent va = virtualAgents.get(key);
		if(va==null) {
			synchronized(virtualAgents) {
				va = virtualAgents.get(key);
				if(va==null) {
					if(initialTracer==null) {
						String msg = String.format("Attempted to acquire VirtualAgent [%s/%s] that had no VirtualTracers. This call must be preceeded by getVirtualTracer(<host>, <agent>, <tracer name>, <timeout>)", host, agent);
						error(msg);
						throw new RuntimeException(msg, new Throwable());
					}
					final String localURI = String.format(ServerTracerFactory.LOCAL_SENDER_URI, vaserial.incrementAndGet()); 
					va = new VirtualAgent(host, agent, localURI, this);
					virtualAgents.put(key, va);
					va.addVirtualTracer(initialTracer);
					expiryQueue.add(va);
					//metricCatalogService.hostAgentState(true, host, "", agent, localURI);
					markCatalogAgentState(true, va);
					incr("InitializationEvents");
					info("Initialized " + va);
				}
			}
		}
		return va;
	}
	
	/**
	 * Sends a virtual agent state change JMX notification
	 * @param agent The agent that changed state
	 * @param newState The new state of the agent
	 * @param priorState The prior state of the agent
	 */
	void sendAgentStateChangeNotification(VirtualAgent agent, VirtualState newState, VirtualState priorState) {
		if(agent==null) throw new IllegalArgumentException("The passed virtual agent was null", new Throwable());
		if(newState==null) throw new IllegalArgumentException("The passed virtual agent new state was null", new Throwable());
		String message = new StringBuilder("The virtual agent [").append(agent.getHost()).append("/").append(agent.getAgent()).append("] changed state from [").append(priorState==null ? null : priorState.name()).append("] to [").append(newState.name()).append("]").toString();
		AttributeChangeNotification notif = new AttributeChangeNotification(new String[]{agent.getHost(), agent.getAgent()}, nserial.incrementAndGet(), System.currentTimeMillis(), message, "State", String.class.getName(), priorState==null ? null : priorState.name(), newState.name());
		notif.setUserData(AGENT_STATE_CHANGE_NOTIF);
		notificationPublisher.sendNotification(notif);
	}
	
	/**
	 * Sends a virtual tracer state change JMX notification
	 * @param agent The agent whose tracer changed state
	 * @param tracerName The name of the tracer that changed state
	 * @param newState The new state of the tracer
	 * @param priorState The prior state of the tracer
	 */
	void sendTracerStateChangeNotification(VirtualAgent agent, String tracerName, VirtualState newState, VirtualState priorState) {
		if(agent==null) throw new IllegalArgumentException("The passed virtual agent was null", new Throwable());
		if(newState==null) throw new IllegalArgumentException("The passed virtual tracer new state was null", new Throwable());
		String message = new StringBuilder("The virtual tracer [").append(agent.getHost()).append("/").append(agent.getAgent()).append("/").append(tracerName).append("] changed state from [").append(priorState==null ? null : priorState.name()).append("] to [").append(newState.name()).append("]").toString();
		AttributeChangeNotification notif = new AttributeChangeNotification(new String[]{agent.getHost(), agent.getAgent(), tracerName}, nserial.incrementAndGet(), System.currentTimeMillis(), message, "State", String.class.getName(), priorState==null ? null : priorState.name(), newState.name());
		notif.setUserData(TRACER_STATE_CHANGE_NOTIF);
		notificationPublisher.sendNotification(notif);
	}
	
	/**
	 * Updates the metric catalog to set the state of the virtual agent and sends a notification to state listeners
	 * @param up true if the VA came up, false if it expired.
	 * @param va The VA
	 */
	protected void markCatalogAgentState(boolean up, VirtualAgent va) {
		DChannelEvent dce = metricCatalogService.hostAgentState(up, va.getHost(), "", va.getAgent(), va.getLocalURI());
		if(up) {
			SharedChannelGroup.getInstance().sendVirtualAgentStartedEvent(dce);
		} else {
			SharedChannelGroup.getInstance().sendVirtualAgentExpiredEvent(dce);
		}
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
		final String key = host + ":" + agent;
		VirtualAgent va = virtualAgents.get(key);
		VirtualTracer vt = null;
		if(va==null) {
			synchronized(virtualAgents) {
				va = virtualAgents.get(key);
				if(va==null) {
					// need to pre-create tracer
					vt = new VirtualTracer(host, agent, tracerName, timeout, metricSubmitter);
					va = getVirtualAgent(host, agent, vt);
				} else {
					if(va.receiver==null) {
						expiryQueue.add(va);
					}
					vt = va.getVirtualTracer(tracerName);
					if(vt==null) {
						synchronized(va) {
							vt = va.getVirtualTracer(tracerName);
							if(vt==null) {
								vt = new VirtualTracer(host, agent, tracerName, timeout, metricSubmitter);
								va.addVirtualTracer(vt);
							}
						}
					} else {
						// this horribly breaks the model, but if we don't touch the VT, the VA will expire immedialty
						vt.touched.set(System.currentTimeMillis());						
					}
				}
			}
		} else {
			vt = va.getVirtualTracer(tracerName);
			if(vt==null) {
				synchronized(va) {
					vt = va.getVirtualTracer(tracerName);
					if(vt==null) {
						vt = new VirtualTracer(host, agent, tracerName, timeout, metricSubmitter);
						va.addVirtualTracer(vt);
					} else {
						// this horribly breaks the model, but if we don't touch the VT, the VA will expire immedialty
						vt.touched.set(System.currentTimeMillis());
					}
				}
			} else {
				// this horribly breaks the model, but if we don't touch the VT, the VA will expire immedialty
				vt.touched.set(System.currentTimeMillis());
				va.addVirtualTracer(vt);
			}
		}					
		if(vt.receiver==null) {
			vt.setDelayChangeReceiver(va);
		}
		if(va.receiver==null || !expiryQueue.contains(va)) {
			expiryQueue.add(va);
		}
		return vt;
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStart()
	 */
	@Override
	protected void doStart() throws Exception {		
		//super.onApplicationContextStart(event);
		expirationThread = new Thread(this, "VirtualAgentExpirationThread#" + serial.incrementAndGet());
		expirationThread.setDaemon(true);
		started.set(true);
		expirationThread.start();
		availabilityThread = new Thread(new Runnable(){
			@Override
			public void run() {
				while(isStarted()) {
					try {			
						Thread.currentThread().join(15000);
						for(VirtualAgent va: virtualAgents.values()) {
							for(VirtualTracer vt: va) {
								vt.traceAvailability();
							}
						}
					} catch (Exception ex) {}
				}
			}
		}, "VirtualTracerAvailabilityThread#" + serial.incrementAndGet());
		availabilityThread.setDaemon(true);
		availabilityThread.start();
		info("Virtual Agent Expiration Thread Started");
		
	}
	
	/**
	 * Returns the expirationThread toString
	 * @return the expirationThread toString
	 */
	@ManagedAttribute(description="The expirationThread toString")
	public String getExpirationThread() {
		return expirationThread==null ? null : expirationThread.toString();
	}
	
	/**
	 * Returns the state of the expiration thread
	 * @return the state of the expiration thread
	 */
	@ManagedAttribute(description="The expirationThread state")
	public Thread.State getExpirationThreadState() {
		return expirationThread==null ? null : expirationThread.getState();
	}

	
	
	/**
	 * Returns the number of active virtual agents
	 * @return the number of active virtual agents
	 */
	@ManagedMetric(category="VirtualAgents", displayName="ActiveVirtualAgentCount", metricType=MetricType.GAUGE, description="The number of active VirtualAgents")
	public int getActiveVirtualAgentCount() {
		return expiryQueue.size();
	}
	
	/**
	 * Returns the number of registered virtual agents
	 * @return the number of registered virtual agents
	 */
	@ManagedMetric(category="VirtualAgents", displayName="RegisteredVirtualAgentCount", metricType=MetricType.GAUGE, description="The number of registered VirtualAgents")
	public int getRegisteredVirtualAgentCount() {
		return virtualAgents.size();
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
				final VirtualAgent va = expiryQueue.poll(pollingPeriod, TimeUnit.MILLISECONDS);
				if(va==null) continue;
				info("Expired " + va);				
				//metricCatalogService.hostAgentState(false, va.getHost(), "", va.getAgent(), va.getLocalURI());
				markCatalogAgentState(false, va);
				va.setPendingInvalidation(invalidationScheduler.schedule(new Runnable(){
					@Override
					public void run() {
						info("Invalidated " + va);
						virtualAgents.remove(va.getKey());
						va.invalidate();						
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


	/**
	 * Returns the configured polling period for expired virtual agents in ms.
	 * @return the configured polling period for expired virtual agents in ms.
	 */
	@ManagedAttribute(description="The configured polling period for expired virtual agents in ms.")
	public long getPollingPeriod() {
		return pollingPeriod;
	}


	/**
	 * Sets the configured polling period for expired virtual agents in ms.
	 * @param pollingPeriod the pollingPeriod to set
	 */
	@ManagedAttribute(description="The configured polling period for expired virtual agents in ms.")
	public void setPollingPeriod(long pollingPeriod) {
		this.pollingPeriod = pollingPeriod;
	}
	
}
