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
package org.helios.apmrouter.destination.h2timeseries;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.sql.DataSource;

import org.helios.apmrouter.collections.ConcurrentLongSlidingWindow;
import org.helios.apmrouter.collections.ILongSlidingWindow;
import org.helios.apmrouter.collections.UnsafeArray;
import org.helios.apmrouter.destination.BaseDestination;
import org.helios.apmrouter.destination.accumulator.FlushQueueReceiver;
import org.helios.apmrouter.destination.accumulator.TimeSizeFlushQueue;
import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.subscription.SubscriptionService;
import org.helios.apmrouter.subscription.criteria.SubscriptionCriteriaInstance;
import org.helios.apmrouter.util.SystemClock;
import org.helios.apmrouter.util.SystemClock.ElapsedTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedNotification;
import org.springframework.jmx.export.annotation.ManagedNotifications;
import org.springframework.jmx.support.MetricType;

/**
 * <p>Title: H2TimeSeriesDestination</p>
 * <p>Description: Basic time-series metric value store, piggy-backing on the H2 metric catalog.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.h2timeseries.H2TimeSeriesDestination</code></p>
 */
@ManagedNotifications({
	@ManagedNotification(notificationTypes={H2TimeSeriesDestination.NOTIF_TYPE}, name="javax.management.Notification", description="Notification issued when a subscribed metric has an interval roll")
})
public class H2TimeSeriesDestination extends BaseDestination implements FlushQueueReceiver<IMetric>,  NotificationListener, NotificationFilter {
	/** The H2 data source */
	protected DataSource dataSource = null;
	/** The subscription service */
	protected SubscriptionService subscriptionService = null;	
	/** The live time-series STEP size in ms. */
	protected long timeSeriesStep = 15000;
	/** The live time-series WIDTH */
	protected long timeSeriesWidth = 60;
	/** The time based flush trigger in ms. */
	protected long timeTrigger = 15000;
	/** The size based flush trigger in number of metrics accumulated */
	protected int sizeTrigger = 30;
	/** The time/size triggered flush queue */
	protected TimeSizeFlushQueue<IMetric> flushQueue = null;
	/** The base sql update statement for fetching time-series values to update */
	protected StringBuilder safeSelectSql = null;
	/** The base sql update statement for fetching time-series values to update */
	protected StringBuilder unsafeSelectSql = null;
	
	/** The notification type emitted from this MBean */
	protected static final String NOTIF_TYPE = "apmrouter.h2timeseries.intervalroll";
	/** The notification template for types emitted from this MBean */
	protected static final String NOTIF_TEMPLATE = NOTIF_TYPE + ".%s";
	
	/** Serial number generator for jmx notifications */
	protected final AtomicLong jmxNotifSerial = new AtomicLong(0L);
	/** The subscription cache containing a map of the number of metricId subscribers keyed by the metricId subscribed to */
	protected final MetricIdSubCache subCache = new MetricIdSubCache();
	
	/** The last elapsed write time in ms */
	protected final ILongSlidingWindow lastElapsedNs = new ConcurrentLongSlidingWindow(60);
	/** The last average elapsed write time per metric in ns */
	protected final ILongSlidingWindow lastAvgPerElapsedNs = new ConcurrentLongSlidingWindow(60);
	/** The last saved batch size */
	protected final ILongSlidingWindow lastBatchSize = new ConcurrentLongSlidingWindow(60);
	
	/**
	 * Creates a new H2TimeSeriesDestination
	 * @param patterns The patterns accepted by this destination
	 */
	public H2TimeSeriesDestination(String... patterns) {
		super(patterns);
	}

	/**
	 * Creates a new H2TimeSeriesDestination
	 * @param patterns The patterns accepted by this destination
	 */
	public H2TimeSeriesDestination(Collection<String> patterns) {
		super(patterns);
	}

