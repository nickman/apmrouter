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

import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * <p>Title: Tier</p>
 * <p>Description: Model and parser for one timeseries tier that is a rollup of the timeseries tier below it. The bottom timeseries tier is <b><code>live</code></b>.</p>
 * <p>Shot codes for tier attributes<ul>
 * <li><b>p</b>:&nbsp;Period</li>
 * <li><b>d</b>:&nbsp;Duration</li>
 * <li><b>c</b>:&nbsp;Period Count</li>
 * <li><b>n</b>:&nbsp;Name</li>
 * </ul></p> 
 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.redis.ts.cor e.Tier</code></p>
 */

public class Tier implements TierMBean {
	/** The number of periods in this timeseries tier */
	protected long periodCount;
	/** The duration of one period in the timeseries tier */
	protected Duration periodDuration;
	/** The duration of one full tier rotation */
	protected Duration tierDuration;
	/** The name of this tier */
	protected String name;
	/** The level of the tier */
	protected int level = -1;
	/** The tier pattern */
	protected final String pattern;
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	/** The field codes for a tier definition */
	public static enum FieldCode {
		/** The period duration  */
		p,
		/** The tier duration */
		t, 
		/** The period count */
		c,
		/** The tier name */
		n;
	}
	/** The regex to parse a tier expression */
	public static final Pattern TIER_EXPR_REGEX = Pattern.compile("([p|t|c|n])=(?:(\\d+)([s|m|h|d|w])|(.*))", Pattern.CASE_INSENSITIVE | Pattern.COMMENTS);
	/** The regex to split a group of tier expressions */
	public static final Pattern TIER_GRP_REGEX = Pattern.compile(",");
	/**  The live ts tier name */ 
	public static final String LIVE_TIER = "live";
	/**  The default non-live  tier prefix */ 
	public static final String TIER_PREFIX = "t";	
	
	/**
	 * Creates a new Tier
	 * @param tierDef The tier definition expressions
	 * @param level The tier level 
	 * @return a new Tier
	 */
	public static Tier newTier(String tierDef, int level) {
		return new Tier(tierDef, level);
	}
	
