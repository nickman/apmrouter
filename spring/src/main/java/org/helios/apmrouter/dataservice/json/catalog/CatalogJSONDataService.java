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

import java.util.Map;

import org.helios.apmrouter.catalog.MetricCatalogService;
import org.helios.apmrouter.dataservice.json.JSONRequestHandler;
import org.helios.apmrouter.dataservice.json.JsonRequest;
import org.helios.apmrouter.server.ServerComponentBean;
import org.jboss.netty.channel.Channel;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>Title: CatalogJSONDataService</p>
 * <p>Description: Data services for the metric catalog service</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.dataservice.json.catalog.CatalogJSONDataService</code></p>
 */
@JSONRequestHandler(name="catalog")
public class CatalogJSONDataService extends ServerComponentBean {
	/** The metric catalog service */
	protected MetricCatalogService catalog = null;

	/**
	 * Sets the metric catalog service
	 * @param catalog the metric catalog service
	 */
	@Autowired(required=true)
	public void setCatalog(MetricCatalogService catalog) {
		this.catalog = catalog;
	}
	
	/**
	 * Returns a JSON list of hosts
	 * @param request The JSON request
	 * @param channel The channel to respond on
	 * @throws JSONException thrown on JSON marshalling errors
	 */
	@JSONRequestHandler(name="listhosts")
	public void listHosts(JsonRequest request, Channel channel)  throws JSONException {
		boolean onlineOnly = request.getArgument(0, false);
		Map<Integer, String> hosts = catalog.listHosts(onlineOnly);
		JSONObject response = new JSONObject();
		response.putOnce("t", "listhosts");
		response.put("data", hosts);
		channel.write(response);
	}
}
