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
import sun.misc.Unsafe;
/**
 * <p>Title: LongsOnlyMemoryCopier</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>vanilla.java.chronicle.impl.LongsOnlyMemoryCopier</code></p>
 */

public class LongsOnlyMemoryCopier implements MemoryCopier {
	private final Unsafe UNSAFE;
	private final int BYTES_OFFSET;
	
	/**
	 * Creates a new LongsOnlyMemoryCopier
	 * @param unsafe The unsafe
	 */
	public LongsOnlyMemoryCopier(Unsafe unsafe) {
		UNSAFE = unsafe;
		BYTES_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);
	}
	
    /**
     * {@inheritDoc}
     * @see vanilla.java.chronicle.impl.MemoryCopier#write(long, int, byte[])
     */
    @Override
    public void write(long position, int offset, byte[] b) {
//        UNSAFE.copyMemory(b, BYTES_OFFSET, null, position, b.length);
//        position += b.length;
    }

    /**
     * {@inheritDoc}
     * @see vanilla.java.chronicle.impl.MemoryCopier#write(long, byte[], int, int)
     */
    @Override
    public void write(long position, byte[] b, int off, int len) {
//        UNSAFE.copyMemory(b, BYTES_OFFSET + off, null, position, len);
//        position += len;
    }
    
    /**
     * {@inheritDoc}
     * @see vanilla.java.chronicle.impl.MemoryCopier#readFully(long, byte[], int, int)
     */
    @Override
    public void readFully(long position, byte[] b, int off, int len) {
//        UNSAFE.copyMemory(null, position, b, BYTES_OFFSET + off, len);
//        position += len;
    }

}
