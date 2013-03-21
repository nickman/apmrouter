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
package org.helios.collector.jmx.tracers.script;

import org.apache.log4j.Logger;
import org.helios.collector.jmx.tracers.AbstractObjectTracer;
import org.helios.collector.jmx.tracers.IObjectTracer;

/**
 * <p>Title: ScriptObjectTracer</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public class ScriptObjectTracer extends AbstractObjectTracer {
	
	protected ScriptBeanWrapper scriptBeanWrapper = null;
	protected ScriptBean scriptBean = null;	
	protected IObjectTracer tracer = null;
	
	public ScriptObjectTracer(){
		log = Logger.getLogger(this.getClass());
	}
	
	public boolean trace(Object obj) {
		try{
			if(scriptBeanWrapper==null){
				if(scriptBean!=null){
					scriptBeanWrapper = new ScriptBeanWrapper(scriptBean);
				}else{
					throw new Exception("Require attribute ScriptBean or scriptBeanWrapper is missing.");
				}
			}
			scriptBeanWrapper.getScriptBindings().putAll(bindings);
			tracer = (IObjectTracer)scriptBeanWrapper.getScriptBeanInterface();
			return tracer.trace(obj);
		}catch(java.lang.reflect.UndeclaredThrowableException utex){
			log.error(utex.getCause(),utex);
			return false;
		}catch(Exception ex){
			ex.printStackTrace();
			return false;
		}
	}


	
	/**
	 * @return the wrapper
	 */
	public ScriptBeanWrapper getWrapper() {
		return scriptBeanWrapper;
	}

	/**
	 * @param wrapper the wrapper to set
	 */
	public void setWrapper(ScriptBeanWrapper wrapper) {
		this.scriptBeanWrapper = wrapper;
	}

	/**
	 * @return the scriptBean
	 */
	public ScriptBean getScriptBean() {
		return scriptBean;
	}

	/**
	 * @param scriptBean the scriptBean to set
	 */
	public void setScriptBean(ScriptBean scriptBean) {
		this.scriptBean = scriptBean;
	}
}
