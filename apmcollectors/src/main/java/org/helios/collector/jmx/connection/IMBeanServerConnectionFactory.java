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

import org.apache.commons.pool.PoolableObjectFactory;

import javax.management.MBeanServerConnection;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>Title: MBeanServerConnectionFactory</p>
 * <p>Description: Interface for MBeanServerConnectionFactory</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */

public interface IMBeanServerConnectionFactory extends PoolableObjectFactory {
	/** The Helios MBeanServer domain where mbean server connection factories are registered */
	public static final String CONNECTION_MBEAN_DOMAIN = "org.helios.jmx.mbeanservers";
	/** The proprery key for hosts */
	public static final String PROP_KEY_HOST = "host";
	/** The proprery key for vms */
	public static final String PROP_KEY_VM = "vm";
	/** The proprery key for default domains */
	public static final String PROP_KEY_DOMAIN = "domain";
	/** The proprery key for sub domains */
	public static final String PROP_KEY_SUB_DOMAIN = "subdomain";

	/** A set of the recognized property keys for MBeanServerConnectionFactory object names */
	public static final Set<String> PROP_KEYS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(PROP_KEY_HOST, PROP_KEY_VM, PROP_KEY_DOMAIN, PROP_KEY_SUB_DOMAIN)));
	
	/**
	 * Returns a reference to the existing connection or creates a new connection if not previously created.
	 * 
	 * @return An MBeanConnection.
	 * @throws MBeanServerConnectionFactoryException
	 */
	public MBeanServerConnection getConnection() throws MBeanServerConnectionFactoryException;
	
	/**
	 * Returns the host Id that this factory connects to 
	 * @return the host Id that this factory connects to
	 */
	public String getHostId();
	
	/**
	 * Returns a new connection
	 * @return An MBeanConnection.
	 * @throws MBeanServerConnectionFactoryException
	 */
	public MBeanServerConnection getNewConnection() throws MBeanServerConnectionFactoryException;	
	
	
	/**
	 * Recreates a new MBeanServerConnection
	 * 
	 * @return An MBeanConnection.
	 * @throws MBeanServerConnectionFactoryException
	 */
	public MBeanServerConnection resetConnection() throws MBeanServerConnectionFactoryException;
	
	/**
	 * Returns a pooled connection
	 * @return An MBeanConnection.
	 * @throws MBeanServerConnectionFactoryException
	 */
	public MBeanServerConnection getPooledConnection() throws MBeanServerConnectionFactoryException;
	
	/**
	 * Returns a pooled connection to the pool
	 * @param connection the MBeanServerConnection to return to the pool
	 * @throws Exception
	 */
	public void returnPooledConnection(MBeanServerConnection connection) throws Exception;
	
	
}

