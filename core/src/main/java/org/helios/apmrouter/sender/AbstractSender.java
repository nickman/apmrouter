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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import org.helios.apmrouter.OpCode;
import org.helios.apmrouter.collections.ConcurrentLongSlidingWindow;
import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.sender.netty.codec.IMetricEncoder;
import org.helios.apmrouter.trace.DirectMetricCollection;
import org.helios.apmrouter.util.TimeoutQueueMap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;


/**
 * <p>Title: AbstractSender</p>
 * <p>Description: Abstract base class for sender implementations</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.sender.AbstractSender</code></p>
 */

public abstract class AbstractSender implements ISender {
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
	/** Sliding window of ping times */
	protected final ConcurrentLongSlidingWindow pingTimes = new ConcurrentLongSlidingWindow(64); 
	protected final URI serverURI;
	
	/** The sending channel */
	protected Channel senderChannel;

	
	/** The server socket to send to */
	protected InetSocketAddress socketAddress;
	/** The server socket to listen on */
	protected InetSocketAddress listeningSocketAddress;
	
	
	protected AbstractSender(URI serverURI) {
		this.serverURI = serverURI;
	}
	
	public long getSentMetrics() {
		return sent.get();
	}
	
	public long getDroppedMetrics() {
		return dropped.get();
	}
	
	public long getFailedMetrics() {
		return failed.get();
	}
	
	/**
	 * Returns a sliding window average of agent ping elapsed times to the server
	 * @return a sliding window average of agent ping elapsed times to the server
	 */
	public long getAveragePingTime() {
		return pingTimes.avg();
	}
	
	
	/**
	 * Sends a ping request to the passed address
	 * @param address The address to ping
	 * @param timeout the timeout in ms.
	 * @return true if ping was confirmed within the timeout, false otherwise
	 */
	public boolean ping(SocketAddress address, long timeout) {
		try {
			long key = System.nanoTime();
			ChannelBuffer ping = ChannelBuffers.buffer(1+8);
			ping.writeByte(OpCode.PING.op());
			ping.writeLong(key);
			senderChannel.write(ping,address);
			CountDownLatch latch = new CountDownLatch(1);
			timeoutMap.put("" + key, latch, timeout);
			return latch.await(timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			return false;
		}		
	}
	
	/**
	 * Sends a ping request to the configured server
	 * @param timeout the timeout in ms.
	 * @return true if ping was confirmed within the timeout, false otherwise
	 */
	public boolean ping(long timeout) {
		return ping(socketAddress, timeout);
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.sender.ISender#send(org.helios.apmrouter.metric.IMetric, long)
	 */
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
	 * @see org.helios.apmrouter.sender.ISender#getURI()
	 */
	@Override
	public URI getURI() {
		return serverURI;
	}

}
