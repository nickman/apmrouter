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
package org.helios.collector.jmx;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;

/**
 * <p>Title: LocalMBeanServerConnectionFactory</p>
 * <p>Description: A faux MBeanServerConnectionFactory for local in-vm MBeanServers</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * @version $LastChangedRevision$
 * <p><code>org.helios.collectors.jmx.LocalMBeanServerConnectionFactory</code></p>
 */

public class LocalMBeanServerConnectionFactory extends AbstractMBeanServerConnectionFactory {
	/** The MBeanServer this factory was created for  */
	protected final MBeanServer server;
	/**  */
	private static final long serialVersionUID = -2495126728370279542L;

	/**
	 * Creates a new LocalMBeanServerConnectionFactory
	 */
	public LocalMBeanServerConnectionFactory(MBeanServer server) {
		super(HSPProtocol.lhsp);
		this.server = server;
	}

	/**
	 * @return
	 * @throws Exception
	 */
	@Override
	protected MBeanServerConnection _getConnection() throws Exception {		
		return server;
	}

}
