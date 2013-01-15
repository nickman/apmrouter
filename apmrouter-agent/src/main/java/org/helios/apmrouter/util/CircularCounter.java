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

import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>Title: CircularCounter</p>
 * <p>Description: Atomic circular counter</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.util.CircularCounter</code></p>
 */

public class CircularCounter {
	/** The value returned after which the counter is reset */
	protected final long resetAt;
	/** The value that the counter is reset to */
	protected final long resetTo;
	/** The internal counter */
	private long counter;
	
	/**
	 * Creates a new CircularCounter
	 * @param startAt The first value returned by the counter
	 * @param resetAt The value returned after which the counter is reset
	 * @param resetTo The value that the counter is reset to
	 * @return The created counter
	 */
	public static CircularCounter newCounter(long startAt, long resetAt, long resetTo) {
		return new CircularCounter(startAt, resetAt, resetTo);
	}
	
	/**
	 * Creates a new CircularCounter starting at zero and resetting to zero
	 * @param startAt The first value returned by the counter
	 * @return The created counter
	 */
	public static CircularCounter newCounter(long startAt) {
		return new CircularCounter(startAt, 0L, 0L);
	}
	
	

	/**
	 * Creates a new CircularCounter
	 * @param startAt The first value returned by the counter
	 * @param resetAt The value returned after which the counter is reset
	 * @param resetTo The value that the counter is reset to
	 */
	protected CircularCounter(long startAt, long resetAt, long resetTo) {
		if(startAt>resetAt) throw new IllegalArgumentException("StartAt value [" + startAt + "] is greater than ResetAt value [" + resetAt + "]", new Throwable());
		if(resetTo>resetAt) throw new IllegalArgumentException("ResetTo value [" + resetTo + "] is greater than ResetAt value [" + resetAt + "]", new Throwable());
		counter = startAt;
		this.resetAt = resetAt;
		this.resetTo = resetTo;
	}
	
	/**
	 * Returns the next counter value, resetting the counter after increment if it has reached the <b><code>resetAt</code></b> value. 
	 * @return The next counter value
	 */
	public synchronized long next() {		
		long next = counter++;
		if(next==resetAt) {
			counter = resetTo;
		}
		return next;
	}
	
	
	
}
