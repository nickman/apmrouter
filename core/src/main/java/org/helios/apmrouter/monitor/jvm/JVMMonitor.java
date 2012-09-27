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
package org.helios.apmrouter.monitor.jvm;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.helios.apmrouter.monitor.AbstractMonitor;
import org.helios.apmrouter.util.SystemClock;

/**
 * <p>Title: JVMMonitor</p>
 * <p>Description: A monitor implementation to collect and trace stats on the JVM's health and status</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.monitor.jvm.JVMMonitor</code></p>
 */

public class JVMMonitor extends AbstractMonitor {
	/** The JVM's ThreadMXBean */
	protected final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
	/** The JVM's MemoryMXBean */
	protected final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
	/** The JVM's ClassLoaderMXBean */
	protected final ClassLoadingMXBean classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();
	/** The JVM's CompilationMXBean */
	protected final CompilationMXBean compilationMXBean = ManagementFactory.getCompilationMXBean();
	/** The JVM's OSMXBean */
	protected final OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();
	/** The JVM's RuntimeMXBean */
	protected final RuntimeMXBean runtimesMXBean = ManagementFactory.getRuntimeMXBean();	
	/** The JVM's MemoryPoolMXBeans */
	protected final Set<MemoryPoolMXBean> memoryPoolMXBeans = new HashSet<MemoryPoolMXBean>(ManagementFactory.getMemoryPoolMXBeans());
	/** The JVM's GCMXBeans */
	protected final Set<GarbageCollectorMXBean> gcMXBeans = new HashSet<GarbageCollectorMXBean>(ManagementFactory.getGarbageCollectorMXBeans());
	
	/** The number of processors */
	public final int PROCESSOR_COUNT = osMXBean.getAvailableProcessors();
	
	/** The last collection timestamp */
	protected long lastCollectTime = -1;
	/** The number of collects after which resetable stats are reset */
	protected int resetLoopCount = 2;
	/** The number of collects since the last reset */
	protected int resetLoops = 0;
	/** Indicates if this is a reset loop */
	protected boolean resetLoop = false;
	/** The max stack depth to collect on interesting threads */
	int maxStackDepth;
	/** A map of the last GC time keyed by the gc-collector name */
	protected final Map<String, Long> lastGCTime = new HashMap<String, Long>(gcMXBeans.size());
	/** A map of the last GC collection time keyed by the gc-collector name */
	protected final Map<String, Long> lastGCCollectTime = new HashMap<String, Long>(gcMXBeans.size());
	
	/**
	 * Creates a new JVMMonitor
	 */
	public JVMMonitor() {
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.monitor.AbstractMonitor#doCollect()
	 */
	@Override
	protected void doCollect() {
		resetLoops++;
		if(resetLoops==resetLoopCount) {
			resetLoop = true;
			resetLoops = 0;
		} else {
			resetLoop = false;
		}
		lastCollectTime = SystemClock.time();
	}
	
	/**
	 * Collects GC activity data
	 */
	protected void collectGc() {
		for(GarbageCollectorMXBean gc: gcMXBeans) {
			long currentTime = SystemClock.time();
			String name = gc.getName();
			tracer.traceDelta(gc.getCollectionCount(), "CollectionCount", "JVM", "GarbageCollection", name);
			long time = gc.getCollectionTime();
			tracer.traceDelta(time, "CollectionTime", "JVM", "GarbageCollection", name);
			Long prior = lastGCTime.put(name, time);
			if(prior!=null) {
				long gcTime = time-prior;
				long elapsedTime = lastGCCollectTime.put(name, currentTime)*PROCESSOR_COUNT;
				tracer.traceLong(percent(elapsedTime, gcTime), "PercentTimeInCollect", "JVM", "GarbageCollection", name);
			} else {
				lastGCCollectTime.put(name, currentTime);
			}
		}
	}
	
	/**
	 * Collects threading stats
	 */
	protected void collectThreads() {
		int tc = threadMXBean.getThreadCount();
		int dtc = threadMXBean.getDaemonThreadCount();
		int ndtc = tc-dtc;
		tracer.traceLong(tc, "ThreadCount", "JVM", "Threads");
		tracer.traceLong(dtc, "DaemonThreadCount", "JVM", "Threads");
		tracer.traceLong(ndtc, "NonDaemonThreadCount", "JVM", "Threads");
		tracer.traceLong(threadMXBean.getPeakThreadCount(), "PeakThreadCount", "JVM", "Threads");
		if(resetLoop) threadMXBean.resetPeakThreadCount();
		long[] deadlocked = threadMXBean.findMonitorDeadlockedThreads();
		tracer.traceLong(deadlocked.length, "DeadlockedThreadCount", "JVM", "Threads");
		if(deadlocked.length>0) {
			StringBuilder dlockInfo = new StringBuilder();
			ThreadInfo[] tis = threadMXBean.getThreadInfo(deadlocked, maxStackDepth);
			for(ThreadInfo ti : tis) {
				dlockInfo.append(ti.getThreadName()).append("-").append(ti.getThreadId());
			}
			tracer.traceString(dlockInfo, "DeadlockedThreadInfo", "JVM", "Threads");
		}
		
	}
	
	/**
	 * Calcs a percentage
	 * @param total The total amount
	 * @param part The part of the total
	 * @return The percentage that the part is of the total
	 */
	protected long percent(double total, double part) {
		if(total==0 || part==0) {
			return 0L;
		}
		double d = part/total*100;
		return (long)d;
	}
}
