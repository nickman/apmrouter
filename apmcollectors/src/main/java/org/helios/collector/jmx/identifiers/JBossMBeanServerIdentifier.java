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
package org.helios.collector.jmx.identifiers;

import org.helios.apmrouter.jmx.JMXHelper;

import javax.management.*;
import java.io.IOException;

/**
 * <p>Title: JBossMBeanServerIdentifier</p>
 * <p>Description: Acquires the host and JBoss Server name from a JBoss MBeanServer connection</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.collectors.jmx.identifiers.JBossMBeanServerIdentifier</code></p>
 */

public class JBossMBeanServerIdentifier extends AbstractMBeanServerIdentifier {

	/**
	 * Returns a string array containing the host and vm identifiers
	 * @param connection The MBeanServerConnection to get the identifiers from
	 * @return A String array with the host id in [0] and the VM id in [1].
	 * @throws IOException 
	 * @throws ReflectionException 
	 * @throws MBeanException 
	 * @throws InstanceNotFoundException 
	 * @throws AttributeNotFoundException 
	 */
	@Override
	public String[] getHostVMId(MBeanServerConnection connection) throws AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException, IOException {
		try {
			String[] ids = new String[3];
			ids[1] = (String)connection.getAttribute(JMXHelper.objectName("jboss.system:type=ServerConfig"), "ServerName"); 
			ids[0] = (String)connection.getAttribute(JMXHelper.objectName("jboss.system:type=ServerInfo"), "HostName");
			ids[2] = connection.getDefaultDomain();
			return ids;
		} catch (Exception e) {
			return getDefaultHostVMId(connection);
		}
	}

}
