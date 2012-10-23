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
package org.helios.apmrouter.subscription.criteria;

import java.util.concurrent.TimeUnit;

/**
 * <p>Title: PollingSubscriptionCriteria</p>
 * <p>Description: An extended {@link SubscriptionCriteria} that supports timing specifications to support polling to generate events in situations where callbacks are not available automatically.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.subscription.criteria.PollingSubscriptionCriteria</code></p>
 * @param <S> The expected type of the source instance identifier/locator
 * @param <F> The expected type of the selection expression
 * @param <E> The expected type of the extended selection expression
 */

public interface PollingSubscriptionCriteria<S, F, E> extends SubscriptionCriteria<S, F, E> {
	/**
	 * The frequency of the polling action
	 * @param time The frequency time period
	 * @param unit The unit of the time
	 */
	public void setPollingFrequence(long time, TimeUnit unit);
	
	
}
