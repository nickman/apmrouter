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
package org.helios.apmrouter.destination.mongodb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.Notification;

import org.helios.apmrouter.catalog.domain.Agent;
import org.helios.apmrouter.catalog.domain.Host;
import org.helios.apmrouter.catalog.domain.Metric;
import org.helios.apmrouter.catalog.jdbc.h2.AbstractTrigger;
import org.helios.apmrouter.catalog.jdbc.h2.NewElementTriggers;
import org.helios.apmrouter.collections.ConcurrentLongSlidingWindow;
import org.helios.apmrouter.destination.BaseDestination;
import org.helios.apmrouter.destination.accumulator.FlushQueueReceiver;
import org.helios.apmrouter.destination.accumulator.TimeSizeFlushQueue;
import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.util.SystemClock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;

import com.mongodb.Mongo;

/**
 * <p>Title: MongoDbDestination</p>
 * <p>Description: Destination for a MongoDb database. Also handles secondary catalog updates.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.mongodb.MongoDbDestination</code></p>
 */

public class MongoDbDestination extends BaseDestination implements Runnable, FlushQueueReceiver<IMetric> {
	/** The mongo DB template */
	protected MongoTemplate mongoTemplate = null;
	/** The raw mongo connection */
	protected Mongo mongo = null;
	/** The catalog update queue to read updates from */
	protected final BlockingQueue<Notification> notificationQueue = NewElementTriggers.notificationQueue;
	/** The catalog queue processor thread */
	protected Thread catalogProcessorThread = null;
	/** The maximum batch size for catalog queue reads */
	protected int catalogMaxBatchSize = 30;
	/** Thread run indicator */
	protected final AtomicBoolean keepRunning = new AtomicBoolean(false);
	/** The last catalog elapsed write time in ms */
	protected final ConcurrentLongSlidingWindow lastCatalogElapsedNs = new ConcurrentLongSlidingWindow(60);
	/** The last metric-data elapsed write time in ms */
	protected final ConcurrentLongSlidingWindow lastMetricElapsedNs = new ConcurrentLongSlidingWindow(60);
	/** The last metric-data batch size */
	protected final ConcurrentLongSlidingWindow lastBatchSize = new ConcurrentLongSlidingWindow(60);
	
