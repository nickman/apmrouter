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

import javassist.bytecode.annotation.*;

import java.lang.reflect.Proxy;
import java.util.Arrays;

/**
 * <p>Title: TraceImpl</p>
 * <p>Description: A concrete impl. for the {@link Trace} annotation</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.instrumentation.TraceImpl</code></p>
 */

public class TraceImpl {
	/** The trace annotation's TXDirective value */
	private final TXDirective txContext;
	/** The trace annotation's name */
	private final String name;
	/** The trace annotation's namespace */
	private final String[] namespace;
	/** the runtime performance data points that will be measured on an intercepted method */
	private final TraceCollection[] collections;
	
	

	/** An empty string array */
	protected static final String[] EMPTY_ARR = {};
	/** The default TraceCollection array */
	protected static final TraceCollection[] DEFAULT_TRACE = {TraceCollection.TIME};
	
	/**
	 * Creates a new TraceImpl
	 * @param annotation An opaque annotation object
	 */
	public TraceImpl(Object annotation) {
		Annotation trace = ((AnnotationImpl)Proxy.getInvocationHandler(annotation)).getAnnotation();
		txContext = getTXDirective(trace);
		name = getName(trace);
		namespace = getNamespace(trace);
		collections = getTraceCollections(trace);
	}
	
	/**
	 * Extracts the {@link TXDirective} from the passed opaque annotation
	 * @param trace the class transformer located opaque annotation
	 * @return the annotation's {@link TXDirective} 
	 */
	protected TXDirective getTXDirective(Annotation trace) {
		MemberValue mv = trace.getMemberValue("txcontext");
		if(mv==null) return TXDirective.NOOP;
		return TXDirective.valueOf(((EnumMemberValue)mv).getValue());		
	}
	
	/**
	 * Extracts the name from the passed opaque annotation
	 * @param trace the class transformer located opaque annotation
	 * @return the annotation's name
	 */
	protected String getName(Annotation trace) {
		StringMemberValue mv = (StringMemberValue)trace.getMemberValue("name");
		return mv==null ? "" : mv.getValue();
	}
	
	/**
	 * Extracts the namespace from the passed opaque annotation
	 * @param trace the class transformer located opaque annotation
	 * @return the annotation's namespace
	 */
	protected String[] getNamespace(Annotation trace) {
		MemberValue mv = trace.getMemberValue("namespace");
		if(mv==null) return EMPTY_ARR;
		MemberValue[] values = ((ArrayMemberValue)mv).getValue();
		String[] stringValues = new String[values.length];
		for(int i = 0; i < values.length; i++) {
			stringValues[i] = ((StringMemberValue)values[i]).getValue();
		}
		return stringValues;	
	}
	
	/**
	 * Extracts the collection data points from the passed opaque annotation
	 * @param trace the class transformer located opaque annotation
	 * @return the annotation's specified collection data points
	 */
	protected TraceCollection[] getTraceCollections(Annotation trace) {
		MemberValue mv = trace.getMemberValue("collections");
		if(mv==null) return DEFAULT_TRACE;
		MemberValue[] values = ((ArrayMemberValue)mv).getValue();
		TraceCollection[] collections = new TraceCollection[values.length];
		for(int i = 0; i < values.length; i++) {
			collections[i] = TraceCollection.valueOf(((EnumMemberValue)values[i]).getValue());
		}
		return collections;	
	}
	
	/**
	 * Returns the runtime performance data points that will be measured on an intercepted method 
	 * @return the runtime performance data points that will be measured on an intercepted method 
	 */
	public TraceCollection[] getCollections() {
		return collections;
	}

	/**
	 * Returns 
	 * @return the txContext
	 */
	public TXDirective getTxContext() {
		return txContext;
	}

	/**
	 * Returns 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns 
	 * @return the namespace
	 */
	public String[] getNamespace() {
		return namespace;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TraceImpl [txContext=");
		builder.append(txContext);
		builder.append(", name=");
		builder.append(name);
		builder.append(", namespace=");
		builder.append(Arrays.toString(namespace));
		builder.append(", collections=");
		builder.append(Arrays.toString(collections));		
		builder.append("]");
		return builder.toString();
	}
	
	
	
//	/**
//	 * Specifies a TXContext operation
//	 */
//	public TXDirective txcontext() default TXDirective.NOOP; 
//	/**
//	 * Specifies the metric name
//	 */
//	public String name();
//	/**
//	 * Specifies the metric name
//	 */
//	public String[] namespace() default {};
	
}
