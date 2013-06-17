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

import java.lang.Thread.UncaughtExceptionHandler;
import org.helios.apmrouter.jmx.ConfigurationHelper;
import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.server.ServerComponentBean;
import org.helios.apmrouter.trace.ITracer;
import org.helios.apmrouter.trace.TracerFactory;
import org.helios.collector.BlackoutInfo;
import org.springframework.jmx.export.annotation.*;

import javax.management.Notification;
import javax.management.ObjectName;

import java.util.Date;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import org.springframework.jmx.support.MetricType;

/**
 * <p>Title: AbstractCollector</p>
 * <p>Description: Base class for all collectors</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 * 
 */
@ManagedNotifications({
	@ManagedNotification(notificationTypes={"org.helios.collector.exception.notification"}, name="javax.management.Notification", description="Notification indicating exception during collect callback for any collector"),
	@ManagedNotification(notificationTypes={"org.helios.collectors.AbstractCollector.CollectorState"}, name="javax.management.Notification", description="Notification indicating change in CollectorState")
})
@ManagedResource
public abstract class AbstractCollector extends ServerComponentBean implements 
					  Callable<CollectionResult>,
					  Thread.UncaughtExceptionHandler,
					  Collector{
	//TODO: 
	// Emit notification - done
	// Expose methods and attributes to JMX - done
	// streamline lifecycle
	// Replace JMX's dependency from Apache Commons pool to BoneCP
	
	private static final AtomicInteger serial = new AtomicInteger();
	
	/** The scheduler shared amongst all collector instances */
	protected static final ScheduledThreadPoolExecutor scheduler = (ScheduledThreadPoolExecutor)Executors.newScheduledThreadPool(10, new ThreadFactory(){
		
		final ThreadGroup threadGroup = new ThreadGroup("CollectorsThreadGroup");
		final ClassLoader context = AbstractCollector.class.getClassLoader();
		
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(threadGroup, r, "CollectorsThread#" + serial.incrementAndGet());
			t.setDaemon(true);
			t.setContextClassLoader(context);
			return t;
		}
	});
	/*
			WAS: //ScheduledThreadPoolFactory.newScheduler("Collector");		 
	*/ 
		
	/** The tracer instance */
	protected ITracer tracer = null;
	/** The scheduler handle for this collector */
	protected ScheduledFuture<?> scheduleHandle = null;
	/** The collection period in ms. */
	protected long collectionPeriod = -1L;
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
	protected String defaultAvailabilityLabel="TargetAvailability";
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
	protected long restartAttemptDelayFrequency = 1200000L;
	
	/** Start of blackout period - expressed in HH:MM format.  HH is in the 24 hour format **/
	protected String blackoutStart = null;
	/** End of blackout period - expressed in HH:MM format.  HH is in the 24 hour format **/
	protected String blackoutEnd = null;
	/** Object to hold detailed blackout information */
	private BlackoutInfo blackoutInfo = null;		
	
	/** The property name where the JMX domain name for all collectors would be picked */
	protected static final String COLLECTORS_DOMAIN_PROPERTY="helios.collector.jmx.domain";
	/** Default JMX domain name for all collectors in case COLLECTORS_DOMAIN_PROPERTY is not specified*/
	protected static final String COLLECTORS_DOMAIN_DEFAULT="org.helios.collector";	
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
    /** The virtual tracer host */
    protected String virtualHost = null;
    /** The virtual tracer agent */
    protected String virtualAgent = null;
    protected String defaultVirtualHost = "APMRouter";
    protected String defaultVirtualAgent = "Collectors";


	
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
    //@ManagedMetric(category="MetricCatalogService",displayName="AverageCallTimeNs",metricType=MetricType.LONG_GAUGE, description="Frequency (ms) at which the collect will happen")
	@ManagedAttribute (description = "Frequency (ms) at which the collect will happen")
	public long getCollectionPeriod() {
		if(collectionPeriod==-1L) {
			collectionPeriod = ConfigurationHelper.getLongSystemThenEnvProperty("org.helios.collector." + getClass().getSimpleName().toLowerCase() + ".period", 15000L);
		}
		return collectionPeriod;
	}

	/**
	 * Set/override collection period for this collector
	 */
    @ManagedAttribute (description = "Frequency (ms) at which the collect will happen")
	public void setCollectionPeriod(long period) {
		collectionPeriod = period;
        unscheduleCollect();
        scheduleCollect();
	}
	
	/**
	 * Indicates whether this collector is started
	 */
	@ManagedAttribute (description = "Whether this collector is currently active")
	public AtomicBoolean getStarted() {
		return started;
	}	
	
	public boolean isLogErrors() {
		return logErrors;
	}

	public void setLogErrors(boolean logErrors) {
		this.logErrors = logErrors;
	}
	@ManagedAttribute (description = "Should collection result be printed out after each collect call")
	public boolean isLogCollectionResult() {
		return logCollectionResult;
	}

	public void setLogCollectionResult(boolean logCollectionResult) {
		this.logCollectionResult = logCollectionResult;
	}
	
	public String getBlackoutStart() {
		return blackoutStart;
	}

  @ManagedOperation(description="Specify start of blackout period ")
  @ManagedOperationParameters({
    @ManagedOperationParameter(name = "blackoutStart", description = "Start of blackout period - expressed in HH:MM format.  HH is in the 24 hour format")})
	public void setBlackoutStart(String blackoutStart) {
		this.blackoutStart = blackoutStart;
	}
	
	public String getBlackoutEnd() {
		return blackoutEnd;
	}
  @ManagedOperation(description="Specify end of blackout period ")
  @ManagedOperationParameters({
    @ManagedOperationParameter(name = "blackoutEnd", description = "End of blackout period - expressed in HH:MM format.  HH is in the 24 hour format")})
	public void setBlackoutEnd(String blackoutEnd) {
		this.blackoutEnd = blackoutEnd;
	}

	public CollectorState getState() {
		return state;
	}
	
	private void setState(CollectorState state) {
		this.state = state;
	}	
	@ManagedAttribute (description = "Last time collect started")
	public String getLastTimeCollectionStarted() {
		return new Date(lastTimeCollectionStarted).toString();
	}
	@ManagedAttribute (description = "Last time collect completed")
	public String getLastTimeCollectionCompleted() {
		return new Date(lastTimeCollectionCompleted).toString();
	}
	@ManagedAttribute (description = "Last time collect completed successfully")
	public String getLastTimeCollectionSucceeded() {
		return new Date(lastTimeCollectionSucceeded).toString();
	}
	@ManagedAttribute (description = "Last time collect failed")
	public String getLastTimeCollectionFailed() {
		return new Date(lastTimeCollectionFailed).toString();
	}
	@ManagedAttribute (description = "Elapsed time (in ms) for the last collect call")
	public long getLastCollectionElapsedTime() {
		return lastCollectionElapsedTime;
	}
	@ManagedAttribute (description = "Total number of collect so far")
	public int getTotalCollectionCount() {
		return totalCollectionCount;
	}
	@ManagedAttribute (description = "Total successful collects so far")
	public int getTotalSuccessCount() {
		return totalSuccessCount;
	}
	@ManagedAttribute (description = "Total failed collect so far")
	public int getTotalFailureCount() {
		return totalFailureCount;
	}
	@ManagedAttribute (description = "Total number of colectors active currently in this JVM")
	public int getNumberOfActiveCollectors() {
		return numberOfActiveCollectors.get();
	}
	@ManagedAttribute (description = "Number of consecutive errors produced by this collector so far")
	public int getConsecutiveFailureCount() {
		return consecutiveFailureCount;
	}
	@ManagedAttribute (description = "Flag that indicates whether the alternate frequency is in effect due to recent error while collection")
	public boolean isFallbackFrequencyActivated() {
		return fallbackFrequencyActivated;
	}
	@ManagedAttribute (description = "Maximum number of attempts that will be made to restart a collector whose initial start has failed")
	public int getMaxRestartAttempts() {
		return maxRestartAttempts;
	}	

	@ManagedAttribute (description = "The root tracing namespace where all collected metrics will be traced to")
	public String[] getTracingNameSpace() {
		return (String[])tracingNameSpace.clone();
	}

	public void setTracingNameSpace(String[] tracingNameSpace) {
		this.tracingNameSpace = tracingNameSpace;
	}

    /**
     * Returns the virtual host for this target MBeanServer
     * @return the virtual host for this target MBeanServer
     */
    @ManagedAttribute
    public String getVirtualHost() {
        return virtualHost;
    }

    /**
     * Sets the virtual host for the Target MBeanServer
     * @param virtualHost
     */
    @ManagedAttribute
    public void setVirtualHost(String virtualHost){
        virtualHost = virtualHost.trim();
        if(virtualHost.contains(".")) {
            String[] frags = virtualHost.replace(" ", "").split("\\.");
            frags = reverseArr(frags);
            this.virtualHost = org.helios.apmrouter.jmx.StringHelper.fastConcatAndDelim(".", frags);
        }else{
            this.virtualHost = virtualHost;
        }
    }


    /**
     * Returns the virtual agent for this target MBeanServer
     * @return the virtual agent for this target MBeanServer
     */
    @ManagedAttribute
    public String getVirtualAgent() {
        return virtualAgent;
    }

    /**
     * Sets the virtual agent for the Target MBeanServer
     * @param virtualAgent
     */
    @ManagedAttribute
    public void setVirtualAgent(String virtualAgent){
        this.virtualAgent = virtualAgent;
    }


	/*  ==================  CUSTOM LIFECYCLE METHODS =============================*/
	
	/**
	 * Initializes basic resources that are critical for any collector to work properly.
	 * This method cannot be overriden by concrete implementation classes.
	 *  
	 */
	public final void init() {
		setState(CollectorState.INITIALIZING);
		try{
            if(virtualHost!=null && virtualAgent!=null) {
                this.tracer = TracerFactory.getTracer(virtualHost, virtualAgent, getClass().getSimpleName()+"-"+getBeanName(), (int)(getCollectionPeriod()*1.2));
            }else{
                // virtualAgent and/or virtualHost property is not provided so switching back to default naming convention
                this.tracer = TracerFactory.getTracer(defaultVirtualHost, defaultVirtualAgent, getClass().getSimpleName()+"-"+getBeanName(), (int)(getCollectionPeriod()*1.2));
                //this.tracer = TracerFactory.getTracer();
            }
			initCollector();
			setState(CollectorState.INITIALIZED);
		} catch(Exception ex){
			setState(CollectorState.INIT_FAILED);
			if(logErrors)
				error("An error occured while initializing the collector bean: "+this.getBeanName(),ex);
		}		
	}

	private void processBlackoutInfo() {
		if(blackoutStart!=null && blackoutEnd!=null){
			blackoutInfo = new BlackoutInfo(blackoutStart, blackoutEnd, beanName);
			if(!blackoutInfo.isValidRange()){
				blackoutInfo = null;
			}
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
	 * for any custom pre startup tasks that needs to be done.
	 */
	public void preStart(){}
	
	/**
	 * This method is an entry point to kickstart any collectors' lifecycle.  It initializes and 
	 * performs startup tasks before this collector is scheduled with HeliosScheduler.
	 * This method cannot be overriden by concrete implementation classes.  
	 *  
	 */
	@ManagedOperation
	public final void doStart() throws Exception {
		try {
			init();
			if(getState() != CollectorState.INITIALIZED){
				error("Initialization error for bean: "+this.getBeanName() + ", so no further attempts would be made to start it.");
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
			if(notificationPublisher!=null)
				notificationPublisher.sendNotification(new Notification("org.helios.collector.state",this,notificationSerialNumber.incrementAndGet(),System.currentTimeMillis(),this.getBeanName() +" - " +getState().toString()));
			info(banner("Collector ", this.getBeanName(), " Started"));
		}catch (Exception ex){
			setState(CollectorState.START_FAILED);
			if(scheduleHandle!=null && !scheduleHandle.isCancelled())
				scheduleHandle.cancel(false);
			error("An error occured while starting the collector bean: "+this.getBeanName());
			scheduleRestart();
			throw ex;
			//executeExceptionScript();
		}
	}
	
	protected void scheduleRestart(){
		if(!(maxRestartAttempts == -1) && restartAttempts >= maxRestartAttempts){
			error(banner("Restart attempts for bean: "+this.getBeanName() + " is exhausted, so no further attempts will be made."));
			return;
		}
		restartAttempts++;
		if(restartAttemptDelayFrequency > 0L){
			scheduler.schedule(new Runnable(){
				public void run() { 
					try {
						start();
					} catch (Exception e) {}
				}
			}, restartAttemptDelayFrequency,TimeUnit.MILLISECONDS);
			info("Another attempt to start bean: "+this.getBeanName() + " will be made in "+ restartAttemptDelayFrequency + " milliseconds.");
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
		lastTimeCollectionStarted=System.currentTimeMillis();
		//Check whether blackout period is active for this collector
		debug("Collect called for bean: " + this.getBeanName());
		processBlackoutInfo();
		if(blackoutInfo!=null && blackoutInfo.isBlackoutActive()){
			info("*** Skipping collection as blackout period is active...");
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
					error("An exception occured while resetting the collector bean: "+this.getBeanName(),ex);
			}
		}
		if(getState() == CollectorState.STARTED){
			final String threadName = Thread.currentThread().getName();
			try {
				Thread.currentThread().setName("Collector[" + this.getBeanName() + "]");				
				debug("[", threadName, "] Starting collect for bean: ", this.getBeanName());
				long start = System.currentTimeMillis();
				setState(CollectorState.COLLECTING);
				//numberOfCollectorsRunning.incrementAndGet();
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
					info("*** Frequency for collector: " + this.getBeanName() +" is switched back to normal now.");
				}
				//debug("Completed collect for bean: ", this.getBeanName());
				info("[", threadName, "] Completed collect for bean [", this.getBeanName(), "] in [", (System.currentTimeMillis()-start), "] ms.");
			} catch (Exception ex){
				lastTimeCollectionFailed=System.currentTimeMillis();
				errors=true;
				totalFailureCount++;
				consecutiveFailureCount++;
				if(consecutiveFailureCount>=failureThreshold && !fallbackFrequencyActivated){
					info("*** Slowing down the collect frequency for bean: " + this.getBeanName() +" as it has exceeded the collectFailureThreshold parameter.");
					fallbackFrequencyActivated=true;
				}				
//				this.sendNotification(new Notification("org.helios.collector.exception.notification",this,notificationSerialNumber.incrementAndGet(),lastTimeCollectionFailed,this.getBeanName()));
				if(logErrors)
					error("Collection failed for bean collector: "+this.getBeanName(),ex);
				//executeExceptionScript();
			}finally {
				try {
					totalCollectionCount++;
					setState(currState);
					if(!errors){
						postCollect();
						lastTimeCollectionCompleted=System.currentTimeMillis();
						lastCollectionElapsedTime = lastTimeCollectionCompleted - lastTimeCollectionStarted;
						debug("Last Collection Elapsed Time: " + lastCollectionElapsedTime + " milliseconds");
						tracer.getDirectTracer().traceGauge(lastCollectionElapsedTime, "ElapsedTime", getTracingNameSpace());
					}
					if(logCollectionResult) 
						logCollectionResultDetails(collectionResult);
					if(collectorLock.isLocked())
						collectorLock.unlock();
					//numberOfCollectorsRunning.decrementAndGet();
					
				} finally {
					Thread.currentThread().setName(threadName);
				}
			}	
		} else {
			trace("Not executing collect method as the collector state is not STARTED.");
		}
	}

	/**
	 * @param collectionResult 
	 * 
	 */
	private void logCollectionResultDetails(CollectionResult collectionResult) {
		debug(collectionResult);
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
	@ManagedOperation
	public final void doStop() {
		if(getState()==CollectorState.STOPPED || getState()==CollectorState.STOPPING)
			return;
		setState(CollectorState.STOPPING);
		try {
			preStop();
			unscheduleCollect();
			stopCollector();
			postStop();
			setState(CollectorState.STOPPED);
			numberOfActiveCollectors.decrementAndGet();
			if(notificationPublisher!=null)
				notificationPublisher.sendNotification(new Notification("org.helios.collector.state",this,notificationSerialNumber.incrementAndGet(),System.currentTimeMillis(),this.getBeanName() +" - " +getState().toString()));
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
		long collectPeriod = getCollectionPeriod();
		final UncaughtExceptionHandler ueh = this;
		scheduleHandle = scheduler.scheduleAtFixedRate(new Runnable(){
			public void run() { Thread.currentThread().setUncaughtExceptionHandler(ueh); call(); }
		}, 0,collectPeriod, TimeUnit.MILLISECONDS);
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
		return name.replace('/', '-');
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
		return name.replace('/', '-');
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

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		error("Uncaught Exception in Scheduled Task on Thread [", t, "]", e);
		e.printStackTrace(System.err);
		
	}

    /**
     * Reverses the order of the passed array
     * @param arr The array to reverse
     * @return The reversed order array
     */
    protected String[] reverseArr(String[] arr) {
        String[] ret = new String[arr.length];

        for(int ri = arr.length-1, i = 0; i < arr.length; i++, ri--) {
            ret[ri] = arr[i];
        }
        return ret;
    }

//	public void handleNotification(Notification notification, Object handback) {}
	
}