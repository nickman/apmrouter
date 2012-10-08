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
package org.helios.apmrouter.sender.netty.codec;


/**
 * <p>Title: IMetricDecodePhase</p>
 * <p>Description: Enumerates the phasesof an IMetric decode</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.sender.netty.codec.IMetricDecodePhase</code></p>
 */

public enum IMetricDecodePhase {
	/** Reads the byte order of the incoming metric */
	BYTE_ORDER,
	/** Determines if the incoming metric ID is tokenized */
	ISTOKENIZED,  
	// if tokenized, next is TIMESTAMP, otherwise: TYPE->FQNLENGTH->FQN->TIMESTAMP
	/** The incoming metric type */
	TYPE,		
	/** The length of the incoming FQN */
	FQNLENGTH,
	/** The incoming FQN bytes */
	FQN,
	/** The incoming metric timestamp */
	TIMESTAMP,
	// if long, goto LONG, otherwise: VLENGTH, VALUE
	/** The incoming metric long value */
	LONGVALUE,         	
	/** The length of the incoming non-long value */
	VLENGTH, 
	/** The incoming non-long value */
	VALUE;
	
	
}
