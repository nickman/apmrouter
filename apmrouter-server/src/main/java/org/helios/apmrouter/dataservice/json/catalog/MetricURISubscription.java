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
package org.helios.apmrouter.dataservice.json.catalog;

import java.net.SocketAddress;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MXBean;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.helios.apmrouter.OpCode;
import org.helios.apmrouter.catalog.EntryStatus;
import org.helios.apmrouter.catalog.domain.Metric;
import org.helios.apmrouter.collections.ConcurrentLongSortedSet;
import org.helios.apmrouter.dataservice.json.JsonResponse;
import org.helios.apmrouter.metric.MetricType;
import org.hibernate.Session;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.ChannelGroupFutureListener;
import org.jboss.netty.channel.group.DefaultChannelGroup;

/**
 * <p>Title: MetricURISubscription</p>
 * <p>Description: Represents a subscription to events emanating from a MetricURI (new metrics, metric state changes)</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.dataservice.json.catalog.MetricURISubscription</code></p>
 */
@MXBean
public class MetricURISubscription implements ChannelGroupFutureListener, MetricURISubscriptionMBean {
	/** The subscribed metricURI */
	protected final MetricURI metricURI;
	
	/** The metric ids that have been determined to match the metric URI */
	protected final ConcurrentLongSortedSet metricIds = new ConcurrentLongSortedSet(128);
	/** The channels subscribed to this metric URI */
	protected final ChannelGroup subscribedChannels = new DefaultChannelGroup(getClass().getSimpleName());
	
	/** A map of {@link MetricURISubscription}s keyed by the subscription's {@link MetricURI} */
	protected static final Map<MetricURI, MetricURISubscription> subscriptions = new ConcurrentHashMap<MetricURI, MetricURISubscription>(128, 0.75f, 16);
	
	/** A map of sets of subscriptions keyed by the metric type/status/subType mask of the metric URI */
	protected static final Map<Long, Set<MetricURISubscription>> subscriptionsByMask = new ConcurrentHashMap<Long, Set<MetricURISubscription>>(128, 0.75f, 16);
	

	
	/** Static class logger */
	protected static final Logger LOG = Logger.getLogger(MetricURISubscription.class);
	
	
	
	/**
	 * Returns an iterator over the subscriptions keyed for the passed type/status/subType mask, or null if there are none.
	 * @param mask The type/status/subType mask to get subscriptions for
	 * @return a subscription iterator or null
	 */
	public static Iterator<MetricURISubscription> getSubscriptionsForSubType(long mask) {
		Set<MetricURISubscription> set = subscriptionsByMask.get(mask);
		if(set==null||set.isEmpty()) return null;
		return set.iterator();
	}
	
 
	/**
	 * Returns the MetricURISubscription for the passed MetricURI or null if one does not already exist
	 * @param metricUri the MetricURI to get the subscription for
	 * @return the MetricURISubscription for the passed MetricURI or null if one does not already exist
	 */
	public static MetricURISubscription getMetricURISubscriptionOrNull(MetricURI metricUri) {
		return subscriptions.get(metricUri);
	}
	