	/** The time based flush trigger in ms. */
	protected long timeTrigger = 15000;
	/** The size based flush trigger in number of metrics accumulated */
	protected int sizeTrigger = 30;
	/** The time/size triggered flush queue */
	protected TimeSizeFlushQueue<IMetric> flushQueue = null;
	
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		final List<Host> hosts = new ArrayList<Host>();
		final List<Agent> agents = new ArrayList<Agent>();
		final List<Metric> metrics = new ArrayList<Metric>();
		long loopTime = SystemClock.time() + 5000;
		while(true) {
			try {
				Notification notif = notificationQueue.poll(1000, TimeUnit.MILLISECONDS);
				if(notif!=null) {
					String type = notif.getType();
					if(AbstractTrigger.NEW_HOST.equals(type)) {
						hosts.add(new Host(notif));
					} else if(AbstractTrigger.NEW_AGENT.equals(type)) {
						agents.add(new Agent(notif));
					} else if(AbstractTrigger.NEW_METRIC.equals(type)) {
						metrics.add(new Metric(notif));
					}
				}	
				if(SystemClock.time() >= loopTime) {
					try {
						boolean inserts = false;
						if(!hosts.isEmpty() || !agents.isEmpty() || !metrics.isEmpty()) {
							inserts = true;
							SystemClock.startTimer();
						}
						if(!hosts.isEmpty()) mongoTemplate.insert(hosts, Host.class);
						if(!agents.isEmpty()) mongoTemplate.insert(agents, Agent.class);
						if(!metrics.isEmpty()) mongoTemplate.insert(metrics, Metric.class);
						if(inserts) {
							lastCatalogElapsedNs.insert(SystemClock.endTimer().elapsedNs);
						}
					} finally {
						loopTime = SystemClock.time() + 5000;
					}
				}
			} catch (InterruptedException iex) {
				if(keepRunning.get()) {
					Thread.interrupted();
				} else {
					 break;
				}
			} finally {
				hosts.clear();
				agents.clear();
				metrics.clear();
			}
		}
	}
	
	/**
	 * Accept Route additive for BaseDestination extensions
	 * @param routable The metric to route
	 */
	@Override
	protected void doAcceptRoute(IMetric routable) {
		try {	
			routable.getLongValue();
			flushQueue.put(routable);
			incr("MetricsForwarded");
		} catch (Exception e) {
			incr("InvalidMetricDrops");
			//error("Invalid Metric Type [", routable, "]");
			//e.printStackTrace(System.err);
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.accumulator.FlushQueueReceiver#flushTo(java.util.Collection)
	 */
	@Override
	public void flushTo(Collection<IMetric> flushedItems) {
	
		
	}	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.BaseDestination#doStart()
	 */
	@Override
	protected void doStart() throws Exception {
		super.doStart();
		flushQueue = new TimeSizeFlushQueue<IMetric>(getClass().getSimpleName(), sizeTrigger, timeTrigger, this);
		catalogProcessorThread = new Thread(this, "MongoCatalogProcessorThread");
		catalogProcessorThread.setDaemon(true);
		catalogProcessorThread.start();
		info("Started MongoDb Catalog Processor Thread");
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponent#getSupportedMetricNames()
	 */
	@Override
	public Set<String> getSupportedMetricNames() {
		Set<String> _metrics = new HashSet<String>(super.getSupportedMetricNames());
		_metrics.add("MetricsForwarded");
		_metrics.add("InvalidMetricDrops");		
		_metrics.add("BroadcastIntervalRolls");
		return _metrics;
	}
	
	
	/**
	 * Returns the last elapsed write time in ms
	 * @return the last elapsed write time in ms
	 */
	@ManagedMetric(category="MongoDbCatalog", metricType=MetricType.GAUGE, description="the last elapsed write time in ms")
	public long getLastElapsedWriteTimeMs() {
		return TimeUnit.MILLISECONDS.convert(getLastElapsedWriteTimeNs(), TimeUnit.NANOSECONDS); 
	}
	
	/**
	 * Returns the rolling average of elapsed write times in ms
	 * @return the rolling average of elapsed write times in ms
	 */
	@ManagedMetric(category="MongoDbCatalog", metricType=MetricType.GAUGE, description="the rolling average of elapsed write times in ms")
	public long getRollingElapsedWriteTimeMs() {
		return TimeUnit.MILLISECONDS.convert(getRollingElapsedWriteTimeNs(), TimeUnit.NANOSECONDS); 
	}
	
	/**
	 * Returns the last elapsed write time in ns
	 * @return the last elapsed write time in ns
	 */
	@ManagedMetric(category="MongoDbCatalog", metricType=MetricType.GAUGE, description="the last elapsed write time in ns")
	public long getLastElapsedWriteTimeNs() {
		return lastCatalogElapsedNs.isEmpty() ? 0 : lastCatalogElapsedNs.get(0); 
	}
	
	/**
	 * Returns the rolling average of elapsed write times in ns
	 * @return the rolling average of elapsed write times in ns
	 */
	@ManagedMetric(category="MongoDbCatalog", metricType=MetricType.GAUGE, description="the rolling average of elapsed write times in ns")
	public long getRollingElapsedWriteTimeNs() {
		return lastCatalogElapsedNs.avg(); 
	}
	
	/**
	 * Returns the last written batch size
	 * @return the last written batch size
	 */
	@ManagedMetric(category="MongoDbMetrics", metricType=MetricType.GAUGE, description="the last written batch size")
	public long getLastBatchSize() {
		return lastBatchSize.isEmpty() ? 0 : lastBatchSize.get(0); 
	}
	
	/**
	 * Returns the rolling average of the written batch sizes
	 * @return the rolling average of the written batch sizes
	 */
	@ManagedMetric(category="MongoDbMetrics", metricType=MetricType.GAUGE, description="the rolling average of the written batch sizes")
	public long getRollingBatchSizes() {
		return lastBatchSize.avg(); 
	}
	
	
	
	
	/**
	 * Creates a new MongoDbDestination
	 * @param patterns The metric patterns accepted by this destination
	 */
	public MongoDbDestination(String... patterns) {
		super(patterns);
	}

	/**
	 * Creates a new MongoDbDestination
	 * @param patterns The metric patterns accepted by this destination
	 */
	public MongoDbDestination(Collection<String> patterns) {
		super(patterns);
	}
	
	/**
	 * Returns the time based flush trigger in ms.
	 * @return the time based flush trigger
	 */
	@ManagedAttribute(description="The elapsed time after which accumulated time-series writes are flushed")
	public long getTimeTrigger() {
		return timeTrigger;
	}

	/**
	 * Sets the time based flush trigger
	 * @param timeTrigger the frequency that the buffer is flushed in ms.
	 */
	@ManagedAttribute(description="The elapsed time after which accumulated time-series writes are flushed")
	public void setTimeTrigger(long timeTrigger) {
		this.timeTrigger = timeTrigger;
	}

	/**
	 * Returns the size based flush trigger
	 * @return the size based flush trigger
	 */
	@ManagedAttribute(description="The number of accumulated time-series writes that triggers a flush")
	public int getSizeTrigger() {
		return sizeTrigger;
	}

	/**
	 * Sets the size based flush trigger
	 * @param sizeTrigger the number of metrics to accumulate before they are flushed
	 */
	@ManagedAttribute(description="The number of accumulated time-series writes that triggers a flush")
	public void setSizeTrigger(int sizeTrigger) {
		this.sizeTrigger = sizeTrigger;
	}
	

	/**
	 * Creates a new MongoDbDestination
	 */
	public MongoDbDestination() {
	}

	/**
	 * Sets the mongo DB template
	 * @param mongoTemplate the mongo DB template
	 */
	@Autowired(required=true)
	public void setMongoTemplate(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	/**
	 * Sets the raw mongo connection
	 * @param mongo the raw mongo connection
	 */
	@Autowired(required=true)
	public void setMongo(Mongo mongo) {
		this.mongo = mongo;
	}



}
