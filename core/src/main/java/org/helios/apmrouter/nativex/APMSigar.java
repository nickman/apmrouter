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
package org.helios.apmrouter.nativex;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hyperic.sigar.Cpu;
import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.DirStat;
import org.hyperic.sigar.DirUsage;
import org.hyperic.sigar.DiskUsage;
import org.hyperic.sigar.FileInfo;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemMap;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.MultiProcCpu;
import org.hyperic.sigar.NetConnection;
import org.hyperic.sigar.NetInfo;
import org.hyperic.sigar.NetInterfaceConfig;
import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.sigar.NetRoute;
import org.hyperic.sigar.NetStat;
import org.hyperic.sigar.NfsClientV2;
import org.hyperic.sigar.NfsClientV3;
import org.hyperic.sigar.NfsServerV2;
import org.hyperic.sigar.NfsServerV3;
import org.hyperic.sigar.ProcCpu;
import org.hyperic.sigar.ProcCred;
import org.hyperic.sigar.ProcCredName;
import org.hyperic.sigar.ProcExe;
import org.hyperic.sigar.ProcFd;
import org.hyperic.sigar.ProcMem;
import org.hyperic.sigar.ProcStat;
import org.hyperic.sigar.ProcState;
import org.hyperic.sigar.ProcTime;
import org.hyperic.sigar.ResourceLimit;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarLoader;
import org.hyperic.sigar.SigarProxy;
import org.hyperic.sigar.Swap;
import org.hyperic.sigar.Tcp;
import org.hyperic.sigar.ThreadCpu;
import org.hyperic.sigar.Uptime;
import org.hyperic.sigar.Who;

/**
 * <p>Title: APMSigar</p>
 * <p>Description: Simplification wrapper for the Sigar native API</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.jzab.plugin.nativex.APMSigar</code></p>
 */
public class APMSigar implements SigarProxy {
	/** The singleton instance */
	private static volatile APMSigar agent = null;
	/** The singleton ctor lock */
	private static final Object lock = new Object();
	/** The native agent version */
	private final String sigarVersion;
	/** The native agent Java API version */
	private String version;
	/** The number of processors visible to the JVM */
	public final int cpuCount = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
	/** Indicates if JVM is running on a multi-cpu system */
	public final boolean multiCpu =  ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors() > 1;
	/** The native agent instance */
	private final Sigar sigar;
	
	/** The JVM PID */
	public final long pid;
	
	/**
	 * Acquires the Agent singleton instance
	 * @return the Agent singleton instance
	 */
	public static APMSigar getInstance() {
		if(agent==null) {
			synchronized(lock) {
				if(agent==null) {
					agent = new APMSigar();
				}
			}
		}
		return agent;
	}
	
	/**
	 * The private ctor for the singleton
	 */
	private APMSigar() {
		try {
			NativeLibLoader.loadLib();
			sigar = new Sigar();
			pid = sigar.getPid();
			sigarVersion = Sigar.VERSION_STRING;
		} catch (Exception e) {
			throw new RuntimeException("Failed to load native agent library", e);
		}
	}
	
	private String getLibNameQuietly() {
		try {
			return SigarLoader.getNativeLibraryName();
		} finally {
		}
	}
	
	/** The approximated location of the native libraries if not loaded from the JAR  (usually during dev) */
	private static final String NO_JAR_NATIVE_DIR = "./src/main/resources/META-INF/native/";
	/** The classloader resource prefix for loading the native lib from the jar */
	private static final String NATIVE_DIR_PREFIX = "META-INF/native/";
	
	/**
	 * Loads the agent lnative library and prints version info.
	 * @param args None
	 */
	public static void main(String[] args) {		
		System.out.println(APMSigar.getInstance());
	}
	
	
	
	/**
	 * Loads the NativeAgent Java API version from the manifest
	 */
	private void loadVersion() {
		String vendor = getClass().getPackage().getImplementationVendor();
		String name = getClass().getPackage().getImplementationTitle();
		String version = getClass().getPackage().getImplementationVersion();
		this.version =  new StringBuilder()
			.append(vendor==null ? "Helios" : vendor).append(" ")
			.append(name==null ? "Sigar NativeAgent" : name).append(" ")
			.append(version==null ? "DEV" : version).toString();
	}

