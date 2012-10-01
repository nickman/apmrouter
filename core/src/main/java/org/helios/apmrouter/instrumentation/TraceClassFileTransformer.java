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
package org.helios.apmrouter.instrumentation;


import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javassist.ByteArrayClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;

/**
 * <p>Title: TraceClassFileTransformer</p>
 * <p>Description: Implementation of a {@link ClassFileTransformer} that instruments {@link Trace} annotated methods</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.instrumentation.TraceClassFileTransformer</code></p>
 */

public class TraceClassFileTransformer implements ClassFileTransformer {
	/** A set of package names that will be inspected for {@link Trace} annotated methods */
	protected final Set<String> targetPackages = new HashSet<String>();
	
	
	
	/**
	 * Creates a new TraceClassFileTransformer
	 * @param targetPackages A set of package names that will be inspected for {@link Trace} annotated methods 
	 */
	public TraceClassFileTransformer(Set<String> targetPackages) {
		if(targetPackages!=null) {
			this.targetPackages.addAll(targetPackages);
		}
	}
	
	public static void log(Object msg) {
		System.out.println("[TraceClassFileTransformer]:" + msg);
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.instrument.ClassFileTransformer#transform(java.lang.ClassLoader, java.lang.String, java.lang.Class, java.security.ProtectionDomain, byte[])
	 */
	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
		ClassPool tClassPool = null;
		try {
			final String clazzName = className.replace('/', '.');
			final String packageName = clazzName.substring(0, clazzName.lastIndexOf("."));
			if(!targetPackages.contains(packageName)) return classfileBuffer;
			log("Inspecting class [" + clazzName + "]");
			tClassPool = new ClassPool(true);
			tClassPool.appendClassPath(new LoaderClassPath(loader));
			tClassPool.appendClassPath(new ByteArrayClassPath(className, classfileBuffer));
			CtClass clazz = tClassPool.get(clazzName);
			Map<CtMethod, TraceImpl> methodAnnotations = new HashMap<CtMethod, TraceImpl>();
			for(CtMethod ctMethod : clazz.getDeclaredMethods()) {
				Object traceAnnotation = ctMethod.getAnnotation(Trace.class);
				if(traceAnnotation!=null) {	
					log("Found @Trace annotation [" + traceAnnotation+ "] on [" + ctMethod.getLongName() + "]");
					try {
						TraceImpl trace = new TraceImpl(traceAnnotation);
						methodAnnotations.put(ctMethod, trace);
						log("Trace:\n" + trace);
					} catch (Throwable t) {
						t.printStackTrace(System.err);
						return classfileBuffer;
					}
				}
			}
			if(!methodAnnotations.isEmpty()) {
				byte[] byteCode = instrumentClass(tClassPool, clazz, methodAnnotations);
				return byteCode;
			}
			return classfileBuffer;
		} catch (Exception e) {
			e.printStackTrace(System.err);
			return classfileBuffer;
		} finally {
			
		}
	}
	
	/**
	 * Instruments the passed class
	 * @param cp The class pool in use
	 * @param clazz The class to instrument
	 * @param methodAnnotations A map of trace annotations keyed by the methods they annotated
	 * @return the byte code array of the instrumented class
	 */
	protected byte[] instrumentClass(ClassPool cp, CtClass clazz, Map<CtMethod, TraceImpl> methodAnnotations) {
		
		
		return null;
	}

}
