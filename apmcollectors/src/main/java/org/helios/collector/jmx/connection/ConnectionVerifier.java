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
import org.helios.apmrouter.jmx.JMXHelper;

import javax.management.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * <p>Title: ConnectionVerifier</p>
 * <p>Description: This task is handed to ThreadPoolExecutor to verify the health of 
 * 	  remote JMX connections registered with Helios.  To check a connection, a call 
 *    to getDomains is made on the remote MBeanServer.  The returned list of domains 
 *    are processed to see of there are any changes from the previous poll.  
 *    Notifications are sent out for online/offline connection status and also for 
 *    any domain changes. </p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public class ConnectionVerifier implements Runnable{
	
	/** Reference to Helios MBeanServer */
	protected MBeanServer heliosJMX = null;
	
	protected ObjectName remoteJMXfactoryObjectName = null;
	
	Logger log = Logger.getLogger(getClass());
	
	public ConnectionVerifier(ObjectName remoteJMXfactoryObjectName){
		heliosJMX = JMXHelper.getHeliosMBeanServer();
		this.remoteJMXfactoryObjectName = remoteJMXfactoryObjectName;
	}

	public void run() {
		long startTime = System.currentTimeMillis();
		MBeanServerConnection remoteJMXConnection = null;
		try {
			remoteJMXConnection = (MBeanServerConnection)heliosJMX.invoke(remoteJMXfactoryObjectName, "getConnection", null, null);
			Object[] params = { new ArrayList(Arrays.asList(remoteJMXConnection.getDomains()))};
			String[] signature = new String[params.length];
			for (int i = 0; i < params.length; i++) {
				signature[i] = params[i].getClass().getName();
			}
			invokeTarget(remoteJMXfactoryObjectName, "processDomains", params, signature);
		}catch(java.rmi.ConnectException jncx){
			//jncx.printStackTrace();
		    updateConnectionStatus(remoteJMXfactoryObjectName);
		}catch(Exception e) {
			//e.printStackTrace();
			updateConnectionStatus(remoteJMXfactoryObjectName);
		}finally{
			log.debug(Thread.currentThread().getName() + " -- Total time spent checking "+ remoteJMXfactoryObjectName+ " = " + (System.currentTimeMillis() - startTime));
		}
	}
	
	/**
	 * Call Remote JMX Connection factory registered in Helios MBeanServer to update the status
	 * 
	 * @param remoteObjectName - ObjectName with which the remote JMXConnectionFactory is registered.
	 */
	private void updateConnectionStatus(ObjectName remoteObjectName) {
		log.info("Offline remote JMX server: " + remoteJMXfactoryObjectName.getCanonicalName());
		Object[] params = { Boolean.valueOf(false)};
		String[] signature = { "boolean" };
		invokeTarget(remoteObjectName, "setConnectionStatus", params, signature);
	}
	
	/**
	 * @param oName
	 * @param params
	 * @param signature
	 */
	private void invokeTarget(ObjectName oName, String method, Object[] params,
			String[] signature) {
		try {
			heliosJMX.invoke(oName, method, params, signature);
		} catch (InstanceNotFoundException e) {
			e.printStackTrace();
		} catch (ReflectionException e) {
			e.printStackTrace();
		} catch (MBeanException e) {
			e.printStackTrace();
		}
	}

}
