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
package org.helios.apmrouter.server.unification.protocol;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.helios.apmrouter.server.ServerComponentBean;
import org.helios.apmrouter.server.unification.pipeline.PipelineModifier;
import org.helios.apmrouter.server.unification.pipeline.PipelineModifierStarted;
import org.helios.apmrouter.server.unification.pipeline.PipelineModifierStopped;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;

/**
 * <p>Title: AbstractProtocolInitiator</p>
 * <p>Description: An abstract base class implementation of a {@link ProtocolInitiator}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.unification.protocol.AbstractProtocolInitiator</code></p>
 */

public abstract class AbstractProtocolInitiator extends ServerComponentBean implements ProtocolInitiator {
	/** Protocol recognition magic int 1 */
	protected int myMagic1 = -1;
	/** Protocol recognition magic int 2 */
	protected int myMagic2 = -1;
	/** The protocol name implemented by this initiator */
	public final String protocol;
	
	/** A map of {@link  PipelineModifier}s keyed by their bean name */
	protected final Map<String, PipelineModifier> modifiers = new ConcurrentHashMap<String, PipelineModifier>();

	
	/**
	 * Creates a new AbstractProtocolInitiator and adds {@link PipelineModifierStarted} and {@link PipelineModifierStopped} to
	 * the supported application event set.
	 * @param protocol The protocol name supported by this initiator
	 */
	public AbstractProtocolInitiator(String protocol) {
		super();
		this.protocol = protocol;
		supportedEventTypes.add(PipelineModifierStarted.class);
		supportedEventTypes.add(PipelineModifierStopped.class);
	}
	
	/**
	 * On start, searches the app context for {@link ProtocolInitiator}s not already registered.
	 * @param event The app context refresh event
	 */
	@Override
	public void onApplicationContextRefresh(ContextRefreshedEvent event) {
		Map<String, PipelineModifier> inits = event.getApplicationContext().getBeansOfType(PipelineModifier.class);
		if(!inits.isEmpty()) {
			for(Map.Entry<String, PipelineModifier> entry: inits.entrySet()) {
				if(!modifiers.containsKey(entry.getKey())) {
					modifiers.put(entry.getKey(), entry.getValue());
					info("Adding Discovered PipelineModifier [", entry.getKey(), "]" );
				}
			}
		}
	}

	/**
	 * Called when a {@link PipelineModifier} starts
	 * @param pipelineModifierStarted The {@link PipelineModifier} start event
	 */
	public void onApplicationEvent(PipelineModifierStarted pipelineModifierStarted) {
		PipelineModifier pm = pipelineModifierStarted.getModifier();
		if(!modifiers.containsKey(pm.getBeanName())) {
			modifiers.put(pm.getBeanName(), pm);
			info("Adding Started PipelineModifier [", pm.getBeanName(), "]" );
		}
	}
	
	/**
	 * Called when a {@link PipelineModifier} stops
	 * @param pipelineModifierStopped The {@link PipelineModifier} stop event
	 */
	public void onApplicationEvent(PipelineModifierStopped pipelineModifierStopped) {
		PipelineModifier pm = pipelineModifierStopped.getModifier();
		if(modifiers.remove(pm.getBeanName())!=null) {
			info("Removing Stopped PipelineModifier [", pm.getBeanName(), "]" );
		}
	}
	
	
	
	/**
	 * Publishes a {@link ProtocolInitiatorStarted} to the app context.
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStart()
	 */
	@Override
	protected void doStart() throws Exception {
		super.doStart();
		applicationContext.publishEvent(new ProtocolInitiatorStarted(this, beanName));
	}
	
	/**
	 * Publishes a {@link ProtocolInitiatorStopped} to the app context.
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStop()
	 */
	@Override
	protected void doStop() {
		super.doStop();
		applicationContext.publishEvent(new ProtocolInitiatorStopped(this, beanName));
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.unification.protocol.ProtocolInitiator#match(int, int)
	 */
	@Override
	public boolean match(int magic1, int magic2) {
		boolean match = (myMagic1==magic1 && myMagic2==magic2);
		if(match) incr("ProtocolInitiates");
		return match;
	}
	
	/**
	 * <p>By default, returns false. Concrete impls should extend this if they cannot match 
	 * on the magic ints but can match on the buffer.</p>
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.unification.protocol.ProtocolInitiator#match(org.jboss.netty.buffer.ChannelBuffer)
	 */
	@Override
	public boolean match(ChannelBuffer buff) {
		return false;
	}
	
	
	/**
	 * Returns the number of protocol initiations 
	 * @return the number of protocol initiations
	 */
	@ManagedMetric(category="ProtocolInitiation", metricType=MetricType.COUNTER, description="The number of protocol initiations", displayName="ProtocolInitiates")
	public long getProtocolInitiates() {
		return getMetricValue("ProtocolInitiates");
	}
	
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponent#getSupportedMetricNames()
	 */
	@Override
	public Set<String> getSupportedMetricNames() {
		Set<String> metrics = new HashSet<String>(super.getSupportedMetricNames());
		metrics.add("ProtocolInitiates");
		return metrics;
	}

	/**
	 * Returns the protocol recognition magic int 1
	 * @return the protocol recognition magic int 1
	 */
	@Override
	@ManagedAttribute(description="The protocol recognition magic int 1")
	public int getMyMagic1() {
		return myMagic1;
	}

	/**
	 * Sets the protocol recognition magic int 1
	 * @param myMagic1 the protocol recognition magic int 1
	 */
	public void setMyMagic1(int myMagic1) {
		this.myMagic1 = myMagic1;
	}

	/**
	 * Returns the protocol recognition magic int 2
	 * @return the protocol recognition magic int 2
	 */
	@Override
	@ManagedAttribute(description="The protocol recognition magic int 1")
	public int getMyMagic2() {
		return myMagic2;
	}

	/**
	 * Sets the protocol recognition magic int 2
	 * @param myMagic2 the protocol recognition magic int 2
	 */
	public void setMyMagic2(int myMagic2) {
		this.myMagic2 = myMagic2;
	}

	/**
	 * Returns 
	 * @return the protocol
	 */
	public String getProtocol() {
		return protocol;
	}
	
}
