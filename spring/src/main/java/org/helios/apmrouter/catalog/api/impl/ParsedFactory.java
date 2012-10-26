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
package org.helios.apmrouter.catalog.api.impl;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Title: ParsedFactory</p>
 * <p>Description: Factory static methods for creating objects for the JSONQueryParser</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.catalog.api.impl.ParsedFactory</code></p>
 */

public class ParsedFactory {
	protected static final Map<String, MethodHandle> primary = new HashMap<String, MethodHandle>();
	
	static {
		generateHandle("q", Query.class);
	}
	
	private static void generateHandle(String key, Class<? extends Parsed> clazz) {
		try {
			
			MethodType desc = MethodType.methodType(clazz, new Class[]{});
			MethodHandle mh = MethodHandles.lookup().findStatic(clazz, "create", desc);
			primary.put(key, mh);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public static Parsed createPrimary(String key) {
		try {
			MethodHandle mh = primary.get(key.trim().toLowerCase());
			if(mh==null) throw new Exception("No handler found for key [" + key + "]", new Throwable());
			return (Parsed) mh.invoke();
		} catch (Throwable ex) {
			throw new RuntimeException(ex);
		}
	}
}
