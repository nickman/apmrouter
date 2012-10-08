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
package org.helios.apmrouter.sender.netty.handler;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelState;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.ExceptionEvent;

/**
 * <p>Title: ChannelStateListener</p>
 * <p>Description: A handler placed in the pipeline to detect specific events and dispatch them back to the configured state change listener</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.net.listener.netty.handlers.state.ChannelStateListener</code></p>
 */
public class ChannelStateListener implements ChannelUpstreamHandler , ChannelDownstreamHandler{

	/** A map of sets of registered listeners keyed by the channel state they are interested in */
	protected final Map<ChannelState, Set<ChannelStateAware>> listeners = new EnumMap<ChannelState, Set<ChannelStateAware>>(ChannelState.class);
	
	/**
	 * Creates a new ChannelStateListener
	 */
	public ChannelStateListener() {
		for(ChannelState cs: ChannelState.values()) {
			listeners.put(cs, new CopyOnWriteArraySet<ChannelStateAware>());
		}
	}
	
	/**
	 * Adds a {@link ChannelStateAware} to be notified of Channel State changes
	 * @param csa The {@link ChannelStateAware} to add
	 */
	public void addChannelStateAware(ChannelStateAware csa) {
		if(csa!=null) {
			for(ChannelState cs: csa.getInterestedChannelStates()) {
				listeners.get(cs).add(csa);
			}
		}
	}
	
	/**
	 * Removes a {@link ChannelStateAware} from Channel State change notifications
	 * @param csa The {@link ChannelStateAware} to remove
	 */
	public void removeChannelStateAware(ChannelStateAware csa) {
		if(csa!=null) {
			for(ChannelState cs: csa.getInterestedChannelStates()) {
				listeners.get(cs).remove(csa);
			}
		}
	}
	

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelUpstreamHandler#handleUpstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		processEvent(true, e);		
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelDownstreamHandler#handleDownstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	@Override
	public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		processEvent(false, e);
	}
	
	/**
	 * Tests the passed {@link ChannelEvent} and if it is a {@link ChannelStateEvent}, dispatches it to listeners interested in that state 
	 * @param up true if it was an upstream event, false if it was downstream
	 * @param ce The channel event
	 */
	protected void processEvent(boolean up, ChannelEvent ce) {
		if(ce!=null && (ce instanceof ChannelStateEvent)) {
			ChannelStateEvent cse = (ChannelStateEvent)ce;
			for(ChannelStateAware csa : listeners.get(cse.getState())) {
				csa.onChannelStateEvent(up, cse);
			}
		}
	}

	
	/**
	 * @param ctx
	 * @param e
	 */
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
		System.err.println("[ChannelStateListener] Caught exception event [" + e.getCause() + "]");
	}

}
