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
package org.helios.apmrouter.destination.cube;

import java.net.SocketAddress;
import java.net.URI;
import java.util.Collection;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.helios.apmrouter.destination.BaseDestination;
import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.wsclient.WebSocketClient;
import org.helios.apmrouter.wsclient.WebSocketEventListener;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;


/**
 * <p>Title: CubeDestination</p>
 * <p>Description: Sends metrics to <a href="https://github.com/square/cube">Cube</a></p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.cube.CubeDestination</code></p>
 */
public class CubeDestination extends BaseDestination implements WebSocketEventListener, Runnable  {
	/** The Cube URI, e.g. <code>ws://localhost:1080/1.0/event/put</code> */
	protected URI cubeUri = null;
	/** The WebSocketClient */
	protected WebSocketClient webSockClient;
	/** The connected state */
	protected final AtomicBoolean connected = new AtomicBoolean(false);
	/** The expect close flag */
	protected final AtomicBoolean expectClose = new AtomicBoolean(false);
	/** The reconnect loop scheduled task */
	protected final AtomicReference<ScheduledFuture<?>> reconnectSchedule = new AtomicReference<ScheduledFuture<?>>(null);
	/** The task scheduler */
	@Autowired(required=false)
	protected ThreadPoolTaskScheduler scheduler = null;

	/**
	 * Starts this listener
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStart()
	 */
	@Override
	protected void doStart() throws Exception {
		super.doStart();
		doConnect();
		
	}
	
	/**
	 * Stops this listener
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStop()
	 */
	@Override
	protected void doStop() {	
		expectClose.set(true);
		webSockClient.close();
		super.doStop();
	}	
	
	/**
	 * Initiates an asynch connect to the graphite server
	 */
	protected void doConnect() {
		webSockClient = WebSocketClient.getNewInstance(cubeUri, this);		
		connected.set(true);
	}
	
	/**
	 * Accept Route additive for BaseDestination extensions
	 * @param routable The metric to route
	 */
	protected void doAcceptRoute(IMetric routable) {
		if(connected.get()) {
			writeToCube(routable.getUnmapped());
			incr("MetricsForwarded");
		} else {
			incr("MetricsDropped");
		}
	}	
	
	/** The format of a Cube message */
	public static final String CUBE_MSG_FORMAT = "{\"type\":\"metric\", \"data\":{" + 
			"\"host\":\"%s\"," +
			"\"agent\":\"%s\"," +
			"\"fqn\":\"%s\"," +
			"\"type\":\"%s\"," +
			"\"ts\":%s," + 
			"\"val\":%s" + 
			"}}";
	
      
      
	
	
	/**
	 * Writes the metric to cube
	 * @param metric The metric to write
	 */
	protected void writeToCube(IMetric metric) {
		String message = String.format(CUBE_MSG_FORMAT, 
				metric.getHost(), metric.getAgent(),
				metric.getFQN(), metric.getType().name(),
				metric.getTime(), metric.getLongValue()
		);
		webSockClient.sendRequest(message);
	}
	

	
	
	/**
	 * Creates a new CubeDestination
	 * @param patterns The {@link IMetric} pattern this destination accepts
	 */
	public CubeDestination(String... patterns) {
		super(patterns);
	}

	/**
	 * Creates a new CubeDestination
	 * @param patterns The {@link IMetric} pattern this destination accepts
	 */
	public CubeDestination(Collection<String> patterns) {
		super(patterns);
	}

	/**
	 * Creates a new CubeDestination
	 */
	public CubeDestination() {
		
	}
	
	/**
	 * Returns the cube websocket URI
	 * @return the cube websocket URI
	 */
	@ManagedAttribute(description="The cube websocket URI")
	public URI getCubeUri() {
		return cubeUri;
	}
	
	/**
	 * Sets the cube websocket URI
	 * @param cubeUri the cube websocket URI
	 */
	public void setCubeUri(URI cubeUri) {
		this.cubeUri = cubeUri;
	}	
	
	
	
