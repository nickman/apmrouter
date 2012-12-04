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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.helios.apmrouter.codahale.annotation.Timed;

/**
 * <p>Title: AnnotationType</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.codahale.agent.AnnotationType</code></p>
 */

public enum AnnotationType {
//    /** Represents the annotation that meters exception on a join-point */
//    EXCEPTION_METERED,
//    /** Represents the annotation that counts invocations of a join-point */
//    COUNTER,
//    /** Represents the annotation that computes statistics regarding a join-point */
//    HISTOGRAM,
    /** Represents the annotation that computes timings of executions of a join-point */
    TIMED(Timed.class);
  
    /** Maps annotation classes to the annotation type enum */
    public static final Map<Class<? extends Annotation>, AnnotationType> CLASS2TYPE;
    /** Maps annotation type enums to annotation classes */
    public static final Map<AnnotationType, Class<? extends Annotation>> TYPE2CLASS;
    
    static {
    	Map<Class<? extends Annotation>, AnnotationType> toType = new HashMap<Class<? extends Annotation>, AnnotationType>(10);
    	Map<AnnotationType, Class<? extends Annotation>> toClass = new HashMap<AnnotationType, Class<? extends Annotation>>(10);
    	for(AnnotationType ann: AnnotationType.values()) {
    		toType.put(ann.annotationClazz, ann);
    		toClass.put(ann, ann.annotationClazz);
    	}
    	CLASS2TYPE = Collections.unmodifiableMap(toType);
    	TYPE2CLASS = Collections.unmodifiableMap(toClass);
    }
    
    /**
     * Returns the annotation type enum for the passed annotation class
     * @param clazz The annotation class to get the annotation type enum for
     * @return the annotation type enum 
     */
    public static AnnotationType getTypeForAnnotation(Class<? extends Annotation> clazz) {
    	if(clazz==null) throw new IllegalArgumentException("The passed class was null", new Throwable());
    	AnnotationType ann = CLASS2TYPE.get(clazz);
    	if(ann==null) throw new IllegalArgumentException("The passed class [" + clazz.getName() + "] is not a supported annotation", new Throwable());
    	return ann;
    }
    
    /**
     * Returns the annotation type enum for the passed annotation instance
     * @param ann The annotation instance to get the annotation type enum for
     * @return the annotation type enum 
     */
    public static AnnotationType getTypeForAnnotation(Annotation ann) {
    	if(ann==null) throw new IllegalArgumentException("The passed annotation instance was null", new Throwable());
    	return getTypeForAnnotation(ann.annotationType());
    	
    }
    
    
    /**
     * Determines if the passed annotation instance is a supported type 
     * @param ann The annotation instance to test
     * @return true if the instance's type is supported, false if it not supported or was null
     */
    public static boolean isSupportedAnnotation(Annotation ann) {
    	if(ann==null) return false;
    	return CLASS2TYPE.get(ann.annotationType())!=null;
    }
    
    
    // GAUGE ?  Not appropriate for byte code instrumentation ?
    
    private AnnotationType(Class<? extends Annotation> annotationClazz) {
    	this.annotationClazz = annotationClazz;
    }
    
    private final Class<? extends Annotation> annotationClazz;
}
