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
package org.helios.apmrouter.jmx;

import java.lang.management.ManagementFactory;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.ObjectName;


/**
 * <p>Title: ThreadPoolFactory</p>
 * <p>Description: JMX instrumented thread pool executor factory</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.jmx.ThreadPoolFactory</code></p>
 */
public class ThreadPoolFactory extends ThreadPoolExecutor implements ThreadFactory, ThreadPoolMXBean {
	/** The ObjectName that will be used to register the thread pool management interface */
	protected final ObjectName objectName;
	/** Serial number factory for thread names */
	protected final AtomicInteger serial = new AtomicInteger(0);
	/** The pool name */
	protected final String name;

	/**
	 * Creates a new ThreadPool
	 * @param domain The JMX domain where the MBean will be published 
	 * @param name The name property for the MBean ObjectName
	 * @return a new thread pool
	 */
	public static Executor newCachedThreadPool(String domain, String name) {
		return new ThreadPoolFactory(domain, name);
	}
	
	/**
	 * Creates a new custmized ThreadPool
	 * @param coreThreads The number of core threads
	 * @param maxThreads The maximum number of threads
	 * @param keepAliveMs The keep alive time of non-core threads
	 * @param queueSize The execution queue size
	 * @param fairQueue true for a fair queue, false otherwise
	 * @param handler The rejection handler to install
	 * @param prestartCoreThreads true to prestart all core threads
	 * @param domain The JMX domain where the MBean will be published 
	 * @param name The name property for the MBean ObjectName
	 */
	public ThreadPoolFactory(int coreThreads, int maxThreads, long keepAliveMs, int queueSize, boolean fairQueue,  RejectedExecutionHandler handler, boolean prestartCoreThreads, String domain, String name) {		
		super(coreThreads, maxThreads, keepAliveMs, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(queueSize, fairQueue));
		this.name = name;
		setRejectedExecutionHandler(handler);
		setThreadFactory(this);
		objectName = JMXHelper.objectName(domain + ":service=ThreadPool,name=" + name);
		try {			
			JMXHelper.registerMBean(objectName, this);
//			String prefix = "threadPools.[" + name + "].";
		} catch (Exception e) {
			//throw new RuntimeException("Failed to register management interface for pool [" + domain + "/" + name + "]", e);
			System.err.println("Failed to register management interface for pool [" + domain + "/" + name + "]");
		}
		if(prestartCoreThreads) prestartAllCoreThreads();
	}
	
	
	/**
	 * Creates a new ThreadPool
	 * @param domain The JMX domain where the MBean will be published 
	 * @param name The name property for the MBean ObjectName
	 */
	private ThreadPoolFactory(String domain, String name) {
		super(0, Integer.MAX_VALUE, 50L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
		setThreadFactory(this);
		this.name = name;
		prestartAllCoreThreads();
		objectName = JMXHelper.objectName(domain + ":service=ThreadPool,name=" + name);
		try {			
			JMXHelper.registerMBean(objectName, this);
//			String prefix = "threadPools.[" + name + "].";
		} catch (Exception e) {
			System.err.println("Failed to register management interface for pool [" + domain + "/" + name + "]");
		}
		
	}
	



	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
	 */
	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(r, name + "Thread#" + serial.incrementAndGet());
		t.setDaemon(true);
		return t;
	}


}