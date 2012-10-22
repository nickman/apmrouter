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

/**
 * <p>Title: SubscriptionCriteria</p>
 * <p>Description: Defines the source and filtering of events delivered to a <b><code>Subscriber</code></b></p>
 * <p><pre>
 * Examples:
 * =========
 * JMX:  [Domain/MBeanServer], ObjectName/Pattern, [Filter]
 * Catalog: Type(HostAgent/Metric), [Regex]
 * LiveMetrics: Regex
 * ===========================================================
 * 
 * i)		Source Instance  (Type S)  Could be: the bean name of the source, jmx domain for jmx, [need to define remote mbean conns, JMXServiceURL ?])
 * ii)		Filter  (Type F)
 * iii)		Extended Filter   (Type E)
 * ========================
 * 
 * JMX:  (Any JMX Notification Emitter)
 * ====
 * 	SourceInstance:  	MBeanServerConnection Locator (Optional), Defaults to PlatformAgent
 * 	Filter: 			ObjectName  (if pattern, aggregates all matching, listens for new matching instances)
 *  Extended Filter:	Notification Filter:      AttributeChangeNotificationFilter, MBeanServerNotificationFilter, NotificationFilterSupport or Scripted Instance
 *  
 * Catalog: (New metric instances, Host/Agent activity through JMX)
 * ========
 *  SourceInstance: 	Singleton
 *  Filter:				Combination of HOST/AGENT/METRIC 
 *  Extended Filter:	Regex, optionally for each of HOST/AGENT/METRIC   
 *   
 * Live Metrics:
 * =============
 *  SourceInstance:		PatternRouter Destination, JMS, Redis, Esper
 *  Filter:				Regex on MetricID
 *  Extended Filter:	If supported by SourceInstance - Metric instance value filter expression
 * </pre></p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.subscription.SubscriptionCriteria</code></p>
 * @param <S> The expected type of the source instance identifier/locator
 * @param <F> The expected type of the filter expression
 * @param <E> The expected type of the extended filter expression
 */

public interface SubscriptionCriteria<S, F, E> {
	
	/**
	 * Returns the event source identifier/locator
	 * @return the event source identifier/locator
	 */
	public S getEventSource();
	
	/**
	 * Returns the event filter expression
	 * @return the event filet
	 */
	public F getEventFilter();
	
	/**
	 * Returns the event extended filter expression
	 * @return the event extended filter
	 */
	public E getEventExtendedFilter();
	
}
