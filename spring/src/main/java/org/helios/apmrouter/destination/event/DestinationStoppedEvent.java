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

/**
 * <p>Title: DestinationStoppedEvent</p>
 * <p>Description: An application event published by a destination when it stops</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.destination.event.DestinationStoppedEvent</code></p>
 */
public class DestinationStoppedEvent extends DestinationEvent {

	/**  */
	private static final long serialVersionUID = -4195736079355271761L;

	/**
	 * Creates a new DestinationStoppedEvent
	 * @param source The destination that stopped
	 * @param beanName The bean name of the destination
	 */
	public DestinationStoppedEvent(Object source, String beanName) {
		super(source, beanName);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.event.DestinationEvent#isStopping()
	 */
	public boolean isStopping() {
		return true;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.destination.event.DestinationEvent#isStarting()
	 */
	public boolean isStarting() {
		return false;
	}


}
