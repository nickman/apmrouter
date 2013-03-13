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
package org.helios.apmrouter.dataservice.json.marshalling.encoders;

import java.lang.reflect.Type;

import org.helios.apmrouter.metric.IMetric;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * <p>Title: IMetricSerializer</p>
 * <p>Description: Json serializer for {@link IMetric} instances</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.dataservice.json.marshalling.encoders.IMetricSerializer</code></p>
 */

public class IMetricSerializer implements JsonSerializer<IMetric> {

	/**
	 * Creates a new IMetricSerializer
	 */
	public IMetricSerializer() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * {@inheritDoc}
	 * @see com.google.gson.JsonSerializer#serialize(java.lang.Object, java.lang.reflect.Type, com.google.gson.JsonSerializationContext)
	 */
	@Override
	public JsonElement serialize(IMetric src, Type typeOfSrc, JsonSerializationContext context) {
		JsonObject jo = new JsonObject();
		jo.addProperty("agent", src.getAgent());
		jo.addProperty("host", src.getHost());
		jo.addProperty("fqn", src.getFQN());
		jo.addProperty("name", src.getName());
		jo.addProperty("typename", src.getType().name());
		jo.addProperty("typeid", src.getType().ordinal());
		jo.addProperty("time", src.getTime());
		jo.addProperty("id", src.getToken());		
		jo.add("fqn", context.serialize(src.getValue()));
		return jo;
	}

}
