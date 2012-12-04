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

import java.nio.ByteBuffer;
import java.security.ProtectionDomain;

/**
 * <p>Title: InstrumentedClassInfo</p>
 * <p>Description: A container class for instrumented class data</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.codahale.agent.InstrumentedClassInfo</code></p>
 */

public class InstrumentedClassInfo {
	/** The binary name of the class that was instrumented */
	protected final String binaryClassName;
	/** The classloader of the instrumented class */
	protected final ClassLoader classLoader;
	/** The protection domain of the instrumented class */
	protected final ProtectionDomain protectionDomain;
	
	/** The byte code of the original class */
	protected final ByteBuffer originalClassBuffer;
	/** The byte code of the instrumented class */
	protected final ByteBuffer instrumentedClassBuffer;
	
	/**
	 * Creates a new InstrumentedClassInfo
	 * @param binaryClassName The binary name of the class that was instrumented 
	 * @param classLoader The classloader of the instrumented class
	 * @param protectionDomain The protection domain of the instrumented class
	 * @param originalClassByteCode The byte code of the original class 
	 * @param instrumentedClassByteCode The byte code of the instrumented class
	 */
	public InstrumentedClassInfo(String binaryClassName, ClassLoader classLoader, ProtectionDomain protectionDomain, byte[] originalClassByteCode, byte[] instrumentedClassByteCode) {
		super();
		this.binaryClassName = binaryClassName;
		this.classLoader = classLoader;
		this.protectionDomain = protectionDomain;
		originalClassBuffer = ByteBuffer.allocateDirect(originalClassByteCode.length);
		originalClassBuffer.put(originalClassByteCode);
		originalClassBuffer.flip();
		instrumentedClassBuffer = ByteBuffer.allocateDirect(instrumentedClassByteCode.length);
		instrumentedClassBuffer.put(instrumentedClassByteCode);
		instrumentedClassBuffer.flip();
	}

	/**
	 * Returns the binary name of the class that was instrumented
	 * @return the binary name of the class that was instrumented
	 */
	public String getBinaryClassName() {
		return binaryClassName;
	}

	/**
	 * Returns the byte code of the original class
	 * @return the byte code of the original class
	 */
	public byte[] getOriginalClassBuffer() {
		byte[] byteCode = new byte[originalClassBuffer.capacity()];
		originalClassBuffer.get(byteCode);
		return byteCode;
	}

	/**
	 * Returns the byte code of the instrumented class
	 * @return the byte code of the instrumented class
	 */
	public byte[] getInstrumentedClassBuffer() {
		byte[] byteCode = new byte[instrumentedClassBuffer.capacity()];
		instrumentedClassBuffer.get(byteCode);
		return byteCode;
	}
	
	/**
	 * Returns the classloader of the instrumented class
	 * @return the classloader of the instrumented class
	 */
	public ClassLoader getClassLoader() {
		return classLoader;
	}

	/**
	 * Returns the protection domain of the instrumented class
	 * @return the protection domain of the instrumented class
	 */
	public ProtectionDomain getProtectionDomain() {
		return protectionDomain;
	}	

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("InstrumentedClassInfo [binaryClassName=");
		builder.append(binaryClassName);
		builder.append(", classLoader=");
		builder.append(classLoader);		
		builder.append(", originalClassBuffer=");
		builder.append(originalClassBuffer.capacity());
		builder.append(", instrumentedClassBuffer=");
		builder.append(instrumentedClassBuffer.capacity());
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
				+ ((binaryClassName == null) ? 0 : binaryClassName.hashCode());
		result = prime * result
				+ ((classLoader == null) ? 0 : classLoader.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InstrumentedClassInfo other = (InstrumentedClassInfo) obj;
		if (binaryClassName == null) {
			if (other.binaryClassName != null)
				return false;
		} else if (!binaryClassName.equals(other.binaryClassName))
			return false;
		if (classLoader == null) {
			if (other.classLoader != null)
				return false;
		} else if (!classLoader.equals(other.classLoader))
			return false;
		return true;
	}
	
	
	
	
	
	
	
	
}
