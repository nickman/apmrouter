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
package org.helios.apmrouter.nativex;

import static org.helios.apmrouter.util.Methods.nvl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Title: TCPSocketState</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.nativex.TCPSocketState</code></p>
 */

public enum TCPSocketState {
	/** The socket state for <i>really</i> UNKNOWN */
	TCP_SUPER_UNKNOWN(0),
	/** The socket state for ESTABLISHED */
	TCP_ESTABLISHED(1),

	/** The socket state for SYN_SENT */
	TCP_SYN_SENT(2),

	/** The socket state for SYN_RECV */
	TCP_SYN_RECV(3),

	/** The socket state for FIN_WAIT1 */
	TCP_FIN_WAIT1(4),

	/** The socket state for FIN_WAIT2 */
	TCP_FIN_WAIT2(5),

	/** The socket state for TIME_WAIT */
	TCP_TIME_WAIT(6),

	/** The socket state for CLOSE */
	TCP_CLOSE(7),

	/** The socket state for CLOSE_WAIT */
	TCP_CLOSE_WAIT(8),

	/** The socket state for LAST_ACK */
	TCP_LAST_ACK(9),

	/** The socket state for LISTEN */
	TCP_LISTEN(10),

	/** The socket state for CLOSING */
	TCP_CLOSING(11),

	/** The socket state for IDLE */
	TCP_IDLE(12),

	/** The socket state for BOUND */
	TCP_BOUND(13),

	/** The socket state for UNKNOWN */
	TCP_UNKNOWN(14);
	
	/** A decoding map to decode the NetFlag code to a TCPSocketState */
	public static final Map<Integer, TCPSocketState> CODE2ENUM;
	
	static {
		TCPSocketState[] values = TCPSocketState.values();
		Map<Integer, TCPSocketState> tmp = new HashMap<Integer, TCPSocketState>(values.length);
		for(TCPSocketState t: values) {
			tmp.put(t.code, t);
		}
		CODE2ENUM = Collections.unmodifiableMap(tmp);
	}
	
	private TCPSocketState(int code) {
		this.code = code;
	}
	
	private final int code;
	
	/**
	 * Decodes the passed name to a TCPSocketState.
	 * Throws a runtime exception if the ordinal is invalid
	 * @param name The socket state name type name to decode. Trimmed and uppercased.
	 * @return the decoded TCPSocketState
	 */
	public static TCPSocketState valueOfName(CharSequence name) {
		String n = nvl(name, "TCPSocketState Name").toString().trim().toUpperCase();
		try {
			return TCPSocketState.valueOf(n);
		} catch (Exception e) {
			throw new IllegalArgumentException("The passed name [" + name + "] is not a valid TCPSocketState name", new Throwable());
		}
	}	
	
	/**
	 * Decodes the passed code to a TCPSocketState.
	 * Throws a runtime exception if the code is invalud
	 * @param code The code to decode
	 * @return the decoded TCPSocketState
	 */
	public static TCPSocketState valueOf(int code) {
		TCPSocketState t = CODE2ENUM.get(code);
		if(t==null) throw new IllegalArgumentException("The passed code [" + code+ "] is not a valid TCPSocketState code", new Throwable());
		return t;
	}	

}
