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
package org.helios.apmrouter.instrumentation.publifier;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javassist.ByteArrayClassPath;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;

import org.helios.apmrouter.jmx.JMXHelper;
import org.helios.apmrouter.util.SimpleLogger;

/**
 * <p>Title: ClassPublifier</p>
 * <p>Description: A class file transformer that redefines a class as being <b>public</b> and retains the byte code so the change can be rolled back.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.instrumentation.publifier.ClassPublifier</code></p>
 */

public class ClassPublifier implements ClassFileTransformer {
	/** The singleton instance */
	private static volatile ClassPublifier instance = null; 
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** A map of classes that are pending publification keyed by their binary class names */
	private final ThreadLocal<Map<String, Class<?>>> toPublify = new ThreadLocal<Map<String, Class<?>>>(){
		@Override
		protected Map<String, Class<?>> initialValue() {
			return new HashMap<String, Class<?>>();
		}		
	};
	/** A map of classes that are pending reversion keyed by their binary class names */
	private final ThreadLocal<Map<String, Class<?>>> toRevert = new ThreadLocal<Map<String, Class<?>>>(){
		@Override
		protected Map<String, Class<?>> initialValue() {
			return new HashMap<String, Class<?>>();
		}		
	};

	/** A map of the original byte code of publified classes, used to revert a publification */
	private final Map<String, byte[]> originalByteCode = new ConcurrentHashMap<String, byte[]>();
	/** A thread local containing a map of the most recently executed publifications by the current thread where they key is the standard class name and the value is the classloader of the class */
	private final ThreadLocal<Map<String, ClassLoader>> lastPub = new ThreadLocal<Map<String, ClassLoader>>() {
		@Override
		protected Map<String, ClassLoader> initialValue() {
			return new HashMap<String, ClassLoader>();
		}
	};
	
	/** The instrumentation instance */
	private final Instrumentation instrumentation;
	/** The name of the marker interface applied to publified classes */
	public static final String PUBLIFIED_IFACE = Publified.class.getName();
	
