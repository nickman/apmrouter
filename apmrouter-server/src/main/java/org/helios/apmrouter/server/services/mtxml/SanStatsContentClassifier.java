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
package org.helios.apmrouter.server.services.mtxml;

import java.util.List;

import org.helios.apmrouter.server.unification.pipeline2.FlushOnCloseBufferAggregator;
import org.helios.apmrouter.server.unification.pipeline2.ProtocolSwitchContext;
import org.helios.apmrouter.server.unification.pipeline2.SwitchPhase;
import org.helios.apmrouter.server.unification.pipeline2.content.ConfigurableStaxContentClassifier;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>Title: SanStatsContentClassifier</p>
 * <p>Description: A content classifier to detect SAN Stats XML content</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.services.mtxml.SanStatsContentClassifier</code></p>
 */

public class SanStatsContentClassifier extends ConfigurableStaxContentClassifier {
	/** The san stats parser tracer */
	@Autowired(required=true)
	protected SanStatsParserTracer sanStatsParserTracer = null;
	
	/**
	 * Creates a new SanStatsContentClassifier
	 */
	public SanStatsContentClassifier() {
		super("SanStatsXml");
		this.targetTags.put("sample", "sample");
	}
	
	/** The name of this decoder in the pipeline */
	public static final String PIPE_NAME = "SanStatsXml";
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.unification.pipeline2.Initiator#process(org.helios.apmrouter.server.unification.pipeline2.ProtocolSwitchContext, java.lang.Object)
	 */
	@Override
	public SwitchPhase process(final ProtocolSwitchContext context, Object matchKey) {
		log.info("Classifier identified content as [" + name + "]");
		ChannelPipeline pipeline = context.getPipeline();
		// ===============================================================
		//   Remove all the pipelines handlers except the executor and me
		// ===============================================================
		StringBuilder b = new StringBuilder("\n\t========\n\tRemoved Handlers\n\t========");
		List<String> names = pipeline.getNames();		
		for(String handlerName: names) {
			if("exec".equals(handlerName) || PIPE_NAME.equals(handlerName)) continue;
			ChannelHandler ch = pipeline.remove(handlerName);
			b.append("\n\t").append(handlerName).append("  [").append(ch.getClass().getSimpleName()).append("]");
		}
		b.append("\n\t========");
		log.info(b);
		// ===============================================================
		if(!context.aggregationHasOccured()) {
			// ----> send to aggregator, then SanStatsParserTracer
			pipeline.addLast(FlushOnCloseBufferAggregator.PIPE_NAME, FlushOnCloseBufferAggregator.getInstance());
			pipeline.addLast(FlushOnCloseBufferAggregator.PIPE_NAME + "-Complete", new SimpleChannelUpstreamHandler(){
				@Override
			 	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
					log.info("[" + FlushOnCloseBufferAggregator.PIPE_NAME + "-Complete] FlushOnCloseBufferAggregator Complete Event");
			 		context.setAggregationHasOccured();
					super.messageReceived(ctx, e);
			 	}
			});
			// what if the channel is already closed ?
		} 
		pipeline.addLast(SanStatsParserTracer.PIPE_NAME, sanStatsParserTracer);
		if(!context.getChannel().isOpen()) {
			System.err.println("\n\t=================\n\tChannel Already Closed\n\t=================\n");
		}
		
//		context.sendCurrentBufferUpstream(FlushOnCloseBufferAggregator.PIPE_NAME);
		context.sendCurrentBufferUpstream(context.getPipeline().getNames().get(0));
		
		return SwitchPhase.COMPLETE;
	}

}
