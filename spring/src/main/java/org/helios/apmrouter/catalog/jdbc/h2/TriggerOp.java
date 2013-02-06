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
package org.helios.apmrouter.catalog.jdbc.h2;

import static org.helios.apmrouter.util.BitMaskedEnum.Support.generateIntMap;
import static org.helios.apmrouter.util.BitMaskedEnum.Support.getIntBitMask;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


import org.helios.apmrouter.util.BitMaskedEnum;

/**
 * <p>Title: TriggerOp</p>
 * <p>Description: Functional enum for H2 {@link org.h2.api.Trigger} operation types</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.catalog.jdbc.h2.TriggerOp</code></p>
 */

public enum TriggerOp implements BitMaskedEnum {
	/** The INSERT trigger operation */
	INSERT(1),

	/** The UPDATE trigger operation */
	UPDATE(2),

	/** The DELETE trigger operation */
	DELETE(4),

	/** The SELECT trigger operation */
	SELECT(8);
	
	/** A decoding map to decode the int code to a TriggerOp */
	public static final Map<Integer, TriggerOp> CODE2ENUM = generateIntMap(TriggerOp.values());

	
	private TriggerOp(int code) {
		this.code = code;
		mask = getIntBitMask(this);
	}
	
	/** The code for this op */
	private final int code;
	/** The mask for this op */
	private final int mask;


	/**
	 * Returns the code for this trigger op
	 * @return the code for this trigger op
	 */
	public int getCode() {
		return code;
	}
	
	/**
	 * Returns the mask for this state
	 * @return the mask for this state
	 */
	public int getMask() {
		return mask;
	}
	
	/**
	 * Determines if the passed mask is enabled for this TriggerOp
	 * @param mask the mask to test
	 * @return true if the passed mask is enabled for this TriggerOp, false otherwise
	 */
	public boolean isEnabled(int mask) {		
		return (mask | this.mask) == mask;
	}
	
	/**
	 * Enables the passed mask for this TriggerOp and returns it
	 * @param mask The mask to modify
	 * @return the modified mask
	 */
	public int enable(int mask) {
		return (mask | this.mask);
	}
	
	
	/**
	 * Returns an array of TriggerOps that enabled in the passed mask
	 * @param mask The masks to get the TriggerOps for 
	 * @return an array of TriggerOps that are enabled in the passed mask
	 */
	public static TriggerOp[] getEnabledStates(int mask) {
		Set<TriggerOp> enabled = new HashSet<TriggerOp>();
		for(TriggerOp t: values()) {
			if(t.isEnabled(mask)) enabled.add(t);
		}
		return enabled.toArray(new TriggerOp[enabled.size()]);
	}
	
	/**
	 * Returns a compound name representing all the TriggerOps that enabled in the passed mask
	 * @param mask The masks to get the TriggerOps for 
	 * @return a compound name representing all the TriggerOps that are enabled in the passed mask
	 */
	public static String getEnabledStatesName(int mask) {
		return Arrays.toString(getEnabledStates(mask)).replace("[", "").replace("]", "").replace(" ", "").replace(',', '|');
	}
	
	

}
