/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2013, Helios Development Group and individual contributors
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
package org.helios.apmrouter.server;

import javax.management.MBeanException;
import javax.management.RuntimeOperationsException;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.RequiredModelMBean;

/**
 * <p>Title: MetricsAwareRequiredModelMBean</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.MetricsAwareRequiredModelMBean</code></p>
 */

public class MetricsAwareRequiredModelMBean extends RequiredModelMBean {

	/**
	 * Creates a new MetricsAwareRequiredModelMBean
	 * @throws MBeanException
	 * @throws RuntimeOperationsException
	 */
	public MetricsAwareRequiredModelMBean() throws MBeanException,
			RuntimeOperationsException {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new MetricsAwareRequiredModelMBean
	 * @param mbi
	 * @throws MBeanException
	 * @throws RuntimeOperationsException
	 */
	public MetricsAwareRequiredModelMBean(ModelMBeanInfo mbi)
			throws MBeanException, RuntimeOperationsException {
		super(mbi);
		// TODO Auto-generated constructor stub
	}

}
