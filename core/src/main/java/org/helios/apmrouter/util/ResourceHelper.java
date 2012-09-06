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
package org.helios.apmrouter.util;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Title: ResourceHelper</p>
 * <p>Description: JVM Resource Helper</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.util.ResourceHelper</code></p>
 */

public class ResourceHelper {

	/**
	 * Computes a delta memory usage for after the test vs. before the test
	 * @param before The memory usage before the test
	 * @param after The memory usage after the test
	 * @return the delta memory usage
	 */
	public static MemoryUsage delta(MemoryUsage before, MemoryUsage after) {
		return new MemoryUsage(
				after.getInit() - before.getInit(),
				after.getUsed() - before.getUsed(),
				after.getCommitted() - before.getCommitted(),
				after.getMax() - before.getMax()
		);
	}

	
	/**
	 * <p>Title: NamedMemoryUsage</p>
	 * <p>Description: A wrapped memory usage extended to compute some additional stats and display a memory type name</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.util.ResourceHelper.NamedMemoryUsage</code></p>
	 */
	public static class NamedMemoryUsage extends MemoryUsage {
		/** The wrapped memory usage type */
		private final String type;
		
		/**
		 * Creates a new NamedMemoryUsage
		 * @param delegate The wrapped memory usage
		 * @param type The wrapped memory usage type
		 */
		public NamedMemoryUsage(MemoryUsage delegate, String type) {
			super(delegate.getInit(), delegate.getUsed(), delegate.getCommitted(), delegate.getMax());
			this.type = type;
		}
		
		/**
		 * Returns the memory type percent usage, that being the percentage <b><code>Used</code></b> is of <b><code>Committed</code></b> 
		 * @return The percentage usage
		 */
		public long getPercentUsage() {
			return SimpleMath.percent(getUsed(), getCommitted());
		}
		
		/**
		 * Returns the memory type percent capacity, that being the percentage <b><code>Used</code></b> is of <b><code>Max</code></b> 
		 * @return The percentage capacity
		 */
		public long getPercentCapacity() {
			return SimpleMath.percent(getUsed(), getMax());
		}
		
		/**
		 * Returns the memory type percent allocated, that being the percentage <b><code>Committed</code></b> is of <b><code>Max</code></b> 
		 * @return The percentage allocated
		 */
		public long getPercentAllocated() {
			return SimpleMath.percent(getCommitted(), getMax());
		}

		
		
		/**
		 * {@inheritDoc}
		 * @see java.lang.management.MemoryUsage#toString()
		 */
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("[").append(type).append("] ").append(super.toString());
			sb.append(" percentUsage = ").append(getPercentUsage());
			sb.append(" percentCapacity = ").append(getPercentCapacity());
			sb.append(" percentAllocated = ").append(getPercentAllocated());
			return sb.toString();
		}
		
		/**
		 * Returns the toString of the super toString (not including the percentages, since this is a delta)
		 * @return the toString of the super toString
		 */
		public String deltaToString() {
			return super.toString();
		}
		
		
		/**
		 * Returns a map of each metric value of the memory usage keyed by the name
		 * @return a map of each metric value of the memory usage keyed by the name
		 */
		public Map<String, Long> getUsageMap() {
			Map<String, Long> map = new HashMap<String, Long>(7);
			map.put("init", getInit());
			map.put("used", getUsed());
			map.put("committed", getCommitted());
			map.put("max", getMax());
			map.put("percentUsage", getPercentUsage());
			map.put("percentCapacity", getPercentCapacity());
			map.put("percentAllocation", getPercentAllocated());
			return map;
		}
		
		/**
		 * Returns a map of each metric value of the memory usage keyed by the name without the percentages, since this is a delta
		 * @return a map of each metric value of the memory usage keyed by the name
		 */
		public Map<String, Long> getDeltaMap() {
			Map<String, Long> map = new HashMap<String, Long>(7);
			map.put("init", getInit());
			map.put("used", getUsed());
			map.put("committed", getCommitted());
			map.put("max", getMax());
			return map;
		}
		
		
	}
	
	/**
	 * Collects the memory usage, optionally calling a gc first
	 * @param gcFirst If true, gc will called first
	 * @return an array of heap memory and non-heap memory usages
	 */
	public static MemoryUsage[] memoryUsage(boolean gcFirst) {		
		if(gcFirst) {
			ManagementFactory.getMemoryMXBean().gc();
			System.runFinalization();
			try { Thread.currentThread().join(500); } catch (Exception e) {}
			ManagementFactory.getMemoryMXBean().gc();
		}
		return new MemoryUsage[]{ManagementFactory.getMemoryMXBean().getHeapMemoryUsage(), ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage()};
	}
	

}
