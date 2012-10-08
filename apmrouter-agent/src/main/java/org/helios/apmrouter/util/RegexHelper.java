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
package org.helios.apmrouter.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>Title: RegexHelper</p>
 * <p>Description: Regular expressions helper utility class</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.util.RegexHelper</code></p>
 */

public class RegexHelper {
	/** An array of reserved regex symbols as chars */
	public static final char[] RESERVED_CHARS = new char[]{'$','(',')','*','+','-','.','?','[','\\',']','^','{','|','}' };
	/** An array of reserved regex symbols as Characters */
	public static final Character[] RESERVED_CHARACTERS = new Character[]{'$','(',')','*','+','-','.','?','[','\\',']','^','{','|','}' };	
	/** An array of reserved regex symbols as strings */
	public static final String[] RESERVED_STRS = new String[]{"$","(",")","*","+","-",".","?","[","\\","]","^","{","|","}" };
	/** An searchable set of reserved regex symbols as strings */
	public static final Set<String> RESERVED_STR_SET = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(RESERVED_STRS)));
	/** An searchable set of reserved regex symbols as characters */
	public static final Set<Character> RESERVED_CHAR_SET = Collections.unmodifiableSet(new HashSet<Character>(Arrays.asList(RESERVED_CHARACTERS)));
	
	
	/**
	 * Indicates if the passed string is a regex reserved character.
	 * @param s the string to test
	 * @return true if the string is a single and reserved regex symbol
	 */
	public static boolean isReserved(String s) {
		if(s==null) return false;
		else return RESERVED_STR_SET.contains(s);
	}
	
	/**
	 * Indicates if the passed character is a regex reserved character.
	 * @param c the character to test
	 * @return true if the character is a single and reserved regex symbol
	 */
	public static boolean isReserved(char c) {
		return RESERVED_STR_SET.contains(new String(new char[]{c}));
	}
	
	/**
	 * Inspects a string for reserved regex characters. Note that the character may be escaped in the string.
	 * @param s the string to inspect
	 * @return true if the string contains at least one reserved regex character.
	 */
	public static boolean containsReserved(String s) {
		if(s==null || s.length()<1) return false;
		for(char c: s.toCharArray()) {
			if(RESERVED_CHAR_SET.contains(c)) return true;
		}
		return false;
	}
	
	/**
	 * Inspects a string for unescaped reserved regex characters.
	 * @param s the string to inspect
	 * @return true if the string contains at least one unescaped reserved regex character.
	 */
	public static boolean containsUnescapedReserved(String s) {
		if(s==null || s.length()<2) return false;
		char[] chars = s.toCharArray();
		for(int i = 1; i < chars.length; i++) {
			if(RESERVED_CHAR_SET.contains(chars[i]) && chars[i-1]!='\\') {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Inspects a string for escaped reserved regex characters.
	 * @param s the string to inspect
	 * @return true if the string contains at least one escaped reserved regex character.
	 */
	public static boolean containsEscapedReserved(String s) {
		if(s==null || s.length()<2) return false;
		char[] chars = s.toCharArray();
		for(int i = 1; i < chars.length; i++) {
			if(RESERVED_CHAR_SET.contains(chars[i]) && chars[i-1]=='\\') {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns the passed text with all unescaped regex symbols escaped.
	 * @param text The text to escape
	 * @return the escaped text
	 */
	public static String escapeUnescapedReserved(CharSequence text) {
		if(text==null) return null;
		if(text.length()<1) return text.toString();
		char[] chars = text.toString().toCharArray();
		if(chars.length==1) {
			if(RESERVED_CHAR_SET.contains(chars[0])) {
				return "\\" + chars[0];
			} else {
				return "" + chars[0];
			}
		}
		StringBuilder b = new StringBuilder(chars.length+10);
		for(int i = 1; i < chars.length; i++) {
			if(RESERVED_CHAR_SET.contains(chars[i]) && chars[i-1]!='\\') {
				b.append('\\').append(chars[i]);
			} else {
				b.append(chars[i]);
			}
		}
		return b.toString();		
	}
	
	

}
