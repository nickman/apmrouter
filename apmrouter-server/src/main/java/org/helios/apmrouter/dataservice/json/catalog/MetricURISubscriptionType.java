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
package org.helios.apmrouter.dataservice.json.catalog;

import static org.helios.apmrouter.util.BitMaskedEnum.Support.generateByteMaskMap;
import static org.helios.apmrouter.util.BitMaskedEnum.Support.generateByteOrdinalMap;

import java.util.Map;

import org.helios.apmrouter.catalog.jdbc.h2.TriggerOp;
import org.helios.apmrouter.util.BitMaskedEnum;
import org.helios.apmrouter.util.BitMaskedEnum.ByteBitMaskOperations;

/**
 * <p>Title: MetricURISubscriptionType</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.dataservice.json.catalog.MetricURISubscriptionType</code></p>
 */

public enum MetricURISubscriptionType implements BitMaskedEnum, ByteBitMaskOperations<MetricURISubscriptionType> {
	/** Subscribes to metric state change events */
	STATE_CHANGE(1),
	/** Subscribes to new metric events */
	NEW_METRIC(2),
	/** Subscribes to a data feed for the metrics */
	DATA(4);

	/** A decoding map to decode the ordinal code to a MetricURISubscriptionType */
	public static final Map<Byte, MetricURISubscriptionType> ORD2ENUM = generateByteOrdinalMap(MetricURISubscriptionType.values());
	/** A decoding map to decode the mask to matching TriggerOps */
	public static final Map<Byte, MetricURISubscriptionType> MASK2ENUM = generateByteMaskMap(MetricURISubscriptionType.values());
	
	private MetricURISubscriptionType(int mask) {
		this.mask = (byte)mask;
	}
	
	/** The code for this op */
	private final byte code = (byte)ordinal();
	/** The mask for this op */
	private final byte mask;
	
	public static void main(String[] args) {
		for(Map.Entry<Byte, MetricURISubscriptionType> to: MASK2ENUM.entrySet()) {
			System.out.println(to.getValue().name() + ":" + to.getKey());
		}
		System.out.println(getNamesFor((byte)enable(STATE_CHANGE, DATA)));
	}	

	/**
	 * Returns a string containing the pipe delimited names of the MetricURISubscriptionType that are enabled in the passed mask
	 * @param mask The mask to render
	 * @return a string containing the pipe delimited names of the MetricURISubscriptionType that are enabled in the passed mask
	 */
	public static String getNamesFor(byte mask) {
		if(mask<0) throw new IllegalArgumentException("Invalid mask value [" + mask + "]", new Throwable());
		StringBuilder b = new StringBuilder();		
		for(MetricURISubscriptionType to: values()) {
			if(to.isEnabled(mask)) {
				b.append(to.name()).append("|");
			}
		}
		if(b.length()<1) b.append("NONE");
		else b.deleteCharAt(b.length()-1);
		return b.toString();
	}	

	/**
	 * Returns the code for this trigger op
	 * @return the code for this trigger op
	 */
	public int getCode() {
		return code;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.util.BitMaskedEnum.ByteBitMaskOperations#getMask()
	 */
	public byte getMask() {
		return mask;
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.util.BitMaskedEnum.ByteBitMaskOperations#forOrdinal(byte)
	 */
	@Override
	public MetricURISubscriptionType forOrdinal(byte ordinal) {		
		MetricURISubscriptionType t = ORD2ENUM.get(ordinal);
		if(t==null) throw new IllegalArgumentException("The ordinal [" + ordinal + "] is invalid for MetricURISubscriptionTypes", new Throwable());
		return t;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.util.BitMaskedEnum.ByteBitMaskOperations#forCode(java.lang.Number)
	 */
	@Override
	public MetricURISubscriptionType forCode(Number code) {
		return forOrdinal(code.byteValue());
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.util.BitMaskedEnum.ByteBitMaskOperations#isEnabled(byte)
	 */
	@Override
	public boolean isEnabled(byte mask) {
		return mask == (mask | this.mask);		
	}



	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.util.BitMaskedEnum.ByteBitMaskOperations#enable(byte)
	 */
	@Override
	public byte enable(byte mask) {
		return (byte) (mask | this.mask);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.util.BitMaskedEnum.ByteBitMaskOperations#disable(byte)
	 */
	@Override
	public byte disable(byte mask) {		
		return (byte) (mask & ~this.mask);
	}
	
	/**
	 * Returns a byte masked to represent the enablement of each passed MetricURISubscriptionType
	 * @param initial The initial byte to modify
	 * @param enable true to enable, false to disable
	 * @param enums The MetricURISubscriptionTypes to enable or disable
	 * @return a byte masked to represent the enablement of each passed MetricURISubscriptionType
	 */
	public static byte mask(byte initial, final boolean enable, MetricURISubscriptionType...enums) {
		return ByteBitMaskSupport.mask(initial, enable, enums);		
	}
	
	/**
	 * Returns a mask enabled for the passed MetricURISubscriptionTypes
	 * @param enums The MetricURISubscriptionTypes to enable
	 * @return a mask enabled for the passed MetricURISubscriptionTypes
	 */
	public static byte enable(MetricURISubscriptionType...enums) {
		return mask((byte)0, true, enums);
	}
	
}
