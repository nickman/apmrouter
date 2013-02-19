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
package org.helios.collector.timeout;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import org.apache.log4j.Logger;
import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.jmx.ConfigurationHelper;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * <p>Title: ThreadWatcher</p>
 * <p>Description: A class that registers watches on a thread and performs a callback if the watch is not cancelled before the specified timeout.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
//@JMXManagedObject(annotated=true, declared=true)
//@JMXNotifications(notifications={
//		@JMXNotification(description="ThreadWatcher Timeout Notification", types={
//				@JMXNotificationType(type="org.helios.threadwatch.timeout")
//		})		
//})

@ManagedResource
public class ThreadWatcher implements ThreadFactory, ThreadWatchListener {
	/** The thread pool size for the scheduler */
	protected int poolSize = 0;
	/** The schduler used to schedule the check callback. */
	protected ScheduledThreadPoolExecutor scheduler = null;
	/** The map of watched threads */
	protected Map<Thread, ThreadWatchInstance> watchedThreads = new ConcurrentHashMap<Thread, ThreadWatchInstance>(100);
	/** Class logger */
	protected static Logger LOG = Logger.getLogger(ThreadWatcher.class);
	/** A serial number generator */
	protected static AtomicInteger serial = new AtomicInteger(0);
	/** The static singleton reference */
	protected static volatile ThreadWatcher threadWatcher = null;
	/** The singleton lock */
	protected static Object lock = new Object();
	
	/** The total number of stops  */
	protected AtomicLong stops = new AtomicLong(0);
	/** The total number of overruns  */
	protected AtomicLong overruns = new AtomicLong(0);
	/** The total overrun processing time */
	protected AtomicLong overrunProcessingTime = new AtomicLong(0);	
	/** The total number of watches */
	protected AtomicLong watches = new AtomicLong(0);
	
	/** The default listener */
	public static final ThreadWatchListener INTERUPTING_LISTENER = new Interruptor();
	/** A simple logging listener */
	public static final ThreadWatchListener LOGGING_LISTENER = new LoggingListener();
	/** The internal listener for maintaining counts*/
	protected static final InternalListener INTERNAL_LISTENER = new InternalListener();

	
	/** The system property or environmental variable that can be set to override the default scheduling thread pool size */
	protected static final String TP_POOL_SIZE_PROP = "org.helios.threadwatcher.poolsize";
	/** The default scheduling thread pool size */
	protected static final int DEFAULT_TP_POOL_SIZE = 2;
	/** The service's JMX ObjectName */
	protected static final ObjectName OBJECT_NAME = JMXHelper.objectName("org.helios:service=ThreadWatcher");
	
	protected static CompositeType ThreadWatchType = null;
	protected static TabularType ThreadWatchTableType = null;
	protected static final String[] ITEM_NAMES = new String[]{"WatchedThreadName", "WatchedThreadId", "Timeout", "ListenerCount"};
	protected static final String[] ITEM_DESCRIPTIONS = new String[]{"The thread name being watched", "The thread ID being watched", "The watch timeout", "The number of listeners registered"};
	protected static final OpenType[] ITEM_TYPES = new OpenType[]{SimpleType.STRING, SimpleType.LONG, SimpleType.LONG, SimpleType.INTEGER};
	
