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
package org.helios.apmrouter.catalog;

import java.util.Arrays;

import com.google.gson.annotations.SerializedName;

/**
 * <p>Title: DChannelEvent</p>
 * <p>Description: Serializable event indicating a host and agent state change</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.services.session.DChannelEvent</code></p>
 */
public class DChannelEvent {
	/** The event type */
	@SerializedName("event")
	public final DChannelEventType eventType;
	/** The host's domain */
	@SerializedName("d")
	public final String[] domain;
	/** The host */
	@SerializedName("h")
	public final String host;
	/** The host id */
	@SerializedName("hi")
	public final int hostId;
	/** The agent */
	@SerializedName("a")
	public final String agent;
	/** The agent id */
	@SerializedName("ai")
	public final int agentId;	
	/** Indicates if a connect is the first for the host, or if a disconnect is the last for a host */
	@SerializedName("hc")
	public final boolean hostChange;
	
	
	
	/**
	 * Creates a new DChannelEvent
	 * @param eventType The state change event type
	 * @param domain The host's domain
	 * @param host The host name
	 * @param hostId The host ID
	 * @param agent The agent name
	 * @param agentId The agent ID
	 * @param hostChange Indicates if a connect is the first for the host, or if a disconnect is the last for a host
	 */
	public DChannelEvent(DChannelEventType eventType, String[] domain,
			String host, int hostId, String agent, int agentId,
			boolean hostChange) {
		this.eventType = eventType;
		this.domain = domain;
		this.host = host;
		this.hostId = hostId;
		this.agent = agent;
		this.agentId = agentId;
		this.hostChange = hostChange;
	}

	/**
	 * Creates a new DChannelEvent
	 * @param eventType The state change event type
	 * @param domain The host's domain
	 * @param host The host name
	 * @param hostId The host ID
	 * @param agent The agent name
	 * @param agentId The agent ID
	 * @param hostChange Indicates if a connect is the first for the host, or if a disconnect is the last for a host
	 * @return The new DChannelEvent
	 */
	public static DChannelEvent newEvent(DChannelEventType eventType, String[] domain,
			String host, int hostId, String agent, int agentId,
			boolean hostChange) {
		return new DChannelEvent(eventType, domain, host, hostId, agent, agentId, hostChange);
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DChannelEvent [eventType=");
		builder.append(eventType);
		builder.append(", domain=");
		builder.append(Arrays.toString(domain));
		builder.append(", host=");
		builder.append(host);
		builder.append(", hostId=");
		builder.append(hostId);
		builder.append(", agent=");
		builder.append(agent);
		builder.append(", agentId=");
		builder.append(agentId);
		builder.append(", hostChange=");
		builder.append(hostChange);
		builder.append("]");
		return builder.toString();
	}
	

	
	
}