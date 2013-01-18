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
package org.helios.apmrouter.jagent;

import static org.helios.apmrouter.util.SimpleLogger.debug;
import static org.helios.apmrouter.util.SimpleLogger.error;
import static org.helios.apmrouter.util.SimpleLogger.info;
import static org.helios.apmrouter.util.SimpleLogger.warn;

import java.io.File;
import java.io.StringReader;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import org.helios.apmrouter.byteman.sockets.impl.SocketImplTransformer;
//import org.helios.apmrouter.byteman.sockets.impl.TrackingSocketImplFactory;
import org.helios.apmrouter.instrumentation.Trace;
import org.helios.apmrouter.instrumentation.TraceClassFileTransformer;
import org.helios.apmrouter.instrumentation.publifier.ClassPublifier;
import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.jmx.XMLHelper;
import org.helios.apmrouter.jmx.threadinfo.ExtendedThreadManager;
import org.helios.apmrouter.monitor.Monitor;
import org.helios.apmrouter.sender.SenderFactory;
import org.helios.apmrouter.util.SimpleLogger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * <p>Title: AgentBoot</p>
 * <p>Description: A core module bootstrap entry point for the java-agent to initialize through</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.jagent.AgentBoot</code></p>
 */

public class AgentBoot {
	/** The provided instrumentation instance */
	protected static Instrumentation instrumentation = null;
	/** The provided agent argument string */
	protected static String agentArgs = null;
	/** The classloader passed by the bootstrap agent */
	protected static URLClassLoader classLoader;
	
	/** The agent boot class for codahale */
	protected static final String CODAHALE_BOOT_CLASS = "org.helios.apmrouter.codahale.agent.Agent";
	/** The target method name for the agent boot class for codahale */
	protected static final String CODAHALE_BOOT_METHOD = "heliosBoot";
	/** The target method signature for the agent boot class for codahale */
	protected static final Class<?>[] CODAHALE_BOOT_SIG = new Class[]{
		String.class, Instrumentation.class, Node.class 
	};
	
	
	/** The reflective method for {@link URLClassLoader}'s addURL method */
	protected static final Method addUrlMethod;
	
