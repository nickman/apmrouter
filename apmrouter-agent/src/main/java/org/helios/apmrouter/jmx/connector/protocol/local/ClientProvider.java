/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2012, Helios Development Group and individual contributors
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
package org.helios.apmrouter.jmx.connector.protocol.local;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorProvider;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;

/**
 * <p>Title: ClientProvider</p>
 * <p>Description: A {@link JMXConnectorProvider} for local in VM connections.</p>
 * <p>Sample {@link JMXServiceURL}: <b><code>service:jmx:local://DefaultDomain</code></b></p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.jmx.connector.protocol.local.ClientProvider</code></p>
 */

public class ClientProvider implements JMXConnectorProvider {

	/**
	 * {@inheritDoc}
	 * @see javax.management.remote.JMXConnectorProvider#newJMXConnector(javax.management.remote.JMXServiceURL, java.util.Map)
	 */
	@Override
	public JMXConnector newJMXConnector(JMXServiceURL serviceURL, Map<String, ?> environment) throws IOException {
		if(serviceURL==null) throw new IllegalArgumentException("The passed JMXServiceURL was null", new Throwable());
		if (!serviceURL.getProtocol().equals("local")) {
            throw new MalformedURLException("Protocol not local: " +
                                            serviceURL.getProtocol());
        }		
		LocalJMXConnector connector = new LocalJMXConnector();
		connector.localURL = serviceURL;
		return connector;
	}

/*
import org.helios.apmrouter.jmx.JMXHelper;
import javax.management.remote.*;
import javax.management.*;

println System.getProperty("jmx.remote.protocol.provider.pkgs");
println System.setProperty("jmx.remote.protocol.provider.pkgs", "com.sun.jmx.remote.protocol|org.helios.apmrouter.jmx.connector.protocol");

println JMXHelper.getJMXConnection("service:jmx:rmi://localhost:8002/jndi/rmi://localhost:8005/jmxrmi").getMBeanServerConnection();
println JMXHelper.getJMXConnection("service:jmx:local://DefaultDomain").getMBeanServerConnection();
 */
	
}
