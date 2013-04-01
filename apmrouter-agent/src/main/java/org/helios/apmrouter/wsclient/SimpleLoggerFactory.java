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
package org.helios.apmrouter.wsclient;

import org.helios.apmrouter.util.SimpleLogger;
import org.helios.apmrouter.util.SimpleLogger.Level;
import org.jboss.netty.logging.InternalLogLevel;
import org.jboss.netty.logging.InternalLogger;
import org.jboss.netty.logging.InternalLoggerFactory;

/**
 * <p>Title: SimpleLoggerFactory</p>
 * <p>Description: An adapter to implement netty's logger subsystem using {@link SimpleLogger} </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.wsclient.SimpleLoggerFactory</code></p>
 */

public class SimpleLoggerFactory extends InternalLoggerFactory {
	/** The simple logger */
	protected static final SimpleLoggerLogger logger = new SimpleLoggerLogger();

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.logging.InternalLoggerFactory#newInstance(java.lang.String)
	 */
	@Override
	public InternalLogger newInstance(String name) {
		return logger;
	}
	
	/**
	 * <p>Title: SimpleLoggerLogger</p>
	 * <p>Description: The simple logger adapter</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.wsclient.SimpleLoggerFactory.SimpleLoggerLogger</code></p>
	 */
	public static class SimpleLoggerLogger implements InternalLogger {

		/**
		 * {@inheritDoc}
		 * @see org.jboss.netty.logging.InternalLogger#isDebugEnabled()
		 */
		@Override
		public boolean isDebugEnabled() {
			return SimpleLogger.getLevel().isEnabledFor(Level.DEBUG);
		}

		/**
		 * {@inheritDoc}
		 * @see org.jboss.netty.logging.InternalLogger#isInfoEnabled()
		 */
		@Override
		public boolean isInfoEnabled() {
			return SimpleLogger.getLevel().isEnabledFor(Level.INFO);
		}

		/**
		 * {@inheritDoc}
		 * @see org.jboss.netty.logging.InternalLogger#isWarnEnabled()
		 */
		@Override
		public boolean isWarnEnabled() {
			return SimpleLogger.getLevel().isEnabledFor(Level.WARN);
		}

		/**
		 * {@inheritDoc}
		 * @see org.jboss.netty.logging.InternalLogger#isErrorEnabled()
		 */
		@Override
		public boolean isErrorEnabled() {
			return SimpleLogger.getLevel().isEnabledFor(Level.ERROR);
		}

		/**
		 * {@inheritDoc}
		 * @see org.jboss.netty.logging.InternalLogger#isEnabled(org.jboss.netty.logging.InternalLogLevel)
		 */
		@Override
		public boolean isEnabled(InternalLogLevel level) {
			switch(level) {
			case DEBUG:
				return isDebugEnabled();				
			case ERROR:
				return isErrorEnabled();
			case INFO:
				return isInfoEnabled();
			case WARN:
				return isWarnEnabled();
			default:
				break;				
			}
			return false;
		}

		/**
		 * {@inheritDoc}
		 * @see org.jboss.netty.logging.InternalLogger#debug(java.lang.String)
		 */
		@Override
		public void debug(String msg) {
			SimpleLogger.debug(msg);
		}

		/**
		 * {@inheritDoc}
		 * @see org.jboss.netty.logging.InternalLogger#debug(java.lang.String, java.lang.Throwable)
		 */
		@Override
		public void debug(String msg, Throwable cause) {
			SimpleLogger.debug(msg, cause);			
		}

		/**
		 * {@inheritDoc}
		 * @see org.jboss.netty.logging.InternalLogger#info(java.lang.String)
		 */
		@Override
		public void info(String msg) {
			SimpleLogger.info(msg);			
		}

		/**
		 * {@inheritDoc}
		 * @see org.jboss.netty.logging.InternalLogger#info(java.lang.String, java.lang.Throwable)
		 */
		@Override
		public void info(String msg, Throwable cause) {
			SimpleLogger.info(msg, cause);
		}

		/**
		 * {@inheritDoc}
		 * @see org.jboss.netty.logging.InternalLogger#warn(java.lang.String)
		 */
		@Override
		public void warn(String msg) {
			SimpleLogger.warn(msg);			
		}

		/**
		 * {@inheritDoc}
		 * @see org.jboss.netty.logging.InternalLogger#warn(java.lang.String, java.lang.Throwable)
		 */
		@Override
		public void warn(String msg, Throwable cause) {
			SimpleLogger.warn(msg, cause);
			
		}

		/**
		 * {@inheritDoc}
		 * @see org.jboss.netty.logging.InternalLogger#error(java.lang.String)
		 */
		@Override
		public void error(String msg) {
			SimpleLogger.error(msg);			
		}

		/**
		 * {@inheritDoc}
		 * @see org.jboss.netty.logging.InternalLogger#error(java.lang.String, java.lang.Throwable)
		 */
		@Override
		public void error(String msg, Throwable cause) {
			SimpleLogger.error(msg, cause);			
		}

		/**
		 * {@inheritDoc}
		 * @see org.jboss.netty.logging.InternalLogger#log(org.jboss.netty.logging.InternalLogLevel, java.lang.String)
		 */
		@Override
		public void log(InternalLogLevel level, String msg) {
			
			
		}

		/**
		 * {@inheritDoc}
		 * @see org.jboss.netty.logging.InternalLogger#log(org.jboss.netty.logging.InternalLogLevel, java.lang.String, java.lang.Throwable)
		 */
		@Override
		public void log(InternalLogLevel level, String msg, Throwable cause) {
			switch(level) {
			case DEBUG:
				debug(msg, cause);				
			case ERROR:
				error(msg, cause);
			case INFO:
				info(msg, cause);
			case WARN:
				warn(msg, cause);
			default:
				break;				
			}			
		}		
	}
}
