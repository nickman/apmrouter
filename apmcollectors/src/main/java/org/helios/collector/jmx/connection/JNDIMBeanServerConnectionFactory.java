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

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServerConnection;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.log4j.Logger;
import org.helios.collector.jmx.HSPProtocol;
import org.springframework.jmx.export.annotation.ManagedResource;


/**
 * <p>Title: JNDIMBeanServerConnectionFactory </p>
 * <p>Description: Gets an MBeanServerConnection through JNDI</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
@ManagedResource
public class JNDIMBeanServerConnectionFactory extends AbstractMBeanServerConnectionFactory {

	private static final long serialVersionUID = -7666482899032445207L;

	/**	The properties to make the JNDI Connection */
	protected Properties jndiProperties = new Properties();
	/**	The JNDI name of the MBeanServerConnection Object to retrieve */
	protected String jndiName = null;
	/**	The context acquired */
	protected Context context = null;
	/** Default key to acquire jndiName passed through Helios configuration.
	 *  If this key is missing from configuration, DEFAULT_JNDI_NAME will be used as 
	 *  a fallback.
	 */ 
	public static final String JNDI_NAME = "jndi.name";
	
	/** Default JNDI name to acquire MBeanServerConnection */
	protected static final String DEFAULT_JNDI_NAME = "jmx/invoker/RMIAdaptor";
	
	/**
	 * The connection to an MBean server is NOT created within this constructor
	 * @param jndiProperties
	 * @throws MBeanServerConnectionFactoryException
	 */
	public JNDIMBeanServerConnectionFactory(Properties jndiProperties) throws MBeanServerConnectionFactoryException {
		super(HSPProtocol.hsp);
		log = Logger.getLogger(JNDIMBeanServerConnectionFactory.class);
		if(jndiProperties == null)
			throw new MBeanServerConnectionFactoryException("JNDI properties are missing for creating an MBean Server connection");
		this.setProperties(jndiProperties);
	}

	/**
	 * Reset properties to acquire the Context
	 * @param properties
	 */
	public void setProperties(Properties properties) {
		jndiProperties.clear();
		jndiName = properties.getProperty(JNDI_NAME, DEFAULT_JNDI_NAME);
		jndiProperties.putAll(properties);
	}
	
	/**
	 * The internal getConnection implementation
	 * @return an MBeanServerConnection
	 * @throws MBeanServerConnectionFactoryException 
	 */
	protected MBeanServerConnection _getConnection() throws MBeanServerConnectionFactoryException {
		Context ctx = null;
		try {
			context = new InitialContext(jndiProperties);
			//return (MBeanServerConnection)context.lookup(jndiName);
			return JMXConnectionUtil.connectWithTimeout(context, jndiName, timeout, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			throw new MBeanServerConnectionFactoryException(e.getMessage(), e);
		} finally {
			if(ctx!=null) try { ctx.close(); } catch (Exception e) {log.debug(e.getMessage());}
		}
	}

}
