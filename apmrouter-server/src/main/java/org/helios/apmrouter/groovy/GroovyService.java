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
package org.helios.apmrouter.groovy;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;
import groovy.lang.GroovySystem;
import groovy.lang.Script;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.management.ObjectName;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.helios.apmrouter.groovy.annotations.Start;
import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.server.ServerComponentBean;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;

/**
 * <p>Title: GroovyService</p>
 * <p>Description: Interactive groovy service</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.groovy.GroovyService</code></p>
 */

public class GroovyService extends ServerComponentBean implements GroovyLoadedScriptListener{
	/** A map of compiled scripts keyed by an arbitrary reference name */
	protected final Map<String, Script> compiledScripts = new ConcurrentHashMap<String, Script>();
	
	/** The compiler configuration for script compilations */
	protected final CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
	/** A groovy classloader for compiling scripts */
	protected final GroovyClassLoader groovyClassLoader; 
	/** The shared bindings */
	protected final Map<String, Object> beans = new ConcurrentHashMap<String, Object>();
	
	/** A set of registered class listeners */
	protected final Set<GroovyLoadedScriptListener> listeners = new CopyOnWriteArraySet<GroovyLoadedScriptListener>();
	
	/** The compiler configuration's JMX ObjectName */
	protected final ObjectName compilerConfigurationObjectName;
	
	/** A set of implicit imports for the compiler configuration */
	protected final Set<String> imports = new CopyOnWriteArraySet<String>();
	/** The initial and default imports customizer for the compiler configuration */
	protected final ImportCustomizer importCustomizer = new ImportCustomizer(); 
	
