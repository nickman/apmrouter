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
package org.helios.apmrouter.trace;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

/**
 * <p>Title: LogTracer</p>
 * <p>Description: Quickie logger wrapper for tracing log events.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.trace.LogTracer</code></p>
 * FIXME: Get rid of this class and create a proper appender.
 */
public class LogTracer extends AppenderSkeleton {
	/** The logger's tracer */
	protected final ITracer tracer;
	/** The metric name derived from the logger name */
	protected static final String metricName = "Log4j";
	/** The metric namespace derived from the logger name */
	protected static final String[] namespace = new String[]{"org", "helios", "apmrouter"};
	
//	/**
//	 * Creates a new LogTracer
//	 * @param clazz The class to derive the metric name from
//	 * @return a new LogTracer
//	 */
//	public static Logger getLogger(Class<?> clazz) {
//		return new LogTracer(clazz.getName());
//	}
	
	/**
	 * Creates a new LogTracer
	 */
	public LogTracer() {
		tracer = TracerFactory.getTracer();
	}
	
//	/**
//	 * <p>Dispatches the logging event to the tracer.
//	 * {@inheritDoc}
//	 * @see org.apache.log4j.Category#callAppenders(org.apache.log4j.spi.LoggingEvent)
//	 */
//	public void callAppenders(LoggingEvent event) {
//		tracer.traceBlob(event, name, namespace);
//	}

	@Override
	public void close() {
	}

	@Override
	public boolean requiresLayout() {
		return false;
	}

	@Override
	protected void append(LoggingEvent event) {
		tracer.traceBlobDirect(event, metricName, namespace);		
	}


}
