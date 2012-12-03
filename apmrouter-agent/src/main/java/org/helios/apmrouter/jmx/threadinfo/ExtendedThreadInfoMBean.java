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
package org.helios.apmrouter.jmx.threadinfo;

import java.lang.Thread.State;
import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;

/**
 * <p>Title: ExtendedThreadInfoMBean</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.jmx.threadinfo.ExtendedThreadInfoMBean</code></p>
 */

public interface ExtendedThreadInfoMBean {

	/**
	 * @return
	 * @see java.lang.Object#hashCode()
	 */
	public abstract int hashCode();

	/**
	 * @param obj
	 * @return
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public abstract boolean equals(Object obj);

	/**
	 * @return
	 * @see java.lang.management.ThreadInfo#getThreadId()
	 */
	public abstract long getThreadId();

	/**
	 * @return
	 * @see java.lang.management.ThreadInfo#getThreadName()
	 */
	public abstract String getThreadName();

	/**
	 * @return
	 * @see java.lang.management.ThreadInfo#getThreadState()
	 */
	public abstract State getThreadState();

	/**
	 * @return
	 * @see java.lang.management.ThreadInfo#getBlockedTime()
	 */
	public abstract long getBlockedTime();

	/**
	 * @return
	 * @see java.lang.management.ThreadInfo#getBlockedCount()
	 */
	public abstract long getBlockedCount();

	/**
	 * @return
	 * @see java.lang.management.ThreadInfo#getWaitedTime()
	 */
	public abstract long getWaitedTime();

	/**
	 * @return
	 * @see java.lang.management.ThreadInfo#getWaitedCount()
	 */
	public abstract long getWaitedCount();

	/**
	 * @return
	 * @see java.lang.management.ThreadInfo#getLockInfo()
	 */
	public abstract LockInfo getLockInfo();

	/**
	 * @return
	 * @see java.lang.management.ThreadInfo#getLockName()
	 */
	public abstract String getLockName();

	/**
	 * @return
	 * @see java.lang.management.ThreadInfo#getLockOwnerId()
	 */
	public abstract long getLockOwnerId();

	/**
	 * @return
	 * @see java.lang.management.ThreadInfo#getLockOwnerName()
	 */
	public abstract String getLockOwnerName();

	/**
	 * @return
	 * @see java.lang.management.ThreadInfo#getStackTrace()
	 */
	public abstract StackTraceElement[] getStackTrace();

	/**
	 * @return
	 * @see java.lang.management.ThreadInfo#isSuspended()
	 */
	public abstract boolean isSuspended();

	/**
	 * @return
	 * @see java.lang.management.ThreadInfo#isInNative()
	 */
	public abstract boolean isInNative();

	/**
	 * @return
	 * @see java.lang.management.ThreadInfo#toString()
	 */
	public abstract String toString();

	/**
	 * @return
	 * @see java.lang.management.ThreadInfo#getLockedMonitors()
	 */
	public abstract MonitorInfo[] getLockedMonitors();

	/**
	 * @return
	 * @see java.lang.management.ThreadInfo#getLockedSynchronizers()
	 */
	public abstract LockInfo[] getLockedSynchronizers();

}