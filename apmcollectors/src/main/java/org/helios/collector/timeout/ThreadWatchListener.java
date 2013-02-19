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
package org.helios.collector.timeout;

/**
 * <p>Title: ThreadWatchListener</p>
 * <p>Description: Defines a callback listener for ThreadWatcher timeouts.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision$
 * $HeadURL$
 * $Id$
 */
public interface ThreadWatchListener {

	/**
	 * Callback that occurs against a registered ThreadWatchListener when the target ThreadWatcher timesout a thread call.
	 * @param watchedThread The watched thread.
	 * @param timeout The configured timeout value.
	 * @param stackEntry The top thread stack entry of the watched thread collected when the timeout occured.
	 * @param blockCount The number of blocks encountered by the target thread during the watch period.
	 * @param waitCount The number of waits encountered by the target thread during the watch period.
	 */
	public void onThreadOverrun(Thread watchedThread, long timeout, String stackEntry, long blockCount, long waitCount);
}
