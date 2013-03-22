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
package org.helios.apmrouter.jagent;

import org.helios.apmrouter.jmx.JMXHelper;

import javax.management.ObjectName;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.UnmodifiableClassException;
import java.util.jar.JarFile;

/**
 * <p>Title: Instrumentation</p>
 * <p>Description: MBean exposed [@link java.lang.instrument.Instrumentation}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.jagent.Instrumentation</code></p>
 */

public class Instrumentation implements InstrumentationMBean {
	/** The delegate instrumentation */
	protected final java.lang.instrument.Instrumentation delegate;
	/** The singleton instance */
	private static volatile Instrumentation instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** The MBean's JMX ObjectName */
	public static final ObjectName OBJECT_NAME = JMXHelper.objectName("org.helios.apmrouter.jagent:service=Instrumentation");
	
	/**
	 * Installs the MBean
	 * @param delegate The actual instrumentation instance
	 * @return the singleton instance
	 */
	public static Instrumentation install(java.lang.instrument.Instrumentation delegate) {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new Instrumentation(delegate);
				}
			}
		}
		return instance;
	}
	
	/**
	 * Creates a new Instrumentation
	 * @param delegate The delegate instrumentation
	 */
	private Instrumentation(java.lang.instrument.Instrumentation delegate) {
		this.delegate = delegate;
		JMXHelper.registerMBean(OBJECT_NAME, this);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jagent.InstrumentationMBean#getInstance()
	 */
	@Override
	public java.lang.instrument.Instrumentation getInstance() {
		return delegate;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jagent.InstrumentationMBean#addTransformer(java.lang.instrument.ClassFileTransformer, boolean)
	 */
	@Override
	public void addTransformer(ClassFileTransformer transformer,
			boolean canRetransform) {
		delegate.addTransformer(transformer, canRetransform);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jagent.InstrumentationMBean#addTransformer(java.lang.instrument.ClassFileTransformer)
	 */
	@Override
	public void addTransformer(ClassFileTransformer transformer) {
		delegate.addTransformer(transformer);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jagent.InstrumentationMBean#removeTransformer(java.lang.instrument.ClassFileTransformer)
	 */
	@Override
	public boolean removeTransformer(ClassFileTransformer transformer) {
		return delegate.removeTransformer(transformer);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jagent.InstrumentationMBean#isRetransformClassesSupported()
	 */
	@Override
	public boolean isRetransformClassesSupported() {
		return delegate.isRetransformClassesSupported();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jagent.InstrumentationMBean#retransformClasses(java.lang.Class[])
	 */
	@Override
	public void retransformClasses(Class<?>... classes)
			throws UnmodifiableClassException {
		delegate.retransformClasses(classes);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jagent.InstrumentationMBean#isRedefineClassesSupported()
	 */
	@Override
	public boolean isRedefineClassesSupported() {
		return delegate.isRedefineClassesSupported();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jagent.InstrumentationMBean#redefineClasses(java.lang.instrument.ClassDefinition[])
	 */
	@Override
	public void redefineClasses(ClassDefinition... definitions)
			throws ClassNotFoundException, UnmodifiableClassException {
		delegate.redefineClasses(definitions);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jagent.InstrumentationMBean#isModifiableClass(java.lang.Class)
	 */
	@Override
	public boolean isModifiableClass(Class<?> theClass) {
		return delegate.isModifiableClass(theClass);
	}


	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jagent.InstrumentationMBean#getInitiatedClasses(java.lang.ClassLoader)
	 */
	@Override
	public Class<?>[] getInitiatedClasses(ClassLoader loader) {
		return delegate.getInitiatedClasses(loader);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jagent.InstrumentationMBean#getObjectSize(java.lang.Object)
	 */
	@Override
	public long getObjectSize(Object objectToSize) {
		return delegate.getObjectSize(objectToSize);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jagent.InstrumentationMBean#appendToBootstrapClassLoaderSearch(java.util.jar.JarFile)
	 */
	@Override
	public void appendToBootstrapClassLoaderSearch(JarFile jarfile) {
		delegate.appendToBootstrapClassLoaderSearch(jarfile);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jagent.InstrumentationMBean#appendToSystemClassLoaderSearch(java.util.jar.JarFile)
	 */
	@Override
	public void appendToSystemClassLoaderSearch(JarFile jarfile) {
		delegate.appendToSystemClassLoaderSearch(jarfile);
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jagent.InstrumentationMBean#isNativeMethodPrefixSupported()
	 */
	@Override
	public boolean isNativeMethodPrefixSupported() {
		return delegate.isNativeMethodPrefixSupported();
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.jagent.InstrumentationMBean#setNativeMethodPrefix(java.lang.instrument.ClassFileTransformer, java.lang.String)
	 */
	@Override
	public void setNativeMethodPrefix(ClassFileTransformer transformer,
			String prefix) {
		delegate.setNativeMethodPrefix(transformer, prefix);
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.instrument.Instrumentation#getAllLoadedClasses()
	 */
	@Override
	public Class<?>[] getAllLoadedClasses() {
		return new Class[]{};
	}
}
