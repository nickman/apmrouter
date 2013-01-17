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
package org.helios.apmrouter.byteman.sockets.impl;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.net.SocketImpl;
import java.net.SocketImplFactory;
import java.net.SocketOptions;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import javassist.ByteArrayClassPath;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.Modifier;
import javassist.NotFoundException;

import org.helios.apmrouter.util.SimpleLogger;

/**
 * <p>Title: SocketImplTransformer</p>
 * <p>Description: Transforms {@link SocketImpl} classes</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.byteman.sockets.impl.SocketImplTransformer</code></p>
 * TODO:  Sysprops and impls for:
 * 		logging
 *      class writes
 */

public class SocketImplTransformer implements ClassFileTransformer {
	/** The class name of {@link SocketImpl} */
	public static final String SOCK_NAME = "java.net.SocketImpl";
	/** The binary class name of {@link SocketImpl} */
	public static final String SOCK_BIN_NAME = "java/net/SocketImpl";
	/** The class name of {@link SocketImplFactory} */
	public static final String SOCK_FACTORY_NAME = "java.net.SocketImplFactory";
	/** The binary class name of {@link SocketImplFactory} */
	public static final String SOCK_FACTORY_BIN_NAME = "java/net/SocketImplFactory";
	/** The wrapping socket impl factory */
	public static final Class<TrackingSocketImpl> TrackingSocketImplClass = TrackingSocketImpl.class; 
	/** The publicizing interface to be applied to socketimpls  */
	public static final Class<ISocketImpl> ISocketImplClass = ISocketImpl.class; 

	/** The class name of {@link SocketOptions} */
	public static final String SOCK_OPT_NAME = "java.net.SocketOptions";
	
	/** The recursion level for the current thread */
	protected static final ThreadLocal<AtomicInteger> recursionLevel = new ThreadLocal<AtomicInteger>() {
		@Override
		protected AtomicInteger initialValue() {			
			return new AtomicInteger();
		}
	};
	
	/** The javassist classpool used by this transformer */
	protected final ClassPool classPool;
	/** The javassist ctclass representation of {@link SocketImpl} */
	protected final CtClass socketImplCtClass;
	/** The javassist ctclass representation of {@link SocketImplFactory} */
	protected final CtClass socketImplFactoryCtClass;
	/** The javassist method representation of {@link SocketImplFactory#createSocketImpl} */
	protected final CtMethod createSocketImplMethod;
	/** The javassist ctclass representation of {@link TrackingSocketImpl} */
	protected final CtClass trackingSocketImplCtClass;
	/** The javassist ctclass representation of {@link ISocketImpl} */
	protected final CtClass iSocketImplCtClass;
	
	
	/** The javassist ctclass representation of {@link SocketOptions} */
	protected final CtClass socketOptionsCtClass;
	/** The javassist ctclass representation of {@link Object} */
	protected final CtClass objectCtClass;
	
