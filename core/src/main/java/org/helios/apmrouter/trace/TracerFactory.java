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
package org.helios.apmrouter.trace;

import static org.helios.apmrouter.util.Methods.nvl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.helios.apmrouter.metric.AgentIdentity;
import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.sender.ISender;
import org.helios.apmrouter.sender.netty.UDPSender;
import org.helios.apmrouter.util.SystemClock;
import org.helios.apmrouter.util.SystemClock.ElapsedTime;

/**
 * <p>Title: TracerFactory</p>
 * <p>Description: Creates new {@link ITracer} instances</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.trace.TracerFactory</code></p>
 */

public class TracerFactory {
	
	/** The name of the system property that specifies the apm router URIs*/
	public static final String SENDER_URI_PROP = "org.helios.apmrouter.uri";
	/** The default apmrouter URI */
	public static final String DEFAULT_SENDER_URI = "udp://localhost:2094";
	
	/** The default tracer */
	private static final ITracer defaultTracer;
	/** The configured apmrouter URIs */
	private static final Set<URI>  endpoints = new HashSet<URI>();
	/** The configured apmrouter senders */
	private static final Map<URI, ISender>  senders = new TreeMap<URI, ISender>();
	/** The current apmrouter sender */
	private static final AtomicReference<ISender>  sender = new AtomicReference<ISender>();
	
	
	/** A map of created tracers keyed by host/agent */
	private static final Map<String, ITracer> tracers = new ConcurrentHashMap<String, ITracer>();
	
	static {
		String uris = System.getProperty(SENDER_URI_PROP, DEFAULT_SENDER_URI);
		for(String uri: uris.split(",")) {
			try {
				if(!uri.trim().isEmpty()) {
					endpoints.add(new URI(uri.trim()));
				}
			} catch (Exception e) {}
		}
		if(endpoints.isEmpty()) {
			try {
				endpoints.add(new URI(DEFAULT_SENDER_URI));
			} catch (URISyntaxException e) {
				throw new RuntimeException("Failed to add default endpoint URI [" + DEFAULT_SENDER_URI + "]", e);
			}
		}
		for(URI uri: endpoints) {
			senders.put(uri, UDPSender.getInstance(uri));
		}
		//sender.set(senders.values().iterator().next());
		sender.set(new ISender(){

			@Override
			public void send(IMetric... metrics) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void sendDirect(IMetric... metrics) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void sendDirect(Collection<IMetric[]> metrics) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public long getSentMetrics() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public long getDroppedMetrics() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public URI getURI() {
				// TODO Auto-generated method stub
				return null;
			}
			
		});
		defaultTracer = new TracerImpl(AgentIdentity.ID.getHostName(), AgentIdentity.ID.getAgentName(), sender);
		
	}
	
	/**
	 * Returns the default tracer instance
	 * @return the default tracer instance
	 */
	public static ITracer getTracer() {
		return defaultTracer;
	}
	
	
	
	/**
	 * Returns a tracer instance for the passed host and agent
	 * @param host The host name to create a tracer for
	 * @param agent The agent name to create a tracer for
	 * @return a tracer instance
	 */
	public static ITracer getTracer(String host, String agent) {
		String key = nvl(host, "Host Name").trim() + ":" + nvl(agent, "Agent Name").trim();
		ITracer tracer = tracers.get(key);
		if(tracer==null) {
			synchronized(tracers) {
				tracer = tracers.get(key);
				if(tracer==null) {
					tracer = new TracerImpl(host.trim(), agent.trim(), sender);
					tracers.put(key, tracer);
				}
			}
		}
		return tracer;
	}
	
	public static void main(String[] args) {
		log("Basic Tracing Test");
		final int LOOPS = 10;
		final ITracer tracer = getTracer();
		DirectMetricCollection dcm = DirectMetricCollection.newDirectMetricCollection(); 
		log("DCM:" + dcm);
		for(int i = 0; i < LOOPS; i++) {
			log("Loop:" + i);
			dcm.append(tracer.traceBlob(new Date(), "foo", "date"));
			log("Loop:" + i);
			dcm.append(tracer.traceLong(i, "foo", "bar"));	
			log("Loop:" + i);
		}
		log("DCM:" + dcm);
		dcm.destroy();
		log("DCM Destroyed");
		dcm = null;
		log("Warmup Complete");
		dcm = DirectMetricCollection.newDirectMetricCollection();
		SystemClock.startTimer();
		for(int i = 0; i < LOOPS; i++) {
			dcm.append(tracer.traceLong(i, "foo", "bar"));						
		}
		ElapsedTime et = SystemClock.endTimer();
		log("DCM:" + dcm + "\nSent:" + tracer.getSentMetrics() + "\nDropped:" + tracer.getDroppedMetrics() + "\nElapsed:" + et + "\nAvg Per:" + et.avgNs(LOOPS) + "ns");
		dcm.destroy();
		dcm = null;
	}
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
}
