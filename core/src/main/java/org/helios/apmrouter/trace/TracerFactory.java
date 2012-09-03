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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.helios.apmrouter.metric.AgentIdentity;
import static org.helios.apmrouter.util.Methods.nvl;

/**
 * <p>Title: TracerFactory</p>
 * <p>Description: Creates new {@link ITracer} instances</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.trace.TracerFactory</code></p>
 */

public class TracerFactory {
	/** The default tracer */
	private static final ITracer defaultTracer = new TracerImpl(AgentIdentity.ID.getHostName(), AgentIdentity.ID.getAgentName());
	
	/** A map of created tracers keyed by host/agent */
	private static final Map<String, ITracer> tracers = new ConcurrentHashMap<String, ITracer>();
	
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
					tracer = new TracerImpl(host.trim(), agent.trim());
					tracers.put(key, tracer);
				}
			}
		}
		return tracer;
	}
	
}
