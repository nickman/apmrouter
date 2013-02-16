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
package org.helios.apmrouter.destination.logstash.senders;


import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.export.annotation.ManagedAttribute;

/**
 * <p>Title: Log4jLogstashSender</p>
 * <p>Description: Relays stash requests to a log4j appender which is assumed to know what to do (like a socket appender to a logstash server)</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.logstash.senders.Log4jLogstashSender</code></p>
 */
@ManagedResource
public class Log4jLogstashSender extends AbstractLogstashSender<LoggingEvent> {
	/** The name of the logger that sends to logstash */
	protected String loggerName = null;
	/** The logger that sends to logstash */
	protected Logger stashLogger = null;
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.logstash.senders.AbstractLogstashSender#doStash(java.lang.Object)
	 */
	@Override
	protected void doStash(LoggingEvent stashee) {
		stashLogger.callAppenders(stashee);
	}
	/**
	 * Returns the name of the logger that sends to logstash
	 * @return the loggerName
	 */
	@ManagedAttribute
	public String getLoggerName() {
		return loggerName;
	}
	
	
	/**
	 * Sets the name of the logger that sends to logstasher
	 * @param loggerName the loggerName to set
	 */
	public void setLoggerName(String loggerName) {
		this.loggerName = loggerName;
		stashLogger = Logger.getLogger(loggerName);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.logstash.senders.LogstashSender#getAcceptedType()
	 */
	public Class<LoggingEvent> getAcceptedType() {
		return LoggingEvent.class;
	}
	
}
