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

import javax.script.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Title: ScriptManagerWrapper</p>
 * <p>Description: Java Scripting Wrapper class to use scripting in Helios.</p> 
 * <p>Company: Helios Development Group</p>
 * @author Sandeep Malhotra (smalhotra@heliosdev.org)
 */
public class ScriptBeanWrapper {
	/* javax.script.ScriptEngineManager */
	protected ScriptEngineManager scriptManager = null;
	/* javax.script.ScriptEngine for the language requested by the user */
	protected ScriptEngine engine = null;
	/* Map to store bindings that will be applied to ScriptEngineManger before acquring 
	 * an engine for a specific language */
	protected Map<String,Object> scriptBindings = new HashMap<String,Object>();
	/* ScriptBean object that holds information about scripts' language, source etc. */
	protected ScriptBean scriptBean = null;
	/* Actual Interface object for the script.  This Interface is used to call methods on the script.*/
	protected Object scriptBeanInterface = null;
	/* Reference to log */
	protected Logger log = null;
	
	/**
	 * Default constructor that initializes log and ScriptEngineManager
	 */
	public ScriptBeanWrapper(){
		log = Logger.getLogger(ScriptBeanWrapper.class);
		scriptManager = new ScriptEngineManager();
	}
	
	/**
	 * Constructor to instantiate wrapper using ScriptBean object
	 * 
	 * @param scriptBean
	 */
	public ScriptBeanWrapper(ScriptBean scriptBean){
		log = Logger.getLogger(ScriptBeanWrapper.class);
		scriptManager = new ScriptEngineManager();
		this.scriptBean = scriptBean;		
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


	/**
	 * @return the scriptManager
	 */
	public ScriptEngineManager getScriptManager() {
		return scriptManager;
	}


	/**
	 * @return the engine
	 */
	public ScriptEngine getEngine() {
		return engine;
	}


	/**
	 * @return the bindings
	 */
	public Map<String,Object> getScriptBindings() {
		return scriptBindings;
	}

	/**
	 * @param scriptBindings the bindings to set
	 */
	public void setScriptBindings(Map<String,Object> scriptBindings) {
		this.scriptBindings = scriptBindings;
	}	
	
	/**
	 * If you want to call script through an interface, you should call this method everytime to acquire that Interface.
	 * Internally, a cached reference to this Interface will returned until the initial script is modified (file or inline)
	 * 
	 * @return the scriptBeanInterface
	 */
	public Object getScriptBeanInterface() throws ScriptBeanException {
		if(scriptBeanInterface!=null && !scriptBean.isModified() ){
			return scriptBeanInterface;
		} else {
			long start = System.currentTimeMillis();
			initializeBean();
			if(scriptBean.getScriptBeanInterfaceClazz()!=null){
				try{
					scriptBeanInterface = ((Invocable)engine).getInterface(scriptBean.getScriptBeanInterfaceClazz());
					log.debug("ScriptBean for language [ " +scriptBean.getLanguage()+ " ] initialized in: " + (System.currentTimeMillis() - start) +" milliseconds." );
				}catch(IllegalArgumentException iaex){
					throw new ScriptBeanException("An error occured while getting Interface.  Check if scriptBeanInterfaceClazz is a valid Interface.", iaex);
				}
			}else{
				throw new ScriptBeanException("Required attribute scriptBeanInterfaceClazz for ScriptBean is missing.");
			}
			scriptBean.setModified(false);
		}		
		return scriptBeanInterface;
	}

	/**
	 * If you want to call script through an interface, you should call this method everytime to acquire that Interface.
	 * Internally, a cached reference to this Interface will returned until the initial script is modified (file or inline)
	 * 
	 * @return the scriptBeanInterface
	 */
	public Object invokeFunction(String functionName, Object...parameters) throws ScriptBeanException {
		long start = System.currentTimeMillis();
		Object returnedObject = null;
		initializeBean();
		try{
			returnedObject = ((Invocable)engine).invokeFunction(functionName, parameters);
			log.debug("InvokeFuntion for language [ " +scriptBean.getLanguage()+ " ] completed in: " + (System.currentTimeMillis() - start) +" milliseconds." );
		}catch(NoSuchMethodException nsmex){
			throw new ScriptBeanException("An error occured while invoking a function on the script.  Check whether number of parameters passed matches function signature in the script.", nsmex);
		}catch(ScriptException sex){
			throw new ScriptBeanException("An error occured while invoking a function on the script.", sex);
		}catch(NullPointerException npex){
			throw new ScriptBeanException("An error occured while invoking a function on the script.  Check whether function name is null.", npex);
		}
		return returnedObject;
	}	
	
	/**
	 * Method that refreshes required references in case the script source if modified.
	 * 
	 * @throws ScriptBeanException
	 */
	private void initializeBean() throws ScriptBeanException{
		SimpleBindings binding = new SimpleBindings(scriptBindings);
		scriptManager.setBindings(binding);
		if(scriptBean.getLanguage()!=null){
			engine = scriptManager.getEngineByName(scriptBean.getLanguage());
			if(! (engine instanceof Invocable)){
				throw new ScriptBeanException("The Scripting Engine for language [" +scriptBean.getLanguage()+ " ] does not support Invocable Interface." );
			}
			if(scriptBean.getScriptFile()!=null){
				try{
					engine.eval (new BufferedReader (new FileReader (scriptBean.getScriptFile())));
					 
				}catch(ScriptException sex){
					throw new ScriptBeanException("An exception occured while evaluating script from file [ " +scriptBean.getScriptFile()+ " ]", sex);
				}catch (FileNotFoundException fnfex){
					throw new ScriptBeanException("Cannot file script file [ " +scriptBean.getScriptFile()+ " ]", fnfex);
				}
			} else if (scriptBean.getInlineScript()!=null){
				try{
					engine.eval(scriptBean.getInlineScript()); 
				}catch(ScriptException sex){
					throw new ScriptBeanException("An exception occured while evaluating inline script [ " +scriptBean.getInlineScript()+ " ]", sex);
				}
			}
		} else {
			throw new ScriptBeanException("Required attribute language is missing for ScriptBean.");
		}
	}

//	public void printBindings(){
//		Iterator iterator = scriptBindings.entrySet().iterator();
//		while(iterator.hasNext()){
//			Map.Entry<String,Object> singleObj = (Map.Entry<String,Object>)iterator.next();
//			log.info(singleObj.getKey() + " ** " + singleObj.getValue());
//		}
//	}
	
}