	static {
		try {
			addUrlMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
			addUrlMethod.setAccessible(true);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Returns the instrumentation instance
	 * @return the instrumentation instance
	 */
	public static Instrumentation getInstrumentation() {
		return instrumentation;
	}
	
	/**
	 * The core module boot hook when installing though the java-agent
	 * @param classLoader The classloader passed by the bootstrap agent
	 * @param agentArgs The agent arguments
	 * @param instrumentation The instrumentation which may be null
	 */
	public static void boot(URLClassLoader classLoader, String agentArgs, Instrumentation instrumentation) {
		instrumentation.addTransformer(new SocketImplTransformer(), true);
		try {
//			instrumentation.retransformClasses(Socket.class);
//			Socket.setSocketImplFactory(new TrackingSocketImplFactory());
		} catch (Exception ex) {}
//		SimpleLogger.info("TrackingSocketFactory Installed:" + TrackingSocketImplFactory.isInstalled());
		
		org.helios.apmrouter.jagent.Instrumentation.install(instrumentation);
		AgentBoot.agentArgs = agentArgs;
		AgentBoot.instrumentation = instrumentation;
		AgentBoot.classLoader = classLoader;
		configure();
		ExtendedThreadManager.install();
		Thread t = new Thread("HotspotInternalLoaderThread") {
			public void run() {
				try {
					Thread.sleep(15000);
					Class<?> clazz = Class.forName("sun.management.HotspotInternal");
					Object obj = clazz.newInstance();
					JMXHelper.getHeliosMBeanServer().registerMBean(obj, new javax.management.ObjectName("sun.management:type=HotspotInternal"));			
				} catch (Exception ex) {
					SimpleLogger.warn("Failed to install HotspotInternal:", ex);
				}				
			}
		};
		t.setDaemon(true);
		t.start();
	}
	
	/**
	 * Configures the agent from the supplied agent args.
	 */
	protected static void configure() {
		if(agentArgs!=null && !agentArgs.trim().isEmpty()) {
			URL xmlUrl = null;
			Node configNode = null;
			try {
				xmlUrl = new URL(agentArgs);
				configNode = getXMLConf(xmlUrl);
			} catch (Exception e) {
				System.err.println("Failed to parse XML configuration defined at [" + agentArgs + "]");
				e.printStackTrace(System.err);
				return;
			}			
			loadProps(XMLHelper.getChildNodeByName(configNode, "props", false));
			publify(XMLHelper.getChildNodeByName(configNode, "publify", false));
			loadJavaAgents(XMLHelper.getChildNodeByName(configNode, "javaagents", false));
			SenderFactory.getInstance();
			
			Node aopNode = XMLHelper.getChildNodeByName(configNode, "aop", false);
			if(aopNode!=null) {
				loadTraceAnnotated(aopNode);
				loadCodahale(XMLHelper.getChildNodeByName(aopNode, "codahale", false));
			}
			loadJmxConnectors(XMLHelper.getChildNodeByName(configNode, "jmx-connector", false));
			loadMonitors(XMLHelper.getChildNodeByName(configNode, "monitors", false));
		}		
	}
	
	/**
	 * The passed node contains the comma separated names of classes that need to be publified
	 * @param publifyNode The publify config node
	 */
	private static void publify(Node publifyNode) {
		SimpleLogger.info("\n\tExecuting publifier");
		if(publifyNode==null) return;
		String[] classes = XMLHelper.getNodeTextValue(publifyNode).split(",");
		SimpleLogger.debug("Publifying Classes ", Arrays.toString(classes));
		try {
			ClassPublifier.getInstance().publify(classes);
				StringBuilder b = new StringBuilder("\n\t===============================================\n\tThe following classes will be transformed to make them public\n\t===============================================");
				for(String className: classes) {
					if(className==null || className.trim().isEmpty()) continue;
					b.append("\n\t").append(className.trim());
				}
				b.append("\n\t===============================================\n");
				SimpleLogger.info(b.toString());
		} catch (Exception ex) {
			/* No Op */
		}
		
		
	}

	/*
	<jmx-connector>
		<connectorclass>javax.management.remote.jmxmp.JMXMPConnectorServer</connectorclass>
		<serviceurl>service:jmx:jmxmp://0.0.0.0:8006</serviceurl>		
		<env>
			<prop name="" value=""/>
			<prop name="" value=""/>
		</env>
		<register-domains>
			<domain></domain>
		</register-domains>
	</jmx-connector>
	<javaagents>
		<javaagent jar="/home/nwhitehead/.m2/repository/org/jboss/byteman/byteman/2.1.1-SNAPSHOT/byteman-2.1.1-SNAPSHOT.jar">
			<args>
			
			</args>
		</javaagent>
	</javaagents>

 */
	/** The pre-main manifest key */
	public static final String PRE_MAIN = "Premain-Class";
	/** The agent-main manifest key */
	public static final String AGENT_MAIN = "Agent-Class";
	
	/** The pre-main method name */
	public static final String PRE_MAIN_METHOD = "premain";
	/** The agent-main method name */
	public static final String AGENT_MAIN_METHOD = "agentmain";
	
	
	/** The method signature when providing instrumentation */
	protected static final Class<?>[] INSTR_SIG = new Class[]{String.class, Instrumentation.class};
	/** The method signature when not providing instrumentation */
	protected static final Class<?>[] NO_INSTR_SIG = new Class[]{String.class};
	
	 
	
	/**
	 * Attempts to initialize the java-agent in the passed agent jar file
	 * @param file The jar file that the java agent is in
	 * @param agentArgs The configured agent arguments
	 * @param supportJars An optional array of supporting jar files to be appended to the bootstrap classpath
	 * @return The name of the java agent class
	 * @throws Exception thrown on any error
	 */
	protected static String initAgent(File file, String agentArgs, File...supportJars) throws Exception {
		JarFile jarFile = new JarFile(file);
		Manifest manifest = jarFile.getManifest();
		Attributes attrs = manifest.getMainAttributes();
		String className = attrs.getValue(PRE_MAIN);
		if(className==null) {
			className = attrs.getValue(AGENT_MAIN);
		}
		if(className==null) {
			throw new Exception("Could not find pre-main or agent-main classname in the jar manifest", new Throwable());
		}
		URLClassLoader classLoader = new URLClassLoader(new URL[]{file.toURI().toURL()});
		Class<?> clazz = Class.forName(className, true, classLoader);
		LinkedHashMap<String, Class<?>[]>  map = new LinkedHashMap<String, Class<?>[]> (4);
		map.put(PRE_MAIN_METHOD, INSTR_SIG);
		map.put(AGENT_MAIN_METHOD, INSTR_SIG);
		map.put(PRE_MAIN_METHOD.toUpperCase(), NO_INSTR_SIG);
		map.put(AGENT_MAIN_METHOD.toUpperCase(), NO_INSTR_SIG);
		Method method = null;
		for(Map.Entry<String, Class<?>[]> entry: map.entrySet()) {
			method = getMethod(clazz, entry.getKey().toLowerCase(), entry.getValue());
			if(method!=null) break;
		}
		if(method==null) {
			throw new Exception("Could not find premain or agentmain methods in the class [" + clazz.getName() + "]", new Throwable());
		}
		//instrumentation.appendToSystemClassLoaderSearch(jarFile);
		for(File f: supportJars) {
			if(!f.canRead()) {
				SimpleLogger.warn("Cannot read support jar file [", f, "]. NOT adding to boot classpath");
				continue;
			}
			instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(f));
		}
		instrumentation.appendToBootstrapClassLoaderSearch(jarFile);
		if(method.getParameterTypes().length==2) {
			method.invoke(null, agentArgs, instrumentation);
		} else {
			method.invoke(null, agentArgs);
		}
		return clazz.getName();
		
	}
	
	protected static Method getMethod(Class<?> clazz, String methodName, Class<?>...sig) {
		try {
			return clazz.getDeclaredMethod(methodName, sig);
		} catch (Exception ex) {
			return null;
		}
	}
	
	/**
	 * Loads the configured chained java-agents
	 * @param agentsNode The java-agent configuration node
	 */
	protected static void loadJavaAgents(Node agentsNode) {
		if(agentsNode==null) return;
		SimpleLogger.info("Loading Chanined Java Agents");
		for(Node agentNode: XMLHelper.getChildNodesByName(agentsNode, "javaagent", false)) {
			String jarName = XMLHelper.getAttributeByName(agentNode, "jar", "null");
			Node argNode = XMLHelper.getChildNodeByName(agentNode, "args", false);
			
			String args = argNode==null ? null : XMLHelper.getNodeTextValue(argNode);
			if(args!=null) {
				args = args.trim();
				if(args.isEmpty()) {
					args = null;
				}
			}
			try {
				File file = new File(jarName.trim());
				if(!file.canRead()) {
					throw new Exception("Cannot read the file [" + file + "]");
				}
				Set<File> supportJars = new HashSet<File>();
				for(Node jarNode: XMLHelper.getChildNodesByName(agentNode, "jar", false)) {
					File f = new File(XMLHelper.getAttributeByName(jarNode, "name", ""));
					if(file.canRead()) {
						supportJars.add(f);
					}
				}
				SimpleLogger.info("Adding support jars ", supportJars);
				String className = initAgent(file, args, supportJars.toArray(new File[supportJars.size()]));
				SimpleLogger.info("Loaded Java Agent from [", className, "]"); 
				
			} catch (Throwable ex) {
				SimpleLogger.warn("Failed to load and configure java agent [", jarName, "]", ex);
			}
		}
	}
	
/*
 <jars>
	<jar name="c:/users/nwhitehe/.m2/repository/org/jboss/byteman/byteman-sample/2.1.1-SNAPSHOT/byteman-sample-2.1.1-SNAPSHOT.jar"/>
 </jars>
 */
	
	/**
	 * Loads the configured JMX Connector Server
	 * @param jmxNode The jmx connector configuration node
	 */
	protected static void loadJmxConnectors(Node jmxNode) {
		if(jmxNode==null) return;
		SimpleLogger.info("Loading JMXConnectorServer");
		String connectorClass = XMLHelper.getNodeTextValue(XMLHelper.getChildNodeByName(jmxNode, "connectorclass", false));
		String serviceUrl = XMLHelper.getNodeTextValue(XMLHelper.getChildNodeByName(jmxNode, "serviceurl", false));
		
		try {
			JMXServiceURL xurl = new JMXServiceURL(serviceUrl);			
			final ObjectName objectName = new ObjectName(String.format("org.helios.jmx:service=JMXConnectorServer,protocol=%s", xurl.getProtocol()));
			final JMXConnectorServer server = JMXConnectorServerFactory.newJMXConnectorServer(xurl, null, JMXHelper.getHeliosMBeanServer());
			JMXHelper.registerMBean(objectName, server);
			Thread shutdown = new Thread("JMXConnectorServerShutdownThread") {
				@Override
				public void run() {
					SimpleLogger.info("Stopping JMXConnectorServer [", objectName, "]");
					try {
						server.stop();
					} catch (Exception ex) {
						SimpleLogger.warn("Failed to stop JMXConnectorServer [", objectName, "]", ex);
					}
				}
			};
			Runtime.getRuntime().addShutdownHook(shutdown);
			server.start();
		} catch (Exception ex) {
			SimpleLogger.warn("Failed to load JMX Connector", ex);
		}
	}
	
	
	/**
	 * Loads and processes the codahale node
	 * @param codahaleNode the codahale AOP node
	 * <p><b>Example:</b>
	 * <pre>
	 * 	&lt;aop&gt;
	 * 		&lt;codahale  jar="&lt;helios codahale jar URL&gt;"&gt;
	 * 			&lt;annotations/&gt;
	 * 			&lt;packages&gt;org.helios.test,org.helios.test2&lt;/packages&gt;
	 * 		&lt;/codahale&gt;		
	 * 	&lt;/aop&gt;
	 * 	</pre></p>
	 * 
	 */
	protected static void loadCodahale(Node codahaleNode) {
		if(codahaleNode==null) return;
		String jarUrl = XMLHelper.getAttributeByName(codahaleNode, "jar", null);
		if(jarUrl==null) {
			System.err.println("No jar URL defined for codeahale");
			return;
		}
		try {
			URL url = new URL(jarUrl);
			info("Codahale jar:[", url ,"]");
			//addURLToClassLoader(url);
//			URL thirdParty = new URL("file:/C:/users/nwhitehe/.m2/repository/com/yammer/metrics/metrics-core/3.0.0-SNAPSHOT/metrics-core-3.0.0-SNAPSHOT.jar");
//			addURLToClassLoader(thirdParty);
			instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(url.getFile()));
			//instrumentation.appendToSystemClassLoaderSearch(new JarFile(url.getFile()));
			//Class<?> bootClazz = classLoader.loadClass(CODAHALE_BOOT_CLASS);
			Class<?> bootClazz = ClassLoader.getSystemClassLoader().loadClass(CODAHALE_BOOT_CLASS);
			Method bootMethod = bootClazz.getDeclaredMethod(CODAHALE_BOOT_METHOD, CODAHALE_BOOT_SIG);
			bootMethod.invoke(null, agentArgs, instrumentation, codahaleNode);
		} catch (Exception ex) {			
			String xml = null;
			try {
				xml = XMLHelper.renderNode(codahaleNode);
			} catch (Exception e) {
				xml = "== Failed to render ==";
			}
			warn("Failed to process codahale AOP configuration. XML was [\n", xml, "\n]  Stack Trace follows.", ex);
		}
	}
	
	
	
	
	/**
	 * Adds a URL to the classloader
	 * @param url the URL to add
	 */
	protected static void addURLToClassLoader(URL url) {
		try {
			addUrlMethod.invoke(classLoader, url);
		} catch (Exception ex) {
			error("Failed to add URL [",  url,  "] to classloader", ex);
			throw new RuntimeException("Failed to add URL [" + url + "] to classloader", ex);
			
		}
	}
	
