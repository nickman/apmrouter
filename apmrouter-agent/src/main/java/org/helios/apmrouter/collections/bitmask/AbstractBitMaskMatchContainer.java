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
 * <p>Title: AbstractBitMaskMatchContainer</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.collections.bitmask.AbstractBitMaskMatchContainer</code></p>
 * @param <T> The numeric type representing the bit mask
 * @param <K> The arbitrary object associated with the bit mask
 */

public abstract class AbstractBitMaskMatchContainer<K extends Number, T> implements BitMaskMatchContainer<K, T> {
	/** The bit mask key */
	protected final K key;
	/** The keyed value */
	protected final T value;
	/**
	 * Creates a new AbstractBitMaskMatchContainer
	 * @param key The bit mask key
	 * @param value The keyed value
	 */
	public AbstractBitMaskMatchContainer(K key, T value) {
		this.key = key;
		this.value = value;
	}
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.bitmask.BitMaskMatchContainer#match(java.lang.Number)
	 */
	@Override
	public abstract boolean match(K toMatch);
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object obj) {
		if(obj==null) return false;
		if(this==obj) return true;
		if(obj instanceof Number) {
			return this.key.equals(obj);
		} else if(obj instanceof BitMaskMatchContainer) {
			Number toMatch = ((BitMaskMatchContainer<? extends Number, ?>)obj).getBitMaskKey();
			return this.match((K) toMatch);
		}
		return false;		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.bitmask.BitMaskMatchContainer#getBitMaskKey()
	 */
	@Override
	public Number getBitMaskKey() {
		return key;
	}
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.collections.bitmask.BitMaskMatchContainer#getValue()
	 */
	@Override
	public T getValue() {
		return value;
	}
	
	
}
