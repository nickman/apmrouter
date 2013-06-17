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

import static org.helios.apmrouter.server.tracing.virtual.VirtualState.HARDDOWN;
import static org.helios.apmrouter.server.tracing.virtual.VirtualState.INIT;

import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.ObjectName;

import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.metric.ICEMetric;
import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.metric.MetricType;
import org.helios.apmrouter.trace.ITracer;
import org.helios.apmrouter.trace.MetricSubmitter;
import org.helios.apmrouter.trace.TracerImpl;

/**
 * <p>Title: VirtualTracer</p>
 * <p>Description: A server-side, in-vm and in APMRouter server tracer that traces metrics
 * and availability under the identity of a {@link VirtualAgent} for a specific monitored resource.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.tracing.virtual.VirtualTracer</code></p>
 */

public class VirtualTracer extends TracerImpl implements VirtualTracerMBean, MetricSubmitter, Comparable<VirtualTracer> {
	/** The virtual tracer's state */
	private final AtomicReference<VirtualState> tracerState = new AtomicReference<VirtualState>(INIT);
	/** the assigned virtual agent serial */
	protected final long serial;
	/** The last touch timestamp which is the timestamp of the last trace through this tracer */
	protected final AtomicLong touched;
	/** The designated soft down period, meaning if there has been no activity for this period, the tracer is marked soft down */
	protected long softDownPeriod = 30000;
	/** The designated hard down period, meaning if there has been no activity for this period, the tracer is marked hard down */
	protected long hardDownPeriod;
	
	/** The virtual tracer name */
	protected final String name;
	/** The delegate metric submitter */
	protected final MetricSubmitter _submitter;
	/** The direct tracer */
	protected final ITracer _directTracer;
	
	/** the availability name space */
	protected final CharSequence[] avns;
	/** The parent virtual agent */
	protected VirtualAgent vAgent;
	/** The JMX object name for this tracer */
	protected final ObjectName objectName;
	/** The serial number factory for new tracers */
	private static final AtomicLong serialFactory = new AtomicLong(0L);
	/** A counter for the number of metrics sent through this tracer */
	protected static final AtomicLong sentMetrics = new AtomicLong(0L);
	
	/**
	 * Transitions the state of this tracer
	 * @param state The state to transition to
	 * @return the prior state
	 */
	protected VirtualState setState(final VirtualState state) {
		if(state==null) throw new IllegalArgumentException("The passed state was null", new Throwable());		
		VirtualState priorState = tracerState.getAndSet(state);
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
			// ================================================================
			// Notifies agent of state change.
			// Agent forwards to vaMgr for JMX notification 
			// and re-adds tracer if it has been unregisters
			// ================================================================			
			vAgent.onTracerStateChange(this, state, priorState);
		}
		return priorState;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.TracerImpl#getSentMetrics()
	 */
	@Override
	public long getSentMetrics() {		
		return sentMetrics.get();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.TracerImpl#resetStats()
	 */
	@Override
	public void resetStats() {
		sentMetrics.set(0L);
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
			try { JMXHelper.unregisterMBean(objectName); } catch (Exception ex) {/* No Op */}
		}		
	}
	

	/**
	 * Creates a new VirtualTracer
	 * @param host The host of the virtual agent we're tracing for
	 * @param agent The agent name of the virtual agent we're tracing for
	 * @param tracerName The name assigned to this tracer
	 * @param softDownPeriod The designated soft down period, meaning if there has been no activity for this period, the tracer is marked soft down
	 * @param touched The agent provided touch
	 * @param submitter The physical metric submitter doing the submitting

	 */
	public VirtualTracer(String host, String agent, String tracerName, long softDownPeriod, AtomicLong touched, MetricSubmitter submitter) {
		super(host, agent, null);			
		this.submitter = this;
		_directTracer = new TracerImpl(host, agent, submitter);
		objectName = JMXHelper.objectName(String.format(VT_OBJ_NAME, host, agent, tracerName));
		_submitter = submitter;
		serial = serialFactory.incrementAndGet();
		name = tracerName;		
		avns = new CharSequence[]{TRACER_NAMESPACE, name};
		this.touched = touched;
		this.softDownPeriod = softDownPeriod;
		hardDownPeriod = softDownPeriod * 4;
		registerJmx();
		touched.set(System.currentTimeMillis());
		traceAvailability(true);
	}
	