	/**
	 * Indicates if the cube channel is connected
	 * @return true if the cube channel is connected, false otherwise
	 */
	@ManagedAttribute(description="Indicates if this destination is connected to the cube endpoint")
	public boolean isConnected() {
		return connected.get();
	}
	
	
	/**
	 * Returns the number of metrics forwarded to Cube
	 * @return the number of metrics forwarded to Cube
	 */
	@ManagedMetric(category="Cube", metricType=MetricType.COUNTER, description="the number of metrics forwarded to Cube")
	public long getMetricsForwarded() {
		return getMetricValue("MetricsForwarded");
	}
	
	/**
	 * Returns the number of metrics that failed on sending to Cube
	 * @return the number of metrics that failed on sending to Cube
	 */
	@ManagedMetric(category="Cube", metricType=MetricType.COUNTER, description="the number of metrics that failed on sending to Cube")
	public long getMetricsForwardFailures() {
		return getMetricValue("MetricsForwardFailures");
	}
	
	/**
	 * Returns the number of metrics that were dropped because Cube was down
	 * @return the number of metrics that were dropped because Cube was down
	 */
	@ManagedMetric(category="Cube", metricType=MetricType.COUNTER, description="the number of metrics that were dropped because Cube was down")
	public long getMetricsDropped() {
		return getMetricValue("MetricsDropped");
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.wsclient.WebSocketEventListener#onConnect(java.net.SocketAddress)
	 */
	@Override
	public void onConnect(SocketAddress remoteAddress) {
		info("Cube WebSocketClient Connected to [", cubeUri, "/", remoteAddress, "]");
		connected.set(true);
		expectClose.set(false);
		ScheduledFuture<?> sf = reconnectSchedule.getAndSet(null);
		if(sf!=null) {
			sf.cancel(true);
			info("Cancelled Cube [" , cubeUri , "] Reconnect Task");
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.wsclient.WebSocketEventListener#onClose(java.net.SocketAddress)
	 */
	@Override
	public void onClose(SocketAddress remoteAddress) {
		connected.set(false);
		if(!expectClose.get()) {
			startReconnectLoop();
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.wsclient.WebSocketEventListener#onError(java.net.SocketAddress, java.lang.Throwable)
	 */
	@Override
	public void onError(SocketAddress remoteAddress, Throwable t) {
		warn("WebSocket error on cubes destination [", cubeUri, "] ", t);
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.wsclient.WebSocketEventListener#onMessage(java.net.SocketAddress, org.json.JSONObject)
	 */
	@Override
	public void onMessage(SocketAddress remoteAddress, JSONObject message) {
		System.out.println(message);
	}
	
	/**
	 * <p>The reconnect task</p>
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		doConnect();
	}

	/**
	 * Starts a periodic reconnect attempt loop
	 */
	protected void startReconnectLoop() {
		if(scheduler==null) {
			warn("Cube destination to [", cubeUri, "] was null so destination will not execute re-connect loop");
			return;
		}
		reconnectSchedule.set(scheduler.getScheduledExecutor().scheduleWithFixedDelay(this, 15, 15, TimeUnit.SECONDS));
		info("Started cube reconnect loop for [", cubeUri, "]");
	}

	
	

//	public void onConnect(WebSocketClient webSock) {
//		info("Cube WebSocketClient Connected");
//		connected.set(true);
//	}

//	public void onDisconnect(WebSocketClient webSock) {
//		this.webSockClient = null;
//		connected.set(false);
//	}

//	public void onError(Throwable t) {
//		error("WebSock Error", t);
//	}

//	public void onMessage(WebSocketClient webSock, WebSocketFrame frame) {
//		if(frame.isText()) {
//			info("Unexpected Cube Response [", frame.getTextData(), "]");
//		} else {
//			info("Unexpected Cube Response Type:", frame.getType());
//		}
//	}
}
