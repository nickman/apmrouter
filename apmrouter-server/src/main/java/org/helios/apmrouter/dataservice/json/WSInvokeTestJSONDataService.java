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
package org.helios.apmrouter.dataservice.json;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.ObjectName;

import org.helios.apmrouter.server.ServerComponentBean;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;

/**
 * <p>Title: WSInvokeTestJSONDataService</p>
 * <p>Description: A testing service providing various different wsinvoke invocation endpoints.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.dataservice.json.WSInvokeTestJSONDataService</code></p>
 */
@JSONRequestHandler(name="wsinvoke")
public class WSInvokeTestJSONDataService extends ServerComponentBean {
	/** The scheduler to generate subscriber events */
	protected final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, new ThreadFactory(){
		protected final AtomicInteger serial = new AtomicInteger();
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "WSInvokeTestJSONDataServiceSubThread#" + serial.incrementAndGet());
			t.setDaemon(true);
			return t;
		}		
	});
	
	/** A map of subscriptions keyed by the channel ID */
	protected final Map<Integer, Subscription> subscriptions = new ConcurrentHashMap<Integer, Subscription>();
	
	private class Subscription implements ChannelFutureListener, Runnable {
		/** The ObjectName subscribed to */
		private final ObjectName objectName;
		/** The schedule handle */
		private final ScheduledFuture<?> schedule;
		/** The channel owning the subscription */
		private final Channel channel;

		/**
		 * Creates a new Subscription
		 * @param objectName The ObjectName subscribed to
		 * @param frequency The frequency of the subscription's events in ms.
		 */
		public Subscription(ObjectName objectName, Channel channel, long frequency) {
			this.objectName = objectName;			
			this.channel = channel;
			this.channel.getCloseFuture().addListener(this);
			if(this.objectName.isPattern()) {
				
			} else {
				
			}
			this.schedule = scheduler.scheduleWithFixedDelay(this, 0, frequency, TimeUnit.MILLISECONDS);			
		}
		
		public void run() {
			try {
				
			} catch (Exception ex) {
				error("Subscription for channel [", channel, "] and ON [", objectName, "] encountered error ", ex);
				// FIXME: Send an error response
			}
		}
		
		/**
		 * Cancels this subscription and deallocates any associated resources
		 * @param internal true if this cancel is being called internally (i.e. triggered by the channel closes
		 */
		public void cancel(boolean internal) {
			schedule.cancel(true);
			if(internal) {
				subscriptions.remove(channel.getId());
			}
		}

		/**
		 * <p>Cancels this subscription.</p>
		 * {@inheritDoc}
		 * @see org.jboss.netty.channel.ChannelFutureListener#operationComplete(org.jboss.netty.channel.ChannelFuture)
		 */
		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			cancel(true);
		}
		
		
	}
	
	
	/**
	 * Simplified event subscription endpoint. The request contains a <b><code>java.lang</code></b> domain MXBean 
	 * and a frequency of updates in ms. When the caller is subscribed, a JSON dump of all the named MBean's attributes
	 * are published to the client on the specified frequency.
	 * @param request The subscribe request
	 * @param channel The channel to write the subscribe confirm and subsequent events.
	 */
	public void subscribeEvents(final JsonRequest request, final Channel channel) {
		
	}

	/**
	 * Client diagnostic helper. Echos the passed content in <b>args</b> keyed by <b>msg</b>.
	 * If a numeric arg is passed keyed by <b>sleep</b>, the service will sleep that number of ms. before responding. 
	 * @param request The JSON request
	 * @param channel The channel to write the response to
	 */
	@JSONRequestHandler(name="echo")
	public void echo(final JsonRequest request, final Channel channel) {
		final String message = request.getArgument("msg");		
		final long sleep = getSleep(request.getArgument("sleep"));
		channel.getPipeline().execute(new Runnable(){
			@Override
			public void run() {
				info("Echo Request [", message, "] sleeping for [", sleep, "] ms.");
				try { Thread.currentThread().join(sleep); } catch (Exception es) {}
				try {
					channel.write(request.response().setContent(message)).addListener(new ChannelFutureListener() {
						@Override
						public void operationComplete(ChannelFuture future) throws Exception {
							if(future.isSuccess()) {
								info("Returned response for Echo Request [", message, "] sleeping for [", sleep, "] ms.");
							} else {
								error("Failed to Echo Request [", message, "] sleeping for [", sleep, "] ms. ", future.getCause());
							}							
						}
					});
				} catch (Exception ex) {
					error("Failed to send response on echo request ", ex);
				}
			}
		});		
	}
	
	private long getSleep(String sleep) {
		try {
			return Math.abs(Long.parseLong(sleep.trim()));
		} catch (Exception ex) {
			return 0;
		}
	}
	

}
