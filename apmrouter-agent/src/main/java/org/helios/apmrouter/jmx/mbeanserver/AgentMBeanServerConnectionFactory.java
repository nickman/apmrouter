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
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import org.helios.apmrouter.OpCode;
import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.jmx.connector.protocol.mxl.MXLocalJMXConnector;
import org.helios.apmrouter.jmx.mbeanserver.proxy.MBeanServerConnectionProxy;
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
 * <p>Title: AgentMBeanServerConnectionFactory</p>
 * <p>Description: A utility class and factory to create {@link MBeanServerConnection}s to supporting agents using the custom APMRouter agent protocol</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.jmx.mbeanserver.AgentMBeanServerConnectionFactory</code></p>
 */

public class AgentMBeanServerConnectionFactory  implements InvocationHandler, MBeanServerConnectionAdmin {
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
	
	/** A map of created MBeanServerConnection instances keyed by the builder key */
	protected static final Map<String, MBeanServerConnection> INSTANCES = new ConcurrentHashMap<String, MBeanServerConnection>();
	
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
	/** The jmx domain of the target MBeanServer */
	protected final String domain;
	/** The byte encoded domain name */
	protected final byte[] domainInfoData;
	/** The channel handler to capture and process JMX responses */
	protected final MBeanServerConnectionInvocationResponseHandler responseHandler = new MBeanServerConnectionInvocationResponseHandler(this);
	/** A map of listener registration request Ids keyed by the registered notification that was registered */
	protected final Map<Integer, NotificationListener> registeredListeners = new ConcurrentHashMap<Integer, NotificationListener>();
	
	/**
	 * Creates a new MBeanServerConnection builder
	 * @param channel The channel that JMX ops are issued through
	 * @return an MBeanServerConnection builder
	 */
	public static Builder builder(Channel channel) {
		return new Builder(channel);
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
	 * <p>Title: Builder</p>
	 * <p>Description: A fluent style builder for channel based MBeanServerConnections.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.jmx.mbeanserver.AgentMBeanServerConnectionFactory.Builder</code></p>
	 */
	public static class Builder {
		/** The channel on which the marshalled op is sent */
		private final Channel channel;
		/** The remote address where the channel should write to */
		private SocketAddress remoteAddress = null;
		/** The request timeout in ms. */
		private long timeout = DEFAULT_TIMEOUT;
		/** The asynch request handler */
		private AsynchJMXResponseListener listener = null;
		/** The name of the default JMX domain for the target MBeanServer */
		private String domain = "DefaultDomain";
		
		/**
		 * Builds the key that uniquely identifies the MBeanServerConnection that would be created by this builder
		 * @return the unique MBeanServerConnection key
		 */
		private String buildKey() {
			StringBuilder b = new StringBuilder();
			b.append(channel.getId());
			b.append(domain);
			if(remoteAddress!=null) {
				b.append(remoteAddress.toString());
			}
			return b.toString();
		}
		
		/**
		 * Creates a new Builder
		 * @param channel The connector channel
		 */
		private Builder(Channel channel) {
			this.channel = channel;
		}
		
		/**
		 * Builds a new AgentMBeanServerConnectionFactory and returns the underlying MBeanServerConnection 
		 * @return a new MBeanServerConnection
		 */
		public MBeanServerConnection build() {
			final String key = buildKey();
			MBeanServerConnection conn = INSTANCES.get(key);
			if(conn==null) {
				synchronized(INSTANCES) {
					conn = INSTANCES.get(key);
					if(conn==null) {
						conn = (MBeanServerConnection)Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{MBeanServerConnection.class}, new AgentMBeanServerConnectionFactory(this));
						INSTANCES.put(key, conn);
						channel.getCloseFuture().addListener(new ChannelFutureListener() {
							@Override
							public void operationComplete(ChannelFuture future) throws Exception {
								INSTANCES.remove(key);
							}
						});
					}
				}
			}
			return conn;
		}


		/**
		 * Sets the builder's remote address which overrides the channel's 
		 * @param remoteAddress the remoteAddress to set
		 * @return this builder
		 */
		public Builder remoteAddress(SocketAddress remoteAddress) {
			this.remoteAddress = remoteAddress;
			return this;
		}

		/**
		 * Sets the JMX operation timeout in ms 
		 * @param timeout the timeout to set
		 * @return this builder
		 */
		public Builder timeout(long timeout) {
			this.timeout = timeout;
			return this;
		}

		/**
		 * Sets the asynchronous response listener for the JMX connection 
		 * @param listener the listener to set
		 * @return this builder
		 */
		public Builder listener(AsynchJMXResponseListener listener) {
			this.listener = listener;
			return this;
		}

		/**
		 * Sets the JMX domain of the target MBeanServer for the JMX connection 
		 * @param domain the domain to set
		 * @return this builder
		 */
		public Builder domain(String domain) {
			this.domain = domain;
			return this;
		}
	}
	
