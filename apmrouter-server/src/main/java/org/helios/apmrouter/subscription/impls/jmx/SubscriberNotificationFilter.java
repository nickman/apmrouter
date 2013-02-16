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
package org.helios.apmrouter.subscription.impls.jmx;

import java.util.Map;
import java.util.regex.Pattern;

import javax.management.Notification;
import javax.management.NotificationFilter;

import org.helios.apmrouter.dataservice.json.JsonRequest;

/**
 * <p>Title: SubscriberNotificationFilter</p>
 * <p>Description: A simplified notification filter that is created and configured based on a set of regular expressions supplied by a remote client.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.subscription.impls.jmx.SubscriberNotificationFilter</code></p>
 */

public class SubscriberNotificationFilter implements NotificationFilter {
	/**  */
	private static final long serialVersionUID = 685757223811880023L;
	/** The map key for the regex to filter by notification type */
	public static final String KEY_NOTIF_TYPE = "nt";
	/** The map key for the regex to filter by the notification class name */
	public static final String KEY_NOTIF_CN = "nc";
	/** The map key for the regex to filter by the notification soure (technically the <code>toString</code> of the source */
	public static final String KEY_NOTIF_SRC = "ns";
	/** The map key for the regex to filter by the user data (technically the <code>toString</code> of the userdata */
	public static final String KEY_NOTIF_USR_DATA = "nu";
	/** The map key for the regex to filter by the handback (technically the <code>toString</code> of the handback */
	public static final String KEY_NOTIF_HANDBACK = "nh";
	/** The default pattern used if no pattern is supplied */
	public static final String DEFAULT_PATTERN = ".*";
	
	/** The match pattern against the notification type */
	protected final Pattern notifType;
	/** The match pattern against the notification class name */
	protected final Pattern notifClass;
	/** The match pattern against the notification source */
	protected final Pattern notifSrc;
	/** The match pattern against the notification user data */
	protected final Pattern notifUsrData;
	/** The match pattern against the notification handback */
	protected final Pattern notifHandback;
	
	private SubscriberNotificationFilter(JsonRequest request) {		
		notifType = Pattern.compile(request.getArgument(KEY_NOTIF_TYPE, DEFAULT_PATTERN));
		notifClass = Pattern.compile(request.getArgument(KEY_NOTIF_TYPE, DEFAULT_PATTERN));
		notifSrc = null;
		notifUsrData = null;
		notifHandback = null;
	}
	
	/**
	 * Extracts the keyed string pattern from the json request 
	 * @param key The key name
	 * @param request The json request's arguments map
	 * @return the compiled pattern, or null if the key did not resolve
	 */
	private static Pattern exp(String key, Map<Object, Object> args ) {
		String p = (String) args.get(key);
		if(p==null) return null;
		return Pattern.compile(p);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationFilter#isNotificationEnabled(javax.management.Notification)
	 */
	@Override
	public boolean isNotificationEnabled(Notification notification) {
		return false;
	}

}
