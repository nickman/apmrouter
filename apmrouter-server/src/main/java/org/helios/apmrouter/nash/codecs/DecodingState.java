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
package org.helios.apmrouter.nash.codecs;

import java.util.HashMap;
import java.util.Map;

import org.helios.apmrouter.nash.NashConstants;
import org.helios.apmrouter.nash.codecs.DecodingState;
import org.helios.apmrouter.nash.codecs.NashRequestDecoder;


/**
 * <p>Title: DecodingState</p>
 * <p>Description: Enumerates the decoding states for {@link NashRequestDecoder}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.nash.codecs.DecodingState</code></p>
 */

public enum DecodingState {
	BYTES((byte)-1),
	TYPE((byte)-2),
    COMMAND(NashConstants.CHUNKTYPE_COMMAND, false),
    WORKING_DIR(NashConstants.CHUNKTYPE_WORKINGDIRECTORY, false),
    ENVIRONMENT(NashConstants.CHUNKTYPE_ENVIRONMENT, false),
    ARGUMENTS(NashConstants.CHUNKTYPE_ARGUMENT, false),
    STDIN(NashConstants.CHUNKTYPE_STDIN),
    STDIN_EOF(NashConstants.CHUNKTYPE_STDIN_EOF),
    STDOUT(NashConstants.CHUNKTYPE_STDOUT),
    STDERR(NashConstants.CHUNKTYPE_STDERR),
    EXIT(NashConstants.CHUNKTYPE_EXIT),
    STARTINPUT(NashConstants.CHUNKTYPE_STARTINPUT),
    DEBUG((byte)Byte.MAX_VALUE);
    
	/**
	 * Returns the DecodingState for the passed chunkType
	 * @param chunkType The chunktype to get the DecodingState for
	 * @return a DecodingState or null in which case the chunk type was not recognized or not a valid state
	 */
	public static DecodingState getState(byte chunkType) {
		return BYTE2STATE.get(chunkType);
	}
	
	private DecodingState(byte chunkType, boolean service) {
		this.chunkType = chunkType;
		this.service = service;
	}
	
	private DecodingState(byte chunkType) {
		this(chunkType, true);
	}
	
	private final byte chunkType;
	private final boolean service;
	
	private static final Map<Byte, DecodingState> BYTE2STATE = new HashMap<Byte, DecodingState>(DecodingState.values().length);
	
	static {
		for(DecodingState ds: DecodingState.values()) {
			if(ds.chunkType>=0) BYTE2STATE.put(ds.chunkType, ds);
		}
	}
	
//	public boolean isService() {
//		return service;
//	}
	
}
