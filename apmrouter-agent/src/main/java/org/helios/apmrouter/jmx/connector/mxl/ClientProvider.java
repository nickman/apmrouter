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
package org.helios.apmrouter.jmx.connector.mxl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorProvider;
import javax.management.remote.JMXServiceURL;

/**
 * <p>Title: ClientProvider</p>
 * <p>Description: JMX remoting client provider for acquiring {@link MBeanServerConnection}s to agents from within the APMRouter server.</p>
 * <p>{@link JMXServiceURL} format is: <b><code>service:jmx:mxl:///[&lt;protocol//&gt;][agent-name]@[host]</code></b></p>
 * <p>Sample {@link JMXServiceURL}: <b><code>service:jmx:mxl:///udp//myagent@myhost</code></b></p> 
 * <p>Sample {@link JMXServiceURL}: <b><code>service:jmx:mxl:///myagent@myhost</code></b></p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.jmx.connector.mxl.ClientProvider</code></p>
 */

public class ClientProvider implements JMXConnectorProvider {
	/** The MXL protocol URI parser */
	public static final Pattern MXL_PATTERN = Pattern.compile("/(.*?)@(.*?);protocol=(.*?)|/(.*?)@(.*?)");
	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnectorProvider#newJMXConnector(javax.management.remote.JMXServiceURL, java.util.Map)
	 */
	@Override
	public JMXConnector newJMXConnector(JMXServiceURL serviceURL, Map<String, ?> environment) throws IOException {
		if(serviceURL==null) throw new IllegalArgumentException("The passed serviceURL was null", new Throwable());
		try {
			String urlPath = serviceURL.getURLPath();
			Matcher m = MXL_PATTERN.matcher(urlPath);
			
			if(!m.matches()) throw new Exception("Failed to match expected pattern [" + MXL_PATTERN.pattern() + "]");
			String[] args = new String[m.groupCount()];
			for(int i = 1; i <= m.groupCount(); i++) {
				args[i-1] = m.group(i);
			}
			if(args[0]==null) {				
				return new MXLocalJMXConnector(args[4], args[3], "DefaultDomain");
			}
			Map<String, String> kvp = parseArgs(args[2]);
			String protocol = kvp.get("protocol");
			String domain = kvp.get("domain");
			return new MXLocalJMXConnector(args[1], args[0], protocol, domain==null ? "DefaultDomain" : domain);
		} catch (Exception ex) {
			throw new IOException("Failed to connect with JMXServiceURL [" + serviceURL + "]", ex);
		}
	}
	
	/**
	 * Parses the JMXServiceURL URL arguments
	 * @param argStr The URL string to parse
	 * @return a map of parsed name value pairs
	 */
	protected Map<String, String> parseArgs(String argStr) {
		Map<String, String> map = new HashMap<String, String>();
		String[] pairs = argStr.trim().replace(" ", "").split(",");
		for(String pair: pairs) {
			String[] keyVal = pair.split("=");
			map.put(keyVal[0], keyVal[1]);
		}
		return map;
	}

}
