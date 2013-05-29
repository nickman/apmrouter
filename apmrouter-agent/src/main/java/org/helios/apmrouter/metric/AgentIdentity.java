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
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import org.helios.apmrouter.jmx.ConfigurationHelper;
import org.helios.apmrouter.jmx.StringHelper;
import org.helios.apmrouter.satellite.services.attach.JVM;

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
	ID("localhost", "127.0.0.1");
	
	/** We don't really want these host names except as a last resort */
	public final Set<String> UNDEZ_HOST_NAMES; 
	
	/** The agent property name for the JVM's main class */
	public static final String JVM_MAIN_CLASS = "sun.java.command";

	/** The pattern for an IPV4 Octet */
	public static final String OCTET = "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.";
	/** Pattern to match an IP V4 address */
	public static final Pattern IPV4_ADDRESS_PATTERN = Pattern.compile("^" + OCTET + OCTET + OCTET + OCTET + "?");
	
	
	/**
	 * Creates a new AgentIdentity by divining the host name and agent name
	 */
	private AgentIdentity(String...undHostNames) {
		UNDEZ_HOST_NAMES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(undHostNames)));
		setHost();
		setAgent();
		
	}
	
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
	 * Tries a couple of ways of getting the fully qualified host name
	 */
	private void setHost() {
		try {
			hostName = InetAddress.getLocalHost().getCanonicalHostName();
		} catch (IOException iex) {/* No Op */}
		if(hostName==null || UNDEZ_HOST_NAMES.contains(hostName.trim().toLowerCase()) || hostName.contains(":") || hostName.contains("%")) { 
			hostName = null;
			try {
				for(Enumeration<NetworkInterface> enumer = NetworkInterface.getNetworkInterfaces(); enumer.hasMoreElements();) {
					NetworkInterface nic = enumer.nextElement();
					if(!nic.isLoopback()) {
						for(InterfaceAddress ia: nic.getInterfaceAddresses()) {
							hostName = ia.getAddress().getCanonicalHostName().trim().toLowerCase();
							if(UNDEZ_HOST_NAMES.contains(hostName) || hostName.contains(":") || hostName.contains("%")) {
								hostName = null;
								continue;
							}
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
		name = name.trim();
		hostName = name;
		String[] ret = new String[3];
		if(IPV4_ADDRESS_PATTERN.matcher(name).matches()) {
			return new String[]{"", hostName, hostName};
		}
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
			agentName = System.getProperty(JVM_MAIN_CLASS, null);
			if(agentName!=null && agentName.trim().isEmpty()) {
				agentName = null;
			} else {
				agentName = JVM.cleanDisplayName(agentName);
			}
		}
		if(agentName==null) {
			try {
				Class<?> clazz = Class.forName("sun.management.Agent");
				clazz.getDeclaredMethod("loadManagementProperties").invoke(null);
				Properties p = (Properties)clazz.getDeclaredMethod("getManagementProperties").invoke(null);
				agentName = p.getProperty(JVM_MAIN_CLASS, null);
				if(agentName!=null && agentName.trim().isEmpty()) {
					agentName = null;
				} else {
					agentName = JVM.cleanDisplayName(agentName);
				}
			} catch (Exception ex) {
				agentName = null;
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
