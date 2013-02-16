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
package org.helios.apmrouter.subscription.criteria;

import org.helios.apmrouter.subscription.session.SubscriptionSession;

/**
 * <p>Title: SubscriptionCriteriaInstance</p>
 * <p>Description: A resolved and activatable container for a specific {@link SubscriptionCriteria}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.subscription.criteria.SubscriptionCriteriaInstance</code></p>
 * @param <T> The expected type of the event emitted by this criteria instance.
 */

public interface SubscriptionCriteriaInstance<T> {
	/**
	 * Directs the instance to resolve its criteria and activate
	 * @param session The subscription session managing this instance
	 * @throws FailedCriteriaResolutionException thrown if criteria cannot be resolved
	 */
	public void resolve(SubscriptionSession session) throws FailedCriteriaResolutionException;
	
	/**
	 * Terminates the instance and de-allocates all resources.
	 */
	public void terminate();
	
	/**
	 * Returns the Id of the resolved criteria instance
	 * @return the Id of the resolved criteria instance
	 */
	public long getCriteriaId();
	
	/**
	 * Returns the subscription criteria that this instance was resolved from
	 * @return the subscription criteria that this instance was resolved from
	 */
	public SubscriptionCriteria<?,?,?> getSubscriptionCriteria();
	
	/**
	 * An optional key of arbitrary type so subscription start/stop listeners can determine what the subscriber is looking for
	 * @return The subscription key, or null if the criteria does not support this
	 */
	public Object getSubcriptionKey();
}
