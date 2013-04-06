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
package org.helios.apmrouter.nash.handler;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.helios.apmrouter.nash.NashRequest;
import org.helios.apmrouter.nash.handler.NashRequestHandler;
import org.helios.apmrouter.nash.handler.RequestHandlerRegistry;
import org.jboss.netty.logging.InternalLogger;
import org.jboss.netty.logging.InternalLoggerFactory;

/**
 * <p>Title: RequestHandlerRegistry</p>
 * <p>Description: Singleton service to register request handlers with and to provide request handler lookups and dispatch.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.nash.handler.RequestHandlerRegistry</code></p>
 */

public class RequestHandlerRegistry {
	/** The internal logger */
	private final InternalLogger log = InternalLoggerFactory.getInstance(getClass());	
	/** A map of request handlers keyed by command name */
	private final Map<String, NashRequestHandler> requestHandlers = new ConcurrentHashMap<String, NashRequestHandler>();
	/** The singleton instance */
	private static volatile RequestHandlerRegistry instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/**
	 * Registers a request handler under one or more command name keys
	 * @param requestHandler The request handler to register
	 * @param commandNames The command names to register the handler under
	 */
	public void register(NashRequestHandler requestHandler, String... commandNames) {
		if(requestHandler==null) throw new IllegalArgumentException("The passed request handler was null", new Throwable());
		if(commandNames!=null && commandNames.length>0) {
			for(String commandName: commandNames) {
				if(commandName!=null && !commandName.trim().isEmpty()) {
					requestHandlers.put(commandName.trim(), requestHandler);
					log.debug("Added request handler [" + commandName + "]");
				}
			}
		}
		requestHandlers.put(requestHandler.getCommandName(), requestHandler);
		log.debug("Added request handler [" + requestHandler.getCommandName() + "]");
	}
	
	/**
	 * Removes all request handlers of the passed type
	 * @param clazz The type of handler to remove from the registry
	 */
	public void remove(Class<? extends NashRequestHandler> clazz) {
		if(clazz==null) throw new IllegalArgumentException("The passed request handler class was null", new Throwable());
		Set<String> removes = new HashSet<String>();
		for(Map.Entry<String, NashRequestHandler> entry: requestHandlers.entrySet()) {
			if(clazz.isAssignableFrom(entry.getValue().getClass())) {
				removes.add(entry.getKey());
			}
		}
		for(String key: removes) {
			requestHandlers.remove(key);
			log.debug("Removed request handler [" + key + "]");
		}
	}
	
	/**
	 * Removes request handlers keyed by the passed command names
	 * @param commandNames The command names to remove request handlers for
	 */
	public void remove(String...commandNames) {
		if(commandNames!=null && commandNames.length>0) {
			for(String commandName: commandNames) {
				if(commandName!=null && !commandName.trim().isEmpty()) {
					requestHandlers.remove(commandName.trim());
					log.debug("Removed request handler [" + commandName + "]");
				}				
			}
		}
	}
	
	/**
	 * Looks up a request handler by command name
	 * @param commandName The command name to get the request handler for
	 * @return The looked up request handler or null if one was not found
	 */
	public NashRequestHandler lookup(String commandName) {
		if(commandName==null || commandName.trim().isEmpty()) throw new IllegalArgumentException("Passed command name was empty or null", new Throwable());
		NashRequestHandler handler = requestHandlers.get(commandName.trim());
		if(handler==null) log.debug("Failed to locate request handler for command [" + commandName + "]");
		else log.debug("Looked up request handler [" + handler.getClass().getSimpleName() + "] for command [" + commandName + "]");
		return handler;
	}
	
	/**
	 * Looks up a request handler by request
	 * @param request The nailgun request to get a request handler for
	 * @return The looked up request handler or null if one was not found
	 */
	public NashRequestHandler lookup(NashRequest request) {
		if(request==null) throw new IllegalArgumentException("Passed nailgun request was null", new Throwable());
		return lookup(request.getCommand());
	}
	
	
	/**
	 * Acquires the RequestHandlerRegistry singleton
	 * @return the RequestHandlerRegistry singleton
	 */
	public static RequestHandlerRegistry getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new RequestHandlerRegistry();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Creates a new RequestHandlerRegistry
	 */
	private RequestHandlerRegistry() {
		log.debug("Created RequestHandlerRegistry");
	}
	
}
