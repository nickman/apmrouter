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

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;

import org.helios.apmrouter.metric.IMetric;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.buffer.DirectChannelBufferFactory;

/**
 * <p>Title: TextMetricAccumulator</p>
 * <p>Description: Accumulates {@link IMetric}s as graphite metrics in preparation for a metric count or time based flush.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.TextMetricAccumulator</code></p>
 */

public class TextMetricAccumulator {
	/** The accumulation buffer */
	protected final ChannelBuffer accum;
	/** The outputstream to write meteric test bytes into the accum buffer */
	protected final OutputStream os;
	/** The number of accumulated metrics */
	protected final AtomicInteger metricCount = new AtomicInteger(0);
	/** The metric formatter */
	protected final MetricTextFormatter formatter;
	
	/** The message format for the submission  */
	public static final String METRIC_FORMAT = "%s %s %s \n";
	

	/**
	 * Creates a new TextMetricAccumulator
	 * @param formatter The metric formatter 
	 * @param bufferSize The initial buffer size (in bytes) for the accumulation buffer
	 */
	public TextMetricAccumulator(MetricTextFormatter formatter, int bufferSize) {
		 accum = ChannelBuffers.dynamicBuffer(bufferSize, new DirectChannelBufferFactory());
		 os = new ChannelBufferOutputStream(accum);
		 this.formatter = formatter;
	}
	
	/**
	 * Appends the passed {@link IMetric}s to the accumulation buffer in the configured metric format
	 * @param metrics The metrics to accumulate
	 * @return The number of metrics accumulated so far
	 * @throws IOException thrown on any error writing to the output stream
	 */
	public int append(IMetric...metrics) throws IOException {
		if(metrics!=null && metrics.length>0) {
			metricCount.addAndGet(formatter.format(os, metrics));
		}
		return metricCount.get();
	}
	
	/**
	 * Returns the number of accumulated graphite metrics
	 * @return the number of accumulated graphite metrics
	 */
	public int size() {
		return metricCount.get();
	}
	
	/**
	 * Copies the accumulated buffer into a new buffer, clears the accumulated buffer and returns the copy
	 * @return the copied buffer to send to the text accepting back end
	 */
	public ChannelBuffer flush() {
		ChannelBuffer toSend = ChannelBuffers.buffer(accum.readableBytes());
		toSend.writeBytes(accum);
		accum.clear();
		metricCount.set(0);
		return toSend;
	}

}
