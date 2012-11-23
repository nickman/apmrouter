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
package org.helios.apmrouter.destination.h2timeseries;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>Title: RefQueueCleaner</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.h2timeseries.RefQueueCleaner</code></p>
 */

public class RefQueueCleaner {
	/** The reference queue to clear */
	private static final ReferenceQueue<UnsafeH2TimeSeries> refQueue = new ReferenceQueue<UnsafeH2TimeSeries>();
	
	/** The counter of created instances */
	private static final AtomicLong createdInstances = new AtomicLong(0L);
	/** The counter of cleared instances */
	private static final AtomicLong clearedInstances = new AtomicLong(0L);
	
	/** The ref queue clearing thread */
	private static final Thread refCleanerThread;
	
	
	static {
		refCleanerThread = new Thread(new Runnable(){
			public void run() {
				while(true) {
					try {
						Reference<? extends UnsafeH2TimeSeries> ref = refQueue.remove();
						UnsafeH2TimeSeries uts = ref.get();
						if(uts!=null) uts.destroy();
						clearedInstances.incrementAndGet();
					} catch (Exception ex) {
						
					}
				}
			}
		}, "RefQueueCleaner");
		refCleanerThread.setDaemon(true);
		refCleanerThread.start();
	}
	
	
	public static void createRef(UnsafeH2TimeSeries hts) {
		new SoftReference<UnsafeH2TimeSeries>(hts, refQueue);
		createdInstances.incrementAndGet();
	}


	/**
	 * Returns the count of created instances
	 * @return the created instance count
	 */
	public static long getCreatedinstances() {
		return createdInstances.get();
	}


	/**
	 * Returns the count of cleared instances
	 * @return the cleared instance count
	 */
	public static long getClearedinstances() {
		return clearedInstances.get();
	}
	
	
}