	/**
	 * Constructs a <code>String</code> with all attributes
	 * in name = value format.
	 *
	 * @return a <code>String</code> representation 
	 * of this object.
	 */
	@Override
	public String toString() {
		final String TAB = "\n\t";
		final StringBuilder b = new StringBuilder("Helios NativeAgent[");
		b.append(TAB).append("Version:").append(this.version);
		b.append(TAB).append("sigarVersion:").append(this.sigarVersion);
		b.append(TAB).append("nativeLibraryName:").append(SigarLoader.getNativeLibraryName());
		b.append(TAB).append("pid:").append(this.pid);
		try {
			b.append(TAB).append("Host:").append(this.getFQDN());
		} catch (Exception e) {}
		
		b.append("\n]");
		return b.toString();
	}

	/**
	 * @return the sigarVersion
	 */
	public String getSigarVersion() {
		return sigarVersion;
	}



	/**
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * @return the pid
	 */
	@Override
	public long getPid() {
		return pid;
	}
	
	public void gatherProc() throws SigarException {
		sigar.getProcCpu(pid).gather(sigar, pid);
	}
	
	public void gatherThread() throws SigarException {
		sigar.getThreadCpu().gather(sigar, pid);
	}
	
	
	public Sigar getSigar() {
		return sigar;
	}

	/**
	 * @param value
	 * @see org.hyperic.sigar.Sigar#enableLogging(boolean)
	 */
	public void enableLogging(boolean value) {
		sigar.enableLogging(value);
	}

	/**
	 * @return
	 * @see org.hyperic.sigar.Sigar#getCpu()
	 */
	@Override
	public Cpu getCpu()  {
		try {
			return sigar.getCpu();
		} catch (SigarException se) {
			throw new RuntimeException("Failed to invokeinternal Sigar call", se);
		}
	}

	/**
	 * Get list of cpu infomation
	 * @return an attay of cpu infos
	 * @see org.hyperic.sigar.Sigar#getCpuInfoList()
	 */
	@Override
	public CpuInfo[] getCpuInfoList() {
		try {
			return sigar.getCpuInfoList();
		} catch (SigarException se) {
			throw new RuntimeException("Failed to invoke sigar getCpuInfoList", se);
		}
	}

	/**
	 * Returns an array of of per-cpu metrics.
	 * @return an array of of per-cpu metrics
	 * @see org.hyperic.sigar.Sigar#getCpuList()
	 */
	@Override
	public Cpu[] getCpuList() {
		try {
			return sigar.getCpuList();
		} catch (SigarException se) {
			throw new RuntimeException("Failed to invoke sigar getCpuList", se);
		}
	}

	/**
	 * Get system CPU info in percentage format.
	 * @return system CPU info in percentage format.
	 * @see org.hyperic.sigar.Sigar#getCpuPerc()
	 */
	@Override
	public CpuPerc getCpuPerc()  {
		try {
			return sigar.getCpuPerc();
		} catch (SigarException se) {
			throw new RuntimeException("Failed to invoke sigar getCpuPerc", se);
		}
	}

	/**
	 * Get system per CPU info in percentage format.
	 * @return system per CPU info in percentage format.
	 * @see org.hyperic.sigar.Sigar#getCpuPercList()
	 */
	@Override
	public CpuPerc[] getCpuPercList() {
		try {
			return sigar.getCpuPercList();
		} catch (SigarException se) {
			throw new RuntimeException("Failed to invoke sigar getCpuPercList", se);
		}
	}

	/**
	 * @param name
	 * @return
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#getDirStat(java.lang.String)
	 */
	@Override
	public DirStat getDirStat(String name) throws SigarException {
		return sigar.getDirStat(name);
	}

	/**
	 * @param name
	 * @return
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#getDirUsage(java.lang.String)
	 */
	@Override
	public DirUsage getDirUsage(String name) throws SigarException {
		return sigar.getDirUsage(name);
	}

