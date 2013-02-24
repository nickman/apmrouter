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

import static org.helios.apmrouter.catalog.jdbc.h2.MetricTrigger.NEW_METRIC_EVENT;
import static org.helios.apmrouter.catalog.jdbc.h2.MetricTrigger.STATE_CHANGE_METRIC_EVENT;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.helios.apmrouter.catalog.domain.Metric;
import org.helios.apmrouter.catalog.jdbc.h2.MetricTrigger;
import org.helios.apmrouter.catalog.jdbc.h2.NewElementTriggers;
import org.helios.apmrouter.dataservice.json.JsonResponse;
import org.helios.apmrouter.dataservice.json.marshalling.JSONMarshaller;
import org.helios.apmrouter.metric.IMetric;
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

public class MetricURISubscriptionService extends ServerComponentBean implements Runnable, UncaughtExceptionHandler, ThreadFactory, MetricURISubscriptionServiceMXBean {
	/** The number of threads to concurrently process the metric event queue  */
	protected int metricQueueThreadCount = DEFAULT_METRIC_QUEUE_THREAD_COUNT;
	/** A serial number factory for metric queue processor threads */
	protected final AtomicInteger serial = new AtomicInteger();
	/** A thread group for metric queue processor threads */
	protected final ThreadGroup metricQueueThreadGroup = new ThreadGroup("MetricQueueProcessorThreads");
	/** Flag to indicate if the worker threads should keep running */
	protected boolean keepRunning = false;
	/** The hibernate session factory */
	protected SessionFactory sessionFactory = null;
	/** The catalog data source */
	protected DataSource catalogDataSource = null;
	
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
		Runnable newEventProcessor = new NewMetricEventProcessor();
		for(int i = 0; i < metricQueueThreadCount; i++) {
			newThread(newEventProcessor).start();
		}
		info("Started [", metricQueueThreadCount, "] Metric Event Queue Processing Threads");
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStop()
	 */
	@Override
	protected void doStop() {
		keepRunning = false;
		metricQueueThreadGroup.interrupt();
		info("Interrupted Metric Event Queue Processing ThreadGroup for stop");
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
			if(session!=null) try { session.close(); } catch (Exception ex) {}
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
	 * @see org.helios.apmrouter.dataservice.json.catalog.MetricURISubscriptionServiceMXBean#getMetricQueueThreadCount()
	 */
	@Override
	@ManagedAttribute(description="The number of threads to concurrently process the metric event queue")
	public int getMetricQueueThreadCount() {
		return metricQueueThreadCount;
	}
	/**
	 * Sets the number of threads to concurrently process the metric event queue
	 * @param metricQueueThreadCount the number of threads to concurrently process the metric event queue
	 */
	public void setMetricQueueThreadCount(int metricQueueThreadCount) {
		this.metricQueueThreadCount = metricQueueThreadCount;
	}
	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
	 */
	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(metricQueueThreadGroup, r, "MetricQueueProcessorThread#" + serial.incrementAndGet());
		t.setDaemon(true);
		t.setUncaughtExceptionHandler(this);
		return t;
	}
	/**
	 * {@inheritDoc}
	 * @see java.lang.Thread.UncaughtExceptionHandler#uncaughtException(java.lang.Thread, java.lang.Throwable)
	 */
	@Override
	public void uncaughtException(Thread t, Throwable e) {
		error("Failed to process metric queue entry on thread [", t, "]", e);
		incr("MetricQueueProcessingErrors");
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.dataservice.json.catalog.MetricURISubscriptionServiceMXBean#getMetricQueueProcessingErrors()
	 */
	@Override
	@ManagedMetric(category="MetricURISubscriptionService", displayName="MetricQueueProcessingErrors", metricType=MetricType.COUNTER, description="The number of errors processing the metric queued events")
	public long getMetricQueueProcessingErrors() {
		return getMetricValue("MetricQueueProcessingErrors");
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.dataservice.json.catalog.MetricURISubscriptionServiceMXBean#getMetricQueueDepth()
	 */
	@Override
	@ManagedMetric(category="MetricURISubscriptionService", displayName="MetricQueueDepth", metricType=MetricType.COUNTER, description="The number of pending metric queued events")
	public long getMetricQueueDepth() {
		return NewElementTriggers.notificationQueue.size();
	}
	
	// STATE_CHANGE_METRIC_EVENT, newRow[METRIC_COLUMN_ID], newRow[STATE_COLUMN_ID]
	// NEW_METRIC_EVENT, newRow[METRIC_COLUMN_ID]	
	// org.helios.apmrouter.catalog.jdbc.h2.MetricTrigger
	
	
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
		 * {@inheritDoc}
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			while(keepRunning) {
				try {	
					Object[] newMetricEvent = NewElementTriggers.newMetricQueue.take();
					long metricId = (Long)newMetricEvent[MetricTrigger.METRIC_COLUMN_ID];
					int metricType = ((Number)newMetricEvent[MetricTrigger.TYPE_COLUMN_ID]).intValue();
					long mask = MetricURI.mask(metricType, (byte)newMetricEvent[MetricTrigger.STATE_COLUMN_ID], MetricURISubscriptionType.NEW_METRIC.getCode());
					Iterator<MetricURISubscription> subIter = MetricURISubscription.getSubscriptionsForSubType(mask);
					if(subIter==null) continue;
					Set<MetricURISubscription> candidates = new HashSet<MetricURISubscription>();
					while(subIter.hasNext()) {
						MetricURISubscription sub = subIter.next();
						if(sub.newMetricCandidacy(metricId, _catalogDataSource)) {
							candidates.add(sub);
						}						
					}
					if(candidates.isEmpty()) continue;
					Session session = null;
					Metric metric = null;
					try {
						session = _sessionFactory.openSession();
						metric = (Metric)session.get(Metric.class, metricId);
					} catch (Exception ex) {
						error("Failed to resolve metricId to Metric through hibernate", ex);
						continue;
					} finally {
						if(session!=null) try { session.close(); } catch (Exception x) {/* No Op*/}
					}
					if(metric==null) continue;
					for(MetricURISubscription sub: candidates) {
						sub.sendSubscribersNewMetric(metric);
					}
				} catch (Exception ex) {
					if(Thread.interrupted()) Thread.interrupted();
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		info("Started MetricQueueThreadProcessor");
		while(keepRunning) {
			try {			
				Object[] newMetricEvent = NewElementTriggers.metricStateChangeQueue.take();
				long metricId = (Long)newMetricEvent[MetricTrigger.METRIC_COLUMN_ID];
				if(STATE_CHANGE_METRIC_EVENT.equals(newMetricEvent[0])) {
					byte newState = (Byte)newMetricEvent[2];
				} else if(NEW_METRIC_EVENT.equals(newMetricEvent[0])) {
					
				}
//				if(notification.getType().startsWith(MetricTrigger.NEW_METRIC)) {
//					Object[] newRow = (Object[])notification.getUserData();
//					String FQN = notification.getType().replace(MetricTrigger.NEW_METRIC, "");
//					System.err.println("Processing new metric notification [" + FQN + "]");
//					long metricId = (Long)newRow[MetricTrigger.METRIC_COLUMN_ID];
//					MetricURISubscription.onNewMetric(metricId, FQN, newRow, catalogDataSource);
//				}
				// process
			} catch (Exception ex) {
				if(Thread.interrupted()) Thread.interrupted();
			}			
		}
	}
	

}
