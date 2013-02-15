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
package org.helios.collector.core;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ObjectName;

import org.helios.apmrouter.server.ServerComponentBean;
import org.helios.apmrouter.jmx.ConfigurationHelper;
import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.jmx.ScheduledThreadPoolFactory;
import org.helios.apmrouter.trace.ITracer;
import org.helios.apmrouter.trace.TracerFactory;
import org.helios.apmrouter.util.SystemClock;
import org.helios.apmrouter.util.SystemClock.ElapsedTime;
import org.helios.collector.BlackoutInfo;

/**
 * <p>Title: AbstractCollector</p>
 * <p>Description: Base class for all collectors</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public abstract class AbstractCollector extends ServerComponentBean implements 
					  Runnable, 
					  Collector, 
					  AbstractCollectorMXBean{
	
	/** The scheduler shared amongst all collector instances */
	protected static final ScheduledThreadPoolExecutor scheduler = ScheduledThreadPoolFactory.newScheduler("Monitor");
	/** The tracer instance */
	protected final ITracer tracer = TracerFactory.getTracer();
	/** The scheduler handle for this collector */
	protected ScheduledFuture<?> scheduleHandle = null;
	/** The collection period in ms. */
	protected long collectionPeriod = -1L;

	/**
	 *======= Following attributes exist in ServerComponentBean/ServerComponent ======
	 */	
/*	
	protected GenericApplicationContext applicationContext = null;
	protected String beanName = null;
	protected SimpleApplicationEventMulticaster eventMulticaster = null;
	protected Executor eventExecutor = null;
	protected int priority = Ordered.LOWEST_PRECEDENCE;
	protected ObjectName objectName = null;
	protected final AtomicBoolean started = new AtomicBoolean(false);
	protected final Set<Class<? extends ApplicationEvent>> supportedEventTypes = new HashSet<Class<? extends ApplicationEvent>>();
	protected final Set<Class<?>> supportedEventSourceTypes = new HashSet<Class<?>>();
	protected NotificationPublisher notificationPublisher = null;
	
*/	
	
	/**
	 *======= Following attributes are ported from original AbstractCollector ======
	 */
	
	/** Indicates if details of a collector failures should be logged. */
	protected boolean logErrors=false;
	
	/** 
	 * Indicates if the base class should trace the status and summary 
	 * results after each collection.  Helpful for dubugging purposes
	 */	
	protected boolean logCollectionResult=false;
	
	
	/**  
	 * An enum that defines the state of the collector. Lifecycle operations 
	 * should automatically set this state in each case.
	 */
	protected enum CollectorState {
		NULL(false), 
		CONSTRUCTED(false), 
		INITIALIZING(true), 
		INITIALIZED(true),
		INIT_FAILED(false),
		STARTING(true), 
		STARTED(true), 
		START_FAILED(false), 
		STOPPING(false), 
		STOPPED(false), 
		COLLECTING(true),
		RESETTING(true);
		
		private CollectorState(boolean running) {
			this.running = running;
		}
		
		private final boolean running;

		/**
		 * Indicates if this state is considered running
		 * @return true if this state is considered running, false otherwise
		 */
		public boolean isRunning() {
			return running;
		}
		
	}
	
	/** Indicates the current state of the collector. */
	private CollectorState state;
	
	/** Last time a collection was started. */
	protected long lastTimeCollectionStarted = 0L;
	
	/** Last time a collection finished. */
	protected long lastTimeCollectionCompleted = 0L;
	
	/** Last time a collection successfully completed. */
	protected long lastTimeCollectionSucceeded = 0L;
	
	/** Last time a collection failed. */
	protected long lastTimeCollectionFailed = 0L;
	
	/** Last collection elapsed time. */
	protected long lastCollectionElapsedTime = 0L;	
	
	/** Total number of times a collection is done. */
	protected int totalCollectionCount=0;
	
	/** Number of time a collector succeed. */
	protected int totalSuccessCount=0;
	
	/** Number of time the collector failed. */
	protected int totalFailureCount=0;		
	
	/** Total number of active collectors in this JVM. */
	protected static AtomicInteger numberOfActiveCollectors = new AtomicInteger();	
	
	private static AtomicLong notificationSerialNumber = new AtomicLong(0);	
	
	/** A label to be displayed on Helios dashboard for Availability of this collector */
	protected String defaultAvailabilityLabel="Availability";
	/** Number of failures during collect before Helios switches to a alternate (slower) frequency for this collector */
	protected int failureThreshold = 5;
	/** Number of consecutive errors produced by this collector so far */
	protected int consecutiveFailureCount = 0;
	/** Flag that indicates whether the alternate frequency is in effect due to recent error while collection. */
	protected boolean fallbackFrequencyActivated=false;
	/** Number of collections to skip when fallbackFrequencyActivated is true */
	protected int actualSkipped=0;
	/** 
	 *  Number of collect iterations to skip when fallbackFrequencyActivated is true.
	 *  Set this parameter to -1 to explicitly disable dormant mode.  This is usually desired when the frequency 
	 *  or schedule (cron expression) for this collector is set high.  For example (collectors that runs 
	 *  once every hour or daily etc...) 
	 * 
	 */
	protected int iterationsToSkip = 5;
	
	/** Number of attempts made so far to restart this collector after the initial start failed. */
	protected int restartAttempts = 0;
	/** 
	 * Maximum number of attempts that will be made to restart a collector whose initial start has failed. 
	 * Though not recommended but if you want the restart to be tried indefinitely, then set 
	 * this parameter to -1.
	 */
	protected int maxRestartAttempts = 3;
	
	/** Delay before another attempt is made to restart a collector.  Default is 2 minutes. */
	protected long restartAttemptDelayFrequency = 120000L;
	
