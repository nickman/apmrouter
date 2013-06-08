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
import java.util.Date;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.helios.apmrouter.collections.delay.NotifyingDelay;
import org.helios.apmrouter.metric.ICEMetric;
import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.metric.MetricType;
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

public class VirtualTracer extends TracerImpl implements NotifyingDelay<VirtualAgent>, VirtualTracerMBean, MetricSubmitter {
	/** The virtual tracer's state */
	private final AtomicReference<VirtualState> tracerState = new AtomicReference<VirtualState>(INIT);
	/** The delay change receiver */
	protected VirtualAgent receiver = null;
	/** the assigned virtual agent serial */
	protected final long serial;
	/** The last touch timestamp */
	protected final AtomicLong touched;
	/** The last tracer tick */
	protected final AtomicLong lastTick = new AtomicLong(System.currentTimeMillis()); 
	
	/** The timeout period in ms. */
	protected final long timeoutPeriod;
	/** The virtual tracer name */
	protected final String name;
	/** The delegate metric submitter */
	protected final MetricSubmitter _submitter;
	/** the availability name space */
	protected final CharSequence[] avns;
	
	/** Indicates if remove mode is enabled */
	protected final AtomicBoolean removeMode = new AtomicBoolean(false);
	/** The remove id set when the VT is put in remove mode */
	protected final AtomicLong removeId = new AtomicLong(0);
	
	/** Serial number generator for setting the tracer into remove mode */
	private static final AtomicLong removeSerial = new AtomicLong(Long.MIN_VALUE);
	
	

	/** The number of sent metrics */
	protected final AtomicLong sentMetrics = new AtomicLong(0L);
	
