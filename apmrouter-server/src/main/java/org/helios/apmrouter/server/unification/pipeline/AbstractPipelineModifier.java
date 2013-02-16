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
package org.helios.apmrouter.server.unification.pipeline;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.helios.apmrouter.server.ServerComponentBean;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;


/**
 * <p>Title: AbstractPipelineModifier</p>
 * <p>Description: Base abstract {@link PipelineModifier} class</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.unification.pipeline.AbstractPipelineModifier</code></p>
 */
public abstract class AbstractPipelineModifier extends ServerComponentBean implements PipelineModifier {
	/** The protocol name supported by this modifier */
	protected final String protocol;
	
	/**
	 * Creates a new AbstractPipelineModifier
	 * @param protocol The protocol name supported by this modifier
	 */
	protected AbstractPipelineModifier(String protocol) {
		if(protocol==null || protocol.trim().isEmpty()) throw new IllegalArgumentException("Null or empty protocol name", new Throwable());
		this.protocol = protocol.trim().toLowerCase();
	}
	
	/**
	 * Publishes a {@link PipelineModifierStarted} to the app context.
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStart()
	 */
	@Override
	protected void doStart() throws Exception {
		applicationContext.publishEvent(new PipelineModifierStarted(this, beanName));
	}
	
	/**
	 * Publishes a {@link PipelineModifierStopped} to the app context.
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStop()
	 */
	@Override
	protected void doStop() {
		applicationContext.publishEvent(new PipelineModifierStopped(this, beanName));
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.unification.pipeline.PipelineModifier#modifyPipeline(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.Channel, org.jboss.netty.buffer.ChannelBuffer)
	 */
	@Override
	public void modifyPipeline(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) {
		doModifyPipeline(ctx, channel, buffer);		
		incr("PipeLineModfies");
	}
	
	
	/**
	 * Delegate pipeline modify
	 * @param ctx The channel handler context
	 * @param channel The current channel
	 * @param buffer  The initiating buffer
	 */
	protected abstract void doModifyPipeline(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer);
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponent#getSupportedMetricNames()
	 */
	@Override
	public Set<String> getSupportedMetricNames() {
		Set<String> metrics = new HashSet<String>(super.getSupportedMetricNames());
		metrics.add("PipeLineModfies");
		return metrics;
	}

	/**
	 * Returns the number of pipelines modified by this modifier 
	 * @return the number of pipelines modified by this modifier
	 */
	@ManagedMetric(category="PipelineModfiier", metricType=MetricType.COUNTER, description="The number of pipelines modified by this modifier", displayName="PipeLineModfies")
	public long getPipeLineModfies() {
		return getMetricValue("PipeLineModfies");
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.unification.pipeline.PipelineModifier#getProtocol()
	 */
	@Override
	public String getProtocol() {
		return protocol;
	}
}