	/**
	 * @param name
	 * @return
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#getDiskUsage(java.lang.String)
	 */
	@Override
	public DiskUsage getDiskUsage(String name) throws SigarException {
		return sigar.getDiskUsage(name);
	}

	/**
	 * Returns the fully qualified host name
	 * @return the fully qualified host name
	 * @see org.hyperic.sigar.Sigar#getFQDN()
	 */
	@Override
	public String getFQDN() {
		try {
			return sigar.getFQDN();
		} catch (SigarException se) {
			throw new RuntimeException("Failed to invokeinternal Sigar call", se);
		}

		
	}

	/**
	 * @param name
	 * @return
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#getFileInfo(java.lang.String)
	 */
	@Override
	public FileInfo getFileInfo(String name) throws SigarException {
		return sigar.getFileInfo(name);
	}

	/**
	 * @return
	 * @see org.hyperic.sigar.Sigar#getFileSystemList()
	 */
	@Override
	public FileSystem[] getFileSystemList() {		
		try {
			return sigar.getFileSystemList();
		} catch (SigarException se) {
			throw new RuntimeException("Failed to invokeinternal Sigar call", se);
		}
	}

	/**
	 * @return
	 * @see org.hyperic.sigar.Sigar#getFileSystemMap()
	 */
	@Override
	public FileSystemMap getFileSystemMap() {
		try {
			return sigar.getFileSystemMap();
		} catch (SigarException se) {
			throw new RuntimeException("Failed to invokeinternal Sigar call", se);
		}
	}

	/**
	 * @param name
	 * @return
	 * @see org.hyperic.sigar.Sigar#getFileSystemUsage(java.lang.String)
	 */
	@Override
	public FileSystemUsage getFileSystemUsage(String name) {		
		try {
			return sigar.getFileSystemUsage(name);
		} catch (SigarException se) {
			throw new RuntimeException("Failed to invokeinternal Sigar call", se);
		}		
	}
	
	/**
	 * Returns the file system usage for the passed file system name or null if one cannot be acquired.
	 * @param name The file system device or directory name
	 * @return the file system usage or null
	 */
	public FileSystemUsage getFileSystemUsageOrNull(String name) {		
		try {
			return sigar.getFileSystemUsage(name);
		} catch (SigarException se) {
			return null;
		}		
	}
	
	/**
	 * Returns the mounted file system usage for the passed file system name or null if one cannot be acquired.
	 * @param name The file system device or directory name
	 * @return the file system usage or null
	 */
	public FileSystemUsage getMountedFileSystemUsageOrNull(String name) {		
		try {
			return sigar.getMountedFileSystemUsage(name);
		} catch (SigarException se) {
			return null;
		}		
	}

	
	/**
	 * @param name
	 * @return
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#getLinkInfo(java.lang.String)
	 */
	@Override
	public FileInfo getLinkInfo(String name) throws SigarException {
		return sigar.getLinkInfo(name);
	}

	/**
	 * @return
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#getLoadAverage()
	 */
	@Override
	public double[] getLoadAverage() throws SigarException {
		return sigar.getLoadAverage();
	}

	/**
	 * @return
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#getMem()
	 */
	@Override
	public Mem getMem() throws SigarException {
		return sigar.getMem();
	}

	/**
	 * @param arg0
	 * @return
	 * @see org.hyperic.sigar.Sigar#getMountedFileSystemUsage(java.lang.String)
	 */
	@Override
	public FileSystemUsage getMountedFileSystemUsage(String arg0)  {
		try {
			return sigar.getMountedFileSystemUsage(arg0);
		} catch (SigarException se) {
			throw new RuntimeException("Failed to invokeinternal Sigar call", se);
		}			
	}

	/**
	 * @param query
	 * @return
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#getMultiProcCpu(java.lang.String)
	 */
	@Override
	public MultiProcCpu getMultiProcCpu(String query) throws SigarException {
		return sigar.getMultiProcCpu(query);
	}

	/**
	 * @param query
	 * @return
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#getMultiProcMem(java.lang.String)
	 */
	@Override
	public ProcMem getMultiProcMem(String query) throws SigarException {
		return sigar.getMultiProcMem(query);
	}

