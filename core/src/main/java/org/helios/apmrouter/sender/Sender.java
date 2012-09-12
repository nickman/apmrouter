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
package org.helios.apmrouter.sender;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.helios.apmrouter.sender.netty.UDPSender;

/**
 * <p>Title: Sender</p>
 * <p>Description: The sender factory and singleton</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.sender.Sender</code></p>
 */

public class Sender {
	/** The Sender singleton instance */
	private static volatile Sender instance = null;
	/** The Sender singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** The configured apmrouter URIs */
	private final Set<URI>  endpoints = new HashSet<URI>();
	/** The configured apmrouter senders */
	private final Map<URI, ISender>  senders = new TreeMap<URI, ISender>();
	
	
	/** The name of the system property that specifies the apm router URIs*/
	public static final String SENDER_URI_PROP = "org.helios.apmrouter.uri";
	/** The default apmrouter URI */
	public static final String DEFAULT_SENDER_URI = "udp://localhost:2094";

	
	/**
	 * Acquires the Sender singleton instance
	 * @return the Sender singleton instance
	 */
	public static Sender getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new Sender();
				}
			}
		}
		return instance;
	}
	
	
	/**
	 * Creates a new Sender
	 */
	private Sender() {
		String uris = System.getProperty(SENDER_URI_PROP, DEFAULT_SENDER_URI);
		for(String uri: uris.split(",")) {
			try {
				if(!uri.trim().isEmpty()) {
					endpoints.add(new URI(uri.trim()));
				}
			} catch (Exception e) {}
		}
		if(endpoints.isEmpty()) {
			try {
				endpoints.add(new URI(DEFAULT_SENDER_URI));
			} catch (URISyntaxException e) {
				throw new RuntimeException("Failed to add default endpoint URI [" + DEFAULT_SENDER_URI + "]", e);
			}
		}
		for(URI uri: endpoints) {
			senders.put(uri, UDPSender.getInstance(uri));
		}
	}
	
	
	/**
	 * Returns the default sender
	 * @return the default sender
	 */
	public ISender getDefaultSender() {
		return senders.values().iterator().next();
	}
}
