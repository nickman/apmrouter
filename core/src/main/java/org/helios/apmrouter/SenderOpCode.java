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
package org.helios.apmrouter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.netty.buffer.ChannelBuffer;



/**
 * <p>Title: SenderOpCode</p>
 * <p>Description: Enumerates the op-codes for senders</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.SenderOpCode</code></p>
 */
public enum SenderOpCode {
	/** An asynch send of a metric payload */
	SEND_METRIC,
	/** A synchronous send of a single metric payload */
	SEND_METRIC_DIRECT;
	
	/** Map of OpCodes keyed by the ordinal */
	private static final Map<Byte, SenderOpCode> ORD2ENUM;
	
	static {
		Map<Byte, SenderOpCode> tmp = new HashMap<Byte, SenderOpCode>(SenderOpCode.values().length);
		for(SenderOpCode op: SenderOpCode.values()) {
			tmp.put((byte)op.ordinal(), op);
		}
		ORD2ENUM = Collections.unmodifiableMap(tmp);
	}
	
	/**
	 * Decodes the passed ordinal to a SenderOpCode.
	 * Throws a runtime exception if the ordinal is invalud
	 * @param ordinal The ordinal to decode
	 * @return the decoded SenderOpCode
	 */
	public static SenderOpCode valueOf(byte ordinal) {
		SenderOpCode op = ORD2ENUM.get(ordinal);
		if(op==null) throw new IllegalArgumentException("The passed ordinal [" + ordinal + "] is not a valid SenderOpCode ordinal", new Throwable());
		return op;
	}
	
	/**
	 * Indicates if the passed byte represents a valid SenderOpCode
	 * @param op the bytes to test
	 * @return true if the passed byte represents a valid SenderOpCode, false otherwise
	 */ 
	public static boolean isSenderOpCode(byte op) {
		return ORD2ENUM.containsKey(op);
	}
	
	/**
	 * Returns the ordinal as a byte
	 * @return the ordinal as a byte
	 */
	public byte op() {
		return (byte)ordinal();
	}

	/**
	 * Determines the op Code of the request in the passed buffer
	 * @param buff The buffer to read the op code from
	 * @return The decoded op type
	 */
	public static SenderOpCode valueOf(ChannelBuffer buff) {
		if(buff==null) throw new IllegalArgumentException("The passed buffer was null", new Throwable()); 
		return valueOf(buff.getByte(0));
	}
	


}
