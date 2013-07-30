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

import static org.helios.apmrouter.server.tracing.virtual.VirtualState.INIT;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.ref.RunnableReferenceQueue;


/**
 * <p>Title: VirtualAgent</p>
 * <p>Description: Represents a notional agent for which no agent actually exists but which is logically 
 * represented by apm-collectors using {@link VirtualTracer}s tagged to this agent.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.tracing.VirtualAgent</code></p>
 */

public class VirtualAgent implements VirtualAgentMXBean, Runnable, Iterable<VirtualTracer> {
	/** The virtual tracer's state */
	private final AtomicReference<VirtualState> agentState = new AtomicReference<VirtualState>(INIT);
	/** A map of virtual tracerExpiryQueue keyed by the tracer name */
	protected final Map<String, WeakReference<VirtualTracer>> tracers = new ConcurrentHashMap<String, WeakReference<VirtualTracer>>(); 
	/** The agent's host */
	protected final String host;
	/** The agent's name */
	protected final String agent;
	/** The agent's local URI */
	protected final String localURI;
	/** The JMX ObjectName for this virtual agent */
	protected final ObjectName objectName;
	/** A reference to the Virtual Agent Manager */
	protected final VirtualAgentManager vaManager;
	/** Instance logger */
	protected final Logger log;
	/** A reference to the ref cleaner */
	protected final RunnableReferenceQueue rrq = RunnableReferenceQueue.getInstance();
	

	
	/**
	 * Creates a new VirtualAgent
	 * @param host The agent's host
	 * @param agent The agent name
	 * @param localURI The designated local URI for this virtual agent
	 * @param vaManager A reference to the Virtual Agent Manager
	 */
	VirtualAgent(String host, String agent, String localURI, VirtualAgentManager vaManager) {
		this.host = host;
		this.agent = agent;
		this.localURI = localURI;
		this.vaManager = vaManager;
		log =  Logger.getLogger(String.format("%s.%s:%s", getClass().getName() ,host, agent));
		objectName = JMXHelper.objectName(String.format(VA_OBJ_NAME, host, agent));
		registerJmx();
		this.vaManager.sendAgentStateChangeNotification(this, VirtualState.INIT, null);
	}
	
	/**
	 * Registers this tracer's management MBean
	 */
	void registerJmx() {
		if(JMXHelper.getHeliosMBeanServer().isRegistered(objectName)) {
			JMXHelper.unregisterMBean(objectName);
		}
		JMXHelper.registerMBean(this, objectName);
	}

	/**
	 * Unegisters this tracer's management MBean
	 */
	void unregisterJmx() {
		if(JMXHelper.getHeliosMBeanServer().isRegistered(objectName)) {
			try { JMXHelper.unregisterMBean(objectName); } catch (Exception ex) {}
		}		
	}
	
	
	/**
	 * Returns a set of all the tracers in this agent
	 * @return a set of all the tracers in this agent
	 */
	protected Set<VirtualTracer> getAllTracers() {
		Set<VirtualTracer> allTracers = new HashSet<VirtualTracer>(tracers.size());
		for(String tracerName: tracers.keySet()) {
			VirtualTracer vt = getTracer(tracerName);
			if(vt!=null) allTracers.add(vt);			
		}
		return allTracers;
	}
	
	/**
	 * Inserts the passed tracer into the tracer map
	 * @param vt the tracer to add
	 */
	protected void putTracer(final VirtualTracer vt) {
		if(vt==null) throw new IllegalArgumentException("The passed virtual tracer was null", new Throwable());
		final String vtName = vt.getName();
		if(!tracers.containsKey(vtName)) {
			synchronized(tracers) {
				if(!tracers.containsKey(vtName)) {
					Runnable r = new Runnable() {			
						final String id = getHost() + "/" + getAgent() + ":" + vtName;
						@Override
						public void run() {
							log.info("Virtual Tracer [" + id + "] was evicted");
							tracers.remove(vtName);
						}
					};
					tracers.put(vt.getName(), rrq.buildWeakReference(vt, r));					
				}
			}
		}
		
	}
	
	/**
	 * Retrieves a tracer from the tracer map
	 * @param name The name of the tracer
	 * @return the tracer
	 */
	protected VirtualTracer getTracer(String name) {
		WeakReference<VirtualTracer> ref = tracers.get(name);
		if(ref==null) {
			
			return null;
		}
		return ref.get();
	}
	
