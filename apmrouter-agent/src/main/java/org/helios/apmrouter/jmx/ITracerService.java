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
package org.helios.apmrouter.jmx;

import org.helios.apmrouter.monitor.script.ScriptContainer;
import org.helios.apmrouter.monitor.script.ScriptMonitor;
import org.helios.apmrouter.trace.TracerImpl;

/**
 * <p>Title: ITracerService</p>
 * <p>Description: Service to expose the tracer as a JMX MBean </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.jmx.ITracerService</code></p>
 */
public class ITracerService extends TracerImpl implements ITracerServiceMBean {

	/**
	 * Creates a new ITracerService
	 * @param tracer The delegate tracer
	 */
	public ITracerService(TracerImpl tracer)  {
		super(tracer.getHost(), tracer.getAgent(), tracer.getSubmitter());
	}
	
	/**
	 * Passes the opaque object to the named compiled script for processing
	 * @param scriptName The script name
	 * @param opaque The object to pass
	 */
	public void traceToScript(String scriptName, Object opaque) {
		if(scriptName==null || scriptName.trim().isEmpty()) return;
		ScriptContainer sc = ScriptMonitor.getScript(scriptName.trim().toLowerCase());
		if(sc!=null) {
			try {
				sc.invoke(opaque);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Determines if the passed script name is registered in the script monitor
	 * @param scriptName ther script name to test for
	 * @return true if the passed script name is registered in the script monitor, false otherwise
	 */
	public boolean hasScript(String scriptName)	{
		if(scriptName==null || scriptName.trim().isEmpty()) return false;
		return ScriptMonitor.getScript(scriptName.trim().toLowerCase()) != null;
	}


}
