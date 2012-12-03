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
import java.lang.management.ThreadInfo;

/**
 * <p>Title: ExtendedThreadInfo</p>
 * <p>Description: Wrapper adding additional functionality for standard {@link ThreadInfo}s </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.jmx.threadinfo.ExtendedThreadInfo</code></p>
 */

public class ExtendedThreadInfo implements ExtendedThreadInfoMBean {
	/** The wrapped thread info */
	private final ThreadInfo delegate;
	
	/**
	 * Wraps an array of {@link ThreadInfo}s.
	 * @param infos The array of {@link ThreadInfo}s to wrap
	 * @return an array of ExtendedThreadInfos
	 */
	public static ExtendedThreadInfo[] wrapThreadInfos(ThreadInfo...infos) {
		ExtendedThreadInfo[] xinfos = new ExtendedThreadInfo[infos.length];
		for(int i = 0; i < infos.length; i++) {
			xinfos[i] = new ExtendedThreadInfo(infos[i]);
		}
		return xinfos;
	}
	
	/**
	 * Creates a new ExtendedThreadInfo
	 * @param threadInfo the delegate thread info
	 */
	ExtendedThreadInfo(ThreadInfo threadInfo) {
		delegate = threadInfo;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.threadinfo.ExtendedThreadInfoMBean#hashCode()
	 */
	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.threadinfo.ExtendedThreadInfoMBean#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.threadinfo.ExtendedThreadInfoMBean#getThreadId()
	 */
	@Override
	public long getThreadId() {
		return delegate.getThreadId();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.threadinfo.ExtendedThreadInfoMBean#getThreadName()
	 */
	@Override
	public String getThreadName() {
		return delegate.getThreadName();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.threadinfo.ExtendedThreadInfoMBean#getThreadState()
	 */
	@Override
	public State getThreadState() {
		return delegate.getThreadState();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.threadinfo.ExtendedThreadInfoMBean#getBlockedTime()
	 */
	@Override
	public long getBlockedTime() {
		return delegate.getBlockedTime();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.threadinfo.ExtendedThreadInfoMBean#getBlockedCount()
	 */
	@Override
	public long getBlockedCount() {
		return delegate.getBlockedCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.threadinfo.ExtendedThreadInfoMBean#getWaitedTime()
	 */
	@Override
	public long getWaitedTime() {
		return delegate.getWaitedTime();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.threadinfo.ExtendedThreadInfoMBean#getWaitedCount()
	 */
	@Override
	public long getWaitedCount() {
		return delegate.getWaitedCount();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.threadinfo.ExtendedThreadInfoMBean#getLockInfo()
	 */
	@Override
	public LockInfo getLockInfo() {
		return delegate.getLockInfo();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.threadinfo.ExtendedThreadInfoMBean#getLockName()
	 */
	@Override
	public String getLockName() {
		return delegate.getLockName();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.threadinfo.ExtendedThreadInfoMBean#getLockOwnerId()
	 */
	@Override
	public long getLockOwnerId() {
		return delegate.getLockOwnerId();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.threadinfo.ExtendedThreadInfoMBean#getLockOwnerName()
	 */
	@Override
	public String getLockOwnerName() {
		return delegate.getLockOwnerName();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.threadinfo.ExtendedThreadInfoMBean#getStackTrace()
	 */
	@Override
	public StackTraceElement[] getStackTrace() {
		return delegate.getStackTrace();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.threadinfo.ExtendedThreadInfoMBean#isSuspended()
	 */
	@Override
	public boolean isSuspended() {
		return delegate.isSuspended();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.threadinfo.ExtendedThreadInfoMBean#isInNative()
	 */
	@Override
	public boolean isInNative() {
		return delegate.isInNative();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.threadinfo.ExtendedThreadInfoMBean#toString()
	 */
	@Override
	public String toString() {
		return delegate.toString();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.threadinfo.ExtendedThreadInfoMBean#getLockedMonitors()
	 */
	@Override
	public MonitorInfo[] getLockedMonitors() {
		return delegate.getLockedMonitors();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.threadinfo.ExtendedThreadInfoMBean#getLockedSynchronizers()
	 */
	@Override
	public LockInfo[] getLockedSynchronizers() {
		return delegate.getLockedSynchronizers();
	}
	

}