	/**
	 * Loads the {@link TraceClassFileTransformer} that will instrument {@link Trace} annotated methods.
	 * @param aopNode The config node for aop
	 */
	protected static void loadTraceAnnotated(Node aopNode) {
		if(aopNode==null) return;
		Set<String> packages = new HashSet<String>();
		Node traceAnnot = XMLHelper.getChildNodeByName(aopNode, "trace-annotated", false);
		if(traceAnnot!=null) {
			Node packageNode = XMLHelper.getChildNodeByName(traceAnnot, "packages", false);
			if(packageNode!=null) {
				String pnames = XMLHelper.getNodeTextValue(packageNode);
				if(pnames!=null && !pnames.trim().isEmpty()) {
					String[] frags = pnames.trim().split(",");
					for(String s: frags) {
						if(s.trim().isEmpty()) continue;
						packages.add(s.trim());
					}
				}
			}
		}
		if(!packages.isEmpty()) {
			TraceClassFileTransformer tcf = new TraceClassFileTransformer(packages);
			instrumentation.addTransformer(tcf, true);
			debug("Added TraceClassFileTransformer for packages ", packages);
		}
		
	}
	
	/**
	 * Configures XML defined monitors
	 * @param monitorsNode The monitors node from the XML config
	 */
	protected static void loadMonitors(Node monitorsNode) {
		if(monitorsNode==null) return;
		for(Node mNode : XMLHelper.getChildNodesByName(monitorsNode, "monitor", false)) {
			try {
				String name = XMLHelper.getAttributeByName(mNode, "name", null);
				long period = XMLHelper.getLongAttributeByName(mNode, "period", 15000);
				long startDelay = XMLHelper.getLongAttributeByName(mNode, "startDelay", -1L);				
				Class<Monitor> monClass = (Class<Monitor>) Class.forName(name);
				Monitor monitor = monClass.newInstance();
				monitor.setCollectPeriod(period);
				Node propertyNode = XMLHelper.getChildNodeByName(mNode, "properties", false);
				if(propertyNode!=null) {
					String props = XMLHelper.getNodeTextValue(propertyNode);
					Properties p = new Properties();
					p.load(new StringReader(props.trim()));
					Properties cleanedProperties = new Properties();
					for(String key: p.stringPropertyNames()) {						
						String value = p.getProperty(key).trim();
						key = key.trim();
						cleanedProperties.setProperty(key, value);
					}
					monitor.setProperties(cleanedProperties);
				}
				if(startDelay<1) {
					monitor.startMonitor();
				} else {
					monitor.startMonitor(startDelay);
				}
			} catch (Exception e) {
				String xml = null;
				try {
					xml = XMLHelper.renderNode(mNode);
				} catch (Exception ex) {
					xml = "== Failed to render ==";
				}
				warn("Failed to process configured monitor. XML was [\n", xml, "\n]  Stack Trace follows.", e);
			}
		}		
	}
	
	
	/**
	 * Loads XML defined properties
	 * @param propsNode The properties node from the XML config
	 */
	protected static void loadProps(Node propsNode) {
		if(propsNode!=null) {
			for(Node pNode : XMLHelper.getChildNodesByName(propsNode, "prop", false)) {
				try {
					System.setProperty(XMLHelper.getAttributeByName(pNode, "name", null), XMLHelper.getAttributeByName(pNode, "value", null));
				} catch (Exception e) {
					System.err.println("Failed to process agent defined property in node [" + XMLHelper.renderNode(pNode) + "]");
				}
			}
		}
	}

	
	
	/**
	 * Parses the XML input from the passed URL and returns the root node
	 * @param xmlUrl The URL of the XML config
	 * @return the parsed root node 
	 */
	protected static Node getXMLConf(URL xmlUrl) {
		Document doc = XMLHelper.parseXML(xmlUrl);
		return doc.getDocumentElement();
	}
}
