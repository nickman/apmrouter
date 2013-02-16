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
package org.helios.apmrouter.dataservice.json.catalog;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.engine.NamedQueryDefinition;

import com.google.gson.annotations.SerializedName;

/**
 * <p>Title: NamedQueryDefinitionContainer</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.dataservice.json.catalog.NamedQueryDefinitionContainer</code></p>
 */

public class NamedQueryDefinitionContainer {
	/** The name of the named query */
	@SerializedName("name")
	protected final String name;
	/** The parameter map */
	@SerializedName("p")
	protected final Map<String, String> params = new HashMap<String, String>();
	
	/**
	 * Creates a new NamedQueryDefinitionContainer
	 * @param name The name of the named query
	 * @param namedQueryDef The named query definition
	 */
	public NamedQueryDefinitionContainer(String name, NamedQueryDefinition namedQueryDef) {
		super();
		this.name = name;
		Set<Map.Entry<String, String>> nameTypeMap = (Set<Map.Entry<String, String>>) namedQueryDef.getParameterTypes().entrySet();
		for(Map.Entry<String, String> entry: nameTypeMap) {
			params.put(entry.getKey(), entry.getValue());			
		}
	}
	
	
	
}
