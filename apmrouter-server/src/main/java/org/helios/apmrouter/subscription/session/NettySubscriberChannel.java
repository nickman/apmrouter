/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2012, Helios Development Group and individual contributors
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
package org.helios.apmrouter.subscription.session;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;

/**
 * <p>Title: NettySubscriberChannel</p>
 * <p>Description: Implementation of {@link AbstractSubscriberChannel} using a Netty {@link Channel} as the event sender transport</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.subscription.session.NettySubscriberChannel</code></p>
 */

public class NettySubscriberChannel extends AbstractSubscriberChannel {
	/** The event sender transport channel */
	protected final Channel channel;
	
	/**
	 * Creates a new NettySubscriberChannel
	 * @param channel The event sender transport channel
	 */
	public NettySubscriberChannel(Channel channel) {
		super(channel.getId());
		this.channel = channel;
		this.channel.getCloseFuture().addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				session.terminate();
			}
		});
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.subscription.session.SubscriberChannel#send(java.lang.Object)
	 */
	@Override
	public void send(Object event) {
		if(channel.isOpen()) {
			channel.write(event).addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture f) throws Exception {
					if(f.isSuccess()) {
						incr("DispatchedEvents");					
					} else {
						incr("DroppedEvents");
					}					
				}
			});
		}
	}

}
