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

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.MXBean;
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
import org.helios.apmrouter.tsmodel.Tier;
import org.helios.apmrouter.tsmodel.TimeSeriesModel;
import org.helios.apmrouter.util.SystemClock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.support.MetricType;

import com.mongodb.Mongo;

/**
 * <p>Title: MongoDbDestination</p>
 * <p>Description: Destination for a MongoDb database. Also handles secondary catalog updates.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.mongodb.MongoDbDestination</code></p>
 */
@MXBean(true) 
public class MongoDbDestination extends BaseDestination implements Runnable, FlushQueueReceiver<IMetric>, MongoDbDestinationMXBean {
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
	/** The time-series model for the MongoDb time-series collections */
	protected TimeSeriesModel tsModel = null;
	/** The time-series model expression */
	protected String tsDefinition = null;
	/** The maximum size of a collection as a factor of the tier specification */
	protected long maxCollectionSizePerPeriod = 650000;
	/** The live step size */
	protected long step = 15000;
	
	
//	BasicDBObject doc = new BasicDBObject();
//	doc.put("$set", new BasicDBObject("word", word));
//	doc.put("$inc", new BasicDBObject("c", 1));
	
	
	
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
				try {
					if(notif!=null) {
						String type = notif.getType();
						if(AbstractTrigger.NEW_HOST.equals(type)) {
							hosts.add(new Host(notif));
							incr("HostsQueued");
						} else if(AbstractTrigger.NEW_AGENT.equals(type)) {
							agents.add(new Agent(notif));
							incr("AgentsQueued");
						} else if(AbstractTrigger.NEW_METRIC.equals(type)) {
							metrics.add(new Metric(notif));
							incr("MetricsQueued");
						}
					}
				} catch (Exception ex) {
					error("Failed to create domain object for MongoDb insert.\n\tNotification:", notif, ex);
					
				}
				
				boolean inserts = false;
				int batchSize = 0;
				try {
					
					if(!hosts.isEmpty() || !agents.isEmpty() || !metrics.isEmpty()) {
						inserts = true;
						SystemClock.startTimer();
					} else {
						continue;
					}
					try {
						if(!hosts.isEmpty()) {
							mongoTemplate.insert(hosts, Host.class);
							incr("HostsInserted", hosts.size());
							batchSize += hosts.size();
						}
					} catch (Exception ex) {
						error("Failed to save host to MongoDb\n\tHosts:", hosts, ex);
					}
					try {
						if(!agents.isEmpty()) {
							mongoTemplate.insert(agents, Agent.class);
							incr("AgentsInserted", agents.size());
							batchSize += agents.size(); 
						}
					} catch (Exception ex) {
						error("Failed to save agent to MongoDb\n\tAgents:", agents, ex);
					}
					try {
						if(!metrics.isEmpty()) {
							mongoTemplate.insert(metrics, Metric.class);
							incr("MetricsInserted", metrics.size());
							batchSize +=  metrics.size();
						}
					} catch (Exception ex) {
						error("Failed to save metrics to MongoDb\n\tMetrics:", metrics, ex);
					}
				} finally {
					if(inserts) {
						lastCatalogElapsedNs.insert(SystemClock.endTimer().elapsedNs);
						lastBatchSize.insert(batchSize);						
					}
					loopTime = SystemClock.time() + 5000;
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
		//info("Flushed [", flushedItems.size(), "] Items");
		for(IMetric metric: flushedItems) {
			final long period = getPeriod(metric.getTime());
			Query query = query(where("period").is(period));
			Update update = new Update().inc("cnt", 1)
					.set("min", metric.getLongValue())
					.set("max", metric.getLongValue());
			
			mongoTemplate.upsert(query, update, "live");
		}
		
	}	
	
