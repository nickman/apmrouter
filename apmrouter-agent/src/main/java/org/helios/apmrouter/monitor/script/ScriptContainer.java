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

import org.helios.apmrouter.collections.ConcurrentLongSlidingWindow;
import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.util.SystemClock;
import org.helios.apmrouter.util.URLHelper;

import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;
import javax.script.*;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>Title: ScriptContainer</p>
 * <p>Description: Container and invoker for a compiled script </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.monitor.script.ScriptContainer</code></p>
 */
public class ScriptContainer extends NotificationBroadcasterSupport implements ScriptContainerMBean {
	/** The source URL */
	protected final URL scriptUrl;
	/** The source name */
	protected final String name;
	/** The script source */
	protected volatile String source;
	
	/** The compiled script */
	protected CompiledScript compiledScript;
	/** The timestamp highwater mark of the compiled script */
	protected long sourceTimestamp;
	
	/** Custom invocation frequency determined by a var called <b><code>customFrequency</code></b> that contains the frequency in ms. */
	protected long customFrequency = -1L;
	
	/** This script container's script engine/compiler */
	protected final ScriptEngine scriptEngine;
	/** This script container's script engine factory */
	protected final ScriptEngineFactory scriptEngineFactory;
	
	/** The script monitor supplied script bindings */
	protected final Bindings globalBindings;
	/** The local script bindings */
	protected final Bindings localBindings;
	/** Indicates if the source URL is writable and modified source can be saved */
	protected final boolean writable;
	
	/** Indicates the script is disabled after n consecutive errors */
	protected boolean disabled = false;
	/** The JMX ObjectName for this container */
	protected final ObjectName objectName;
	
	/** The context for this script */
	protected final ScriptContext ctx = new SimpleScriptContext();
	/** The overriden timestamp of the last change to the source  */
	protected final AtomicLong lastChangeOverride = new AtomicLong(-1L);

	
	// ========================
	// 	Counters
	// ========================
	/** The number of consecutive times the script has executed with errors */
	protected final AtomicLong consecutiveErrors = new AtomicLong(0L);
	/** The total number of times the script has executed with errors */
	protected final AtomicLong totalErrors = new AtomicLong(0L);
	/** The total number of times the script has executed  */
	protected final AtomicLong totalInvocations = new AtomicLong(0L);
	/** The local collection sweep, starting at 0 and incrementing each period */
	protected long localCollectionSweep = 0;
	
	/** The script invocation time recorder */
	protected final ConcurrentLongSlidingWindow invocationTimes = new ConcurrentLongSlidingWindow(100);
	/** The script compilation time recorder */
	protected final ConcurrentLongSlidingWindow compilationTimes = new ConcurrentLongSlidingWindow(20);
	
	/** JMX script helper */
	protected static final JMXScriptHelper jmx = new JMXScriptHelper(); 
	
	/** Source prepended to the read file */
	public static final String SRC_HEADER = "if(!inited) pout.println('\\n\\t[<< %s >>] Initializing');\n";
	/** Source appended to the read file */
	public static final String SRC_FOOTER = "\nif(!inited) { pout.println('\\n\\t[<< %s >>] Monitor OK'); inited = true;}";
	/** The binding name for the collection sweep */
	public static final String COLLECTION_SWEEP = "localsweep";
	
	
	/**
	 * Creates a new ScriptContainer
	 * @param se This script container's script engine/compiler
	 * @param globalBindings The monitor supplied script bindings
	 * @param url The source URL
	 * @throws ScriptException thrown if the script cannot be compiled
	 */
	public ScriptContainer(ScriptEngine se, Bindings globalBindings, URL url) throws ScriptException {
		scriptEngine = se;
		scriptEngineFactory = se.getFactory();
		localBindings = se.createBindings();
		//localBindings.put("jmx", jmx);
		this.globalBindings = globalBindings;
		ctx.setBindings(localBindings, ScriptContext.ENGINE_SCOPE);
		ctx.setBindings(globalBindings, ScriptContext.GLOBAL_SCOPE);
				
		this.scriptUrl = url;
		try {
			String cleanedUrl = this.scriptUrl.toURI().toString();
			cleanedUrl = cleanedUrl.substring(cleanedUrl.indexOf(":")+1);
			cleanedUrl = cleanedUrl.substring(cleanedUrl.indexOf(":")+1);
			cleanedUrl = cleanedUrl.replace(':', ';');
			
			System.out.println("Creating ObjectName [" + String.format(OBJECT_NAME_TEMPLATE, cleanedUrl, scriptEngineFactory.getLanguageName()) + "]");
			objectName = JMXHelper.objectName(String.format(OBJECT_NAME_TEMPLATE, cleanedUrl, scriptEngineFactory.getLanguageName()));
			JMXHelper.getHeliosMBeanServer().registerMBean(this, objectName);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to register JMX interface for [" + this.scriptUrl + "]", ex);
		}
		
		writable = URLHelper.isWritable(this.scriptUrl);
		name = urlToName(url);
		sourceTimestamp = URLHelper.getLastModified(scriptUrl);
		acquireScript();
		compileScript();
		
	}
	
