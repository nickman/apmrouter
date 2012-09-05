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
package org.helios.apmrouter.util;

/**
 * <p>Title: SimpleMath</p>
 * <p>Description: Some simple math routines</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.util.SimpleMath</code></p>
 */

public class SimpleMath {
	/**
	 * Calculates a percentage
	 * @param part The partial share
	 * @param total The total share
	 * @return The percentage that the partial is of the total
	 */
	public static int percent(double part, double total) {
		if(total==0 || part==0) return 0;
		double d = (part/total)*100;
		return (int)d;
	}
	
	/**
	 * Calculates the average
	 * @param count The number of instances
	 * @param total The total
	 * @return the average
	 */
	public static long avg(double count, double total) {
		if(total==0 || count==0) return 0;
		double d = total/count;
		return (long)d;		
	}
}
