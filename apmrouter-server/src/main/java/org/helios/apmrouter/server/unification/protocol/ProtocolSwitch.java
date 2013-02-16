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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.helios.apmrouter.server.ServerComponentBean;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.MessageEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.jmx.export.annotation.ManagedAttribute;

/**
 * <p>Title: ProtocolSwitch</p>
 * <p>Description: An upfront channel handler to determine if the incoming is HTTP or plain socket text for submissions</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.unification.protocol.ProtocolSwitch</code></p>
 */
public class ProtocolSwitch extends ServerComponentBean implements ChannelUpstreamHandler {
	
	/** A map of {@link  ProtocolInitiator}s keyed by their bean name */
	protected final Map<String, ProtocolInitiator> initiators = new ConcurrentHashMap<String, ProtocolInitiator>();
	/** A map of {@link  ProtocolInitiator}s keyed by a magic long know to be a match */
	protected final Map<Long, ProtocolInitiator> cachedMatchInitiators = new ConcurrentHashMap<Long, ProtocolInitiator>();
	
	/**
	 * Creates a new ProtocolSwitch and adds {@link ProtocolInitiatorStarted} and {@link ProtocolInitiatorStopped} to
	 * the supported application event set.
	 */
	public ProtocolSwitch() {
		super();
		supportedEventTypes.add(ProtocolInitiatorStarted.class);
		supportedEventTypes.add(ProtocolInitiatorStopped.class);
	}
	
	/**
	 * On start, searches the app context for {@link ProtocolInitiator}s not already registered.
	 * @param event The app context refresh event
	 */
	@Override
	public void onApplicationContextRefresh(ContextRefreshedEvent event) {
		Map<String, ProtocolInitiator> inits = event.getApplicationContext().getBeansOfType(ProtocolInitiator.class);
		if(!inits.isEmpty()) {
			for(Map.Entry<String, ProtocolInitiator> entry: inits.entrySet()) {
				if(!initiators.containsKey(entry.getKey())) {
					initiators.put(entry.getKey(), entry.getValue());
					info("Adding Discovered ProtocolInitiator [", entry.getKey(), "]" );
				}
			}
		}
	}

	/**
	 * Called when a {@link ProtocolInitiator} starts
	 * @param protocolInitiatorStarted The {@link ProtocolInitiator} start event
	 */
	public void onApplicationEvent(ProtocolInitiatorStarted protocolInitiatorStarted) {
		ProtocolInitiator pi = protocolInitiatorStarted.getInitiator();
		if(!initiators.containsKey(pi.getBeanName())) {
			initiators.put(pi.getBeanName(), pi);
			info("Adding Started ProtocolInitiator [", pi.getBeanName(), "]" );
		}
	}
	
	/**
	 * Called when a {@link ProtocolInitiator} stops
	 * @param protocolInitiatorStopped The {@link ProtocolInitiator} stop event
	 */
	public void onApplicationEvent(ProtocolInitiatorStopped protocolInitiatorStopped) {
		ProtocolInitiator pi = protocolInitiatorStopped.getInitiator();
		if(initiators.remove(pi.getBeanName())!=null) {
			info("Removing stopped ProtocolInitiator [", pi.getBeanName(), "]" );
		}
	}
	
	
	
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelUpstreamHandler#handleUpstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		if(e instanceof MessageEvent) {
			MessageEvent me = (MessageEvent) e;
			if(me.getMessage() instanceof ChannelBuffer) {
				protocolSwitch(ctx, e.getChannel(), (ChannelBuffer)me.getMessage(), e);
			}
		}
		ctx.sendUpstream(e);		
	}	
	

	/**
	 * Examines the first 2 unsigned bytes of the channel buffer and attempts to match the protocol of the request and invoke the matching {@link ProtocolInitiator}.
	 * @param ctx The channel handler context
	 * @param channel The channel
	 * @param buffer The message buffer
	 * @param e The channel event
	 */
	protected void protocolSwitch(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer, ChannelEvent e)  {
		// Will use the first two bytes to detect a protocol.
		if (buffer.readableBytes() < 2) {			
			return;
		}	
		ChannelPipeline pipeline = ctx.getPipeline();
		final int magic1 = buffer.getUnsignedByte(buffer.readerIndex());  // 22 and 3 for RMI/JMX
		final int magic2 = buffer.getUnsignedByte(buffer.readerIndex() + 1);
		final long magicLong = buffer.getLong(buffer.readerIndex());
		
		if(!cachedMatchInitiators.isEmpty()) {
			ProtocolInitiator pi  = cachedMatchInitiators.get(magicLong);
			if(pi!=null) {
				pi.modifyPipeline(ctx, channel, buffer);
				ctx.getPipeline().remove(this);
				return;
			}
		}
		for(ProtocolInitiator pi : initiators.values()) {
			if(pi.match(magic1, magic2)) {
				cachedMatchInitiators.put(magicLong, pi);
				pi.modifyPipeline(ctx, channel, buffer);
				ctx.getPipeline().remove(this);
				return;
			}
			if(pi.match(buffer)) {
				pi.modifyPipeline(ctx, channel, buffer);
				ctx.getPipeline().remove(this);
				return;				
			}
		}
		// if we get here, it means we did not find a protocol match
		// so pass to the default protocol initiator.
	}
	
	/**
	 * Returns a map of {@link ProtocolInitiator} bean names keyed by protocol name they implement
	 * @return a map of {@link ProtocolInitiator} bean names keyed by protocol name they implement
	 */
	@ManagedAttribute(description="The supported protocols")
	public Map<String, String> getProtocols() {
		Map<String, String> map = new HashMap<String, String>();
		for(ProtocolInitiator pi: initiators.values()) {
			map.put(pi.getProtocol(), pi.getBeanName());
		}
		return map;
	}
	
	


}
