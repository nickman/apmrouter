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
package org.helios.apmrouter.server.services.session;

import org.springframework.context.ApplicationEvent;

/**
 * <p>Title: ChannelSessionEvent</p>
 * <p>Description: The common base class for channel session events</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.server.services.session.ChannelSessionEvent</code></p>
 */

public abstract class ChannelSessionEvent extends ApplicationEvent {

	/**  */
	private static final long serialVersionUID = -5916164030401475870L;

	/**
	 * Creates a new ChannelSessionEvent
	 * @param source The channel
	 */
	public ChannelSessionEvent(DecoratedChannelMBean source) {
		super(source);
	}
	
	/**
	 * <p>Title: ChannelSessionStartedEvent</p>
	 * <p>Description: Spring application event published when a channel session starts</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.server.services.session.ChannelSessionEvent.ChannelSessionStartedEvent</code></p>
	 */
	public static class ChannelSessionStartedEvent extends ChannelSessionEvent {
		/**  */
		private static final long serialVersionUID = -5593920964283374871L;

		/**
		 * Creates a new ChannelSessionStartedEvent
		 * @param source the connected channel
		 */
		public ChannelSessionStartedEvent(DecoratedChannelMBean source) {
			super(source);
		}
		
	}
	
	/**
	 * <p>Title: ChannelSessionStoppedEvent</p>
	 * <p>Description: Spring application event published when a channel session closes</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.server.services.session.ChannelSessionEvent.ChannelSessionStoppedEvent</code></p>
	 */
	public static class ChannelSessionStoppedEvent extends ChannelSessionEvent {

		/**  */
		private static final long serialVersionUID = -8744869071142569182L;

		/**
		 * Creates a new ChannelSessionStartedEvent
		 * @param source the connected channel
		 */
		public ChannelSessionStoppedEvent(DecoratedChannelMBean source) {
			super(source);
		}
		
	}
	

}
