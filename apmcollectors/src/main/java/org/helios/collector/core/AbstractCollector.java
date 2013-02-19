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

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * <p>Title: AbstractCollector</p>
 * <p>Description: Base class for all collectors</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 * 
 */
@ManagedResource
public abstract class AbstractCollector extends ServerComponentBean implements 
					  Callable<CollectionResult>, 
					  Collector,
					  AbstractCollectorMXBean{
	//TODO: 
	// Emit notification
	// Expose methods and attributes to JMX
	// streamline lifecycle
	// Replace JMX's dependency from Apache Commons pool to BoneCP
	
	/** The scheduler shared amongst all collector instances */
	protected static final ScheduledThreadPoolExecutor scheduler = ScheduledThreadPoolFactory.newScheduler("Collector");
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
	protected CollectorState state;
	
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
	
	/** Used during collect process  */
	protected final ReentrantLock collectorLock = new ReentrantLock();
	
	/**
	 * Scheduled collect call should wait for this period if the current  
	 * CollectorState is RESETTING
	 */
	protected long waitPeriodIfAlreadyCollecting = 0L;
	
	/** Flag to indicate whether reset is called on this collector */
	protected AtomicBoolean resetFlag = new AtomicBoolean(false);
	
	/** Indicates that the collector should be reset every resetCount collections. */
	protected int resetCount;	
	
	/** Total number of collectors running at this time time in this JVM. */
	protected static AtomicInteger numberOfCollectorsRunning = new AtomicInteger();	
	
	/** Reference of a POJO that stores information about the last collection result  */
	protected CollectionResult collectionResult;
	
	protected static final Pattern namePattern = Pattern.compile("\\{(\\d++)\\}");
	protected static final Pattern thisPattern = Pattern.compile("\\{THIS-PROPERTY:([a-zA-Z\\(\\)\\s-]+)}");
	protected static final Pattern thisDomainPattern = Pattern.compile("\\{THIS-DOMAIN:([\\d+])}");	
	protected static final Pattern segmentPattern = Pattern.compile("\\{SEGMENT:([\\d+])}");	
	protected static final Pattern targetDomainPattern = Pattern.compile("\\{TARGET-DOMAIN:([\\d+])}");	
	protected static final Pattern targetPattern = Pattern.compile("\\{TARGET-PROPERTY:([a-zA-Z\\(\\)\\s-]+)}");
	/** The root tracing namespace where all collected metrics will be traced to */
	protected String[] tracingNameSpace;
	
	static {
		Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override
			public void run() {
				scheduler.shutdownNow();
			}
		});
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

	/**
	 * @return the tracingNameSpace
	 */
	@ManagedAttribute	
	public String[] getTracingNameSpace() {
		return (String[])tracingNameSpace.clone();
	}

	/**
	 * @param tracingNameSpace the tracingNameSpace to set
	 */
	public void setTracingNameSpace(String[] tracingNameSpace) {
		this.tracingNameSpace = tracingNameSpace;
	}	
	
	/*  ==================  CUSTOM LIFECYCLE METHODS =============================*/
	
