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

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;

/**
 * <p>Title: TraceCollection</p>
 * <p>Description: Enumerates the data points that can be collected in a method interception</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.instrumentation.TraceCollection</code></p>
 */

public enum TraceCollection {
	/** The elapsed time in ms. */
	TIME,
	/** The elapsed time in ns. */
	TIMENS,
	/** The number of times the thread was blocked */
	BLOCKS,
	/** The amount of time the thread was blocked in ms. */
	BLOCKTIME,
	/** The number of times the thread waited */
	WAITS,	
	/** The amount of time the thread waited in ms. */
	WAITTIME,
	/** The amount of CPU time the thread consumed in ns. */
	CPU,	
	/** THe number of threads concurrently executing through the instrumented method */
	CONCURRENCY,
	/** The number of times the method has been invoked */
	INVOCATIONS,
	/** The number of times the method returned successfully */
	RETURNS,
	/** The number of times the method invocation resulted in a thrown exception */
	EXCEPTIONS,
	/** Traces the exception stack trace */
	EXCEPTIONSTRACE;
	
	
	/**
	 * <p>Title: Instrumentor</p>
	 * <p>Description: Defines a TraceCollection member instrumentation operation</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.instrumentation.TraceCollection.Instrumentor</code></p>
	 */
	protected interface Instrumentor {
		/**
		 * Adds the supported instrumentation byte code to the passed method in the passed class
		 * @param clazz The class to instrument
		 * @param method The method to instrument
		 * @throws CannotCompileException 
		 * @throws NotFoundException 
		 */
		public void intruments(CtClass clazz, CtMethod method) throws CannotCompileException, NotFoundException ;
	}
	
	/**
	 * <p>Title: AbstractInstrumentor</p>
	 * <p>Description: Abstract intrumentor that does most of the heavy lifting</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.instrumentation.TraceCollection.AbstractInstrumentor</code></p>
	 */
	protected abstract class AbstractInstrumentor implements Instrumentor {
		
		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.instrumentation.TraceCollection.Instrumentor#intruments(javassist.CtClass, javassist.CtMethod)
		 */
		@Override
		public void intruments(CtClass clazz, CtMethod method) throws CannotCompileException, NotFoundException {
			String originalName = method.getName();
			String nname = originalName +"$impl";
		    method.setName(nname);
		    CtMethod wrapperMethod = CtNewMethod.copy(method, originalName, clazz, null);
			StringBuilder body = addPreInvoke(clazz, method, wrapperMethod);
			addPostInvoke(clazz, method, wrapperMethod, body);
			wrapperMethod.setBody(body.toString());
	        clazz.addMethod(wrapperMethod);			
		}
		
		/**
		 * @param clazz
		 * @param renamedMethod
		 * @param wrapperMethod
		 * @return
		 * @throws CannotCompileException
		 * @throws NotFoundException
		 */
		protected StringBuilder addPreInvoke(CtClass clazz, CtMethod renamedMethod, CtMethod wrapperMethod) throws CannotCompileException, NotFoundException {
		    StringBuilder body = new StringBuilder("{\n");
		    doPreInvoke(clazz, renamedMethod, wrapperMethod, body);
		    String type = renamedMethod.getReturnType().getName();	        
	        if (!"void".equals(type)) {
	            body.append(type + " result = ");
	        }
	        body.append(renamedMethod.getName() + "($$);\n");
		    return body;
		}
		
		/**
		 * @param clazz
		 * @param renamedMethod
		 * @param wrapperMethod
		 * @param body
		 * @throws CannotCompileException
		 * @throws NotFoundException
		 */
		protected void addPostInvoke(CtClass clazz, CtMethod renamedMethod, CtMethod wrapperMethod, StringBuilder body) throws CannotCompileException, NotFoundException {
			doPostInvoke(clazz, renamedMethod, wrapperMethod, body);
		}
		
		/**
		 * @param clazz
		 * @param renamedMethod
		 * @param wrapperMethod
		 * @param body
		 * @throws CannotCompileException
		 * @throws NotFoundException
		 */
		protected abstract void doPreInvoke(CtClass clazz, CtMethod renamedMethod, CtMethod wrapperMethod, StringBuilder body) throws CannotCompileException, NotFoundException;
		/**
		 * @param clazz
		 * @param renamedMethod
		 * @param wrapperMethod
		 * @param body
		 * @throws CannotCompileException
		 * @throws NotFoundException
		 */
		protected abstract void doPostInvoke(CtClass clazz, CtMethod renamedMethod, CtMethod wrapperMethod, StringBuilder body) throws CannotCompileException, NotFoundException;
		
	}
	
	/**
	 * <p>Title: TimeInstrumentor</p>
	 * <p>Description: Instruments a method for measuring elapsed time in ms.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.instrumentation.TraceCollectionTime.Instrumentor</code></p>
	 */
	protected class TimeInstrumentor extends AbstractInstrumentor {

		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.instrumentation.TraceCollection.AbstractInstrumentor#doPreInvoke(javassist.CtClass, javassist.CtMethod, javassist.CtMethod, java.lang.StringBuilder)
		 */
		@Override
		protected void doPreInvoke(final CtClass clazz, final CtMethod renamedMethod, final CtMethod wrapperMethod, final StringBuilder body) throws CannotCompileException, NotFoundException {
			// TODO Auto-generated method stub
			
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.instrumentation.TraceCollection.AbstractInstrumentor#doPostInvoke(javassist.CtClass, javassist.CtMethod, javassist.CtMethod, java.lang.StringBuilder)
		 */
		@Override
		protected void doPostInvoke(CtClass clazz, CtMethod renamedMethod,
				CtMethod wrapperMethod, StringBuilder body)
				throws CannotCompileException, NotFoundException {
			// TODO Auto-generated method stub
			
		}
		
	}
}
