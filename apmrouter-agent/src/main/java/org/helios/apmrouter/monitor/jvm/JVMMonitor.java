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

import java.lang.Thread.State;
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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.Attribute;
import javax.management.ObjectName;

import org.helios.apmrouter.jmx.JMXHelper;
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
	/** Indicates if this is Java 7*/
	protected final boolean isJava7;
	/** The JMX ObjectName pattern for the NIO MXBeans */
	protected final ObjectName NIO_MXBEAN_PATTERN = JMXHelper.objectName("java.nio:type=BufferPool,name=*");
	/** The JMX ObjectNames for the NIO MXBeans */
	protected final Map<String, ObjectName> nioObjectNames = new HashMap<String, ObjectName>();
	/** Convenience map for the NIO MXBean attribute unmapping */
	protected final Map<String, Long> nioAttrValues = new HashMap<String, Long>(3);

	/** The attribute names we want to retrieve for the NIO mxbeans */
	protected static final String[] NIO_ATTR_NAMES = new String[]{"Count", "MemoryUsed", "TotalCapacity"};
	

	
	/**
	 * Creates a new JVMMonitor
	 */
	public JVMMonitor() {
		boolean tmp = false;
		try {
			Class.forName("java.lang.management.BufferPoolMXBean");
			tmp = true;
		} catch (Exception e) {
			tmp = false;			
		}
		isJava7 = tmp;
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.monitor.AbstractMonitor#doCollect(long)
	 */
	@Override
	protected void doCollect(long collectionSweep) {
		resetLoops++;
		if(resetLoops==resetLoopCount) {
			resetLoop = true;
			resetLoops = 0;
		} else {
			resetLoop = false;
		}
		lastCollectTime = SystemClock.time();
		tracer.traceCounter(Integer.parseInt(runtimeMXBean.getName().split("@")[0]), "PID");
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
		if(isJava7) {
			try { collectNioBuffers(); } catch (Exception e) {}
		}
	}
	
	protected void collectNioBuffers() {
		if(nioObjectNames.isEmpty()) {
			for(ObjectName on: ManagementFactory.getPlatformMBeanServer().queryNames(NIO_MXBEAN_PATTERN, null)) {
				nioObjectNames.put(on.getKeyProperty("name"), on);
			}
		}
		for(Map.Entry<String, ObjectName> entry: nioObjectNames.entrySet()) {
			nioAttrValues.clear();
			try {
				for(Attribute attr: ManagementFactory.getPlatformMBeanServer().getAttributes(entry.getValue(), NIO_ATTR_NAMES).asList()) {
					nioAttrValues.put(attr.getName(), (Long)attr.getValue());
				}
				Long value = null;
				
				if((value=nioAttrValues.get("Count")) != null) {
					tracer.traceGauge(value, "Count", "platform=JVM", "category=NIOBufferPools", "type=" + entry.getKey());
				}
				if((value=nioAttrValues.get("MemoryUsed")) != null) {
					tracer.traceGauge(value, "MemoryUsed", "platform=JVM", "category=NIOBufferPools", "type=" + entry.getKey());
				}
				if((value=nioAttrValues.get("TotalCapacity")) != null) {
					tracer.traceGauge(value, "TotalCapacity", "platform=JVM", "category=NIOBufferPools", "type=" + entry.getKey());
				}				
			} catch (Exception e) {
			}
		}
		
	}
	
	/**
	 * Collects GC activity data
	 */
	protected void collectGc() {
		for(GarbageCollectorMXBean gc: gcMXBeans) {
			long currentTime = SystemClock.time();
			String name = gc.getName();
			tracer.traceDeltaGauge(gc.getCollectionCount(), "CollectionCount", "platform=JVM", "category=GarbageCollection", "collector=" + name.replace(" ", ""));
			long time = gc.getCollectionTime();
			tracer.traceDeltaGauge(time, "CollectionTime", "platform=JVM", "category=GarbageCollection", "collector=" + name.replace(" ", ""));
			Long prior = lastGCTime.put(name, time);
			if(prior!=null) {
				long gcTime = time-prior;
				long elapsedTime = lastGCCollectTime.put(name, currentTime)*PROCESSOR_COUNT;
				tracer.traceGauge(percent(elapsedTime, gcTime), "PercentTimeInCollect", "platform=JVM", "category=GarbageCollection", "collector=" + name.replace(" ", ""));
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
		tracer.traceGauge(tc, "ThreadCount", "platform=JVM", "category=Threads");
		tracer.traceGauge(dtc, "DaemonThreadCount", "platform=JVM", "category=Threads");
		tracer.traceGauge(ndtc, "NonDaemonThreadCount", "platform=JVM", "category=Threads");
		tracer.traceGauge(threadMXBean.getPeakThreadCount(), "PeakThreadCount", "platform=JVM", "category=Threads");
		for(Map.Entry<Thread.State, AtomicInteger> entry: getThreadStates().entrySet()) {
			tracer.traceGauge(entry.getValue().intValue(), entry.getKey().name(), "platform=JVM", "category=Threads", "type=State");
		}
		if(resetLoop) threadMXBean.resetPeakThreadCount();
		long[] deadlocked = threadMXBean.findMonitorDeadlockedThreads();
		tracer.traceGauge(deadlocked==null ? 0 : deadlocked.length, "DeadlockedThreadCount", "platform=JVM", "category=Threads");
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
			tracer.traceString(dlockInfo, "DeadlockedThreadInfo", "platform=JVM", "category=Threads");
		}		
	}
	
	/**
	 * Collects the number of threads in each thread state
	 * @return an EnumMap with Thread states as the key and the number of threads in that state as the value
	 */
	public EnumMap<Thread.State, AtomicInteger> getThreadStates() {
		EnumMap<Thread.State, AtomicInteger> map = new EnumMap<State, AtomicInteger>(Thread.State.class);
		for(Thread.State ts: Thread.State.values()) {
			map.put(ts, new AtomicInteger(0));
		}
		for(ThreadInfo ti : threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds())) {
			map.get(ti.getThreadState()).incrementAndGet();
		}
		return map;
	}
	
	
	/**
	 * Collects compilation time 
	 */
	protected void collectCompilation() {
		tracer.traceDeltaGauge(compilationMXBean.getTotalCompilationTime(), "CompilationTime", "platform=JVM", "category=Compilation", "compiler=" + compilationMXBean.getName().replace(" ", ""));
	}
	
	/**
	 * Collects the static memory pool data
	 */
	protected void collectInitialMemoryPools() {
		for(MemoryPoolMXBean pool: memoryPoolMXBeans) {
			MemoryUsage usage = pool.getUsage();
			tracer.traceGauge(usage.getInit(), "Initial", "platform=JVM", "category=MemoryPools", "type=" + pool.getType().name(), "pool=" + pool.getName().replace(" ", ""));
			tracer.traceGauge(usage.getMax(), "Maximum", "platform=JVM", "category=MemoryPools", "type=" + pool.getType().name(), "pool=" + pool.getName().replace(" ", ""));
			maxPoolSize.put(pool.getName(), usage.getMax());
		}
	}
	
	/**
	 * Collects the dynamic memory pool data
	 */
	protected void collectMemoryPools() {
		for(MemoryPoolMXBean pool: memoryPoolMXBeans) {
			MemoryUsage usage = pool.getUsage();
			tracer.traceGauge(usage.getCommitted(), "Committed", "platform=JVM", "category=MemoryPools", "type=" + pool.getType().name(), "pool=" + pool.getName().replace(" ", ""));
			tracer.traceGauge(usage.getUsed(), "Used", "platform=JVM", "category=MemoryPools", "type=" + pool.getType().name(), "pool=" + pool.getName().replace(" ", ""));
			tracer.traceGauge(percent(usage.getCommitted(), usage.getUsed()), "PercentUsed", "platform=JVM", "category=MemoryPools", "type=" + pool.getType().name(), "pool=" + pool.getName().replace(" ", ""));
			tracer.traceGauge(percent(maxPoolSize.get(pool.getName()), usage.getUsed()), "PercentCapacity", "platform=JVM", "category=MemoryPools", "type=" + pool.getType().name(), "pool=" + pool.getName().replace(" ", ""));
		}
	}
	
	/**
	 * Collects the static memory data
	 */
	protected void collectInitialMemory() {
			MemoryUsage usage = memoryMXBean.getHeapMemoryUsage();
			tracer.traceGauge(usage.getInit(), "Initial", "platform=JVM", "category=Memory", "type=Heap");
			tracer.traceGauge(usage.getMax(), "Maximum", "platform=JVM", "category=Memory", "type=Heap");
			maxPoolSize.put("Heap", usage.getMax());
			usage = memoryMXBean.getNonHeapMemoryUsage();
			tracer.traceGauge(usage.getInit(), "Initial", "platform=JVM", "category=Memory", "type=NonHeap");
			tracer.traceGauge(usage.getMax(), "Maximum", "platform=JVM", "category=Memory", "type=NonHeap");
			maxPoolSize.put("NonHeap", usage.getMax());
	}
	
	/**
	 * Collects the dynamic memory data
	 */
	protected void collectMemory() {
		MemoryUsage usage = memoryMXBean.getHeapMemoryUsage();
		tracer.traceGauge(memoryMXBean.getObjectPendingFinalizationCount(), "PendingFinalization", "platform=JVM", "category=Memory");
		tracer.traceGauge(usage.getCommitted(), "Committed", "platform=JVM", "category=Memory", "type=Heap");
		tracer.traceGauge(usage.getUsed(), "Used", "platform=JVM", "category=Memory", "type=Heap");
		tracer.traceGauge(percent(usage.getCommitted(), usage.getUsed()), "PercentUsed", "platform=JVM", "category=Memory", "type=Heap");
		tracer.traceGauge(percent(maxPoolSize.get("Heap"), usage.getUsed()), "PercentCapacity", "platform=JVM", "category=Memory", "type=Heap");
		
		usage = memoryMXBean.getNonHeapMemoryUsage();
		tracer.traceGauge(usage.getCommitted(), "Committed", "platform=JVM", "category=Memory", "type=NonHeap");
		tracer.traceGauge(usage.getUsed(), "Used", "platform=JVM", "category=Memory", "type=NonHeap");
		tracer.traceGauge(percent(usage.getCommitted(), usage.getUsed()), "PercentUsed", "platform=JVM", "category=Memory", "type=NonHeap");
		tracer.traceGauge(percent(maxPoolSize.get("NonHeap"), usage.getUsed()), "PercentCapacity", "platform=JVM", "category=Memory", "type=NonHeap");
	}
	
	/**
	 * Collects class loading stats
	 * FIXME: TotalLoadedClasses needs traceLongLast
	 */
	protected void collectClassLoading() {
		tracer.traceGauge(classLoadingMXBean.getTotalLoadedClassCount(), "TotalLoadedClasses", "platform=JVM", "category=ClassLoading");
		tracer.traceDeltaGauge(classLoadingMXBean.getTotalLoadedClassCount(), "ClassLoadRate", "platform=JVM", "category=ClassLoading");
		tracer.traceGauge(classLoadingMXBean.getUnloadedClassCount(), "UnloadedClassCount", "platform=JVM", "category=ClassLoading");
		tracer.traceDeltaGauge(classLoadingMXBean.getUnloadedClassCount(), "ClassUnloadRate", "platform=JVM", "category=ClassLoading");
		tracer.traceGauge(classLoadingMXBean.getLoadedClassCount(), "CurrentClassCount", "platform=JVM", "category=ClassLoading");				
	}
	
	
	/**
	 * Collects static runtime info 
	 */
	protected void collectInitialRuntime() {
		tracer.traceGauge(runtimeMXBean.getStartTime(), "StartTime", "platform=JVM", "category=Runtime");
		tracer.traceString(runtimeMXBean.getVmName(), "VmName", "platform=JVM", "category=Runtime");
		tracer.traceString(runtimeMXBean.getVmVendor(), "VmVendor", "platform=JVM", "category=Runtime");
		tracer.traceString(runtimeMXBean.getVmVersion(), "VmVersion", "platform=JVM", "category=Runtime");
		tracer.traceString(runtimeMXBean.getInputArguments().toString(), "InputArguments", "platform=JVM", "category=Runtime");
		tracer.traceString(runtimeMXBean.getName(), "RuntimeName", "platform=JVM", "category=Runtime");
		initialRuntimeCollected = true;
	}
	
	/**
	 * Collects dynamic runtime info 
	 * FIXME: Uptime needs long last
	 */
	protected void collectRuntime() {
		tracer.traceGauge(runtimeMXBean.getUptime(), "UpTime", "platform=JVM", "category=Runtime");
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
