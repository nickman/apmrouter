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

import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.trace.DirectMetricCollection;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

/**
 * <p>Title: IMetricEncoder</p>
 * <p>Description: A netty encoder for {@link IMetric}s</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.sender.netty.codec.IMetricEncoder</code></p>
 */
@ChannelHandler.Sharable
public class IMetricEncoder extends OneToOneEncoder {
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.handler.codec.oneone.OneToOneEncoder#encode(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.Channel, java.lang.Object)
	 */
	@Override
	protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
		if(msg instanceof DirectMetricCollection) {
			ChannelBuffer cb = ((DirectMetricCollection)msg).toChannelBuffer();
			cb.setByte(DirectMetricCollection.BYTE_ORDER_OFFSET, cb.getByte(DirectMetricCollection.BYTE_ORDER_OFFSET)==DirectMetricCollection.BYTE_ZERO ? DirectMetricCollection.BYTE_ONE : DirectMetricCollection.BYTE_ZERO);
			return cb;
		} else if(msg instanceof ChannelBuffer) {
			return msg;
		}
		return null;
	}
	
	/**
	 * @param ctx
	 * @param e
	 */
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
		System.err.println("[MetricEncoder] Caught exception event [" + e.getCause() + "]");
	}

	
	/**
	 * Writes the metric's metricId if it has not been tokenized yet
	 * @param cb The channel buffer to write to 
	 * @param metric The metric to write the metricId for
	 */
	protected static void writeMetricId(ChannelBuffer cb, IMetric metric) {
		byte[] fqn = metric.getFQN().getBytes();
		cb.writeInt(fqn.length);
		cb.writeBytes(fqn);
	}

}
