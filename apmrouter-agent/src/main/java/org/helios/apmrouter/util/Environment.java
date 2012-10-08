/**
 * ICE Futures, US
 */
package org.helios.apmrouter.util;

import java.nio.ByteOrder;

/**
 * <p>Title: Environment</p>
 * <p>Description: Environment and config helper methods</p> 
 * <p>Company: ICE Futures US</p>
 * @author Whitehead (nicholas.whitehead@theice.com)
 * @version $LastChangedRevision$
 * <p><code>org.helios.apmrouter.util.Environment</code></p>
 */

public class Environment {
	private static final byte LITTLE_END = 1;
	private static final byte BIG_END = 0;
	
	/** Byte sized byteorder indicator. Little Endian is 1, BigEndian is 0 */
	public static final byte ENDIAN = ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN) ? LITTLE_END : BIG_END;
	
	/**
	 * Decodes the endian code to a byte order
	 * @param code A byte zero for little endian, a byte one for big endian
	 * @return a byte order
	 */
	public static ByteOrder byteOrder(byte code) {
		if(LITTLE_END==code) return ByteOrder.LITTLE_ENDIAN;
		else if(BIG_END==code) return ByteOrder.BIG_ENDIAN;
		else throw new IllegalArgumentException("The code [" + code + "] is not a valid endian");
	}
}
