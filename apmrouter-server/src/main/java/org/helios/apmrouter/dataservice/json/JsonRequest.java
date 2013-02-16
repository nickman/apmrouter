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

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * <p>Title: JsonRequest</p>
 * <p>Description: Encapsulates the decoded standard parts of a JSON data service request.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.dataservice.json.AbstractJSONDataService.JsonRequest</code></p>
 */
public class JsonRequest {
	/** the type code of the request */
	public final String tCode;
	/** The client supplied request ID */
	public final long rid;
	/** The requested service name */
	public final String serviceName;
	/** The requested op name */
	public final String opName;
	/** The original request, in case there is other stuff in there that the data service needs */
	public final JSONObject request;
	
	/** The arguments supplied to the op */
	public final Map<Object, Object> arguments = new TreeMap<Object, Object>();
	
	/**
	 * Creates a new JsonRequest
	 * @param tCode the type code of the request
	 * @param rid The client supplied request ID
	 * @param serviceName The service name requested
	 * @param opName The op name requested
	 * @param request The original request
	 */
	public JsonRequest(String tCode, long rid, String serviceName, String opName, JSONObject request) {
		this.tCode = tCode;
		this.rid = rid;
		this.serviceName = serviceName;
		this.opName = opName;
		this.request = request;
	}
	
	/**
	 * Returns a {@link JsonResponse} for this request
	 * @return a {@link JsonResponse} for this request
	 */
	public JsonResponse response() {
		return new JsonResponse(rid, JsonResponse.RESP_TYPE_RESP);
	}
	
	/**
	 * Returns a subscription send {@link JsonResponse} for the subscription issued by this request
	 * @return a subscription send {@link JsonResponse} for the subscription issued by this request
	 */
	public JsonResponse subResponse() {
		return new JsonResponse(rid, JsonResponse.RESP_TYPE_SUB);
	}	
	
	/**
	 * Adds an op argument to the map
	 * @param key The argument key (if the args was an array, this is the sequence, if it was a map, this is the key)
	 * @param value The argument value
	 */
	public void addArg(Object key, Object value) {
		arguments.put(key, value);
	}
	
	/**
	 * Returns the named argument from the argument map
	 * @param key The argument key
	 * @param defaultValue The default value to return if the key does not resolve
	 * @return the value for the passed key
	 */
	public <T> T getArgument(String key,  T defaultValue) {
		Object value = arguments.get(key);
		if(Map.class.isAssignableFrom(defaultValue.getClass()) && value instanceof JSONObject) {
			JSONObject jsonMap = (JSONObject)value;
			Map<String, Object> map = new HashMap<String, Object>();
			try {
				for(String mapKey: JSONObject.getNames(jsonMap)) {
					map.put(mapKey, jsonMap.get(mapKey));
				}
			} catch (JSONException ex) {
				throw new RuntimeException(ex);
			}
			return (T)map;
		}
			
		
		if(value==null || !defaultValue.getClass().isInstance(value)) {
			return defaultValue;
		}
		return (T)value;
	}
	
	/**
	 * Returns an argument as a string
	 * @param key The argument key
	 * @return The string value of the argument or null if no value was found
	 */
	public String getArgument(String key) {
		Object value = arguments.get(key);
		if(value!=null) return value.toString().trim();
		return null;
	}
	
	/**
	 * Returns the named argument from the argument map returning null if not found
	 * @param key The argument key
	 * @param type The expected type of the value
	 * @return the value for the passed key
	 */
	public <T> T getArgumentOrNull(String key,  Class<T> type) {
		Object value = arguments.get(key);
		if(value==null) {
			return null;
		}
		return (T)value;
	}
	
	
	/**
	 * Returns the indexed argument from the argument array
	 * @param index The argument index
	 * @param defaultValue The default value to return if the key does not resolve
	 * @return the value for the passed index
	 */
	public <T> T getArgument(int index,  T defaultValue) {
		Object value = arguments.get(index);
		if(value==null || !defaultValue.getClass().isInstance(value)) {
			return defaultValue;
		}
		return (T)value;
	}
	
}