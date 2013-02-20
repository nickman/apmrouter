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
package org.helios.apmrouter.destination.accumulator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.springframework.jmx.export.annotation.ManagedAttribute;

/**
 * <p>Title: TimeSizeFlushQueue</p>
 * <p>Description: A queue that is flushed when triggered by a size threshold and/or an elapsed time between flushes.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.accumulator.TimeSizeFlushQueue</code></p>
 * @param <T> The expected type of the queued item
 */

public class TimeSizeFlushQueue<T> implements Runnable {
	/** The default scheduler for instances not provided one */
	private static volatile ScheduledExecutorService defaultScheduler;
	/** The default threadPool for instances not provided one */
	private static volatile ExecutorService defaultThreadPool;
	/** Creation lock for the default scheduler */
	private static final Object schedulerLock = new Object();
	/** Creation lock for the default threadPool */
	private static final Object threadPoolLock = new Object();
	/** The name of the flushQueue */
	protected final String name;	
	/** The queue size threshold */
	protected final AtomicInteger sizeTrigger = new AtomicInteger(0);
	/** The elapsed time since last flush threshold in ms. */
	protected final AtomicLong timeTrigger = new AtomicLong(0);
	/** The flush queue */
	protected final BlockingQueue<T> queue;
	/** The flush queue runnable */
	protected final FlushQueueReceiver<T> receiver;
	/** The flush running lock */
	protected final ReentrantLock flushLock = new ReentrantLock(false);
	/** The scheduler for time triggered flushes */
	protected final ScheduledExecutorService scheduler;
	/** The thread pool for processing flushes */
	protected final ExecutorService flushThreadPool;
	/** The timer scheduled task handle */
	protected ScheduledFuture<?> handle = null;
	/** The elapsed time in ms. of the last flush */
	protected final AtomicLong lastFlushElapsed = new AtomicLong(0L);
	/** The total number of completed flushes */
	protected final AtomicLong flushCount = new AtomicLong(0L);
	/** The total number of flush exceptions */
	protected final AtomicLong flushExceptionCount = new AtomicLong(0L);
	/** The total number of queue drops on account of a full queue */
	protected final AtomicLong queueDropCount = new AtomicLong(0L);
	/** Indicates that the size and time configuration does not support buffering and the enqueue will be bypassed */
	protected final boolean bypassQueue;
	/** Instance logger */
	protected final Logger log;
	
	
	/** The flush buffer which is the queue drains to synchronously in the trigger thread */
	protected final Set<T> flushBuffer;
	/** static logger */
	protected static final Logger LOG = Logger.getLogger(TimeSizeFlushQueue.class);
	
	/**
	 * Creates the default scheduler
	 * @return a scheduler
	 */
 	private static ScheduledExecutorService getDefaultScheduler() {
		if(defaultScheduler==null) {
			synchronized(schedulerLock) {
				if(defaultScheduler==null) {
					defaultScheduler = Executors.newScheduledThreadPool(2, new ThreadFactory(){
						private final AtomicInteger serial = new AtomicInteger(0);
						private final ThreadGroup threadGroup = new ThreadGroup("TimeSizeFlushQueueSchedulerThreadGroup");
						@Override
						public Thread newThread(Runnable r) {
							Thread t = new Thread(threadGroup, r, "TimeSizeFlushQueueSchedulerThread#" + serial.incrementAndGet());
							t.setDaemon(true);
							return t;
						}
					});
				}
			}
		}
		return defaultScheduler;
	}
	
	/**
	 * Creates the default thread pool
	 * @return a thread pool
	 */
 	private static ExecutorService getDefaultExecutor() {
		if(defaultThreadPool==null) {
			synchronized(threadPoolLock) {
				if(defaultThreadPool==null) {
					defaultThreadPool = Executors.newFixedThreadPool(5, new ThreadFactory(){
						private final AtomicInteger serial = new AtomicInteger(0);
						private final ThreadGroup threadGroup = new ThreadGroup("TimeSizeFlushQueueThreadGroup");
						@Override
						public Thread newThread(Runnable r) {
							Thread t = new Thread(threadGroup, r, "TimeSizeFlushQueueThread#" + serial.incrementAndGet());
							t.setDaemon(true);
							return t;
						}
					});
				}
			}
		}
		return defaultScheduler;
	}
	
	
	
