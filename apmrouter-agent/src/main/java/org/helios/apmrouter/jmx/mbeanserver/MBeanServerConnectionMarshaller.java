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
package org.helios.apmrouter.jmx.mbeanserver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.management.MBeanServerConnection;

import org.helios.apmrouter.OpCode;
import org.helios.apmrouter.util.SimpleLogger;
import org.helios.apmrouter.util.TimeoutListener;
import org.helios.apmrouter.util.TimeoutQueueMap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLogLevel;

/**
 * <p>Title: MBeanServerConnectionMarshaller</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.jmx.mbeanserver.MBeanServerConnectionMarshaller</code></p>
 */

public class MBeanServerConnectionMarshaller  implements InvocationHandler, MBeanServerConnectionAdmin {
	/** A map of byte op codes keyed by the method represented */
	protected static final Map<Method, Byte> methodToKey;
	/** A map of methods keyed by the byte op code */
	protected static final Map<Byte, Method> keyToMethod;
	/** A map of asynch response methods keyed by the byte op code */
	protected static final Map<Byte, Method> keyToAsynchMethod;
	
	/** The default request timeout in ms. */
	public static long DEFAULT_TIMEOUT = 2000;
	/** The timeout map for asynchronous invocations */
	protected static final TimeoutQueueMap<Integer, AsynchJMXResponseListener> asynchTimeoutMap = new TimeoutQueueMap<Integer, AsynchJMXResponseListener>(DEFAULT_TIMEOUT); 
	/** The timeout map for synchronous invocations */
	protected static final TimeoutQueueMap<Integer, CountDownLatch> synchTimeoutMap = new TimeoutQueueMap<Integer, CountDownLatch>(DEFAULT_TIMEOUT); 
	/** The timeout map for synchronous invocation results */
	protected static final TimeoutQueueMap<Integer, Object> synchResultMap = new TimeoutQueueMap<Integer, Object>(DEFAULT_TIMEOUT); 
	
	/** A request serial number that allows a response to be matched up with a request */
	protected static final AtomicInteger requestId = new AtomicInteger(0);
	
	/** The channel on which the marshalled op is sent */
	protected final Channel channel;
	/** The remote address where the channel should write to */
	protected final SocketAddress remoteAddress;
	/** The request timeout in ms. */
	protected final long timeout;
	/** The asynch request handler */
	protected final AsynchJMXResponseListener listener;
	/** The channel handler to capture and process JMX responses */
	protected final MBeanServerConnectionInvocationResponseHandler responseHandler = new MBeanServerConnectionInvocationResponseHandler(this);
	
