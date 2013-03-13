/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2013, Helios Development Group and individual contributors
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
package org.helios.apmrouter.destination.subdestination;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.metric.IMetricListener;
import org.jboss.netty.channel.Channel;

/**
 * <p>Title: AgentSubDestinationListener</p>
 * <p>Description: A proxy metric listener for a subscribed agent</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.subdestination.AgentSubDestinationListener</code></p>
 */

public class AgentSubDestinationListener implements IMetricListener {
	/** The listener's patterns */
	protected final Set<String> patterns;
	/** The listener's channels */
	protected final Channel channel;
	
	/** The sub dest sub id */
	protected long subId = -1;
	
	
	
	
	/**
	 * Creates a new AgentSubDestinationListener
	 * @param channel
	 * @param patterns
	 */
	public AgentSubDestinationListener(Channel channel, String...patterns) {
		this.channel = channel;
		this.patterns = new HashSet<String>(Arrays.asList(patterns));
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetricListener#getPatterns()
	 */
	@Override
	public Set<String> getPatterns() {
		return patterns;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetricListener#setSubscriptionId(long)
	 */
	@Override
	public void setSubscriptionId(long subId) {
		this.subId = subId;

	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetricListener#getSubscriptionId()
	 */
	@Override
	public long getSubscriptionId() {
		return subId;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.metric.IMetricListener#onMetric(org.helios.apmrouter.metric.IMetric)
	 */
	@Override
	public void onMetric(IMetric metric) {
		channel.write(metric);
	}

}
