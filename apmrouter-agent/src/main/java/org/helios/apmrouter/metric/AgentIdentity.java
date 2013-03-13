/**
 * ICE Futures, US
 */
package org.helios.apmrouter.metric;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.helios.apmrouter.jmx.ConfigurationHelper;
import org.helios.apmrouter.jmx.StringHelper;

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
	 * Generates a formated string displaying the agent identity
	 * @return the agent identity
	 */
	public static String print() {
		return new StringBuilder("Agent Identity [")
		.append("\n\tAgent Name: [").append(ID.agentName).append("]")
		.append("\n\tDomain Name: [").append(ID.domain).append("]")
		.append("\n\tNQ Host Name: [").append(ID.host).append("]")
		.append("\n\tQ Host Name: [").append(ID.hostName).append("]")
		.append("\n]").toString();
	}
	
	public static void main(String[] args) {
		System.out.println(print());
	}
	
	/**
	 * Returns this JVM's determined agent name
	 * @return this JVM's determined agent name
	 */
	public String getAgentName() {
		return agentName;
	}
	
	/** This agent's default fully qualified host name */
	private String hostName = null;
	/** This agent's default agent name */
	private String agentName = null;
	/** This agent's default non-qualified host name */
	private String host = null;
	/** This agent's default domain name */
	private String domain = null;
	
	/**
	 * Creates a new AgentIdentity by divining the host name and agent name
	 */
	private AgentIdentity() {
		setHost();
		setAgent();
	}
	
	/** We don't really want these host names except as a last resort */
	public static final Set<String> UNDEZ_HOST_NAMES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
			"localhost", "127.0.0.1"
	)));
	
	/**
	 * Tries a couple of ways of getting the fully qualified host name
	 */
	private void setHost() {
		try {
			hostName = InetAddress.getLocalHost().getCanonicalHostName();
		} catch (IOException iex) {/* No Op */}
		if(hostName==null || UNDEZ_HOST_NAMES.contains(hostName.trim().toLowerCase())) {
			hostName = null;
			try {
				for(Enumeration<NetworkInterface> enumer = NetworkInterface.getNetworkInterfaces(); enumer.hasMoreElements();) {
					NetworkInterface nic = enumer.nextElement();
					if(!nic.isLoopback()) {
						for(InterfaceAddress ia: nic.getInterfaceAddresses()) {
							if(UNDEZ_HOST_NAMES.contains(ia.getAddress().getHostAddress().trim().toLowerCase())) continue;
							hostName = ia.getAddress().getCanonicalHostName();
							break;
						}
					}
					if(hostName!=null) break;
				}
			} catch (Exception e) {/* No Op */}
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
		String[] names = buildNames(hostName);
		hostName = names[2].toLowerCase();
		host = names[1].toLowerCase();
		domain = names[0].toLowerCase();
	}
	
	
	/**
	 * Extracts the non-qualified host name and domain from the passed name
	 * @param name The discovered host name
	 * @return and array with [0] = domain (reversed for hierarchy), [1] = the non-qualified host name, [2] = the full qualified host name (reversed for hierarchy)
	 */
	protected String[] buildNames(String name) {
		hostName = name.trim();
		String[] ret = new String[3];
		if(hostName.contains(".")) {
			String[] frags = hostName.replace(" ", "").split("\\.");
			ret[1] = frags[0];
			
			frags = reverseArr(frags);
			ret[2] = StringHelper.fastConcatAndDelim(".", frags);
			if(frags.length>1) {
				String[] domain = new String[frags.length-1];
				System.arraycopy(frags, 0, domain, 0, frags.length-1);
				ret[0] = StringHelper.fastConcatAndDelim(".", domain);
			} else {
				ret[0] = "";
			}
			return ret;
		}
		
		return new String[]{"", hostName, hostName};
	}
	
	/**
	 * Reverses the order of the passed array
	 * @param arr The array to reverse
	 * @return The reversed order array
	 */
	protected String[] reverseArr(String[] arr) {
		String[] ret = new String[arr.length];
		
		for(int ri = arr.length-1, i = 0; i < arr.length; i++, ri--) {
			ret[ri] = arr[i];
		}
		return ret;
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

	/**
	 * Returns the non-qualified host name
	 * @return  the non-qualified host name
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Returns the domain name which may be blank if one could not determined
	 * @return the domain name 
	 */
	public String getDomain() {
		return domain;
	}
	
}
