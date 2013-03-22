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

import javassist.*;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.atomic.AtomicInteger;



/**
 * <p>Title: TraceCollection</p>
 * <p>Description: Enumerates the data points that can be collected in a method interception</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.instrumentation.TraceCollection</code></p>
 */

public enum TraceCollection {
	/** Rolls the TXContext */
	TXROLL(new TXRoll()),
	/** Rolls the TXContext */
	TXCLEAR(new TXClear()),
	
	/** The elapsed time in ns. */
	TIMENS(new TimeNsInstrumentor()),
	/** The elapsed time in ms. */
	TIME(new TimeInstrumentor()),
	/** The number of times the thread was blocked */
	BLOCKS(new TimeInstrumentor()),
	/** The amount of time the thread was blocked in ms. */
	BLOCKTIME(new TimeInstrumentor()),
	/** The number of times the thread waited */
	WAITS(new TimeInstrumentor()),	
	/** The amount of time the thread waited in ms. */
	WAITTIME(new TimeInstrumentor()),
	/** The amount of CPU time the thread consumed in ns. */
	CPU(new TimeInstrumentor()),	
	/** THe number of threads concurrently executing through the instrumented method */
	CONCURRENCY(new TimeInstrumentor()),
	/** The number of times the method has been invoked */
	INVOCATIONS(new TimeInstrumentor()),
	/** The number of times the method returned successfully */
	RETURNS(new TimeInstrumentor()),
	/** The number of times the method invocation resulted in a thrown exception */
	EXCEPTIONS(new TimeInstrumentor()),
	/** Traces the exception stack trace */
	EXCEPTIONSTRACE(new TimeInstrumentor());
	
	/** The name of the created tracer field */
	public static final String TRACER_FIELD = "_$_tracer";
	
	/** The thread MX bean */
	public static final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
	
	/** Indicates if thread contention monitoring is supported */
	public static final boolean THREAD_CONT_SUPPORTED = threadMXBean.isThreadContentionMonitoringSupported();
	/** Indicates if thread cpu time is supported */
	public static final boolean THREAD_CPU_SUPPORTED = threadMXBean.isThreadCpuTimeSupported();
	
	private TraceCollection(Instrumentor instrumentor) {
		this.instrumentor = instrumentor;
	}
	
	private final Instrumentor instrumentor;
	
	/**
	 * Adds the pre-invocation instrumentation byte code to the wrapping method in the passed class
	 * @param clazz The class to instrument
	 * @param renamedMethod The method being instrumented (wrapped)
	 * @param wrapperMethod The method that wraps the intercepted method
	 * @param ti The instrumentation annotation
	 * @param body The body of the wrapping method
	 * @throws CannotCompileException  thrown if the generated code cannot be compiled
	 * @throws NotFoundException  thrown if a targetted javassist lookup does not resolved
	 */
	public void addPreInvoke(CtClass clazz, CtMethod renamedMethod, CtMethod wrapperMethod, TraceImpl ti, final StringBuilder body) throws CannotCompileException, NotFoundException {
		instrumentor.addPreInvoke(clazz, renamedMethod, wrapperMethod, ti, body);
	}
	
	/**
	 * Adds the post-invocation instrumentation byte code to the wrapping method in the passed class
	 * @param clazz The class to instrument
	 * @param renamedMethod The method being instrumented (wrapped)
	 * @param wrapperMethod The method that wraps the intercepted method
	 * @param ti The instrumentation annotation
	 * @param body The body of the wrapping method
	 * @throws CannotCompileException  thrown if the generated code cannot be compiled
	 * @throws NotFoundException  thrown if a targetted javassist lookup does not resolved
	 */
	public void addPostInvoke(CtClass clazz, CtMethod renamedMethod, CtMethod wrapperMethod, TraceImpl ti, final StringBuilder body) throws CannotCompileException, NotFoundException {
		instrumentor.addPostInvoke(clazz, renamedMethod, wrapperMethod, ti, body);
	}

	
	
