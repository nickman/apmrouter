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
package org.helios.apmrouter.byteman.sockets;

import org.helios.apmrouter.util.SimpleLogger;

import java.util.concurrent.atomic.AtomicReference;

import static org.helios.apmrouter.util.Methods.nvl;

/**
 * <p>Title: SocketTracingLevel</p>
 * <p>Description: Functional Enumeration of the levels of verbosity available for the socket monitor </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.byteman.SocketTracingLevel</code></p>
 */
public enum SocketTracingLevel {
	/** Traces active connections only */
	CONNECTIONS(false, false),
	/** Traces active connections and traffic by address */
	ADDRESS_TRAFFIC(true, false),
	/** Traces active connections and traffic by address/port */
	PORT_TRAFFIC(false, true),
	/** Traces active connections and traffic by address/port, and summary by address */
	ADDRESS_PORT_TRAFFIC(false, true);
	
	/**
	 * Creates a new SocketTracingLevel
	 * @param addressCollecting Indicates if this level implements collecting by address
	 * @param portCollecting Indicates if this level implements collecting by address/port
	 */
	private SocketTracingLevel(boolean addressCollecting, boolean portCollecting) {
		this.addressCollecting = addressCollecting;
		this.portCollecting = portCollecting;
	}
	
	/** Indicates if this level implements collecting by address */
	private final boolean addressCollecting;
	/** Indicates if this level implements collecting by address */
	private final boolean portCollecting;
	
	/** The name of the system property the specifies the socket tracing level */
	public static final String LEVEL_PROP = "org.helios.apmrouter.socketlevel";
	
	/**
	 * Indicates if this level implements active connections only
	 * @return true if this level implements active connections only, false otherwise
	 */
	public boolean isActiveConnections() {
		return !addressCollecting && !portCollecting;
	}
	
	
	/**
	 * Indicates if this level implements collecting by address
	 * @return true if this level implements collecting by address, false otherwise
	 */
	public boolean isAddressCollecting() {
		return addressCollecting;
	}

	/**
	 * Indicates if this level implements collecting by address and port
	 * @return true if this level implements collecting by address and port, false otherwise
	 */
	public boolean isPortCollecting() {
		return portCollecting;
	}

	
	/**
	 * Decodes the passed name to a SocketTracingLevel level.
	 * Throws a runtime exception if the name is invalid
	 * @param name The SocketTracingLevel name to decode. Trimmed and uppercased.
	 * @return the decoded SocketMetric
	 */
	public static SocketTracingLevel valueOfName(CharSequence name) {
		String n = nvl(name, "SocketTracingLevel Name").toString().trim().toUpperCase();
		try {
			return SocketTracingLevel.valueOf(n);
		} catch (Exception e) {
			throw new IllegalArgumentException("The passed name [" + name + "] is not a valid SocketTracingLevel name", new Throwable());
		}
	}
	
	/**
	 * Reads the system property named {@literal SocketTracingLevel#LEVEL_PROP} and returns the configured level.
	 * If the level is not configured, or the configured name is invalid, returns {@link SocketTracingLevel#CONNECTIONS}.
	 * @return the configured SocketTracingLevel.
	 */
	public static SocketTracingLevel getLevel() {
		String prop = System.getProperty(LEVEL_PROP, null);
		if(prop==null) {
			System.setProperty(LEVEL_PROP, CONNECTIONS.name());
			return CONNECTIONS;
		}
		try {
			return valueOfName(prop);
		} catch (Exception ex) {
			SimpleLogger.warn("Invalid SocketTracingLevel Specified [", prop, "]. Setting to the default level of CONNECTIONS");
			System.setProperty(LEVEL_PROP, CONNECTIONS.name());
			return CONNECTIONS;
		}
	}
	
	/**
	 * <p>Title: SocketTracingLevelListener</p>
	 * <p>Description: Defines an interface for listeners that need to be notified of changes in a specified SocketTracingLevel.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.byteman.SocketTracingLevel.SocketTracingLevelListener</code></p>
	 */
	public static interface SocketTracingLevelListener {
		/**
		 * Fired when the old value was null and the new value is the passed value
		 * @param newLevel the new SocketTracingLevel
		 */
		public void fromNullTo(SocketTracingLevel newLevel);
		
