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
package org.helios.apmrouter.instrumentation.interceptors;

import org.helios.apmrouter.metric.catalog.ICEMetricCatalog;

/*import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.DefaultCompositeByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
*/
/**
 * <p>Title: IntervalMetricAccumulator</p>
 * <p>Description: An off heap metric accumulator for accumulating metrics during an interval</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.instrumentation.interceptors.IntervalMetricAccumulator</code></p>
 */

public class IntervalMetricAccumulator {
/*	*//** The unpooled bytebuff allocator *//*
	protected static final UnpooledByteBufAllocator BUFF_ALLOCATOR = UnpooledByteBufAllocator.DIRECT_BY_DEFAULT;
	*//** The master container for individual metric sub-buffers *//*
	protected final CompositeByteBuf metricBuffer = new DefaultCompositeByteBuf(BUFF_ALLOCATOR, true, MAX_METRIC_BUFFERS);
*/	
	/** The maximum number of metric sub-buffers */
	public static final int MAX_METRIC_BUFFERS = 1024;
	/**
	 * Creates a new IntervalMetricAccumulator
	 */
	public IntervalMetricAccumulator() {
		//ICEMetricCatalog.getInstance().get(host, agent, name, type, namespace)
	}

}
