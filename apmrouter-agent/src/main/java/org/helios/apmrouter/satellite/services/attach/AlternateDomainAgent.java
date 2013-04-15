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
package org.helios.apmrouter.satellite.services.attach;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;

import sun.management.Agent;
import sun.management.AgentConfigurationError;
import sun.management.jmxremote.ConnectorBootstrap.DefaultValues;
import sun.management.jmxremote.ConnectorBootstrap.PropertyNames;
import sun.management.jmxremote.LocalRMIServerSocketFactory;
import sun.misc.VMSupport;
import sun.rmi.server.UnicastServerRef;
import sun.rmi.server.UnicastServerRef2;

import com.sun.jmx.remote.internal.RMIExporter;

/**
 * <p>Title: AlternateDomainAgent</p>
 * <p>Description: Mimics the standard management agent <b><code>sun.management.Agent</code></b> except it is used for exposing
 * non platform MBeanServers</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.satellite.services.attach.AlternateDomainAgent</code></p>
 */

public class AlternateDomainAgent {
	/** Platform EOL */
	public static final String EOL = System.getProperty("line.separator", "\n");
	/** Platform TMP Dir */
	public static final String TMP;
	
	/** The agent property prefix for the connector addresses */
	public static final String LOCAL_CONNECTOR_ADDRESS_PROP = "com.sun.management.jmxremote.localConnectorAddress";
	/** The agent property into which the comma separated MBeanServer default domain names are written */
	public static final String JMX_DOMAINS_PROP = "javax.management.agent.domains";
	
	/** The agent property name where we store an exception message on an agent deploy failure */
	public static final String AGENT_DEPLOY_ERR_PROP = "javax.management.agent.deploy.error";
	/** The agent deploy failure message delimiter */
	public static final String AGENT_DEPLOY_ERR_DELIM = "\t~";
	
	/** The agent version */
	public static final double VERSION = 1;
	/**
	 * Command line agent startup entry point
	 * @param args The agent arguments
	 * @throws Exception thrown on any error
	 */
	public static void premain(String args) throws Exception {
		agentmain(args);		
	}
	
	static {
		String tmpDir = System.getProperty("java.io.tmpdir");
		if(!tmpDir.endsWith(File.separator)) {
			tmpDir = tmpDir + File.separator;
		}
		TMP = tmpDir;
	}
	
	/**
	 * Entry point for the attach API
	 * @param args The agent arguments
	 */
	public static void agentmain(String args)  {
		
		final Set<String> directives = new HashSet<String>();
		if(args!=null) {
			String[] directiveArgs = args.split("\n");
			if(directiveArgs!=null) {
				for(String s: directiveArgs) {
					if(s==null || s.trim().isEmpty()) continue;
					directives.add(s.trim().toLowerCase());
				}
			}
		}
		log("Starting AlternateDomainAgent, Directives: " + directives);
		// Clear the agent deploy error messages
		if(directives.contains("clear.errors")) {
			clearAgentProperty(AGENT_DEPLOY_ERR_PROP);
			return;
		}
		// Install the platform mbeanserver
		if(directives.contains("install-platform")) {			
			log("Installing Platform MBeanServer Management Agent");
			final String mbeanBuilder = System.getProperty("javax.management.builder.initial", null);
			final ClassLoader cl = Thread.currentThread().getContextClassLoader();
			try {
				Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());
				System.clearProperty("javax.management.builder.initial");
				MBeanServer server = ManagementFactory.getPlatformMBeanServer();
				log("Acquired Platform MBeanServer [" + server.getDefaultDomain() + "]");
				startAgent(server);
			} catch (Exception ex) {
				ex.printStackTrace(System.err);
				appendAgentException("DefaultDomain", ex);
			} finally {
				Thread.currentThread().setContextClassLoader(cl);
				if(mbeanBuilder!=null) {
					System.setProperty("javax.management.builder.initial", mbeanBuilder); 
				}
			}
			System.clearProperty("javax.management.builder.initial");			
			return;
		}
		
