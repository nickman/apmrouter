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

import org.cliffc.high_scale_lib.Counter;

/**
 * <p>Title: InvocationInterceptor</p>
 * <p>Description: An interceptor that measures the metrics enumerated in {@link ThreadResource}.
 * These metrics are accumulated in a buffer and then flushed on a timer because they represent real-time
 * invocation activity and we don't want to blow up the transport layer to the server.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.instrumentation.interceptors.InvocationInterceptor</code></p>
 */

public class InvocationInterceptor {
	/** The number of metrics currently in the buffer */
	protected final Counter metricCount = new Counter();
	/**
	 * Creates a new InvocationInterceptor
	 */
	public InvocationInterceptor() {
		
	}

}
