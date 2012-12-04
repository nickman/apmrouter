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
package org.helios.apmrouter.codahale.agent;

import java.lang.annotation.Annotation;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;

import org.helios.apmrouter.codahale.annotation.Timed;

/**
 * <p>Title: CodahaleClassTransformer</p>
 * <p>Description: Java Agent class transformer for <a href="http://metrics.codahale.com">Codahale</a> instrumentation</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.codahale.agent.CodahaleClassTransformer</code></p>
 */

public class CodahaleClassTransformer implements ClassFileTransformer {
	/** A set of the package names (in binary format) to be inspected for instrumentation */
	protected final Set<String> targetPackages = new CopyOnWriteArraySet<String>();
	/** A set of the package names (in binary format) that should not be instrumented */
	protected final Set<String> prohibitedPackages = new CopyOnWriteArraySet<String>();
	/** A set of instrumented class info */
	protected final Set<InstrumentedClassInfo> classInfo = new CopyOnWriteArraySet<InstrumentedClassInfo>();
	
	/**
	 * Creates a new CodahaleClassTransformer
	 * @param packageNames An array of package names to instrument
	 */
	public CodahaleClassTransformer(String...packageNames) {
		for(String s: packageNames) {
			if(s.indexOf('.')!=-1) {
				targetPackages.add(s.replace('.', '/').trim());
			}
		}
	}
	
	/**
	 * Creates a new CodahaleClassTransformer
	 * @param packageNames A collection of package names to instrument
	 */
	public CodahaleClassTransformer(Collection<String> packageNames) {
		this(packageNames.toArray(new String[0]));
	}
	
	/**
	 * Converts from a standard java class name to the binary class name
	 * @param className The dot separated java class name
	 * @return The / separated class name
	 */
	public static String toBinaryName(String className) {
		return className.replace('.', '/');
	}
	
	/**
	 * Converts from a binary java class name to the java internal class name
	 * @param binClassName The / separated java class name
	 * @return The . separated class name
	 */
	public static String toName(String binClassName) {
		return binClassName.replace('/', '.');
	}
	
	/**
	 * Returns the binary package name for the passed binary class name
	 * @param binClassName The / separated class name
	 * @return the / separated package name
	 */
	public static String getBinaryPackage(String binClassName) {
		int index = binClassName.lastIndexOf('/');
		if(index==0) return binClassName;
		return binClassName.substring(0, index);
	}
	
	/**
	 * Out logger
	 * @param msg the message
	 */
	protected static void log(Object msg) {
		System.out.println(msg);
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.instrument.ClassFileTransformer#transform(java.lang.ClassLoader, java.lang.String, java.lang.Class, java.security.ProtectionDomain, byte[])
	 */
	@Override
	public byte[] transform(ClassLoader classLoader, String className, Class<?> classToBeTransformed,
			ProtectionDomain protectionDomain, byte[] classFileBuffer) throws IllegalClassFormatException {
		final byte[] original = classFileBuffer;
		try {
			String packageName = getBinaryPackage(className);
			if(!targetPackages.contains(packageName) || prohibitedPackages.contains(packageName)) {
				return original;
			}
			ClassPool cp = ClassPool.getDefault();
			cp.appendClassPath(new LoaderClassPath(classLoader));
			CtClass clazz = cp.get(toName(className));
			log("Loaded CtClass [" + clazz.getName() + "]");
			for(CtMethod method: clazz.getDeclaredMethods()) {
				Annotation[] annotations = getAnnotations(method);
				if(annotations.length>0) {
					log("Found [" + annotations.length + "] annotations on [" + clazz.toString() + "]");
					instrumentMethod(method, annotations);
				}
			}
		} catch (Exception ex) {
			log("Failed to instrument class [" + className + "]:" + ex + ". Stack trace follows.");
			ex.printStackTrace(System.err);
			return original;
		} 
		return null;
	}
	
	protected void instrumentMethod(CtMethod method, Annotation...annotations) {
		for(Annotation annotation: annotations) {
			if(AnnotationType.isSupportedAnnotation(annotation)) {
				AnnotationType annotationType = AnnotationType.getTypeForAnnotation(annotation);
			}
		}
	}
	
	protected void instrumentMethod(CtMethod method, Annotation ann, AnnotationType annotationType) {
		switch (annotationType) {
			case TIMED:
				instrumentMethod(method, (Timed)ann);
				break;
			default:
				break;
		}
	}
	
	protected void instrumentMethod(CtMethod method, Timed timed) {
		
	}
	
	
	/**
	 * Returns an array of annotation instances found for the passed CtMethod.
	 * @param method The CtMethod to look for annotations on
	 * @return A possible empty array of annotations
	 */
	protected Annotation[] getAnnotations(CtMethod method) {
		Set<Annotation> annotations = new HashSet<Annotation>();
		try {
			annotations.add((Annotation) method.getAnnotation(Timed.class));
		} catch (ClassNotFoundException e) { /* No Op */ }
		
		return annotations.toArray(new Annotation[annotations.size()]);
	}

}
