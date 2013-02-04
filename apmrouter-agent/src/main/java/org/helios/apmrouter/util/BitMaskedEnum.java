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

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Title: BitMaskedEnum</p>
 * <p>Description: Support interface for bit masked enums</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.util.BitMaskedEnum</code></p>
 */

public interface BitMaskedEnum {
	/** An all zeroes bit mask template */
	public static final String BITS = "0000000000000000000000000000000000000000000000000000000000000000";
	
	/**
	 * <p>Title: DecodeMapPopulator</p>
	 * <p>Description: </p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.util.BitMaskedEnum.DecodeMapPopulator</code></p>
	 */
	public static class Support<E extends Enum<E>> {
		/**
		 * Generates a long decode map for the passed enum type based on the ordinal
		 * @param enums An array of the enum items
		 * @return a map of the enum items keyed by the bit mask
		 */
		public static <E extends Enum<E>> Map<Long, E> generateLongMap(E...enums) {
			if(enums.length>Long.SIZE) throw new IllegalArgumentException("Invalid number of enums for a long  bitmask [" + enums.length + "]. Max allowble is [" + Long.SIZE + "]");
			Map<Long, E> map = new HashMap<Long, E>(enums.length);
			for(E e: enums) {
				long mask = Long.parseLong("1" + BITS.substring(0, e.ordinal()), 2);
				map.put(mask, e);
			}
			return map;
		}
		
		/**
		 * Generates an int  decode map for the passed enum type based on the ordinal
		 * @param enums An array of the enum items
		 * @return a map of the enum items keyed by the bit mask
		 */ 
		public static <E extends Enum<E>> Map<Integer, E> generateIntMap(E...enums) {
			if(enums.length>Integer.SIZE) throw new IllegalArgumentException("Invalid number of enums for an integer bitmask [" + enums.length + "]. Max allowble is [" + Integer.SIZE + "]");
			Map<Integer, E> map = new HashMap<Integer, E>(enums.length);
			for(E e: enums) {
				int mask = Integer.parseInt("1" + BITS.substring(0, e.ordinal()), 2);
				map.put(mask, e);
			}
			return map;
		}
		
		/**
		 * Generates a short decode map for the passed enum type based on the ordinal
		 * @param enums An array of the enum items
		 * @return a map of the enum items keyed by the bit mask
		 */
		public static <E extends Enum<E>> Map<Short, E> generateShortMap(E...enums) {
			if(enums.length>Short.SIZE) throw new IllegalArgumentException("Invalid number of enums for a short bitmask [" + enums.length + "]. Max allowble is [" + Short.SIZE + "]");
			Map<Short, E> map = new HashMap<Short, E>(enums.length);
			for(E e: enums) {
				short mask = Short.parseShort("1" + BITS.substring(0, e.ordinal()), 2);
				map.put(mask, e);
			}
			return map;
		}
		
		/**
		 * Generates the short bitmask for the passed enum entry
		 * @param e the enum entry
		 * @return the short bitmask
		 */
		public static <E extends Enum<E>> short getBitMask(E e) {
			return Short.parseShort("1" + BITS.substring(0, e.ordinal()), 2);
		}
		
		/**
		 * Generates a byte decode map for the passed enum type based on the ordinal
		 * @param enums An array of the enum items
		 * @return a map of the enum items keyed by the bit mask
		 */
		public static <E extends Enum<E>> Map<Byte, E> generateByteMap(E...enums) {
			if(enums.length>Byte.SIZE) throw new IllegalArgumentException("Invalid number of enums for a byte bitmask [" + enums.length + "]. Max allowble is [" + Byte.SIZE + "]");
			Map<Byte, E> map = new HashMap<Byte, E>(enums.length);
			for(E e: enums) {
				byte mask = Byte.parseByte("1" + BITS.substring(0, e.ordinal()), 2);
				map.put(mask, e);
			}
			return map;
		}
	}
}
