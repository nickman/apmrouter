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
	protected final ObjectName on;
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	/** A counter of the number of calls to this trigger */
	protected final AtomicLong callCount = new AtomicLong(0L);
	
	/**
	 * Creates a new AbstractTrigger
	 * @param infos The MBeanNotificationInfos for the trigger
	 */
	protected AbstractTrigger(MBeanNotificationInfo...infos) {
		super(NewElementTriggers.threadPool, infos);
		on = JMXHelper.objectName(NewElementTriggers.class.getPackage().getName(), "trigger", getClass().getSimpleName());
		if(JMXHelper.getHeliosMBeanServer().isRegistered(on)) {
			try { JMXHelper.getHeliosMBeanServer().unregisterMBean(on); } catch (Exception ex) {}
		}
		try {
			JMXHelper.getHeliosMBeanServer().registerMBean(this, on);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to register H2 Trigger [" + on + "]", ex);
		}
		log.info("Created Trigger [" + getClass().getSimpleName() + "]");
	}
	
	/**
	 * Sends a notification of the passed type 
	 * @param type The event type
	 * @param newRow The contents of the new row
	 * @throws SQLException Thrown if the thread is interrupted while waiting to enqueue the notification.
	 */
	protected void sendNotification(String type, Object[] newRow) throws SQLException {
		Notification n = new Notification(type, on, NewElementTriggers.serial.incrementAndGet(), SystemClock.time(), "New Host Event [" + newRow[1] + "]");
		n.setUserData(newRow);
		try {
			NewElementTriggers.notificationQueue.put(n);
		} catch (Exception ex) {
			throw new SQLException("Failed to enqueue notification for [" + getClass().getSimpleName() + "] with new row " + Arrays.toString(newRow), ex);
		}
		sendNotification(n);
		callCount.incrementAndGet();
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
		/* No Op */
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