/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2012, Helios Development Group and individual contributors
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
package org.helios.apmrouter.codahale.annotation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.yammer.metrics.annotation.Timed;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationDefaultAttribute;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.EnumMemberValue;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.StringMemberValue;

/**
 * <p>Title: TimedImpl</p>
 * <p>Description: Instantiation of the {@link Timed} annotation.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.codahale.annotation.TimedImpl</code></p>
 */

public class TimedImpl {
    /** The group of the timer. */
    protected final String group;
    /** The type of the timer. */
    protected final String type;
    /** The optional scope of the timer. */
    protected final String scope;
    /** The name of the timer. */
    protected final String name;
    /** The rate unit of the timer. */
    protected final TimeUnit rateUnit;
    /** The duration unit of the timer. */
    protected final TimeUnit durationUnit;
    
	/** Scoped CtMethod template for a codahale timer */
	public static final String SCOPED_TIMER_TEMPLATE = "com.yammer.metrics.Metrics.defaultRegistry().newTimer(%s.class, \"%s\", \"%s\", java.util.concurrent.TimeUnit.%s, java.util.concurrent.TimeUnit.%s);";
	/** No Scope CtMethod template for a codahale timer */
	public static final String TIMER_TEMPLATE = "com.yammer.metrics.Metrics.defaultRegistry().newTimer(%s.class, \"%s\",  java.util.concurrent.TimeUnit.%s, java.util.concurrent.TimeUnit.%s);";
    
	/** A map of named default values since we're having issues getting them from javassist */
	public static final Map<String, String> DEFAULT_VALUES;
	
	static {
		Map<String, String> tmp = new HashMap<String, String>();
		tmp.put("name", "");
		tmp.put("group", "");
		tmp.put("type", "");
		tmp.put("scope", "");
		tmp.put("rateUnit", TimeUnit.SECONDS.name());
		tmp.put("durationUnit", TimeUnit.MILLISECONDS.name());
		DEFAULT_VALUES = Collections.unmodifiableMap(tmp);
	}
	
    
	/**
	 * Creates a new TimedImpl
	 * @param group The group of the timer
	 * @param type The type of the timer
	 * @param scope The scope of the timer
	 * @param name The name of the timer
	 * @param rateUnit The rate unit of the timer
	 * @param durationUnit The duration unit of the timer
	 */
	public TimedImpl(String group, String type, String scope, String name, TimeUnit rateUnit, TimeUnit durationUnit) {
		this.group = group;
		this.type = type;
		this.scope = scope;
		this.name = name;
		this.rateUnit = rateUnit;
		this.durationUnit = durationUnit;
	}
	
	/**
	 * Creates a new TimedImpl
	 * @param name The name of the timer
	 */
	public TimedImpl(String name) {
		this.group = "";
		this.type = "";
		this.scope = "";
		this.name = name;
		this.rateUnit = TimeUnit.SECONDS;
		this.durationUnit = TimeUnit.MILLISECONDS;
	}
	
	/**
	 * Returns the javassist field initializer string for the timer created for this annotation impl instance
	 * @param clazzName The class name being instrumented
	 * @return a javassist field initializer string 
	 */
	public String getTimerInitializer(String clazzName) {
		if("".equals(scope)) {
			return String.format(TIMER_TEMPLATE, clazzName, name, durationUnit, rateUnit);
		}
		return String.format(SCOPED_TIMER_TEMPLATE, clazzName, name, scope, durationUnit, rateUnit);
	}
	
	/**
	 * Creates a new TimedImpl from the annotation data extracted from the passed method
	 * @param method The method that is annotated
	 * @throws NotFoundException thrown if supporting classes cannot be found
	 */
	public TimedImpl(CtMethod method) throws NotFoundException {		
		MethodInfo minfo = method.getMethodInfo();
		AnnotationsAttribute attr = (AnnotationsAttribute)minfo.getAttribute(AnnotationsAttribute.visibleTag);
		Annotation an = attr.getAnnotation(Timed.class.getName());
		this.group = getValueFor("group", an);
		this.type = getValueFor("type", an);
		this.scope = getValueFor("scope", an);
		String _name = getValueFor("name", an);
		if("".equals(_name)) {
			this.name = method.getName();
		} else {
			this.name = _name;
		}
		this.rateUnit = TimeUnit.valueOf(getValueFor("rateUnit", an));
		this.durationUnit = TimeUnit.valueOf(getValueFor("durationUnit", an));
	}
	
	
	/**
	 * Extracts the value or default value for the passed annotation attribute name
	 * @param name The annotation attribute name
	 * @param an The annotation
	 * @return The annotation attribute value as a string
	 * @throws NotFoundException thrown if supporting classes cannot be found
	 */
	protected String getValueFor(String name, Annotation an) throws NotFoundException {
		MemberValue mv = an.getMemberValue(name);
		if(mv==null) {
			return DEFAULT_VALUES.get(name);
		}
		if(mv instanceof StringMemberValue) {
			return ((StringMemberValue)mv).getValue();
		} else if(mv instanceof EnumMemberValue) {
			return ((EnumMemberValue)mv).getValue();
		} else {
			throw new RuntimeException("Unexpected member value type [" + mv.getClass().getSimpleName() + "]", new Throwable());
		}
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "TimedImpl [group=" + group + ", type=" + type + ", scope="
				+ scope + ", name=" + name + ", rateUnit=" + rateUnit
				+ ", durationUnit=" + durationUnit + "]";
	}

    
    
    

}
