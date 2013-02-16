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
package org.helios.apmrouter.destination.chronicletimeseries;

/**
 * <p>Title: InvalidIndexExcetpion</p>
 * <p>Description: Runtime exception thrown when a chronicle excerpt requests an invalid index.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.chronicletimeseries.InvalidIndexExcetpion</code></p>
 */

public class InvalidIndexExcetpion extends RuntimeException {

	/**  */
	private static final long serialVersionUID = -6847715291506962973L;

	/**
	 * Creates a new InvalidIndexExcetpion
	 */
	public InvalidIndexExcetpion() {
	}

	/**
	 * Creates a new InvalidIndexExcetpion
	 * @param message
	 */
	public InvalidIndexExcetpion(String message) {
		super(message);
	}

	/**
	 * Creates a new InvalidIndexExcetpion
	 * @param cause
	 */
	public InvalidIndexExcetpion(Throwable cause) {
		super(cause);
	}

	/**
	 * Creates a new InvalidIndexExcetpion
	 * @param message
	 * @param cause
	 */
	public InvalidIndexExcetpion(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Creates a new InvalidIndexExcetpion
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public InvalidIndexExcetpion(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		// TODO Auto-generated constructor stub
	}

}
