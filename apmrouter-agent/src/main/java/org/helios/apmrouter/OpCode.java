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

import org.jboss.netty.buffer.ChannelBuffer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Title: OpCode</p>
 * <p>Description: Enumerates sender and receiver operation codes.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.OpCode</code></p>
 */

public enum OpCode {
	/** An asynch send of a metric payload */
	SEND_METRIC,
	/** A return of a metric's token value back to a client that sent an untokenized metric */
	SEND_METRIC_TOKEN,	
	/** A synchronous send of a single metric payload */
	SEND_METRIC_DIRECT,
	/** A direct metric trace handshake */
	CONFIRM_METRIC(SEND_METRIC_DIRECT),	
	/** A synchronous send of a ping */
	PING,		
	/** A direct metric trace handshake */
	PING_RESPONSE(PING),
	/** Inquiry as to the identity of a connecting agent */
	WHO,
	/** Response as to the identity of a connecting agent */
	WHO_RESPONSE(WHO),
	/** Indicates a client has started and is announcing itself */
	HELLO,
	/** A HELLO handshake from the server */
	HELLO_CONFIRM(HELLO),		
	/** Indicates a client is about to disconnect */
	BYE,
	/** Directive from the server to flush metric catalog because a reset has occured */
	RESET,
	/** Confirm from the agent that the reset is complete */
	RESET_CONFIRM(RESET),
	/** A JMX request to the agent's MBeanServer */
	JMX_REQUEST,
	/** A response to a JMX request */
	JMX_RESPONSE(JMX_REQUEST),
	/** A JMX notification to a server listener */
	JMX_NOTIFICATION,
	/** Indicator sent to the agent that the server has closed the MBeanServerConnection but not the channel */
	JMX_CONN_CLOSED,
	/** Inquiry from server about available MBeanServers */
	JMX_MBS_INQUIRY,
	/** Response from agent to inquiry from server about available MBeanServers */
	JMX_MBS_INQUIRY_RESPONSE(JMX_MBS_INQUIRY),
	/** MetricURI subscription request */
	METRIC_URI_SUBSCRIBE,
	/** MetricURI subscription confirm */
	METRIC_URI_SUB_CONFIRM(METRIC_URI_SUBSCRIBE),	
	/** MetricURI subscription cancel */
	METRIC_URI_UNSUBSCRIBE,
	/** MetricURI subscription cancel confirm */
	METRIC_URI_UNSUB_CONFIRM(METRIC_URI_UNSUBSCRIBE),	
	/** MetricURI subscription event */
	ON_METRIC_URI_EVENT,
	/** Starts a SubDestination subscription */
	START_SUB_DEST,
	/** Starts a SubDestination subscription */
	START_SUB_DEST_CONFIRM(START_SUB_DEST),	
	/** Stops a SubDestination subscription */
	STOP_SUB_DEST,
	/** Stops a SubDestination subscription */
	STOP_SUB_DEST_CONFIRM(STOP_SUB_DEST);
	
	
	
	public static void main(String[] args) {
		for(OpCode op: OpCode.values()) {
			System.out.println("[" + op.name() + "]:" + op.op());
		}
	}
	
	private OpCode() {
		code = (byte)ordinal();
	}
	
	private OpCode(OpCode req) {
		code = (byte)(req.ordinal()*-1);
	}
	
	
	/** The byte representation of this OpCode */
	private final byte code;
	
	/**
	 * Returns the byte representation of this OpCode
	 * @return the byte representation of this OpCode
	 */
	public byte getCode() {
		return code;
	}
	
	/** Map of OpCodes keyed by the byte code */
	private static final Map<Byte, OpCode> BYTE2ENUM;
	/** Map of OpCodes keyed by the ordinal */
	private static final Map<Integer, OpCode> ORD2ENUM;
	
	static {
		Map<Byte, OpCode> tmp = new HashMap<Byte, OpCode>(OpCode.values().length);
		Map<Integer, OpCode> itmp = new HashMap<Integer, OpCode>(OpCode.values().length);
		for(OpCode op: OpCode.values()) {
			itmp.put(op.ordinal(), op);
			OpCode prior = tmp.put(op.code, op);			
			if(prior!=null){ 
				throw new RuntimeException("Duplicate Byte Code for Ops: [" + op + "] and [" + prior + "]", new Throwable());
			}
		}
		BYTE2ENUM = Collections.unmodifiableMap(tmp);
		ORD2ENUM = Collections.unmodifiableMap(itmp);
	}	
	
	/**
	 * Decodes the passed byte code to a OpCode.
	 * Throws a runtime exception if the byte code is invalud
	 * @param byteCode The byte code to decode
	 * @return the decoded OpCode
	 */
	public static OpCode valueOf(byte byteCode) {
		OpCode op = BYTE2ENUM.get(byteCode);
		if(op==null) throw new IllegalArgumentException("The passed byteCode [" + byteCode + "] is not a valid OpCode byteCode", new Throwable());
		return op;
	}
	
	/**
	 * Decodes the passed ordinal to a OpCode.
	 * Throws a runtime exception if the ordinal is invalud
	 * @param ordinal The ordinal to decode
	 * @return the decoded OpCode
	 */
	public static OpCode ordinal(int ordinal) {
		OpCode op = ORD2ENUM.get(ordinal);
		if(op==null) throw new IllegalArgumentException("The passed ordinal [" + ordinal + "] is not a valid OpCode ordinal", new Throwable());
		return op;
	}
	
	
	/**
	 * Indicates if the passed byte represents a valid OpCode
	 * @param op the bytes to test
	 * @return true if the passed byte represents a valid OpCode, false otherwise
	 */ 
	public static boolean isOpCode(byte op) {
		return BYTE2ENUM.containsKey(op);
	}
	
	/**
	 * Returns the ordinal as a byte
	 * @return the ordinal as a byte
	 */
	public byte op() {
		return code;
	}

	/**
	 * Determines the op Code of the request in the passed buffer
	 * @param buff The buffer to read the op code from
	 * @return The decoded op type
	 */
	public static OpCode valueOf(ChannelBuffer buff) {
		if(buff==null) throw new IllegalArgumentException("The passed buffer was null", new Throwable()); 
		return valueOf(buff.getByte(0));
	}

}
