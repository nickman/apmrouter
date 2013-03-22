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
package org.helios.apmrouter.jmx.mbeanserver;

import javax.management.*;
import java.util.Set;

/**
 * <p>Title: AsynchJMXResponseListener</p>
 * <p>Description: Defines a listener for receiving responses from asynchronous {@link MBeanServerConnection} invocations.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.jmx.mbeanserver.AsynchJMXResponseListener</code></p>
 */

public interface AsynchJMXResponseListener {

	/**
	 * Asynch callback for a response for {@link MBeanServerConnection#invoke(javax.management.ObjectName, String, Object[], String[])}
	 * @param requestId The request ID of the original request
	 * @param result The invocation return value
	 */
	public void invokeResponse(int requestId, Object result);
	/**
	 * Asynch callback for a response for {@link MBeanServerConnection#getDefaultDomain()}
	 * @param requestId The request ID of the original request
	 * @param result The invocation return value
	 */
	public void getDefaultDomainResponse(int requestId, String result);
	/**
	 * Asynch callback for a response for {@link MBeanServerConnection#isRegistered(javax.management.ObjectName)}
	 * @param requestId The request ID of the original request
	 * @param result The invocation return value
	 */
	public void isRegisteredResponse(int requestId, boolean result);
	/**
	 * Asynch callback for a response for {@link MBeanServerConnection#getAttributes(javax.management.ObjectName, String[])}
	 * @param requestId The request ID of the original request
	 * @param result The invocation return value
	 */
	public void getAttributesResponse(int requestId, AttributeList result);
	/**
	 * Asynch callback for a response for {@link MBeanServerConnection#getAttribute(javax.management.ObjectName, String)}
	 * @param requestId The request ID of the original request
	 * @param result The invocation return value
	 */
	public void getAttributeResponse(int requestId, Object result);
	/**
	 * Asynch callback for a response for {@link MBeanServerConnection#isInstanceOf(javax.management.ObjectName, String)}
	 * @param requestId The request ID of the original request
	 * @param result The invocation return value
	 */
	public void isInstanceOfResponse(int requestId, boolean result);
	/**
	 * Asynch callback for a response for {@link MBeanServerConnection#setAttribute(javax.management.ObjectName, javax.management.Attribute)}
	 * @param requestId The request ID of the original request
	 */
	public void setAttributeResponse(int requestId);
	/**
	 * Asynch callback for a response for {@link MBeanServerConnection#setAttributes(javax.management.ObjectName, AttributeList)}
	 * @param requestId The request ID of the original request
	 * @param result The invocation return value
	 */
	public void setAttributesResponse(int requestId, AttributeList result);
	/**
	 * Asynch callback for a response for {@link MBeanServerConnection#addNotificationListener(javax.management.ObjectName, javax.management.NotificationListener, javax.management.NotificationFilter, Object)}
	 * and other overloaded signatures
	 * @param requestId The request ID of the original request
	 */
	public void addNotificationListenerResponse(int requestId);
	/**
	 * Asynch callback for a response for {@link MBeanServerConnection#createMBean(String, javax.management.ObjectName)}
	 * and other overloaded signatures
	 * @param requestId The request ID of the original request
	 * @param result The invocation return value
	 */
	public void createMBeanResponse(int requestId, ObjectInstance result);
	/**
	 * Asynch callback for a response for {@link MBeanServerConnection#getDomains()}
	 * @param requestId The request ID of the original request
	 * @param result The invocation return value
	 */
	public void getDomainsResponse(int requestId, String[] result);
	/**
	 * Asynch callback for a response for {@link MBeanServerConnection#getMBeanCount()}
	 * @param requestId The request ID of the original request
	 * @param result The invocation return value
	 */
	public void getMBeanCountResponse(int requestId, Integer result);
	/**
	 * Asynch callback for a response for {@link MBeanServerConnection#getMBeanInfo(javax.management.ObjectName)}
	 * @param requestId The request ID of the original request
	 * @param result The invocation return value
	 */
	public void getMBeanInfoResponse(int requestId, MBeanInfo result);
	/**
	 * Asynch callback for a response for {@link MBeanServerConnection#getObjectInstance(javax.management.ObjectName)}
	 * @param requestId The request ID of the original request
	 * @param result The invocation return value
	 */
	public void getObjectInstanceResponse(int requestId, ObjectInstance result);
	/**
	 * Asynch callback for a response for {@link MBeanServerConnection#queryMBeans(javax.management.ObjectName, javax.management.QueryExp)}
	 * @param requestId The request ID of the original request
	 * @param result The invocation return value
	 */
	public void queryMBeansResponse(int requestId, Set<ObjectInstance> result);
	/**
	 * Asynch callback for a response for {@link MBeanServerConnection#queryNames(javax.management.ObjectName, javax.management.QueryExp)}
	 * @param requestId The request ID of the original request
	 * @param result The invocation return value
	 */
	public void queryNamesResponse(int requestId, Set<ObjectName> result);
	/**
	 * Asynch callback for a response for {@link MBeanServerConnection#removeNotificationListener(ObjectName, javax.management.NotificationListener)}
	 * and other overloaded signatures
	 * @param requestId The request ID of the original request
	 */
	public void removeNotificationListenerResponse(int requestId);
	/**
	 * Asynch callback for a response for {@link MBeanServerConnection#unregisterMBean(ObjectName)}
	 * @param requestId The request ID of the original request
	 */
	public void unregisterMBeanResponse(int requestId);
	
	/**
	 * Callback received when an asynchronous {@link MBeanServerConnection} invocation throws an exceptiom
	 * @param requestId The request ID of the original request
	 * @param t The throwable thrown from the asynchronous invocation
	 */
	public void onException(int requestId, Throwable t);
	
	/**
	 * Callback received when an asynchronous {@link MBeanServerConnection} invocation request times out
	 * @param requestId The request ID of the original request
	 * @param timeout The in-effect timeout period in ms.
	 */
	public void onTimeout(int requestId, long timeout);
	
	/**
	 * Callback received when the underlying connection is closed.
	 */
	public void onClose();

}


/*
import javax.management.*;
unique = [:];
MBeanServerConnection.class.getDeclaredMethods().each() {
    unique.put(it.getName(), it);
}
unique.values().each() {
    if(it.getReturnType()==void.class) {
        println "public void ${it.getName()}Response(int requestId);";
    } else {
        println "public void ${it.getName()}Response(int requestId, ${it.getReturnType().getSimpleName()} result);";
    }
}
return null;
*/