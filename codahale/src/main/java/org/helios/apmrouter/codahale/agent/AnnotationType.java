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

import com.yammer.metrics.annotation.Timed;

import javassist.bytecode.annotation.AnnotationImpl;
import javassist.bytecode.annotation.EnumMemberValue;
import javassist.bytecode.annotation.StringMemberValue;




/**
 * <p>Title: AnnotationType</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.codahale.agent.AnnotationType</code></p>
 */

public enum AnnotationType implements AnnotationValuesExtractor {
//    /** Represents the annotation that meters exception on a join-point */
//    EXCEPTION_METERED,
//    /** Represents the annotation that counts invocations of a join-point */
//    COUNTER,
//    /** Represents the annotation that computes statistics regarding a join-point */
//    HISTOGRAM,
    /** Represents the annotation that computes timings of executions of a join-point */
    TIMED(Timed.class, new TimedAnnotationExtractor());
  
    /** Maps annotation classes to the annotation type enum */
    public static final Map<String, AnnotationType> CLASS2TYPE;
    /** Maps annotation type enums to annotation classes */
    public static final Map<AnnotationType, Class<? extends Annotation>> TYPE2CLASS;
    
    static {
    	Map<String, AnnotationType> toType = new HashMap<String, AnnotationType>(10);
    	Map<AnnotationType, Class<? extends Annotation>> toClass = new HashMap<AnnotationType, Class<? extends Annotation>>(10);
    	for(AnnotationType ann: AnnotationType.values()) {
    		toType.put(ann.annotationClazz.getName(), ann);
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
    public static AnnotationType getTypeForAnnotation(Class<?> clazz) {
    	if(clazz==null) throw new IllegalArgumentException("The passed class was null", new Throwable());
    	AnnotationType ann = CLASS2TYPE.get(clazz.getName());
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
    public static boolean isSupportedAnnotation(Object ann) {
    	if(ann==null) return false;
    	return CLASS2TYPE.get(ann.getClass().getName())!=null;
    }
    
    
    // GAUGE ?  Not appropriate for byte code instrumentation ?
    
    private AnnotationType(Class<? extends Annotation> annotationClazz, AnnotationValuesExtractor extractor) {
    	this.annotationClazz = annotationClazz;
    	this.extractor = extractor;
    }
    
    private final Class<? extends Annotation> annotationClazz;
    private final AnnotationValuesExtractor extractor;

	/**
	 * Returns this enum instance's annotation type
	 * @return this enum instance's annotation type
	 */
	public Class<? extends Annotation> getAnnotationClazz() {
		return annotationClazz;
	}
	
	/**
	 * Returns this enum instance's {@link AnnotationValuesExtractor}
	 * @return this enum instance's {@link AnnotationValuesExtractor}
	 */
	public AnnotationValuesExtractor getExtractor() {
		return extractor;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.apmrouter.codahale.agent.AnnotationValuesExtractor#extract(javassist.bytecode.annotation.AnnotationImpl)
	 */
	@Override
	public Map<String, ?> extract(AnnotationImpl annotation) {
		return this.extractor.extract(annotation);
	}
	
	/**
	 * <p>Title: TimedAnnotationExtractor</p>
	 * <p>Description: Extractor for Timed annotation instances</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.apmrouter.codahale.agent.AnnotationType.TimedAnnotationExtractor</code></p>
	 */
	public static class TimedAnnotationExtractor implements AnnotationValuesExtractor {

		/**
		 * {@inheritDoc}
		 * @see org.helios.apmrouter.codahale.agent.AnnotationValuesExtractor#extract(javassist.bytecode.annotation.AnnotationImpl)
		 */
		@Override
		public Map<String, ?> extract(AnnotationImpl annotation) {
			Map<String, Object> map = new HashMap<String, Object>();
			javassist.bytecode.annotation.Annotation ann = annotation.getAnnotation();
			map.put("group", ((StringMemberValue)ann.getMemberValue("group")).getValue());
			map.put("type", ((StringMemberValue)ann.getMemberValue("type")).getValue());
			map.put("scope", ((StringMemberValue)ann.getMemberValue("scope")).getValue());
			map.put("name", ((StringMemberValue)ann.getMemberValue("name")).getValue());
			map.put("rateUnit", ((EnumMemberValue)ann.getMemberValue("rateUnit")).getValue());
			map.put("durationUnit", ((EnumMemberValue)ann.getMemberValue("durationUnit")).getValue());
			return map;
		}
		


		
	}



}
