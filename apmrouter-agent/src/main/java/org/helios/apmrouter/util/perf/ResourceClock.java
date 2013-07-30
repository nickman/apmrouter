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
package org.helios.apmrouter.util.perf;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.helios.apmrouter.jmx.threadinfo.ExtendedThreadManager;
import org.helios.apmrouter.util.Binaries;

/**
 * <p>Title: ResourceClock</p>
 * <p>Description: Resource measurement functional enum for elapsed usages</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.util.perf.ResourceClock</code></p>
 */

public enum ResourceClock {
	/** The elapsed time in ns. */
	TIME(Binaries.getBinaryIterator(10).next()),
	/** The elapsed system CPU time in ns */
	SYS_CPU(Binaries.getBinaryIterator(10).next()),
	/** The elapsed user CPU time in ns */
	USER_CPU(Binaries.getBinaryIterator(10).next()),
	/** The thread blocked time in ms */
	BLOCKED_TIME(Binaries.getBinaryIterator(10).next()),
	/** The number of times the thread was blocked */
	BLOCKED_COUNT(Binaries.getBinaryIterator(10).next()),
	/** The thread waited time in ms */
	WAITED_TIME(Binaries.getBinaryIterator(10).next()),
	/** The number of times the thread waited */
	WAITED_COUNT(Binaries.getBinaryIterator(10).next());
	
	/** Decode map to decode the int code to the ResourceClock enum */
	public static final Map<Integer, ResourceClock> CODE2ENUM;
	/** Decode map to decode the ordinal to the ResourceClock enum */
	public static final Map<Integer, ResourceClock> ORD2ENUM;
	/** Decode map to decode the int code to the ResourceClock enum */
	public static final Map<String, ResourceClock> CODESTR2ENUM;
	/** Decode map to decode the ordinal to the ResourceClock enum */
	public static final Map<String, ResourceClock> ORDSTR2ENUM;
	/** Decode map to decode the ordinal to the ResourceClock enum */
	public static final Map<String, ResourceClock> ALLSTR2ENUM;
	/** Decode map to decode the ordinal to the ResourceClock enum */
	public static final Map<Integer, ResourceClock> ALL2ENUM;
	
	/** Indicates if the extended thread mx bean is installed */
	private static final AtomicBoolean extendedThreadMXBean = new AtomicBoolean(false);
	/** Indicates if thread cpu time is enabled */
	private static final AtomicBoolean threadCpuTimeEnabled = new AtomicBoolean(false);
	/** Indicates if thread contention monitoring is enabled */
	private static final AtomicBoolean threadContentionEnabled = new AtomicBoolean(false);
	
	/** Indicates if thread cpu time is supported */
	public static final boolean cpuTimeSupported = ManagementFactory.getThreadMXBean().isThreadCpuTimeSupported();
	/** Indicates if thread contention monitoring is supported */
	public static final boolean threadContentionSupported = ManagementFactory.getThreadMXBean().isThreadContentionMonitoringSupported();
	
	static {
		Binaries.reset();
		ResourceClock[] values = ResourceClock.values();
		Map<Integer, ResourceClock> tmp = new HashMap<Integer, ResourceClock>(values.length);
		Map<Integer, ResourceClock> otmp = new HashMap<Integer, ResourceClock>(values.length);
		Map<String, ResourceClock> tmpStr = new HashMap<String, ResourceClock>(values.length);
		Map<String, ResourceClock> oTmpStr = new HashMap<String, ResourceClock>(values.length);
		Map<Integer, ResourceClock> all = new HashMap<Integer, ResourceClock>(values.length*2);
		Map<String, ResourceClock> allStr = new HashMap<String, ResourceClock>(values.length*2);
		for(ResourceClock rc: values) {
			tmp.put(rc.code, rc);
			otmp.put(rc.ordinal(), rc);
			oTmpStr.put(Integer.toString(rc.ordinal()), rc);
			tmpStr.put(Integer.toString(rc.code), rc);
			allStr.put(Integer.toString(rc.code), rc);
			allStr.put(Integer.toString(rc.ordinal()), rc);
			all.put(rc.code, rc);
			all.put(rc.ordinal(), rc);			
		}
		CODE2ENUM = Collections.unmodifiableMap(tmp);
		ORD2ENUM = Collections.unmodifiableMap(otmp);
		ORDSTR2ENUM = Collections.unmodifiableMap(oTmpStr);
		CODESTR2ENUM = Collections.unmodifiableMap(tmpStr);
		ALLSTR2ENUM = Collections.unmodifiableMap(allStr);
		ALL2ENUM = Collections.unmodifiableMap(all);		
		try {
			if(!ExtendedThreadManager.isInstalled()) {
				ExtendedThreadManager.install();
			}
			extendedThreadMXBean.set(ExtendedThreadManager.isInstalled());
		} catch (Exception ex) {
			extendedThreadMXBean.set(false);
		}
	}
	
	private ResourceClock(int code) {
		this.code = code;
	}
	
	/** The int decode for the resource */
	public final int code;
	
