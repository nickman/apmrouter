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
package org.helios.apmrouter.util;

import org.helios.apmrouter.jmx.ConfigurationHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;



/**
 * <p>Title: SimpleLogger</p>
 * <p>Description: Zero framework logger </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.util.SimpleLogger</code></p>
 */

public class SimpleLogger {
	
	/** The property name to configure the simple logger level */
	public static final String LEVEL_PROP = "org.helios.simplelogger.level";
	/** The default level if no level is defined */
	public static final Level DEFAULT_LEVEL = Level.INFO;
	/** The default level if an invalid level is defined */
	public static final Level INVALID_LEVEL = Level.DEBUG;
	/** The current level */
	public static Level level = DEFAULT_LEVEL;
	
	
	static {
		level = Level.forName(ConfigurationHelper.getSystemThenEnvProperty(LEVEL_PROP, null));
		System.setProperty(LEVEL_PROP, level.name());
	}
	
	/**
	 * <p>Title: Level</p>
	 * <p>Description: Enumerates the verbosity levels of the {@link SimpleLogger}</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.util.SimpleLogger.Level</code></p>
	 */
	public static enum Level {
		/** Logging turned off */
		OFF(null),
		/** Logs fatals only to System.err */
		FATAL(System.err),		
		/** Logs errors and below to System.err */
		ERROR(System.err),
		/** Logs warnings and below to System.err */
		WARN(System.err),
		/** Logs info and below to System.out */
		INFO(System.out),
		/** Logs debug and below to System.out */
		DEBUG(System.out),
		/** Logs everything to System.out */
		TRACE(System.out);
		
		/**
		 * Determines if the passed level enables logging vs. this level
		 * @param otherLevel The level to compare to
		 * @return true if logging is enabled, false otherwise.
		 */
		public boolean isEnabledFor(Level otherLevel) {
			if(otherLevel==OFF) return false;
			return ordinal()>=otherLevel.ordinal();
		}
		
		/**
		 * Determines if the passed name is a valid level
		 * @param name the name to test
		 * @return true if the passed name is a valid level, false otherwise
		 */
		public static boolean isValidLevel(CharSequence name) {
			if(name==null) return false;
			try {
				Level.valueOf(name.toString().trim().toUpperCase());
				return true;
			} catch (Exception ex) {
				return false;
			}
		}
		
		/**
		 * Returns a level for the passed name.
		 * If the passed name is null or empty, returns the default level.
		 * If the passed name is invalid, returns the invalid level.
		 * @param name The name to get the level for
		 * @return a level
		 */
		public static Level forName(CharSequence name) {
			if(name==null || name.toString().trim().isEmpty()) return DEFAULT_LEVEL;
			if(!isValidLevel(name)) return INVALID_LEVEL;
			return Level.valueOf(name.toString().trim().toUpperCase());
		}
		
		private Level(PrintStream ps) {
			this.ps = ps;
		}
		
		private final PrintStream ps;

		/**
		 * Returns the print stream for this level
		 * @return the ps the print stream for this level
		 */
		public PrintStream getPs() {
			return ps;
		}		
	}
	
	/**
	 * Returns the SimpleLogger's current level
	 * @return the SimpleLogger's current level
	 */
	public static Level getLevel() {
		return level;
	}	
	
	/**
	 * Issues a trace level logging request
	 * @param msgs The objects to format into a log message
	 */
	public static void trace(Object...msgs) {
		logAtLevel(Level.TRACE, msgs);
	}
	
	/**
	 * Logs the passed message at the specified level
	 * @param level the level to log at
	 * @param msgs the message fragments
	 */
	public static void log(Level level, Object...msgs) {
		logAtLevel(level==null ? Level.INFO : level, msgs);
	}
	
	/**
	 * Issues a debug level logging request
	 * @param msgs The objects to format into a log message
	 */
	public static void debug(Object...msgs) {
		logAtLevel(Level.DEBUG, msgs);
	}
	
	/**
	 * Issues an info level logging request
	 * @param msgs The objects to format into a log message
	 */
	public static void info(Object...msgs) {
		logAtLevel(Level.INFO, msgs);
	}
	
	/**
	 * Issues a warn level logging request
	 * @param msgs The objects to format into a log message
	 */
	public static void warn(Object...msgs) {
		logAtLevel(Level.WARN, msgs);
	}
	
	/**
	 * Issues a error level logging request
	 * @param msgs The objects to format into a log message
	 */
	public static void error(Object...msgs) {
		logAtLevel(Level.ERROR, msgs);
	}
	
	/**
	 * Issues a fatal level logging request
	 * @param msgs The objects to format into a log message
	 */
	public static void fatal(Object...msgs) {
		logAtLevel(Level.FATAL, msgs);
	}
	
	
	
	/**
	 * Forwards the logging directive if the current level is enabled for the passed level
	 * @param l The requested level
	 * @param msgs The logged messages
	 */
	protected static void logAtLevel(Level l, Object...msgs) {
		if(level.isEnabledFor(l)) {
			l.ps.println(format(l, msgs));
		}				
	}
	
	/**
	 * Formats the passed objects into a loggable string. 
	 * If the last object is a {@link Throwable}, it will be formatted into a stack trace.
	 * @param level The level at which this log is being made
	 * @param msgs The objects to log
	 * @return the loggable string
	 */
	public static String format(Level level, Object...msgs) {
		if(msgs==null||msgs.length<1) return "";
		StringBuilder b = new StringBuilder(level.name());
		b.append(" [").append(Thread.currentThread().getName()).append("]");
		int c = msgs.length-1;
		for(int i = 0; i <= c; i++) {
			if(i==c && msgs[i] instanceof Throwable) {
				b.append(formatStackTrace((Throwable)msgs[i]));
			} else {
				b.append(msgs[i]);
			}
		}
		return b.toString();
	}
	
	/**
	 * Formats a throwable's stack trace
	 * @param t The throwable to format
	 * @return the formatted stack trace
	 */
	public static String formatStackTrace(Throwable t) {
		if(t==null) return "";
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		t.printStackTrace(new PrintStream(baos, true));
		try {
			baos.flush();
		} catch (IOException e) {
		}
		return baos.toString();
	}
	
	public static void main(String[] args) {
		info("Testing SimpleLogger. Initial Level:" + level);
		level = Level.TRACE;
		trace("This is TRACE");
		info("This is INFO");
		level = Level.WARN;
		warn("This is WARN");
		info("Should not see this");
		
	}

}
