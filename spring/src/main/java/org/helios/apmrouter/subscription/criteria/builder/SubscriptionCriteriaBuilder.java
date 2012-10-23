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
package org.helios.apmrouter.subscription.criteria.builder;

import org.helios.apmrouter.subscription.criteria.SubscriptionCriteria;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * <p>Title: SubscriptionCriteriaBuilder</p>
 * <p>Description: Defines a builder class for specific types of {@link SubscriptionCriteria}. Typically, there will be one defined class of this type per criteria type.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.subscription.criteria.SubscriptionCriteriaBuilder</code></p>
 * @param <S> The expected type of the source instance identifier/locator
 * @param <F> The expected type of the filter expression
 * @param <E> The expected type of the extended filter expression
 */

public interface SubscriptionCriteriaBuilder<S, F, E> {
	/** The json key for the event source */
	public static final String JSON_EVENT_SOURCE = "es";
	/** The json key for the filter */
	public static final String JSON_EVENT_FILTER = "fil";
	/** The json key for the extended filter */
	public static final String JSON_EXTENDED_EVENT_FILTER = "efil";
	
	/**
	 * Builds a typed {@link SubscriptionCriteria} from the passed arguments. 
	 * @param subRequest The subscripton request
	 * @return a typed {@link SubscriptionCriteria}
	 * @throws JSONException thrown on any JSON unmarshalling error
	 */
	public SubscriptionCriteria<S, F, E> build(JSONObject subRequest) throws JSONException;
	
}
