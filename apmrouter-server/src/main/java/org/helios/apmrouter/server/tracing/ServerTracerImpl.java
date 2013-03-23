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
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ObjectName;

import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.trace.MetricSubmitter;
import org.helios.apmrouter.trace.TracerImpl;

/**
 * <p>Title: ServerTracerImpl</p>
 * <p>Description: A special tracer implementation that sends directly to the in-vm pattern router.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.tracing.ServerTracerImpl</code></p>
 */

public class ServerTracerImpl extends TracerImpl implements Delayed, VirtualAgentMXBean  {
	/** the assigned virtual agent serial */
	protected final long serial;
	/** The last touch timestamp */
	protected final AtomicLong touched; 
	/** The timeout period */
	protected final long timeoutPeriod;
	/** Indicates if the virtual agent is expired */
	protected AtomicBoolean expired = new AtomicBoolean(false);	
	/** The JMX ObjectName for this virtual agent */
	protected final ObjectName objectName;
	
	/** The ObjectName template for non-0-serial virtual agents */
	public static final String VA_OBJ_NAME = "org.helios.apmrouter.agent:type=VirtualAgent,host=%s,agent=%s";
	
	/**
	 * Creates a new ServerTracerImpl
	 * @param host The tracer's host
	 * @param agent The tracer's agent
	 * @param submitter The metric submitter
	 * @param serial the assigned virtual agent serial
	 * @param timeoutPeriod The timeout period in ms. which is the period of time without any submissions coming through this virtual agent that it is considered down.
	 */
	public ServerTracerImpl(String host, String agent, MetricSubmitter submitter, long serial, long timeoutPeriod) {
		super(host, agent, submitter);
		touched = serial==0 ? null : new AtomicLong(System.currentTimeMillis());
		this.serial = serial;		
		if(this.serial==0) {
			this.timeoutPeriod = -1L;
			objectName = null;			
		} else {
			this.timeoutPeriod = timeoutPeriod;
			objectName = JMXHelper.objectName(String.format(VA_OBJ_NAME, host, agent));	
			try {
				JMXHelper.registerMBean(objectName, this);
			} catch (Exception ex) {
				ex.printStackTrace(System.err);
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.TracerImpl#getHost()
	 */
	@Override
	public String getHost() {
		return host;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.VirtualAgentMXBean#getLastTouchTimestamp()
	 */
	@Override
	public long getLastTouchTimestamp() {
		return touched.get();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.VirtualAgentMXBean#getLastTouchDate()
	 */
	@Override
	public Date getLastTouchDate() {
		return new Date(touched.get());
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.TracerImpl#getAgent()
	 */
	@Override
	public String getAgent() {
		return agent;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.VirtualAgentMXBean#getSerial()
	 */
	@Override
	public long getSerial() {
		return serial;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.VirtualAgentMXBean#touch()
	 */
	@Override
	public void touch() {
		if(expired.get()) throw new IllegalStateException("This virtual agent [" + host + ":" + agent + "] already expired", new Throwable());
		touched.set(System.currentTimeMillis());		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.tracing.VirtualAgentMXBean#expire()
	 */
	@Override
	public void expire() {
		if(expired.compareAndSet(false, true)) {
			touched.set(-1);
			try {
				JMXHelper.getHeliosMBeanServer().unregisterMBean(objectName);
			} catch (Exception ex) {
				ex.printStackTrace(System.err);
			}
		}
	}

	
	/**
	 * Determines if this va is expired
	 * @return true if this va is expired, false otherwise
	 */
	public boolean isExpired() {
		return expired.get();
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
	 * @see org.helios.apmrouter.server.tracing.VirtualAgentMXBean#getTimeToExpiry()
	 */
	@Override
	public long getTimeToExpiry() {
		long d = System.currentTimeMillis()-touched.get();			
		return timeoutPeriod - d;			
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
	

}
