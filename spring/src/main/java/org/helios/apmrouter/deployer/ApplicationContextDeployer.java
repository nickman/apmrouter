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
package org.helios.apmrouter.deployer;

import java.io.File;

import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.spring.ctx.ApplicationContextService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;

/**
 * <p>Title: ApplicationContextDeployer</p>
 * <p>Description: Manages the deployment/undeployment of hot deployed application contexts</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.deployer.ApplicationContextDeployer</code></p>
 */

public class ApplicationContextDeployer {
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	
	
	/**
	 * Hot deploys the application context defined in the passed file
	 * @param parent The parent context
	 * @param fe The file event referencing a new or modified file
	 * @return the deployed application context
	 */
	protected GenericApplicationContext deploy(ApplicationContext parent, FileEvent fe) {
		try {
			log.info("Deploying AppCtx [" + fe.getFileName() + "]");
			File f = new File(fe.getFileName());
			if(!f.canRead()) throw new Exception("Cannot read file [" + fe + "]", new Throwable());
			GenericXmlApplicationContext appCtx = new GenericXmlApplicationContext();
			appCtx.setDisplayName(f.getAbsolutePath());
			appCtx.setParent(parent);
			appCtx.load(f.getAbsolutePath());
			ObjectName on = JMXHelper.objectName(ApplicationContextService.HOT_OBJECT_NAME_PREF + ObjectName.quote(f.getAbsolutePath()));
			ApplicationContextService.register(on, appCtx);
			return appCtx;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to deploy application context [" + fe + "]", ex);
		}
	}
}
