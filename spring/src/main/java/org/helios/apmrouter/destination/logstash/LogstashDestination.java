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

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.helios.apmrouter.destination.BaseDestination;

/**
 * <p>Title: LogstashDestination</p>
 * <p>Description: Destination that accepts BLOB metric types, inspects them to see if they are log4j log-events and relays them to a logstash server</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.logstash.LogstashDestination</code></p>
 */

public class LogstashDestination extends BaseDestination {
	/** A set of logstash senders, each of which will be passed objects to 'stash */
	protected Set<LogstashSender> senders = new CopyOnWriteArraySet<LogstashSender>();
	
	
}
