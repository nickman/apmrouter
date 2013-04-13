/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2013, Helios Development Group and individual contributors
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
package org.helios.apmrouter.satellite;

import static org.helios.apmrouter.sender.SenderFactory.DEFAULT_SENDER_URI;
import static org.helios.apmrouter.sender.SenderFactory.SENDER_URI_PROP;

import javax.management.ObjectName;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.jmxmp.JMXMPConnectorServer;

import org.helios.apmrouter.jmx.ConfigurationHelper;
import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.monitor.DefaultMonitorBoot;
import org.helios.apmrouter.satellite.services.attach.AttachService;
import org.helios.apmrouter.satellite.services.cascade.Cascader;
import org.helios.apmrouter.util.SimpleLogger;



/**
 * <p>Title: Satellite</p>
 * <p>Description: Command line entry point to launch Satellite</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.satellite.Satellite</code></p>
 */

public class Satellite {
	/** The system prop name to override the default JMXMP listening port */
	public static final String JMXMP_LISTEN_PORT_PROP = "org.helios.jmxmp.port";
	/** The default JMXMP listening port */
	public static final int DEFAULT_JMXMP_LISTEN_PORT = 8006;
	
	/** The system prop name to override the default JMXMP listening iface */
	public static final String JMXMP_LISTEN_IFACE_PROP = "org.helios.jmxmp.iface";
	/** The default JMXMP listening port */
	public static final String DEFAULT_JMXMP_LISTEN_IFACE = "0.0.0.0";
	
	private static JMXServiceURL serviceURL = null;
	private static JMXMPConnectorServer jmxMpServer = null;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String uri = ConfigurationHelper.getSystemThenEnvProperty(SENDER_URI_PROP, DEFAULT_SENDER_URI);
		
		String agent = ConfigurationHelper.getSystemThenEnvProperty("org.helios.agent", "satellite");
		System.setProperty("org.helios.agent", agent);
		StringBuilder b = new StringBuilder("\n\t========================================================================");
		b.append("\n\tStarting Helios APMRouter Satellite Agent v ").append(Satellite.class.getPackage().getImplementationVersion());
		b.append("\n\tHelios APMRouter Server:").append(uri);
		b.append("\n\tHelios Agent Name:").append(agent);
		b.append("\n\t========================================================================\n");
		SimpleLogger.info(b.toString());
		//System.setProperty(SENDER_URI_PROP, uri);
		//SenderFactory.getInstance().getDefaultSender().sendHello();
		//SystemClock.sleep(2000);
		SimpleLogger.info("Starting Satellite Monitors");
		DefaultMonitorBoot.satellite();
		initJmxMpServer();
		AttachService.initAttachService();
		Cascader.initCascade();
		try { Thread.currentThread().join(); } catch (Exception ex) {/* No Op */}
	}
	
	/**
	 * Initializes the JMXMP server
	 */
	protected static void initJmxMpServer() {
		int jmxmpPort = ConfigurationHelper.getIntSystemThenEnvProperty(JMXMP_LISTEN_PORT_PROP, DEFAULT_JMXMP_LISTEN_PORT);
		String jmxmpIface = ConfigurationHelper.getSystemThenEnvProperty(JMXMP_LISTEN_IFACE_PROP, DEFAULT_JMXMP_LISTEN_IFACE);
		try {
			serviceURL = new JMXServiceURL(String.format("service:jmx:jmxmp://%s:%s", jmxmpIface, jmxmpPort));
			jmxMpServer = new JMXMPConnectorServer(serviceURL, null, JMXHelper.getHeliosMBeanServer());
			JMXHelper.getHeliosMBeanServer().registerMBean(jmxMpServer, JMXHelper.objectName("javax.management.remote.jmxmp:service=JMXMPConnectorServer"));
			jmxMpServer.start();
			SimpleLogger.info("Started JMXMPConnectorServer. Listening on [", serviceURL, "]");
			final JMXMPConnectorServer f = jmxMpServer; 
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					try { f.stop(); } catch (Exception ex) {/* No Op */}
				}
			});
		} catch (Exception ex) {
			System.err.println("Failed to start JMXMP Server. Stack trace follows:");
			ex.printStackTrace(System.err);
		}
	}
	

}