	/**
	 * Called by the {@link MetricURISubscriptionService} when it is processing and incoming metric state change event.
	 * The intent of this call is to try and eliminate the event as applicable to this subscription as early as possible.
	 * If the event can be excluded, returns <b>false</b>. If it cannot be excluded, returns true
	 * and the {@link MetricURISubscriptionService} will record this instance for a second pass.
	 * The candidacy is broken out into 2 phases to avoid fetching the actual metric instance for transmission to subscribers
	 * until it is verified that it is needed.
	 * @param metricId The id of the metric that changed state.
	 * @param newState The byte code of the new state.
	 * @return false if the event is positively filtered <i>out</i> of this subscription, true otherwise.
	 */
	public boolean stateChangeCandidacy(long metricId, byte newState) {
		if(MetricURISubscriptionType.STATE_CHANGE.isEnabled(metricURI.getSubscriptionType())) return false;
		//if(metricIds.contains(metricId) && )
		
		return false;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.dataservice.json.catalog.MetricURISubscriptionMBean#getMetricURI()
	 */
	@Override
	public MetricURI getMetricURI() {
		return metricURI;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.dataservice.json.catalog.MetricURISubscriptionMBean#getMetricIdCount()
	 */
	@Override
	public int getMetricIdCount() {
		return metricIds.size();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.dataservice.json.catalog.MetricURISubscriptionMBean#getChannelCount()
	 */
	@Override
	public int getChannelCount() {
		return subscribedChannels.size();
	}
	
	
	/**
	 * Returns the MetricURISubscription for the passed MetricURI
	 * @param session A hibernate session to load the initial metric id set
	 * @param metricUri the MetricURI to get the subscription for
	 * @return the MetricURISubscription for the passed MetricURI
	 */
	public static MetricURISubscription getMetricURISubscription(Session session, MetricURI metricUri) {
		if(metricUri==null) throw new IllegalArgumentException("The passed metricUri was null", new Throwable());
		MetricURISubscription metricUriSub = subscriptions.get(metricUri);
		if(metricUriSub==null) {
			synchronized(subscriptions) {
				metricUriSub = subscriptions.get(metricUri);
				if(metricUriSub==null) {
					metricUriSub = new MetricURISubscription(session, metricUri);
					subscriptions.put(metricUri, metricUriSub);
				}
			}
		}
		return metricUriSub;
	}
	
	/**
	 * Called when the MetricURISubscriptionService is notified of a new metric.
	 * @param metricId The id of the metric
	 * @param fQN The fully qualified metric name of the new metric
	 * @param newRow The attributes of the new metric 
	 * @param ds The catalog data source in case we need to query the catalog
	 */
	public static void onNewMetric(long metricId, String fQN, Object[] newRow, DataSource ds) {
		for(MetricURISubscription sub: subscriptions.values()) {
			sub.handleNewMetric(metricId, fQN, newRow, ds);
		}
	}
	
	public static Set<MetricURISubscription> stateChangeCandidates(long metricId, byte newState) {
		if(subscriptions.isEmpty()) return Collections.emptySet();
		Set<MetricURISubscription> candidates = new HashSet<MetricURISubscription>();
		for(MetricURISubscription sub: subscriptions.values()) {
			
		}
		return candidates;
	}
	
	/**
	 * Called when the MetricURISubscriptionService is notified of a new metric.
	 * @param metricId The id of the metric
	 * @param fQN The fully qualified metric name of the new metric
	 * @param newRow The attributes of the new metric 
	 * @param ds The catalog data source in case we need to query the catalog
	 */
	protected void handleNewMetric(long metricId, String fQN, Object[] newRow, DataSource ds) {
		if(!metricIds.contains(metricId)) {
			Connection conn  = null;
			Statement st = null;
			ResultSet rset = null;
			boolean process = false;
			try {
				conn = ds.getConnection();
				st = conn.createStatement();
				rset = st.executeQuery(metricURI.metricIdSql + " AND M.METRIC_ID = " + metricId);
				process = rset.next();
				rset.close(); rset = null; st.close(); st = null; conn.close(); conn = null;
				if(process) {
					sendSubscribersNewMetric(newRow);
				}
			} catch (Exception ex) {
				
			} finally {
				if(rset != null) try { rset.close(); } catch (Exception x) {/* No Op */};
				if(st!= null) try { st.close(); } catch (Exception x) {/* No Op */};
				if(conn != null) try { conn.close(); } catch (Exception x) {/* No Op */};
			}
			
		}
	}
	
	/**
	 * Called when a new metric comes in the door and is determined to be a member of this metric uri, but not in the set already.
	 * @param newRow The new metric attributes
	 */
	protected void sendSubscribersNewMetric(Object[] newRow) {
//		GSONJSONMarshaller marshaller = new GSONJSONMarshaller();
//		for(Channel channel: subscribedChannels) {
//			LOG.info("Writing new metric event to channel [" + channel + "]");
//			SharedChannelGroup.getInstance().find(channel.getId());
//			marshaller.marshallToChannel(newRow, channel, null);
//			
//		}
		for(Channel channel: subscribedChannels) {
			((ChannelJsonResponsePair)channel).write(newRow, OpCode.ON_METRIC_URI_EVENT);
		}
		
	}
	
	
	/**
	 * Creates a new MetricURISubscription
	 * @param session A hibernate session to load the initial metric id set
	 * @param metricURI The metric URI being subscribed to 
	 */
	private MetricURISubscription(Session session, MetricURI metricURI) {
		this.metricURI = metricURI;
		try {
			addMetricIds(metricURI.execute(session));
		} catch (Exception ex) {
			throw new RuntimeException("Failed to poplulate initial metric id set", ex);
		}
	}
	
	/**
	 * Adds the metric ids of the passed metrics to this subscriptions metricId tracking
	 * @param metrics the metrics initially determined to be associated to this subscription
	 */
	void addMetricIds(Collection<Metric> metrics) {
		for(Metric metric: metrics) {
			metricIds.add(metric.getMetricId());
		}
	}

	
	/**
	 * Subscribes the passed channel to this MetricURISubscription
	 * @param channel The channel to subscribe
	 * @param response The response formatter for the channel's remote client
	 */
	public void subscribeChannel(final Channel channel, JsonResponse response) {
		if(channel==null) throw new IllegalArgumentException("The passed channel was null", new Throwable());
		if(response==null) throw new IllegalArgumentException("The passed response was null", new Throwable());
		final ChannelJsonResponsePair pair = ChannelJsonResponsePair.getChannelJsonResponsePair(channel, response);
		if(subscribedChannels.add(pair)) {
			pair.getCloseFuture().addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					subscribedChannels.remove(pair);
					if(subscribedChannels.size()<1) {
						subscriptions.remove(metricURI);
					}
				}
			});
			pair.incrSubs();
		}
	}
	