	/**
	 * Transitions the state of this tracer
	 * @param state The state to transition to
	 */
	protected void setState(final VirtualState state) {
		if(state==null) throw new IllegalArgumentException("The passed state was null", new Throwable());
		VirtualState priorState = tracerState.getAndSet(state);
		if(priorState!=state) {
			receiver.onTracerStateChange(name, state, priorState);
			if(state.ordinal() >= VirtualState.SOFTDOWN.ordinal()) {
				//traceAvailability(false);
			}
		}
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

	
	/** A serial number factory for virtual tracerExpiryQueue */
	private static final AtomicLong serialFactory = new AtomicLong();

	/**
	 * Creates a new VirtualTracer
	 * @param host The host of the virtual agent we're tracing for
	 * @param agent The agent name of the virtual agent we're tracing for
	 * @param tracerName The name assigned to this tracer
	 * @param timeoutPeriod The timeout period in ms. for this tracer
	 * @param submitter The physical metric submitter doing the submitting
	 */
	public VirtualTracer(String host, String agent, String tracerName, long timeoutPeriod, MetricSubmitter submitter) {
		super(host, agent, null);			
		this.submitter = this;
		_submitter = submitter;
		serial = serialFactory.incrementAndGet();
		name = tracerName;		
		avns = new CharSequence[]{TRACER_NAMESPACE, name};
		this.timeoutPeriod = timeoutPeriod;
		touched = new AtomicLong(System.currentTimeMillis());
		traceAvailabilityX(true);
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
		if(!state.canTrace()) throw new InvalidatedVirtualTracerException("This virtual tracer [" + name + "] for VirtualAgent [" + host + ":" + agent + "] has been invalidated", new Throwable());		
		//touched.set(System.currentTimeMillis());
		if(state==VirtualState.SOFTDOWN) {
			setState(VirtualState.UP);			
		} else if(state==VirtualState.INIT) {
			setState(VirtualState.UP);
		}
		
//		traceAvailability(true);
		receiver.onDelayChange(this, System.currentTimeMillis());		
	}
	
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualTracerMBean#expire()
	 */
	@Override
	public void expire() {
		touched.set(0);
		setState(VirtualState.SOFTDOWN);
		receiver.onDelayChange(this, 0L);
	}
	
	/**
	 * Invalidates this tracer.
	 */
	@Override
	public void invalidate() {
		touched.set(0);
		setState(VirtualState.HARDDOWN);
		//JMXHelper.unregisterMBean(objectName);
	}	
	
	/**
	 * Indicates if this virtual tracer is expired
	 * @return true if this virtual tracer is expired, false otherwise
	 */
	public boolean isInvalidated() {
		return tracerState.get()==HARDDOWN;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualTracerMBean#getTimeToExpiry()
	 */
	@Override
	public long getTimeToExpiry() {
		if(isInvalidated()) return -1L;
		long d = System.currentTimeMillis()-touched.get();			
		return timeoutPeriod - d;			
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.Delayed#getDelay(java.util.concurrent.TimeUnit)
	 */
	@Override
	public long getDelay(TimeUnit unit) {
		if(isRemoveMode()) return removeId.get();
		return unit.convert(getTimeToExpiry(), TimeUnit.MILLISECONDS);
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
	 * {@inheritDoc}
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Delayed otherDelayed) {
		if(this==otherDelayed) return 0;
		long myDelay = getTimeToExpiry();
		long hisDelay = otherDelayed.getDelay(TimeUnit.MILLISECONDS);
		if(myDelay > hisDelay) return 1;
		if(myDelay < hisDelay) return -1;
		if(otherDelayed instanceof VirtualTracer) {
			return ((VirtualTracer)otherDelayed).serial > serial ? 1 : -1;
		}
		return 0;
	}

//	/**
//	 * {@inheritDoc}
//	 * @see org.helios.apmrouter.collections.delay.NotifyingDelay#setDelayChangeReceiver(org.helios.apmrouter.collections.delay.DelayChangeReceiver)
//	 */
//	@Override
//	public void setDelayChangeReceiver(DelayChangeReceiver<? extends NotifyingDelay> receiver) {
//		this.receiver = receiver;		
//	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.delay.NotifyingDelay#setDelayChangeReceiver(org.helios.apmrouter.collections.delay.DelayChangeReceiver)
	 */
	@Override
	public void setDelayChangeReceiver(VirtualAgent receiver) {
		this.receiver = receiver;
		
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.delay.NotifyingDelay#clearDelayChangeReceiver()
	 */
	@Override
	public void clearDelayChangeReceiver() {
		setState(VirtualState.SOFTDOWN);
		receiver = null;		
	}
	
	/**
	 * Traces the virtual tracer availability, bypassing the virtual tracer's internal controls
	 * @param up true to mark the virtual tracer up, false to mark it down
	 */
	protected void traceAvailabilityX(boolean up) {
		//System.out.println("Tracing [" + (up ? 1 : 0) + "] for [" + host + "/" + agent + "/" + name);
		_submitter.submit(ICEMetric.trace(up ? 1L : 0L, host, agent, AVAIL_METRIC_NAME, MetricType.LONG_GAUGE, avns));
	}
	
	/**
	 * Traces the tracers availability
	 */
	void traceAvailability() {
		if(System.currentTimeMillis()-lastTick.get() < timeoutPeriod) {
			touch();
		}
		traceAvailabilityX(getState().ordinal() < VirtualState.SOFTDOWN.ordinal());
	}
	
	/**
	 * {@inheritDoc}
	 * <p>Since this is a virtual agent, it uses a simple submit and is asynchronous.</p>
	 * @see org.helios.apmrouter.trace.MetricSubmitter#submitDirect(org.helios.apmrouter.metric.IMetric, long)
	 */
	@Override
	public void submitDirect(IMetric metric, long timeout) throws TimeoutException {
		//touch();
		lastTick.set(System.currentTimeMillis());
		if(metric!=null) {
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
		//touch();
		
		lastTick.set(System.currentTimeMillis());
		if(metrics!=null) {
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
		//touch();
		
		lastTick.set(System.currentTimeMillis());
		if(metrics!=null) {
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
				.format("VirtualTracer [serial:%s, timeoutPeriod:%s, name:%s, host:%s, agent:%s, getLastTouchDate():%s, getTimeToExpiry():%s, getState():%s]",
						serial, timeoutPeriod, name, host, agent,
						getLastTouchDate(), getTimeToExpiry(), getState());
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
	 * @see org.helios.apmrouter.collections.delay.NotifyingDelay#setUpdatedTimestamp(long)
	 */
	@Override
	public void setUpdatedTimestamp(long timestamp) {
		touched.set(timestamp);		
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (serial ^ (serial >>> 32));
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
		VirtualTracer other = (VirtualTracer) obj;
		if (serial != other.serial)
			return false;
		return true;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualTracerMBean#getTimeoutPeriod()
	 */
	@Override
	public long getTimeoutPeriod() {
		return timeoutPeriod;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualTracerMBean#isRemoveMode()
	 */
	@Override
	public boolean isRemoveMode() {
		return removeMode.get();
	}
	
	/**
	 * Returns the next negative serial number, resetting the sequence if it has reached zero.
	 * @return the next negative serial number
	 */
	long getRemoveSerial() {
		long serial = removeSerial.incrementAndGet();
		if(serial>=0) {
			synchronized(removeSerial) {
				removeSerial.set(Long.MIN_VALUE);
				serial = removeSerial.incrementAndGet();
			}
		}
		return serial;
	}
	
	/**
	 * Sets the remove state of this VT
	 * @param enable true to enable, false to reset
	 * @return true if the mode was changed, false otherwise
	 */
	boolean setRemoveMode(boolean enable) {
		final boolean set = removeMode.compareAndSet(!enable, enable);
		if(!set) return false;
		if(enable) {
			removeId.set(getRemoveSerial());
		} else {
			removeId.set(0);
		}
		return true;
	}


}
