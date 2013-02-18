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


/**
 * <p>Title: ServiceMBeanStateTracer</p>
 * <p>Description: Simple Tracer to interpret whether an
 * MBean is online based on value for attribute state returned by an MBean Server</p>
 * <p>Company: Helios Development Group</p>
 * @author Whitehead (whitehead.nicholas@gmail.com)
 */
public class ServiceMBeanStateFormatter extends AbstractObjectFormatter {

	/**
	 * Reads a string value, converts to an int, then returns 1 if the value is 3 and 0 for all other values.
	 * @param obj An object presumed to be an Integer.
	 * @return A 1 if the service is <code>Started</code> (3) or 0.
	 */
	public String format(Object obj) {
		try {
			Integer state = (Integer)obj;
			if(state.intValue()==3)
				return "1";
			else
				return "0";
		} catch (Exception e) {
			return "0";
		}
	}


}
