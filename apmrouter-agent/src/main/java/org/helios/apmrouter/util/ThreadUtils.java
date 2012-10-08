/**
 * ICE Futures, US
 */
package org.helios.apmrouter.util;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>Title: ThreadUtils</p>
 * <p>Description: Thread utility methods</p> 
 * <p>Company: ICE Futures US</p>
 * @author Whitehead (nicholas.whitehead@theice.com)
 * @version $LastChangedRevision$
 * <p><code>org.helios.apmrouter.util.ThreadUtils</code></p>
 */

public class ThreadUtils {
	
	
	/**
	 * Watches a thread and collects instances of Locks the thread waits on
	 * @param thread The thread to watch
	 * @param maxTime The maximum time to watch for
	 * @param unit The unit of the max time
	 * @param maxInfos The maximum number of infos to process
	 * @return the collected lock infos
	 */
	public static LockInfos trackLocksForThread(final Thread thread, final long maxTime, final TimeUnit unit, final int maxInfos) {
		final LockInfos li = new LockInfos();
		final ThreadMXBean threadMX = ManagementFactory.getThreadMXBean();
		final long threadId = thread.getId();
		final long end = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(maxTime, unit);
		while(thread.isAlive()) {
			if(System.currentTimeMillis() >= end || li.processed>=maxInfos) break;
			ThreadInfo ti = threadMX.getThreadInfo(threadId);
			if(ti!=null) {
				li.addLockInfo(ti.getLockInfo());
			}
		}
		System.err.println(li);
		return li;
	}
	
	
	/**
	 * <p>Title: LockInfos</p>
	 * <p>Description: An accumulator of {@link LockInfo}s</p> 
	 * <p>Company: ICE Futures US</p>
	 * @author Whitehead (nicholas.whitehead@theice.com)
	 * @version $LastChangedRevision$
	 * <p><code>org.helios.apmrouter.util.LockInfos</code></p>
	 */
	public static class LockInfos {
		/** A map of lock infos keyed by the lock info's {@link LockInfo#getIdentityHashCode()} */
		protected final Map<String, Integer> lockInfos = new HashMap<String, Integer>();
		/** The number of lock infos processed */
		protected int processed = 0; 
		/**
		 * Adds a lock info
		 * @param info the lock info to add
		 */
		public void addLockInfo(LockInfo info) {
			if(info!=null) {
				if(lockInfos.size()<128) {					
					if(!lockInfos.containsKey(info.toString())) {
						lockInfos.put(info.toString(), 1);
					} else {
						lockInfos.put(info.toString(), lockInfos.get(info.toString())+1);
					}
					processed++;
				}
			}
		}
		
		public String toString() {
			StringBuilder b = new StringBuilder("Thread LockInfo [");
			for(Map.Entry<String, Integer> entry: lockInfos.entrySet()) {
				b.append("\n\t").append(entry.getKey()).append(":").append(entry.getValue());
			}
			b.append("\n]");
			return b.toString();
		}
	}
}
