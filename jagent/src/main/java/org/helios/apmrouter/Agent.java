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
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;


/**
 * <p>Title: Agent</p>
 * <p>Description: The apmrouter client java agent and main entry point</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.Agent</code></p>
 */
public class Agent {
	/** The provided instrumentation instance */
	protected static Instrumentation instrumentation = null;
	/** The provided agent argument string */
	protected static String agentArgs = null;
	/** The core classloader */
	protected static ClassLoader coreClassLoader = null;
	
	/** The name of the boot class */
	public static final String BOOT_CLASS = "org.helios.apmrouter.jagent.AgentBoot";
	
	
	/**
	 * Returns the version of the passed class
	 * @param clazz The class to get the version of
	 * @return the version
	 */
	public static String version(Class<?> clazz) {
		String version = clazz.getPackage().getImplementationVersion();
		if(version==null || version.trim().isEmpty()) version = "Development Snapshot";
		return version;
	}


	/**
	 * The main entry point for javaagents or optional standalone monitor deploy.
	 * @param args One parameter processed which is the URL to an XML config
	 */
	public static void main(String[] args) {
		coreClassLoader = getIsolatedClassLoader();
		final ClassLoader current = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(coreClassLoader);
			Class<?> bootClass = Class.forName(BOOT_CLASS, true, coreClassLoader);
			Method method = bootClass.getDeclaredMethod("boot", String.class, Instrumentation.class);
			method.invoke(null, args.length==0 ? null : args[0], instrumentation);
			log("\n\t=============================\n\tAPMRouter JavaAgent v " + version(Agent.class) + " Successfully Started\n\t=============================\n");
		} catch (Exception e) {
			System.err.println("Failed to load apmrouter java-agent core. Stack trace follows:");
			e.printStackTrace(System.err);
		} finally {
			Thread.currentThread().setContextClassLoader(current);
		}
	}
	
	/**
	 * Creates an isolated classloader to load the core library underlying the agent 
	 * @return a classloader
	 */
	private static ClassLoader getIsolatedClassLoader() {
		try {
			URL agentURL = Agent.class.getProtectionDomain().getCodeSource().getLocation();
			//new URL("jar:file:/C:/proj/parser/jar/parser.jar!/test.xml");
			URL coreURL = new URL("jar:" + agentURL + "!/core.jar");
			//URL coreURL = new URL(agentURL + "!/core.jar");
			
			log("Core URL:" + coreURL);			
			return new JarClassLoader(new URL[]{agentURL, coreURL}, Agent.class.getClassLoader());
		} catch (Exception e) {
			throw new RuntimeException("Failed to create isolated class loader", e);
		}
	}
	
	/**
	 * The pre-main entry point
	 * @param agentArgs The agent bootstrap arguments
	 * @param inst The Instrumentation instance
	 */
	public static void premain(String agentArgs, Instrumentation inst) {
		Agent.agentArgs = agentArgs;
		Agent.instrumentation = inst;
		main(new String[]{agentArgs});
	}
	
	/**
	 * The pre-main entry point for JVMs not supporting a <b><code>java.lang.instrument.Instrumentation</code></b> implementation.
	 * @param agentArgs The agent bootstrap arguments
	 */	
	public static void premain(String agentArgs) {
		Agent.agentArgs = agentArgs;
		main(new String[]{agentArgs});
	}
	
	/**
	 * The agent attach entry point
	 * @param agentArgs The agent bootstrap arguments
	 * @param inst The Instrumentation instance
	 */
	public static void agentmain(String agentArgs, Instrumentation inst) {
		Agent.agentArgs = agentArgs;
		Agent.instrumentation = inst;
		main(new String[]{agentArgs});
	}
	
	/**
	 * The agent attach entry point for JVMs not supporting a <b><code>java.lang.instrument.Instrumentation</code></b> implementation.
	 * @param agentArgs The agent bootstrap arguments
	 */
	public static void agentmain(String agentArgs) {
		Agent.agentArgs = agentArgs;
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
