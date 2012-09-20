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
package org.helios.apmrouter.sentry;

/**
 * <p>Title: SentryState</p>
 * <p>Description: Enumerates the states that a sentry supporting object can be in.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.sentry.SentryState</code></p>
 */

public enum SentryState {
	/** Object has not enrolled with sentry yet */
	PENDING,
	/** Object is in callback state where a callback or exception will trigger a state change */
	CALLBACK,
	/** Object requires sentry polling to check its state */
	POLLING,
	/** Object has lost desired state and requires sentry to periodically execute a connection task to re-acquire state */
	DISCONNECTED,
	/** Object has deliberately exited from sentry watch */
	CANCELLED;
}