	/**
	 * Creates a new SocketImplTransformer
	 */
	public SocketImplTransformer() {
		try {
			classPool = new ClassPool(true);
			classPool.appendClassPath(new ClassClassPath(TrackingSocketImplClass));
			classPool.importPackage(TrackingSocketImplClass.getPackage().getName());
			trackingSocketImplCtClass = classPool.get(TrackingSocketImplClass.getName());
			iSocketImplCtClass = classPool.get(ISocketImplClass.getName());
			socketImplCtClass = classPool.get(SOCK_NAME);
			socketOptionsCtClass = classPool.get(SOCK_OPT_NAME);
			socketImplFactoryCtClass = classPool.get(SOCK_FACTORY_NAME);
			createSocketImplMethod = socketImplFactoryCtClass.getDeclaredMethod("createSocketImpl");
			objectCtClass = classPool.get("java.lang.Object");
			SimpleLogger.info("Created SocketImplTransformer");
		} catch (Exception ex) {
			SimpleLogger.error("Failed to initialize SocketImpleTransformer", ex);
			throw new RuntimeException("Failed to initialize SocketImpleTransformer", ex);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.instrument.ClassFileTransformer#transform(java.lang.ClassLoader, java.lang.String, java.lang.Class, java.security.ProtectionDomain, byte[])
	 */
	@Override
	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			final byte[] classfileBuffer) throws IllegalClassFormatException {
		LoaderClassPath lcp = null;
		final ByteArrayClassPath bcp = new ByteArrayClassPath(convertFromBinaryName(className), classfileBuffer);
		final int recLevel = recursionLevel.get().incrementAndGet();
		try {
			
			if(loader!=null) {
				lcp = new LoaderClassPath(loader);
				classPool.appendClassPath(lcp);
			}
			CtClass clazz = classPool.get(convertFromBinaryName(className));
			if(clazz.isInterface()) {
				for(CtClass iface: clazz.getInterfaces()) {
					
				}
			} else {
				if(Arrays.binarySearch(clazz.getInterfaces(), socketOptionsCtClass)<0) {
					return classfileBuffer;
				}
			}
			
		} catch (Throwable t) {
			t.printStackTrace(System.err);
			return classfileBuffer;
		} finally{
			if(recursionLevel.get().decrementAndGet()==0) {
				recursionLevel.remove();
			}			
			classPool.removeClassPath(bcp);
			if(lcp!=null) classPool.removeClassPath(lcp);
		}
		
		return null;
	}
	
	/**
	 * Determines if the passed class has {@link SocketImpl} as a direct or indirect superclass
	 * @param clazz The class to test  
	 * @return true if the passed class has {@link SocketImpl} as a direct or indirect superclass
	 * @throws NotFoundException thrown when the superclass cannot be loaded
	 */
	protected boolean isSocketImpl(CtClass clazz) throws NotFoundException {
		CtClass parent = clazz.getSuperclass();
		while(parent!=null && !parent.getName().equals(objectCtClass.getName())) {
			if(parent.getName().equals(SOCK_NAME)) return true;
			parent = parent.getSuperclass();
		}
		return false;
	}
	
	/**
	 * Determines if the passed class implements the passed interface and is not abstract
	 * @param clazz The class to test
	 * @param iface The interface to test for
	 * @return true if the passed class implements the passed interface, false otherwise
	 * @throws NotFoundException thrown on error getting the class interfaces
	 */
	protected boolean implementsInterface(CtClass clazz, CtClass iface) throws NotFoundException {
		if(Modifier.isAbstract(clazz.getModifiers())) return false;
		boolean isIface = false;
		for(CtClass i: clazz.getInterfaces()) {
			if(i.getName().equals(iface.getName())) {
				isIface = true;
				break;
			}
		}
		return isIface;
	}
	
	/**
	 * Instruments the passed {@link SocketImplFactory}
	 * @param socketImplFactoryImpl the javassist representation of a {@link SocketImplFactory} 
	 * @return the class byte code
	 */
	protected byte[] transformSocketImplFactory(CtClass socketImplFactoryImpl) {
		try {
			CtMethod originalMethod = socketImplFactoryImpl.getMethod(createSocketImplMethod.getName(), createSocketImplMethod.getSignature());
			originalMethod.setName("_" + createSocketImplMethod.getName());
			CtMethod newMethod = new CtMethod(socketImplCtClass, createSocketImplMethod.getName(), new CtClass[0], socketImplFactoryImpl);
			newMethod.setBody("return new TrackingSocketImpl(" + originalMethod.getName() + ");");
			return socketImplFactoryImpl.toBytecode();
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			throw new RuntimeException("Failed to instrument [" + socketImplFactoryImpl.getName() + "]", ex);
		}		
	}
	
	/**
	 * @param socketImplImpl
	 * @return
	 */
	protected byte[] transformSocketImpl(CtClass socketImplImpl) {
		try {
			socketImplImpl.addInterface(iSocketImplCtClass);
			for(CtMethod method: iSocketImplCtClass.getDeclaredMethods()) {
				CtMethod toPub = socketImplImpl.getMethod(method.getName(), method.getSignature());
				toPub.setModifiers(toPub.getModifiers() & ~Modifier.ABSTRACT);
				toPub.setModifiers(toPub.getModifiers() & Modifier.PUBLIC);
			}
			return socketImplImpl.toBytecode();
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
			throw new RuntimeException("Failed to instrument [" + socketImplImpl.getName() + "]", ex);
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
		

}
