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
package org.helios.apmrouter.monitor.script;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.helios.apmrouter.jmx.ConfigurationHelper;
import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.monitor.AbstractMonitor;
import org.helios.apmrouter.nativex.APMSigar;
import org.helios.apmrouter.trace.ITracer;
import org.helios.apmrouter.trace.TracerFactory;
import org.helios.apmrouter.util.URLHelper;

/**
 * <p>Title: ScriptMonitor</p>
 * <p>Description: A monitor implementation that loads scripts from a sysprop specified URL, loads scripts from that URL
 * and executes those scripts on an interval. There ae currently only 3 supported URL patterns:<ol>
 * 	<li>HTTP URLs are supported provided they point to one and only one JS resource</li>
 *  <li>File based URLs that point to one JS file</li>
 *  <li>File based URLs that point to a directory which will be scanned for JS files.</li>
 * </ol></p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.monitor.script.ScriptMonitor</code></p>
 * FIXME: Only supports JS for now.
 * FIXME: Need to periodically scan for updates
 */

public class ScriptMonitor extends AbstractMonitor {
	/** The script engine that manages the monitoring scripts */
	protected final ScriptEngine scriptEngine;
	/** A map of compiled scripts keyed by the script URL resource */
	protected static final Map<String, ScriptContainer> compiledScripts = new ConcurrentHashMap<String, ScriptContainer>();
	/** The bindings that are passed to each script */
	protected final Bindings scriptBindings = new SimpleBindings();
	/** The configured maximum number of times a script can fail before it is ignored */
	protected int maxErrors = DEFAULT_SCRIPT_ERRCNT;
	/** The tracer to bind */
	protected final ITracer tracer;
	/** The JMXHelper to bind. JMXHelper is all static but JS will not accept straight classes */
	protected final JMXHelper jmxHelper = new JMXHelper();
	
	/** The system propety that specifies the maximum number of times a script can fail before it is ignored */
	public static final String SCRIPT_ERRCNT_PROP = "org.helios.agent.monitor.script.maxerr";
	/** The default maximum number of times a script can fail before it is ignored */
	public static final int DEFAULT_SCRIPT_ERRCNT = 3;
	
	/** The system propety that specifies the URL of the script location */
	public static final String SCRIPT_URL_PROP = "org.helios.agent.monitor.script.url";
	/** The default URL of the script location */
	public static final URL DEFAULT_SCRIPT_URL = URLHelper.toURL(new File(System.getProperty("user.home") + File.separator + ".apmrouter" + File.separator + "mscripts"));

	/** The binding name of the tracer */
	public static final String TRACER_BINDING_KEY = "tracer";
	/** The binding name of the bindings for transfering state*/
	public static final String BINDINGS_BINDING_KEY = "state";
	/** The binding name of the JMXHelper */
	public static final String JMXHELPER_BINDING_KEY = "jmx";
	/** The binding name for the stdout stream */
	public static final String STD_OUT = "pout";
	/** The binding name for the stderr stream */
	public static final String STD_ERR = "perr";
	/** The binding name for the collection sweep */
	public static final String COLLECTION_SWEEP = "sweep";
	/** The binding name for the {@link APMSigar} instance */
	public static final String APM_SIGAR = "sigar";
	
	/**
	 * Creates a new ScriptMonitor
	 */
	public ScriptMonitor() {
		maxErrors = ConfigurationHelper.getIntSystemThenEnvProperty(SCRIPT_ERRCNT_PROP, DEFAULT_SCRIPT_ERRCNT);
		scriptEngine = new ScriptEngineManager().getEngineByExtension("js");
		tracer = TracerFactory.getTracer();
		scriptBindings.put(TRACER_BINDING_KEY, tracer);
		scriptBindings.put(BINDINGS_BINDING_KEY, scriptBindings);
		scriptBindings.put(JMXHELPER_BINDING_KEY, jmxHelper);
		scriptBindings.put(STD_OUT, System.out);
		scriptBindings.put(STD_ERR, System.err);
		scriptBindings.put(APM_SIGAR, APMSigar.getInstance());
		scriptLoad();
	}
	
