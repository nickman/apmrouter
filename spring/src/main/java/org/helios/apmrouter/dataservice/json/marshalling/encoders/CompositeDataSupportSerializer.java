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
package org.helios.apmrouter.dataservice.json.marshalling.encoders;

import java.lang.reflect.Type;

import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * <p>Title: CompositeDataSupportSerializer</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.dataservice.json.marshalling.encoders.CompositeDataSupportSerializer</code></p>
 */

public class CompositeDataSupportSerializer implements JsonSerializer<CompositeDataSupport> {

	/**
	 * {@inheritDoc}
	 * @see com.google.gson.JsonSerializer#serialize(java.lang.Object, java.lang.reflect.Type, com.google.gson.JsonSerializationContext)
	 */
	@Override
	public JsonElement serialize(CompositeDataSupport src, Type typeOfSrc, JsonSerializationContext context) {
		JsonObject json = new JsonObject();
		CompositeType ctype = src.getCompositeType();		
		for(String key: ctype.keySet()) {
			if(src.containsKey(key)) {
				OpenType<?> oType = ctype.getType(key);
				if(oType instanceof SimpleType) {
					json.addProperty(key, src.get(key).toString());
				} else  {
					json.add(key, context.serialize(src.get(key)));
				}				
			}
		}
		return json;
	}

}
