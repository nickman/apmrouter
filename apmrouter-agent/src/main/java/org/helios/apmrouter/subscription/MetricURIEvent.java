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
package org.helios.apmrouter.subscription;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Title: MetricURIEvent</p>
 * <p>Description: Enumerates the metric URI subscription events</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.subscription.MetricURIEvent</code></p>
 */

public enum MetricURIEvent {
	/** A new metric entered the subscription membership */
	NEW_METRIC("metric.event.new"),
	/** A metric state-change in a metric already in this subscription membership */
	STATE_CHANGE("metric.event.statechange.state"),	
	/** A metric state-change added a metric to the subscription membership */
	STATE_CHANGE_ENTRY("metric.event.statechange.entry"),
	/** A metric state-change removed a metric from the subscription membership */
	STATE_CHANGE_EXIT("metric.event.statechange.exit"),	
	/** Data for a subscribed metric has been received */
	DATA("metric.event.data"),
	/** Synchronous MetricURI Op Response  */
	SYNCH_METRIC_URI_OP_RESP("metric.event.synch.response");	
	
	
	/** A decode map from the event name to the MetricURIEvent */
	public static final Map<String, MetricURIEvent> NAME2ENUM;
	/** A decode map from the ordinal to the MetricURIEvent */
	public static final Map<Byte, MetricURIEvent> ORD2ENUM;
	
	
	static {
		MetricURIEvent[] values = MetricURIEvent.values();
		Map<String, MetricURIEvent> tmp = new HashMap<String, MetricURIEvent>(values.length);
		Map<Byte, MetricURIEvent> btmp = new HashMap<Byte, MetricURIEvent>(values.length);
		for(MetricURIEvent ev: values) {
			tmp.put(ev.eventName, ev);
			btmp.put(ev.byteOrd, ev);
		}
		NAME2ENUM = Collections.unmodifiableMap(tmp);
		ORD2ENUM = Collections.unmodifiableMap(btmp);
	}
	
	private MetricURIEvent(String eventName) {
		this.eventName = eventName;
		byteOrd = (byte)ordinal();
	}
	
	private final String eventName;
	private final byte byteOrd;

	/**
	 * Returns the MetricURIEvent's ordinal as a byte 
	 * @return the MetricURIEvent's ordinal as a byte
	 */
	public byte getByteOrdinal() {
		return byteOrd;
	}
	
	/**
	 * Returns the event key for this event 
	 * @return the eventName
	 */
	public String getEventName() {
		return eventName;
	}
	
	/**
	 * Decodes the passed name to a MetriCURIEvent, trimming and lower-casing the passed string
	 * @param name The name to decode
	 * @return a MetriCURIEvent
	 */
	public static MetricURIEvent forEvent(CharSequence name) {
		if(name==null || name.toString().trim().isEmpty()) throw new IllegalArgumentException("The passed MetricURIEvent name was null or empty", new Throwable());
		name = name.toString().trim().toLowerCase();
		byte b = -1;
		try { b = Byte.parseByte(name.toString()); } catch (Exception ex) {}
		if(b!=-1) return forByteOrd(b);
		MetricURIEvent ev = NAME2ENUM.get(name);
		if(ev==null) throw new IllegalArgumentException("The passed MetricURIEvent name [" + name + "] was not a valid event name", new Throwable());
		return ev;
	}
	
	/**
	 * Decodes the passed byte ordinal to a MetriCURIEvent
	 * @param byteOrd The byte to decode
	 * @return a MetriCURIEvent
	 */
	public static MetricURIEvent forByteOrd(byte byteOrd) {
		MetricURIEvent ev = ORD2ENUM.get(byteOrd);
		if(ev==null) throw new IllegalArgumentException("The passed MetricURIEvent byte ordinal [" + byteOrd + "] was not a valid event byte ordinal", new Throwable());
		return ev;
	}
	
	
	
//	/** The JMX notification type for new metric events */
//	public static final String NEW_METRIC_EVENT = "metric.event.new";
//	/** The JMX notification type for new metric events */
//	public static final String STATE_CHANGE_METRIC_EVENT = "metric.event.statechange";
//	/** The JMX notification type for new metric events */
//	public static final String DATA_METRIC_EVENT = "metric.event.data";
	
}
