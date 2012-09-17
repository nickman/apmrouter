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
package org.helios.apmrouter.destination.graphite;

import java.util.concurrent.atomic.AtomicInteger;

import org.helios.apmrouter.metric.IMetric;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.buffer.DirectChannelBufferFactory;

/**
 * <p>Title: GraphiteMetricAccumulator</p>
 * <p>Description: Accumulates {@link IMetric}s as graphite metrics in preparation for a metric count or time based flush.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.graphite.GraphiteMetricAccumulator</code></p>
 */

public class GraphiteMetricAccumulator {
	/** The accumulation buffer */
	protected final ChannelBuffer accum;
	/** The number of accumulated graphite metrics */
	protected final AtomicInteger metricCount = new AtomicInteger(0);
	/**
	 * Creates a new GraphiteMetricAccumulator
	 * @param bufferSize The initial buffer size (in bytes) for the accumulation buffer
	 */
	public GraphiteMetricAccumulator(int bufferSize) {
		 accum = ChannelBuffers.dynamicBuffer(bufferSize, new DirectChannelBufferFactory());
	}
	
	public int append(IMetric...metrics) {
		
	}
}