	Tier(String tierDef, int level) {
		if(tierDef==null) throw new InvalidTierDefinitionException("The passed tier definition was null");
		tierDef = tierDef.trim().replace(" ", "").toLowerCase();
		pattern = tierDef;
		if(tierDef.isEmpty()) throw new InvalidTierDefinitionException("The passed tier definition was empty");
		if(level<0) throw new InvalidTierDefinitionException("The passed tier level [" + level + "] is invalid (<0)");
		if(level==0) {
			name = LIVE_TIER;
		}
		this.level = level;
		String[] expressions = TIER_GRP_REGEX.split(tierDef);		
		Map<String, Triplet> triplets = new LinkedHashMap<String, Triplet>(3); // we need 2 out of a possible 3 attributes to complete the tier definition
		Set<FieldCode> pending = EnumSet.of(FieldCode.p, FieldCode.t, FieldCode.c);
		for(String expression: expressions) {
			Matcher matcher = TIER_EXPR_REGEX.matcher(expression);
			if(!matcher.matches()) {
				throw new InvalidTierDefinitionException("The passed tier definition contained an invalid expression [" + expression + "]");				
			}
			long size = -1L;
			String unit = null;
			String name = null;
			String attr = matcher.group(1);
			pending.remove(FieldCode.valueOf(attr));
			if("n".equals(attr)) {
				name = matcher.group(4);
				if(name.isEmpty()) name = TIER_PREFIX + level;
			} else {
				
				if("c".equals(attr)) {
					size = Long.parseLong(matcher.group(4));
					if(size<1) throw new InvalidTierDefinitionException("The passed tier definition specified an invalid size (<1) [" + size + "]");
				} else {
					size = Long.parseLong(matcher.group(2));
					if(size<1) throw new InvalidTierDefinitionException("The passed tier definition specified an invalid size (<1) [" + size + "]");
					unit = matcher.group(3);
				}
				if(triplets.put(attr, new Triplet(attr, unit, size))!=null) {
					throw new InvalidTierDefinitionException("The passed tier definition contained duplicate attributes [" + tierDef + "]");
				}
			}
			if(triplets.size()==2) {
				twoTripletsValidate(triplets, pending.iterator().next());
			}
		}   //  End of triplet processing loop
 		if(name==null) {
			name = TIER_PREFIX + level;
		}
		if(triplets.size()<2) {
			throw new InvalidTierDefinitionException("The passed tier definition contained an insufficient number of attributes [" + tierDef + "]");
		}
		pending = EnumSet.of(FieldCode.p, FieldCode.t, FieldCode.c);
		// This is so we validate in the order set, so that if the 3rd value is derived, that should be the first to be checked.
		LinkedList<FieldCode> validationOrder = new LinkedList<FieldCode>();
		for(Triplet triplet: triplets.values()) {
			switch (triplet.fc) {
			case p:								
				periodDuration = new Duration(triplet.size, triplet.unit).refine();
				if(log.isDebugEnabled()) log.debug("Set p [" + periodDuration + "]");
				pending.remove(FieldCode.p);
				validationOrder.add(FieldCode.p);
				break;
			case t:				
				tierDuration = new Duration(triplet.size, triplet.unit).refine();
				if(log.isDebugEnabled()) log.debug("Set t [" + tierDuration + "]");
				pending.remove(FieldCode.t);
				validationOrder.add(FieldCode.t);
				break;				
			case c:				
				periodCount = triplet.size;
				if(log.isDebugEnabled()) log.debug("Set c [" + periodCount + "]");
				pending.remove(FieldCode.c);
				validationOrder.add(FieldCode.c);
			}
		}
		// If we only got 2 triplets, we need to calculate the third
		if(!pending.isEmpty()) {
			switch(pending.iterator().next()) {
			case p:
				periodDuration = new Duration(tierDuration.renderIn(TSUnit.SECONDS).size/periodCount, TSUnit.SECONDS).refine();
				if(log.isDebugEnabled()) log.debug("Calced p [" + periodDuration + "]");
				validationOrder.add(FieldCode.p);
				break;
			case t:
				tierDuration = new Duration(periodDuration.renderIn(TSUnit.SECONDS).size*periodCount, TSUnit.SECONDS).refine();
				if(log.isDebugEnabled()) log.debug("Calced t [" + tierDuration + "]");
				validationOrder.add(FieldCode.t);
				break;				
			case c:
				periodCount = tierDuration.renderIn(TSUnit.SECONDS).size / periodDuration.renderIn(TSUnit.SECONDS).size;
				if(log.isDebugEnabled()) log.debug("Calced c [" + periodCount + "]");
				validationOrder.add(FieldCode.c);
			}
		}
		validate(validationOrder);
	}
	
	/**
	 * Validates two triplets
	 * @param triplets The map containing the two triplets to validate
	 * @param thirdTriplet The third triplet which identifies the two triplets being validated
	 */
	protected void twoTripletsValidate(Map<String, Triplet> triplets, FieldCode thirdTriplet) {
		switch(thirdTriplet) {
		case p:
			if(log.isDebugEnabled()) log.debug("2Trip Validating t/c");
			long tierSize = triplets.get("t").size;
			long periodCount = triplets.get("c").size;
			if(periodCount%tierSize!=0) {
				throw new InvalidTierTripletPairException("The period count [" + periodCount + "] is not an integral multiple of the tier duration [" + tierSize + "]");
			}			
			
			break;
		case t:
			if(log.isDebugEnabled()) log.debug("2Trip Validating p/c");
			long periodSize = triplets.get("p").size;
			periodCount = triplets.get("c").size;
			if(periodCount%periodSize!=0) {
				throw new InvalidTierTripletPairException("The period count [" + periodCount + "] is not an integral multiple of the period duration [" + periodSize + "]");
			}			
			break;				
		case c:
			if(log.isDebugEnabled()) log.debug("2Trip Validating p/t");
			Duration p = new Duration(triplets.get("p").size, triplets.get("p").unit);
			Duration t = new Duration(triplets.get("t").size, triplets.get("t").unit);
			if(t.seconds%p.seconds!=0) {
				throw new InvalidTierTripletPairException("The tier duration [" + t + "] is not an integral multiple of the period duration [" + p + "]");
			}
		}		
	}
	