	/**
	 * Unsubscribes the passed channel from this MetricURISubscription
	 * @param channel The channel to unsubscribe
	 */
	public void unSubscribeChannel(Channel channel) {
		if(channel==null) throw new IllegalArgumentException("The passed channel was null", new Throwable());
		ChannelJsonResponsePair pair = ChannelJsonResponsePair.getChannelJsonResponsePair(channel);
		if(subscribedChannels.remove(pair)) {
			pair.decrSubs();
		}		
		if(subscribedChannels.size()<1) {
			subscriptions.remove(metricURI);
		}
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.group.ChannelGroupFutureListener#operationComplete(org.jboss.netty.channel.group.ChannelGroupFuture)
	 */
	@Override
	public void operationComplete(ChannelGroupFuture future) throws Exception {
		
	}

}

/**
 * <p>Title: ChannelJsonResponsePair</p>
 * <p>Description: A wrapper for a channel and json reponse pair so that responses can be formatted correctly for each channel</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.dataservice.json.catalog.MetricURISubscription.ChannelJsonResponsePair</code></p>
 */
@SuppressWarnings("javadoc")
class ChannelJsonResponsePair implements Channel {
	/** The wrapped channel */
	private final Channel channel;
		
	/** The wrapped channel's jsonResponse */
	private final JsonResponse response;
	/** The number of subscriptions this pair is attached to */
	private final AtomicInteger subscriptions = new AtomicInteger(0);
	/** A map of existing ChannelJsonResponsePairs keyed by the inner channel */
	protected static final Map<Channel, ChannelJsonResponsePair> instances = new ConcurrentHashMap<Channel, ChannelJsonResponsePair>();
	
	
	/**
	 * Returns the ChannelJsonResponsePair for the passed channel or null if one does not exist
	 * @param channel the inner channel
	 * @return the created ChannelJsonResponsePair
	 */
	public static ChannelJsonResponsePair getChannelJsonResponsePair(Channel channel) {
		return instances.get(channel);
	}
	

	/**
	 * Returns the ChannelJsonResponsePair for the passed channel
	 * @param channel the inner channel
	 * @param response the channel's JsonResponse
	 * @return the created ChannelJsonResponsePair
	 */
	public static ChannelJsonResponsePair getChannelJsonResponsePair(Channel channel, JsonResponse response) {
		ChannelJsonResponsePair pair = instances.get(channel);
		if(pair==null) {
			synchronized(instances) {
				pair = instances.get(channel);
				if(pair==null) {
					pair = new ChannelJsonResponsePair(channel, response);
					instances.put(channel, pair);
				}
			}
		}
		return pair;
	}
	
	/**
	 * Increments the number of subscriptions this pair is attached to and returns the new total
	 * @return the number of subscriptions this pair is attached to
	 */
	public int incrSubs() {
		return subscriptions.incrementAndGet();
	}
	
	/**
	 * Decrements the number of subscriptions this pair is attached to and returns the new total.
	 * If the subscription count is decremented below zero, the pair is removed from the {@link #instances} cache. 
	 * @return the number of subscriptions this pair is attached to
	 */
	public int decrSubs() {
		int remaining =  subscriptions.decrementAndGet();
		if(remaining<1) {
			instances.remove(channel);
		}
		return remaining;
	}
	
	/**
	 * Returns the number of subscriptions this pair is attached to
	 * @return the number of subscriptions this pair is attached to
	 */
	public int subs() {
		return subscriptions.get();
	}
	
	
	
	/**
	 * Returns the inner channel
	 * @return the inner channel
	 */
	public Channel getChannel() {
		return channel;
	}

	/**
	 * Returns the json response
	 * @return the json response
	 */
	public JsonResponse getResponse() {
		return response;
	}

	/**
	 * Creates a new ChannelJsonResponsePair
	 * @param channel the inner channel
	 * @param response the channel's JsonResponse
	 */
	private ChannelJsonResponsePair(Channel channel, JsonResponse response) {		
		this.channel = channel;
		this.response = response;
	}

