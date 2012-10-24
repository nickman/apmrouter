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
package org.helios.apmrouter.dataservice.json;

/**
 * <p>Title: JsonResponse</p>
 * <p>Description: The standard object container for sending a response to a JSON data service caller</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.dataservice.json.JsonResponse</code></p>
 */

public class JsonResponse {
	/** The client provided request ID that this response is being sent for */
	protected final long reRequestId;
	/** The identifier of the subscription this message is being sent for. Should be ignored if -1 */
	protected final long subId;
	/** The type flag. Currently "err" for an error message, "resp" for a response, "sub" for subcription event */
	protected final String type;
	
	/**
	 * Creates a new JsonResponse
	 * @param reRequestId The client provided request ID that this response is being sent for
	 * @param subId The identifier of the subscription this message is being sent for. Should be ignored if -1 
	 * @param type The type flag. Currently "err" for an error message, "resp" for a response, "sub" for subcription event
	 */
	public JsonResponse(long reRequestId, long subId, String type) {
		super();
		this.reRequestId = reRequestId;
		this.subId = subId;
		this.type = type;
	}
	
	/**
	 * Creates a new JsonResponse
	 * @param reRequestId The client provided request ID that this response is being sent for
	 * @param type The type flag. Currently "err" for an error message, "resp" for a response, "sub" for subcription event
	 */
	public JsonResponse(long reRequestId, String type) {
		super();
		this.reRequestId = reRequestId;
		this.subId = -1L;
		this.type = type;
	}
	
	
}
