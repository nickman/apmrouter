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

import java.io.StringReader;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.helios.apmrouter.instrumentation.Trace;
import org.helios.apmrouter.instrumentation.TraceClassFileTransformer;
import org.helios.apmrouter.jmx.XMLHelper;
import org.helios.apmrouter.monitor.Monitor;
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
	
	/**
	 * The core module boot hook when installing though the java-agent
	 * @param agentArgs The agent arguments
	 * @param instrumentation The instrumentation which may be null
	 */
	public static void boot(String agentArgs, Instrumentation instrumentation) {
		AgentBoot.agentArgs = agentArgs;
		AgentBoot.instrumentation = instrumentation;
		//DefaultMonitorBoot.boot();
		configure();
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
			loadMonitors(XMLHelper.getChildNodeByName(configNode, "monitors", false));
			loadTraceAnnotated(XMLHelper.getChildNodeByName(configNode, "aop", false));
		}		
	}
	
	/*
	 * 	<aop>
		<trace-annotated>
			<packages>org.helios.test,org.helios.test2</packages>

	 */
	
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
			System.out.println("Added TraceClassFileTransformer for packages " + packages);
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
				System.err.println("Failed to process configured monitor [" + XMLHelper.renderNode(mNode) + "]");
			}
		}		
	}
	
	
	
	/*
<agent>
	<props>
		<prop name="org.helios.agent" value="eggs-cellent"/>
		<prop name="org.helios.agent.monitor.script.url" value="eggs-cellent"/>
	</props>
	<monitors>
		<monitor name="org.helios.apmrouter.monitor.jvm.JVMMonitor" period="5000"/>
	</monitors>
</agent>
	 */
	
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
