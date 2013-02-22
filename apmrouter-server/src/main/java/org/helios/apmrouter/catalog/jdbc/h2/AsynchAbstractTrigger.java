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
package org.helios.apmrouter.catalog.jdbc.h2;

import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.SQLException;

import javax.management.MBeanNotificationInfo;
import javax.sql.DataSource;

import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.spring.ctx.ApplicationContextService;
import org.helios.apmrouter.util.thread.ManagedThreadPool;
import org.helios.apmrouter.util.thread.ThreadPoolConfig;
import org.springframework.context.ApplicationContext;

/**
 * <p>Title: AsynchAbstractTrigger</p>
 * <p>Description: Trigger base class that fires triggers asynchronously</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.catalog.jdbc.h2.AsynchAbstractTrigger</code></p>
 */

public abstract class AsynchAbstractTrigger extends AbstractTrigger implements AsynchAbstractTriggerMBean {
	/** The thread pool used for asynch execution */
	protected ManagedThreadPool threadPool = null;
	/** A data source of connections back into the same used by asynch trigger excutions that need to get data from the DB */
	protected DataSource dataSource = null;
	/** A proxy reference to the root application context */
	protected ApplicationContext rootAppCtx = null;
	/**
	 * Creates a new AsynchAbstractTrigger
	 * @param infos The MBean notification metadata
	 */
	public AsynchAbstractTrigger(MBeanNotificationInfo... infos) {
		super(infos);
	}
	
	/** The number of cores available */
	public static final int CORES = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.catalog.jdbc.h2.AbstractTrigger#init(java.sql.Connection, java.lang.String, java.lang.String, java.lang.String, boolean, int)
	 */
	@Override
	public void init(Connection conn, String schemaName, String triggerName,
			String tableName, boolean before, int type) throws SQLException {
		super.init(conn, schemaName, triggerName, tableName, before, type);
		rootAppCtx = ApplicationContextService.getRootInstance();
		log.info("Acquired Root AppCtx [" + rootAppCtx.getDisplayName() + "]");
		dataSource = rootAppCtx.getBean("DataSource", DataSource.class);
		if(dataSource==null) throw new RuntimeException("Datasource was null", new Throwable());
		ThreadPoolConfig tpc = new ThreadPoolConfig();
		tpc.setCorePoolSize(2);
		tpc.setMaximumPoolSize(CORES);
		tpc.setDaemonThreads(true);
		tpc.setFairQueue(false);
		tpc.setQueueSize(100);
		tpc.setCoreThreadsStarted(2);
		tpc.setKeepAliveTime(60000);
		
		threadPool = new ManagedThreadPool(tpc);
		threadPool.setBeanName(schemaName + "-" + tableName + "-" + triggerName + (before ? "_before" : "_after"));
		threadPool.setObjectName(
				JMXHelper.objectName(
						NewElementTriggers.class.getPackage().getName(), 
						"service", "AsynchTriggerThreadPool",
						"class", getClass().getSimpleName(),
						"schema", schemaName,
						"table", tableName,
						"trigger", triggerName,
						"order", (before ? "before" : "after"),
						"type", TriggerOp.getEnabledStatesName(type))
		);
		
		try {
			threadPool.start();
		} catch (Exception ex) {			
			throw new RuntimeException("Failed to start asynch trigger thread pool", ex);
		}
		
//		try {
//			JMXHelper.getHeliosMBeanServer().registerMBean(threadPool, threadPool.getObjectName());
//		} catch (Exception ex) {			
//			log.warn("Failed to register management interface for asynch trigger thread pool", ex);
//		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.h2.api.Trigger#fire(java.sql.Connection, java.lang.Object[], java.lang.Object[])
	 */
	@Override
	public void fire(Connection conn, final Object[] oldRow, final Object[] newRow) throws SQLException {
		final DataSource finalDs = dataSource;
		threadPool.submit(new Runnable(){
			@Override
			public void run() {
				doFire(finalDs, oldRow, newRow);
			}
		});

	}
	
	/**
	 * The asynch trigger execution defined by concrete implementations
	 * @param dataSource A data source of connections back into the same used by asynch trigger excutions that need to get data from the DB
	 * @param oldRow The row values before change
	 * @param newRow  The row values after change
	 */
	protected abstract void doFire(DataSource dataSource, Object[] oldRow, Object[] newRow);

	/**
	 * @return
	 * @see org.helios.apmrouter.util.thread.ManagedThreadPool#getActiveCount()
	 */
	public int getActiveCount() {
		return threadPool.getActiveCount();
	}
	
	public int getCorePoolSize() {
		return threadPool.getCorePoolSize();
	}	

	/**
	 * @return
	 * @see org.helios.apmrouter.server.ServerComponentBean#getBeanName()
	 */
	public String getBeanName() {
		return threadPool.getBeanName();
	}

	/**
	 * @return
	 * @see org.helios.apmrouter.util.thread.ManagedThreadPool#getPoolSize()
	 */
	public int getPoolSize() {
		return threadPool.getPoolSize();
	}
	
	public int getLargestPoolSize() {
		return threadPool.getLargestPoolSize();
	}
	
	public int getMaximumPoolSize() {
		return threadPool.getMaximumPoolSize();
	}

	/**
	 * @return
	 * @see org.helios.apmrouter.util.thread.ManagedThreadPool#getQueueSize()
	 */
	public int getQueueSize() {
		return threadPool.getQueueSize();
	}

	/**
	 * 
	 * @see org.helios.apmrouter.server.ServerComponent#resetMetrics()
	 */
	public void resetMetrics() {
		threadPool.resetMetrics();
	}

	/**
	 * @return
	 * @see org.helios.apmrouter.util.thread.ManagedThreadPool#getTaskCount()
	 */
	public long getTaskCount() {
		return threadPool.getTaskCount();
	}

	/**
	 * @return
	 * @see org.helios.apmrouter.util.thread.ManagedThreadPool#isShutdown()
	 */
	public boolean isShutdown() {
		return threadPool.isShutdown();
	}

	/**
	 * @return
	 * @see org.helios.apmrouter.util.thread.ManagedThreadPool#isTerminated()
	 */
	public boolean isTerminated() {
		return threadPool.isTerminated();
	}

	/**
	 * @return
	 * @see org.helios.apmrouter.util.thread.ManagedThreadPool#isTerminating()
	 */
	public boolean isTerminating() {
		return threadPool.isTerminating();
	}

	/**
	 * 
	 * @see org.helios.apmrouter.util.thread.ManagedThreadPool#purge()
	 */
	public void purge() {
		threadPool.purge();
	}

	/**
	 * @return
	 * @see org.helios.apmrouter.util.thread.ManagedThreadPool#getThreadStats()
	 */
	public String[] getThreadStats() {
		return threadPool.getThreadStats();
	}

	/**
	 * @return
	 * @see org.helios.apmrouter.server.ServerComponentBean#isStarted()
	 */
	public boolean isStarted() {
		return threadPool.isStarted();
	}

}