	/**
	 * Decodes the passed string to a ResourceClock, trimming and uppercasing the passed value 
	 * @param value The value to decode
	 * @return the decoded ResourceClock
	 */
	public static ResourceClock forValue(Object value) {
		if(value==null || value.toString().trim().isEmpty()) throw new RuntimeException("The passed value was null or empty");
		Integer vi = toInteger(value);
		if(vi!=null) {
			ResourceClock rc = ALL2ENUM.get(vi);
			if(rc!=null) return rc;
			throw new RuntimeException("The passed numeric value [" + vi + "] was not a valid Resource");
		}
		String v = value.toString().trim().toUpperCase();
		try {
			return ResourceClock.valueOf(v);
		} catch (Exception ex) {
			throw new RuntimeException("The passed value [" + value + "] was not a valid Resource", ex);
		}
	}
	
	/**
	 * Returns a mask enabled for all the passed resources
	 * @param resources The resources to enable 
	 * @return an int but mask with the passed resources enabled, or zero if no resources are passed.
	 */
	public static int enableFor(ResourceClock...resources) {
		int _code = 0;
		if(resources==null || resources.length==0) return _code;
		for(ResourceClock rc: resources) {
			_code = rc.enable(_code);
		}
		return _code;
	}
	
	/**
	 * Returns a mask enabled for all the passed resources
	 * @param resources The resources to enable 
	 * @return an int but mask with the passed resources enabled, or zero if no resources are passed.
	 */
	public static int enableFor(int...resources) {
		int _code = 0;
		if(resources==null || resources.length==0) return _code;
		for(int resource: resources) {
			ResourceClock rc = ResourceClock.forValue(resource);
			_code = rc.enable(_code);
		}
		return _code;
	}
	
	/**
	 * Returns a mask enabled for all the passed resources
	 * @param resources The resources to enable 
	 * @return an int but mask with the passed resources enabled, or zero if no resources are passed.
	 */
	public static int enableFor(String...resources) {
		int _code = 0;
		if(resources==null || resources.length==0) return _code;
		for(String resource: resources) {
			ResourceClock rc = ResourceClock.forValue(resource);
			_code = rc.enable(_code);
		}
		return _code;
	}
	
	/**
	 * Returns an array of resource enums for each resource enabled in the passed mask
	 * @param mask The mask to return the resources for
	 * @return an array of resource enums
	 */
	public static ResourceClock[] getEnabledResources(int mask) {
		Set<ResourceClock> matched = EnumSet.noneOf(ResourceClock.class);
		for(ResourceClock rc: ResourceClock.values()) {
			if(rc.isEnabled(mask)) {
				matched.add(rc);
			}
		}
		return matched.toArray(new ResourceClock[matched.size()]);
	}
	
	/**
	 * Determines if the passed mask has the passed bit turned on.  
	 * @param value The bit to test for.  
	 * @param mask The bit mask to test 
	 * @return true if the passed bitMask value is enabled in the passed bitMask
	 */
	public static boolean isEnabledFor(final int value, final int mask) {
		return (value & mask)!=0;
	}
	
	
	
	
	/**
	 * Enables the current resource in the passed int
	 * @param mask The value int to enable this resource in
	 * @return The passed value enabled for this resource
	 */
	public int enable(int mask) {
		mask = mask | code;
		return mask;
	}
	
	/**
	 * Disables the current resource in the passed int
	 * @param mask The value int to disable this resource in
	 * @return The passed value disabled for this resource
	 */
	public int disable(int mask) {
		mask = mask ^ code;  
		return mask;
	}
	
	/**
	 * Determines if the passed mask is enabled for the current resource
	 * @param mask The mask to test
	 * @return true if the passed mask is enabled, false otherwise
	 */
	public boolean isEnabled(int mask) {
		return (code & mask)!=0;
	}
	
	
	
	
	public static void main(String[] args) {
		for(ResourceClock rc: ResourceClock.values()) {
			log(rc.name() + "\t" + Integer.toBinaryString(rc.code));
		}
		int v = 0;
		log("=== Enabling Odds ===");
		for(ResourceClock rc: ResourceClock.values()) {
			if(rc.ordinal()%2==0) continue;
			v = rc.enable(v);
			log("Enabled for [" + rc.name() + "]:[" + Integer.toBinaryString(v) + "]");
		}
		log("=== Disabling Odds === [" + Arrays.toString(ResourceClock.getEnabledResources(v)) + "]");
		for(ResourceClock rc: ResourceClock.values()) {
			if(rc.ordinal()%2==0) continue;
			v = rc.disable(v);
			log("Disabled for [" + rc.name() + "]:[" + Integer.toBinaryString(v) + "]");
		}
		
		v = 0;
		log("=== Enabling Evens ===");
		for(ResourceClock rc: ResourceClock.values()) {
			if(rc.ordinal()%2!=0) continue;
			v = rc.enable(v);
			log("Enabled for [" + rc.name() + "]:[" + Integer.toBinaryString(v) + "]");
		}
		log("=== Disabling Evens ===");
		for(ResourceClock rc: ResourceClock.values()) {
			if(rc.ordinal()%2!=0) continue;
			v = rc.disable(v);
			log("Disabled for [" + rc.name() + "]:[" + Integer.toBinaryString(v) + "]");
		}
		
		
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	/**
	 * Converts the passed object to an integer, returning it or null if the conversion cannot be made
	 * @param obj The object to convert
	 * @return the converted integer or null if it could not be converted
	 */
	public static Integer toInteger(Object obj) {
		if(obj==null) return null;
		try {
			if(obj instanceof Number) {			
				return ((Number)obj).intValue();
			}
			return Integer.parseInt(obj.toString().trim());
		} catch (Exception ex) {
			return null;
		}
		
	}
	
}
