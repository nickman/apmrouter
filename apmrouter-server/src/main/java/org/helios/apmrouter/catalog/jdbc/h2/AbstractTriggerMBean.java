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
package org.helios.apmrouter.catalog.jdbc.h2;

import javax.management.ObjectName;

/**
 * <p>Title: AbstractTriggerMBean</p>
 * <p>Description: Base trigger mbean interface</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.catalog.jdbc.h2.NewElementTriggers.AbstractTriggerMBean</code></p>
 */
public interface AbstractTriggerMBean {
	/** The JMX notification type for a new host event */
	public static final String NEW_HOST = "h2.event.host";
	/** The JMX notification type for a new agent event */
	public static final String NEW_AGENT = "h2.event.agent";
	/** The JMX notification type for a new metric event */
	public static final String NEW_METRIC = "h2.event.metric.";	
	/**
	 * Returns the number of calls to this trigger
	 * @return the number of calls to this trigger
	 */
	public long getCallCount();
	
	/**
	 * Returns the size of the notification queue
	 * @return the size of the notification queue
	 */	
	public int getQueueSize();	
	
	/**
	 * Returns this trigger's JMX {@link ObjectName}
	 * @return this trigger's JMX {@link ObjectName}
	 */
	public ObjectName getOn();

	/**
	 * Returns the schema that this trigger is installed in
	 * @return the schema that this trigger is installed in
	 */
	public String getSchemaName();

	/**
	 * Returns the name of the trigger
	 * @return the name of the trigger
	 */
	public String getTriggerName();

	/**
	 * Returns the table that this trigger is attached to
	 * @return the table that this trigger is attached to
	 */
	public String getTableName();

	/**
	 * Indicates if this trigger is fired before the operation, or after
	 * @return true if this trigger is fired before the operation, false if after
	 */
	public boolean isBefore();

	/**
	 * Returns the bitmask of the operations that this trigger fires on
	 * @return the bitmask of the operations that this trigger fires on
	 */
	public int getType();
	
	/**
	 * Returns the names of the operations that this trigger fires on
	 * @return the names of the operations that this trigger fires on
	 */
	public String getTypeNames();	
}