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
package org.helios.apmrouter.server;

import java.util.Date;

import org.cliffc.high_scale_lib.Counter;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;



/**
 * <p>Title: ServerComponentMXBean</p>
 * <p>Description: Optional MXBean interface for MX component interfaces to extend so built in metrics are not hidden</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.ServerComponentMXBean</code></p>
 */

public interface ServerComponentMXBean {
	/**
	 * Returns the bean name
	 * @return the bean name
	 */
	public String getBeanName();
	
	/**
	 * Called when bean is started.
	 * @throws Exception thrown on any error
	 */
	public void start() throws Exception;
	
	/**
	 * Called when bean is stopped
	 */
	public void stop();
	
	/**
	 * Indicates if this component is started
	 * @return true if this component is started, false otherwise
	 */
	public boolean isStarted();	
	
	/**
	 * Returns the level of this instance's logger
	 * @return the level of this instance's logger
	 */
	public String getLevel();
	
	/**
	 * Sets the logging level for this instance
	 * @param levelName the name of the logging level for this instance
	 */
	public void setLevel(String levelName);
	
	/**
	 * Resets all the metrics
	 */
	public void resetMetrics();
	
	/**
	 * Returns the names of the metrics supported by this component
	 * @return the names of the metrics supported by this component
	 */
	public String[] getMetricNames();
	
	/**
	 * Returns the UTC long timestamp of the last time the metrics were reset
	 * @return a UTC long timestamp 
	 */	
	public long getLastMetricResetTime();

	/**
	 * Returns the java date timestamp of the last time the metrics were reset
	 * @return a java date
	 */
	public Date getLastMetricResetDate();	
	
	

}
