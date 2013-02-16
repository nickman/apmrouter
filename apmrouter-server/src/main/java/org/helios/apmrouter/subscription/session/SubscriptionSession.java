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

import org.helios.apmrouter.dataservice.json.JsonRequest;
import org.helios.apmrouter.dataservice.json.JsonResponse;
import org.helios.apmrouter.subscription.criteria.FailedCriteriaResolutionException;
import org.helios.apmrouter.subscription.criteria.SubscriptionCriteria;
import org.helios.apmrouter.subscription.criteria.SubscriptionCriteriaInstance;

/**
 * <p>Title: SubscriptionSession</p>
 * <p>Description: A temporal manager of a set of {@link SubscriptionCriteriaInstance}s and the linkage to the actual subscriber.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.subscription.criteria.SubscriptionSession</code></p>
 */

public interface SubscriptionSession {
	/**
	 * Terminates this session, causing all the subsidiary subscription criteria instances to be closed.
	 */
	public void terminate();
	
	/**
	 * Returns the unique ID for this session
	 * @return the unique ID for this session
	 */
	public long getSubscriptionSessionId();
	
	/**
	 * Sends a {@link JsonResponse} to the subscriber
	 * @param response the {@link JsonResponse} to send
	 */
	public void send(JsonResponse response);
	
	
	/**
	 * Cancels the subscription criteria with the passed id.
	 * @param criteriaId The id of the c=subscription criteria to cancel
	 * @return The canceled criteria or null if it was not found
	 */
	public SubscriptionCriteria<?,?,?> cancelCriteria(long criteriaId);
	
	/**
	 * Adds a new subscription to the subscription session
	 * @param criteria The subscription criteria
	 * @param session The subscription session the criteria is being added for
	 * @param request The original json request
	 * @return the assigned Id for the created subscription criteria, or the id of the existing criteria if the same instance has alreay been registered.
	 * @throws FailedCriteriaResolutionException thrown if the criteria cannot be resolved
	 */
	public long addCriteria(SubscriptionCriteria<?,?,?> criteria, SubscriptionSession session, JsonRequest request) throws FailedCriteriaResolutionException;
	
	/**
	 * Sends a subscription started notification
	 * @param criteria The criteria for which the subscription was started
	 */
	public void sendSubStarted(SubscriptionCriteriaInstance<?> criteria);
	
	/**
	 * Sends a subscription stopped notification
	 * @param criteria The criteria for which the subscription was stopped
	 */
	public void sendSubStopped(SubscriptionCriteriaInstance<?> criteria);
	
	/**
	 * Cancels all the subscriptions for this session
	 */
	public void cancelAll();	
	
}