		/**
		 * Fired when the old value is replace by null
		 * @param oldLevel the new SocketTracingLevel
		 */
		public void toNull(SocketTracingLevel oldLevel);
		
		
		/**
		 * Fired when the old value is replaced with the new value 
		 * @param oldLevel The prior value
		 * @param newLevel the new SocketTracingLevel
		 */
		public void change(SocketTracingLevel oldLevel, SocketTracingLevel newLevel);

	}
	
	/**
	 * <p>Title: EmptySocketTracingLevelListener</p>
	 * <p>Description: An empty implementation of {@link SocketTracingLevelListener} for simple extending.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.byteman.SocketTracingLevel.EmptySocketTracingLevelListener</code></p>
	 */
	public static class EmptySocketTracingLevelListener implements SocketTracingLevelListener {

		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.byteman.sockets.SocketTracingLevel.SocketTracingLevelListener#fromNullTo(org.helios.apmrouter.byteman.sockets.SocketTracingLevel)
		 */
		@Override
		public void fromNullTo(SocketTracingLevel newLevel) {
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.byteman.sockets.SocketTracingLevel.SocketTracingLevelListener#toNull(org.helios.apmrouter.byteman.sockets.SocketTracingLevel)
		 */
		@Override
		public void toNull(SocketTracingLevel oldLevel) {
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.byteman.sockets.SocketTracingLevel.SocketTracingLevelListener#change(org.helios.apmrouter.byteman.sockets.SocketTracingLevel, org.helios.apmrouter.byteman.sockets.SocketTracingLevel)
		 */
		@Override
		public void change(SocketTracingLevel oldLevel, SocketTracingLevel newLevel) {
		}
	}
	
	/**
	 * <p>Title: SocketTracingLevelWatcher</p>
	 * <p>Description: A convenience implementation of a {@link SocketTracingLevel} container that fires {@link SocketTracingLevelListener} events when the value changes.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.byteman.SocketTracingLevelWatcher</code></p>
	 */
	public static class SocketTracingLevelWatcher {
		/** The supplied listener */
		private final SocketTracingLevelListener listener;
		/** The container for the level */
		private final AtomicReference<SocketTracingLevel> level = new AtomicReference<SocketTracingLevel>(null); 

		/**
		 * Creates a new SocketTracingLevelWatcher
		 * @param initialLevel The initial level to install. Ignored if null
		 * @param listener The listener that will be called back on when the level is changed
		 */
		public SocketTracingLevelWatcher(SocketTracingLevel initialLevel, SocketTracingLevelListener listener) {			
			this.listener = nvl(listener, "Null listener");
			if(initialLevel!=null) {
				level.set(initialLevel);
			}
		}
		
		/**
		 * Creates a new SocketTracingLevelWatcher with a null initial value
		 * @param listener The listener that will be called back on when the level is changed
		 */
		public SocketTracingLevelWatcher(SocketTracingLevelListener listener) {
			this(null, listener);
		}
		
		/**
		 * Returns the current level
		 * @return the current level
		 */
		public SocketTracingLevel get() {
			return level.get();
		}
		
		/**
		 * Sets the level
		 * @param newLevel level to set
		 */
		public void set(final SocketTracingLevel newLevel) {
			final SocketTracingLevel oldLevel = level.getAndSet(newLevel);
			if(newLevel==null) {
				if(oldLevel==null) {
					// null to null. No action
				} else {
					// switched to null
					listener.toNull(oldLevel);
				}
			} else {
				if(oldLevel==null) {
					// from null to new
					listener.fromNullTo(newLevel);
				} else {
					// from old to new
					listener.change(oldLevel, newLevel);
				}
			}
		}
		
		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			SocketTracingLevel stl = level.get();
			return stl==null ? "null" : stl.name();
		}
		
		
	}

}

