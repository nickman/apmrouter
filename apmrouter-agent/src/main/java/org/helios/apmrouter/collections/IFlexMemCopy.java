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
 * <p>Title: IFlexMemCopy</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.collections.IFlexMemCopy</code></p>
 */

public interface IFlexMemCopy {
	/**
	 * Copies a range of bytes from one memory address to another
	 * @param srcBase The address of the base source object.
	 * @param srcOffset The offset from the <code>srcBase</code> or the absolute address if <code>srcBase</code> is null 
	 * @param destBase The address of the base destination object.
	 * @param destOffset The offset from the <code>destBase</code> or the absolute address if <code>destBase</code> is null
	 * @param bytes The number of bytes to copy
	 */
	public void copyMemory(Object srcBase, long srcOffset, Object destBase, long destOffset, long bytes);
}