	/**
	 * @param o
	 * @return
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */	
	@Override
	public int compareTo(Channel o) {
		return channel.compareTo(o);
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#getId()
	 */
	@Override
	public Integer getId() {
		return channel.getId();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#getFactory()
	 */
	@Override
	public ChannelFactory getFactory() {
		return channel.getFactory();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#getParent()
	 */
	@Override
	public Channel getParent() {
		return channel.getParent();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#getConfig()
	 */
	@Override
	public ChannelConfig getConfig() {
		return channel.getConfig();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#getPipeline()
	 */
	@Override
	public ChannelPipeline getPipeline() {
		return channel.getPipeline();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#isOpen()
	 */
	@Override
	public boolean isOpen() {
		return channel.isOpen();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#isBound()
	 */
	@Override
	public boolean isBound() {
		return channel.isBound();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#isConnected()
	 */
	@Override
	public boolean isConnected() {
		return channel.isConnected();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#getLocalAddress()
	 */
	@Override
	public SocketAddress getLocalAddress() {
		return channel.getLocalAddress();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#getRemoteAddress()
	 */
	@Override
	public SocketAddress getRemoteAddress() {
		return channel.getRemoteAddress();
	}

	/**
	 * @param message
	 * @return
	 * @see org.jboss.netty.channel.Channel#write(java.lang.Object)
	 */
	@Override
	public ChannelFuture write(Object message) {
		return channel.write(response.setContent(message));
	}
	
	/**
	 * Writes a JSON response to a subscribing agent with an op code
	 * @param message The payload to write
	 * @param opCode The op code of the response
	 * @return the channel future of the write
	 */
	public ChannelFuture write(Object message, OpCode opCode) {
		return channel.write(response.clone().setOpCode(opCode).setContent(message), channel.getRemoteAddress());
	}
	

	/**
	 * @param message
	 * @param remoteAddress
	 * @return
	 * @see org.jboss.netty.channel.Channel#write(java.lang.Object, java.net.SocketAddress)
	 */
	@Override
	public ChannelFuture write(Object message, SocketAddress remoteAddress) {
		return channel.write(response.setContent(message), remoteAddress);
	}
	
	/**
	 * Writes a JSON response to a subscribing agent with an op code
	 * @param message The payload to write
	 * @param remoteAddress The remote address
	 * @param opCode The op code of the response
	 * @return the channel future of the write
	 */
	public ChannelFuture write(Object message, SocketAddress remoteAddress, OpCode opCode) {
		return channel.write(response.clone().setOpCode(opCode).setContent(message), remoteAddress);
	}
	

	/**
	 * @param localAddress
	 * @return
	 * @see org.jboss.netty.channel.Channel#bind(java.net.SocketAddress)
	 */
	@Override
	public ChannelFuture bind(SocketAddress localAddress) {
		return channel.bind(localAddress);
	}

	/**
	 * @param remoteAddress
	 * @return
	 * @see org.jboss.netty.channel.Channel#connect(java.net.SocketAddress)
	 */
	@Override
	public ChannelFuture connect(SocketAddress remoteAddress) {
		return channel.connect(remoteAddress);
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#disconnect()
	 */
	@Override
	public ChannelFuture disconnect() {
		return channel.disconnect();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#unbind()
	 */
	@Override
	public ChannelFuture unbind() {
		return channel.unbind();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#close()
	 */
	@Override
	public ChannelFuture close() {
		instances.remove(channel);
		return channel.close();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#getCloseFuture()
	 */
	@Override
	public ChannelFuture getCloseFuture() {
		return channel.getCloseFuture();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#getInterestOps()
	 */
	@Override
	public int getInterestOps() {
		return channel.getInterestOps();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#isReadable()
	 */
	@Override
	public boolean isReadable() {
		return channel.isReadable();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#isWritable()
	 */
	@Override
	public boolean isWritable() {
		return channel.isWritable();
	}

	/**
	 * @param interestOps
	 * @return
	 * @see org.jboss.netty.channel.Channel#setInterestOps(int)
	 */
	@Override
	public ChannelFuture setInterestOps(int interestOps) {
		return channel.setInterestOps(interestOps);
	}

	/**
	 * @param readable
	 * @return
	 * @see org.jboss.netty.channel.Channel#setReadable(boolean)
	 */
	@Override
	public ChannelFuture setReadable(boolean readable) {
		return channel.setReadable(readable);
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#getAttachment()
	 */
	@Override
	public Object getAttachment() {
		return channel.getAttachment();
	}

	/**
	 * @param attachment
	 * @see org.jboss.netty.channel.Channel#setAttachment(java.lang.Object)
	 */
	@Override
	public void setAttachment(Object attachment) {
		channel.setAttachment(attachment);
	}
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((channel == null) ? 0 : channel.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ChannelJsonResponsePair other = (ChannelJsonResponsePair) obj;
		if (channel == null) {
			if (other.channel != null) {
				return false;
			}
		} else if (!channel.equals(other.channel)) {
			return false;
		}
		return true;
	}
	
}
