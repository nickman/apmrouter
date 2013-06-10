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
package org.helios.apmrouter.server.services.mtxml;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>Title: SanStatsParsingContext</p>
 * <p>Description: A context shared across parsing operations for a single san stat xml document</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.services.mtxml.SanStatsParsingContext</code></p>
 */

public class SanStatsParsingContext {
//    <serial_number>1302666</serial_number>
//    <sys_name>pdk-mg-3par-01</sys_name>
//    <cpu_mhz>2327</cpu_mhz>
//    <ip_name>pdk-mg-3par-01</ip_name>
//    <os_rev>3.1.2</os_rev>
//    <system_model>InServ F400</system_model>
//    <ch_size_mb>256</ch_size_mb>
	/** An unknown value */
	public static final String UNKNOWN = "<unknown>";
	
	/** The system serial number */
	private String serialNumber = UNKNOWN;
	/** The system name */
	private String systemName = UNKNOWN;
	/** The system cpu speed */
	private int cpuMhz = -1;
	/** The ip name */
	private String ipName = UNKNOWN;
	/** The system os version */
	private String osVersion = UNKNOWN;
	/** The system model name */
	private String modelName = UNKNOWN;
	/** The cache size in mb */
	private int cacheSize = -1;
	
	
	
	
	/** The number of successfully parsed lun fragments */
	private final AtomicInteger lunsParsed = new AtomicInteger(0);
	/** The completion queue */
	private final BlockingQueue<Map<String, String>> completionQueue = new ArrayBlockingQueue<Map<String, String>>(2000, false);
	
	
	/**
	 * Sets the system info
	 * @param sysInfo a map of the system info values keyed by the xml tag name
	 */
	public void setSysInfo(Map<String, String> sysInfo) {
		serialNumber = sysInfo.get("serial_number");
		systemName = sysInfo.get("system_name");
		ipName = sysInfo.get("ip_name");
		osVersion = sysInfo.get("os_version");
		modelName = sysInfo.get("model_name");
		cacheSize = Integer.parseInt(sysInfo.get("ch_size_mb"));
		cpuMhz = Integer.parseInt(sysInfo.get("cpu_mhz"));
	}
	
	/**
	 * Adds a vlunstat sampling
	 * @param vlunstats a vlunstat sampling
	 * @throws InterruptedException thrown if the current thread is interrupted while waiting to write the map to the completion queue 
	 */
	public void addVLun(Map<String, String> vlunstats) throws InterruptedException {
		this.lunsParsed.incrementAndGet();
		completionQueue.put(vlunstats);
	}

	/**
	 * Returns the SAN serial number 
	 * @return the serialNumber
	 */
	public String getSerialNumber() {
		return serialNumber;
	}

	/**
	 * Returns the SAN system name
	 * @return the systemName
	 */
	public String getSystemName() {
		return systemName;
	}

	/**
	 * Returns the SAN cpu speed in Mhz
	 * @return the cpuMhz
	 */
	public int getCpuMhz() {
		return cpuMhz;
	}

	/**
	 * Returns the SAN ip name
	 * @return the ipName
	 */
	public String getIpName() {
		return ipName;
	}

	/**
	 * Returns the SAN OS version
	 * @return the osVersion
	 */
	public String getOsVersion() {
		return osVersion;
	}

	/**
	 * Returns the SAN model name
	 * @return the modelName
	 */
	public String getModelName() {
		return modelName;
	}

	/**
	 * Returns the SAN cache size in MB
	 * @return the cacheSize
	 */
	public int getCacheSize() {
		return cacheSize;
	}

	/**
	 * Returns the number of vlunstats parsed for this file
	 * @return the lunsParsed
	 */
	public int getLunsParsed() {
		return lunsParsed.get();
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return new StringBuilder("SANStatsParsingContext[").append(this.systemName).append("] vluns parsed:").append(lunsParsed.get()).toString();
	}

}
