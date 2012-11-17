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

import javax.management.MBeanNotificationInfo;
import javax.management.Notification;

/**
 * <p>Title: MetricTrigger</p>
 * <p>Description: H2 new metric trigger</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.catalog.jdbc.h2.MetricTrigger</code></p>
 */

public class MetricTrigger extends AbstractTrigger implements MetricTriggerMBean {
	/**
	 * Creates a new MetricTrigger
	 */
	public MetricTrigger() {
		super(new MBeanNotificationInfo(new String[]{NEW_METRIC}, Notification.class.getName(), "A new metric registration event"));
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.h2.api.Trigger#fire(java.sql.Connection, java.lang.Object[], java.lang.Object[])
	 */
	@Override
	public void fire(Connection conn, Object[] oldRow, Object[] newRow) throws SQLException {
		if(log.isDebugEnabled()) log.debug("\n\t=================\n\tNEW AGENT:" + Arrays.toString(newRow) + "\n\t=================\n");
		sendNotification(NEW_METRIC, newRow);
	}
	
}