	/**
	 * Creates a new TimeSizeFlushQueue
	 * @param name The name for this flushQueue
	 * @param sizeTrigger The flush size trigger
	 * @param timeTrigger The flush time trigger
	 * @param receiver The receiver runnable responsible for processing the flush
	 * @param scheduler An externally provided scheduler. If null, uses the default shared scheduler.
	 * @param threadPool An externally provided thread pool. If null, uses the default shared pool. 
	 */
	public TimeSizeFlushQueue(String name, int sizeTrigger, long timeTrigger, FlushQueueReceiver<T> receiver, ScheduledExecutorService scheduler, ExecutorService threadPool) {
		this.name = name;
		log = Logger.getLogger(getClass().getName() + "." + this.name);
		this.sizeTrigger.set(sizeTrigger);
		this.timeTrigger.set(timeTrigger);
		bypassQueue = (sizeTrigger<2 && timeTrigger<1);
		if(!bypassQueue) {
			queue = new ArrayBlockingQueue<T>(sizeTrigger+2, false);
			flushBuffer = new HashSet<T>(sizeTrigger+2);
			this.scheduler = scheduler==null ? getDefaultScheduler() : scheduler;			
		} else {
			queue = null;
			flushBuffer = null;
			this.scheduler = null;
		}
		this.receiver = receiver;		
		this.flushThreadPool = threadPool==null ? getDefaultExecutor() : threadPool;
		schedule();
		log.info("Created TimeSizeFlushQueue [" + this.name + "]");
	}
	
	/**
	 * Creates a new TimeSizeFlushQueue using the default shared scheduler and thread pool.
	 * @param name The name for this flushQueue
	 * @param sizeTrigger The flush size trigger
	 * @param timeTrigger The flush time trigger in ms.
	 * @param receiver The receiver runnable responsible for processing the flush
	 */
	public TimeSizeFlushQueue(String name, int sizeTrigger, long timeTrigger, FlushQueueReceiver<T> receiver) {
		this(name, sizeTrigger, timeTrigger, receiver, null, null);
	}
	
	/**
	 * Schedules the time flush callback.
	 */
	protected void schedule() {
		long time = timeTrigger.get();
		if(time>0 && scheduler!=null) {
			handle = scheduler.schedule(new Runnable(){
				@Override
				public void run() {timeFlush();};
			}, time, TimeUnit.MILLISECONDS);
			if(log.isDebugEnabled()) log.debug("Scheduled for timed trigger in [" + time + "] ms.");
		}
	}
	
	/**
	 * Determines if the size threshold has been met for a flush.
	 * @return true if the size threshold has been met for a flush.
	 */
	protected boolean sizeTriggered() {
		int trig = sizeTrigger.get();
		if(trig<2) return false;
		return queue.size()>=trig;
	}
	
	/**
	 * Triggered when the size trigger is exceeded
	 */
	public void sizeFlush() {
		if(log.isDebugEnabled()) log.debug("Starting Size Triggered Flush");
		boolean acquiredLock = false;
		try {
			acquiredLock = flushLock.tryLock();
			if(!acquiredLock) {
				return;
			}
			if(handle!=null) handle.cancel(true);
			if(!queue.isEmpty()) {
				queue.drainTo(flushBuffer);
				flushThreadPool.execute(this);
			}
		} finally {
			try { 
				schedule(); 
			} catch (Exception e) {
				handle=null;
				LOG.fatal("Failed to reschedule timer trigger", e);
			}
			if(acquiredLock) {
				while(flushLock.getHoldCount()>0) {
					flushLock.unlock();
				}
			}
		}
	}
	
	/**
	 * Triggered when the flush time elapsed
	 */
	public void timeFlush() {
		if(log.isDebugEnabled()) log.debug("Starting Time Triggered Flush");
		boolean acquiredLock = false;
		try {
			acquiredLock = flushLock.tryLock();
			if(!acquiredLock) {
				return;
			}
			if(!queue.isEmpty()) {
				queue.drainTo(flushBuffer);
				flushThreadPool.execute(this);
			}
		} finally {
			try { 
				schedule(); 
			} catch (Exception e) {
				handle=null;
				LOG.fatal("Failed to reschedule timer trigger", e);
			}
			if(acquiredLock) {
				while(flushLock.getHoldCount()>0) {
					flushLock.unlock();
				}
			}
		}		
	}
	
