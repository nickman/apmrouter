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
package org.helios.apmrouter.monitor.nativex;

import org.helios.apmrouter.jmx.ConfigurationHelper;
import org.helios.apmrouter.monitor.AbstractMonitor;
import org.helios.apmrouter.nativex.APMSigar;
import org.helios.apmrouter.util.SystemClock;
import org.hyperic.sigar.*;

import java.net.NetworkInterface;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * <p>Title: NativeMonitor</p>
 * <p>Description: Monitor implementation to monitor native OS resources and performance</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.monitor.nativex.NativeMonitor</code></p>
 */

public class NativeMonitor extends AbstractMonitor {
	/** The wrapped sigar instance */
	protected final APMSigar hsigar = APMSigar.getInstance();
	/** The native sigar instance */
	protected final Sigar sigar = hsigar.getSigar();
	/** A map maintaining the last usage and timestamp for each file system */
	protected final Map<String, long[]> fileSystemState = new HashMap<String, long[]>();
	
	/** The number of collection sweeps before the file systems are rescanned */
	protected int fsRescanCollectionSweep = 10;
	/** The number of collection sweeps before a file system time-to-full is computed */
	protected int fsTimeToFullCollectionSweep = 10;
	/** Indicates if static os meta-data should be traced during rescans */
	protected boolean traceMeta = false;
	/** Indicates if detailed localStats for all cpus should be traced */
	protected boolean traceAllCpus = false;
	

	/** Indicates if this is windows */
	public static final boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows") ;

	/** The NIC name tag foramat */
	public static final String NIC_NAME = "if=%s";			
	/** The CPU resource tag */
	public static final String NIC_RESOURCE = "resource=if";
	/** The swap resource tag */
	public static final String SWAP_RESOURCE = "resource=swap";
	/** The TCP resource tag */
	public static final String TCP_RESOURCE = "resource=tcp";
	/** The Net stat resource tag */
	public static final String NET_RESOURCE = "resource=netstat";
	/** The process stat resource tag */
	public static final String PS_RESOURCE = "resource=ps";
	
	/** The JVM process resource tag */
	public static final String JVM_PROCESS_RESOURCE = "resource=jvm";
	/** The CPU name tag foramat */
	public static final String CPU_NAME = "cpu=%s";			
	/** The CPU resource tag */
	public static final String CPU_RESOURCE = "resource=cpu";
	/** The System memory resource tag */
	public static final String SYSMEM_RESOURCE = "resource=sysmem";
	
	/** The Filesystem resource tag */
	public static final String FS_RESOURCE = "resource=filesystem";
	/** The Filesystem name tag foramat */
	public static final String FS_NAME = "fsname=%s";		
	/** The OS patform tag */
	public static final String PLAT = "platform=os";
	/** The tracing tag for OS static meta-data */
	public static final String META_TAG = "info=meta";
	
	/** The size of the long array in fs-state */
	public static final int FS_STATE_SIZE = 6;
	/** The fs-state long arr index for the monitor flag */
	public static final int FS_STATE_FLAG = 0;
	/** The fs-state long arr index for the last timestamp collected */
	public static final int FS_STATE_TS = 1;
	/** The fs-state long arr index for the kb-used collected in the last period */
	public static final int FS_STATE_USED = 2;
	/** The fs-state long arr index for the kb-total collected in the last period */
	public static final int FS_STATE_TOTAL = 3;
	/** The fs-state long arr index for the time until full for the last period */
	public static final int FS_STATE_TTF = 4;
	/** The fs-state long arr index for the number of consecutive times there has been no increment in fs usage */
	public static final int FS_STATE_NOINCR = 5;
	
