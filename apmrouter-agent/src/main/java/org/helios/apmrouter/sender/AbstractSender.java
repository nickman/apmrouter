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
package org.helios.apmrouter.sender;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.helios.apmrouter.OpCode;
import org.helios.apmrouter.collections.ConcurrentLongSlidingWindow;
import org.helios.apmrouter.collections.ILongSlidingWindow;
import org.helios.apmrouter.jmx.ConfigurationHelper;
import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.jmx.ScheduledThreadPoolFactory;
import org.helios.apmrouter.jmx.ThreadPoolFactory;
import org.helios.apmrouter.metric.AgentIdentity;
import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.metric.catalog.ICEMetricCatalog;
import org.helios.apmrouter.metric.catalog.IMetricCatalog;
import org.helios.apmrouter.sender.netty.UDPSender;
import org.helios.apmrouter.sender.netty.codec.IMetricEncoder;
import org.helios.apmrouter.sender.netty.handler.ChannelStateAware;
import org.helios.apmrouter.sender.netty.handler.ChannelStateListener;
import org.helios.apmrouter.sentry.PollingSentryWatched;
import org.helios.apmrouter.sentry.Sentry;
import org.helios.apmrouter.sentry.SentryState;
import org.helios.apmrouter.sentry.SentryStateControl;
import org.helios.apmrouter.trace.DirectMetricCollection;
import org.helios.apmrouter.util.TimeoutQueueMap;
import org.jboss.netty.bootstrap.ConnectionlessBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelState;
import org.jboss.netty.channel.ChannelStateEvent;


/**
 * <p>Title: AbstractSender</p>
 * <p>Description: Abstract base class for sender implementations</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.sender.AbstractSender</code></p>
 */

public abstract class AbstractSender implements AbstractSenderMXBean, ISender, ChannelPipelineFactory, ChannelStateAware {
	/** A map of created senders keyed by the URI */
	protected static final Map<URI, ISender> senders = new ConcurrentHashMap<URI, ISender>(); 
	/** The metric encoder */
	protected static final IMetricEncoder metricEncoder = new IMetricEncoder();
	/** The synchronous request timeout map */
	protected static final TimeoutQueueMap<String, CountDownLatch> timeoutMap = new TimeoutQueueMap<String, CountDownLatch>(2000);
	/** The count of metric sends */
	protected final AtomicLong sent = new AtomicLong(0);
	/** The count of dropped metric sends */
	protected final AtomicLong dropped = new AtomicLong(0);
	/** The count of failed metric sends */
	protected final AtomicLong failed = new AtomicLong(0);
	/** The count of timed out pings */
	protected final AtomicLong pingTimeOuts = new AtomicLong(0);
	/** The logical connected state of this sender */
	protected final AtomicBoolean connected = new AtomicBoolean(false);
	
	/** Sliding window of ping times */
	protected final ILongSlidingWindow pingTimes = new ConcurrentLongSlidingWindow(64); 
	/** The URI of the apmrouter server to connect to */
	protected final URI serverURI;
	/** The sending channel */
	protected Channel senderChannel;
	/** The listener channel */
	protected Channel listenerChannel;
	
	/** The server socket to send to */
	protected InetSocketAddress socketAddress;
	/** The server socket to listen on */
	protected InetSocketAddress listeningSocketAddress;
	/** The sender's scheduler */
	protected final ScheduledThreadPoolExecutor scheduler = ScheduledThreadPoolFactory.newScheduler("AgentScheduler");
	/** The frequency in ms. of heartbeat pings to the apmrouter server */
	protected long heartbeatPingPeriod = 5000;
	/** The heartbeat ping timeout in ms. */
	protected long heartbeatTimeout = 1000;
	/** The number of consecutive heartbeat ping timeouts that trigger a disconnected state */
	protected int heartbeatTimeoutDiscTrigger = 2;
	/** The number of consecutive heartbeat ping timeouts */
	protected final AtomicLong consecutiveTimeouts = new AtomicLong(0);
	/** The number of tokens received and applied to the catalog */
	protected final AtomicLong processedTokens = new AtomicLong(0);
	
	/** The metric catalog for token updates */
	protected final IMetricCatalog metricCatalog;
	/** The channel close future */
	protected ChannelFuture closeFuture = null;
	/** The channel state listener */
	protected final ChannelStateListener channelStateListener = new ChannelStateListener();
	/** The netty server worker pool */
	protected final Executor workerPool;
	/** The netty channel factory */
	protected ChannelFactory channelFactory;
	
