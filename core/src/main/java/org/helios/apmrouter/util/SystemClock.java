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
package org.helios.apmrouter.util;

import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>Title: SystemClock</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.util.SystemClock</code></p>
 */

public enum SystemClock {
	DIRECT(new DirectClock()),
	NANO(new NanoClock()),
	TEST(new TestClock());
	
	
	private SystemClock(Clock clock) {
		this.clock = clock;
	}
	
	private final Clock clock;
	
	public static long time() {
		return currentClock.get().getTime();
	}
	
	public static SystemClock currentClock() {
		return currentClock.get();
	}
	
	public static void setCurrentClock(SystemClock clock) {
		if(clock==null) throw new IllegalArgumentException("SystemClock cannot be set to null", new Throwable());
		currentClock.set(clock);
	}
	
	
	/**
	 * Returns the current time from this clock
	 * @return the current time
	 */
	public long getTime() {
		return clock.time();
	}
	
	/** The VM start time in ms. */
	public static final long startTime = ManagementFactory.getRuntimeMXBean().getStartTime();
	/** The test time */
	private static final AtomicLong testTime = new AtomicLong(0L);
	/** A reference to the current clock impl. */
	private static final AtomicReference<SystemClock> currentClock = new AtomicReference<SystemClock>(DIRECT);
	/** Holds the start timestamp of an elapsed time measurement */
	private static final ThreadLocal<long[]> timerStart = new ThreadLocal<long[]>() {
		protected long[] initialValue() {
			return new long[1];
		}
	};
	
	
	/**
	 * Starts an elapsed timer for the current thread
	 * @return the start time in ns.
	 */
	public static long startTimer() {
		long st = System.nanoTime();
		timerStart.get()[0] = st;
		return st;
	}
	
	public static ElapsedTime endTimer() {
		return ElapsedTime.newInstance(System.nanoTime());
	}
	
	public static ElapsedTime lapTimer() {
		return ElapsedTime.newInstance(true, System.nanoTime());
	}
	
	/**
	 * <p>Title: AggregatedElapsedTime</p>
	 * <p>Description: An aggregate for multiple {@link ElapsedTime}s</p> 
	 * <p>Company: ICE Futures US</p>
	 * @author Whitehead (nicholas.whitehead@theice.com)
	 * @version $LastChangedRevision$
	 * <p><code>org.helios.apmrouter.util.SystemClock.AggregatedElapsedTime</code></p>
	 */
	public static class AggregatedElapsedTime {
		/** The maximum elapsed time of the aggregate in ns. */
		public final long maxElapsedNs;
		/** The minimum elapsed time of the aggregate in ns. */
		public final long minElapsedNs;
		/** The average elapsed time of the aggregate in ns. */
		public final long avgElapsedNs;
		/** The maximum elapsed time of the aggregate in ms. */
		public final long maxElapsedMs;
		/** The minimum elapsed time of the aggregate in Ms. */
		public final long minElapsedMs;
		/** The average elapsed time of the aggregate in Ms. */
		public final long avgElapsedMs;
		
		/**
		 * Creates a new AggregatedElapsedTime
		 * @param maxElapsedNs The maximum elapsed time of the aggregate in ns.
		 * @param minElapsedNs The minimum elapsed time of the aggregate in ns.
		 * @param avgElapsedNs The average elapsed time of the aggregate in ns.
		 */
		private AggregatedElapsedTime(long maxElapsedNs, long minElapsedNs, long avgElapsedNs) {
			this.maxElapsedNs = maxElapsedNs;
			this.minElapsedNs = minElapsedNs;
			this.avgElapsedNs = avgElapsedNs;
			maxElapsedMs = TimeUnit.MILLISECONDS.convert(this.maxElapsedNs, TimeUnit.NANOSECONDS);
			minElapsedMs = TimeUnit.MILLISECONDS.convert(this.minElapsedNs, TimeUnit.NANOSECONDS);
			avgElapsedMs = TimeUnit.MILLISECONDS.convert(this.avgElapsedNs, TimeUnit.NANOSECONDS);
		}
		
		/**
		 * Aggregates a collection of {@link ElapsedTime}s 
		 * @param times The times to aggregate
		 * @return the aggregated elapsed time
		 */
		public static AggregatedElapsedTime aggregate(Collection<ElapsedTime> times) {
			return aggregate(times.toArray(new ElapsedTime[0]));
		}
		

