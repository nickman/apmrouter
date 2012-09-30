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
package org.helios.apmrouter.destination.wily;

import java.util.Collection;

import org.helios.apmrouter.destination.BaseDestination;

/**
 * <p>Title: WilyIntroscopeDestination</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.wily.WilyIntroscopeDestination</code></p>
 */

public class WilyIntroscopeDestination extends BaseDestination {

	/**
	 * Creates a new WilyIntroscopeDestination
	 * @param patterns
	 */
	public WilyIntroscopeDestination(String... patterns) {
		super(patterns);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new WilyIntroscopeDestination
	 * @param patterns
	 */
	public WilyIntroscopeDestination(Collection<String> patterns) {
		super(patterns);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new WilyIntroscopeDestination
	 */
	public WilyIntroscopeDestination() {
		// TODO Auto-generated constructor stub
	}

}