	/**
	 * @return
	 * @see org.hyperic.sigar.Sigar#getNativeLibrary()
	 */
	public File getNativeLibrary() {
		return sigar.getNativeLibrary();
	}

	/**
	 * @param flag
	 * @return
	 * @see org.hyperic.sigar.Sigar#getNetConnectionList(int)
	 */
	@Override
	public NetConnection[] getNetConnectionList(int flag) {
		try {
			return sigar.getNetConnectionList(flag);
		} catch (SigarException se) {
			throw new RuntimeException("Failed to invokeinternal Sigar call", se);
		}
	}
	
	/**
	 * Returns an array of NetConnections established to or from the processes identified by the passed pids.
	 * @param flag The connection type flag
	 * @param pids The list of PIDS.
	 * @return
	 * @see org.hyperic.sigar.Sigar#getNetConnectionList(int)
	 */
	public NetConnection[] getNetConnectionList(int flag, long...pids) {
		if(pids==null || pids.length<1) return new NetConnection[0];
		List<NetConnection> netConns = new ArrayList<NetConnection>();			
		Set<Long> Pids = new HashSet<Long>(pids.length);
		for(long pid: pids) {
			Pids.add(pid);
		}
		for(NetConnection nc: getNetConnectionList(flag)) {
			try {
				//byte[] address = InetAddress.getByName(nc.getLocalAddress()).getAddress();
				if(Pids.contains(sigar.getProcPort(flag, nc.getLocalPort()))) {
					netConns.add(nc);
				}
			} catch (Exception e) {}
			
		}
		return netConns.toArray(new NetConnection[netConns.size()]);
	}
	

	/**
	 * @return
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#getNetInfo()
	 */
	@Override
	public NetInfo getNetInfo() throws SigarException {
		return sigar.getNetInfo();
	}

	/**
	 * @return
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#getNetInterfaceConfig()
	 */
	@Override
	public NetInterfaceConfig getNetInterfaceConfig() throws SigarException {
		return sigar.getNetInterfaceConfig();
	}

	/**
	 * @param name
	 * @return
	 * @see org.hyperic.sigar.Sigar#getNetInterfaceConfig(java.lang.String)
	 */
	@Override
	public NetInterfaceConfig getNetInterfaceConfig(String name) {
		try {
			return sigar.getNetInterfaceConfig(name);
		} catch (SigarException se) {
			throw new RuntimeException("Failed to invokeinternal Sigar call", se);
		}
	}

	/**
	 * @return
	 * @see org.hyperic.sigar.Sigar#getNetInterfaceList()
	 */
	@Override
	public String[] getNetInterfaceList() {
		try {
			return sigar.getNetInterfaceList();
		} catch (SigarException se) {
			throw new RuntimeException("Failed to invokeinternal Sigar call", se);
		}
	}

	/**
	 * @param name
	 * @return
	 * @see org.hyperic.sigar.Sigar#getNetInterfaceStat(java.lang.String)
	 */
	@Override
	public NetInterfaceStat getNetInterfaceStat(String name) {
		try {
			return sigar.getNetInterfaceStat(name);
		} catch (SigarException se) {
			throw new RuntimeException("Failed to invokeinternal Sigar call", se);
		}
	}

	/**
	 * @param arg0
	 * @return
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#getNetListenAddress(long)
	 */
	@Override
	public String getNetListenAddress(long arg0) throws SigarException {
		return sigar.getNetListenAddress(arg0);
	}

	/**
	 * @param port
	 * @return
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#getNetListenAddress(java.lang.String)
	 */
	@Override
	public String getNetListenAddress(String port) throws SigarException {
		return sigar.getNetListenAddress(port);
	}

