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

import javax.management.AttributeList;
import javax.management.MBeanInfo;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.util.Set;

/**
 * <p>Title: EmptyAsynchJMXResponseListener</p>
 * <p>Description: An empty implementation of {@link AsynchJMXResponseListener} for simple extending implementations.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.jmx.mbeanserver.EmptyAsynchJMXResponseListener</code></p>
 */

public class EmptyAsynchJMXResponseListener implements AsynchJMXResponseListener {
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.mbeanserver.AsynchJMXResponseListener#onClose()
	 */
	@Override
	public void onClose() {
		/* No Op */
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.mbeanserver.AsynchJMXResponseListener#onTimeout(int, long)
	 */
	@Override
	public void onTimeout(int requestId, long timeout) {
		/* No Op */
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.mbeanserver.AsynchJMXResponseListener#invokeResponse(int, java.lang.Object)
	 */
	@Override
	public void invokeResponse(int requestId, Object result) {
		/* No Op */

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.mbeanserver.AsynchJMXResponseListener#getDefaultDomainResponse(int, java.lang.String)
	 */
	@Override
	public void getDefaultDomainResponse(int requestId, String result) {
		/* No Op */

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.mbeanserver.AsynchJMXResponseListener#isRegisteredResponse(int, boolean)
	 */
	@Override
	public void isRegisteredResponse(int requestId, boolean result) {
		/* No Op */

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.mbeanserver.AsynchJMXResponseListener#getAttributesResponse(int, javax.management.AttributeList)
	 */
	@Override
	public void getAttributesResponse(int requestId, AttributeList result) {
		/* No Op */

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.mbeanserver.AsynchJMXResponseListener#getAttributeResponse(int, java.lang.Object)
	 */
	@Override
	public void getAttributeResponse(int requestId, Object result) {
		/* No Op */

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.mbeanserver.AsynchJMXResponseListener#isInstanceOfResponse(int, boolean)
	 */
	@Override
	public void isInstanceOfResponse(int requestId, boolean result) {
		/* No Op */

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.mbeanserver.AsynchJMXResponseListener#setAttributeResponse(int)
	 */
	@Override
	public void setAttributeResponse(int requestId) {
		/* No Op */

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.mbeanserver.AsynchJMXResponseListener#setAttributesResponse(int, javax.management.AttributeList)
	 */
	@Override
	public void setAttributesResponse(int requestId, AttributeList result) {
		/* No Op */

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.mbeanserver.AsynchJMXResponseListener#addNotificationListenerResponse(int)
	 */
	@Override
	public void addNotificationListenerResponse(int requestId) {
		/* No Op */

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.mbeanserver.AsynchJMXResponseListener#createMBeanResponse(int, javax.management.ObjectInstance)
	 */
	@Override
	public void createMBeanResponse(int requestId, ObjectInstance result) {
		/* No Op */

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.mbeanserver.AsynchJMXResponseListener#getDomainsResponse(int, java.lang.String[])
	 */
	@Override
	public void getDomainsResponse(int requestId, String[] result) {
		/* No Op */

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.mbeanserver.AsynchJMXResponseListener#getMBeanCountResponse(int, java.lang.Integer)
	 */
	@Override
	public void getMBeanCountResponse(int requestId, Integer result) {
		/* No Op */

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.mbeanserver.AsynchJMXResponseListener#getMBeanInfoResponse(int, javax.management.MBeanInfo)
	 */
	@Override
	public void getMBeanInfoResponse(int requestId, MBeanInfo result) {
		/* No Op */

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.mbeanserver.AsynchJMXResponseListener#getObjectInstanceResponse(int, javax.management.ObjectInstance)
	 */
	@Override
	public void getObjectInstanceResponse(int requestId, ObjectInstance result) {
		/* No Op */

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.mbeanserver.AsynchJMXResponseListener#queryMBeansResponse(int, java.util.Set)
	 */
	@Override
	public void queryMBeansResponse(int requestId, Set<ObjectInstance> result) {
		/* No Op */

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.mbeanserver.AsynchJMXResponseListener#queryNamesResponse(int, java.util.Set)
	 */
	@Override
	public void queryNamesResponse(int requestId, Set<ObjectName> result) {
		/* No Op */

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.mbeanserver.AsynchJMXResponseListener#removeNotificationListenerResponse(int)
	 */
	@Override
	public void removeNotificationListenerResponse(int requestId) {
		/* No Op */

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.mbeanserver.AsynchJMXResponseListener#unregisterMBeanResponse(int)
	 */
	@Override
	public void unregisterMBeanResponse(int requestId) {
		/* No Op */

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.mbeanserver.AsynchJMXResponseListener#onException(int, java.lang.Throwable)
	 */
	@Override
	public void onException(int requestId, Throwable t) {
		/* No Op */

	}

}
