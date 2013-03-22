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
package org.helios.collector.jmx.connection.triggers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>Title: AbstractOnFirstConnectTrigger</p>
 * <p>Description: An abstract impl of {@link OnFirstConnectTrigger}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.collector.jmx.connection.triggers.AbstractOnFirstConnectTrigger</code></p>
 */

public abstract class AbstractOnFirstConnectTrigger implements OnFirstConnectTrigger {
	/** Instance logger */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	/** Indicates if this trigger considers the connection ok */
	protected final AtomicBoolean connected = new AtomicBoolean(false); 
	/**
	 * {@inheritDoc}
	 * @see org.helios.collector.jmx.connection.triggers.ConnectTrigger#onConnectionFailed()
	 */
	@Override
	public void onConnectionFailed() {
		connected.set(false);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.collector.jmx.connection.triggers.OnFirstConnectTrigger#isFirstConnect()
	 */
	@Override
	public boolean isFirstConnect() {
		return !connected.get();
	}


}
