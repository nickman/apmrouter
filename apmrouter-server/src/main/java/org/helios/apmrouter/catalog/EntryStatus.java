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

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import org.helios.apmrouter.collections.ConcurrentLongSortedSet;
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
	ACTIVE((byte)1),
	/** The entry is stale and has not seen inserts within the stale window */
	STALE((byte)2),
	/** The entry is offline and has not seen inserts within one time series tier */
	OFFLINE((byte)4);
	
	/** A decode map for ByteMask -> EntryStatus */
	public static final Map<Byte, EntryStatus> MASK2ENUM = BitMaskedEnum.Support.generateByteMap(EntryStatus.values());
	
	private EntryStatus(byte mask) {
		this.mask = mask;
	}
	
	private final byte mask;
	
	/**
	 * Returns the byte ordinal for this EntryStatus
	 * @return the byte ordinal
	 */
	public byte byteOrdinal() {
		return (byte)ordinal();
	}
	
	public static void main(String[] args) {
		for(EntryStatus e: EntryStatus.values()) {
			System.out.println(e.name() + ":" + e.byteOrdinal());
		}
	}
	
	/**
	 * Returns the bit mask representing each of the entry state ordinals passed
	 * @param ordinals the oridnals to mask
	 * @return the mask
	 */
	public static byte getMaskFor(byte[] ordinals) {
		if(ordinals==null || ordinals.length==0) return 0;
		byte m = 0;
		for(byte o: ordinals) {
			m = (byte) (m | EntryStatus.forByte(o).mask);
		}
		return m;
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
	
	/**
	 * Decodes the passed int to an EntryStatus name
	 * @param code the into to decode
	 * @return the status name
	 */
	public static String decode(int code) {
		try {
			return EntryStatus.forByte((byte)code).name();			
		} catch (Exception ex) {
			return "INVALID CODE:" + code;
		}
	}
	
	/**
	 * Renders a state change summary from a change map
	 * @param changeMap the change map to render
	 * @return a string
	 */
	public static String renderStatusCounts(Map<EntryStatus, EntryStatusChange> changeMap) {
		StringBuilder b = new StringBuilder("State Change Summary:");
		for(Map.Entry<EntryStatus, EntryStatusChange> entry: changeMap.entrySet()) {
			b.append("\n\t").append(entry.getKey().name()).append(":").append(entry.getValue().getMetricIds().size());
		}
		return b.toString();
		
	}
	
	
	/**
	 * <p>Title: EntryChange</p>
	 * <p>Description: Represents an entry state being changed to, a timestamp and an array of metric ids that changed state.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.catalog.EntryStatusChange</code></p>
	 */
	public static class EntryStatusChange {
		private final EntryStatus toStatus;		
		private final long timestamp;
		private final ConcurrentLongSortedSet metricIds;
		
		
		/**
		 * Creates a new map of EntryStatusChanges keyed by the entry status
		 * @param timestamp the timestamp that the changes to be added were effective as of
		 * @return an EntryStatusChange map
		 */
		public static Map<EntryStatus, EntryStatusChange> getChangeMap(long timestamp) {
			return getChangeMap(timestamp, 2048);
		}
		
		/**
		 * Creates a new map of EntryStatusChanges keyed by the entry status
		 * @param timestamp the timestamp that the changes to be added were effective as of
		 * @param sizeEstimate the initial size of the metric id array
		 * @return an EntryStatusChange map
		 */
		public static Map<EntryStatus, EntryStatusChange> getChangeMap(long timestamp, int sizeEstimate) {
			Map<EntryStatus, EntryStatusChange> map = new EnumMap<EntryStatus, EntryStatusChange>(EntryStatus.class);
			for(EntryStatus es: EntryStatus.values()) {
				map.put(es, new EntryStatusChange(es, timestamp, sizeEstimate));
			}
			return map;
		}
		
		/**
		 * Creates a change map for a single state change
		 * @param timestamp The timestamp of the change
		 * @param metricId The metric id
		 * @param status the new status
		 * @return a singleton change map
		 */
		public static Map<EntryStatus, EntryStatusChange> getChangeMap(long timestamp, long metricId, EntryStatus status) {
			return new EnumMap<EntryStatus, EntryStatusChange>(Collections.singletonMap(status, new EntryStatusChange(status, timestamp, 1).appendMetricIds(metricId)));
		}
		
		
		
		/**
		 * Creates a new EntryStatusChange
		 * @param toStatus the status being transitioned to 
		 * @param timestamp the timestamp of the transition in a UTC long.
		 */
		private EntryStatusChange(EntryStatus toStatus, long timestamp) {
			this(toStatus, timestamp, 2048);
		}
		
		/**
		 * Creates a new EntryStatusChange
		 * @param toStatus the status being transitioned to 
		 * @param timestamp the timestamp of the transition in a UTC long.
		 * @param sizeEstimate the initial size of the metric id array
		 */
		private EntryStatusChange(EntryStatus toStatus, long timestamp, int sizeEstimate) {
			this.toStatus = toStatus;
			this.timestamp = timestamp;
			metricIds = new ConcurrentLongSortedSet(false, sizeEstimate);
		}
		
		/**
		 * Appends metric Ids to this state change 
		 * @param metricIds The metric ids to append
		 * @return this state change
		 */
		public EntryStatusChange appendMetricIds(long...metricIds) {
			this.metricIds.add(metricIds);
			return this;
		}
		

		/**
		 * Returns the status being transitioned to
		 * @return the status being transitioned to
		 */
		public EntryStatus getToStatus() {
			return toStatus;
		}


		/**
		 * Returns the time of the state change
		 * @return the time of the state change
		 */
		public long getTimestamp() {
			return timestamp;
		}

		/**
		 * Returns the metricIds that changed state
		 * @return the metricIds that changed state
		 */
		public ConcurrentLongSortedSet getMetricIds() {
			return metricIds;
		}
		
		/**
		 * Adds an array of metric ids to this state change
		 * @param metricIds an array of metric ids.
		 */
		public void addMetricIds(long...metricIds) {
			this.metricIds.add(metricIds);
		}
		
		
	}
}
















