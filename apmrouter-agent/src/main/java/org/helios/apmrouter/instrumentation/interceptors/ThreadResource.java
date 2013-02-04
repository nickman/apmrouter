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
package org.helios.apmrouter.instrumentation.interceptors;

import java.util.Map;


import org.helios.apmrouter.nativex.TCPSocketState;
import org.helios.apmrouter.util.BitMaskedEnum;
import static org.helios.apmrouter.util.BitMaskedEnum.Support.*;

/**
 * <p>Title: ThreadResource</p>
 * <p>Description: A functional enumeration of metrics that can be collected on a thread in an interceptor.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.instrumentation.interceptors.ThreadResource</code></p>
 */

public enum ThreadResource implements BitMaskedEnum, BitMaskedEnum.ShortBitMaskOperations {
	/** The elapsed time in ms. */
	TIME_MS,
	/** The elapsed time in ns. */
	TIME_NS,
	/** The elapsed time in us. */
	TIME_US,	
	/** The consumed total cpu time in ns. */
	CPU,
	/** The consumed user mode cpu time in ns. */
	CPU_USER,
	/** The number of times the thread was blocked */
	BLOCK_COUNT,
	/** The total time the thread was blocked in ms. */
	BLOCK_TIME,
	/** The number of times the thread was blocked */
	WAIT_COUNT,	
	/** The total time the thread waited in ms. */
	WAIT_TIME;
	
	/** A decoding map to decode the NetFlag code to a TCPSocketState */
	public static final Map<Integer, ThreadResource> CODE2ENUM = generateIntMap(ThreadResource.values());
	
	
	private ThreadResource() {
		mask = getShortBitMask(this);
	}
	
	/** The binary mask for this ThreadResource */
	private final short mask;
	
	/**
	 * Returns the binary mask for this ThreadResource
	 * @return the binary mask for this ThreadResource
	 */
	public short getMask() {
		return mask;
	}

	public static void main(String[] args) {
		for(ThreadResource t: ThreadResource.values()) {
			log(t.name() + ":" + Integer.toBinaryString(t.mask));
		}
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.util.BitMaskedEnum.ShortBitMaskOperations#isEnabled(short)
	 */
	@Override
	public boolean isEnabled(short mask) {		
		return (mask | this.mask) == mask;	
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.util.BitMaskedEnum.ShortBitMaskOperations#enable(short)
	 */
	@Override
	public short enable(short mask) {
		return (short) (mask | this.mask);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.util.BitMaskedEnum.ShortBitMaskOperations#disable(short)
	 */
	@Override
	public short disable(short mask) {
		return (short) (mask & ~this.mask);
	}
	
	/**
	 * Accepts the passed int state and enables the passed enums on its bit mask 
	 * @param initial The initial state code
	 * @param enums The enums to enable
	 * @return the modified state code
	 */
	public static short enable(short initial, ThreadResource...enums) {
		return ShortBitMaskSupport.mask(initial, true, enums);
	}
	
	/**
	 * Creates an int state enabled for the passed enums on its bit mask 
	 * @param enums The enums to enable
	 * @return the state code
	 */
	public static short enable(ThreadResource...enums) {
		return ShortBitMaskSupport.mask((short)0, true, enums);
	}
	
	/**
	 * Accepts the passed int state and disables the passed enums on its bit mask 
	 * @param initial The initial state code
	 * @param enums The enums to disable
	 * @return the modified state code
	 */
	public static short disable(short initial, ThreadResource...enums) {
		return ShortBitMaskSupport.mask(initial, false, enums);
	}
	
	/**
	 * Creates an int state disabled for the passed enums on its bit mask 
	 * @param enums The enums to disable
	 * @return the state code
	 */
	public static short disable(ThreadResource...enums) {
		return ShortBitMaskSupport.mask((short)0, false, enums);
	}	
	
	
}
