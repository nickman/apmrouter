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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.helios.apmrouter.collections.delay.DelayChangeReceiver;
import org.helios.apmrouter.collections.delay.NotifyingDelay;
import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.trace.MetricSubmitter;
import org.helios.apmrouter.trace.TracerImpl;

/**
 * <p>Title: VirtualTracer</p>
 * <p>Description: A server-side, in-vm and in APMRouter server tracer that traces metrics
 * and availability under the identity of a {@link VirtualAgent} for a specific monitored resource.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.tracing.VirtualTracer</code></p>
 */

public class VirtualTracer extends TracerImpl implements NotifyingDelay, VirtualTracerMBean {

	/** The virtual tracer's state */
	private final AtomicReference<VirtualState> tracerState = new AtomicReference<VirtualState>(INIT);
	/** The delay change receiver */
	protected DelayChangeReceiver<NotifyingDelay> receiver = null;
	/** the assigned virtual agent serial */
	protected final long serial;
	/** The last touch timestamp */
	protected final AtomicLong touched; 
	/** The timeout period in ms. */
	protected final long timeoutPeriod;
	/** The virtual tracer name */
	protected final String name;
	/** The delegate metric submitter */
	protected final MetricSubmitter submitter;
	
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
		super(host, agent, submitter);
		serial = serialFactory.incrementAndGet();
		name = tracerName;
		this.submitter = submitter;
		this.timeoutPeriod = timeoutPeriod;
		touched = new AtomicLong(System.currentTimeMillis());
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
		if(!state.canTrace()) throw new IllegalStateException("This virtual tracer [" + name + "] for VirtualAgent [" + host + ":" + agent + "] has been invalidated", new Throwable());		
		touched.set(System.currentTimeMillis());
		if(state==VirtualState.SOFTDOWN) {
			tracerState.set(VirtualState.UP);			
		} else if(state==VirtualState.INIT) {
			tracerState.set(VirtualState.UP);
		}		
		receiver.onDelayChange(this);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualTracerMBean#expire()
	 */
	@Override
	public void expire() {
		touched.set(0);
		tracerState.set(VirtualState.SOFTDOWN);
		receiver.onDelayChange(this);
	}
	
	/**
	 * Invalidates this tracer.
	 */
	@Override
	public void invalidate() {
		touched.set(0);
		tracerState.set(VirtualState.HARDDOWN);
		receiver.onDelayChange(this);
	}	
	
	/**
	 * Indicates if this virtual tracer is expired
	 * @return true if this virtual tracer is expired, false otherwise
	 */
	public boolean isExpired() {
		return tracerState.get()==HARDDOWN;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualTracerMBean#getTimeToExpiry()
	 */
	@Override
	public long getTimeToExpiry() {
		if(isExpired()) return -1L;
		long d = System.currentTimeMillis()-touched.get();			
		return timeoutPeriod - d;			
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.Delayed#getDelay(java.util.concurrent.TimeUnit)
	 */
	@Override
	public long getDelay(TimeUnit unit) {
		return unit.convert(getTimeToExpiry(), TimeUnit.MILLISECONDS);
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.virtual.VirtualTracerMBean#getState()
	 */
	@Override
	public VirtualState getState() {
		return tracerState.get();
	}
	

	

	/**
	 * {@inheritDoc}
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Delayed otherDelayed) {
		long myDelay = getTimeToExpiry();
		long hisDelay = otherDelayed.getDelay(TimeUnit.MILLISECONDS);
		if(myDelay > hisDelay) return 1;
		if(myDelay < hisDelay) return -1;
		if(otherDelayed instanceof VirtualTracer) {
			return ((VirtualTracer)otherDelayed).serial > serial ? 1 : -1;
		}
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.delay.NotifyingDelay#setDelayChangeReceiver(org.helios.apmrouter.collections.delay.DelayChangeReceiver)
	 */
	@Override
	public void setDelayChangeReceiver(DelayChangeReceiver<NotifyingDelay> receiver) {
		this.receiver = receiver;		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.delay.NotifyingDelay#clearDelayChangeReceiver()
	 */
	@Override
	public void clearDelayChangeReceiver() {
		tracerState.set(VirtualState.SOFTDOWN);
		receiver = null;		
	}
	
	/**
	 * {@inheritDoc}
	 * <p>Since this is a virtual agent, it uses a simple submit and is asynchronous.</p>
	 * @see org.helios.apmrouter.trace.MetricSubmitter#submitDirect(org.helios.apmrouter.metric.IMetric, long)
	 */
	@Override
	public void submitDirect(IMetric metric, long timeout) throws TimeoutException {
		touch();
		this.submitter.submit(metric);		
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.MetricSubmitter#submit(java.util.Collection)
	 */
	@Override
	public void submit(Collection<IMetric> metrics) {
		touch();
		this.submitter.submit(metrics);				
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.MetricSubmitter#submit(org.helios.apmrouter.metric.IMetric[])
	 */
	@Override
	public void submit(IMetric... metrics) {
		touch();
		this.submitter.submit(metrics);		
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
	 * Returns the tracer name
	 * @return the name
	 */
	public String getName() {
		return name;
	}

}