	static {
		try {
			ThreadWatchType = new CompositeType(
					ThreadWatchInstance.class.getName(), 
					"The internal data of a ThreadWatchInstance", 
					ITEM_NAMES, 
					ITEM_DESCRIPTIONS,
					ITEM_TYPES);
			ThreadWatchTableType = new TabularType("ThreadWatchTable", "A Tabular representation of current thread watches", ThreadWatchType, new String[]{"WatchedThreadId"});
		} catch (OpenDataException e) {
			e.printStackTrace();
		}		
	}
	
	
	/**
	 * Constructs the singleton ThreadWatcher 
	 */
	protected ThreadWatcher() {
		String tmp = ConfigurationHelper.getEnvThenSystemProperty(TP_POOL_SIZE_PROP, "" + DEFAULT_TP_POOL_SIZE);
		try { poolSize = Integer.parseInt(tmp); } catch (Exception e) {poolSize = DEFAULT_TP_POOL_SIZE;}
		scheduler = new ScheduledThreadPoolExecutor(poolSize, this);
		//scheduler.allowCoreThreadTimeOut(false);
		scheduler.prestartAllCoreThreads();
		try {
			JMXHelper.getHeliosMBeanServer().registerMBean(this, OBJECT_NAME);
		} catch (Exception e) {
			LOG.warn("Failed to register management interface. Continuing without.");
		}
		INTERNAL_LISTENER.setInternalListener(this);
		LOG.info("ThreadWatcher Service Started with [" + poolSize + "] scheduling threads.");
	}
	
	/**
	 * Acquires the singleton instance of the ThreadWatcher.
	 * @return the ThreadWatcher
	 */
	public static ThreadWatcher getInstance() {
		if(threadWatcher!=null) return threadWatcher;
		synchronized(lock) {
			if(threadWatcher!=null) return threadWatcher;
			threadWatcher = new ThreadWatcher();
		}
		return threadWatcher;
	}
	
	/**
	 * Puts a watch on the passed thread.
	 * @param timeout The timeout on the watch in ms. If the watch is not stopped by this time, the listeners will be called. 
	 * @param watchedThread The thread to watch.
	 * @param listeners An optional array of listeners.
	 * @throws InterruptedException
	 */
	public void watch(long timeout, Thread watchedThread, ThreadWatchListener...listeners) throws InterruptedException {
		if(watchedThreads.containsKey(watchedThread)) {
			throw new RuntimeException("There is an active watch on this thread [" + watchedThread + "]");
		}
		ThreadWatchInstance twi = new ThreadWatchInstance(watchedThreads, scheduler, timeout, watchedThread, listeners);
		watchedThreads.put(watchedThread, twi);
		watches.incrementAndGet();
	}
	
	/**
	 * Puts a watch on the current thread.
	 * @param timeout The timeout on the watch in ms. If the watch is not stopped by this time, the listeners will be called. 
	 * @param listeners An optional array of listeners.
	 * @throws InterruptedException
	 */
	public void watch(long timeout,ThreadWatchListener...listeners) throws InterruptedException {
		watch(timeout, Thread.currentThread(), listeners);
	}
	
	
	/**
	 * Stops the watch on this thread.
	 * @param watchedThread
	 */
	public void stop(Thread watchedThread) {
		ThreadWatchInstance twi = watchedThreads.get(watchedThread);
		if(twi!=null) {
			twi.cancel();
		}
		stops.incrementAndGet();
	}
	
	/**
	 * Stops the watch on the current thread.
	 */
	public void stop() {
		stop(Thread.currentThread());
	}
	
