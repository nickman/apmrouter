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

import org.helios.apmrouter.jmx.ConfigurationHelper;
import org.helios.apmrouter.monitor.AbstractMonitor;
import org.helios.apmrouter.nativex.APMSigar;
import org.helios.apmrouter.trace.ITracer;
import org.helios.apmrouter.trace.TracerFactory;
import org.helios.apmrouter.util.URLHelper;

import javax.script.*;
import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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
	/** The script engine manager that creates the monitoring script engines */
	protected final ScriptEngineManager scriptEngineManager;
	/** A map of compiled scripts keyed by the script URL resource */
	protected static final Map<String, ScriptContainer> compiledScripts = new ConcurrentHashMap<String, ScriptContainer>();
	/** The bindings that are passed to each script */
	protected final Bindings scriptBindings = new SimpleBindings();
	/** The configured maximum number of times a script can fail before it is ignored */
	protected int maxErrors = DEFAULT_SCRIPT_ERRCNT;
	/** The tracer to bind */
	protected final ITracer tracer;
	/** The JMXHelper to bind. JMXHelper is all static but JS will not accept straight classes */
	protected final JMXScriptHelper jmxHelper = new JMXScriptHelper();;
	
	
	
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
	/** The binding name for the {@link APMSigar} instance */
	public static final String APM_SIGAR = "sigar";
	
	/** Supporting scripts */
	public static final String[] supportScripts = {
		"function isNumber(v) {try { var i = parseInt(v); return !isNaN(i); } catch (e) { return false; }}"
	};
	/*
	 * set inited
	 * state-control   (state.get().get('inited') + ":" + state.get().get('lastelapsed'))
	 */
	/**
	 * Creates a new ScriptMonitor
	 */
	public ScriptMonitor() {
		this(null);
	}
	
	/**
	 * Creates a new ScriptMonitor
	 * @param scriptLibs An optional array of URLs for additional scripting libraries
	 */
	public ScriptMonitor(URL...scriptLibs) {
		if(scriptLibs!=null && scriptLibs.length>0) {
			URLClassLoader ucl = new URLClassLoader(scriptLibs, getClass().getClassLoader());
			scriptEngineManager = new ScriptEngineManager(ucl);
		} else {
			scriptEngineManager = new ScriptEngineManager();
		}
		maxErrors = ConfigurationHelper.getIntSystemThenEnvProperty(SCRIPT_ERRCNT_PROP, DEFAULT_SCRIPT_ERRCNT);
		tracer = TracerFactory.getTracer();
		scriptBindings.put(TRACER_BINDING_KEY, tracer);
		//scriptBindings.put(BINDINGS_BINDING_KEY, scriptBindings);
		scriptBindings.put(JMXHELPER_BINDING_KEY, jmxHelper);
		scriptBindings.put(STD_OUT, System.out);
		scriptBindings.put(STD_ERR, System.err);
		scriptBindings.put(APM_SIGAR, APMSigar.getInstance());
		//scriptLoad();
		scheduleNewScriptCheck();
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
				if(sc.getCustomFrequency()==-1L) {
					sc.invoke();
				}
			} catch (UnavailableMBeanServerException uex) {
				/* No Op */
			} catch (Throwable e) {
				long err = sc.getConsecutiveErrors();
				if(err>=maxErrors) {
					sc.setDisabled(true);
					System.err.println("Monitor Script [" + sc.scriptUrl + "] has failed [" + err + "] consecutive times. It is being ignored until modified. Last error follows:");
					e.printStackTrace(System.err);
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
		Set<URL> deployed = new HashSet<URL>();
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
				Collections.addAll(deployed, processFileScripts(scriptURL));
			} else if(scriptURL.getProtocol().equals("http")) {
				Collections.addAll(deployed, processHttpScript(scriptURL));
			}
		} catch (Exception e) {
			System.err.println("Failed to resolve monitor scripts from [" + url + "]");
		}
		if(!deployed.isEmpty()) {
			StringBuilder b = new StringBuilder("\n\tDeployed Monitoring Scripts:\n\t============================");
			for(URL u: deployed) {
				b.append("\n\t\t").append(u);
			}
			b.append("\n");
			System.out.println(b);
		}
	}
	
	/**
	 * Schedules a task to check for new script files
	 */
	protected void scheduleNewScriptCheck() {
		scheduler.scheduleWithFixedDelay(new Runnable(){
			public void run() {
				scriptLoad();
			}
		}, 15000, 5000, TimeUnit.MILLISECONDS);
	}
	
	
	/**
	 * Attempts to load a monitor script from the passed URL
	 * @param scriptURL the monitor script URL
	 * @throws ScriptException rethrows any exception thrown from the container
	 * @return The URL of the deployed script or null if one was not deployed
	 */
	private URL processHttpScript(URL scriptURL) throws ScriptException {
		if(compiledScripts.containsKey(ScriptContainer.urlToName(scriptURL))) return null;
		ScriptEngine se = scriptEngineManager.getEngineByExtension(URLHelper.getExtension(scriptURL, "").toLowerCase());
		if(se==null) throw new RuntimeException("No script engine found for [" + scriptURL + "]", new Throwable());
		ScriptContainer sc = new ScriptContainer(se, scriptBindings, scriptURL);
		if(sc.getCustomFrequency()>0) scheduleCustomFrequencyScript(sc, -1L);
		compiledScripts.put(sc.getName(), sc);
		return scriptURL;
	}
	
	/** An empty URL array */
	protected static final URL[] EMPTY_URL_ARR = new URL[0];

	/**
	 * Attempts to load file based monitor scripts from the passed URL.
	 * If the URL resolves to a directory, that directory will be scanned for JS monitor scripts.
	 * Otherwise, if the extension of the file is [case insensitive] JS, that single file will be loaded.
	 * @param scriptURL The file based script URL
	 * @throws ScriptException rethrows any exception thrown from the container
	 * @return The URLs of the deployed scripts
	 */
	private URL[] processFileScripts(URL scriptURL) throws ScriptException {
		if(compiledScripts.containsKey(ScriptContainer.urlToName(scriptURL))) return EMPTY_URL_ARR;
		String fileName = scriptURL.getFile();
		File file = new File(fileName);
		if(!file.exists()) return EMPTY_URL_ARR;
		if(file.isFile()) {
			ScriptEngine se = scriptEngineManager.getEngineByExtension(URLHelper.getFileExtension(file, "").toLowerCase());
			if(se!=null) {
				ScriptContainer sc = new ScriptContainer(se, scriptBindings, scriptURL);
				if(sc.getCustomFrequency()>0) scheduleCustomFrequencyScript(sc, -1L);
				compiledScripts.put(sc.getName(), sc);				
				return new URL[]{scriptURL};
			}
			return EMPTY_URL_ARR;
		} else if(file.isDirectory()) {
			Set<URL> deployed = new HashSet<URL>();
			for(File scriptFile: file.listFiles(new FilenameFilter(){
				@Override
				public boolean accept(File dir, String name) {
					return scriptEngineManager.getEngineByExtension(URLHelper.getFileExtension(name, "").toLowerCase())!=null;
				}})) {
				try {
					URL fileURL = scriptFile.toURI().toURL();
					if(compiledScripts.containsKey(ScriptContainer.urlToName(fileURL))) continue;
					ScriptEngine se = scriptEngineManager.getEngineByExtension(URLHelper.getFileExtension(scriptFile, "").toLowerCase());
					ScriptContainer sc = new ScriptContainer(se, scriptBindings, fileURL);
					if(sc.getCustomFrequency()>0) scheduleCustomFrequencyScript(sc, -1L);
					compiledScripts.put(sc.getName(), sc);
					deployed.add(fileURL);
				} catch (Exception e) {
					crap(e);
				}
			}
			return deployed.toArray(new URL[deployed.size()]);
		}
		return EMPTY_URL_ARR;
	}
	
	/**
	 * Schedules the passed ScriptContainer for recurring invocations at custom intervals
	 * @param sc The script container to schedule invocations for
	 * @param frequency The delay until the next execution. If zero or less, ignored.
	 */
	protected void scheduleCustomFrequencyScript(final ScriptContainer sc, long frequency) {
		scheduler.schedule(new Runnable(){
			public void run() {
				long nextFrequency = sc.getCustomFrequency();
				try {
					sc.invoke();
				} catch (UnavailableMBeanServerException umx) {
					/* No Op */					
				} catch (Exception ex) {
					nextFrequency = nextFrequency * 5;
					long err = sc.getConsecutiveErrors();
					if(err>=maxErrors) {
						sc.setDisabled(true);
						System.err.println("Monitor Script [" + sc.scriptUrl + "] has failed [" + err + "] consecutive times. It is being ignored until modified. Last error follows:");
						ex.printStackTrace(System.err);
					}
				}
				scheduleCustomFrequencyScript(sc, nextFrequency);
			}
		}, frequency<0 ? sc.getCustomFrequency() : frequency, TimeUnit.MILLISECONDS);
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

