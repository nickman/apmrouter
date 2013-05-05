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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.server.ServerComponentBean;
import org.helios.apmrouter.util.StringHelper;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

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
	/** A map of subscriptions keyed by the rid */
	protected final Map<Integer, Subscription> subscriptionsByRid = new ConcurrentHashMap<Integer, Subscription>();
	
	private class Subscription implements ChannelFutureListener, Runnable, NotificationListener, NotificationFilter {
		/** The original JsonRequest for this sub */
		private final JsonRequest jsonRequest;
		/** The ObjectName subscribed to */
		private final ObjectName objectName;
		/** The schedule handle */
		private final ScheduledFuture<?> schedule;
		/** The channel owning the subscription */
		private final Channel channel;
		/** the names of the atributes we'll be retrieving for each matching ObjectName */
		private final Map<ObjectName, String[]> attributeNames;

		private long sentMessages = 0;

		/**
		 * Creates a new Subscription
		 * @param jsonRequest The original JsonRequest for this sub
		 * @param objectName The ObjectName subscribed to
		 * @param channel The channel to write events to
		 * @param frequency the frequency of the JMX poll in ms.
		 */
		public Subscription(JsonRequest jsonRequest, ObjectName objectName, Channel channel, long frequency) {
			this.jsonRequest = jsonRequest;
			this.objectName = objectName;			
			this.channel = channel;
			this.channel.getCloseFuture().addListener(this);
			Set<ObjectName> matches = JMXHelper.getHeliosMBeanServer().queryNames(objectName, null);
			attributeNames = new HashMap<ObjectName, String[]>(matches.size());
			for(ObjectName on: matches) {
				try {
					Set<String> numericAttrs = new HashSet<String>();
					for(Map.Entry<String, Object> attr : JMXHelper.getAttributes(on).entrySet()) {
						if(attr.getValue()!=null && Number.class.isInstance(attr.getValue())) {
							numericAttrs.add(attr.getKey());
						}
					}
					info("Will poll [", on, "] for these attributes:", numericAttrs.toString().replace("[", "[\n\t").replace("]", "\n]").replace(",", "\n\t"));
					attributeNames.put(on, numericAttrs.toArray(new String[numericAttrs.size()]));
				} catch (Exception ex) {}
			}
			this.schedule = scheduler.scheduleWithFixedDelay(this, frequency, frequency, TimeUnit.MILLISECONDS);
			subscriptions.put(channel.getId(), this);
		}
		
		/**
		 * {@inheritDoc}
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			try {
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("subkey", objectName.toString());				
				
				for(Map.Entry<ObjectName, String[]> entry: attributeNames.entrySet()) {
					Map<String, Object> onmap = JMXHelper.getAttributes(entry.getKey(), entry.getValue());
					map.put(entry.getKey().toString(), onmap);
				}
				jsonRequest.subResponse(objectName.toString()).setContent(map).send(channel);
				sentMessages++;
			} catch (Exception ex) {
				error("Subscription for channel [", channel, "] on ObjectName [", objectName, "] encountered error ", ex);
				jsonRequest.error(StringHelper.fastConcat("Subscription for channel [", channel.toString(), "] and ON [", objectName.toString(), "] encountered error "), ex).send(channel);
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
			info("Cancelling Subscription " , this);
			cancel(true);
		}

		/**
		 * {@inheritDoc}
		 * @see javax.management.NotificationFilter#isNotificationEnabled(javax.management.Notification)
		 */
		@Override
		public boolean isNotificationEnabled(Notification notification) {
			// TODO Auto-generated method stub
			return false;
		}

		/**
		 * {@inheritDoc}
		 * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
		 */
		@Override
		public void handleNotification(Notification notification,
				Object handback) {
			// TODO Auto-generated method stub
			
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return String.format(
					"Subscription [\\n\\tobjectName:%s, channel:%s]",
					objectName, channel);
		}
		
		
	}
	
	
	/**
	 * Simplified event subscription endpoint. The request contains a <b><code>java.lang</code></b> domain MXBean 
	 * and a frequency of updates in ms. When the caller is subscribed, a JSON dump of all the named MBean's attributes
	 * are published to the client on the specified frequency.
	 * @param request The subscribe request
	 * @param channel The channel to write the subscribe confirm and subsequent events.
	 */
	@JSONRequestHandler(name="subscribe")
	public void subscribeEvents(final JsonRequest request, final Channel channel) {
		info("Processing Subscription Request:", request);
		try {
			final String objName = request.getArgument("objectname");
			final long frequency = request.getArgument("freq", 10000L);
			new Subscription(request, JMXHelper.objectName(objName), channel, frequency);
			ChannelFutureListener completionListener = new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					info("SubReg Future:\n\tIsCancelled:", future.isCancelled(), 
							"\n\tIsDone:", future.isDone(),
							"\n\tIsSuccess:", future.isSuccess(),
							"\n\tChannel:", future.getChannel(),
							"\n\tCause:", future.getCause()							
					);
					if(!future.isDone()) {
						info("Subscription Request Incomplete");
					}
					if(future.isSuccess()) {						
						info("Subscription Request Complete:", request);
					} else {
						request.error(StringHelper.fastConcat("Subscription request for channel [", channel.toString(), "] on ObjectName [", objectName.toString(), "] failed"), future.getCause()).send(channel);
						error("Failed to send ", future.getCause());
					}
				}				
			};
			request.subConfirm(objName).send(completionListener, channel);
		} catch (Exception ex) {
			error("Failed to initiate subscription with [", request, "] from channel [", channel, "] ", ex);
			request.error(StringHelper.fastConcat("Subscription request for channel [", channel.toString(), "] on ObjectName [", objectName.toString(), "] failed"), ex).send(channel);
		}
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
