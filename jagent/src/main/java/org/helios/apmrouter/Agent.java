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

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarFile;

import org.helios.apmrouter.byteman.sockets.impl.SocketImplTransformer;
import org.helios.apmrouter.util.SimpleLogger;


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
	public static void main(String[] args)  {
		final long start = System.currentTimeMillis();
		appendToBootClassPath();
		installBoostrapInstrumentation();
		coreClassLoader = getIsolatedClassLoader();
		final ClassLoader current = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(coreClassLoader);
			Class<?> bootClass = Class.forName(BOOT_CLASS, true, coreClassLoader);
			
			//Class<?> bootClass = Class.forName(BOOT_CLASS);
			Method method = bootClass.getDeclaredMethod("boot", URLClassLoader.class, String.class, Instrumentation.class);
			method.invoke(null, args.length==0 ? null : Thread.currentThread().getContextClassLoader(), args[0], instrumentation);
			final long elapsed = System.currentTimeMillis()-start;
			log("\n\t=============================================================================" 
					+ "\n\tAPMRouter JavaAgent v " + version(Agent.class) + " Successfully Started"
					+ "\n\tCurrent Server URI:" + System.getProperty("org.helios.apmrouter.uri", "udp://localhost:2094") 
					+ "\n\tAgent start time:" + elapsed + " ms"
					+ "\n\t=============================================================================\n");
		} catch (Exception e) {
			System.err.println("Failed to load apmrouter java-agent core. Stack trace follows:");
			e.printStackTrace(System.err);
		} finally {
			Thread.currentThread().setContextClassLoader(current);
		}
	}
	
	protected static void appendToBootClassPath() {
		try {
			URL jarUrl = Agent.class.getProtectionDomain().getCodeSource().getLocation();
			File agentFile = new File(jarUrl.getFile());
			JarFile jarFile = new JarFile(agentFile);
			log("Agent Code Source: [" + jarUrl + "]   File: [" + agentFile + "]");
			Agent.instrumentation.appendToBootstrapClassLoaderSearch(jarFile);
			
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
	
	/**
	 * Installs bootstrap instrumentation shims.
	 */
	protected static void installBoostrapInstrumentation() {
		installSocketTracker();
	}
	
	/**
	 * Starts the socket tracking instrumentation unless disabled by sysprop
	 */
	protected static void installSocketTracker() {
		SocketImplTransformer sit = new SocketImplTransformer();
		instrumentation.addTransformer(sit, true);
		try {
			Object sock = Class.forName("java.net.Socket").newInstance();
			Field f = sock.getClass().getDeclaredField("impl");
			f.setAccessible(true);
			Object sockImpl = f.get(sock); 			
			instrumentation.retransformClasses(sockImpl.getClass(), sockImpl.getClass().getSuperclass());			
			SimpleLogger.info("Retransformed [" + sockImpl.getClass().getName() + "]");
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}		
	}
	
	/*
			String agentFile = System.getProperty("agent.file");
			JarFile jarFile = new JarFile(agentFile);
			Agent.instrumentation.appendToBootstrapClassLoaderSearch(jarFile);			
			ClassFileTransformer transformer = (ClassFileTransformer)Class.forName("org.helios.apmrouter.byteman.sockets.impl.SocketImplTransformer").newInstance();
			Agent.instrumentation.addTransformer(transformer);			

	 */
	
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
	
	/**
	 * Error logger
	 * @param msg The error message
	 * @param t The throwable to print the stack trace for
	 */
	public static void loge(Object msg, Throwable t) {
		System.err.println(msg);
		t.printStackTrace(System.err);
	}	
	

}