	/**
	 * Executes the flush
	 */
	@Override
	public void run() {
		int itemsToFlush = flushBuffer.size();
		if(log.isDebugEnabled()) log.debug("Starting Flush of [" + itemsToFlush + "] items.");
		long start = System.currentTimeMillis();
		try {
			if(itemsToFlush>0) {
				receiver.flushTo(flushBuffer);
				flushBuffer.clear();
			} else {
				return;
			}			
		} catch (Exception e) {
			flushExceptionCount.incrementAndGet();
		} finally {
			lastFlushElapsed.set(System.currentTimeMillis()-start);
			flushCount.incrementAndGet();
		}
	}
	
	
	/**
	 * Calls the receiver with the passed items directly (i.e. not from the flushQueue)
	 * @param items A collection of items to flush
	 */
	protected void directRun(final Collection<T> items) {
		flushThreadPool.execute(new Runnable(){
			@Override
			public void run() {
				long start = System.currentTimeMillis();
				try {
					if(items.size()>0) {
						receiver.flushTo(items);
						items.clear();
					} else {
						return;
					}
				} catch (Exception e) {
					flushExceptionCount.incrementAndGet();
				} finally {
					lastFlushElapsed.set(System.currentTimeMillis()-start);
					flushCount.incrementAndGet();
				}				
			}
		});
	}

	/**
	 * Adds an item to the queue.
	 * @param t the item to add
	 * @return true if the item was successfully processed
	 * @see java.util.concurrent.BlockingQueue#add(java.lang.Object)
	 */
	public boolean add(T t) {
		if(t==null) return false;
		if(bypassQueue) {
			directRun(new ArrayList<T>(Arrays.asList(t)));
			return true;
		}
		boolean b = queue.add(t);
		if(sizeTriggered()) {
			sizeFlush();
		}
		return b;
	}

	/**
	 * Adds a collection of Ts to the queue
	 * @param tcoll The collection of Ts to add.
	 * @return true if the add succeeded, false if it did not.
	 * @see java.util.Collection#addAll(java.util.Collection)
	 */
	public boolean addAll(Collection<T> tcoll) {
		if(tcoll==null) return true;
		if(bypassQueue) {
			directRun(tcoll);
			return true;
		}		
		try {
			boolean b = queue.addAll(tcoll);
			if(!b) queueDropCount.addAndGet(tcoll.size());
			 if(sizeTriggered()) {
				 flushThreadPool.execute(new Runnable(){
					 @Override
					 public void run() {sizeFlush();};
				 });				 
			 }
			 return b;
		} catch (Exception e) {
			queueDropCount.addAndGet(tcoll.size());
			return false;
		}
	}

	/**
	 * Offers a T to the queue, waiting the defined time to insert if the queue is full.
	 * @param t The instance of T to offer
	 * @param waitTime The time to wait to insert if the queue is full
	 * @param unit The unit of time to wait
	 * @return true if the insert succeeded.
	 * @see java.util.concurrent.BlockingQueue#offer(java.lang.Object, long, java.util.concurrent.TimeUnit)
	 */
	public boolean offer(T t, long waitTime, TimeUnit unit) {
		if(t==null) return true;
		if(bypassQueue) {
			directRun(new ArrayList<T>(Arrays.asList(t)));
			return true;
		}		
		try {
			boolean b = queue.offer(t, waitTime, unit);
			if(!b) queueDropCount.incrementAndGet();
			 if(sizeTriggered()) {
				 flushThreadPool.execute(new Runnable(){
					 @Override
					 public void run() {sizeFlush();};
				 });				 
			 }
			 return b;
		} catch (Exception e) {
			queueDropCount.incrementAndGet();
			return false;
		}
	}

