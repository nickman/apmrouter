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
package org.helios.apmrouter.codahale.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * <p>Title: Timed</p>
 * <p>Description: An annotation for marking a method of a java agent instrumented object as timed. </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.codahale.annotation.Timed</code></p>
 * <p>Source code copied from {@link com.yammer.metrics.annotation.Timed}</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Timed {
    /**
     * The group of the timer.
     */
    String group() default "";

    /**
     * The type of the timer.
     */
    String type() default "";
    
    /**
     * The scope of the timer.
     */
    String scope() default "";
    

    /**
     * The name of the timer.
     */
    String name();

    /**
     * The time unit of the timer's rate.
     */
    TimeUnit rateUnit() default TimeUnit.SECONDS;

    /**
     * The time unit of the timer's duration.
     */
    TimeUnit durationUnit() default TimeUnit.MILLISECONDS;
}
