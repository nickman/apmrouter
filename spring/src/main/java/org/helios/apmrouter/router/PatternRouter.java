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
package org.helios.apmrouter.router;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * <p>Title: PatternRouter</p>
 * <p>Description: A pettern routing engine that distributes {@link Routable} instances to subscribers that advertise a pattern that matches the routing key supplied by the routables</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.router.PatternRouter</code></p>
 * @param <T> The expected type of the routable
 */

public class PatternRouter<T extends Routable> {
	/** The worker thread pool that reads the routing queue */
	protected final ThreadPoolExecutor threadPool;
	
	/**
	 * Creates a new PatternRouter
	 * @param threadPool The worker thread pool that reads the routing queue
	 */
	private PatternRouter(BlockingQueue<Runnable> routingQueue, ThreadPoolExecutor threadPool) {		
		this.threadPool = threadPool;
	}
	
	/**
	 * Routes an array of routables to their pattern matched endpoints
	 * @param routables The routables to route
	 */
	public void route(T...routables) {
		
	}
	
	/**
	 * Routes a collection of routables to their pattern matched endpoints
	 * @param routables The routables to route
	 */
	public void route(Collection<T> routables) {
		
	}
	
	
	
}