	/**
	 * @return
	 * @throws OpenDataException
	 */
	@ManagedAttribute
	public TabularData getThreadWatches() throws Exception {
		try {
			TabularDataSupport tds =  new TabularDataSupport(ThreadWatchTableType);
			for(ThreadWatchInstance twi: watchedThreads.values()) {
				tds.put(twi.getThreadWatchOpenType());
			}
			return tds;
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
	


	/**
	 * Constructs threads for the ThreadWatcher scheduling thread pool.
	 * @param r
	 * @return
	 * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
	 */
	public Thread newThread(Runnable r) {
		Thread t = new Thread(r, "ThreadWatcher-SchedulingThread#" + serial.incrementAndGet());
		t.setDaemon(true);
		t.setPriority(Thread.MAX_PRIORITY);
		return t;
	}
	
	static class Interruptor implements ThreadWatchListener {
		/**
		 * Interrupts the watched thread.
		 * @param watchedThread
		 * @param timeout
		 * @param stackEntry
		 * @param blockCount
		 * @param waitCount
		 * @see org.helios.collectors.timeout.ThreadWatchListener#onThreadOverrun(java.lang.Thread, long, java.lang.String, long, long)
		 */
		public void onThreadOverrun(Thread watchedThread, long timeout,
				String stackEntry, long blockCount, long waitCount) {
				if(LOG.isDebugEnabled()) LOG.debug("Interrupting timed out watched thread [" + watchedThread.getName() + "/" + watchedThread.getId() + "] after [" + timeout + "] ms.");
				watchedThread.interrupt();
		}
	}
	
	static class InternalListener implements ThreadWatchListener {
		protected ThreadWatchListener internalListener = null;
		public void setInternalListener(ThreadWatchListener internalListener) {
			this.internalListener = internalListener;
		}

		/**
		 * @param watchedThread
		 * @param elapsed time to process overrun.
		 * @param stackEntry
		 * @param blockCount
		 * @param waitCount
		 * @see org.helios.collectors.timeout.ThreadWatchListener#onThreadOverrun(java.lang.Thread, long, java.lang.String, long, long)
		 */
		public void onThreadOverrun(Thread watchedThread, long elapsed,
				String stackEntry, long blockCount, long waitCount) {
			internalListener.onThreadOverrun(watchedThread, elapsed, stackEntry, blockCount, waitCount);
		}
		
	}
	
	static class LoggingListener implements ThreadWatchListener {
		/**
		 * Logs the timeout if INFO is enabled and nothing else.
		 * @param watchedThread
		 * @param timeout
		 * @param stackEntry
		 * @param blockCount
		 * @param waitCount
		 * @see org.helios.collectors.timeout.ThreadWatchListener#onThreadOverrun(java.lang.Thread, long, java.lang.String, long, long)
		 */
		public void onThreadOverrun(Thread watchedThread, long timeout, String stackEntry, long blockCount, long waitCount) {
			if(LOG.isInfoEnabled()) {
				StringBuilder b = new StringBuilder("\n\t**************\n\tThreadWatcher Time Out.\n\t**************");
				b.append("\n\tWatched Thread Name:").append(watchedThread.getName());
				b.append("\n\tWatched Thread ID:").append(watchedThread.getId());
				b.append("\n\tTime Out:").append(timeout).append(" ms.");
				b.append("\n\tStack Trace Element:").append(stackEntry);
				b.append("\n\tWatched Thread Blocks:").append(blockCount);
				b.append("\n\tWatched Thread Waits:").append(waitCount);
				b.append("\n");				
				LOG.info(b);
			}
				
		}
	}

	/**
	 * The thread pool size
	 * @return the poolSize
	 */
	@ManagedAttribute
	public int getPoolSize() {
		return poolSize;
	}
	
	/**
	 * The current number of thread watches.
	 * @return
	 */
	@ManagedAttribute
	public int getCurrentWatches() {
		return watchedThreads.size();
	}
	
	/**
	 * @return
	 */
	@ManagedAttribute
	public long getTotalTasks() {
		return scheduler.getCompletedTaskCount();
	}

	/**
	 * @return the stops
	 */
	@ManagedAttribute
	public long getStops() {
		return stops.get();
	}

	/**
	 * @return the overruns
	 */
	@ManagedAttribute
	public long getOverruns() {
		return overruns.get();
	}

	/**
	 * @return the watches
	 */
	@ManagedAttribute
	public long getWatches() {
		return watches.get();
	}
	
	/**
	 * The average elapsed time for overrun processing. 
	 * @return
	 */
	@ManagedAttribute
	public long getAverageOverrunProcessingTime() {
		return avg(overrunProcessingTime.floatValue(), overruns.floatValue());
	}
	
	protected static long avg(float t, float c) {
		float a = t/c;
		return (long)a;
	}
	

	/**
	 * Insternal listener callback.
	 * @param watchedThread
	 * @param elapsed time to process overrun
	 * @param stackEntry
	 * @param blockCount
	 * @param waitCount
	 * @see org.helios.collectors.timeout.ThreadWatchListener#onThreadOverrun(java.lang.Thread, long, java.lang.String, long, long)
	 */
	public void onThreadOverrun(Thread watchedThread, long elapsed,
			String stackEntry, long blockCount, long waitCount) {
		overruns.incrementAndGet();
		overrunProcessingTime.addAndGet(elapsed);
	}
	
	

}

class ThreadWatchInstance implements Runnable {
	protected ScheduledFuture<?> future = null;
	protected ThreadWatchListener[] listeners = null;
	protected Thread watchedThread = null;
	protected long initialBlocks = 0;
	protected long initialWaits = 0;
	protected long timeout = 0;
	protected Map<Thread, ThreadWatchInstance> watchedThreads = null;
	

	protected static ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
	
	/**
	 * @return
	 * @throws OpenDataException
	 */
	protected CompositeData getThreadWatchOpenType() throws OpenDataException {
		return new CompositeDataSupport(
				ThreadWatcher.ThreadWatchType, 
				ThreadWatcher.ITEM_NAMES, 
				new Object[]{watchedThread.getName(), watchedThread.getId(), timeout, listeners.length});
	}
	
	/**
	 * Creates a new ThreadWatchInstance.
	 * @param the timeout of the watcher.
	 * @param future The scheudled task to be used to cancel the watch.
	 * @param listeners The listeners to be called back if the watch times out.
	 * @param watchedThread The thread to be watched.
	 */
	public ThreadWatchInstance(Map<Thread, ThreadWatchInstance> watchedThreads, ScheduledThreadPoolExecutor scheduler, long timeout, Thread watchedThread, ThreadWatchListener...listeners) {
		ThreadInfo ti = threadMXBean.getThreadInfo(watchedThread.getId());
		initialBlocks = ti.getBlockedCount();
		initialWaits = ti.getWaitedCount();
		future = scheduler.schedule(this, timeout, TimeUnit.MILLISECONDS);
		this.timeout = timeout;
		this.listeners = listeners;
		this.watchedThread = watchedThread;
		if(this.listeners==null || this.listeners.length<1) {
			this.listeners = new ThreadWatchListener[]{ThreadWatcher.INTERUPTING_LISTENER};
		}
		this.watchedThreads = watchedThreads;
	}
	
	/**
	 * Cancels the watch task.
	 */
	public void cancel() {
		watchedThreads.remove(watchedThread);
		future.cancel(true);
	}

	/**
	 * Collects the block/wait count elapsed during the watch phase, the top stack element of the watched trace and invokes the callback on each listener. 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		try {
			watchedThreads.remove(watchedThread);
			long start = System.currentTimeMillis();
			ThreadInfo ti = threadMXBean.getThreadInfo(watchedThread.getId(), 2);
			initialBlocks = ti.getBlockedCount()-initialBlocks;
			initialWaits = ti.getWaitedCount()-initialWaits;
			StackTraceElement[] stack = ti.getStackTrace();
			String stackElement = "";
			if(stack!=null && stack.length>0) {
				stackElement=stack[0].getClassName() + "." + stack[0].getMethodName();
			}
			for(ThreadWatchListener listener: listeners) {
				listener.onThreadOverrun(watchedThread, timeout, stackElement, initialBlocks, initialWaits);
			}
			long elapsed = System.currentTimeMillis()-start;
			ThreadWatcher.INTERNAL_LISTENER.onThreadOverrun(watchedThread, elapsed, stackElement, initialBlocks, initialWaits);
			listeners = null;
		} catch (Exception ie) {
			watchedThreads.remove(watchedThread);
			listeners = null;
		}
		
	}
	
	
}
