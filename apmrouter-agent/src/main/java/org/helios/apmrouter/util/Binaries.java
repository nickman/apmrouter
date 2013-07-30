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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * <p>Title: Binaries</p>
 * <p>Description: Binary constants</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.util.Binaries</code></p>
 */

public class Binaries {
	
	/** All binary mask ints integers in order */
	public static final Set<Integer> BINARY_INTS = Collections.unmodifiableSet(new LinkedHashSet<Integer>(Arrays.asList(
			1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536, 131072, 262144, 524288, 1048576, 2097152, 4194304, 8388608, 16777216, 33554432, 67108864, 134217728, 268435456, 536870912, 1073741824, -2147483648
	)));
	
	/** The current iterator state for a thread */
	public static final ThreadLocal<Iterator<Integer>> DEFAULT_INT_STATE_ITERATOR = new ThreadLocal<Iterator<Integer>>(){
		@Override
		protected Iterator<Integer> initialValue() {
			return new LinkedHashSet<Integer>(BINARY_INTS).iterator();
		}
	};
	
	/** The current iterator state for a thread */
	public static final ThreadLocal<Iterator<Integer>> INT_STATE_ITERATOR = new ThreadLocal<Iterator<Integer>>();

	
	/**
	 * Returns a thread-local based int iterator fast-forwarded to the specified entry
	 * @param initial The number of times to increment the iterator after initialization and before returning
	 * @return an integer iterator
	 */
	public static Iterator<Integer> getBinaryIterator(int initial) {
		if(initial < 0 || initial > 32) throw new IllegalArgumentException("Invalid initial value [" + initial + "]");
		Iterator<Integer> iterator = INT_STATE_ITERATOR.get();
		if(iterator==null) {
			iterator = new LinkedHashSet<Integer>(BINARY_INTS).iterator();
			INT_STATE_ITERATOR.set(iterator);
			for(int i = 0; i < initial; i++) {
				iterator.next();
			}
		}
		return iterator;
	}
	
	/**
	 * Resets all the current thread's thread-locals
	 */
	public static void reset() {
		DEFAULT_INT_STATE_ITERATOR.remove();
		INT_STATE_ITERATOR.remove();
	}
}
