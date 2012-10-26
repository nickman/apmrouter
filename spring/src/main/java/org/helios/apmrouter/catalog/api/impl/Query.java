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

public class Query implements Parsed {
	
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
	
	
	
//	String maxSize = attrs.getValue("maxSize");
//	String fetchSize = attrs.getValue("fetchSize");
//	String firstResult = attrs.getValue("firstResult");
//	String cacheEnabled = attrs.getValue("cacheEnabled");
//	String cacheMode = attrs.getValue("cacheMode");
//	String flushMode = attrs.getValue("flushMode");
//	String fetchMode = attrs.getValue("fetchMode");
//	String lockMode = attrs.getValue("lockMode");
//	String timeOut = attrs.getValue("timeOut");
//	String rowCountOnly = attrs.getValue("rowCountOnly");
	
}
