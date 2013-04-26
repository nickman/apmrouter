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
import static org.helios.apmrouter.util.Methods.nvl;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.helios.apmrouter.catalog.EntryStatus;
import org.helios.apmrouter.metric.MetricType;
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
	
	/** A bit mask of all statuses turned on */
	public static final byte ALL_SUB_TYPES_MASK;
	
	/** An array of all bit masks */
	private static final byte[] ALL_SUB_TYPE_MASKS;
	/** An array of all type ordinals */
	private static final byte[] ALL_SUB_TYPE_ORDS;
	
	
	static {
		MetricURISubscriptionType[] values = MetricURISubscriptionType.values();
		byte stat = 0;
		byte[] allStats = new byte[values.length];
		byte[] allOrds = new byte[values.length];
		for(int i = 0; i < values.length; i++) {
			allStats[i] = values[i].mask;
			allOrds[i] = values[i].code;
			stat = (byte)(stat | values[i].mask);
		}
		ALL_SUB_TYPES_MASK = stat;
		ALL_SUB_TYPE_MASKS = allStats;
		ALL_SUB_TYPE_ORDS = allOrds;
	}

	/**
	 * Returns an array of all status mask bytes
	 * @return an array of all status mask bytes
	 */
	public static byte[] getAllSubTypeMask() {
		return ALL_SUB_TYPE_MASKS.clone();
	}
	
	
	
	/**
	 * Returns an array of all status ordinals
	 * @return an array of all status ordinals
	 */
	public static byte[] getAllSubTypeOrdinals() {
		return ALL_SUB_TYPE_ORDS.clone();
	}
	
	
	/**
	 * Returns an array of all status mask bytes
	 * @return an array of all status mask bytes
	 */
	public static byte[] getAllSubTypeMasks() {
		return ALL_SUB_TYPE_MASKS.clone();
	}
	
	
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
	 * Returns an array of the MetricURISubscriptionTypes that are enabled in the passed mask
	 * @param mask The mask to render
	 * @return an array of the MetricURISubscriptionTypes that are enabled in the passed mask
	 */
	public static MetricURISubscriptionType[] getEnabledFor(byte mask) {
		if(mask<0) throw new IllegalArgumentException("Invalid mask value [" + mask + "]", new Throwable());
		Set<MetricURISubscriptionType> enabled = EnumSet.noneOf(MetricURISubscriptionType.class);
		for(MetricURISubscriptionType to: values()) {
			if(to.isEnabled(mask)) {
				enabled.add(to);
			}
		}
		return enabled.toArray(new MetricURISubscriptionType[enabled.size()]);
	}	
	

	/**
	 * Returns the code for this trigger op
	 * @return the code for this trigger op
	 */
	public byte getCode() {
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
	
	/**
	 * Returns a mask enabled for the passed MetricURISubscriptionType ordinals
	 * @param ordinals The MetricURISubscriptionType ordinals to enable
	 * @return a mask enabled for the passed MetricURISubscriptionType ordinals
	 */
	public static byte enable(byte...ordinals) {
		byte mask = 0;
		for(byte ordinal: ordinals) {
			mask = (byte) (mask | MetricURISubscriptionType.valueOf(ordinal).mask);
		}
		return mask;
	}
	
	
	/**
	 * Decodes the passed ordinal to a MetricURISubscriptionType.
	 * Throws a runtime exception if the ordinal is invalud
	 * @param ordinal The ordinal to decode
	 * @return the decoded MetricURISubscriptionType
	 */
	public static MetricURISubscriptionType valueOf(byte ordinal) {
		MetricURISubscriptionType t = ORD2ENUM.get(ordinal);
		if(t==null) throw new IllegalArgumentException("The passed ordinal [" + ordinal + "] was invalid for MetricURISubscriptionType", new Throwable());
		return t;
	}	
	
	/**
	 * Attempts to decode an arbitrary object into a MetricURISubscriptionTypes
	 * @param typeCode The arbitrary object to convert
	 * @return a MetricURISubscriptionTypes if successfully converted.
	 */
	public static MetricURISubscriptionType valueOf(Object typeCode) {
		if(typeCode==null) throw new IllegalArgumentException("The passed typeCode was null", new Throwable());
		if(typeCode instanceof MetricType) return (MetricURISubscriptionType)typeCode;
		if(typeCode instanceof Number) return valueOf(((Number)typeCode).byteValue());
		try { 
			byte type = Byte.parseByte(typeCode.toString().trim());
			return valueOf(type);
		} catch (NumberFormatException nfe) {/* No Op */}
		return valueOfName(typeCode.toString());
	}	
	
	/**
	 * Decodes the passed name to a MetricURISubscriptionType.
	 * Throws a runtime exception if the ordinal is invalid
	 * @param name The MetricURISubscriptionType name to decode. Trimmed and uppercased.
	 * @return the decoded MetricURISubscriptionType
	 */
	public static MetricURISubscriptionType valueOfName(CharSequence name) {
		String n = nvl(name, "MetricURISubscriptionType Name").toString().trim().toUpperCase();
		try {			
			return MetricURISubscriptionType.valueOf(n);
		} catch (Exception e) {
			byte id = -1;
			try { id = Byte.parseByte(n); } catch (Exception ex) {}
			if(id!=-1) {
				return MetricURISubscriptionType.valueOf(id);
			}
			throw new IllegalArgumentException("The passed name [" + name + "] is not a valid MetricURISubscriptionType name", new Throwable());
		}
	}	
}
