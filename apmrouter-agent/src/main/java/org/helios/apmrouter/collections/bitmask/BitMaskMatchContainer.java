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
package org.helios.apmrouter.collections.bitmask;

/**
 * <p>Title: BitMaskMatchContainer</p>
 * <p>Description: A container for an arbitrary type <b><code>K</code></b> keyed by a numeric which is used to evaluate matches against another container using bit masks </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.collections.bitmask.BitMaskMatchContainer</code></p>
 * @param <T> The numeric type representing the bit mask
 * @param <K> The arbitrary object associated with the bit mask
 */

public interface BitMaskMatchContainer<T extends Number, K> {
	/**
	 * Determines if there is a bit mask match on keys
	 * @param comparisonKey The numeric key to bit-mask match
	 * @return true for a match, false otherwise
	 */
	public boolean match(T comparisonKey);
	
	/**
	 * Returns the bitmasking key
	 * @return the bitmasking key
	 */
	public Number getBitMaskKey();
	
	/**
	 * Returns the value
	 * @return the value
	 */
	public K getValue();
}
