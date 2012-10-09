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
import java.net.URL;
import java.util.Date;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.helios.apmrouter.util.URLHelper;

/**
 * <p>Title: ScriptContainer</p>
 * <p>Description: Container and invoker for a compiled script </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.monitor.script.ScriptContainer</code></p>
 */
public class ScriptContainer {
	/** The source URL */
	protected final URL scriptUrl;
	/** The source name */
	protected final String name;
	
	/** The compiled script */
	protected CompiledScript compiledScript;
	/** The timestamp of the compiled script */
	protected long sourceTimestamp;
	/** This script's bindings */
	protected Bindings instanceBindings = new SimpleBindings();
	/** This script container's script engine/compiler */
	protected final ScriptEngine scriptEngine;
	/** The script monitor supplied script bindings */
	protected final Bindings scriptBindings;
	/** The number of times this script has thrown an exception */
	protected int errorCount = 0;
	/** Indicates the script is disabled after n consecutive errors */
	protected boolean disabled = false;
	
	
	/**
	 * Creates a new ScriptContainer
	 * @param scriptEngine This script container's script engine/compiler
	 * @param scriptBindings The monitor supplied script bindings
	 * @param url The source URL
	 * @throws ScriptException thrown if the script cannot be compiled
	 */
	public ScriptContainer(ScriptEngine scriptEngine, Bindings scriptBindings, URL url) throws ScriptException {
		this.scriptEngine = scriptEngine;
		this.scriptBindings = scriptBindings;
		this.scriptUrl = url;
		name = new File(url.getFile()).getName().toLowerCase();
		String src = URLHelper.getTextFromURL(scriptUrl);
		try {
			compiledScript = ((Compilable)scriptEngine).compile(src);
			sourceTimestamp = URLHelper.getLastModified(scriptUrl);
		} catch (Exception e) {
			System.err.println("Failed to compile script [" + url + "]. Will be ignored until modified.");
			disabled = true;
		}
				
	}
	
	/**
	 * Returns the number of consecutive errors
	 * @return the number of consecutive errors
	 */
	public int getErrorCount() {
		return errorCount;
	}
	
	/**
	 * Increments the consecutive error count
	 * @return The new error count
	 */
	public int incrementErrors() {
		errorCount++;
		return errorCount;
	}
	
	/**
	 * Resets the consecutive error count
	 */
	public void resetErrors() {
		errorCount=0;
	}
	
	
	/**
	 * Invokes the script passing in the passed bindings
	 * @param sharedBindings the shared bindings
	 * @return the return value of the script execution
	 * @throws ScriptException thrown on any error during invocation
	 */
	public Object invoke(Bindings sharedBindings) throws ScriptException {
		checkForUpdate();
		if(disabled) return null;
		sharedBindings.put("args", new Object[]{});
		return compiledScript.eval(sharedBindings);
	}
	
	/**
	 * Invokes the script its own bindings
	 * @param args The arguments passed to the script
	 * @return the return value of the script execution
	 * @throws ScriptException thrown on any error during invocation
	 */
	public Object invoke(Object...args) throws ScriptException {
		scriptBindings.put("argsX", args);
		return invoke(scriptBindings);
	}

	
	/**
	 * Checks to see if the script has been updated and recompiles if it has
	 * @throws ScriptException thrown if the script cannot be recompiled
	 */
	protected void checkForUpdate() throws ScriptException {
		if(URLHelper.resolves(scriptUrl)) {
			long ts = URLHelper.getLastModified(scriptUrl);
			if(ts>sourceTimestamp) {				
				String src = URLHelper.getTextFromURL(scriptUrl);
				sourceTimestamp = ts;
				compiledScript = ((Compilable)scriptEngine).compile(src);
				if(disabled) {
					disabled = false;
					System.out.println("Reloaded and re-enabled [" + scriptUrl + "]");
				} else {
					System.out.println("Reloaded Script [" + scriptUrl + "]");
				}
			}
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
	public String getName() {
		return name;
	}

	/**
	 * Indicates the script is disabled after n consecutive errors
	 * @return true if disabled, false otherwise
	 */
	public boolean isDisabled() {
		return disabled;
	}

	/**
	 * Sets the disabled state of this script
	 * @param true to disable, false to enable
	 */
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}
	
}
