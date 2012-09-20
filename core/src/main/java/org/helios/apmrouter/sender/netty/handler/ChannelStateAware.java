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

import org.jboss.netty.channel.ChannelState;
import org.jboss.netty.channel.ChannelStateEvent;

/**
 * <p>Title: ChannelStateAware</p>
 * <p>Description: Defines a class that wants to be notified of state changes in the pipeline</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.net.listener.netty.handlers.state.ChannelStateAware</code></p>
 */
public interface ChannelStateAware {
	
	/**
	 * Returns the channel states that the aware object wants to notified of
	 * @return and array of {@link ChannelState}s.
	 */
	public ChannelState[] getInterestedChannelStates();
	
	/**
	 * Callback from the channel state listener indicating an interesting event has occured
	 * @param upstream true if the event was an upstream event, false if it was downstream 
	 * @param stateEvent the {@link ChannelStateEvent}
	 */
	public void onChannelStateEvent(boolean upstream, ChannelStateEvent stateEvent);
}
