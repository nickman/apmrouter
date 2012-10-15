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
package org.helios.apmrouter.destination;

import java.util.concurrent.atomic.AtomicLong;

import javax.management.MXBean;

import org.helios.apmrouter.router.PatternMatch;
import org.jboss.netty.channel.Channel;

/**
 * <p>Title: MetricSubscription</p>
 * <p>Description: Wraps a metric subscription from a nash client</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.MetricSubscription</code></p>
 */

public class MetricSubscription implements MetricSubscriptionMBean {
	/** The subscription pattern match */
	private final PatternMatch patternMatch;
	/** A count of accepted matches */
	private final AtomicLong matches = new AtomicLong(0);
	/** A count of unaccepted matches */
	private final AtomicLong misses = new AtomicLong(0);
	/** The channel subscriber */
	private final Channel channel;
	
	/**
	 * Creates a new MetricSubscription
	 * @param patternMatch The subscription pattern match
	 * @param channel The channel subscriber
	 */
	public MetricSubscription(PatternMatch patternMatch, Channel channel) {
		this.patternMatch = patternMatch;
		this.channel = channel;
	}

	/**
	 * Determines if the passed string is a match for this subscription
	 * @param toMatch The string to match against
	 * @return true if a match, false otherwise
	 */
	public boolean matches(CharSequence toMatch) {
		boolean match = patternMatch.matches(toMatch);
		if(match) matches.incrementAndGet();
		else misses.incrementAndGet();
		return match;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.MetricSubscriptionMBean#getMatches()
	 */
	@Override
	public long getMatches() {
		return matches.longValue();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.MetricSubscriptionMBean#getMisses()
	 */
	@Override
	public long getMisses() {
		return misses.longValue();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.MetricSubscriptionMBean#getRemote()
	 */
	@Override
	public String getRemote() {
		return channel.getRemoteAddress().toString();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.MetricSubscriptionMBean#getMatchPattern()
	 */
	@Override
	public String getMatchPattern() {
		return patternMatch.getPatternValue();
	}
	
	
}
