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
package org.helios.apmrouter.wsclient;

import java.net.SocketAddress;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.helios.apmrouter.subscription.MetricURISubscriptionEventListener;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * <p>Title: WebSocketAgent</p>
 * <p>Description: Agent implementation using websockets.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.wsclient.WebSocketAgent</code></p>
 */

public class WebSocketAgent implements WebSocketEventListener {
	/** The WebSocketClient this agent will use to comm with the server */
	protected final WebSocketClient wsClient;
	/** A set of subscribed MetricURISubscriptionEventListeners */
	protected final Set<MetricURISubscriptionEventListener> subListeners = new CopyOnWriteArraySet<MetricURISubscriptionEventListener>();
	
	/**
	 * Registers a {@link MetricURISubscriptionEventListener}
	 * @param listener the listener to register
	 */
	public void registerMetricURISubscriptionEventListener(MetricURISubscriptionEventListener listener) {
		if(listener!=null) {
			subListeners.add(listener);
		}
	}
	
	/**
	 * Unregisters a {@link MetricURISubscriptionEventListener}
	 * @param listener the listener to unregister
	 */
	public void unRegisterMetricURISubscriptionEventListener(MetricURISubscriptionEventListener listener) {
		if(listener!=null) {
			subListeners.remove(listener);
		}
	}
	
	
	/**
	 * Returns a WebSocketAgent for the passed web socket URI
	 * @param wsUri the web socket URI to connect to
	 * @return a WebSocketAgent connected to the passed web socket URI
	 */
	public static WebSocketAgent newInstance(URI wsUri) {
		return new WebSocketAgent(WebSocketClient.getInstance(wsUri));
	}

	/**
	 * Creates a new WebSocketAgent
	 * @param wsClient The WebSocketClient this agent will use to comm with the server
	 */
	protected WebSocketAgent(WebSocketClient wsClient) {
		super();
		this.wsClient = wsClient;
	}
	
	/**
	 * Sends a MetricURI subscription request
	 * @param asynch true for an asynchronous call, false for a synchronous call
	 * @param metricURI The MetricURI to subscribe to
	 * @param listeners The listeners that will be invoked on with incoming data for this subscription. 
	 * If empty, the responses will be routed to the agent's global event listeners
	 */
	public void subscribeMetricURI(boolean asynch, URI metricURI, MetricURISubscriptionEventListener...listeners) {
		JSONObject request = JsonRequestBuilder.newBuilder()
				.put("svc", "catalog")
				.put("op", "submetricuri")
				.put("uri", metricURI.toASCIIString())
				.build();
		final long rid;
		try {
			request.getLong("rid");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		wsClient.sendRequest(asynch, request);				
	}
	
	/**
	 * Asynchronously cancels a MetricURI subscription
	 * @param metricURI The MetricURI to subscribe to
	 */
	public void unsubscribeMetricURI(URI metricURI) {
		JSONObject request = JsonRequestBuilder.newBuilder()
				.put("svc", "catalog")
				.put("op", "unsubmetricuri")
				.put("uri", metricURI.toASCIIString())
				.build();
		wsClient.sendRequest(true, request);				
	}
	
	
	/**
	 * Subscribes to the passed MetricURI asynchronously
	 * @param metricURI The MetricURI to subscribe to
	 */
	public void subscribeMetricURIAsynch(URI metricURI) {
		subscribeMetricURI(true, metricURI);
	}

	/**
	 * Subscribes to the passed MetricURI synchronously
	 * @param metricURI The MetricURI to subscribe to
	 */
	public void subscribeMetricURISynch(URI metricURI) {
		subscribeMetricURI(true, metricURI);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.wsclient.WebSocketEventListener#onConnect(java.net.SocketAddress)
	 */
	@Override
	public void onConnect(SocketAddress remoteAddress) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.wsclient.WebSocketEventListener#onClose(java.net.SocketAddress)
	 */
	@Override
	public void onClose(SocketAddress remoteAddress) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.wsclient.WebSocketEventListener#onError(java.net.SocketAddress, java.lang.Throwable)
	 */
	@Override
	public void onError(SocketAddress remoteAddress, Throwable t) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.wsclient.WebSocketEventListener#onMessage(java.net.SocketAddress, org.json.JSONObject)
	 */
	@Override
	public void onMessage(SocketAddress remoteAddress, JSONObject message) {
		// TODO Auto-generated method stub
		
	}
	
	
	/*
	 * 
	 * 	@JSONRequestHandler(name="submetricuri")
	public void subscribeMetricURI(JsonRequest request, Channel channel) {
		String muri = request.getArgument("uri");
		try {

			JSONObject request = new JSONObject();
			int reqId = client.requestSerial.incrementAndGet();
			request.put("rid", reqId);
			request.put("t", "req");
			request.put("svc", "sub");
			request.put("op", "start");
			JSONObject ags = new JSONObject();
			ags.put("es", "jmx");
			ags.put("esn", "service:jmx:local://DefaultDomain");
			ags.put("f", "org.helios.apmrouter.session:service=SharedChannelGroup");
			request.put("args", ags);

	 */
	
}
