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
package org.helios.apmrouter.collections;

/**
 * <p>Title: PartialArrayOverflowException</p>
 * <p>Description: Exception thrown when an addition of items to an array partially completes.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.collections.PartialArrayOverflowException</code></p>
 */

public class PartialArrayOverflowException extends ArrayOverflowException {
	/**  */
	private static final long serialVersionUID = 5930277580751072763L;
	/** The number of items successfully inserted */
	private final int succeeded;
	/**
	 * Creates a new PartialArrayOverflowException
	 * @param succeeded The number of items successfully inserted
	 * @param message The exception message
	 * @param cause The exception cause
	 */
	public PartialArrayOverflowException(int succeeded, String message, Throwable cause) {
		super(message, cause);
		this.succeeded = succeeded;
	}
	
	/**
	 * Returns the number of items successfully inserted
	 * @return the number of items successfully inserted
	 */
	public int getSucceeded() {
		return succeeded;
	}

}
