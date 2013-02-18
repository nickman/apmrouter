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
package org.helios.collector.jmx.tracers;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;


/**
 * <p>Title: CollectionSizeFormatter</p>
 * <p>Description: A formatter implementation that simply returns the size of an array, collection or map.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 * @version $LastChangedRevision: 1167 $
 * $HeadURL: https://helios.dev.java.net/svn/helios/helios-collectors-pre-maven/trunk/src/org/helios/collectors/jmx/tracers/CollectionSizeFormatter.java $
 * $Id: CollectionSizeFormatter.java 1167 2009-03-28 12:03:03Z nwhitehead $
 */
public class CollectionSizeFormatter extends AbstractObjectFormatter {

	/**
	 * Inspects the passed object to extract the size of the underlying collection, map, or array.
	 * If the object is null, or not a collection/map/array or an exception occurs, a <code>"0"</code> will be returned.
	 * @param obj The object to extract a size from.
	 * @return A number in the form of a string representing the size of the underlying object.
	 * @see org.helios.collector.jmx.tracers.collectors.jmx.IObjectFormatter#format(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	public String format(Object obj) {
		if(obj==null) return "0";
		try {
			if(obj instanceof Collection) {
				return "" + ((Collection)obj).size();
			} else if(obj instanceof Map) {
				return "" + ((Map)obj).size();
			}  else if(obj.getClass().isArray()) {
				return "" + Array.getLength(obj);
			}	else {
				return "0";
			}
		} catch (Exception e) {
			return "0";
		}
	}
}
