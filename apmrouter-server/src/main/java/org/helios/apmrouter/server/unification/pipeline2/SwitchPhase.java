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
package org.helios.apmrouter.server.unification.pipeline2;

/**
 * <p>Title: SwitchPhase</p>
 * <p>Description: Enumerates the possible phases of a protocol switch negotiation</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.unification.pipeline2.SwitchPhase</code></p>
 */

public enum SwitchPhase {
	/** Negotiation just started */
	INIT,
	/** Detected compression (or some other encoding) */
	COMPDETECT,
	/** Decompress (or decode) */
	DECOMP,
	/** Classify the content */
	CONTENTDETECT,
	/** Handle the content */
	CONTENT,
	/** The request is complete so far as switching is concerned */
	COMPLETE,
	/** The switch could not be completed */
	ERROR;
}
