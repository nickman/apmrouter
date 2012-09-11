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
package org.helios.apmrouter.sender.netty.codec;

import java.nio.ByteOrder;

/**
 * <p>Title: IMetricDecodePhase</p>
 * <p>Description: Enumerates the phasesof an IMetric decode</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.sender.netty.codec.IMetricDecodePhase</code></p>
 */

public enum IMetricDecodePhase {
	BYTE_ORDER,
	TOKENIZED,
	METRIC_ID,
	TIMESTAMP,
	METRIC_TYPE,
	
//	buff.writeByte(ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN) ? 0 : 1); // 0 for LITTLE, 1 for BIG
//	long token = metric.getToken();
//	// write metric ID
//	if(token!=-1) {
//		buff.writeByte(1);
//		buff.writeLong(token);
//	} else {
//		buff.writeByte(0);
//		writeMetricId(buff, metric);
//	}
//	// Write the metric timestamp
//	buff.writeLong(metric.getTime());
//	// Write the metric type
//	buff.writeInt(metric.getType().ordinal());
//	// Write the metric value
//	if(metric.getType().isLong()) {
//		// just the long if this is a long type
//		buff.writeLong(metric.getLongValue());
//	} else {
//		// get the bytebuffer if its not a long
//		buff.writeBytes(metric.getRawValue());
//	}
	
}
