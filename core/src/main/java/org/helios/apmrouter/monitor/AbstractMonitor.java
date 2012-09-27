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
package org.helios.apmrouter.monitor;

import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.helios.apmrouter.jmx.ScheduledThreadPoolFactory;
import org.helios.apmrouter.trace.ITracer;
import org.helios.apmrouter.trace.TracerFactory;
import org.helios.apmrouter.util.SystemClock;
import org.helios.apmrouter.util.SystemClock.ElapsedTime;

/**
 * <p>Title: AbstractMonitor</p>
 * <p>Description: Base class for monitor implementations</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.monitor.AbstractMonitor</code></p>
 */

public abstract class AbstractMonitor implements Monitor {
	/** The scheduler shared amongst all monitor instances */
	protected static final ScheduledThreadPoolExecutor scheduler = ScheduledThreadPoolFactory.newScheduler("Monitor");
	/** The tracer instance */
	protected final ITracer tracer = TracerFactory.getTracer();
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.monitor.Monitor#collect()
	 */
	@Override
	public void collect() {
		SystemClock.startTimer();
		doCollect();
		ElapsedTime et = SystemClock.endTimer();
		tracer.traceLong(et.elapsedMs, "ElpasedTimeMs", "Monitors", getClass().getSimpleName());
	}
	
	/**
	 * Directs a concrete monitor to collect and trace
	 */
	protected abstract void doCollect();
}
