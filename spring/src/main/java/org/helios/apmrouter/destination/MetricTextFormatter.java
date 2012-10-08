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

import org.helios.apmrouter.metric.IMetric;

/**
 * <p>Title: MetricTextFormatter</p>
 * <p>Description: Defines a formatter that converts {@link IMetric} instances into text</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.MetricTextFormatter</code></p>
 */

public interface MetricTextFormatter {
	/**
	 * Converts the passed metrics into a textual byte array
	 * @param accumulator The output stream to write the metric text to
	 * @param metrics The metrics to format
	 * @return the number of metrics written
	 * @throws IOException thrown on any error writing to the output stream
	 */
	public int format(OutputStream accumulator, IMetric...metrics) throws IOException;
}
