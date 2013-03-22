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

import org.helios.apmrouter.util.BitMaskedEnum;
import org.helios.apmrouter.util.SimpleLogger;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.helios.apmrouter.util.BitMaskedEnum.Support.generateIntMaskMap;
import static org.helios.apmrouter.util.BitMaskedEnum.Support.getIntBitMask;
import static org.helios.apmrouter.util.Methods.nvl;

/**
 * <p>Title: TCPSocketState</p>
 * <p>Description: A functional bitmasked enum representing TCP socket states</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.nativex.TCPSocketState</code></p>
 */

public enum TCPSocketState implements BitMaskedEnum {
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
	public static final Map<Integer, TCPSocketState> CODE2ENUM = generateIntMaskMap(TCPSocketState.values());
	
	
	private TCPSocketState(int code) {
		this.code = code;
		mask = getIntBitMask(this);
	}
	
	/** The code for this state */
	private final int code;
	/** The mask for this state */
	private final int mask;
	/**
	 * Returns the code for this state
	 * @return the code for this state
	 */
	public int getCode() {
		return code;
	}

	/**
	 * Returns the mask for this state
	 * @return the mask for this state
	 */
	public int getMask() {
		return mask;
	}

	/**
	 * Accepts the passed int state and enables the passed socket states on it
	 * @param i The initial state code
	 * @param states The states to enable
	 * @return the modified state code
	 */
	public static int enable(int i, TCPSocketState...states) {
		int mask = i;
		if(states!=null) {
			for(TCPSocketState state: states) {
				if(state==null) continue;
				mask = mask | state.mask;
			}
		}
		return mask;
	}
		
	/**
	 * Accepts the passed int state and disables the passed socket states on it
	 * @param i The initial state code
	 * @param states The states to disable
	 * @return the modified state code
	 */
	public static int disable(int i, TCPSocketState...states) {
		int mask = i;
		if(states!=null) {
			for(TCPSocketState state: states) {
				if(state==null) continue;
				mask = mask & ~state.mask;
			}
		}
		return mask;
	}	

	/**
	 * Returns a state code that represents the mask of all the passed socket states
	 * @param states The socket states to enable
	 * @return the mask that represents all the socket states.
	 */
	public static int enable(TCPSocketState...states) {
		return enable(0, states);
	}
	
	
	
	/**
	 * Determines if the passed mask is enabled for all the specified socket states
	 * @param mask The mask to test
	 * @param state The socket states to test for
	 * @return true if the passed mask is enabled for all the specified socket states, false otherwise
	 */
	public static boolean enabledForAll(int mask, TCPSocketState...state) {
		if(state==null) throw new IllegalArgumentException("The passed state was null", new Throwable());
		for(TCPSocketState t: state) {
			if(t==null) continue;
			if((mask| t.mask) != mask) return false;
		}
		return true;
	}
	
	/**
	 * Determines if the passed mask is disabled for all the specified socket states
	 * @param mask The mask to test
	 * @param state The socket states to test for
	 * @return true if the passed mask is disabled for all the specified socket states, false otherwise
	 */
	public static boolean disabledForAll(int mask, TCPSocketState...state) {
		if(state==null) throw new IllegalArgumentException("The passed state was null", new Throwable());
		for(TCPSocketState t: state) {
			if(t==null) continue;
			if((mask & ~t.mask) != mask) return false;
		}
		return true;
	}
	
	
	/**
	 * Determines if the passed mask is enabled for any the specified socket states
	 * @param mask The mask to test
	 * @param state The socket states to test for
	 * @return true if the passed mask is enabled for at least one of the specified socket states, false otherwise
	 */
	public static boolean enabledForAny(int mask, TCPSocketState...state) {
		if(state==null) throw new IllegalArgumentException("The passed state was null", new Throwable());
		for(TCPSocketState t: state) {
			if(t==null) continue;
			if((mask | t.mask) == mask) return true;
		}
		return false;
	}
	
	/**
	 * Determines if the passed mask is disabled for any the specified socket states
	 * @param mask The mask to test
	 * @param state The socket states to test for
	 * @return true if the passed mask is disabled for at least one of the specified socket states, false otherwise
	 */
	public static boolean disabledForAny(int mask, TCPSocketState...state) {
		if(state==null) throw new IllegalArgumentException("The passed state was null", new Throwable());
		for(TCPSocketState t: state) {
			if(t==null) continue;
			if((mask & ~t.mask) == mask) return true;
		}
		return false;
	}
	