	/** Final shutdown flag */
	protected static final AtomicBoolean shutdown = new AtomicBoolean(false);
	
	
	/** The ping schedule handle */
	protected ScheduledFuture<?> pingScheduleHandle = null;
	
	/**
	 * Creates a new AbstractSender
	 * @param serverURI The URI of the apmrouter server to connect to
	 */
	protected AbstractSender(URI serverURI) {
		this.serverURI = serverURI;
		socketAddress = new InetSocketAddress(serverURI.getHost(), serverURI.getPort());
		heartbeatPingPeriod = ConfigurationHelper.getLongSystemThenEnvProperty(HBEAT_PERIOD_PROP, DEFAULT_HBEAT_PERIOD);
		heartbeatTimeout = ConfigurationHelper.getLongSystemThenEnvProperty(HBEAT_TO_PROP, DEFAULT_HBEAT_TO);
		resetPingSchedule();
		metricCatalog = ICEMetricCatalog.getInstance();
		workerPool =  ThreadPoolFactory.newCachedThreadPool(getClass().getPackage().getName(), getClass().getSimpleName() + "Worker/" + serverURI.getHost() + "/" + serverURI.getPort());
		try {
			JMXHelper.registerMBean(JMXHelper.objectName(
					new StringBuilder("org.helios.apmrouter.sender:protocol=")
					.append(serverURI.getScheme())
					.append(",host=").append(serverURI.getHost())
					.append(",port=").append(serverURI.getPort()))
			,this);
		} catch (Exception e) {
			System.err.println("Failed to publish management interface for sender [" + serverURI + "]. Continuing without");
			e.printStackTrace(System.err);
		}
		Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override
			public void run() {
				try {	
					shutdown.set(true);
					senderChannel.write(ChannelBuffers.wrappedBuffer(new byte[]{OpCode.BYE.op()}), socketAddress).addListener(new ChannelFutureListener() {
						@Override
						public void operationComplete(ChannelFuture f) throws Exception {						
							channelFactory.releaseExternalResources();
						}
					});
					log("Attempted to send Bye");
				} catch (Exception ex) {}
			}
		});		
	}
	
	/**
	 * Cancels the existing ping schedule if one exists and starts a new one
	 */
	protected void resetPingSchedule() {
		if(pingScheduleHandle!=null) {
			pingScheduleHandle.cancel(true);
			pingScheduleHandle = null;
		}
		log("Scheduling pings every [" + heartbeatPingPeriod + "] with a [" + heartbeatTimeout + "] timeout.");
		pingScheduleHandle = scheduler.scheduleAtFixedRate(new Runnable(){
			final long finalTimeout = heartbeatTimeout;
			@Override
			public void run() {
				log("Scheduled Ping");
				if(!ping(finalTimeout)) {
					pingTimeOuts.incrementAndGet();
				}
			}
		}, 1, heartbeatPingPeriod, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sender.AbstractSenderMXBean#getSentMetrics()
	 */
	@Override
	public long getSentMetrics() {
		return sent.get();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sender.AbstractSenderMXBean#getDroppedMetrics()
	 */
	@Override
	public long getDroppedMetrics() {
		return dropped.get();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sender.AbstractSenderMXBean#getFailedMetrics()
	 */
	@Override
	public long getFailedMetrics() {
		return failed.get();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sender.AbstractSenderMXBean#getAveragePingTime()
	 */
	@Override
	public long getAveragePingTime() {
		return pingTimes.avg();
	}
	
	
	/**
	 * Sends a ping request to the passed address
	 * @param address The address to ping
	 * @param timeout the timeout in ms.
	 * @return true if ping was confirmed within the timeout, false otherwise
	 */
	@Override
	public boolean ping(SocketAddress address, long timeout) {
		try {
			StringBuilder key = new StringBuilder();
			ChannelBuffer ping = encodePing(key);
			senderChannel.write(ping,address);
			CountDownLatch latch = new CountDownLatch(1);
			log("Sent ping [" + key + "]");
			timeoutMap.put(key.toString(), latch, timeout);
			boolean success = latch.await(timeout, TimeUnit.MILLISECONDS);
			if(success) {
				log("Ping Confirmed");
				resetConsecutiveTimeouts();
			} else {
				log("Ping Timed Out");
				incrConsecutiveTimeouts();
			}
			return success;
		} catch (InterruptedException e) {
			return false;
		}		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sender.AbstractSenderMXBean#ping(long)
	 */
	@Override
	public boolean ping(long timeout) {
		return ping(socketAddress, timeout);
	}
	
	/**
	 * Creates a ping channel buffer and appends the key to the passed buffer 
	 * @param key The buffer to place the key in
	 * @return the ping ChannelBuffer
	 */
	protected ChannelBuffer encodePing(final StringBuilder key) {
		String _key = new StringBuilder(AgentIdentity.ID.getHostName()).append("-").append(AgentIdentity.ID.getAgentName()).append("-").append(System.nanoTime()).toString();
		key.append(_key);
		byte[] bytes = _key.getBytes();
		ChannelBuffer ping = ChannelBuffers.buffer(1+4+bytes.length);
		ping.writeByte(OpCode.PING.op());
		ping.writeInt(bytes.length);
		ping.writeBytes(bytes);
		return ping;
	}
	
	/**
	 * Called whenever a heartbeat ping succeeds.
	 */
	private void resetConsecutiveTimeouts() {
		consecutiveTimeouts.set(0);
		if(connected.compareAndSet(false, true)) {
			// fire event
		}
	}
	
	/**
	 * Increments the consecutive timeout counter, possibly triggering a disconnect 
	 */
	private void incrConsecutiveTimeouts() {
		long tos = consecutiveTimeouts.incrementAndGet();
		if(tos>heartbeatTimeoutDiscTrigger) {
			if(connected.compareAndSet(true, false)) {
				// fire event
			}
		}
	}
	
	/**
	 * Decodes a ping from the passed channel buffer, and if the resulting key locates a latch in the timeout map, counts it down.
	 * @param cb The ChannelBuffer to read the ping from
	 */
	protected void decodePing(ChannelBuffer cb) {
		int byteCount = cb.readInt();
		byte[] bytes = new byte[byteCount];
		cb.readBytes(bytes);
		String key = new String(bytes);
		//log("Processing ping response [" + key + "]");
		CountDownLatch latch = timeoutMap.remove(key);
		if(latch!=null) latch.countDown();
		try {
			pingTimes.insert(System.nanoTime()-Long.parseLong(key.split("-")[2]));
		} catch (Exception e) {}
		//pingTimes.insert(System.nanoTime()-pingKey);
	}
	
	
	/**
	 * Out log
	 * @param msg the message to log
	 */
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sender.ISender#send(org.helios.apmrouter.metric.IMetric, long)
	 */
	@Override
	public void send(IMetric metric, long timeout) throws TimeoutException {
		DirectMetricCollection dcm = DirectMetricCollection.newDirectMetricCollection(metric);
		dcm.setOpCode(OpCode.SEND_METRIC_DIRECT);
		CountDownLatch latch = new CountDownLatch(1);
		String key = new StringBuilder(metric.getFQN()).append(metric.getTime()).toString();
		send(dcm);
		timeoutMap.put(key, latch, timeout);
		try {
			if(!latch.await(timeout, TimeUnit.MILLISECONDS)) {
				throw new TimeoutException("Direct Metric Trace timed out after " + timeout + " ms. "); //[" + metric + "]");
			}
		} catch (InterruptedException e) {
			throw new RuntimeException("Thread interrupted while waiting for Direct Metric Trace confirm for " + timeout + " ms. [" + metric + "]", e);
		}
	}
	

	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sender.AbstractSenderMXBean#getURI()
	 */
	@Override
	public URI getURI() {
		return serverURI;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sender.AbstractSenderMXBean#getHeartbeatPingPeriod()
	 */
	@Override
	public long getHeartbeatPingPeriod() {
		return heartbeatPingPeriod;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sender.AbstractSenderMXBean#setHeartbeatPingPeriod(long)
	 */
	@Override
	public void setHeartbeatPingPeriod(long heartbeatPingPeriod) {
		boolean reset = (this.heartbeatPingPeriod != heartbeatPingPeriod);
		this.heartbeatPingPeriod = heartbeatPingPeriod;
		if(reset) {
			resetPingSchedule();
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sender.AbstractSenderMXBean#getHeartbeatTimeout()
	 */
	@Override
	public long getHeartbeatTimeout() {
		return heartbeatTimeout;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sender.AbstractSenderMXBean#setHeartbeatTimeout(long)
	 */
	@Override
	public void setHeartbeatTimeout(long heartbeatTimeout) {
		boolean reset = (this.heartbeatTimeout != heartbeatTimeout);
		this.heartbeatTimeout = heartbeatTimeout;
		if(reset) {
			resetPingSchedule();
		}
		
		this.heartbeatTimeout = heartbeatTimeout;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sender.AbstractSenderMXBean#getHeartbeatTimeoutTrigger()
	 */
	@Override
	public int getHeartbeatTimeoutTrigger() {
		return heartbeatTimeoutDiscTrigger;
	}
	
	/**
	 * Sets the number of consecutive heartbeat timeouts that will trigger a disconnect state
	 * @param heartbeatTimeoutDiscTrigger the number of consecutive heartbeat timeouts that will trigger a disconnect state
	 */
	@Override
	public void setHeartbeatTimeoutTrigger(int heartbeatTimeoutDiscTrigger) {
		this.heartbeatTimeoutDiscTrigger = heartbeatTimeoutDiscTrigger;
	}
	
	/**
	 * Returns the number of consecutive heartbeat ping timeouts
	 * @return the number of consecutive heartbeat ping timeouts
	 */
	@Override
	public long getConsecutiveTimeouts() {
		return consecutiveTimeouts.get();
	}
	
	/**
	 * Returns the number of processed metric tokens
	 * @return the number of processed metric tokens
	 */
	@Override
	public long getProcessedTokens() {
		return processedTokens.longValue();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sender.AbstractSenderMXBean#isConnnected()
	 */
	@Override
	public boolean isConnnected() {
		return connected.get();
	}
	
	// ====================================================
	//		Moved up from UDPSender
	// ====================================================
	
	
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sender.AbstractSenderMXBean#getName()
	 */
	@Override
	public String getName() {
		return new StringBuilder(getClass().getSimpleName()).append("[").append(socketAddress.getHostName()).append(":").append(socketAddress.getPort()).append("]").toString();
	}


	
	
	// ==================================================================
	//		SenderFactory Specific Impls.
	// ==================================================================
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sender.netty.handler.ChannelStateAware#getInterestedChannelStates()
	 */
	@Override
	public abstract ChannelState[] getInterestedChannelStates();

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sender.netty.handler.ChannelStateAware#onChannelStateEvent(boolean, org.jboss.netty.channel.ChannelStateEvent)
	 */
	@Override
	public abstract void onChannelStateEvent(boolean upstream, ChannelStateEvent stateEvent);

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelPipelineFactory#getPipeline()
	 */
	@Override
	public abstract ChannelPipeline getPipeline();

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sender.ISender#send(org.helios.apmrouter.trace.DirectMetricCollection)
	 */
	@Override
	public abstract void send(final DirectMetricCollection dcm);
	
	public abstract void doSendHello();

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.MetricSubmitter#submitDirect(org.helios.apmrouter.metric.IMetric, long)
	 * FIXME: Merge send and submit, they're redundant
	 */
	@Override
	public void submitDirect(IMetric metric, long timeout) throws TimeoutException {
		send(metric, timeout);
		
	}
	
	/**
	 * Sends a HELLO op to the server
	 */
	public void sendHello() {
		log("Sending HELLO");
		CountDownLatch latch = new CountDownLatch(1);
		String key = "Hello";
		doSendHello();
		timeoutMap.put(key, latch, 5000);
		try {
			if(!latch.await(5000, TimeUnit.MILLISECONDS)) {
				timeoutMap.remove("Hello");
				scheduler.execute(new Runnable(){
					@Override
					public void run() {
						sendHello();
					}
				});
			}
		} catch (InterruptedException e) {
			//throw new RuntimeException("Thread interrupted while waiting for Direct Metric Trace confirm for " + timeout + " ms. [" + metric + "]", e);
		}
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.MetricSubmitter#submit(java.util.Collection)
	 * FIXME: Merge send and submit, they're redundant
	 */
	@Override
	public void submit(Collection<IMetric> metrics) {
		send(DirectMetricCollection.newDirectMetricCollection(metrics.toArray(new IMetric[0])));
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.MetricSubmitter#submit(org.helios.apmrouter.metric.IMetric[])
	 * FIXME: Merge send and submit, they're redundant
	 */
	@Override
	public void submit(IMetric... metrics) {
		send(DirectMetricCollection.newDirectMetricCollection(metrics));	
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.MetricSubmitter#resetStats()
	 */
	@Override
	public void resetStats() {
		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.trace.MetricSubmitter#getQueuedMetrics()
	 */
	@Override
	public long getQueuedMetrics() {
		return 0;
	}




	
}
