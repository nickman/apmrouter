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
package org.helios.apmrouter.tsmodel;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Title: TSUnit</p>
 * <p>Description: An extended analog of {@link java.util.concurrent.TimeUnit} that starts at SECONDS adds an extra member called WEEK and provides decodes for short names. </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.tsmodel.TSUnit</code></p>
 */

public enum TSUnit  {
	/** The seconds TSUnit */
	SECONDS(1L, "S") {
		@Override
		public long convertToSeconds(long value) { return value;};
		@Override
		public long convertToMinutes(long value) { return value/60;};
		@Override
		public long convertToHours(long value) { return value/60/60;};
		@Override
		public long convertToDays(long value) { return value/60/60/24;};
		@Override
		public long convertToWeeks(long value) { return value/60/60/24/7;};
		@Override
		public long convert(long d, TSUnit u) { return u.convertToSeconds(d); }
	},
	/** The minutes TSUnit */
	MINUTES(60L, "M"){
		@Override
		public long convertToSeconds(long value) { return value*60;};
		@Override
		public long convertToMinutes(long value) { return value;};
		@Override
		public long convertToHours(long value) { return value/60;};
		@Override
		public long convertToDays(long value) { return value/60/24;};
		@Override
		public long convertToWeeks(long value) { return value/60/24/7;};
		@Override
		public long convert(long d, TSUnit u) { return u.convertToMinutes(d); }
	},
	/** The hours TSUnit */
	HOURS(3600L, "H") {
		@Override
		public long convertToSeconds(long value) { return value*60*60;};
		@Override
		public long convertToMinutes(long value) { return value*60;};
		@Override
		public long convertToHours(long value) { return value;};
		@Override
		public long convertToDays(long value) { return value/24;};
		@Override
		public long convertToWeeks(long value) { return value/24/7;};
		@Override
		public long convert(long d, TSUnit u) { return u.convertToHours(d); }
	},
	/** The days TSUnit */
	DAYS(86400L, "D"){
		@Override
		public long convertToSeconds(long value) { return value*60*60*24;};
		@Override
		public long convertToMinutes(long value) { return value*60*24;};
		@Override
		public long convertToHours(long value) { return value*24;};
		@Override
		public long convertToDays(long value) { return value;};
		@Override
		public long convertToWeeks(long value) { return value/7;};
		@Override
		public long convert(long d, TSUnit u) { return u.convertToDays(d); }
	},
	/** The weeks TSUnit */
	WEEKS(86400L*7, "W"){
		@Override
		public long convertToSeconds(long value) { return value*60*60*24*7;};
		@Override
		public long convertToMinutes(long value) { return value*60*24*7;};
		@Override
		public long convertToHours(long value) { return value*24*7;};
		@Override
		public long convertToDays(long value) { return value*7;};
		@Override
		public long convertToWeeks(long value) { return value;};
		@Override
		public long convert(long d, TSUnit u) { return u.convertToDays(d); }		
	};
	
	private static final Map<String, TSUnit> CODE2TSUNIT = new HashMap<String, TSUnit>(TSUnit.values().length*2);
	private static final Map<Integer, TSUnit> ORD2TSUNIT = new HashMap<Integer, TSUnit>(TSUnit.values().length);
	
	static {
		for(TSUnit ts: TSUnit.values()) {
			CODE2TSUNIT.put(ts.shortCode, ts);
			CODE2TSUNIT.put(ts.shortCode.toLowerCase(), ts);
			ORD2TSUNIT.put(ts.ordinal(), ts);
		}
	}
	
	/**
	 * Determines if the passed code is a valid short name regardless of case
	 * @param code The code to test
	 * @return true if the code is a valid code, false if it is not.
	 */
	public static boolean isValidCode(String code) {
		if(code==null) return false;
		return CODE2TSUNIT.containsKey(code);
	}
	
