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
package org.helios.apmrouter.wsclient;

import org.helios.apmrouter.sender.SynchOpSupport;
import org.json.JSONObject;

/**
 * <p>Title: JsonRequest</p>
 * <p>Description: Simple wrapper for a {@link JSONObject} that adds some bits and pieces </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.wsclient.JsonRequest</code></p>
 */

public class JsonRequest extends JSONObject {
	/** The request id */
	protected final long rid;
	/**
	 * Creates a new JsonRequest
	 */
	public JsonRequest() {
		rid = SynchOpSupport.nextRequestId();
		try { put("rid", rid); } catch (Exception ex) {/* No Op */}
	}
	
	/**
	 * Returns the request id
	 * @return the request id
	 */
	public long getRequestId() {
		return rid;
	}


}
