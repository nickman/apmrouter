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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.helios.apmrouter.jmx.XMLHelper;
import org.helios.apmrouter.trace.ITracer;
import org.helios.collector.jdbc.binding.provider.BindVariableProviderFactory;
import org.helios.collector.jdbc.binding.provider.IBindVariableProvider;
import org.helios.collector.jdbc.binding.provider.ProviderNotFoundException;
import org.helios.collector.jdbc.binding.provider.ProviderToken;
import org.helios.collector.jdbc.extract.ProcessedResultSet;
import org.helios.collector.jdbc.mapping.InvalidMetricMappingException;
import org.helios.collector.jdbc.mapping.MetricMap;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.w3c.dom.Node;

import javax.management.MBeanServer;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;

/**
 * <p>Title: SQLMapping</p>
 * <p>Description: A collection mapping that associates a SQL query to a set of collected metrics.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
@ManagedResource
public class SQLMapping implements ApplicationContextAware, BeanNameAware {
	/** The collecting query text */
	protected AtomicReference<String> sql = new AtomicReference<String>(null);
	/** The allowed elapsed time for a SQLMap operation */
	protected long operationTimeOut = 3000;
	/** Indicates if bind variables are supported */
	protected boolean bindVarsSupported = true;
	/** SQLMapping Name */
	protected String mappingName = null;
