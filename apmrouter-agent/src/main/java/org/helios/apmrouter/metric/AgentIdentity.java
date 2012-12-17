/**
 * ICE Futures, US
 */
package org.helios.apmrouter.metric;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import org.helios.apmrouter.jmx.ConfigurationHelper;

/**
 * <p>Title: AgentIdentity</p>
 * <p>Description: Class to auto-configure the identity of the default agent for this JVM and to register identity plugins that can override the default identity locator.</p> 
 * <p>Company: ICE Futures US</p>
 * @author Whitehead (nicholas.whitehead@theice.com)
 * @version $LastChangedRevision$
 * <p><code>org.helios.apmrouter.metric.AgentIdentity</code></p>
 */

public enum AgentIdentity {
	/** The AgentIdentity singleton */
	ID;
	
	

	/**
	 * Returns this JVM's determined host name
	 * @return this JVM's determined host name
	 */	
	public String getHostName() {
		return hostName;
	}
	
	/**
	 * Returns this JVM's determined agent name
	 * @return this JVM's determined agent name
	 */
	public String getAgentName() {
		return agentName;
	}
	
	/** This agent's default host name */
	private String hostName = null;
	/** This agent's default agent name */
	private String agentName = null;
	
	/**
	 * Creates a new AgentIdentity by divining the host name and agent name
	 */
	private AgentIdentity() {
		setHost();
		setAgent();
	}
	
	/**
	 * Tries a couple of ways of getting the fully qualified host name
	 */
	private void setHost() {
		try {
			hostName = InetAddress.getLocalHost().getCanonicalHostName();
		} catch (IOException iex) {}
		if(hostName==null) {
			try {
				for(Enumeration<NetworkInterface> enumer = NetworkInterface.getNetworkInterfaces(); enumer.hasMoreElements();) {
					NetworkInterface nic = enumer.nextElement();
					if(!nic.isLoopback()) {
						for(InterfaceAddress ia: nic.getInterfaceAddresses()) {
							if("127.0.0.1".equals(ia.getAddress().getHostAddress())) continue;
							hostName = ia.getAddress().getCanonicalHostName();
							break;
						}
					}
					if(hostName!=null) break;
				}
			} catch (Exception e) {}
		}
		if(hostName==null) {
			boolean isWindows = System.getProperty("os.name", "no").toLowerCase().contains("windows");
			if(isWindows) hostName = System.getenv("COMPUTERNAME");
			else hostName = System.getenv("HOSTNAME");
		}
		if(hostName==null) {
			hostName = ManagementFactory.getRuntimeMXBean().getName().split("@")[1];
		}
		if(hostName==null) {
			hostName = "UNKNOWN";
		}
		hostName = cleanHostName(hostName).toLowerCase();
	}
	
	
	protected String cleanHostName(String hostName) {
		if(hostName.contains(".")) {
			String[] frags = hostName.split("\\.");
			StringBuilder b = new StringBuilder();
			for(int i = 0; i < frags.length; i++) {
				b.insert(0, (frags[i] + "."));				
			}
			b.deleteCharAt(b.length()-1);
			return b.toString();
		}
		
		return hostName;
	}
	
	/**
	 * Tries a couple of ways of getting the agent name
	 */
	private void setAgent() {
		// Try system props, then env for the agent name property
		agentName = ConfigurationHelper.getEnvThenSystemProperty("org.helios.agent", null);
		if(agentName!=null && !agentName.trim().isEmpty()) {
			System.setProperty("org.helios.agent", agentName);
			return;
		}		
				//System.getProperty("theice.agent.name", System.getenv("theice.agent.name"));
		if(agentName==null) {
			// Perhaps we're running in a jboss server
			agentName = System.getProperty("jboss.server.name");
			if(agentName!=null && !agentName.trim().isEmpty()) {
				System.setProperty("org.helios.agent", agentName);
				return;
			}
		}
		if(agentName==null) {
			// Ok, we'll use the PID until we implement agent naming plugins
			agentName = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
		}
	}
	
}
