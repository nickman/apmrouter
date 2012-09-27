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
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
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
	protected final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();	
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
	protected int maxStackDepth = 100;
	/** A map of the last GC time keyed by the gc-collector name */
	protected final Map<String, Long> lastGCTime = new HashMap<String, Long>(gcMXBeans.size());
	/** A map of the last GC collection time keyed by the gc-collector name */
	protected final Map<String, Long> lastGCCollectTime = new HashMap<String, Long>(gcMXBeans.size());
	/** A map of the max pool sizes keyed by pool name */
	protected final Map<String, Long> maxPoolSize = new HashMap<String, Long>(gcMXBeans.size());
	/** Indicates if the initial static runtime stats have been collected */
	protected boolean initialRuntimeCollected = false;
	

	
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
		try { collectGc(); } catch (Exception e) {}
		try { collectThreads();  } catch (Exception e) {}
		try { collectCompilation(); } catch (Exception e) {}
		if(maxPoolSize.size()<1) {
			try { collectInitialMemory(); } catch (Exception e) {}
			try { collectInitialMemoryPools(); } catch (Exception e) {}
		}
		try { collectMemory(); } catch (Exception e) {}
		try { collectMemoryPools(); } catch (Exception e) {}
		try { collectClassLoading(); } catch (Exception e) {}
		if(!initialRuntimeCollected) {
			try { collectInitialRuntime(); } catch (Exception e) {}
		}
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
		tracer.traceLong(deadlocked==null ? 0 : deadlocked.length, "DeadlockedThreadCount", "JVM", "Threads");
		if(deadlocked != null && deadlocked.length>0) {
			StringBuilder dlockInfo = new StringBuilder();
			ThreadInfo[] tis = threadMXBean.getThreadInfo(deadlocked, maxStackDepth);
			for(ThreadInfo ti : tis) {
				dlockInfo.append("\nID:").append(ti.getThreadName()).append("-").append(ti.getThreadId());
				dlockInfo.append("\nLockName:").append(ti.getLockName());
				dlockInfo.append("\nLockOwnerId:").append(ti.getLockOwnerId());
				dlockInfo.append("\nLockOwnerName:").append(ti.getLockOwnerName());
				dlockInfo.append("\nStack:");
				for(StackTraceElement ste: ti.getStackTrace()) {
					dlockInfo.append("\n\t").append(ste.toString());
				}
			}
			tracer.traceString(dlockInfo, "DeadlockedThreadInfo", "JVM", "Threads");
		}		
	}
	
	/**
	 * Collects compilation time 
	 */
	protected void collectCompilation() {
		tracer.traceDelta(compilationMXBean.getTotalCompilationTime(), "CompilationTime", "JVM", "Compilation", compilationMXBean.getName());
	}
	
	/**
	 * Collects the static memory pool data
	 */
	protected void collectInitialMemoryPools() {
		for(MemoryPoolMXBean pool: memoryPoolMXBeans) {
			MemoryUsage usage = pool.getUsage();
			tracer.traceLong(usage.getInit(), "Initial", "JVM", "MemoryPools", pool.getType().name(), pool.getName());
			tracer.traceLong(usage.getMax(), "Maximum", "JVM", "MemoryPools", pool.getType().name(), pool.getName());
			maxPoolSize.put(pool.getName(), usage.getMax());
		}
	}
	
	/**
	 * Collects the dynamic memory pool data
	 */
	protected void collectMemoryPools() {
		for(MemoryPoolMXBean pool: memoryPoolMXBeans) {
			MemoryUsage usage = pool.getUsage();
			tracer.traceLong(usage.getCommitted(), "Committed", "JVM", "MemoryPools", pool.getType().name(), pool.getName());
			tracer.traceLong(usage.getUsed(), "Used", "JVM", "MemoryPools", pool.getType().name(), pool.getName());
			tracer.traceLong(percent(usage.getCommitted(), usage.getUsed()), "PercentUsed", "JVM", "MemoryPools", pool.getType().name(), pool.getName());
			tracer.traceLong(percent(maxPoolSize.get(pool.getName()), usage.getUsed()), "PercentCapacity", "JVM", "MemoryPools", pool.getType().name(), pool.getName());
		}
	}
	
	/**
	 * Collects the static memory data
	 */
	protected void collectInitialMemory() {
			MemoryUsage usage = memoryMXBean.getHeapMemoryUsage();
			tracer.traceLong(usage.getInit(), "Initial", "JVM", "Memory", "Heap");
			tracer.traceLong(usage.getMax(), "Maximum", "JVM", "Memory", "Heap");
			maxPoolSize.put("Heap", usage.getMax());
			usage = memoryMXBean.getNonHeapMemoryUsage();
			tracer.traceLong(usage.getInit(), "Initial", "JVM", "Memory", "NonHeap");
			tracer.traceLong(usage.getMax(), "Maximum", "JVM", "Memory", "NonHeap");
			maxPoolSize.put("NonHeap", usage.getMax());
	}
	
	/**
	 * Collects the dynamic memory data
	 */
	protected void collectMemory() {
		MemoryUsage usage = memoryMXBean.getHeapMemoryUsage();
		tracer.traceLong(usage.getCommitted(), "Committed", "JVM", "Memory", "Heap");
		tracer.traceLong(usage.getUsed(), "Used", "JVM", "Memory", "Heap");
		tracer.traceLong(percent(usage.getCommitted(), usage.getUsed()), "PercentUsed", "JVM", "Memory", "Heap");
		tracer.traceLong(percent(maxPoolSize.get("Heap"), usage.getUsed()), "PercentCapacity", "JVM", "Memory", "Heap");
		
		usage = memoryMXBean.getNonHeapMemoryUsage();
		tracer.traceLong(usage.getCommitted(), "Committed", "JVM", "Memory", "NonHeap");
		tracer.traceLong(usage.getUsed(), "Used", "JVM", "Memory", "NonHeap");
		tracer.traceLong(percent(usage.getCommitted(), usage.getUsed()), "PercentUsed", "JVM", "Memory", "NonHeap");
		tracer.traceLong(percent(maxPoolSize.get("NonHeap"), usage.getUsed()), "PercentCapacity", "JVM", "Memory", "NonHeap");
	}
	
	/**
	 * Collects class loading stats
	 */
	protected void collectClassLoading() {
		tracer.traceLong(classLoadingMXBean.getTotalLoadedClassCount(), "TotalLoadedClasses", "JVM", "ClassLoading");
		tracer.traceDelta(classLoadingMXBean.getTotalLoadedClassCount(), "ClassLoadRate", "JVM", "ClassLoading");
		tracer.traceLong(classLoadingMXBean.getUnloadedClassCount(), "UnloadedClassCount", "JVM", "ClassLoading");
		tracer.traceDelta(classLoadingMXBean.getUnloadedClassCount(), "ClassUnloadRate", "JVM", "ClassLoading");
		tracer.traceLong(classLoadingMXBean.getLoadedClassCount(), "CurrentClassCount", "JVM", "ClassLoading");		
	}
	
	
	/**
	 * Collects static runtime info 
	 */
	protected void collectInitialRuntime() {
		tracer.traceLong(runtimeMXBean.getStartTime(), "StartTime", "JVM", "Runtime");
		tracer.traceString(runtimeMXBean.getVmName(), "VmName", "JVM", "Runtime");
		tracer.traceString(runtimeMXBean.getVmVendor(), "VmVendor", "JVM", "Runtime");
		tracer.traceString(runtimeMXBean.getVmVersion(), "VmVersion", "JVM", "Runtime");
		tracer.traceString(runtimeMXBean.getInputArguments().toString(), "InputArguments", "JVM", "Runtime");
		tracer.traceString(runtimeMXBean.getName(), "RuntimeName", "JVM", "Runtime");
		initialRuntimeCollected = true;
	}
	
	/**
	 * Collects dynamic runtime info 
	 */
	protected void collectRuntime() {
		tracer.traceLong(runtimeMXBean.getUptime(), "UpTime", "JVM", "Runtime");
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
