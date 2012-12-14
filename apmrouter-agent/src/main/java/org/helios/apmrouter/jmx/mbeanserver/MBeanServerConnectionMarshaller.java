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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.MBeanServerConnection;

import org.helios.apmrouter.OpCode;
import org.helios.apmrouter.util.SimpleLogger;
import org.helios.apmrouter.util.TimeoutListener;
import org.helios.apmrouter.util.TimeoutQueueMap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelDownstreamHandler;

/**
 * <p>Title: MBeanServerConnectionMarshaller</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.jmx.mbeanserver.MBeanServerConnectionMarshaller</code></p>
 */

public class MBeanServerConnectionMarshaller  implements InvocationHandler {
	/** A map of byte op codes keyed by the method represented */
	protected static final Map<Method, Byte> methodToKey;
	/** A map of methods keyed by the byte op code */
	protected static final Map<Byte, Method> keyToMethod;
	/** A map of asynch response methods keyed by the byte op code */
	protected static final Map<Byte, Method> keyToAsynchMethod;
	
	/** The default request timeout in ms. */
	public static long DEFAULT_TIMEOUT = 2000;
	/** The timeout map for asynchronous invocations */
	protected static final TimeoutQueueMap<Integer, AsynchJMXResponse> timeoutMap = new TimeoutQueueMap<Integer, AsynchJMXResponse>(DEFAULT_TIMEOUT); 
	
	/** A request serial number that allows a response to be matched up with a request */
	protected static final AtomicInteger requestId = new AtomicInteger(0);
	
	/** The channel on which the marshalled op is sent */
	protected final Channel channel;
	/** The remote address where the channel should write to */
	protected final SocketAddress remoteAddress;
	/** The request timeout in ms. */
	protected final long timeout;
	/** The asynch request handler */
	protected final AsynchJMXResponse listener;
	