//	/**
//	 * This method can be overridden by concrete implementation classes 
//	 * for any custom pre initialization tasks that needs to be done.
//	 */
//	public void preInit() {}	
	
	/**
	 * Initializes basic resources that are critical for any collector to work properly.
	 * This method cannot be overriden by concrete implementation classes.
	 *  
	 */
	public final void init() {
		setState(CollectorState.INITIALIZING);
		try{
			//preInit();
			initCollector();
			//postInit();
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
	
//	/**
//	 * This method can be overridden by concrete implementation classes 
//	 * for any custom post initialization tasks that needs to be done.
//	 */
//	public void postInit(){}

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
	 * This method can be overridden by concrete implementation classes 
	 * for any custom post startup tasks that needs to be done.
	 */
	public void postStart(){}	
	
	/**
	 * To be implemented by concrete classes for any custom startup tasks
	 */
	public void startCollector() {}

//	public void collect() {
//		SystemClock.startTimer();
//		//doCollect(collectionSweep);
//		ElapsedTime et = SystemClock.endTimer();
//		//collectionSweep++;
//		tracer.traceGauge(et.elapsedMs, "ElpasedTimeMs", "Collectors", getClass().getSimpleName());		
//	}	
	
	/**
	 * Callback method for HeliosScheduler to trigger the start of a collection. 
	 */
	public CollectionResult call() throws CollectorException {
		collect();
		return collectionResult;
	}	
	
	/**
	 * This method can be overridden by concrete implementation classes 
	 * for any custom pre collect tasks that needs to be done.
	 */
	public void preCollect() {}
	
	/**
	 * This method ties up the functionality and sequencing of pre, post and collectCallback methods.  It cannot be 
	 * overridden by concrete collector classes
	 */
	public final void collect(){
		//Check whether blackout period is active for this collector
		if(blackoutInfo!=null && blackoutInfo.isBlackoutActive()){
			debug("*** Skipping collection as blackout period is active...");
			return;
		}
		
		//Check whether this collector is running in a dormant mode
		if(fallbackFrequencyActivated && iterationsToSkip!=-1){
			if(actualSkipped < iterationsToSkip ){
				actualSkipped++;
				debug("*** Skipping iteration " + actualSkipped + " for collector " +this.getBeanName()+" as it is in fallbackFrequencyActivated mode");
				return;
			}else{
				actualSkipped=0;
			}
		}
		CollectorState currState = getState();
		boolean errors=false;
		boolean gotLock=false;
		
		if(collectorLock.isLocked()){
			try{
				gotLock = collectorLock.tryLock(waitPeriodIfAlreadyCollecting, TimeUnit.MILLISECONDS);
				if(!gotLock){
					if(currState == CollectorState.COLLECTING){
						debug("The previous collect process is alreading running so skipping current collect call for bean: "+this.getBeanName());
						return;
					}else{
						error("Unable to obtain a lock on collector bean: "+this.getBeanName());
						return;
					}
				}
			}catch(InterruptedException ex){}
		}
		if(resetFlag.get()==true || (resetCount>0 && totalCollectionCount!=0 && totalCollectionCount%resetCount==0)){
			try{
				this.reset();
				resetFlag.getAndSet(false);
			}catch(Exception ex){
				if(logErrors)
					log.error("An exception occured while resetting the collector bean: "+this.getBeanName(),ex);
			}
		}
		if(getState() == CollectorState.STARTED){
			try {
				lastTimeCollectionStarted=System.currentTimeMillis();
				setState(CollectorState.COLLECTING);
				numberOfCollectorsRunning.incrementAndGet();
				preCollect();
				collectionResult = collectCallback();
				if(collectionResult.getResultForLastCollection() == CollectionResult.Result.FAILURE){
					throw new Exception(collectionResult.getAnyException());
				}
				lastTimeCollectionSucceeded=System.currentTimeMillis();
				totalSuccessCount++;
				if(fallbackFrequencyActivated){
					/* This collector is running on a fallback frequency.  Since this collect call was
					 * successful, switch the collect frequency back to normal schedule now. */
					fallbackFrequencyActivated=false;
					consecutiveFailureCount=0;
					actualSkipped=0;
					log.info("*** Frequency for collector: " + this.getBeanName() +" is switched back to normal now.");
				}
				info(banner("Completed Collect for", this.getBeanName()));
			} catch (Exception ex){
				lastTimeCollectionFailed=System.currentTimeMillis();
				errors=true;
				totalFailureCount++;
				consecutiveFailureCount++;
				if(consecutiveFailureCount>=failureThreshold && !fallbackFrequencyActivated){
					log.info("*** Slowing down the collect frequency for bean: " + this.getBeanName() +" as it has exceeded the collectFailureThreshold parameter.");
					fallbackFrequencyActivated=true;
				}				
//				this.sendNotification(new Notification("org.helios.collectors.exception.notification",this,notificationSerialNumber.incrementAndGet(),lastTimeCollectionFailed,this.getBeanName()));
				if(logErrors)
					log.error("Collection failed for bean collector: "+this.getBeanName(),ex);
				//executeExceptionScript();
			}finally {
				totalCollectionCount++;
				setState(currState);
				if(!errors){
					postCollect();
					lastTimeCollectionCompleted=System.currentTimeMillis();
					log.debug("Last Collection Elapsed Time: " + (lastTimeCollectionCompleted - lastTimeCollectionStarted)+ " milliseconds");
				}
				if(logCollectionResult) 
					logCollectionResultDetails(collectionResult);
				if(collectorLock.isLocked())
					collectorLock.unlock();
				numberOfCollectorsRunning.decrementAndGet();
			}	
		} else {
			log.trace("Not executing collect method as the collector state is not STARTED.");
		}
	}

	/**
	 * @param collectionResult 
	 * 
	 */
	private void logCollectionResultDetails(CollectionResult collectionResult) {
		info(collectionResult);
	}
	
	/**
	 * Collector specific collection tasks that should be implemented by concrete collector classes
	 */
	public abstract CollectionResult collectCallback() throws CollectorException;
	
	/**
	 * This method can be overridden by concrete implementation classes 
	 * for any custom pre collect tasks that needs to be done.
	 */
	public void postCollect() {}	
	
	/**
	 * This method can be overridden by concrete implementation classes 
	 * for any custom pre stop tasks that needs to be done.
	 */
	public void preStop() {}
	
	/**
	 * This method ties up the functionality and sequencing of pre, post and stopCollector methods.  It cannot be 
	 * overridden by concrete collector classes
	 */
	public final void doStop() {
		if(getState()==CollectorState.STOPPED || getState()==CollectorState.STOPPING)
			return;
		setState(CollectorState.STOPPING);
		try {
			preStop();
//			unregisterFromMBeanServer();
			unscheduleCollect();
//			unScheduleReset();			
			stopCollector();
			postStop();
			setState(CollectorState.STOPPED);
			info(banner("Collector", this.getBeanName()+" Stopped"));
		} catch (Exception ex){
			debug("An error occured while stopping collector bean: " + this.getBeanName(),ex); 
		} 
	}
	
	
	/**
	 * To be implemented by concrete classes for any custom stop tasks
	 */
	public void stopCollector() {}
	
	/**
	 * This method can be overridden by concrete implementation classes 
	 * for any custom post stop tasks that needs to be done.
	 */
	public void postStop() {
		
	}	

//	/**
//	 * This method can be overridden by concrete implementation classes 
//	 * for any custom pre reset tasks that needs to be done.
//	 */
//	public void preReset() {}
//	
	/**
	 * This method ties up the functionality and sequencing of pre, post and resetCollector methods.  It cannot be 
	 * overridden by concrete collector classes
	 */
	public final void reset(){
		CollectorState currState = getState();
		setState(CollectorState.RESETTING);
		// acquires a re-enterent lock to prevent collectorCallback from 
		// executing while reset is happening.
		collectorLock.lock();
		try {
			//preReset();
			resetCollector();
			//postReset();
			setState(currState);
			info(banner("Reset Completed for Collector", this.getBeanName()));
		} catch (Exception ex){
			debug("An error occured while resetting the collector bean: " + this.getBeanName(),ex);
		} finally {
			collectorLock.unlock();
		}		
	}

	/**
	 * An additional convenience method provided for implementing task that needs to be 
	 * performed for resetting this collector
	 */
	public void resetCollector() {}

	/**
	 * This method ties up the functionality and sequencing of pre, post and destroyCollector methods.  It cannot be 
	 * overridden by concrete collector classes
	 */
	public final void destroy() throws CollectorException{
		try {
			doStop();
			destroyCollector();
			info(banner("Collector", this.getBeanName()," Destroyed"));
		} catch(Exception ex){
			throw new CollectorException("An error occured while destroying collector bean: " + this.getBeanName(),ex);
		}	
	}
	
	/**
	 * An additional convenience method provided for implementing task that needs to be 
	 * performed for destroying this collector
	 */
	public void destroyCollector() {}	
	
//
//	/**
//	 * An additional convenience method provided for implementing task that needs to be 
//	 * performed for resetting this collector
//	 */
//	public void resetCollector() {}
//	
//	/**
//	 * This method can be overridden by concrete implementation classes 
//	 * for any custom post reset tasks that needs to be done.
//	 */
//	public void postReset() {}

	/**
	 * @return the current state of this collector
	 */
	@ManagedAttribute
	public String currentState() {
		if(getState()==CollectorState.STARTED) return "Started";
        else if(getState()==CollectorState.COLLECTING) return "Collecting";
		else if(getState()==CollectorState.STOPPED) return "Stopped";
		else if(getState()==CollectorState.INITIALIZED) return "Initialized";
		else if(getState()==CollectorState.INITIALIZING) return "Initializing";
        else if(getState()==CollectorState.INIT_FAILED) return "Initialization Failed";
        else if(getState()==CollectorState.STARTING) return "Starting";
        else if(getState()==CollectorState.START_FAILED) return "Start Failed";
        else if(getState()==CollectorState.STOPPING) return "Stopping";
        else if(getState()==CollectorState.CONSTRUCTED) return "Constructed";
        else if(getState()==CollectorState.RESETTING) return "Resetting";
        else return "Unknown";
	}	
	
	/**
	 * Schedule this collector with fixed frequency  
	 */
	public void scheduleCollect() {
		long collectPeriod = getCollectPeriod();
		scheduleHandle = scheduler.schedule(new Runnable(){
			public void run() { collect(); }
		}, collectPeriod, TimeUnit.MILLISECONDS);
		info("Started collection schedule with frequency of ["+ collectPeriod + "] ms. for collector [" + this.getBeanName() + "]");
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
	
	/**
	 * Applies pattern substitutions to the passed string for target properties from this MBean.
	 * @param name A value to be formatted.
	 * @return A formatted name.
	 */
	protected String formatName(String name) {
		if(name.contains("{THIS-PROPERTY")) {
			name = bindTokens(objectName, name, thisPattern);
		}
		if(name.contains("{THIS-DOMAIN")) {
			name = bindTokens(objectName, name, thisDomainPattern);
		}
		if(name.contains("{SEGMENT")) {
			name = bindTokens(objectName, name, segmentPattern);
		}				
		return name;
	}
	
	/**
	 * Applies pattern substitutions to the passed string for target properties from the target mbean.
	 * @param name A value to be formatted.
	 * @return A formatted name.
	 */
	protected String formatName(String name, ObjectName remoteMBean) {
		if(name.contains("{TARGET-PROPERTY")) {
			name = bindTokens(remoteMBean, name, targetPattern);
		}
		if(name.contains("{THIS-DOMAIN")) {
			name = bindTokens(objectName, name, targetDomainPattern);
		}				
		return name;
	}
	
	
	/**
	 * Takes the text passed and replaces tokens in accordance with the pattern 
	 * supplied taking the substitution vale from properties in the passed object name.
	 * @param targetObjectName The substitution values come from this object name.
	 * @param text The original text that will be substituted.
	 * @param p The pattern matcher to locate substitution tokens.
	 * @return The substituted string.
	 */
	public String bindTokens(ObjectName targetObjectName, String text, Pattern p) {
		Matcher matcher = p.matcher(text);
		String token = null;
		String property = null;
		String propertyValue = null;
		int pos = -1;
		while(matcher.find()) {
			token = matcher.group(0);
			property = matcher.group(1);
			propertyValue = targetObjectName.getKeyProperty(property);
            if(token.toUpperCase().contains("DOMAIN")) {
                pos = Integer.parseInt(property);
                propertyValue = targetObjectName.getDomain().split("\\.")[pos];
            } else if(token.toUpperCase().contains("SEGMENT")) {
            	pos = Integer.parseInt(property);
            	propertyValue = tracingNameSpace[pos];
            } else {
                propertyValue = targetObjectName.getKeyProperty(property);
            }			
			text = text.replace(token, propertyValue);
		}
		return text;
	}		
	
}
