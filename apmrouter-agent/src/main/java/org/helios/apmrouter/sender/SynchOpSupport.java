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
package org.helios.apmrouter.sender;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import org.helios.apmrouter.util.TimeoutQueueMap;

/**
 * <p>Title: SynchOpSupport</p>
 * <p>Description: Constructs to support synchronous operations with timeout.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.sender.SynchOpSupport</code></p>
 */

public class SynchOpSupport {
	/** The synchronous request timeout map */
	protected static final TimeoutQueueMap<Object, CountDownLatch> timeoutMap = new TimeoutQueueMap<Object, CountDownLatch>(2000);
	/** The synchronous request failure map */
	protected static final Map<Object, Byte> failMap = new ConcurrentHashMap<Object, Byte>();
	/** A serial number generator for server request Ids. */
	protected static final AtomicLong ridSerial = new AtomicLong();
	
	/** The fail code for a synch op fail */
	public static final byte OP_FAIL = 0;
	/** The fail code for a synch op success */
	public static final byte OP_SUCCESS = 1;
	
	/**
	 * Returns the next request id serial number
	 * @return the next request id serial number
	 */
	public static long nextRequestId() {
		return ridSerial.incrementAndGet();
	}
	
	/**
	 * Registers a synch op timeout
	 * @param key the operation key
	 * @param timeout The timeout in ms.
	 * @return the latch the synch waiter should await on
	 */
	public static CountDownLatch registerSynchOp(Object key, long timeout) {
		CountDownLatch latch = new CountDownLatch(1);
		if(timeout<1) {
			timeoutMap.put(key, latch);			
		} else {
			timeoutMap.put(key, latch, timeout);
		}
		return latch;
	}
	
	/**
	 * Registers a synch op timeout using gthe default timeout
	 * @param key the operation key
	 * @return the latch the synch waiter should await on
	 */
	public static CountDownLatch registerSynchOp(String key) {
		return registerSynchOp(key);
	}
	
	/**
	 * Cancels a pending latch, if it is found
	 * @param rid The request id
	 * @return true if the latch was found and counted down, false if the latch has already been removed
	 */
	public static boolean cancelLatch(Object rid) {
		CountDownLatch latch = timeoutMap.remove(rid);
		if(latch==null) return false;
		latch.countDown();
		cancelFail(rid);
		return true;
	}
	
	
	/**
	 * Cancels the fail flag for the passed request id
	 * @param rid the id of the request to cancel the fail flag for
	 * @return The removed fail code, or null if one was not found
	 */
	public static Byte cancelFail(Object rid) {
		return failMap.remove(rid);
	}
	
	/**
	 * Sets the fail code for the passed request id
	 * @param rid the request id to set the fail code for
	 * @param failCode the fail code to set
	 */
	public static void setFail(long rid, byte failCode) {
		failMap.put(rid, failCode);
	}


}
