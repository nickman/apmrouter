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
package org.helios.apmrouter.hibernate;

import java.util.Date;

import org.helios.apmrouter.server.ServerComponentBean;
import org.hibernate.SessionFactory;
import org.hibernate.jmx.StatisticsService;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.jmx.support.MetricType;

/**
 * <p>Title: HibernateStatsService</p>
 * <p>Description: Wrapper for the Hibernate stats service {@link StatisticsService} to expose internal stats as open data types</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.hibernate.HibernateStatsService</code></p>
 */
public class HibernateStatsService extends ServerComponentBean implements HibernateStatsServiceMXBean {
	/** The delegate stats service */
	protected final StatisticsService delegate = new StatisticsService();

	/**
	 * Creates a new HibernateStatsService
	 * @param sessionFactory The session factory to monitor
	 */
	public HibernateStatsService(SessionFactory sessionFactory) {
		delegate.setSessionFactory(sessionFactory);
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#clear()
	 */
	@Override
	@ManagedOperation(description="Resets the JMX Hibernate statistics")
	public void clear() {
		delegate.clear();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#getEntityStatistics(java.lang.String)
	 */
	@Override
	@ManagedOperation(description="Retrieves the Hibernate entity statistics for the passed entity name")
	@ManagedOperationParameters({@ManagedOperationParameter(name="entityName", description="The name of the Hibernate entity to acquire statistics for")})
	public OpenEntityStatisticsMBean getEntityStatistics(String entityName) {
		return new OpenEntityStatistics(delegate.getEntityStatistics(entityName));
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#getEntityStatistics()
	 */
	@Override
	@ManagedAttribute(description="The Hibernate entity statistics")
	public OpenEntityStatisticsMBean[] getEntityStatistics() {
		String[] keys = getEntityNames();
		OpenEntityStatisticsMBean[] stats = new OpenEntityStatisticsMBean[keys.length];
		for(int i = 0; i < keys.length; i++) {
			stats[i] = getEntityStatistics(keys[i]);
		}
		return stats;
	}	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#getCollectionStatistics(java.lang.String)
	 */
	@Override
	@ManagedOperation(description="Retrieves the Hibernate collection statistics for the passed collection role")
	@ManagedOperationParameters({@ManagedOperationParameter(name="role", description="The name of the Hibernate role to acquire collection statistics for")})
	public OpenCollectionStatisticsMBean getCollectionStatistics(String role) {
		return new OpenCollectionStatistics(delegate.getCollectionStatistics(role));
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#getCollectionStatistics()
	 */
	@Override
	@ManagedAttribute(description="The Hibernate collection statistics")
	public OpenCollectionStatisticsMBean[] getCollectionStatistics() {
		String[] keys = getCollectionRoleNames();
		OpenCollectionStatisticsMBean[] stats = new OpenCollectionStatisticsMBean[keys.length];
		for(int i = 0; i < keys.length; i++) {
			stats[i] = getCollectionStatistics(keys[i]);
		}
		return stats;
	}
	

//	/**
//	 * @param regionName
//	 * @return
//	 * @see org.hibernate.jmx.StatisticsService#getSecondLevelCacheStatistics(java.lang.String)
//	 */
//	public SecondLevelCacheStatistics getSecondLevelCacheStatistics(
//			String regionName) {
//		return delegate.getSecondLevelCacheStatistics(regionName);
//	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#getQueryStatistics(java.lang.String)
	 */
	@Override
	@ManagedOperation(description="Retrieves the Hibernate query statistics for the passed sql")
	@ManagedOperationParameters({@ManagedOperationParameter(name="hql", description="The hql to retrieve the Hibernate query statistics for")})
	public OpenQueryStatisticsMBean getQueryStatistics(String hql) {
		return new OpenQueryStatistics(delegate.getQueryStatistics(hql));
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#getQueryStatistics()
	 */
	@Override
	@ManagedAttribute(description="The Hibernate query statistics")
	public OpenQueryStatisticsMBean[] getQueryStatistics() {
		String[] keys = getQueries();
		OpenQueryStatisticsMBean[] stats = new OpenQueryStatisticsMBean[keys.length];
		for(int i = 0; i < keys.length; i++) {
			stats[i] = getQueryStatistics(keys[i]);
		}
		return stats;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#getEntityDeleteCount()
	 */
	@Override
	@ManagedMetric(category="Hibernate", displayName="EntityDeleteCount", metricType=MetricType.COUNTER, description="The total number of deleted entities")
	public long getEntityDeleteCount() {
		return delegate.getEntityDeleteCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#getEntityInsertCount()
	 */
	@Override
	@ManagedMetric(category="Hibernate", displayName="EntityInsertCount", metricType=MetricType.COUNTER, description="The total number of inserted entities")
	public long getEntityInsertCount() {
		return delegate.getEntityInsertCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#getEntityLoadCount()
	 */
	@Override
	@ManagedMetric(category="Hibernate", displayName="EntityLoadCount", metricType=MetricType.COUNTER, description="The total number of loaded entities")
	public long getEntityLoadCount() {
		return delegate.getEntityLoadCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#getEntityFetchCount()
	 */
	@Override
	@ManagedMetric(category="Hibernate", displayName="EntityFetchCount", metricType=MetricType.COUNTER, description="The total number of fetched entities")
	public long getEntityFetchCount() {
		return delegate.getEntityFetchCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#getEntityUpdateCount()
	 */
	@Override
	@ManagedMetric(category="Hibernate", displayName="EntityUpdateCount", metricType=MetricType.COUNTER, description="The total number of updated entities")
	public long getEntityUpdateCount() {
		return delegate.getEntityUpdateCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#getQueryExecutionCount()
	 */
	@Override
	@ManagedMetric(category="Hibernate", displayName="QueryExecutionCount", metricType=MetricType.COUNTER, description="The total number of executed queries")
	public long getQueryExecutionCount() {
		return delegate.getQueryExecutionCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#getQueryCacheHitCount()
	 */
	@Override
	@ManagedMetric(category="Hibernate", displayName="QueryCacheHitCount", metricType=MetricType.COUNTER, description="The query cache hit count")
	public long getQueryCacheHitCount() {
		return delegate.getQueryCacheHitCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#getQueryExecutionMaxTime()
	 */
	@Override
	@ManagedMetric(category="Hibernate", displayName="QueryExecutionMaxTime", metricType=MetricType.GAUGE, description="The maximum query execution time in ms.")
	public long getQueryExecutionMaxTime() {
		return delegate.getQueryExecutionMaxTime();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#getQueryCacheMissCount()
	 */
	@Override
	@ManagedMetric(category="Hibernate", displayName="QueryCacheMissCount", metricType=MetricType.COUNTER, description="The query cache miss count")
	public long getQueryCacheMissCount() {
		return delegate.getQueryCacheMissCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#getQueryCachePutCount()
	 */
	@Override
	@ManagedMetric(category="Hibernate", displayName="QueryCachePutCount", metricType=MetricType.COUNTER, description="The query cache put count")
	public long getQueryCachePutCount() {
		return delegate.getQueryCachePutCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#getFlushCount()
	 */
	@Override
	@ManagedMetric(category="Hibernate", displayName="FlushCount", metricType=MetricType.COUNTER, description="The hibernate flush count")
	public long getFlushCount() {
		return delegate.getFlushCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#getConnectCount()
	 */
	@Override
	@ManagedMetric(category="Hibernate", displayName="ConnectCount", metricType=MetricType.COUNTER, description="The global number of connections asked by the sessions")
	public long getConnectCount() {
		return delegate.getConnectCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#getSessionCloseCount()
	 */
	@Override
	@ManagedMetric(category="Hibernate", displayName="CloseCount", metricType=MetricType.COUNTER, description="The global number of sessions closed")
	public long getSessionCloseCount() {
		return delegate.getSessionCloseCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#getSessionOpenCount()
	 */
	@Override
	@ManagedMetric(category="Hibernate", displayName="SessionOpenCount", metricType=MetricType.COUNTER, description="The number of open sessions")
	public long getSessionOpenCount() {
		return delegate.getSessionOpenCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#getCollectionLoadCount()
	 */
	@Override
	@ManagedMetric(category="Hibernate", displayName="CollectionLoadCount", metricType=MetricType.COUNTER, description="The number of loaded collections")
	public long getCollectionLoadCount() {
		return delegate.getCollectionLoadCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#getCollectionFetchCount()
	 */
	@Override
	@ManagedMetric(category="Hibernate", displayName="CollectionFetchCount", metricType=MetricType.COUNTER, description="The number of fetched collections")
	public long getCollectionFetchCount() {
		return delegate.getCollectionFetchCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#getCollectionUpdateCount()
	 */
	@Override
	@ManagedMetric(category="Hibernate", displayName="CollectionUpdateCount", metricType=MetricType.COUNTER, description="The number of updated collections")
	public long getCollectionUpdateCount() {
		return delegate.getCollectionUpdateCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#getCollectionRemoveCount()
	 */
	@Override
	@ManagedMetric(category="Hibernate", displayName="CollectionRemoveCount", metricType=MetricType.COUNTER, description="The number of removed collections")
	public long getCollectionRemoveCount() {
		return delegate.getCollectionRemoveCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#getCollectionRecreateCount()
	 */
	@Override
	@ManagedMetric(category="Hibernate", displayName="CollectionRecreateCount", metricType=MetricType.COUNTER, description="The number of recreated collections")
	public long getCollectionRecreateCount() {
		return delegate.getCollectionRecreateCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#getStartTime()
	 */
	@Override
	@ManagedAttribute(description="The service start timestamp")
	public long getStartTime() {
		return delegate.getStartTime();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#getStartDate()
	 */
	@Override
	@ManagedAttribute(description="The service start date")
	public Date getStartDate() {
		return new Date(delegate.getStartTime());
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#isStatisticsEnabled()
	 */
	@Override
	@ManagedAttribute(description="The enabled state of the service")
	public boolean isStatisticsEnabled() {
		return delegate.isStatisticsEnabled();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#setStatisticsEnabled(boolean)
	 */
	@Override
	@ManagedAttribute(description="The enabled state of the service")
	public void setStatisticsEnabled(boolean enable) {
		delegate.setStatisticsEnabled(enable);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#logSummary()
	 */
	@Override
	@ManagedOperation(description="Logs the statistics to standard out")
	public void logSummary() {
		delegate.logSummary();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#getCollectionRoleNames()
	 */
	@Override
	@ManagedAttribute(description="An array of the collection role names")
	public String[] getCollectionRoleNames() {
		return delegate.getCollectionRoleNames();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#getEntityNames()
	 */
	@Override
	@ManagedAttribute(description="An array of the entity names")
	public String[] getEntityNames() {
		return delegate.getEntityNames();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#getQueries()
	 */
	@Override
	@ManagedAttribute(description="An array of the queries")
	public String[] getQueries() {
		return delegate.getQueries();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#getSuccessfulTransactionCount()
	 */
	@Override
	@ManagedMetric(category="Hibernate", displayName="SuccessfulTransactionCount", metricType=MetricType.COUNTER, description="The number of successful transactions")
	public long getSuccessfulTransactionCount() {
		return delegate.getSuccessfulTransactionCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#getTransactionCount()
	 */
	@Override
	@ManagedMetric(category="Hibernate", displayName="TransactionCount", metricType=MetricType.COUNTER, description="The number of executed transactions")
	public long getTransactionCount() {
		return delegate.getTransactionCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#getCloseStatementCount()
	 */
	@Override
	@ManagedMetric(category="Hibernate", displayName="CloseStatementCount", metricType=MetricType.COUNTER, description="The number of closed statements")
	public long getCloseStatementCount() {
		return delegate.getCloseStatementCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#getPrepareStatementCount()
	 */
	@Override
	@ManagedMetric(category="Hibernate", displayName="PrepareStatementCount", metricType=MetricType.COUNTER, description="The number of prepared statements acquired")
	public long getPrepareStatementCount() {
		return delegate.getPrepareStatementCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#getOptimisticFailureCount()
	 */
	@Override
	@ManagedMetric(category="Hibernate", displayName="OptimisticFailureCount", metricType=MetricType.COUNTER, description="The number of optimistic failures")
	public long getOptimisticFailureCount() {
		return delegate.getOptimisticFailureCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.hibernate.HibernateStatsServiceMXBean#getQueryExecutionMaxTimeQueryString()
	 */
	@Override
	@ManagedAttribute(description="The HQL of the slowest query")
	public String getQueryExecutionMaxTimeQueryString() {
		return delegate.getQueryExecutionMaxTimeQueryString();
	}

	/**
	 * @return
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return delegate.toString();
	}

}