	/**
	 * Creates a new GroovyService
	 */
	public GroovyService() {
		objectName = JMXHelper.objectName(getClass().getPackage().getName(), "service", getClass().getSimpleName());
		compilerConfigurationObjectName = JMXHelper.objectName(getClass().getPackage().getName(), "service", getClass().getSimpleName(), "type", "CompilerConfiguration");
		imports.add("import org.helios.apmrouter.groovy.annotations.*");
		compilerConfiguration.setOptimizationOptions(Collections.singletonMap("indy", true));
		groovyClassLoader =  new GroovyClassLoader(getClass().getClassLoader(), compilerConfiguration);
		registerLoadListener(this);
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.server.ServerComponentBean#doStart()
	 */
	protected void doStart() {
		applyImports(importCustomizer, imports.toArray(new String[imports.size()]));		
	}
	
	/**
	 * Applies the configured imports to the compiler configuration
	 * @param impCustomizer The import customizer to add the imports to
	 * @param imps  The imports to add
	 */
	protected void applyImports(ImportCustomizer impCustomizer, String...imps) {		
		for(String imp: imps) {
			String _imp = imp.trim().replaceAll("\\s+", " ");
			if(!_imp.startsWith("import")) {
				warn("Unrecognized import [", imp, "]");
				continue;
			}
			if(_imp.startsWith("import static ")) {
				if(_imp.endsWith(".*")) {
					impCustomizer.addStaticStars(_imp.replace("import static ", "").replace(".*", ""));
				} else {
					String cleaned = _imp.replace("import static ", "").replace(".*", "");
					int index = cleaned.lastIndexOf('.');
					if(index==-1) {
						warn("Failed to parse non-star static import [", imp, "]");
						continue;
					}
					impCustomizer.addStaticImport(cleaned.substring(0, index), cleaned.substring(index+1));
				}
			} else {
				if(_imp.endsWith(".*")) {
					impCustomizer.addStarImports(_imp.replace("import ", "").replace(".*", ""));
				} else {
					impCustomizer.addImports(_imp.replace("import ", ""));
				}
			}
		}
		compilerConfiguration.addCompilationCustomizers(impCustomizer);
	}
	
	/**
	 * Registers the passed load listener
	 * @param listener the load listener
	 */
	public void registerLoadListener(GroovyLoadedScriptListener listener) {
		if(listener!=null) {
			listeners.add(listener);
		}
	}
	
	
	/**
	 * Unregisters the passed load listener
	 * @param listener the load listener
	 */
	public void unregisterLoadListener(GroovyLoadedScriptListener listener) {
		if(listener!=null) {
			listeners.remove(listener);
		}
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
	 * Compiles the passed source and assignes it the passed name
	 * @param name The name assigned to the compiled script
	 * @param source The source code of the script to compiled
	 */
	@ManagedOperation(description="Compiles the passed source and assignes it the passed name")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name="ScriptName", description="The name assigned to the compiled script"),
		@ManagedOperationParameter(name="Source", description="The source code of the script to be compiled")
	})
	public void compile(String name, String source) {
		if(name==null || name.trim().isEmpty()) throw new IllegalArgumentException("The passed script name was null or empty", new Throwable());
		if(source==null || source.length()==0) throw new IllegalArgumentException("The passed source was null or empty", new Throwable());		
		Script script = null;
		try {
			script = new GroovyShell(compilerConfiguration).parse(source, name);
			info("Compiled script named [" , name , "]. Class is: [", script.getClass().getName(), "]");
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			throw new RuntimeException(ex);
		}		
		Binding bindings = getBindings();
		script.setBinding(bindings);
		script.setProperty("bindings", bindings);
		compiledScripts.put(name, script);
		
		Class<?> clazz = script.getClass();//groovyClassLoader.parseClass(source);
		scanLoadedClass(clazz, script);
	}
	
	
	
	/**
	 * Scans the passed class and looks for matches with registered groovy loaded class listeners, firing their callbacks when matches are found
	 * @param clazz The class to scan
	 * @param instance The instance of the scanned class
	 */
	protected void scanLoadedClass(Class<?> clazz, Object instance) {
		if(listeners.isEmpty()) return;
		Map<Class<? extends Annotation>, Annotation> typeAnns = new HashMap<Class<? extends Annotation>, Annotation>();
		Map<Class<? extends Annotation>, Map<Method, Set<Annotation>>> methodAnns = new HashMap<Class<? extends Annotation>, Map<Method, Set<Annotation>>>();
		for(Annotation annot: clazz.getAnnotations()) {
			typeAnns.put(annot.annotationType(), annot);
		}
		for(Method m: clazz.getMethods()) {
			for(Annotation annot: m.getAnnotations()) {
				addMethodAnnotation(methodAnns, m, annot);
			}				
		}
		for(Method m: clazz.getDeclaredMethods()) {
			for(Annotation annot: m.getAnnotations()) {
				addMethodAnnotation(methodAnns, m, annot);
			}				
		}
		for(GroovyLoadedScriptListener listener: listeners) {
			Set<Annotation> matchedAnnotations = new HashSet<Annotation>();
			for(Class<? extends Annotation> cl: listener.getScanTypeAnnotations()) {
				Annotation matchedAnnotation = typeAnns.get(cl);
				if(matchedAnnotation!=null) {
					matchedAnnotations.add(matchedAnnotation);
				}
			}			
			if(!matchedAnnotations.isEmpty()) {
				listener.onScanType(matchedAnnotations, clazz, instance);				
			}
			Map<Method, Set<Annotation>> matchedMethodAnnotations = new HashMap<Method, Set<Annotation>>();
			Set<Class<? extends Annotation>> listenerMethodAnnotationTypes = listener.getScanMethodAnnotations();
			for(Method method: clazz.getMethods()) {
				for(Annotation methodAnnotation: method.getAnnotations()) {
					if(listenerMethodAnnotationTypes.contains(methodAnnotation.annotationType())) {
						Set<Annotation> annotationSet = matchedMethodAnnotations.get(method);
						if(annotationSet==null) {
							annotationSet = new HashSet<Annotation>();
							matchedMethodAnnotations.put(method, annotationSet);
						}
						annotationSet.add(methodAnnotation);
					}
				}
			}
			for(Method method: clazz.getDeclaredMethods()) {
				for(Annotation methodAnnotation: method.getAnnotations()) {
					if(listenerMethodAnnotationTypes.contains(methodAnnotation.annotationType())) {
						Set<Annotation> annotationSet = matchedMethodAnnotations.get(method);
						if(annotationSet==null) {
							annotationSet = new HashSet<Annotation>();
							matchedMethodAnnotations.put(method, annotationSet);
						}
						annotationSet.add(methodAnnotation);
					}
				}
			}
			if(!matchedMethodAnnotations.isEmpty()) {
				listener.onScanMethod(matchedMethodAnnotations, clazz, instance);
			}
			Set<Class<?>> matchedParentClasses = new HashSet<Class<?>>();
			for(Class<?> parentClass: listener.getScanClasses()) {
				if(parentClass.isAssignableFrom(clazz)) {
					matchedParentClasses.add(parentClass);
				}				
			}
			if(!matchedParentClasses.isEmpty()) {
				listener.onScanClasses(matchedParentClasses, clazz, instance);
			}
		}
	}
	
	/**
	 * Adds the passed method and it's associated annotation to the passed annotation tree
	 * @param annotationTree The method annotation tree for the class being scanned
	 * @param method The scanned method
	 * @param annotation The annotation associated with the passed method
	 */
	protected void addMethodAnnotation(final Map<Class<? extends Annotation>, Map<Method, Set<Annotation>>> annotationTree, final Method method, final Annotation annotation) {
		Class<? extends Annotation> annClass = annotation.annotationType();
		Map<Method, Set<Annotation>> methodSets = annotationTree.get(annClass);
		if(methodSets==null) {
			methodSets = new HashMap<Method, Set<Annotation>>();
			methodSets.put(method, new HashSet<Annotation>());
			annotationTree.put(annClass, methodSets);
		}
		methodSets.get(method).add(annotation);
	}
	
	
	/**
	 * Compiles the passed source and assignes it the passed name
	 * @param name The name assigned to the compiled script
	 * @param source The source code of the script to compiled
	 */
	@ManagedOperation(description="Compiles the passed source and assignes it the passed name")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name="ScriptName", description="The name assigned to the compiled script"),
		@ManagedOperationParameter(name="Source", description="The source code of the script to be compiled")
	})
	public void compileBuffer(String name, CharSequence source) {
		if(source==null || source.length()==0) throw new IllegalArgumentException("The passed source was null or empty", new Throwable());
		compile(name, source.toString());
	}
	
	
	/** Empty object array constant */
	protected static final Object[] EMPTY_OBJ_ARR = {}; 
	
	/**
	 * Executes the main function of the named script
	 * @param name The name of the script to execute
	 * @param args The optional arguments to pass to the script
	 * @return the return value from the script invocation
	 */
	@ManagedOperation(description="Executes the main function of the named script")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name="ScriptName", description="The name of the compiled script to run"),
		@ManagedOperationParameter(name="Args", description="The optional arguments to pass to the script")
	})
	public Object runScript(String name, Object...args) {
		if(name==null || name.trim().isEmpty()) throw new IllegalArgumentException("The passed script name was null or empty", new Throwable());
		Script script = compiledScripts.get(name);
		if(script==null) throw new RuntimeException("Failed to find script named [" + name + "]");
		script.setProperty("args", (args!=null && args.length>0) ? args : EMPTY_OBJ_ARR);
		return script.run();
	}
	
	/**
	 * Executes the main function of the named script
	 * @param name The name of the script to execute
	 * @return the return value from the script invocation
	 */
	@ManagedOperation(description="Executes the main function of the named script")
	@ManagedOperationParameters({
		@ManagedOperationParameter(name="ScriptName", description="The name of the compiled script to run")
	})
	public Object runScript(String name) {
		return runScript(name, EMPTY_OBJ_ARR);
	}
	
	
	
	/**
	 * Returns a bindings instance
	 * @return a bindings instance
	 */
	protected Binding getBindings() {
		if(beans.isEmpty()) {
			synchronized(beans) {
				if(beans.isEmpty()) {
					for(String beanName: applicationContext.getBeanDefinitionNames()) {
						beans.put(beanName, applicationContext.getBean(beanName));
					}
					beans.put("RootCtx", applicationContext);					
				}
			}
		}
		return new Binding(beans);
	}
	
	
	/**
	 * Launches the groovy console 
	 */
	@ManagedOperation(description="Launches the groovy console")
	public void launchConsole() {
		try {
			Class<?> clazz = Class.forName("groovy.ui.Console");
			Constructor<?> ctor = clazz.getDeclaredConstructor(Binding.class);
			Object console = ctor.newInstance(getBindings());
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

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.groovy.GroovyLoadedScriptListener#getScanTypeAnnotations()
	 */
	@Override
	public Set<Class<? extends Annotation>> getScanTypeAnnotations() {
		return Collections.emptySet();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.groovy.GroovyLoadedScriptListener#getScanMethodAnnotations()
	 */
	@Override
	public Set<Class<? extends Annotation>> getScanMethodAnnotations() {
		return new HashSet<Class<? extends Annotation>>(Arrays.asList(Start.class));
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.groovy.GroovyLoadedScriptListener#getScanClasses()
	 */
	@Override
	public Set<Class<?>> getScanClasses() {
		return Collections.emptySet();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.groovy.GroovyLoadedScriptListener#onScanType(java.util.Set, java.lang.Class, java.lang.Object)
	 */
	@Override
	public void onScanType(Set<? extends Annotation> annotations, Class<?> clazz, Object instance) {
		info("\n\t===================================\n\tType Annotation Match:", clazz.getName(), "\n\t===================================\n");
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.groovy.GroovyLoadedScriptListener#onScanMethod(java.util.Map, java.lang.Class, java.lang.Object)
	 */
	@Override
	public void onScanMethod(Map<Method, Set<Annotation>> methods, Class<?> clazz, Object instance) {
		StringBuilder b = new StringBuilder("\n\t===================================\n\tMethod Annotation Match:" + clazz.getName() + "\n\t===================================\n");
		for(Map.Entry<Method, Set<Annotation>> match: methods.entrySet()) {
			b.append("\n\tMethod:").append(match.getKey().getName());
			for(Annotation ann: match.getValue()) {
				b.append("\n\t\tAnn:" + ann.annotationType().getSimpleName());
			}
		}
		info(b);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.groovy.GroovyLoadedScriptListener#onScanClasses(java.util.Set, java.lang.Class, java.lang.Object)
	 */
	@Override
	public void onScanClasses(Set<Class<?>> annotations, Class<?> clazz, Object instance) {
		info("\n\t===================================\n\tInherritance Match:", clazz.getName(), "\n\t===================================\n");
	}


	/**
	 * Returns the currently configured compiler imports
	 * @return the currently configured compiler imports
	 */
	@ManagedAttribute(description="The currently configured compiler imports")
	public Set<String> getImports() {
		return imports;
	}
	
	/**
	 * Adds the passed imports to the configured compiler imports
	 * @param imps the imports to add
	 */
	public void setImports(Set<String> imps) {
		if(imps!=null) {
			imps.removeAll(imports);
			if(imps.isEmpty()) return;
			if(this.isStarted()) {								
				applyImports(new ImportCustomizer(), imps.toArray(new String[imps.size()]));
				imports.addAll(imps);
			} else {
				imports.addAll(imps);
			}
		}
	}
}
