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
package org.helios.apmrouter.server.services;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.helios.apmrouter.server.ServerComponentBean;
import org.helios.apmrouter.server.services.events.PipelineModifierStarted;
import org.helios.apmrouter.server.services.events.PipelineModifierStopped;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;

/**
 * <p>Title: AbstractPipelineModifier</p>
 * <p>Description: Base abstract {@link PipelineModifier} class</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.services.AbstractPipelineModifier</code></p>
 */

public abstract class AbstractPipelineModifier extends ServerComponentBean implements PipelineModifier {
	/** The name of the modifier from the perspective of a channel pipeline */
	protected String name = null;
	/** A set of uriPatterns that this modifier will be activated for */
	protected Set<String> uriPatterns = new HashSet<String>();
	
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
	 * @see org.helios.apmrouter.server.services.PipelineModifier#getName()
	 */
	@Override
	@ManagedAttribute(description="The name of the handler added by this modified within a channel pipeline")
	public String getName() {
		return name;
	}
	
	/**
	 * Sets the name of this modifier
	 * @param name the name of this modifier
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.services.PipelineModifier#getChannelHandler()
	 */
	@Override
	public ChannelHandler getChannelHandler() {
		return doGetChannelHandler();
	}
	
	/**
	 * Delegate channel handler accessor for concrete impls.
	 * @return The channel handler to add to the pipeline
	 */
	protected abstract ChannelHandler doGetChannelHandler();
	

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.services.PipelineModifier#modifyPipeline(org.jboss.netty.channel.ChannelPipeline)
	 */
	@Override
	public void modifyPipeline(ChannelPipeline pipeline) {
		doModifyPipeline(pipeline);		
		incr("PipeLineModfies");
	}
	
	/**
	 * Delegate pipeline modification for concrete impls.
	 * @param pipeline The pipeline to modify
	 */
	protected abstract void doModifyPipeline(ChannelPipeline pipeline);
	
	/**
	 * Sets the URIs that this modifier is activated for 
	 * @param uris the URIs that this modifier is activated for 
	 */
	public void setUriPatterns(Set<String> uris) {
		if(uris!=null) {
			uriPatterns.addAll(uris);
		}
	}

	/**
	 * Returns an unmodifiable set of URI patterns that this modifier is activated for 
	 * @return an unmodifiable set of URI patterns that this modifier is activated for
	 */
	@ManagedAttribute(description="URI patterns that this modifier is activated for")
	public Set<String> getUriPatterns() {
		return Collections.unmodifiableSet(uriPatterns);
	}
	
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
	
}
