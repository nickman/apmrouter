/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2012, Helios Development Group and individual contributors
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
package org.helios.apmrouter.destination.incrementors;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.metric.MetricType;
import org.helios.apmrouter.server.ServerComponentBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;

/**
 * <p>Title: IncrementorDestination</p>
 * <p>Description: Destination for handling incrementors</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.incrementors.IncrementorDestination</code></p>
 */

public class IncrementorDestination extends ServerComponentBean implements Runnable {
	/** The H2 data source */
	protected DataSource dataSource = null;
	/** The H2 connection */
	protected Connection conn = null;
	/** The H2 prepared statement for increments */
	protected PreparedStatement psIncrements = null;
	/** The H2 prepared statement for interval increments */
	protected PreparedStatement psInterIncrements = null;
	/** The queue size */
	protected int queueSize = 1000;
	
	/** The queue */
	protected BlockingQueue<IMetric> queue;
	/** The queue processing thread */
	protected Thread incrementProcessorThread = null;
	/** The keep running flag */
	protected boolean keepRunning = false;

	/**
	 * Creates a new IncrementorDestination
	 */
	public IncrementorDestination() {
		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.BaseDestination#doStart()
	 */
	protected void doStart() throws Exception {
		conn = dataSource.getConnection();
		psIncrements = conn.prepareStatement("UPDATE INCREMENTOR SET INC_VALUE = INC_VALUE + ?, LAST_INC = CURRENT_TIMESTAMP WHERE METRIC_ID = ?");
		psInterIncrements = conn.prepareStatement("UPDATE INTER_INCREMENTOR SET INC_VALUE = INC_VALUE + ?, LAST_INC = CURRENT_TIMESTAMP WHERE METRIC_ID = ?");
		queue = new ArrayBlockingQueue<IMetric>(queueSize, false);
		incrementProcessorThread = new Thread(this, "IncrementProcessorThread");
		incrementProcessorThread.setDaemon(true);
		keepRunning = true;
		incrementProcessorThread.start();
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		while(keepRunning) {
			try {
				IMetric metric = queue.poll(5000, TimeUnit.MILLISECONDS);
				if(metric!=null) {
					PreparedStatement ps = MetricType.INCREMENTOR==metric.getType() ? psIncrements : psInterIncrements; 
					ps.setLong(1, metric.getLongValue());
					ps.setLong(2, metric.getToken());
					if(ps.executeUpdate()<1) {
						incr("FailedUpdates");
					}
				}
			} catch (InterruptedException iex) {
				if(keepRunning) {
					Thread.interrupted();
				}
			} catch (Exception ex) {
				incr("FailedUpdates");
				error("Increment failed", ex);
				
			}
		}
	}

	/**
	 * Adds the passed metric to the increment update queue
	 * @param routable The metric to queue
	 */
	public void queue(IMetric routable) {
		try {
			if(!queue.offer(routable)) {
				incr("DroppedUpdates");
			}
		} catch (Exception ex) {
			incr("DroppedUpdates");
		}
	}
	
	/**
	 * Sets the H2 datasource
	 * @param dataSource the dataSource to set
	 */
	@Autowired(required=true)
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}	
	
	@Override
	public Set<String> getSupportedMetricNames() {
		Set<String> _metrics = new HashSet<String>(super.getSupportedMetricNames());
		_metrics.add("FailedUpdates");
		_metrics.add("DroppedUpdates");
		_metrics.add("CompletedUpdates");
		return _metrics;
	}

	/**
	 * Returns the incrementor update initial queue size
	 * @return the incrementor update initial queue size
	 */
	@ManagedAttribute(description="The incrementor update initial queue size")
	public int getQueueSize() {
		return queueSize;
	}

	/**
	 * Sets the incrementor update initial queue size
	 * @param queueSize the incrementor update initial queue size
	 */
	public void setQueueSize(int queueSize) {
		this.queueSize = queueSize;
	}

}
