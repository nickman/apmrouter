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
package org.helios.apmrouter.dataservice.json.catalog;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.helios.apmrouter.collections.bitmask.AbstractBitMaskMatchContainer;

/**
 * <p>Title: MetricURIBitMaskContainer</p>
 * <p>Description: A bit mask container for matching {@link MetricURI} instances with overlapping interests</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.dataservice.json.catalog.MetricURIBitMaskContainer</code></p>
 */

public class MetricURIBitMaskContainer extends AbstractBitMaskMatchContainer<Long, Set<MetricURISubscription>> {
	/** An empty set for keys */
	private static final Set<MetricURISubscription> EMPTY_SUBS = Collections.unmodifiableSet(new HashSet<MetricURISubscription>(0));
	
	/**
	 * Returns a key for looking up other MetricURIBitMaskContainer
	 * @param bitMask The bit mask that will equals
	 * @return a key
	 */
	public static MetricURIBitMaskContainer Key(long bitMask) {
		return new  MetricURIBitMaskContainer(bitMask, EMPTY_SUBS);
	}
	
	/**
	 * Creates a new MetricURIBitMaskContainer
	 * @param bitMask the bitmask describing the match fields of the {@link MetricURI}
	 * @param subs the {@link MetricURI}s represented that have the passed bit mask
	 */
	public MetricURIBitMaskContainer(long bitMask, Set<MetricURISubscription> subs) {
		super(bitMask, subs);
	}
	
	/**
	 * Creates a new MetricURIBitMaskContainer
	 * @param bitMask the bitmask describing the match fields of the {@link MetricURI}
	 */
	public MetricURIBitMaskContainer(long bitMask) {
		super(bitMask, new CopyOnWriteArraySet<MetricURISubscription>());
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.bitmask.AbstractBitMaskMatchContainer#match(java.lang.Number)
	 * Sets and Lists use <code> if (o.equals(elements[i])) </code> which means that:<ul>
	 * 	<li>turn off bits from incoming in each element to see if the element remains unchanged</li>
	 * 	<li>So if <i>this</i> is the element, we need to turn off <code>toMatch</code>'s bits in this key</li>
	 * 	<li>If this key is unchanged, then match is true, otherwise, false</li>
	 * </ul>
	 */
	@Override
	public boolean match(Long toMatch) {
		if(toMatch==null) return false;		
		return 0 != (this.key & toMatch);
	}
	
	/**
	 * Returns the primitive key
	 * @return the primitive key
	 */
	public long getMask() {
		return this.key;
	}

}
