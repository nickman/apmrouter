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

import org.helios.apmrouter.collections.ILongStack;
import org.helios.apmrouter.collections.UnsafeLongStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>Title: NestedSystemClock</p>
 * <p>Description: SystemClock timing collector that supports nested calls and detects skipped pops.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.util.NestedSystemClock</code></p>
 */

public class NestedSystemClock {
	/** Holds the start timestamp of an elapsed time measurement */
	private static final ThreadLocal<ILongStack> timerStart = new ThreadLocal<ILongStack>() {
		@Override
		protected ILongStack initialValue() {
			return new UnsafeLongStack();
		}
		@Override
		public void remove() {
			ILongStack stack = this.get();
			if(stack!=null) stack.destroy();
			super.remove();
		};
	};
	
	/**
	 * Starts a timer, returning the key that should be passed to retrieve the elapsed time
	 * for this specific start instance.
	 * @return the elapsed time retrieval key
	 */
	public static int startTimer() {
		final ILongStack stack = timerStart.get();
		stack.push(SystemClock.time());
		return stack.size();
	}
	
	/**
	 * Ends the current timer and returns an ElapedTime instance
	 * @param key The clock key provided when this timer was started
	 * @return an ElapsedTime instance
	 */
	public static ElapsedTime endTimer(int key) {		
		if(key<1) throw new IllegalArgumentException("The passed value [" + key + "] is an invalid clock key", new Throwable());		
		final long currentTime = SystemClock.time();
		final ILongStack stack = timerStart.get();		
		final int currentSize = stack.size();
		if(currentSize!=key) {
			if(key>currentSize) {
				timerStart.remove();
				throw new IllegalStateException("Provided key [" + key + "] was greater than the size of the timestamp stack [" + currentSize + "]. Stack has been destroyed.", new Throwable());
			}
			while(stack.size() > key) {
				stack.pop();
			}
		}
		final long startTime = stack.pop();
		if(stack.size()<1) timerStart.remove();
		return new ElapsedTime(startTime, currentTime);		
	}
	
	/**
	 * Returns the elapsed lap time based off the top start time in the start time stack
	 * @return a lap ElapsedTime instance
	 */
	public static ElapsedTime lapTimer() {
		final long currentTime = SystemClock.time();
		final ILongStack stack = timerStart.get();		
		final int currentSize = stack.size();
		if(currentSize<1) {
			throw new IllegalStateException("lapTimer called, but there is no start time in the stack.", new Throwable());
		}
		return new ElapsedTime(stack.peek(), currentTime);
	}
	
	/**
	 * Returns an elapsed lap time based on the indexed start time.
	 * Note that this is not the same value as the time key since it references the stack directly.
	 * To use a provided key, subtract 1 from that value.
	 * @param index The index to get the start time with
	 * @return a lap ElapsedTime instance
	 */
	public static ElapsedTime lapTimer(int index) {
		final long currentTime = SystemClock.time();
		final ILongStack stack = timerStart.get();		
		final int currentSize = stack.size();
		if(index>currentSize) {
			throw new IllegalStateException("Invalid lap time request. Provided index [" + index + "] was greater than the size of the timestamp stack [" + currentSize + "].", new Throwable());
		}
		return new ElapsedTime(stack.get(index), currentTime);
	}
	
	/**
	 * Returns the size of the elapsed time sack
	 * @return the size of the elapsed time sack
	 */
	public static int getCurrentStackSize() {
		final ILongStack stack = timerStart.get();
		if(stack.size()==0) {
			timerStart.remove();
			return 0;
		}
		return stack.size();
	}
	
	/**
	 * <p>Title: ElapsedTime</p>
	 * <p>Description: Encapsulates various values associated to a timer's elapsed time.</p> 
	 */
	public static class ElapsedTime {
		/** The start timestamp in ns. */
		public final long startNs;
		/** The end timestamp in ns. */
		public final long endNs;
		/** The elapsed time in ns. */
		public final long elapsedNs;
		/** The elapsed time in ms. */
		public final long elapsedMs;
		
	
		
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
		
		private ElapsedTime(long startTime, long endTime) {
			endNs = endTime;			
			startNs = startTime;
			elapsedNs = endNs - startNs;
			elapsedMs = TimeUnit.MILLISECONDS.convert(elapsedNs, TimeUnit.NANOSECONDS);
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
			return Math.round(d);
		}
		
		
		public String toString() {
			StringBuilder b = new StringBuilder("[");
			b.append(elapsedNs).append("] ns.");
			b.append(" / [").append(elapsedMs).append("] ms.");
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
	
		

}