	/**
	 * Returns the current period
	 * @param timestamp The timestamp to get the period for
	 * @return the period
	 */
	public long getPeriod(long timestamp) {
		return (timestamp - (timestamp%step));
	}

	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.mongodb.MongoDbDestinationMXBean#resetMetrics()
	 */
	@Override
	public void resetMetrics() {		
		this.lastBatchSize.clear();
		this.lastCatalogElapsedNs.clear();
		this.lastMetricElapsedNs.clear();
		super.resetMetrics();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.BaseDestination#doStart()
	 */
	@Override
	protected void doStart() throws Exception {
		super.doStart();
		tsModel = TimeSeriesModel.create(tsDefinition);
		flushQueue = new TimeSizeFlushQueue<IMetric>(getClass().getSimpleName(), sizeTrigger, timeTrigger, this);
		catalogProcessorThread = new Thread(this, "MongoCatalogProcessorThread");
		catalogProcessorThread.setDaemon(true);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.BaseDestination#doStop()
	 */
	@Override
	protected void doStop() {
		keepRunning.set(false);
		catalogProcessorThread.interrupt();
		
	}
	
	
	/**
	 * On start, registers this instance as a notification listener on notifications from the sub service
	 * @param event The app context refresh event
	 */
	@Override
	public void onApplicationContextRefresh(ContextRefreshedEvent event) {
		catalogProcessorThread.start();
		info("Started MongoDb Catalog Processor Thread");
		for(Tier tier: getTimeSeriesTiers()) {
			if(!mongoTemplate.collectionExists(tier.getName())) {
				long maxCollectionSize = tier.getPeriodCount() * maxCollectionSizePerPeriod;
				mongoTemplate.createCollection(tier.getName(), new CollectionOptions((int)maxCollectionSize, (int) tier.getPeriodCount(), true));
				info("Created tier [", tier.getName(), "]\n\tSize:" , maxCollectionSize, "\n\tDocs:", tier.getPeriodCount());
			}
		}
		step = TimeUnit.MILLISECONDS.convert(tsModel.getModelTiers().get(0).getPeriodDuration().seconds, TimeUnit.SECONDS);
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.mongodb.MongoDbDestinationMXBean#getSupportedMetricNames()
	 */
	@Override
	public Set<String> getSupportedMetricNames() {
		Set<String> _metrics = new HashSet<String>(super.getSupportedMetricNames());
		_metrics.add("MetricsForwarded");
		_metrics.add("InvalidMetricDrops");		
		_metrics.add("AgentsQueued");
		_metrics.add("AgentsInserted");
		_metrics.add("HostsQueued");
		_metrics.add("HostsInserted");
		_metrics.add("MetricsQueued");
		_metrics.add("MetricsInserted");
		return _metrics;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.mongodb.MongoDbDestinationMXBean#getHostsQueuedForInsert()
	 */
	@Override
	@ManagedMetric(category="MongoDbCatalog", metricType=MetricType.COUNTER, description="the number of hosts queued for insert")
	public long getHostsQueuedForInsert() {
		return getMetricValue("HostsQueued"); 
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.mongodb.MongoDbDestinationMXBean#getHostsInserted()
	 */
	@Override
	@ManagedMetric(category="MongoDbCatalog", metricType=MetricType.COUNTER, description="the number of hosts inserted")
	public long getHostsInserted() {
		return getMetricValue("HostsInserted"); 
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.mongodb.MongoDbDestinationMXBean#getAgentsQueuedForInsert()
	 */
	@Override
	@ManagedMetric(category="MongoDbCatalog", metricType=MetricType.COUNTER, description="the number of agents queued for insert")
	public long getAgentsQueuedForInsert() {
		return getMetricValue("AgentsQueued"); 
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.mongodb.MongoDbDestinationMXBean#getAgentsInserted()
	 */
	@Override
	@ManagedMetric(category="MongoDbCatalog", metricType=MetricType.COUNTER, description="the number of agents inserted")
	@ManagedOperation
	public long getAgentsInserted() {
		return getMetricValue("AgentsInserted"); 
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.mongodb.MongoDbDestinationMXBean#getMetricsQueuedForInsert()
	 */
	@Override
	@ManagedMetric(category="MongoDbCatalog", metricType=MetricType.COUNTER, description="the number of metrics queued for insert")
	public long getMetricsQueuedForInsert() {
		return getMetricValue("MetricsQueued"); 
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.mongodb.MongoDbDestinationMXBean#getMetricsInserted()
	 */
	@Override
	@ManagedMetric(category="MongoDbCatalog", metricType=MetricType.COUNTER, description="the number of metrics inserted")
	public long getMetricsInserted() {
		return getMetricValue("MetricsInserted"); 
	}
	
	
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.mongodb.MongoDbDestinationMXBean#getLastElapsedWriteTimeMs()
	 */
	@Override
	@ManagedMetric(category="MongoDbCatalog", metricType=MetricType.GAUGE, description="the last elapsed write time in ms")
	public long getLastElapsedWriteTimeMs() {
		return TimeUnit.MILLISECONDS.convert(getLastElapsedWriteTimeNs(), TimeUnit.NANOSECONDS); 
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.mongodb.MongoDbDestinationMXBean#getRollingElapsedWriteTimeMs()
	 */
	@Override
	@ManagedMetric(category="MongoDbCatalog", metricType=MetricType.GAUGE, description="the rolling average of elapsed write times in ms")
	public long getRollingElapsedWriteTimeMs() {
		return TimeUnit.MILLISECONDS.convert(getRollingElapsedWriteTimeNs(), TimeUnit.NANOSECONDS); 
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.mongodb.MongoDbDestinationMXBean#getLastElapsedWriteTimeNs()
	 */
	@Override
	@ManagedMetric(category="MongoDbCatalog", metricType=MetricType.GAUGE, description="the last elapsed write time in ns")
	public long getLastElapsedWriteTimeNs() {
		return lastCatalogElapsedNs.isEmpty() ? 0 : lastCatalogElapsedNs.get(0); 
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.mongodb.MongoDbDestinationMXBean#getRollingElapsedWriteTimeNs()
	 */
	@Override
	@ManagedMetric(category="MongoDbCatalog", metricType=MetricType.GAUGE, description="the rolling average of elapsed write times in ns")
	public long getRollingElapsedWriteTimeNs() {
		return lastCatalogElapsedNs.avg(); 
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.mongodb.MongoDbDestinationMXBean#getLastBatchSize()
	 */
	@Override
	@ManagedMetric(category="MongoDbMetrics", metricType=MetricType.GAUGE, description="the last written batch size")
	public long getLastBatchSize() {
		return lastBatchSize.isEmpty() ? 0 : lastBatchSize.get(0); 
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.mongodb.MongoDbDestinationMXBean#getRollingBatchSizes()
	 */
	@Override
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
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.mongodb.MongoDbDestinationMXBean#getTimeTrigger()
	 */
	@Override
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
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.mongodb.MongoDbDestinationMXBean#getSizeTrigger()
	 */
	@Override
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

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.mongodb.MongoDbDestinationMXBean#getTsDefinition()
	 */
	@Override
	@ManagedAttribute(description="The time-series model definition")
	public String getTsDefinition() {
		return tsDefinition;
	}

	/**
	 * Returns the time-series model definition
	 * @param tsDefinition the time-series model definition
	 */
	public void setTsDefinition(String tsDefinition) {
		this.tsDefinition = tsDefinition;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.mongodb.MongoDbDestinationMXBean#getModelMatrix()
	 */
	@Override
	@ManagedAttribute(description="The time-series model matrix")
	public long[][] getModelMatrix() {
		if(tsModel==null) return null;
		return tsModel.getModelMatrix();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.mongodb.MongoDbDestinationMXBean#getModelMatrixString()
	 */
	@Override
	@ManagedAttribute(description="The time-series model matrix as a string [periodDuration.seconds, tier.tierDuration.seconds, tier.periodCount]")
	public String getModelMatrixString() {
		if(tsModel==null) return null;
		return Arrays.deepToString(tsModel.getModelMatrix());
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.mongodb.MongoDbDestinationMXBean#getTimeSeriesTiers()
	 */
	@Override
	@ManagedAttribute(description="The time-series tiers")
	public Tier[] getTimeSeriesTiers() {
		return tsModel.getModelTiers().toArray(new Tier[0]); 
	}


}