		//"install-platform"
		//final String mbeanBuilder = System.getProperty("javax.management.builder.initial", null);
		//System.clearProperty("javax.management.builder.initial");
		//try {
		for(MBeanServer server : MBeanServerFactory.findMBeanServer(null)) {
			String dd = server.getDefaultDomain();
			if(agentPropertyArrayContains(JMX_DOMAINS_PROP, ",", dd)) continue;
			// Don't load the platform mbean server unless there is a directive override
			// (sometimes the default domain for the platform mbeanserver is null)
			if(dd==null || dd.trim().isEmpty() || dd.equals("DefaultDomain")) {
				if(!directives.contains("include-platform")) {
					continue;
				}
				dd = "DefaultDomain";
			}
			try {
				log("Starting connector server for [" + dd + "]");
				startAgent(server);
				appendAgentProperty(JMX_DOMAINS_PROP, ",", dd);
			} catch (Exception ex) {
				appendAgentException(dd, ex);
			}
		}
//		} 
//		catch (Exception ex) {
//			ex.printStackTrace(System.err);
//		} finally {
//			if(mbeanBuilder!=null) {
//				System.setProperty("javax.management.builder.initial", mbeanBuilder);
//			}
//		}
	}
	
	/**
	 * Retrieves an agent property value
	 * @param propertyName The name of the property
	 * @param defaultValue The default value to return if the property is not set
	 * @return the property value or the default
	 */
	protected static String getAgentProperty(String propertyName, String defaultValue) {
		return VMSupport.getAgentProperties().getProperty(propertyName, defaultValue);
	}
	
	/**
	 * Retrieves an agent property value as a string array
	 * @param propertyName the property name
	 * @param delimiter The delimiter to parse the value
	 * @param defaultValue The default value to return if the property is not set
	 * @return the parsed array or the default value
	 */
	protected static synchronized String[] getAgentPropertyArray(String propertyName, String delimiter, String...defaultValue) {
		String arr = getAgentProperty(propertyName, null);
		if(arr==null || arr.trim().isEmpty()) return defaultValue;
		String[] array = arr.trim().split(delimiter);
		for(int i = 0; i < array.length; i++) {
			if(array[i]!=null) array[i] = array[i].trim();
		}
		return array;
	}
	
	/**
	 * Determines if the passed value is present in the array in the named agent property
	 * @param propertyName The agent property name
	 * @param delimiter The array delimiter
	 * @param value The value to search for
	 * @return true if the value is found, false otherwise
	 */
	protected static synchronized boolean agentPropertyArrayContains(String propertyName, String delimiter, String value) {
		if(value==null || value.trim().isEmpty()) return false;
		String[] arr = getAgentPropertyArray(propertyName, delimiter, new String[]{});
		if(arr.length==0) return false;
		return Arrays.binarySearch(arr, value)>=0;
	}
	
	/**
	 * Appends the agent deployment exception to the agent property {@link #AGENT_DEPLOY_ERR_PROP}. 
	 * @param domainName The default domain name of the target MBeanServer for which the agent deploy failed
	 * @param ex The exception to append the message for
	 */
	protected static synchronized void appendAgentException(String domainName, Exception ex) {		
		appendAgentProperty(AGENT_DEPLOY_ERR_PROP, AGENT_DEPLOY_ERR_DELIM, 
				new StringBuilder("[")
					.append(domainName)
					.append("]:")
					.append(ex.toString())
					.toString()
		);
	}
	
	/**
	 * Sets or appends the passed agent property 
	 * @param propertyName The agent property name
	 * @param delimiter The value delimiter
	 * @param values The values to set or append
	 */
	protected static synchronized void appendAgentProperty(String propertyName, String delimiter, String...values) {
		if(values==null || values.length==0) return;
		Properties agentProps = VMSupport.getAgentProperties();
		String currentValue = agentProps.getProperty(propertyName, "");
		StringBuilder exmessage = new StringBuilder(currentValue);
		boolean atLeastOne = false;
		for(String v: values) {
			if(v==null || v.trim().isEmpty()) continue;
			if(exmessage.length()!=0) {
				exmessage.append(delimiter);
			}						
			exmessage.append(v.trim()).append(delimiter);
			atLeastOne = true;
		}
		if(atLeastOne) {
			for(int i = 0; i < delimiter.length(); i++) {
				exmessage.deleteCharAt(exmessage.length()-1);
			}
		}
		agentProps.put(propertyName, exmessage.toString());		
	}
	
	/**
	 * Removes the passed values from the implied array in the passed agent property  
	 * @param propertyName The agent property name
	 * @param delimiter The value delimiter
	 * @param values The values to remove
	 */
	protected static synchronized void removeAgentArrProperty(String propertyName, String delimiter, String...values) {
		String[] arr = getAgentPropertyArray(propertyName, delimiter, new String[]{});
		if(values==null || values.length==0 || arr==null || arr.length==0) return;
		Set<String> arrValues = new HashSet<String>(Arrays.asList(arr));
		for(String v: values) {
			arrValues.remove(v);
		}
		
	}
	
	
	/**
	 * Clears the agent property identified by the passed property name
	 * @param propertyName the name of the property to clear
	 */
	protected static synchronized void clearAgentProperty(String propertyName) {
		VMSupport.getAgentProperties().remove(propertyName);
	}
	
	
	
	/**
	 * Creates the alt-domain management agent jar in <b><code>${java.io.tmpdir}</code></b>.
	 * @return the name of the file
	 * @throws Exception thrown on any error
	 */
	public static String writeAgentJar() throws Exception {
		String fileName = TMP + "altdomain-management-agent.jar";
		File file = new File(fileName);
		if(!file.exists())  {
			JarOutputStream jos = null;
			try {
				Manifest manifest = new Manifest(new ByteArrayInputStream(manifest().getBytes()));
				jos = new JarOutputStream(new FileOutputStream(file), manifest);
				JarEntry entry = new JarEntry(AlternateDomainAgent.class.getName().replace('.', '/') + ".class");
				jos.putNextEntry(entry);
				jos.write(getAgentClassBytes(null));
				entry = new JarEntry(AlternateDomainAgent.class.getName().replace('.', '/') + "$PermanentExporter.class"); 
				jos.putNextEntry(entry);
				jos.write(getAgentClassBytes(AlternateDomainAgent.class.getName().replace('.', '/') + "$PermanentExporter.class"));				
				
				//AlternateDomainAgent.class.getName().replace('.', '/') + ".class"
			} finally {
				if(jos!=null) {
					try { jos.flush(); } catch (Exception e) {}
					try { jos.close(); } catch (Exception e) {}
				}
			}
		}
		return fileName;
		
	}
	
	public static void main(String[] args) {
		log("AgentClassBytes test [" + TMP + "]");
		new File(TMP  + "altdomain-management-agent.jar").delete();
		
		try {
			String agentJar = writeAgentJar();
			log("AgentJar:" + agentJar);
			JarFile jarFile = new JarFile(agentJar);
			Manifest mf = jarFile.getManifest();
			if(mf!=null) {
				StringBuilder b = new StringBuilder("Manifest Listing:");
				b.append("\n\t").append("Main");
				for(Map.Entry<Object, Object> antry : mf.getMainAttributes().entrySet()) {
					b.append("\n\t\t").append(antry.getKey()).append(":").append(antry.getValue());
				}

				for(Map.Entry<String, Attributes> entry : mf.getEntries().entrySet()) {
					b.append("\n\t").append(entry.getKey());					
					for(Map.Entry<Object, Object> antry : entry.getValue().entrySet()) {
						b.append("\n\t\t").append(antry.getKey()).append(":").append(antry.getValue());
					}
				}

				log(b);
			} else {
				log("Manifest was null");
			}
			
			String ver = jarFile.getManifest().getMainAttributes().getValue("Implementation-Version");
			double version = Double.parseDouble(ver.trim());
			log("Version:" + version);
//			boolean deleted = new File(TMP + "altdomain-management-agent.jar").delete();
//			log("AgentJar Deleted:" + deleted);
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
	
	private static byte[] getAgentClassBytes(String clazz) {
		ClassLoader cl = AlternateDomainAgent.class.getClassLoader();
		InputStream is = null;
		byte[] buff = new byte[10240];
		int bytesRead = 0;
		try {
			String resource = clazz==null ? AlternateDomainAgent.class.getName().replace('.', '/') + ".class" : clazz; 
			log("Reading resource [" + resource + "]");
			is = cl.getResourceAsStream(resource);
			ByteArrayOutputStream baos = new ByteArrayOutputStream(is.available());
			while((bytesRead=is.read(buff))!=-1) {
				baos.write(buff, 0, bytesRead);
			}
			buff = baos.toByteArray();
			log("Read Agent Class Bytes:" + buff.length);
			return buff;
		} catch (Exception ex) {
			loge("Failed to read class bytes:" + ex.toString());
			throw new RuntimeException("Failed to read class bytes", ex);
		} finally {
			if(is!=null) try { is.close(); } catch (Exception e) {}
		}		
	}
	
	
	/**
	 * Starts the management agent for the passed MBeanServer
	 * @param server The MBeanServer to start the agent for
	 * @throws Exception thrown on any error
	 */
	private static void startAgent(MBeanServer server) throws Exception {
		final ClassLoader cl = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(server.getClass().getClassLoader());
			Properties agentProps = VMSupport.getAgentProperties();
			String dd = server.getDefaultDomain();
			if(dd==null || dd.trim().isEmpty()) {
				dd = "DefaultDomain";
			}
			JMXConnectorServer cs = startLocalConnectorServer(server);
	        String address = cs.getAddress().toString();
	        // Add the local connector address to the agent properties
	        if("DefaultDomain".equals(dd)) {
	        	agentProps.put(LOCAL_CONNECTOR_ADDRESS_PROP, address);
	        } else {
	        	agentProps.put(LOCAL_CONNECTOR_ADDRESS_PROP + "." + dd, address);
	        }
	        log("Started management connector server for [" + dd + "]");
		} finally {
			Thread.currentThread().setContextClassLoader(cl);
		}
	}
	
	private static JMXConnectorServer startLocalConnectorServer(MBeanServer mbs) {
		System.setProperty("java.rmi.server.randomIDs", "true");
        Map<String, Object> env = new HashMap<String, Object>();
        env.put(RMIExporter.EXPORTER_ATTRIBUTE, new PermanentExporter());

        // The local connector server need only be available via the
        // loopback connection.
        String localhost = "localhost";
        InetAddress lh = null;
        try {
            lh = InetAddress.getByName(localhost);
            localhost = lh.getHostAddress();
        } catch (UnknownHostException x) {
        }

        // localhost unknown or (somehow) didn't resolve to
        // a loopback address.
        if (lh == null || !lh.isLoopbackAddress()) {
            localhost = "127.0.0.1";
        }
        try {
            JMXServiceURL url = new JMXServiceURL("rmi", localhost, 0);
            // Do we accept connections from local interfaces only?
            Properties props = Agent.getManagementProperties();
            if (props ==  null) {
                props = new Properties();
            }
            String useLocalOnlyStr = props.getProperty(
                    PropertyNames.USE_LOCAL_ONLY, DefaultValues.USE_LOCAL_ONLY);
            boolean useLocalOnly = Boolean.valueOf(useLocalOnlyStr).booleanValue();
            if (useLocalOnly) {
                env.put(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE,
                        new LocalRMIServerSocketFactory());
            }
            JMXConnectorServer server =
                    JMXConnectorServerFactory.newJMXConnectorServer(url, env, mbs);
            server.start();
            return server;
        } catch (Exception e) {
        	loge("Agent Configuration Exception for JMX Domain [" + mbs.getDefaultDomain() + "]");
            throw new AgentConfigurationError("Agent Configuration Exception for JMX Domain [" + mbs.getDefaultDomain() + "]", e, e.toString());
        }
        
		
	}
	
	
	/**
	 * Out logging
	 * @param msg the message to log
	 */
	protected static void log(Object msg) {
		System.out.println("[DomainAgent]" + msg);
	}
	
	/**
	 * Err logging
	 * @param msg the message to log
	 */
	protected static void loge(Object msg) {
		System.err.println("[DomainAgent]" + msg);
	}
	
	
	
	/**
	 * Generates the manifest for the agent jar
	 * @return the maifest text
	 */
	public static String manifest() {
		return "Manifest-Version: 1.0" + EOL +
				"Implementation-Version: " + VERSION +  EOL +
				"Agent-Class: " + AlternateDomainAgent.class.getName() + EOL + 
				"Premain-Class: " + AlternateDomainAgent.class.getName() + EOL;
	}
	
	
    /**
     * <p>Prevents our RMI server objects from keeping the JVM alive.</p>
     *
     * <p>We use a private interface in Sun's JMX Remote API implementation
     * that allows us to specify how to export RMI objects.  We do so using
     * UnicastServerRef, a class in Sun's RMI implementation.  This is all
     * non-portable, of course, so this is only valid because we are inside
     * Sun's JRE.</p>
     *
     * <p>Objects are exported using {@link
     * UnicastServerRef#exportObject(Remote, Object, boolean)}.  The
     * boolean parameter is called <code>permanent</code> and means
     * both that the object is not eligible for Distributed Garbage
     * Collection, and that its continued existence will not prevent
     * the JVM from exiting.  It is the latter semantics we want (we
     * already have the former because of the way the JMX Remote API
     * works).  Hence the somewhat misleading name of this class.</p>
     */
    private static class PermanentExporter implements RMIExporter {
        public Remote exportObject(Remote obj,
                int port,
                RMIClientSocketFactory csf,
                RMIServerSocketFactory ssf)
                throws RemoteException {

            synchronized (this) {
                if (firstExported == null)
                    firstExported = obj;
            }

            final UnicastServerRef ref;
            if (csf == null && ssf == null)
                ref = new UnicastServerRef(port);
            else
                ref = new UnicastServerRef2(port, csf, ssf);
            return ref.exportObject(obj, null, true);
        }

        // Nothing special to be done for this case
        public boolean unexportObject(Remote obj, boolean force)
        throws NoSuchObjectException {
            return UnicastRemoteObject.unexportObject(obj, force);
        }

        Remote firstExported;
    }

	
}


/*


*/