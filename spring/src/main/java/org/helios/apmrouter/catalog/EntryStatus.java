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
package org.helios.apmrouter.catalog;

import java.util.Map;

import org.helios.apmrouter.util.BitMaskedEnum;

/**
 * <p>Title: EntryStatus</p>
 * <p>Description: The status of an entry with respect to the Up/Down state of the metric</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.chronicletimeseries.EntryStatus</code></p>
 */

public enum EntryStatus {
	/** The entry is active and has had recent inserts */
	ACTIVE,
	/** The entry is stale and has not seen inserts within the stale window */
	STALE,
	/** The entry is offline and has not seen inserts within one time series tier */
	OFFLINE;
	
	/** A decode map for Byte -> EntryStatus */
	public static final Map<Byte, EntryStatus> ORD2ENUM = BitMaskedEnum.Support.generateByteMap(EntryStatus.values());
	
	/**
	 * Returns the byte ordinal for this EntryStatus
	 * @return the byte ordinal
	 */
	public byte byteOrdinal() {
		return (byte)ordinal();
	}
	
	
	/**
	 * Returns the EntryStatus with the specified byte ordinal
	 * @param b The byte ordinal to get an EntryStatus for 
	 * @return an EntryStatus 
	 */
	public static EntryStatus forByte(byte b) {
		switch(b) {
			case 0: return ACTIVE;
			case 1: return STALE;
			case 2: return OFFLINE;
			default: throw new IllegalArgumentException("Invalid byte [" + b + "]", new Throwable());
				
		}
	}
}