		/**
		 * Aggregates an array of {@link ElapsedTime}s 
		 * @param times The times to aggregate
		 * @return the aggregated elapsed time
		 */
		public static AggregatedElapsedTime aggregate(ElapsedTime...times) {
			if(times==null || times.length<1) throw new IllegalArgumentException("Must pass at least 1 ElapsedTime to aggregate", new Throwable());
			long max = Long.MIN_VALUE;
			long min = Long.MAX_VALUE;
			BigDecimal total = new BigDecimal(0);
			long avg = 0;
			int cnt = times.length;
			for(ElapsedTime et: times) {
				total.add(BigDecimal.valueOf(et.elapsedNs));
				if(et.elapsedNs>max) max = et.elapsedNs;
				if(et.elapsedNs<min) min = et.elapsedNs;
			}
			avg = total.divide(BigDecimal.valueOf(cnt)).longValue();
			return new AggregatedElapsedTime(max, min, avg);
		}

		/**
		 * {@inheritDoc} 
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("AggregatedElapsedTime [maxElapsedNs=");
			builder.append(maxElapsedNs);
			builder.append(", minElapsedNs=");
			builder.append(minElapsedNs);
			builder.append(", avgElapsedNs=");
			builder.append(avgElapsedNs);
			builder.append(", maxElapsedMs=");
			builder.append(maxElapsedMs);
			builder.append(", minElapsedMs=");
			builder.append(minElapsedMs);
			builder.append(", avgElapsedMs=");
			builder.append(avgElapsedMs);
			builder.append("]");
			return builder.toString();
		}
		
		/**
		 * Returns the average elapsed time in ms. for the passed number of events
		 * @param cnt The number of events
		 * @return The average elapsed time in ms.
		 */
		public long avgMs(double cnt) {
			return _avg(avgElapsedMs, cnt);
		}
		
		/**
		 * Returns the maximum elapsed time in ms. for the passed number of events
		 * @param cnt The number of events
		 * @return The maximum elapsed time in ms.
		 */
		public long maxMs(double cnt) {
			return _avg(maxElapsedMs, cnt);
		}
		
		/**
		 * Returns the minimum elapsed time in ms. for the passed number of events
		 * @param cnt The number of events
		 * @return The minimum elapsed time in ms.
		 */
		public long minMs(double cnt) {
			return _avg(minElapsedMs, cnt);
		}
		
		/**
		 * Returns the average elapsed time in ns. for the passed number of events
		 * @param cnt The number of events
		 * @return The average elapsed time in ns.
		 */
		public long avgNs(double cnt) {
			return _avg(avgElapsedNs, cnt);
		}
		
		/**
		 * Returns the maximum elapsed time in ns. for the passed number of events
		 * @param cnt The number of events
		 * @return The maximum elapsed time in ns.
		 */
		public long maxNs(double cnt) {
			return _avg(maxElapsedNs, cnt);
		}
		
		/**
		 * Returns the minimum elapsed time in ns. for the passed number of events
		 * @param cnt The number of events
		 * @return The minimum elapsed time in ns.
		 */
		public long minNs(double cnt) {
			return _avg(minElapsedNs, cnt);
		}
		
		
		
		private long _avg(double time, double cnt) {
			if(time==0 || cnt==0 ) return 0L;
			double d = time/cnt;
			return (long)d;
		}
		
		
	}
	
	
	/**
	 * <p>Title: ElapsedTime</p>
	 * <p>Description: Encapsulates various values associated to a timer's elapsed time.</p> 
	 */
	public static class ElapsedTime {
		public final long startNs;
		public final long endNs;
		public final long elapsedNs;
		public final long elapsedMs;
		public volatile long lastLapNs = -1L;
		public volatile long elapsedSinceLastLapNs = -1L;
		public volatile long elapsedSinceLastLapMs = -1L;
		/** Holds the start last lap of an elapsed time measurement */
		private static final ThreadLocal<long[]> lapTime = new ThreadLocal<long[]>();
	
		static ElapsedTime newInstance(long endTime) {
			return newInstance(false, endTime);
		}
		
		static ElapsedTime newInstance(boolean lap, long endTime) {
			return new ElapsedTime(lap, endTime);
		}
		
		
		
		/** Some extended time unit entries */
		public static final Map<TimeUnit, String> UNITS;
		
