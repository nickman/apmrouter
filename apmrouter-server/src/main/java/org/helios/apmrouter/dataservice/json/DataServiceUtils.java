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

import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * <p>Title: DataServiceUtils</p>
 * <p>Description: Static helper classes for JSON data services.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.dataservice.json.DataServiceUtils</code></p>
 */

public class DataServiceUtils {
	
	/**
	 * Extracts a map from a json request
	 * @param json The json request to extract the map from 
	 * @param keys The keys to navigate the hirerarchy of the json object
	 * @return A map of values
	 * @throws JSONException thrown on any JSON unmarshalling error
	 */
	public static Map<String, Object> getMap(JSONObject json, String...keys) throws JSONException {
		if(json==null) throw new IllegalArgumentException("The passed JSON object was null", new Throwable());
		JSONObject target = json;
		Map<String, Object> results = new LinkedHashMap<String, Object>();
		for(String key: keys) {
			target = target.getJSONObject(key);
		}
		for(String key: JSONObject.getNames(target)) {
			results.put(key, target.get(key));
		}
		return results;
	}
}
