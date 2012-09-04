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
package org.helios.apmrouter.metric.catalog.direct.chronicle;

import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import vanilla.java.chronicle.Excerpt;
import vanilla.java.chronicle.impl.IndexedChronicle;

/**
 * <p>Title: ChronicleController</p>
 * <p>Description: Singleton controller for managing the chronicle</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.metric.ChronicleController</code></p>
 */
public class ChronicleController {
	/** The chronicle file name */
	protected final String chronicleName;
	/** The chronicle parth */
	protected final String chroniclePath;	
	/** The chronicle */
	protected final IndexedChronicle chronicle;
	/** Indicates if the chronicle initialized to DIRECT mode */
	protected final boolean initDirect;
	/** Indicates if the chronicle is retained */
	protected final boolean retain;
	
	/** The singleton instance */
	private static volatile ChronicleController instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** The system property that defines the chronicle name */
	public static final String CHRONICLE_PROP = "apmrouter.chronicle.name";
	/** The default chronicle name */
	public static final String DEFAULT_CHRONICLE_NAME = "apmrouter";
	/** The system property that defines if the chronicle is direct */
	public static final String CHRONICLE_DIRECT_PROP = "apmrouter.chronicle.direct";
	/** The default chronicle direct */
	public static final String DEFAULT_CHRONICLE_DIRECT = "false";
	/** The system property that defines if chronicles should be retained on shutdown (and not deleted on start) */
	public static final String CHRONICLE_RETAIN_PROP = "apmrouter.chronicle.retain";
	/** The default chronicle retain */
	public static final String DEFAULT_CHRONICLE_RETAIN = "false";
	
	
	/** The chronicle home directory */
	public static final File CHRONICLE_HOME_DIR = new File(System.getProperty("user.home") + File.separator + ".apmrouter");
	/** The default chronicle databit size estimate */
	public static final int CHRONICLE_SIZE_EST = 10;
	
	/**
	 * Acquires the ChronicleController singleton instance
	 * @return the ChronicleController singleton instance
	 */
	public static ChronicleController getInstance() {
		if(instance == null) {
			synchronized(lock) {
				if(instance == null) {
					instance = new ChronicleController();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Creates a new ChronicleController and initializes the underlying chronicle
	 */
	private ChronicleController() {
		retain = "true".equalsIgnoreCase(System.getProperty(CHRONICLE_RETAIN_PROP, DEFAULT_CHRONICLE_RETAIN).trim());
		

		chronicleName = System.getProperty(CHRONICLE_PROP, DEFAULT_CHRONICLE_NAME);
		if(!CHRONICLE_HOME_DIR.exists()) {
			if(!CHRONICLE_HOME_DIR.mkdir()) {
				throw new RuntimeException("Failed to create apmrouter home directory [" + CHRONICLE_HOME_DIR + "]", new Throwable());
			}
		} else {
			if(!CHRONICLE_HOME_DIR.isDirectory()) {
				throw new RuntimeException("apmrouter home directory [" + CHRONICLE_HOME_DIR + "] is a file not a directory", new Throwable());
			}
		}
		chroniclePath = CHRONICLE_HOME_DIR + File.separator + chronicleName;
		if(!retain) {
			deleteChronicle();
			Runtime.getRuntime().addShutdownHook(new Thread(){
				@Override
				public void run() {
					deleteChronicle();
				}
			});
		}		
		try {
			chronicle = new IndexedChronicle(chroniclePath, CHRONICLE_SIZE_EST);
			chronicle.useUnsafe(false);
		} catch (IOException e) {
			throw new RuntimeException("Failed to create chronicle on path [" + chroniclePath + "]", e);
		}
		initDirect = "true".equalsIgnoreCase(System.getProperty(CHRONICLE_DIRECT_PROP, DEFAULT_CHRONICLE_DIRECT).trim());
		chronicle.useUnsafe(initDirect);
		
		//log("Initialized chronicle [" + chronicle.name() + "] on path [" + chroniclePath + "] with size [" + chronicle.size() + "]  Direct:" + initDirect);
	}
	
	
	private void deleteChronicle() {
		new File(chroniclePath + ".data").delete();
		new File(chroniclePath + ".index").delete();
	}
	
	/**
	 * Console logger
	 * @param msg The message to log
	 */
	public static void log(Object msg) {
		System.out.println(msg);
	}

	/**
	 * @return
	 * @see vanilla.java.chronicle.impl.AbstractChronicle#size()
	 */
	public long size() {
		return chronicle.size();
	}

	/**
	 * @param useUnsafe
	 * @see vanilla.java.chronicle.impl.IndexedChronicle#useUnsafe(boolean)
	 */
	public void useUnsafe(boolean useUnsafe) {
		chronicle.useUnsafe(useUnsafe);
	}

	/**
	 * @return
	 * @see vanilla.java.chronicle.impl.IndexedChronicle#useUnsafe()
	 */
	public boolean useUnsafe() {
		return chronicle.useUnsafe();
	}

	/**
	 * @return
	 * @see vanilla.java.chronicle.impl.IndexedChronicle#byteOrder()
	 */
	public ByteOrder byteOrder() {
		return chronicle.byteOrder();
	}

	/**
	 * @return
	 * @see vanilla.java.chronicle.impl.IndexedChronicle#createExcerpt()
	 */
	public Excerpt<IndexedChronicle> createExcerpt() {
		return chronicle.createExcerpt();
	}

	/**
	 * @param capacity
	 * @return
	 * @see vanilla.java.chronicle.impl.IndexedChronicle#startExcerpt(int)
	 */
	public long startExcerpt(int capacity) {
		return chronicle.startExcerpt(capacity);
	}

	/**
	 * @return
	 * @see vanilla.java.chronicle.impl.AbstractChronicle#name()
	 */
	public String name() {
		return chronicle.name();
	}

	/**
	 * 
	 * @see vanilla.java.chronicle.impl.IndexedChronicle#clear()
	 */
	public void clear() {
		chronicle.clear();
	}
	
}
