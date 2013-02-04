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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.helios.apmrouter.nativex.TCPSocketState;

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
	public static class Support {
		/**
		 * Generates a long decode map for the passed enum type based on the ordinal
		 * @param offset A constant value to add to each enums ordinals for situations 
		 * where we're tracking a code that is an offset off the ordinal
		 * @param enums An array of the enum items
		 * @return a map of the enum items keyed by the bit mask
		 */
		public static <E extends Enum<E>> Map<Long, E> generateLongMap(int offset, E...enums) {
			if(enums.length>Long.SIZE) throw new IllegalArgumentException("Invalid number of enums for a long  bitmask [" + enums.length + "]. Max allowble is [" + Long.SIZE + "]");
			Map<Long, E> map = new HashMap<Long, E>(enums.length);
			for(E e: enums) {
				long mask = Long.parseLong("1" + BITS.substring(0, e.ordinal()+offset), 2);
				map.put(mask, e);
			}
			return Collections.unmodifiableMap(map);
		}
		
		/**
		 * Generates a long decode map for the passed enum type based on the ordinal 
		 * where we're tracking a code that is an offset off the ordinal
		 * @param enums An array of the enum items
		 * @return a map of the enum items keyed by the bit mask
		 */
		public static <E extends Enum<E>> Map<Long, E> generateLongMap(E...enums) {
			return generateLongMap(0, enums);
		}
		
		
		/**
		 * Generates an int  decode map for the passed enum type based on the ordinal
		 * @param offset A constant value to add to each enums ordinals for situations 
		 * where we're tracking a code that is an offset off the ordinal
		 * @param enums An array of the enum items
		 * @return a map of the enum items keyed by the bit mask
		 */ 
		public static <E extends Enum<E>> Map<Integer, E> generateIntMap(int offset, E...enums) {
			if(enums.length>Integer.SIZE) throw new IllegalArgumentException("Invalid number of enums for an integer bitmask [" + enums.length + "]. Max allowble is [" + Integer.SIZE + "]");
			Map<Integer, E> map = new HashMap<Integer, E>(enums.length);
			for(E e: enums) {
				int mask = Integer.parseInt("1" + BITS.substring(0, e.ordinal()+offset), 2);
				map.put(mask, e);
			}
			return Collections.unmodifiableMap(map);
		}
		
		/**
		 * Generates an int  decode map for the passed enum type based on the ordinal
		 * @param enums An array of the enum items
		 * @return a map of the enum items keyed by the bit mask
		 */ 
		public static <E extends Enum<E>> Map<Integer, E> generateIntMap(E...enums) {
			return generateIntMap(0, enums);
		}
		
		
		/**
		 * Generates a short decode map for the passed enum type based on the ordinal
		 * @param offset A constant value to add to each enums ordinals for situations 
		 * where we're tracking a code that is an offset off the ordinal
		 * @param enums An array of the enum items
		 * @return a map of the enum items keyed by the bit mask
		 */
		public static <E extends Enum<E>> Map<Short, E> generateShortMap(short offset, E...enums) {
			if(enums.length>Short.SIZE) throw new IllegalArgumentException("Invalid number of enums for a short bitmask [" + enums.length + "]. Max allowble is [" + Short.SIZE + "]");
			Map<Short, E> map = new HashMap<Short, E>(enums.length);
			for(E e: enums) {
				short mask = Short.parseShort("1" + BITS.substring(0, e.ordinal()+offset), 2);
				map.put(mask, e);
			}
			return Collections.unmodifiableMap(map);
		}
		
		/**
		 * Generates a short decode map for the passed enum type based on the ordinal
		 * @param enums An array of the enum items
		 * @return a map of the enum items keyed by the bit mask
		 */
		public static <E extends Enum<E>> Map<Short, E> generateShortMap(E...enums) {
			return generateShortMap((short)0, enums);
		}
		
		/**
		 * Generates a byte decode map for the passed enum type based on the ordinal
		 * @param offset A constant value to add to each enums ordinals for situations 
		 * where we're tracking a code that is an offset off the ordinal
		 * @param enums An array of the enum items
		 * @return a map of the enum items keyed by the bit mask
		 */
		public static <E extends Enum<E>> Map<Byte, E> generateByteMap(byte offset, E...enums) {
			if(enums.length>Byte.SIZE) throw new IllegalArgumentException("Invalid number of enums for a byte bitmask [" + enums.length + "]. Max allowble is [" + Byte.SIZE + "]");
			Map<Byte, E> map = new HashMap<Byte, E>(enums.length);
			for(E e: enums) {
				byte mask = Byte.parseByte("1" + BITS.substring(0, e.ordinal()+offset), 2);
				map.put(mask, e);
			}
			return Collections.unmodifiableMap(map);
		}
		
		/**
		 * Generates a byte decode map for the passed enum type based on the ordinal
		 * @param enums An array of the enum items
		 * @return a map of the enum items keyed by the bit mask
		 */
		public static <E extends Enum<E>> Map<Byte, E> generateByteMap(E...enums) {			
			return generateByteMap((byte)0, enums);
		}
		
		// ===============================================================================
		//    BitMask generators
		// ===============================================================================

		/**
		 * Generates the bitmask for the passed enum entry
		 * @param e the enum entry
		 * @return the bitmask
		 */
		public static <E extends Enum<E>> long getLongBitMask(E e) {
			return getBitMask(0L, e);
		}
		
		
		/**
		 * Generates the bitmask for the passed enum entry
		 * @param offset A constant value to add to each enums ordinals for situations 
		 * where we're tracking a code that is an offset off the ordinal
		 * @param e the enum entry
		 * @return the bitmask
		 */
		public static <E extends Enum<E>> long getBitMask(long offset, E e) {
			return Long.parseLong("1" + BITS.substring(0, (int)(e.ordinal()+offset)), 2);
		}
		
		
		/**
		 * Generates the bitmask for the passed enum entry
		 * @param e the enum entry
		 * @return the bitmask
		 */
		public static <E extends Enum<E>> int getIntBitMask(E e) {
			return getBitMask(0, e);
		}
		
		
		/**
		 * Generates the bitmask for the passed enum entry
		 * @param offset A constant value to add to each enums ordinals for situations 
		 * where we're tracking a code that is an offset off the ordinal
		 * @param e the enum entry
		 * @return the bitmask
		 */
		public static <E extends Enum<E>> int getBitMask(int offset, E e) {
			return Integer.parseInt("1" + BITS.substring(0, e.ordinal()+offset), 2);
		}
		
		
		/**
		 * Generates the short bitmask for the passed enum entry
		 * @param e the enum entry
		 * @return the bitmask
		 */
		public static <E extends Enum<E>> short getShortBitMask(E e) {
			return getBitMask((short)0, e);
		}
		
		
		/**
		 * Generates the short bitmask for the passed enum entry
		 * @param offset A constant value to add to each enums ordinals for situations 
		 * where we're tracking a code that is an offset off the ordinal
		 * @param e the enum entry
		 * @return the bitmask
		 */
		public static <E extends Enum<E>> short getBitMask(short offset, E e) {
			return Short.parseShort("1" + BITS.substring(0, e.ordinal()+offset), 2);
		}
		
		/**
		 * Generates the byte bitmask for the passed enum entry
		 * @param e the enum entry
		 * @return the bitmask
		 */
		public static <E extends Enum<E>> byte getByteBitMask(E e) {
			return getBitMask((byte)0, e);
		}
		
		
		/**
		 * Generates the bitmask for the passed enum entry
		 * @param offset A constant value to add to each enums ordinals for situations 
		 * where we're tracking a code that is an offset off the ordinal
		 * @param e the enum entry
		 * @return the bitmask
		 */
		public static <E extends Enum<E>> byte getBitMask(byte offset, E e) {
			return Byte.parseByte("1" + BITS.substring(0, e.ordinal()+offset), 2);
		}
		
	}
	
	/**
	 * <p>Title: ShortBitMaskOperations</p>
	 * <p>Description: Defines the bit mask operations for an <code>int</code> based bit masked enum</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.util.BitMaskedEnum.ShortBitMaskOperations</code></p>
	 * @param <E> The expected type of the enum
	 */
	public static interface ShortBitMaskOperations<E extends Enum<E>> {
		/**
		 * Determines if the passed mask is enabled for this enum entry
		 * @param mask the mask to test
		 * @return true if the passed mask is enabled for this enum state, false otherwise
		 */
		public boolean isEnabled(short mask);		
		
		/**
		 * Returns the mask for this enum entry
		 * @return the mask for this enum entry
		 */
		public short getMask();
		
		/**
		 * Enables the passed mask for this socket state and returns it
		 * @param mask The mask to modify
		 * @return the modified mask
		 */
		public short enable(short mask);
		
		/**
		 * Disables the passed mask for this socket state and returns it
		 * @param mask The mask to modify
		 * @return the modified mask
		 */
		public short disable(short mask);
		
		
	

	}
	
	/**
	 * <p>Title: ShortBitMaskSupport</p>
	 * <p>Description: Bit mask operation implementations for an <code>int</code> based bit masked enum</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.util.BitMaskedEnum.ShortBitMaskSupport</code></p>
	 */
	public static class ShortBitMaskSupport  {

		/**
		 * Accepts the passed int state and enables or disables the passed enums on it
		 * @param initial The initial state code
		 * @param enable true to enable, false to disable
		 * @param enums The enums to enable or disable
		 * @return the modified state code
		 */
		public static <E extends ShortBitMaskOperations<?>> short mask(short initial, final boolean enable, E...enums) {
			short mask = initial;
			if(enums!=null) {
				for(E en: enums) {
					if(en==null) continue;					
					mask = enable ? (short) (en.getMask() | mask) : (short) (en.getMask() & ~mask); 
				}
			}
			return mask;
		}
		
	}
}
