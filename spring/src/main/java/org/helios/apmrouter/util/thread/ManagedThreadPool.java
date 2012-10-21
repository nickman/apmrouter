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
package org.helios.apmrouter.util.thread;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.server.ServerComponentBean;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.export.naming.ObjectNamingStrategy;

/**
 * <p>Title: ManagedThreadPool</p>
 * <p>Description: A JMX exposed thread pool</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.util.thread.ManagedThreadPool</code></p>
 */
@ManagedResource
public class ManagedThreadPool extends ServerComponentBean implements ExecutorService, ObjectNamingStrategy, ThreadFactory, RejectedExecutionHandler, UncaughtExceptionHandler {
	/** The delegate pool */
	protected ThreadPoolExecutor inner;
	/** The thread group owning this pool's threads */
	protected ThreadGroup threadGroup;
	/** The initial config for this pool */
	protected final ThreadPoolConfig tpc;
	/** A serial number factory for created threads */
	protected final AtomicLong threadNameSerial = new AtomicLong(0L);
	
    /**
      * Creates a new {@code ThreadPoolExecutor} with the parameters taken from a {@link ThreadPoolConfig} instance.
      * @param tpc the ThreadPoolConfig instance to configure this pool
      */
	public ManagedThreadPool(ThreadPoolConfig tpc) {
		this.tpc = tpc;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.springframework.jmx.export.naming.ObjectNamingStrategy#getObjectName(java.lang.Object, java.lang.String)
	 */
	@Override
	public ObjectName getObjectName(Object managedBean, String beanKey) throws MalformedObjectNameException {
		objectName = JMXHelper.objectName(new StringBuilder(getClass().getPackage().getName()).append(":service=ThreadPool,name=").append(beanName));
		return objectName;
	}

	
	/**
	 * <p>Configures and starts the thread pool.
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStart()
	 */
	@Override
	protected void doStart() throws Exception {
		threadGroup = new ThreadGroup(beanName);
		
		inner = new ThreadPoolExecutor(
					tpc.corePoolSize, 
					tpc.maximumPoolSize, 
					tpc.keepAliveTime, 
					TimeUnit.MILLISECONDS, 
					tpc.queueSize==1 
					? new SynchronousQueue<Runnable>(false)
					: new ArrayBlockingQueue<Runnable>(tpc.queueSize, tpc.fairQueue),
					this, this);
		inner.allowCoreThreadTimeOut(tpc.coreThreadsTimeout);
		for(int i = 0; i < tpc.coreThreadsStarted; i++) {
			if(!inner.prestartCoreThread()) break;
		}
		setEventExecutor(this);
	}
	
	/**
	 * <p>Stops the thread pool
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStop()
	 */
	@Override
	protected void doStop() {
		int i = inner.shutdownNow().size();		
		info("Stopped with [", i, "] tasks in flight");
	}	
	
	/**
	 * Sets the spring event executor 
	 * @param eventExecutor the eventExecutor to set
	 */
	@Override
	public void setEventExecutor(Executor eventExecutor) {
		this.eventExecutor = eventExecutor;
		if(eventMulticaster!=null) {
			eventMulticaster.setTaskExecutor(eventExecutor);
		}
	}	
	
	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.RejectedExecutionHandler#rejectedExecution(java.lang.Runnable, java.util.concurrent.ThreadPoolExecutor)
	 */
	@Override
	public void rejectedExecution(Runnable r, ThreadPoolExecutor tpe) {
		error("Rejected Execution [", r + "]");
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Thread.UncaughtExceptionHandler#uncaughtException(java.lang.Thread, java.lang.Throwable)
	 */
	@Override
	public void uncaughtException(Thread thread, Throwable t) {
		error("Uncaught Exception on thread [", thread,  "]", t);
	}	

	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
	 */
	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(threadGroup, r, beanName + "Thread#" + threadNameSerial.incrementAndGet());
		t.setUncaughtExceptionHandler(this);
		t.setDaemon(true);		
		return t;
	}	

	/**
	 * @param value
	 * @see java.util.concurrent.ThreadPoolExecutor#allowCoreThreadTimeOut(boolean)
	 */
	@ManagedOperation
	public void allowCoreThreadTimeOut(boolean value) {
		inner.allowCoreThreadTimeOut(value);
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#allowsCoreThreadTimeOut()
	 */
	@ManagedAttribute
	public boolean allowsCoreThreadTimeOut() {
		return inner.allowsCoreThreadTimeOut();
	}

	/**
	 * @param timeout
	 * @param unit
	 * @return
	 * @throws InterruptedException
	 * @see java.util.concurrent.ThreadPoolExecutor#awaitTermination(long, java.util.concurrent.TimeUnit)
	 */
	@Override
	@ManagedOperation
	public boolean awaitTermination(long timeout, TimeUnit unit)
			throws InterruptedException {
		return inner.awaitTermination(timeout, unit);
	}

	/**
	 * @param command
	 * @see java.util.concurrent.ThreadPoolExecutor#execute(java.lang.Runnable)
	 */
	@Override
	@ManagedOperation
	public void execute(Runnable command) {
		inner.execute(command);
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getActiveCount()
	 */
	@ManagedAttribute
	public int getActiveCount() {
		return inner.getActiveCount();
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getCompletedTaskCount()
	 */
	@ManagedAttribute
	public long getCompletedTaskCount() {
		return inner.getCompletedTaskCount();
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getCorePoolSize()
	 */
	@ManagedAttribute
	public int getCorePoolSize() {
		return inner.getCorePoolSize();
	}

	/**
	 * @param unit
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getKeepAliveTime(java.util.concurrent.TimeUnit)
	 */
	public long getKeepAliveTime(TimeUnit unit) {
		return inner.getKeepAliveTime(unit);
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getLargestPoolSize()
	 */
	@ManagedAttribute
	public int getLargestPoolSize() {
		return inner.getLargestPoolSize();
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getMaximumPoolSize()
	 */
	@ManagedAttribute
	public int getMaximumPoolSize() {
		return inner.getMaximumPoolSize();
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getPoolSize()
	 */
	@ManagedAttribute
	public int getPoolSize() {
		return inner.getPoolSize();
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getQueue()
	 */
	public BlockingQueue<Runnable> getQueue() {
		return inner.getQueue();
	}
	
	@ManagedAttribute
	public int getQueueSize() {
		return inner.getQueue().size();
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getRejectedExecutionHandler()
	 */
	public RejectedExecutionHandler getRejectedExecutionHandler() {
		return inner.getRejectedExecutionHandler();
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getTaskCount()
	 */
	@ManagedAttribute
	public long getTaskCount() {
		return inner.getTaskCount();
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#getThreadFactory()
	 */
	public ThreadFactory getThreadFactory() {
		return inner.getThreadFactory();
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @return
	 * @throws InterruptedException
	 * @see java.util.concurrent.AbstractExecutorService#invokeAll(java.util.Collection, long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public <T> List<Future<T>> invokeAll(
			Collection<? extends Callable<T>> arg0, long arg1, TimeUnit arg2)
			throws InterruptedException {
		return inner.invokeAll(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @return
	 * @throws InterruptedException
	 * @see java.util.concurrent.AbstractExecutorService#invokeAll(java.util.Collection)
	 */
	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> arg0)
			throws InterruptedException {
		return inner.invokeAll(arg0);
	}

	/**
	 * @param tasks
	 * @param timeout
	 * @param unit
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws TimeoutException
	 * @see java.util.concurrent.AbstractExecutorService#invokeAny(java.util.Collection, long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks,
			long timeout, TimeUnit unit) throws InterruptedException,
			ExecutionException, TimeoutException {
		return inner.invokeAny(tasks, timeout, unit);
	}

	/**
	 * @param arg0
	 * @return
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @see java.util.concurrent.AbstractExecutorService#invokeAny(java.util.Collection)
	 */
	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> arg0)
			throws InterruptedException, ExecutionException {
		return inner.invokeAny(arg0);
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#isShutdown()
	 */
	@Override
	@ManagedAttribute
	public boolean isShutdown() {
		return inner.isShutdown();
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#isTerminated()
	 */
	@Override
	@ManagedAttribute
	public boolean isTerminated() {
		return inner.isTerminated();
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#isTerminating()
	 */
	@ManagedAttribute
	public boolean isTerminating() {
		return inner.isTerminating();
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#prestartAllCoreThreads()
	 */
	@ManagedOperation
	public int prestartAllCoreThreads() {
		return inner.prestartAllCoreThreads();
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#prestartCoreThread()
	 */
	@ManagedOperation
	public boolean prestartCoreThread() {
		return inner.prestartCoreThread();
	}

	/**
	 * 
	 * @see java.util.concurrent.ThreadPoolExecutor#purge()
	 */
	@ManagedOperation
	public void purge() {
		inner.purge();
	}

	/**
	 * @param task
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#remove(java.lang.Runnable)
	 */
	public boolean remove(Runnable task) {
		return inner.remove(task);
	}

	/**
	 * @param corePoolSize
	 * @see java.util.concurrent.ThreadPoolExecutor#setCorePoolSize(int)
	 */
	@ManagedAttribute
	public void setCorePoolSize(int corePoolSize) {
		inner.setCorePoolSize(corePoolSize);
	}

	/**
	 * @param time
	 * @param unit
	 * @see java.util.concurrent.ThreadPoolExecutor#setKeepAliveTime(long, java.util.concurrent.TimeUnit)
	 */
	public void setKeepAliveTime(long time, TimeUnit unit) {
		inner.setKeepAliveTime(time, unit);
	}

	/**
	 * @param maximumPoolSize
	 * @see java.util.concurrent.ThreadPoolExecutor#setMaximumPoolSize(int)
	 */
	@ManagedAttribute
	public void setMaximumPoolSize(int maximumPoolSize) {
		inner.setMaximumPoolSize(maximumPoolSize);
	}

	/**
	 * @param handler
	 * @see java.util.concurrent.ThreadPoolExecutor#setRejectedExecutionHandler(java.util.concurrent.RejectedExecutionHandler)
	 */
	public void setRejectedExecutionHandler(RejectedExecutionHandler handler) {
		inner.setRejectedExecutionHandler(handler);
	}

	/**
	 * @param threadFactory
	 * @see java.util.concurrent.ThreadPoolExecutor#setThreadFactory(java.util.concurrent.ThreadFactory)
	 */
	public void setThreadFactory(ThreadFactory threadFactory) {
		inner.setThreadFactory(threadFactory);
	}

	/**
	 * 
	 * @see java.util.concurrent.ThreadPoolExecutor#shutdown()
	 */
	@Override
	@ManagedOperation
	public void shutdown() {
		inner.shutdown();
	}

	/**
	 * @return
	 * @see java.util.concurrent.ThreadPoolExecutor#shutdownNow()
	 */
	@Override
	public List<Runnable> shutdownNow() {
		return inner.shutdownNow();
	}

	/**
	 * @param task
	 * @return
	 * @see java.util.concurrent.AbstractExecutorService#submit(java.util.concurrent.Callable)
	 */
	@Override
	public <T> Future<T> submit(Callable<T> task) {
		return inner.submit(task);
	}

	/**
	 * @param task
	 * @param result
	 * @return
	 * @see java.util.concurrent.AbstractExecutorService#submit(java.lang.Runnable, java.lang.Object)
	 */
	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		return inner.submit(task, result);
	}

	/**
	 * @param task
	 * @return
	 * @see java.util.concurrent.AbstractExecutorService#submit(java.lang.Runnable)
	 */
	@Override
	public Future<?> submit(Runnable task) {
		return inner.submit(task);
	}

	/**
	 * @return
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return inner.toString();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponent#getSupportedMetricNames()
	 */
	@Override
	public Set<String> getSupportedMetricNames() {		
		return Collections.emptySet();
	}

	/**
	 * Returns an array of thread details for this pool
	 * @return an array of strings
	 */
	@ManagedAttribute
	public String[] getThreadStats() {
		Set<String> threadNames = new HashSet<String>();
		Thread[] threads = new Thread[inner.getMaximumPoolSize()];
		int threadCount = threadGroup.enumerate(threads);
		for(int i = 0; i < threadCount; i++) {
			Thread t = threads[i];
			threadNames.add(String.format("%s(%s) [%s]", t.getName(), t.getId(), t.getState().name()));
		}
		return  threadNames.toArray(new String[threadNames.size()]);
	}

	

}
