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
package org.helios.apmrouter.dataservice.json.jmx;

import javax.management.MBeanServer;

import org.helios.apmrouter.dataservice.json.JSONRequestHandler;
import org.helios.apmrouter.dataservice.json.JsonRequest;
import org.helios.apmrouter.dataservice.json.JsonResponse;
import org.jboss.netty.channel.Channel;

/**
 * <p>Title: JMXDataService</p>
 * <p>Description: Data and subscription services for JMX</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.dataservice.json.jmx.JMXDataService</code></p>
 */
@JSONRequestHandler(name="jmx")
public class JMXDataService {

	/*
	 * var r2 = "{t:'req', svc:'hostagent', op:'listhosts', args:[false]}";
	 */
	
	/**
	 * <p>Creates a subscription for JMX notifications on the specified mbeans.</p>
	 * <p>Args are <b>map</b> based:<ol>
	 * 	<li><b>server</b>: (Optional) The jmx domain of the {@link MBeanServer} to subscribe to. Default is <b><code>DefaultDomain</code></b>.</li>
	 *  <li><b>on</b>: (Mandatory) The object name of the MBeans to subscribe to.</li>
	 * </ol></p>
	 * <p>Sample request:
	 * <pre>"{t:'req', svc:'jmx', op:'subscribe', args:{}}"</pre>
	 * </p>
	 * @param request The JSON request
	 * @param channel The channel to respond on
	 */
	@JSONRequestHandler(name="sub")
	public void subscribe(JsonRequest request, Channel channel)   {
		boolean onlineOnly = request.getArgument(0, false);
//		Map<Integer, String> hosts = catalog.listHosts(onlineOnly);
		JsonResponse response = request.response();
//		response.setContent(hosts);
		channel.write(response);
	}

}
