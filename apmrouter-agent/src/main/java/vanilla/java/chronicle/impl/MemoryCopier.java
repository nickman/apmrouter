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
package vanilla.java.chronicle.impl;

/**
 * <p>Title: MemoryCopier</p>
 * <p>Description: Defines the Unsafe.copyMemory operations</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>vanilla.java.chronicle.impl.MemoryCopier</code></p>
 */

public interface MemoryCopier {
	
	
    /**
     * Writes the passed bytes to the specified offset
     * @param position The chronicle position
     * @param offset The offset
     * @param b The bytes to write
     */
    public void write(long position, int offset, byte[] b);

    /**
     * Writes the specified number of bytes in the passed bytes to the specified offset
     * @param position The chronicle position
     * @param b The bytes to write from
     * @param off The offset
     * @param len The number of bytes to write
     */
    public void write(long position, byte[] b, int off, int len);
    
    /**
     * Writes into the passed byte array
     * @param position The chronicle position
     * @param b The byte array to write into
     * @param off The offset
     * @param len The number of bytes
     */
    public void readFully(long position, byte[] b, int off, int len);

}
