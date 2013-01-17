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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringBufferInputStream;
import java.lang.reflect.Array;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Title: StringHelper</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.util.StringHelper</code></p>
 */

public class StringHelper {
	/** Regex pattern that defines a range of numbers */
	protected static final Pattern intRange = Pattern.compile("([\\d+]+)-([\\d+]+)");
	/** Regex pattern that defines a range of numbers with a wildcard terminator */
	protected static final Pattern endRange = Pattern.compile("([\\d+]+)-\\*");
	/** A thread local to hold a StringBuilder for thread safe high speed string appending */
	protected static ThreadLocal<StringBuilder> buffer = new ThreadLocal<StringBuilder>();
	
	/** Cleans a string value before conversion to an integral */
	public static final Pattern CLEAN_INTEGRAL = Pattern.compile("\\..*|[\\D&&[^\\-]]");
	
	/**
	 * Cleans a string value of all non numerics and anything after (and including) the decimal point
	 * @param number A number text
	 * @return A clean integral string
	 */
	public static String cleanNumber(CharSequence number) {
		return CLEAN_INTEGRAL.matcher(number).replaceAll("");
	}
	
	/**
	 * Acquires and truncates the current thread's StringBuilder.
	 * @return A truncated string builder for use by the current thread.
	 */
	public static StringBuilder getStringBuilder() {
		StringBuilder sb = buffer.get();
		if(sb==null) {
			sb = new StringBuilder();
			buffer.set(sb);
		}
		sb.setLength(0);
		return sb;
	}	
	
	
	/**
	 * Formats the stack trace of the passed throwable and generates a formatted string.
	 * @param t The throwable
	 * @return A string representing the stack trace.
	 */
	public static String formatStackTrace(Throwable t) {
		if(t==null) return "";
		StackTraceElement[] stacks = t.getStackTrace();
		StringBuilder b = new StringBuilder(stacks.length * 50);
		for(StackTraceElement ste: stacks) {
			b.append("\n\t").append(ste.toString());
		}
		return b.toString();
	}
	
	/**
	 * Formats the stack trace of the passed thread and generates a formatted string.
	 * @param t The thread
	 * @return A string representing the stack trace of the passed thread
	 */
	public static String formatStackTrace(Thread t) {
		if(t==null) return "";
		StackTraceElement[] stacks = t.getStackTrace();
		StringBuilder b = new StringBuilder(stacks.length * 50);
		for(StackTraceElement ste: stacks) {
			b.append("\n\t").append(ste.toString());
		}
		return b.toString();
	}
	
	
	
	/**
	 * Acquires and truncates the current thread's StringBuilder.
	 * @return A truncated string builder for use by the current thread.
	 */
	public static StringBuilder getStringBuilder(int size) {
		StringBuilder sb = buffer.get();
		if(sb==null) {
			sb = new StringBuilder();
			buffer.set(sb);
		}
		sb.setLength(0);
		sb.setLength(size);
		return sb;
	}
	
	public static String fastConcat(String...args) {
		StringBuilder buff = getStringBuilder();
		for(String s: args) {
			buff.append(s);
		}
		return buff.toString();
	}
	