	/**
	 * Creates a new MBeanServerConnection
	 * @param channel The channel on which the marshalled op is sent
	 * @param remoteAddress The remote address where the channel should write to 
	 * @param timeout The request timeout in ms.
	 * @param listener An asynch request repsonse listener
	 * @return an MBeanServerConnection
	 */
	public static MBeanServerConnection getMBeanServerConnection(Channel channel, SocketAddress remoteAddress, long timeout, AsynchJMXResponseListener listener) {
		return (MBeanServerConnection)Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{MBeanServerConnection.class}, new MBeanServerConnectionMarshaller(channel, remoteAddress, timeout, listener));
	}
	
	/**
	 * Creates a new MBeanServerConnection with the default timeout
	 * @param channel The channel on which the marshalled op is sent
	 * @param remoteAddress The remote address where the channel should write to 
	 * @param listener An asynch request repsonse listener
	 * @return an MBeanServerConnection
	 */
	public static MBeanServerConnection getMBeanServerConnection(Channel channel, SocketAddress remoteAddress, AsynchJMXResponseListener listener) {
		return getMBeanServerConnection(channel, remoteAddress, DEFAULT_TIMEOUT, listener);
	}
	
	/**
	 * Creates a new MBeanServerConnection with no asynch listener, meaning it will be a synchronous request
	 * @param channel The channel on which the marshalled op is sent
	 * @param remoteAddress The remote address where the channel should write to 
	 * @param timeout The request timeout in ms.
	 * @return an MBeanServerConnection
	 */
	public static MBeanServerConnection getMBeanServerConnection(Channel channel, SocketAddress remoteAddress, long timeout) {
		return getMBeanServerConnection(channel, remoteAddress, timeout, null);
	}
	
	/**
	 * Creates a new MBeanServerConnection with the default timeout and no asynch listener, meaning it will be a synchronous request
	 * @param channel The channel on which the marshalled op is sent
	 * @param remoteAddress The remote address where the channel should write to 
	 * @return an MBeanServerConnection
	 */
	public static MBeanServerConnection getMBeanServerConnection(Channel channel, SocketAddress remoteAddress) {
		return getMBeanServerConnection(channel, remoteAddress, DEFAULT_TIMEOUT, null);
	}
	
	/**
	 * Creates a new MBeanServerConnection that connects to the channel's established remote address
	 * @param channel The channel on which the marshalled op is sent
	 * @param timeout The request timeout in ms.
	 * @param listener An asynch request repsonse listener
	 * @return an MBeanServerConnection
	 */
	public static MBeanServerConnection getMBeanServerConnection(Channel channel, long timeout, AsynchJMXResponseListener listener) {
		return getMBeanServerConnection(channel, channel.getRemoteAddress(), timeout, listener);
	}
	
	/**
	 * Creates a new MBeanServerConnection that connects to the channel's established remote address with the default timeout
	 * @param channel The channel on which the marshalled op is sent
	 * @param listener An asynch request repsonse listener
	 * @return an MBeanServerConnection
	 */
	public static MBeanServerConnection getMBeanServerConnection(Channel channel, AsynchJMXResponseListener listener) {
		return getMBeanServerConnection(channel, channel.getRemoteAddress(), DEFAULT_TIMEOUT, null);
	}
	
	/**
	 * Creates a new MBeanServerConnection that connects to the channel's established remote address with no asynch listener, meaning it will be a synchronous request
	 * @param channel The channel on which the marshalled op is sent
	 * @param timeout The request timeout in ms.
	 * @return an MBeanServerConnection
	 */
	public static MBeanServerConnection getMBeanServerConnection(Channel channel, long timeout) {
		return getMBeanServerConnection(channel, channel.getRemoteAddress(), timeout, null);
	}
	
	/**
	 * Creates a new MBeanServerConnection that connects to the channel's established remote address with the default timeout and no asynch listener, meaning it will be a synchronous request
	 * @param channel The channel on which the marshalled op is sent
	 * @return an MBeanServerConnection
	 */
	public static MBeanServerConnection getMBeanServerConnection(Channel channel) {
		return getMBeanServerConnection(channel, channel.getRemoteAddress(), DEFAULT_TIMEOUT, null);		
	}
	
	/**
	 * Quickie init test of the static initializer and print out of asynch response mappings
	 * @param args none
	 */
	public static void main(String[] args) {
		SimpleLogger.info("Initialized");
		for(Map.Entry<Byte, Method> entry: keyToMethod.entrySet()) {
			Method am = keyToAsynchMethod.get(entry.getKey());
			SimpleLogger.info("\t", entry.getValue().getName(), " --> ", am.getName());
		}
		StringBuilder b = new StringBuilder("Mappings");
		for(Map.Entry<Byte, Method> entry: keyToMethod.entrySet()) {
			b.append("\n\t[").append(entry.getKey()).append("]").append("   --").append(entry.getValue().getName()).append("--   ").append("[").append(methodToKey.get(entry.getValue())).append("]");
		}
		SimpleLogger.info(b);
	}
	
	static {
		try {
			Map<String, Method> orderedMethods = new TreeMap<String, Method>();
			for(Method m: MBeanServerConnection.class.getDeclaredMethods()) {
				orderedMethods.put(m.toGenericString(), m);
			}
			Method[] methods = orderedMethods.values().toArray(new Method[orderedMethods.size()]);
			Map<String, Method> asynchMethods = new TreeMap<String, Method>();
			for(Method asynchMethod: AsynchJMXResponseListener.class.getDeclaredMethods()) {
				asynchMethods.put(asynchMethod.getName(), asynchMethod);
			}
			
			Map<Method, Byte> m2k = new HashMap<Method, Byte>(methods.length);
			Map<Byte, Method> k2m = new HashMap<Byte, Method>(methods.length);
			Map<Byte, Method> k2am = new HashMap<Byte, Method>(methods.length);
			for(int i = 0; i < methods.length; i++) {
				m2k.put(methods[i], (byte)i);
				k2m.put((byte)i, methods[i]);
				Method asynchMethod = asynchMethods.get(methods[i].getName() + "Response");
				if(asynchMethod==null) throw new RuntimeException("Failed to find asynch handler for [" + methods[i].toGenericString() + "]", new Throwable());
				k2am.put((byte)i, asynchMethod);
			}
			methodToKey = Collections.unmodifiableMap(m2k);
			keyToMethod = Collections.unmodifiableMap(k2m);
			keyToAsynchMethod = Collections.unmodifiableMap(k2am);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Creates a new MBeanServerConnectionMarshaller
	 * @param channel The channel on which the marshalled op is sent
	 * @param remoteAddress The remote address where the channel should write to
	 * @param timeout The request timeout in ms.
	 * @param listener An asynch request repsonse listener
	 */
	protected MBeanServerConnectionMarshaller(Channel channel, SocketAddress remoteAddress, long timeout, AsynchJMXResponseListener listener) {
		this.channel = channel;
		this.remoteAddress = remoteAddress;
		this.timeout = timeout;
		this.listener = listener;
		if(channel.getPipeline().get(getClass().getSimpleName())==null) {
			this.channel.getPipeline().addFirst(getClass().getSimpleName(), responseHandler);
			LoggingHandler logg = new LoggingHandler(InternalLogLevel.ERROR, true);
			this.channel.getPipeline().addFirst("logger", logg);
			
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.mbeanserver.MBeanServerConnectionAdmin#closeMBeanServerConnection()
	 */
	@Override
	public void closeMBeanServerConnection() {
		channel.getPipeline().remove(getClass().getSimpleName());
		if(listener!=null) {
			listener.onClose();
		}
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.mbeanserver.MBeanServerConnectionAdmin#waitForSynchronousResponse(int, long)
	 */
	@Override
	public void waitForSynchronousResponse(int requestId, long timeout) {
		CountDownLatch latch = new CountDownLatch(1);
		synchTimeoutMap.put(requestId, latch);
		try {
			latch.await(timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException iex) {			
			synchResultMap.putIfAbsent(requestId, new IOException("Thread was interrupted while waiting on Operation completion", new Throwable()));
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.mbeanserver.MBeanServerConnectionAdmin#onSynchronousResponse(int, java.lang.Object)
	 */
	@Override
	public void onSynchronousResponse(int requestId, Object value) {
		synchResultMap.putIfAbsent(requestId, value);
		CountDownLatch latch = synchTimeoutMap.get(requestId);
		if(latch!=null) latch.countDown();
		
	}
	
	
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
	 */
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if(channel.getPipeline().get(getClass().getSimpleName())==null) {
			throw new IOException("This MBeanServerConnection has been closed", new Throwable());
		}
		byte[] sargs = getOutput(args);
		SimpleLogger.debug("MBeanServerConnection [", method.getName(), "] Payload Size [", sargs.length+6+4, "]");
		ChannelBuffer cb = ChannelBuffers.directBuffer(sargs.length+6+4);
		final int reqId = requestId.incrementAndGet();
		cb.writeByte(OpCode.JMX_REQUEST.op());
		cb.writeInt(reqId);
		cb.writeByte(methodToKey.get(method));
		cb.writeInt(sargs.length);
		cb.writeBytes(sargs);		
		
		if(listener==null) {
			synchTimeoutMap.addListener(new TimeoutListener<Integer, CountDownLatch>() {
				@Override
				public void onTimeout(Integer key, CountDownLatch value) {
					if(reqId == key) { 
						synchTimeoutMap.remove(key);
						synchTimeoutMap.removeListener(this);
						onSynchronousResponse(reqId, new IOException("Operation timed out after [" + timeout + "] ms.", new Throwable()));
					}
				}
			});
		} else {			
			asynchTimeoutMap.put(reqId, listener, timeout);
			asynchTimeoutMap.addListener(new TimeoutListener<Integer, AsynchJMXResponseListener>() {
				@Override
				public void onTimeout(Integer key, AsynchJMXResponseListener value) {
					if(reqId == key) {
						asynchTimeoutMap.remove(key);
						listener.onTimeout(reqId, timeout);
						asynchTimeoutMap.removeListener(this);
					}
				}
			});
		}
		
		channel.write(cb, remoteAddress).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if(future.isSuccess()) {
					SimpleLogger.info("Sent JMX Request to [", remoteAddress, "]");
				} else {
					SimpleLogger.error("Failed to send JMX Request to [", remoteAddress, "]", future.getCause());
				}
			}
		});
		if(listener==null) {
			waitForSynchronousResponse(reqId, timeout);
			Object result =  synchResultMap.get(reqId);
			if(result!=null && result instanceof Throwable) {
				throw (Throwable)result;
			}
			return result;
		}
		return null;
	}
	
	
	/**
	 * Handles a received {@link MBeanServerConnection} invocation
	 * @param channel The channel the request was received on
	 * @param remoteAddress The remote address of the caller
	 * @param buffer THe buffer received
	 * @param server The {@link MBeanServerConnection} to invoke against
	 */
	public static void handleJMXRequest(Channel channel, SocketAddress remoteAddress, ChannelBuffer buffer, MBeanServerConnection server) {
		buffer.resetReaderIndex();
		/* The request write */
//		cb.writeByte(OpCode.JMX_REQUEST.op());
//		cb.writeInt(reqId);
//		cb.writeByte(methodToKey.get(method));
//		cb.writeInt(sargs.length);
//		cb.writeBytes(sargs);		
		buffer.skipBytes(1);
		int reqId = buffer.readInt();
		byte methodId = buffer.readByte();
		int payloadSize = buffer.readInt();
		byte[] payload = new byte[payloadSize];
		buffer.readBytes(payload);
		Object[] params = getInput(payload);		
		Object result = null;
		Method targetMethod = null;
		try {
			targetMethod = keyToMethod.get(methodId);
			if(targetMethod==null) {
				result = new Exception("Failed to handle MBeanServerConnection invocation because method Op Code [" + methodId + "] was not recognized");
			} else {
				result = targetMethod.invoke(server, params);
			}
		} catch (Throwable t) {
			SimpleLogger.warn("Failed to invoke [", targetMethod, "]", t);
			result = t;
		}
		writeJMXResponse(reqId, methodId, channel, remoteAddress, result);
		
	}
	
	/**
	 * Writes a JMX invocation response back to the caller
	 * @param requestId The original request id
	 * @param methodId The {@link MBeanServerConnection} method ID byte
	 * @param channel The channel to write to
	 * @param remoteAddress The remote address that the channel will write to
	 * @param response AN array of object responses
	 */
	public static void writeJMXResponse(int requestId, byte methodId, Channel channel, SocketAddress remoteAddress, Object...response) {
		byte[] payload = getOutput(response);
		int size = payload.length + 1 + 4 + 1 + 4;  // size is <payload size> + <OpCode> + <requestId> + <method ID byte> + <payload length>
		ChannelBuffer cb = ChannelBuffers.directBuffer(size);
		cb.writeByte(OpCode.JMX_RESPONSE.op());
		cb.writeInt(requestId);
		cb.writeByte(methodId);
		cb.writeInt(payload.length);
		cb.writeBytes(payload);
		channel.write(cb, remoteAddress);
		SimpleLogger.info("Wrote JMX Response [", size, "] bytes");
	}
	
	/**
	 * Deserializes an array of arguments from a byte array
	 * @param bytes The byte array containing the serialized arguments
	 * @return an object array
	 */
	public static Object[] getInput(byte[] bytes) {
		if(bytes.length==0 || (bytes.length==1 && bytes[0]==0)) return new Object[0];
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes); 
		ObjectInputStream ois = null;
		GZIPInputStream gzis = null;
		Object[] args = null;
		try {			
			gzis = new GZIPInputStream(bais); 
			ois = new ObjectInputStream(gzis);
			Object obj = ois.readObject();
			if(obj.getClass().isArray()) {
				args = new Object[Array.getLength(obj)];
				System.arraycopy(obj, 0, args, 0, args.length);
			} else {
				args = new Object[]{obj};
			}
			return args;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to decode MBeanServerConnection Invocation arguments", ex);
		} finally {
			try { bais.close(); } catch (Exception ex) {/* No Op */}
			if(ois!=null) try { ois.close(); } catch (Exception ex) {/* No Op */}
			if(gzis!=null) try { gzis.close(); } catch (Exception ex) {/* No Op */}
		}
		
	}
	
	/**
	 * Serializes an array of invocation arguments to a byte array
	 * @param args The arguments to marshall
	 * @return a byte array
	 */
	public static  byte[] getOutput(Object...args) {
		if(args==null || args.length==0) return new byte[]{};
		ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
		GZIPOutputStream gzos = null;
		ObjectOutputStream oos = null;
		try {			
			gzos= new GZIPOutputStream(baos);
			oos = new ObjectOutputStream(gzos);
			oos.writeObject(args);
			oos.flush();
			gzos.finish();
			gzos.flush();
			baos.flush();
			return baos.toByteArray();
		} catch (Exception ex) {
			throw new RuntimeException("Failed to encode MBeanServerConnection Invocation arguments " + Arrays.toString(args), ex);
		} finally {
			try { baos.close(); } catch (Exception ex) {/* No Op */}
			if(oos!=null) try { oos.close(); } catch (Exception ex) {/* No Op */}
			if(gzos!=null) try { gzos.close(); } catch (Exception ex) {/* No Op */}
		}
	}
}
