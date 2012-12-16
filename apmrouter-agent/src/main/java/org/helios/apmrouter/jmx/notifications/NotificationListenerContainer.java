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
package org.helios.apmrouter.jmx.notifications;

import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;

/**
 * <p>Title: NotificationListenerContainer</p>
 * <p>Description: Wraps and delegates to pairs of a {@link NotificationListener} and a {@link NotificationFilter}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.jmx.notifications.NotificationListenerContainer</code></p>
 */

public class NotificationListenerContainer implements NotificationListener, NotificationFilter {
	/**  */
	private static final long serialVersionUID = -6820674878866680096L;
	/** The triplet's listener */
	private final NotificationListener listener;
	/** The triplet's optional filter */
	private final NotificationFilter filter;
	/** The contextual handback */
	private final Object handback;
	
	/**
	 * Creates a new NotificationListenerContainer
	 * @param listener The triplet's listener
	 * @param filter The triplet's optional filter
	 * @param handback The contextual handback
	 */
	public NotificationListenerContainer(NotificationListener listener, NotificationFilter filter, Object handback) {
		this.listener = listener;
		this.filter = filter;
		this.handback = handback;
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationFilter#isNotificationEnabled(javax.management.Notification)
	 */
	@Override
	public boolean isNotificationEnabled(Notification notification) {
		return filter==null ? true : filter.isNotificationEnabled(notification);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
	 */
	@Override
	public void handleNotification(Notification notification, Object notAhandback) {
		if(isNotificationEnabled(notification)) {
			listener.handleNotification(notification, handback);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((filter == null) ? 0 : filter.hashCode());
		result = prime * result
				+ ((handback == null) ? 0 : handback.hashCode());
		result = prime * result
				+ ((listener == null) ? 0 : listener.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		NotificationListenerContainer other = (NotificationListenerContainer) obj;
		if (filter == null) {
			if (other.filter != null) {
				return false;
			}
		} else if (!filter.equals(other.filter)) {
			return false;
		}
		if (handback == null) {
			if (other.handback != null) {
				return false;
			}
		} else if (!handback.equals(other.handback)) {
			return false;
		}
		if (listener == null) {
			if (other.listener != null) {
				return false;
			}
		} else if (!listener.equals(other.listener)) {
			return false;
		}
		return true;
	}


	
	
}
