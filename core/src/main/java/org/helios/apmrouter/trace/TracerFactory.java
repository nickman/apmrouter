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

import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.helios.apmrouter.metric.AgentIdentity;
import org.helios.apmrouter.metric.MetricType;
import org.helios.apmrouter.metric.catalog.ICEMetricCatalog;
import org.helios.apmrouter.sender.ISender;
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
	
	
	/** The default tracer */
	private static final ITracer defaultTracer;
	/** The current apmrouter sender */
	private static final AtomicReference<ISender>  sender = new AtomicReference<ISender>();
	
	/** The collection funnel */
	private static final CollectionFunnel funnel;
	
	
	/** A map of created tracers keyed by host/agent */
	private static final Map<String, ITracer> tracers = new ConcurrentHashMap<String, ITracer>();
	
	static {
		funnel = CollectionFunnel.getInstance();
		defaultTracer = new TracerImpl(AgentIdentity.ID.getHostName(), AgentIdentity.ID.getAgentName(), funnel);
		if(System.getProperties().containsKey("debug-stop")) {
			long time = 60000;
			try { 
				time = Long.parseLong(System.getProperty("debug-stop").trim()); 
			} catch (Exception e) {
				time = 60000;
			};
			final long ftime = time;
			String cmdLine = ManagementFactory.getRuntimeMXBean().getInputArguments().toString();
			System.err.println("Args:" + cmdLine);
			Thread t = new Thread("DEBUG STOP THREAD") {
				public void run() {
					SystemClock.sleep(ftime);
					System.err.println("\n\t===========================================\n\tDEBUG STOP THREAD EXITING\n\t===========================================\n");
					System.exit(-99);
				}
			};
			t.start();
			System.err.println("\n\t===========================================\n\tDEBUG STOP THREAD STARTED\n\tTimeout:" + ftime + " ms." + "\n\t===========================================\n");
		}
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
					tracer = new TracerImpl(host.trim(), agent.trim(), funnel);
					tracers.put(key, tracer);
				}
			}
		}
		return tracer;
	}
	public static void mainx(String[] args) {
		log("DMC Decode Test");
		getTracer().traceDirect(1, "foo", MetricType.LONG, "bar");	
	}
	
	
	public static void main(String[] args) {
		log("Basic Tracing Test");
		MetricType.setCompress(false);
		MetricType.setDirect(false);
		boolean traceBlob = false;
		final int LOOPS = 1000;
		final ITracer tracer = getTracer();
		for(int x = 0; x < 100; x++) {			
			for(int i = 0; i < LOOPS; i++) {
				if(traceBlob) {
					tracer.traceBlob(new Date(), "foo", "date");
				} else {
					tracer.traceLong(i, "foo", "bar");
				}					
			}
		}
//		SystemClock.sleep(CollectionFunnel.getInstance().getTimerPeriod()+1000);
		log("Warmup Complete");
		log("Starting Untokenized");
//		tracer.resetStats();
		SystemClock.startTimer();
		for(int i = 0; i < LOOPS; i++) {
			if(traceBlob) {
				tracer.traceBlob(new Date(), "foo", "date");
			} else {
				tracer.traceLong(i, "foo", "bar");
			}					
		}
		ElapsedTime et = SystemClock.endTimer();
		log("FULL:\nSent:" + tracer.getSentMetrics() + "\nDropped:" + tracer.getDroppedMetrics() + "\nElapsed:" + et + "\nAvg Per:" + et.avgNs(LOOPS) + " ns");
		/// TOKENIZE
		ICEMetricCatalog.getInstance().setToken(tracer.getHost(), tracer.getAgent(), "foo", MetricType.LONG, "bar");
//		SystemClock.sleep(CollectionFunnel.getInstance().getTimerPeriod()+1000);
		log("Starting Tokenized");
//		tracer.resetStats();
		SystemClock.startTimer();
		for(int i = 0; i < LOOPS; i++) {
			if(traceBlob) {
				tracer.traceBlob(new Date(), "foo", "date");
			} else {
				tracer.traceLong(i, "foo", "bar");
			}					
		}
		et = SystemClock.endTimer();
		log("TOKEN:\nSent:" + tracer.getSentMetrics() + "\nDropped:" + tracer.getDroppedMetrics() + "\nElapsed:" + et + "\nAvg Per:" + et.avgNs(LOOPS) + " ns");
		SystemClock.sleep(CollectionFunnel.getInstance().getTimerPeriod()+3000);
		log("\n\tTotal Sent:" + tracer.getSentMetrics());
		log("\tTotal Dropped:" + tracer.getDroppedMetrics());
		log("\tTotal Queued:" + tracer.getQueuedMetrics());
		log(CollectionFunnel.getInstance().status());
	}
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
}
