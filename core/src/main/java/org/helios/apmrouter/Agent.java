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
package org.helios.apmrouter;

import java.lang.instrument.Instrumentation;

import org.helios.apmrouter.metric.AgentIdentity;
import org.helios.apmrouter.monitor.DefaultMonitorBoot;
import org.helios.apmrouter.util.VersionHelper;

/**
 * <p>Title: Agent</p>
 * <p>Description: The apmrouter client java agent and main entry point</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.Agent</code></p>
 */
public class Agent {

	/**
	 * Creates a new Agent
	 */
	public Agent() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {		
		log("\n\t=============================\n\tAPMRouter JavaAgent v " + VersionHelper.version(Agent.class) + " Successfully Started"  
		+ "\n\tAgent name is [" + AgentIdentity.ID.getHostName() + "/" + AgentIdentity.ID.getAgentName() + "]" +  
		"\n\t=============================\n");
		DefaultMonitorBoot.boot();
	}
	
	/*
	 * The pre-main entry point
	 * @param agentArgs The agent bootstrap arguments
	 * @param inst The Instrumentation instance
	 */
	public static void premain(String agentArgs, Instrumentation inst) {
		main(new String[]{agentArgs});
	}
	
	/**
	 * The pre-main entry point for JVMs not supporting a <b><code>java.lang.instrument.Instrumentation</code></b> implementation.
	 * @param agentArgs The agent bootstrap arguments
	 */	
	public static void premain(String agentArgs) {
		main(new String[]{agentArgs});
	}
	
	/**
	 * The agent attach entry point
	 * @param agentArgs The agent bootstrap arguments
	 * @param inst The Instrumentation instance
	 */
	public static void agentmain(String agentArgs, Instrumentation inst) {
		main(new String[]{agentArgs});
	}
	
	/**
	 * The agent attach entry point for JVMs not supporting a <b><code>java.lang.instrument.Instrumentation</code></b> implementation.
	 * @param agentArgs The agent bootstrap arguments
	 */
	public static void agentmain(String agentArgs) {
		main(new String[]{agentArgs});
	}
	
	
	
	/**
	 * Simple out logger
	 * @param msg the message
	 */
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	/**
	 * Simple err logger
	 * @param msg the message
	 */
	public static void elog(Object msg) {
		System.err.println(msg);
	}
	

}