	/**
	 * Transitions the state of this agent
	 * @param state The state to transition to
	 * @return the prior state
	 */
	protected VirtualState setState(final VirtualState state) {
		if(state==null) throw new IllegalArgumentException("The passed state was null", new Throwable());
		VirtualState priorState = agentState.getAndSet(state);
		if(priorState!=state) {
			if(priorState!=state) {
				// ================================================================
				// Case statement for non-timestamp or state actions to fire
				// on a valid state change
				// ================================================================
				switch(state) {
					case HARDDOWN:
						unregisterJmx();
						break;
					case SOFTDOWN:
						break;
					case UP:
						registerJmx();
						break;
				}
				this.vaManager.sendAgentStateChangeNotification(this, state, priorState);
			}			
		}
		return priorState;
	}

	/**
	 * Callback from a virtual tracer when it changes state
	 * @param tracerName The name of the tracer that changed state
	 * @param state The state the tracer transitioned to
	 * @param priorState The prior state of the tracer
	 */
	void onTracerStateChange(VirtualTracer tracer, VirtualState state, VirtualState priorState) {
		if(state==VirtualState.UP && priorState!=state) {
			putTracer(tracer);			
		}
		vaManager.sendTracerStateChangeNotification(this, tracer.getName(), state, priorState);
	}
	

	/**
	 * Returns the named virtual tracer
	 * @param name The name of the virtual tracer to retrieve
	 * @return the named virtual tracer
	 */
	public VirtualTracer getVirtualTracer(String name) {
		return getTracer(name);
	}
	
	/**
	 * Returns the named virtual tracer
	 * @param name The name of the virtual tracer to retrieve
	 * @param timeout The timeout for this tracer in ms.
	 * @return the named virtual tracer
	 * @FIXME
	 */
	public VirtualTracer getVirtualTracer(String name, long timeout) {
		return getTracer(name); 
	}
	
	
	/**
	 * Returns the reference key for this virtual agent
	 * @return the reference key
	 */
	public String getKey() {
		return String.format("%s:%s", host, agent);
	}
	
