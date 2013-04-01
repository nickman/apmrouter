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
package org.helios.apmrouter.server.tracing.virtual;

/**
 * <p>Title: VirtualState</p>
 * <p>Description: Enumerates the possible states of {@link VirtualTracer}s and {@link VirtualAgent}s </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.tracing.VirtualTracerState</code></p>
 */

public enum VirtualState {
	/** The virtual instance is up but has not seen any activity yet */
	INIT(true),
	/** The virtual instance is up and active */
	UP(true),
	/** The virtual instance has timed out but is still valid  */
	SOFTDOWN(true),
	/** The virtual agent has timed so this instance has been invalidated */
	HARDDOWN(false);
	
	private VirtualState(boolean canTrace) {
		this.canTrace = canTrace;
	}
	
	private final boolean canTrace;
	
	/**
	 * Indicates if tracing is allowed when in this state
	 * @return true if tracing is allowed when in this state, false otherwise
	 */
	public boolean canTrace() {
		return canTrace;
	}
}
