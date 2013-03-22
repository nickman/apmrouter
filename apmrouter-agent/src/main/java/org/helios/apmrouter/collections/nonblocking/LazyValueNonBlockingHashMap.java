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
package org.helios.apmrouter.collections.nonblocking;

import org.cliffc.high_scale_lib.NonBlockingHashMap;

import java.lang.reflect.Field;

/**
 * <p>Title: LazyValueNonBlockingHashMap</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.collections.nonblocking.LazyValueNonBlockingHashMap</code></p>
 */

public class LazyValueNonBlockingHashMap<TypeK, TypeV> extends NonBlockingHashMap<TypeK, TypeV> {

	/**  */
	private static final long serialVersionUID = 2253492021603171571L;
	
	protected static final Object TOMBSTONE;
	
	static {
		try {
			Field f = NonBlockingHashMap.class.getDeclaredField("TOMBSTONE");
			f.setAccessible(true);
			TOMBSTONE = f.get(null);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Creates a new LazyValueNonBlockingHashMap
	 */
	public LazyValueNonBlockingHashMap() {
	}

	/**
	 * Creates a new LazyValueNonBlockingHashMap
	 * @param initial_sz The initial size of the map
	 */
	public LazyValueNonBlockingHashMap(int initial_sz) {
		super(initial_sz);
	}
	
	


}