	/**
	 * Looks up and returns the named compiled script
	 * @param name The name of the script
	 * @return the named script or null if one was not found
	 */
	public static ScriptContainer getScript(String name) {
		if(name==null || name.trim().isEmpty()) return null;
		return compiledScripts.get(name.trim().toLowerCase());
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.monitor.AbstractMonitor#doCollect(long)
	 */
	@Override
	protected void doCollect(long collectionSweep) {		
		for(Iterator<ScriptContainer> iter = compiledScripts.values().iterator(); iter.hasNext();) {
			ScriptContainer sc = iter.next();
			try {
				scriptBindings.put(COLLECTION_SWEEP, collectionSweep);
				sc.invoke(scriptBindings);
				sc.resetErrors();
			} catch (UnavailableMBeanServerException uex) {
				/* No Op */
			} catch (Exception e) {
				int err = sc.incrementErrors();
				if(err>=maxErrors) {
					sc.setDisabled(true);
					System.err.println("Monitor Script [" + sc.scriptUrl + "] has failed [" + err + "] consecutive times. It is being ignored until modified.");
				}
			}
		}
	}
	
	
	/**
	 * Searches for loadable scripts
	 */
	protected void scriptLoad() {
		URL scriptURL = null;
		String url = ConfigurationHelper.getSystemThenEnvProperty(SCRIPT_URL_PROP, null);
		if(url==null) {
			File scriptDir = new File(DEFAULT_SCRIPT_URL.getFile());
			if(scriptDir.exists() && scriptDir.isDirectory()) {
				scriptURL = DEFAULT_SCRIPT_URL;
			}			
		}
		try {
			if(scriptURL==null) {
				if(url==null) return;
				scriptURL = URLHelper.toURL(url);
			}
			if(scriptURL.getProtocol().equals("file")) {
				processFileScripts(scriptURL);
			} else if(scriptURL.getProtocol().equals("http")) {
				processHttpScript(scriptURL);
			}
		} catch (Exception e) {
			System.err.println("Failed to resolve monitor scripts from [" + url + "]");
		}
		if(compiledScripts.size()>0) {
			System.out.println("Prepared [" + compiledScripts.size() + "] compiled monitor scripts");
		}
	}
	
	/**
	 * Attempts to load a monitor script from the passed URL
	 * @param scriptURL the monitor script URL
	 * @throws ScriptException rethrows any exception thrown from the container
	 */
	private void processHttpScript(URL scriptURL) throws ScriptException {
		ScriptContainer sc = new ScriptContainer(scriptEngine, scriptBindings, scriptURL);
		compiledScripts.put(sc.getName(), sc);
	}

	/**
	 * Attempts to load file based monitor scripts from the passed URL.
	 * If the URL resolves to a directory, that directory will be scanned for JS monitor scripts.
	 * Otherwise, if the extension of the file is [case insensitive] JS, that single file will be loaded.
	 * @param scriptURL The file based script URL
	 * @throws ScriptException rethrows any exception thrown from the container
	 */
	private void processFileScripts(URL scriptURL) throws ScriptException {
		String fileName = scriptURL.getFile();
		File file = new File(fileName);
		if(!file.exists()) return;
		if(file.isFile()) {
			ScriptContainer sc = new ScriptContainer(scriptEngine, scriptBindings, scriptURL);
			compiledScripts.put(sc.getName(), sc);
		} else if(file.isDirectory()) {
			// FIXME: Start periodic scan of the directory for new scripts
			// FIXME: Currently hard coded to support js only. Need to extend to discover supported extensions 
			for(File scriptFile: file.listFiles(new FilenameFilter(){
				@Override
				public boolean accept(File dir, String name) {
					String lcName = name.toLowerCase();					
					return lcName.endsWith(".js"); 
				}})) {
				try {
					ScriptContainer sc = new ScriptContainer(scriptEngine, scriptBindings, scriptFile.toURI().toURL());
					compiledScripts.put(sc.getName(), sc);
				} catch (Exception e) {
					crap(e);
				}
			}		
		}
	}
	
	/**
	 * Take a crap with this exception
	 * @param e The exception to take a crap with
	 */
	protected void crap(Exception e) {
		System.err.println("Script load took a crap. CrapTrace follows:");
		e.printStackTrace(System.err);
	}


}

