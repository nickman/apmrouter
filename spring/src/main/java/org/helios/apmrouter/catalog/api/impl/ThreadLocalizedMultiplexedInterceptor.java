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
package org.helios.apmrouter.catalog.api.impl;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.hibernate.EmptyInterceptor;
import org.hibernate.Interceptor;

/**
 * <p>Title: ThreadLocalizedMultiplexedInterceptor</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.catalog.api.impl.ThreadLocalizedMultiplexedInterceptor</code></p>
 */

public class ThreadLocalizedMultiplexedInterceptor extends EmptyInterceptor {
	/**  */
	private static final long serialVersionUID = 978989473632853871L;
	/** A set of interceptors invoked for all calling threads */
	protected final Set<Interceptor> sharedInterceptors = Collections.synchronizedSet(new LinkedHashSet<Interceptor>());
	/** A set of interceptors registered by individual threads and scoped only to the registering thread */
	protected static final ThreadLocal<Set<Interceptor>> threadLocalInterceptors = new ThreadLocal<Set<Interceptor>>() {
		@Override
		protected Set<Interceptor> initialValue() {
			return new LinkedHashSet<Interceptor>(2);
		}
	};
	
	/** The singleton instance */
	private static volatile ThreadLocalizedMultiplexedInterceptor instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	
	/**
	 * Acquires a ThreadLocalizedMultiplexedInterceptor instance.
	 * If the shared version has been created, that will be returned.
	 * Otherwise, a new instance is created for each call to this method.
	 * @return a ThreadLocalizedMultiplexedInterceptor
	 */
	public static ThreadLocalizedMultiplexedInterceptor getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					return new ThreadLocalizedMultiplexedInterceptor();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Acquires the shared ThreadLocalizedMultiplexedInterceptor and initializes it if it has not been initialized already.
	 * @param interceptors A map of interceptors keyed by an integer representing the order of execution
	 * @return the shared ThreadLocalizedMultiplexedInterceptor
	 */
	public static ThreadLocalizedMultiplexedInterceptor getInstance(Map<Integer, Interceptor> interceptors) {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new ThreadLocalizedMultiplexedInterceptor(interceptors);
				}
			}
		}
		return instance;
	}
	
	/**
	 * Creates a new ThreadLocalizedMultiplexedInterceptor
	 */
	private ThreadLocalizedMultiplexedInterceptor() {
		
	}
	
	/**
	 * Creates a new ThreadLocalizedMultiplexedInterceptor
	 * @param interceptors The shared interceptor stack
	 */
	private ThreadLocalizedMultiplexedInterceptor(Map<Integer, Interceptor> interceptors) {
		if(interceptors!=null) {
			TreeMap<Integer, Interceptor> sortedMap = new TreeMap<Integer, Interceptor>(interceptors); 
			for(Interceptor interceptor : sortedMap.values()) {
				if(interceptor==null) continue;
				sharedInterceptors.add(interceptor);
			}
		}
	}

	
	/**
	 * Registers the passed interceptor for execution scoped to the calling thread
	 * @param interceptor the interceptor to register
	 */
	public void registerThreadLocalInterceptor(Interceptor interceptor) {
		if(interceptor!=null) {
			threadLocalInterceptors.get().add(interceptor);
		}
	}
	
	/**
	 * Unregisters the passed interceptor from the calling thread's scoped interceptor stack.
	 * @param interceptor the interceptor to remove.
	 */
	public void unregisterThreadLocalInterceptor(Interceptor interceptor) {
		if(interceptor!=null) {
			threadLocalInterceptors.get().remove(interceptor);
			if(threadLocalInterceptors.get().isEmpty()) {
				threadLocalInterceptors.remove();
			}
		}
	}
	

}