	/**
	 * Creates a new AgentMBeanServerConnectionFactory
	 * @param builder The AgentMBeanServerConnectionFactory builder
	 */
	protected AgentMBeanServerConnectionFactory(Builder builder) {
		this.channel = builder.channel;
		this.remoteAddress = builder.remoteAddress==null ? this.channel.getRemoteAddress() : builder.remoteAddress;
		this.timeout = builder.timeout;
		this.listener = builder.listener;
		this.domain = builder.domain;
		if("DefaultDomain".equals(domain)) {
			domainInfoData = new byte[]{0};
		} else {
			byte[] domainBytes = domain.getBytes();
			domainInfoData = new byte[domainBytes.length + 1];
			domainInfoData[0] = (byte) domainBytes.length;
			System.arraycopy(domainBytes, 0, domainInfoData, 1, domainBytes.length);
		}
		if(channel.getPipeline().get(getClass().getSimpleName())==null) {
			this.channel.getPipeline().addFirst(getClass().getSimpleName(), responseHandler);
//			LoggingHandler logg = new LoggingHandler(InternalLogLevel.ERROR, true);
//			this.channel.getPipeline().addFirst("logger", logg);
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
			SimpleLogger.debug("Waiting for response to [", requestId, "]....");
			boolean timedout = latch.await(timeout, TimeUnit.MILLISECONDS);
			SimpleLogger.debug("Result of Waiting for response to [", requestId, "]:", timedout);
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
		SimpleLogger.debug("Response for req [", requestId, "]:[", value, "]");
		synchResultMap.putIfAbsent(requestId, value);
		CountDownLatch latch = synchTimeoutMap.remove(requestId);
		if(latch!=null) {
			latch.countDown();
			SimpleLogger.debug("Counted Down Latch for req [", requestId, "]:[", value, "]");
		}
		
	}
	
	
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
	 */
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if(MBeanServerConnection.class!=method.getDeclaringClass()) {
			return method.invoke(Modifier.isStatic(method.getModifiers()) ? null : this, args);
		}
		if(channel.getPipeline().get(getClass().getSimpleName())==null) {
			throw new IOException("This MBeanServerConnection has been closed", new Throwable());
		}
		//SimpleLogger.debug("MBeanServerConnection [", method.getName(), "] Payload Size [", sargs.length+6+4, "]");
		final int reqId = requestId.incrementAndGet();
		if("addNotificationListener".equals(method.getName()) && !method.getParameterTypes()[1].equals(ObjectName.class)) {
			NotificationListener listener = (NotificationListener)args[1]; 
			args[1] = reqId;
			addRegisteredListener(reqId, listener);
		} else if("removeNotificationListener".equals(method.getName()) && !method.getParameterTypes()[1].equals(ObjectName.class)) {
			removeRegisteredListener((NotificationListener)args[1]);
			args = new Object[0];
		}
		byte[] sargs = getOutput(args);
		ChannelBuffer cb = ChannelBuffers.directBuffer(1 + domainInfoData.length + 4 +1 + 4 + sargs.length);
		cb.writeByte(OpCode.JMX_REQUEST.op());  // 1
		cb.writeBytes(domainInfoData);   // domain data
		cb.writeInt(reqId);					// 4
		cb.writeByte(methodToKey.get(method)); // 1
		cb.writeInt(sargs.length);  			// 4
		cb.writeBytes(sargs);		           // sargs.length
		
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
					SimpleLogger.debug("Sent JMX Request to [", remoteAddress, "]");
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
	 * Returns a string array containing the default JMX domains of all available MBeanServers in this JVM.
	 * @return a string array of JMX default domains
	 */
	public static String[] getMBeanServerDomains() {
		Set<String> domains = new HashSet<String>();
		for(MBeanServer mbs: MBeanServerFactory.findMBeanServer(null)) {
			String domain = mbs.getDefaultDomain();
			if(domain==null) domain = "DefaultDomain";
			domains.add(domain);
		}
		return domains.toArray(new String[domains.size()]);
	}
	
	/**
	 * Sends the available MBeanServer domains in this JVM through the passed channel to the passed remote address in response to a {@link OpCode#JMX_MBS_INQUIRY} request.
	 * @param channel The channel to write to 
	 * @param remoteAddress The remote address to send to
	 */
	public static void sendMBeanServerDomains(Channel channel, SocketAddress remoteAddress) {
		String[] domains = getMBeanServerDomains();
		int size = 4 + (domains.length*4) + 1;
		for(String s: domains) {
			size += s.getBytes().length;
		}
		ChannelBuffer cb = ChannelBuffers.directBuffer(size);
		cb.writeByte(OpCode.JMX_MBS_INQUIRY_RESPONSE.op());
		cb.writeInt(domains.length);
		for(String s: domains) {
			byte[] bytes = s.getBytes();
			cb.writeInt(bytes.length);
			cb.writeBytes(bytes);
		}
		SimpleLogger.info("Sending MBeanServer Domain List ", Arrays.toString(domains));
		channel.write(cb, remoteAddress);
	}
	
	/**
	 * Creates and registers a remote MBeanServerProxy MBean for each of the passed MBeanServer default domains passed 
	 * @param channel The underlying comm channel
	 * @param remoteAddress The remote address of the agent
	 * @param host The host of the agent
	 * @param agent The agent name
	 * @param protocol The protocol name
	 * @param jmxDomains An array of MBeanServer default domains to register MBeanServerProxys for
	 */
	public static void registerRemoteMBeanServerConnections(Channel channel, SocketAddress remoteAddress, String host, String agent, String protocol, String...jmxDomains) {
		for(String domain: jmxDomains) {
			try {
				JMXConnector connector = new MXLocalJMXConnector(host, agent, protocol, domain);
				MBeanServerConnectionProxy proxy = new MBeanServerConnectionProxy(connector, host, agent, protocol, domain);
			} catch (Exception ex) {
				SimpleLogger.error("Failed to register MBeanServerConnection Proxy for:\n\tHost:", host, "\n\tAgent:", agent, "\n\tDomain:", domain, "\n\tRemoteAddress:", remoteAddress, ex);
			}
		}
	}
	
	/**
	 * Writes a notification back to the originating listener registrar
	 * @param channel The channel to write on
	 * @param remoteAddress The remote address to write to
	 * @param requestId The request ID of the original listener registration
	 * @param notification The notification to write
	 * @param handback The optional contextual handback
	 */
	public static void writeNotification(Channel channel, SocketAddress remoteAddress, int requestId, Notification notification, Object handback) {
		byte[] payload = getOutput(notification, handback);
		int size = payload.length + 1 + 4 + 4;  // size is <payload size> + <OpCode> + <requestId> + <<payload length>
		ChannelBuffer cb = ChannelBuffers.directBuffer(size);
		cb.writeByte(OpCode.JMX_NOTIFICATION.op());
		cb.writeInt(requestId);
		cb.writeInt(payload.length);
		cb.writeBytes(payload);
		channel.write(cb, remoteAddress);
		SimpleLogger.info("Wrote JMX Notification [", size, "] bytes");
	}
	
	
	/**
	 * Handles a received {@link MBeanServerConnection} invocation
	 * @param channel The channel the request was received on
	 * @param remoteAddress The remote address of the caller
	 * @param buffer THe buffer received
	 * 
	 */
	public static void handleJMXRequest(Channel channel, SocketAddress remoteAddress, ChannelBuffer buffer) {
		buffer.resetReaderIndex();
		/* The request write */
//		cb.writeByte(OpCode.JMX_REQUEST.op());  // 1
//		cb.writeBytes(domainInfoData);   // domain data
//		cb.writeInt(reqId);					// 4
//		cb.writeByte(methodToKey.get(method)); // 1
//		cb.writeInt(sargs.length);  			// 4
//		cb.writeBytes(sargs);		           // sargs.length
		Object result = null;
		MBeanServerConnection server = null;
		buffer.skipBytes(1);
		byte domainIndicator = buffer.readByte();
		if(domainIndicator==0) {
			server = JMXHelper.getHeliosMBeanServer();
		} else {
			byte[] domainBytes = new byte[domainIndicator];
			buffer.readBytes(domainBytes);
			String domain = new String(domainBytes);
			server = JMXHelper.getLocalMBeanServer(true, domain);
			if(server==null) {
				result = new SmallException("Failed to locate MBeanServer for domain [" + domain + "]");
			}
		}
		int reqId = buffer.readInt();
		byte methodId = buffer.readByte();		
		if(result==null) {
			int payloadSize = buffer.readInt();
			byte[] payload = new byte[payloadSize];
			buffer.readBytes(payload);
			Object[] params = getInput(payload);		
			Method targetMethod = null;
			try {
				targetMethod = keyToMethod.get(methodId);
				if(targetMethod==null) {
					result = new SmallException("Failed to handle MBeanServerConnection invocation because method Op Code [" + methodId + "] was not recognized");
				} else {
					if("addNotificationListener".equals(targetMethod.getName()) && !targetMethod.getParameterTypes()[1].equals(ObjectName.class)) {
						
					} else if("removeNotificationListener".equals(targetMethod.getName()) && !targetMethod.getParameterTypes()[1].equals(ObjectName.class)) {
						
					} else {
						result = targetMethod.invoke(server, params);
					}
				}			
			} catch (Throwable t) {
				SimpleLogger.warn("Failed to invoke [", targetMethod, "]", t);
				result = new SmallException(t.toString());
			}
		}
		writeJMXResponse(reqId, methodId, channel, remoteAddress, result);		
	}
	
	/**
	 * <p>Title: SmallThrowable</p>
	 * <p>Description: Extension of {@link Exception} that minimizes it's size by removing the stack trace.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.jmx.mbeanserver.AgentMBeanServerConnectionFactory.SmallException</code></p>
	 */
	public static class SmallException extends Exception {
		/**  */
		private static final long serialVersionUID = -6927976563869115032L;

		/**
		 * Creates a new SmallThrowable
		 * @param message the error message
		 */
		public SmallException(String message) {
			super(message);
			setStackTrace(new StackTraceElement[0]);
		}
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
	
//	/**
//	 * Serializes an attribute list to a byte array.
//	 * Handled differently from {@link #getOutput(Object...)} in that it discards attributes that fail to serialize.
//	 * Consequently, it requires a custom {@link #getInput(byte[])}
//	 * @param attrList The attribute list to serialize
//	 * @return a byte array
//	 */
//	public static byte[] getOutput(AttributeList attrList) {
//		if(attrList==null || attrList.size()==0) return new byte[]{};
//		ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
//		GZIPOutputStream gzos = null;
//		try {			
//			int writes = 0;
//			gzos= new GZIPOutputStream(baos);
//			for(Attribute attr: attrList.asList()) {
//				byte[] attrBytes = serializeNoCompress(attr);
//				if(attrBytes.length>0) {
//					gzos.write(attrBytes);
//					writes++;
//				}
//			}
//			gzos.finish();
//			gzos.flush();
//			baos.flush();
//			byte[] payload = baos.toByteArray();
//			byte[] sizedPayload = new byte[payload.length+1];
//			byte[] writeBytes = ByteBuffer.allocate(4).putInt(writes).array();
//			System.arraycopy(writeBytes, 0, sizedPayload, 0, 4);
//			System.arraycopy(payload, 0, sizedPayload, 4, sizedPayload.length);
//			return sizedPayload;
//		} catch (Exception ex) {
//			//throw new RuntimeException("Failed to encode MBeanServerConnection Invocation arguments " + Arrays.toString(args), ex);
//			return new byte[0];
//		} finally {
//			try { baos.close(); } catch (Exception ex) {/* No Op */}
//			if(gzos!=null) try { gzos.close(); } catch (Exception ex) {/* No Op */}
//		}
//	}
//	
//	public static AttributeList getAttributeListInput(byte[] bytes) {
//		
//	}
	
	/**
	 * Serializes the passed object to a byte array, returning a zero byte array if the passed object is null, or fails serialization
	 * @param arg The object to serialize
	 * @return the serialized object
	 */
	public static  byte[] serializeNoCompress(Object arg) {
		if(arg==null) return new byte[]{};
		ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
		ObjectOutputStream oos = null;
		try {			
			oos = new ObjectOutputStream(baos);
			oos.writeObject(arg);
			oos.flush();
			baos.flush();
			return baos.toByteArray();
		} catch (Exception ex) {
			return new byte[]{};
		} finally {
			try { baos.close(); } catch (Exception ex) {/* No Op */}
			if(oos!=null) try { oos.close(); } catch (Exception ex) {/* No Op */}
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


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.mbeanserver.MBeanServerConnectionAdmin#addRegisteredListener(int, javax.management.NotificationListener)
	 */
	@Override
	public void addRegisteredListener(int requestId, NotificationListener listener) {
		registeredListeners.put(requestId, listener);
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.mbeanserver.MBeanServerConnectionAdmin#removeRegisteredListener(javax.management.NotificationListener)
	 */
	@Override
	public void removeRegisteredListener(NotificationListener listener) {
		if(listener!=null) {
			registeredListeners.remove(listener);
		}
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jmx.mbeanserver.MBeanServerConnectionAdmin#onNotification(int, javax.management.Notification, java.lang.Object)
	 */
	@Override
	public void onNotification(int requestId, Notification notification, Object handback) {
		NotificationListener listener = registeredListeners.get(requestId);
		if(listener!=null) {
			listener.handleNotification(notification, handback);
		}
	}
}
