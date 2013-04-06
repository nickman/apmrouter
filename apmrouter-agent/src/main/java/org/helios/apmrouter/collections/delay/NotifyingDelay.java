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
package org.helios.apmrouter.collections.delay;

import java.util.concurrent.Delayed;

/**
 * <p>Title: NotifyingDelay</p>
 * <p>Description: An extended {@link Delayed} interface that emits a notification when its delay driver is updated. This is
 * intended to notify its parent container that it should resort its content.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.collections.delay.NotifyingDelay</code></p>
 * @param <T> The expected type of the {@link DelayChangeReceiver} that will be injected
 */

public interface NotifyingDelay<T extends DelayChangeReceiver<?>> extends Delayed {
	/**
	 * Callback from a dynamic delay queue when an item is added to the queue, providing the item with the queue's notification receiver
	 * that it should invoke when its delay driver changes
	 * @param receiver The receiver instance that should be invoked
	 */
	public void setDelayChangeReceiver(T receiver);
	
	/**
	 * The timestamp to set on the notifying delay once it has been dequeued
	 * @param timestamp The new timetamp
	 */
	public void setUpdatedTimestamp(long timestamp);
	
//	/**
//	 * Freezes the comparable so that items retain a consistent identity during the timestamp update
//	 */
//	public void freeze();
//	
//	/**
//	 * Thaws the comparable after the timestamp update
//	 */
//	public void thaw();
	
	/**
	 * Callback from the delay queue when this item is permamnently removed from the queue as a result of an expiry.
	 */
	public void clearDelayChangeReceiver();
}
