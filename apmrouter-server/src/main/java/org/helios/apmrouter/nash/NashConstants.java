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
package org.helios.apmrouter.nash;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.helios.apmrouter.nash.NashConstants;

/**
 * <p>Title: NashConstants</p>
 * <p>Description: Constants used in decoding client input</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.nash.NashConstants</code></p>
 */

public class NashConstants {
	//=========================================================================================
	//		Misc Items
	//=========================================================================================
	/** The default listening port */
	public static final int DEFAULT_PORT = 2113;
	//=========================================================================================
	//		Payload Type Chunks
	//=========================================================================================
	
	private static final Map<Byte, String> DECODES = new HashMap<Byte, String>();
	
	static {
		try {
			 for(Field f: NashConstants.class.getDeclaredFields()) {
				 if(f.getType().equals(byte.class)) {
					 DECODES.put(f.getByte(null), f.getName());
				 }
			 }
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	
	/**
	 * Decodes a byte marker to its textual name
	 * @param b The byte marker
	 * @return The textual name or null if it could not be decoded.
	 */
	public static String decode(byte b) {
		return DECODES.get(b);
	}
	/**
	 * Chunk type marker for command line arguments
	 */
	public static final byte CHUNKTYPE_ARGUMENT = 'A';

	/**
	 * Chunk type marker for client environment variables
	 */
	public static final byte CHUNKTYPE_ENVIRONMENT = 'E';
	
	/**
	 * Chunk type marker for the command (alias or class)
	 */
	public static final byte CHUNKTYPE_COMMAND = 'C';
	
	/**
	 * Chunk type marker for client working directory
	 */	
	public static final byte CHUNKTYPE_WORKINGDIRECTORY = 'D';
	
	//=========================================================================================
	//		Service Type Chunks
	//=========================================================================================
	
	/**
	 * Chunk type marker for stdin
	 */
	public static final byte CHUNKTYPE_STDIN = '0';

	/**
	 * Chunk type marker for the end of stdin
	 */
	public static final byte CHUNKTYPE_STDIN_EOF = '.';

	/**
	 * Chunk type marker for stdout
	 */
	public static final byte CHUNKTYPE_STDOUT = '1';
	
	/**
	 * Chunk type marker for stderr
	 */	
	public static final byte CHUNKTYPE_STDERR = '2';
	
	/**
	 * Chunk type marker for client exit chunks
	 */	
	public static final byte CHUNKTYPE_EXIT = 'X';
	


    /**
     * Chunk type marker for a "startinput" chunk.
     * This chunk type is sent from the server to the client and indicates
     * that the client should begin sending stdin to the server.  It
     * is automatically sent the first time the client's inputstream
     * is read.
     */
    public static final byte CHUNKTYPE_STARTINPUT = 'S';
    
	//=========================================================================================


    /** A publically accessible set of the valid chunk types */
    public static final Set<Byte> VALID_CHUNK_TYPES = Collections.unmodifiableSet(getChunkTypes());
    
    /**
     * Creates a set of all the valid chunk types
     * @return a set of all the valid chunk types
     */
    private static Set<Byte> getChunkTypes() {
    	Set<Byte> types = new HashSet<Byte>();
    	try {
    		for(Field f: NashConstants.class.getDeclaredFields()) {
    			if(!Modifier.isStatic(f.getModifiers())) continue;
    			if(f.getName().startsWith("CHUNKTYPE_")) continue;
    			if(!f.getType().equals(byte.class)) continue;
    			types.add(f.getByte(null));
    			
    		}
    	} catch (Exception e) {
    		String msg = "Failed to compile set of chunk types";
    		System.err.println(msg + ":" + e);
    		throw new RuntimeException(msg, e);
    	}
    	return types;
    }

}
