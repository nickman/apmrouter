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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.Notification;

import org.helios.apmrouter.jmx.ThreadPoolFactory;


/**
 * <p>Title: NewElementTriggers</p>
 * <p>Description: Triggers on HOST, AGENT and METRIC</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.catalog.jdbc.h2.NewElementTriggers</code></p>
 */

public class NewElementTriggers {
	/** The notification broadcaster thread pool */
	protected static final Executor threadPool = ThreadPoolFactory.newCachedThreadPool(NewElementTriggers.class.getPackage().getName(), "H2Triggers");
	/** A serial number for notification sequences */
	protected static final AtomicLong serial = new AtomicLong(0L);
	/** A queue to write updates into */
	public static final BlockingQueue<Notification> notificationQueue = new ArrayBlockingQueue<Notification>(10000, true);

	/** A queue to write metric state change events into */
	public static final BlockingQueue<Object[]> metricStateChangeQueue = new ArrayBlockingQueue<Object[]>(10000, true);
	/** A queue to write new metric events into */
	public static final BlockingQueue<Object[]> newMetricQueue = new ArrayBlockingQueue<Object[]>(10000, true);
	/** A queue to write real time data events into */
	public static final BlockingQueue<Object[]> realTimeDataQueue = new ArrayBlockingQueue<Object[]>(10000, false);

	

}
