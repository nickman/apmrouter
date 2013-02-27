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
package org.helios.apmrouter.dataservice.json.catalog;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.management.ManagementFactory;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.helios.apmrouter.catalog.EntryStatus;
import org.helios.apmrouter.catalog.domain.Metric;
import org.helios.apmrouter.catalog.jdbc.h2.MetricTrigger;
import org.helios.apmrouter.catalog.jdbc.h2.NewElementTriggers;
import org.helios.apmrouter.collections.ConcurrentLongSlidingWindow;
import org.helios.apmrouter.dataservice.json.JsonResponse;
import org.helios.apmrouter.dataservice.json.marshalling.JSONMarshaller;
import org.helios.apmrouter.metric.catalog.ICEMetricCatalog;
import org.helios.apmrouter.metric.catalog.IMetricCatalog;
import org.helios.apmrouter.server.ServerComponentBean;
import org.helios.apmrouter.util.SystemClock;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jboss.netty.channel.Channel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;

/**
 * <p>Title: MetricURISubscriptionService</p>
 * <p>Description: Service to manage subscriptions to metric events in the form of {@link MetricURI}s.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.dataservice.json.catalog.MetricURISubscriptionService</code></p>
 */

public class MetricURISubscriptionService extends ServerComponentBean implements UncaughtExceptionHandler, MetricURISubscriptionServiceMXBean {
	/** Flag to indicate if the worker threads should keep running */
	protected boolean keepRunning = false;
	/** The hibernate session factory */
	protected SessionFactory sessionFactory = null;
	/** The catalog data source */
	protected DataSource catalogDataSource = null;
	
	/** The number of new metric event processing threads */
	protected int newMetricEventThreads = 1;
	/** The number of metric state change event processing threads */
	protected int metricStateChangeEventThreads = 1;
	/** A serial number factory for new metric queue processor threads */
	protected final AtomicInteger newMetricSerial = new AtomicInteger();
	/** A serial number factory for metric state change event queue processor threads */
	protected final AtomicInteger metricStateChangeSerial = new AtomicInteger();
	
	/** The new metric event processing thread group */
	protected final ThreadGroup newMetricEventThreadGroup = new ThreadGroup("NewMetricEventThreadGroup");
	/** The metric state change event processing thread group */
	protected final ThreadGroup metricStateChangeEventThreadGroup = new ThreadGroup("MetricStateChangeEventThreadGroup");
	
	/** A sliding window of the last 50 elapsed times for new metric event processing in ns. */
	protected final ConcurrentLongSlidingWindow newMetricEventProcessingTime = new ConcurrentLongSlidingWindow(50); 
	/** A sliding window of the last 50 elapsed times for metric state change event processing in ns. */
	protected final ConcurrentLongSlidingWindow metricStateChangeEventProcessingTime = new ConcurrentLongSlidingWindow(50);
	
	
	
	/** The metric catalog */
	protected final IMetricCatalog metricCatalog = ICEMetricCatalog.getInstance();
	
	

	/** The Json marshaller */
	protected JSONMarshaller marshaller = null;
	
	
	
	/** The default number of threads to concurrently process the metric event queue */
	public static final int DEFAULT_METRIC_QUEUE_THREAD_COUNT = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStart()
	 */
	@Override
	protected void doStart() throws Exception {		
		keepRunning = true;
		resetMetrics();
		startNewMetricEventProcessor();
		startMetricStateChangeEventProcessor();		
	}
	
	/**
	 * Starts the new metric event queue processor threads
	 */
	protected void startNewMetricEventProcessor() {
		for(int i = 0; i < newMetricEventThreads; i++) {
			Thread t = new Thread(newMetricEventThreadGroup, new NewMetricEventProcessor(), "NewMetricEventProcessorThread#" + newMetricSerial.incrementAndGet());
			t.setDaemon(true);
			t.setUncaughtExceptionHandler(this);
			t.start();
		}
		info("Started [", newMetricEventThreads, "] New Metric Event Queue Processing Threads");
	}
	