	/**
	 * Offers a T to the queue without waiting if the queue is full
	 * @param t The instance of T to offer
	 * @return true if the insert succeeded.
	 * @see java.util.concurrent.BlockingQueue#offer(java.lang.Object)
	 */
	public boolean offer(T t) {
		if(t==null) return true;
		if(bypassQueue) {
			directRun(new ArrayList<T>(Arrays.asList(t)));
			return true;
		}		
		try {
			boolean b = queue.offer(t);
			if(!b) queueDropCount.incrementAndGet();
			 if(sizeTriggered()) {
				 flushThreadPool.execute(new Runnable(){
					 @Override
					 public void run() {sizeFlush();};
				 });				 
			 }
			 return b;
		} catch (Exception e) {
			queueDropCount.incrementAndGet();
			return false;
		}		
	}

	/**
	 * Puts a T to the queue waiting if the queue is full
	 * @param t The instance of T to offer
	 * @throws InterruptedException Thrown if put is interrupted
	 * @see java.util.concurrent.BlockingQueue#put(java.lang.Object)
	 */
	public void put(T t) throws InterruptedException {
		if(t==null) return;
		if(bypassQueue) {
			directRun(new ArrayList<T>(Arrays.asList(t)));
			return;
		}		
		try {
			queue.put(t);
			 if(sizeTriggered()) {
				 flushThreadPool.execute(new Runnable(){
					 @Override
					 public void run() {sizeFlush();};
				 });				 
			 }
		} catch (Exception e) {
			queueDropCount.incrementAndGet();
		}		
	}

	/**
	 * Returns the size trigger threshold
	 * @return the sizeTrigger
	 */
	@ManagedAttribute(description="The size queue flush trigger")
	public int getSizeTrigger() {
		return sizeTrigger.get();
	}
	
	/**
	 * Sets the size trigger
	 * @param size the size to set the size triger
	 */
	public void setSizeTrigger(int size) {
		if(size<1) throw new IllegalArgumentException("Size cannot be less than one");
		sizeTrigger.set(size);
	}

	/**
	 * Returns the time trigger threshold in ms.
	 * @return the timeTrigger
	 */
	@ManagedAttribute(description="The time queue flush trigger in ms.")
	public long getTimeTrigger() {
		return timeTrigger.get();
	}
	
	/**
	 * Sets the time trigger in ms.
	 * @param time the time to set the time triger
	 */
	public void setTimeTrigger(long time) {
		if(time<1) throw new IllegalArgumentException("Time cannot be less than one");
		timeTrigger.set(time);
	}

	/**
	 * Returns the queue size
	 * @return the queue size
	 */
	@ManagedAttribute(description="The number of items in the queue")
	public int getQueueSize() {
		return queue.size();
	}

	/**
	 * Returns the type of the flush receiver
	 * @return the receiver class name
	 */
	@ManagedAttribute(description="The flush receiver type")
	public String getReceiverType() {
		return receiver.getClass().getName();
	}

	/**
	 * Returns the lock state of the flush lock
	 * @return true if the flushLock is locked
	 */
	@ManagedAttribute(description="The state of the flush lock")
	public boolean getFlushLockState() {
		return flushLock.isLocked();
	}

	/**
	 * @return the scheduler
	 */
	public ScheduledExecutorService getScheduler() {
		return scheduler;
	}

	/**
	 * @return the flushThreadPool
	 */
	public ExecutorService getFlushThreadPool() {
		return flushThreadPool;
	}

	/**
	 * Returns the elapsed time of the last flush (ms.)
	 * @return the lastFlushElapsed
	 */
	@ManagedAttribute(description="The elapsed time of the last flush (ms.)")
	public long getLastFlushElapsed() {
		return lastFlushElapsed.get();
	}

	/**
	 * Returns the total number of flush events
	 * @return the flushCount
	 */
	@ManagedAttribute(description="The number of flush events")
	public long getFlushCount() {
		return flushCount.get();
	}

	/**
	 * Returns the number of flush exceptions
	 * @return the flushExceptionCount
	 */
	@ManagedAttribute(description="The number of flush exceptions")
	public long getFlushExceptionCount() {
		return flushExceptionCount.get();
	}

	/**
	 * Returns the number of items dropped on account of a full flush queue
	 * @return the queueDropCount
	 */
	@ManagedAttribute(description="The number of items dropped on account of a full flush queue")
	public long getQueueDropCount() {
		return queueDropCount.get();
	}

	/**
	 * Returns this flushQueue's name
	 * @return the name
	 */
	@ManagedAttribute(description="The name of this flush queue")
	public String getName() {
		return name;
	}
	

}