//	/** A reference to the collector's collector cache */
//	protected Ehcache collectorCache = null;
	/** A reference to the MBeanServer where the collector is registered */
	protected MBeanServer server = null;
	/** A handle to the application context so bind providers can be located */
	protected ApplicationContext appContext = null;
	/** A bind sequence sorted map of bind variables */
	protected SortedMap<Integer, IBindVariableProvider> binds = new TreeMap<Integer, IBindVariableProvider>();
	/** A map of bind variables keyed by token */
	protected Map<String, IBindVariableProvider> tbinds = new HashMap<String, IBindVariableProvider>();
	/** The connection meta-data */
	protected Map<String, Object> connMetaData = null;
	/** The metric mapping prefix */
	protected String[] prefix = null;
	/** The tracer */
	protected ITracer tracer = null;
	/** the set of metric maps */
	protected Set<MetricMap> metricMaps = new HashSet<MetricMap>();
	/** Flag indicating this mapping is a preQuery */
	protected boolean pre = false;
	/** An array of sqlmap names of sqlmaps that are pres for this map */
	protected String[] pres = null;
	/** Class logger */
	protected static Logger LOG = Logger.getLogger(SQLMapping.class);
	

	
	
	
	/**
	 * Called on collector init.
	 * @param prefix The collector provided metric name prefix.
	 * @param tracer The collector provided  HOT tracer 
	 * @param server The collector provided MBeanServer.
	 * @throws ProviderNotFoundException
	 */
	public void init(String[] prefix, ITracer tracer, /*Ehcache collectorCache,*/ MBeanServer server) throws ProviderNotFoundException {
		this.prefix = prefix;
		//this.collectorCache = collectorCache;
		this.tracer = tracer;
		this.server = server;
		if(sql.get() != null) {
			sql.set(preBind(sql.get()));
		}		
		for(Iterator<MetricMap> mm =  metricMaps.iterator(); mm.hasNext(); ) {
			MetricMap metricMap = null;
			try {
				metricMap = mm.next();
				metricMap.init(prefix, tracer, /*collectorCache,*/ server);
			} catch (InvalidMetricMappingException e) {
				LOG.error("Failed to initialize MetricMap", e);
				mm.remove();
			}
		}
	}
	
	/**
	 * @param conn
	 * @throws SQLException
	 */
	public void execute(Connection conn) throws SQLException {
		PreparedStatement ps = null;
		Statement st = null;
		ResultSet rset = null;
		long start = System.currentTimeMillis(), elapsed = 0;
		try {
			if(bindVarsSupported) {
				if(LOG.isDebugEnabled()) LOG.debug("Compiling PreparedQuery SQL");
				String rSql = sql.get();
				for(Map.Entry<String, IBindVariableProvider> bind: tbinds.entrySet()) {					
						rSql = bind.getValue().bind(rSql, bind.getKey()).toString();				
				}
				if(LOG.isDebugEnabled()) LOG.debug("Prepared SQL:[" + rSql + "]");
				ps = conn.prepareStatement(rSql);
				for(Map.Entry<Integer, IBindVariableProvider> bind: binds.entrySet()) {
					bind.getValue().bind(ps, bind.getKey());
				}
				if(LOG.isDebugEnabled()) LOG.debug("Executing PreparedQuery");
				rset = ps.executeQuery();
			} else {
				CharSequence boundSql  = sql.get();
				for(Map.Entry<String, IBindVariableProvider> bind: tbinds.entrySet()) {
					boundSql = bind.getValue().bind(boundSql, bind.getKey());
				}				
				if(LOG.isDebugEnabled()) LOG.debug("Prepared SQL:[" + boundSql + "]");
				st = conn.createStatement();
				if(LOG.isDebugEnabled()) LOG.debug("Executing Statement");
				rset = st.executeQuery(boundSql.toString());
			}
			ProcessedResultSet prs  = new ProcessedResultSet(rset);
			prs.setQueryName(mappingName);
			//if(LOG.isDebugEnabled()) LOG.debug("Retrieved [" + prs.getRowCount() + "] rows.");
			try { rset.close(); } catch (Exception e) {}
			elapsed = System.currentTimeMillis()-start;
			//LOG.info("Elapsed Time to PRS:" + elapsed + " ms.");
			if(!pre) {
				if(LOG.isDebugEnabled()) LOG.debug("Resetting Scope on Metric Maps");
				for(MetricMap mm: metricMaps) {
					mm.resetScope();
				}
				if(LOG.isDebugEnabled()) LOG.debug("Reset Scope on Metric Maps");
				if(LOG.isDebugEnabled()) LOG.debug("Firing Trace on Metric Maps");
				while(prs.next()) {
					for(MetricMap mm: metricMaps) {					
						mm.traceMetrics(prs, connMetaData);
						mm.executeBinds(prs, connMetaData);
					}					
				}
				if(LOG.isDebugEnabled()) LOG.debug("Fired Trace on Metric Maps");
				if(LOG.isDebugEnabled()) LOG.debug("Firing Scope Failures on Metric Maps");
				for(MetricMap mm: metricMaps) {
					mm.traceScopeFailures();
				}
				if(LOG.isDebugEnabled()) LOG.debug("Fired Scope Failures on Metric Maps");				
			} else {
				if(LOG.isDebugEnabled()) LOG.debug("Firing Binds on Pre Metric Maps");
				for(MetricMap mm: metricMaps) {
					while(prs.next()) {
						mm.executeBinds(prs, connMetaData);
					}
				}
				if(LOG.isDebugEnabled()) LOG.debug("Fired Binds on Pre Metric Maps");				
			}
			elapsed = System.currentTimeMillis()-start;
			//- tracer.traceSticky(elapsed, "Collection Time (ms)", "Helios", "Collectors", "Database", mappingName);
			tracer.traceGauge(elapsed, "ElapsedTime",  "Collectors", getClass().getSimpleName(),mappingName);
		} catch (Exception e) {
			if(LOG.isEnabledFor(Level.ERROR)) LOG.error("SQLMap Execution Error:\n\tSQL:" + sql, e);
		} finally {
			try { if(rset!=null) rset.close(); } catch (Exception e) {}
			try { if(st!=null) st.close(); } catch (Exception e) {}
			try { if(ps!=null) ps.close(); } catch (Exception e) {}
		}
	}
	
	
	/**
	 * Processes tokens in the configured sql statement.
	 * For prepared statements, the tokens are replaced with <code>?</code> bind targets and the providers are stored keyed by bind sequence.
	 * For statements, the sql is not modified and the providers are stored keyed by token.
	 * @param sql The configured sql
	 * @return The processed sql, modified if using a prepared statement, unmodified if a regular statement.
	 * @throws ProviderNotFoundException 
	 */
	protected String preBind(String sql) throws ProviderNotFoundException {
		Matcher m =ProviderToken.BIND_VAR_PATTERN.matcher(sql);
		String processedSql = sql;
		int bindSeq = 1;
		while(m.find()) {
			String token = m.group();
			IBindVariableProvider provider = BindVariableProviderFactory.getInstance().getProvider(token);			
			if(bindVarsSupported && !provider.isForceNoBind()) {
				processedSql = processedSql.replace(token, "?");			
				binds.put(bindSeq, provider);
				bindSeq++;
			} else {
				tbinds.put(token, provider);
			}
		}
		LOG.info("[" + mappingName + "]Processed SQL:" + processedSql);
		return processedSql;
	}
	

