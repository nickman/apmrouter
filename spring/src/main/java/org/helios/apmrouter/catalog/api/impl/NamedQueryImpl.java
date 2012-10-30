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
package org.helios.apmrouter.catalog.api.impl;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/**
 * <p>Title: NamedQueryImpl</p>
 * <p>Description: An encapsulation of a hibernate named query request</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.catalog.api.impl.NamedQueryImpl</code></p>
 */

public class NamedQueryImpl {
	/** The named query name */
	protected String name = null;
	/** The parameters */
	@SerializedName("p") 
	protected Map<String,?> parameters = new HashMap<String, Object>();
	
	
	public static void main(String[] args) {
		log("NamedQuery Unmarshall Test");
		final String jsonText = "{ 'name' : 'findAgentsByHost', 'p' : { 'hostId' : 1 } }";
		Gson gson = new Gson();
		NamedQueryImpl impl = gson.fromJson(jsonText, NamedQueryImpl.class);
		log("Impl:" + impl);
		
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("NamedQueryImpl [name=");
		builder.append(name);
		builder.append(", parameters=");
		builder.append(parameters);
		builder.append("]");
		return builder.toString();
	}
	
	
}
