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

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ObjectName;

import org.w3c.dom.Node;

/**
 * <p>Title: ScheduledThreadPoolFactory</p>
 * <p>Description: Task scheduler</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.jmx.ScheduledThreadPoolFactory</code></p>
 */

public class ScheduledThreadPoolFactory extends ScheduledThreadPoolExecutor implements ThreadFactory, SchedulerMXBean, RejectedExecutionHandler, UncaughtExceptionHandler, TaskScheduler {
	/** The ObjectName that will be used to register the scheduled thread pool management interface */
	protected final ObjectName objectName;
	/** Serial number factory for thread names */
	protected final AtomicInteger serial = new AtomicInteger(0);
	/** The scheduler name */
	protected final String name;
	/** Indicates if threads should be daemons */
	protected final boolean daemonThreads;
	/** The currently scheduled tasks */
	protected final Set<TrackedScheduledFuture> activeTasks = new CopyOnWriteArraySet<TrackedScheduledFuture>();
	/** Externally scheduled task handles */
	protected final Map<Long, TrackedScheduledFuture> externalTasks = new ConcurrentHashMap<Long, TrackedScheduledFuture>();
	/** Serial number generator for external task handles */
	protected final AtomicLong externalTaskSerial = new AtomicLong(0L);

	/** A map of created and started scheduler factories */
	protected static final Map<String, ScheduledThreadPoolFactory> tpSchedulers = new ConcurrentHashMap<String, ScheduledThreadPoolFactory>();
	
	/** The configuration node name */
	public static final String NODE = "scheduler";
	
//	/** Tab type for tasks */
//	protected static final TabularType TASK_TAB_TYPE ;
//	
//	static {
//		try {
//			TASK_TAB_TYPE = new TabularType("ScheduledTasks", "All the scheduled tasks for this scheduler",  TrackedScheduledFuture.COMPOSITE_TYPE, new String[]{"Id"});
//		} catch (Exception e) {
//			throw new RuntimeException(e);
//		}
//	}
	
	/**
	 * Returns the named scheduler
	 * @param name The name of the scheduler to retrieve
	 * @return The named scheduler
	 * @throws  IllegalStateException Thrown if the named scheduler does not exist
	 */
	public static ScheduledThreadPoolFactory getFullInstance(String name) {
		if(name==null) throw new IllegalArgumentException("The passed name was null", new Throwable());
		ScheduledThreadPoolFactory tps = tpSchedulers.get(name);
		if(tps==null) throw new IllegalStateException("The scheduler named  [" + name + "] has not been initialized" , new Throwable());
		return tps;
	}
	
	/**
	 * Returns the named scheduler
	 * @param name The name of the scheduler to retrieve
	 * @return The named scheduler
	 * @throws  IllegalStateException Thrown if the named scheduler does not exist
	 */
	public static TaskScheduler getInstance(String name) {
		return getFullInstance(name);
	}
	
	

	/**
	 * Creates a new ScheduledThreadPoolFactory
	 * @param tps A thread pool configuration 
	 */
	protected ScheduledThreadPoolFactory(ScheduledThreadPoolConfig tps) {
		super(tps.coreSize);		
		setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
		setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
		setRejectedExecutionHandler(this);
		setThreadFactory(this);
		name = tps.name;
		daemonThreads = tps.daemonThreads;
		objectName = JMXHelper.objectName("org.helios.apmrouter.jmx:service=Scheduler,name=" + name);
		JMXHelper.registerMBean(JMXHelper.getHeliosMBeanServer(), objectName, this);
		Thread shutdownThread = new Thread(){
			public void run() {
				shutdownNow();
			}
		};
		Runtime.getRuntime().addShutdownHook(shutdownThread);
	}	
	
	/**
	 * Creates or retrieves a ScheduledThreadPoolFactory
	 * @param configNode The XML configuration node 
	 * @return the ScheduledThreadPoolFactory named in the passed configNode
	 */
	public static ScheduledThreadPoolFactory newScheduler(Node configNode) {
		if(configNode==null) throw new IllegalArgumentException("Passed configuration node was null", new Throwable());
		String nodeName = configNode.getNodeName(); 
		if(!NODE.equals(nodeName)) {
			throw new RuntimeException("Configuration Node expected to have node name [" + NODE + "] but was [" + nodeName + "]", new Throwable());
		}
		String poolName = XMLHelper.getAttributeByName(configNode, "name", null);
		if(poolName==null || poolName.trim().isEmpty()) {
			throw new RuntimeException("Scheduler Node had null name [" + XMLHelper.renderNode(configNode), new Throwable());
		}
		ScheduledThreadPoolFactory tps = tpSchedulers.get(poolName);
		if(tps==null) {
			synchronized(tpSchedulers) {
				tps = tpSchedulers.get(poolName);
				if(tps==null) {
					tps = new ScheduledThreadPoolFactory(ScheduledThreadPoolConfig.getInstance(configNode));
					tpSchedulers.put(poolName, tps);
				}
			}
		}
		return tps;
	}
	
