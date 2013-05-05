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
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.helios.apmrouter.server.ServerComponentBean;
import org.helios.apmrouter.util.StringHelper;
import org.jboss.netty.channel.Channel;
import org.json.JSONArray;
import org.json.JSONException;
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
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	/** The key indicating a request is being sent (as opposed to .... ) */
	public static final String REQUEST_FLAG = "t";
	/** The request ID sent by the client which will be returned with every response */
	public static final String REQUEST_ID = "rid";
	/** The correlation id in reference to the original request. i.e. if a subscription request comes in as <code>43</code>,
	 * all of the published events for that subscription will have a <b><code>rerid</code></b> of <code>43</code> */
	public static final String RE_REQUEST_ID = "rerid";
	/** The name of the json data service the client wishes to invoke */
	public static final String SERVICE_NAME = "svc";
	/** The name of the json data service operation the client wishes to invoke */
	public static final String OP_NAME = "op";
	/** The name of the json key for the client submitted arguments to the op */
	public static final String ARGS_NAME = "args";
	/** The name of the json key for a response error message */
	public static final String ERR_NAME = "err";
	
	
	/**
	 * Common JSON request parser 
	 * @param request The JSON request
	 * @param channel The channel the request was received from (and where the response should be written back to)
	 * @return the parsed request
	 * @throws JSONException thrown on any JSON unmarshalling error
	 */
	protected JsonRequest parse(JSONObject request, Channel channel)  throws JSONException  {
		try {
			String flag = request.getString(REQUEST_FLAG);
			long requestId = request.getLong(REQUEST_ID);
			String service = request.getString(SERVICE_NAME);
			String op = request.getString(OP_NAME);
			JsonRequest jreq = new JsonRequest(channel, flag, requestId, service, op, request);
			if(request.has(ARGS_NAME)) {
				JSONArray arr = null;
				try {
					arr = request.getJSONArray(ARGS_NAME);
					for(int i = 0; i < arr.length(); i++) {
						jreq.addArg(i, arr.get(i));
					}
				} catch (Exception ex) {}
				if(arr==null) {
					JSONObject map = request.getJSONObject(ARGS_NAME);
					for(Iterator<?> iter = map.keys(); iter.hasNext();) {
						Object key = iter.next();
						jreq.addArg(key.toString(), map.get(key.toString()));
					}
				}
			}
			return jreq;
		} catch (JSONException ex) {
			throw ex;
		} catch (Exception ex) {
			log.error("Unexpected exception parsing request [" + request + "]", ex);
			throw new RuntimeException("Unexpected exception parsing request [" + request + "]", ex);
		}
	}
	
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
				info("Added JSON Handler Service [", entry.getKey(), "]");				
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
	
	
	/**
	 * Attempts to decode and invoke the passed request
	 * @param request A JSON encoded request
	 * @param channel The channel to respond on
	 */
	public void invoke(JSONObject request, Channel channel) {
		try {
			JsonRequest req = parse(request, channel);
			Map<String, JSONRequestHandlerImpl> service = services.get(req.serviceName);
			if(service!=null) {
				JSONRequestHandlerImpl handler = service.get(req.opName);
				handler.processRequest(req, channel);
			} else {
				req.error(StringHelper.fastConcat("JSON Invocation from [", channel.toString(), "] for req [", req.serviceName, "/", req.opName, "] failed. Could not locate service/op")).send(channel);
			}
		} catch (Exception ex) {
			error("Failed to invoke request", ex);
			long requestId = -1;
			try {
				requestId = request.getLong(REQUEST_ID);
			} catch (Exception e) {}
			sendError(ex, requestId, channel);
		}
	}
	
	/**
	 * Formats a throwable into a JSON error message and sends to the caller
	 * @param t The throwable to report
	 * @param requestId The request Id that failed
	 * @param channel The channel to write to
	 */
	protected void sendError(Throwable t, long requestId, Channel channel) {
		try {
			JSONObject msg = new JSONObject();
			msg.put(REQUEST_FLAG, "err");
			msg.put(REQUEST_ID, requestId);
			msg.put(ERR_NAME, t.toString());
			channel.write(msg);
		} catch (Exception ex) {
			error("Failed to write error message to client [", t, "]", ex);
		}
	}
	
	
}
