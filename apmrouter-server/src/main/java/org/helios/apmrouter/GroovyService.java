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
package org.helios.apmrouter;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.GroovySystem;
import groovy.lang.Script;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.helios.apmrouter.server.ServerComponentBean;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;

/**
 * <p>Title: GroovyService</p>
 * <p>Description: Interactive groovy service</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.GroovyService</code></p>
 */

public class GroovyService extends ServerComponentBean {
	/** A map of compiled scripts keyed by an arbitrary reference name */
	protected final Map<String, Script> compiledScripts = new ConcurrentHashMap<String, Script>();
	
	/** The compiler configuration for script compilations */
	protected final CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
	
	
	/**
	 * Creates a new GroovyService
	 */
	public GroovyService() {
		compilerConfiguration.setOptimizationOptions(Collections.singletonMap("indy", true));
	}
	
	/**
	 * Flushes the compiled script cache
	 */
	@ManagedOperation(description="Flushes the compiled script cache")
	public void flushScriptCache() {
		compiledScripts.clear();
	}
	
	/**
	 * Removes the named script from the script cache
	 * @param name The name of the script to remove
	 */
	@ManagedOperation(description="Removes the named script from the script cache")
	@ManagedOperationParameter(name="ScriptName", description="The name of the script to remove")
	public void flushScript(String name) {
		if(name==null || name.trim().isEmpty()) throw new IllegalArgumentException("The passed name was null or empty", new Throwable());
		compiledScripts.remove(name);
	}
	
	/**
	 * Returns the groovy version
	 * @return the groovy version
	 */
	@ManagedAttribute(description="The groovy version")
	public String getGroovyVersion() {
		return GroovySystem.getVersion();
	}
	
	/**
	 * Returns the names of the cached compiled scripts
	 * @return the names of the cached compiled scripts
	 */
	@ManagedAttribute(description="The names of the cached compiled scripts")
	public String[] getScriptNames() {
		return compiledScripts.keySet().toArray(new String[compiledScripts.size()]);
	}
	
	/**
	 * Indicates if groovy is using reflection
	 * @return true if groovy is using reflection, false if using .... ?
	 */
	@ManagedAttribute(description="Indicates if groovy is using reflection")
	public boolean isUseReflection() {
		return GroovySystem.isUseReflection();
	}
	
	/*
	 * compile(String name, String source)
	 * compile(String name, URL source) // needs check for source update
	 * compile(String name, File source) // needs check for source update
	 * 
	 * invoke(String name, OutputStream os, Object...args)  // run, with args in bindings
	 * invoke(String name, Object...args)  // run, with args in bindings, ditch output
	 * invokeMethod(String name, String methodName, Object...args)
	 * invokeMethod(String name, OutputStream os, String methodName, Object...args)
	 * 
	 * compileAndInvoke(...)
	 * 
	 */
	
	/**
	 * @param name
	 * @param source
	 */
	@ManagedOperation(description="Removes the named script from the script cache")
	@ManagedOperationParameter(name="ScriptName", description="The name of the script to remove")

	public void compile(String name, CharSequence source) {
		if(name==null || name.trim().isEmpty()) throw new IllegalArgumentException("The passed script name was null or empty", new Throwable());
		if(source==null || source.length()==0) throw new IllegalArgumentException("The passed source was null or empty", new Throwable());
		Script script = new GroovyShell(compilerConfiguration).parse(source.toString(), name);
		compiledScripts.put(name, script);
	}
	
	
	/**
	 * Launches the groovy console 
	 */
	@ManagedOperation(description="Launches the groovy console")
	public void launchConsole() {
		try {
			Map<String, Object> beans = new HashMap<String, Object>();
			for(String beanName: applicationContext.getBeanDefinitionNames()) {
				beans.put(beanName, applicationContext.getBean(beanName));
			}
			beans.put("RootCtx", applicationContext);
			Binding binding = new Binding(beans);
			Class<?> clazz = Class.forName("groovy.ui.Console");
			Constructor<?> ctor = clazz.getDeclaredConstructor(Binding.class);
			Object console = ctor.newInstance(binding);
			console.getClass().getDeclaredMethod("run").invoke(console);
		} catch (Exception e) {
			error("Failed to launch console", e);
			throw new RuntimeException("Failed to launch console", e);
		}		
	}
	
