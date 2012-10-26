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
package org.helios.apmrouter.js;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import com.sun.script.javascript.RhinoScriptEngineFactory;

/**
 * <p>Title: LoadEnabledRhinoEngineFactory</p>
 * <p>Description: Adding a loadJs function to the standard JS engine</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.js.LoadEnabledRhinoEngineFactory</code></p>
 */

public class LoadEnabledRhinoEngineFactory extends RhinoScriptEngineFactory {

	/** The load script source */
	public static final String LOAD_JS = 
		"importPackage(java.io); " +
		"var script = ''; " +
		"var ctx = null; " +
		"function loadScript(name) { " +
		    "var f = new File(name); " +
		    "var br = new BufferedReader(new FileReader(f)); " +
		    "var line = null; " +		    
		    "while((line = br.readLine())!=null) { " +
		    	"    script += line; " +
		    "} " +
		    "_e_ngine.eval(script);" + 
		"} ";
	
	/**
	 * {@inheritDoc}
	 * @see com.sun.script.javascript.RhinoScriptEngineFactory#getScriptEngine()
	 */
	@Override
	public ScriptEngine getScriptEngine() {
		ScriptEngine se = super.getScriptEngine();
		Bindings b = se.createBindings();
		b.put("_e_ngine", se);
		se.setBindings(b, ScriptContext.GLOBAL_SCOPE);
		try {
			se.eval(LOAD_JS);
		} catch (ScriptException e) {
			throw new RuntimeException(e);
		}
		return se;
	}
	
	
	public static void main(String[] args) {
		ScriptEngine se = new LoadEnabledRhinoEngineFactory().getScriptEngine();
		try {
			se.eval("loadScript('c:/temp/hellouser.js'); hello('Nicholas');");
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
	
	private static void log(Object msg) {
		System.out.println(msg);
	}

}
