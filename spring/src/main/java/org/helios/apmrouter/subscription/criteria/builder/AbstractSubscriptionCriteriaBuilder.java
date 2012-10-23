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
package org.helios.apmrouter.subscription.criteria.builder;

import org.helios.apmrouter.server.ServerComponentBean;
import org.helios.apmrouter.subscription.criteria.SubscriptionCriteria;

/**
 * <p>Title: AbstractSubscriptionCriteriaBuilder</p>
 * <p>Description: Abstract base class for {@link SubscriptionCriteriaBuilder} concrete impls. Handles publishing the lifecycle events. </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.subscription.criteria.builder.AbstractSubscriptionCriteriaBuilder</code></p>
 * @param <S> The expected type of the source instance identifier/locator
 * @param <F> The expected type of the filter expression
 * @param <E> The expected type of the extended filter expression
 */

public abstract class AbstractSubscriptionCriteriaBuilder<S, F, E> extends ServerComponentBean implements SubscriptionCriteriaBuilder<S, F, E> {
	
	/**
	 * <p>Calls super{@link #doStart()} and then publishes a {@link SubscriptionCriteriaBuilderStartedEvent} to the application context.
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStart()
	 */
	protected void doStart() throws Exception {
		super.doStart();
		applicationContext.publishEvent(new SubscriptionCriteriaBuilderStartedEvent(this, beanName));
	}
	
	/**
	 * <p>Publishes a {@link SubscriptionCriteriaBuilderStartedEvent} to the application context and then calls super{@link #doStop()} .
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStart()
	 */
	protected void doStop() {		
		applicationContext.publishEvent(new SubscriptionCriteriaBuilderStoppedEvent(this, beanName));
		super.doStop();
	}
	

}
