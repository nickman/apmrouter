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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.SerializedName;

/**
 * <p>Title: JsonResponse</p>
 * <p>Description: The standard object container for sending a response to a JSON data service caller</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.dataservice.json.JsonResponse</code></p>
 */

public class JsonResponse {
	/** The client provided request ID that this response is being sent for */
	@SerializedName("rerid")
	protected final long reRequestId;
	@SerializedName("t")
	protected final String type;
	/** The content payload */
	@SerializedName("msg")
	protected Object content = null;
	
	/** Response flag for an error message */
	public static final String RESP_TYPE_ERR = "err";
	/** Response flag for a request response */
	public static final String RESP_TYPE_RESP = "resp";
	/** Response flag for a subscription event delivery */
	public static final String RESP_TYPE_SUB = "sub";
	
	/**
	 * Creates a new JsonResponse
	 * @param reRequestId The client provided request ID that this response is being sent for
	 * @param type The type flag. Currently "err" for an error message, "resp" for a response, "sub" for subcription event
	 */
	public JsonResponse(long reRequestId, String type) {
		super();
		this.reRequestId = reRequestId;
		this.type = type;
	}
	
	public static void main(String[] args) {
		log("GSON Test");
		JsonResponse resp = new JsonResponse(3, RESP_TYPE_RESP);
		Map<Integer, String> hosts = new HashMap<Integer, String>();
		int cnt = 0;
		List<String> sp = new ArrayList<String>(System.getProperties().stringPropertyNames());
		while(cnt < 10) {
			hosts.put(cnt, sp.get(cnt) + ":" + System.getProperty(sp.get(cnt)));
			cnt++;
		}
		resp.setContent(hosts);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		log(gson.toJson(resp));
	}
	
	private static class MapSerializer implements JsonSerializer<Map<?,?>> {
		public JsonElement serialize(Map<?,?> src, Type typeOfSrc, JsonSerializationContext context) {
			if(!src.isEmpty()) {
				Map.Entry<?, ?> entry = src.entrySet().iterator().next();
				boolean keysAreNumbers = isNumber(entry.getKey());
				boolean valuesAreNumbers = isNumber(entry.getValue());
				if(keysAreNumbers || valuesAreNumbers) {
					JsonObject map = new JsonObject();
				}
			}
			return context.serialize(src, typeOfSrc);
		}
		
		private static boolean isNumber(Object obj) {
			if(obj==null) return false;
			if(obj instanceof Number) return true;
			try {
				Double.parseDouble(obj.toString());
				return true;
			} catch (Exception ex) {
				return false;
			}
		}
	}

	public static void log(Object msg) {
		System.out.println(msg);
	}

	/**
	 * Returns the content payload
	 * @return the content
	 */
	public Object getContent() {
		return content;
	}

	/**
	 * Sets the payload content
	 * @param content the content to set
	 * @return this json response
	 */
	public JsonResponse setContent(Object content) {
		this.content = content;
		return this;
	}

	/**
	 * Returns the in reference to request id
	 * @return the in reference to request id
	 */
	public long getReRequestId() {
		return reRequestId;
	}


	/**
	 * Returns the type flag
	 * @return the type
	 */
	public String getType() {
		return type;
	}
	
	
}
