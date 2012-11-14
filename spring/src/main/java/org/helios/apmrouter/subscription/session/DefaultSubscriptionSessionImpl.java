/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2012, Helios Development Group and individual contributors
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
package org.helios.apmrouter.subscription.session;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.helios.apmrouter.dataservice.json.JsonRequest;
import org.helios.apmrouter.dataservice.json.JsonResponse;
import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.server.ServerComponentBean;
import org.helios.apmrouter.subscription.SubscriptionService;
import org.helios.apmrouter.subscription.criteria.FailedCriteriaResolutionException;
import org.helios.apmrouter.subscription.criteria.RecoverableFailedCriteriaResolutionException;
import org.helios.apmrouter.subscription.criteria.SubscriptionCriteria;
import org.helios.apmrouter.subscription.criteria.SubscriptionCriteriaInstance;

/**
 * <p>Title: DefaultSubscriptionSessionImpl</p>
 * <p>Description: The default {@link SubscriptionSession} implementation.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.subscription.session.DefaultSubscriptionSessionImpl</code></p>
 */
public class DefaultSubscriptionSessionImpl extends ServerComponentBean implements SubscriptionSession {
	/** A reference to the subscription service */
	protected final SubscriptionService subscriptionService;
	/** The session's subscriber channel */
	protected final SubscriberChannel subscriberChannel;
	/** The registered criteria */
	protected final Set<SubscriptionCriteria<?,?,?>> criteria = new CopyOnWriteArraySet<SubscriptionCriteria<?,?,?>>();
	/** The resolved criteria keyed by the criteria Id.*/
	protected final Map<Long, SubscriptionCriteriaInstance<?>> resolvedCriteria = new ConcurrentHashMap<Long, SubscriptionCriteriaInstance<?>>();
	
	
	/**
	 * Creates a new DefaultSubscriptionSessionImpl
	 * @param subscriptionService A reference to the subscription service
	 * @param subscriberChannel The session's subscriber channel
	 */
	public DefaultSubscriptionSessionImpl(SubscriptionService subscriptionService, SubscriberChannel subscriberChannel) {
		super();
		this.subscriptionService = subscriptionService;
		objectName = JMXHelper.objectName(new StringBuilder(getClass().getPackage().getName())
			.append(":service=SubscriptionSession,")
			.append("subscriber=").append(subscriberChannel.getSubscriberId())
		);
		this.subscriberChannel = subscriberChannel;
		this.subscriberChannel.setSubscriptionSession(this);					
	}



	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.subscription.session.SubscriptionSession#terminate()
	 */
	@Override
	public void terminate() {
		for(SubscriptionCriteriaInstance<?> sci: resolvedCriteria.values()) {
			sci.terminate();
		}
		resolvedCriteria.clear();
		criteria.clear();
	}
	
	
	/**
	 * Sends a {@link JsonResponse} to the subscriber
	 * @param response the {@link JsonResponse} to send
	 */
	public void send(JsonResponse response) {
		subscriberChannel.send(response);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.subscription.session.SubscriptionSession#addCriteria(org.helios.apmrouter.subscription.criteria.SubscriptionCriteria, org.helios.apmrouter.subscription.session.SubscriptionSession, org.helios.apmrouter.dataservice.json.JsonRequest)
	 */
	@Override
	public long addCriteria(SubscriptionCriteria<?,?,?> criteria, SubscriptionSession session, JsonRequest request) throws FailedCriteriaResolutionException {
		if(criteria==null) throw new IllegalArgumentException("The passed criteria was null", new Throwable());
		this.criteria.add(criteria);
		try {
			SubscriptionCriteriaInstance<?> sci = criteria.instantiate(request);
			sci.resolve(session);
			resolvedCriteria.put(sci.getCriteriaId(), sci);			
			session.send(request.response().setContent(sci.getCriteriaId()));
			sendSubStarted(sci);
			return sci.getCriteriaId();
		} catch (FailedCriteriaResolutionException fce) {
			if(!(fce instanceof RecoverableFailedCriteriaResolutionException)) {
				this.criteria.remove(criteria);
			}
			throw fce;
		}
	}
	
	/**
	 * Cancels the subscription criteria with the passed id.
	 * @param criteriaId The id of the c=subscription criteria to cancel
	 */
	public SubscriptionCriteria<?,?,?> cancelCriteria(long criteriaId) {
		SubscriptionCriteriaInstance<?> sci = resolvedCriteria.remove(criteriaId);
		if(sci!=null) {
			sci.terminate();
			criteria.remove(sci.getSubscriptionCriteria());
			sendSubStopped(sci);
			return sci.getSubscriptionCriteria();
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.subscription.session.SubscriptionSession#getSubscriptionSessionId()
	 */
	@Override
	public long getSubscriptionSessionId() {
		return subscriberChannel.getSubscriberId();
	}
	
	/**
	 * Sends a subscription started notification
	 * @param criteria The criteria for which the subscription was started
	 */
	public void sendSubStarted(SubscriptionCriteriaInstance<?> criteria) {
		subscriptionService.sendSubStarted(criteria);
	
	}
	
	/**
	 * Sends a subscription stopped notification
	 * @param criteria The criteria for which the subscription was stopped
	 */
	public void sendSubStopped(SubscriptionCriteriaInstance<?> criteria) {
		subscriptionService.sendSubStopped(criteria);
	}
	
	

}