//	/**
//	 * @param cache the cache to set
//	 */
//	public void setCache(Ehcache cache) {
//		this.collectorCache = cache;
//	}

	/**
	 * @param appContext the appContext to set
	 */
	public void setApplicationContext(ApplicationContext appContext) {
		this.appContext = appContext;
	}

	/**
	 * @return the sql
	 */
	public String getSql() {
		return sql.get();
	}

	/**
	 * @param sql the sql to set
	 */
	public void setSql(String sql) {
		this.sql.set(sql);
	}

	/**
	 * @return the operationTimeOut
	 */
	public long getOperationTimeOut() {
		return operationTimeOut;
	}

	/**
	 * @param operationTimeOut the operationTimeOut to set
	 */
	public void setOperationTimeOut(long operationTimeOut) {
		this.operationTimeOut = operationTimeOut;
	}

	/**
	 * @return the bindVarsSupported
	 */
	public boolean isBindVarsSupported() {
		return bindVarsSupported;
	}

	/**
	 * @param bindVarsSupported the bindVarsSupported to set
	 */
	public void setBindVarsSupported(boolean bindVarsSupported) {
		this.bindVarsSupported = bindVarsSupported;
	}

	/**
	 * @return
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((sql == null) ? 0 : sql.hashCode());
		return result;
	}

	/**
	 * @param obj
	 * @return
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SQLMapping other = (SQLMapping) obj;
		if (sql == null) {
			if (other.sql != null)
				return false;
		} else if (!sql.equals(other.sql))
			return false;
		return true;
	}

	/**
	 * Sets the bean name for this SQLMapping.
	 * @param beanName the bean name.
	 * @see org.springframework.beans.factory.BeanNameAware#setBeanName(java.lang.String)
	 */
	public void setBeanName(String beanName) {
		mappingName = beanName;		
	}

	/**
	 * @param connMetaData the connMetaData to set
	 */
	public void setConnMetaData(Map<String, Object> connMetaData) {
		this.connMetaData = connMetaData;
	}

	/**
	 * @param prefix the prefix to set
	 */
	public void setPrefix(String[] prefix) {
		this.prefix = prefix;
	}

	/**
	 * @param tracer the tracer to set
	 */
	public void setTracer(ITracer tracer) {
		this.tracer = tracer;
	}

	/**
	 * @return the mappingName
	 */
	public String getName() {
		return mappingName;
	}

	/**
	 * @param mappingName the mappingName to set
	 */
	public void setName(String mappingName) {
		this.mappingName = mappingName;
	}

	/**
	 * Adds a set of metric maps to the SQLMapping
	 * @param metricMaps the metricMaps to set
	 */
	public void setMetricMaps(Set<MetricMap> metricMaps) {
		this.metricMaps.addAll(metricMaps);
	}
	
	/**
	 * Adds a node of <code>&lt;MetricMaps&gt;</code> to the SQLMapping.
	 * @param configNode An XML node containing XML defined <code>MetricMap</code>s.
	 */
	public void setMetricMapsNode(Node configNode) {		
		Node metricMapNode = XMLHelper.getChildNodeByName(configNode, "MetricMaps", false);
		for(Node node: XMLHelper.getChildNodesByName(metricMapNode, "MetricMap", false)) {
			try {
				MetricMap mm = new MetricMap(node);
				metricMaps.add(mm);
				if(LOG.isDebugEnabled()) {
					LOG.debug("Added MetricMap to SQLMap:" + mm);
				}
			} catch (InvalidMetricMappingException e) {
				LOG.error("Failed to create metric map", e);
				
			}
		}
	}

	/**
	 * @return the pre
	 */
	public boolean isPre() {
		return pre;
	}

	/**
	 * @param pre the pre to set
	 */
	public void setPre(boolean pre) {
		this.pre = pre;
	}

	/**
	 * @return the pres
	 */
	public String[] getPres() {
		return pres;
	}

	/**
	 * @param pres the pres to set
	 */
	public void setPres(String[] pres) {
		this.pres = pres;
	}


}
