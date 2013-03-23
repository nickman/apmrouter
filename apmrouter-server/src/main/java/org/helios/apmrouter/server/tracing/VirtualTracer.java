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

import java.util.Collection;
import java.util.Date;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ObjectName;

import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.trace.MetricSubmitter;
import org.helios.apmrouter.trace.TracerImpl;

/**
 * <p>Title: VirtualTracer</p>
 * <p>Description: A tracer allocated for in-vm server-side virtual agents</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.tracing.VirtualTracer</code></p>
 */

public class VirtualTracer extends TracerImpl implements Delayed, VirtualTracerMBean {
	/** the assigned virtual agent serial */
	protected final long serial;
	/** The JMX ObjectName for this virtual agent */
	protected final ObjectName objectName;
	/** The virtual metric submitter */
	protected final VirtualMetricSubmitter vsubmitter;
	
	
	/**
	 * Creates a new VirtualTracer
	 * @param host The tracer's host
	 * @param agent The tracer's agent
	 * @param submitter The metric submitter
	 * @param serial the assigned virtual agent serial
	 * @param timeoutPeriod The timeout period in ms. which is the period of time without any submissions coming through this virtual agent that it is considered down.
	 */
	public VirtualTracer(String host, String agent, MetricSubmitter submitter, long serial, long timeoutPeriod) {		
		super(host, agent, new VirtualMetricSubmitter(submitter, timeoutPeriod, host, agent));
		vsubmitter = (VirtualMetricSubmitter)super.submitter;
		vsubmitter.vt = this;
		this.serial = serial;		
	    objectName = JMXHelper.objectName(String.format(VA_OBJ_NAME, host, agent));	
		try {
			JMXHelper.getHeliosMBeanServer().registerMBean(this, objectName);
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.VirtualTracerMBean#getLastTouchTimestamp()
	 */
	@Override
	public long getLastTouchTimestamp() {
		return vsubmitter.touched.get();
	}
	
	/**
	 * Returns this virtual agent's JMX object name
	 * @return this virtual agent's JMX object name
	 */
	public ObjectName getObjectName() {
		return objectName;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.VirtualTracerMBean#getLastTouchDate()
	 */
	@Override
	public Date getLastTouchDate() {
		return new Date(vsubmitter.touched.get());
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.VirtualTracerMBean#getSerial()
	 */
	@Override
	public long getSerial() {
		return serial;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.VirtualTracerMBean#touch()
	 */
	@Override
	public void touch() {
		vsubmitter.touch();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.VirtualTracerMBean#expire()
	 */
	@Override
	public void expire() {		
		if(vsubmitter.expired.compareAndSet(false, true)) {
			vsubmitter.touched.set(-1);
		}
	}
	
	/**
	 * Determines if the passed metric represents an availability down for the represented virtual agent
	 * @param metric The metric to inspect 
	 * @return true if the passed metric represents an availability down for the represented virtual agent, false otherwise
	 */
	protected boolean isAvailabilityDownMetric(IMetric metric) {
		return (
				metric.getNamespaceSize()==0 &&
				metric.getType().isLong() && 
				metric.getName().equalsIgnoreCase("Availability") &&
				metric.getLongValue()==0 
		);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.VirtualTracerMBean#isExpired()
	 */
	@Override
	public boolean isExpired() {
		return vsubmitter.expired.get();
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Delayed otherDelayed) {
		long myDelay = getDelay(TimeUnit.MILLISECONDS);
		long hisDelay = otherDelayed.getDelay(TimeUnit.MILLISECONDS);
		if(myDelay > hisDelay) return 1;
		if(myDelay < hisDelay) return -1;
		if(otherDelayed instanceof ServerTracerImpl) {
			return ((ServerTracerImpl)otherDelayed).serial > serial ? 1 : -1;
		}
		return 0;

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
	 * @see org.helios.apmrouter.server.tracing.VirtualTracerMBean#getTimeToExpiry()
	 */
	@Override
	public long getTimeToExpiry() {
		long t = vsubmitter.touched.get();
		if(t==-1) return 0;
		long d = System.currentTimeMillis()-t;			
		return vsubmitter.timeoutPeriod - d;			
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.TracerImpl#getSentMetrics()
	 */
	@Override
	public long getSentMetrics() {
		return vsubmitter.getSentMetrics();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.TracerImpl#getDroppedMetrics()
	 */
	@Override
	public long getDroppedMetrics() {
		return vsubmitter.getDroppedMetrics();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.TracerImpl#resetStats()
	 */
	@Override
	public void resetStats() {
		vsubmitter.resetStats();
	}

	/**
	 * <p>Title: VirtualMetricSubmitter</p>
	 * <p>Description: An expiry instrumented submitter for virtual agents</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.server.tracing.VirtualTracer.VirtualMetricSubmitter</code></p>
	 */
	protected static class VirtualMetricSubmitter implements MetricSubmitter {
		/** The submitter delegate */
		private final MetricSubmitter submitter;
		/** The count of submitted metrics for this virtual agent */
		protected final AtomicLong submittedMetrics = new AtomicLong(0L);
		/** The count of dropped metrics for this virtual agent */
		protected final AtomicLong droppedMetrics = new AtomicLong(0L);
		/** The last touch timestamp */
		protected final AtomicLong touched; 
		/** The timeout period */
		protected final long timeoutPeriod;
		/** Indicates if the virtual agent is expired */
		protected AtomicBoolean expired = new AtomicBoolean(false);	
		/** The virtual agent submitter's host */
		protected final String host;
		/** The virtual agent submitter's agent name */
		protected final String agent;
		/** The enclosing virtual tracer */
		protected VirtualTracer vt = null;
		
		/**
		 * Creates a new VirtualMetricSubmitter
		 * @param submitter The delegate submitter
		 * @param timeoutPeriod The virtual agent timeout period in ms.
		 * @param host The virtual agent submitter's host
		 * @param agent The virtual agent submitter's agent name
		 */
		public VirtualMetricSubmitter(MetricSubmitter submitter, long timeoutPeriod, String host, String agent) {
			super();
			this.submitter = submitter;
			this.touched = new AtomicLong(System.currentTimeMillis());
			this.timeoutPeriod = timeoutPeriod;
			this.host = host;
			this.agent = agent;
		}

		/**
		 * 
		 */
		public void touch() {
			if(expired.get()) throw new IllegalStateException("This virtual agent [" + host + ":" + agent + "] already expired", new Throwable());
			touched.set(System.currentTimeMillis());		
		}


		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.trace.MetricSubmitter#submitDirect(org.helios.apmrouter.metric.IMetric, long)
		 */
		@Override
		public void submitDirect(IMetric metric, long timeout)  {			
			touch();
			this.submitter.submit(metric);		
			if(metric!=null) submittedMetrics.incrementAndGet();
			if(vt.isAvailabilityDownMetric(metric)) vt.expire();
		}
		
		


		
		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.trace.MetricSubmitter#submit(java.util.Collection)
		 */
		@Override
		public void submit(Collection<IMetric> metrics) {
			touch();
			this.submitter.submit(metrics);				
			if(metrics!=null) submittedMetrics.addAndGet(metrics.size());
			for(IMetric metric: metrics) {
				if(vt.isAvailabilityDownMetric(metric)) {
					vt.expire();
					break;
				}
			}
		}


		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.trace.MetricSubmitter#submit(org.helios.apmrouter.metric.IMetric[])
		 */
		@Override
		public void submit(IMetric... metrics) {
			touch();
			this.submitter.submit(metrics);		
			if(metrics!=null) submittedMetrics.addAndGet(metrics.length);
			for(IMetric metric: metrics) {
				if(vt.isAvailabilityDownMetric(metric)) {
					vt.expire();
					break;
				}
			}
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.trace.MetricSubmitter#getSentMetrics()
		 */
		@Override
		public long getSentMetrics() {
			return submittedMetrics.get();
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.trace.TracerImpl#getDroppedMetrics()
		 */
		@Override
		public long getDroppedMetrics() {
			return droppedMetrics.get();
		}
		
		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.trace.TracerImpl#resetStats()
		 */
		@Override
		public void resetStats() {
			submittedMetrics.set(0L);
			droppedMetrics.set(0L);
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.trace.MetricSubmitter#getQueuedMetrics()
		 */
		@Override
		public long getQueuedMetrics() {
			return 0;
		}
		
	}
	
	
}
