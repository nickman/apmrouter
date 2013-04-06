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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MXBean;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.cliffc.high_scale_lib.Counter;
import org.cliffc.high_scale_lib.NonBlockingHashMapLong;
import org.helios.apmrouter.OpCode;
import org.helios.apmrouter.catalog.EntryStatus;
import org.helios.apmrouter.catalog.domain.Metric;
import org.helios.apmrouter.collections.ConcurrentLongSortedSet;
import org.helios.apmrouter.dataservice.json.JsonResponse;
import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.metric.MetricType;
import org.helios.apmrouter.metric.catalog.ICEMetricCatalog;
import org.helios.apmrouter.metric.catalog.IMetricCatalog;
import org.helios.apmrouter.subscription.MetricURIEvent;
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
 * <p>Handles metric state changes against current and candidate (potential) subscriptions.
 * In essence, we want <b>NEW METRIC</b> and <b>METRIC STATE CHANGE</b> events, which are relatively infrequent,
 * should keep {@link MetricURISubscription} instances up to date with respect to their {@link #metricIds} metric set
 * so that the much more frequent <b>METRIC DATA</b> events can be rapidly processed by simply determining if:
 * <ol>
 * 	<li>the subscription's subscription type includes data updates (a one byte bit mask)</li>
 *  <li>the subscription's {@link #metricIds} set contains the ID of the incoming data event (a lookup of a long in a {@link ConcurrentLongSortedSet}.</li>
 * </ol>. Both operations should evaluate quickly enough, so the remaining challenge of this hotspot is keeping the subscription's membership correctly up to date.</p>
 * <p>To recap the dimensions of an update triggered by a metric state change event:<ul>
 * 	<li><b>metricId</b>: The <code>long</code> id of the metric that changed state (and the implied {@link IMetric#getFQN()} portions of the metric)</li>
 * 	<li><b>metricType</b>: The {@link MetricType} of the incoming metric</li>
 * 	<li><b>metricStatus</b>: A <code>byte</code> representing the new state ({@link EntryStatus}) of the metric.</li>
 * </ul> The dimensions of the state change event need to be bounced up against the dimensions of each subscription:<ul>
 * 	<li><b>metricIds</b>: The subscription's {@link #metricIds} set which contains the metric ids of the current membership, that is,
 *    metric ids for metrics that have been determined to be members based on the subscription's criteria.</li>
 * 	<li><b>subscriptionType</b>: A <code>byte</code> mask representing the subscription's interest types ({@link MetricURISubscriptionType}), which can be:<ul>
 * 		<li>New Metric Events</li>
 * 		<li>Metric State Change Events</li>
 * 		<li>Metric Live Data Events</li>
 * 	</ul> The metric membership (the metric id set) is initially populated when the subscription is initiated, and then updated as applicable <b>NEW METRIC</b> and <b>METRIC STATE CHANGE</b> events are applied.</li>
 * 	<li><b>metricType</b>: An <code>int</code> mask representing the types of metrics ({@link MetricType}s) the subscription is interested in.</li>
 * 	<li><b>stateChangeType</b>: A <code>byte</code> mask representing the subscription's interest in metric state change types ({@link EntryStatus})</li>
 * 	<li><b>metricName</b>: The subscription's [possibly wildcarded] expression defining the domain, host, agent, metric namespace and metric name.</li>
 * </ul> Of the subscription dimensions, we want to use all the dimensions before <b><code>metricName</code></b> to determine if the incoming metric is applicable to the subscriptions, since they
 * 	allow fast filtering (inclusion/exclusion) without querying the database to determine membership. Ultimately, however, if [non-]membership cannot be confirmed (and the metric is not already a member),
 * 	a database call must be made to determine this. This is referred to as <i>membershipResolution</i>.</p>
 * <p>The implications of a given <b>subscriptionType</b> that is different from a subscription's  <b>stateChangeType</b> subscription interest, does not exclude an event from updating the membership.
 * A subscription's <b>subscriptionType</b> <i>might not</i> register interest in <b>Metric State Change Events</b>, while at the same time specifying one or more
 *   metric <b>stateChangeType</b>s that it <i>is</i> interested in. The absence of a state-change-event interest in a subscription does not exempt it from processing metric state changes.
 *   The <b>stateChangeType</b> interest defines the states of metrics that the subscription is interested in, and when a member metric changes state, the subscription membership
 *   may be changed by a previously excluded metric entering, or a currently included metric becoming inelligible and exiting the membership set. In other words, all events need to be considered
 *   if they [might] affect a subscription's membership by triggering a member entry or exit. The distinguishing aspect of a <b>subscriptionType</b> that includes state change events is that it indicates
 *   that the subscriber wishes to be informed of metric state change events in its membership even if the event did not trigger an entry or an exit (a change in memership). A typical scenario for this type
 *   of subscription is a UI, like the console metric tree, that updates the visual attributes of a visualized metric when it changes state, even though the tree's membership has not changed.
 * </p>
 * This is a complicated update since there are multi-dimensional considerations.</p>
 * <p>Possible actions are a combination of:<ul>
 * 	<li>membershipResolution</li>
 * 	<li>removeMetricId</li>
 * 	<li>addMetricId</li>
 * 	<li>sendEntry</li>
 * 	<li>sendExit</li>
 * 	<li>sendStateChange</li>
 * </ul></p>
 * <table border='1'>
 * <tr colspan='3'><th>&nbsp;</th><th>Has Metric ID</th><th>No Metric ID</th></tr>
 * <tr colspan='3'><th>State Change On Sub</th><td>
 * 		<table border='1'>
 *		<tr colspan='3'><th>HAS MET/HAS SUB: No membershipResolution needed.</th><th>Metric Not Member</th><th>Metric is Member</th></tr>
 * 		<tr colspan='3'><th>State Interest</th><td>N/A. Has metricId</td><td>Send state change.</td></tr>
 *	  	<tr colspan='3'><th>No State Interest</th><td>N/A. Has metricId</td><td>No state interest. Remove from group. Send exit.</td></tr>
 *	 	</table>
 * </td><td>
 * 		<table border='1'>
 * 		<tr colspan='3'><th>NO MET/HAS SUB: Run membershipResolution</th><th>Metric Not Member</th><th>Metric is Member</th></tr>
 * 		<tr colspan='3'><th>State Interest</th><td>Not a member. Nothing.</td><td>Add to group. Send enty.</td></tr>
 * 		<tr colspan='3'><th>No State Interest</th><td>Not a member. Nothing.</td><td>Not interested in this state. Nothing.</td></tr>
 * 		</table>
 * </td></tr>
 * <tr colspan='3'><th>No State Change Sub</th><td>
 * 		<table border='1'>
 *		<tr colspan='3'><th>HAS MET/NO SUB: No membershipResolution needed.</th><th>Metric Not Member</th><th>Metric is Member</th></tr>
 * 		<tr colspan='3'><th>State Interest</th><td>N/A. Has metricId</td><td>Not interested in state changes. Nothing.</td></tr>
 *	  	<tr colspan='3'><th>No State Interest</th><td>N/A. Has metricId</td><td>No state interest. Remove from group. Send exit.</td></tr>
 * 		</table> 
 * </td><td>
 * 		<table border='1'>
 * 		<tr colspan='3'><th>NO MET/NO SUB: Run membershipResolution</th><th>Metric Not Member</th><th>Metric is Member</th></tr>
 * 		<tr colspan='3'><th>State Interest</th><td>Not a member. Nothing.</td><td>Add to group. Send enty.</td></tr>
 * 		<tr colspan='3'><th>No State Interest</th><td>Not a member. Nothing.</td><td>Not interested in this state. Nothing.</td></tr>
 * 		</table>
 * </td></tr>
 * </table>
 */
@MXBean
public class MetricURISubscription implements ChannelGroupFutureListener, MetricURISubscriptionMBean {
	/** The subscribed metricURI */
	protected final MetricURI metricURI;
	
	/** The metric ids that have been determined to match the metric URI */
	protected final ConcurrentLongSortedSet metricIds = new ConcurrentLongSortedSet(128);
	/** The channels subscribed to this metric URI */
	protected final ChannelGroup subscribedChannels = new DefaultChannelGroup(getClass().getSimpleName());
	
	/** The metric catalog */
	protected static final IMetricCatalog metricCatalog = ICEMetricCatalog.getInstance();
	
	/** A map of {@link MetricURISubscription}s keyed by the subscription's {@link MetricURI} */
	protected static final ConcurrentHashMap<MetricURI, MetricURISubscription> subscriptions = new ConcurrentHashMap<MetricURI, MetricURISubscription>(128, 0.75f, 16);
	
	/** A list of MetricURIBitMaskContainers */
	protected static final List<MetricURIBitMaskContainer> subscriptionsByBitMask = new CopyOnWriteArrayList<MetricURIBitMaskContainer>();
	
	/** A map representing the superset of subscribed-to metric ids as keys and a set of subscribers as the value */
	protected static final NonBlockingHashMapLong<Set<MetricURISubscription>> metricIdSuperSet = new NonBlockingHashMapLong<Set<MetricURISubscription>>(false); 
	

	
	/** Static class logger */
	protected static final Logger LOG = Logger.getLogger(MetricURISubscription.class);
	
	
	
	/**
	 * Returns an iterator over the subscriptions keyed for the passed type/status/subType mask, or null if there are none.
	 * @param mask The type/status/subType mask to get subscriptions for
	 * @return a subscription iterator or null
	 */
	public static Iterator<MetricURISubscription> getSubscriptionsForSubType(long mask) {
		int index = subscriptionsByBitMask.indexOf(mask);
		if(index==-1) return null;
		MetricURIBitMaskContainer container = subscriptionsByBitMask.get(index);
		return Collections.unmodifiableSet(container.getValue()).iterator();
	}
	
	/**
	 * Returns an iterator over the subscriptions with a matching type/status/subType mask, or null if there are none.
	 * @param mask The type/status/subType mask to get subscriptions that have a bit mask matching type/status/subType mask
	 * @return a subscription iterator or null
	 */
	public static Iterator<MetricURISubscription> getMatchingSubscriptions(long mask) {
		MetricURIBitMaskContainer key = MetricURIBitMaskContainer.Key(mask);
		int index = subscriptionsByBitMask.indexOf(key);
		if(index==-1) return null;
		MetricURIBitMaskContainer container = subscriptionsByBitMask.get(index);
		return Collections.unmodifiableSet(container.getValue()).iterator();		
	}
	
	
	
	/**
	 * Returns a set of MetricURISubscriptions that are subscribed to the passed metric id 
	 * @param id The metric id
	 * @return a set of MetricURISubscriptions or null if there were no subscriptions for the passed id 
	 */
	public static Set<MetricURISubscription> getSubscriptionsForMetric(long id) {
		return metricIdSuperSet.get(id);		
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
	 * Returns an iterator of {@link MetricURISubscription}s that have bit mask matches
	 * @param metricTypeMask The mask of the {@link MetricType} id to match
	 * @param stateChangeTypeMask The mask of the {@link EntryStatus} id to match
	 * @param subscriptionEventTypeMask The mask of the {@link MetricURISubscriptionType} id to match
	 * @return an iterator of matching {@link MetricURISubscription}s or null if there are no matches
	 */
	public static Iterator<MetricURISubscription> getMatchingSubscriptions(int metricTypeMask, byte stateChangeTypeMask, byte subscriptionEventTypeMask) {
		return getMatchingSubscriptions(MetricURI.mask(metricTypeMask, stateChangeTypeMask, subscriptionEventTypeMask));
	}
	
	
	/**
	 * @return the number of broadcasts as a result of this state change
	 */
	public static int processMetricStateChange() {
		return -1;
	}
	
	
	
	
	/*
	 * MetricType Mask
	 * State Change Event
	 * Subscription Type
	 * 
	 */
	
	/**
	 * Determines if the passed metricId is already in this subscription's set
	 * @param metricId the metric id to test for 
	 * @return true if the passed metricId is already in this subscription's set, false otherwise
	 */
	public boolean hasMetricId(long metricId) {
		return metricIds.contains(metricId);
	}
	
	/**
	 * Adds the passed metric Id to this subscription's metric ID set
	 * @param metricId the metric id to add
	 */
	public void addMetricId(long metricId) {
		metricIds.add(metricId);
		Set<MetricURISubscription> subs = metricIdSuperSet.get(metricId);
		if(subs==null) {
			synchronized(metricIdSuperSet) {
				subs = metricIdSuperSet.get(metricId);
				if(subs==null) {
					subs = new CopyOnWriteArraySet<MetricURISubscription>();
					metricIdSuperSet.put(metricId, subs);
				}
			}
		}
		subs.add(this);
	}
	
	/**
	 * Adds the metric ids of the passed metrics to this subscriptions metricId tracking
	 * @param metrics the metrics initially determined to be associated to this subscription
	 */
	void addMetricIds(Collection<Metric> metrics) {
		for(Metric metric: metrics) {
			// FIXME: Optimize this:
			//metricIds.add(metric.getMetricId());
			addMetricId(metric.getMetricId());
		}
	}
	
	
	/**
	 * Removes the passed metric Id from the subscription's metric ID set
	 * @param metricId the metric id to remove
	 */
	public void removeMetricId(long metricId) {
		metricIds.remove(metricId);
		Set<MetricURISubscription> subs = metricIdSuperSet.get(metricId);
		if(subs!=null) {
			synchronized(subs) {
				subs.remove(this);
				if(subs.isEmpty()) {
					metricIdSuperSet.remove(metricId);
				}
			}
		}
	}
	
	/**
	 * Determines if the passed metric id needs to be added to this subscription
	 * @param metricId the metric ID to test for
	 * @param catalogDataSource the catalog datasource
	 * @return true if the passed metric id needs to be added to this subscription, false otherwise
	 */
	public boolean resolveMembership(long metricId, DataSource catalogDataSource ) {
		if(metricIds.contains(metricId)) return false;
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rset = null;
		try {
			conn = catalogDataSource.getConnection();
			ps = conn.prepareStatement(this.metricURI.metricIdSql + " AND METRIC_ID = ?");
			ps.setLong(1, metricId);
			rset = ps.executeQuery();
			return rset.next();
		} catch (Exception ex) {
			LOG.error("Failed to verify candidacy for new metric", ex);
			return false;
		} finally {
			if(rset!=null) try { rset.close(); } catch (Exception x) {/* No Op */}
			if(ps!=null) try { ps.close(); } catch (Exception x) {/* No Op */}
			if(conn!=null) try { conn.close(); } catch (Exception x) {/* No Op */}
		}
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
					int index = subscriptionsByBitMask.indexOf(metricUri.getMetricTypeStatusSubTypeMask());
					MetricURIBitMaskContainer subsContainer = null;
					if(index==-1) {
						subsContainer = new MetricURIBitMaskContainer(metricUri.getMetricTypeStatusSubTypeMask());						
						subscriptionsByBitMask.add(subsContainer);
					} else {
						subsContainer = subscriptionsByBitMask.get(index);
					}
					subsContainer.getValue().add(metricUriSub);
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
			//sub.handleNewMetric(metricId, fQN, newRow, ds);
		}
	}
	
	public static Set<MetricURISubscription> stateChangeCandidates(long metricId, byte newState) {
		if(subscriptions.isEmpty()) return Collections.emptySet();
		Set<MetricURISubscription> candidates = new HashSet<MetricURISubscription>();
		for(MetricURISubscription sub: subscriptions.values()) {
			
		}
		return candidates;
	}
	
//	/**
//	 * Called when the MetricURISubscriptionService is notified of a new metric.
//	 * @param metricId The id of the metric
//	 * @param fQN The fully qualified metric name of the new metric
//	 * @param newRow The attributes of the new metric 
//	 * @param ds The catalog data source in case we need to query the catalog
//	 */
//	protected void handleNewMetric(long metricId, String fQN, Object[] newRow, DataSource ds) {
//		if(!metricIds.contains(metricId)) {
//			Connection conn  = null;
//			Statement st = null;
//			ResultSet rset = null;
//			boolean process = false;
//			try {
//				conn = ds.getConnection();
//				st = conn.createStatement();
//				rset = st.executeQuery(metricURI.metricIdSql + " AND M.METRIC_ID = " + metricId);
//				process = rset.next();
//				rset.close(); rset = null; st.close(); st = null; conn.close(); conn = null;
//				if(process) {
//					sendSubscribersNewMetric(newRow);
//				}
//			} catch (Exception ex) {
//				
//			} finally {
//				if(rset != null) try { rset.close(); } catch (Exception x) {/* No Op */};
//				if(st!= null) try { st.close(); } catch (Exception x) {/* No Op */};
//				if(conn != null) try { conn.close(); } catch (Exception x) {/* No Op */};
//			}
//			
//		}
//	}
	
	/**
	 * Called when a new metric comes in the door and is determined to be a member of this metric uri, but not in the set already.
	 * @param metric The new metric 
	 */
	protected void sendSubscribersNewMetric(Metric metric) {
		for(Channel channel: subscribedChannels) {
			((ChannelJsonResponsePair)channel).write(metric, MetricURIEvent.NEW_METRIC.getEventName(), OpCode.ON_METRIC_URI_EVENT);
		}
	}
	
//	/**
//	 * Called when metric changes state and is determined to be a member of this metric uri
//	 * @param newStateName The name of the metric's new state
//	 * @param metric the metric in its new state
//	 */
//	protected void sendSubscribersMetricStateChange(String newStateName, Metric metric) {
//		String type = MetricTrigger.STATE_CHANGE_METRIC_EVENT + "." + newStateName;
//		for(Channel channel: subscribedChannels) {
//			((ChannelJsonResponsePair)channel).write(metric, type, OpCode.ON_METRIC_URI_EVENT);
//		}		
//	}
	
	/**
	 * Sends subscribers a notification that a metric changed state and entered this subscription
	 * @param metric The metric that entered the subscription due to a state change
	 */
	protected void sendSubscribersEntryMetric(Metric metric) {
		for(Channel channel: subscribedChannels) {
			((ChannelJsonResponsePair)channel).write(metric, MetricURIEvent.STATE_CHANGE_ENTRY.getEventName(), OpCode.ON_METRIC_URI_EVENT);
		}
	}
	
	/**
	 * Sends a metric-data update to all subscribed channels
	 * @param event The real-time data event 
	 */
	protected void sendRealTimeDataEvent(Object[] event) {
		for(Channel channel: subscribedChannels) {
			((ChannelJsonResponsePair)channel).write(event, MetricURIEvent.DATA.getEventName(), OpCode.ON_METRIC_URI_EVENT);
		}
	}
	
	
	/**
	 * Sends the subscriber a metric exit when a state change causes a metric to be removed from the metric id set
	 * @param metricId The metric id to remove
	 */
	protected void sendSubscribersExitMetric(long metricId) {
		for(Channel channel: subscribedChannels) {
			((ChannelJsonResponsePair)channel).write(metricId, MetricURIEvent.STATE_CHANGE_EXIT.getEventName(), OpCode.ON_METRIC_URI_EVENT);
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
	 * Determines if this subscription would still be interested in a metric when it transitions to the passed state
	 * @param entryState The state to test interest for
	 * @return true if interested, false if not 
	 */
	public boolean isInterestedInState(byte entryState) {
		return (metricURI.metricStatusMask | entryState)==metricURI.metricStatusMask; 
	}
	
	/**
	 * Determines if this subscription wants to be notified of non-membership change status transitions
	 * @return true if it is, false otherwise
	 */
	public boolean isInterestedInStateChanges() {
		return MetricURISubscriptionType.STATE_CHANGE.isEnabled(metricURI.subscriptionType);
	}
	
	/**
	 * Sends a state change event to subscribers that have a metric that transitioned to a non-terminal state in their membership
	 * @param metricId The id of the metric
	 * @param status the new status of the metric
	 */
	public void sendStateChangeEvent(long metricId, EntryStatus status) {
		String message = new StringBuilder().append(metricId).append(":").append(status.name()).toString(); 
		for(Channel channel: subscribedChannels) {
			((ChannelJsonResponsePair)channel).write(message , MetricURIEvent.STATE_CHANGE.getEventName(), OpCode.ON_METRIC_URI_EVENT);
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
	 * @param type the type of the new json response
	 * @param opCode The op code of the response
	 * @return the channel future of the write
	 */
	public ChannelFuture write(Object message, String type, OpCode opCode) {
		return channel.write(response.clone(type).setOpCode(opCode).setContent(message), channel.getRemoteAddress());
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
