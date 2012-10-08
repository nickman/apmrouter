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
package org.helios.apmrouter.jmx;

/**
 * <p>Title: StringHelper</p>
 * <p>Description: String helper utility class</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.jmx.StringHelper</code></p>
 */
public class StringHelper {
	

	/**
	 * Acquires and truncates the current thread's StringBuilder.
	 * @return A truncated string builder for use by the current thread.
	 */
	public static StringBuilder getStringBuilder() {
		return new StringBuilder();
	}	
	
	/**
	 * Escapes quote characters in the passed string
	 * @param s The string to esacape
	 * @return the escaped string
	 */
	public static String escapeQuotes(CharSequence s) {
		return s.toString().replace("\"", "\\\"");
	}
	
	/**
	 * Escapes json characters in the passed string
	 * @param s The string to esacape
	 * @return the escaped string
	 */
	public static String jsonEscape(CharSequence s) {
		return s.toString().replace("\"", "\\\"").replace("[", "\\[").replace("]", "\\]").replace("{", "\\{").replace("}", "\\}");
	}
	
	
	/**
	 * Acquires and truncates the current thread's StringBuilder.
	 * @param size the inited size of the stringbuilder
	 * @return A truncated string builder for use by the current thread.
	 */
	public static StringBuilder getStringBuilder(int size) {
		return new StringBuilder(size);
	}
	
	/**
	 * Concatenates all the passed strings
	 * @param args The strings to concatentate
	 * @return the concatentated string
	 */
	public static String fastConcat(CharSequence...args) {
		StringBuilder buff = getStringBuilder();
		for(CharSequence s: args) {
			if(s==null) continue;
			buff.append(s);
		}
		return buff.toString();
	}
	
	/**
	 * Accepts an array of strings and returns the array flattened into a single string, optionally delimeted.
	 * @param skipBlanks If true, blank or null items in the passed array will be skipped.
	 * @param delimeter The delimeter to insert between each item.
	 * @param args The string array to flatten
	 * @return the flattened string
	 */
	public static String fastConcatAndDelim(boolean skipBlanks, String delimeter, CharSequence...args) {
		StringBuilder buff = getStringBuilder();
		if(args!=null && args.length > 0) {			
			for(CharSequence s: args) {				
				if(!skipBlanks || (s!=null && s.length()>0)) {
					buff.append(s).append(delimeter);
				}
			}
			if(buff.length()>0) {
				buff.deleteCharAt(buff.length()-1);
			}
		}
		return buff.toString();
	}
	
	/**
	 * Accepts an array of strings and returns the array flattened into a single string, optionally delimeted.
	 * Blank or zero length items in the array will be skipped.
	 * @param delimeter The delimeter to insert between each item.
	 * @param args The string array to flatten
	 * @return the flattened string
	 */
	public static String fastConcatAndDelim(String delimeter, CharSequence...args) {
		return fastConcatAndDelim(true, delimeter, args);
	}
	
	/**
	 * Accepts an array of strings and returns the array flattened into a single string, optionally delimeted.
	 * @param skip Skip this many
	 * @param delimeter The delimeter
	 * @param args The strings to concat
	 * @return the resulting string
	 */
	public static String fastConcatAndDelim(int skip, String delimeter, CharSequence...args) {
		StringBuilder buff = getStringBuilder();
		int cnt = args.length - skip;
		int i = 0;
		for(; i < cnt; i++) {
			if(args[i] != null && args[i].length() > 0) {
				buff.append(args[i]).append(delimeter);
			}
		}
		StringBuilder b = buff.reverse();
		while(b.subSequence(0, delimeter.length()).equals(delimeter)) {
			b.delete(0, delimeter.length());
		}
		return b.reverse().toString();
	}	

}
