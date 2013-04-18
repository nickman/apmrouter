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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>Title: RepeatingEventHandler</p>
 * <p>Description: Utility class to handle repeating exceptions and log them only occassionally</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.util.RepeatingEventHandler</code></p>
 * @param <T> The expected type of the events to track
 */

public class RepeatingEventHandler<T> {
	/** A map of tracked exception types */
	protected final ConcurrentHashMap<T, CounterTimer> eventCounters = new ConcurrentHashMap<T, CounterTimer>();
	
	
	/**
	 * Registers a repeating event handler 
	 * @param t The event to track
	 * @param initialPasses The initial number of events to report
	 * @param countThreshold The maximum number of events without reporting
	 * @param timeThreshold The maximum elapsed time without reporting
	 * @param supressMessage The message generated when event supression starts
	 */
	public void register(T t, int initialPasses, int countThreshold, long timeThreshold, CharSequence supressMessage) {
		CounterTimer ct = new CounterTimer(initialPasses, countThreshold, timeThreshold, supressMessage);
		eventCounters.putIfAbsent(t, ct);
	}
	
	/**
	 * Returns the current count
	 * @param t The event to get the count for
	 * @return the current count
	 */
	public long getCount(T t) {
		if(t==null) return -1;
		CounterTimer ct = eventCounters.get(t);
		if(ct==null) return -1;
		return ct.getCount();		
	}
	
	/**
	 * Returns the configured initial for the passed event
	 * @param t The event to get the initial for
	 * @return the event initial
	 */
	public long getInitial(T t) {
		if(t==null) return -1;
		CounterTimer ct = eventCounters.get(t);
		if(ct==null) return -1;
		return ct.initialPasses;				
	}
	
	/**
	 * Returns the remaining number of initial passes for the passed event
	 * @param t The event to get the remaining for
	 * @return the remaining initial passes or zero
	 */
	public long getRemaining(T t) {
		if(t==null) return -1;
		CounterTimer ct = eventCounters.get(t);
		if(ct==null) return -1;
		return ct.getRemaining();				
	}
	/**
	 * Returns the supression message formated with the remaining number of initial passes
	 * @param t The event to get the remaining for
	 * @return the supression message formated with the remaining number of initial passes
	 */
	public String getRemainingMessage(T t) {
		if(t==null) return "No Repeating Handler for ["+ t + "]";
		CounterTimer ct = eventCounters.get(t);
		if(ct==null) return "No Repeating Handler for ["+ t + "]";
		return ct.getRemainingMessage();				
		
	}

	
	/**
	 * Returns the count threshold
	 * @param t The event to get the count threshold for
	 * @return the count threshold
	 */
	public long getCountThreshold(T t) {
		if(t==null) return -1;
		CounterTimer ct = eventCounters.get(t);
		if(ct==null) return -1;
		return ct.countThreshold;		
	}
	
	/**
	 * Resets the counter timer
	 * @param t The event to reset the counter timer
	 */
	public void reset(T t) {		
		CounterTimer ct = eventCounters.get(t);
		if(ct!=null) ct.reset();		
	}
	
	
	
	/**
	 * Indicates if the passed event should be handled
	 * @param t the event to test
	 * @return true to handle, false otherwise
	 */
	public boolean report(T t) {
		if(t==null) return false;
		CounterTimer ct = eventCounters.get(t);
		if(ct==null) return true;
		return ct.event();
	}
	
	private static class CounterTimer {
		/** Exception counter */
		final AtomicLong counter = new AtomicLong(0);
		/** Maximum elapsed time without a reported event */
		final long timeThreshold;
		/** The maximum number of exceptions to suppress before reporting */
		final int countThreshold;
		/** The initial number of exceptions that should be logged without a reset */
		final int initialPasses;
		/** The message generated when event supression starts */
		final String supressMessage;

		/** The time of the last reported event */
		final AtomicLong timer = new AtomicLong(0);
		
		/**
		 * Returns the number of remaining initial passes
		 * @return the number of remaining initial passes
		 */
		long getRemaining() {
			long r = initialPasses - counter.get();
			return r<0 ? 0 : r;
		}
		
		/**
		 * Returns the supression message formated with the remaining number of initial passes
		 * @return the supression message formated with the remaining number of initial passes
		 */
		String getRemainingMessage() {
			String msg = null;
			try {
				msg = String.format(supressMessage, getRemaining());
			} catch (Exception ex) {
				msg = "(Bad format:)" + supressMessage;
			}
			return msg;
		}
		
		/**
		 * Creates a new CounterTimer
		 * @param countThreshold The maximum number of exceptions to suppress before reporting
		 * @param timeThreshold The maximum elapsed time before reporting
		 * @param supressMessage The message generated when event supression starts
		 */
		CounterTimer(int initialPasses, int countThreshold, long timeThreshold, CharSequence message) {			
			this.timeThreshold = timeThreshold;
			this.initialPasses = initialPasses;
			this.countThreshold = countThreshold;
			this.supressMessage = message.toString();
		}
		
		/**
		 * Returns the current count
		 * @return the current count
		 */
		long getCount() {
			return counter.get();
		}
		
		void reset() {			
			timer.set(System.currentTimeMillis());
		}
		
		/**
		 * Ticks the counter. If the count or time thresholds have been met, returns true.
		 * @return true if either of the thresholds have been exceeded, false otherwise
		 */
		boolean event() {
			long tick = counter.incrementAndGet();
			if(tick<initialPasses) return true;
			long now = System.currentTimeMillis();
			if((tick > 0 && tick%countThreshold==0) || now-timer.get()>timeThreshold) {				
				reset();
				return true;
			}
			return false;
			
		}
	}
	
	
}
