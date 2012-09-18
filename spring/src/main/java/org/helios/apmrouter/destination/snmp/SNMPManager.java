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

import java.io.IOException;

import org.helios.apmrouter.server.ServerComponentBean;
import org.snmp4j.CommunityTarget;
import org.snmp4j.MessageDispatcher;
import org.snmp4j.MessageDispatcherImpl;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.mp.MPv1;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultTcpTransportMapping;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.MultiThreadedMessageDispatcher;
import org.snmp4j.util.ThreadPool;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.export.annotation.ManagedAttribute;

/**
 * <p>Title: SNMPManager</p>
 * <p>Description: Builder for an SNMP community target</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.snmp.SNMPManager</code></p>
 */
public class SNMPManager extends ServerComponentBean implements InitializingBean {

	/** The transport address */
	protected Address taddress;
	/** The community name */
	protected String communityName = "public";
	/** The community target */
	protected CommunityTarget target = null;
	/** The transport mapping */
	protected TransportMapping transport = null;
	/** The message dispatcher */
	protected MessageDispatcher msgDispatcher = null;
	/** The Snmp instances to send to this community */
	protected Snmp snmp = null;
	/** The send retry count */
	protected int retryCount = 2;
	/** The thread pool size for the MultiThreadedMessageDispatcher */
	protected int dispatcherThreadCount = 1;
	
	
	
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
	@ManagedAttribute
	public void setAddress(String address) {
		try {
			taddress = GenericAddress.parse(address);
			if(target!=null) {
				target.setAddress(taddress);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Returns the address as a string
	 * @return the address
	 */
	@ManagedAttribute
	public String getAddress() {
		return taddress.toString();
	}
	
	/**
	 * Sets the community name
	 * @param communityName the community name 
	 */
	@ManagedAttribute
	public void setCommunityName(String communityName) {
		this.communityName = communityName;
		if(target!=null) {
			target.setCommunity(new OctetString(this.communityName));
		}
		
	}
	
	/**
	 * Returns the community name
	 * @return the community name
	 */
	@ManagedAttribute
	public String getCommunityName() {
		return communityName;
	}
	
	
	
	/**
	 * Returns the target for this factory
	 * @return the target
	 */
	@ManagedAttribute
	public CommunityTarget getTarget() {
		return target;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStart()
	 */
	@Override
	protected void doStart() throws Exception {		
		target = new CommunityTarget();
		target.setCommunity(new OctetString(communityName));
		target.setAddress(taddress);
		target.setVersion(SnmpConstants.version2c);
		target.setTimeout(1500);
		target.setRetries(retryCount);
		if(taddress instanceof UdpAddress) {
			transport = new DefaultUdpTransportMapping();
		} else {
			transport = new DefaultTcpTransportMapping();			
		}
		MessageDispatcher dispatcher = new MessageDispatcherImpl();
		if(dispatcherThreadCount>1) {
			msgDispatcher = new MultiThreadedMessageDispatcher(ThreadPool.create(beanName, 5), dispatcher);
		} else {
			msgDispatcher = dispatcher;
		}
		msgDispatcher.addMessageProcessingModel(new MPv2c());
		msgDispatcher.addMessageProcessingModel(new MPv1());			
		
		snmp = new Snmp(msgDispatcher, transport);
	}
	
	
	
	/**
	 * Sends the passed PDU to this community
	 * @param pdu the pdu to send
	 * @throws IOException thrown on a send exception
	 */
	public void send(PDU pdu) throws IOException {
		transport.removeTransportListener(msgDispatcher);
		snmp.send(pdu, target, null, null);
	}
	
	@Override
	public String toString() {
		return String.format(
				"SNMPManager [address=%s, communityName=%s]",
				taddress, communityName);
	}

	/**
	 * Returns the Snmp used to send to this community
	 * @return the snmp the Snmp used to send to this community
	 */
	public Snmp getSnmp() {
		return snmp;
	}

	/**
	 * Returns the sender rety count
	 * @return the retryCount
	 */
	@ManagedAttribute
	public int getRetryCount() {
		return retryCount;
	}

	/**
	 * Sets the sender rety count
	 * @param retryCount the retryCount to set
	 */
	@ManagedAttribute
	public void setRetryCount(int retryCount) {
		this.retryCount = retryCount;
		if(target!=null) {
			target.setRetries(retryCount);
		}
	}

	/**
	 * Returns the number of threads allocated to the message dispatcher
	 * @return the dispatcherThreadCount
	 */
	@ManagedAttribute
	public int getDispatcherThreadCount() {
		return dispatcherThreadCount;
	}

	/**
	 * Sets the number of threads allocated to the message dispatcher
	 * @param dispatcherThreadCount the dispatcherThreadCount to set
	 */
	public void setDispatcherThreadCount(int dispatcherThreadCount) {
		this.dispatcherThreadCount = dispatcherThreadCount;
	}
	
}
