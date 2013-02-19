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
package org.helios.collector.core;


/**
 * <p>Title: SocketAbstractCollector </p>
 * <p>Description: Abstract class for all collectors that may need to port tunnel</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public abstract class SocketAbstractCollector extends AbstractCollector {

	protected PortTunnel portTunnel = null;

	/**
	 * @return the portTunnel
	 */
	public PortTunnel getPortTunnel() {
		return portTunnel;
	}

	/**
	 * @param portTunnel the portTunnel to set
	 */
	public void setPortTunnel(PortTunnel portTunnel) {
		this.portTunnel = portTunnel;
	}	
	
}
