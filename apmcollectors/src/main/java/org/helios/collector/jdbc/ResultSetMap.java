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


import java.net.URL;

import org.helios.collector.jdbc.extract.IProcessedResultSet;
import org.w3c.dom.Node;


/**
 * <p>Title: ResultSetMap</p>
 * <p>Description: A syntax parser for defining a subsidiary mapping within a <code>SQLMapping<code>.</p>
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public class ResultSetMap {
	/**	The segment portion of the metric */
	protected String[] metricSegment = null;
	/**	The metric name */
	protected String metricName = null;
	/** The column id the data should come from */
	protected int column = -1;
	/** The tracing counter type */
	protected String counterType = null;
	/**	The attribute name */
	protected String attributeName = null;
	/** Indicates if a CacheResult is applied to this query */
	protected boolean cacheResult = false;
	/** The attribute name within the MBean in which to store the cached result set */
	protected String cacheResultAttributeName = null;
	/** Indicates if a trace is applied to this query */
	protected boolean traceDefined = false;
	/** The URL for the source groovy code to perform the result set post-process */
	protected URL postProcessorURL = null;
	/** The last processing elapsed time for the post processor */
	protected long postProcessorElapsedTime = 0;
	/** Indicates if PreparedStatements and Bind variables should be used */
	protected boolean useBinds = true;
	/** Indicates if the trace should be scoped */
	protected boolean scoped = false;
	/** The scope value */
	protected Object scopeValue = null;

	/**
	 * @param rsetMapNode
	 */
	public ResultSetMap(Node rsetMapNode) {

	}

	/**
	 * Extracts and traces metrics from the processed result set.
	 * @param prs the IProcessedResultSet to extract metrics from.
	 */
	public void trace(IProcessedResultSet prs) {

	}

}
