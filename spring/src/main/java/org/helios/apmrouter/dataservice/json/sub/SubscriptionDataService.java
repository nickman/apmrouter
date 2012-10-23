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

import org.helios.apmrouter.dataservice.json.JSONRequestHandler;
import org.helios.apmrouter.subscription.SubscriptionService;
import org.helios.apmrouter.subscription.criteria.builder.SubscriptionCriteriaBuilder;
import org.jboss.netty.channel.Channel;
import org.json.JSONException;
import org.json.JSONObject;
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
	
	
	@JSONRequestHandler(name="start")
	public void start(JSONObject request, Channel channel)  throws JSONException {
		SubscriptionCriteriaBuilder<?,?,?> builder = subService.getBuilder(request.getString("type"));
		SubscriptionCriteria<?,?,?> criteria =  builder.build(request);
		long sessionId = subService.startSubscriptionSession(channel);
		subService.addCriteria(channel, criteria)
	}
	
	@JSONRequestHandler(name="stop")
	public void stop(JSONObject request, Channel channel)  throws JSONException {
		
	}
	
	@JSONRequestHandler(name="stopall")
	public void stopAll(JSONObject request, Channel channel)  throws JSONException {
		
	}

}