		static {
			Map<TimeUnit, String> tmp = new HashMap<TimeUnit, String>();
			tmp.put(TimeUnit.DAYS, "days");
			tmp.put(TimeUnit.HOURS, "hrs.");
			tmp.put(TimeUnit.MICROSECONDS, "us.");
			tmp.put(TimeUnit.MILLISECONDS, "ms.");
			tmp.put(TimeUnit.MINUTES, "min.");
			tmp.put(TimeUnit.NANOSECONDS, "ns.");
			tmp.put(TimeUnit.SECONDS, "s.");
			UNITS = Collections.unmodifiableMap(tmp);
			
		}
		
		private ElapsedTime(boolean lap, long endTime) {
			endNs = endTime;
			startNs = timerStart.get()[0];
			long[] lastLapRead = lapTime.get();
			if(lastLapRead!=null) {
				lastLapNs = lastLapRead[0];
			}
			if(lap) {
				lapTime.set(new long[]{endTime});
			} else {
				timerStart.remove();
				lapTime.remove();
			}
			elapsedNs = endNs-startNs;
			elapsedMs = TimeUnit.MILLISECONDS.convert(elapsedNs, TimeUnit.NANOSECONDS);
			if(lastLapNs!=-1L) {
				elapsedSinceLastLapNs = endTime -lastLapNs;
				elapsedSinceLastLapMs = TimeUnit.MILLISECONDS.convert(elapsedSinceLastLapNs, TimeUnit.NANOSECONDS);
			}
			 
		}
		/**
		 * Returns the average elapsed time in ms. for the passed number of events
		 * @param cnt The number of events
		 * @return The average elapsed time in ms.
		 */
		public long avgMs(double cnt) {
			return _avg(elapsedMs, cnt);
		}
		
		/**
		 * Returns the average elapsed time in ns. for the passed number of events
		 * @param cnt The number of events
		 * @return The average elapsed time in ns.
		 */
		public long avgNs(double cnt) {
			return _avg(elapsedNs, cnt);
		}
		
		
		private long _avg(double time, double cnt) {
			if(time==0 || cnt==0 ) return 0L;
			double d = time/cnt;
			return (long)d;
		}
		
		
		public String toString() {
			StringBuilder b = new StringBuilder("[");
			b.append(elapsedNs).append("] ns.");
			b.append(" / [").append(elapsedMs).append("] ms.");
			if(elapsedSinceLastLapNs!=-1L) {
				b.append("  Elapsed Lap: [").append(elapsedSinceLastLapNs).append("] ns. / [").append(elapsedSinceLastLapMs).append("] ms.");
				
			}
			return b.toString();
		}
		
		public long elapsed() {
			return elapsed(TimeUnit.NANOSECONDS);
		}
		
		public long elapsed(TimeUnit unit) {
			if(unit==null) unit = TimeUnit.NANOSECONDS;
			return unit.convert(elapsedNs, TimeUnit.NANOSECONDS);
		}
		
		public String elapsedStr(TimeUnit unit) {
			if(unit==null) unit = TimeUnit.NANOSECONDS;
			return new StringBuilder("[").append(unit.convert(elapsedNs, TimeUnit.NANOSECONDS)).append("] ").append(UNITS.get(unit)).toString();
		}

		public String elapsedStr() {			
			return elapsedStr(TimeUnit.NANOSECONDS);
		}
		
	}
	
	
	/**
	 * <p>Title: Clock</p>
	 * <p>Description: Defines a clock impl.</p> 
	 */
	private static interface Clock {
		long time();
	}
	
	/**
	 * <p>Title: DirectClock</p>
	 * <p>Description: A clock implementation that simply returns {@code System.currentTimeMillis()}</p> 
	 */
	private static class DirectClock implements Clock {
		public long time() {
			return System.currentTimeMillis();
		}
	}
	
	private static class NanoClock implements Clock {		
		public long time() {
			return TimeUnit.MILLISECONDS.convert(System.nanoTime(), TimeUnit.NANOSECONDS) + startTime;
		}
	}
	
	private static class TestClock implements Clock {		
		public long time() {
			return testTime.get();
		}
	}
	
	public static long setTestTime(long time) {
		testTime.set(time);
		return time;
	}
	
	public static long setTestTime() {
		testTime.set(DIRECT.getTime());
		return testTime.get();
	}
	
	public static long tickTestTime() {
		return testTime.incrementAndGet();
	}

}