	/**
	 * Acquires the singleton instance
	 * @return the ClassPublifier singleton instance
	 */
	public static final ClassPublifier getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new ClassPublifier();					
				}
			}
		}
		return instance;
	}
	
	private ClassPublifier() { 
		try {
			instrumentation = (Instrumentation)JMXHelper.getHeliosMBeanServer().getAttribute(org.helios.apmrouter.jagent.Instrumentation.OBJECT_NAME, "Instance");
		} catch (Exception ex) {
			throw new RuntimeException("Failed to load instrumentation", ex);
		}
	}
	
	/** Empty class array constant */
	private static final Class<?>[] NULL_CLASS_ARR = new Class[0];
	
	/**
	 * Publifies the passed class names. This call cannot be reverted.
	 * @param classNames The names of the classes to publify
	 */
	public void publify(String...classNames) {
		if(classNames==null || classNames.length<1) return;
		final Set<String> binaryClassNames = new HashSet<String>(classNames.length);
		for(String className: classNames) {
			if(className==null || className.trim().isEmpty()) continue;
			binaryClassNames.add(convertToBinaryName(className.trim()));
		}
		instrumentation.addTransformer(new ClassFileTransformer(){
			/**
			 * {@inheritDoc}
			 * @see java.lang.instrument.ClassFileTransformer#transform(java.lang.ClassLoader, java.lang.String, java.lang.Class, java.security.ProtectionDomain, byte[])
			 */
			@Override
			public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
				ClassPool tClassPool = null;
				try {
					if(binaryClassNames.contains(className)) {
						tClassPool = new ClassPool();
						tClassPool.appendSystemPath();
						CtClass clazz = tClassPool.get(convertFromBinaryName(className));
						clazz.setModifiers(clazz.getModifiers() | Modifier.PUBLIC);
						
						byte[] byteCode = clazz.toBytecode();
						SimpleLogger.info("\n\tPublified [" , clazz.getName(), "]\n");
						return byteCode;
					}
				} catch (Throwable ex) {
					SimpleLogger.warn("Failed to  publify [", className, "]", ex);
					return classfileBuffer;
				}
				return classfileBuffer;				
			}
		}, true);
		for(String className: classNames) {
			try { ClassLoader.getSystemClassLoader().getParent().loadClass(className); } catch (Throwable ex) {/* No Op*/}
		}
	}	
	
	/**
	 * Publifies the passed classes
	 * @param redefine If true, classes are redefined, otherwise, they are retransformed
	 * @param classes The classes to publify
	 * @return An array of the publified classes
	 */
	public Class<?>[] publify(boolean redefine, Class<?>...classes) {
		if(classes==null || classes.length<1) return NULL_CLASS_ARR; 
		try {
			for(Class<?> clazz: classes) {
				if(clazz==null) continue;
				toPublify.get().put(convertToBinaryName(clazz.getName()), clazz);
			}			
			instrumentation.addTransformer(this, true);
			try {
				if(redefine) {
					try {
						instrumentation.redefineClasses(getClassDefinitions(classes));
					} catch (ClassNotFoundException e) {						
						throw new RuntimeException("Failed to redefine classes " + Arrays.toString(classes), e);
					}
				} else {
					instrumentation.retransformClasses(classes);
				}
			} catch (UnmodifiableClassException e) {
				throw new RuntimeException("A class was found unmodifiable", e);
			}
			Set<Class<?>> publifiedClasses = new HashSet<Class<?>>();
			for(Map.Entry<String, ClassLoader> entry: lastPub.get().entrySet()) {
				try {
					publifiedClasses.add(entry.getValue().loadClass(entry.getKey()));
				} catch (Exception ex) {
					SimpleLogger.error("Failed to load class [" , entry.getKey(), "]", ex);
				}
			}
			return publifiedClasses.toArray(new Class[publifiedClasses.size()]);
		} finally {
			lastPub.remove();
			instrumentation.removeTransformer(this);
		}
	}
	
	/**
	 * Acquires {@link ClassDefinition}s for the passed classes
	 * @param classes The classes to get {@link ClassDefinition}s for 
	 * @return an array of {@link ClassDefinition}s
	 */
	protected ClassDefinition[] getClassDefinitions(Class<?>...classes) {
		if(classes==null || classes.length<1) return new ClassDefinition[0];
		Set<ClassDefinition> defs = new HashSet<ClassDefinition>();
		try {			
			ClassPool cp = new ClassPool();
			for(Class<?> clazz: classes) {
				cp.appendClassPath(new ClassClassPath(clazz));
			}
			for(Class<?> clazz: classes) {
				byte[] byteCode = originalByteCode.get(clazz.getName());
				if(byteCode!=null) {
					defs.add(new ClassDefinition(clazz, byteCode));
				} else {
					CtClass ctClazz = cp.get(clazz.getName());
					defs.add(new ClassDefinition(clazz, ctClazz.toBytecode()));
					ctClazz.detach();
				}
			}			
		} catch (Exception ex) {
			throw new RuntimeException("Failed to create class definitions for " + Arrays.toString(classes), ex);
		} finally {
			/* No Op */
		}
		
		return defs.toArray(new ClassDefinition[defs.size()]);
	}
	
	/**
	 * Reverts the passed classes to their original byte code
	 * @param redefine If true, classes are redefined, otherwise, they are retransformed
	 * @param classes The classes to revert
	 * @return The reverted classes
	 */
	public synchronized Class<?>[] revert(boolean redefine, Class<?>...classes) {
		if(classes==null || classes.length<1) return NULL_CLASS_ARR;
		try {
			for(Class<?> clazz: classes) {
				if(clazz==null) continue;
				toRevert.get().put(convertToBinaryName(clazz.getName()), clazz);
			}						
			instrumentation.addTransformer(this, true);
			try {
				if(redefine) {
					try {
						instrumentation.redefineClasses(getClassDefinitions(classes));
					} catch (ClassNotFoundException e) {						
						throw new RuntimeException("Failed to redefine classes " + Arrays.toString(classes), e);
					}
				} else {
					instrumentation.retransformClasses(classes);
				}
			} catch (UnmodifiableClassException e) {
				throw new RuntimeException("A class was found unmodifiable", e);
			}
			Set<Class<?>> revertedClasses = new HashSet<Class<?>>();
			for(Map.Entry<String, ClassLoader> entry: lastPub.get().entrySet()) {
				try {
					revertedClasses.add(entry.getValue().loadClass(entry.getKey()));
				} catch (Exception ex) {
					SimpleLogger.error("Failed to load class [" , entry.getKey(), "]", ex);
				}
			}
			return revertedClasses.toArray(new Class[revertedClasses.size()]);
		} finally {
			lastPub.remove();
			instrumentation.removeTransformer(this);
		}
	}
	
	

	
	/**
	 * Converts a class name to the binary name used by the class file transformer, or returns the passed name if it is already a binary name
	 * @param name The class name to convert
	 * @return the binary name
	 */
	protected String convertToBinaryName(String name) {
		int index = name.indexOf('.');
		if(index!=-1) {
			return name.replace('.', '/');
		}
		return name;
	}
	
	/**
	 * Converts a class name from the binary name used by the class file transformer to the standard dot notated form, or returns the passed name if it is already a binary name
	 * @param name The class name to convert
	 * @return the standard dot notated class name
	 */
	protected String convertFromBinaryName(String name) {
		int index = name.indexOf('/');
		if(index!=-1) {
			return name.replace('/', '.');
		}
		return name;
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.instrument.ClassFileTransformer#transform(java.lang.ClassLoader, java.lang.String, java.lang.Class, java.security.ProtectionDomain, byte[])
	 */
	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		ClassPool tClassPool = null;
		try {
			if(toPublify.get().containsKey(className)) {
				String name = convertFromBinaryName(className);
				tClassPool = new ClassPool();
				tClassPool.appendClassPath(new ByteArrayClassPath(name, classfileBuffer));
				tClassPool.appendSystemPath();
				CtClass clazz = tClassPool.get(name);
				clazz.setModifiers(clazz.getModifiers() | Modifier.PUBLIC);
				byte[] byteCode = clazz.toBytecode();
				SimpleLogger.info("\n\tGenerated Byte Code for [" , clazz.getName(), "]\n");
				originalByteCode.put(className, byteCode);
				lastPub.get().put(name, loader);
				return byteCode;
			} else	if(toRevert.get().containsKey(className)) {				
				byte[] byteCode = originalByteCode.remove(className);
				if(byteCode!=null) {
					String name = convertFromBinaryName(className);
					lastPub.get().put(name, loader);
					return byteCode;
				}
			}			
			return classfileBuffer;
		} catch (Throwable ex) {
			SimpleLogger.warn("Failed to  transform [", className, "]", ex);
			throw new RuntimeException("Failed to transform  [" + className + "]", ex);
		}
	}

}
