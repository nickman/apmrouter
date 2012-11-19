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
package org.helios.apmrouter.tsmodel;

/**
 * <p>Title: IllegalTierStateException</p>
 * <p>Description: A tier parsing exception that occurs when the validation of a constructed tier fails because a calc a triplet member is not 
 * equal to the calc using the other 2</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.tsmodel.IllegalTierStateException</code></p>
 */
public class IllegalTierStateException extends InvalidTierDefinitionException {

	/**  */
	private static final long serialVersionUID = -7837093835843651717L;


	/**
	 * Creates a new IllegalTierStateException
	 * @param message The error message and erroneous tier definition
	 */
	public IllegalTierStateException(String message) {
		super(message);
	}


	/**
	 * Creates a new IllegalTierStateException
	 * @param message The error message and erroneous tier definition
	 * @param cause THe underlying cause of the defintion exception
	 */
	public IllegalTierStateException(String message, Throwable cause) {
		super(message, cause);
	}

}