//	/** Handle to a scheduled cron task */
//	protected ScheduledTaskHandle<AbstractCollector> restartTaskHandle = null;	
	
	/** Start of blackout period - expressed in HH:MM format.  HH is in the 24 hour format **/
	protected String blackoutStart = null;
	/** End of blackout period - expressed in HH:MM format.  HH is in the 24 hour format **/
	protected String blackoutEnd = null;
	/** Object to hold detailed blackout information */
	private BlackoutInfo blackoutInfo = null;		
	
	/** The property name where the JMX domain name for all collectors would be picked */
	protected static final String COLLECTORS_DOMAIN_PROPERTY="helios.collectors.jmx.domain";
	/** Default JMX domain name for all collectors in case COLLECTORS_DOMAIN_PROPERTY is not specified*/
	protected static final String COLLECTORS_DOMAIN_DEFAULT="org.helios.collectors";	
	
	static {
		Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override
			public void run() {
				scheduler.shutdownNow();
			}
		});
	}

	/**
	 * 
	 */
	public void collect() {
		SystemClock.startTimer();
		//doCollect(collectionSweep);
		ElapsedTime et = SystemClock.endTimer();
		//collectionSweep++;
		tracer.traceGauge(et.elapsedMs, "ElpasedTimeMs", "Collectors", getClass().getSimpleName());		
	}

	/**
	 * Returns collection period for this collector
	 */
	public long getCollectPeriod() {
		if(collectionPeriod==-1L) {
			collectionPeriod = ConfigurationHelper.getLongSystemThenEnvProperty("org.helios.collector." + getClass().getSimpleName().toLowerCase() + ".period", 15000L);
		}
		return collectionPeriod;
	}

	/**
	 * Set/override collection period for this collector
	 */
	public void setCollectPeriod(long period) {
		collectionPeriod = period;		
	}

	/**
	 * Triggers the start of this collector
	 */
	public void startCollector() {
		long collectPeriod = getCollectPeriod();
		scheduleHandle = scheduler.scheduleWithFixedDelay(this, 1, collectPeriod, TimeUnit.MILLISECONDS);
		info("Started collection schedule with frequency of ["+ collectPeriod + "] ms.");		
	}

	/**
	 * Schedule the start of this collector after specified seconds
	 */
	public void startCollector(long seconds) {
		info("Delaying start of [" + getClass().getSimpleName() + "] for [" + seconds + "] seconds");
		scheduler.schedule(new Runnable(){
			public void run() { startCollector(); }
		}, seconds, TimeUnit.SECONDS);		
	}

	@Override
	public void stopCollector() {
		if(scheduleHandle!=null) {
			scheduleHandle.cancel(false);
			info("Stopped collector ["+ getClass().getSimpleName() + "]");
		}		
	}

