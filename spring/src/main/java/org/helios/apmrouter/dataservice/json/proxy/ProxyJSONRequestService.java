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
package org.helios.apmrouter.dataservice.json.proxy;

import java.util.Map;

import org.helios.apmrouter.dataservice.json.DataServiceUtils;
import org.helios.apmrouter.dataservice.json.JSONRequestHandler;
import org.helios.apmrouter.server.unification.pipeline.http.proxy.HttpRequestProxy;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.UpstreamMessageEvent;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.http.QueryStringEncoder;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>Title: ProxyJSONRequestService</p>
 * <p>Description: A JSON data service that dispatches to the defined proxy.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.dataservice.json.proxy.ProxyJSONRequestService</code></p>
 */
@JSONRequestHandler(name="http")
public class ProxyJSONRequestService {
	/** The proxy request handler */
	@Autowired(required=true)
	protected HttpRequestProxy requestProxy = null;

	/**
	 * <p>Accepts a JSON encoded request for a proxy call to an HTTP endpoint. Parameters are:<ol>
	 * 	<li><b>hostport</b>: An optional destination specification in the form of <b><code>[hostname]&lt;:[port]&gt;</code></b>. If not specified, request will be routed within the current http context.</li>
	 * 	<li><b>uri</b>: The mandatory URI of the http request to be proxied.</li>
	 *  <li><b>method</b>: The method http request to be proxied. Defaults to <b>GET</b></li>
	 * 	<li><b>args</b>: An optional map of parameters to attach to the forwarded HTTP request</li>
	 * 	<li><b>hdrs</b>: An optional map of headers to attach to the forwarded HTTP request</li>
	 * 	<li><b>creds</b>: Simple HTTP auth credentials. <b>Not Implemented</b></li> 
	 * </ol></p>
	 * @param request The JSON request
	 * @param channel The channel through which the request came
	 * @throws JSONException thrown on any JSON unmarshalling exception
	 */
	@JSONRequestHandler(name="req")
	public void proxyRequest(JSONObject request, Channel channel)  throws JSONException {
		String uri = null;
		
		if(!request.has("uri")) {
			throw new JSONException("No uri supplied");			
		}
		uri = request.getString("uri");
		
		
		String method = null;
		String hostPort = null;
		Map<String, Object> params = null;
		Map<String, Object> headers = null;
		if(request.has("method")) {
			method = request.getString("method").trim().toUpperCase();
		} else {
			method = "GET";
		}
		if(request.has("hostport")) hostPort = request.getString("hostport");
		if(request.has("args")) params = DataServiceUtils.getMap(request, "args");
		if(request.has("headers")) headers = DataServiceUtils.getMap(request, "headers");
		QueryStringEncoder encoder = new QueryStringEncoder(uri);
		if(params!=null) {
			for(Map.Entry<String, Object> entry: params.entrySet()) {
				encoder.addParam(entry.getKey(), entry.getValue().toString());				
			}
		}
		
		HttpRequest proxyRequest = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.valueOf(method), encoder.toString());
		if(headers!=null) {
			for(Map.Entry<String, Object> entry: headers.entrySet()) {
				proxyRequest.addHeader(entry.getKey(), entry.getValue());
			}
		}
		if(hostPort==null) {
			// Need to insert an interceptor in the pipeline so we can get our response back
			ChannelFuture cf = Channels.future(channel);
			channel.getPipeline().getContext(channel.getPipeline().getFirst()).sendUpstream(new UpstreamMessageEvent(channel, proxyRequest, channel.getRemoteAddress()));
			
		} else {
			
		}
	}
	
}
