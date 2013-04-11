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
package org.helios.collector.jmx.connection;

import org.apache.log4j.Logger;
import org.helios.collector.jmx.HSPProtocol;
import org.springframework.jmx.export.annotation.ManagedResource;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXServiceURL;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * <p>Title: JMXServiceURLConnectionFactory </p>
 * <p>Description: Gets an MBeanServerConnection using JMXServiceURL</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */

public class JMXServiceURLConnectionFactory extends AbstractMBeanServerConnectionFactory {
	
	private static final long serialVersionUID = -5166257255134636487L;
	protected String jmxServiceURL = null;
	protected JMXServiceURL serviceURL = null;
	protected Map<String, Object> environment = null;
	/**
	 * The connection to an MBean server is NOT created within this constructor
	 * @param jmxServiceURL
	 * @throws MBeanServerConnectionFactoryException
	 */
	public JMXServiceURLConnectionFactory(String jmxServiceURL) throws MBeanServerConnectionFactoryException{
		super(HSPProtocol.hsp);
		log = Logger.getLogger(JMXServiceURLConnectionFactory.class);
		if(jmxServiceURL == null)
			throw new MBeanServerConnectionFactoryException("JMX Service URL is missing for making an MBean Server connection");
		this.jmxServiceURL = jmxServiceURL;
		try {
			serviceURL = new JMXServiceURL(jmxServiceURL);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create new JMXServiceURL from [" + jmxServiceURL + "]", e);	
		}
	}
	
	/**
	 * The internal getConnection implementation
	 * @return an MBeanServerConnection
	 * @throws MBeanServerConnectionFactoryException
	 */
	protected MBeanServerConnection _getConnection() throws MBeanServerConnectionFactoryException {
		try {
			//return JMXConnectorFactory.connect(serviceURL).getMBeanServerConnection();
			return JMXConnectionUtil.connectWithTimeout(serviceURL, environment, timeout, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			throw new MBeanServerConnectionFactoryException(e.getMessage(), e);
		}
	}

	public Map<String, Object> getEnvironment() {
		return environment;
	}

	public void setEnvironment(Map<String, Object> environment) {
		this.environment = environment;
	}
	



}
