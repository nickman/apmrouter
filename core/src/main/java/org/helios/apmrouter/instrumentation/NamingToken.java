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
package org.helios.apmrouter.instrumentation;

import static org.helios.apmrouter.util.Methods.nvl;



/**
 * <p>Title: NamingToken</p>
 * <p>Description: Enumerates fillin tokens for populating the name and namespaces for metrics generated from AOP instrumented methods</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.instrumentation.NamingToken</code></p>
 */

public enum NamingToken {
	/** The instrumented class's package name */
	PACKAGE,
	/** The instrumented class's name */
	CLASS,
	/** The instrumented method name */
	METHOD,
	/** The string value of an indexed parameter */
	PARAM;
	
	/**
	 * Decodes the passed name to a NamingToken.
	 * Throws a runtime exception if the ordinal is invalud
	 * @param name The metricId type name to decode. Trimmed and uppercased.
	 * @return the decoded NamingToken
	 */
	public static NamingToken valueOfName(CharSequence name) {
		String n = nvl(name, "NamingToken Name").toString().trim().toUpperCase();
		try {
			return NamingToken.valueOf(n);
		} catch (Exception e) {
			throw new IllegalArgumentException("The passed name [" + name + "] is not a valid NamingToken name", new Throwable());
		}
	}
	
	/**
	 * Determines if the passed string, trimmed and uppercased, is a valid NamingToken
	 * @param name The name to test
	 * @return true if the passed string is a valid NamingToken, false otherwise
	 */
	public static boolean isNamingToken(CharSequence name) {
		try {
			valueOfName(name);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

}