	/**
	 * Validates the calculated values for this tier.
	 * @param validationOrder A list of field codes to supply the order to validate in
	 */
	protected void validate(LinkedList<FieldCode> validationOrder) {
		for(Iterator<FieldCode> iter = validationOrder.descendingIterator(); iter.hasNext();) {
			FieldCode fc = iter.next();
			switch(fc) {
			case p:
				if(log.isDebugEnabled()) log.debug("Validating p");
				Duration pDur = new Duration(tierDuration.seconds/periodCount, TSUnit.SECONDS).refine();
				if(!pDur.equals(periodDuration)) {
					throw new IllegalTierStateException("Invalid Period Duration [" + periodDuration + "] for Tier Duration [" + tierDuration + "] and Period Count [" + periodCount + "]. Should be [" + pDur + "]");
				}
				break;
			case t:
				if(log.isDebugEnabled()) log.debug("Validating t");
				Duration tDur = new Duration(periodDuration.seconds*periodCount, TSUnit.SECONDS).refine();
				if(!tDur.equals(tierDuration)) {
					throw new IllegalTierStateException("Invalid Tier Duration [" + tierDuration + "] for Period Duration [" + periodDuration + "] and Period Count [" + periodCount + "]. Should be [" + tDur + "]");
				}
				break;				
			case c:
				if(log.isDebugEnabled()) log.debug("Validating c");
				long pCount = tierDuration.renderIn(TSUnit.SECONDS).size / periodDuration.renderIn(TSUnit.SECONDS).size;
				if(periodCount!=pCount) {
					throw new IllegalTierStateException("Invalid Period Count [" + periodCount + "] for Period Duration [" + periodDuration + "] and Tier Duration [" + tierDuration + "]. Should be [" + pCount + "]");
				}
			}
		}

		
	}
	
	
	/**
	 * Constructs a <code>String</code> with all attributes in <code>name:value</code> format.
	 * @return a <code>String</code> representation of this object.
	 */
	@Override
	public String toString() {
	    final String C = ",";
	    StringBuilder retValue = new StringBuilder();
	    retValue.append(FieldCode.n).append("=").append(this.name).append(C);
	    retValue.append(FieldCode.p).append("=").append(this.periodDuration).append(C);
	    retValue.append(FieldCode.t).append("=").append(this.tierDuration).append(C);
	    retValue.append(FieldCode.c).append("=").append(this.periodCount);
	    return retValue.toString();
	}
	
	public static void main(String[] args) {
		Logger.getLogger(Tier.class).setLevel(Level.DEBUG);
		log("Tier Test");
		//log(new Tier("p=60s,t=23d", 1));
		//log(new Tier("p=60s,c=33120", 1));
		log(new Tier("t=23d,c=33120", 1));

		
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	
	private static class Triplet {
		protected final FieldCode fc;
		protected final TSUnit unit;
		protected final long size;
		
		/**
		 * Creates a new Triplet
		 * @param fc
		 * @param unit
		 * @param size
		 */
		public Triplet(String attr, String unit, long size) {
			this.fc = FieldCode.valueOf(attr);
			this.size = size;
			this.unit = unit==null ? null : TSUnit.forCode(unit);			
		}
	}


	/**
	 * Returns the number of periods in the tier
	 * @return the number of periods in the tier
	 */
	public long getPeriodCount() {
		return periodCount;
	}

	/**
	 * Returns the duration of one period in the tier
	 * @return the duration of one period in the tier
	 */
	public Duration getPeriodDuration() {
		return periodDuration;
	}

	/**
	 * Returns the duration of the full tier
	 * @return the duration of the full tier
	 */
	public Duration getTierDuration() {
		return tierDuration;
	}

	/**
	 * Returns the name of the tier
	 * @return the name of the tier
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the level of tier within the time series model
	 * @return the level of tier within the time series model
	 */
	public int getLevel() {
		return level;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + level;
		result = prime * result
				+ ((periodDuration == null) ? 0 : periodDuration.hashCode());
		result = prime * result
				+ ((tierDuration == null) ? 0 : tierDuration.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Tier other = (Tier) obj;
		if (level != other.level)
			return false;
		if (periodDuration == null) {
			if (other.periodDuration != null)
				return false;
		} else if (!periodDuration.equals(other.periodDuration))
			return false;
		if (tierDuration == null) {
			if (other.tierDuration != null)
				return false;
		} else if (!tierDuration.equals(other.tierDuration))
			return false;
		return true;
	}

	/**
	 * Returns the tier definition pattern
	 * @return the tier definition pattern
	 */
	public String getPattern() {
		return pattern;
	}




	
}


