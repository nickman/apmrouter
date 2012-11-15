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
package org.helios.apmrouter.dataservice.json.sub;

import static org.helios.apmrouter.subscription.criteria.builder.SubscriptionCriteriaBuilder.JSON_EVENT_SOURCE;

import org.apache.log4j.Logger;
import org.helios.apmrouter.dataservice.json.JSONRequestHandler;
import org.helios.apmrouter.dataservice.json.JsonRequest;
import org.helios.apmrouter.subscription.SubscriptionService;
import org.helios.apmrouter.subscription.criteria.SubscriptionCriteria;
import org.helios.apmrouter.subscription.criteria.builder.SubscriptionCriteriaBuilder;
import org.helios.apmrouter.subscription.session.SubscriptionSession;
import org.jboss.netty.channel.Channel;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>Title: SubscriptionDataService</p>
 * <p>Description: A JSON data service to sign up for and cancel event subscriptions</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.dataservice.json.sub.SubscriptionDataService</code></p>
 */
@JSONRequestHandler(name="sub")
public class SubscriptionDataService {
	/** The subscription management service */
	@Autowired(required=true)
	protected SubscriptionService subService = null;
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	
	
	/**
	 * Starts a subscription
	 * @param request The JSON subscription request
	 * @param channel The subscribing channel
	 */
	@JSONRequestHandler(name="start")
	public void start(JsonRequest request, Channel channel)  {
		SubscriptionCriteriaBuilder<?,?,?> builder = subService.getBuilder(request.arguments.get(JSON_EVENT_SOURCE).toString());
		SubscriptionCriteria<?,?,?> criteria =  builder.build(request);
		long subId = subService.addCriteria(channel, criteria, request);
		log.info("Started subId [" + subId + "] for channel [" + channel + "] with criteria [" + criteria + "]");
	}
	
	/**
	 * Stops a subscription
	 * @param request The JSON stop subscription request
	 * @param channel The subscribed channel
	 */
	@JSONRequestHandler(name="stop")
	public void stop(JsonRequest request, Channel channel) {
		Number subId = request.getArgumentOrNull("subId", Number.class);
		if(subId!=null) {
			long criteriaId = subId.longValue();
			SubscriptionSession subSession = subService.getSubscriptionSession(channel);
			if(subSession!=null) {
				SubscriptionCriteria<?,?,?> sc = subSession.cancelCriteria(criteriaId);
				if(sc!=null) {
					log.info("Cancelled Subscription ID [" + criteriaId + "] for channel [" + channel + "]. Criteria was:\n" + sc);
				}
			}
		}
	}
	
	/**
	 * Stops all subscriptions for a channel
	 * @param request The JSON stop all subscription request
	 * @param channel The subscribed channel
	 */
	@JSONRequestHandler(name="stopall")
	public void stopAll(JsonRequest request, Channel channel) {
		SubscriptionSession subSession = subService.getSubscriptionSession(channel);
		
	}

}
