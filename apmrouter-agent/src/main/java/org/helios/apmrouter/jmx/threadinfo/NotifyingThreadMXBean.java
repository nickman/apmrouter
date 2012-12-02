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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.MBeanNotificationInfo;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;

import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.util.SystemClock;

/**
 * <p>Title: NotifyingThreadMXBean</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.jmx.threadinfo.NotifyingThreadMXBean</code></p>
 */

public class NotifyingThreadMXBean extends NotificationBroadcasterSupport implements ThreadMXBean {
	private static final MBeanNotificationInfo[] notificationInfo = createMBeanInfo();
	/** The delegate ThreadMXBean */
	protected final ThreadMXBean delegate;
	/** Indicates if the delegate is installed */
	protected static final AtomicBoolean installed = new AtomicBoolean(false);
	/** The platform MBeanServer */
	protected static final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
	/** The ThreadMXBean object name */
	protected static final ObjectName THREAD_MX_NAME = JMXHelper.objectName(ManagementFactory.THREAD_MXBEAN_NAME);
	/** The JMX notification type emitted when Thread Contention Monitoring is enabled */
	public static final String NOTIF_TCM_ENABLED = "threadmxbean.tcm.enabled";
	/** The JMX notification type emitted when Thread Contention Monitoring is disabled */
	public static final String NOTIF_TCM_DISABLED = "threadmxbean.tcm.disabled";
	/** The JMX notification type emitted when Thread Timing is enabled */
	public static final String NOTIF_TCT_ENABLED = "threadmxbean.tct.enabled";
	/** The JMX notification type emitted when Thread Timing is disabled */
	public static final String NOTIF_TCT_DISABLED = "threadmxbean.tct.disabled";
	
	/** JMX notification serial number generator */
	private static final AtomicLong serial = new AtomicLong(0L);
	/** The original ThreadMXBean */
	public static final ThreadMXBean original = ManagementFactory.getThreadMXBean();
	/** Indicates if thread contention monitoring is supported */
	public static final boolean TCM_SUPPORTED = original.isThreadContentionMonitoringSupported();
	/** Indicates if thread cpu timing monitoring is supported */
	public static final boolean TCT_SUPPORTED = original.isThreadCpuTimeSupported();
	
	// record initial tct and tcm states, store in statics
	
