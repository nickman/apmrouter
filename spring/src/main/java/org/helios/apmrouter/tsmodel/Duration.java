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
package org.helios.apmrouter.tsmodel;


/**
 * <p>Title: Duration</p>
 * <p>Description: The factored object of a {@link TSUnit} and a size.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.tsmodel.Duration</code></p>
 */
public class Duration implements Comparable<Duration> {
	/** The length of the duration */
	public final long size;
	/** The unit of the duration */
	public final TSUnit unit;
	/** The duration in seconds */
	public final long seconds;
	
	/**
	 * Creates a new Duration
	 * @param size The length of the duration
	 * @param unit The unit of the duration
	 */
	public Duration(long size, TSUnit unit) {
		if(unit==null) throw new IllegalArgumentException("The passed unit was null", new Throwable());
		this.size = size;
		this.unit = unit;
		seconds = TSUnit.SECONDS.convert(size, unit);
	}
	
	public static void main(String[] args) {
		log("Duration");
		Duration d = new Duration(1987180, TSUnit.SECONDS);
		Duration d23 = new Duration(23, TSUnit.DAYS);
		log("D23 Secs: [" + d23.seconds + "]");
		log("Base: [" + d + "]");
		log("Refined: [" + d.refine() + "]");

		
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	

	/**
	 * Returns the length of the duration
	 * @return the size
	 */
	public long getSize() {
		return size;
	}

	/**
	 * Returns the unit of the duration
	 * @return the unit of the duration
	 */
	public TSUnit getUnit() {
		return unit;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "" + size + unit.shortCode.toLowerCase();
	}

	/**
	 * Returns the duration in seconds
	 * @return the duration in seconds
	 */
	public long getSeconds() {
		return seconds;
	}
	
	/**
	 * Reduces the size by raising the unit as long as there is an exact conversion with no remainder
	 * @return a refined duration
	 */
	public Duration refine() {
		return unit.refine(size);
	}
	
	/**
	 * Returns a Duration rendered in the passed unit
	 * @param unit The unit to render in 
	 * @return a new Duration
	 */
	public Duration renderIn(TSUnit unit) {
		return new Duration(unit.convert(size, this.unit), unit);
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (size ^ (size >>> 32));
		result = prime * result + ((unit == null) ? 0 : unit.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Duration other = (Duration) obj;
		if (size != other.size)
			return false;
		if (unit != other.unit)
			return false;
		return true;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Duration other) {
		if(other.seconds==seconds) return 0;
		return seconds<other.seconds ? -1 : 1;
	}
	
	
	
}