	/**
	 * Accepts an array of strings and returns the array flattened into a single string, optionally delimeted.
	 * @param skipBlanks If true, blank or null items in the passed array will be skipped.
	 * @param delimiter The delimeter to insert between each item.
	 * @param args The string array to flatten
	 * @return the flattened string
	 */
	public static String fastConcatAndDelim(boolean skipBlanks, String delimiter, String...args) {
		StringBuilder buff = getStringBuilder();
		if(args!=null && args.length > 0) {
			for(String s: args) {
				if(!skipBlanks || (s!=null && s.length()>0)) {
					buff.append(delimiter).append(s);
				}
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
	public static String fastConcatAndDelim(String delimeter, String...args) {
		return fastConcatAndDelim(true, delimeter, args);
	}
	
	public static String fastConcatAndDelim(int skip, String delimeter, String...args) {
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
		//if(i>0) buff.deleteCharAt(buff.length()-delimeter.length());
		return b.reverse().toString();
	}	
	
	@SuppressWarnings("deprecation")
	public static Properties toProperties(String props) {
		Properties p = new Properties();
		StringBufferInputStream sbi = new StringBufferInputStream(props);
		try {
			p.load(sbi);
		} catch (IOException e) {
			throw new RuntimeException("Unexpected Exception Converting String to Properties", e);
		}
		return p;
	}
	
	/**
	 * Replaces all instances of the passed target String with the replacement String in the passed file.
	 * @param fileName
	 * @param targetString
	 * @param replacementString
	 */
	public static void replaceStringInFile(String fileName, String targetString, String replacementString) {
		FileReader fileReader = null;
		FileWriter fileWriter = null;
		BufferedReader bufferedReader = null;
		BufferedWriter bufferedWriter = null;
		try {
			File file = new File(fileName);
			fileReader = new FileReader(file);
			bufferedReader = new BufferedReader(fileReader);
			StringBuilder buff = new StringBuilder((int)file.length());
			char[] charBuff = new char[8092];
			while(true) {
				int charsRead = bufferedReader.read(charBuff);
				if(charsRead==-1) break;
				buff.append(charBuff, 0, charsRead);
			}
			bufferedReader.close();			
			String output = buff.toString().replace(targetString, replacementString);
			fileWriter = new FileWriter(file, false);
			bufferedWriter = new BufferedWriter(fileWriter);
			bufferedWriter.write(output);
			bufferedWriter.flush(); 
			bufferedWriter.close(); 
		} catch (Exception e) {
			throw new RuntimeException("Failed to replaceStringInFile for " + fileName, e);
		} finally {
			try { bufferedWriter.flush(); } catch (Exception e) {}
			try { bufferedWriter.close(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Adds the passed new values to the end of the passed array.
	 * Null values are dropped from both arrays.
	 * @param array The prefix array.
	 * @param newValues The array to be appended.
	 * @return A newly sized or created array.
	 */
	@SuppressWarnings("unchecked")
	public static <E> E[] append(E[] array, E...newValues) {
		return append(array, true, newValues);		
	}
	
	/**
	 * Adds the passed new values to the end of the passed array.
	 * @param array The prefix array.
	 * @param dropNulls Indicates if null array elements from either should be dropped.
	 * @param newValues The array to be appended.
	 * @return A newly sized or created array.
	 */
	@SuppressWarnings("unchecked")
	public static <E> E[] append(E[] array, boolean dropNulls, E...newValues) {
		try {
			if(newValues==null || newValues.length < 1) return (E[]) (dropNulls ? Array.newInstance(array.getClass().getComponentType(), 0) :array);
			if(array==null) {
				if(newValues!=null) {
					Class clazz = newValues.getClass();
					Class arrType = clazz.getComponentType();
					array=(E[]) Array.newInstance(arrType, 0);
				} else {
					return array;
				}
			}
			int nullItemCount = 0;
			if(dropNulls) {
				for(E e: array) {if(e==null) nullItemCount++;}
				for(E e: newValues) {if(e==null) nullItemCount++;}
			}
			int newSize = array==null ? newValues.length : array.length + newValues.length - nullItemCount;	
			E[] newArray = (E[])Array.newInstance(newValues.getClass().getComponentType(), newSize);   		
			int offset = 0;
			if(!dropNulls) {
				if(!(array==null && array.length < 1)) {			
					System.arraycopy(array, 0, newArray, 0, array.length);
					offset=array.length;
				}
				System.arraycopy(newValues, 0, newArray, offset, newValues.length);
			} else {
				int index = 0;
				for(E e: array) {if(e!=null) {newArray[index] = e; index++;} }
				for(E e: newValues) {if(e!=null) {newArray[index] = e; index++;} }
			}
			return newArray;
		} catch (Exception e) {
			throw new RuntimeException("Failed to append", e);
		}
	}

	
	/**
	 * Returns true if the passed string matches any of the passed patterns.
	 * False if it does not.
	 * @param target
	 * @param filters
	 * @return
	 */
	public static boolean anyMatches(String target, Collection<Pattern> filters) {
		if(target==null || filters==null || filters.size()<1) return false;
		for(Pattern p: filters) {
			Matcher m = p.matcher(target);
			if(m.matches()) return true;
		}
		return false;
	}
	
	/**
	 * Flattens an array of objects into a string delimeted by the passed delimeter.
	 * @param delimeter The string delimeter.
	 * @param array The array of objects to flatten.
	 * @return The flattened string.
	 */
	public static String flattenArray(Object delimeter, Object...array) {
		StringBuilder buff = new StringBuilder();
		String delm = delimeter==null ? "" : delimeter.toString();
		if(array != null && array.length > 0) {
			for(Object o: array) {
				if(o==null || "".equals(o.toString())) continue;
				if(o.getClass().isArray()) {
					int size = Array.getLength(o);
					for(int i = 0; i < size; i++) {
						buff.append(Array.get(o, i).toString()).append(delm);
					}
				} else {
					buff.append(o.toString()).append(delm);
				}
			}
			buff.delete(buff.length()-delm.length(), buff.length());
		}
		return buff.toString();
	}
	
	/**
	 * @param range
	 * @param delimeter
	 * @param array
	 * @return
	 */
	public static String flattenArray(int[] range, Object delimeter, Object...array) {
		StringBuilder buff = new StringBuilder();
		String delm = delimeter==null ? "" : delimeter.toString();
		if(array != null) {
			int alength = array.length;
			for(int i : range) {
				if(i < alength) {
					buff.append(array[i]).append(delm);
				}
			}
			buff.delete(buff.length()-delm.length(), buff.length());
		}
		return buff.toString();
	}
	
	/**
	 * Removes the <code>items</code> array elements from the end of the array.
	 * @param array The array to truncate
	 * @param items The number of items to truncate from the end of the array
	 * @return The truncated array.
	 */
	public static  <E> E[] truncate(E[] array, int items) {
		if(items==0) return array;
		if(array==null) throw new RuntimeException("Array was null");
		if(items < 0) throw new RuntimeException("Item count was < 0");
		if(items>=array.length) return (E[])Array.newInstance(array.getClass().getComponentType(), 0);
		int diff = array.length-items;
		E[] newArray = (E[])Array.newInstance(array.getClass().getComponentType(), diff);
		System.arraycopy(array, 0, newArray, 0, diff);
		return newArray;
	}
	
	/**
	 * Removes the <code>items</code> array elements from the beginning of the array.
	 * @param array The array to trim
	 * @param items The number of items to trim from the end of the array
	 * @return The truncated array.
	 */
	public static  <E> E[] trim(E[] array, int items) {
		if(items==0) return array;
		if(array==null) throw new RuntimeException("Array was null");
		if(items < 0) throw new RuntimeException("Item count was < 0");
		if(items>=array.length) return (E[])Array.newInstance(array.getClass().getComponentType(), 0);		
		int diff = array.length-items;
		E[] newArray = (E[])Array.newInstance(array.getClass().getComponentType(), diff);
		System.arraycopy(array, items, newArray,0, diff);
		return newArray;
	}
	
	public static <E> E[][] split(E[] array, int count) {
		if(count<1) throw new RuntimeException("Cannot split into zero arrays");
		if(array==null) throw new RuntimeException("Array was null");
		if(array.length<1) return (E[][]) Array.newInstance(array.getClass(), 0);
		
		E[][] arrArr = null;
		
		if(array.length<count) {
			arrArr = (E[][]) Array.newInstance(array.getClass(), 1);
			arrArr[0] = array;			
		} else {
			int arrCount = array.length / count;
			int mod = array.length%count;
			
		}
		
		return arrArr;
	}
	
	
    /**
     * Converts an int range expression to an array of integers.
     * The values are comma separated. Each value can be an int or a range in the format <code>x-y</code>.
     * For example, the expresion <b>"1,2,4,7-10"</b> would return an in array <code>{1,2,4,7,8,9,10}</code>.
     * @param valuesStr The range expression.
     * @return An array of ints.
     */
    public static int[] compileRange(String valuesStr) {
    	Set<Integer> values = new TreeSet<Integer>();
    	String[] fragments = valuesStr.split(",");
    	for(String frag: fragments) {
    		frag = frag.trim();
    		if(frag.contains("-")) {
    			Matcher rangeMatcher = intRange.matcher(frag);
    			if(rangeMatcher.matches() && rangeMatcher.groupCount()==2) {
    				rangeMatcher.group();
    				int f1 = Integer.parseInt(rangeMatcher.group(1));
    				int f2 = Integer.parseInt(rangeMatcher.group(2));
    				if(f1==f2) {
    					values.add(f1);
    				} else {
    					int start = f1 > f2 ? f2 : f1;
    					int end = f1 > f2 ? f1 : f2;
    					while(start <= end) {
    						values.add(start);
    						start++;
    					}
    				}
    			}

    		} else {
    			try {
    				if(!frag.endsWith("-*")) {
    					values.add(Integer.parseInt(frag.trim()));
    				}
    			} catch (Exception e) {
    				
    			}
    		}

    	}
    	int[] valuesArr = new int[values.size()];
    	int index = 0;
    	for(Integer i: values) {
    		valuesArr[index] = i;
    		index++;
    	}    	
    	return valuesArr;
    }
    
    public static String buildFromRange(String valuesStr, String delimeter, String...dataCells) {
    	StringBuilder values = new StringBuilder();
    	String[] fragments = valuesStr.split(",");
    	for(String frag: fragments) {
    		frag = frag.trim();
    		Matcher rangeMatcher = intRange.matcher(frag);
    		Matcher endMatcher = endRange.matcher(frag);
    		if(rangeMatcher.matches() && rangeMatcher.groupCount()==2) {
    			rangeMatcher.group();
    			int f1 = Integer.parseInt(rangeMatcher.group(1));
    			int f2 = Integer.parseInt(rangeMatcher.group(2));
    			if(f1==f2) {
    				values.append(dataCells[f1]).append(delimeter);
    			} else {
    				int start = f1 > f2 ? f2 : f1;
    				int end = f1 > f2 ? f1 : f2;
    				while(start <= end) {
    					values.append(dataCells[start]).append(delimeter);
    					start++;
    				}
    			}
    		} else if(endMatcher.matches() && endMatcher.groupCount()==1) {
    			endMatcher.group();
    			int f1 = Integer.parseInt(endMatcher.group(1));
    			for(; f1 < dataCells.length; f1++) {
    				values.append(dataCells[f1]).append(delimeter);
    			}
    		} else {
    			try {
    				values.append(dataCells[Integer.parseInt(frag.trim())]).append(delimeter);
    			} catch (Exception e) {}
    		}
    	}
    	values.deleteCharAt(values.length()-1);
    	return values.toString();
    }
    
    /**
     * Returns the passed array with all instances of <code>target</code> removed.
     * @param values The array to clean.
     * @param target The value to remove from the array.
     * @return The cleaned array.
     */
    @SuppressWarnings("unchecked")
	public static <E> E[] removeEntry(E[] values, E target) {
    	List<E> cleanedValues = new ArrayList<E>(values.length);
    	for(E e: values) {
    		if(!e.equals(target)) {
    			cleanedValues.add(e);
    		}
    	}
    	E[] array = (E[])Array.newInstance(target.getClass(), cleanedValues.size());
		return cleanedValues.toArray(array);
    }
    
    /**
     * Returns the passed array with all instances contained in <code>targets</code> removed.
     * @param values The array to clean.
     * @param target The collection of values to remove from the array.
     * @return The cleaned array.
     */
    @SuppressWarnings("unchecked")
	public static <E> E[] removeEntry(E[] values, Collection<E> targets) {
    	if(targets==null || targets.size()<1) return values;
    	List<E> cleanedValues = new ArrayList<E>(values.length);
    	for(E e: values) {
    		if(!targets.contains(e)) {
    			cleanedValues.add(e);
    		}
    	}    	
    	E[] array = (E[])Array.newInstance(targets.iterator().next().getClass(), cleanedValues.size());
		return cleanedValues.toArray(array);    	
    }
    
 
    
    public static void log(Object message) {
    	System.out.println(message);
    }
    
	
	
	/**
	 * Cleans a regEx for display
	 * @param aRegexFragment
	 * @return
	 */
	public static String clean(String aRegexFragment){
		    final StringBuilder result = new StringBuilder();

		    final StringCharacterIterator iterator = new StringCharacterIterator(aRegexFragment);
		    char character =  iterator.current();
		    while (character != CharacterIterator.DONE ){
		      /*
		      * All literals need to have backslashes doubled.
		      */
		      if (character == '.') {
		        result.append("\\.");
		      }
		      else if (character == '\n') {
		        result.append("\\n");
		      }
		      else if (character == '\\') {
		        result.append("\\\\");
		      }
		      else if (character == '?') {
		        result.append("\\?");
		      }
		      else if (character == '*') {
		        result.append("\\*");
		      }
		      else if (character == '+') {
		        result.append("\\+");
		      }
		      else if (character == '&') {
		        result.append("\\&");
		      }
		      else if (character == ':') {
		        result.append("\\:");
		      }
		      else if (character == '{') {
		        result.append("\\{");
		      }
		      else if (character == '}') {
		        result.append("\\}");
		      }
		      else if (character == '[') {
		        result.append("\\[");
		      }
		      else if (character == ']') {
		        result.append("\\]");
		      }
		      else if (character == '(') {
		        result.append("\\(");
		      }
		      else if (character == ')') {
		        result.append("\\)");
		      }
		      else if (character == '^') {
		        result.append("\\^");
		      }
		      else if (character == '$') {
		        result.append("\\$");
		      }
		      else {
		        //the char is not a special one
		        //add it to the result as is
		        result.append(character);
		      }
		      character = iterator.next();
		    }
		    return result.toString();
		  }
	
	
	/** Regex pattern to parse text by whitespace except in quoted sequences */
	public static final Pattern WHITE_SPACE_QTD_PATTERN = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
	
	/**
	 * Parses a string with whitespace as a delimeter except when wrapped in double-quotes.
	 * Thanks to <a href="http://stackoverflow.com/users/33358/jan-goyvaerts">Jan Goyvaerts</a> for source code.
	 * @param value the string to parse
	 * @return a list of the parsed values
	 */
	public static List<String> parseWhiteSpaceQuoted(CharSequence value) {
		List<String> matchList = new ArrayList<String>();
		
		Matcher regexMatcher = WHITE_SPACE_QTD_PATTERN.matcher(value);
		while (regexMatcher.find()) {
		    if (regexMatcher.group(1)!=null) {
		        // Add double-quoted string without the quotes
		        matchList.add(regexMatcher.group(1));
		    } else if (regexMatcher.group(2) != null) {
		        // Add single-quoted string without the quotes
		        matchList.add(regexMatcher.group(2));
		    } else {
		        // Add unquoted word
		        matchList.add(regexMatcher.group());
		    }
		}
		return matchList;
	}
	
	/**
	 * Replaces all matched tokens with the matching system property value. 
	 * Tokens are in the format <b><code>${&lt;system property&gt;[:&lt;default&gt;]}</code></b>.
	 * @param text The text to process
	 * @return The processed text
	 */
	public static String tokenReplaceSysProps(CharSequence text) {
		return tokenReplaceSysProps("$", "{", "}", ":", text);
	}
	
	/**
	 * Replaces all matched tokens with the matching system property value.
	 * @param prefix An optional token prefix
	 * @param opener The token opener
	 * @param closer The token closer
	 * @param text The text to process
	 * @return The substituted string
	 */
	public static String tokenReplaceSysProps(String prefix, String opener, String closer, String defaultDelimeter, CharSequence text) {
		if(prefix==null) prefix = "";
		StringBuilder regex = new StringBuilder();
		regex.append(RegexHelper.escapeUnescapedReserved(prefix));
		regex.append(RegexHelper.escapeUnescapedReserved(opener));
		regex.append("(.*?)(?::(.*?))??");
		regex.append(RegexHelper.escapeUnescapedReserved(closer));
		StringBuffer ret = new StringBuffer();
		Pattern p = Pattern.compile(regex.toString());
		Matcher m = p.matcher(text);
		while(m.find()) {
//			log("\t[" + m.group(1) + "]");
//			log("\t[" + m.group(2) + "]");
			
			m.appendReplacement(ret, System.getProperty(m.group(1), m.group(2)==null ? "<null>" : m.group(2)));
		}
		m.appendTail(ret);
		return ret.toString();
	}
	
	/**
	 * Performs a clean/safe split on the passed source using the passed delimeter.
	 * @param source The value to split
	 * @param delimeter The delimeter to split the source with
	 * @param stripWhitespace If true, removes leading and trailing whitespace from individual fragments. If a fragment is all blanks, it will not be returned in the string array.
	 * @return A string array containing the split fragments of the passed source 
	 */
	public static String[] split(CharSequence source, CharSequence delimeter, boolean stripWhitespace) {
		if(source==null) return new String[0];
		if(delimeter==null) return new String[]{source.toString()};
		List<String> frags = new ArrayList<String>();
		for(String s: source.toString().split(delimeter.toString())) {
			if(s==null) continue;
			if(stripWhitespace) {
				s = s.trim();
				if("".equals(s)) continue;
			}
			frags.add(s);			
		}		
		return frags.toArray(new String[frags.size()]);
	}
	
	/**
	 * Performs a clean/safe split on the passed source using the passed delimeter.
	 * Removes leading and trailing whitespace from individual fragments. If a fragment is all blanks, it will not be returned in the string array.
	 * @param source The value to split
	 * @param delimeter The delimeter to split the source with
	 * @return A string array containing the split fragments of the passed source 
	 */
	public static String[] split(CharSequence source, CharSequence delimeter) {
		return split(source, delimeter, true);
	}
	
	/**
	 * Builds a string of <i>count</i> concatenated tabs 
	 * @param count The number of tabs to repeat
	 * @return the built string
	 */
	public static String buildIndent(int count) {
		return buildIndent("\t",count);
	}
	
	
	/**
	 * Builds a string by concatenating the passed character <i>count</i> times.
	 * @param character The character to repeat
	 * @param count The number of times to repeat
	 * @return the built string
	 */
	public static String buildIndent(String character, int count) {
		if(character==null) character = "\t";
		StringBuilder b = new StringBuilder();
		for(int i = 0; i < count; i++) {
			b.append(character);
		}
		return b.toString();
	}
	
	/**
	 * Trims all instances of <code>trimChar</code> from the end of the passed StringBuilder.
	 * @param buff The StringBuilder to trim
	 * @param trimChar The string to trim off the end of the buffer
	 * @return the modified buffer
	 */
	public static StringBuilder trimCharacters(final StringBuilder buff, String trimChar) {
		if(buff==null) return new StringBuilder("");
		if(trimChar==null) trimChar = " ";
		int trimCharLength = trimChar.length();
		if(buff.length()>=trimCharLength) {
			int start = buff.length()-(trimCharLength);
			int end = buff.length();
			while(buff.substring(start, end).equals(trimChar)) { 
				buff.delete(start, end);
				start = buff.length()-(trimCharLength);
				end = buff.length();
			}
		}
		return buff;
	}
	
	/**
	 * Converts a class name to the binary name used by the class file transformer, or returns the passed name if it is already a binary name
	 * @param name The class name to convert
	 * @return the binary name
	 */
	public static String convertToBinaryName(String name) {
		int index = name.indexOf('.');
		if(index!=-1) {
			return name.replace('.', '/');
		}
		return name;
	}
	
	/**
	 * Converts a class name from the binary name used by the class file transformer to the standard dot notated form, or returns the passed name if it is already a binary name
	 * @param name The class name to convert
	 * @return the standard dot notated class name
	 */
	public static String convertFromBinaryName(String name) {
		int index = name.indexOf('/');
		if(index!=-1) {
			return name.replace('/', '.');
		}
		return name;
	}
	
	
	/**
	 * Creates a new Unformatter
	 * @param pattern the Unformatter's pattern
	 * @return a new Unformatter
	 */
	public static Unformatter unformatter(CharSequence pattern) {
		return new Unformatter(pattern.toString());
	}
	
	/**
	 * <p>Title: Unformatter</p>
	 * <p>Description: A utility class which performs an extract of values from a string described by a regular expression
	 * and a set of named keys. Hence, it acts a bit like the reverse of {@link String#format(String, Object...)} </p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.util.StringHelper.Unformatter</code></p>
	 */
	public static class Unformatter {
		/** The parsing regex pattern */
		protected final Pattern pattern;
		/** The keys to key the returned map with */
		protected final String[] keys;
		/**
		 * Creates a new Unformatter
		 * @param pattern The regex pattern
		 * @param options The match flags bit mask
		 * @param keys The keys to key the returned map with
		 */
		public Unformatter(CharSequence pattern, int options, String...keys) {
			this.pattern = Pattern.compile(pattern.toString(), options);	
			this.keys = keys;
		}
		/**
		 * Creates a new Unformatter with no options
		 * @param pattern The regex pattern
		 * @param keys The keys to key the returned map with
		 */
		public Unformatter(CharSequence pattern, String...keys) {
			this(pattern, 0, keys);
		}
		
		/**
		 * Unformats the passed string value into a map of key/values
		 * @param str The string value to parse
		 * @return A map of the extracted values keyed by the positional string keys
		 */
		public Map<String, String> unformat(CharSequence str) {
			if(str==null || str.toString().trim().isEmpty()) return Collections.emptyMap();
			Matcher matcher = pattern.matcher(str);
			final int grpCount = matcher.groupCount(); 
			if(grpCount!=keys.length) throw new IllegalArgumentException("The group count of the expression [" + str + "] does not match the number of keys " + Arrays.toString(keys), new Throwable());
			if(!matcher.matches()) throw new IllegalArgumentException("The expression [" + str + "] is not matched with the pattern [" + pattern.toString() + "]", new Throwable());
			Map<String, String> map = new HashMap<String, String>(grpCount);
			for(int i = 1; i <= grpCount; i++) {
				map.put(keys[i-1], matcher.group(i));
			}
			return map;
		}
		
	}

}