//	/**
//	 * Set properties for this collector
//	 */
//	public void setProperties(Properties p) {
//		if(p!=null) configProps.putAll(p);		
//	}	
	
	/**
	 * Indicates whether this collector is started
	 */
	public AtomicBoolean getStarted() {
		return started;
	}	
	
	/**
	 * Implemented from Runnable interface to direct collection and 
	 * tracing of metrics at specified intervals
	 */
	public void run() {
		try { collect(); } catch (Throwable t) {}		
	}

	public long getCollectionPeriod() {
		return collectionPeriod;
	}

	public void setCollectionPeriod(long collectionPeriod) {
		this.collectionPeriod = collectionPeriod;
	}

	public boolean isLogErrors() {
		return logErrors;
	}

	public void setLogErrors(boolean logErrors) {
		this.logErrors = logErrors;
	}

	public boolean isLogCollectionResult() {
		return logCollectionResult;
	}

	public void setLogCollectionResult(boolean logCollectionResult) {
		this.logCollectionResult = logCollectionResult;
	}

	public String getBlackoutStart() {
		return blackoutStart;
	}

	public void setBlackoutStart(String blackoutStart) {
		this.blackoutStart = blackoutStart;
	}

	public String getBlackoutEnd() {
		return blackoutEnd;
	}

	public void setBlackoutEnd(String blackoutEnd) {
		this.blackoutEnd = blackoutEnd;
	}

	public CollectorState getState() {
		return state;
	}
	
	private void setState(CollectorState state) {
		this.state = state;
	}	
	
	public long getLastTimeCollectionStarted() {
		return lastTimeCollectionStarted;
	}

	public long getLastTimeCollectionCompleted() {
		return lastTimeCollectionCompleted;
	}

	public long getLastTimeCollectionSucceeded() {
		return lastTimeCollectionSucceeded;
	}

	public long getLastTimeCollectionFailed() {
		return lastTimeCollectionFailed;
	}

	public long getLastCollectionElapsedTime() {
		return lastCollectionElapsedTime;
	}

	public int getTotalCollectionCount() {
		return totalCollectionCount;
	}

	public int getTotalSuccessCount() {
		return totalSuccessCount;
	}

	public int getTotalFailureCount() {
		return totalFailureCount;
	}

	public static AtomicInteger getNumberOfActiveCollectors() {
		return numberOfActiveCollectors;
	}

	public int getConsecutiveFailureCount() {
		return consecutiveFailureCount;
	}

	public boolean isFallbackFrequencyActivated() {
		return fallbackFrequencyActivated;
	}

	public int getMaxRestartAttempts() {
		return maxRestartAttempts;
	}	

	/*  ==================  CUSTOM LIFECYCLE OVERRIDE METHODS =============================*/
	
	/**
	 * This method can be overridden by concrete implementation classes 
	 * for any custom pre initialization tasks that needs to be done.
	 */
	public void preInit() {}	
	
	/**
	 * Initializes basic resources that are critical for any collector to work properly.
	 * This method cannot be overriden by concrete implementation classes.
	 *  
	 */
	public final void init() {
		setState(CollectorState.INITIALIZING);
		try{
			preInit();
			initCollector();
			postInit();
			if(blackoutStart!=null && blackoutEnd!=null){
				blackoutInfo = new BlackoutInfo(blackoutStart, blackoutEnd, beanName);
				if(!blackoutInfo.isValidRange()){
					blackoutInfo = null;
				}
			}
			setState(CollectorState.INITIALIZED);
		} catch(Exception ex){
			setState(CollectorState.INIT_FAILED);
			if(logErrors)
				log.error("An error occured while initializing the collector bean: "+this.getBeanName(),ex);
		}		
	}
	
	public ObjectName getObjectName() {
		return JMXHelper.objectName(System.getProperty(COLLECTORS_DOMAIN_PROPERTY, COLLECTORS_DOMAIN_DEFAULT)+":type="+this.getClass().getName().substring( this.getClass().getName().lastIndexOf( '.' ) + 1 )+",name="+this.getBeanName());
	}
	
	/**
	 * An additional convenience method provided for implementing task that needs to be 
	 * performed for initialization of this collector
	 */
	public void initCollector(){}		
	
	/**
	 * This method can be overridden by concrete implementation classes 
	 * for any custom post initialization tasks that needs to be done.
	 */
	public void postInit(){}

	/**
	 * This method can be overridden by concrete implementation classes 
	 * for any custom pre startup tasks that needs to be done.
	 */
	public void preStart(){}
	
	/**
	 * This method is an entry point to kickstart any collectors' lifecycle.  It initializes and 
	 * performs startup tasks before this collector is scheduled with HeliosScheduler.
	 * This method cannot be overriden by concrete implementation classes.  
	 *  
	 */
	public final void doStart() {
		try {
			init();
			if(getState() != CollectorState.INITIALIZED){
				log.error("Initialization error for bean: "+this.getBeanName() + ", so no further attempts would be made to start it.");
				//executeExceptionScript();
				return;
			}
			setState(CollectorState.STARTING);
			preStart();
			startCollector();
			postStart();	
			//registerInMBeanServer(this);
			scheduleCollect();
			//scheduleReset();
			setState(CollectorState.STARTED);
			numberOfActiveCollectors.incrementAndGet();
			info(banner("Collector ", this.getBeanName(), " Started"));
		}catch (Exception ex){
			setState(CollectorState.START_FAILED);
			if(logErrors)
				log.error("An error occured while starting the collector bean: "+this.getBeanName(),ex);
			else
				log.error("An error occured while starting the collector bean: "+this.getBeanName());
			//scheduleRestart();
			//executeExceptionScript();
		}
	}
	
	/**
	 * Schedule this collector to be triggered right away
	 */
	public void scheduleCollect() {
		long collectPeriod = getCollectPeriod();
		scheduleHandle = scheduler.scheduleWithFixedDelay(this, 1000, collectPeriod, TimeUnit.MILLISECONDS);
		info("Started collection schedule with frequency of ["+ collectPeriod + "] ms. for collector [" + this.getBeanName() + "]");
	}
	
	/**
	 * Schedule this collector to be triggered after specified number of seconds
	 */
	public void scheduleCollect(long seconds) {
		info("Delaying start of [" + this.getBeanName() + "] for [" + seconds + "] seconds");
		scheduler.schedule(new Runnable(){
			public void run() { startCollector(); }
		}, seconds, TimeUnit.SECONDS);
	}
	
	/**
	 * Unschedule this collector so it doesn't get triggered anymore
	 */
	public void unscheduleCollect() {
		if(scheduleHandle!=null) {
			scheduleHandle.cancel(false);
			info("Unscheduled collector [" + this.getBeanName() + "]");
		}
	}	
	
}
