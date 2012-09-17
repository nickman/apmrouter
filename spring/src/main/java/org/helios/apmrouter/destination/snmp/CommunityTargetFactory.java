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
package org.helios.apmrouter.destination.snmp;

import java.net.InetAddress;
import java.net.URI;

import org.snmp4j.CommunityTarget;
import org.snmp4j.TransportMapping;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TcpAddress;
import org.snmp4j.smi.TransportIpAddress;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.TransportMappings;
import org.springframework.beans.factory.InitializingBean;

/**
 * <p>Title: CommunityTargetFactory</p>
 * <p>Description: Builder for an SNMP community target</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.snmp.CommunityTargetFactory</code></p>
 */
public class CommunityTargetFactory implements InitializingBean {

	/** The transport address */
	protected TransportIpAddress taddress;
	/** The community name */
	protected String communityName = "public";
	/** The community target */
	protected CommunityTarget target = null;
	/** The transport mapping */
	protected TransportMapping transport = null;
	
	/**
	 * Returns the transport mapping for this target
	 * @return the transport mapping for this target
	 */
	public TransportMapping getTransport() {
		return transport;
	}
	/**
	 * Sets the community address
	 * @param address the community address
	 */
	public void setAddress(String address) {
		try {
			URI uri  = new URI(address);
			InetAddress addr = InetAddress.getByName(uri.getHost());
			if("UDP".equalsIgnoreCase(uri.getScheme())) {
				taddress = new UdpAddress(addr, uri.getPort());
			} else {
				taddress = new TcpAddress(addr, uri.getPort());
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	/**
	 * Sets the community name
	 * @param communityName the community name 
	 */
	public void setCommunityName(String communityName) {
		this.communityName = communityName;
	}
	
	/**
	 * Returns the target for this factory
	 * @return the target
	 */
	public CommunityTarget getTarget() {
		return target;
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {		
		target = new CommunityTarget(taddress, new OctetString(communityName));
		target.setVersion(SnmpConstants.version2c);
		transport = TransportMappings.getInstance().createTransportMapping(taddress);
	}
	
	@Override
	public String toString() {
		return String.format(
				"CommunityTargetFactory [address=%s, communityName=%s]",
				taddress, communityName);
	}
	
}
