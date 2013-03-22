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
package org.helios.apmrouter.dataservice.json;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.json.JSONObject;

/**
 * <p>Title: JSONRequestHandlerImpl</p>
 * <p>Description: Invocable implementation wrapper around a {@link JSONRequestHandler} annotated method.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.dataservice.json.JSONRequestHandlerImpl</code></p>
 */

public class JSONRequestHandlerImpl implements JSONDataService {
	/** Static class logger */
	protected static final Logger LOG = Logger.getLogger(JSONRequestHandlerImpl.class);
	/** The handler implementation instance */
	protected final Object impl;
	/** The invocation method handle */
	protected final MethodHandle methodHandle;
	/** The handler name */
	protected final String name;
	/** The service name */
	protected final String serviceName;
	
	/**
	 * Creates a new JSONRequestHandlerImpl
	 * @param impl The handler implementation instance
	 * @param methodHandle The invocation method handle
	 * @param name The handler name
	 * @param serviceName The service name
	 */
	public JSONRequestHandlerImpl(Object impl, MethodHandle methodHandle, String name, String serviceName) {
		this.impl = impl;
		this.methodHandle = methodHandle;
		this.name = name;
		this.serviceName = serviceName;
	}

	/**
	 * {@inheritDoc}
	 *
	 */
	@Override
	public void processRequest(JsonRequest request, Channel channel) {
		try {
			methodHandle.invoke(impl, request, channel);
		} catch (Throwable e) {
			LOG.error("Failed to invoke handler [" + name + "]", e);
			channel.write(renderInvocationError(e));
		}		
	}
	
	/**
	 * Renders a JSONObject from the passed throwable
	 * @param t The throwable to render
	 * @return the rendered JSONObject
	 */
	protected JSONObject renderInvocationError(Throwable t) {
		try {
			JSONObject err = new JSONObject();
			err.putOnce(JSONRequestRouter.REQUEST_FLAG, "err");
			err.putOnce(JSONRequestRouter.SERVICE_NAME, serviceName);
			err.putOnce(JSONRequestRouter.OP_NAME, name);
			err.putOnce(JSONRequestRouter.ERR_NAME, t.getMessage());
			return err;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to build error response", ex);
		}
	}

	/**
	 * Generates a map of handlers keyed by the handler name
	 * @param serviceName The service name these handlers are being atached to
	 * @param serviceImpl The service implementation
	 * @return A map of handlers keyed by the handler name
	 */
	public static Map<String, JSONRequestHandlerImpl> generateHandlers(String serviceName, Object serviceImpl) {
		Map<String, JSONRequestHandlerImpl> map = new ConcurrentHashMap<String, JSONRequestHandlerImpl>();
		for(Method m: serviceImpl.getClass().getMethods()) {
			JSONRequestHandler rh = m.getAnnotation(JSONRequestHandler.class);
			if(rh!=null) {
				String name = rh.name();
				try {
					MethodType desc = MethodType.methodType(void.class, new Class[]{JsonRequest.class, Channel.class});
					MethodHandle mh = MethodHandles.lookup().findVirtual(serviceImpl.getClass(), m.getName(), desc);					
					map.put(name, new JSONRequestHandlerImpl(serviceImpl, mh, name, serviceName));
				} catch (Exception e) {
				}
			}
		}
		for(Method m: serviceImpl.getClass().getDeclaredMethods()) {
			JSONRequestHandler rh = m.getAnnotation(JSONRequestHandler.class);
			if(rh!=null) {
				String name = rh.name();
				try {
					MethodType desc = MethodType.methodType(void.class, new Class[]{JsonRequest.class, Channel.class});
					MethodHandle mh = MethodHandles.lookup().findVirtual(serviceImpl.getClass(), m.getName(), desc);					
					map.put(name, new JSONRequestHandlerImpl(serviceImpl, mh, name, serviceName));
				} catch (Exception e) {
				}
			}
		}		
		return map;
	}
	
}