	/**
	 * Returns the matching TSUnit for the passed short code. The passed code is trimmed.
	 * @param code The short code to get the TSUnit for
	 * @return the matching TSUnit
	 */
	public static TSUnit forCode(String code) {
		if(code==null) throw new IllegalArgumentException("The passed code was null", new Throwable());
		code = code.trim();
		TSUnit ts = CODE2TSUNIT.get(code);
		if(ts==null) throw new IllegalArgumentException("The passed code [" + code + "] was not a valid TSUnit short code", new Throwable());
		return ts;
	}
	
	/**
	 * Determines if there is a higher unit than this one
	 * @return true if there is a higher unit than this one, false otherwise
	 */
	public boolean hasHigher() {
		return ORD2TSUNIT.containsKey(this.ordinal()+1);
	}
	
	/**
	 * Determines if there is a lower unit than this one
	 * @return true if there is a lower unit than this one, false otherwise
	 */
	public boolean hasLower() {
		return ORD2TSUNIT.containsKey(this.ordinal()-1);
	}
	
	/**
	 * Returns the next highest unit
	 * @return the next highest unit or null if there is no higher unit
	 */
	public TSUnit getHigher() {
		return ORD2TSUNIT.get(this.ordinal()+1);
	}
	
	/**
	 * Returns the next lowest unit
	 * @return the next lowest unit or null if there is no lower unit
	 */
	public TSUnit getLower() {
		return ORD2TSUNIT.get(this.ordinal()-1);
	}
	
	/**
	 * Returns the number of this unit's in the passed unit.
	 * Throws an exception if this unit is larger than the passed unit.
	 * @param unit The unit to determine the conversion for
	 * @return The number of this unit's in the passed unit
	 */
	public long conversion(TSUnit unit) {
		if(this.ordinal()>unit.ordinal()) throw new IllegalArgumentException("Invalid conversion [" + name() + "-->" + unit.name() + "]. This unit is larger than the passed unit", new Throwable());
		return unit.secs / this.secs;
	}
	
	
	
	/**
	 * Attempts to create an equivalently sized duration in a higher unit
	 * @param size The size of the duration to refine
	 * @return the refined duration or the same duration if it could not be refined
	 */
	public Duration refine(long size) {
		TSUnit higher = getHigher();
		if(higher==null) return new Duration(size, this);
		if(mod(size, conversion(higher))==0) {
			Duration d = new Duration(size/conversion(higher), higher);
			return d.refine();
		} 
		return new Duration(size, this);
	}
	
	private double mod(double d, double mod) {
		return d%mod;
	}
	
	/**
	 * Creates a new TSUnit
	 * @param secs The number of seconds in one unit of this TSUnit
	 * @param shortCode The short code for this unit
	 */
	private TSUnit(long secs, String shortCode) {
		this.secs= secs;
		this.shortCode = shortCode; 
	}
	
	/** The number of seconds in this TSUnit */
	public final long secs;
	/** The short name of this TSUnit */
	public final String shortCode;
	
	public long convert(long sourceValue, TSUnit sourceUnit) {
		throw new AbstractMethodError();
	}
	
	public static void main(String[] args) {
		log("TSUnit Test");
		log("6 Hours in Minutes:" + TSUnit.HOURS.convert(6, TSUnit.MINUTES));		
		log("90 Hours in Minutes:" + TSUnit.HOURS.convert(90, TSUnit.MINUTES));
		log("120 Minutes in Hours:" + TSUnit.MINUTES.convert(120, TSUnit.HOURS));
		log("Minutes in Day:" + TSUnit.MINUTES.conversion(TSUnit.DAYS));
		log("Refine 1440 Minutes:" + TSUnit.MINUTES.refine(1440));
		log("Render 15 Minutes as Seconds:" + TSUnit.MINUTES.convert(15, TSUnit.SECONDS));
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	public long convertToSeconds(long value) {throw new AbstractMethodError();}
	public long convertToMinutes(long value) {throw new AbstractMethodError();};
	public long convertToHours(long value) {throw new AbstractMethodError();};
	public long convertToDays(long value) {throw new AbstractMethodError();};
	public long convertToWeeks(long value) {throw new AbstractMethodError();};
	
	
}
