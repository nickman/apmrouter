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
import java.util.concurrent.ConcurrentHashMap;

import org.helios.apmrouter.server.ServerComponentBean;
import org.jboss.netty.channel.Channel;
import org.json.JSONObject;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.jmx.export.annotation.ManagedAttribute;

/**
 * <p>Title: JSONRequestRouter</p>
 * <p>Description: Examines JSON requests and routes them to the correct {@link JSONDataService} instance.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.dataservice.json.JSONRequestRouter</code></p>
 */

public class JSONRequestRouter extends ServerComponentBean {
	/** A map of {@link JSONDataService}s keyed by the service name */
	protected final Map<String, Map<String, JSONRequestHandlerImpl>> services = new ConcurrentHashMap<String, Map<String, JSONRequestHandlerImpl>>();



	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ApplicationContextLifecycleListener#onApplicationContextRefresh(org.springframework.context.event.ContextRefreshedEvent)
	 */
	@Override
	public void onApplicationContextRefresh(ContextRefreshedEvent event) {
		Map<String, Object> handlerServices = applicationContext.getBeansWithAnnotation(JSONRequestHandler.class);
		info("Processing [", handlerServices.size(), "] JSON Handler Services");
		for(Map.Entry<String, Object> entry: handlerServices.entrySet()) {
			Map<String, JSONRequestHandlerImpl> ops = JSONRequestHandlerImpl.generateHandlers(entry.getKey(), entry.getValue());
			if(ops.size()<1) {
				warn("JSON Service [", entry.getKey(), "] had zero ops");
			} else {
				services.put(entry.getKey(), ops);
			}
		}
	}
	
	/**
	 * Returns a map of service names and ops
	 * @return A map of arrays of ops keyed by the service name
	 */
	@ManagedAttribute(description="A map of arrays of ops keyed by the service name")
	public Map<String, String> getServiceOps() {
		Map<String, String> map = new HashMap<String, String>(services.size());
		for(Map.Entry<String, Map<String, JSONRequestHandlerImpl>> entry: services.entrySet()) {
			map.put(entry.getKey(), entry.getValue().keySet().toString());
		}
		return map;
	}
	
	public static final String SERVICE_KEY = "svc";
	public static final String OP_KEY = "op";
	
	/**
	 * Attempts to decode and invoke the passed request
	 * @param request A JSON encoded request
	 * @param channel The channel to respond on
	 */
	public void invoke(JSONObject request, Channel channel) {
		try {
			String svc = request.getString(SERVICE_KEY);
			String op = request.getString(OP_KEY);
			Map<String, JSONRequestHandlerImpl> service = services.get(svc);
			if(service!=null) {
				JSONRequestHandlerImpl handler = service.get(op);
				handler.processRequest(request, channel);
			}
		} catch (Exception ex) {
			error("Failed to invoke request", ex);
		}
	}
	
	
}