	/**
	 * <p>Fired when the app context starts. Triggers the creation of default bindings provided to all script invocations.</p>
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#onApplicationContextStart(org.springframework.context.event.ContextStartedEvent)
	 */
	@Override
	public void onApplicationContextStart(ContextStartedEvent event) {
		/* Build bindings */
	}

	/**
	 * Returns the compiler warning level
	 * @return the compiler warning level
	 * @see org.codehaus.groovy.control.CompilerConfiguration#getWarningLevel()
	 */
	@ManagedAttribute(description="The compiler warning level. Recognized values are: NONE:0, LIKELY_ERRORS:1, POSSIBLE_ERRORS:2, PARANOIA:3")
	public int getWarningLevel() {
		return compilerConfiguration.getWarningLevel();
	}

	/**
	 * Sets the compiler warning level
	 * @param level the compler warning level
	 * @see org.codehaus.groovy.control.CompilerConfiguration#setWarningLevel(int)
	 */
	@ManagedAttribute(description="The compiler warning level. Recognized values are: NONE:0, LIKELY_ERRORS:1, POSSIBLE_ERRORS:2, PARANOIA:3")
	public void setWarningLevel(int level) {
		compilerConfiguration.setWarningLevel(level);
	}

	/**
	 * Returns the compiler source encoding
	 * @return the compiler source encoding
	 * @see org.codehaus.groovy.control.CompilerConfiguration#getSourceEncoding()
	 */
	@ManagedAttribute(description="The compiler source encoding")
	public String getSourceEncoding() {
		return compilerConfiguration.getSourceEncoding();
	}

	/**
	 * Sets the compiler source encoding
	 * @param encoding the compiler source encoding
	 * @see org.codehaus.groovy.control.CompilerConfiguration#setSourceEncoding(java.lang.String)
	 */
	@ManagedAttribute(description="The compiler source encoding")
	public void setSourceEncoding(String encoding) {
		compilerConfiguration.setSourceEncoding(encoding);
	}

	/**
	 * Returns the compiler target directory
	 * @return the compiler target directory
	 * @see org.codehaus.groovy.control.CompilerConfiguration#getTargetDirectory()
	 */
	@ManagedAttribute(description="The compiler target directory")
	public String getTargetDirectory() {		
		File f = compilerConfiguration.getTargetDirectory();
		return f==null ? null : f.getAbsolutePath();
	}

	/**
	 * Sets the compiler target directory
	 * @param directory the compiler target directory
	 * @see org.codehaus.groovy.control.CompilerConfiguration#setTargetDirectory(java.lang.String)
	 */
	@ManagedAttribute(description="The compiler target directory")
	public void setTargetDirectory(String directory) {
		compilerConfiguration.setTargetDirectory(directory);
	}

	/**
	 * Indicates if the compiler is verbose
	 * @return true if the compiler is verbose, false otherwise
	 * @see org.codehaus.groovy.control.CompilerConfiguration#getVerbose()
	 */
	@ManagedAttribute(description="Indicates if the compiler is verbose")
	public boolean isVerbose() {
		return compilerConfiguration.getVerbose();
	}

	/**
	 * Sets the verbosity of the compiler
	 * @param verbose true to make verbose, false otherwise
	 * @see org.codehaus.groovy.control.CompilerConfiguration#setVerbose(boolean)
	 */
	@ManagedAttribute(description="Indicates if the compiler is verbose")
	public void setVerbose(boolean verbose) {
		compilerConfiguration.setVerbose(verbose);
	}

	/**
	 * Indicates if the compiler is in debug mode
	 * @return true if the compiler is in debug mode, false otherwise
	 * @see org.codehaus.groovy.control.CompilerConfiguration#getDebug()
	 */
	@ManagedAttribute(description="Indicates if the compiler is in debug mode")
	public boolean isDebug() {
		return compilerConfiguration.getDebug();
	}

	/**
	 * Sets the debug mode of the compiler
	 * @param debug true for debug, false otherwise
	 * @see org.codehaus.groovy.control.CompilerConfiguration#setDebug(boolean)
	 */
	@ManagedAttribute(description="Indicates if the compiler is in debug mode")
	public void setDebug(boolean debug) {
		compilerConfiguration.setDebug(debug);
	}