	/**
	 * Adds a virtual tracer to this virtual agent
	 * @param vt The virtual tracer to add
	 */
	public void addVirtualTracer(VirtualTracer vt) {
		if(vt!=null) {
			if(!host.equals(vt.getHost())) throw new IllegalArgumentException("The virtual tracer for host [" + vt.getHost() + "] does not belong in this agent for host [" + host + "]", new Throwable());
			if(!agent.equals(vt.getAgent())) throw new IllegalArgumentException("The virtual tracer for agent [" + vt.getAgent() + "] does not belong in this agent for agent [" + agent + "]", new Throwable());
			if(!tracers.containsKey(vt.getName())) {
				synchronized(tracers) {
					if(!tracers.containsKey(vt.getName())) {
						putTracer(vt);
					}					
				}
			}
		}		
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualAgentMXBean#getStateName()
	 */
	@Override
	public String getStateName() {
		return agentState.get().name();
	}

	/**
	 * Returns the state of this VirtualAgent
	 * @return the state
	 */
	public VirtualState getState() {
		return agentState.get();
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualAgentMXBean#getTimeToSoftDown()
	 */
	@Override
	public long getTimeToSoftDown() {
		TreeSet<VirtualTracer> sorter = new TreeSet<VirtualTracer>(DESCENDING_SOFT_SORTER);
		sorter.addAll(getAllTracers());
		if(sorter.isEmpty()) return -1L;
		return sorter.iterator().next().getTimeToSoftDown();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualAgentMXBean#getTimeToHardDown()
	 */
	@Override
	public long getTimeToHardDown() {
		TreeSet<VirtualTracer> sorter = new TreeSet<VirtualTracer>(DESCENDING_HARD_SORTER);
		sorter.addAll(getAllTracers());
		if(sorter.isEmpty()) return -1L;
		return sorter.iterator().next().getTimeToHardDown();		
	}
	
	/** A descending comparator for VirtualTracers by time to soft down */
	public static final Comparator<VirtualTracer> DESCENDING_SOFT_SORTER = new VirtualTracer.SoftDownDescendingComparator();
	/** A descending comparator for VirtualTracers by time to hard down */
	public static final Comparator<VirtualTracer> DESCENDING_HARD_SORTER = new VirtualTracer.HardDownDescendingComparator();
	/** A descending comparator for VirtualTracers by last touch time */
	public static final Comparator<VirtualTracer> DESCENDING_LT_SORTER = new VirtualTracer.LastTouchDescendingComparator();
	
	
	/**
	 * Periodic check for expirations
	 */
	public void check() {
		long currentTime = System.currentTimeMillis();
		for(VirtualTracer vt: getAllTracers()) {
			if(vt.isInvalid()) continue;
			vt.checkState(currentTime);
		}
		if(getTimeToHardDown()<1) {
			invalidate();
		} else if(getTimeToSoftDown()<1) {
			expire();
		}
//		long age = currentTime - touched.get();
//		if(age < softDownPeriod) return;
//		if(age >= softDownPeriod && age < hardDownPeriod) {
//			// SOFTDOWN
//			setState(VirtualState.SOFTDOWN);
//		} else {
//			// HARDDOWN
//			setState(VirtualState.HARDDOWN);
//		}
		
	}
	

	/**
	 * Touches all the agent's tracers
	 */
	@Override
	public void touch() {
		for(VirtualTracer vt: getAllTracers()) {
			vt.touch();
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualAgentMXBean#getHost()
	 */
	@Override
	public String getHost() {
		return host;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualAgentMXBean#getAgent()
	 */
	@Override
	public String getAgent() {
		return agent;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualAgentMXBean#expire()
	 */
	@Override
	public void expire() {
		setState(VirtualState.SOFTDOWN);
		for(VirtualTracer vt: getAllTracers()) {
			vt.expire();
		}		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualAgentMXBean#invalidate()
	 */
	@Override
	public void invalidate() {		
		setState(VirtualState.HARDDOWN);
		JMXHelper.unregisterMBean(objectName);
		for(VirtualTracer vt: getAllTracers()) {
			vt.invalidate();
		}		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualAgentMXBean#getVirtualTracers()
	 */
	@Override
	public Map<String, VirtualTracerMBean> getVirtualTracers() {
		Map<String, VirtualTracerMBean> map = new HashMap<String, VirtualTracerMBean>(tracers.size());
		for(VirtualTracer vt: getAllTracers()) {
			map.put(vt.getName(), vt);
		}
		return Collections.unmodifiableMap(map);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualAgentMXBean#isInvalid()
	 */
	@Override
	public boolean isInvalid() {
		return getState()==VirtualState.HARDDOWN;
	}
	
	/**
	 * Determines if this virtual agent has been expired or invalidated
	 * @return true if this virtual agent has been expired or invalidated, false otherwise
	 */
	public boolean isExpired() {
		return getState().ordinal()>=VirtualState.SOFTDOWN.ordinal();
	}
	
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualAgentMXBean#getLastTouchTimestamp()
	 */
	@Override
	public long getLastTouchTimestamp() {
		try {			
			TreeSet<VirtualTracer> sorter = new TreeSet<VirtualTracer>(DESCENDING_LT_SORTER);
			sorter.addAll(getAllTracers());
			if(sorter.isEmpty()) return -1L;
			return sorter.iterator().next().getLastTouchTimestamp();		
		} catch (Exception nse) {
			return 0;
		}		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualAgentMXBean#getLastTouchDate()
	 */
	@Override
	public Date getLastTouchDate() {
		try {			
			return new Date(getLastTouchTimestamp());
		} catch (Exception nse) {
			return null;
		}		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualAgentMXBean#getTracerCount()
	 */
	@Override
	public int getTracerCount() {
		return tracers.size();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualAgentMXBean#getActiveTracerCount()
	 */
	@Override
	public int getActiveTracerCount() {
		return tracers.size();
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((agent == null) ? 0 : agent.hashCode());
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VirtualAgent other = (VirtualAgent) obj;
		if (agent == null) {
			if (other.agent != null)
				return false;
		} else if (!agent.equals(other.agent))
			return false;
		if (host == null) {
			if (other.host != null)
				return false;
		} else if (!host.equals(other.host))
			return false;
		return true;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("VirtualAgent [host:%s, agent:%s, uri:%s]", host, agent, localURI);
	}

	/**
	 * Returns the local URI for this virtual agent
	 * @return the localURI
	 */
	public String getLocalURI() {
		return localURI;
	}
	
	/**
	 * <p>Called when this virtual agent is invalidated</p>
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		log.info("Invalidating " + this);
		for(VirtualTracer t: this) {
			t.invalidate();
		}			
	}
	

	/**
	 * {@inheritDoc}
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<VirtualTracer> iterator() {
		return Collections.unmodifiableCollection(getAllTracers()).iterator();
	}

	/**
	 * <p>Title: HardDownDescendingComparator</p>
	 * <p>Description: Hard down time descending based comparator for virtual agents</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.server.tracing.virtual.VirtualAgent.HardDownDescendingComparator</code></p>
	 */
	public static class HardDownDescendingComparator implements Comparator<VirtualAgent> {
		/**
		 * {@inheritDoc}
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(VirtualAgent vt1, VirtualAgent vt2) {
			long vt1Time = vt1.getTimeToHardDown();
			long vt2Time = vt2.getTimeToHardDown();
			if(vt1Time<0) vt1Time = Long.MAX_VALUE;
			if(vt2Time<0) vt2Time = Long.MAX_VALUE;			
			if(vt1Time < vt2Time) return 1;
			if(vt2Time < vt1Time) return -1;
			return 1;
		}
		
	}




	

}