	/**
	 * Creates a new MBeanServerConnection
	 * @param channel The channel on which the marshalled op is sent
	 * @param remoteAddress The remote address where the channel should write to 
	 * @param timeout The request timeout in ms.
	 * @param listener An asynch request repsonse listener
	 * @return an MBeanServerConnection
	 */
	public static MBeanServerConnection getMBeanServerConnection(Channel channel, SocketAddress remoteAddress, long timeout, AsynchJMXResponse listener) {
		return (MBeanServerConnection)Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{MBeanServerConnection.class}, new MBeanServerConnectionMarshaller(channel, remoteAddress, timeout, listener));
	}
	
	/**
	 * Creates a new MBeanServerConnection with the default timeout
	 * @param channel The channel on which the marshalled op is sent
	 * @param remoteAddress The remote address where the channel should write to 
	 * @param listener An asynch request repsonse listener
	 * @return an MBeanServerConnection
	 */
	public static MBeanServerConnection getMBeanServerConnection(Channel channel, SocketAddress remoteAddress, AsynchJMXResponse listener) {
		return (MBeanServerConnection)Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{MBeanServerConnection.class}, new MBeanServerConnectionMarshaller(channel, remoteAddress, DEFAULT_TIMEOUT, listener));
	}
	
	/**
	 * Creates a new MBeanServerConnection with no asynch listener, meaning it will be a synchronous request
	 * @param channel The channel on which the marshalled op is sent
	 * @param remoteAddress The remote address where the channel should write to 
	 * @param timeout The request timeout in ms.
	 * @return an MBeanServerConnection
	 */
	public static MBeanServerConnection getMBeanServerConnection(Channel channel, SocketAddress remoteAddress, long timeout) {
		return (MBeanServerConnection)Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{MBeanServerConnection.class}, new MBeanServerConnectionMarshaller(channel, remoteAddress, timeout, null));
	}
	
	/**
	 * Creates a new MBeanServerConnection with the default timeout and no asynch listener, meaning it will be a synchronous request
	 * @param channel The channel on which the marshalled op is sent
	 * @param remoteAddress The remote address where the channel should write to 
	 * @return an MBeanServerConnection
	 */
	public static MBeanServerConnection getMBeanServerConnection(Channel channel, SocketAddress remoteAddress) {
		return (MBeanServerConnection)Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{MBeanServerConnection.class}, new MBeanServerConnectionMarshaller(channel, remoteAddress, DEFAULT_TIMEOUT, null));
	}
	
	/**
	 * Creates a new MBeanServerConnection that connects to the channel's established remote address
	 * @param channel The channel on which the marshalled op is sent
	 * @param timeout The request timeout in ms.
	 * @param listener An asynch request repsonse listener
	 * @return an MBeanServerConnection
	 */
	public static MBeanServerConnection getMBeanServerConnection(Channel channel, long timeout, AsynchJMXResponse listener) {
		return (MBeanServerConnection)Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{MBeanServerConnection.class}, new MBeanServerConnectionMarshaller(channel, channel.getRemoteAddress(), timeout, listener));
	}
	
	/**
	 * Creates a new MBeanServerConnection that connects to the channel's established remote address with the default timeout
	 * @param channel The channel on which the marshalled op is sent
	 * @param listener An asynch request repsonse listener
	 * @return an MBeanServerConnection
	 */
	public static MBeanServerConnection getMBeanServerConnection(Channel channel, AsynchJMXResponse listener) {
		return (MBeanServerConnection)Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{MBeanServerConnection.class}, new MBeanServerConnectionMarshaller(channel, channel.getRemoteAddress(), DEFAULT_TIMEOUT, listener));
	}
	
	/**
	 * Creates a new MBeanServerConnection that connects to the channel's established remote address with no asynch listener, meaning it will be a synchronous request
	 * @param channel The channel on which the marshalled op is sent
	 * @param timeout The request timeout in ms.
	 * @return an MBeanServerConnection
	 */
	public static MBeanServerConnection getMBeanServerConnection(Channel channel, long timeout) {
		return (MBeanServerConnection)Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{MBeanServerConnection.class}, new MBeanServerConnectionMarshaller(channel, channel.getRemoteAddress(), timeout, null));
	}
	
	/**
	 * Creates a new MBeanServerConnection that connects to the channel's established remote address with the default timeout and no asynch listener, meaning it will be a synchronous request
	 * @param channel The channel on which the marshalled op is sent
	 * @return an MBeanServerConnection
	 */
	public static MBeanServerConnection getMBeanServerConnection(Channel channel) {
		return (MBeanServerConnection)Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{MBeanServerConnection.class}, new MBeanServerConnectionMarshaller(channel, channel.getRemoteAddress(), DEFAULT_TIMEOUT, null));
	}
	
	
	static {
		try {
			Method[] methods = MBeanServerConnection.class.getDeclaredMethods();
			Map<String, Method> asynchMethods = new HashMap<String, Method>();
			for(Method asynchMethod: AsynchJMXResponse.class.getDeclaredMethods()) {
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
			
			//keyToAsynchMethod
			
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
	protected MBeanServerConnectionMarshaller(Channel channel, SocketAddress remoteAddress, long timeout, AsynchJMXResponse listener) {
		this.channel = channel;
		this.remoteAddress = remoteAddress;
		this.timeout = timeout;
		this.listener = listener;
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
	 */
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		byte[] sargs = getOutput(args);
		SimpleLogger.debug("MBeanServerConnection [", method.getName(), "] Payload Size [", sargs.length+6, "]");
		ChannelBuffer cb = ChannelBuffers.directBuffer(sargs.length+6);
		cb.writeByte(OpCode.JMX_REQUEST.op());
		final int reqId = requestId.incrementAndGet();
		final String reqHandler = "JMXRequest-" + reqId;
		cb.writeInt(reqId);
		cb.writeByte(methodToKey.get(method));
		cb.writeBytes(sargs);
		final AtomicReference<Object> response = new AtomicReference<Object>(null);
		final CountDownLatch latch;
		if(listener==null) {
			latch = new CountDownLatch(1);
		} else {
			latch = new CountDownLatch(0);
			timeoutMap.put(reqId, listener, timeout);
			timeoutMap.addListener(new TimeoutListener<Integer, AsynchJMXResponse>() {
				@Override
				public void onTimeout(Integer key, AsynchJMXResponse value) {
					timeoutMap.remove(key);
					listener.onTimeout(reqId, timeout);
				}
			});
		}
		channel.getPipeline().addFirst(reqHandler, new SimpleChannelDownstreamHandler(){
			@Override
			public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
				if(e instanceof MessageEvent) {
					MessageEvent me = (MessageEvent)e;
					Object message = me.getMessage();
					if(message instanceof ChannelBuffer) {
						ChannelBuffer cb = (ChannelBuffer)message;
						byte op = cb.getByte(0);
						if(OpCode.JMX_RESPONSE.op()==op) {
							if(reqId==cb.getInt(1)) {
								AsynchJMXResponse listener = timeoutMap.remove(reqId);
								cb.skipBytes(5);
								byte[] bytes = new byte[cb.readableBytes()];
								cb.readBytes(bytes);
								Object[] resp = getInput(bytes);
								if(listener!=null) {
									if(resp.length==1 && resp[0]!=null && resp[0] instanceof Throwable) {
										listener.onException(reqId, (Throwable)resp[0]);
									} else {
										// need to figure out what the callback is
									}
								} else {
									response.set(resp.length==1 ? resp[0] : null);
									latch.countDown();									
								}
								
							}
						}
					}
				}
				super.handleDownstream(ctx, e);
			}			
		});
		channel.write(cb);
		return null;
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
		Object[] args = new Object[bytes[0]];
		try {			
			ois = new ObjectInputStream(bais);
			ois.readByte();
			for(int i = 0; i < bytes[0]; i++) {
				if(ois.readByte()==0) {
					args[i] = null;
				} else {
					args[i] = ois.readObject();
				}
			}
			return args;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to decode MBeanServerConnection Invocation arguments", ex);
		} finally {
			try { bais.close(); } catch (Exception ex) {}
			try { ois.close(); } catch (Exception ex) {}
		}
		
	}
	
	/**
	 * Serializes an array of invocation arguments to a byte array
	 * @param args The arguments to marshall
	 * @return a byte array
	 */
	public static  byte[] getOutput(Object...args) {
		if(args.length==0) return new byte[]{0};
		ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
		ObjectOutputStream oos = null;
		try {			
			oos = new ObjectOutputStream(baos);
			oos.writeByte(args.length);
			for(Object obj:args) {
				if(obj==null) {
					oos.writeByte(0);
				} else {
					oos.writeByte(1);
					oos.writeObject(obj);
				}
				
			}
			oos.flush();
			baos.flush();
			return baos.toByteArray();
		} catch (Exception ex) {
			throw new RuntimeException("Failed to encode MBeanServerConnection Invocation arguments " + Arrays.toString(args), ex);
		} finally {
			try { baos.close(); } catch (Exception ex) {}
			try { oos.close(); } catch (Exception ex) {}
		}
	}

}
