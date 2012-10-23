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

import org.springframework.context.ApplicationEvent;

/**
 * <p>Title: SubscriptionCriteriaBuilderStartedEvent</p>
 * <p>Description: An application event published by a {@link SubscriptionCriteriaBuilder} when it starts</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.subscription.criteria.builder.SubscriptionCriteriaBuilderStartedEvent</code></p>
 */
public class SubscriptionCriteriaBuilderStartedEvent extends ApplicationEvent {
	/**  */
	private static final long serialVersionUID = 1082522220898295412L;
	/** The bean name of the SubscriptionCriteriaBuilder */
	protected final String beanName;
	
	/**
	 * Creates a new SubscriptionCriteriaBuilderStartedEvent
	 * @param source the SubscriptionCriteriaBuilder that started
	 * @param beanName The bean name of the SubscriptionCriteriaBuilder
	 */
	public SubscriptionCriteriaBuilderStartedEvent(SubscriptionCriteriaBuilder<?,?,?> source, String beanName) {
		super(source);
		this.beanName = beanName;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.util.EventObject#getSource()
	 */
	public SubscriptionCriteriaBuilder<?,?,?> getSource() {
		return getSource();
	}

	/**
	 * Returns the bean name of the builder referenced in the event
	 * @return the beanName of the builder referenced in the event
	 */
	public String getBeanName() {
		return beanName;
	}

}