	/**
	 * Sets the virtual agent
	 * @param vAgent the virtual agent
	 */
	void setAgent(VirtualAgent vAgent) {
		this.vAgent = vAgent;
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualTracerMBean#getLastTouchTimestamp()
	 */
	@Override
	public long getLastTouchTimestamp() {
		return touched.get();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualTracerMBean#getLastTouchDate()
	 */
	@Override
	public Date getLastTouchDate() {
		return new Date(touched.get());
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualTracerMBean#getSerial()
	 */
	@Override
	public long getSerial() {
		return serial;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualTracerMBean#touch()
	 */
	@Override
	public void touch() {
		VirtualState state = tracerState.get(); 
		touched.set(System.currentTimeMillis());
		if(state!=VirtualState.UP) {
			setState(VirtualState.UP);			
		}
	}
	
	/**
	 * Checks the state of the tracer to see if a state change is required
	 */
	public void checkState() {
		checkState(System.currentTimeMillis());
	}
	
	
	/**
	 * Checks the state of the tracer to see if a state change is required
	 * @param currentTime The current time, which will be brought up to date if equal to -1.
	 */
	public void checkState(long currentTime) { 
		long age = currentTime - touched.get();
		if(age < softDownPeriod) return;
		if(age >= softDownPeriod && age < hardDownPeriod) {
			// SOFTDOWN
			setState(VirtualState.SOFTDOWN);
		} else {
			// HARDDOWN
			setState(VirtualState.HARDDOWN);
		}
	}
	
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualTracerMBean#expire()
	 */
	@Override
	public void expire() {
		if(getState().ordinal()<VirtualState.SOFTDOWN.ordinal()) {
			touched.set(System.currentTimeMillis()-(softDownPeriod+1));
			setState(VirtualState.SOFTDOWN);
		}
	}
	
	/**
	 * Invalidates this tracer.
	 */
	@Override
	public void invalidate() {
		if(getState().ordinal()<VirtualState.HARDDOWN.ordinal()) {
			touched.set(System.currentTimeMillis()-(hardDownPeriod+1));
			setState(VirtualState.HARDDOWN);
		}
		JMXHelper.unregisterMBean(objectName);
	}	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualTracerMBean#isInvalid()
	 */
	@Override
	public boolean isInvalid() {
		return getState()==VirtualState.HARDDOWN;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualTracerMBean#isExpired()
	 */
	@Override
	public boolean isExpired() {
		return getState().ordinal()>=VirtualState.SOFTDOWN.ordinal();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualTracerMBean#getTimeToSoftDown()
	 */
	@Override
	public long getTimeToSoftDown() {
		long t = (touched.get() + softDownPeriod) - System.currentTimeMillis();
		return t<0 ? -1L : t;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualTracerMBean#getTimeToHardDown()
	 */
	@Override
	public long getTimeToHardDown() {
		long t = (touched.get() + hardDownPeriod) - System.currentTimeMillis();
		return t<0 ? -1L : t;		
	}


	/**
	 * Returns the state of this virtual tracer
	 * @return the state of this virtual tracer
	 */
	public VirtualState getState() {
		return tracerState.get();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualTracerMBean#getStateName()
	 */
	@Override
	public String getStateName() {
		return tracerState.get().name();
	}
	
	/**
	 * Traces the virtual tracer availability, bypassing the virtual tracer's internal controls
	 * @param up true to mark the virtual tracer up, false to mark it down
	 */
	protected void traceAvailability(boolean up) {
		_submitter.submit(ICEMetric.trace(up ? 1L : 0L, host, agent, AVAIL_METRIC_NAME, MetricType.LONG_GAUGE, avns));
	}
	
	/**
	 * Traces the availability of this tracer as up if it's not down.
	 */
	protected void traceAvailability() {
		traceAvailability(getState().ordinal() < VirtualState.SOFTDOWN.ordinal());
	}
	
//	/**
//	 * Traces the tracers availability
//	 */
//	void traceAvailability() {
//		if(System.currentTimeMillis()-lastTick.get() < timeoutPeriod) {
//			touch();
//		}
//		traceAvailabilityX(getState().ordinal() < VirtualState.SOFTDOWN.ordinal());
//	}
	
	/**
	 * {@inheritDoc}
	 * <p>Since this is a virtual agent, it uses a simple submit and is asynchronous.</p>
	 * @see org.helios.apmrouter.trace.MetricSubmitter#submitDirect(org.helios.apmrouter.metric.IMetric, long)
	 */
	@Override
	public void submitDirect(IMetric metric, long timeout) throws TimeoutException {
		touch();		
		if(metric!=null) {
			touch();
			_submitter.submit(metric);
			sentMetrics.incrementAndGet();
		}
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.MetricSubmitter#submit(java.util.Collection)
	 */
	@Override
	public void submit(Collection<IMetric> metrics) {
		if(metrics!=null && !metrics.isEmpty()) {
			touch();
			_submitter.submit(metrics);
			sentMetrics.addAndGet(metrics.size());
		}
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.MetricSubmitter#submit(org.helios.apmrouter.metric.IMetric[])
	 */
	@Override
	public void submit(IMetric... metrics) {
		if(metrics!=null && metrics.length!=0) {
			touch();
			_submitter.submit(metrics);
			sentMetrics.addAndGet(metrics.length);
		}
	}
	


	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String
				.format("VirtualTracer [serial:%s, name:%s, host:%s, agent:%s, state:%s]",
						serial, name, host, agent, getState());
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualTracerMBean#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualTracerMBean#getKey()
	 */
	@Override
	public String getKey() {
		return name;
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualTracerMBean#getSoftDownPeriod()
	 */
	@Override
	public long getSoftDownPeriod() {
		return softDownPeriod;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualTracerMBean#setSoftDownPeriod(long)
	 */
	@Override
	public void setSoftDownPeriod(long softDownPeriod) {
		this.softDownPeriod = softDownPeriod;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualTracerMBean#getHardDownPeriod()
	 */
	@Override
	public long getHardDownPeriod() {
		return hardDownPeriod;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualTracerMBean#setHardDownPeriod(long)
	 */
	@Override
	public void setHardDownPeriod(long hardDownPeriod) {
		this.hardDownPeriod = hardDownPeriod;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(VirtualTracer otherVt) {
		long myTouch = touched.get();
		long otherTouch = touched.get();
		if(myTouch < otherTouch) return -1;
		if(otherTouch < myTouch) return 1;		
		return serial < otherVt.serial ? -1 : 1;
	}
	
	/**
	 * <p>Title: LastTouchDescendingComparator</p>
	 * <p>Description: A {@link VirtualTracer} comparator to sort virtual tracers by the descending last touch time </p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.server.tracing.virtual.VirtualTracer.LastTouchDescendingComparator</code></p>
	 */
	public static class LastTouchDescendingComparator implements Comparator<VirtualTracer> {
		/**
		 * {@inheritDoc}
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(VirtualTracer vt1, VirtualTracer vt2) {
			long vt1Time = vt1.touched.get();
			long vt2Time = vt2.touched.get();		
			if(vt1Time < vt2Time) return 1;
			if(vt2Time < vt1Time) return -1;
			return vt1.serial < vt2.serial ? -1 : 1;
		}
		
	}
	

	
	/**
	 * <p>Title: SoftDownDescendingComparator</p>
	 * <p>Description: A {@link VirtualTracer} comparator to sort virtual tracers by the descending time to soft down.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.server.tracing.virtual.VirtualTracer.SoftDownDescendingComparator</code></p>
	 */
	public static class SoftDownDescendingComparator implements Comparator<VirtualTracer> {
		/**
		 * {@inheritDoc}
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(VirtualTracer vt1, VirtualTracer vt2) {
			long vt1Time = vt1.getTimeToSoftDown();
			long vt2Time = vt2.getTimeToSoftDown();		
			if(vt1Time<0) vt1Time = Long.MAX_VALUE;
			if(vt2Time<0) vt2Time = Long.MAX_VALUE;
			if(vt1Time < vt2Time) return 1;
			if(vt2Time < vt1Time) return -1;
			return vt1.serial < vt2.serial ? -1 : 1;
		}
		
	}
	
	/**
	 * <p>Title: HardDownDescendingComparator</p>
	 * <p>Description: A {@link VirtualTracer} comparator to sort virtual tracers by the descending time to hard down.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.server.tracing.virtual.VirtualTracer.HardDownDescendingComparator</code></p>
	 */
	public static class HardDownDescendingComparator implements Comparator<VirtualTracer> {
		/**
		 * {@inheritDoc}
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(VirtualTracer vt1, VirtualTracer vt2) {
			long vt1Time = vt1.getTimeToHardDown();
			long vt2Time = vt2.getTimeToHardDown();
			if(vt1Time<0) vt1Time = Long.MAX_VALUE;
			if(vt2Time<0) vt2Time = Long.MAX_VALUE;			
			if(vt1Time < vt2Time) return 1;
			if(vt2Time < vt1Time) return -1;
			return vt1.serial < vt2.serial ? -1 : 1;
		}
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.ITracer#getDirectTracer()
	 */
	@Override
	public ITracer getDirectTracer() {
		return _directTracer;
	}


	

}