	/**
	 * <p>Title: Instrumentor</p>
	 * <p>Description: Defines a TraceCollection member instrumentation operation</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.instrumentation.TraceCollection.Instrumentor</code></p>
	 */
	protected static interface Instrumentor {
		/**
		 * Adds the pre-invocation instrumentation byte code to the wrapping method in the passed class
		 * @param clazz The class to instrument
		 * @param renamedMethod The method being instrumented (wrapped)
		 * @param wrapperMethod The method that wraps the intercepted method
		 * @param ti The instrumentation annotation
		 * @param body The body of the wrapping method
		 * @throws CannotCompileException  thrown if the generated code cannot be compiled
		 * @throws NotFoundException  thrown if a targetted javassist lookup does not resolved
		 */
		public void addPreInvoke(CtClass clazz, CtMethod renamedMethod, CtMethod wrapperMethod, TraceImpl ti, final StringBuilder body) throws CannotCompileException, NotFoundException;
		
		/**
		 * Adds the post-invocation instrumentation byte code to the wrapping method in the passed class
		 * @param clazz The class to instrument
		 * @param renamedMethod The method being instrumented (wrapped)
		 * @param wrapperMethod The method that wraps the intercepted method
		 * @param ti The instrumentation annotation
		 * @param body The body of the wrapping method
		 * @throws CannotCompileException  thrown if the generated code cannot be compiled
		 * @throws NotFoundException  thrown if a targetted javassist lookup does not resolved
		 */
		public void addPostInvoke(CtClass clazz, CtMethod renamedMethod, CtMethod wrapperMethod, TraceImpl ti, final StringBuilder body) throws CannotCompileException, NotFoundException;
	}
	
	
	/**
	 * <p>Title: TimeInstrumentor</p>
	 * <p>Description: Instruments a method for measuring elapsed time in ms.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.instrumentation.TraceCollectionTime.Instrumentor</code></p>
	 */
	protected static class TimeInstrumentor implements Instrumentor {

		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.instrumentation.TraceCollection.Instrumentor#addPreInvoke(javassist.CtClass, javassist.CtMethod, javassist.CtMethod, org.helios.apmrouter.instrumentation.TraceImpl, java.lang.StringBuilder)
		 */
		@Override
		public void addPreInvoke(CtClass clazz, CtMethod renamedMethod, CtMethod wrapperMethod, TraceImpl ti, final StringBuilder body) throws CannotCompileException, NotFoundException {
			body.append("long start = System.currentTimeMillis();\n");
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.instrumentation.TraceCollection.Instrumentor#addPostInvoke(javassist.CtClass, javassist.CtMethod, javassist.CtMethod, org.helios.apmrouter.instrumentation.TraceImpl, java.lang.StringBuilder)
		 */
		@Override
		public void addPostInvoke(CtClass clazz, CtMethod renamedMethod, CtMethod wrapperMethod, TraceImpl ti, final StringBuilder body) throws CannotCompileException, NotFoundException {
			body.append("long elapsed = System.currentTimeMillis()-start;\n");
			String namespaceFieldName = wrapperMethod.getName() + "_" + renamedMethod.getSignature().hashCode();
			body.append(TRACER_FIELD).append(".traceGauge(elapsed, \"").append("".equals(ti.getName()) ? "ElapsedTimeMs" : ti.getName() + "Ms").append("\", ").append(namespaceFieldName).append(");\n");
		}
		
	}
	
	/**
	 * <p>Title: TimeNsInstrumentor</p>
	 * <p>Description: Instruments a method for measuring elapsed time in ns.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.instrumentation.TraceCollection.TimeNsInstrumentor</code></p>
	 */
	protected static class TimeNsInstrumentor implements Instrumentor {

		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.instrumentation.TraceCollection.Instrumentor#addPreInvoke(javassist.CtClass, javassist.CtMethod, javassist.CtMethod, org.helios.apmrouter.instrumentation.TraceImpl, java.lang.StringBuilder)
		 */
		@Override
		public void addPreInvoke(CtClass clazz, CtMethod renamedMethod, CtMethod wrapperMethod, TraceImpl ti, final StringBuilder body) throws CannotCompileException, NotFoundException {
			body.append("long start = System.nanoTime();\n");
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.instrumentation.TraceCollection.Instrumentor#addPostInvoke(javassist.CtClass, javassist.CtMethod, javassist.CtMethod, org.helios.apmrouter.instrumentation.TraceImpl, java.lang.StringBuilder)
		 */
		@Override
		public void addPostInvoke(CtClass clazz, CtMethod renamedMethod, CtMethod wrapperMethod, TraceImpl ti, final StringBuilder body) throws CannotCompileException, NotFoundException {
			body.append("long elapsed = System.nanoTime()-start;\n");
			String namespaceFieldName = wrapperMethod.getName() + "_" + renamedMethod.getSignature().hashCode();
			body.append(TRACER_FIELD).append(".traceGauge(elapsed, \"").append("".equals(ti.getName()) ? "ElapsedTimeNs" : ti.getName() + "Ns").append("\", ").append(namespaceFieldName).append(");\n");
		}
		
	}
	
