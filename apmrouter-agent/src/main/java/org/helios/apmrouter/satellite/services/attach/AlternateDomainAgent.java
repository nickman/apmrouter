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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
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
	/** The agent property prefix for the connector addresses */
	public static final String LOCAL_CONNECTOR_ADDRESS_PROP = "com.sun.management.jmxremote.localConnectorAddress.";
	/**
	 * Command line agent startup entry point
	 * @param args The agent arguments
	 * @throws Exception thrown on any error
	 */
	public static void premain(String args) throws Exception {
		agentmain(args);		
	}
	
	/**
	 * Entry point for the attach API
	 * @param args The agent arguments
	 */
	public static void agentmain(String args)  {
		log("Starting AlternateDomainAgent");
		final String mbeanBuilder = System.getProperty("javax.management.builder.initial", null);
		System.clearProperty("javax.management.builder.initial");
		try {
			for(MBeanServer server : MBeanServerFactory.findMBeanServer(null)) {
				String dd = server.getDefaultDomain();
				// Don't load the platform mbean server. 
				// (sometimes the default domain for the platform mbeanserver is null)
				if(dd==null || dd.trim().isEmpty() || dd.equals("DefaultDomain")) continue;
				log("Starting connector server for [" + dd + "]");
				startAgent(server);
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		} finally {
			if(mbeanBuilder!=null) {
				System.setProperty("javax.management.builder.initial", mbeanBuilder);
			}
		}
	}
	
	
	/**
	 * Creates the alt-domain management agent jar in <b><code>${java.io.tmpdir}</code></b>.
	 * @return the name of the file
	 * @throws Exception thrown on any error
	 */
	public static String writeAgentJar() throws Exception {
		String fileName = System.getProperty("java.io.tmpdir") + File.separator + "altdomain-management-agent.jar";
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
		log("AgentClassBytes test");
		new File(System.getProperty("java.io.tmpdir") + File.separator + "altdomain-management-agent.jar").delete();
		
		try {
			String agentJar = writeAgentJar();
			log("AgentJar:" + agentJar);
			new File(System.getProperty("java.io.tmpdir") + File.separator + "altdomain-management-agent.jar").delete();
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
	
	
	private static void startAgent(MBeanServer server) throws Exception {
		Properties agentProps = VMSupport.getAgentProperties();
		String dd = server.getDefaultDomain();
		JMXConnectorServer cs = startLocalConnectorServer(server);
        String address = cs.getAddress().toString();
        // Add the local connector address to the agent properties
        agentProps.put(LOCAL_CONNECTOR_ADDRESS_PROP + dd, address);
        log("Started management connector server for [" + dd + "] at [" + address + "]");
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