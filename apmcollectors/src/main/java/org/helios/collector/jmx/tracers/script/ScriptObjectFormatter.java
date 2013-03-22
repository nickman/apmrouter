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
import org.helios.collector.jmx.tracers.AbstractObjectFormatter;
import org.helios.collector.jmx.tracers.IObjectFormatter;

/**
 * <p>Title: ScriptObjectFormatter</p>
 * <p>Description: An implementation of Object Formatter that's based on the script provided.  
 * The scipt can either be inline or come from a file.  </p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public class ScriptObjectFormatter extends AbstractObjectFormatter {

	protected ScriptBeanWrapper scriptBeanWrapper = null;
	protected ScriptBean scriptBean = null;	
	protected IObjectFormatter formatter = null;
	
	public ScriptObjectFormatter(){
		log = Logger.getLogger(this.getClass());
	}
	
	
	public String format(Object obj) {
		try{
			if(scriptBeanWrapper==null){
				scriptBeanWrapper = new ScriptBeanWrapper(scriptBean);
			}
			formatter = (IObjectFormatter)scriptBeanWrapper.getScriptBeanInterface();
			return formatter.format(obj)+"";
		}catch(Exception ex){
			ex.printStackTrace();
			return "0";
		}
	}

	/**
	 * @return the wrapper
	 */
	public ScriptBeanWrapper getScriptBeanWrapper() {
		return scriptBeanWrapper;
	}

	/**
	 * @param wrapper the wrapper to set
	 */
	public void setScriptBeanWrapper(ScriptBeanWrapper wrapper) {
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
