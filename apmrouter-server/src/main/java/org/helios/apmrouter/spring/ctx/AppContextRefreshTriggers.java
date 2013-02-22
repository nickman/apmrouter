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
package org.helios.apmrouter.spring.ctx;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * <p>Title: AppContextRefreshTriggers</p>
 * <p>Description: A class intended to collect all bean instances in an app context that are qualified with <b><code>PreRefreshTrigger</code></b></p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.spring.ctx.AppContextRefreshTriggers</code></p>
 */

public class AppContextRefreshTriggers {
	/** App logger */
	protected final Logger log = Logger.getLogger(getClass());
	/** The set of qualified beans */
	protected final Set<Object> refreshTriggers = new HashSet<Object>();
	
	/**
	 * Returns the qualified beans
	 * @return the qualified beans
	 */
	public Set<Object> getRefreshTriggers() {
		return refreshTriggers;
	}
	/**
	 * Injection point for the qualified beans
	 * @param refreshTriggers
	 */
	@Autowired(required=false)
	@Qualifier("PreRefreshTrigger")
	public void setRefreshTriggers(Set<Object> refreshTriggers) {
		log.info("Injecting refresh triggers");
		if(refreshTriggers!=null) {
			this.refreshTriggers.addAll(refreshTriggers);
		}
		log.info("Injected [" + this.refreshTriggers.size() + "] refresh triggers");
	}

}
