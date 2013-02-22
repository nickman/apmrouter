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
package org.helios.apmrouter.dataservice.json.catalog;


/**
 * <p>Title: MetricURIMBean</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.dataservice.json.catalog.MetricURIMBean</code></p>
 */

public interface MetricURIMBean {

	/**
	 * Returns the underlying URI that represents this MetricURI
	 * @return the underlying URI that represents this MetricURI
	 */
	public abstract String getMetricUri();

	/**
	 * Returns the metric domain 
	 * @return the metric domain
	 */
	public abstract String getDomain();

	/**
	 * Returns the metric agent 
	 * @return the metric agent
	 */
	public abstract String getAgent();

	/**
	 * Returns the maximum depth to recurse from the starting point
	 * @return the maximum depth to recurse 
	 */
	public abstract int getMaxDepth();

	/**
	 * Returns the metric name specified in the URI
	 * @return the metric name
	 */
	public abstract String getMetricName();

	/**
	 * Returns an array of the metric type ordinals specified in the URI
	 * @return an array of the metric type ordinals
	 */
	public abstract int[] getMetricType();
	
	/**
	 * Returns an array of the metric type names specified in the URI
	 * @return an array of the metric type names
	 */
	public String[] getMetricTypeNames();
	
	/**
	 * Returns an array of the metric status names specified in the URI
	 * @return an array of the metric status names
	 */
	public String[] getMetricStatusNames();	

	/**
	 * Returns an array of the metric status ordinals specified in the URI
	 * @return an array of the metric status ordinals
	 */
	public abstract int[] getMetricStatus();

	/**
	 * Returns the sql to be used to retrieve the metric Ids for this metric URI
	 * @return the sql to be used to retrieve the metric Ids for this metric URI
	 */
	public abstract String getMetricIdSql();
	
	/**
	 * Returns the enabled subscription type bit mask 
	 * @return the enabled subscription type bit mask
	 */
	public byte getSubscriptionType();
	
	/**
	 * Returns a pipe delimited string of the enabled subscription type names
	 * @return a pipe delimited string of the enabled subscription type names
	 */
	public String getSubscriptionTypeNames();
	

}