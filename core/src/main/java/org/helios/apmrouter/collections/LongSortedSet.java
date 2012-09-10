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
package org.helios.apmrouter.collections;

/**
 * <p>Title: LongSortedSet</p>
 * <p>Description: A managed off-heap array of unique longs, maintained in sorted order.</p>
 * <p><b><font color='red'>!!  NOTE !!&nbsp;&nbsp;</font>:&nbsp;&nbsp;</b>This class is THREAD UNSAFE. Only use with one thread at a time, or used one
 * of the concurrent/synchronized versions</p>  
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.collections.LongSortedSet</code></p>
 */
public class LongSortedSet {
	/*
	 * Required Ops from UnsafeLongArray:
	 * ==================================
	 * long get(int)
	 * boolean contains(long)
	 * int size()
	 * 
	 * add(long)
	 * clear()
	 * remove(long)
	 * 
	 * 
	 * 
	 * 
	 */
	
	public LongSortedSet() {
		
	}
	
	public LongSortedSet(int initialCapacity) {
		
	}
	
	public LongSortedSet(long[] values) {
		
	}
	
	
	
	
}
