/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2012, Helios Development Group and individual contributors
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
package org.helios.apmrouter.jagent.aop.bytecode;

import java.util.*;

/**
 * <p>Title: TypeModifier</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.jagent.aop.bytecode.TypeModifier</code></p>
 */

public enum TypeModifier {
	/** Modifier Attribute for public */
	PUBLIC(1),
	/** Modifier Attribute for private */
	PRIVATE(2),
	/** Modifier Attribute for protected */
	PROTECTED(4),
	/** Modifier Attribute for static */
	STATIC(8),
	/** Modifier Attribute for final */
	FINAL(16),
	/** Modifier Attribute for synchronized */
	SYNCHRONIZED(32),
	/** Modifier Attribute for volatile */
	VOLATILE(64),
	/** Modifier Attribute for transient */
	TRANSIENT(128),
	/** Modifier Attribute for native */
	NATIVE(256),
	/** Modifier Attribute for interface */
	INTERFACE(512),
	/** Modifier Attribute for abstract */
	ABSTRACT(1024),
	/** Modifier Attribute for strict */
	STRICT(2048),
	/** Modifier Attribute for annotation */
	ANNOTATION(8192),
	/** Modifier Attribute for enum */
	ENUM(16384);
	
	/** A map of TypeModifiers keyed by their keys */
	public static final Map<Integer, TypeModifier> KEY2MOD;
	
	static {
		Map<Integer, TypeModifier> tmp = new HashMap<Integer, TypeModifier>(TypeModifier.values().length);
		for(TypeModifier tm: TypeModifier.values()) {
			tmp.put(tm.key, tm);
		}
		KEY2MOD = Collections.unmodifiableMap(tmp);
	}
	

	private TypeModifier(int key) {
		this.key = key;
	}
	/** The modfier key */
	public final int key;
	
	/**
	 * Determines if the passed name is a valid modifier
	 * @param name the name to test
	 * @return true if the passed name is a valid modifier, false otherwise
	 */
	public static boolean isValidLevel(CharSequence name) {
		if(name==null) return false;
		try {
			TypeModifier.valueOf(name.toString().trim().toUpperCase());
			return true;
		} catch (Exception ex) {
			return false;
		}
	}
	
	/**
	 * Determines if the passed int is a valid modifier
	 * @param value the int to test
	 * @return true if the passed int is a valid modifier, false otherwise
	 */
	public static boolean isValidLevel(int value) {
		return KEY2MOD.get(value)!=null;
	}
	
	
	/**
	 * Returns a TypeModifier for the passed name.
	 * @param name The name to get the TypeModifier for
	 * @return a TypeModifier
	 */
	public static TypeModifier forName(CharSequence name) {
		try {
			return TypeModifier.valueOf(name.toString().trim().toUpperCase());
		} catch (Exception ex) {
			throw new IllegalArgumentException("The passed name [" + name + "] was an invalid TypeModifier", new Throwable());
		}
	}
	
	/**
	 * Returns a TypeModifier for the passed int value.
	 * @param value The int value to get the TypeModifier for
	 * @return a TypeModifier
	 */
	public static TypeModifier forKey(int value) {
		try {
			TypeModifier tm = KEY2MOD.get(value);
			if(tm==null) throw new Exception();
			return tm;
		} catch (Exception ex) {
			throw new IllegalArgumentException("The passed value [" + value + "] was an invalid TypeModifier", new Throwable());
		}
	}	
	
	/**
	 * Returns an array of TypeModifiers that are turned on in the passed value 
	 * @param value The value to test for TypeModifiers 
	 * @return an array of TypeModifiers
	 */
	public static TypeModifier[] modifiersFor(int value) {
		Set<TypeModifier> set = EnumSet.noneOf(TypeModifier.class);
		for(TypeModifier tm: TypeModifier.values()) {
			if((tm.key & value) == tm.key) set.add(tm);
		}
		return set.toArray(new TypeModifier[set.size()]);
	}
	
	/**
	 * Returns an array of TypeModifiers that are present in each of the passed string values 
	 * @param values The string values to test
	 * @return an array of TypeModifiers
	 */
	public static TypeModifier[] modifiersFor(CharSequence...values) {
		Set<TypeModifier> set = EnumSet.noneOf(TypeModifier.class);
		for(CharSequence cs: values) {
			if(isValidLevel(cs)) set.add(forName(cs));
		}
		return set.toArray(new TypeModifier[set.size()]);
	}
	
	/**
	 * Turns on the bit on the passed value for each passed modifier
	 * @param value The int value to update
	 * @param modifiers The modifiers to turn on
	 * @return the modified modifier int
	 */
	public static int turnOn(int value, TypeModifier...modifiers) {
		return turn(true, value, modifiers);
	}
	
	/**
	 * Turns off the bit on the passed value for each passed modifier
	 * @param value The int value to update
	 * @param modifiers The modifiers to turn off
	 * @return the modified modifier int
	 */
	public static int turnOff(int value, TypeModifier...modifiers) {
		return turn(false, value, modifiers);
	}
	
	/**
	 * Toggles the modifier bits in the passed int for the passed modifiers
	 * @param on true to enable the modifiers, false to disable
	 * @param value The int to apply to
	 * @param modifiers The modifiers to apply
	 * @return the modified modifier int
	 */
	public static int turn(boolean on, int value, TypeModifier...modifiers) {
		int a = value;
		for(TypeModifier tm: modifiers) {
			if(tm==null) continue;
			if(on) {
				a = a | tm.key;
			} else {
				a = a ^ tm.key;
			}
		}
		return a;
	}
	
	
	/**
	 * Determines if the passed int is enabled for the passed modifier
	 * @param value The int to test
	 * @param modifier The modifier to test for
	 * @return true if the passed int is enabled for the passed modifier, false otherwise
	 */
	public boolean isEnabled(int value, TypeModifier modifier) {
		if(modifier==null) throw new IllegalArgumentException("The passed modifier was null", new Throwable());
		return (modifier.key & value)==modifier.key;
	}
	
	
	
}
