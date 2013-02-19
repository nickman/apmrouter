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
package org.helios.apmrouter.destination.nash;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.helios.apmrouter.destination.BaseDestination;
import org.helios.apmrouter.metric.IMetric;
import org.helios.apmrouter.router.PatternMatch;
import org.helios.nash.NashRequest;
import org.helios.nash.handler.NashRequestHandler;
import org.helios.nash.handler.RequestHandlerRegistry;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.springframework.jmx.export.annotation.ManagedAttribute;


/**
 * <p>Title: NashDestination</p>
 * <p>Description: Command line server for APMRouter</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.nash.NashDestination</code></p>
 */

public class NashDestination extends BaseDestination implements NashRequestHandler, NashDestinationMXBean {
	/** A map of pattern matches keyed by the nash requests that requested them */
	protected Map<NashRequest, MetricSubscription> channelMap = new ConcurrentHashMap<NashRequest, MetricSubscription>();
	
	/**
	 * Creates a new NashDestination
	 */
	public NashDestination() {
		super(".*");
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.BaseDestination#doStart()
	 */
	@Override
	protected void doStart() throws Exception {
		RequestHandlerRegistry.getInstance().register(this, getCommandName());
		super.doStart();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.BaseDestination#doStop()
	 */
	@Override
	protected void doStop() {
		super.doStop();
		for(NashRequest nr: channelMap.keySet()) {
			nr.end();
		}
		channelMap.clear();
		RequestHandlerRegistry.getInstance().remove(this.getClass());
	}
	
	
	/**
	 * Accept Route additive for BaseDestination extensions
	 * @param routable The metric to route
	 */
	@Override
	protected void doAcceptRoute(IMetric routable) {
		if(channelMap.isEmpty()) return;
		for(Map.Entry<NashRequest, MetricSubscription> entry: channelMap.entrySet()) {
			if(entry.getValue().matches(routable.getRoutingKey())) {
				entry.getKey().out(new StringBuilder(routable.toString()).append("\n"));
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.nash.handler.NashRequestHandler#onNashRequest(org.helios.nash.NashRequest)
	 */
	@Override
	public void onNashRequest(final NashRequest request) {
		String pattern = null;
		if(request.getArguments()==null || request.getArguments().length<1) {
			pattern = ".*";
		} else {
			pattern = request.getArguments()[0].replace("'", "");
		}		
		PatternMatch pm = PatternMatch.getInstance(pattern);		
		request.getChannel().getCloseFuture().addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture f) throws Exception {
				channelMap.remove(request);
			}
		});		
		channelMap.put(request, new MetricSubscription(pm, request.getChannel()));
		info("Added NashRequest from [", request.getChannel().getRemoteAddress(),"] with pattern [", pattern, "]");		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.nash.NashDestinationMXBean#getMetricSubscriptions()
	 */
	@Override
	@ManagedAttribute(description="The current subscribers")
	public Set<MetricSubscription> getMetricSubscriptions() {
		return new HashSet<MetricSubscription>(channelMap.values());
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.nash.handler.NashRequestHandler#getCommandName()
	 */
	@Override
	public String getCommandName() {
		return "metricsub";
	}	
	

}
