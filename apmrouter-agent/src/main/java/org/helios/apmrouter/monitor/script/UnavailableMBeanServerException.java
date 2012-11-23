/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2012, Helios Development Group and individual contributors
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
package org.helios.apmrouter.monitor.script;

/**
 * <p>Title: UnavailableMBeanServerException</p>
 * <p>Description: Runtime exception thrown when the requested MBeanServer is not available. Usually supressed. </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.monitor.script.UnavailableMBeanServerException</code></p>
 */

public class UnavailableMBeanServerException extends RuntimeException {

	/**  */
	private static final long serialVersionUID = 5445593258113248723L;

	/**
	 * Creates a new UnavailableMBeanServerException
	 */
	public UnavailableMBeanServerException() {
	}

	/**
	 * Creates a new UnavailableMBeanServerException
	 * @param message The error message
	 */
	public UnavailableMBeanServerException(String message) {
		super(message);
	}

	/**
	 * Creates a new UnavailableMBeanServerException
	 * @param cause The underlying cause
	 */
	public UnavailableMBeanServerException(Throwable cause) {
		super(cause);
	}

	/**
	 * Creates a new UnavailableMBeanServerException
	 * @param message The error message
	 * @param cause The underlying cause
	 */
	public UnavailableMBeanServerException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Creates a new UnavailableMBeanServerException
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public UnavailableMBeanServerException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		// TODO Auto-generated constructor stub
	}

}