	/**
	 * Creates or retrieves a ScheduledThreadPoolFactory
	 * @param name the name of the scheduler 
	 * @return the ScheduledThreadPoolFactory named in the passed configNode
	 */
	public static ScheduledThreadPoolFactory newScheduler(String name) {
		if(name==null || name.trim().isEmpty()) throw new IllegalArgumentException("Passed name was null or empty", new Throwable());
		ScheduledThreadPoolFactory tps = tpSchedulers.get(name);
		if(tps==null) {
			synchronized(tpSchedulers) {
				tps = tpSchedulers.get(name);
				if(tps==null) {
					tps = new ScheduledThreadPoolFactory(ScheduledThreadPoolConfig.getInstance(name));
					tpSchedulers.put(name, tps);
				}
			}
		}
		return tps;
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.ScheduledThreadPoolExecutor#decorateTask(java.lang.Runnable, java.util.concurrent.RunnableScheduledFuture)
	 */
	@Override
	protected <V> RunnableScheduledFuture<V> decorateTask(Runnable runnable, RunnableScheduledFuture<V> task) {
		return new ListenerAwareRunnableScheduledFuture<V>(task);
	}
	
//	/**
//	 * Post execution hook
//	 * @param r The runnable
//	 * @param t The throwable
//	 */
//	@Override
//	protected void afterExecute(Runnable r, Throwable t) {
//		if(r instanceof TrackedScheduledFuture) {
//			activeTasks.remove(r);
//		}
//		super.afterExecute(r, t);
//	}
	
	/**
	 * Returns the number of pending tasks
	 * @return the number of pending tasks
	 */
	@Override
	public int getPendingTaskCount() {
		return activeTasks.size();
	}
	
	/**
	 * Enlists the tracked task
	 * @param description The description of the task
	 * @param delayPeriod The task delay or period in seconds
	 * @param task The task to enlist
	 * @return The enlisted task
	 */	
	@SuppressWarnings("rawtypes")
	protected TrackedScheduledFuture enlistTask(String description, long delayPeriod, ScheduledFuture<?> task) {
		ListenerAwareRunnableScheduledFuture sf = (ListenerAwareRunnableScheduledFuture)task;
		final TrackedScheduledFuture trackedTask = new TrackedScheduledFuture(sf, description, delayPeriod, activeTasks);
		sf.addCompletionListener(new Runnable() {
			@Override
			public void run() {
					activeTasks.remove(trackedTask);
			}
		});
		activeTasks.add(trackedTask);
		return trackedTask;			
	}
	
    /**
     * Creates and executes a one-shot action that becomes enabled after the given delay.
     * @param description A description of the command
     * @param command The runnable to schedule
     * @param delay The delay time
     * @param unit The delay unit
     * @return the scheduled future for the task
     */
	@Override
	public TrackedScheduledFuture schedule(String description, Runnable command, long delay, TimeUnit unit) {    	
    	return enlistTask(description, TimeUnit.SECONDS.convert(delay, unit), schedule(command, delay, unit)); 
    }
    
    /**
     * Creates and executes a one-shot action that becomes enabled after the given delay.
     * @param description A description of the command
     * @param callable The callable to schedule
     * @param delay The delay time
     * @param unit The delay unit
     * @return the scheduled future for the task
     */    
	@Override
	public TrackedScheduledFuture schedule(String description, Callable<?> callable, long delay, TimeUnit unit) {
    	return enlistTask(description, TimeUnit.SECONDS.convert(delay, unit), super.schedule(callable, delay, unit));
    }
    
    /**
     * Creates and executes a periodic action that becomes enabled first after the given initial delay, 
     * and subsequently with the given period; that is executions will commence after initialDelay 
     * then initialDelay+period, then initialDelay + 2 * period, and so on.
     * @param description A description of the command
     * @param command The command to schedule
     * @param initialDelay the time to delay first execution
     * @param period the period between successive executions
     * @param unit The period unit
     * @return the scheduled future for the task
     */
	@Override
	public TrackedScheduledFuture scheduleAtFixedRate(String description, Runnable command, long initialDelay, long period, TimeUnit unit) {
    	return enlistTask(description, TimeUnit.SECONDS.convert(period, unit), super.scheduleAtFixedRate(command, initialDelay, period, unit));
    }
    
    /**
     * Creates and executes a periodic action that becomes enabled first after the given initial delay, 
     * and subsequently with the given delay between the termination of one execution and the commencement of the next. 
     * If any execution of the task encounters an exception, subsequent executions are suppressed. Otherwise, the task will only terminate via cancellation or termination of the executor. 
     * @param description A description of the command
     * @param command The command to schedule
     * @param initialDelay the time to delay first execution
     * @param period the period between successive executions
     * @param unit The period unit
     * @return the scheduled future for the task
     */
    @Override
	public TrackedScheduledFuture scheduleWithFixedDelay(String description, Runnable command, long initialDelay, long period, TimeUnit unit) {
    	return enlistTask(description, TimeUnit.SECONDS.convert(period, unit), super.scheduleWithFixedDelay(command, initialDelay, period, unit));
    }
    
    /**
     * Creates and executes a periodic action that becomes enabled first after the given initial delay, 
     * and subsequently with the given delay between the termination of one execution and the commencement of the next. 
     * If any execution of the task encounters an exception, subsequent executions are suppressed. Otherwise, the task will only terminate via cancellation or termination of the executor. 
     * @param description A description of the command
     * @param task The ObjectName of the task to schedule
     * @param initialDelay the time to delay first execution
     * @param period the period between successive executions
     * @param unit The period unit
     * @return the task schedule handle use to cancel the task
     */
    @Override
	public long scheduleWithFixedDelay(String description, final ObjectName task, final long initialDelay, final long period, final String unit) {    	
    	TrackedScheduledFuture tsf = enlistTask(description, TimeUnit.SECONDS.convert(period, TimeUnit.valueOf(unit)), super.scheduleWithFixedDelay(new Runnable(){
    		@Override
    		public void run() {
    			JMXHelper.invoke(task, JMXHelper.getHeliosMBeanServer(), "run", 
    					new Object[]{}, 
    					new String[]{});
    		}
    	}, initialDelay, period, TimeUnit.valueOf(unit)));
    	long serial = externalTaskSerial.incrementAndGet();
    	externalTasks.put(serial, tsf);
    	return serial;
    }
    
    /**
     * Cancels the task associated with the passed handle
     * @param taskId The task handle
     * @param mayInterruptIfRunning true if the task can be interrupted if running
     */
    public void cancelTask(long taskId, boolean mayInterruptIfRunning) {
    	TrackedScheduledFuture tsf = externalTasks.remove(taskId);
    	if(tsf!=null) tsf.cancel(mayInterruptIfRunning);
    }
    
    
	/**
	 * Returns an array of the scheduled tasks
	 * @return an array of the scheduled tasks
	 */
	@Override
	public Set<TrackedScheduledFuture> getScheduledTasks() {
		return  new HashSet<TrackedScheduledFuture>(activeTasks);
	}
	
	/**
	 * Returns the assigned JMX ObjectName
	 * @return the objectName
	 */
	public ObjectName getObjectName() {
		return objectName;
	}

	/**
	 * Returns the scheduler name
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.RejectedExecutionHandler#rejectedExecution(java.lang.Runnable, java.util.concurrent.ThreadPoolExecutor)
	 */
	@Override
	public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
		System.err.println("Scheduler rejected execution of [" + r + "]");
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.Thread.UncaughtExceptionHandler#uncaughtException(java.lang.Thread, java.lang.Throwable)
	 */
	@Override
	public void uncaughtException(Thread t, Throwable e) {
		System.err.println("Scheduler had uncaught exception. Stack trace follows.");
		e.printStackTrace(System.err);
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
	 */
	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(r, name + "SchedulerThread#" + serial.incrementAndGet());
		t.setDaemon(true);
		t.setUncaughtExceptionHandler(this);
		return t;
	}

	

	/**
	 * <p>Title: ScheduledThreadPoolConfig</p>
	 * <p>Description: Value container and parser for a scheduled thread pool config</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.jmx.ScheduledThreadPoolFactory.ScheduledThreadPoolConfig</code></p>
	 */
	protected static class ScheduledThreadPoolConfig {
		/** The name of the pool */
		protected final String name;
		/** The pool's core thread count */
		protected final int coreSize;
		/** The maximum amount of time allowed for threads to finish their work on a non-immediate shutdown  */
		protected final long termTime;
		/** Indicates if the pool should be terminated immdeiately on shutdown notice */
		protected final boolean immediateTerm;
		
		/** Indicates if threads should be daemons */
		protected final boolean daemonThreads;
		
		
		/** The default core size which is 1 */
		public static final int DEF_CORE_SIZE = 1;
		/** The default max size which is the number of core available X 4 */
		public static final int DEF_MAX_SIZE = DEF_CORE_SIZE;
		/** The default keep alive time for idles threads which is 60s */
		public static final long DEF_KEEPALIVE = 60;
		/** The default daemon status of pool threads which is true */
		public static final boolean DEF_DAEMON = true;
		/** The default immediate termination which is true */
		public static final boolean DEF_IMMEDIATE_TERM = true;
		/** The default allowed termination time which is 5 s */
		public static final long DEF_TERM_TIME = 5;
		
		/**
		 * Creates a new ScheduledThreadPoolConfig with a default configurtation
		 * @param name The name of the scheduler to create
		 */
		private ScheduledThreadPoolConfig(String name) {
			this.name = name;
			coreSize = DEF_CORE_SIZE;
			termTime = DEF_TERM_TIME;
			immediateTerm = DEF_IMMEDIATE_TERM;
			daemonThreads = DEF_DAEMON;
		}
		
		
		/**
		 * Creates a new ScheduledThreadPoolConfig
		 * @param configNode The configuration node
		 */
		public ScheduledThreadPoolConfig(Node configNode) {
			if(configNode==null) throw new RuntimeException("Passed configuration node was null", new Throwable());
			String nodeName = configNode.getNodeName(); 
			if(!NODE.equals(nodeName)) {
				throw new RuntimeException("Configuration Node expected to have node name [" + NODE + "] but was [" + nodeName + "]", new Throwable());
			}
			name = XMLHelper.getAttributeByName(configNode, "name", null);
			if(name==null || name.trim().isEmpty()) {
				throw new RuntimeException("ThreadPool Node had null name [" + XMLHelper.renderNode(configNode), new Throwable());
			}
			
			// ===== Pool Stuff ===== 
			Node currentNode = XMLHelper.getChildNodeByName(configNode, "pool", false);
			coreSize = XMLHelper.getAttributeByName(currentNode, "core", DEF_CORE_SIZE);
			daemonThreads = XMLHelper.getAttributeByName(currentNode, "daemon", DEF_DAEMON);
			
			// ===== Termination Stuff =====
			currentNode = XMLHelper.getChildNodeByName(configNode, "termination", false);
			immediateTerm = XMLHelper.getAttributeByName(currentNode, "immediate", DEF_IMMEDIATE_TERM);
			if(immediateTerm) {
				termTime = 0;
			} else {
				termTime = XMLHelper.getAttributeByName(currentNode, "termTime", DEF_TERM_TIME);
			}
					
		}
		
		

		/**
		 * Returns a ScheduledThreadPoolConfig for the passed config node
		 * @param configNode The configuration node
		 * @return a ThreadPoolConfig for the passed config node
		 */
		static ScheduledThreadPoolConfig getInstance(Node configNode) {
			return new ScheduledThreadPoolConfig(configNode);
		}
		
		/**
		 * Creates a default configuration scheduler config
		 * @param name The name of the scheduler to create
		 * @return the named default configuration
		 */
		static ScheduledThreadPoolConfig getInstance(String name) {
			return new ScheduledThreadPoolConfig(name);
		}

		/**
		 * Returns the name of the pool
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * Returns the core pool size
		 * @return the coreSize
		 */
		public int getCoreSize() {
			return coreSize;
		}


		/**
		 * Returns the amount of time in s. that threads will be given to complete their tasks on a shutdown notice.
		 * Not relevant if {@link #immediateTerm} is true.
		 * @return the termTime
		 */
		public long getTermTime() {
			return termTime;
		}

		/**
		 * Indicates if this pool will be shutdown immediately on a shutdown notice
		 * @return the immediateTerm
		 */
		public boolean isImmediateTerm() {
			return immediateTerm;
		}



		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("ScheduledThreadPoolConfig [name=").append(name)
					.append(", coreSize=").append(coreSize)
					.append(", termTime=").append(termTime)
					.append(", immediateTerm=").append(immediateTerm);
			return builder.toString();
		}
	}


	

}
