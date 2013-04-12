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

import java.util.concurrent.Executor;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.util.Assert;

/**
 * <p>Title: HotDeployedApplicationContext</p>
 * <p>Description: App Context with hot deploy specific overrides and behaviour.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.deployer.HotDeployedApplicationContext</code></p>
 */

public class HotDeployedApplicationContext extends GenericXmlApplicationContext {
	/** The context's multicaster */
	protected final SimpleApplicationEventMulticaster eventMulticaster;
	/** Instance logger */
	protected final Logger log;
	
	
	HotDeployedApplicationContext(Executor eventExecutor, String id) {
		super();		
		setId(id);
		log = Logger.getLogger(getClass().getName() + "." + id);
		eventMulticaster = new SimpleApplicationEventMulticaster(this);
		eventMulticaster.setTaskExecutor(eventExecutor);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.springframework.context.support.AbstractApplicationContext#addApplicationListener(org.springframework.context.ApplicationListener)
	 */
	@Override
	public void addApplicationListener(ApplicationListener<?> listener) {
		eventMulticaster.addApplicationListener(listener);
	}


	/**
	 * <p>Overriden to not publish events to the parent application context.</p>
	 * {@inheritDoc}
	 * @see org.springframework.context.support.AbstractApplicationContext#publishEvent(org.springframework.context.ApplicationEvent)
	 */
	@Override
	public void publishEvent(ApplicationEvent event) {
		Assert.notNull(event, "Event must not be null");
		if (logger.isTraceEnabled()) {
			logger.trace("Publishing event in " + getDisplayName() + ": " + event);
		}		
		eventMulticaster.multicastEvent(event);
	}				

	
}