	public static void main(String[] args) {
		log("Installing Notifier");
		install();
		try { Thread.currentThread().join(); } catch (Exception ex) {};
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	public static void install() {
		if(!installed.get()) {
			NotifyingThreadMXBean mxb = new NotifyingThreadMXBean(ManagementFactory.getThreadMXBean());
			try {
				server.unregisterMBean(THREAD_MX_NAME);
				server.registerMBean(mxb, THREAD_MX_NAME);
				installed.set(true);
			} catch (Exception ex) {
				ex.printStackTrace(System.err);
			}
		}
	}
	public static void remove() {
		if(installed.get()) {
			
		}
	}
	public static boolean isInstalled() {
		return installed.get();
	}
	
	/**
	 * Creates a new NotifyingThreadMXBean
	 * @param delegate the ThreadMXBean delegate
	 */
	private NotifyingThreadMXBean(ThreadMXBean delegate) {
		super(Executors.newFixedThreadPool(1, new ThreadFactory(){
			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r, "ThreadMXBeanNotifier");
				t.setDaemon(true);
				return t;
			}
		}), notificationInfo);
		this.delegate = delegate;
	}
	
	public ObjectName getObjectName() {
		return delegate.getObjectName();
	}
	public int getThreadCount() {
		return delegate.getThreadCount();
	}
	public int getPeakThreadCount() {
		return delegate.getPeakThreadCount();
	}
	public long getTotalStartedThreadCount() {
		return delegate.getTotalStartedThreadCount();
	}
	public int getDaemonThreadCount() {
		return delegate.getDaemonThreadCount();
	}
	public long[] getAllThreadIds() {
		return delegate.getAllThreadIds();
	}
	public ThreadInfo getThreadInfo(long id) {
		return delegate.getThreadInfo(id);
	}
	public ThreadInfo[] getThreadInfo(long[] ids) {
		return delegate.getThreadInfo(ids);
	}
	public ThreadInfo getThreadInfo(long id, int maxDepth) {
		return delegate.getThreadInfo(id, maxDepth);
	}
	public ThreadInfo[] getThreadInfo(long[] ids, int maxDepth) {
		return delegate.getThreadInfo(ids, maxDepth);
	}
	public boolean isThreadContentionMonitoringSupported() {
		return delegate.isThreadContentionMonitoringSupported();
	}
	public boolean isThreadContentionMonitoringEnabled() {
		return delegate.isThreadContentionMonitoringEnabled();
	}
	public void setThreadContentionMonitoringEnabled(boolean enable) {
		delegate.setThreadContentionMonitoringEnabled(enable);
		if(enable) {
			sendNotification(new Notification(NOTIF_TCM_ENABLED, THREAD_MX_NAME, serial.incrementAndGet(), SystemClock.time(), "Thread Contention Monitoring Enabled"));
		} else {
			sendNotification(new Notification(NOTIF_TCM_DISABLED, THREAD_MX_NAME, serial.incrementAndGet(), SystemClock.time(), "Thread Contention Monitoring Disabled"));
		}
	}
	public long getCurrentThreadCpuTime() {
		return delegate.getCurrentThreadCpuTime();
	}
	public long getCurrentThreadUserTime() {
		return delegate.getCurrentThreadUserTime();
	}
	public long getThreadCpuTime(long id) {
		return delegate.getThreadCpuTime(id);
	}
	public long getThreadUserTime(long id) {
		return delegate.getThreadUserTime(id);
	}
	public boolean isThreadCpuTimeSupported() {
		return delegate.isThreadCpuTimeSupported();
	}
	public boolean isCurrentThreadCpuTimeSupported() {
		return delegate.isCurrentThreadCpuTimeSupported();
	}
	public boolean isThreadCpuTimeEnabled() {
		return delegate.isThreadCpuTimeEnabled();
	}
	public void setThreadCpuTimeEnabled(boolean enable) {
		delegate.setThreadCpuTimeEnabled(enable);
		if(enable) {
			sendNotification(new Notification(NOTIF_TCT_ENABLED, THREAD_MX_NAME, serial.incrementAndGet(), SystemClock.time(), "Thread CPU Time Monitoring Enabled"));
		} else {
			sendNotification(new Notification(NOTIF_TCT_DISABLED, THREAD_MX_NAME, serial.incrementAndGet(), SystemClock.time(), "Thread CPU Time Monitoring Disabled"));
		}		
	}
	public long[] findMonitorDeadlockedThreads() {
		return delegate.findMonitorDeadlockedThreads();
	}
	public void resetPeakThreadCount() {
		delegate.resetPeakThreadCount();
	}
	public long[] findDeadlockedThreads() {
		return delegate.findDeadlockedThreads();
	}
	public boolean isObjectMonitorUsageSupported() {
		return delegate.isObjectMonitorUsageSupported();
	}
	public boolean isSynchronizerUsageSupported() {
		return delegate.isSynchronizerUsageSupported();
	}
	public ThreadInfo[] getThreadInfo(long[] ids, boolean lockedMonitors,
			boolean lockedSynchronizers) {
		return delegate.getThreadInfo(ids, lockedMonitors, lockedSynchronizers);
	}
	public ThreadInfo[] dumpAllThreads(boolean lockedMonitors,
			boolean lockedSynchronizers) {
		return delegate.dumpAllThreads(lockedMonitors, lockedSynchronizers);
	}
	

	
	private static MBeanNotificationInfo[] createMBeanInfo() {
		return new MBeanNotificationInfo[]{
			new MBeanNotificationInfo(new String[]{NOTIF_TCM_ENABLED, NOTIF_TCM_DISABLED}, Notification.class.getName(), "Notification indicating if ThreadContentionMonitoring (tcm) enablement has changed"),
			new MBeanNotificationInfo(new String[]{NOTIF_TCT_ENABLED, NOTIF_TCT_DISABLED}, Notification.class.getName(), "Notification indicating if ThreadContentionMonitoring (tcm) enablement has changed"),
		};		
	}
}