	protected static class TXRoll implements Instrumentor {

		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.instrumentation.TraceCollection.Instrumentor#addPreInvoke(javassist.CtClass, javassist.CtMethod, javassist.CtMethod, org.helios.apmrouter.instrumentation.TraceImpl, java.lang.StringBuilder)
		 */
		@Override
		public void addPreInvoke(CtClass clazz, CtMethod renamedMethod, CtMethod wrapperMethod, TraceImpl ti, final StringBuilder body) throws CannotCompileException, NotFoundException {
			body.append("TXContext.rollContext();\n");
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.instrumentation.TraceCollection.Instrumentor#addPostInvoke(javassist.CtClass, javassist.CtMethod, javassist.CtMethod, org.helios.apmrouter.instrumentation.TraceImpl, java.lang.StringBuilder)
		 */
		@Override
		public void addPostInvoke(CtClass clazz, CtMethod renamedMethod, CtMethod wrapperMethod, TraceImpl ti, final StringBuilder body) throws CannotCompileException, NotFoundException {
		}
		
	}
	
	/**
	 * <p>Title: TXClear</p>
	 * <p>Description: Joinpoint to clear the TXContext at the end of a method</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.instrumentation.TraceCollection.TXClear</code></p>
	 */
	protected static class TXClear implements Instrumentor {

		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.instrumentation.TraceCollection.Instrumentor#addPreInvoke(javassist.CtClass, javassist.CtMethod, javassist.CtMethod, org.helios.apmrouter.instrumentation.TraceImpl, java.lang.StringBuilder)
		 */
		@Override
		public void addPreInvoke(CtClass clazz, CtMethod renamedMethod, CtMethod wrapperMethod, TraceImpl ti, final StringBuilder body) throws CannotCompileException, NotFoundException {
			
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.instrumentation.TraceCollection.Instrumentor#addPostInvoke(javassist.CtClass, javassist.CtMethod, javassist.CtMethod, org.helios.apmrouter.instrumentation.TraceImpl, java.lang.StringBuilder)
		 */
		@Override
		public void addPostInvoke(CtClass clazz, CtMethod renamedMethod, CtMethod wrapperMethod, TraceImpl ti, final StringBuilder body) throws CannotCompileException, NotFoundException {
			body.append("TXContext.clearContext();\n");
		}
		
	}
	
	
	static final CtClass atomicIntegerClazz;
	
	static {
		try {
			atomicIntegerClazz = ClassPool.getDefault().get(AtomicInteger.class.getName());
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * <p>Title: TXConcurrency</p>
	 * <p>Description: Joinpoint to track the number of threads concurrently executing in the body of the instrumented method</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.instrumentation.TraceCollection.TXConcurrency</code></p>
	 */
	protected static class TXConcurrency implements Instrumentor {

		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.instrumentation.TraceCollection.Instrumentor#addPreInvoke(javassist.CtClass, javassist.CtMethod, javassist.CtMethod, org.helios.apmrouter.instrumentation.TraceImpl, java.lang.StringBuilder)
		 */
		@Override
		public void addPreInvoke(CtClass clazz, CtMethod renamedMethod, CtMethod wrapperMethod, TraceImpl ti, final StringBuilder body) throws CannotCompileException, NotFoundException {
			clazz.addField(new CtField(atomicIntegerClazz, "concurrency", clazz), " new AtomicInteger(0)");
		}

		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.instrumentation.TraceCollection.Instrumentor#addPostInvoke(javassist.CtClass, javassist.CtMethod, javassist.CtMethod, org.helios.apmrouter.instrumentation.TraceImpl, java.lang.StringBuilder)
		 */
		@Override
		public void addPostInvoke(CtClass clazz, CtMethod renamedMethod, CtMethod wrapperMethod, TraceImpl ti, final StringBuilder body) throws CannotCompileException, NotFoundException {
			
		}
		
	}

	
}
