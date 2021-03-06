/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2013, Helios Development Group and individual contributors
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
package org.helios.apmrouter.groovy.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * <p>Title: Scheduled</p>
 * <p>Description: Annotates a method in a groovy script that should be invoked on a schedule defined in the annotation instance</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.groovy.annotations.Scheduled</code></p>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Scheduled {
	/**
	 * The period in ms. at which the annotated method should be excuted
	 */
	public long period();
	
	/**
	 * The unit of the period
	 */
	public TimeUnit unit() default TimeUnit.MILLISECONDS;
	
	/**
	 * If true, task will be scheduled on a fixed delay, otherwise at a fixed rate
	 */
	public boolean fixedDelay() default true;
	
	/**
	 * The zero parameter method name to call in order to cancel the scheduled task.
	 * Can also be specified as <b><code>stop#</code></p> to prefix the <b><code>@Scheduled</code></p> 
	 * annotated method name as a stop method. 
	 */
	public String stopOn() default "stop";
}
