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
package org.helios.apmrouter.router;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Pattern;

import org.helios.apmrouter.collections.ConcurrentLongSortedSet;

/**
 * <p>Title: PatternMatch</p>
 * <p>Description: A regex pattern matcher that caches hit and miss patterns for improved performance</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.router.PatternMatch</code></p>
 */

public class PatternMatch {
	/** The long hashcodes of routing keys that are known to match this PatternMatch */
	protected final ConcurrentLongSortedSet hits = new ConcurrentLongSortedSet();
	/** The long hashcodes of routing keys that are known to NOT match this PatternMatch */
	protected final ConcurrentLongSortedSet misses = new ConcurrentLongSortedSet();
	/** The pattern string for this pattern match */
	protected final String patternValue;
	/** The regex pattern for this pattern match */
	protected final Pattern pattern;
	
	/** A cache of created patterns */
	private static final Map<String, PatternMatch> PATTERNS = new ConcurrentHashMap<String, PatternMatch>();
	
	/**
	 * Returns the PatternMatch for the passed pattern
	 * @param patternValue The pattern for the PattrnMatch
	 * @return the PatternMatch for the passed pattern
	 */
	public static PatternMatch getInstance(String patternValue) {
		if(patternValue==null) throw new IllegalArgumentException("The passed pattern was null", new Throwable());
		PatternMatch pm = PATTERNS.get(patternValue);
		if(pm==null) {
			synchronized(PATTERNS) {
				pm = PATTERNS.get(patternValue);
				if(pm==null) {
					pm = new PatternMatch(patternValue);
					PATTERNS.put(patternValue, pm);
				}
			}
		}
		return pm;
	}
	
	/**
	 * Creates a new PatternMatch
	 * @param patternValue The pattern string for this pattern match
	 */
	private PatternMatch(String patternValue) {
		this.patternValue = patternValue;
		pattern = Pattern.compile(patternValue);
	}

	/**
	 * Indicates if the passed string matches this PatternMatch
	 * @param toMatch The value to match
	 * @return true if there is a match, false if there is no match or if the passed value is null
	 */
	public boolean matches(CharSequence toMatch) {
		if(toMatch==null) return false;
		long key = longHashCode(toMatch.toString());
		if(hits.contains(key)) return true;
		if(misses.contains(key)) return false;
		if(pattern.matcher(toMatch).matches()) {
			hits.add(key);
			return true;
		}
		misses.add(key);
		return false;
	}
	
	/**
	 * Returns the number of cached hits
	 * @return the number of cached hits
	 */
	public int getHitCount() {
		return hits.size();
	}
	
	/**
	 * Returns the number of cached misses
	 * @return the number of cached misses
	 */
	public int getMissCount() {
		return misses.size();
	}
	
	public static final PatternMatchGroup newPatternMatchGroup(CharSequence...patternValues) {
		PatternMatchGroup pmg = new PatternMatchGroup();
		for(CharSequence cs: patternValues) {
			if(cs==null) continue;
			pmg.add(PatternMatch.getInstance(cs.toString()));
		}
		return pmg;
	}
	
	public static class PatternMatchGroup {
		private final Set<PatternMatch> patternMatches = new CopyOnWriteArraySet<PatternMatch>();
		
		public boolean add(PatternMatch pm) {
			return patternMatches.add(pm);
		}
		
		public boolean add(CharSequence cs) {
			if(cs!=null) {
				return add(PatternMatch.getInstance(cs.toString()));
			}
			return false;
		}
		
		public String[] getPatterns() {
			Set<String> set = new HashSet<String>();
			for(Iterator<PatternMatch> iter = patternMatches.iterator(); iter.hasNext();) {
				set.add(iter.next().patternValue);
			}
			return set.toArray(new String[set.size()]);
			
		}
		
		public boolean remove(CharSequence cs) {
			if(cs==null) return false;
			String s = cs.toString();
			for(Iterator<PatternMatch> iter = patternMatches.iterator(); iter.hasNext();) {
				if(iter.next().patternValue.equals(s)) {
					iter.remove();
					return true;
				}
			}
			return false;
		}
		
		public int size() {
			return patternMatches.size();
		}
		
		public boolean matches(CharSequence cs) {
			if(cs==null) return false;
			for(PatternMatch pm: patternMatches) {
				if(pm.matches(cs)) return true;
			}
			return false;
		}
		
	}
	
	
	/**
	 * Calculates a low collision hash code for the passed string
	 * @param s The string to calculate the hash code for
	 * @return the long hashcode
	 */
	public static long longHashCode(String s) {
		long h = 0;
        int len = s.length();
    	int off = 0;
    	int hashPrime = s.hashCode();
    	char val[] = s.toCharArray();
        for (int i = 0; i < len; i++) {
            h = (31*h + val[off++] + (hashPrime*h));
        }
        return h;
	}
	
//	public static void main(String[] args) {
//		log("PatternMatch Test");
//		PatternMatchGroup pmg = newPatternMatchGroup(".*foo", ".*bar", ".*snafu", ".*haha");
//		log("PM Size:" + pmg.size());
//		log("Match for HeeHeefoo:" + pmg.matches("HeeHeefoo"));
//		log("Match for HeeHee:" + pmg.matches("HeeHee"));
//	}
//	
//	public static void log(Object msg) {
//		System.out.println(msg);
//	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((patternValue == null) ? 0 : patternValue.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		PatternMatch other = (PatternMatch) obj;
		if (patternValue == null) {
			if (other.patternValue != null) {
				return false;
			}
		} else if (!patternValue.equals(other.patternValue)) {
			return false;
		}
		return true;
	}




}
