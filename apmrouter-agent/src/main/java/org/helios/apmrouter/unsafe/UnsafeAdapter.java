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
package org.helios.apmrouter.unsafe;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * <p>Title: UnsafeAdapter</p>
 * <p>Description: Adapter for {@link sun.misc.Unsafe} that detects the version and provides adapter methods for
 * the different supported signatures.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.unsafe.UnsafeAdapter</code></p>
 */

public class UnsafeAdapter {
    /** The unsafe instance */
    private static final Unsafe UNSAFE;
    /** The address size */
    public static final int ADDRESS_SIZE;
    /** Byte array offset */
    public static final int BYTES_OFFSET;
    /** Object array offset */
    public static final long OBJECTS_OFFSET;
    
    /** Indicates if the 5 param copy memory is supported */
    public static final boolean FIVE_COPY;
    /** Indicates if the 4 param set memory is supported */
    public static final boolean FOUR_SET;

    static {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (Unsafe) theUnsafe.get(null);
            ADDRESS_SIZE = UNSAFE.addressSize();
            BYTES_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);
            OBJECTS_OFFSET = UNSAFE.arrayBaseOffset(Object[].class);
            int copyMemCount = 0;
            int setMemCount = 0;
            for(Method method: Unsafe.class.getDeclaredMethods()) {
            	if("copyMemory".equals(method.getName())) copyMemCount++;
            	if("setMemory".equals(method.getName())) setMemCount++;
            }
            FIVE_COPY = copyMemCount>1;
            FOUR_SET = setMemCount>1;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
    
    /**
     * Sets all bytes in a given block of memory to a copy of another block
     * @param srcBase The source object
     * @param srcOffset The source object offset
     * @param destBase The destination object
     * @param destOffset The destination object offset
     * @param bytes The byte count to copy
     */
    public static void copyMemory(Object srcBase, long srcOffset, Object destBase, long destOffset, long bytes) {
    	if(FIVE_COPY) {
    		UNSAFE.copyMemory(srcBase, srcOffset, destBase, destOffset, bytes);
    	} else {
    		UNSAFE.copyMemory(srcOffset + getAddressOf(srcBase), destOffset + getAddressOf(destBase), bytes);
    	}
    }
    
    /**
     * Sets all bytes in a given block of memory to a fixed value
     * @param obj The target object
     * @param offset  The target object offset
     * @param bytes The numer of bytes to set
     * @param value The value to set the bytes to
     */
    public static void setMemory(Object obj, long offset, long bytes, byte value) {
    	if(FOUR_SET) {
    		UNSAFE.setMemory(obj, offset, bytes, value);
    	} else {
    		UNSAFE.setMemory(offset + getAddressOf(obj), bytes, value);
    	}
    }
    
    /**
     * Returns the address of the passed object
     * @param obj The object to get the address of 
     * @return the address of the passed object or zero if the passed object is null
     */
    public static long getAddressOf(Object obj) {
    	if(obj==null) return 0;
    	Object[] array = new Object[] {obj};
    	return ADDRESS_SIZE==4 ? UNSAFE.getInt(array, OBJECTS_OFFSET) : UNSAFE.getLong(array, OBJECTS_OFFSET);
    }
    
    
    
}
