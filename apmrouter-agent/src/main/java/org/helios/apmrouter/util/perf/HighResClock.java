/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2012, Helios Development Group and individual contributors
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
package org.helios.apmrouter.util.perf;

import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;

import org.helios.apmrouter.util.SystemClock.Clock;

import sun.misc.Perf;

/**
 * <p>Title: HighResClock</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.util.perf.HighResClock</code></p>
 */

public class HighResClock implements Clock {
	/** The JVM start time */
	private static final long START_DATE = ManagementFactory.getRuntimeMXBean().getStartTime();
	/** The JVM up time */
	private static final long START_TIME = ManagementFactory.getRuntimeMXBean().getUptime();
	
	/** The perf instance */	
	private static final Perf PERF = Perf.getPerf();
	/** The high res clock ticks per ms */
	private static final long TICK_FREQ = TimeUnit.SECONDS.convert(PERF.highResFrequency(), TimeUnit.MILLISECONDS);
	
	
	public static long millisSinceJVMStart() {
		return START_TIME + (PERF.highResCounter()/TICK_FREQ);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.util.SystemClock.Clock#time()
	 */
	@Override
	public long time() {
		return START_DATE + (PERF.highResCounter()/TICK_FREQ);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.util.SystemClock.Clock#unixTime()
	 */
	@Override
	public long unixTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.util.SystemClock.Clock#timeTick()
	 */
	@Override
	public long timeTick() {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.util.SystemClock.Clock#period(long)
	 */
	@Override
	public long period(long step) {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.util.SystemClock.Clock#period(long, long)
	 */
	@Override
	public long period(long step, long time) {
		// TODO Auto-generated method stub
		return 0;
	}

}
