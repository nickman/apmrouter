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
package org.helios.apmrouter.destination.event;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.context.ApplicationEvent;

/**
 * <p>Title: DestinationEvent</p>
 * <p>Description: An application event published by destinations for specific lifecycle events</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.event.DestinationEvent</code></p>
 */
public abstract class DestinationEvent extends ApplicationEvent {
	/** The bean name of the destination */
	protected final String beanName;
	/** The event ID */
	protected final long id = serial.incrementAndGet();
	/** Serial number for event ID generation */
	private static final AtomicLong serial = new AtomicLong(0L);
	/**  */
	private static final long serialVersionUID = -4976212451972691435L;

	/**
	 * Creates a new DestinationEvent
	 * @param source The destination publishing the event
	 * @param beanName The bean name of the destination
	 */
	public DestinationEvent(Object source, String beanName) {
		super(source);
		this.beanName = beanName;
	}
	
	/**
	 * Indicates if this event references a starting destination
	 * @return true if this event references a starting destination, false otherwise
	 */
	public abstract boolean isStopping();
	
	/**
	 * Indicates if this event references a stopping destination
	 * @return true if this event references a stopping destination, false otherwise
	 */
	public abstract boolean isStarting();

	/**
	 * The bean name of the destination
	 * @return the beanName
	 */
	public String getBeanName() {
		return beanName;
	}

	/**
	 * Returns the event ID
	 * @return the event ID
	 */
	public long getId() {
		return id;
	}

}
