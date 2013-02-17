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

import org.apache.log4j.Logger;
import org.helios.collector.jmx.AbstractObjectTracer;
import org.helios.apmrouter.util.StringHelper;

/**
 * <p>Title: JMXObjectTracer </p>
 * <p>Description: An object tracer implemtation provided by a POJO</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public class JMXObjectTracer extends AbstractObjectTracer {

	public JMXObjectTracer(){
		log = Logger.getLogger(this.getClass());
	}	
	
	public boolean trace(Object obj) {
/*		try {
			Integer state = (Integer)obj;
			int value = state.intValue()==3?1:0;
			ITracer tracer = (ITracer)bindings.get("tracer");
			String[] tracingNameSpace = (String[])bindings.get("tracingNameSpace");
			tracer.traceSticky(value,getMetricName(), StringHelper.append(tracingNameSpace,true,segmentPrefix), segmentSuffix);
		}catch (Exception e) {
			e.printStackTrace();
			return false;
		}*/
		return true;
	}

}