	/** The property name for configuring the number of collection sweeps before the file systems are rescanned  */
	public static final String RESCAN_PROP = "monitor.nativex.fs.rescan";
	/** The default count of collection sweeps before the file systems are rescanned  */
	public static final int DEFAULT_RESCAN = 10;
	/** The property name for configuring the number of collection sweeps before a file system time-to-full is computed   */
	public static final String TTF_PROP = "monitor.nativex.fs.ttf";
	/** The default count of collection sweeps before a file system time-to-full is computed  */
	public static final int DEFAULT_TTF = 10;
	/** The property name for configuring if static os data (e.g. file-system meta etc.) should be traced when systems are rescanned  */
	public static final String OS_META_PROP = "monitor.nativex.tracemeta";
	/** The default configuration that specifies if static os data should be traced when systems are rescanned */
	public static final boolean DEFAULT_OS_META = false;
	/** The property name for configuring if detailed localStats on each cpu should be traced, or just the combined */
	public static final String CPU_ALL_PROP = "monitor.nativex.cpu.traceall";
	/** The default configuration for detailed localStats on each cpu should be traced */
	public static final boolean DEFAULT_CPU_ALL = false;
	
	/**
	 * Creates a new NativeMonitor
	 */
	public NativeMonitor() {
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.monitor.AbstractMonitor#setProperties(java.util.Properties)
	 */
	@Override
	public void setProperties(Properties p) {
		super.setProperties(p);
		fsRescanCollectionSweep = ConfigurationHelper.getIntSystemThenEnvProperty(RESCAN_PROP, DEFAULT_RESCAN, p);
		fsTimeToFullCollectionSweep = ConfigurationHelper.getIntSystemThenEnvProperty(TTF_PROP, DEFAULT_TTF, p);
		traceMeta = ConfigurationHelper.getBooleanSystemThenEnvProperty(OS_META_PROP, DEFAULT_OS_META, p);
		traceAllCpus = ConfigurationHelper.getBooleanSystemThenEnvProperty(CPU_ALL_PROP, DEFAULT_CPU_ALL, p);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.monitor.AbstractMonitor#doCollect(long)
	 */
	@Override
	protected void doCollect(long collectionSweep) {
		traceCpus();
		traceFileSystemUsage();
		traceNics();
		traceSystemMem();
		traceSwap();
		traceTCP();
		traceNetstat();
		traceMe();
		traceProcessStats();
	}
	
	/**
	 * Traces the states of system processes
	 */
	protected void traceProcessStats() {
		ProcStat ps = hsigar.getProcStat();
		tracer.traceGauge(ps.getIdle(), "Idle", PLAT, PS_RESOURCE);
		tracer.traceGauge(ps.getRunning(), "Running", PLAT, PS_RESOURCE);
		tracer.traceGauge(ps.getSleeping(), "Sleeping", PLAT, PS_RESOURCE);
		tracer.traceGauge(ps.getStopped(), "Stopped", PLAT, PS_RESOURCE);
		tracer.traceGauge(ps.getThreads(), "Threads", PLAT, PS_RESOURCE);
		tracer.traceGauge(ps.getTotal(), "Total", PLAT, PS_RESOURCE);
		tracer.traceGauge(ps.getZombie(), "Zombie", PLAT, PS_RESOURCE);
	}
	
	/**
	 * Traces network localStats
	 */
	protected void traceNetstat() {
		NetStat net = hsigar.getNetStat();
		tracer.traceDeltaGauge(net.getAllInboundTotal(), "Inbound", PLAT, NET_RESOURCE);
		tracer.traceDeltaGauge(net.getAllOutboundTotal(), "Outbound", PLAT, NET_RESOURCE);
		tracer.traceCounter(net.getTcpBound(), "TcpBound", PLAT, NET_RESOURCE);
		tracer.traceCounter(net.getTcpClose(), "TcpClose", PLAT, NET_RESOURCE);
		tracer.traceCounter(net.getTcpCloseWait(), "TcpCloseWait", PLAT, NET_RESOURCE);
		tracer.traceCounter(net.getTcpClosing(), "TcpClosing", PLAT, NET_RESOURCE);
		tracer.traceCounter(net.getTcpEstablished(), "TcpEstablished", PLAT, NET_RESOURCE);
		tracer.traceCounter(net.getTcpFinWait1(), "TcpFinWait1", PLAT, NET_RESOURCE);
		tracer.traceCounter(net.getTcpFinWait2(), "TcpFinWait2", PLAT, NET_RESOURCE);
		tracer.traceCounter(net.getTcpIdle(), "TcpIdle", PLAT, NET_RESOURCE);
		tracer.traceCounter(net.getTcpInboundTotal(), "TcpInbound", PLAT, NET_RESOURCE);
		tracer.traceCounter(net.getTcpOutboundTotal(), "TcpOutbound", PLAT, NET_RESOURCE);
		tracer.traceCounter(net.getTcpLastAck(), "TcpLastAck", PLAT, NET_RESOURCE);
		tracer.traceCounter(net.getTcpListen(), "TcpListen", PLAT, NET_RESOURCE);
		tracer.traceCounter(net.getTcpSynRecv(), "TcpSynRecv", PLAT, NET_RESOURCE);
		tracer.traceCounter(net.getTcpSynSent(), "TcpSynSent", PLAT, NET_RESOURCE);
		tracer.traceCounter(net.getTcpTimeWait(), "TcpTimeWait", PLAT, NET_RESOURCE);		
	}
	
	/**
	 * Traces metrics about this process.
	 */
	protected void traceMe() {
		ProcCpu procCpu = hsigar.getProcCpu(hsigar.pid);
		tracer.traceGauge(procCpu.getSys(), "System", "platform=JVM", "category=cpu");
		tracer.traceGauge(procCpu.getTotal(), "Total", "platform=JVM", "category=cpu");
		tracer.traceGauge(procCpu.getUser(), "User", "platform=JVM", "category=cpu");
		tracer.traceGauge( dbl2longPerc(procCpu.getPercent()), "PercentUsage", "platform=JVM", "category=cpu");
		ProcMem pmem = hsigar.getProcMem(hsigar.pid);
		tracer.traceGauge(pmem.getMajorFaults(), "MajorFaults", "platform=JVM", "category=processMemory");
		tracer.traceGauge(pmem.getMinorFaults(), "MinorFaults", "platform=JVM", "category=processMemory");
		tracer.traceGauge(pmem.getPageFaults(), "PageFaults", "platform=JVM", "category=processMemory");
		tracer.traceGauge(pmem.getResident(), "Resident", "platform=JVM", "category=processMemory");
		if(!isWindows) tracer.traceGauge(pmem.getShare(), "Shared", "platform=JVM", "category=processMemory");
		tracer.traceGauge(pmem.getSize(), "Size", "platform=JVM", "category=processMemory");
		tracer.traceGauge(hsigar.getProcFd(hsigar.pid).getTotal(), "OpenFileDescriptors", "platform=JVM", "category=fd");
		
	}
	
	/**
	 * Traces tcp metris
	 */
	protected void traceTCP() {
		Tcp tcp = hsigar.getTcp();
		tracer.traceDeltaGauge(tcp.getActiveOpens(), "Opens", PLAT, TCP_RESOURCE);
		tracer.traceDeltaGauge(tcp.getAttemptFails(), "Fails", PLAT, TCP_RESOURCE);
		tracer.traceGauge(tcp.getCurrEstab(), "Established", PLAT, TCP_RESOURCE);
		tracer.traceDeltaGauge(tcp.getEstabResets(), "Resets", PLAT, TCP_RESOURCE);
		tracer.traceDeltaGauge(tcp.getInErrs(), "InErrors", PLAT, TCP_RESOURCE);
		tracer.traceDeltaGauge(tcp.getInSegs(), "InSegs", PLAT, TCP_RESOURCE);
		tracer.traceDeltaGauge(tcp.getOutRsts(), "OutResets", PLAT, TCP_RESOURCE);
		tracer.traceDeltaGauge(tcp.getOutSegs(), "OutSegs", PLAT, TCP_RESOURCE);
		tracer.traceDeltaGauge(tcp.getPassiveOpens(), "PassiveOpens", PLAT, TCP_RESOURCE);
		tracer.traceDeltaGauge(tcp.getRetransSegs(), "RetransSegs", PLAT, TCP_RESOURCE);		
	}
	
	/**
	 * Traces metrics on swap space usage
	 */
	protected void traceSwap() {
		Swap swap = hsigar.getSwap();
		long total = swap.getTotal();
		if((collectionSweep==0 || collectionSweep%fsRescanCollectionSweep==0)) {
			tracer.traceCounter(total, "Total", PLAT, SWAP_RESOURCE);			
		}
		long free = swap.getFree();
		long used = swap.getUsed();
		tracer.traceCounter(free, "Free", PLAT, SWAP_RESOURCE);
		tracer.traceCounter(used, "Used", PLAT, SWAP_RESOURCE);
		tracer.traceCounter(percent(total,free), "FreePercent", PLAT, SWAP_RESOURCE);
		tracer.traceCounter(percent(total,used), "UsedPercent", PLAT, SWAP_RESOURCE);
		tracer.traceCounter(swap.getPageIn(), "PageIn", PLAT, SWAP_RESOURCE);
		tracer.traceCounter(swap.getPageOut(), "PageOut", PLAT, SWAP_RESOURCE);
	}
	
	private static long percent(double total, double part) {
		if(total==0 || part==0) return 0;
		double d = part/total*100;
		return (long)d;
	}
	
	/**
	 * Traces metrics on system memory
	 */
	protected void traceSystemMem() {
		Mem mem = hsigar.getMem();
		if((collectionSweep==0 || collectionSweep%fsRescanCollectionSweep==0)) {
			tracer.traceCounter(mem.getRam(), "TotalMemMB", PLAT, SYSMEM_RESOURCE);
		}
		tracer.traceCounter(mem.getActualFree(), "ActualFree", PLAT, SYSMEM_RESOURCE);
		tracer.traceCounter(mem.getActualUsed(), "ActualUsed", PLAT, SYSMEM_RESOURCE);
		tracer.traceCounter(mem.getFree(), "Free", PLAT, SYSMEM_RESOURCE);
		tracer.traceCounter(mem.getUsed(), "Used", PLAT, SYSMEM_RESOURCE);
		
		tracer.traceCounter(dbl2longPerc(mem.getFreePercent()), "FreePercent", PLAT, SYSMEM_RESOURCE);
		tracer.traceCounter(dbl2longPerc(mem.getUsedPercent()), "UsedPercent", PLAT, SYSMEM_RESOURCE);
	}
	
	/**
	 * Traces metrics on NIC trafic and meta
	 */
	protected void traceNics() {
		StringBuilder b = new StringBuilder("\n\t=================================\n\tDiscovered NICs\n\t=================================");
		for(String nic: hsigar.getNetInterfaceList()) {
			NetworkInterface jnic = null;
			try { 
				jnic = NetworkInterface.getByName(("lo0".equals(nic) ? "lo" : nic)); // Windows reports "lo" where sigar sees "lo0" 
				if(jnic==null || !jnic.isUp()) {
					continue;
				}
			} catch (Exception e) { continue; }
			
			if(collectionSweep==0) {
				NetInterfaceConfig config = hsigar.getNetInterfaceConfig(nic);
				b.append("\n\t").append(nic).append("/").append(config.getName()).append("\t(").append(config.getDescription()).append(")");
			}
			NetInterfaceStat nicStat = hsigar.getNetInterfaceStat(nic);
			tracer.traceDeltaGauge(nicStat.getRxBytes(), "RXBytes", PLAT, NIC_RESOURCE, String.format(NIC_NAME, nic));
			tracer.traceDeltaGauge(nicStat.getTxBytes(), "TXBytes", PLAT, NIC_RESOURCE, String.format(NIC_NAME, nic));
			tracer.traceDeltaGauge(nicStat.getRxDropped(), "RXDropped", PLAT, NIC_RESOURCE, String.format(NIC_NAME, nic));
			tracer.traceDeltaGauge(nicStat.getTxDropped(), "TXDropped", PLAT, NIC_RESOURCE, String.format(NIC_NAME, nic));
			tracer.traceDeltaGauge(nicStat.getRxErrors(), "RXErrors", PLAT, NIC_RESOURCE, String.format(NIC_NAME, nic));
			tracer.traceDeltaGauge(nicStat.getTxErrors(), "TXErrors", PLAT, NIC_RESOURCE, String.format(NIC_NAME, nic));
			tracer.traceDeltaGauge(nicStat.getRxPackets(), "RXPackets", PLAT, NIC_RESOURCE, String.format(NIC_NAME, nic));
			tracer.traceDeltaGauge(nicStat.getTxPackets(), "TXPackets", PLAT, NIC_RESOURCE, String.format(NIC_NAME, nic));
			tracer.traceDeltaGauge(nicStat.getRxOverruns(), "RXOverruns", PLAT, NIC_RESOURCE, String.format(NIC_NAME, nic));
			tracer.traceDeltaGauge(nicStat.getTxOverruns(), "TXOverruns", PLAT, NIC_RESOURCE, String.format(NIC_NAME, nic));
			
			tracer.traceDeltaGauge(nicStat.getTxCollisions(), "TXCollisions", PLAT, NIC_RESOURCE, String.format(NIC_NAME, nic));
			tracer.traceDeltaGauge(nicStat.getTxCarrier(), "TxCarrier", PLAT, NIC_RESOURCE, String.format(NIC_NAME, nic));
			
			tracer.traceDeltaGauge(nicStat.getSpeed(), "Speed", PLAT, NIC_RESOURCE, String.format(NIC_NAME, nic));
			if((collectionSweep==0 || collectionSweep%fsRescanCollectionSweep==0) && traceMeta) {
				NetInterfaceConfig config = hsigar.getNetInterfaceConfig(nic);
				
				tracer.traceString(config.getAddress(), "Address", PLAT, NIC_RESOURCE, String.format(NIC_NAME, nic), META_TAG);
				tracer.traceString(config.getBroadcast(), "Broadcast", PLAT, NIC_RESOURCE, String.format(NIC_NAME, nic), META_TAG);
				tracer.traceString(jnic.getDisplayName(), "DisplayName", PLAT, NIC_RESOURCE, String.format(NIC_NAME, nic), META_TAG);
				tracer.traceString(config.getDescription(), "Description", PLAT, NIC_RESOURCE, String.format(NIC_NAME, nic), META_TAG);
				tracer.traceString(config.getDestination(), "Destination", PLAT, NIC_RESOURCE, String.format(NIC_NAME, nic), META_TAG);
				tracer.traceString(config.getHwaddr(), "MAC", PLAT, NIC_RESOURCE, String.format(NIC_NAME, nic), META_TAG);
				tracer.traceString(config.getName(), "Name", PLAT, NIC_RESOURCE, String.format(NIC_NAME, nic), META_TAG);				
				tracer.traceString(config.getNetmask(), "Netmask", PLAT, NIC_RESOURCE, String.format(NIC_NAME, nic), META_TAG);
				tracer.traceString(config.getType(), "Type", PLAT, NIC_RESOURCE, String.format(NIC_NAME, nic), META_TAG);
				tracer.traceCounter(config.getMetric(), "Metric", PLAT, NIC_RESOURCE, String.format(NIC_NAME, nic), META_TAG);
				tracer.traceCounter(config.getMtu(), "Mtu", PLAT, NIC_RESOURCE, String.format(NIC_NAME, nic), META_TAG);
				tracer.traceCounter(config.getFlags(), "Flags", PLAT, NIC_RESOURCE, String.format(NIC_NAME, nic), META_TAG);
			}
		}
		if(collectionSweep==0) {
			b.append("\n");
			log(b);
		}

	}
	
	/**
	 * Collects individual and aggregate CPU percentage utilization localStats.
	 */
	protected void traceCpus() {
		int cid = 0;
		if(traceAllCpus) {
			for(CpuPerc c : hsigar.getCpuPercList()) {
				traceCpuPerc(c, String.format(CPU_NAME, cid));
				cid++;
			}
		}
		traceCpuPerc(hsigar.getCpuPerc(), String.format(CPU_NAME, "all")); 
		if((collectionSweep==0 || collectionSweep%fsRescanCollectionSweep==0) && traceMeta && traceAllCpus) {
			CpuInfo[] infos = hsigar.getCpuInfoList();
			tracer.traceCounter(infos[0].getTotalCores(),"TotalCores", PLAT, CPU_RESOURCE, META_TAG);
			tracer.traceCounter(infos[0].getTotalCores(),"TotalSockets", PLAT, CPU_RESOURCE, META_TAG);
			for(int i = 0; i < infos.length; i++) {
				tracer.traceString(infos[i].getModel(),"Model", PLAT, CPU_RESOURCE, META_TAG, String.format(CPU_NAME, i));
				tracer.traceString(infos[i].getVendor(),"Vendor", PLAT, CPU_RESOURCE, META_TAG, String.format(CPU_NAME, i));
				tracer.traceCounter(infos[i].getCacheSize(),"CacheSize", PLAT, CPU_RESOURCE, META_TAG, String.format(CPU_NAME, i));
				tracer.traceCounter(infos[i].getCoresPerSocket(),"CoresPerSocket", PLAT, CPU_RESOURCE, META_TAG, String.format(CPU_NAME, i));
				tracer.traceCounter(infos[i].getMhz(),"Mhz", PLAT, CPU_RESOURCE, META_TAG, String.format(CPU_NAME, i));
				tracer.traceCounter(infos[i].getCoresPerSocket(),"CoresPerSocket", PLAT, CPU_RESOURCE, META_TAG, String.format(CPU_NAME, i));
			}
		}
	}
	
	/**
	 * Traces the passed PCU percentage localStats
	 * @param c The CPU percentage localStats
	 * @param cpuName The cpu name
	 */
	private void traceCpuPerc(CpuPerc c, String cpuName) {
		tracer.traceGauge(dbl2longPerc(c.getCombined()), "Combined", PLAT, CPU_RESOURCE, cpuName);
		tracer.traceGauge(dbl2longPerc(c.getIdle()), "Idle", PLAT, CPU_RESOURCE, cpuName);
		tracer.traceGauge(dbl2longPerc(c.getIrq()), "Irq", PLAT, CPU_RESOURCE, cpuName);
		tracer.traceGauge(dbl2longPerc(c.getNice()), "Nice", PLAT, CPU_RESOURCE, cpuName);
		tracer.traceGauge(dbl2longPerc(c.getSoftIrq()), "SoftIrq", PLAT, CPU_RESOURCE, cpuName);
		tracer.traceGauge(dbl2longPerc(c.getStolen()), "Stolen", PLAT, CPU_RESOURCE, cpuName);
		tracer.traceGauge(dbl2longPerc(c.getSys()), "Sys", PLAT, CPU_RESOURCE, cpuName);
		tracer.traceGauge(dbl2longPerc(c.getUser()), "User", PLAT, CPU_RESOURCE, cpuName);
		tracer.traceGauge(dbl2longPerc(c.getWait()), "Wait", PLAT, CPU_RESOURCE, cpuName);					
	}
	

	
	/**
	 * Traces usage localStats about the local file systems
	 */
	protected void traceFileSystemUsage() {		
		StringBuilder b = new StringBuilder("\n\t=================================\n\tDiscovered File Systems\n\t=================================");
		if(collectionSweep==0 || collectionSweep%fsRescanCollectionSweep==0) {
			refreshCollectedFileSystems();
		}
		if(collectionSweep==0) {
			for(String fsName:fileSystemState.keySet()) {
				b.append("\n\t").append(fsName);
			}		
			b.append("\n");
			log(b);
		}

		for(Map.Entry<String, long[]> entry: fileSystemState.entrySet()) {
			final String dirName = entry.getKey();
			final long[] fsState = entry.getValue();
			final FileSystemUsage fsu = hsigar.getFileSystemUsageOrNull(dirName);
			if(fsu==null) {
				String msg = "Detected unmounted filesystem [" + dirName + "]";
				log(msg);
				tracer.traceString(msg, "FileSystemEvents", PLAT, FS_RESOURCE);
				continue;
			}
			
			long used = fsu.getUsed();
			long total = fsu.getTotal();
			long now = SystemClock.time();
			if(collectionSweep==0 || collectionSweep%fsTimeToFullCollectionSweep==0) {
				if(fsState[FS_STATE_TS]!=0L) {
					long secondsUntilFull = timeUntilFull(total, used, now, fsState);
					tracer.traceGauge(secondsUntilFull, "SecondsToFull", PLAT, FS_RESOURCE, String.format(FS_NAME, dirName.replace(":\\", "")));	
				}
				fsState[FS_STATE_USED] = used;
				fsState[FS_STATE_TOTAL] = total;
				fsState[FS_STATE_TS] = now;
			}
			tracer.traceGauge(fsu.getAvail(), "AvailableKb", PLAT, FS_RESOURCE, String.format(FS_NAME, dirName.replace(":\\", "")));
			tracer.traceDeltaGauge(fsu.getDiskReadBytes(), "DiskReadBytes", PLAT, FS_RESOURCE, String.format(FS_NAME, dirName.replace(":\\", "")));
			tracer.traceDeltaGauge(fsu.getDiskWriteBytes(), "DiskWriteBytes", PLAT, FS_RESOURCE, String.format(FS_NAME, dirName.replace(":\\", "")));
			tracer.traceDeltaGauge(fsu.getDiskReads(), "DiskReads", PLAT, FS_RESOURCE, String.format(FS_NAME, dirName.replace(":\\", "")));
			tracer.traceDeltaGauge(fsu.getDiskWrites(), "DiskWrites", PLAT, FS_RESOURCE, String.format(FS_NAME, dirName.replace(":\\", "")));
			
			tracer.traceGauge(fsu.getFree(), "FreeKb", PLAT, FS_RESOURCE, String.format(FS_NAME, dirName.replace(":\\", "")));
			tracer.traceGauge(fsu.getTotal(), "TotalKb", PLAT, FS_RESOURCE, String.format(FS_NAME, dirName.replace(":\\", "")));
			tracer.traceGauge(fsu.getUsed(), "UsedKb", PLAT, FS_RESOURCE, String.format(FS_NAME, dirName.replace(":\\", "")));
			tracer.traceGauge((long)fsu.getDiskQueue(), "DiskQueue", PLAT, FS_RESOURCE, String.format(FS_NAME, dirName.replace(":\\", "")));
			// No impl. for Win
			tracer.traceGauge((long)fsu.getDiskServiceTime(), "DiskServiceTime", PLAT, FS_RESOURCE, String.format(FS_NAME, dirName.replace(":\\", "")));
			
			
		}
//		for(FileSystem fs: hsigar.getFileSystemList()) {
//			log("[FS]:DevName:" + fs.getDevName()
//				+ "\n\tDirName:" + fs.getDirName()
//				+ "\n\tOptions:" + fs.getOptions()
//				+ "\n\tSysTypeName:" + fs.getSysTypeName()
//				+ "\n\tTypeName:" + fs.getTypeName()				
//			);
//			FileSystemUsage fsu = hsigar.getFileSystemUsageOrNull(fs.getDirName());
//			if(fsu==null) log("No FSU for [" + fs.getDirName() + "]");
//			else {
//				log("[FSU]:DirName:" + fs.getDirName()
//					+ "\n\tAvail KB:" + fsu.getAvail()
//					+ "\n\tFree KB:" + fsu.getFree()
//					+ "\n\tTotal KB:" + fsu.getTotal()
//					+ "\n\tUsed KB:" + fsu.getUsed()
//					+ "\n\t%Used KB:" + dbl2longPerc(fsu.getUsePercent())
//					+ "\n\tDiskQueue:" + fsu.getDiskQueue()
//					+ "\n\tDiskServiceTime:" + fsu.getDiskServiceTime()
//					+ "\n\tDiskReadBytes:" + fsu.getDiskReadBytes()
//					+ "\n\tDiskReads:" + fsu.getDiskReads()
//					+ "\n\tDiskWriteBytes:" + fsu.getDiskWriteBytes()
//					+ "\n\tDiskWrites:" + fsu.getDiskWrites()
//					
//						
//				);
//			}
//		}
	}
	
	/**
	 * Calculates the approximate number of seconds until a file system is full based on the incremental usage in the last period.
	 * If the file system is already full, returns zero. If there has been no usage, returns {@link Long#MAX_VALUE}  (Infinity)
	 * @param total The file system's total capacity
	 * @param used The file system usage in this collection period
	 * @param now The current timestamp
	 * @param fsState The collected localStats from the prior period
	 * @return the approximate number of seconds until a file system is full 
	 */
	protected long timeUntilFull(long total, long used, long now, long[] fsState) {		
		long increment = used - fsState[FS_STATE_USED];
		if(increment<1) {
			fsState[FS_STATE_NOINCR]++;
			if(fsState[FS_STATE_TTF]==Long.MIN_VALUE) {
				return Long.MAX_VALUE;
			}
			return fsState[FS_STATE_TTF];						
		}
		fsState[FS_STATE_NOINCR]=0;
		
		
		long elapsed = now - fsState[FS_STATE_TS];
		long free = total-used;
		if(free==0) return 0;
		long periodsUntilFull = free/increment;
		return TimeUnit.SECONDS.convert(periodsUntilFull * elapsed, TimeUnit.MILLISECONDS);		
	}
	
	
	/**
	 * Updates the file system state, adding any new file systems and removing any unmounted file systems.
	 */
	protected void refreshCollectedFileSystems() {
		// mark long[0] to 0 before refresh
		// during collection, set long[0] to 1
		// when done, any dir left with long[0]==0 has been unmounted.
		for(Map.Entry<String, long[]> entry: fileSystemState.entrySet()) {
			entry.getValue()[FS_STATE_FLAG] = 0;
		}
		for(FileSystem fs: hsigar.getFileSystemList()) {
			String dirName = fs.getDirName();
			FileSystemUsage fsu = hsigar.getFileSystemUsageOrNull(dirName);
			if(fsu==null) continue;
			if(fsu.getDiskReadBytes()==-1 && fsu.getDiskWriteBytes()==-1) continue; 
			long[] fsState = fileSystemState.get(dirName);
			if(fsState==null) {
				fsState = new long[]{1L,0L,0L,0L,Long.MIN_VALUE,0L};
				fileSystemState.put(dirName, fsState);				
			} else {
				fsState[FS_STATE_FLAG] = 1;
			}
		}
		Set<String> removed = new HashSet<String>(fileSystemState.size());
		for(Map.Entry<String, long[]> entry: fileSystemState.entrySet()) {
			if(entry.getValue()[FS_STATE_FLAG] == 0) {
				removed.add(entry.getKey());
			}
		}
		if(!removed.isEmpty()) {
			for(String removedFs : removed) {
				fileSystemState.remove(removedFs);
				String msg = "Detected unmounted filesystem [" + removedFs + "]";
				log(msg);
				tracer.traceString(msg, "FileSystemEvents", PLAT, FS_RESOURCE);
			}
		}
		if(traceMeta) {
			for(FileSystem fs: hsigar.getFileSystemList()) {
				String dirName = fs.getDirName();
				if(fileSystemState.containsKey(dirName)) continue;
				tracer.traceString(fs.getDevName(), "DeviceName", PLAT, FS_RESOURCE, META_TAG, "dirname=" + dirName.replace(":\\", "").replace("/", "~"));
				tracer.traceString(fs.getOptions(), "Options", PLAT, FS_RESOURCE, META_TAG, "dirname=" + dirName.replace(":\\", "").replace("/", "~"));
				tracer.traceString(fs.getSysTypeName(), "SysType", PLAT, FS_RESOURCE, META_TAG, "dirname=" + dirName.replace(":\\", "").replace("/", "~"));
				tracer.traceString(fs.getTypeName(), "Type", PLAT, FS_RESOURCE, META_TAG, "dirname=" + dirName.replace(":\\", "").replace("/", "~"));
			}
		}
		
	}
	
	private static void l(Object msg) {
		System.out.println(msg);
	}
	
	private static long dbl2longPerc(double value) {
		double d = value*100;
		return (long)d;
	}
	
	public static void main(String[] args) {
		NativeMonitor nm = new NativeMonitor();
		for(String s: nm.hsigar.getNetInterfaceList()) {
			l("NIC:" + s);		
		}
	}

}
