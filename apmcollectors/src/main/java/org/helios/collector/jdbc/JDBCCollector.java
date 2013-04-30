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
package org.helios.collector.jdbc;

import org.apache.log4j.Logger;
import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.collector.core.AbstractCollector;
import org.helios.collector.core.CollectionResult;
import org.helios.collector.core.CollectorException;
import org.helios.collector.jdbc.connection.IJDBCConnectionFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jmx.export.annotation.ManagedResource;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>Title: JDBCCollector</p>
 * <p>Description: Helios collector for JDBC sources.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
@ManagedResource
public class JDBCCollector extends AbstractCollector implements ApplicationContextAware {
	private static final long serialVersionUID = -2720226379379045990L;
	protected IJDBCConnectionFactory connectionFactory = null;
	protected long connectionTimeout = 5000;
	protected long operationTimeout = 5000;
	protected Map<String, SQLMapping> sqlMaps = new ConcurrentHashMap<String, SQLMapping>();
	protected ApplicationContext appContext = null;
	
	
	/**
	 * 
	 */
	public JDBCCollector() {
	}
	
	/**
	 * Starts the collector and initializes all the sql mappings.
	 * @throws CollectorException
	 * @see org.helios.collector.core.AbstractCollector#startCollector()
	 */
	public void startCollector() throws CollectorException {
		log = Logger.getLogger(getClass().getName() + "." + this.beanName);
	}
	
	/**
	 * @throws CollectorException
	 * @see org.helios.collector.core.AbstractCollector#initCollector()
	 */
	public void initCollector() throws CollectorException {
		for(Iterator<SQLMapping> iter = sqlMaps.values().iterator(); iter.hasNext();) {
			SQLMapping mapping = iter.next();
			try {
				//mapping.init(tracingNameSpace, tracer, collectorCache, JMXHelper.getHeliosMBeanServer());
				mapping.init(tracingNameSpace, tracer, JMXHelper.getHeliosMBeanServer());
			} catch (Exception e) {
				if(this.logErrors) {
					error("Failed to initialize SQLMapping [" + mapping.getName() + "]. Removing from map.", e);
				}
				iter.remove();
			}
			debug("Initialized SQLMapping [" + mapping.getName() + "]");
		}
		info("Initialized [" + sqlMaps.size() + " SQLMappings for [" + this.beanName + "]");
		if(sqlMaps.size()<1) {
			throw new CollectorException("No SQLMappings were active after in [" + this.beanName + "] initialization");
		}		
	}
	

	/**
	 * @return
	 * @see org.helios.collector.core.AbstractCollector#collectCallback()
	 */
	@Override
	public CollectionResult collectCallback() {
		Connection conn = null;
		CollectionResult result = new CollectionResult();		
		try {
			debug("Connecting");
			long start = System.currentTimeMillis();
//			tracer.startThreadInfoCapture();
			conn = connectionFactory.getJDBCConnection(connectionTimeout);
//			tracer.endThreadInfoCapture("Local Postgres", "Connect");
			long elapsed = System.currentTimeMillis()-start;			
			debug("Connected in [" + elapsed + "] ms.");
			Map<String, Object> connMetaData = getConnMetaData(conn);
			for(SQLMapping sqlMap: sqlMaps.values()) {
				if(!sqlMap.isPre()) {
					sqlMap.setConnMetaData(connMetaData);
					sqlMap.execute(conn);
				}
			}
			result.setResultForLastCollection(CollectionResult.Result.SUCCESSFUL);
		} catch (Exception e) {			
			result.setResultForLastCollection(CollectionResult.Result.FAILURE);
			if(logErrors) {
				error("Failed to acquire connection", e);
			}
		} finally {
			try { conn.close(); } catch (Exception e) {}
		}
		return result;
	}

	/**
	 * Extracts key values from the DB Connection meta data to pass to mappers.
	 * @param conn The connection to get the meta-data from.
	 * @return A map of meta-data key-value pairs.
	 * @throws SQLException 
	 */
	protected  Map<String, Object> getConnMetaData(Connection conn) throws SQLException {
		Map<String, Object> map = new HashMap<String, Object>();
		DatabaseMetaData dmd = conn.getMetaData();
		map.put("db-product-name", dmd.getDatabaseProductName());
		map.put("db-product-version", dmd.getDatabaseProductVersion());
		map.put("db-url", dmd.getURL());
		map.put("db-user", dmd.getUserName());
		map.put("db-catalog", conn.getCatalog());
		map.put("db-catalog", conn.getCatalog());
		return map;
	}

//	/**
//	 * @return
//	 * @see org.helios.collectors.AbstractCollector#getCollectorVersion()
//	 */
//	@Override
//	public String getCollectorVersion() {
//		return null;
//	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {

	}

	/**
	 * @return the connectionTimeout
	 */
	public long getConnectionTimeout() {
		return connectionTimeout;
	}

	/**
	 * @param connectionTimeout the connectionTimeout to set
	 */
	public void setConnectionTimeout(long connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	/**
	 * @return the operationTimeout
	 */
	public long getOperationTimeout() {
		return operationTimeout;
	}

	/**
	 * @param operationTimeout the operationTimeout to set
	 */
	public void setOperationTimeout(long operationTimeout) {
		this.operationTimeout = operationTimeout;
	}

	/**
	 * @param connectionFactory the connectionFactory to set
	 */
	public void setConnectionFactory(IJDBCConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	/**
	 * Adds a sqlMap to be executed.
	 * @param sqlMaps the sqlMaps to add
	 */
	public void setSqlMaps(Set<SQLMapping> sqlMaps) {
		if(sqlMaps!=null) {
			for(SQLMapping map: sqlMaps) {
				this.sqlMaps.put(map.getName(), map);
			}
		}
	}

	/**
	 * @param appContext
	 * @throws BeansException
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
	 */
	public void setApplicationContext(ApplicationContext appContext)
			throws BeansException {
		this.appContext = appContext;
		
	}


}
