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
package org.helios.apmrouter.server.unification.pipeline2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.helios.apmrouter.server.ServerComponent;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.CompositeChannelBuffer;
import org.jboss.netty.buffer.DirectChannelBufferFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelLocal;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.UpstreamMessageEvent;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;

/**
 * <p>Title: FlushOnCloseBufferAggregator</p>
 * <p>Description: An upstream channel buffer aggregator that accumulates upstream message events if they are {@link ChannelBuffer}s
 * and then flushes the accumulated buffer on channel close.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.unification.pipeline2.FlushOnCloseBufferAggregator</code></p>
 */

public class FlushOnCloseBufferAggregator extends ServerComponent implements ChannelUpstreamHandler {
	/** The singleton instance */
	private static volatile FlushOnCloseBufferAggregator instance = null;
	/** The singleton ctor lock */
	private static final Object lock = new Object();
	
	/** The buffer factory to create aggregated sub-buffers for the dynamic channel buffer */
	private final DirectChannelBufferFactory channelBufferFactory = new DirectChannelBufferFactory(); 
	
	/** The dynamic buffer channel local into which all content is aggregated */
	private final ChannelLocal<List<ChannelBuffer>> aggregation = new ChannelLocal<List<ChannelBuffer>>(false);
	
	/** A counter of aggregations in flight */
	private final AtomicLong inflight = new AtomicLong(0L); 
	
	/**
	 * Acquires the FlushOnCloseBufferAggregator singleton instance
	 * @return the FlushOnCloseBufferAggregator singleton instance 
	 */
	public static FlushOnCloseBufferAggregator getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new FlushOnCloseBufferAggregator();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Creates a new FlushOnCloseBufferAggregator
	 */
	private FlushOnCloseBufferAggregator() {
		
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelUpstreamHandler#handleUpstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	@Override
	public void handleUpstream(final ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		if(e instanceof UpstreamMessageEvent) {
			Object msg = ((UpstreamMessageEvent)e).getMessage();
			if(msg instanceof ChannelBuffer) {
				incr("ChannelBuffersProcessed");
				final ChannelBuffer buff = (ChannelBuffer)msg;
				final Channel channel = e.getChannel();
				List<ChannelBuffer> aggBuffs = aggregation.get(channel);
				if(aggBuffs==null) {
					inflight.incrementAndGet();
					aggBuffs = new ArrayList<ChannelBuffer>();
					writeBufferToAggregation(buff, aggBuffs);
					aggregation.set(channel, aggBuffs);					
					// starting a new aggregation, so add a close handler
					channel.getCloseFuture().addListener(new ChannelFutureListener() {
						@Override
						public void operationComplete(ChannelFuture future) throws Exception {
							try {
								List<ChannelBuffer> aggregatedChannelBuffers = aggregation.get(channel);
								log.info("Preparing Aggregated CompositeChannelBuffer from [" + aggregatedChannelBuffers.size() + "] aggregated buffers");
								ChannelBuffer totalAggregate = new CompositeChannelBuffer(buff.order(), aggregatedChannelBuffers,true);
								log.info("Aggregated CompositeChannelBuffer has [" + totalAggregate.readableBytes() + "] readable bytes");
								ctx.sendUpstream(new UpstreamMessageEvent(channel, totalAggregate, channel.getRemoteAddress()));
							} finally {								
								inflight.decrementAndGet();
								aggregation.remove(channel);
							}
						}
					});
				} else {
					writeBufferToAggregation(buff, aggBuffs);
				}
				return;
			}
		}
		ctx.sendUpstream(e);
	}
	
	/**
	 * Transfers the incoming channel buffer to a new direct allocated channel of the same size which is then added to the aggregate.
	 * @param incoming The incoming channel buffer
	 * @param target The target aggregate to add the newly allocated buffer to
	 */
	protected void writeBufferToAggregation(ChannelBuffer incoming, List<ChannelBuffer> target) {
		log.info("Readable bytes of target before aggregate:" + incoming.readableBytes());
		ChannelBuffer dbuff = channelBufferFactory.getBuffer(incoming.order(), incoming.readableBytes());
		dbuff.writeBytes(incoming);
		target.add(dbuff);					
		log.info("Readable bytes of target after aggregate:" + incoming.readableBytes());		
	}
	
	/** The name of this decoder in the pipeline */
	public static final String PIPE_NAME = "FlushOnCloseBufferAggregator";

	
	/**
	 * Reutrns the number of inflight aggregations
	 * @return the number of inflight aggregations
	 */
	@ManagedMetric(category="FlushOnCloseBufferAggregator", displayName="InflightAggregations", metricType=MetricType.GAUGE, description="The number of inflight aggregations")
	public long getInflightAggregations() {
		return inflight.get();
	}
	
	/**
	 * Reutrns the number of completed aggregations
	 * @return the number of completed aggregations
	 */
	@ManagedMetric(category="FlushOnCloseBufferAggregator", displayName="CompletedAggregations", metricType=MetricType.COUNTER, description="The number of completed aggregations")
	public long getCompletedAggregations() {
		return getMetricValue("CompletedAggregations");
	}
	
	/**
	 * Reutrns the number of upstream channel buffers processed
	 * @return the number of upstream channel buffers processed
	 */
	@ManagedMetric(category="FlushOnCloseBufferAggregator", displayName="ChannelBuffersProcessed", metricType=MetricType.COUNTER, description="The number of upstream channel buffers processed")
	public long getChannelBuffersProcessed() {
		return getMetricValue("ChannelBuffersProcessed");
	}


}
