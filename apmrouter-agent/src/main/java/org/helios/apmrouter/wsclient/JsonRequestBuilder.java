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
package org.helios.apmrouter.wsclient;

import java.util.Stack;
import java.util.concurrent.atomic.AtomicLong;

import org.json.JSONObject;



/**
 * <p>Title: JsonRequestBuilder</p>
 * <p>Description: A fluent style builder for JSON formatted requests to the server </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.wsclient.JsonRequestBuilder</code></p>
 */

public class JsonRequestBuilder {
	/** A stack of json objects that are being push into and popped out of focus */
	protected final Stack<JSONObject> jsonStack = new Stack<JSONObject>();	
	/** A request id generator */
	protected static final AtomicLong requestIdSerial = new AtomicLong();
	
	/**
	 * Creates a new JsonRequestBuilder
	 */
	protected JsonRequestBuilder() {
		JSONObject root = new JSONObject();
		try { root.put("rid", requestIdSerial.incrementAndGet()); } catch (Exception ex) {}
	}
	/*
	EXAMPLE:
	=======
			JSONObject request = new JSONObject();
			int reqId = client.requestSerial.incrementAndGet();
			request.put("rid", reqId);
			request.put("t", "req");
			request.put("svc", "sub");
			request.put("op", "start");
			JSONObject ags = new JSONObject();
			ags.put("es", "jmx");
			ags.put("esn", "service:jmx:local://DefaultDomain");
			ags.put("f", "org.helios.apmrouter.session:service=SharedChannelGroup");
			request.put("args", ags);
	
	 */
	
}
