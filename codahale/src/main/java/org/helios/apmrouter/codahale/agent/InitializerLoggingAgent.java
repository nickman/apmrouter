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

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
/**
 * <p>Title: InitializerLoggingAgent</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.codahale.agent.InitializerLoggingAgent</code></p>
 */

public class InitializerLoggingAgent implements ClassFileTransformer {
	  private final ClassPool pool = new ClassPool(true);

	  public byte[] transform(ClassLoader loader, String className, Class classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)  {
	    try {
	      if (className.equals("java/lang/ExceptionInInitializerError")) {
	        CtClass klass = pool.makeClass(new ByteArrayInputStream(classfileBuffer));
	        CtConstructor[] ctors = klass.getConstructors();
	        for (int i = 0; i < ctors.length; i++) {
	          ctors[i].insertAfter("this.printStackTrace();");
	        }
	        return klass.toBytecode();
	      } else {
	        return null;
	      }
	    } catch (Throwable t) {
	      return null;
	    }
	  }
	}