	/**
	 * Generates the script's logical name for the passed URL
	 * @param url The script's URL
	 * @return the script's logical name
	 */
	public static String urlToName(URL url) {
		String urlName = url.getFile();
		if(urlName.lastIndexOf(".")!=-1) {
			return urlName.substring(0, urlName.lastIndexOf("."));
		}
		return urlName;							
	}
	
	/**
	 * Acquires the script source and compiles it.
	 */
	protected void acquireScript() {
		source = URLHelper.getTextFromURL(scriptUrl);
	}
	
	/**
	 *  Compiles the acquired source
	 */
	protected void compileScript() {
		try {			
			SystemClock.startTimer();
			compiledScript = ((Compilable)scriptEngine).compile(String.format(SRC_HEADER, name) + source + String.format(SRC_FOOTER, name));
			if(localBindings.containsKey("customFrequency")) {
				try {				
					Long f = ((Number)localBindings.get("customFrequency")).longValue();
					customFrequency = f;
				} catch (Exception e) { /* No Op */ }
			}
			localBindings.put("inited", false);
			localCollectionSweep = 0L;
		} catch (Exception e) {
			System.err.println("Failed to compile script [" + scriptUrl + "]. Will be ignored until modified.");
			e.printStackTrace(System.err);
			disabled = true;
		} finally {
			compilationTimes.insert(SystemClock.endTimer().elapsedMs);
		}
	}
	
	
	/**
	 * Increments the consecutive error count
	 * @return The new error count
	 */
	public long incrementErrors() {
		return consecutiveErrors.incrementAndGet();
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.monitor.script.ScriptContainerMBean#invoke()
	 */
	@Override
	public Object invoke() throws ScriptException {
		checkForUpdate();
		localCollectionSweep++;
		if(localCollectionSweep==Long.MAX_VALUE) {
			localCollectionSweep=0L;
		}
		if(disabled) return null;
		boolean completed = false;
		SystemClock.startTimer();
		try {
			localBindings.put(COLLECTION_SWEEP, localCollectionSweep);
			Object response = compiledScript.eval(ctx);			
			totalInvocations.incrementAndGet();
			consecutiveErrors.set(0);
			invocationTimes.insert(SystemClock.endTimer().elapsedMs);
			completed=true;
			return response;
		} catch (Exception ex) {
			consecutiveErrors.incrementAndGet();
			totalErrors.incrementAndGet();
			if(ex instanceof ScriptException) {
				ScriptException se = (ScriptException)ex;
				throw new ScriptException(String.format("Script execution exception on [%s]\n\tFileName:%s\n\tErrorMessage:%s\n\tLineNumber:%s\n\tColNumber:%s", name, se.getMessage(), se.getFileName(), se.getLineNumber(), se.getColumnNumber()));
			}
			throw new RuntimeException("Script execution exception on [" + name + "]", ex);
		} finally {
			if(!completed) SystemClock.endTimer();			
		}		
	}
	
//	/**
//	 * {@inheritDoc}
//	 * @see org.helios.apmrouter.monitor.script.ScriptContainerMBean#invoke()
//	 */
//	@Override
//	public void invoke() {
//		try {
//			invoke(globalBindings);
//		} catch (ScriptException e) {
//			throw new RuntimeException("Script Exception on [" + name + "]", e);
//		}
//	}

	
	/**
	 * Invokes the script its own bindings
	 * @param args The arguments passed to the script
	 * @return the return value of the script execution
	 * @throws ScriptException thrown on any error during invocation
	 */
	public Object invoke(Object...args) throws ScriptException {		
		localBindings.put(ScriptEngine.ARGV, args);
		return invoke(ctx);
	}

	
	/**
	 * Checks to see if the script has been updated and recompiles if it has
	 * @throws ScriptException thrown if the script cannot be recompiled
	 */
	protected void checkForUpdate() throws ScriptException {
		boolean changed = false;
		final long inMemTs = lastChangeOverride.get();
		if(inMemTs>sourceTimestamp) {			
			changed = true;
			sourceTimestamp = inMemTs;
		} else {
			// this means we can reload the script from the URL source
			if(URLHelper.getLastModified(scriptUrl)>sourceTimestamp) {					
				sourceTimestamp = URLHelper.getLastModified(scriptUrl);
				acquireScript();
				changed = true;
			}			
		}
		if(changed) {
			try {
				compileScript();
				if(disabled) {
					disabled = false;
					System.out.println("Reloaded and re-enabled [" + scriptUrl + "]");
				} else {
					System.out.println("Reloaded Script [" + scriptUrl + "]");
				}									
			} catch (Exception se) {
				disabled = true;
				System.err.println("Failed to compile Script [" + scriptUrl + "]. Script has been disabled.");
				System.err.println("\n\t=================================================\n" + source + "\n\t=================================================\n");
				se.printStackTrace(System.err);
			}
			localBindings.put("inited", false);
		}
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ScriptContainer [scriptUrl=\n");
		builder.append(scriptUrl);
		builder.append("\nsourceTimestamp=");
		builder.append(new Date(sourceTimestamp));
		builder.append("]");
		return builder.toString();
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((scriptUrl == null) ? 0 : scriptUrl.hashCode());
		return result;
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ScriptContainer other = (ScriptContainer) obj;
		if (scriptUrl == null) {
			if (other.scriptUrl != null) {
				return false;
			}
		} else if (!scriptUrl.equals(other.scriptUrl)) {
			return false;
		}
		return true;
	}

	/**
	 * Returns the source file's file name
	 * @return the source file's file name
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.monitor.script.ScriptContainerMBean#isEnabled()
	 */
	@Override
	public boolean isEnabled() {
		return !disabled;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.monitor.script.ScriptContainerMBean#setEnabled(boolean)
	 */
	@Override
	public void setEnabled(boolean enabled) {
		disabled = !enabled;
		
	}
	

	/**
	 * Sets the disabled state of this script
	 * @param disabled true to disable, false to enable
	 */
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.monitor.script.ScriptContainerMBean#getSource()
	 */
	@Override
	public String getSource() {
		return source;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.monitor.script.ScriptContainerMBean#setSource(java.lang.String)
	 */
	@Override
	public void setSource(String src) {
		source = src;
		if(isWritable()) {
			URLHelper.writeToURL(scriptUrl, source.getBytes(), false);
		} else {
			lastChangeOverride.set(SystemClock.time());
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.monitor.script.ScriptContainerMBean#getScriptURL()
	 */
	@Override
	public URL getScriptURL() {
		return scriptUrl;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.monitor.script.ScriptContainerMBean#getConsecutiveErrors()
	 */
	@Override
	public long getConsecutiveErrors() {
		return consecutiveErrors.get();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.monitor.script.ScriptContainerMBean#getTotalErrors()
	 */
	@Override
	public long getTotalErrors() {		
		return totalErrors.get();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.monitor.script.ScriptContainerMBean#getTotalInvocations()
	 */
	@Override
	public long getTotalInvocations() {
		return totalInvocations.get();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.monitor.script.ScriptContainerMBean#getLastElapsedTimeMs()
	 */
	@Override
	public long getLastElapsedTimeMs() {
		return invocationTimes.isEmpty() ? -1L : invocationTimes.get(0);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.monitor.script.ScriptContainerMBean#getRollingElapsedTimeMs()
	 */
	@Override
	public long getRollingElapsedTimeMs() {
		return invocationTimes.avg();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.monitor.script.ScriptContainerMBean#getLocalBindings()
	 */
	@Override
	public Map<String, String> getLocalBindings() {
		Map<String, String> b = new HashMap<String, String>(localBindings.size());
		for(Map.Entry<String, Object> e: localBindings.entrySet()) {
			Object val = e.getValue();
			b.put(e.getKey(), val==null ? "" : val.toString());
		}
		return b;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.monitor.script.ScriptContainerMBean#getGlobalBindings()
	 */
	@Override
	public Map<String, String> getGlobalBindings() {
		Map<String, String> b = new HashMap<String, String>(globalBindings.size());
		for(Map.Entry<String, Object> e: globalBindings.entrySet()) {
			Object val = e.getValue();
			b.put(e.getKey(), val==null ? "" : val.toString());
		}
		return b;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.monitor.script.ScriptContainerMBean#getSourceTimestamp()
	 */
	@Override
	public long getSourceTimestamp() {
		if(lastChangeOverride.get()<1) {
			return URLHelper.getLastModified(scriptUrl);
		}
		return lastChangeOverride.get();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.monitor.script.ScriptContainerMBean#getSourceDate()
	 */
	@Override
	public Date getSourceDate() {
		return new Date(getSourceTimestamp());
	}





	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.monitor.script.ScriptContainerMBean#reset()
	 */
	@Override
	public void reset() {
		localCollectionSweep = 0L;
		invocationTimes.clear();
		consecutiveErrors.set(0);
		totalErrors.set(0);
		totalInvocations.set(0);
		acquireScript();
		compileScript();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.monitor.script.ScriptContainerMBean#getEngineName()
	 */
	@Override
	public String getEngineName() {
		return scriptEngineFactory.getEngineName();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.monitor.script.ScriptContainerMBean#getEngineVersion()
	 */
	@Override
	public String getEngineVersion() {
		return scriptEngineFactory.getEngineVersion();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.monitor.script.ScriptContainerMBean#getExtensions()
	 */
	@Override
	public List<String> getExtensions() {
		return scriptEngineFactory.getExtensions();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.monitor.script.ScriptContainerMBean#getMimeTypes()
	 */
	@Override
	public List<String> getMimeTypes() {
		return scriptEngineFactory.getMimeTypes();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.monitor.script.ScriptContainerMBean#getNames()
	 */
	@Override
	public List<String> getNames() {
		return scriptEngineFactory.getNames();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.monitor.script.ScriptContainerMBean#getLanguageName()
	 */
	@Override
	public String getLanguageName() {
		return scriptEngineFactory.getLanguageName();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.monitor.script.ScriptContainerMBean#getLanguageVersion()
	 */
	@Override
	public String getLanguageVersion() {
		return scriptEngineFactory.getLanguageVersion();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.monitor.script.ScriptContainerMBean#getParameter(java.lang.String)
	 */
	@Override
	public Object getParameter(String key) {
		return scriptEngineFactory.getParameter(key);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.monitor.script.ScriptContainerMBean#getMethodCallSyntax(java.lang.String, java.lang.String, java.lang.String[])
	 */
	@Override
	public String getMethodCallSyntax(String obj, String m, String... args) {
		return scriptEngineFactory.getMethodCallSyntax(obj, m, args);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.monitor.script.ScriptContainerMBean#getOutputStatement(java.lang.String)
	 */
	@Override
	public String getOutputStatement(String toDisplay) {
		return scriptEngineFactory.getOutputStatement(toDisplay);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.monitor.script.ScriptContainerMBean#getProgram(java.lang.String[])
	 */
	@Override
	public String getProgram(String... statements) {
		return scriptEngineFactory.getProgram(statements);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.monitor.script.ScriptContainerMBean#isWritable()
	 */
	@Override
	public boolean isWritable() {
		return writable;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.monitor.script.ScriptContainerMBean#getLastCompileTimeMs()
	 */
	@Override
	public long getLastCompileTimeMs() {
		return compilationTimes.isEmpty() ? -1L : compilationTimes.get(0);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.monitor.script.ScriptContainerMBean#getRollingCompileTimeMs()
	 */
	@Override
	public long getRollingCompileTimeMs() {
		return compilationTimes.avg();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.monitor.script.ScriptContainerMBean#getCustomFrequency()
	 */
	@Override
	public long getCustomFrequency() {
		return customFrequency;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.monitor.script.ScriptContainerMBean#setCustomFrequency(long)
	 */
	@Override
	public void setCustomFrequency(long customFrequency) {
		this.customFrequency = customFrequency;
	}
	
}
