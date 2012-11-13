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

import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.sql.DataSource;

import org.helios.apmrouter.collections.ConcurrentLongSlidingWindow;
import org.helios.apmrouter.destination.BaseDestination;
import org.helios.apmrouter.destination.accumulator.FlushQueueReceiver;
import org.helios.apmrouter.destination.accumulator.TimeSizeFlushQueue;
import org.helios.apmrouter.jmx.ThreadPoolFactory;
import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.util.SystemClock;
import org.helios.apmrouter.util.SystemClock.ElapsedTime;
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
public class H2TimeSeriesDestination extends BaseDestination implements FlushQueueReceiver<IMetric>, NotificationEmitter {
	/** The H2 data source */
	protected DataSource dataSource = null;
	/** The live time-series step size in ms. */
	protected long timeSeriesStep = 15000;
	/** The live time-series width */
	protected long timeSeriesWidth = 60;
	/** The time based flush trigger in ms. */
	protected long timeTrigger = 15000;
	/** The size based flush trigger in number of metrics accumulated */
	protected int sizeTrigger = 30;
	/** The time/size triggered flush queue */
	protected TimeSizeFlushQueue<IMetric> flushQueue = null;
	/** The base sql update statement for fetching time-series values to update */
	protected StringBuilder selectSql = null;
	/** The notification type emitted from this MBean */
	protected static final String NOTIF_TYPE = "apmrouter.h2timeseries.intervalroll";
	/** The notification template for types emitted from this MBean */
	protected static final String NOTIF_TEMPLATE = NOTIF_TYPE + ".%s";
	
	/** The notification info for the notification broadcaster for rolled periods */
	protected final MBeanNotificationInfo MBEAN_INFO = new MBeanNotificationInfo(
			new String[]{NOTIF_TYPE}, Notification.class.getName(), "Notification issued when a subscribed metric has an interval roll"
	); 
	/** The notification broadcaster for rolled periods */
	protected final NotificationBroadcasterSupport broadcaster = new NotificationBroadcasterSupport(ThreadPoolFactory.newCachedThreadPool(getClass().getPackage().getName(), getClass().getSimpleName()), MBEAN_INFO); 
	/** Serial number generator for jmx notifications */
	protected final AtomicLong jmxNotifSerial = new AtomicLong(0L);
	
	/** The last elapsed write time in ms */
	protected final ConcurrentLongSlidingWindow lastElapsedNs = new ConcurrentLongSlidingWindow(60);
	/** The last average elapsed write time per metric in ns */
	protected final ConcurrentLongSlidingWindow lastAvgPerElapsedNs = new ConcurrentLongSlidingWindow(60);
	/** The last saved batch size */
	protected final ConcurrentLongSlidingWindow lastBatchSize = new ConcurrentLongSlidingWindow(60);
	
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
		selectSql = new StringBuilder("select METRIC_ID, NVL2(V, V, MAKE_MV(").append(timeSeriesStep).append(",").append(timeSeriesWidth).append(",false)) from METRIC M left outer join METRIC_VALUES MV on MV.ID = m.METRIC_ID where  M.METRIC_ID IN (");
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.accumulator.FlushQueueReceiver#flushTo(java.util.Collection)
	 */
	@Override
	public void flushTo(Collection<IMetric> items) {
		
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
				StringBuilder sql = new StringBuilder(selectSql.toString());
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
			    	if(rolledPeriod!=null) sendIntervalRollEvent(rolledPeriod, im);
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
	
	/**
	 * Sends an interval roll event
	 * @param data The prior period's data
	 * @param metric The metric that the data is for
	 */
	protected void sendIntervalRollEvent(long[] data, IMetric metric) {		
		Notification notif = new Notification(String.format(NOTIF_TEMPLATE, metric.getToken()), objectName, jmxNotifSerial.incrementAndGet(), SystemClock.time(), "TimeSeries Interval Roll for [" + metric + "]");		
		notif.setUserData(new Object[]{data, metric});
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
	 * Sets the H2 datasource
	 * @param dataSource the dataSource to set
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * Returns the live time-series step size in ms.
	 * @return the live time-series step size in ms.
	 */
	@ManagedAttribute(description="The time-series step in ms.")
	public long getTimeSeriesStep() {
		return timeSeriesStep;
	}

	/**
	 * Sets live time-series step size in ms.
	 * @param timeSeriesStep live time-series step size in ms.
	 */	
	public void setTimeSeriesStep(long timeSeriesStep) {
		this.timeSeriesStep = timeSeriesStep;
	}

	/**
	 * Returns the time series width
	 * @return the time series width
	 */
	@ManagedAttribute(description="The time-series width")
	public long getTimeSeriesWidth() {
		return timeSeriesWidth;
	}

	/**
	 * Sets the time series width
	 * @param timeSeriesWidth the time series width
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
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationBroadcaster#addNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) {
		if(filter==null) {
			throw new IllegalArgumentException("This notification broadcaster requires a filter", new Throwable());
		}
		broadcaster.addNotificationListener(listener, filter, handback);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationBroadcaster#removeNotificationListener(javax.management.NotificationListener)
	 */
	public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
		broadcaster.removeNotificationListener(listener);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationEmitter#removeNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	public void removeNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) throws ListenerNotFoundException {
		broadcaster.removeNotificationListener(listener, filter, handback);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationBroadcaster#getNotificationInfo()
	 */
	public MBeanNotificationInfo[] getNotificationInfo() {
		return broadcaster.getNotificationInfo();
	}

	/**
	 * Sends a JMX notification
	 * @param notification The notifi
	 */
	public void sendNotification(Notification notification) {
		broadcaster.sendNotification(notification);
	}



}