	/**
	 * Creates a new H2TimeSeriesDestination
	 */
	public H2TimeSeriesDestination() {
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.BaseDestination#doStart()
	 */
	@Override
	protected void doStart() throws Exception {
		super.doStart();
		flushQueue = new TimeSizeFlushQueue<>(getClass().getSimpleName(), sizeTrigger, timeTrigger, this);
		unsafeSelectSql = new StringBuilder("select METRIC_ID, NVL2(V, V, UNSAFE_MAKE_MV(").append(timeSeriesStep).append(",").append(timeSeriesWidth).append(",false)) from METRIC M left outer join UNSAFE_METRIC_VALUES MV on MV.ID = m.METRIC_ID where  M.METRIC_ID IN (");
		safeSelectSql = new StringBuilder("select METRIC_ID, NVL2(V, V, MAKE_MV(").append(timeSeriesStep).append(",").append(timeSeriesWidth).append(",false)) from METRIC M left outer join METRIC_VALUES MV on MV.ID = m.METRIC_ID where  M.METRIC_ID IN (");
	}
	
	/**
	 * On start, registers this instance as a notification listener on notifications from the sub service
	 * @param event The app context refresh event
	 */
	@Override
	public void onApplicationContextRefresh(ContextRefreshedEvent event) {
		registerSubListener();
	}
	
	public void flushToSafe(Collection<IMetric> items) {
		Set<IMetric> flushedItemss = new HashSet<IMetric>(items);
		Map<Long, IMetric> metricMap = new HashMap<Long, IMetric>(flushedItemss.size());
		for(IMetric im: flushedItemss) {
			metricMap.put(im.getToken(), im);
		}
		ElapsedTime et = null;
		final int fiSize = metricMap.size();
		if(!metricMap.isEmpty()) {
			SystemClock.startTimer();
			Connection conn = null;
			Statement st = null;
			PreparedStatement updatePs = null;
			ResultSet rset = null;
			try {
				StringBuilder sql = new StringBuilder(safeSelectSql.toString());
				sql.append(metricMap.keySet().toString().replace("[", "").replace("]","")).append(")");
			    conn = dataSource.getConnection();
			    st = conn.createStatement();
			    updatePs = conn.prepareStatement("MERGE INTO METRIC_VALUES KEY(ID) VALUES(?,?)");
			    rset = st.executeQuery(sql.toString());			    
			    while(rset.next()) {
			    	long metricId = rset.getLong(1);
			    	H2TimeSeries hts = (H2TimeSeries)rset.getObject(2);
			    	IMetric im = metricMap.get(metricId);
			    	if(im==null) continue;
			    	long[] rolledPeriod = hts.addValue(im.getTime(), im.getLongValue());
			    	if(rolledPeriod!=null && subCache.containsKey(metricId)) sendIntervalRollEvent(rolledPeriod, im);
			    	updatePs.setLong(1, metricId);
			    	updatePs.setObject(2, hts);
			    	updatePs.addBatch();
			    }
		    	updatePs.executeBatch();
		    	updatePs.clearBatch();
			    et = SystemClock.endTimer();
			    lastBatchSize.insert(fiSize);
			    lastElapsedNs.insert(et.elapsedNs);
			    lastAvgPerElapsedNs.insert(et.avgNs(fiSize));
			    debug("\n\tSaved Batch of [", fiSize , "] Metrics.\n\tElapsed Time:", et);
			} catch (Exception ex) {
				ex.printStackTrace(System.err);
			} finally {
				if(rset!=null) try { rset.close(); } catch (Exception e) {}
				if(st!=null) try { st.close(); } catch (Exception e) {}
				if(updatePs!=null) try { updatePs.close(); } catch (Exception e) {}
				if(conn!=null) try { conn.close(); } catch (Exception e) {}
			}
		}
	}
	
	public void flushTo(Collection<IMetric> items) {
		//flushToSafe(items);
		flushToUnsafe(items);
	}
	
	public void flushToUnsafe(Collection<IMetric> items) {
		
		Set<IMetric> flushedItemss = new HashSet<IMetric>(items);
		Map<Long, IMetric> metricMap = new HashMap<Long, IMetric>(flushedItemss.size());
		for(IMetric im: flushedItemss) {
			metricMap.put(im.getToken(), im);
		}
		ElapsedTime et = null;
		final int fiSize = metricMap.size();
		if(!metricMap.isEmpty()) {
			SystemClock.startTimer();
			Connection conn = null;
			Statement st = null;
			PreparedStatement updatePs = null;
			ResultSet rset = null;
			try {
				StringBuilder sql = new StringBuilder(unsafeSelectSql.toString());
				sql.append(metricMap.keySet().toString().replace("[", "").replace("]","")).append(")");
			    conn = dataSource.getConnection();
			    st = conn.createStatement();
			    updatePs = conn.prepareStatement("MERGE INTO UNSAFE_METRIC_VALUES KEY(ID) VALUES(?,?)");
			    rset = st.executeQuery(sql.toString());			    
			    while(rset.next()) {
			    	UnsafeH2TimeSeries hts = null;
			    	try {
				    	long metricId = rset.getLong(1);
				    	//hts = UnsafeH2TimeSeries.deserialize(rset.getBytes(2));
				    	Blob blob = rset.getBlob(2);
				    	hts = new UnsafeH2TimeSeries(blob.getBinaryStream(), blob.length());
				    	IMetric im = metricMap.get(metricId);
				    	if(im==null) continue;
				    	long[] rolledPeriod = hts.addValue(im.getTime(), im.getLongValue());
				    	if(rolledPeriod!=null && subCache.containsKey(metricId)) sendIntervalRollEvent(rolledPeriod, im);
				    	updatePs.setLong(1, metricId);
				    	//updatePs.setBytes(2, UnsafeH2TimeSeries.serialize(hts));
				    	updatePs.setBlob(2, hts.toInputStream());
				    	updatePs.addBatch();
			    	} finally {
			    		if(hts!=null) hts.destroy();
			    	}
			    }
		    	updatePs.executeBatch();
		    	updatePs.clearBatch();
			    et = SystemClock.endTimer();
			    lastBatchSize.insert(fiSize);
			    lastElapsedNs.insert(et.elapsedNs);
			    lastAvgPerElapsedNs.insert(et.avgNs(fiSize));
			    debug("\n\tSaved Batch of [", fiSize , "] Metrics.\n\tElapsed Time:", et);
			} catch (Exception ex) {
				ex.printStackTrace(System.err);
			} finally {
				if(rset!=null) try { rset.close(); } catch (Exception e) {/* No Op */}
				if(st!=null) try { st.close(); } catch (Exception e) {/* No Op */}
				if(updatePs!=null) try { updatePs.close(); } catch (Exception e) {/* No Op */}
				if(conn!=null) try { conn.close(); } catch (Exception e) {/* No Op */}
			}
		}
	}		
	
	
	/**
	 * Sends an interval roll event
	 * @param data The prior period's data
	 * @param metric The metric that the data is for
	 */
	protected void sendIntervalRollEvent(long[] data, IMetric metric) {		
		Notification notif = new Notification(String.format(NOTIF_TEMPLATE, metric.getToken()), objectName, jmxNotifSerial.incrementAndGet(), SystemClock.time(), "TimeSeries Interval Roll for [" + metric + "]");		
		notif.setUserData(new Object[]{data, metric.getToken()});
		debug("Sent Interval Roll Event [", notif.getSequenceNumber() , "] for Metric:", metric.getToken());
		sendNotification(notif);
		incr("BroadcastIntervalRolls");
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
	 * Returns the cummulative number of H2 TimeSeries serialization reads since the last reset
	 * @return the cummulative number of H2 TimeSeries serialization reads
	 */
	@ManagedMetric(category="H2TimeSeries", metricType=MetricType.COUNTER, description="The cummulative number of H2 TimeSeries serialization reads")
	public long getSerializationReads() {
		return UnsafeH2TimeSeries.getSerializationReads();
	}
	
	/**
	 * Returns the cummulative number of H2 TimeSeries serialization writes since the last reset
	 * @return the cummulative number of H2 TimeSeries serialization writes
	 */
	@ManagedMetric(category="H2TimeSeries", metricType=MetricType.COUNTER, description="The cummulative number of H2 TimeSeries serialization writes")
	public long getSerializationWrites() {
		return UnsafeH2TimeSeries.getSerializationWrites();
	}
	
	/**
	 * Returns the number of unmanaged pointers from {@link UnsafeArray}s.
	 * @return the number of unmanaged pointers
	 */
	@ManagedMetric(category="H2TimeSeries", metricType=MetricType.COUNTER, description="The number of unmanaged pointers")
	public long getUnmanagedPointers() {
		return UnsafeArray.getPointerCount();
	}	
	
	/**
	 * Returns the number of allocated {@link UnsafeH2TimeSeries} instances
	 * @return the number of allocated {@link UnsafeH2TimeSeries} instances
	 */
	@ManagedMetric(category="H2TimeSeries", metricType=MetricType.COUNTER, description="the number of allocated UnsafeH2TimeSeries instances")
	public long getAllocatedTimeSeriesInstances() {
		return UnsafeH2TimeSeries.getAllocatedInstances();
	}		
	
	/**
	 * Returns the rolling average the byte array read from H2 to populate H2 TimeSeries
	 * @return the rolling average the byte array read from H2 to populate H2 TimeSeries
	 */
	@ManagedMetric(category="H2TimeSeries", metricType=MetricType.COUNTER, description="rolling average the byte array read from H2 to populate H2 TimeSeries")
	public long getRollingDeserBytes() {
		return UnsafeH2TimeSeries.getRollingDeserBytes();
	}
	
	
	
	
	/**
	 * Returns the number of time-series interval roll notifications sent
	 * @return the number of time-series interval roll notifications sent
	 */
	@ManagedMetric(category="H2TimeSeries", metricType=MetricType.COUNTER, description="The number of time-series interval roll notifications sent")
	public long getBroadcastIntervalRolls() {
		return getMetricValue("BroadcastIntervalRolls");
	}
	
	
	/**
	 * Returns the number of metrics that were dropped because of invalid metrics received
	 * @return the number of metrics that were dropped because of invalid metrics received
	 */
	@ManagedMetric(category="H2TimeSeries", metricType=MetricType.COUNTER, description="the number of metrics that were dropped because of invalid metrics received")
	public long getMetricsDropped() {
		return getMetricValue("InvalidMetricDrops");
	}
	
	/**
	 * Returns the last elapsed write time in ms
	 * @return the last elapsed write time in ms
	 */
	@ManagedMetric(category="H2TimeSeries", metricType=MetricType.GAUGE, description="the last elapsed write time in ms")
	public long getLastElapsedWriteTimeMs() {
		return TimeUnit.MILLISECONDS.convert(getLastElapsedWriteTimeNs(), TimeUnit.NANOSECONDS); 
	}
	
	/**
	 * Returns the rolling average of elapsed write times in ms
	 * @return the rolling average of elapsed write times in ms
	 */
	@ManagedMetric(category="H2TimeSeries", metricType=MetricType.GAUGE, description="the rolling average of elapsed write times in ms")
	public long getRollingElapsedWriteTimeMs() {
		return TimeUnit.MILLISECONDS.convert(getRollingElapsedWriteTimeNs(), TimeUnit.NANOSECONDS); 
	}
	
	/**
	 * Returns the number of created soft references
	 * @return the number of created soft references
	 */
	@ManagedMetric(category="H2TimeSeries", metricType=MetricType.GAUGE, description="the number of created references")
	public long getCreatedInstances() {
		return RefQueueCleaner.getCreatedinstances(); 
	}
	
	/**
	 * Returns the number of cleared soft references
	 * @return the number of cleared soft references
	 */
	@ManagedMetric(category="H2TimeSeries", metricType=MetricType.GAUGE, description="the number of cleared references")
	public long getClearedInstances() {
		return RefQueueCleaner.getClearedinstances(); 
	}
	
	/**
	 * Returns the last elapsed write time in ns
	 * @return the last elapsed write time in ns
	 */
	@ManagedMetric(category="H2TimeSeries", metricType=MetricType.GAUGE, description="the last elapsed write time in ns")
	public long getLastElapsedWriteTimeNs() {
		return lastElapsedNs.isEmpty() ? 0 : lastElapsedNs.get(0); 
	}
	
	/**
	 * Returns the rolling average of elapsed write times in ns
	 * @return the rolling average of elapsed write times in ns
	 */
	@ManagedMetric(category="H2TimeSeries", metricType=MetricType.GAUGE, description="the rolling average of elapsed write times in ns")
	public long getRollingElapsedWriteTimeNs() {
		return lastElapsedNs.avg(); 
	}
	
	/**
	 * Returns the last written batch size
	 * @return the last written batch size
	 */
	@ManagedMetric(category="H2TimeSeries", metricType=MetricType.GAUGE, description="the last written batch size")
	public long getLastBatchSize() {
		return lastBatchSize.isEmpty() ? 0 : lastBatchSize.get(0); 
	}
	
	/**
	 * Returns the rolling average of the written batch sizes
	 * @return the rolling average of the written batch sizes
	 */
	@ManagedMetric(category="H2TimeSeries", metricType=MetricType.GAUGE, description="the rolling average of the written batch sizes")
	public long getRollingBatchSizes() {
		return lastBatchSize.avg(); 
	}
	
	/**
	 * Returns the last per metric write time in ns
	 * @return the last per metric write time in ns
	 */
	@ManagedMetric(category="H2TimeSeries", metricType=MetricType.GAUGE, description="the last per metric write time in ns")
	public long getLastPerMetricWriteTimeNs() {
		return lastAvgPerElapsedNs.isEmpty() ? 0 : lastAvgPerElapsedNs.get(0); 
	}
	
	/**
	 * Returns the rolling average per metric write time in ns
	 * @return the rolling average per metric write time in ns
	 */
	@ManagedMetric(category="H2TimeSeries", metricType=MetricType.GAUGE, description="the rolling average per metric write time in ns")
	public long getRollingPerMetricWriteTimeNs() {
		return lastAvgPerElapsedNs.avg(); 
	}
	
	/**
	 * Returns the rolling average per metric write time in ms
	 * @return the rolling average per metric write time in ms
	 */
	@ManagedMetric(category="H2TimeSeries", metricType=MetricType.GAUGE, description="the rolling average per metric write time in ms")
	public long getRollingPerMetricWriteTimeMs() {
		return TimeUnit.MILLISECONDS.convert(lastAvgPerElapsedNs.avg(), TimeUnit.NANOSECONDS); 
	}
	
	/**
	 * Returns the rolling average per metric write time in microseconds
	 * @return the rolling average per metric write time in microseconds
	 */
	@ManagedMetric(category="H2TimeSeries", metricType=MetricType.GAUGE, description="the rolling average per metric write time in microseconds")
	public long getRollingPerMetricWriteTimeUs() {
		return TimeUnit.MICROSECONDS.convert(lastAvgPerElapsedNs.avg(), TimeUnit.NANOSECONDS); 
	}
	
	/**
	 * Returns the number of metric Ids in the subcache
	 * @return the number of metric Ids in the subcache
	 */
	@ManagedMetric(category="H2TimeSeries", metricType=MetricType.GAUGE, description="the number of metric Ids in the subcache")
	public long getMetricIdSubCount() {
		return subCache.size(); 
	}	
	
	/**
	 * Returns the subcache map
	 * @return the subcache map
	 */
	@ManagedAttribute(description="the subcache map")
	public Map<Long, AtomicLong> getSubCache() {		 		
		return new HashMap<Long, AtomicLong>(subCache.getSubCache()); 
	}	
	
	

	/**
	 * Sets the H2 datasource
	 * @param dataSource the dataSource to set
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * Returns the live time-series STEP size in ms.
	 * @return the live time-series STEP size in ms.
	 */
	@ManagedAttribute(description="The time-series STEP in ms.")
	public long getTimeSeriesStep() {
		return timeSeriesStep;
	}
	
	/**
	 * Returns the flush queue size
	 * @return the flush queue size
	 */
	@ManagedAttribute(description="The flush queue size")
	public int getFlushQueueSize() {
		return flushQueue.getQueueSize();
	}
	

	/**
	 * Sets live time-series STEP size in ms.
	 * @param timeSeriesStep live time-series STEP size in ms.
	 */	
	public void setTimeSeriesStep(long timeSeriesStep) {
		this.timeSeriesStep = timeSeriesStep;
	}

	/**
	 * Returns the time series WIDTH
	 * @return the time series WIDTH
	 */
	@ManagedAttribute(description="The time-series WIDTH")
	public long getTimeSeriesWidth() {
		return timeSeriesWidth;
	}

	/**
	 * Sets the time series WIDTH
	 * @param timeSeriesWidth the time series WIDTH
	 */
	public void setTimeSeriesWidth(long timeSeriesWidth) {
		this.timeSeriesWidth = timeSeriesWidth;
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
		if(flushQueue!=null) {
			flushQueue.setSizeTrigger(this.sizeTrigger);
		}
	}

	/**
	 * Sends a JMX notification
	 * @param notification The notifi
	 */
	public void sendNotification(Notification notification) {
		notificationPublisher.sendNotification(notification);
	}
	
	/**
	 * Injects the subscription service
	 * @param subscriptionService the subscription service
	 */
	@Autowired(required=true)
	public void setSubscriptionService(SubscriptionService subscriptionService) {
		this.subscriptionService = subscriptionService;
	}
	
	/**
	 * Registers this instance as a {@link SubscriptionService} listener 
	 * so we can get advanced notice of listeners that will be interested in live metric feeds. 
	 */
	protected void registerSubListener() {
		ObjectName subServiceObjectName = subscriptionService.getObjectName();
		try {
			JMXHelper.getHeliosMBeanServer().addNotificationListener(subServiceObjectName, this, this, getClass().getSimpleName());
		} catch (Exception e) {
			throw new RuntimeException("Failed to register listener with subscription service at [" + subServiceObjectName + "]", e);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
	 */
	@Override
	public void handleNotification(Notification notification, Object handback) {
		Object userData = notification.getUserData();
		if(!(userData instanceof SubscriptionCriteriaInstance)) {
			warn("Received JMX notification with user data that was not a SubscriptionCriteriaInstance. Was [", userData==null ? "null" : userData.getClass().getName(), "]");
			return;
		}
		SubscriptionCriteriaInstance<?> sci = (SubscriptionCriteriaInstance<?>)userData;
		Object subKey = sci.getSubcriptionKey();
		if(!(subKey instanceof String[])) {
			warn("Received JMX notification with Subscription Key that was not a String[]. Was [", subKey==null ? "null" : subKey.getClass().getName(), "]");
			return;
		}
		String[] metricSubNotifs = (String[])subKey;
		if(SubscriptionService.NOTIF_SUB_STARTED.equals(notification.getType())) {
			addToSubCache(extractMetricIds(metricSubNotifs));
		} else if(SubscriptionService.NOTIF_SUB_STOPPED.equals(notification.getType())) {
			removeFromSubCache(extractMetricIds(metricSubNotifs));
		} else {
			warn("Received JMX notification with unexpected type [", notification.getType(), "]");
		}		
	}

	/**
	 * Adds the passed metric IDs to the subscriber cache to indicate someone is interested in them
	 * @param metricIds and array of metric IDs
	 */
	protected void addToSubCache(long[] metricIds) {
		if(metricIds!=null) {
			for(long id: metricIds) {
				subCache.add(id);
			}
		}
	}
	
	/**
	 * Removes the passed metric IDs from the subscriber cache to indicate one less interested subscriber
	 * @param metricIds and array of metric IDs
	 */
	protected void removeFromSubCache(long[] metricIds) {
		if(metricIds!=null) {
			for(long id: metricIds) {
				subCache.remove(id);
			}
		}		
	}
	
	
	/**
	 * Extracts an array of metric IDs from the passed metric subscription notification messages
	 * @param metricSubNotifs An array of metric subscription notification messages
	 * @return an array of longs
	 */
	protected long[] extractMetricIds(String[] metricSubNotifs) {
		if(metricSubNotifs==null || metricSubNotifs.length<1) return new long[0];
		long[] ids = new long[metricSubNotifs.length];	
		String s = null;
		for(int i = 0; i < metricSubNotifs.length; i++) {			
			try {
				s = metricSubNotifs[i];
				ids[i] = Long.parseLong(s.substring(s.lastIndexOf('.')+1));
			} catch (Exception ex) {  
				warn("Invalid metricSubNotif [", s, "]");
			}
		}
		return ids;
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationFilter#isNotificationEnabled(javax.management.Notification)
	 */
	@Override
	public boolean isNotificationEnabled(Notification notification) {
		return objectName.toString().equals(notification.getSource().toString());
	}



}
