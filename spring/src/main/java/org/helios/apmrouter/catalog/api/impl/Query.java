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

/**
 * <p>Title: Query</p>
 * <p>Description: A parser tree container for queries.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.catalog.api.impl.Query</code></p>
 */

public class Query implements Parsed<ExecutableQuery> {
	/** The query executor for this query */
	protected ExecutableQuery edc = null;
	
	/** The JSON key for the entity name */
	public static final String ENTITY_OP = "ent";
	/** The JSON key for a named query */
	public static final String NAMED_QUERY_OP = "named";
	
	/**
	 * Creates a new Query
	 * @return a new Query
	 */
	public static Query create() {
		return new Query();
	}
	
	/**
	 * Creates a new Query
	 */
	protected Query() {
		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.catalog.api.impl.Parsed#applyPrimitive(java.lang.String, java.lang.Object)
	 */
	@Override
	public Parsed<ExecutableQuery> applyPrimitive(String key, Object value) {
		log("Applying [" + key + "]:" + value);
		if(ENTITY_OP.equals(key)) {
			edc = new ExtendedDetachedCriteria(value.toString());
		} else if(NAMED_QUERY_OP.equals(key)) {
			edc = new NamedQueryAccumulator((String)value);
		} else {
			throw new RuntimeException(new Throwable());			
		}
		return (Parsed<ExecutableQuery>)edc;
	}
	
	
	private static void log(Object msg) {
		System.out.println(msg);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.catalog.api.impl.Parsed#get()
	 */
	@Override
	public ExecutableQuery get() {
		return edc;
	}


	
	
}
