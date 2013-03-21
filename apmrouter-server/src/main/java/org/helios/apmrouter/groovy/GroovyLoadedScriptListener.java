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
package org.helios.apmrouter.groovy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

/**
 * <p>Title: GroovyLoadedScriptListener</p>
 * <p>Description: A listener that is notified when a groovy class matching the specified criteria is loaded.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.groovy.GroovyLoadedScriptListener</code></p>
 */

public interface GroovyLoadedScriptListener {
	/**
	 * Returns a set of type annotations this listener is interested in
	 * @return a set of type annotations this listener is interested in
	 */
	Set<Class<? extends Annotation>> getScanTypeAnnotations();
	
	/**
	 * Callback to the listener when a groovy class matching the specified type annotations is loaded
	 * @param annotations The annotations that were matched
	 * @param clazz The class that was loaded
	 * @param instance The instance of the class if the class was loaded as a result of a script compilation. Otherwise null.
	 */
	public void onScanType(Set<? extends Annotation> annotations, Class<?> clazz, Object instance);
	
	/**
	 * Returns a set of method annotations this listener is interested in
	 * @return a set of method annotations this listener is interested in
	 */
	Set<Class<? extends Annotation>> getScanMethodAnnotations();

	/**
	 * Callback to the listener when a groovy class matching the specified method annotations is loaded
	 * @param methods A map of sets of annotations that were matched keyed by the method they were found on
	 * @param clazz The class that was loaded
	 * @param instance The instance of the class if the class was loaded as a result of a script compilation. Otherwise null.
	 */
	public void onScanMethod(Map<Method, Set<Annotation>> methods, Class<?> clazz, Object instance);

	
	/**
	 * Returns a set of classes that loaded classes might extend or implement that this listener is interested in
	 * @return a set of classes that loaded classes might extend or implement that this listener is interested in
	 */
	public Set<Class<?>> getScanClasses();
	
	/**
	 * Callback to the listener when a groovy class with the specified inherritance is loaded
	 * @param parentClasses The parent types that were matched
	 * @param clazz The class that was loaded
	 * @param instance The instance of the class if the class was loaded as a result of a script compilation. Otherwise null.
	 */
	public void onScanClasses(Set<Class<?>> parentClasses, Class<?> clazz, Object instance);
	
	
	
}
