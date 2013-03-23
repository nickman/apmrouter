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
package org.helios.apmrouter.server.tracing;

import java.util.Date;

/**
 * <p>Title: VirtualTracerMBean</p>
 * <p>Description: Mbean interface for {@link VirtualTracer}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.tracing.VirtualTracerMBean</code></p>
 */

public interface VirtualTracerMBean {

	/** The ObjectName template for non-0-serial virtual agents */
	public static final String VA_OBJ_NAME = "org.helios.apmrouter.agent:type=VirtualAgent,host=%s,agent=%s";

	/**
	 * Returns the timestamp of the last touch
	 * @return the timestamp of the last touch
	 */
	public abstract long getLastTouchTimestamp();

	/**
	 * Returns the date of the last touch
	 * @return the date of the last touch
	 */
	public abstract Date getLastTouchDate();

	/**
	 * Returns the virtual agent's serial id
	 * @return the virtual agent's serial id
	 */
	public abstract long getSerial();

	/**
	 * Touches the virtual agent's timestamp
	 */
	public abstract void touch();

	/**
	 * Expires this virtual agent
	 */
	public abstract void expire();

	/**
	 * Determines if this va is expired
	 * @return true if this va is expired, false otherwise
	 */
	public abstract boolean isExpired();

	/**
	 * Returns the number of ms. until this virtual agent's expiry unless it is touched before then
	 * @return Returns the number of ms. until this virtual agent's expiry
	 */
	public abstract long getTimeToExpiry();
	
	/**
	 * Returns the number of sent metrics
	 * @return the number of sent metrics
	 */
	public long getSentMetrics();

	/**
	 * Returns the number of sent metrics
	 * @return the number of sent metrics
	 */
	public long getDroppedMetrics();
	
	/**
	 * Resets the localStats
	 */
	public void resetStats();
	
	/**
	 * Returns this virtual agent's host
	 * @return this virtual agent's host
	 */
	public String getHost();
	
	/**
	 * Returns this virtual agent's agent name
	 * @return this virtual agent's agent name
	 */
	public String getAgent();
	

}