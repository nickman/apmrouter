/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2013, Helios Development Group and individual contributors
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
package org.helios.collector.jmx.tracers;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.management.counter.Counter;
import sun.management.counter.Variability;

/**
 * <p>Title: ManagementCounterTracer</p>
 * <p>Description: An object tracer to trace lists of {@link sun.management.counter.Counter} instances</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.collector.jmx.tracers.ManagementCounterTracer</code></p>
 */

@SuppressWarnings("restriction")
public class ManagementCounterTracer implements IObjectTracer {
	/** Static class logger */
	protected static final Logger LOG = LoggerFactory.getLogger(ManagementCounterTracer.class);
	
	/**
	 * Creates a new ManagementCounterTracer
	 */
	public ManagementCounterTracer() {
		LOG.info("Created ManagementCounterTracer");
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.collector.jmx.tracers.IObjectTracer#trace(java.lang.Object)
	 */
	@Override
	public boolean trace(Object obj) {
		if(obj == null || !(obj instanceof List) ) return false;
		if(((List<?>)obj).size()<1) return false;
		if(!(((List<?>)obj).get(0) instanceof Counter)) return false;
		List<Counter> counters = (List<Counter>)obj;
		for(Counter counter: counters) {
			traceCounter(counter);
		}
		return true;
	}
	
	
	protected void traceCounter(Counter counter) {
		Variability type = counter.getVariability(); // INVALID, CONSTANT, MONOTONIC OR VARIABLE
		if(type==Variability.INVALID || type==Variability.CONSTANT) return;
		String name = counter.getName();		
		String units = counter.getUnits().toString();
		
		LOG.info("Counter Trace: Name:{}, Type:{}, Units:{}, Value:{}", new Object[]{name, type.toString(), units, counter.getValue()});
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.collector.jmx.tracers.IObjectTracer#getSegmentPrefix()
	 */
	@Override
	public String getSegmentPrefix() {
		return "management";
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.collector.jmx.tracers.IObjectTracer#getSegmentSuffix()
	 */
	@Override
	public String getSegmentSuffix() {
		return "counters";
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.collector.jmx.tracers.IObjectTracer#getMetricName()
	 */
	@Override
	public String getMetricName() {
		return "Delegate";
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.collector.jmx.tracers.IObjectTracer#prepareBindings(java.lang.Object[])
	 */
	@Override
	public void prepareBindings(Object... args) {
		LOG.info("prepareBindings {}", Arrays.toString(args));
	}

}
