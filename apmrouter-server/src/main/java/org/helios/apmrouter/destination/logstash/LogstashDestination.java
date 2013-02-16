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
package org.helios.apmrouter.destination.logstash;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.helios.apmrouter.destination.BaseDestination;
import org.helios.apmrouter.destination.logstash.senders.LogstashSender;
import org.helios.apmrouter.metric.ExpandedMetric;
import org.helios.apmrouter.metric.IMetric;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;

/**
 * <p>Title: LogstashDestination</p>
 * <p>Description: Destination that accepts BLOB metric types, inspects them to see if they are log4j log-events and relays them to a logstash server</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.logstash.LogstashDestination</code></p>
 */

public class LogstashDestination extends BaseDestination {
	/** A map of logstash senders keyed by the class name of the type they 'stash */
	protected Map<String, LogstashSender<Object>> senders = new ConcurrentHashMap<String, LogstashSender<Object>>();
	
	@Override
	protected void doStart() throws Exception {
		StringBuilder b = new StringBuilder();
		for(String c: senders.keySet()) {
			b.append(c).append("-BLOB.*|");
		}
		if(b.charAt(b.length()-1)=='|') {
			b.deleteCharAt(b.length()-1);
		}
		setMatchPatterns(Collections.singleton(b.toString()));
		super.doStart();
	}
	
	/**
	 * Accept Route additive for BaseDestination extensions
	 * @param routable The metric to route
	 */
	protected void doAcceptRoute(IMetric routable) {
		if(!(routable instanceof ExpandedMetric)) {
			incr("Discarded");
			return;
		}
		ExpandedMetric em = (ExpandedMetric)routable;
		LogstashSender<Object> sender = senders.get(em.getValueClassName());
		if(sender==null) {
			incr("NoSender");
			return;
		}
		sender.stash(em.getValue());
		incr("AcceptedStashes");
	}	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponent#getSupportedMetricNames()
	 */
	@Override
	public Set<String> getSupportedMetricNames() {
		Set<String> _metrics = new HashSet<String>(super.getSupportedMetricNames());
		_metrics.add("AcceptedStashes");		
		_metrics.add("Discarded");
		_metrics.add("NoSender");
		return _metrics;
	}
	
	/**
	 * Returns the number of discarded stashes because they were not expanded metrics
	 * @return the number of discarded stashes
	 */
	@ManagedMetric(category="Logstash", metricType=MetricType.COUNTER, description="the number of discarded stashes because they were not expanded metrics")
	public long getDiscarded() {
		return getMetricValue("Discarded");
	}	
	
	/**
	 * Returns the number of discarded stashes because there was no sender registered for the passed event
	 * @return the number of discarded stashes
	 */
	@ManagedMetric(category="Logstash", metricType=MetricType.COUNTER, description="the number of discarded stashes because there was no sender registered for the passed event")
	public long getNoSender() {
		return getMetricValue("NoSender");
	}	
	
	/**
	 * Returns the number of forwarded stashes
	 * @return the number of forwarded stashes
	 */
	@ManagedMetric(category="Logstash", metricType=MetricType.COUNTER, description="the number of forwarded stashes")
	public long getAcceptedStashes() {
		return getMetricValue("AcceptedStashes");
	}	
	
	/**
	 * Returns the number of registered senders
	 * @return the number of registered senders
	 */
	@ManagedAttribute
	public int getSenderCount() {
		return senders.size();
	}
	
	
	
	/**
	 * Adds the passed senders to this destinations sender relays
	 * @param senders A collection of logstash senders
	 */
	@Autowired(required=true)
	public void setSenders(Collection<LogstashSender<Object>> senders) {
		if(senders!=null) {
			for(LogstashSender<Object> sender: senders) {
				if(sender!=null) {
					this.senders.put(sender.getAcceptedType().getName(), sender);
				}
			}
		}
	}
}