	/**
	 * Determines if the passed mask is enabled for this socket state
	 * @param mask the mask to test
	 * @return true if the passed mask is enabled for this socket state, false otherwise
	 */
	public boolean isEnabled(int mask) {		
		return (mask | this.mask) == mask;
	}
	
	/**
	 * Enables the passed mask for this socket state and returns it
	 * @param mask The mask to modify
	 * @return the modified mask
	 */
	public int enable(int mask) {
		return (mask | this.mask);
	}
	
	/**
	 * Disables the passed mask for this socket state and returns it
	 * @param mask The mask to modify
	 * @return the modified mask
	 */
	public int disable(int mask) {
		return (mask & ~this.mask);
	}
	
	/**
	 * Returns an array of socket states that enabled in the passed mask
	 * @param mask The masks to get the states for 
	 * @return an array of socket states that enabled in the passed mask
	 */
	public static TCPSocketState[] getEnabledStates(int mask) {
		Set<TCPSocketState> enabled = new HashSet<TCPSocketState>();
		for(TCPSocketState t: values()) {
			if(t.isEnabled(mask)) enabled.add(t);
		}
		return enabled.toArray(new TCPSocketState[enabled.size()]);
	}
	
	/**
	 * Returns a compound name representing all the socket states that enabled in the passed mask
	 * @param mask The masks to get the states for 
	 * @return a compound name representing all the socket states that enabled in the passed mask
	 */
	public static String getEnabledStatesName(int mask) {
		return Arrays.toString(getEnabledStates(mask)).replace("[", "").replace("]", "").replace(" ", "").replace(',', '|');
	}
	
	/**
	 * Returns the masked int for the passed NetStat socket state int array
	 * @param arr a NetStat socket state int array (an array of 14 ints)
	 * @return a socket state mask
	 */
	public static int getMaskedArray(int[] arr) {
		return enable(valueOf(arr));
	}
	
	
	
	public static void main(String[] args) {
		for(TCPSocketState t: values()) {
			//SimpleLogger.info(t.name(), "[", t.mask, "]:", Integer.toBinaryString(t.mask));
		}
		int mask = enable(TCP_CLOSE_WAIT, TCP_FIN_WAIT2);
		SimpleLogger.info("CloseWait and FinWait2:", mask, "[", Integer.toBinaryString(mask), "]");
		for(TCPSocketState t: values()) {
			if(t.isEnabled(mask)) SimpleLogger.info("Enabled for [", t, "]");
		}
		SimpleLogger.info("Enabled for All CloseWait and FinWait2:", enabledForAll(mask, TCP_CLOSE_WAIT, TCP_FIN_WAIT2), "  ", getEnabledStatesName(mask)); 
		SimpleLogger.info("Enabled for All CloseWait and FinWait2 and TimeWait:", enabledForAll(mask, TCP_CLOSE_WAIT, TCP_FIN_WAIT2, TCP_TIME_WAIT), "  ", getEnabledStatesName(mask));
		SimpleLogger.info("Adding TimeWait");
		mask = TCP_TIME_WAIT.enable(mask);
		SimpleLogger.info("Enabled for All CloseWait and FinWait2 and TimeWait:", enabledForAll(mask, TCP_CLOSE_WAIT, TCP_FIN_WAIT2, TCP_TIME_WAIT), "  ", getEnabledStatesName(mask));
		SimpleLogger.info("Disabling TimeWait");
		mask = TCP_TIME_WAIT.disable(mask);
		SimpleLogger.info("Enabled for All CloseWait and FinWait2 and TimeWait:", enabledForAll(mask, TCP_CLOSE_WAIT, TCP_FIN_WAIT2, TCP_TIME_WAIT), "  ", getEnabledStatesName(mask));
		
	}
	
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
	 * Decodes the passed array of states to an array of TCPSocketStates.
	 * Ignores any invalid indexes in the array
	 * @param codes A netstat array of socket state codes
	 * @return the decoded TCPSocketState array
	 */
	public static TCPSocketState[] valueOf(int...codes) {
		int[] c = nvl(codes, "TCPSocketState Codes");
		Set<TCPSocketState> states = new HashSet<TCPSocketState>();
		for(int i = 1; i < codes.length; i++) {
			if(c[i] > 0) try { states.add(TCPSocketState.valueOf(i)); } catch (Exception e) {}
		}
		return states.toArray(new TCPSocketState[states.size()]);
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
