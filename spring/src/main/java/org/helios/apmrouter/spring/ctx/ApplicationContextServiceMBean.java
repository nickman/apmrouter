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
package org.helios.apmrouter.spring.ctx;

import javax.management.ObjectName;

import org.helios.apmrouter.APMRouter;
import org.helios.apmrouter.jmx.JMXHelper;
import org.springframework.context.ApplicationContext;

/**
 * <p>Title: ApplicationContextServiceMBean</p>
 * <p>Description: JMX MBean interface for {@link ApplicationContextService}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.spring.ctx.ApplicationContextServiceMBean</code></p>
 */

public interface ApplicationContextServiceMBean extends ApplicationContext {
	/** The AppCtx ObjectName prefix */
	public static final String OBJECT_NAME_PREF = "org.helios.apmrouter.spring:service=AppCtx,name=";

	/** The JMX ObjectName of the APMRouter root app context */
	public static final ObjectName ROOT_APP_CTX_ON = JMXHelper.objectName(OBJECT_NAME_PREF + APMRouter.ROOT_DISPLAY_NAME);
	
	
}
