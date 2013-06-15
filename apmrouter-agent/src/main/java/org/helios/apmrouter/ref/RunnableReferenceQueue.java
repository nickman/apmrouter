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
package org.helios.apmrouter.ref;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.management.ManagementFactory;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.helios.apmrouter.jmx.JMXHelper;

/**
 * <p>Title: RunnableReferenceQueue</p>
 * <p>Description: Singleton to manage the generic weak reference enqueue callback service.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.ref.RunnableReferenceQueue</code></p>
 */

public class RunnableReferenceQueue extends Thread implements RunnableReferenceQueueMBean, ThreadFactory,  RejectedExecutionHandler, UncaughtExceptionHandler  {
	/** The singleton reference */
	private static volatile RunnableReferenceQueue instance = null;
	/** The singleton ctor lock */
	private static final Object lock = new Object();
	
	/** The reference queue that weak references are enqueued into */
	private final ReferenceQueue<Object> refQueue = new ReferenceQueue<Object>(); 
	/** The thread pool work queue */
	private final BlockingQueue<Runnable> blockinqQueue = new LinkedBlockingQueue<Runnable>(); 
	/** A thread pool executor to run the actual callbacks */
	private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, CORES, 60, TimeUnit.SECONDS, blockinqQueue, this, this); 
	/** A counter of rejected executions */
	private final AtomicLong rejectedExecutions = new AtomicLong(0);
	/** A counter of uncaught exceptions */
	private final AtomicLong failedExecutions = new AtomicLong(0);
	
	/** A counter of cleared callbacks */
	private final AtomicLong completedCallbacks = new AtomicLong(0);
	/** A counter of submitted callbacks */
	private final AtomicLong submittedCallbacks = new AtomicLong(0);
	/** A serial number factory for worker threads */
	private final AtomicInteger serial = new AtomicInteger(0);
	
	/** The number of processors available to the JVM */
	public static final int CORES = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
	
	/**
	 * Acquires the RunnableReferenceQueue singleton instance
	 * @return the RunnableReferenceQueue singleton instance
	 */
	public static RunnableReferenceQueue getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new RunnableReferenceQueue();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Creates a new RunnableReferenceQueue
	 */
	private RunnableReferenceQueue() {
		this.setDaemon(true);
		this.setName("RunnableReferenceQueueThread");
		this.start();
		JMXHelper.registerMBean(JMXHelper.objectName(getClass().getPackage().getName(), "service", getClass().getSimpleName()), this);
	}
	
	public static void main(String[] args) {
		log("RunnableReferenceQueue Test");
		final Map<Long, WeakReference<byte[]>> m = new ConcurrentHashMap<Long, WeakReference<byte[]>>();
		Random r = new Random();		
		int loopCount = 10000;
		while(true) {
			for(int i = 0; i < loopCount; i++) {
				byte[] bytes = new byte[1000];
				final long key = r.nextLong();
				r.nextBytes(bytes);			
				m.put(key, RunnableReferenceQueue.getInstance().buildWeakReference(bytes, new Runnable(){
					@Override
					public void run() {
						m.remove(key);
					}
				}));
			}			
			log("M size:" + m.size());
			try { Thread.currentThread().join(1000); } catch (Exception x) {}
		}
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		while(true) {
			try {
				Reference<?> ref = refQueue.remove();
				if(ref instanceof RunnableWeakReference) {
					executor.submit((RunnableWeakReference<?>)ref);
					submittedCallbacks.incrementAndGet();
				}				
			} catch (Throwable t) {}
		}
	}
	

	/**
	 * Returns the cleaner work queue depth
	 * @return the cleaner work queue depth
	 */
	@Override
	public int getWorkQueueDepth() {
		return blockinqQueue.size();
	}
	
	/**
	 * Returns the cleaner work queue remaining capacity
	 * @return the cleaner work queue remaining capacity
	 */
	@Override
	public int getWorkQueueCapacity() {
		return blockinqQueue.remainingCapacity();
	}
	

	/**
	 * Returns the worker completed task count 
	 * @return the worker completed task count
	 */
	@Override
	public long getCompletedTaskCount() {
		return executor.getCompletedTaskCount();
	}
	
	/**
	 * Returns the number of active worker threads
	 * @return the number of active worker threads
	 */
	@Override
	public int getActiveWorkers() {
		return executor.getActiveCount();
	}
	
	/**
	 * Returns the current number of worker threads
	 * @return the current number of worker threads
	 */
	@Override
	public int getWorkerCount() {
		return executor.getPoolSize();
	}
	
	/**
	 * Returns the highwater number of worker threads
	 * @return the highwater number of worker threads
	 */
	@Override
	public int getHighwaterWorkerCount() {
		return executor.getLargestPoolSize();
	}
	
	

	/**
//	 * Returns the number of rejected executions
	 * @return the number of rejected executions
	 */
	@Override
	public long getRejectedExecutions() {
		return rejectedExecutions.get();
	}

	/**
	 * Returns the number of failed executions
	 * @return the number of failed executions
	 */
	@Override
	public long getFailedExecutions() {
		return failedExecutions.get();
	}

	/**
	 * Returns the number of completed callbacks
	 * @return the number of completed callbacks
	 */
	@Override
	public long getCompletedCallbacks() {
		return completedCallbacks.get();
	}

	/**
	 * Returns the number of submitted callbacks
	 * @return the number of submitted callbacks
	 */
	@Override
	public long getSubmittedCallbacks() {
		return submittedCallbacks.get();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.ref.RunnableReferenceQueueMBean#getPendingCallbacks()
	 */
	@Override
	public long getPendingCallbacks() {		
		return submittedCallbacks.get() - completedCallbacks.get(); 
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.RejectedExecutionHandler#rejectedExecution(java.lang.Runnable, java.util.concurrent.ThreadPoolExecutor)
	 */
	@Override
	public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
		System.err.println("[RunnableReferenceQueue] Rejected execution for enqueue callback [" + r + "]");
		rejectedExecutions.incrementAndGet();
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Thread.UncaughtExceptionHandler#uncaughtException(java.lang.Thread, java.lang.Throwable)
	 */
	@Override
	public void uncaughtException(Thread t, Throwable e) {
		failedExecutions.incrementAndGet();
		System.err.println("[RunnableReferenceQueue] Failed callback execution. Stack trace follows:");
		if(e!=null) e.printStackTrace(System.err);
	}
	

	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
	 */
	@Override
	public Thread newThread(final Runnable r) {
		Thread t = new Thread(r, "RunnableReferenceQueueWorkerThread#" + serial.incrementAndGet());
		t.setDaemon(true);
		return t;
	}
	
	
	/**
	 * Creates a new WeakReference for the passed referent and runnable callback
	 * @param t The reference
	 * @param r The on enqueue runnable callback
	 * @return a weak reference
	 */
	public <T> WeakReference<T> buildWeakReference(T t, Runnable r) {
		return new RunnableWeakReference<T>(t, r); 
	}

	
	/**
	 * <p>Title: RunnableWeakReference</p>
	 * <p>Description: A {@link WeakReference} extension that holds an enqueue runnable</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.ref.RunnableReferenceQueue.RunnableWeakReference</code></p>
	 */
	private class RunnableWeakReference<T> extends WeakReference<T> implements Runnable {
		/** The callback runnable */
		private final Runnable runnable;
		
		/**
		 * Creates a new RunnableWeakReference
		 * @param referent The referent
		 * @param runnable The callback runnable
		 */
		public RunnableWeakReference(T referent, Runnable runnable) {
			super(referent, refQueue);
			this.runnable = runnable;
		}
		
		/**
		 * {@inheritDoc}
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			try {
				if(runnable!=null) {
					runnable.run();
				}
			} finally {
				completedCallbacks.incrementAndGet();
				
			}
		}
		
	}




	
}