	/**
	 * @return
	 * @see org.hyperic.sigar.Sigar#getNetRouteList()
	 */
	@Override
	public NetRoute[] getNetRouteList() {
		try {
			return sigar.getNetRouteList();
		} catch (SigarException se) {
			throw new RuntimeException("Failed to invokeinternal Sigar call", se);
		}				
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see org.hyperic.sigar.Sigar#getNetServicesName(int, long)
	 */
	@Override
	public String getNetServicesName(int arg0, long arg1) {
		return sigar.getNetServicesName(arg0, arg1);
	}

	/**
	 * @return
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#getNetStat()
	 */
	@Override
	public NetStat getNetStat() throws SigarException {
		return sigar.getNetStat();
	}

	/**
	 * @param address
	 * @param port
	 * @return
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#getNetStat(byte[], long)
	 */
	public NetStat getNetStat(byte[] address, long port) throws SigarException {
		return sigar.getNetStat(address, port);
	}

	/**
	 * @return
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#getNfsClientV2()
	 */
	@Override
	public NfsClientV2 getNfsClientV2() throws SigarException {
		return sigar.getNfsClientV2();
	}

	/**
	 * @return
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#getNfsClientV3()
	 */
	@Override
	public NfsClientV3 getNfsClientV3() throws SigarException {
		return sigar.getNfsClientV3();
	}

	/**
	 * @return
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#getNfsServerV2()
	 */
	@Override
	public NfsServerV2 getNfsServerV2() throws SigarException {
		return sigar.getNfsServerV2();
	}

	/**
	 * @return
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#getNfsServerV3()
	 */
	@Override
	public NfsServerV3 getNfsServerV3() throws SigarException {
		return sigar.getNfsServerV3();
	}

	/**
	 * @param arg0
	 * @return
	 * @see org.hyperic.sigar.Sigar#getProcArgs(long)
	 */
	@Override
	public String[] getProcArgs(long arg0) {
		try {
			return sigar.getProcArgs(arg0);
		} catch (SigarException se) {
			throw new RuntimeException("Failed to invokeinternal Sigar call", se);
		}
	}

	/**
	 * @param pid
	 * @return
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#getProcArgs(java.lang.String)
	 */
	@Override
	public String[] getProcArgs(String pid) throws SigarException {
		return sigar.getProcArgs(pid);
	}

	/**
	 * @param pid
	 * @return
	 * @see org.hyperic.sigar.Sigar#getProcCpu(long)
	 */
	@Override
	public ProcCpu getProcCpu(long pid) {
		try {
			return sigar.getProcCpu(pid);
		} catch (SigarException se) {
			throw new RuntimeException("Failed to invokeinternal Sigar call", se);
		}				
	}

	/**
	 * @param pid
	 * @return
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#getProcCpu(java.lang.String)
	 */
	@Override
	public ProcCpu getProcCpu(String pid) throws SigarException {
		return sigar.getProcCpu(pid);
	}

	/**
	 * @param pid
	 * @return
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#getProcCred(long)
	 */
	@Override
	public ProcCred getProcCred(long pid) throws SigarException {
		return sigar.getProcCred(pid);
	}

	/**
	 * @param pid
	 * @return
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#getProcCred(java.lang.String)
	 */
	@Override
	public ProcCred getProcCred(String pid) throws SigarException {
		return sigar.getProcCred(pid);
	}

	/**
	 * @param pid
	 * @return
	 * @see org.hyperic.sigar.Sigar#getProcCredName(long)
	 */
	@Override
	public ProcCredName getProcCredName(long pid) {
		try {
			return sigar.getProcCredName(pid);
		} catch (SigarException se) {
			throw new RuntimeException("Failed to invokeinternal Sigar call", se);
		}				
	}

	/**
	 * @param pid
	 * @return
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#getProcCredName(java.lang.String)
	 */
	@Override
	public ProcCredName getProcCredName(String pid) throws SigarException {
		return sigar.getProcCredName(pid);
	}

	/**
	 * @param pid
	 * @param key
	 * @return
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#getProcEnv(long, java.lang.String)
	 */
	@Override
	public String getProcEnv(long pid, String key) throws SigarException {
		return sigar.getProcEnv(pid, key);
	}

	/**
	 * @param pid
	 * @return
	 * @see org.hyperic.sigar.Sigar#getProcEnv(long)
	 */
	@Override
	public Map<?, ?> getProcEnv(long pid) {
		try {
			return sigar.getProcEnv(pid);
		} catch (SigarException se) {
			throw new RuntimeException("Failed to invokeinternal Sigar call", se);
		}		
	}

	/**
	 * @param pid
	 * @param key
	 * @return
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#getProcEnv(java.lang.String, java.lang.String)
	 */
	@Override
	public String getProcEnv(String pid, String key) throws SigarException {
		return sigar.getProcEnv(pid, key);
	}

	/**
	 * @param pid
	 * @return
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#getProcEnv(java.lang.String)
	 */
	@Override
	public Map<?, ?> getProcEnv(String pid) throws SigarException {
		return sigar.getProcEnv(pid);
	}

	/**
	 * @param pid
	 * @return
	 * @see org.hyperic.sigar.Sigar#getProcExe(long)
	 */
	@Override
	public ProcExe getProcExe(long pid) {		
		try {
			return sigar.getProcExe(pid);
		} catch (SigarException se) {
			throw new RuntimeException("Failed to invokeinternal Sigar call", se);
		}		
	}

	/**
	 * @param pid
	 * @return
	 * @see org.hyperic.sigar.Sigar#getProcExe(java.lang.String)
	 */
	@Override
	public ProcExe getProcExe(String pid)  {		
		try {
			return sigar.getProcExe(pid);
		} catch (SigarException se) {
			throw new RuntimeException("Failed to invokeinternal Sigar call", se);
		}		
	}

	/**
	 * @param pid
	 * @return
	 * @see org.hyperic.sigar.Sigar#getProcFd(long)
	 */
	@Override
	public ProcFd getProcFd(long pid) {		
		try {
			return sigar.getProcFd(pid);
		} catch (SigarException se) {
			throw new RuntimeException("Failed to invokeinternal Sigar call", se);
		}		
		
	}

	/**
	 * @param pid
	 * @return
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#getProcFd(java.lang.String)
	 */
	@Override
	public ProcFd getProcFd(String pid) throws SigarException {
		return sigar.getProcFd(pid);
	}

	/**
	 * @return
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#getProcList()
	 */
	@Override
	public long[] getProcList() throws SigarException {
		return sigar.getProcList();
	}

	/**
	 * @param pid
	 * @return
	 * @see org.hyperic.sigar.Sigar#getProcMem(long)
	 */
	@Override
	public ProcMem getProcMem(long pid) {		
		try {
			return sigar.getProcMem(pid);
		} catch (SigarException se) {
			throw new RuntimeException("Failed to invokeinternal Sigar call", se);
		}		
		
	}

	/**
	 * @param pid
	 * @return
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#getProcMem(java.lang.String)
	 */
	@Override
	public ProcMem getProcMem(String pid) throws SigarException {
		return sigar.getProcMem(pid);
	}

	/**
	 * @param pid
	 * @return
	 * @see org.hyperic.sigar.Sigar#getProcModules(long)
	 */
	@Override
	public List<?> getProcModules(long pid) {
		try {
			return sigar.getProcModules(pid);
		} catch (SigarException se) {
			throw new RuntimeException("Failed to invokeinternal Sigar call", se);
		}						
	}

	/**
	 * @param pid
	 * @return
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#getProcModules(java.lang.String)
	 */
	@Override
	public List<?> getProcModules(String pid) throws SigarException {
		return sigar.getProcModules(pid);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see org.hyperic.sigar.Sigar#getProcPort(int, long)
	 */
	@Override
	public long getProcPort(int arg0, long arg1) {		
		try {
			return sigar.getProcPort(arg0, arg1);
		} catch (SigarException se) {
			throw new RuntimeException("Failed to invokeinternal Sigar call", se);
		}						
	}

	/**
	 * @param protocol
	 * @param port
	 * @return
	 * @see org.hyperic.sigar.Sigar#getProcPort(java.lang.String, java.lang.String)
	 */
	@Override
	public long getProcPort(String protocol, String port) {
		try {
			return sigar.getProcPort(protocol, port);
		} catch (SigarException se) {
			throw new RuntimeException("Failed to invokeinternal Sigar call", se);
		}						
	}

	/**
	 * @return
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#getProcStat()
	 */
	@Override
	public ProcStat getProcStat() throws SigarException {
		return sigar.getProcStat();
	}

	/**
	 * @param pid
	 * @return
	 * @see org.hyperic.sigar.Sigar#getProcState(long)
	 */
	@Override
	public ProcState getProcState(long pid) {		
		try {
			return sigar.getProcState(pid);
		} catch (SigarException se) {
			throw new RuntimeException("Failed to invokeinternal Sigar call", se);
		}
		
	}

	/**
	 * @param pid
	 * @return
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#getProcState(java.lang.String)
	 */
	@Override
	public ProcState getProcState(String pid) throws SigarException {
		return sigar.getProcState(pid);
	}

	/**
	 * @param pid
	 * @return
	 * @see org.hyperic.sigar.Sigar#getProcTime(long)
	 */
	@Override
	public ProcTime getProcTime(long pid)  {		
		try {
			return sigar.getProcTime(pid);
		} catch (SigarException se) {
			throw new RuntimeException("Failed to invokeinternal Sigar call", se);
		}
	}

	/**
	 * @param pid
	 * @return
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#getProcTime(java.lang.String)
	 */
	@Override
	public ProcTime getProcTime(String pid) throws SigarException {
		return sigar.getProcTime(pid);
	}

	/**
	 * @return
	 * @see org.hyperic.sigar.Sigar#getResourceLimit()
	 */
	@Override
	public ResourceLimit getResourceLimit()  {
		try {
			return sigar.getResourceLimit();
		} catch (SigarException se) {
			throw new RuntimeException("Failed to invokeinternal Sigar call", se);
		}			
	}

	/**
	 * @param arg0
	 * @return
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#getServicePid(java.lang.String)
	 */
	@Override
	public long getServicePid(String arg0) throws SigarException {
		return sigar.getServicePid(arg0);
	}

	/**
	 * @return
	 * @see org.hyperic.sigar.Sigar#getSwap()
	 */
	@Override
	public Swap getSwap() {
		try {
			return sigar.getSwap();
		} catch (SigarException  se) {
			throw new RuntimeException("Failed to invokeinternal Sigar call", se);
		}
	}

	/**
	 * @return
	 * @see org.hyperic.sigar.Sigar#getTcp()
	 */
	@Override
	public Tcp getTcp()  {
		try {
			return sigar.getTcp();
		} catch (SigarException  se) {
			throw new RuntimeException("Failed to invokeinternal Sigar call", se);
		}
	}

	/**
	 * @return
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#getThreadCpu()
	 */
	public ThreadCpu getThreadCpu() {
		try {
			return sigar.getThreadCpu();
		} catch (SigarException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @return
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#getUptime()
	 */
	@Override
	public Uptime getUptime() throws SigarException {
		return sigar.getUptime();
	}

	/**
	 * @return
	 * @see org.hyperic.sigar.Sigar#getWhoList()
	 */
	@Override
	public Who[] getWhoList()  {
		try {
			return sigar.getWhoList();
		} catch (SigarException  se) {
			throw new RuntimeException("Failed to invokeinternal Sigar call", se);
		}
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#kill(long, int)
	 */
	public void kill(long arg0, int arg1) throws SigarException {
		sigar.kill(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#kill(long, java.lang.String)
	 */
	public void kill(long arg0, String arg1) throws SigarException {
		sigar.kill(arg0, arg1);
	}
	
	/**
	 * Reloads the sigar instance
	 * @return this instance
	 */
	public APMSigar load() {
		try {
			sigar.load();
			return this;
		} catch (Exception e) {
			throw new RuntimeException("Failed to reload the sigar instance", e);
		}
		
	}

	/**
	 * @param pid
	 * @param signum
	 * @throws SigarException
	 * @see org.hyperic.sigar.Sigar#kill(java.lang.String, int)
	 */
	public void kill(String pid, int signum) throws SigarException {
		sigar.kill(pid, signum);
	}

	/**
	 * Returns the number of processors visible to the JVM
	 * @return the number of processors visible to the JVM
	 */
	public int getCpuCount() {
		return cpuCount;
	}

	/**
	 * Indicates if JVM is running on a multi-cpu system
	 * @return true if more than one CPU is available, false if only one is available.
	 */
	public boolean isMultiCpu() {
		return multiCpu;
	}

}
