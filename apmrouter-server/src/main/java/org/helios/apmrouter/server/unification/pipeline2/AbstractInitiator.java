/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2013, Helios Development Group and individual contributors
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

import org.apache.log4j.Logger;


/**
 * <p>Title: AbstractInitiator</p>
 * <p>Description: A base class for initiators</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.unification.pipeline2.AbstractInitiator</code></p>
 */

public abstract class AbstractInitiator implements Initiator {
	/** The number of bytes required for this Initiator to do its job */
	protected final int requiredBytes;
	/** The name of this Initiator */
	protected final String name;
	
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	
	/**
	 * Creates a new AbstractInitiator
	 * @param requiredBytes The number of bytes required for this Initiator to do its job
	 * @param name The name of this Initiator
	 */
	public AbstractInitiator(int requiredBytes, String name) {
		this.requiredBytes = requiredBytes;
		this.name = name;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.unification.pipeline2.Initiator#requiredBytes()
	 */
	@Override
	public int requiredBytes() {
		return requiredBytes;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.unification.pipeline2.Initiator#requiresFullPayload()
	 */
	@Override
	public boolean requiresFullPayload() {	
		return false;
	}	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.unification.pipeline2.Initiator#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

}
