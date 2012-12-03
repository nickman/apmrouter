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
 * <p>Title: ILongStack</p>
 * <p>Description: Defines a class implementing a stack of primitive longs</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.collections.ILongStack</code></p>
 */

public interface ILongStack {
	
	/**
	 * De-allocates this LongStack 
	 */
	public void destroy();

	/**
	 * Pushes an array of longs into the beginning of the stack
	 * @param values The longs to push onto the stack
	 * @return this ILongStack instance
	 */
	public ILongStack push(long...values);
	
	/**
	 * Returns the long value at the specified index
	 * @param index The index to retrieve the long from
	 * @return the long value
	 */
	public long get(int index);
	
	/**
	 * Removes and returns the first long value from the stack
	 * @return the removed long value
	 */
	public long pop();
	/**
	 * Returns but does not remove the first long value from the stack
	 * @return the first long value from the stack
	 */
	public long peek();
	/**
	 * Returns the number of long values in the stack
	 * @return the size of the stack
	 */
	public int size();
	
	/**
	 * Returns the number of empty slots in the stack.
	 * @return the number of empty slots in the stack.
	 */
	public int emptySlotsFree();
}
