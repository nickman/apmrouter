/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2013, Helios Development Group and individual contributors
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

/**
 * <p>Title: BusyThread</p>
 * <p>Description: Represents collected data on a thread when computing top threads.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.jmx.threadinfo.BusyThread</code></p>
 */

public class BusyThread implements Comparable<BusyThread> {
	/** The cpu time in ns. */
	protected final long cpuTime;	
	/** The thread name */
	protected String threadName;
	
	
	
	/**
	 * Creates a new BusyThread
	 * @param cpuTime The cpu time in ns.
	 * @param threadName The thread name
	 */
	public BusyThread(long cpuTime, String threadName) {
		this.cpuTime = cpuTime;
		this.threadName = threadName;
	}



	/**
	 * {@inheritDoc}
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(BusyThread bt) {
		if(bt.cpuTime==cpuTime) return threadName.compareTo(bt.threadName);
		return bt.cpuTime>cpuTime ? 1 : -1;
	}



	@Override
	public String toString() {
		return String.format("%s\t:%s", threadName, cpuTime);
	}



	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((threadName == null) ? 0 : threadName.hashCode());
		return result;
	}



	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BusyThread other = (BusyThread) obj;
		if (threadName == null) {
			if (other.threadName != null)
				return false;
		} else if (!threadName.equals(other.threadName))
			return false;
		return true;
	}

}
