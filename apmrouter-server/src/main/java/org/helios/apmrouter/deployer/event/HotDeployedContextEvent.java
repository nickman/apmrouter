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
package org.helios.apmrouter.deployer.event;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;

/**
 * <p>Title: HotDeployedContextEvent</p>
 * <p>Description: Parent class for hot deployed app context events</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.deployer.event.HotDeployedContextEvent</code></p>
 */

public class HotDeployedContextEvent extends ApplicationEvent {
	/**  */
	private static final long serialVersionUID = 6549810946739748142L;
	/** The hot deployed app context's id */
	protected final String appCtxId;
	/** The hot deployed app context */
	protected final ApplicationContext appCtx;
	
	/**
	 * Creates a new HotDeployedContextEvent
	 * @param appCtx the hot deployed application context
	 */
	public HotDeployedContextEvent(ApplicationContext appCtx) {
		super(appCtx);
		appCtxId = appCtx.getId();
		this.appCtx = appCtx;
	}
	/**
	 * Returns the hot deployed app context's id
	 * @return the hot deployed app context's id
	 */
	public String getAppCtxId() {
		return appCtxId;
	}
	/**
	 * Returns the hot deployed app context
	 * @return the hot deployed app context
	 */
	public ApplicationContext getAppCtx() {
		return appCtx;
	}

}