	/**
	 * Returns the compiler tolerance, which is the maximum number of non-fatal errors before compilation is aborted 
	 * @return the maximum number of non-fatal errors before compilation is aborted
	 * @see org.codehaus.groovy.control.CompilerConfiguration#getTolerance()
	 */
	@ManagedAttribute(description="The maximum number of non-fatal errors before compilation is aborted")
	public int getTolerance() {
		return compilerConfiguration.getTolerance();
	}

	/**
	 * Sets the compiler tolerance, which is the maximum number of non-fatal errors before compilation is aborted
	 * @param tolerance the maximum number of non-fatal errors before compilation is aborted
	 * @see org.codehaus.groovy.control.CompilerConfiguration#setTolerance(int)
	 */
	@ManagedAttribute(description="The maximum number of non-fatal errors before compilation is aborted")
	public void setTolerance(int tolerance) {
		compilerConfiguration.setTolerance(tolerance);
	}

	/**
	 * Returns the compiler's base script class
	 * @return the compiler's base script class
	 * @see org.codehaus.groovy.control.CompilerConfiguration#getScriptBaseClass()
	 */
	@ManagedAttribute(description="The compiler's base script class")
	public String getScriptBaseClass() {
		return compilerConfiguration.getScriptBaseClass();
	}

	/**
	 * Sets the compiler's base script class
	 * @param scriptBaseClass the compiler's base script class
	 * @see org.codehaus.groovy.control.CompilerConfiguration#setScriptBaseClass(java.lang.String)
	 */
	@ManagedAttribute(description="The compiler's base script class")
	public void setScriptBaseClass(String scriptBaseClass) {
		compilerConfiguration.setScriptBaseClass(scriptBaseClass);
	}

	/**
	 * Sets the compiler's minimum recompilation interval in seconds
	 * @param time the compiler's minimum recompilation interval in seconds
	 * @see org.codehaus.groovy.control.CompilerConfiguration#setMinimumRecompilationInterval(int)
	 */
	@ManagedAttribute(description="The compiler's minimum recompilation interval in seconds")
	public void setMinimumRecompilationInterval(int time) {
		compilerConfiguration.setMinimumRecompilationInterval(time);
	}

	/**
	 * Returns the compiler's minimum recompilation interval in seconds
	 * @return the compiler's minimum recompilation interval in seconds
	 * @see org.codehaus.groovy.control.CompilerConfiguration#getMinimumRecompilationInterval()
	 */
	@ManagedAttribute(description="The compiler's minimum recompilation interval in seconds")
	public int getMinimumRecompilationInterval() {
		return compilerConfiguration.getMinimumRecompilationInterval();
	}

	/**
	 * Sets the compiler's target bytecode version
	 * @param version the compiler's target bytecode version
	 * @see org.codehaus.groovy.control.CompilerConfiguration#setTargetBytecode(java.lang.String)
	 */
	@ManagedAttribute(description="The compiler's target bytecode version")
	public void setTargetBytecode(String version) {
		compilerConfiguration.setTargetBytecode(version);
	}

	/**
	 * Returns the compiler's target bytecode version
	 * @return the compiler's target bytecode version
	 * @see org.codehaus.groovy.control.CompilerConfiguration#getTargetBytecode()
	 */
	@ManagedAttribute(description="The compiler's target bytecode version")
	public String getTargetBytecode() {
		return compilerConfiguration.getTargetBytecode();
	}

	/**
	 * Returns the compiler's optimization options
	 * @return the compiler's optimization options
	 * @see org.codehaus.groovy.control.CompilerConfiguration#getOptimizationOptions()
	 */
	@ManagedAttribute(description="The compiler's optimization options")
	public Map<String, Boolean> getOptimizationOptions() {
		return compilerConfiguration.getOptimizationOptions();
	}

	/**
	 * Sets the compiler's optimization options
	 * @param options the compiler's optimization options
	 * @see org.codehaus.groovy.control.CompilerConfiguration#setOptimizationOptions(java.util.Map)
	 */
	@ManagedAttribute(description="The compiler's optimization options")
	public void setOptimizationOptions(Map<String, Boolean> options) {
		compilerConfiguration.setOptimizationOptions(options);
	}
}