	/**
	 * Starts the metric state change event queue processor threads
	 */
	protected void startMetricStateChangeEventProcessor() {
		for(int i = 0; i < metricStateChangeEventThreads; i++) {
			Thread t = new Thread(metricStateChangeEventThreadGroup, new MetricStateChangeEventProcessor(), "MetricStateChangeEventProcessorThread#" + metricStateChangeSerial.incrementAndGet());
			t.setDaemon(true);
			t.setUncaughtExceptionHandler(this);
			t.start();
		}		
		info("Started [", metricStateChangeEventThreads, "] Metric State Change Event Queue Processing Threads");
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStop()
	 */
	@Override
	protected void doStop() {
		keepRunning = false;
		metricStateChangeEventThreadGroup.interrupt();
		newMetricEventThreadGroup.interrupt();
		info("Interrupted Metric Event Queue Processor ThreadGroups for stop");
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.dataservice.json.catalog.MetricURISubscriptionServiceMXBean#getSubscriptions()
	 */
	@Override
	public MetricURISubscription[] getSubscriptions() {
		return MetricURISubscription.subscriptions.values().toArray(new MetricURISubscription[MetricURISubscription.subscriptions.size()]);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.dataservice.json.catalog.MetricURISubscriptionServiceMXBean#getSubscriptionCount()
	 */
	@Override
	public int getSubscriptionCount() {
		return MetricURISubscription.subscriptions.size();
	}
	
	/**
	 * Subscribes the passed {@link Channel} to the specified {@link MetricURI}.
	 * @param metricUri The {@link MetricURI} to subscribe to
	 * @param response The json response to format the response (with the correct RID)
	 * @param channel The {@link Channel} to subscribe
	 */
	public void subscribeMetricURI(MetricURI metricUri, JsonResponse response, Channel channel) {
		Session session = null;
		try {
			session = sessionFactory.openSession();
			MetricURISubscription sub = MetricURISubscription.getMetricURISubscription(session, metricUri);
			sub.subscribeChannel(channel, response);
		} finally {
			if(session!=null) try { session.close(); } catch (Exception ex) {/* No Op */}
		}
	}
	
	
	/**
	 * Unsubscribes the passed {@link Channel} from the subscription to the specified {@link MetricURI}.
	 * @param metricUri The {@link MetricURI} to unsubscribe from
	 * @param response The json response to format the response (with the correct RID)
	 * @param channel The {@link Channel} to unsubscribe
	 */
	public void cancelMetricURISubscription(MetricURI metricUri, JsonResponse response, Channel channel) {
		MetricURISubscription sub = MetricURISubscription.getMetricURISubscriptionOrNull(metricUri);
		if(sub!=null) {
			sub.unSubscribeChannel(channel);
		}
	}
	
	/**
	 * Processes the resolution of a client supplied {@link MetricURI} into a list of matching metrics
	 * @param metricUri The metric URI to resolve
	 * @param response The json response to format the response (with the correct RID)
	 * @param channel The channel to write the response to
	 * @param subscribe If true, also subscribes the session to the metric URI, otherwise only retrieves the data
	 */
	public void resolveMetricURI(MetricURI metricUri, JsonResponse response, Channel channel, boolean subscribe) {
		Session session = null;
		try {
			SystemClock.startTimer();
			//session = sessionFactory.openSession(new org.helios.apmrouter.catalog.api.impl.DataServiceInterceptor());
			session = sessionFactory.openSession();
			//Object obj = metricUri.execute(session).toArray(new DomainObject[0]);			
			List<Metric> metrics = metricUri.execute(session);
			channel.write(response.setContent(metrics));
			if(subscribe) subscribeMetricURI(metricUri, response, channel);
			info("Metric URI Query ", SystemClock.endTimer());
		} finally {
			if(session!=null && session.isOpen()) try { session.close(); } catch (Exception e) {/* No Op */}
		}
	}
	
	/**
	 * Sets the object Json marshaller
	 * @param marshaller the object Json marshaller
	 */
	@Autowired(required=true)
	public void setMarshaller(JSONMarshaller marshaller) {
		this.marshaller = marshaller;
	}
	
	/**
	 * Sets the hibernate session factory
	 * @param sessionFactory the hibernate session factory
	 */
	@Autowired(required=true)
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}	
	
	/**
	 * Sets the catalog data source
	 * @param catalogDataSource the catalog DataSource to set
	 */
	@Autowired(required=true)
	public void setCatalogDataSource(DataSource catalogDataSource) {
		this.catalogDataSource = catalogDataSource;
	}
	

	/**
	 * {@inheritDoc}
	 * @see java.lang.Thread.UncaughtExceptionHandler#uncaughtException(java.lang.Thread, java.lang.Throwable)
	 */
	@Override
	public void uncaughtException(Thread t, Throwable e) {
		error("Failed to process metric queue entry on thread [", t, "]", e);		
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.dataservice.json.catalog.MetricURISubscriptionServiceMXBean#getNewMetricQueueProcessingErrors()
	 */
	@Override
	@ManagedMetric(category="MetricURISubscriptionService", displayName="NewMetricQueueProcessingErrors", metricType=MetricType.COUNTER, description="The number of errors processing the new metric queued events")
	public long getNewMetricQueueProcessingErrors() {
		return getMetricValue("NewMetricQueueProcessingErrors");
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.dataservice.json.catalog.MetricURISubscriptionServiceMXBean#getMetricStateChangeQueueProcessingErrors()
	 */
	@Override
	@ManagedMetric(category="MetricURISubscriptionService", displayName="MetricStateChangeQueueProcessingErrors", metricType=MetricType.COUNTER, description="The number of errors processing the metric state change queued events")
	public long getMetricStateChangeQueueProcessingErrors() {
		return getMetricValue("MetricStateChangeQueueProcessingErrors");
	}
	
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.dataservice.json.catalog.MetricURISubscriptionServiceMXBean#getNewMetricEventQueueDepth()
	 */
	@Override
	@ManagedMetric(category="MetricURISubscriptionService", displayName="NewMetricEventQueueDepth", metricType=MetricType.GAUGE, description="The number of pending new metric queued events")
	public long getNewMetricEventQueueDepth() {
		return NewElementTriggers.newMetricQueue.size();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.dataservice.json.catalog.MetricURISubscriptionServiceMXBean#getMetricStateChangeEventQueueDepth()
	 */
	@Override
	@ManagedMetric(category="MetricURISubscriptionService", displayName="MetricStateChangeEventQueueDepth", metricType=MetricType.GAUGE, description="The number of pending metric state change queued events")
	public long getMetricStateChangeEventQueueDepth() {
		return NewElementTriggers.metricStateChangeQueue.size();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.dataservice.json.catalog.MetricURISubscriptionServiceMXBean#getMetricStateChangeBroadcasts()
	 */
	@Override
	@ManagedMetric(category="MetricURISubscriptionService", displayName="MetricStateChangeBroadcasts", metricType=MetricType.GAUGE, description="The number of broadcast metric state change events")
	public long getMetricStateChangeBroadcasts() {
		return getMetricValue("MetricStateChangeBroadcasts");
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.dataservice.json.catalog.MetricURISubscriptionServiceMXBean#getNewMetricBroadcasts()
	 */
	@Override
	@ManagedMetric(category="MetricURISubscriptionService", displayName="NewMetricBroadcasts", metricType=MetricType.GAUGE, description="The number of broadcast new metric events")
	public long getNewMetricBroadcasts() {
		return getMetricValue("NewMetricBroadcasts");
	}
	
	
	
	
	
	/**
	 * <p>Title: NewMetricEventProcessor</p>
	 * <p>Description: Queue processor for new metric events</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.dataservice.json.catalog.MetricURISubscriptionService.NewMetricEventProcessor</code></p>
	 */
	protected class NewMetricEventProcessor implements Runnable {
		/** The hibernate session factory */
		final SessionFactory _sessionFactory = sessionFactory;
		/** The catalog data source */
		final DataSource _catalogDataSource = catalogDataSource;
		
		/**
		 * Consumes events from the new metric queue and processes them as follows to find interested subscribers for the consumed new metric event:<ol>
		 * <li>Executes a 2 phase lookup to find interested subscribers:<ol>
		 * 		<li>Builds a {@link MetricType}/{@link EntryStatus}/{@link MetricURISubscriptionType} representing the consumed event.</li>
		 * 		<li>Acquires an iterator of {@link MetricURISubscription} instances that match the created mask. If none, drop event.</li> 		
		 * 		<li>For each {@link MetricURISubscription} in the returned iterator:<ol>
		 * 			<li>If the metric ID is already in the subscription's metric id set (unlikely), then drops the event.</li>
		 * 			<li>If the metric ID is <b>not</b> in the subscription's metric id set, the subscription's criteria query is executed to see if the new metric id is a member of the subscription.</li>
		 * 			<li>Once it has been determined that there is at least one interested subscriber, the event is resolved into the actual {@link Metric} instance.</li>
		 * 			<li>If the metric ID <b>is</b> a member of the subscription's criteria, it is added to the subscription's metric id set and the {@link Metric} instance is published to the subscriber.</li>
		 * 		</ol></li>
		 * </ol></li>		
		 * </ol>
		 * {@inheritDoc}
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			while(keepRunning) {
				try {	
					Object[] newMetricEvent = NewElementTriggers.newMetricQueue.take();
					incr("NewMetricEvents");
					final long startTime = System.nanoTime();
					long metricId = (Long)newMetricEvent[MetricTrigger.METRIC_COLUMN_ID];
					int metricType = ((Number)newMetricEvent[MetricTrigger.TYPE_COLUMN_ID]).intValue();
					Iterator<MetricURISubscription> subIter = MetricURISubscription.getMatchingSubscriptions(
							org.helios.apmrouter.metric.MetricType.valueOf(metricType).getMask(),
							EntryStatus.ALL_STATUS_MASK,  // new metrics are always active, but we want the search to be neutral for status, so we turn all the bits on
							MetricURISubscriptionType.NEW_METRIC.getMask()							
					);
					if(subIter==null) continue;
					Metric lazyMetric = null;
					while(subIter.hasNext()) {
						MetricURISubscription subscription = subIter.next();
						if(subscription.hasMetricId(metricId)) {
							continue;
						}
						if(!subscription.metricCandidacy(metricId, _catalogDataSource)) {
							continue;
						}
						subscription.addMetricId(metricId);
						if(lazyMetric==null) {
							lazyMetric = getMetric(metricId, _sessionFactory);
							if(lazyMetric==null) {
								incr("NewMetricQueueProcessingErrors");
								break;  //  we don't want to handle this error here.
							}
						}
						subscription.sendSubscribersNewMetric(lazyMetric);
						incr("NewMetricBroadcasts");
					}
					final long elapsed = System.nanoTime() - startTime;
					newMetricEventProcessingTime.insert(elapsed);
				} catch (Exception ex) {
					if(Thread.interrupted()) Thread.interrupted();
					if(keepRunning) {
						incr("NewMetricQueueProcessingErrors");
					}
				}
			}
		}
	}
	
	/**
	 * <p>Title: MetricStateChangeEventProcessor</p>
	 * <p>Description: Queue processor for metric state change events</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.dataservice.json.catalog.MetricURISubscriptionService.MetricStateChangeEventProcessor</code></p>
	 */
	protected class MetricStateChangeEventProcessor implements Runnable {
		/** The hibernate session factory */
		final SessionFactory _sessionFactory = sessionFactory;
		/** The catalog data source */
		final DataSource _catalogDataSource = catalogDataSource;
		
		/**
		 * Consumes events from the metric state change event queue and processes them as follows to find interested subscribers for the consumed metric state change event:<ol>
		 * <li>Executes a 2 phase lookup to find interested subscribers:<ol>
		 * 		<li>Builds a {@link MetricType}/{@link EntryStatus}/{@link MetricURISubscriptionType} representing the consumed event.</li>
		 * 		<li>Acquires an iterator of {@link MetricURISubscription} instances that match the created mask. If none, drop event.</li> 		
		 * 		<li>For each {@link MetricURISubscription} in the returned iterator:<ol>
		 * 			<li>If the metric ID is <b>not</b> in the subscription's metric id set, the subscription's criteria query is executed to see if the new metric id is a member of the subscription.</li>
		 * 			<li>Once it has been determined that there is at least one interested subscriber, the event is resolved into the actual {@link Metric} instance.</li>
		 * 			<li>If the metric ID <b>is</b> a member of the subscription's criteria, it is added to the subscription's metric id set and the {@link Metric} instance is published to the subscriber.</li>
		 * 		</ol></li>
		 * </ol></li>		
		 * </ol>
		 * {@inheritDoc}
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			while(keepRunning) {
				try {	
					Object[] metricStateChangeEvent = NewElementTriggers.metricStateChangeQueue.take();
					incr("MetricStateChangeEvents");
					final long startTime = System.nanoTime();
					long metricId = (Long)metricStateChangeEvent[MetricTrigger.METRIC_COLUMN_ID];
					int metricType = ((Number)metricStateChangeEvent[MetricTrigger.TYPE_COLUMN_ID]).intValue();
					byte newState = ((Number)metricStateChangeEvent[MetricTrigger.STATE_COLUMN_ID]).byteValue();
					EntryStatus newStatus = EntryStatus.forByte(newState);
					Iterator<MetricURISubscription> subIter = MetricURISubscription.getMatchingSubscriptions(
							org.helios.apmrouter.metric.MetricType.valueOf(metricType).getMask(),
							newStatus.getMask(),
							MetricURISubscriptionType.STATE_CHANGE.getMask()							
					);
					if(subIter==null) continue;
					Metric lazyMetric = null;
					while(subIter.hasNext()) {
						MetricURISubscription subscription = subIter.next();
						if(subscription.hasMetricId(metricId)) {
							continue;
						}
						if(!subscription.metricCandidacy(metricId, _catalogDataSource)) {
							continue;
						}
						subscription.addMetricId(metricId);
						if(lazyMetric==null) {
							lazyMetric = getMetric(metricId, _sessionFactory);
							if(lazyMetric==null) {
								incr("MetricStateChangeQueueProcessingErrors");
								break;  //  we don't want to handle this error here.
							}
						}
						subscription.sendSubscribersMetricStateChange(newStatus.name(), lazyMetric);
						incr("MetricStateChangeBroadcasts");
					}
					final long elapsed = System.nanoTime() - startTime;
					metricStateChangeEventProcessingTime.insert(elapsed);

				} catch (Exception ex) {
					if(Thread.interrupted()) Thread.interrupted();
					if(keepRunning) {
						incr("MetricStateChangeQueueProcessingErrors");
					}
				}
			}
		}
	}
	
	
	/**
	 * Looks up the {@link Metric} for the passed metric ID
	 * @param metricId The metric ID to look up
	 * @param sf The factory to provide the lookup session
	 * @return the located metric or null
	 */
	public Metric getMetric(long metricId, SessionFactory sf) {
		Session session = null;
		try {
			session = sf.openSession();
			//return (Metric)session.get(Metric.class, metricId);
			return (Metric)session.get("metric", metricId);
		} catch (Exception ex) {
			error("Failed to resolve metricId to Metric through hibernate", ex);
			return null;
		} finally {
			if(session!=null) try { session.close(); } catch (Exception x) {/* No Op*/}
		}		
	}

	/**
	 * Returns the number of new metric event queue processing threads
	 * @return the number of new metric event queue processing threads
	 */
	@Override
	@ManagedAttribute(description="The number of threads to concurrently process the new metric event queue")
	public int getNewMetricEventThreads() {
		return newMetricEventThreads;
	}

	/**
	 * Sets the number of new metric event queue processing threads 
	 * @param newMetricEventThreads the number of new metric event queue processing threads
	 */
	@Override
	@ManagedAttribute(description="The number of threads to concurrently process the new metric event queue")
	public void setNewMetricEventThreads(int newMetricEventThreads) {
		this.newMetricEventThreads = newMetricEventThreads;
	}

	/**
	 * Returns the number of metric state change event queue processing threads
	 * @return the number of metric state change event queue processing threads
	 */
	@Override
	@ManagedAttribute(description="The number of threads to concurrently process the metric state change event queue")
	public int getMetricStateChangeEventThreads() {
		return metricStateChangeEventThreads;
	}

	/**
	 * Sets the number of metric state change event queue processing threads
	 * @param metricStateChangeEventThreads the number of metric state change event queue processing threads
	 */
	@Override
	@ManagedAttribute(description="The number of threads to concurrently process the metric state change event queue")
	public void setMetricStateChangeEventThreads(int metricStateChangeEventThreads) {
		this.metricStateChangeEventThreads = metricStateChangeEventThreads;
	}

//	/**
//	 * {@inheritDoc}
//	 * @see java.lang.Runnable#run()
//	 */
//	@Override
//	public void run() {
//		info("Started MetricQueueThreadProcessor");
//		while(keepRunning) {
//			try {			
//				Object[] newMetricEvent = NewElementTriggers.metricStateChangeQueue.take();
//				long metricId = (Long)newMetricEvent[MetricTrigger.METRIC_COLUMN_ID];
//				if(STATE_CHANGE_METRIC_EVENT.equals(newMetricEvent[0])) {
//					byte newState = (Byte)newMetricEvent[2];
//				} else if(NEW_METRIC_EVENT.equals(newMetricEvent[0])) {
//					
//				}
////				if(notification.getType().startsWith(MetricTrigger.NEW_METRIC)) {
////					Object[] newRow = (Object[])notification.getUserData();
////					String FQN = notification.getType().replace(MetricTrigger.NEW_METRIC, "");
////					System.err.println("Processing new metric notification [" + FQN + "]");
////					long metricId = (Long)newRow[MetricTrigger.METRIC_COLUMN_ID];
////					MetricURISubscription.onNewMetric(metricId, FQN, newRow, catalogDataSource);
////				}
//				// process
//			} catch (Exception ex) {
//				if(Thread.interrupted()) Thread.interrupted();
//			}			
//		}
//	}

	/**
	 * Returns the rolling average of the last 50 new metric event processing elapsed times in ns.
	 * @return the rolling average of the last 50 new metric event processing elapsed times in ns.
	 */
	@Override
	@ManagedMetric(category="MetricURISubscriptionNewMetrics", displayName="AverageNewMetricProcessingTimeNs", metricType=MetricType.GAUGE, description="The rolling average of the last 50 new metric event processing elapsed times in ns.")
	public long getAverageNewMetricProcessingTimeNs() {
		return newMetricEventProcessingTime.avg();
	}
	
	/**
	 * Returns the rolling average of the last 50 new metric event processing elapsed times in ms.
	 * @return the rolling average of the last 50 new metric event processing elapsed times in ms.
	 */
	@Override
	@ManagedMetric(category="MetricURISubscriptionNewMetrics", displayName="AverageNewMetricProcessingTimeMs", metricType=MetricType.GAUGE, description="The rolling average of the last 50 new metric event processing elapsed times in ms.")
	public long getAverageNewMetricProcessingTimeMs() {
		return TimeUnit.MILLISECONDS.convert(newMetricEventProcessingTime.avg(), TimeUnit.NANOSECONDS);
	}
	
	/**
	 * Returns the last metric event processing elapsed time in ns.
	 * @return the last metric event processing elapsed time in ns.
	 */
	@Override
	@ManagedMetric(category="MetricURISubscriptionNewMetrics", displayName="LastNewMetricProcessingTimeNs", metricType=MetricType.GAUGE, description="The last new metric event processing elapsed time in ns.")
	public long getLastNewMetricProcessingTimeNs() {
		return newMetricEventProcessingTime.getNewest(); 
	}
	
	/**
	 * Returns the last metric event processing elapsed time in ms.
	 * @return the last metric event processing elapsed time in ms.
	 */
	@Override
	@ManagedMetric(category="MetricURISubscriptionNewMetrics", displayName="LastNewMetricProcessingTimeMs", metricType=MetricType.GAUGE, description="The last new metric event processing elapsed time in ms.")
	public long getLastNewMetricProcessingTimeMs() {
		return TimeUnit.MILLISECONDS.convert(getLastNewMetricProcessingTimeNs(), TimeUnit.NANOSECONDS); 
	}
	
	//===============================================================

	/**
	 * Returns the rolling average of the last 50 metric state change processing elapsed times in ns.
	 * @return the rolling average of the last 50 metric state change processing elapsed times in ns.
	 */
	@Override
	@ManagedMetric(category="MetricURISubscriptionStatChanges", displayName="AverageMetricStateChangeProcessingTimeNs", metricType=MetricType.GAUGE, description="The rolling average of the last 50 metric state change processing elapsed times in ns.")
	public long getAverageMetricStateChangeProcessingTimeNs() {
		return metricStateChangeEventProcessingTime.avg();
	}
	
	/**
	 * Returns the rolling average of the last 50 metric state change processing elapsed times in ms.
	 * @return the rolling average of the last 50 metric state change processing elapsed times in ms.
	 */
	@Override
	@ManagedMetric(category="MetricURISubscriptionStatChanges", displayName="AverageMetricStateChangeProcessingTimeMs", metricType=MetricType.GAUGE, description="The rolling average of the last 50 metric state change processing elapsed times in ms.")
	public long getAverageMetricStateChangeProcessingTimeMs() {
		return TimeUnit.MILLISECONDS.convert(metricStateChangeEventProcessingTime.avg(), TimeUnit.NANOSECONDS);
	}
	
	/**
	 * Returns the last metric event processing elapsed time in ns.
	 * @return the last metric event processing elapsed time in ns.
	 */
	@Override
	@ManagedMetric(category="MetricURISubscriptionStatChanges", displayName="LastMetricStateChangeProcessingTimeNs", metricType=MetricType.GAUGE, description="The last metric state change processing elapsed time in ns.")
	public long getLastMetricStateChangeProcessingTimeNs() {
		return metricStateChangeEventProcessingTime.getNewest(); 
	}
	
	/**
	 * Returns the last metric event processing elapsed time in ms.
	 * @return the last metric event processing elapsed time in ms.
	 */
	@Override
	@ManagedMetric(category="MetricURISubscriptionStatChanges", displayName="LastMetricStateChangeProcessingTimeMs", metricType=MetricType.GAUGE, description="The last metric state change processing elapsed time in ms.")
	public long getLastMetricStateChangeProcessingTimeMs() {
		return TimeUnit.MILLISECONDS.convert(getLastMetricStateChangeProcessingTimeNs(), TimeUnit.NANOSECONDS); 
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.dataservice.json.catalog.MetricURISubscriptionServiceMXBean#getNewMetricEvents()
	 */
	@Override
	@ManagedMetric(category="MetricURISubscriptionNewMetrics", displayName="NewMetricEvents", metricType=MetricType.COUNTER, description="The total number of new metric events received")
	public long getNewMetricEvents() {
		return getMetricValue("NewMetricEvents");
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.dataservice.json.catalog.MetricURISubscriptionServiceMXBean#getMetricStateChangeEvents()
	 */
	@Override
	@ManagedMetric(category="MetricURISubscriptionStatChanges", displayName="MetricStateChangeEvents", metricType=MetricType.COUNTER, description="The total number of metric state change events received")
	public long getMetricStateChangeEvents() {
		return getMetricValue("MetricStateChangeEvents");
	}
	


}
