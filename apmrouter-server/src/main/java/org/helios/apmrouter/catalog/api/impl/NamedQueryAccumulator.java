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
package org.helios.apmrouter.catalog.api.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.engine.QueryParameters;
import org.hibernate.impl.QueryImpl;

/**
 * <p>Title: NamedQueryAccumulator</p>
 * <p>Description: Accumulator for named queries</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.catalog.api.impl.NamedQueryAccumulator</code></p>
 */

public class NamedQueryAccumulator implements Parsed<NamedQueryAccumulator>, ExecutableQuery {
	/** The named query name */
	protected final String queryName;
	/** The accumulated bind parameters */
	protected final Map<Object, Object> binds = new HashMap<Object, Object>();
	/** Indicates if the query has named parameters */
	protected boolean namedParams = false;
	
	public static NamedQueryAccumulator create(String name) {
		return new NamedQueryAccumulator(name);
	}
	
	/**
	 * Creates a new NamedQueryAccumulator
	 * @param queryName The named query
	 */
	public NamedQueryAccumulator(String queryName) {
		super();
		this.queryName = queryName;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.catalog.api.impl.ExecutableQuery#execute(org.hibernate.Session)
	 */
	@Override
	public List<?> execute(Session session) {
		return prepare(session).list();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.catalog.api.impl.ExecutableQuery#unique(org.hibernate.Session)
	 */
	@Override
	public Object unique(Session session) {
		return prepare(session).uniqueResult();
	}
	
	
	/**
	 * Acquires the named query and executes the binds
	 * @param session The hibernate session
	 * @return A query
	 */
	protected QueryImpl prepare(Session session) {
		QueryImpl query = (QueryImpl)session.getNamedQuery(queryName);
		QueryParameters qp = query.getQueryParameters(binds);		
		namedParams = query.hasNamedParameters();
		if(!binds.isEmpty()) {
			if(namedParams) {
				doNamedBinds(query);
			} else {
				doOrdinalBinds(query);
			}
		}
		return query;
	}

	
	
	/**
	 * Executes the named parameter binds for this query
	 * @param query The query to execute the binds for
	 */
	protected void doNamedBinds(QueryImpl query) {
		for(Map.Entry<Object, Object> bind: binds.entrySet()) {
			String pName = bind.getKey().toString();
			query.setParameter(pName, bind.getValue());
		}
	}
	
	/**
	 * Executes the positional parameter binds for this query
	 * @param query The query to execute the binds for
	 */
	protected void doOrdinalBinds(QueryImpl query) {
		for(Map.Entry<Object, Object> bind: binds.entrySet()) {
			int pos = ((Number)bind.getKey()).intValue();
			query.setParameter(pos, bind.getValue());			
		}
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.catalog.api.impl.Parsed#applyPrimitive(java.lang.String, java.lang.Object)
	 */
	@Override
	public Parsed<NamedQueryAccumulator> applyPrimitive(String op, Object value) {
		
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.catalog.api.impl.Parsed#get()
	 */
	@Override
	public NamedQueryAccumulator get() {
		return this;
	}

}
