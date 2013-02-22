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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.h2.api.Trigger;
import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.util.SystemClock;

/**
 * <p>Title: AbstractTrigger</p>
 * <p>Description: Base trigger class</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.catalog.jdbc.h2.NewElementTriggers.AbstractTrigger</code></p>
 */
public abstract class AbstractTrigger extends NotificationBroadcasterSupport implements Trigger, AbstractTriggerMBean {
	/** The trigger's JMX ObjectName */
	protected ObjectName on;
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	/** A counter of the number of calls to this trigger */
	protected final AtomicLong callCount = new AtomicLong(0L);
	/** The schema name where the trigger resides */
	protected String schemaName = null;
	/** The H2 trigger name */
	protected String triggerName = null; 
	/** The table name that the trigger is attached to  */
	protected String tableName = null;
	/** Indicates if the trigger is fired before the op, or after  */
	protected boolean before = false;
	/** The operation types that this trigger is fired on */
	protected int type = -1;
	
	/**
	 * Creates a new AbstractTrigger
	 * @param infos The MBeanNotificationInfos for the trigger
	 */
	protected AbstractTrigger(MBeanNotificationInfo...infos) {
		super(NewElementTriggers.threadPool, infos);
		log.info("Created Trigger [" + getClass().getSimpleName() + "]");
	}
	
	/**
	 * Sends a notification of the passed type 
	 * @param elementType The element type for which a new event is being broadcast (ie. METRIC, AGENT or HOST)
	 * @param type The event type
	 * @param newRow The contents of the new row
	 */
	protected void sendNotification(String elementType, String type, Object[] newRow)  {
		Notification n = new Notification(type, on, NewElementTriggers.serial.incrementAndGet(), SystemClock.time(), "New " + elementType + " Event [" + newRow[1] + "]");
		n.setUserData(newRow);
		try {
			NewElementTriggers.notificationQueue.put(n);
			//log.info("Added Notification to NewElementTriggers.notificationQueue [" + n + "]");
		} catch (Exception ex) {
			throw new RuntimeException("Failed to enqueue notification for [" + getClass().getSimpleName() + "] with new row " + Arrays.toString(newRow), ex);
		}
		sendNotification(n);		
	}
	
	
	/**
	 * Returns the number of calls to this trigger
	 * @return the number of calls to this trigger
	 */
	@Override
	public long getCallCount() {
		return callCount.get();
	}
	
	/**
	 * Returns the size of the notification queue
	 * @return the size of the notification queue
	 */
	@Override
	public int getQueueSize() {
		return NewElementTriggers.notificationQueue.size();
	}

	/**
	 * {@inheritDoc}
	 * @see org.h2.api.Trigger#init(java.sql.Connection, java.lang.String, java.lang.String, java.lang.String, boolean, int)
	 */
	@Override
	public void init(Connection conn, String schemaName, String triggerName, String tableName, boolean before, int type) throws SQLException {
		this.schemaName = schemaName;
		this.triggerName = triggerName;
		this.tableName = tableName;
		this.before = before;
		this.type = type;
		on = JMXHelper.objectName(NewElementTriggers.class.getPackage().getName(), "trigger", getClass().getSimpleName(), "type", TriggerOp.getEnabledStatesName(type));
		if(JMXHelper.getHeliosMBeanServer().isRegistered(on)) {
			try { JMXHelper.getHeliosMBeanServer().unregisterMBean(on); } catch (Exception ex) {/* No Op */}
		}
		try {
			JMXHelper.getHeliosMBeanServer().registerMBean(this, on);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to register H2 Trigger [" + on + "]", ex);
		}
		log.info("Initialized Trigger [" + getClass().getSimpleName() + "]  Type [" + TriggerOp.getEnabledStatesName(type) + "]");
		
	}
	
	/**
	 * Returns this trigger's JMX {@link ObjectName}
	 * @return this trigger's JMX {@link ObjectName}
	 */
	@Override
	public ObjectName getOn() {
		return on;
	}

	/**
	 * Returns the schema that this trigger is installed in
	 * @return the schema that this trigger is installed in
	 */
	@Override
	public String getSchemaName() {
		return schemaName;
	}

	/**
	 * Returns the name of the trigger
	 * @return the name of the trigger
	 */
	@Override
	public String getTriggerName() {
		return triggerName;
	}

	/**
	 * Returns the table that this trigger is attached to
	 * @return the table that this trigger is attached to
	 */
	@Override
	public String getTableName() {
		return tableName;
	}

	/**
	 * Indicates if this trigger is fired before the operation, or after
	 * @return true if this trigger is fired before the operation, false if after
	 */
	@Override
	public boolean isBefore() {
		return before;
	}

	/**
	 * Returns the bitmask of the operations that this trigger fires on
	 * @return the bitmask of the operations that this trigger fires on
	 */
	@Override
	public int getType() {
		return type;
	}
	
	/**
	 * Returns the names of the operations that this trigger fires on
	 * @return the names of the operations that this trigger fires on
	 */
	@Override
	public String getTypeNames() {
		return TriggerOp.getEnabledStatesName(type);
	}

	/**
	 * {@inheritDoc}
	 * @see org.h2.api.Trigger#close()
	 */
	@Override
	public void close() throws SQLException {
		/* No Op */			
	}

	/**
	 * {@inheritDoc}
	 * @see org.h2.api.Trigger#remove()
	 */
	@Override
	public void remove() throws SQLException {
		/* No Op */			
	}		
}