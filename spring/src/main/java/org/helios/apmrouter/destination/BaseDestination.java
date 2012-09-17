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

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.router.PatternMatch;
import org.helios.apmrouter.router.PatternMatch.PatternMatchGroup;
import org.helios.apmrouter.router.RouteDestination;
import org.helios.apmrouter.server.ServerComponentBean;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.support.MetricType;

/**
 * <p>Title: BaseDestination</p>
 * <p>Description: Convenience base class for implementing routing destinations</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.BaseDestination</code></p>
 */

public class BaseDestination extends ServerComponentBean implements RouteDestination<IMetric> {
	/** The pattern match group for this destination */
	protected final PatternMatchGroup pmg;
	/** The match patterns for this destination */
	protected final Set<String> matchPatterns = new CopyOnWriteArraySet<String>();
	
	
	/**
	 * Creates a new BaseDestination
	 * @param patterns The patterns to initialize with
	 */
	public BaseDestination(String...patterns) {
		 pmg = PatternMatch.newPatternMatchGroup(patterns);
		 Collections.addAll(matchPatterns, pmg.getPatterns());
	}
	
	/**
	 * Creates a new BaseDestination
	 * @param patterns The patterns to initialize with
	 */
	public BaseDestination(Collection<String> patterns) {
		this(patterns==null||patterns.isEmpty() ? new String[]{} : patterns.toArray(new String[0]));
	}
	
	/**
	 * Creates a new BaseDestination
	 */
	public BaseDestination() {
		this(new String[]{});
	}
	
	/**
	 * Adds the passed patterns as destinaion route matches
	 * @param patterns The patterns to add
	 */
	public void setMatchPatterns(Set<String> patterns) {
		if(patterns!=null) {
			for(String s: patterns) {
				if(s==null) continue;
				if(pmg.add(s.trim())) {
					matchPatterns.add(s.trim());
				}
			}
		}
	}
	
	/**
	 * Returns the match patterns for this destination
	 * @return the match patterns for this destination
	 */
	@ManagedAttribute
	public Set<String> getMatchPatterns() {
		return matchPatterns;
	}
	
	
	/**
	 * Adds a new match pattern
	 * @param pattern the pattern to add
	 */
	@ManagedOperation
	public void addMatchPattern(String pattern) {
		if(pattern!=null) {
			if(pmg.add(pattern.trim())) {
				matchPatterns.add(pattern.trim());
			}			
		}
	}
	
	/**
	 * Removes a match pattern
	 * @param pattern the pattern to remove
	 */
	@ManagedOperation
	public void removeMatchPattern(String pattern) {
		if(pattern!=null) {
			pmg.remove(pattern.trim());
			matchPatterns.remove(pattern.trim());
		}			
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponent#getSupportedMetricNames()
	 */
	@Override
	public String[] getSupportedMetricNames() {
		return new String[]{"AcceptedRoutes"};
	}
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.router.RouteDestination#acceptRoute(org.helios.apmrouter.router.Routable)
	 */
	@Override
	public void acceptRoute(IMetric routable) {
		if(pmg.matches(routable.getRoutingKey())) {
			incr("AcceptedRoutes");
		}		
	}
	
	/**
	 * Returns the number messages accepted by this destination
	 * @return the number messages accepted by this destination
	 */
	@ManagedMetric(category="RoutingDestinations", metricType=MetricType.COUNTER, description="The number messages accepted by this destination", displayName="AcceptedRoutes")
	public long getAcceptedRouteCount() {
		return getMetricValue("AcceptedRoutes");
	}

}
